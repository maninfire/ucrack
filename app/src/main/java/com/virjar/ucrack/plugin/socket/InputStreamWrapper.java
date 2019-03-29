package com.virjar.ucrack.plugin.socket;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.virjar.ucrack.plugin.LogUtil;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by virjar on 2018/4/26.<br>在读取数据的时候，产生一个拷贝
 */
public class InputStreamWrapper extends InputStream {
    private Socket socket;
    private InputStream delegate;
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private static final String tag = "SocketMonitor";
    private static ThreadLocal<Object> reentryFlag = new ThreadLocal<>();

    InputStreamWrapper(InputStream delegate, Socket socket) {
        this.delegate = delegate;
        this.socket = socket;
    }


    @Override
    public int read() throws IOException {
        boolean reEntry = reentryFlag.get() != null;
        if (!reEntry) {
            reentryFlag.set(new Object());
        }
        try {
            int data = delegate.read();
            if (reEntry) {
                return data;
            }
            if (data > 0) {
                byteArrayOutputStream.write(data);
                checkAndOut();
            } else if (data < 0) {
                streamEnd = true;

            }
            return data;
        } finally {
            if (!reEntry) {
                reentryFlag.remove();
            }
        }
    }

    @Override
    public int read(@NonNull byte[] b) throws IOException {
        boolean reEntry = reentryFlag.get() != null;

        if (!reEntry) {
            reentryFlag.set(new Object());
        }
        try {
            int readSize = delegate.read(b);
            if (reEntry) {
                return readSize;
            }
            if (readSize > 0) {
                byteArrayOutputStream.write(b, 0, readSize);
                checkAndOut();
            } else if (readSize < 0) {
                streamEnd = true;

            }
            return readSize;
        } finally {
            if (!reEntry) {
                reentryFlag.remove();
            }
        }
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        boolean reEntry = reentryFlag.get() != null;

        if (!reEntry) {
            reentryFlag.set(new Object());
        }
        try {
            int readSize = delegate.read(b, off, len);
            if (reEntry) {
                return readSize;
            }
            if (readSize > 0) {
                byteArrayOutputStream.write(b, off, readSize);
                checkAndOut();
            } else if (readSize < 0) {
                streamEnd = true;

            }
            return readSize;
        } finally {
            if (!reEntry) {
                reentryFlag.remove();
            }
        }
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        boolean reEntry = reentryFlag.get() != null;
        if (!reEntry) {
            reentryFlag.set(new Object());
        }
        try {
            delegate.close();
            if (reEntry) {
                return;
            }
            streamEnd = true;
            checkAndOut();
        } finally {
            if (!reEntry) {
                reentryFlag.remove();
            }
        }
    }

    private synchronized void resetStatus() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        headData = null;
        printState = printStateInit;
        streamEnd = false;
        headerSplitByte = 0;
        contentLength = 0;
        trunckCursor = -1;
        isGzip = false;
        trunckItemList = Lists.newArrayList();
        headers = null;
        charset = null;
        contentType = null;
    }

    private byte[] headData = null;

    private static final int printStateInit = 0;
    private static final int printStateIsHttpResponse = 1;
    private static final int printStateReadHttpHeader = 2;
    private static final int printStateLogPrinted = 3;
    private static final int printStateNotHttpResponse = 4;
    private boolean streamEnd = false;
    private static final int printStateReadBody = 6;
    private static final int printStateReadChuncked = 7;

    private int printState = printStateInit;

    private long headerSplitByte = 0;
    private int contentLength = 0;
    private long trunckCursor = -1;

    private boolean isGzip = false;


    private List<TrunckItem> trunckItemList = Lists.newArrayList();

    private Charset charset = null;
    private String contentType = null;

    private synchronized void checkAndOut() throws IOException {
        if (printState == printStateLogPrinted) {
            return;
        }
        if (byteArrayOutputStream.size() == 0) {
            return;
        }
        if (!LogUtil.isComponentStarted()) {
            return;
        }

        if (printState == printStateNotHttpResponse && streamEnd) {
            printState = printStateLogPrinted;
            logStream(byteArrayOutputStream.toInputStream(), LogUtil.getTrack());
            Log.i(tag, "httpResponse状态错误");
            resetStatus();
            return;
        }

        if (byteArrayOutputStream.size() < 10) {
            //当前读到的数据太短，无法输出
            return;
        }
        if (printState == printStateNotHttpResponse) {
            return;
        }
        if (printState == printStateInit) {
            //test if the input data may be a http response
            byte[] buf = new byte[128];
            try {
                //这个inputStream是一个view，不会消耗额外空间资源，所以我们使用了就丢弃
                InputStream inputStream = byteArrayOutputStream.toInputStream();
                //  int read = inputStream.read(buf, 0, 128);
                long read = HttpStreamUtil.readFully(inputStream, buf);
                //判断是否是http协议，通过头部的HTTP/前缀
                if (maybeHttpResponse(buf, read)) {
                    printState = printStateIsHttpResponse;

                } else {
                    printState = printStateNotHttpResponse;

                }
            } catch (IOException e) {
                e.printStackTrace();
                printState = printStateNotHttpResponse;
            }
        }

        //then the data may be http protocol,
        if (printState == printStateIsHttpResponse) {
            //寻找http的头部
            try {
                int rlen = 0;
                InputStream inputStream = byteArrayOutputStream.toInputStream();
                byte[] buf = new byte[HttpStreamUtil.BUFSIZE];
                int read = inputStream.read(buf, 0, HttpStreamUtil.BUFSIZE);
                while (read > 0) {
                    rlen += read;
                    headerSplitByte = HttpStreamUtil.findHeaderEnd(buf, rlen);
                    if (headerSplitByte > 0) {
                        break;
                    }
                    read = inputStream.read(buf, rlen, HttpStreamUtil.BUFSIZE - rlen);
                }
                if (headerSplitByte == 0) {
                    //还没有读取到完整的header数据
                    return;
                }
                // now parse http header
                if (null == this.headers) {
                    this.headers = new HashMap<>();
                } else {
                    this.headers.clear();
                }

                // Create a BufferedReader for parsing the header.
                BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, rlen)));

                // Decode the header into parms and header java properties
                if (!decodeHeader(hin, this.headers)) {
                    printState = printStateNotHttpResponse;
                    return;
                }
                printState = printStateReadHttpHeader;

            } catch (IOException e) {
                e.printStackTrace();
                printState = printStateNotHttpResponse;
            }
        }
        //头部已经解析成功，解析数据长度
        if (printState == printStateReadHttpHeader) {
            String contentLengthStr = headers.get("Content-Length".toLowerCase(Locale.US));
            if (StringUtils.isNotBlank(contentLengthStr)) {
                contentLength = NumberUtils.toInt(StringUtils.trim(contentLengthStr));
                printState = printStateReadBody;
            } else {
                //test if body is chuncked transfer
                if (StringUtils.equalsIgnoreCase(headers.get("Transfer-Encoding".toLowerCase(Locale.US)), "chunked")) {
                    printState = printStateReadChuncked;
                } else {
                    //warning http response ,lost content length
                    printState = printStateNotHttpResponse;
                }
            }

            contentType = headers.get("Content-Type".toLowerCase(Locale.US));

            //test for gzip
            //Content-Type: application/zip
            if (StringUtils.equalsIgnoreCase(headers.get("Content-Encoding".toLowerCase(Locale.US)), "gzip")) {
                isGzip = true;
            }
            if (StringUtils.containsIgnoreCase(contentType, "application/zip")) {
                isGzip = true;
            }

            //如果是文本，解析数据的编码集，防止数据乱码
            if (contentType != null && contentType.contains(";")) {
                String[] arr2 = contentType.split(";");
                if (arr2[1].contains("=")) {
                    arr2 = arr2[1].split("=");
                    try {
                        charset = Charset.forName(StringUtils.trimToNull(arr2[1]));
                    } catch (UnsupportedCharsetException e) {
                        //ignore
                    }
                }
            }
            if (StringUtils.containsIgnoreCase(contentType, "text") || StringUtils.containsIgnoreCase(contentType, "application/json")) {
                //此时，必然是文本，如果charset解析失败，使用默认兜底
                if (charset == null) {
                    charset = Charsets.UTF_8;
                }
            }


            //解析头部数据
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                headData = IOUtils.toByteArray(byteArrayInputStream, headerSplitByte);
            } catch (IOException e) {
                // not happened
                e.printStackTrace();
                printState = printStateNotHttpResponse;
            }
        }


        //报文完整
        if (printState == printStateReadBody && byteArrayOutputStream.size() >= contentLength + headerSplitByte) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            //byteArrayInputStream.skip(headerSplitByte);
            HttpStreamUtil.consume(byteArrayInputStream, headerSplitByte);
            InputStream httpContent = byteArrayInputStream;

            if (isGzip) {
                try {
                    httpContent = new GZIPInputStream(httpContent);
                } catch (IOException e) {
                    isGzip = false;
                }
            }
            if (StringUtils.startsWithIgnoreCase(headers.get("Content-Type".toLowerCase(Locale.US)), "image/")) {
                httpContent = new ByteArrayInputStream("this content is a image!".getBytes());
                charset = null;
            }
            final InputStream finalInputStream = httpContent;
            final String finalTrack = LogUtil.getTrack();
            final byte[] finalHeaderData = headData;
            LogUtil.outLog(new LogUtil.LogMessage() {
                @Override
                public void handle(OutputStream outputStream) throws IOException {
                    outputStream.write(getPrefix(finalTrack).getBytes());
                    outputStream.write(finalHeaderData);
                    if (charset == null) {
                        IOUtils.copy(finalInputStream, outputStream);
                    } else {
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, Charsets.UTF_8);
                        outputStreamWriter.append(IOUtils.toString(finalInputStream, charset));
                        outputStreamWriter.flush();
                    }
                }
            });
            //the  end state
            printState = printStateLogPrinted;
            resetStatus();
            return;
        }

        if (printState == printStateReadChuncked) {
            //数据分片处理
            if (trunckCursor <= 0) {
                trunckCursor = headerSplitByte;
            }

            InputStream inputStream = byteArrayOutputStream.toInputStream();
            long skipTrunckCursor = HttpStreamUtil.consume(inputStream, trunckCursor);
            if (skipTrunckCursor != trunckCursor) {
                Log.w(tag, "跳过Header错误,真实跳过了：" + skipTrunckCursor + "==应该跳过：" + trunckCursor);
                return;
            }


            InputStream aggregatedStream;

            while (true) {
                //读取8个字节的数据（2个字节为/n/r)
                byte[] buffer = new byte[10];
                int bytes = HttpStreamUtil.readFully(inputStream, buffer);//inputStream.read(buffer);
                if (bytes < 3) {
                    return;
                }

                //找到数据长度所在的cursor位置
                int lineEnd = HttpStreamUtil.findLineEnd(buffer, 10);
                if (lineEnd <= 0) {
                    Log.w(tag, "寻找cursor失败，数据分片不完全");
                    return;
                }

                int chunckLength = Integer.parseInt(new String(buffer, 0, lineEnd - 2, Charsets.UTF_8), 16);


                if (chunckLength == 0) {
                    //output
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    for (TrunckItem trunckItem : trunckItemList) {
                        byteArrayOutputStream.write(trunckItem.getData().toInputStream());
                    }
                    aggregatedStream = byteArrayOutputStream.toInputStream();
                    break;
                }

                TrunckItem trunckItem = new TrunckItem();
                if (10 > lineEnd) {
                    trunckItem.getData().write(buffer, lineEnd, bytes - lineEnd);
                }

                //加2把/n/r给读进去
                byte[] tempBuffer = new byte[chunckLength - bytes + lineEnd];
                ///
                int readBytes = HttpStreamUtil.readFully(inputStream, tempBuffer);
                if (HttpStreamUtil.consume(inputStream, 2) != 2) {
                    return;
                }

                if (readBytes != tempBuffer.length) {
                    return;
                }

                trunckItem.getData().write(tempBuffer);
                trunckItemList.add(trunckItem);
                trunckCursor += lineEnd + chunckLength + 2;

            }

            if (aggregatedStream == null) {
                return;
            }


            InputStream httpChunckedContent = aggregatedStream;
            if (isGzip) {
                try {
                    httpChunckedContent = new GZIPInputStream(httpChunckedContent);
                } catch (IOException e) {
                    isGzip = false;
                }
            }
            if (StringUtils.startsWithIgnoreCase(headers.get("Content-Type".toLowerCase(Locale.US)), "image/")) {
                httpChunckedContent = new ByteArrayInputStream("this content is a image!".getBytes());
                charset = null;
            }

            final InputStream finalInputStream = httpChunckedContent;
            final String track = LogUtil.getTrack();
            final byte[] finalHeaderData = headData;
            LogUtil.outLog(new LogUtil.LogMessage() {
                @Override
                public void handle(OutputStream outputStream) throws IOException {
                    outputStream.write(getPrefix(track).getBytes());
                    outputStream.write(finalHeaderData);
                    if (charset == null) {
                        IOUtils.copy(finalInputStream, outputStream);
                    } else {
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, Charsets.UTF_8);
                        outputStreamWriter.append(IOUtils.toString(finalInputStream, charset));
                        outputStreamWriter.flush();
                    }
                }
            });
            //the  end state
            printState = printStateLogPrinted;
            resetStatus();
            return;
        }


        if (printState == printStateNotHttpResponse && streamEnd) {
            printState = printStateLogPrinted;
            logStream(byteArrayOutputStream.toInputStream(), LogUtil.getTrack());
        }
    }


    private Map<String, String> headers = null;

    private static final String httpResponseMagic = "HTTP/";

    private boolean maybeHttpResponse(byte[] data, long dataLength) {
        //first,find the start of data
        int pos = 0;
        while (pos < dataLength) {
            if (!isWhitespace(data[pos])) {
                break;
            }
            pos++;
        }
        if (pos + httpResponseMagic.length() >= dataLength) {
            return false;
        }
        //then the HTTP/1.1 200 OK
        //read 5 byte
        return StringUtils.equalsIgnoreCase(httpResponseMagic, new String(data, pos, httpResponseMagic.length()));
    }


    private static boolean isWhitespace(final byte ch) {
        return ch == HttpStreamUtil.SP || ch == HttpStreamUtil.HT || ch == HttpStreamUtil.CR || ch == HttpStreamUtil.LF;
    }

    private boolean decodeHeader(BufferedReader in, Map<String, String> headers) {
        try {
            // Read the request line
            String inLine = in.readLine();
            if (inLine == null) {
                // not happen
                return false;
            }

            String line = in.readLine();
            while (line != null && !line.trim().isEmpty()) {
                int p = line.indexOf(':');
                if (p >= 0) {
                    headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
                }
                line = in.readLine();
            }
            return true;
        } catch (IOException ioe) {
            //the exception will not happen
            ioe.printStackTrace();
            return false;
        }
    }

    private void logStream(InputStream inputStream, String track) {
        if (!LogUtil.isComponentStarted()) {
            return;
        }
        LogUtil.outLog(getPrefix(track), inputStream);
    }

    private String getPrefix(String track) {
        int localPort = socket.getLocalPort();
        int remotePort = socket.getPort();
        InetAddress inetAddress = socket.getInetAddress();

        String remoteAddress;
        if (inetAddress != null) {
            remoteAddress = inetAddress.getHostAddress();
        } else {
            remoteAddress = socket.toString();
        }
        String sessionID = "socket_" + socket.hashCode();

        return "Socket response, local port: " + localPort +
                " remote address:" +
                remoteAddress +
                ":" +
                remotePort +
                "\n" +
                "sessionID:" +
                sessionID +
                "\n" +
                "StackTrace:" +
                track +
                "data:" +
                "\n";
    }

    @Override
    public synchronized void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }
}

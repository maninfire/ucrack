package com.virjar.ucrack.plugin.socket;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.ucrack.plugin.LogUtil;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

/**
 * Created by virjar on 2018/4/26.<br>
 * 在写数据的时候，产生一个拷贝
 */

public class OutputStreamWrapper extends OutputStream {
    private Socket socket;
    private OutputStream delegate;
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private static final String tag = "SocketMonitor";
    private Charset charset = null;
    private static ThreadLocal<Object> reentryFlag = new ThreadLocal<>();


    OutputStreamWrapper(OutputStream delegate, Socket socket) {
        this.delegate = delegate;
        this.socket = socket;
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
        boolean reEntry = reentryFlag.get() != null;
        if (!reEntry) {
            reentryFlag.set(new Object());
        }
        try {
            if (reEntry) {
                return;
            }
            byteArrayOutputStream.write(b);
            checkAndOut();
        } finally {
            if (!reEntry) {
                reentryFlag.remove();
            }
        }
    }

    @Override
    public void write(@NonNull byte[] b) throws IOException {
        delegate.write(b);
        boolean reEntry = reentryFlag.get() != null;
        if (!reEntry) {
            reentryFlag.set(new Object());
        }
        try {
            if (reEntry) {
                return;
            }
            byteArrayOutputStream.write(b);
            checkAndOut();
        } finally {
            if (!reEntry) {
                reentryFlag.remove();
            }
        }
    }

    @Override
    public void write(@NonNull byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
        boolean reEntry = reentryFlag.get() != null;
        if (!reEntry) {
            reentryFlag.set(new Object());
        }
        try {
            if (reEntry) {
                return;
            }
            byteArrayOutputStream.write(b, off, len);
            checkAndOut();
        } finally {
            if (!reEntry) {
                reentryFlag.remove();
            }
        }
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();

        checkAndOut();
    }

    enum Method {
        GET,
        PUT,
        POST,
        DELETE,
        HEAD,
        OPTIONS,
        TRACE,
        CONNECT,
        PATCH,
        PROPFIND,
        PROPPATCH,
        MKCOL,
        MOVE,
        COPY,
        LOCK,
        UNLOCK;

//        public static Method lookup(String method) {
//            if (method == null)
//                return null;
//
//            try {
//                return valueOf(method);
//            } catch (IllegalArgumentException e) {
//                // TODO: Log it?
//                return null;
//            }
//        }
    }

    private static class Trie {
        private Map<Byte, Trie> values = Maps.newHashMap();
        private String method = null;

        void addToTree(byte[] data, int index, String worldEntry) {
            if (index >= data.length) {
                //the last
                if (this.method == null) {
                    this.method = worldEntry;
                }
                return;
            }
            Trie trie = values.get(data[index]);
            if (trie == null) {
                trie = new Trie();
                values.put(data[index], trie);
            }
            trie.addToTree(data, index + 1, worldEntry);
        }

        String find(byte[] pingyings, int index) {
            if (index >= pingyings.length) {
                return this.method;
            }

            Trie trie = values.get(pingyings[index]);
            if (trie == null) {
                return this.method;
//                return null;
            }
            return trie.find(pingyings, index + 1);

        }

    }

    private static Trie methodCharacterTree = new Trie();

    static {
        for (Method method : Method.values()) {
            String name = method.name();
            methodCharacterTree.addToTree(name.getBytes(), 0, name);
        }
    }


    private Map<String, String> headers;

    private void logStream(InputStream inputStream, String track) {
        if (!LogUtil.isComponentStarted()) {
            return;
        }
        LogUtil.outLog(getPrefix(track), inputStream);

    }

    private String getPrefix(String track) {
        int localPort = socket.getLocalPort();
        int remotePort = socket.getPort();
        String remoteAddress = socket.getInetAddress().getHostAddress();
        String sessionID = "socket_" + socket.hashCode();
        return "Socket request, local port: " + localPort +
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

    /**
     * print the client out data，guess http protocol,guess gzip stream。this code migrated from NanoHTTPD
     * https://github.com/NanoHttpd/nanohttpd
     */
    private void checkAndOut() throws IOException {
        if (!LogUtil.isComponentStarted()) {
            return;
        }
        if(byteArrayOutputStream.size()==0){
            return;
        }
        try {
            // Read the first 8192 bytes.
            // The full header should fit in here.
            // Apache's default header limit is 8KB.
            // Do NOT assume that a single read will get the entire header
            // at once!
            byte[] buf = new byte[HttpStreamUtil.BUFSIZE];
            int splitbyte = 0;
            int rlen = 0;

            InputStream inputStream = byteArrayOutputStream.toInputStream();

            inputStream.mark(HttpStreamUtil.BUFSIZE);


            //int read = inputStream.read(buf, 0, HttpStreamUtil.BUFSIZE);
            int read = HttpStreamUtil.readFully(inputStream, buf, 10);
            if (read == -1) {
                // socket was been closed,not happend
                return;
            }


//            判断是否是http协议，通过头部的POST、GET、PUT等

            if (StringUtils.isBlank(methodCharacterTree.find(buf, 0))) {
//                logStream(inputStream);
                return;
            }


            //读取到完整的包头
            while (read > 0) {
                rlen += read;
                splitbyte = HttpStreamUtil.findHeaderEnd(buf, rlen);
                if (splitbyte > 0) {
                    break;
                }
                read = inputStream.read(buf, rlen, HttpStreamUtil.BUFSIZE - rlen);
            }

            Map<String, List<String>> parms = new HashMap<>();
            if (null == this.headers) {
                this.headers = new HashMap<>();
            } else {
                this.headers.clear();
            }

            // Create a BufferedReader for parsing the header.
            BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, rlen)));


            // Decode the header into parms and header java properties
            Map<String, String> pre = new HashMap<>();
            if (!decodeHeader(hin, pre, parms, this.headers)) {
                //inputStream.reset();
                logStream(inputStream, LogUtil.getTrack());
                return;
            }

            //now print the http request body,hand gzip
            String contentType = headers.get("Content-Type".toLowerCase(Locale.US));


            //test gzip
            String contentEncoding = headers.get("Content-Encoding".toLowerCase(Locale.US));

            InputStream newInputStream = byteArrayOutputStream.toInputStream();

            final byte[] headData = IOUtils.toByteArray(newInputStream, splitbyte);

            InputStream remainData = inputStream;

            if (contentEncoding != null && StringUtils.equalsIgnoreCase(contentEncoding, "gzip")) {
                remainData = new GZIPInputStream(remainData);
            }
            if (StringUtils.containsIgnoreCase(contentType, "application/zip")) {
                remainData = new GZIPInputStream(remainData);
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


            //上传的数据为图片，则不解析数据
            if (StringUtils.startsWithIgnoreCase(headers.get("Content-Type".toLowerCase(Locale.US)), "image/")) {
                remainData = new ByteArrayInputStream("this content is a image!".getBytes());
                charset = null;
            }


            final InputStream finalInputStream = remainData;
            final String track = LogUtil.getTrack();
            LogUtil.outLog(new LogUtil.LogMessage() {
                @Override
                public void handle(OutputStream outputStream) throws IOException {
                    outputStream.write(getPrefix(track).getBytes());
                    outputStream.write(headData);
                    if (charset == null) {
                        IOUtils.copy(finalInputStream, outputStream);
                    } else {
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                        outputStreamWriter.append(IOUtils.toString(finalInputStream, charset));
                        outputStreamWriter.flush();
                    }
                }
            });
            byteArrayOutputStream = new ByteArrayOutputStream();
            charset = null;
        } catch (IOException e) {
            //TODO
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Decodes the sent headers and loads the data into Key/value pairs
     */
    private boolean decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, List<String>> parms, Map<String, String> headers) {
        try {
            // Read the request line
            String inLine = in.readLine();
            if (inLine == null) {
                return false;
            }

            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens()) {
                return false;
            }

            pre.put("method", st.nextToken());

            if (!st.hasMoreTokens()) {
                return false;
            }

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), parms);
                uri = decodePercent(uri.substring(0, qmi));
            } else {
                uri = decodePercent(uri);
            }

            // If there's another token, its protocol version,
            // followed by HTTP headers.
            // NOTE: this now forces header names lower case since they are
            // case insensitive and vary by client.
            String protocolVersion;
            if (st.hasMoreTokens()) {
                protocolVersion = st.nextToken();
            } else {
                protocolVersion = "HTTP/1.1";
                //  NanoHTTPD.LOG.log(Level.FINE, "no protocol version specified, strange. Assuming HTTP/1.1.");
            }
            String line = in.readLine();
            while (line != null && !line.trim().isEmpty()) {
                int p = line.indexOf(':');
                if (p >= 0) {
                    headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
                }
                line = in.readLine();
            }

            pre.put("uri", uri);
            return true;
        } catch (IOException ioe) {
            //the exception will not happen
            ioe.printStackTrace();
            return false;
        }
    }

    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given Map.
     */
    private void decodeParms(String parms, Map<String, List<String>> p) {
        String queryParameterString;
        if (parms == null) {
            queryParameterString = "";
            return;
        }

        queryParameterString = parms;
        StringTokenizer st = new StringTokenizer(parms, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            String key;
            String value;

            if (sep >= 0) {
                key = decodePercent(e.substring(0, sep)).trim();
                value = decodePercent(e.substring(sep + 1));
            } else {
                key = decodePercent(e).trim();
                value = "";
            }

            List<String> values = p.get(key);
            if (values == null) {
                values = Lists.newArrayList();
                p.put(key, values);
            }

            values.add(value);
        }
    }

    private static String decodePercent(String str) {
        String decoded = null;

        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException var3) {
            Log.i(tag, "Encoding not supported, ignored", var3);
        }

        return decoded;
    }
}

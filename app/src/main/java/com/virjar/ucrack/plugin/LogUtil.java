package com.virjar.ucrack.plugin;

import android.support.annotation.NonNull;

import com.google.common.base.Charsets;
import com.virjar.xposed_extention.SharedObject;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by virjar on 2018/4/26.<br>日志模块，日志写入到文件
 */

public class LogUtil {
    //异步任务队列，最多2048个，超过后忽略日志
    private static BlockingDeque<LogMessage> blockingDeque = new LinkedBlockingDeque<>(2048);
    private static Thread syncLogThread = null;
    private static OutputStream outputStream = null;
    private static byte[] newLine = System.getProperty("line.separator", "\n").getBytes();

    public synchronized static void startRecord() {
        if (syncLogThread != null) {
            return;
        }

        XposedBridge.log("file start");
        File dir = new File(SharedObject.context.getFilesDir(), "monitor_log");
        boolean create = dir.mkdirs();
        String processName = SharedObject.loadPackageParam.processName;
        XposedBridge.log("create file : " + create + ",processName=" + processName);

        processName = processName.replace(":", "_");

        //clear history log file
        File[] fs = dir.listFiles();
        if (fs != null && fs.length > 0) {
            for (File f : fs) {
                if (f.getName().startsWith(processName + "__")) {
                    f.delete();
                }
            }
        }


        File filename = new File(dir, processName + "__" + System.currentTimeMillis() + ".txt");
        XposedBridge.log("filename:" + filename.getAbsolutePath());
        try {
            if (filename.createNewFile()) {
                outputStream = new FileOutputStream(filename, true);
            } else {
                XposedBridge.log("failed to create log file :" + filename.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        XposedBridge.log("start thread");
        syncLogThread = new Thread() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        blockingDeque.take().handle(outputStream);
                        outputStream.write(newLine);
                        //多增加两次回车换行，分割各个报文
                        outputStream.write(newLine);
                        outputStream.write(newLine);
                        outputStream.flush();
                    } catch (InterruptedException e) {
                        blockingDeque.clear();
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        outLog("\n<=========================================================>\n");
        syncLogThread.setDaemon(true);
        syncLogThread.start();
    }

    public synchronized static void stopRecord() {
        if (syncLogThread == null) {
            return;
        }
        if (syncLogThread.isInterrupted()) {
            syncLogThread = null;
            return;
        }
        syncLogThread.interrupt();
        syncLogThread = null;
        IOUtils.closeQuietly(outputStream);
        outputStream = null;
        blockingDeque.clear();
    }

    public static boolean isComponentStarted() {
        return syncLogThread != null;
    }

    public static void outLog(final String message) {
        if (!isComponentStarted()) {
            return;
        }
        blockingDeque.offer(new LogMessage() {
            @Override
            public void handle(OutputStream outputStream) throws IOException {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, Charsets.UTF_8);
                outputStreamWriter.write(message);
                outputStreamWriter.flush();

            }
        });
    }

    public static void outLog(final InputStream inputStream) {
        if (!isComponentStarted()) {
            return;
        }
        blockingDeque.offer(new LogMessage() {
            @Override
            public void handle(OutputStream outputStream) throws IOException {
                IOUtils.copy(inputStream, outputStream);
                outputStream.flush();
            }
        });
    }

    public static void outLog(final byte[] data) {
        if (!isComponentStarted()) {
            return;
        }
        blockingDeque.offer(new LogMessage() {
            @Override
            public void handle(OutputStream outputStream) throws IOException {
                outputStream.write(data);
            }
        });
    }

    public static void outLog(final String tag, final String message) {
        if (!isComponentStarted()) {
            return;
        }
        blockingDeque.offer(new LogMessage() {
            @Override
            public void handle(OutputStream outputStream) throws IOException {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, Charsets.UTF_8);
                outputStreamWriter.append(tag);
                outputStreamWriter.append(message);
                outputStreamWriter.flush();
            }
        });
    }

    public static void outLog(final String tag, final InputStream inputStream) {
        if (!isComponentStarted()) {
            return;
        }
        blockingDeque.offer(new LogMessage() {
            @Override
            public void handle(OutputStream outputStream) throws IOException {
                outputStream.write(tag.getBytes());
                IOUtils.copy(inputStream, outputStream);
            }
        });
    }

    public static void outLog(final String tag, final byte[] data) {
        if (!isComponentStarted()) {
            return;
        }
        blockingDeque.offer(new LogMessage() {
            @Override
            public void handle(OutputStream outputStream) throws IOException {
                outputStream.write(tag.getBytes());
                outputStream.write(data);
            }
        });
    }

    public static void outLog(final LogMessage logMessage) {
        if (!isComponentStarted()) {
            return;
        }
        blockingDeque.offer(new LogMessage() {
            @Override
            public void handle(final OutputStream outputStream) throws IOException {
                logMessage.handle(new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        outputStream.write(b);
                    }

                    @Override
                    public void write(@NonNull byte[] b) throws IOException {
                        outputStream.write(b);
                    }

                    @Override
                    public void write(@NonNull byte[] b, int off, int len) throws IOException {
                        outputStream.write(b, off, len);
                    }

                    @Override
                    public void flush() throws IOException {
                        outputStream.flush();
                    }

                    @Override
                    public void close() throws IOException {
                    }
                });
            }
        });
    }

    public interface LogMessage {
        void handle(OutputStream outputStream) throws IOException;
    }


    public static void outTrack(String append) {
        String msg = append + getTrack();
        outLog(msg);
    }

    public static String getTrack() {
        return getTrack(ThreadPoolHookV2.stackTraceChain());
    }

    public static String getOwnerThreadTrack() {
        return getTrack(new Throwable());
    }

    public static String getTrack(Throwable e) {
        StringBuilder msg = new StringBuilder("\n=============>\n");
        while (e != null) {
            StackTraceElement[] ste = e.getStackTrace();
            for (StackTraceElement stackTraceElement : ste) {
                msg.append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append(":").append(stackTraceElement.getLineNumber()).append("\n");
            }
            e = e.getCause();
            if (e != null) {
                msg.append("cause:").append(e.getMessage()).append("\n\n");
            }
        }
        msg.append("<================\n");
        return msg.toString();
    }

}

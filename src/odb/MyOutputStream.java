package odb;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import pack.Pair;
    
public class MyOutputStream {

    private final int pagesize = 10 * 1024; // 10KB threshold
    private OutputStream os;
    private ObjectOutputStream oos;
    private boolean isodb;
    private boolean oisEnabled;
    private boolean skipSniff;
    private StringBuilder sniffer = new StringBuilder();

    public MyOutputStream(OutputStream os, boolean isodb, ObjectOutputStream oos) {
        this(os, isodb, oos, false);
    }

    public MyOutputStream(OutputStream os, boolean isodb, ObjectOutputStream oos, boolean skipSniff) {
        this.os = os;
        this.isodb = isodb;
        this.oos = oos;
        this.skipSniff = skipSniff;
        this.oisEnabled = (skipSniff && isodb);
    }

    private void ensureOOS() throws IOException {
        if (oos == null) {
            oos = new ObjectOutputStream(os);
            oos.flush();
        }
    }

    public void write(int b) throws IOException {
        if (oisEnabled) {
            ensureOOS();
            oos.writeObject(new RealDescriptor(b));
        } else {
            os.write(b);
            if (!skipSniff) {
                if (!isodb) {
                    sniffer.append((char)b);
                    if (sniffer.toString().contains("X-ODB: true")) isodb = true;
                    if (sniffer.length() > 2000) sniffer.delete(0, 1000);
                }
                if (isodb && !oisEnabled) {
                    if (sniffer.toString().endsWith("\r\n\r\n")) oisEnabled = true;
                }
            }
        }
    }

    public void write(Pair buff, int off, int len) throws IOException {
        if (len > 0) {
            if (oisEnabled) {
                if (!buff._access) {
                    VirtualDescriptor desc = (VirtualDescriptor)buff._desc;
                    if ((off == 0) && (len == desc.length())) {
                        ensureOOS();
                        oos.writeObject(desc);
                        oos.flush();
                        return;
                    } else {
                        byte[] ret = Downloader.download(desc.host, desc.port, desc.payloadid);
                        buff._buff = ret;
                        buff._access = true;
                    }
                }
                ensureOOS();
                if (len > pagesize) {
                    byte[] b = new byte[len];
                    System.arraycopy(buff._buff, off, b, 0, len);
                    Downloader d = Downloader.getInstance();
                    int id = d.addPayload(b);
                    String ip = Downloader.getLocalIP();
                    VirtualDescriptor desc = new VirtualDescriptor(ip, d.getPort(), id, len);
                    oos.writeObject(desc);
                } else {
                    byte[] b = new byte[len];
                    System.arraycopy(buff._buff, off, b, 0, len);
                    oos.writeObject(new RealDescriptor(len, b));
                }
                oos.flush();
            } else {
                if (!buff._access) {
                    VirtualDescriptor desc = (VirtualDescriptor)buff._desc;
                    byte[] ret = Downloader.download(desc.host, desc.port, desc.payloadid);
                    buff._buff = ret;
                    buff._access = true;
                }
                os.write(buff._buff, off, len);
                if (!skipSniff) {
                    if (!isodb) {
                        String s = new String(buff._buff, off, len);
                        sniffer.append(s);
                        if (sniffer.toString().contains("X-ODB: true")) isodb = true;
                        if (sniffer.length() > 2000) sniffer.delete(0, 1000);
                    }
                    if (isodb && !oisEnabled) {
                        if (sniffer.toString().contains("\r\n\r\n")) oisEnabled = true;
                    }
                }
            }
        }
    }

    public void write(Pair buff) throws IOException {
        write(buff, 0, buff.length());
    }

    public void flush() throws IOException {
        if (oos != null) oos.flush();
        os.flush();
    }

    public void close() throws IOException {
        if (oos != null) oos.flush();
        if (oos != null) oos.close();
        else os.close();
    }
}

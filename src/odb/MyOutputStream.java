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

    public void write(int b) throws IOException {
        if (oisEnabled) {
            if (oos == null) oos = new ObjectOutputStream(os);
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
            if (!buff._access) {
                VirtualDescriptor desc = (VirtualDescriptor)buff._desc;
                if (oisEnabled && (off == 0) && (len == desc.len)) {
                    // send virtual (forwarding)
                    System.out.println("MyOutputStream: forward descriptor ("+desc.len+")");
                    if (oos == null) oos = new ObjectOutputStream(os);
                    oos.writeObject(desc);
                    return;
                } else {
                    // download the payload
                    System.out.println("MyOutputStream: download payload ("+desc.len+") from " + desc.host + ":" + desc.port);
                    byte[] ret = Downloader.download(desc.host, desc.port, desc.payloadid);
                    buff._buff = ret;
                    buff._access = true;
                }
            }
            if (oisEnabled) {
                if (oos == null) oos = new ObjectOutputStream(os);
                if (len > pagesize) {
                    // send virtual
                    byte[] b = new byte[len];
                    System.arraycopy(buff._buff, off, b, 0, len);
                    Downloader d = Downloader.getInstance();
                    int id = d.addPayload(b);
                    String ip = Downloader.getLocalIP();
                    VirtualDescriptor desc = new VirtualDescriptor(ip, d.getPort(), id, len);
                    System.out.println("MyOutputStream: virtualize local data ("+len+") at " + ip + ":" + d.getPort());
                    oos.writeObject(desc);
                } else {
                    // send real
                    System.out.println("MyOutputStream: send real data ("+len+")");
                    byte[] b = new byte[len];
                    System.arraycopy(buff._buff, off, b, 0, len);
                    oos.writeObject(new RealDescriptor(len, b));
                }
            } else {
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
        if (!buff._access) {
            VirtualDescriptor desc = (VirtualDescriptor)buff._desc;
            write(buff, 0, desc.len);
        } else
            write(buff, 0, buff._buff.length);
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

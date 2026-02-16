package odb;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import pack.Pair;

public class MyInputStream {

    protected InputStream is;
    private ObjectInputStream ois;
    private boolean isodb;
    private boolean oisEnabled;
    private boolean skipSniff;
    private StringBuilder sniffer = new StringBuilder();

    private byte[] pendingBuff;
    private int pendingOff;
    private int pendingLen;

    public MyInputStream(InputStream is, boolean isodb, ObjectInputStream ois) {
        this(is, isodb, ois, false);
    }

    public MyInputStream(InputStream is, boolean isodb, ObjectInputStream ois, boolean skipSniff) {
        this.is = is;
        this.isodb = isodb;
        this.ois = ois;
        this.skipSniff = skipSniff;
        this.oisEnabled = (skipSniff && isodb) || (ois != null);
    }

    public int read() throws IOException {
        if (oisEnabled) {
            if (pendingLen > 0) {
                int b = pendingBuff[pendingOff++] & 0xFF;
                pendingLen--;
                return b;
            }
            try {
                if (ois == null) ois = new ObjectInputStream(is);
                Object desc = ois.readObject();
                if (desc instanceof RealDescriptor) {
                    RealDescriptor rdesc = (RealDescriptor)desc;
                    if (rdesc.len == 0) return -1;
                    if (rdesc.len == 1) return rdesc.buff[0] & 0xFF;
                    pendingBuff = rdesc.buff;
                    pendingOff = 1;
                    pendingLen = rdesc.len - 1;
                    return rdesc.buff[0] & 0xFF;
                } else {
                    VirtualDescriptor vdesc = (VirtualDescriptor)desc;
                    byte[] payload = Downloader.download(vdesc.host,vdesc.port,vdesc.payloadid);
                    if (vdesc.len == 0) return -1;
                    if (vdesc.len == 1) return payload[0] & 0xFF;
                    pendingBuff = payload;
                    pendingOff = 1;
                    pendingLen = vdesc.len - 1;
                    return payload[0] & 0xFF;
                }
            } catch (Exception e) {
                oisEnabled = false;
            }
        }
        int b = is.read();
        if (b != -1 && !skipSniff) {
            if (!isodb) {
                sniffer.append((char)b);
                if (sniffer.toString().contains("X-ODB: true")) {
                    isodb = true;
                }
                if (sniffer.length() > 2000) sniffer.delete(0, 1000);
            }
            if (isodb && !oisEnabled) {
                if (sniffer.toString().endsWith("\r\n\r\n")) {
                    oisEnabled = true;
                }
            }
        }
        return b;
    }

    public int read(Pair buff, int off, int len) throws IOException {
        if (oisEnabled) {
            if (pendingLen > 0) {
                int toCopy = Math.min(len, pendingLen);
                System.arraycopy(pendingBuff, pendingOff, buff._buff, off, toCopy);
                pendingOff += toCopy;
                pendingLen -= toCopy;
                buff._access = true;
                return toCopy;
            }
            try {
                if (ois == null) ois = new ObjectInputStream(is);
                Object desc = ois.readObject();
                if (desc instanceof RealDescriptor) {
                    RealDescriptor rdesc = (RealDescriptor)desc;
                    int toCopy = Math.min(len, rdesc.len);
                    System.arraycopy(rdesc.buff, 0, buff._buff, off, toCopy);
                    if (rdesc.len > len) {
                        pendingBuff = rdesc.buff;
                        pendingOff = len;
                        pendingLen = rdesc.len - len;
                    }
                    buff._access = true;
                    return toCopy;
                } else {
                    VirtualDescriptor vdesc = (VirtualDescriptor)desc;
                    // ODB specification: don't download yet
                    buff._desc = vdesc;
                    buff._access = false;
                    buff._vlen = vdesc.len;
                    return vdesc.len;
                }
            } catch (Exception e) {
                oisEnabled = false;
            }
        }
        int ret = is.read(buff._buff, off, len);
        if (ret != -1 && !skipSniff) {
            if (!isodb) {
                String s = new String(buff._buff, off, ret);
                sniffer.append(s);
                if (sniffer.toString().contains("X-ODB: true")) {
                    isodb = true;
                }
                if (sniffer.length() > 2000) sniffer.delete(0, 1000);
            }
            if (isodb && !oisEnabled) {
                if (sniffer.toString().contains("\r\n\r\n")) {
                    oisEnabled = true;
                }
            }
        }
        return ret;
    }

    public Pair readAllBytes() throws IOException {
        if (oisEnabled || isodb) {
            try {
                if (ois == null) ois = new ObjectInputStream(is);
                Object obj = ois.readObject();
                if (obj instanceof RealDescriptor) {
                     RealDescriptor rd = (RealDescriptor)obj;
                     return new Pair(rd.buff, true, rd);
                } else if (obj instanceof VirtualDescriptor) {
                     VirtualDescriptor vd = (VirtualDescriptor)obj;
                     // We create a dummy buff of the right size, but keep access=false
                     return new Pair(new byte[0], false, vd);
                }
            } catch (Exception e) {
                // fallback
            }
        }
        byte[] bytes = is.readAllBytes();
        return Pair.wrap(bytes);
    }

    public void close() throws IOException {
        is.close();
    }
}

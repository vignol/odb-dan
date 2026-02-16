package odb;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import pack.Pair;

public class MyInputStream {

    protected InputStream is;
    private ObjectInputStream ois;
    private boolean isodb;

    private byte[] pendingBuff;
    private int pendingOff;
    private int pendingLen;

    public MyInputStream(InputStream is, boolean isodb, ObjectInputStream ois) {
        this.is = is;
        this.isodb = isodb;
        this.ois = ois;
    }

    public int read() throws IOException {
        if (isodb) {
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
                return -1;
            }
        }
        return is.read();
    }

    public int read(Pair buff, int off, int len) throws IOException {
        if (isodb) {
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
                // read a descriptor
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
                    return vdesc.len;
                }
            } catch (Exception e) {
                return -1;
            }
        } else
            return is.read(buff._buff, off, len);
    }

    public Pair readAllBytes() throws IOException {
        byte[] bytes = is.readAllBytes();
        return Pair.wrap(bytes);
    }

    public void close() throws IOException {
        is.close();
    }
}

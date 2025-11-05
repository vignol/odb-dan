package odb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import pack.Pair;

public class MyInputStream {

    protected InputStream is;
    private ObjectInputStream ois;
    private boolean isodb;

    public MyInputStream(InputStream is, boolean isodb, ObjectInputStream ois) {
        this.is = is;
        this.isodb = isodb;
        this.ois = ois;
    }

    public int read() throws IOException {
        if (isodb) {
            try {
                // read a descriptor
                Object desc = ois.readObject();
                if (desc instanceof RealDescriptor) {
                    RealDescriptor rdesc = (RealDescriptor)desc;
                    if (rdesc.len == 1)
                        return rdesc.buff[0];
                } else {
                    VirtualDescriptor vdesc = (VirtualDescriptor)desc;
                    byte[] payload = Downloader.download(vdesc.host,vdesc.port,vdesc.payloadid);
                    if (vdesc.len == 1)
                        return payload[0];
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return is.read();
    }

    public int read(Pair buff, int off, int len) throws IOException {
        if (isodb) {
            try {
                // read a descriptor
                Object desc = ois.readObject();
                if (desc instanceof RealDescriptor) {
                    RealDescriptor rdesc = (RealDescriptor)desc;
                    System.arraycopy(rdesc.buff, 0, buff._buff, off, rdesc.len);
                    buff._access = true;
                    return rdesc.len;
                } else {
                    VirtualDescriptor vdesc = (VirtualDescriptor)desc;
                    byte[] payload = Downloader.download(vdesc.host,vdesc.port,vdesc.payloadid);
                    System.arraycopy(payload, 0, buff._buff, off, vdesc.len);
                    buff._access = true;
                    return vdesc.len;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return 0;
        } else
            return is.read(buff._buff, off, len);
    }

    public Pair readAllBytes() throws IOException {
        byte[] bytes = is.readAllBytes();
        // The data is local, so mark it as accessed.
        return new Pair(bytes, true);
    }

    public void close() throws IOException {
        is.close();
    }
}

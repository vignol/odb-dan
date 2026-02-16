package odb;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import pack.Pair;
    
public class MyOutputStream {

    private final int pagesize = 1*1024*1024;
    private OutputStream os;
    private ObjectOutputStream oos;
    private Downloader d;
    private boolean isodb;

    public MyOutputStream(OutputStream os, boolean isodb, ObjectOutputStream oos) {
        this.os = os;
        this.isodb = isodb;
        this.oos = oos;
        if (isodb) {
            d = new Downloader();
            d.start();
        }
    }

    public void write(int b) throws IOException {
        if (isodb) {
            if (oos == null) oos = new ObjectOutputStream(os);
            oos.writeObject(new RealDescriptor(b));
        } else {
            os.write(b);
        }
    }

    public void write(Pair buff, int off, int len) throws IOException {
        if (len>0)
        if (!buff._access) {
            VirtualDescriptor desc = (VirtualDescriptor)buff._desc;
            if ((off==0) && (len==desc.len) && isodb) {
                // send virtual (forwarding)
                System.out.println("MyOutputStream.write: virtual remote (forwarding "+desc.len+")");
                if (oos == null) oos = new ObjectOutputStream(os);
                oos.writeObject(desc);
                return;
            } else {
                // download the payload
                System.out.println("MyOutputStream.write: download payload ("+desc.len+")");
                byte[] ret = Downloader.download(desc.host, desc.port, desc.payloadid);
                buff._buff = ret;
                buff._access = true;
                // will send the payload below
            }
        }
        if (isodb) {
            if (oos == null) oos = new ObjectOutputStream(os);
            if (len > pagesize) {
                // send virtual
                byte[] b = new byte[len];
                System.arraycopy(buff._buff, off, b, 0, len);
                int id = d.addPayload(b);
                String ip = Downloader.getLocalIP();
                VirtualDescriptor desc = new VirtualDescriptor(ip, d.getPort(), id, len);
                System.out.println("MyOutputStream.write: virtual local ("+desc.len+") at "+ip+":"+d.getPort());
                oos.writeObject(desc);
            } else {
                System.out.println("MyOutputStream.write: real ("+len+")");
                // send real
                byte[] b = new byte[len];
                System.arraycopy(buff._buff, off, b, 0, len);
                oos.writeObject(new RealDescriptor(len, b));
            }
        } else {
            os.write(buff._buff, off, len);
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
        if (oos != null) oos.close();
        os.close();
        if (d != null) {
            d.kill();
        }
    }
}

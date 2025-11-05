package odb;

import java.io.Serializable;

public class RealDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int len;
    public final byte[] buff;

    public RealDescriptor(int len, byte[] buff) {
        this.len = len;
        this.buff = buff;
    }

    public RealDescriptor(int b) {
        this.len = 1;
        this.buff = new byte[1];
        this.buff[0] = (byte)b;
    }
}

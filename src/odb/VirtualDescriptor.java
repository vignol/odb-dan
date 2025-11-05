package odb;

import java.io.Serializable;

public class VirtualDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String host;
    public final int port;
    public final int payloadid;
    public final int len;

    public VirtualDescriptor(String host, int port, int payloadid, int len) {
        this.host = host;
        this.port = port;
        this.payloadid = payloadid;
        this.len = len;
    }
}

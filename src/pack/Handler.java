package pack;

import odb.Downloader;
import odb.VirtualDescriptor;

public class Handler {

    public static void bufferFault(Pair p) {
        if (!p._access && p._desc instanceof VirtualDescriptor) {
            VirtualDescriptor vd = (VirtualDescriptor)p._desc;
            System.out.println("ODB: Fault detected! Downloading payload " + vd.payloadid + " from " + vd.host + ":" + vd.port);
            p._buff = Downloader.download(vd.host, vd.port, vd.payloadid);
            p._access = true;
        }
    }

}

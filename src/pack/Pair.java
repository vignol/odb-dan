package pack;

import odb.VirtualDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class Pair {

    public boolean _access;
    public byte[] _buff;
    public Object _desc;
    
    public Pair(byte[] _buff, boolean _access) {
        this._access = _access;
        this._buff = _buff;
        if (!_access) {
             // We don't have enough info here to create a real VirtualDescriptor.
             // Let's create a placeholder. The actual descriptor will be created
             // by the write method in MyOutputStream.
            this._desc = new VirtualDescriptor(null, 0, 0, _buff != null ? _buff.length : 0);
        }
    }

    public Pair(byte[] _buff, boolean _access, Object _desc) {
        this._buff = _buff;
        this._access = _access;
        this._desc = _desc;
    }

    public static Pair wrap(byte[] b) {
        if (b != null && b.length > 4 && b[0] == (byte)0xAC && b[1] == (byte)0xED) {
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(b))) {
                Object obj = ois.readObject();
                if (obj instanceof VirtualDescriptor) {
                    VirtualDescriptor vd = (VirtualDescriptor)obj;
                    return new Pair(new byte[vd.len], false, vd);
                }
            } catch (Exception e) {
                // Not a serialized descriptor, or different version
            }
        }
        return new Pair(b, true);
    }
}

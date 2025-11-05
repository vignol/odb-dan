package pack;

import odb.VirtualDescriptor;

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

    

}

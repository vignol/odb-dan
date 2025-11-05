package odb;

import java.io.IOException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

public class MyServletOutputStream extends ServletOutputStream {

    private final MyOutputStream mos;

public MyServletOutputStream(jakarta.servlet.ServletOutputStream sos) {
    this.mos = new MyOutputStream(sos, false, null);
}
    public MyServletOutputStream(MyOutputStream mos) {
        this.mos = mos;
    }

    @Override
    public void write(int b) throws IOException {
        mos.write(b);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        // Non utilisé
    }

    // This method is not called directly by source code, but is the target
    // of the bytecode transformation from write(byte[]). It must exist
    // to satisfy the verifier.
    public void write(pack.Pair p) throws IOException {
        mos.write(p);
    }
}

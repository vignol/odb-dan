package odb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import pack.Pair;

public class MyFileInputStream extends MyInputStream {

    public MyFileInputStream(String name) throws FileNotFoundException {
        super(new FileInputStream(name), false, null);
    }

    public Pair readAllBytes() throws IOException {
        // This is not efficient, but it's a simple way to implement this
        // on top of the existing InputStream.
        byte[] bytes = ((FileInputStream)super.is).readAllBytes();
        return new Pair(bytes, true);
    }

}

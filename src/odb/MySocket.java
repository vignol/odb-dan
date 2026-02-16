package odb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import pack.Handler;
import pack.Pair;

public class MySocket {

    Socket s;
    boolean isodb;
    
    static List<String> clients = new ArrayList<>(Arrays.asList(System.getProperty("odb.clients", "localhost:2001,localhost:2002").split(",")));
    static List<Integer> servers = new ArrayList<>(Arrays.stream(System.getProperty("odb.servers", "2001,2002").split(",")).filter(s -> !s.isEmpty()).map(Integer::parseInt).collect(Collectors.toList()));

    static {
        final Function<Pair, Integer> handler = (p) -> {
            VirtualDescriptor d = (VirtualDescriptor)p._desc;
            System.out.println("Handler.bufferFault: download payload("+d.len+") from " + d.host + ":" + d.port);
            p._buff = odb.Downloader.download(d.host, d.port, d.payloadid);
            p._access = true;
            return null;
        };
        Handler.registerHandler(handler);
    }

    public MySocket(String host, int port) throws UnknownHostException, IOException {
        s = new Socket(host, port);
        isodb = isClientODB(host, port);
    }

    public MySocket(Socket s) throws IOException {
        this.s = s;
        isodb = isServerODB(s.getLocalPort());
    }

    private boolean isClientODB(String host, int port) {
        if (Boolean.getBoolean("odb.all")) return true;
        return clients.contains(host + ":" + port) || clients.contains(host);
    }

    private boolean isServerODB(int port) {
        if (Boolean.getBoolean("odb.all")) return true;
        return servers.contains(port);
    }

    public MyOutputStream getOutputStream() throws IOException {
        return new MyOutputStream(s.getOutputStream(), isodb, null);
    }

    public MyInputStream getInputStream() throws IOException {
        return new MyInputStream(s.getInputStream(), isodb, null);
    }

    public void close() throws IOException {
        s.close();
    }
}

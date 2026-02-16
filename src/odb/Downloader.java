package odb;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

// We create on the server side a Downloader instance when a payload is remotely accessible 
// on the client side, the addPayyload() static method allows to register a payload
// on the client side, the download() static method allows to download a payload
public class Downloader extends Thread {

    private boolean running = true;
    private int port;
    private int index = 0;
    private Map<Integer,byte[]> payloads = new HashMap<Integer,byte[]>();

    public Downloader() {
    }

    public int getPort() {
        return this.port;
    }

    public int addPayload(byte[] b) {
        payloads.put(index,b);
        return index++;
    }

    public void kill() {
        running = false;
    }

    public static String getLocalIP() {
        try {
            // In some environments getLocalHost().getHostAddress() returns 127.0.0.1
            // We could use a more robust way if needed, but this is a good start.
            String ip = InetAddress.getLocalHost().getHostAddress();
            if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
                // Try to find a non-loopback address
                java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    java.net.NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;
                    java.util.Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof java.net.Inet4Address) return addr.getHostAddress();
                    }
                }
            }
            return ip;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static byte[] download(String host, int port, int payloadid) {
        try {
            Socket s = new Socket(host, port);
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            oos.writeObject(payloadid);
            byte[] ret = null;
            try {
                ret = (byte[])ois.readObject();
                s.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
            return ret;
        } catch (Exception e) {
            System.err.println("Failed to download payload " + payloadid + " from " + host + ":" + port);
            e.printStackTrace();
            return null;
        }
    }

    public void run() {
        try {
            @SuppressWarnings("resource")
            ServerSocket ss = new ServerSocket();
            ss.bind(null);
            this.port = ss.getLocalPort();
            while (running) {
                Socket s = ss.accept();
                ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                int payloadid = (int)ois.readObject();
                byte[] ret = payloads.get(payloadid);
                oos.writeObject(ret);
                System.out.println("Downloader: provide payload ("+(ret != null ? ret.length : "null")+") for id "+payloadid);
                // payloads.remove(payloadid); // Keep it for subsequent requests
                s.close();
            }
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
    }
}

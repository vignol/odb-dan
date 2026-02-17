package odb;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Downloader extends Thread {

    private static Downloader instance;
    private volatile boolean running = true;
    private int port;
    private int index = 0;
    private Map<Integer, byte[]> payloads = Collections.synchronizedMap(new LinkedHashMap<Integer, byte[]>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest) {
            return size() > 1000;
        }
    });
    private ServerSocket ss;

    private Downloader() {
        super("ODB-Downloader");
        setDaemon(true);
    }

    public static synchronized Downloader getInstance() {
        if (instance == null) {
            instance = new Downloader();
            instance.start();
            while (instance.port == 0) {
                try { Thread.sleep(10); } catch (InterruptedException e) {}
            }
        }
        return instance;
    }

    public int getPort() {
        return this.port;
    }

    public int addPayload(byte[] b) {
        int id = index++;
        payloads.put(id, b);
        return id;
    }

    public void kill() {
        running = false;
        try {
            if (ss != null) ss.close();
        } catch (Exception e) {}
    }

    public static String getLocalIP() {
        try {
            // Priority to a property if provided
            String preferredIP = System.getProperty("odb.localip");
            if (preferredIP != null) return preferredIP;

            InetAddress local = InetAddress.getLocalHost();
            String ip = local.getHostAddress();
            if ("127.0.0.1".equals(ip) || "localhost".equals(ip) || local.isLoopbackAddress()) {
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
        try (Socket s = new Socket(host, port)) {
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            oos.writeObject(payloadid);
            return (byte[]) ois.readObject();
        } catch (Exception e) {
            System.err.println("ODB: Failed to download payload " + payloadid + " from " + host + ":" + port + " - " + e.getMessage());
            return null;
        }
    }

    public void run() {
        try {
            int preferredPort = Integer.getInteger("odb.servers", 0);
            ss = new ServerSocket(preferredPort);
            this.port = ss.getLocalPort();
            System.out.println("ODB: Downloader listening on " + getLocalIP() + ":" + this.port);
            while (running) {
                try (Socket s = ss.accept()) {
                    ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                    Object obj = ois.readObject();
                    if (obj instanceof Integer) {
                        int payloadid = (Integer) obj;
                        byte[] ret = payloads.get(payloadid);
                        oos.writeObject(ret);
                    }
                } catch (Exception e) {
                    if (running) System.err.println("ODB: Downloader accept error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
    }
}

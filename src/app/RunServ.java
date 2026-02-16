package app;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.URLDecoder;
import java.util.List;

public class RunServ {
    static boolean headersSent = false;

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String path = args[1];
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(path, exchange -> {
            headersSent = false;
            try {
                ServletContext mockContext = (ServletContext) java.lang.reflect.Proxy.newProxyInstance(
                    ServletContext.class.getClassLoader(),
                    new Class[] { ServletContext.class },
                    (p2, m2, a2) -> {
                        if (m2.getName().equals("getResourceAsStream")) {
                            String p = (String)a2[0];
                            if (p.startsWith("/")) p = p.substring(1);
                            return new java.io.FileInputStream("bin/out/" + p);
                        }
                        if (m2.getName().equals("getMimeType")) return "image/png";
                        return null;
                    }
                );

                Serv servlet = new Serv() {
                    @Override
                    public ServletContext getServletContext() {
                        return mockContext;
                    }
                };

                HttpServletRequest req = (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class[] { HttpServletRequest.class },
                    (proxy, method, mArgs) -> {
                        if (method.getName().equals("getParameter")) {
                            String query = exchange.getRequestURI().getQuery();
                            if (query == null) return null;
                            for (String pair : query.split("&")) {
                                String[] kv = pair.split("=");
                                if (kv[0].equals(mArgs[0])) return kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
                            }
                            return null;
                        }
                        if (method.getName().equals("getServletContext")) return mockContext;
                        if (method.getName().equals("getHeader")) {
                             List<String> values = exchange.getRequestHeaders().get((String)mArgs[0]);
                             return (values != null && !values.isEmpty()) ? values.get(0) : null;
                        }
                        return null;
                    }
                );

                HttpServletResponse resp = (HttpServletResponse) java.lang.reflect.Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class[] { HttpServletResponse.class },
                    (proxy, method, mArgs) -> {
                        if (method.getName().equals("getOutputStream")) {
                            if (!headersSent) {
                                exchange.sendResponseHeaders(200, 0);
                                headersSent = true;
                            }
                            return new jakarta.servlet.ServletOutputStream() {
                                public void write(int b) throws IOException { exchange.getResponseBody().write(b); }
                                public void write(byte[] b, int o, int l) throws IOException { exchange.getResponseBody().write(b, o, l); }
                                public boolean isReady() { return true; }
                                public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}
                            };
                        }
                        if (method.getName().equals("setContentType")) {
                            exchange.getResponseHeaders().set("Content-Type", (String)mArgs[0]);
                        }
                        if (method.getName().equals("setHeader")) {
                            exchange.getResponseHeaders().set((String)mArgs[0], (String)mArgs[1]);
                        }
                        if (method.getName().equals("setContentLength")) {
                            if (!headersSent) {
                                int len = (int)mArgs[0];
                                exchange.sendResponseHeaders(200, len <= 0 ? -1 : len);
                                headersSent = true;
                            }
                        }
                        return null;
                    }
                );

                servlet.doGet(req, resp);
                if (!headersSent) exchange.sendResponseHeaders(200, -1);
                exchange.close();
            } catch (Exception e) {
                e.printStackTrace();
                try { if (!headersSent) exchange.sendResponseHeaders(500, -1); } catch (Exception ex) {}
                exchange.close();
            }
        });
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
    }
}

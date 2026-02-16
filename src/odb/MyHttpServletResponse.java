package odb;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class MyHttpServletResponse extends HttpServletResponseWrapper {

    private boolean isodb = false;

    public MyHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    public MyHttpServletResponse(HttpServletResponse response, MyHttpServletRequest request) {
        super(response);
        this.isodb = request.isODB();
        if (isodb) {
            System.out.println("MyHttpServletResponse: ODB enabled via request");
            response.setHeader("X-ODB", "true");
        }
    }

    @Override
    public MyServletOutputStream getOutputStream() throws IOException {
        return new MyServletOutputStream(new MyOutputStream(super.getOutputStream(), isodb, null, true));
    }

    @Override
    public void setContentLength(int len) {
        if (!isodb) {
            super.setContentLength(len);
        }
    }

    @Override
    public void setContentLengthLong(long len) {
        if (!isodb) {
            super.setContentLengthLong(len);
        }
    }

    @Override
    public void setHeader(String name, String value) {
        if (isodb && "Content-Length".equalsIgnoreCase(name)) return;
        super.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        if (isodb && "Content-Length".equalsIgnoreCase(name)) return;
        super.addHeader(name, value);
    }
}

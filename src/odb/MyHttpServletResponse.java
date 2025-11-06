package odb;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class MyHttpServletResponse extends HttpServletResponseWrapper {

    public MyHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public MyServletOutputStream getOutputStream() throws IOException {
        return new MyServletOutputStream(super.getOutputStream());
    }
}

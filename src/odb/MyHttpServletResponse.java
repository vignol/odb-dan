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
        }
    }

    @Override
    public MyServletOutputStream getOutputStream() throws IOException {
        return new MyServletOutputStream(new MyOutputStream(super.getOutputStream(), isodb, null));
    }
}

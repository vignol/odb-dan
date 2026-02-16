package odb;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class MyHttpServletRequest extends HttpServletRequestWrapper {
    
    private boolean isodb = false;

    public MyHttpServletRequest(HttpServletRequest request) {
        super(request);
        String odbHeader = request.getHeader("X-ODB");
        if ("true".equals(odbHeader)) {
            this.isodb = true;
            System.out.println("MyHttpServletRequest: ODB enabled via header");
        }
    }

    public boolean isODB() {
        return isodb;
    }

    @Override
    public jakarta.servlet.ServletInputStream getInputStream() throws IOException {
        return new MyServletInputStream(new MyInputStream(super.getInputStream(), isodb, null, true));
    }
}

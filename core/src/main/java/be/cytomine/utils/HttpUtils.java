package be.cytomine.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class HttpUtils {
    public static String getContentFromUrl(String weburl) throws IOException {
        URL url = new URL(weburl);
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        return new String(in.readAllBytes());
    }
}

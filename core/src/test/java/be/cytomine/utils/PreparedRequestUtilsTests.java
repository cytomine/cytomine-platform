package be.cytomine.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PreparedRequestUtilsTests {


    @Test
    public void set_url() {
        PreparedRequest pr = new PreparedRequest();
        pr.setUrl("http://domain.com");
        assertThat(pr.getScheme()).isEqualTo("http");
        assertThat(pr.getHost()).isEqualTo("domain.com");
        assertThat(pr.getPort()).isEqualTo(-1);
        assertThat(pr.getPath()).isEqualTo("");
        assertThat(pr.getHeaders()).containsKey("Host");
        assertThat(pr.getHeaders()).containsEntry("Host", List.of("domain.com"));

        pr = new PreparedRequest();
        pr.setUrl("https://domain.com:10900/basepath");
        assertThat(pr.getScheme()).isEqualTo("https");
        assertThat(pr.getHost()).isEqualTo("domain.com");
        assertThat(pr.getPort()).isEqualTo(10900);
        assertThat(pr.getPath()).isEqualTo("/basepath");
        assertThat(pr.getHeaders()).containsKey("Host");
        assertThat(pr.getHeaders()).containsEntry("Host", List.of("domain.com"));
    }

    @Test
    public void build_path() {
        PreparedRequest pr = new PreparedRequest();
        pr.addPathFragment("a");
        pr.addPathFragment("/b/");
        pr.addPathFragment("c/");
        pr.addPathFragment("/d");
        assertThat(pr.getPath()).isEqualTo("/a/b/c/d");

        pr.addPathFragment("/dir/path,&2%3.jpg", true);
        assertThat(pr.getPath()).isEqualTo("/a/b/c/d/dir/path%2C%262%253.jpg");
    }

    @Test
    public void build_query() {
        PreparedRequest pr = new PreparedRequest();
        pr.addQueryParameter("a", "1");
        pr.addQueryParameter("b", "");
        pr.addQueryParameter("c", "value");
        assertThat(pr.getQuery()).isEqualTo("a=1&c=value");

        pr.addQueryParameter("d", null);
        pr.addQueryParameter("e", 3);
        assertThat(pr.getQuery()).isEqualTo("a=1&c=value&e=3");
    }

    @Test
    public void build_uri() {
        PreparedRequest pr = new PreparedRequest();
        pr.setUrl("https://domain.com:10900/basepath");
        pr.addPathFragment("/b/");
        pr.addPathFragment("c/");
        pr.addQueryParameter("d", null);
        pr.addQueryParameter("e", 3);
        assertThat(pr.getURI().toString()).isEqualTo("https://domain.com:10900/basepath/b/c?e=3");
    }
}

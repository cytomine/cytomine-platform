package be.cytomine.utils;

import be.cytomine.exceptions.ServerException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;


@Getter
@Setter
public class PreparedRequest {

    private String scheme;

    private String host;

    private int port;

    private String path;

    private LinkedHashMap<String, Object> queryParameters;

    private HttpHeaders headers;

    private HttpMethod method;

    private Object body;

    public PreparedRequest() {
        queryParameters = new LinkedHashMap<>();
        headers = new HttpHeaders();
        path = "";
    }

    public void setUrl(String url){
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new ServerException(e.getMessage(), e.getCause());
        }
        this.scheme = uri.getScheme();
        this.host = uri.getHost();
        this.port = uri.getPort();
        this.path = uri.getPath();
        this.headers.add(HttpHeaders.HOST, this.host);
    }

    public void addQueryParameter(String key, Object value) {
        this.queryParameters.put(key, value);
    }


    public void addPathFragment(String fragment) {
        addPathFragment(fragment, false);
    }

    public void addPathFragment(String fragment, boolean encode) {
        if (fragment == null || fragment.isEmpty()) {
            return;
        }
        if (encode) {
            // Apache reverse proxy does not support '%2F' encoding inside a path
            // whereas pims supports both '/' and '%2F'. Therefore, we revert the
            // encoding of the `/` to support routing through an Apache proxy.
            // see issue cm/rnd/cytomine/core/core-ce#84
            fragment = URLEncoder.encode(fragment , StandardCharsets.UTF_8).replace("%2F", "/");
        }
        fragment = org.apache.commons.lang3.StringUtils.strip(fragment, "/");
        this.path += "/" + fragment;
    }

    public String getQuery() {
        return this.queryParameters.entrySet()
                .stream()
                .filter(e -> e.getValue() != null && !e.getValue().toString().isEmpty())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    public URI getURI() {
        try {
            return new URI(this.scheme, null, this.host, this.port, this.path, this.getQuery(), null);
        } catch (URISyntaxException e) {
            throw new ServerException(e.getMessage(), e.getCause());
        }
    }

    public void setJsonBody(JsonObject body) {
        this.body = JsonObject.toJsonString(
                body.entrySet()
                .stream()
                .filter(e -> e.getValue() != null && !e.getValue().toString().isEmpty())
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()))
        );
    }

    public <T> T toObject(Class<T> returnType) {
        if (method.equals(GET)) {
            return new RestTemplate().getForObject(this.getURI(), returnType);
        } else if (method.equals(POST)) {
            return new RestTemplate().postForObject(this.getURI(), this.body, returnType);
        }
        throw new NotImplementedException("toObject is not implemented for method: " + method);
    }

    public <T> ResponseEntity<T> toResponseEntity(ProxyExchange<T> proxy, Class<T> returnType) {
        if (proxy == null) {
            if (method.equals(GET)) {
                HttpEntity<?> request = new HttpEntity<>(this.headers);
                return new RestTemplate().exchange(this.getURI(), this.method, request, returnType);
            } else if (method.equals(POST)) {
                HttpEntity<?> request = new HttpEntity<>(this.body, this.headers);
                return new RestTemplate().exchange(this.getURI(), this.method, request, returnType);
            }
        }
        else {
            if (method.equals(GET)) {
                return proxy.headers(this.headers)
                        .uri(this.getURI())
                        .get();
            } else if (method.equals(POST)) {
                return proxy.headers(this.headers)
                        .uri(this.getURI())
                        .body(this.body)
                        .post();
            }
        }
        throw new NotImplementedException("toResponseEntity is not implemented for method: " + method);
    }
}

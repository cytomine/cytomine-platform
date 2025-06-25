package be.cytomine.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class PimsResponse {
    private byte[] content;
    private Map<String, String> headers;

    public PimsResponse(byte[] content, Map<String, String> headers) {
        this.content = content;
        this.headers = headers;
    }

    public PimsResponse(byte[] content) {
        this(content, new LinkedHashMap<>());
    }

    public void setCacheControlMaxAge(int timeToLive) {
        String cacheControl = headers.get("Cache-Control");
        if (!headers.containsKey("Cache-Control")) {
            headers.put("Cache-Control", "max-age=" + timeToLive);
        } else {
            String[] parts = cacheControl.split(",");
            String header = Arrays.stream(parts)
                    .map(x -> x.startsWith("max-age")? "max-age="+timeToLive : x).collect(Collectors.joining(","));
            headers.put("Cache-Control", header);
        }
    }



}

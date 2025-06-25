package be.cytomine.config.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AppEngineProperties {

    private boolean enabled;

    private String url;

    private String apiBasePath;
}

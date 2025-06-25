package be.cytomine.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = true)
@Getter
@Setter
public class ApplicationProperties {

    private String serverId;

    private String version;

    private String serverURL;

//    private NotificationProperties notification;

    private String storagePath;

    private CustomUIProperties customUI;

    private AuthenticationProperties authentication = new AuthenticationProperties();

    private AppEngineProperties appEngine;

    private String instanceHostWebsite;

    private String instanceHostSupportMail;

    private String instanceHostPhoneNumber;

    @NotNull
    @NotBlank
    private String adminPassword;

    @NotNull
    @NotBlank
    private String adminEmail;

    @NotNull
    @NotBlank
    private String adminPrivateKey;

    @NotNull
    @NotBlank
    private String adminPublicKey;

    @NotNull
    @NotBlank
    private String superAdminPrivateKey;

    @NotNull
    @NotBlank
    private String superAdminPublicKey;

    private String ImageServerPrivateKey;

    private String ImageServerPublicKey;

    private String defaultLanguage;

    private String internalProxyURL;

    private Annotation annotation;

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }
}

@ToString
class Annotation {
    int maxNumberOfPoint;

    public int getMaxNumberOfPoint() {
        return maxNumberOfPoint;
    }

    public void setMaxNumberOfPoint(int maxNumberOfPoint) {
        this.maxNumberOfPoint = maxNumberOfPoint;
    }
}

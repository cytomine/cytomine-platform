package be.cytomine.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class ResourcesUtils {

    @Qualifier("resourceLoader")
    private  static ResourceLoader resourceLoader;

    private static Environment environment;

    public ResourcesUtils(ResourceLoader resourceLoader, Environment environment) {
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }
}

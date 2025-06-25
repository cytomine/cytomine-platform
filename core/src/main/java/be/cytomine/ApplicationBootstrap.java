package be.cytomine;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.config.nosqlmigration.InitialMongodbSetupMigration;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.PermissionService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.database.BootstrapDataService;
import be.cytomine.service.database.BootstrapTestsDataService;
import be.cytomine.service.database.BootstrapUtilsService;
import be.cytomine.service.utils.Dataset;
import be.cytomine.utils.EnvironmentUtils;


import static be.cytomine.service.database.BootstrapTestsDataService.*;

@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties({LiquibaseProperties.class, ApplicationProperties.class})
@Transactional
class ApplicationBootstrap {

    @Autowired
    private final UserRepository userRepository;

    private final ApplicationProperties applicationProperties;

    private final Environment environment;

    @Autowired
    BootstrapDataService bootstrapDataService;

    @Autowired
    BootstrapUtilsService bootstrapUtilDataService;

    private final InitialMongodbSetupMigration initialSetupMigration;

    private final Dataset dataset;

    @Autowired
    BootstrapTestsDataService bootstrapTestsDataService;



    @PostConstruct
    public void init() {

        initialSetupMigration.changeSet();

        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        String cytomineWelcomMessage = "                   _____      _                  _\n" +
                "                  / ____|    | |                (_)\n" +
                "                 | |    _   _| |_ ___  _ __ ___  _ _ __   ___\n" +
                "                 | |   | | | | __/ _ \\| '_ ` _ \\| | '_ \\ / _ \\\n" +
                "                 | |___| |_| | || (_) | | | | | | | | | |  __/\n" +
                "                  \\_____\\__, |\\__\\___/|_| |_| |_|_|_| |_|\\___|\n" +
                "                 |  _ \\  __/ |     | |     | |\n" +
                "                 | |_) ||___/  ___ | |_ ___| |_ _ __ __ _ _ __\n" +
                "                 |  _ < / _ \\ / _ \\| __/ __| __| '__/ _` | '_ \\\n" +
                "                 | |_) | (_) | (_) | |_\\__ \\ |_| | | (_| | |_) |\n" +
                "                 |____/ \\___/ \\___/ \\__|___/\\__|_|  \\__,_| .__/\n" +
                "                                                         | |\n" +
                "                                                         |_|";
        log.info (cytomineWelcomMessage);
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("Environment:" + Arrays.toString(environment.getActiveProfiles()));
        log.info ("Current directory:" + new File( "./" ).getAbsolutePath());
        log.info ("HeadLess:" + java.awt.GraphicsEnvironment.isHeadless());
        log.info ("JVM Args" + ManagementFactory.getRuntimeMXBean().getInputArguments());
        log.info (applicationProperties.toString());
        log.info ("#############################################################################");
        log.info ("#############################################################################");
        log.info ("#############################################################################");

        UrlApi.setServerURL(applicationProperties.getServerURL());

        if (EnvironmentUtils.isTest(environment) && userRepository.count() == 0) {
            bootstrapDataService.initData();
            //noSQLCollectionService.cleanActivityDB() TODO:
            bootstrapUtilDataService.createUser(dataset.ADMINLOGIN, "Just an", "Admin", List.of("ROLE_USER", "ROLE_ADMIN"));
            bootstrapUtilDataService.createUser(dataset.ANOTHERLOGIN, "Just another", "User", List.of("ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"));
            bootstrapUtilDataService.createUser(dataset.SUPERADMINLOGIN, "Super", "Admin", List.of("ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"));

            // We need these users for all authorization tests
            // So we create them at the beginning in order to avoid creating them before each authorization tests
            bootstrapTestsDataService.createUserForTests(ADMIN);
            bootstrapTestsDataService.createUserForTests(SUPERADMIN);
            bootstrapTestsDataService.createUserForTests(USER_ACL_READ);
            bootstrapTestsDataService.createUserForTests(USER_ACL_WRITE);
            bootstrapTestsDataService.createUserForTests(USER_ACL_CREATE);
            bootstrapTestsDataService.createUserForTests(USER_ACL_DELETE);
            bootstrapTestsDataService.createUserForTests(USER_ACL_ADMIN);
            bootstrapTestsDataService.createUserForTests(USER_NO_ACL);
            bootstrapTestsDataService.createUserForTests(GUEST);
            bootstrapTestsDataService.createUserForTests(CREATOR);


        } else if (userRepository.count() == 0) {
            //if database is empty, put minimal data
            bootstrapDataService.initData();
        }

        // Deprecated API keys. Will be removed in a future release. Still used for communication PIMS->core for now
        if (applicationProperties.getImageServerPrivateKey()!=null && applicationProperties.getImageServerPublicKey()!=null) {
            User imageServerUser = userRepository.findByUsernameLikeIgnoreCase("ImageServer1")
                    .orElseThrow(() -> new ObjectNotFoundException("No user imageserver1, cannot assign keys"));
            imageServerUser.setPrivateKey(applicationProperties.getImageServerPrivateKey());
            imageServerUser.setPublicKey(applicationProperties.getImageServerPublicKey());
            userRepository.save(imageServerUser);
        }

        log.info("Check image filters...");
        bootstrapDataService.updateImageFilters();
        log.info ("#############################################################################");
        log.info ("###################              READY              #########################");
        log.info ("#############################################################################");
    }
}

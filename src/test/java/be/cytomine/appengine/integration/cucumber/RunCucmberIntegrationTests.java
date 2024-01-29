package be.cytomine.appengine.integration.cucumber;

import be.cytomine.appengine.AppEngineApplication;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {"pretty" , "html:build/reports/tests/cucumber/cucmber-report.html"},
        features = {"src/test/resources"},
        glue = {"be.cytomine.appengine.integration.cucumber"}
        ,tags = "not @Scheduler"

)
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RunCucmberIntegrationTests {
}

package be.cytomine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories("be.cytomine.repositorynosql")
@EnableJpaRepositories("be.cytomine.repository")
@EntityScan("be.cytomine.domain")
@SpringBootApplication
public class CytomineCoreApplication {
	public static void main(String[] args) {
		SpringApplication.run(CytomineCoreApplication.class, args);
	}
}

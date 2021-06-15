package de.benvorth.pushr;

import de.benvorth.pushr.model.BaseRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
// @EnableJpaRepositories("de.benvorth.pushr")
// @EntityScan("de.benvorth.pushr")
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl.class)
public class PushrApplication {

	public final static Logger logger = LoggerFactory.getLogger(PushrApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(PushrApplication.class, args);
	}

}

package de.benvorth.pushr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PushrApplication {

	public final static Logger logger = LoggerFactory.getLogger(PushrApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(PushrApplication.class, args);
	}

}

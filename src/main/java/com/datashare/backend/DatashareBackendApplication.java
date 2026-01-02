package com.datashare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de l'application Spring Boot.
 * Active également le scheduling pour le nettoyage automatique des fichiers.
 */
@SpringBootApplication
@EnableScheduling
public class DatashareBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DatashareBackendApplication.class, args);
	}

}

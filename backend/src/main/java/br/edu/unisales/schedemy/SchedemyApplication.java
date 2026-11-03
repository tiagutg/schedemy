package br.edu.unisales.schedemy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // RotinaDeAgenda: conclusao das reunioes e lembretes
public class SchedemyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchedemyApplication.class, args);
	}

}

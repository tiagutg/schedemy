package br.edu.unisales.schedemy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // RotinaDeAgenda: conclusao das reunioes e lembretes
public class SchedemyApplication {

	/**
	 * Fuso usado em todo "agora" do sistema (LocalDateTime.now() nos prazos RN 05/RN 14, na RotinaDeAgenda...).
	 * Fixado aqui para nao depender do fuso do servidor: em nuvem a JVM costuma rodar em UTC e os prazos
	 * ficariam 3 horas deslocados. Sobrescreva com a variavel de ambiente SCHEDEMY_FUSO.
	 * Obs.: o RNF 08 pede armazenamento em UTC; ver docs/ANALISE.md.
	 */
	static final String FUSO_PADRAO = "America/Sao_Paulo";

	public static void main(String[] args) {
		String fuso = System.getenv().getOrDefault("SCHEDEMY_FUSO", FUSO_PADRAO);
		TimeZone.setDefault(TimeZone.getTimeZone(fuso));
		SpringApplication.run(SchedemyApplication.class, args);
	}

}

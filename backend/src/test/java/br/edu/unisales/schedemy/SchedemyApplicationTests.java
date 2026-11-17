package br.edu.unisales.schedemy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe a aplicacao inteira contra o banco configurado: o ddl-auto=validate confere entidades x schema e o
 * Spring Data valida todas as queries JPQL. Tag "banco": precisa do banco ligado (veja o pom.xml).
 * A rotina de agenda fica desligada para nao mexer em dados durante o teste.
 */
@Tag("banco")
@SpringBootTest(properties = "schedemy.rotina-agenda.ativa=false")
class SchedemyApplicationTests {

	@Test
	void contextLoads() {
	}

}

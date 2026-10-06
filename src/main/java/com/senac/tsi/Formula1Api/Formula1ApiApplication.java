package com.senac.tsi.Formula1Api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// classe principal, e por aqui que tudo comeca
// o @SpringBootApplication faz o Spring achar sozinho as entidades, controllers e repositorios do pacote
@SpringBootApplication
public class Formula1ApiApplication {

	// sobe o banco, as tabelas, o servidor na porta 8080 e o Swagger
	public static void main(String[] args) {
		SpringApplication.run(Formula1ApiApplication.class, args);
	}

}

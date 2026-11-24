package com.utp.tienda;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TiendaWebBackendApplication {

	@PostConstruct
	public void init() {
		// Fijar zona horaria de Perú (UTC-5) para servidores en la nube (Render / Koyeb / Docker)
		TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
	}

	public static void main(String[] args) {
		SpringApplication.run(TiendaWebBackendApplication.class, args);
	}

}



package com.clubakos.backend; // Asegúrate de que el paquete sea el correcto

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // <-- Esta es la importación que falta

@SpringBootApplication
@EnableScheduling
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
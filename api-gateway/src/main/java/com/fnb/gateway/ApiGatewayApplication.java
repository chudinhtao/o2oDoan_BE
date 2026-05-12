package com.fnb.gateway;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory("..") // .env is in backend/ directory
                .ignoreIfMissing()
                .load();
        
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        
        String internalSecret = System.getProperty("INTERNAL_SECRET");
        System.out.println(">>> [DOTENV] INTERNAL_SECRET loaded: " + (internalSecret != null ? "YES (starts with " + internalSecret.substring(0, 5) + "...)" : "NO"));

        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

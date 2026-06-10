package com.fnb.report;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication(scanBasePackages = {"com.fnb.report", "com.fnb.common"})
@EnableDiscoveryClient

public class ReportServiceApplication {
    public static void main(String[] args) {
        Dotenv.configure()
                .directory("..")
                .ignoreIfMissing()
                .load()
                .entries()
                .forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        SpringApplication.run(ReportServiceApplication.class, args);
    }
}

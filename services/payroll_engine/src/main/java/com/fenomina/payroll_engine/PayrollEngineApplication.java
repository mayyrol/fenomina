package com.fenomina.payroll_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@EnableFeignClients
public class PayrollEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayrollEngineApplication.class, args);
	}

}

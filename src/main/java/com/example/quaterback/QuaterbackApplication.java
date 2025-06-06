package com.example.quaterback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableWebSocket
public class QuaterbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuaterbackApplication.class, args);
    }
}

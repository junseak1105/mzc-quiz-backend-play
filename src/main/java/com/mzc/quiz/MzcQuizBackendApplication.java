package com.mzc.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class MzcQuizBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MzcQuizBackendApplication.class, args);
    }

}

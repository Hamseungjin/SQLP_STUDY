package com.example.sqlp_hsj;

import org.springframework.boot.SpringApplication;

public class TestSqlpHsjApplication {

    public static void main(String[] args) {
        SpringApplication.from(SqlpHsjApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

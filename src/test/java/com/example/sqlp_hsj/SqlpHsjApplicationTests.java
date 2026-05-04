package com.example.sqlp_hsj;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SqlpHsjApplicationTests {

    @Test
    void contextLoads() {
    }

}

package com.flightcontrol;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlightControlApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldLoadApplicationContext_whenApplicationStarts() {
        assertThat(context).isNotNull();
    }
}

package com.TravelBuddy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(TestConfiguration.class)
class TravelBuddyApplicationTests {

    @Test
    void contextLoads() {
    }
}

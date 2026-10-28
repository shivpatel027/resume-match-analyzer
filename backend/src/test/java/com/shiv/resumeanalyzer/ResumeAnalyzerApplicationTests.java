package com.shiv.resumeanalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Boots the whole app against in-memory H2 to catch wiring and mapping mistakes. */
@SpringBootTest
@ActiveProfiles("test")
class ResumeAnalyzerApplicationTests {

    @Test
    void contextLoads() {
    }
}

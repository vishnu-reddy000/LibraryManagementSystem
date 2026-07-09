package com.library.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilsTests {

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void testTokenLifecycle() {
        String email = "test@library.com";
        String token = jwtUtils.generateToken(email);

        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));

        String extractedEmail = jwtUtils.getUsernameFromToken(token);
        assertEquals(email, extractedEmail);
    }
}

package com.example.sds;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("dev")
class DocumentServiceApplicationTests {

    // Prevents Spring from trying to contact the OIDC issuer URI on startup
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }

}

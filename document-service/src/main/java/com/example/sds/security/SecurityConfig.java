package com.example.sds.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Central security configuration for the Secure Document Service.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Fully stateless – no HTTP session is ever created.</li>
 *   <li>JWT issuer-URI is NOT hardcoded here; Spring Boot reads it from
 *       {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}
 *       in {@code application.yaml} and auto-configures the {@code JwtDecoder}.</li>
 *   <li>A cleanup filter placed before {@link BearerTokenAuthenticationFilter}
 *       guarantees {@link OrganisationContext} is cleared after every request,
 *       preventing ThreadLocal leaks in Tomcat's thread pool.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtOrganisationConverter()))
            )
            // Must sit BEFORE BearerTokenAuthenticationFilter so the try/finally
            // wraps the entire security chain, including authentication itself.
            .addFilterBefore(organisationContextClearFilter(), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    JwtOrganisationConverter jwtOrganisationConverter() {
        return new JwtOrganisationConverter();
    }

    /**
     * Clears {@link OrganisationContext} after every request regardless of outcome.
     * Registered as a named bean so it can be referenced in tests.
     */
    @Bean
    OncePerRequestFilter organisationContextClearFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain)
                    throws ServletException, IOException {
                try {
                    chain.doFilter(request, response);
                } finally {
                    OrganisationContext.clear();
                }
            }
        };
    }
}

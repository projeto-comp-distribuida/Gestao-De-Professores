package com.distrischool.teacher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Value("${AUTH0_DOMAIN:}")
    private String auth0Domain;

    @Value("${AUTH0_ISSUER_URI:}")
    private String auth0IssuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String springIssuerUri;

    @Value("${auth0.audience}")
    private String audience;

    @Value("${security.disable:false}")
    private boolean securityDisable;
    
    /**
     * Gets the issuer URI, constructing it from AUTH0_DOMAIN if AUTH0_ISSUER_URI is not set
     * Priority: AUTH0_ISSUER_URI > AUTH0_DOMAIN > spring.security.oauth2.resourceserver.jwt.issuer-uri
     */
    private String getIssuerUri() {
        // First priority: AUTH0_ISSUER_URI environment variable
        if (auth0IssuerUri != null && !auth0IssuerUri.trim().isEmpty()) {
            log.debug("Using AUTH0_ISSUER_URI: {}", auth0IssuerUri);
            return auth0IssuerUri;
        }
        // Second priority: Construct from AUTH0_DOMAIN
        if (auth0Domain != null && !auth0Domain.trim().isEmpty()) {
            // Ensure domain doesn't have trailing slash or protocol
            String domain = auth0Domain.trim();
            if (domain.startsWith("https://")) {
                domain = domain.substring(8);
            }
            if (domain.startsWith("http://")) {
                domain = domain.substring(7);
            }
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            String issuerUri = "https://" + domain + "/";
            log.debug("Constructed issuer URI from AUTH0_DOMAIN {}: {}", auth0Domain, issuerUri);
            return issuerUri;
        }
        // Third priority: Use spring property
        if (springIssuerUri != null && !springIssuerUri.trim().isEmpty()) {
            log.debug("Using spring.security.oauth2.resourceserver.jwt.issuer-uri: {}", springIssuerUri);
            return springIssuerUri;
        }
        // Fallback
        log.warn("No Auth0 issuer URI configured. Using default fallback.");
        return "https://your-tenant.auth0.com/";
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (securityDisable) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/health/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        }

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "security.disable", havingValue = "false", matchIfMissing = true)
    public JwtDecoder jwtDecoder() {
        String issuerUri = getIssuerUri();
        log.info("Configuring JWT decoder with issuer URI: {}", issuerUri);
        log.info("Using Auth0 audience: {}", audience);
        
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${spring.web.cors.allowed-origins:*}") String allowedOrigins,
        @Value("${spring.web.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}") String allowedMethods,
        @Value("${spring.web.cors.allowed-headers:*}") String allowedHeaders,
        @Value("${spring.web.cors.allow-credentials:true}") boolean allowCredentials
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of(allowedMethods.split(",")));
        configuration.setAllowedHeaders(List.of(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Converts JWT claims to Spring Security authorities. Uses standard scope claims,
     * maps Auth0 "permissions" array to SCOPE_ authorities, and also maps "roles" 
     * array to ROLE_ authorities so it works seamlessly with hasAuthority('SCOPE_xxx') 
     * and hasRole('ADMIN') checks.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        scopesConverter.setAuthorityPrefix("SCOPE_");

        Converter<Jwt, Collection<GrantedAuthority>> aggregateConverter = jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopesConverter.convert(jwt));
            
            // Mapeia permissions do Auth0 para SCOPE_ authorities
            Object permissionsClaim = jwt.getClaims().get("permissions");
            if (permissionsClaim instanceof Collection<?> perms) {
                for (Object p : perms) {
                    if (p != null) {
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + p.toString()));
                    }
                }
            }
            
            // Mapeia roles do Auth0 para ROLE_ authorities
            Object rolesClaim = jwt.getClaims().get("roles");
            if (rolesClaim instanceof Collection<?> roles) {
                for (Object r : roles) {
                    if (r != null) {
                        String role = r.toString();
                        // Adiciona como ROLE_xxx para compatibilidade com hasRole()
                        if (!role.startsWith("ROLE_")) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        } else {
                            authorities.add(new SimpleGrantedAuthority(role.toUpperCase()));
                        }
                    }
                }
            }
            
            // Também verifica se há roles em formato diferente (ex: https://distrischool.com/roles)
            var allClaims = jwt.getClaims();
            for (Map.Entry<String, Object> entry : allClaims.entrySet()) {
                if (entry.getKey().contains("role") && entry.getValue() instanceof Collection<?> roles) {
                    for (Object r : roles) {
                        if (r != null) {
                            String role = r.toString();
                            if (!role.startsWith("ROLE_")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                            } else {
                                authorities.add(new SimpleGrantedAuthority(role.toUpperCase()));
                            }
                        }
                    }
                }
            }
            
            return authorities;
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(aggregateConverter);
        return converter;
    }

    /**
     * Validates that the JWT contains the expected audience (Auth0 API identifier)
     */
    static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String audience;

        AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (jwt.getAudience() != null && jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}



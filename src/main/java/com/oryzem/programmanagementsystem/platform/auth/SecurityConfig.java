package com.oryzem.programmanagementsystem.platform.auth;

import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationFilter;
import com.oryzem.programmanagementsystem.platform.audit.RequestCorrelationContext;
import com.oryzem.programmanagementsystem.platform.auth.AuthenticationLoggingFilter;
import com.oryzem.programmanagementsystem.platform.auth.CognitoAudienceValidator;
import com.oryzem.programmanagementsystem.platform.auth.CognitoJwtAuthenticationConverter;
import com.oryzem.programmanagementsystem.platform.auth.JsonAccessDeniedHandler;
import com.oryzem.programmanagementsystem.platform.auth.JsonAuthenticationEntryPoint;
import com.oryzem.programmanagementsystem.platform.auth.AuthenticatedUserSynchronizationFilter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(CognitoProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            RequestCorrelationFilter requestCorrelationFilter,
            RequestCorrelationContext requestCorrelationContext,
            AuthenticatedUserSynchronizationFilter authenticatedUserSynchronizationFilter,
            ObjectMapper objectMapper) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/public/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(new CognitoJwtAuthenticationConverter())))
                .exceptionHandling(exceptionHandling(objectMapper, requestCorrelationContext))
                .addFilterBefore(requestCorrelationFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(authenticatedUserSynchronizationFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new AuthenticationLoggingFilter(requestCorrelationContext), AuthenticatedUserSynchronizationFilter.class);

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(CognitoProperties cognitoProperties) {
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(cognitoProperties.issuerUri()),
                new CognitoAudienceValidator(cognitoProperties.appClientId()));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(cognitoProperties.jwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CognitoProperties cognitoProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cognitoProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Access-Context", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("Location", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private Customizer<ExceptionHandlingConfigurer<HttpSecurity>> exceptionHandling(
            ObjectMapper objectMapper,
            RequestCorrelationContext requestCorrelationContext) {
        return exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(new JsonAuthenticationEntryPoint(objectMapper, requestCorrelationContext))
                .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper, requestCorrelationContext));
    }
}



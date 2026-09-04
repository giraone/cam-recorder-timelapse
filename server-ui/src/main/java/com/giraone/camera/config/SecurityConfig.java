package com.giraone.camera.config;

import com.giraone.camera.views.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final ApplicationProperties applicationProperties;

    public SecurityConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {

        // This code uses script and style in html ('unsafe-inline') and images and media loaded from the image server
        // Vaadin itself uses 'unsafe-eval' and data: for images/fonts
        final String cspPolicy = "default-src 'self'; " +
            "img-src 'self' data: " + applicationProperties.getHostUrl() + "; " +
            "media-src 'self' " + applicationProperties.getHostUrl() + "; " +
            "connect-src 'self'; " +
            "frame-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "font-src 'self' data: ";
        http.headers(configurer -> configurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        http.headers(configurer -> configurer.contentSecurityPolicy(
            contentSecurityPolicyConfig -> contentSecurityPolicyConfig.policyDirectives(cspPolicy)));
        // Registered before the Vaadin configurer, so these rules take precedence over its "anyRequest" rule
        http.authorizeHttpRequests(auth ->
            auth.requestMatchers(
                withDefaults().matcher(HttpMethod.GET, "/images/*.png"),
                withDefaults().matcher(HttpMethod.GET, "/actuator"),
                withDefaults().matcher(HttpMethod.GET, "/actuator/**")
            ).permitAll());
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));
        return http.build();
    }

    @Bean
    public UserDetailsService users() {

        // Created using: BCrypt.hashpw("<user>-secret", BCrypt.gensalt());
        UserDetails user = User.builder()
            .username("cam")
            .password("{bcrypt}$2a$10$VuBTJ/Iz.R16uiEwZsDPCeBh8NxuhTmPXX3LQEMhIS9iW7KANUVu2")
            .roles("USER")
            .build();
        UserDetails admin = User.builder()
            .username("boss")
            .password("{bcrypt}$2a$10$RX/BG7JYqhTxJ3JAAVl.Peb3PHmiwZQs4opkiELxNES3zV9.hJVpi")
            .roles("USER", "ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user, admin);
    }
}

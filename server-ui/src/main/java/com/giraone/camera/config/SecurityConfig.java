package com.giraone.camera.config;

import com.giraone.camera.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    private ApplicationProperties applicationProperties;

    public SecurityConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // This code uses script and style in html ('unsafe-inline') and images loaded from the image server
        // Vaadin itself uses 'unsafe-eval' and data: for images/fonts
        final String cspPolicy = "default-src 'self'; frame-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: " + applicationProperties.getHostUrl() + "; " +
                "font-src 'self' data: ";
        http.headers(configurer -> configurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        http.headers(configurer -> configurer.contentSecurityPolicy(
            contentSecurityPolicyConfig -> contentSecurityPolicyConfig.policyDirectives(cspPolicy)));
        http.authorizeHttpRequests(auth ->
            auth.requestMatchers(
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/images/*.png"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/actuator"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/actuator/**")
            ).permitAll());
        super.configure(http);
        setLoginView(http, LoginView.class);
    }

    @Bean
    public UserDetailsService users() {

        // Created using: BCrypt.hashpw("<user>-secret", BCrypt.gensalt()));
        UserDetails user = User.builder()
            .username("cam")
            .password("{bcrypt}$2a$10$VuBTJ/Iz.R16uiEwZsDPCeBh8NxuhTmPXX3LQEMhIS9iW7KANUVu2")
            .roles("USER")
            .build();
        UserDetails admin = User.builder()
            .username("boss")
            .password("{bcrypt}$2a$10$7eDGvhlM1ZN7nem972h7MeRkaDNlOMRT1XtSlmLOxV7PS3fsh5o.C")
            .roles("USER", "ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user, admin);
    }


}

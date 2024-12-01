package com.giraone.camera.config;

import com.giraone.camera.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
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
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.headers(configurer -> configurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
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
package com.giraone.streaming.config;

import com.giraone.streaming.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

        http
            .authorizeHttpRequests(auth ->
                auth.requestMatchers(
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/images/*.png"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/actuator"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/actuator/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/ping"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/camera-settings"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/camera-image-infos"),
                    AntPathRequestMatcher.antMatcher("/camera-images/**")
                ).permitAll());

        http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/camera-images/**")));

        http.authorizeHttpRequests(auth ->
                auth.requestMatchers(
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/camera-settings/**"),
                    // Camera needs no authentication yet for upload
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST,"/camera-images/**")
                ).anonymous());
        super.configure(http);
        setLoginView(http, LoginView.class);
    }

    @Bean
    public UserDetailsService users() {
        UserDetails user = User.builder()
            .username("cam")
            .password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
            .roles("USER")
            .build();
        UserDetails admin = User.builder()
            .username("boss")
            .password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
            .roles("USER", "ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user, admin);
    }
}
package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges.anyExchange().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Bean
    MapReactiveUserDetailsService authentication(PasswordEncoder passwordEncoder) {
        var users = Map.of(
                "mpollack", "pw",
                "ctsolov", "pw",
                "jlong", "pw"
        );
        var usersList = new ArrayList<UserDetails>();
        for (var entry : users.entrySet()) {
            usersList.add(org.springframework.security.core.userdetails.User
                    .withUsername(entry.getKey())
                    .password(passwordEncoder.encode(entry.getValue()))
                    .roles("USER")
                    .build());
        }
        return new MapReactiveUserDetailsService(usersList);
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        var host = "http://localhost:3002";
        var messageEndpoint = "/mcp/message";
        var sseEndpoint = "/sse";
        return rlb
                .routes()
                .route(rs -> rs
                        .path(messageEndpoint)
                        .uri(host + messageEndpoint)
                )
                .route(r -> r.path(sseEndpoint).uri(host + sseEndpoint))
                .build();
    }
}

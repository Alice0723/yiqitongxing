package com.ticket.config;

import com.ticket.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/events", "/event/**", "/schedule", "/groupbuy", "/group-purchase", "/ai-recommend", "/register", "/login", "/h2-console/**", "/css/**", "/uploads/**").permitAll()
                    .requestMatchers("/teacher", "/teacher/**").permitAll()
                    .requestMatchers("/analytics", "/analytics/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/theater/**").hasRole("THEATER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            String loginRole = request.getParameter("loginRole");
                            Map<String, String> roleMapping = Map.of(
                                    "student", "ROLE_USER",
                                    "teacher", "ROLE_TEACHER",
                                    "theater", "ROLE_THEATER",
                                    "admin", "ROLE_ADMIN"
                            );

                            if (loginRole != null && roleMapping.containsKey(loginRole)) {
                                String expectedRole = roleMapping.get(loginRole);
                                boolean matched = authentication.getAuthorities().stream()
                                        .anyMatch(a -> expectedRole.equals(a.getAuthority()));
                                if (!matched) {
                                    if (request.getSession(false) != null) {
                                        request.getSession(false).invalidate();
                                    }
                                    response.sendRedirect("/login?roleError");
                                    return;
                                }
                            }
                            response.sendRedirect("/");
                        })
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("ticket-remember-me-key")
                        .tokenValiditySeconds(60 * 60 * 24 * 30)
                        .alwaysRemember(true)
                        .userDetailsService(userDetailsService)
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                    .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider());
        // 允许 H2 控制�?iframe
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

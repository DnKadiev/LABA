package com.autoservice.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(AppUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                // Возвращаем 401 (а не 403) когда токен отсутствует или истёк
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Token missing, invalid or expired. Please login again.\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // --- Публичные ---
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()

                // --- Клиенты ---
                .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole("ADMIN", "MECHANIC")
                .requestMatchers("/api/customers/**").hasRole("ADMIN")

                // --- Автомобили ---
                .requestMatchers(HttpMethod.GET, "/api/vehicles/**").hasAnyRole("ADMIN", "MECHANIC", "CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/vehicles/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasRole("ADMIN")
                .requestMatchers("/api/vehicles/**").hasRole("ADMIN") // catchall: ADMIN всегда имеет доступ

                // --- Механики ---
                .requestMatchers(HttpMethod.GET, "/api/mechanics/**").hasAnyRole("ADMIN", "MECHANIC")
                .requestMatchers("/api/mechanics/**").hasRole("ADMIN")

                // --- Запчасти ---
                .requestMatchers(HttpMethod.GET, "/api/parts/**").authenticated()
                .requestMatchers("/api/parts/**").hasRole("ADMIN")

                // --- Бизнес-операции (ПЕРЕД общими правилами orders!) ---
                .requestMatchers("/api/orders/*/auto-assign").hasAnyRole("ADMIN", "MECHANIC")
                .requestMatchers("/api/orders/*/close").hasAnyRole("ADMIN", "MECHANIC")
                .requestMatchers("/api/orders/*/cost").hasAnyRole("ADMIN", "MECHANIC", "CUSTOMER")
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MECHANIC")

                // --- Заказ-наряды (общие правила после специфичных) ---
                .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("ADMIN", "MECHANIC")
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasAnyRole("ADMIN", "MECHANIC")
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")
                .requestMatchers("/api/orders/**").hasRole("ADMIN") // catchall: ADMIN всегда имеет доступ

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

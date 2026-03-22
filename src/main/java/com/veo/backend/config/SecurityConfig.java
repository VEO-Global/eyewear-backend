package com.veo.backend.config;

import com.veo.backend.security.CustomUserDetailsService;
import com.veo.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. DÒNG SINH TỬ CHO SWAGGER (BẮT BUỘC PHẢI CÓ)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/consultation-appointments").permitAll()

                        // 2. NHỮNG API PUBLIC CHO CUSTOMER
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/products/**", "/api/variants/**", "/api/categories/**", "/api/lens_products/**", "/api/lens-products/**", "/lens-products/**", "/api/locations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/my", "/api/orders/*").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/orders/my", "/orders/*").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/checkout", "/orders", "/orders/checkout").hasRole("CUSTOMER")
                        .requestMatchers("/api/staff/consultation-appointments/**").hasAnyRole("SALES", "OPERATIONS", "MANAGER", "ADMIN")
                        .requestMatchers("/api/staff/prescriptions/**", "/api/staff/orders/*/prescription", "/staff/prescriptions/**", "/staff/orders/*/prescription").hasAnyRole("SALES", "MANAGER", "ADMIN")
                        .requestMatchers("/api/notifications/**").hasAnyRole("CUSTOMER", "ADMIN", "MANAGER", "SALES", "OPERATIONS")

                        // 3. XEM PROFILE (CẦN LOGIN)
                        .requestMatchers(HttpMethod.GET, "/api/user/profile").hasAnyRole("CUSTOMER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/user/profile").hasAnyRole("CUSTOMER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/user/profile").hasAnyRole("CUSTOMER", "ADMIN", "MANAGER")

                        // 4. QUYỀN CỦA MANAGER (QUẢN LÝ SẢN PHẨM)
                        .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/variants/**", "/api/lens_products/**", "/api/categories").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/variants/**", "/api/lens_products/**", "/api/categories/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/variants/**", "/api/lens_products/**", "/api/categories/**").hasRole("MANAGER")

                        // 5. QUYỀN CỦA ADMIN (QUẢN LÝ USER)
                        .requestMatchers("/api/user/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

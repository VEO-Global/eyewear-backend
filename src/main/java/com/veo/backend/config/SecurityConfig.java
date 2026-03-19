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
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. NHỮNG API PUBLIC (Ai cũng vào được, không cần Token)
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/profile", "/api/products/**", "/api/variants/**", "/api/categories/**", "/api/orders/**", "/api/lens_products/**").permitAll()

                        // 2. QUYỀN CỦA MANAGER (Thêm/Sửa/Xóa sản phẩm)
                        .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/variants/**", "/api/categories").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/variants/**", "/api/lens_products/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/variants/**", "/api/user/**", "/api/lens_products/**").hasRole("MANAGER")

                        // 3. QUYỀN CỦA ADMIN
                        .requestMatchers("/api/user/**").hasRole("ADMIN")

                        // 4. Các request khác phải đăng nhập
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
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

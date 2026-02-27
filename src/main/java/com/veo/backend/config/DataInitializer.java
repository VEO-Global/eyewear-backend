package com.veo.backend.config;

import com.veo.backend.entity.Role;
import com.veo.backend.entity.User;
import com.veo.backend.repository.RoleRepository;
import com.veo.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Initializing data...");

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

        roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER not found"));

        if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setPasswordHash(passwordEncoder.encode("123456789"));
            admin.setFullName("Admin");
            admin.setRole(adminRole);
            admin.setIsActive(true);

            userRepository.save(admin);

            System.out.println("Admin account created");
        } else {
            System.out.println("Admin already exists");
        }
    }
}

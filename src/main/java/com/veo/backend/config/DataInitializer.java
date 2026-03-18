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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Initializing data...");

        Map<String, String> emailMap = new HashMap<>();
        emailMap.put("ADMIN", "admin@gmail.com");
        emailMap.put("CUSTOMER", "customer@gmail.com");
        emailMap.put("MANAGER", "manager@gmail.com");
        emailMap.put("OPERATIONS", "operation@gmail.com");
        emailMap.put("SALES", "sale@gmail.com");

        Map<String, String> passwordMap = new HashMap<>();
        passwordMap.put("ADMIN", "Admin@123");
        passwordMap.put("CUSTOMER", "Customer!123");
        passwordMap.put("MANAGER", "Manager!123");
        passwordMap.put("OPERATIONS", "Operation!123");
        passwordMap.put("SALES", "Sale!123");

        List<Role> roles = roleRepository.findAll();
        for (Role role : roles) {
            String roleName = role.getName();
            String email = emailMap.get(roleName);
            String password = passwordMap.get(roleName);

            if (email == null || password == null) continue;

            if (userRepository.findByEmail(email).isEmpty()) {
                User user = new User();
                user.setEmail(email);
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setFullName(roleName + " SYSTEM");
                user.setRole(role);
                user.setIsActive(true);
                userRepository.save(user);
                System.out.println("Created user: " + user.getEmail());
            }else  {
                System.out.println("Email already exists: " + email);
            }
        }
    }
}
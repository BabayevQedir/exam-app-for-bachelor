package com.exam.exam_system.config;

import com.exam.exam_system.model.User;
import com.exam.exam_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Admin — tələbələrin kimliyini görür
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .build());
            log.info("✅ Admin → admin / admin123");
        }

        // Müəllim
        if (!userRepository.existsByUsername("teacher")) {
            userRepository.save(User.builder()
                    .username("teacher")
                    .password(passwordEncoder.encode("teacher123"))
                    .fullName("Default Müəllim")
                    .role(User.Role.TEACHER)
                    .enabled(true)
                    .build());
            log.info("✅ Müəllim → teacher / teacher123");
        }

        // Nəzarətçi
        if (!userRepository.existsByUsername("invig")) {
            userRepository.save(User.builder()
                    .username("invig")
                    .password(passwordEncoder.encode("invig123"))
                    .fullName("Default Nəzarətçi")
                    .role(User.Role.INVIGILATOR)
                    .enabled(true)
                    .build());
            log.info("✅ Nəzarətçi → invig / invig123");
        }
    }
}
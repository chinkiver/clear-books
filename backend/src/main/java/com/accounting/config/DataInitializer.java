package com.accounting.config;

import com.accounting.entity.User;
import com.accounting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 应用启动时自动创建默认 admin 用户
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 检查是否已存在 admin 用户
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .nickname("管理员")
                    .email("admin@example.com")
                    .build();
            userRepository.save(admin);
            log.info("============================================");
            log.info("Default admin user created:");
            log.info("Username: admin");
            log.info("Password: admin123");
            log.info("============================================");
        }
    }
}

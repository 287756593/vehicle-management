package com.company.vehicle.config;

import com.company.vehicle.entity.SysUser;
import com.company.vehicle.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        SysUser admin = sysUserMapper.selectById(1L);
        if (admin != null && (admin.getPassword() == null || admin.getPassword().isBlank())) {
            String initialPassword = System.getenv().getOrDefault("INITIAL_ADMIN_PASSWORD", "change-me-admin");
            String encodedPassword = passwordEncoder.encode(initialPassword);
            admin.setPassword(encodedPassword);
            sysUserMapper.updateById(admin);
            System.out.println("Admin password initialized");
        }
    }
}

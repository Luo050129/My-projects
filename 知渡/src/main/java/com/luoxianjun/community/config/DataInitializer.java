package com.luoxianjun.community.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动种子数据：首次运行注入 admin / 演示用户，便于直接体验登录与 RBAC。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()) > 0) {
            return;
        }
        insert("admin", "admin123", "管理员", "ADMIN");
        insert("alice", "alice123", "Alice", "USER");
        insert("bob", "bob123", "Bob", "USER");
        log.info("已初始化种子用户：admin/admin123, alice/alice123, bob/bob123");
    }

    private void insert(String username, String rawPassword, String nickname, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setNickname(nickname);
        u.setRole(role);
        u.setStatus(1);
        userMapper.insert(u);
    }
}

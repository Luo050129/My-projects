package com.luoxianjun.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.luoxianjun.community.module.*.mapper")
public class CommunityBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityBackendApplication.class, args);
    }
}

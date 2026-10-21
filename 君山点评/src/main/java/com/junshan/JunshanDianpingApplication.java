package com.junshan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.junshan.mapper")
@EnableRabbit
@SpringBootApplication
public class JunshanDianpingApplication {
    //
    public static void main(String[] args) {
        SpringApplication.run(JunshanDianpingApplication.class, args);
    }

}

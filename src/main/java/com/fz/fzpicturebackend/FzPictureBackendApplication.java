package com.fz.fzpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.fz.fzpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)  // 暴露代理
public class FzPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FzPictureBackendApplication.class, args);
    }

}

package io.github.xiejx618.replace.demo.config;

import io.github.xiejx618.replace.demo.service.BeanService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public BeanService beanService() {
        return new BeanService();
    }
}

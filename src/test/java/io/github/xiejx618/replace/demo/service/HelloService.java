package io.github.xiejx618.replace.demo.service;

import org.springframework.stereotype.Service;
@Service
public class HelloService {
    public void sayHello() {
        System.out.println("调用了org.exam.demo.service.HelloService.sayHello.");
    }
}
package io.github.xiejx618.replace.demo.ext;

import io.github.xiejx618.replace.demo.service.HelloService;
import io.github.xiejx618.replace.Replace;

@Replace(value = "helloService", order = 0)
public class HelloServiceExt extends HelloService {
    @Override
    public void sayHello() {
        System.out.println("hello world ext!");
    }
}
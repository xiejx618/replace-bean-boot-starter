package io.github.xiejx618.replace.demo;

import io.github.xiejx618.replace.demo.service.HelloService;
import io.github.xiejx618.replace.demo.service.TestBeanPostProcessor;
import io.github.xiejx618.replace.demo.service.TestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);
        HelloService helloService = context.getBean(HelloService.class);
        System.out.println("获取helloService = " + helloService);
        helloService = context.getBean(HelloService.class);
        System.out.println("再次获取helloService = " + helloService);
        helloService.sayHello();

        TestService testService = context.getBean(TestService.class);
        testService.test();

        TestBeanPostProcessor testBeanPostProcessor = context.getBean(TestBeanPostProcessor.class);
        testBeanPostProcessor.test();
    }


}

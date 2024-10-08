package io.github.xiejx618.replace.demo;

import io.github.xiejx618.replace.demo.service.HelloService;
import io.github.xiejx618.replace.demo.service.TestOrderedBeanPostProcessor;
import io.github.xiejx618.replace.demo.service.TestPriorityOrderedBeanPostProcessor;
import io.github.xiejx618.replace.demo.service.TestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);
        //第一次获取
        HelloService helloService = context.getBean(HelloService.class);
        System.out.println("获取helloService = " + helloService);
        //第二次获取，目的为了验证单例或原型bean
        helloService = context.getBean(HelloService.class);
        System.out.println("再次获取helloService = " + helloService);
        //调用sayHello方法
        helloService.sayHello();

        //验证依赖注入是否生效
        TestService testService = context.getBean(TestService.class);
        testService.test();

        //验证OrderedBeanPostProcessor替换是否生效
        TestOrderedBeanPostProcessor testOrdered = context.getBean(TestOrderedBeanPostProcessor.class);
        testOrdered.test();

        //验证PriorityOrderedBeanPostProcessor替换是否生效
        TestPriorityOrderedBeanPostProcessor testPriorityOrdered = context.getBean(TestPriorityOrderedBeanPostProcessor.class);
        testPriorityOrdered.test();

        System.out.println("耗时(毫秒) = " + (System.currentTimeMillis()-start));
    }
}

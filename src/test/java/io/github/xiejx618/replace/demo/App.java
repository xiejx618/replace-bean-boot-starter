package io.github.xiejx618.replace.demo;

import io.github.xiejx618.replace.demo.ext.*;
import io.github.xiejx618.replace.demo.service.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);
        test(context);
        System.out.println("耗时(毫秒) = " + (System.currentTimeMillis()-start));
    }

    private static void test(ConfigurableApplicationContext context) {
        Boolean enabled = context.getEnvironment().getProperty("replace.enabled", Boolean.class);
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }
        //第一次获取
        HelloService helloService = context.getBean(HelloService.class);
        System.out.println("获取helloService = " + helloService);
        //第二次获取，目的为了验证单例或原型bean
        helloService = context.getBean(HelloService.class);
        System.out.println("再次获取helloService = " + helloService);
        //调用sayHello方法
        helloService.sayHello();
        Assert.isTrue(helloService instanceof HelloServiceExt, "helloService替换失败");

        //验证依赖注入是否生效
        TestService testService = context.getBean(TestService.class);
        testService.test();
        Assert.isTrue(testService instanceof TestServiceExt, "testService替换失败");

        //验证@Bean替换是否生效
        BeanService beanService = context.getBean(BeanService.class);
        beanService.sayHello();
        Assert.isTrue(beanService instanceof BeanServiceExt,"beanService替换失败");

        //验证OrderedBeanPostProcessor替换是否生效
        TestOrderedBeanPostProcessor testOrdered = context.getBean(TestOrderedBeanPostProcessor.class);
        testOrdered.test();
        Assert.isTrue(testOrdered instanceof TestOrderedBeanPostProcessorExt, "testOrderedBeanPostProcessor替换失败");

        //验证PriorityOrderedBeanPostProcessor替换是否生效
        TestPriorityOrderedBeanPostProcessor testPriorityOrdered = context.getBean(TestPriorityOrderedBeanPostProcessor.class);
        testPriorityOrdered.test();
        Assert.isTrue(testPriorityOrdered instanceof TestPriorityOrderedBeanPostProcessorExt,
                "testPriorityOrderedBeanPostProcessor替换失败");
    }
}

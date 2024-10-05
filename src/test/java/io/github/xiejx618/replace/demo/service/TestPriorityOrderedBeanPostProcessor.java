package io.github.xiejx618.replace.demo.service;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;


@Component
public class TestPriorityOrderedBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    public void test(){
        System.out.println("TestPriorityOrderedBeanPostProcessor.test");
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

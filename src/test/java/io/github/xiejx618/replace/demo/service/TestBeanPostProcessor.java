package io.github.xiejx618.replace.demo.service;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;


@Component
public class TestBeanPostProcessor implements BeanPostProcessor, Ordered {

    public void test(){
        System.out.println("TestBeanPostProcessor = " + true);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

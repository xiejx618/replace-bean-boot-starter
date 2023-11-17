package io.github.xiejx618.replace.demo.ext;

import io.github.xiejx618.replace.Replace;
import io.github.xiejx618.replace.demo.service.TestBeanPostProcessor;

@Replace
public class TestBeanPostProcessorExt extends TestBeanPostProcessor {
    @Override
    public void test() {
        System.out.println("调用了org.exam.demo.ext.TestBeanPostProcessorExt.test");
    }
}

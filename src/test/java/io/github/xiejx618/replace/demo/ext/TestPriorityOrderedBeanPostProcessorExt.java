package io.github.xiejx618.replace.demo.ext;

import io.github.xiejx618.replace.Replace;
import io.github.xiejx618.replace.demo.service.TestPriorityOrderedBeanPostProcessor;

@Replace
public class TestPriorityOrderedBeanPostProcessorExt extends TestPriorityOrderedBeanPostProcessor {
    @Override
    public void test() {
        System.out.println("调用了TestPriorityOrderedBeanPostProcessorExt.test");
    }
}

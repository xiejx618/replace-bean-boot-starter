package io.github.xiejx618.replace.demo.ext;

import io.github.xiejx618.replace.Replace;
import io.github.xiejx618.replace.demo.service.TestOrderedBeanPostProcessor;

@Replace
public class TestOrderedBeanPostProcessorExt extends TestOrderedBeanPostProcessor {
    @Override
    public void test() {
        System.out.println("调用了TestOrderedBeanPostProcessorExt.test");
    }
}

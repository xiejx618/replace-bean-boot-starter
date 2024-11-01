/*
 * @(#)XssssService.java 1.0 20.1.8
 * <p>
 * Copyright (c) 2020, YUNXI. All rights reserved.
 * YUNXI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package io.github.xiejx618.replace.demo.ext;

import io.github.xiejx618.replace.demo.service.HelloService;
import io.github.xiejx618.replace.demo.service.TestService;
import io.github.xiejx618.replace.Replace;

@Replace
public class TestServiceExt extends TestService {

    public TestServiceExt(HelloService helloService) {
        super(helloService);
    }

    public void test(){
        System.out.println("调用了org.exam.demo.ext.TestServiceExt.test");
        helloService.sayHello();
    }
}

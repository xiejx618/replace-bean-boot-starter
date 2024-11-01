/*
 * @(#)XssssService.java 1.0 20.1.8
 * <p>
 * Copyright (c) 2020, YUNXI. All rights reserved.
 * YUNXI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package io.github.xiejx618.replace.demo.service;

import org.springframework.stereotype.Service;



@Service
public class TestService {

    protected final HelloService helloService;

    public TestService(HelloService helloService) {
        this.helloService = helloService;
    }


    public void test() {
        System.out.println("调用了org.exam.demo.service.TestService.test," + "helloService的注入为" + helloService);
        helloService.sayHello();
    }
}

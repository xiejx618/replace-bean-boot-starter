package io.github.xiejx618.replace.demo.ext;

import io.github.xiejx618.replace.Replace;
import io.github.xiejx618.replace.demo.service.BeanService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.env.Environment;

@Replace(instantiateMethod = "instantiate")
public class BeanServiceExt extends BeanService {

    private static BeanServiceExt instantiate(/*BeanFactory beanFactory, Environment environment*/) {
        return new BeanServiceExt();
    }


    public void sayHello() {
        System.out.println("调用了org.exam.demo.service.BeanServiceExt.sayHello");
    }
}

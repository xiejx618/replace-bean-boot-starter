package io.github.xiejx618.replace.demo;

import io.github.xiejx618.replace.Replace;
import io.github.xiejx618.replace.demo.ext.HelloServiceExt;
import io.github.xiejx618.replace.demo.service.HelloService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.env.Environment;

/**
 * 能替换@Bean定义的Bean,也能替换@Repository,@Service,@Component,@Controller等定义的Bean
 * 但此方式定义的Bean如果依赖比较多,则需要在工厂方法中重新注入依赖,配置可能更麻烦复杂
 */
public class ReplaceBeanConfig {
    //这里是Spring的BeanFactory,会自动注入
    private BeanFactory beanFactory;
    //这里是Spring的Environment,会自动注入
    private Environment environment;

    //通过@Replace定义bean替换
    @Replace(order = -2)
    public HelloService helloService() {
        return new HelloServiceExt();
    }
}

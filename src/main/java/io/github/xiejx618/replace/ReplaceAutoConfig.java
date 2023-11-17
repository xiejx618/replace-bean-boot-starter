package io.github.xiejx618.replace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;


@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "replace", value = "enabled", havingValue = "true", matchIfMissing = true)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ReplaceAutoConfig {
    private static final Logger logger = LoggerFactory.getLogger(ReplaceAutoConfig.class);

    /**
     * ReplaceProperties不使用ConfigurationProperties,
     * 是因为此时ConfigurationPropertiesBindingPostProcessor未注册到beanFactory.
     * 准备bean替换信息,输出bean替换信息
     *
     * @param environment 环境变量
     * @return Replace Bean后置处理器
     */
    @Bean
    public ReplaceBeanPostProcessor replaceBeanPostProcessor(Environment environment) {
        ReplaceProperties replaceProperties = Binder.get(environment)
                .bind("replace", ReplaceProperties.class)
                .orElse(new ReplaceProperties());
        String packages = replaceProperties.getPackages();
        if (StringUtils.hasText(packages)) {
            ReplaceBeanPostProcessor.registerScan(packages.split(","));
        }
        Map<String, String> replaceMap = replaceProperties.getReplaceMap();
        if (!CollectionUtils.isEmpty(replaceMap)) {
            replaceMap.forEach(ReplaceBeanPostProcessor::register);
        }
        logger.info(ReplaceBeanPostProcessor.replaceMapToString(true));
        return new ReplaceBeanPostProcessor();
    }
}

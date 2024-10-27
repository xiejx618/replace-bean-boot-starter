package io.github.xiejx618.replace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 监听Spring应用上下文初始化事件
 */
public class ReplaceBeanInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger logger = LoggerFactory.getLogger(ReplaceBeanInitializer.class);
    private static final String PACKAGES = "replace.packages";
    private static final String FACTORIES = "replace.factories";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        //1.获取替换Bean配置. 默认值不启用bean替换功能
        ConfigurableEnvironment environment = context.getEnvironment();
        ReplaceProperties replaceProperties = Binder.get(environment)
                .bind("replace", ReplaceProperties.class)
                .orElse(new ReplaceProperties());
        if (!replaceProperties.isEnabled()) {
            return;
        }
        //2.获取包名和替换Bean配置, 注册替换Bean到ReplaceBeanPostProcessor;
        Config config = optimizeConfig(environment.getPropertySources());
        ReplaceBeanPostProcessor.registerFromScan(context, config.packages);
        ReplaceBeanPostProcessor.registerFromFactory(context, config.factories);

        //3.打印替换配置.可以在此之前, 考虑提供移除配置
        logger.info(AnsiOutput.toString(AnsiColor.GREEN, ReplaceBeanPostProcessor.replaceMapToString(true)));
        //4.将ReplaceBeanPostProcessor添加到Spring容器;
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        ReplaceBeanPostProcessor replaceBeanPostProcessor = new ReplaceBeanPostProcessor(beanFactory);
        beanFactory.addBeanPostProcessor(replaceBeanPostProcessor);
        BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
        if (parentBeanFactory instanceof ConfigurableBeanFactory) {
            //父级可能还有父级,这里不知道有什么场景,所以就不循环了
            ((ConfigurableBeanFactory) parentBeanFactory).addBeanPostProcessor(replaceBeanPostProcessor);
        }
    }

    /**
     * 获取配置
     *
     * @param propertySources 多个配置源
     * @return 获取优化后的配置
     */
    private static Config optimizeConfig(MutablePropertySources propertySources) {
        Set<String> packageSet = new HashSet<>();
        Set<String> factorySet = new HashSet<>();
        propertySources.stream().forEach(source -> {
            Object packagesProperty = source.getProperty(PACKAGES);
            if (packagesProperty instanceof String) {
                String value = (String) packagesProperty;
                packageSet.addAll(StringUtils.commaDelimitedListToSet(value));
            }
            Object factoriesProperty = source.getProperty(FACTORIES);
            if (factoriesProperty instanceof String) {
                String value = (String) factoriesProperty;
                factorySet.addAll(StringUtils.commaDelimitedListToSet(value));
            }
        });
        return new Config(packages(packageSet), factories(factorySet));
    }

    /**
     * 配置类
     */
    private static class Config {
        public final List<String> packages;
        public final List<Object> factories;

        public Config(List<String> packages, List<Object> factories) {
            this.packages = packages;
            this.factories = factories;
        }
    }

    /**
     * 获取扫描路径.如果路径有包含关系,则只保留最短路径
     *
     * @param packages 扫描路径列表
     */
    private static List<String> packages(Set<String> packages) {
        if (CollectionUtils.isEmpty(packages)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(packages);
        if (packages.size() > 1) {
            result.sort(String::compareTo);
            List<String> removes = new ArrayList<>();
            for (int i = 1, size = result.size(); i < size; i++) {
                if ((result.get(i) + ".").startsWith(result.get(i - 1) + ".")) {
                    removes.add(result.get(i));
                }
            }
            result.removeAll(removes);
        }
        return result;
    }

    /**
     * 获取指定Bean替换的配置工厂实例
     *
     * @param factories 工厂类
     * @return 工厂实例
     */
    private static List<Object> factories(Set<String> factories) {
        if (CollectionUtils.isEmpty(factories)) {
            return Collections.emptyList();
        }
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        return factories.stream().map(factory -> {
            try {
                Class<?> factoryClass = ClassUtils.forName(factory, classLoader);
                return ReflectionUtils.accessibleConstructor(factoryClass, new Class[0]).newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
    }
}
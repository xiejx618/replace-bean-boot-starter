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
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 监听Spring应用上下文初始化事件
 */
public class ReplaceBeanInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger logger = LoggerFactory.getLogger(ReplaceBeanInitializer.class);
    private static final String PACKAGES = "replace.packages";
    private static final String FACTORIES = "replace.factories";

    private static final String BOOTSTRAP_ENABLED_PROPERTY = "spring.cloud.bootstrap.enabled";
    private static final String MARKER_CLASS = "org.springframework.cloud.bootstrap.marker.Marker";
    private static final String USE_LEGACY_PROCESSING_PROPERTY = "spring.config.use-legacy-processing";
    private static final AtomicInteger TIMES = new AtomicInteger(0);//进来次数

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        //如果启用了bootstrap,就只有第二次进来才处理,第一次和后面再进来都不处理. 主要为了拦住第一次,拿不到application.yaml
        //没启用bootstrap,就只有第一次进来才处理,后面再进来都不处理
        if (bootstrapEnabled(environment)) {
            if (TIMES.incrementAndGet() != 2) {
                return;
            }
        } else {
            if (TIMES.incrementAndGet() != 1) {
                return;
            }
        }
        //1.获取替换Bean配置. 默认值不启用bean替换功能
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
     * 是否启用bootstrap
     */
    private static boolean bootstrapEnabled(Environment environment) {
        return environment.getProperty(BOOTSTRAP_ENABLED_PROPERTY, Boolean.class, false)
                || ClassUtils.isPresent(MARKER_CLASS, null)
                || environment.getProperty(USE_LEGACY_PROCESSING_PROPERTY, Boolean.class, false);
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
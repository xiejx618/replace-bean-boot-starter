package io.github.xiejx618.replace;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.util.*;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 处理所有Bean实例化前先检查是否有替换bean配置.如果有,就做替换.
 */
public class ReplaceBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    private static final Map<String, ReplaceInfo> replaceMap = new HashMap<>();

    private final ConfigurableBeanFactory beanFactory;

    public ReplaceBeanPostProcessor(ConfigurableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        //如果bean经过了scope代理,则取原beanName获取替换信息
        ReplaceInfo replaceInfo = replaceMap.get(ScopedProxyUtils.isScopedTarget(beanName) ?
                ScopedProxyUtils.getOriginalBeanName(beanName) : beanName);
        if (replaceInfo != null) {
            BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
            Method method = replaceInfo.getMethod();
            Object factory = replaceInfo.getFactory();
            String clazz = replaceInfo.getClazz();
            if (method != null && factory != null) {
                //通过工厂方法直接生成实例
                if (beanDefinition instanceof AbstractBeanDefinition) {
                    Supplier<?> instanceSupplier = () -> ReflectionUtils.invokeMethod(method, factory);
                    ((AbstractBeanDefinition) beanDefinition).setInstanceSupplier(instanceSupplier);
                } else {
                    throw new IllegalStateException("不支持的BeanDefinition类型:" + beanDefinition.getClass());
                }
            } else if (StringUtils.hasText(clazz)) {
                //通过beanClass反射生成实例
                beanDefinition.setBeanClassName(clazz);
                if (beanDefinition instanceof AbstractBeanDefinition) {
                    //为了兼容spring aot,强制不使用InstanceSupplier
                    ((AbstractBeanDefinition) beanDefinition).setInstanceSupplier(null);
                }
            } else {
                throw new IllegalStateException("method和clazz为空,替换失败");
            }
            replaceInfo.replaced = true;
        }
        return null;
    }

    /**
     * 通过工厂方法注册
     *
     * @param context   上下文
     * @param factories 来源工厂实例
     */
    public static void registerFromFactory(ConfigurableApplicationContext context, List<Object> factories) {
        if (CollectionUtils.isEmpty(factories)) {
            return;
        }
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        ConfigurableEnvironment environment = context.getEnvironment();
        for (Object factory : factories) {
            List<Method> methods = Arrays.stream(factory.getClass().getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())
                            && m.isAnnotationPresent(Replace.class)).collect(Collectors.toList());
            if (methods.isEmpty()) {
                continue;
            }
            injectFactoryFields(factory, beanFactory, environment);
            for (Method method : methods) {
                Replace annotation = method.getAnnotation(Replace.class);
                String beanName = StringUtils.hasText(annotation.value()) ? annotation.value() : method.getName();
                ReplaceInfo replaceInfo = replaceMap.get(beanName);
                int order = annotation.order();
                if (replaceInfo == null || order < replaceInfo.getOrder()) {
                    replaceMap.put(beanName, new ReplaceInfo(order, method, factory));
                }
            }
        }
    }

    /**
     * 注入工厂的beanFactory和environment
     *
     * @param factory     替换工厂实例
     * @param beanFactory bean工厂
     * @param environment 环境
     */
    private static void injectFactoryFields(Object factory, ConfigurableListableBeanFactory beanFactory,
                                            ConfigurableEnvironment environment) {
        Field[] fields = factory.getClass().getDeclaredFields();
        for (Field field : fields) {
            Class<?> type = field.getType();
            if (type.isAssignableFrom(ConfigurableListableBeanFactory.class)) {
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, factory, beanFactory);
            } else if (type.isAssignableFrom(ConfigurableEnvironment.class)) {
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, factory, environment);
            }
        }
    }

    /**
     * 通过包名注册
     *
     * @param packages 包名
     */
    public static void registerFromScan(ConfigurableApplicationContext context, Collection<String> packages) {
        if (CollectionUtils.isEmpty(packages)) {
            return;
        }
        //将读取过Resource的MetadataReader缓存起来,供后面的CachingMetadataReaderFactory使用.
        CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(context);
        for (String pkg : packages) {
            try {
                Resource[] resources = context.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                        + ClassUtils.convertClassNameToResourcePath(pkg.trim()) + "/**/*.class");
                for (Resource resource : resources) {
                    AnnotationMetadata metadata = readerFactory.getMetadataReader(resource).getAnnotationMetadata();
                    Map<String, Object> attributes = metadata.getAnnotationAttributes(Replace.class.getName());
                    if (attributes != null) {
                        String superClassName = metadata.getSuperClassName();
                        Assert.isTrue(StringUtils.hasText(superClassName), "替换bean的类不能没有父类");
                        String beanName = deduceBeanName(attributes, superClassName);
                        ReplaceInfo replaceInfo = replaceMap.get(beanName);
                        int order = (int) attributes.get("order");
                        if (replaceInfo == null || order < replaceInfo.getOrder()) {
                            replaceMap.put(beanName, new ReplaceInfo(order, metadata.getClassName()));
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 优先从注解获取beanName;获取不到时,再从父类类名推断
     *
     * @param attributes     注解属性
     * @param superClassName 父类类名
     * @return bean名称
     */
    private static String deduceBeanName(Map<String, Object> attributes, String superClassName) {
        String beanName = (String) attributes.get("value");
        if (StringUtils.hasText(beanName)) {
            return beanName;
        }
        return Introspector.decapitalize(ClassUtils.getShortName(superClassName));
    }

    /**
     * 断言替换Bean信息是否为空,并且打印替换Bean信息
     *
     * @param assertEmpty 是否断言为空
     * @return 打印信息
     */
    public static String replaceMapToString(boolean assertEmpty) {
        Assert.isTrue(!assertEmpty || !replaceMap.isEmpty(),
                "已启用Bean替换,但没有找到替换配置,请重新检查配置或者关闭Bean替换.");
        StringBuilder sb = new StringBuilder("替换Bean配置如下:\n");
        replaceMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> sb.append("  ")
                .append(e.getValue().print()).append("替换").append(e.getKey()).append(";\n"));
        return sb.toString();
    }

    /**
     * 获取未替换的bean(应在应用启动后调用)
     */
    public static List<String> unReplacedBean() {
        return replaceMap.entrySet().stream().filter(entry -> !entry.getValue().replaced)
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    static class ReplaceInfo implements Serializable {
        //顺序
        private final int order;
        //实例化工厂方法和工厂实例
        private final Method method;
        private final Object factory;
        //实例class
        private final String clazz;
        //是否已替换
        private boolean replaced;

        public int getOrder() {
            return order;
        }

        public Method getMethod() {
            return method;
        }

        public Object getFactory() {
            return factory;
        }

        public String getClazz() {
            return clazz;
        }

        //通过clazz方式
        public ReplaceInfo(int order, String clazz) {
            this.order = order;
            this.clazz = clazz;
            this.method = null;
            this.factory = null;
        }

        //通过工厂方式
        public ReplaceInfo(int order, Method method, Object factory) {
            this.order = order;
            this.method = method;
            this.factory = factory;
            this.clazz = null;
        }

        public String print() {
            return (method != null && factory != null ? factory.getClass().getName() + "#" + method.getName()
                    : clazz) + "[" + order + "]";
        }
    }
}

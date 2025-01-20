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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 处理所有Bean实例化前先检查是否有替换bean配置.如果有,就做替换.
 */
public class ReplaceBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

    private static final String SCOPED_PROXY_FACTORY_BEAN = "org.springframework.aop.scope.ScopedProxyFactoryBean";
    private static final Map<String, ReplaceInfo> replaceMap = new HashMap<>();

    private final ConfigurableBeanFactory beanFactory;

    public ReplaceBeanPostProcessor(ConfigurableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        //如果bean经过了scope代理,就取原beanName替换信息
        ReplaceInfo replaceInfo = replaceMap.get(ScopedProxyUtils.isScopedTarget(beanName) ?
                ScopedProxyUtils.getOriginalBeanName(beanName) : beanName);
        if (replaceInfo == null) {
            return null;
        }
        BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
        //排除ScopedProxyFactoryBean替换
        if (SCOPED_PROXY_FACTORY_BEAN.equals(beanDefinition.getBeanClassName())) {
            return null;
        }
        Method method = replaceInfo.getMethod();
        String clazz = replaceInfo.getClazz();
        if (method != null) {
            //通过工厂方法直接生成实例
            if (beanDefinition instanceof AbstractBeanDefinition) {
                Supplier<?> instanceSupplier = () -> ReflectionUtils.invokeMethod(method, null, replaceInfo.getArgs());
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
        return null;
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
                    if (attributes == null) {
                        continue;
                    }
                    String superClassName = metadata.getSuperClassName();
                    Assert.isTrue(StringUtils.hasText(superClassName), "替换bean的类不能没有父类");
                    String beanName = deduceBeanName(attributes, superClassName);
                    ReplaceInfo replaceInfo = replaceMap.get(beanName);
                    int order = (int) attributes.get("order");
                    if (replaceInfo != null && order >= replaceInfo.getOrder()) {
                        continue;
                    }
                    String instantiateMethod = (String) attributes.get("instantiateMethod");
                    String className = metadata.getClassName();
                    register(context, beanName, order, className, instantiateMethod);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 根据指定类和名称查找唯一的静态方法
     *
     * @param clazz 要找的类
     * @param name  方法名
     * @return 找到的方法
     * @throws NoSuchMethodException 找不到方法异常
     */
    private static Method findUniqueStaticMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        Method method = Stream.of(clazz.getDeclaredMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()) && m.getName().equals(name))
                .reduce((m1, m2) -> {
                    throw new IllegalArgumentException("在" + clazz.getCanonicalName() + "类找到" + name + "方法多于一个");
                }).orElseThrow(() -> new NoSuchMethodException(
                        "在" + clazz.getCanonicalName() + "类上找不到静态的" + name + "方法"));
        if (!Modifier.isPublic(method.getModifiers())) {
            method.setAccessible(true);
        }
        return method;
    }

    /**
     * 最终注册替换信息
     *
     * @param context    ConfigurableApplicationContext
     * @param beanName   bean名称
     * @param order      顺序
     * @param className  扩展类名
     * @param methodName 静态实例化方法名
     * @throws ClassNotFoundException 找不到扩展类时会抛此异常
     * @throws NoSuchMethodException  找不到静态实例方法会抛此异常
     */
    private static void register(ConfigurableApplicationContext context, String beanName, int order, String className,
                                 String methodName) throws ClassNotFoundException, NoSuchMethodException {
        if (!StringUtils.hasText(methodName)) {
            replaceMap.put(beanName, new ReplaceInfo(order, className));
            return;
        }
        Method method = findUniqueStaticMethod(Class.forName(className), methodName);
        int paramCount = method.getParameterCount();
        Object[] args = new Object[paramCount];
        if (paramCount > 0) {
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < paramCount; i++) {
                Class<?> type = parameters[i].getType();
                if (type.isAssignableFrom(ConfigurableApplicationContext.class)) {
                    args[i] = context;
                } else if (type.isAssignableFrom(ConfigurableListableBeanFactory.class)) {
                    args[i] = context.getBeanFactory();
                } else if (type.isAssignableFrom(ConfigurableEnvironment.class)) {
                    args[i] = context.getEnvironment();
                } else {
                    throw new IllegalArgumentException("不支持的方法[" + className + "#" + methodName + "]参数类型" + type);
                }
            }
        }
        replaceMap.put(beanName, new ReplaceInfo(order, className, method, args));
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

    /**
     * 替换信息
     */
    static class ReplaceInfo implements Serializable {
        //顺序
        private final int order;
        //实例化Bean方法
        private final Method method;
        //使用的参数
        private final Object[] args;
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

        public Object[] getArgs() {
            return args;
        }

        public String getClazz() {
            return clazz;
        }

        //通过beanClass方式
        public ReplaceInfo(int order, String clazz) {
            this(order, clazz, null, null);
        }

        //通过自定义实例化方法
        public ReplaceInfo(int order, String clazz, Method method, Object[] params) {
            this.order = order;
            this.clazz = clazz;
            this.method = method;
            this.args = params;
        }

        public String print() {
            return clazz + "[" + order + (method != null ? "," + method.getName() : "") + "]";
        }
    }
}

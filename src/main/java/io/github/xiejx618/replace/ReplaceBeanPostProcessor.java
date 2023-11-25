package io.github.xiejx618.replace;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理所有Bean实例化前先检查是否有替换子类实现.如果有,就做替换.
 * 注册成PriorityOrdered BeanPostProcessor是为了更早注册到beanFactory.
 * 而在实例化ReplaceBeanPostProcessor Bean,就会准备好bean替换信息, 然后就会注册到beanFactory
 */
public class ReplaceBeanPostProcessor implements BeanFactoryAware, InstantiationAwareBeanPostProcessor, PriorityOrdered {

    private static final Map<String, BeanInfo> replaceMap = new HashMap<>();

    private AbstractBeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        Assert.isTrue(beanFactory instanceof AbstractBeanFactory,
                "ReplaceBeanPostProcessor注入beanFactory失败:类型非AbstractBeanFactory");
        this.beanFactory = (AbstractBeanFactory) beanFactory;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if (replaceMap.containsKey(beanName)) {
            //如果bean不存在时,会抛异常
            BeanDefinition mergedBeanDefinition = beanFactory.getMergedBeanDefinition(beanName);
            mergedBeanDefinition.setBeanClassName(replaceMap.get(beanName).getBeanClass());
            if (mergedBeanDefinition instanceof AbstractBeanDefinition) {
                //为了兼容spring aot,强制不使用InstanceSupplier
                ((AbstractBeanDefinition) mergedBeanDefinition).setInstanceSupplier(null);
            }
        }
        return InstantiationAwareBeanPostProcessor.super.postProcessBeforeInstantiation(beanClass, beanName);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 断言替换Bean信息是否为空,并且打印替换Bean信息
     *
     * @param assertEmpty 是否断言为空
     * @return 打印信息
     */
    public static String replaceMapToString(boolean assertEmpty) {
        Assert.isTrue(!assertEmpty || !replaceMap.isEmpty(),
                "已启用替换Bean,但没有找到扩展子类,请检查配置.");
        StringBuilder sb = new StringBuilder("替换Bean如下:\n");
        replaceMap.forEach((k, v) -> sb.append(v.getBeanClass()).append("[").append(v.getOrder())
                .append("]替换了").append(k).append(";\n"));
        return sb.toString();
    }

    /**
     * 单个注册
     *
     * @param beanName     bean名称
     * @param replaceClass 替换类
     */
    public static void register(String beanName, String replaceClass) {
        int index = replaceClass.lastIndexOf(':');
        BeanInfo beanInfo = (index == -1) ? new BeanInfo(replaceClass, Replace.DEFAULT) :
                new BeanInfo(replaceClass.substring(0, index), Integer.parseInt(replaceClass.substring(index + 1)));
        register(beanName, beanInfo);
    }


    /**
     * 通过包名注册
     *
     * @param packages 包名
     */
    public static void registerScan(String... packages) {
        Assert.isTrue(packages != null && packages.length > 0, "替换bean扫描路径不能为空");
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
        for (String pkg : packages) {
            try {
                Resource[] resources = resourcePatternResolver.getResources(
                        ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                                + ClassUtils.convertClassNameToResourcePath(pkg.trim()) + "**/*.class");
                for (Resource resource : resources) {
                    AnnotationMetadata metadata = readerFactory.getMetadataReader(resource).getAnnotationMetadata();
                    Map<String, Object> attributes = metadata.getAnnotationAttributes(Replace.class.getName());
                    if (attributes != null) {
                        String superClassName = metadata.getSuperClassName();
                        Assert.isTrue(StringUtils.hasText(superClassName), "替换bean的类不能没有父类");
                        register(deduceBeanName(attributes, superClassName),
                                new BeanInfo(metadata.getClassName(), (int) attributes.get("order")));
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
     * 按优先级注册
     *
     * @param beanName bean名称
     * @param beanInfo 替换类信息
     */
    private static void register(String beanName, BeanInfo beanInfo) {
        if (!replaceMap.containsKey(beanName) || beanInfo.getOrder() <= replaceMap.get(beanName).getOrder()) {
            replaceMap.put(beanName, beanInfo);
        }
    }


    static class BeanInfo implements Serializable {

        private final String beanClass;
        private final int order;

        public String getBeanClass() {
            return beanClass;
        }

        public int getOrder() {
            return order;
        }

        public BeanInfo(String beanClass, int order) {
            this.beanClass = beanClass;
            this.order = order;
        }
    }
}

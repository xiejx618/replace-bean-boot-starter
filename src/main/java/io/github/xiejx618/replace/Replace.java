package io.github.xiejx618.replace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识为Bean替换
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Replace {

    /**
     * 需要替换的Bean的beanName.
     * 当没指定时, 会从父类推断(推断不一定正确)
     * @return 返回beanName
     */
    String value() default "";

    int DEFAULT = Integer.MAX_VALUE;

    /**
     * 如果一个bean被多次扩展时, 就选择排序最小值的类. 可为负数.
     * 定义排序时, 建议选择现值减去一个小值, 比如现值为DEFAULT-10, 则可定义为DEFAULT-11.
     * 注意:对于bean已被扩展, 定义值不要和现值一样或比现值还小, 这样可能会导致扩展无法生效;
     * 另外初次扩展定义不应为Integer.MIN_VALUE, 这样会导致后面无法扩展.
     * @return 返回排序值
     */
    int order() default DEFAULT;
}
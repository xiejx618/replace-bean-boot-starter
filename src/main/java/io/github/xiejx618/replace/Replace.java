package io.github.xiejx618.replace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在类上标识为Bean替换
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Replace {

    /**
     * 需要替换的Bean的beanName. 当没指定时,会从父类名推断;
     *
     * @return 返回beanName
     */
    String value() default "";

    /**
     * 如果一个bean被多次扩展时, 就选择排序最小值的类. 值范围-2147483648到2147483647.
     * 定义排序时, 建议选择现值减去一个小值, 比如现值为DEFAULT-10, 则可定义为DEFAULT-11.
     * 注意:对于bean已被扩展, 定义值不要和现值一样或比现值还大, 这样会导致扩展无法生效;
     * 另外初次扩展定义不应为Integer.MIN_VALUE, 这样会导致后面无法再次扩展.
     *
     * @return 返回排序值
     */
    int order() default Integer.MAX_VALUE;

    /**
     * 指定的扩展类的静态实例化方法名称.
     * 当有指定时,使用这个静态方法实例化Bean对象,对于@Bean声明的bean,可以使用此方式指定;
     * 否则,默认使用扩展类构造函数反射实例化.
     *
     * @return 实例化方法名称
     */
    String instantiateMethod() default "";
}
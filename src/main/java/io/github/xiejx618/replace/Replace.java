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
     * 如果一个bean被多次替换时, 就选择排序最小值的类. 值范围-2147483648到2147483647.
     * 定义排序时, 建议选择现值减去一个小值, 比如现值为DEFAULT-10, 则可定义为DEFAULT-11.
     * 注意:对于bean已被替换, 定义值必须比现值大, 否则替换无法生效;
     * 另外初次定义不应为Integer.MIN_VALUE, 这样会导致后面无法再次替换.
     *
     * @return 返回排序值
     */
    int order() default Integer.MAX_VALUE;

    /**
     * 指定的替换类的静态实例化方法名称,一般不指定就可生效,建议不指定.
     * 当有指定时,使用这个静态方法实例化Bean对象,对于@Bean声明的Bean,需使用此方式替换;
     *
     * @return 实例化方法名称
     *
     * <pre>{@code
     * @Replace(instantiateMethod = "instantiate")
     * public class MyDispatcherServlet extends DispatcherServlet {
     *
     *     public static DispatcherServlet instantiate(BeanFactory beanFactory, Environment environment) {
     *         WebMvcProperties webMvcProperties = beanFactory.getBean(WebMvcProperties.class);
     *         MyDispatcherServlet dispatcherServlet = new MyDispatcherServlet();
     *         dispatcherServlet.setDispatchOptionsRequest(webMvcProperties.isDispatchOptionsRequest());
     *         dispatcherServlet.setDispatchTraceRequest(webMvcProperties.isDispatchTraceRequest());
     *         dispatcherServlet.setThrowExceptionIfNoHandlerFound(webMvcProperties.isThrowExceptionIfNoHandlerFound());
     *         dispatcherServlet.setPublishEvents(webMvcProperties.isPublishRequestHandledEvents());
     *         dispatcherServlet.setEnableLoggingRequestDetails(webMvcProperties.isLogRequestDetails());
     *         return dispatcherServlet;
     *     }
     *
     *     @Override
     *     protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
     *         //System.out.println("MyDispatcherServlet#doDispatch:" + request.getServletPath());
     *         super.doDispatch(request, response);
     *     }
     * }
     * }</pre>
     */
    String instantiateMethod() default "";
}
package cn.com.xcar.bean;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * 可用于Java Main方法引导和启动Spring应用程序的类
 *
 * @Author lxj
 * @Date 2018-10-24 10:12
 **/
public class DefineSpringApplication extends SpringApplication {

    /**
     * 默认情况用于非Web环境的应用程序上下文的类名
     */
    public static final String DEFAULT_CONTEXT_CLASS = "org.springframework.context.annotation.AnnotationConfigApplicationContext";

    /**
     * Web环境默认使用的应用程序上下文类名
     */
    public static final String DEFAULT_WEB_CONTEXT_CLASS = "org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext";

    /**
     * Web环境的类名集合
     */
    public static final String[] WEB_ENVIRONMENT_CLASSES = {"javax.servlet.Servlet", "org.springframework.web.context.ConfigurableWebApplicationContext"};

    /**
     * 默认情况下用于响应式Web环境应用程序上下文类名
     */
    public static final String DEFAULT_REACTIVE_WEB_CONTEXT_CLASS = "org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext";

    /**
     * 响应式Web环境类名
     */
    private static final String REACTIVE_WEB_ENVIRONMENT_CLASS = "org.springframework.web.reactive.DispatcherHandler";

    /**
     * MVC Web环境类名
     */
    private static final String MVC_WEB_ENVIRONMENT_CLASS = "org.springframework.web.servlet.DispatcherServlet";

    /**
     * Jersey Web环境类名
     */
    private static final String JERSEY_WEB_ENVIRONMENT_CLASS = "org.glassfish.jersey.server.ResourceConfig";

    /**
     * 默认Banner的位置 -> SpringApplicationBannerPrinter#DEFAULT_BANNER_LOCATION
     */
    public static final String BANNER_LOCATION_PROPERTY_VALUE = "banner.txt";

    /**
     * Banner位置的属性key -> SpringApplicationBannerPrinter#BANNER_LOCATION_PROPERTY
     */
    public static final String BANNER_LOCATION_PROPERTY = "spring.banner.location";

    /**
     * 系统属性
     */
    private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

    /**
     * 日志
     */
    private static final Log logger = LogFactory.getLog(SpringApplication.class);

    /**
     * 主要源
     */
    private Set<Class<?>> primarySources;

    /**
     * 源
     */
    private Set<String> sources = new LinkedHashSet<>();

    /**
     * 主要应用类
     */
    private Class<?> mainApplicationClass;

    /**
     * Banner输出控制
     */
    private Banner.Mode bannerMode = Banner.Mode.CONSOLE;

    /**
     * 记录启动信息
     */
    private boolean logStartupInfo = true;

    /**
     * 添加命令行属性
     */
    private boolean addCommandLineProperties = true;

    /**
     * Banner
     */
    private Banner banner;

    /**
     * 资源加载器
     */
    private ResourceLoader resourceLoader;

    /**
     * Bean名称生成器
     */
    private BeanNameGenerator beanNameGenerator;

    /**
     * 上下文环境
     */
    private ConfigurableEnvironment environment;

    /**
     * 上下文类
     */
    private Class<? extends ConfigurableApplicationContext> applicationContextClass;

    /**
     * 应用程序类型
     */
    private WebApplicationType webApplicationType;

    /**
     * 是否设置java.awt.headless
     */
    private boolean headless = true;

    /**
     * 是否注册关闭处理
     */
    private boolean registerShutdownHook = true;

    /**
     * 应用程序初始器
     */
    private List<ApplicationContextInitializer<?>> initializers;

    /**
     * 应用程序监听器
     */
    private List<ApplicationListener<?>> listeners;

    /**
     * 默认属性
     */
    private Map<String, Object> defaultProperties;

    /**
     * 额外属性
     */
    private Set<String> additionalProfiles = new HashSet<>();

    /**
     * 是否自定义环境
     */
    private boolean isCustomEnvironment = false;

    /******************************************=== 初始化 head ===*********************************××**************/

    public DefineSpringApplication(Class<?>... primarySources) {

        this(null, primarySources);

    }

    /**
     * 构造方法
     * 1.设置资源加载器
     * 2.设置主要源
     * 3.推断应用程序类型       {@link this#deduceWebApplicationType}
     * 4.设置应用程序初始器      {@link this#getSpringFactoriesInstances}
     * 5.设置应用程序监听器     {@link this#getSpringFactoriesInstances}
     * 6.推断主要应用程序类     {@link this#deduceMainApplicationClass}
     *
     * @param resourceLoader
     * @param primarySources
     */
    public DefineSpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {

        this.resourceLoader = resourceLoader;

        Assert.notNull(primarySources, "PrimarySources must not be null");

        this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));

        this.webApplicationType = deduceWebApplicationType();

        setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));

        setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));

        this.mainApplicationClass = deduceMainApplicationClass();

    }

    /**
     * 推断应用程序类型
     * 通过类名是否存在并且可以加载判断
     * 1.存在响应式Web类名，不存在MVC Web环境类名，不存在Jersey Web环境类名,则返回响应式Web类型
     * 2.遍历Web环境类名集合，当有一个类名不存在返回为Web应用类型
     * 3.否则返回Servlet Web类型
     *
     * @return
     */
    private WebApplicationType deduceWebApplicationType() {


        if (ClassUtils.isPresent(REACTIVE_WEB_ENVIRONMENT_CLASS, null) && !ClassUtils.isPresent(MVC_WEB_ENVIRONMENT_CLASS, null) && !ClassUtils.isPresent(JERSEY_WEB_ENVIRONMENT_CLASS, null)) {

            return WebApplicationType.REACTIVE;

        }

        for (String className : WEB_ENVIRONMENT_CLASSES) {

            if (!ClassUtils.isPresent(className, null)) return WebApplicationType.NONE;

        }

        return WebApplicationType.SERVLET;

    }

    /**
     * 设置应用上下文初始器
     *
     * @param initializers 初始器集合
     */
    public void setInitializers(Collection<? extends ApplicationContextInitializer<?>> initializers) {

        this.initializers = new ArrayList<>();

        this.initializers.addAll(initializers);

    }

    /**
     * 根据Class获取配置集合
     *
     * @param type type
     * @param <T>  <T>
     * @return instances
     */
    private <T> Collection<T> getSpringFactoriesInstances(Class<T> type) {

        return getSpringFactoriesInstances(type, new Class<?>[]{});

    }

    /**
     * 1.获取当前上下文类加载器
     * 2.SpringFactoriesLoader加载spring.factories配置项,ApplicationContextInitializer初始配置既在里边
     * 3.类加载器获取资源可能有多个spring.factories,location:spring-boot.jar + location:spring-boot-autoconfigure.jar
     * 4.根据AnnotationAwareOrderComparator排序,支持spring Ordered/@Order/@Priority,具体用法不太明白，暂时先当排序
     *
     * @param type           type
     * @param parameterTypes parameterTypes
     * @param args           args
     * @param <T>            <T>
     * @return instances
     */
    private <T> Collection<T> getSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes, Object... args) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Set<String> names = new LinkedHashSet<>(SpringFactoriesLoader.loadFactoryNames(type, classLoader));

        List<T> instances = createSpringFactoriesInstances(type, parameterTypes, classLoader, args, names);

        AnnotationAwareOrderComparator.sort(instances);

        return instances;

    }

    /**
     * 根据class实例
     *
     * @param type           type
     * @param parameterTypes parameterTypes
     * @param classLoader    classLoader
     * @param args           args
     * @param names          names
     * @param <T>            <T>
     * @return instances
     */
    private <T> List<T> createSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes, ClassLoader classLoader, Object[] args, Set<String> names) {

        List<T> instances = new ArrayList<>(names.size());

        for (String name : names) {

            try {

                Class<?> instanceClass = ClassUtils.forName(name, classLoader);

                Assert.isAssignable(type, instanceClass);

                Constructor<?> constructor = instanceClass.getDeclaredConstructor(parameterTypes);

                T instance = (T) BeanUtils.instantiateClass(constructor, args);

                instances.add(instance);

            } catch (Throwable ex) {

                throw new IllegalArgumentException("Cannot instantiate " + type + " : " + name, ex);

            }

        }

        return instances;

    }

    /**
     * 推断主要应用类
     *
     * @return Class
     */
    private Class<?> deduceMainApplicationClass() {

        try {

            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();

            for (StackTraceElement stackTraceElement : stackTrace) {

                if ("main".equals(stackTraceElement.getMethodName()))

                    return Class.forName(stackTraceElement.getClassName());

            }

        } catch (ClassNotFoundException ex) {

        }

        return null;

    }

    /******************************************=== 初始化 foot ===*********************************××**************/

    /**
     * 静态助手，可用于默认设置从指定的源运行
     *
     * @param primarySource 加载的主要源
     * @param args          应用程序参数，通常使用Main方法传递，例如 --spring.profiles.active/--spring.main.sources/etc
     * @return
     */
    public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {

        return run(new Class<?>[]{primarySource}, args);

    }

    /**
     * 静态助手，可用于默认设置从指定的源运行
     *
     * @param primarySources 加载的主要源
     * @param args           应用程序参数，通常使用Main方法传递，例如 --spring.profiles.active/--spring.main.sources/etc
     * @return
     */
    public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {

        return new DefineSpringApplication(primarySources).run(args);

    }

    /**
     * 运行Spring应用程序，创建和刷新{@link ApplicationContext}
     *
     * @param args 应用程序参数，通常使用Main方法传递，例如 --spring.profiles.active/--spring.main.sources/etc
     * @return {@link ConfigurableApplicationContext}
     */
    public ConfigurableApplicationContext run(String... args) {

        StopWatch stopWatch = new StopWatch();

        stopWatch.start();

        ConfigurableApplicationContext context = null;

        Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();

        configureHeadlessProperty();

        SpringApplicationRunListeners listeners = getRunListeners(args);

        listeners.starting();

        try {

            ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);

            ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);

            configureIgnoreBeanInfo(environment);

            Banner printedBanner = printBanner(environment);

            context = createApplicationContext();

            exceptionReporters = getSpringFactoriesInstances(SpringBootExceptionReporter.class, new Class[]{ConfigurableApplicationContext.class}, context);

            prepareContext(context, environment, listeners, applicationArguments, printedBanner);

            refreshContext(context);

            afterRefresh(context, applicationArguments);

            stopWatch.stop();

            if (this.logStartupInfo) new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);

            listeners.started(context);

            callRunners(context, applicationArguments);

        } catch (Throwable ex) {

            handleRunFailure(context, ex, exceptionReporters, listeners);

            throw new IllegalStateException(ex);

        }

        try {

            listeners.running(context);

        } catch (Throwable ex) {

            handleRunFailure(context, ex, exceptionReporters, null);

            throw new IllegalStateException(ex);

        }

        return context;

    }

}

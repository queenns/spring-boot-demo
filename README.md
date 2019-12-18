#Spring Boot源码阅读记录

- 该项目主要用户源码研究使用
- 之后将源码阅读记录做一个详细的项目记录



- 刚开始研究[typora](https://www.typora.io/),在正式开始记录之前正好熟悉用法


```java
package cn.com.queenns.source;


import cn.com.xcar.copy.EnvironmentConverter;
import cn.com.xcar.copy.SpringApplicationRunListeners;
import cn.com.xcar.copy.SpringApplicationBannerPrinter;
import cn.com.xcar.copy.StartupInfoLogger;
import cn.com.xcar.copy.BeanDefinitionLoader;
import cn.com.xcar.copy.ExitCodeGenerators;
import cn.com.xcar.copy.SpringBootExceptionHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.*;
import org.springframework.boot.context.ContextIdApplicationContextInitializer;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.*;
import org.springframework.web.context.support.StandardServletEnvironment;

import java.lang.reflect.Constructor;
import java.security.AccessControlException;
import java.util.*;

/**
 * SpringApplication源码
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
     * 2.遍历Web环境类名集合，当有一个类名不存在,返回非Web应用类型
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
     * <p>
     * 1.创建一个{@link StopWatch}任务计时秒表并启动，统计run运行时间
     * 2.配置Java_AWT_HEADLESS           {@link this#configureHeadlessProperty()}
     * 3.获取Spring应用程序运行监听器并启动   {@link this#getRunListeners(String[])}
     * 4.转换args                        {@link ApplicationArguments}
     * 5.做环境准备                       {@link this#prepareEnvironment(SpringApplicationRunListeners, ApplicationArguments)}
     * 6.设置spring.beaninfo.ignore      {@link this#configureIgnoreBeanInfo(ConfigurableEnvironment)}
     * 7.输出Banner                      {@link this#printBanner(ConfigurableEnvironment)}
     * 8.根据环境类型创建应用程序上下文环境     {@link this#createApplicationContext()}
     * 9.获取异常报告器
     * 10.准备应用程序上下文                {@link this#prepareContext(ConfigurableApplicationContext, ConfigurableEnvironment, SpringApplicationRunListeners, ApplicationArguments, Banner)}
     * 11.刷新应用程序上下文                {@link this#refreshContext(ConfigurableApplicationContext)}
     * 12.刷新后置处理                     {@link this#afterRefresh(ConfigurableApplicationContext, ApplicationArguments)}
     * 13.停止任务计时秒表
     * 14.打印启动信息日志
     * 15.上下文刷新成功监听器启动,步骤3中是为了早期的初始化
     * 16.调用Runner                      {@link this#callRunners(ApplicationContext, ApplicationArguments)}
     * 17.监听器执行
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

            if (this.logStartupInfo) {

                new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);

            }

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

    /**
     * 配置Java_AWT_HEADLESS
     * 获取系统JAVA_AWT_HEADLESS配置,如果System获取不到按照按照headless默认设置
     */
    private void configureHeadlessProperty() {

        String value = System.getProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless));

        System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, value);

    }

    /**
     * 获取Spring应用程序运行监听器
     *
     * @param args 应用程序参数，通常使用Main方法传递，例如 --spring.profiles.active/--spring.main.sources/etc
     * @return {@link SpringApplicationRunListeners}
     */
    private SpringApplicationRunListeners getRunListeners(String[] args) {

        Class<?>[] types = new Class<?>[]{SpringApplication.class, String[].class};

        return new SpringApplicationRunListeners(logger, getSpringFactoriesInstances(SpringApplicationRunListener.class, types, this, args));

    }

    /**
     * 准备环境
     * 1.获取或创建环境 {@link this#getOrCreateEnvironment()}
     * 2.配置环境 {@link this#configureEnvironment(ConfigurableEnvironment, String[])}
     * 3.为监听器做环境准备
     * 4.将环境绑定到应用程序
     * 5.如果应用程序是非Web应用类型,将环境转化为{@link StandardEnvironment}
     * 6.将{@link ConfigurationPropertySource}支持附加到环境上,将环境管理的{@link PropertySource}转化为{@link ConfigurationPropertySource},方便后续处理
     *
     * @param listeners            listeners
     * @param applicationArguments applicationArguments
     * @return {@link ConfigurableEnvironment}
     */
    private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners, ApplicationArguments applicationArguments) {

        ConfigurableEnvironment environment = getOrCreateEnvironment();

        configureEnvironment(environment, applicationArguments.getSourceArgs());

        listeners.environmentPrepared(environment);

        bindToSpringApplication(environment);

        if (this.webApplicationType == WebApplicationType.NONE)

            environment = new EnvironmentConverter(getClassLoader()).convertToStandardEnvironmentIfNecessary(environment);

        ConfigurationPropertySources.attach(environment);

        return environment;

    }

    /**
     * 获得或创建环境
     * 1.如果环境不为null,返回                    {@link this#environment}
     * 2.如果应用程序类型是Servlet Web类型,返回     {@link StandardServletEnvironment}
     * 3.否则返回                               {@link StandardEnvironment}
     *
     * @return
     */
    private ConfigurableEnvironment getOrCreateEnvironment() {

        if (this.environment != null) return this.environment;

        if (this.webApplicationType == WebApplicationType.SERVLET) return new StandardServletEnvironment();

        return new StandardEnvironment();

    }

    /**
     * 设置环境
     * 1.配置属性源
     * 2.配置属性
     *
     * @param environment environment
     * @param args        args
     */
    protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {

        configurePropertySources(environment, args);

        configureProfiles(environment, args);

    }

    /**
     * 在此应用程序的环境添加/删除/重排序{@link PropertySource}s
     * 1.创建一个可变的属性源集合类 {@link MutablePropertySources}
     * 2.如果默认属性不为空,将默认属性设置到属性源集合
     * 3.如果设置命令行为true且有参数,将命令行参数设置到属性源集合
     *
     * @param environment 应用程序环境
     * @param args        args
     */
    protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {

        MutablePropertySources sources = environment.getPropertySources();

        if (this.defaultProperties != null && !this.defaultProperties.isEmpty())

            sources.addLast(new MapPropertySource("defaultProperties", this.defaultProperties));

        if (this.addCommandLineProperties && args.length > 0) {

            String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;

            if (sources.contains(name)) {

                PropertySource<?> source = sources.get(name);

                CompositePropertySource composite = new CompositePropertySource(name);

                composite.addPropertySource(new SimpleCommandLinePropertySource("springApplicationCommandLineArgs", args));

                composite.addPropertySource(source);

                sources.replace(name, composite);

            } else {

                sources.addFirst(new SimpleCommandLinePropertySource(args));

            }

        }

    }

    /**
     * 将额外的属性和激活的属性综合
     * 1.确保激活的配置属性被初始化
     * 2.通过有序集合确保顺序,将额外属性添加到有序集合的首位,再将激活的属性添加之后
     * 3.将综合的属性转化为数组形式设置到环境中
     * <p>
     * 源码中的注释，从代码解读中感觉有歧义，暂时按照上边的注释理解，如果理解有偏差后续再修正
     * 配置此应用程序环境激活的或默认的配置文件
     * 在配置文件处理期间激活其他配置文件可以通过{@code spring.profiles.active}属性,
     * 如spring.profiles.active=sandbox
     * 更多的可以查看{@link org.springframework.boot.context.config.ConfigFileApplicationListener}
     *
     * @param environment environment
     * @param args        args
     */
    protected void configureProfiles(ConfigurableEnvironment environment, String[] args) {

        environment.getActiveProfiles();

        Set<String> profiles = new LinkedHashSet<>(this.additionalProfiles);

        profiles.addAll(Arrays.asList(environment.getActiveProfiles()));

        environment.setActiveProfiles(StringUtils.toStringArray(profiles));

    }

    /**
     * 将环境绑定到应用程序
     * 通过{@link Bindable}绑定
     *
     * @param environment 绑定的环境
     */
    protected void bindToSpringApplication(ConfigurableEnvironment environment) {

        try {

            Binder.get(environment).bind("spring.main", Bindable.ofInstance(this));

        } catch (Exception ex) {

            throw new IllegalStateException("Cannot bind to SpringApplication", ex);

        }

    }

    /**
     * 设置系统环境 : spring.beaninfo.ignore
     * 该属性大概意思涉及到java内省机制和Spring自己的内省,主要是应用程序类加载器自身的缓存,避免依赖Jdk系统缓存的BeanInfo,在关闭类加载器造成泄露问题
     * 后续深入了解再补充
     * {@link CachedIntrospectionResults}
     * {@link java.beans.BeanInfo}
     * {@link java.beans.Introspector}
     *
     * @param environment environment
     */
    private void configureIgnoreBeanInfo(ConfigurableEnvironment environment) {

        if (System.getProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME) == null) {

            Boolean ignore = environment.getProperty("spring.beaninfo.ignore", Boolean.class, Boolean.TRUE);

            System.setProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME, ignore.toString());

        }

    }

    /**
     * 输出 SpringBoot Banner
     * 对应控制台或日志中SpringBoot启动时的Spring图标及Spring Boot标志和版本号
     * 内部实现大概是找图片Banner,再找文本Banner,一般最后都找不到,使用默认Banner
     * 默认Banner使用数组画出来的{@link SpringBootBanner}
     *
     * @param environment
     * @return
     */
    private Banner printBanner(ConfigurableEnvironment environment) {

        if (this.bannerMode == Banner.Mode.OFF) return null;

        ResourceLoader resourceLoader = (this.resourceLoader != null ? this.resourceLoader : new DefaultResourceLoader(getClassLoader()));

        SpringApplicationBannerPrinter bannerPrinter = new SpringApplicationBannerPrinter(resourceLoader, this.banner);

        if (this.bannerMode == Banner.Mode.LOG)

            return bannerPrinter.print(environment, this.mainApplicationClass, logger);

        return bannerPrinter.print(environment, this.mainApplicationClass, System.out);

    }

    /**
     * 创建应用程序上下文
     *
     * @return
     */
    protected ConfigurableApplicationContext createApplicationContext() {

        Class<?> contextClass = this.applicationContextClass;

        if (contextClass == null) {

            try {

                switch (this.webApplicationType) {

                    case SERVLET:
                        contextClass = Class.forName(DEFAULT_WEB_CONTEXT_CLASS);
                        break;

                    case REACTIVE:
                        contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS);
                        break;

                    default:
                        contextClass = Class.forName(DEFAULT_CONTEXT_CLASS);

                }

            } catch (ClassNotFoundException ex) {

                throw new IllegalStateException("Unable create a default ApplicationContext, please specify an ApplicationContextClass", ex);

            }

        }

        return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass);

    }

    /**
     * 准备应用程序上下文
     * 1.为上下文设置环境
     * 2.为上下文自定义扩展一些东西             {@link this#postProcessApplicationContext(ConfigurableApplicationContext)}
     * 3.在上下文刷新之前将初始器应用到上下文      {@link this#applyInitializers(ConfigurableApplicationContext)}
     * 4.上下文加载准备时，加载源之前监听器处理，从当前看执行监听器没有具体处理的内容
     * 5.打印启动信息日志及激活属性信息日志
     * 6.将{@link ApplicationArguments}以单例形式注册到BeanFactory
     * 7.根据条件判断Banner输出器是否以单例形式注册到BeanFactory
     * 8.合并主要源和其它源                   {@link this#getAllSources()}
     * 9.将所有源注册到注册列表
     * 10.将监听器加载到上下文
     *
     * @param context              context
     * @param environment          environment
     * @param listeners            listeners
     * @param applicationArguments applicationArguments
     * @param printedBanner        printedBanner
     */
    private void prepareContext(ConfigurableApplicationContext context, ConfigurableEnvironment environment, SpringApplicationRunListeners listeners, ApplicationArguments applicationArguments, Banner printedBanner) {

        context.setEnvironment(environment);

        postProcessApplicationContext(context);

        applyInitializers(context);

        listeners.contextPrepared(context);

        if (this.logStartupInfo) {

            logStartupInfo(context.getParent() == null);

            logStartupProfileInfo(context);

        }

        // Add boot specific singleton beans
        context.getBeanFactory().registerSingleton("springApplicationArguments", applicationArguments);

        if (printedBanner != null)

            context.getBeanFactory().registerSingleton("springBootBanner", printedBanner);

        // Load the sources
        Set<Object> sources = getAllSources();

        Assert.notEmpty(sources, "Sources must not be empty");

        load(context, sources.toArray(new Object[0]));

        listeners.contextLoaded(context);

    }

    /**
     * 为上下文应用任何相关的后置处理,子类可以根据需要应用附加的后置处理
     * 1.如果Bean名称生成器不为null,将该生产器注册到BeanFactory
     * {@link AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR} Spring内部管理的bean名称,用于处理{@Configuration}
     * 2.如果资源加载器不为null,为上下文设置资源加载器
     * <p>
     * 该方法默认执行时，俩个步骤都是执行不到的，而且方法名和处理内容感觉有歧义，不过通过方法是protected可以看出可以自定义扩展，可以为上下文自定义扩展一些东西
     *
     * @param context
     */
    protected void postProcessApplicationContext(ConfigurableApplicationContext context) {

        if (this.beanNameGenerator != null)

            context.getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, this.beanNameGenerator);

        if (this.resourceLoader != null) {

            if (context instanceof GenericApplicationContext)

                ((GenericApplicationContext) context).setResourceLoader(this.resourceLoader);

            if (context instanceof DefaultResourceLoader)

                ((DefaultResourceLoader) context).setClassLoader(this.resourceLoader.getClassLoader());

        }

    }

    /**
     * 在上下文刷新之前将初始器应用到上下文
     * 1.解决初始器类型参数
     * 2.验证初始器类型
     * 3.将初始器初始
     * <p>
     * 具体初始器何时加载回顾{@link this#getSpringFactoriesInstances}调用,具体初始器在加载时可以看到,主要还是配置在spring.factories
     * 例如{@link ContextIdApplicationContextInitializer#initialize(ConfigurableApplicationContext)}初始上下文Id
     * 当前查看是6个初始器,不同初始器具体做什么后续深入了解再进行补全,暂时理解为前置准备一些东西
     *
     * @param context context
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void applyInitializers(ConfigurableApplicationContext context) {

        for (ApplicationContextInitializer initializer : getInitializers()) {

            Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(), ApplicationContextInitializer.class);

            Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");

            initializer.initialize(context);

        }

    }

    /**
     * 获取初始器集合
     *
     * @return
     */
    public Set<ApplicationContextInitializer<?>> getInitializers() {

        return asUnmodifiableOrderedSet(this.initializers);

    }

    /**
     * 转化为不可修改的有序集合
     *
     * @param elements elements
     * @param <E>      <E>
     * @return <E>
     */
    private static <E> Set<E> asUnmodifiableOrderedSet(Collection<E> elements) {

        List<E> list = new ArrayList<>();

        list.addAll(elements);

        list.sort(AnnotationAwareOrderComparator.INSTANCE);

        return new LinkedHashSet<>(list);

    }

    /**
     * 调用记录启动信息日志，子类可以重写添加额外的信息
     *
     * @param isRoot 是否为根上下文
     */
    protected void logStartupInfo(boolean isRoot) {

        if (isRoot)

            new StartupInfoLogger(this.mainApplicationClass).logStarting(getApplicationLog());

    }

    /**
     * 调用记录激活的属性信息日志
     *
     * @param context 上下文
     */
    protected void logStartupProfileInfo(ConfigurableApplicationContext context) {

        Log log = getApplicationLog();

        if (log.isInfoEnabled()) {

            String[] activeProfiles = context.getEnvironment().getActiveProfiles();

            if (ObjectUtils.isEmpty(activeProfiles)) {

                String[] defaultProfiles = context.getEnvironment().getDefaultProfiles();

                log.info("No active profile set, falling back to default profiles: " + StringUtils.arrayToCommaDelimitedString(defaultProfiles));

            } else {

                log.info("The following profiles are active: " + StringUtils.arrayToCommaDelimitedString(activeProfiles));

            }

        }

    }

    /**
     * 合并主要源和其它源
     *
     * @return Set
     */
    public Set<Object> getAllSources() {

        Set<Object> allSources = new LinkedHashSet<>();

        if (!CollectionUtils.isEmpty(this.primarySources))

            allSources.addAll(this.primarySources);

        if (!CollectionUtils.isEmpty(this.sources))

            allSources.addAll(this.sources);

        return Collections.unmodifiableSet(allSources);

    }

    /**
     * 将bean加载到应用程序上下文
     *
     * @param context context
     * @param sources sources
     */
    protected void load(ApplicationContext context, Object[] sources) {

        if (logger.isDebugEnabled())

            logger.debug("Loading source " + StringUtils.arrayToCommaDelimitedString(sources));

        BeanDefinitionLoader loader = createBeanDefinitionLoaderCopy(getBeanDefinitionRegistry(context), sources);

        if (this.beanNameGenerator != null)

            loader.setBeanNameGenerator(this.beanNameGenerator);

        if (this.resourceLoader != null)

            loader.setResourceLoader(this.resourceLoader);

        if (this.environment != null)

            loader.setEnvironment(this.environment);

        loader.load();

    }

    /**
     * 获取bean定义注册器
     * 判断上下文转化类型
     *
     * @param context context
     * @return {@link BeanDefinitionRegistry}
     */
    private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {

        if (context instanceof BeanDefinitionRegistry)

            return (BeanDefinitionRegistry) context;

        if (context instanceof AbstractApplicationContext)

            return (BeanDefinitionRegistry) ((AbstractApplicationContext) context).getBeanFactory();

        throw new IllegalStateException("Could not locate BeanDefinitionRegistry");

    }

    /**
     * 创建bean定义加载器
     *
     * @param registry registry
     * @param sources  sources
     * @return {@link BeanDefinitionLoader}
     */
    protected BeanDefinitionLoader createBeanDefinitionLoaderCopy(BeanDefinitionRegistry registry, Object[] sources) {

        return new BeanDefinitionLoader(registry, sources);

    }

    /**
     * 刷新应用程序上下文
     * 判断是否注册上下文关闭钩子
     *
     * @param context context
     */
    private void refreshContext(ConfigurableApplicationContext context) {

        refresh(context);

        if (this.registerShutdownHook) {

            try {

                context.registerShutdownHook();

            } catch (AccessControlException ex) {
                // Not allowed in some environments.
            }

        }

    }

    /**
     * 刷新上下文
     *
     * @param applicationContext the application context to refresh
     */
    protected void refresh(ApplicationContext applicationContext) {

        Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);

        ((AbstractApplicationContext) applicationContext).refresh();

    }

    /**
     * 上下文刷新后调用，该方法可扩展.
     *
     * @param context context
     * @param args    args
     */
    protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {

    }

    /**
     * 调用Runner
     * ApplicationRunner,CommandLineRunner
     * Runner调用时机为容器启动完成,可以处理一些自定义操作
     *
     * @param context context
     * @param args    args
     */
    private void callRunners(ApplicationContext context, ApplicationArguments args) {

        List<Object> runners = new ArrayList<>();

        runners.addAll(context.getBeansOfType(ApplicationRunner.class).values());
        runners.addAll(context.getBeansOfType(CommandLineRunner.class).values());

        AnnotationAwareOrderComparator.sort(runners);

        for (Object runner : new LinkedHashSet<>(runners)) {

            if (runner instanceof ApplicationRunner)

                callRunner((ApplicationRunner) runner, args);

            if (runner instanceof CommandLineRunner)

                callRunner((CommandLineRunner) runner, args);

        }

    }

    /**
     * 调用ApplicationRunner
     *
     * @param runner runner
     * @param args   args
     */
    private void callRunner(ApplicationRunner runner, ApplicationArguments args) {

        try {

            (runner).run(args);

        } catch (Exception ex) {

            throw new IllegalStateException("Failed to execute ApplicationRunner", ex);

        }

    }

    /**
     * 调用CommandLineRunner
     *
     * @param runner runner
     * @param args   args
     */
    private void callRunner(CommandLineRunner runner, ApplicationArguments args) {

        try {

            (runner).run(args.getSourceArgs());

        } catch (Exception ex) {

            throw new IllegalStateException("Failed to execute CommandLineRunner", ex);

        }

    }

    /**
     * 处理运行错误
     *
     * @param context            context
     * @param exception          exception
     * @param exceptionReporters exceptionReporters
     * @param listeners          listeners
     */
    private void handleRunFailure(ConfigurableApplicationContext context, Throwable exception, Collection<SpringBootExceptionReporter> exceptionReporters, SpringApplicationRunListeners listeners) {

        try {

            try {

                handleExitCode(context, exception);

                if (listeners != null) {

                    listeners.failed(context, exception);

                }

            } finally {

                reportFailure(exceptionReporters, exception);

                if (context != null) {

                    context.close();

                }

            }

        } catch (Exception ex) {

            logger.warn("Unable to close ApplicationContext", ex);

        }

        ReflectionUtils.rethrowRuntimeException(exception);

    }

    /**
     * 处理退出代码错误
     *
     * @param context
     * @param exception
     */
    private void handleExitCode(ConfigurableApplicationContext context, Throwable exception) {

        int exitCode = getExitCodeFromException(context, exception);

        if (exitCode != 0) {

            if (context != null) {

                context.publishEvent(new ExitCodeEvent(context, exitCode));

            }

            SpringBootExceptionHandler handler = getSpringBootExceptionHandler();

            if (handler != null) {

                handler.registerExitCode(exitCode);

            }

        }

    }

    /**
     * 从异常中获取退出代码
     *
     * @param context   context
     * @param exception exception
     * @return {@link int}
     */
    private int getExitCodeFromException(ConfigurableApplicationContext context, Throwable exception) {

        int exitCode = getExitCodeFromMappedException(context, exception);

        if (exitCode == 0) {

            exitCode = getExitCodeFromExitCodeGeneratorException(exception);

        }

        return exitCode;

    }

    /**
     * 从映射异常中获取退出代码
     *
     * @param context   context
     * @param exception exception
     * @return {@link int}
     */
    private int getExitCodeFromMappedException(ConfigurableApplicationContext context, Throwable exception) {

        if (context == null || !context.isActive()) return 0;

        ExitCodeGenerators generators = new ExitCodeGenerators();

        Collection<ExitCodeExceptionMapper> beans = context.getBeansOfType(ExitCodeExceptionMapper.class).values();

        generators.addAll(exception, beans);

        return generators.getExitCode();

    }

    /**
     * 从退出代码生成器中获取退出代码
     *
     * @param exception exception
     * @return {@link int}
     */
    private int getExitCodeFromExitCodeGeneratorException(Throwable exception) {

        if (exception == null) return 0;

        if (exception instanceof ExitCodeGenerator) return ((ExitCodeGenerator) exception).getExitCode();

        return getExitCodeFromExitCodeGeneratorException(exception.getCause());

    }

    /**
     * 获取SpringBoot异常处理器
     *
     * @return {@link SpringBootExceptionHandler}
     */
    SpringBootExceptionHandler getSpringBootExceptionHandler() {

        if (isMainThread(Thread.currentThread())) {

            return SpringBootExceptionHandler.forCurrentThread();

        }

        return null;

    }

    /**
     * 判断是不是主线程
     *
     * @param currentThread currentThread
     * @return {@link Boolean}
     */
    private boolean isMainThread(Thread currentThread) {

        return ("main".equals(currentThread.getName()) || "restartedMain".equals(currentThread.getName())) && "main".equals(currentThread.getThreadGroup().getName());

    }

    /**
     * 报告失败
     *
     * @param exceptionReporters exceptionReporters
     * @param failure            failure
     */
    private void reportFailure(Collection<SpringBootExceptionReporter> exceptionReporters, Throwable failure) {

        try {

            for (SpringBootExceptionReporter reporter : exceptionReporters) {

                if (reporter.reportException(failure)) {

                    registerLoggedException(failure);

                    return;

                }

            }

        } catch (Throwable ex) {
            // Continue with normal handling of the original failure
        }

        if (logger.isErrorEnabled()) {

            logger.error("Application run failed", failure);

            registerLoggedException(failure);

        }

    }

}

```

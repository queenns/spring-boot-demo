# What - spring boot 应用程序入口
***
### Class 
```java
org.springframework.boot.SpringApplication
```
***
### 未解问题
如果有知道的感谢帮忙解答
Issues or liuxiaojian.mail@qq.com
- java.awt.headless 
***
```java
/**
 * 应用程序上下文类名,默认情况用于非Web环境
 */
public static final String DEFAULT_CONTEXT_CLASS =
"org.springframework.context.annotation.AnnotationConfigApplicationContext";

/**
 * 应用程序上下文类名,默认情况用于Web环境
 */
public static final String DEFAULT_SERVLET_WEB_CONTEXT_CLASS =
"org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext";

/**
 * 应用程序上下文类名,默认情况用于响应式环境
 */
public static final String DEFAULT_REACTIVE_WEB_CONTEXT_CLASS =
"org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext";

/**	
 * Default banner location.
 */
public static final String BANNER_LOCATION_PROPERTY_VALUE = 
    SpringApplicationBannerPrinter.DEFAULT_BANNER_LOCATION;

/**
 * Banner location property key.
 */
public static final String BANNER_LOCATION_PROPERTY = 
    SpringApplicationBannerPrinter.BANNER_LOCATION_PROPERTY;

/**
* 系统配置模式
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

private boolean addConversionService = true;

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

private boolean allowBeanDefinitionOverriding;

/**
* 是否自定义环境
*/
private boolean isCustomEnvironment = false;

private boolean lazyInitialization = false;




```
***
queenns
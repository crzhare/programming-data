# 第三章：高级装配

---
## 1 环境与profile

不同的环境下，可能需要创建不同的实现，比如在开发环境、QA环境、生成环境需要不同的 DataSource，如果利用构建工具配置，那么在切换环境的时候就需要重新构建，而 Spring 所提供的解决方案并不需要重新构建。

### 配置 profile bean

Spring 为环境相关的 bean 所提供了解决方案，可以在运行时根据不同的环境创建不同的 Bean，这样的结果就是同一个部署单元（可能会是WAR文件）能够适用于所有的环境，没有必要进行重新构建。Spring 3.1 引入了 bean profile 的功能。要使用 profile，首先要将所有不同的 bean 定义整理到一个或多个 profile 之中，在将应用部署到每个环境时，要确保对应的 profile 处于激活（active）的状态。

使用 JavaConfig 配置 Profile：
-  `@Profile` 注解指定某个 bean 属于哪一个 profile，比如`@Profile("dev")`注解应用在类级别上，则这个配置类中的 bean 只有在 dev profile 激活时才会创建。
- 在 Spring 3.1 中，只能在类级别上使用 `@Profile` 注解。从 Spring 3.2 开始，在方法级别上也可以使用 `@Profile` 注解，它与 `@Bean` 注解一同使用。
- 没有指定 profile 的 bean 始终都会被创建，与激活哪个 profile 没有关系。

在 XML 中配置 Profile：

- 通过根元素 `<beans>` 的 profile 属性，可以在 XML 中配置 profile bean。
- 可以在根 `<beans>` 元素中嵌套定义 `<beans>` 元素，而不是为每个环境都创建一个 profileXML 文件。这能够将所有的 profile bean 定义放到同一个 XML 文件中

### 激活 profile

Spring 在确定哪个 profile 处于激活状态时，需要依赖两个独立的属性：`spring.profiles.active` 和 `spring.profiles.default`。查找顺序为：

- 优先查找`spring.profiles.active` 配置
- 其次查找`spring.profiles.default` 配置
- 如果没有找到 profile 配置，则所有配置了 profile 的 bean 都不会被创建

有多种方式来设置这两个属性：

- 作为 DispatcherServlet 的初始化参数
- 作为 Web 应用的上下文参数
- 作为 JNDI 条目
- 作为环境变量
- 作为 JVM 的系统属性
- 在集成测试类上，使用 `@ActiveProfiles` 注解设置
- 可以同时设置多个彼此不相关的 profile

---
## 2 条件化的 bean

条件化的 bean 即在特定的条件下会被被创建的 bean，比如下面场景：

- 一个或多个 bean 只有在应用的类路径下包含特定的库时才创建
- 希望某个 bean 只有当另外某个特定的bean也声明了之后才会创建
- 只有某个特定的环境变量设置之后，才会创建某个 bean

Spring 4 引入的 `@Conditional` 注解，它可以用到带有 `@Bean` 注解的方法上。如果给定的条件计算结果为 true，就会创建这个 bean，否则的话，这个 bean 会被忽略。`@Conditional` 的默认属性接收任意实现了 Condition 接口的类，如果实现的 `matches()` 方法返回 true，那么就会创建带有 `@Conditional` 注解的 bean。

`matches()` 方法的 ConditionContext 参数：通过 ConditionContext，可以做到如下几点：

- 借助 getRegistry() 返回的 BeanDefinitionRegistry 检查 bean 定义；
- 借助 getBeanFactory() 返回的 ConfigurableListableBeanFactory 检查 bean 是否存在，甚至探查 bean 的属性；
- 借助 getEnvironment() 返回的 Environment 检查环境变量是否存在以及它的值是什么；
- 读取并探查 getResourceLoader() 返回的 ResourceLoader 所加载的资源；
- 借助 getClassLoader() 返回的 ClassLoader 加载并检查类是否存在。

`matches()` 方法的 AnnotatedTypeMetadata 参数：AnnotatedTypeMetadata 能够让我们检查带有 `@Bean` 注解的方法上还有什么其他的注解。

`@Profile` 是如何实现的：`@Profile` 本身也使用了 `@Conditional` 注解，并且引用 ProfileCondition 作为 Condition 实现。如下所示，ProfileCondition 实现了 Condition 接口，并且在做出决策的过程中，考虑到了 ConditionContext 和 AnnotatedTypeMetadata 中的多个因素。

---
## 3 处理自动装配的歧义性

当 Spring 进行 bean 的装配时，如果同时有多个满足条件的 bean，Spring 会因为无法确实使用那个 bean 而抛出 NoUniqueBeanDefinitionException，解决的方式有：

- 配置首选（primary）bean：
-  `@Primary` 能够与 `@Component` 组合用在组件扫描的 bean 上，也可以与 `@Bean` 组合用在 Java 配置的 bean 声明中
- 如果使用使用 XML 配置 bean 的话，`<bean>`元素有一个 primary 属性用来指定首选的 bean
- 使用 `@Qualifier` 注解

`@Qualifier` 注解非常强大，有多种用途：

- 与 `@Autowired` 和 `@Inject` 协同使用，在注入的时候指定想要注入进去的是哪个 bean
- 与 `@Component` 组合使用，为 bean 设置自己的限定符，注意，每个 bean 默认都有自己的限定符，即它们的 id
- 与 `@Bean` 注解一起使用，为 bean 设置自己的限定符
- 使用`@Qualifier` 创建新的限定符

---
## 4 bean的作用域

在默认情况下，Spring 应用上下文中所有 bean 都是作为以单例（singleton）的形式创建的。也就是说，不管给定的一个 bean 被注入到其他bean多少次，每次所注入的都是同一个实例。如果不希望让 bean 以单例的形式创建，也可以使用 Spring 提供的其他多种作用域，可以基于这些作用域创建 bean，这包括：

- 单例（Singleton）：在整个应用中，只创建 bean 的一个实例。
- 原型（Prototype）：每次注入或者通过 Spring 应用上下文获取的时候，都会创建一个新的 bean实 例。
- 会话（Session）：在 Web 应用中，为每个会话创建一个 bean 实例。
- 请求（Rquest）：在 Web 应用中，为每个请求创建一个 bean 实例。

如果要使用其他作用域，则需要使用 `@Scope` 注解，它可以与 `@Component` 或 `@Bean` 一起使用。`@Scope` 接收字符串类型指定具体的作用域，可以引用 ConfigurableBeanFactory 中定义的常量。

### 用会话和请求作用域

在 Web 应用中，如果能够实例化在会话和请求范围内共享的 bean，那将是非常有价值的事情。例如，在典型的电子商务应用中，可能会有一个 bean 代表用户的购物车，这个 bean 适合使用 Session 作用域。

- WebApplicationContext 中定义了 SCOPE_SESSION 等常量
-  `@Scope` 的 proxyMode 属性，用于设置注入还没有初始化的 bean 的代理，proxyMode 有两种模式，分别是 ScopedProxyMode.INTERFACES 和 ScopedProxyMode.TARGET_CLASS
- 如果注入还没有初始化的 bean 是接口类型，则使用 ScopedProxyMode.INTERFACES
- 如果注入还没有初始化的 bean 是类类型，则使用 ScopedProxyMode.TARGET_CLASS，Spring 使用 CGLib 为该 bean 创建代理
- 还没有初始化的 bean 的代理具与 bean 一样的方法，在代理的方法被调用时，它会根据情况(比如会话状态)决定调用哪个被代理的 bean

### 在XML中声明作用域代理

在 XML 中声明作用域代理，需要使用 Spring aop 命名空间的一个元素：`<aop:scoped-proxy>`

---
## 5 运行时值注入


Spring 提供了两种在运行时求值的方式：

- 属性占位符（Property placeholder）。
- Spring表达式语言（SpEL）。

### 注入外部的值

在 Spring 中，处理外部值的最简单方式就是声明属性源并通过 Spring 的 Environment 来检索属性。

- 通过 `@PropertySource` 可以引用类路径中的 properties 的文件，这个属性文件会加载到 Spring 的 Environment 中，然后可以从 Environment 中获取属性

使用占位符加载属性：

- Spring 支持使用占位符来从外部的属性的文件中加载属性，占位符的形式为使用 `${ ... }` 包装的属性名称
- 在 XML 中，可以直接使用占位符，而在 Java 代码中，可以通过 `@Value` 来声明使用占位符来获取属性
- 使用占位符，必须要配置一个 PropertyPlaceholderConfigurer bean或 PropertySourcesPlaceholderConfigurer bean
- Spring 3.1开始，推荐使用 PropertySourcesPlaceholderConfigurer，因为它能够基于 Spring Environment 及其属性源来解析占位符
- 在 Java 代码中，使用添加了 `@Bean` 注解的方法返回一个 PropertySourcesPlaceholderConfigurer 来配置
- 在 XML 配置中，则需要使用 context 命名空间中的 `<context:propertyplaceholder>`，该元素会生成 PropertySourcesPlaceholderConfigurer bean

### 用Spring表达式语言进行装配

Spring 3 引入了 **Spring 表达式语言**（Spring Expression Language，SpEL），它能够以简洁的方式将值装配到 bean 属性和构造器参数中，SpEL拥有很多特性，包括：

- 使用 bean 的 ID 来引用 bean
- 调用方法和访问对象的属性
- 对值进行算术、关系和逻辑运算
- 正则表达式匹配；集合操作

SpEL 还能够用在依赖注入以外的其他地方，比如：

- SpringSecurity支持使用SpEL表达式定义安全限制规则
- 如果在 Spring MV C应用中使用 Thymeleaf 模板作为视图，则可以使用 SpEL 表达式引用模型数据

SpEL 表达式的语法为：`#{ ... }`，下面是一些使用示例：

-  `#{ T(System).currentTimeMillis() }`：获取毫秒值
-  `#{ systemProperties['name'] }`：获取系统属性

如何使用 SpEL：

- 使用 `@Value` 注解，定义 SpEL
- 而在 XML 中，则可以直接使用 SpEL ，可以将SpEL表达式传入 `<property>` 或 `<constructor-arg>` 的 value 属性中，或者将其作为 `p-命名空间` 或 `c-命名空间` 条目的值

SpEL 常见用法：

- 表示字面值：`#{3.12}`、`#{false}`
- 引用bean、属性和方法：`#{beanId}`、`#{beanId.method()}`、`#{beanId.property}`
- SpEL 支持 `?.` 运算符，如果调用者是 null，则返回值也是 null：`#{beanId?.property}`
- 在表达式中使用类型，在SpEL中访问类作用域的方法和常量，要依赖 `T()` 这个关键的运算符，使用 `T()` 运算符的能够访问目标类型的静态方法和常量：`#{T(java.lang.Math).PI}`

#### SpEL运算符

SpEL 提供了多个运算符，这些运算符可以用在 SpEL 表达式的值上：

- 算术运算 `+、-、*、/、%、^`
- 比较运算 `<、>、==、<=、>=、lt、gt、eq、le、ge`
- 逻辑运算 `and、or、not、│`
- 条件运算 `?: (ternary)、?: (Elvis)`
- 正则表达式 `matches`
- 三元运算符
- get运算符：`[]`
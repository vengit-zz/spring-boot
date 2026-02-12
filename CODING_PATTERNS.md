# Coding Patterns in Spring Boot

A comprehensive catalog of design patterns and architectural patterns found throughout the Spring Boot codebase.

---

## Table of Contents

1. [Creational Patterns](#1-creational-patterns)
2. [Structural Patterns](#2-structural-patterns)
3. [Behavioral Patterns](#3-behavioral-patterns)
4. [Spring-Specific Architectural Patterns](#4-spring-specific-architectural-patterns)

---

## 1. Creational Patterns

### 1.1 Factory Method

Defines an interface for creating objects, letting subclasses decide which class to instantiate.

| Factory Interface | Concrete Implementations | Location |
|---|---|---|
| `ServletWebServerFactory` | `TomcatServletWebServerFactory`, `JettyServletWebServerFactory`, `UndertowServletWebServerFactory` | `spring-boot/src/main/java/org/springframework/boot/web/` |
| `ReactiveWebServerFactory` | `TomcatReactiveWebServerFactory`, `JettyReactiveWebServerFactory`, `NettyReactiveWebServerFactory` | same |
| `LoggingSystemFactory` | `DelegatingLoggingSystemFactory` | `spring-boot/.../boot/logging/` |
| `ApplicationContextFactory` | `DefaultApplicationContextFactory` | `spring-boot/.../boot/ApplicationContextFactory.java` |
| `EnvironmentPostProcessorsFactory` | `SpringFactoriesEnvironmentPostProcessorsFactory`, `ReflectionEnvironmentPostProcessorsFactory` | `spring-boot/.../boot/env/` |
| `EndpointObjectNameFactory` | `DefaultEndpointObjectNameFactory` | `spring-boot-actuator/.../endpoint/jmx/` |
| `MongoClientFactorySupport` | `MongoClientFactory`, `ReactiveMongoClientFactory` | `spring-boot-autoconfigure/.../mongo/` |

**Key example** — `ServletWebServerFactory.java:30-45`:
```java
public interface ServletWebServerFactory extends WebServerFactory {
    WebServer getWebServer(ServletContextInitializer... initializers);
}
```
Concrete factories (`TomcatServletWebServerFactory`, `JettyServletWebServerFactory`) implement this to produce vendor-specific web servers.

### 1.2 Abstract Factory

Provides an interface for creating families of related objects.

| Abstract Factory | Products | Location |
|---|---|---|
| `ClientHttpRequestFactoryBuilder` | HTTP client factories for HttpComponents, Jetty, Reactor, JDK | `spring-boot/.../http/client/` |
| `ConfigurableWebServerFactory` hierarchy | Server configuration families (SSL, HTTP/2, Compression) per vendor | `spring-boot/.../web/server/` |

**Key example** — `ClientHttpRequestFactoryBuilder.java:45-127`:
```java
public interface ClientHttpRequestFactoryBuilder<T extends ClientHttpRequestFactory> {
    static HttpComponentsClientHttpRequestFactoryBuilder httpComponents() { ... }
    static JettyClientHttpRequestFactoryBuilder jetty() { ... }
    static ReactorClientHttpRequestFactoryBuilder reactor() { ... }
    static JdkClientHttpRequestFactoryBuilder jdk() { ... }
}
```
Each builder creates a complete family of HTTP client objects with consistent configuration APIs.

### 1.3 Builder

Separates construction of complex objects from their representation via fluent APIs.

| Builder | What It Builds | Location |
|---|---|---|
| `SpringApplicationBuilder` | `SpringApplication` with context hierarchy | `spring-boot/.../builder/SpringApplicationBuilder.java` |
| `DataSourceBuilder` | `DataSource` with auto-detection of pool impl | `spring-boot/.../jdbc/DataSourceBuilder.java` |
| `RestTemplateBuilder` | `RestTemplate` with interceptors, auth, converters | `spring-boot/.../web/client/RestTemplateBuilder.java` |
| `ConnectionFactoryBuilder` | R2DBC `ConnectionFactory` | `spring-boot/.../r2dbc/ConnectionFactoryBuilder.java` |
| `ThreadPoolTaskExecutorBuilder` | `ThreadPoolTaskExecutor` | `spring-boot/.../task/ThreadPoolTaskExecutorBuilder.java` |
| `ThreadPoolTaskSchedulerBuilder` | `ThreadPoolTaskScheduler` | `spring-boot/.../task/ThreadPoolTaskSchedulerBuilder.java` |
| `WebServiceTemplateBuilder` | `WebServiceTemplate` | `spring-boot/.../webservices/client/WebServiceTemplateBuilder.java` |
| `Health.Builder` | `Health` status objects | `spring-boot-actuator/.../health/Health.java` |

**Key example** — `SpringApplicationBuilder.java:75+`:
```java
new SpringApplicationBuilder(Application.class)
    .profiles("production")
    .properties("server.port=8080")
    .parent(parentContext)
    .run(args);
```
Many builders use immutable copy-on-write semantics where each method returns a new instance (e.g., `RestTemplateBuilder`).

### 1.4 Singleton

Ensures a class has only one instance.

| Class | Technique | Location |
|---|---|---|
| `ApplicationConversionService` | Double-checked locking with `volatile` | `spring-boot/.../convert/ApplicationConversionService.java:74,200-212` |
| `BindConverter` | Lazy static field | `spring-boot/.../bind/BindConverter.java:57,132-137` |
| `PropertyMapper` | Static final `INSTANCE` field | `spring-boot/.../properties/PropertyMapper.java:63` |
| `JavaBeanBinder` | Static final `INSTANCE` | `spring-boot/.../bind/JavaBeanBinder.java:57` |
| `DefaultPropertyMapper` | Public static final `INSTANCE` | `spring-boot/.../source/DefaultPropertyMapper.java:36` |
| `SystemEnvironmentPropertyMapper` | Public static final `INSTANCE` | `spring-boot/.../source/SystemEnvironmentPropertyMapper.java:40` |

**Key example** — `ApplicationConversionService.java:200-212` (double-checked locking):
```java
public static ConversionService getSharedInstance() {
    ApplicationConversionService sharedInstance = ApplicationConversionService.sharedInstance;
    if (sharedInstance == null) {
        synchronized (ApplicationConversionService.class) {
            sharedInstance = ApplicationConversionService.sharedInstance;
            if (sharedInstance == null) {
                sharedInstance = new ApplicationConversionService(null, true);
                ApplicationConversionService.sharedInstance = sharedInstance;
            }
        }
    }
    return sharedInstance;
}
```

### 1.5 Prototype (Immutable Copy)

Creates new objects by copying existing instances with modifications.

| Class | Technique | Location |
|---|---|---|
| `RestTemplateBuilder` | Each fluent method returns a new builder copy | `spring-boot/.../web/client/RestTemplateBuilder.java` |
| `PropertyMapper` | `alwaysApplying()` returns new mapper | `spring-boot/.../properties/PropertyMapper.java:79-96` |
| `ClientHttpRequestFactoryBuilder` | `withCustomizers()` wraps and returns new builder | `spring-boot/.../http/client/ClientHttpRequestFactoryBuilder.java:70-88` |

---

## 2. Structural Patterns

### 2.1 Decorator

Wraps objects of the same interface to add behavior dynamically.

| Decorator | Wraps | Purpose | Location |
|---|---|---|---|
| `LoaderHidingResource` | Jetty `Resource` | Filters out Spring Boot loader classes from being served | `spring-boot/.../embedded/jetty/LoaderHidingResource.java` |
| `JarFileWrapper` | `JarFile` | Allows safe close without affecting original | `spring-boot-loader-classic/.../jar/JarFileWrapper.java` |
| `ErrorWrapperResponse` (inner class) | `HttpServletResponse` | Intercepts error status for error page handling | `spring-boot/.../servlet/support/ErrorPageFilter.java` |

### 2.2 Adapter

Converts one interface to another that clients expect.

| Adapter | From | To | Location |
|---|---|---|---|
| `ValidatorAdapter` | `jakarta.validation.Validator` | Spring `SmartValidator` | `spring-boot-autoconfigure/.../validation/ValidatorAdapter.java` |
| `HealthIndicatorReactiveAdapter` | `HealthIndicator` (blocking) | `ReactiveHealthIndicator` | `spring-boot-actuator/.../health/HealthIndicatorReactiveAdapter.java` |
| `CompositeHealthContributorReactiveAdapter` | `CompositeHealthContributor` | `CompositeReactiveHealthContributor` | `spring-boot-actuator/.../health/CompositeHealthContributorReactiveAdapter.java` |
| `StatsdPropertiesConfigAdapter` | `StatsdProperties` | `StatsdConfig` (Micrometer) | `spring-boot-actuator-autoconfigure/.../metrics/export/statsd/StatsdPropertiesConfigAdapter.java` |

The properties-to-config adapter pattern is used extensively across all Micrometer metrics exporters (Prometheus, Datadog, Graphite, etc.).

### 2.3 Proxy

Provides a surrogate to control access to another object.

| Proxy | Controls Access To | Location |
|---|---|---|
| `DelegatingFilterProxyRegistrationBean` | Servlet filters (delays initialization) | `spring-boot/.../servlet/DelegatingFilterProxyRegistrationBean.java` |
| `ServerHeaderHandler` (Handler.Wrapper) | Jetty handlers (adds server header) | `spring-boot/.../embedded/jetty/JettyHandlerWrappers.java:56-75` |

### 2.4 Composite

Composes objects into tree structures; clients treat individual objects and compositions uniformly.

| Composite | Element Type | Location |
|---|---|---|
| `CompositeHealth` | `HealthComponent` | `spring-boot-actuator/.../health/CompositeHealth.java` |
| `CompositeHandlerMapping` | `HandlerMapping` | `spring-boot-actuator-autoconfigure/.../web/servlet/CompositeHandlerMapping.java` |
| `CompositeHandlerAdapter` | `HandlerAdapter` | `spring-boot-actuator-autoconfigure/.../web/servlet/CompositeHandlerAdapter.java` |
| `CompositeHandlerExceptionResolver` | `HandlerExceptionResolver` | `spring-boot-actuator-autoconfigure/.../web/servlet/CompositeHandlerExceptionResolver.java` |
| `CompositeDataSourcePoolMetadataProvider` | `DataSourcePoolMetadataProvider` | `spring-boot/.../jdbc/metadata/CompositeDataSourcePoolMetadataProvider.java` |
| `CompositeResourceManager` | `ResourceManager` (Undertow) | `spring-boot/.../embedded/undertow/CompositeResourceManager.java` |

### 2.5 Facade

Provides a simplified interface to a complex subsystem.

| Facade | Subsystem | Location |
|---|---|---|
| `ApplicationConversionService` | Type conversion, formatting | `spring-boot/.../convert/ApplicationConversionService.java` |
| `UndertowWebServerFactoryDelegate` | Undertow builder configuration | `spring-boot/.../embedded/undertow/UndertowWebServerFactoryDelegate.java` |
| `SpringApplication` | Application context, environment, listeners, initializers | `spring-boot/.../boot/SpringApplication.java` |

### 2.6 Bridge

Decouples abstraction from implementation so they vary independently.

| Abstraction | Implementations | Location |
|---|---|---|
| `CompositePropagationFactory` | B3, B3_MULTI, W3C propagation factories | `spring-boot-actuator-autoconfigure/.../tracing/CompositePropagationFactory.java` |
| `CompositeTextMapPropagator` | Separate injector/extractor implementations | `spring-boot-actuator-autoconfigure/.../tracing/CompositeTextMapPropagator.java` |
| `NamedContributorsMapAdapter` | Function-based value adaptation between contributor types | `spring-boot-actuator/.../health/NamedContributorsMapAdapter.java` |

---

## 3. Behavioral Patterns

### 3.1 Strategy

Defines a family of interchangeable algorithms.

| Strategy Interface | Implementations | Location |
|---|---|---|
| `FlywayMigrationStrategy` | User-provided bean overrides default migration | `spring-boot-autoconfigure/.../flyway/FlywayMigrationStrategy.java` |
| `ClassPathRestartStrategy` | `PatternClassPathRestartStrategy` | `spring-boot-devtools/.../classpath/ClassPathRestartStrategy.java` |
| `StatusAggregator` | `SimpleStatusAggregator` | `spring-boot-actuator/.../health/StatusAggregator.java` |
| `HttpMessageConvertersProvider` | Various message converter strategies | `spring-boot-autoconfigure/.../http/` |

### 3.2 Observer (Event/Listener)

Event-driven communication where listeners respond to published events.

| Component | Role | Location |
|---|---|---|
| `EventPublishingRunListener` | Publisher — broadcasts lifecycle events | `spring-boot/.../context/event/EventPublishingRunListener.java:54-154` |
| `LoggingApplicationListener` | Subscriber — initializes logging system | `spring-boot/.../context/logging/LoggingApplicationListener.java` |
| `ClearCachesApplicationListener` | Subscriber — clears caches on refresh | `spring-boot/.../ClearCachesApplicationListener.java` |
| `ParentContextCloserApplicationListener` | Subscriber — closes child on parent close | `spring-boot/.../builder/ParentContextCloserApplicationListener.java` |
| `AbstractAuditListener` | Subscriber — processes audit events | `spring-boot-actuator/.../audit/listener/AbstractAuditListener.java` |

**Application lifecycle events** (fired in order):
1. `ApplicationStartingEvent`
2. `ApplicationEnvironmentPreparedEvent`
3. `ApplicationContextInitializedEvent`
4. `ApplicationPreparedEvent`
5. `ApplicationStartedEvent`
6. `AvailabilityChangeEvent` (LivenessState.CORRECT)
7. `ApplicationReadyEvent`
8. `AvailabilityChangeEvent` (ReadinessState.ACCEPTING_TRAFFIC)

### 3.3 Template Method

Abstract classes define algorithm skeletons; subclasses fill in the steps.

| Abstract Class | Template Method | Abstract Steps | Location |
|---|---|---|---|
| `AbstractLoggingSystem` | `initialize()` | `loadConfiguration()`, `loadDefaults()` | `spring-boot/.../logging/AbstractLoggingSystem.java:40-84` |
| `AbstractHealthIndicator` | `health()` (final) | `doHealthCheck(Health.Builder)` | `spring-boot-actuator/.../health/AbstractHealthIndicator.java:39-94` |
| `AbstractScriptDatabaseInitializer` | `initializeDatabase()` | `isEmbeddedDatabase()`, `runScripts()` | `spring-boot/.../sql/init/AbstractScriptDatabaseInitializer.java` |
| `ServletComponentHandler` | `handle()` | `doHandle()` | `spring-boot/.../servlet/ServletComponentHandler.java:36-82` |
| `AbstractFailureAnalyzer<T>` | `analyze(Throwable)` | `analyze(Throwable, T cause)` | `spring-boot/.../diagnostics/analyzer/AbstractFailureAnalyzer.java` |

### 3.4 Chain of Responsibility

Requests pass through a chain of handlers; each can handle or delegate.

| Chain | Handler Type | Location |
|---|---|---|
| Servlet Filter Chain | `Filter` implementations (`ErrorPageFilter`, `ApplicationContextHeaderFilter`) | `spring-boot/.../servlet/` |
| `CentralDirectoryParser` | `CentralDirectoryVisitor` list | `spring-boot-loader-classic/.../jar/CentralDirectoryParser.java:32-99` |
| `DelegatingFilterProxyRegistrationBean` | Dynamic filter chain registration | `spring-boot/.../servlet/DelegatingFilterProxyRegistrationBean.java` |

### 3.5 Command

Encapsulates operations as objects.

| Command Class | Operations | Location |
|---|---|---|
| `DockerCliCommand` (sealed hierarchy) | `Context`, `Inspect`, `ComposeUp`, `ComposeDown`, `ComposePs`, `ComposeConfig`, `ComposeStart`, `ComposeStop` | `spring-boot-docker-compose/.../core/DockerCliCommand.java:37-298` |
| Gradle `PluginApplicationAction` | `JavaPluginAction`, `KotlinPluginAction`, `WarPluginAction`, `NativeImagePluginAction` | `spring-boot-gradle-plugin/.../plugin/` |

### 3.6 Iterator

Custom traversal of aggregate structures.

| Iterator | Collection | Location |
|---|---|---|
| `InfoProperties.PropertiesIterator` | Key-value property entries | `spring-boot/.../info/InfoProperties.java:95-112` |
| `SpringConfigurationPropertySources.SourcesIterator` | Configuration property sources (with caching) | `spring-boot/.../source/SpringConfigurationPropertySources.java` |
| `ConfigurationPropertyNamesIterator` | Filtered property names with ancestor checking | `spring-boot/.../source/SpringIterableConfigurationPropertySource.java` |

### 3.7 Visitor

Separates algorithms from object structure they operate on.

| Visitor Interface | Element Structure | Location |
|---|---|---|
| `CentralDirectoryVisitor` | JAR central directory entries (`visitStart`, `visitFileHeader`, `visitEnd`) | `spring-boot-loader-classic/.../jar/CentralDirectoryVisitor.java` |
| `TreeVisitor` | Java AST nodes for configuration value extraction | `spring-boot-configuration-processor/.../fieldvalues/javac/TreeVisitor.java` |

### 3.8 State

Object behavior changes based on internal state.

| State Machine | States | Location |
|---|---|---|
| Application availability | `LivenessState.CORRECT / BROKEN`, `ReadinessState.ACCEPTING_TRAFFIC / REFUSING_TRAFFIC` | `spring-boot/.../availability/` |
| `AvailabilityChangeEvent` | State transitions published as events | `spring-boot/.../availability/AvailabilityChangeEvent.java` |
| `ErrorPageFilter` (inner `ErrorWrapperResponse`) | `hasErrorToSend`, `errorSent` — behavior changes on error state | `spring-boot/.../servlet/support/ErrorPageFilter.java` |

---

## 4. Spring-Specific Architectural Patterns

### 4.1 Auto-Configuration

Automatic bean registration driven by classpath detection and conditional annotations.

**Key example** — `DataSourceAutoConfiguration.java:59-64`:
```java
@AutoConfiguration(before = SqlInitializationAutoConfiguration.class)
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnMissingBean(type = "io.r2dbc.spi.ConnectionFactory")
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({ DataSourcePoolMetadataProvidersConfiguration.class,
          DataSourceCheckpointRestoreConfiguration.class })
public class DataSourceAutoConfiguration { ... }
```

There are **100+** auto-configuration classes in `spring-boot-autoconfigure`.

### 4.2 Conditional Bean Registration

Fine-grained control over bean creation using the `@Conditional*` annotation family.

| Annotation | Condition | Location |
|---|---|---|
| `@ConditionalOnClass` | Class present on classpath | `spring-boot-autoconfigure/.../condition/ConditionalOnClass.java` |
| `@ConditionalOnMissingBean` | No existing bean of type | `spring-boot-autoconfigure/.../condition/ConditionalOnMissingBean.java` |
| `@ConditionalOnProperty` | Property has specific value | `spring-boot-autoconfigure/.../condition/ConditionalOnProperty.java` |
| `@ConditionalOnBean` | Bean of type exists | `spring-boot-autoconfigure/.../condition/ConditionalOnBean.java` |
| `@ConditionalOnWebApplication` | Web application context | `spring-boot-autoconfigure/.../condition/ConditionalOnWebApplication.java` |
| `@ConditionalOnJava` | Java version check | `spring-boot-autoconfigure/.../condition/ConditionalOnJava.java` |
| `@ConditionalOnResource` | Resource exists | `spring-boot-autoconfigure/.../condition/ConditionalOnResource.java` |
| `@ConditionalOnExpression` | SpEL expression evaluates to true | `spring-boot-autoconfigure/.../condition/ConditionalOnExpression.java` |
| `@ConditionalOnThreading` | Threading model check | `spring-boot-autoconfigure/.../condition/ConditionalOnThreading.java` |

### 4.3 Configuration Properties Binding

Typed binding of external configuration (`application.properties` / `application.yml`) to Java beans.

| Properties Class | Prefix | Location |
|---|---|---|
| `DataSourceProperties` | `spring.datasource` | `spring-boot-autoconfigure/.../jdbc/DataSourceProperties.java` |
| `ServerProperties` | `server` | `spring-boot-autoconfigure/.../web/ServerProperties.java` |
| `JpaProperties` | `spring.jpa` | `spring-boot-autoconfigure/.../orm/jpa/JpaProperties.java` |

Bound via `ConfigurationPropertiesBindingPostProcessor` at `spring-boot/.../properties/ConfigurationPropertiesBindingPostProcessor.java`.

### 4.4 Starter Aggregation

Starter modules are dependency-only artifacts (no code) that bring in a curated set of transitive dependencies. There are **58+ starters** covering web, data, messaging, security, and more.

Structure: `spring-boot-starters/spring-boot-starter-*/build.gradle` — each contains only dependency declarations.

### 4.5 Health Indicator

Pluggable health checks that feed into the `/actuator/health` endpoint.

| Base Class | Key Method | Location |
|---|---|---|
| `AbstractHealthIndicator` | `doHealthCheck(Health.Builder)` (template method) | `spring-boot-actuator/.../health/AbstractHealthIndicator.java` |
| `PingHealthIndicator` | Always returns `UP` | `spring-boot-actuator/.../health/PingHealthIndicator.java` |
| `ReactiveHealthIndicator` | `health()` returns `Mono<Health>` | `spring-boot-actuator/.../health/ReactiveHealthIndicator.java` |

### 4.6 Failure Analyzer

Translates raw startup exceptions into human-readable diagnostics.

| Analyzer | Exception Type | Location |
|---|---|---|
| `BeanDefinitionOverrideFailureAnalyzer` | `BeanDefinitionOverrideException` | `spring-boot/.../diagnostics/analyzer/` |
| `MissingParameterNamesFailureAnalyzer` | Missing parameter names | same |
| `BindFailureAnalyzer` | `BindException` | same |
| `InvalidConfigurationPropertyValueFailureAnalyzer` | Invalid property values | same |
| `NoSuchMethodFailureAnalyzer` | `NoSuchMethodError` | same |

All extend `AbstractFailureAnalyzer<T>` which uses the Template Method pattern.

### 4.7 Environment Post-Processor

Modifies the `ConfigurableEnvironment` before the application context is created.

| Post-Processor | Purpose | Location |
|---|---|---|
| `RandomValuePropertySourceEnvironmentPostProcessor` | Adds `random.*` property source | `spring-boot/.../env/RandomValuePropertySourceEnvironmentPostProcessor.java` |
| `SpringApplicationJsonEnvironmentPostProcessor` | Parses `SPRING_APPLICATION_JSON` | `spring-boot/.../env/SpringApplicationJsonEnvironmentPostProcessor.java` |
| `ConfigDataEnvironmentPostProcessor` | Loads `application.properties/yml` | `spring-boot/.../config/ConfigDataEnvironmentPostProcessor.java` |

### 4.8 Customizer Callback

`*Customizer` functional interfaces provide extension points for auto-configured beans.

| Customizer Interface | Target | Location |
|---|---|---|
| `WebServerFactoryCustomizer<T>` | Web server factories | `spring-boot/.../web/server/WebServerFactoryCustomizer.java` |
| `TomcatWebServerFactoryCustomizer` | Tomcat configuration | `spring-boot-autoconfigure/.../web/embedded/TomcatWebServerFactoryCustomizer.java` |
| `JettyWebServerFactoryCustomizer` | Jetty configuration | same directory |
| `UndertowWebServerFactoryCustomizer` | Undertow configuration | same directory |
| `NettyWebServerFactoryCustomizer` | Netty configuration | same directory |

### 4.9 Lifecycle Callback (SpringApplicationRunListener)

Notifies listeners of application startup phases.

**Interface** — `SpringApplicationRunListener.java:39-100`:
```
starting() → environmentPrepared() → contextPrepared() → contextLoaded() → started() → ready()
```

**Implementation** — `EventPublishingRunListener` translates each callback into an `ApplicationEvent`.

### 4.10 ImportSelector / DeferredImportSelector

Programmatically selects configuration classes to import.

| Selector | Purpose | Location |
|---|---|---|
| `AutoConfigurationImportSelector` | Loads auto-configuration candidates from `META-INF/spring/` | `spring-boot-autoconfigure/.../AutoConfigurationImportSelector.java` |
| `ImportAutoConfigurationImportSelector` | Test-oriented auto-config selection | `spring-boot-autoconfigure/.../ImportAutoConfigurationImportSelector.java` |
| `ManagementContextConfigurationImportSelector` | Selects management-context configs | `spring-boot-actuator-autoconfigure/.../web/server/ManagementContextConfigurationImportSelector.java` |

### 4.11 BeanPostProcessor

Intercepts bean creation to customize or enhance beans.

| Post-Processor | Targets | Location |
|---|---|---|
| `ConfigurationPropertiesBindingPostProcessor` | `@ConfigurationProperties` beans | `spring-boot/.../properties/ConfigurationPropertiesBindingPostProcessor.java` |
| `WebServerFactoryCustomizerBeanPostProcessor` | `WebServerFactory` beans | `spring-boot/.../web/server/WebServerFactoryCustomizerBeanPostProcessor.java` |
| `ErrorPageRegistrarBeanPostProcessor` | Error page registrars | `spring-boot/.../web/server/ErrorPageRegistrarBeanPostProcessor.java` |

### 4.12 ApplicationRunner / CommandLineRunner

Execute custom code after the application context is fully started.

| Interface | Argument Type | Location |
|---|---|---|
| `CommandLineRunner` | `String... args` (raw) | `spring-boot/.../boot/CommandLineRunner.java` |
| `ApplicationRunner` | `ApplicationArguments` (parsed) | `spring-boot/.../boot/ApplicationRunner.java` |

**Example implementation**: `JobLauncherApplicationRunner` in `spring-boot-autoconfigure/.../batch/` launches Spring Batch jobs on startup.

---

## Summary

| Category | Patterns Found | Count of Implementations |
|---|---|---|
| **Creational** | Factory Method, Abstract Factory, Builder, Singleton, Prototype | 50+ |
| **Structural** | Decorator, Adapter, Proxy, Composite, Facade, Bridge | 30+ |
| **Behavioral** | Strategy, Observer, Template Method, Chain of Responsibility, Command, Iterator, Visitor, State | 40+ |
| **Spring-Specific** | Auto-Configuration, Conditional Beans, Config Properties, Health Indicator, Failure Analyzer, Env Post-Processor, Customizer, Lifecycle Callback, ImportSelector, BeanPostProcessor, Runner | 200+ |

The Spring Boot codebase demonstrates a rich and disciplined application of both classic GoF design patterns and framework-specific architectural patterns. The most pervasive patterns are:

- **Factory Method** — used across web servers, logging, HTTP clients, and connection handling
- **Builder** — used for complex object construction with immutable copy-on-write semantics
- **Template Method** — used in health indicators, logging systems, failure analyzers, and database initializers
- **Observer** — the event-listener system is central to the entire application lifecycle
- **Auto-Configuration + Conditional Beans** — the defining architectural innovation of Spring Boot, enabling convention-over-configuration with 100+ auto-configuration classes
- **Customizer Callback** — the primary extension mechanism allowing users to customize auto-configured beans without replacing them

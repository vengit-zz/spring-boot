# Spring Boot Architecture Diagram

## 1. High-Level Module Dependency Graph

```
                         +------------------------------------------+
                         |             User Application              |
                         +------------------------------------------+
                              |          |          |          |
                    uses      |   uses   |   uses   |   uses   |
                              v          v          v          v
                    +------------------+ +--------+ +--------+ +------------------+
                    | spring-boot-     | | spring | | spring | | spring-boot-     |
                    | starter-*  (58)  | | -boot- | | -boot- | | docker-compose   |
                    | (dependency-only)| | test   | | dev-   | +------------------+
                    +------------------+ | -auto- | | tools  |        |
                              |          | config | +--------+        |
                              v          +--------+    |              |
                    +------------------+    |          v              |
                    | spring-boot-     |    |  +-------------+       |
                    | actuator-        |    |  | spring-boot |       |
                    | autoconfigure    |    |  | -test       |       |
                    +------------------+    |  +-------------+       |
                      |       |       |     |        |               |
                      v       v       v     v        v               v
              +-----------+ +-------------------+ +---------------------+
              | spring-   | | spring-boot-      | |    spring-boot      |
              | boot-     | | autoconfigure     | |    (core)           |
              | actuator  | +-------------------+ +---------------------+
              +-----------+         |                      |
                    |               v                      v
                    +--------> spring-boot (core) <--------+
                                    |
                                    v
                          +-------------------+
                          | Spring Framework  |
                          | (spring-context,  |
                          |  spring-web, etc) |
                          +-------------------+
```

## 2. Build Tools Module Graph

```
                    +-------------------+     +--------------------+
                    | spring-boot-      |     | spring-boot-       |
                    | gradle-plugin     |     | maven-plugin       |
                    +-------------------+     +--------------------+
                         |          |
                         v          v
          +--------------------+  +----------------------+
          | spring-boot-       |  | spring-boot-         |
          | buildpack-platform |  | loader-tools         |
          +--------------------+  +----------------------+
                                     |        |        |
                                     v        v        v
                              +--------+ +--------+ +---------+
                              | loader | | loader | | jarmode |
                              |        | | classic| | -tools  |
                              +--------+ +--------+ +---------+

          +---------------------------+     +---------------------------+
          | spring-boot-              |     | spring-boot-              |
          | configuration-processor   |     | autoconfigure-processor   |
          | (annotation processor)    |     | (annotation processor)    |
          +---------------------------+     +---------------------------+
```

## 3. Core Module Internal Architecture

```
spring-boot (core)
|
+-- SpringApplication                   <-- Main entry point & bootstrap
|   +-- SpringApplicationRunListener    <-- Lifecycle callback contract
|   +-- ApplicationContextFactory       <-- Creates the right ApplicationContext
|   +-- ExitCodeGenerator              <-- Application exit codes
|
+-- context/
|   +-- config/                         <-- Configuration data loading (48 classes)
|   |   +-- ConfigDataEnvironment            Orchestrates config loading
|   |   +-- ConfigDataEnvironmentPostProcessor
|   |   +-- ConfigDataLocation               Represents config file locations
|   |   +-- ConfigDataLocationResolver       Resolves locations to resources
|   |   +-- ConfigDataLoader                 Loads data from resources
|   |   +-- StandardConfigDataLocationResolver  (file:, classpath:, optional:)
|   |   +-- StandardConfigDataLoader            (YAML, .properties)
|   |   +-- Profiles                            Profile management
|   |
|   +-- event/                          <-- Application lifecycle events
|   |   +-- EventPublishingRunListener
|   |   +-- ApplicationStartingEvent
|   |   +-- ApplicationEnvironmentPreparedEvent
|   |   +-- ApplicationContextInitializedEvent
|   |   +-- ApplicationPreparedEvent
|   |   +-- ApplicationStartedEvent
|   |   +-- ApplicationReadyEvent
|   |   +-- ApplicationFailedEvent
|   |
|   +-- properties/                     <-- @ConfigurationProperties binding
|       +-- ConfigurationPropertiesBindingPostProcessor
|       +-- bind/Binder                      Type-safe property binding
|       +-- source/ConfigurationPropertySource
|
+-- web/
|   +-- server/                         <-- Web server abstraction (22 classes)
|   |   +-- WebServer                        Interface: start/stop/getPort
|   |   +-- WebServerFactory                 Factory interface
|   |   +-- ConfigurableWebServerFactory     Configuration (SSL, HTTP/2, etc)
|   |   +-- WebServerFactoryCustomizer       Customization callback
|   |   +-- Ssl, Http2, Compression, Cookie  Configuration models
|   |   +-- GracefulShutdownCallback         Graceful shutdown support
|   |
|   +-- embedded/                       <-- 4 embedded server implementations
|   |   +-- tomcat/    (19 classes)     Tomcat servlet + reactive factories
|   |   +-- jetty/     (15 classes)     Jetty servlet + reactive factories
|   |   +-- undertow/  (17 classes)     Undertow servlet + reactive factories
|   |   +-- netty/     (8 classes)      Netty reactive factory only
|   |
|   +-- servlet/                        <-- Servlet integration
|   |   +-- ServletRegistrationBean
|   |   +-- FilterRegistrationBean
|   |   +-- ServletComponentHandler
|   |
|   +-- client/                         <-- HTTP client builders
|       +-- RestTemplateBuilder
|       +-- ClientHttpRequestFactoryBuilder
|
+-- env/                                <-- Environment post-processors
|   +-- EnvironmentPostProcessor             Modify env before context creation
|   +-- RandomValuePropertySource            random.* properties
|   +-- SpringApplicationJsonEnvironmentPostProcessor
|   +-- SystemEnvironmentPropertySourceEnvironmentPostProcessor
|
+-- logging/                            <-- Logging system abstraction
|   +-- LoggingSystem                        Abstract base
|   +-- logback/LogbackLoggingSystem         Logback impl
|   +-- log4j2/Log4J2LoggingSystem           Log4J2 impl
|   +-- java/JavaLoggingSystem               JUL impl
|
+-- availability/                       <-- Application availability state
|   +-- LivenessState                        CORRECT | BROKEN
|   +-- ReadinessState                       ACCEPTING_TRAFFIC | REFUSING_TRAFFIC
|   +-- AvailabilityChangeEvent              State change notification
|
+-- diagnostics/                        <-- Failure analysis
|   +-- FailureAnalyzer                      Interface
|   +-- AbstractFailureAnalyzer              Template method base
|   +-- FailureAnalysis                      Human-readable diagnosis
|
+-- ssl/                                <-- SSL/TLS support
|   +-- SslBundle, SslBundles
|   +-- SslOptions, SslStoreBundle
|
+-- builder/                            <-- Fluent API for app building
|   +-- SpringApplicationBuilder             Context hierarchies
|
+-- convert/                            <-- Type conversion
|   +-- ApplicationConversionService         Singleton converter registry
|
+-- http/                               <-- HTTP client abstraction
|   +-- client/                              HTTP request factory builders
|       +-- ClientHttpRequestFactoryBuilder
|       +-- HttpComponentsClientHttpRequestFactoryBuilder
|       +-- JettyClientHttpRequestFactoryBuilder
|       +-- ReactorClientHttpRequestFactoryBuilder
|       +-- JdkClientHttpRequestFactoryBuilder
|
+-- jdbc/                               <-- DataSource support
+-- jms/                                <-- JMS support
+-- jpa/                                <-- JPA/Hibernate support
+-- r2dbc/                              <-- Reactive DB support
+-- task/                               <-- TaskExecutor/Scheduler builders
+-- info/                               <-- Build/Git/Java info
+-- io/                                 <-- Resource loading
+-- jackson/                            <-- Jackson JSON integration
+-- sql/                                <-- SQL init scripts
+-- validation/                         <-- Validation support
```

## 4. Auto-Configuration Architecture

```
spring-boot-autoconfigure
|
+-- AutoConfigurationImportSelector     <-- DeferredImportSelector
|   Discovers @AutoConfiguration classes via SpringFactoriesLoader
|   Applies exclusions, filtering, and ordering
|
+-- condition/                          <-- @Conditional* annotations
|   +-- @ConditionalOnClass                  "only if class on classpath"
|   +-- @ConditionalOnMissingBean            "only if no existing bean"
|   +-- @ConditionalOnProperty               "only if property set"
|   +-- @ConditionalOnBean                   "only if bean exists"
|   +-- @ConditionalOnWebApplication         "only if web app"
|   +-- @ConditionalOnJava                   "only if Java version"
|   +-- @ConditionalOnResource               "only if resource exists"
|   +-- @ConditionalOnExpression             "only if SpEL expression"
|   +-- @ConditionalOnThreading              "only if threading model"
|
+-- 57 technology-specific packages:
|
|   Web:           web/, jersey/, graphql/, hateoas/, websocket/
|   Data:          jdbc/, data/, jooq/, mongo/, cassandra/, couchbase/,
|                  elasticsearch/, neo4j/, ldap/, r2dbc/
|   Messaging:     amqp/, jms/, kafka/, pulsar/
|   Security:      security/
|   Cache:         cache/
|   Batch:         batch/
|   Serialization: jackson/, gson/, jsonb/
|   Template:      freemarker/, thymeleaf/, mustache/, groovy/
|   Logging:       logging/
|   Ops:           info/, availability/, context/, task/
|   Migration:     flyway/, liquibase/
|   Session:       session/
|   Mail:          mail/, sendgrid/
|   Misc:          aop/, validation/, http/, codec/, ssl/, thread/
```

## 5. Actuator Architecture

```
spring-boot-actuator
|
+-- endpoint/                           <-- Endpoint infrastructure
|   +-- annotation/
|   |   +-- @Endpoint(id=...)                Declares an endpoint
|   |   +-- @ReadOperation                   GET - query data
|   |   +-- @WriteOperation                  POST - modify state
|   |   +-- @DeleteOperation                 DELETE - remove
|   |
|   +-- web/                                 HTTP endpoint exposure
|   |   +-- WebEndpointResponse
|   |   +-- servlet/WebMvcEndpointHandlerMapping
|   |   +-- reactive/WebFluxEndpointHandlerMapping
|   |
|   +-- jmx/                                JMX endpoint exposure
|       +-- EndpointMBean
|       +-- JmxEndpointExporter
|
+-- health/                             <-- Health check system
|   +-- Health                               Status + details
|   +-- HealthIndicator                      Blocking health check
|   +-- ReactiveHealthIndicator              Reactive health check
|   +-- AbstractHealthIndicator              Template method base
|   +-- CompositeHealth                      Aggregated health
|   +-- StatusAggregator                     Combine statuses
|   Built-in: Disk, DB, Redis, Mongo, ES, Cassandra, LDAP, Neo4j, Mail...
|
+-- metrics/                            <-- Micrometer metrics
|   +-- MetricsEndpoint                      /actuator/metrics
|   +-- Various metric binders
|
+-- info/                               <-- Application info
+-- env/                                <-- Environment properties
+-- beans/                              <-- Bean listing
+-- logging/                            <-- Log level management
+-- audit/                              <-- Audit event tracking
+-- web/                                <-- Request tracing/mappings
+-- scheduling/                         <-- Scheduled task info
+-- startup/                            <-- Startup step timing
+-- sbom/                               <-- Software bill of materials

spring-boot-actuator-autoconfigure
|
+-- Auto-configures all actuator endpoints
+-- endpoint/web/                       <-- WebMVC/WebFlux endpoint config
+-- health/                             <-- Health indicator auto-config
+-- metrics/export/                     <-- Metrics exporters
|   +-- prometheus/, datadog/, graphite/, statsd/, influx/, etc.
+-- tracing/                            <-- Distributed tracing
|   +-- OpenTelemetry, Brave, Zipkin, Wavefront
+-- security/                           <-- Actuator security config
```

## 6. Application Startup Sequence

```
main(String[] args)
       |
       v
SpringApplication.run(primarySource, args)
       |
       v
+------------------------------------------------------+
|  1. STARTING                                         |
|     - Create BootstrapContext                         |
|     - Load SpringApplicationRunListeners (SPI)       |
|     - Fire ApplicationStartingEvent                  |
+------------------------------------------------------+
       |
       v
+------------------------------------------------------+
|  2. ENVIRONMENT PREPARED                             |
|     - Create ConfigurableEnvironment                 |
|     - Run EnvironmentPostProcessors:                 |
|       * ConfigDataEnvironmentPostProcessor           |
|         -> Load application.properties / .yml        |
|         -> Resolve profiles                          |
|       * RandomValuePropertySourceEnvironmentPostProcessor |
|       * SpringApplicationJsonEnvironmentPostProcessor |
|     - Fire ApplicationEnvironmentPreparedEvent       |
+------------------------------------------------------+
       |
       v
+------------------------------------------------------+
|  3. CONTEXT CREATED                                  |
|     - Create ApplicationContext:                     |
|       * AnnotationConfigServletWebServerApplicationContext  (servlet) |
|       * AnnotationConfigReactiveWebServerApplicationContext (reactive)|
|       * AnnotationConfigApplicationContext                  (none)    |
|     - Apply ApplicationContextInitializers           |
|     - Fire ApplicationContextInitializedEvent        |
+------------------------------------------------------+
       |
       v
+------------------------------------------------------+
|  4. CONTEXT LOADED                                   |
|     - Register primary sources as bean definitions   |
|     - Process @Import, @ComponentScan                |
|     - AutoConfigurationImportSelector discovers      |
|       @AutoConfiguration classes via SPI             |
|     - Evaluate @Conditional* annotations             |
|     - Fire ApplicationPreparedEvent                  |
+------------------------------------------------------+
       |
       v
+------------------------------------------------------+
|  5. CONTEXT REFRESHED                                |
|     - Instantiate all singleton beans                |
|     - Run BeanPostProcessors:                        |
|       * ConfigurationPropertiesBindingPostProcessor  |
|       * WebServerFactoryCustomizerBeanPostProcessor  |
|     - Create embedded web server:                    |
|       * Tomcat / Jetty / Undertow / Netty            |
|     - Start web server on configured port            |
|     - Fire ApplicationStartedEvent                   |
|     - Publish AvailabilityChangeEvent                |
|       (LivenessState.CORRECT)                        |
+------------------------------------------------------+
       |
       v
+------------------------------------------------------+
|  6. RUNNERS EXECUTED                                 |
|     - Run ApplicationRunner beans (ordered)          |
|     - Run CommandLineRunner beans (ordered)          |
+------------------------------------------------------+
       |
       v
+------------------------------------------------------+
|  7. APPLICATION READY                                |
|     - Fire ApplicationReadyEvent                     |
|     - Publish AvailabilityChangeEvent                |
|       (ReadinessState.ACCEPTING_TRAFFIC)             |
|                                                      |
|     Application is now serving requests              |
+------------------------------------------------------+
       |
       | (on failure at any step)
       v
+------------------------------------------------------+
|  FAILURE HANDLING                                    |
|     - Fire ApplicationFailedEvent                    |
|     - Run FailureAnalyzers -> human-readable report  |
|     - Exit with error code                           |
+------------------------------------------------------+
```

## 7. Web Request Processing Flow

```
HTTP Request
     |
     v
+------------------+
| Embedded Server  |     Tomcat / Jetty / Undertow / Netty
| (Connector)      |
+------------------+
     |
     v
+------------------+
| Servlet Filters  |     ErrorPageFilter, CharacterEncodingFilter,
| (Filter Chain)   |     HiddenHttpMethodFilter, FormContentFilter
+------------------+
     |
     v
+------------------+
| DispatcherServlet|     (auto-configured by DispatcherServletAutoConfiguration)
+------------------+
     |
     v
+------------------+
| HandlerMapping   |     RequestMappingHandlerMapping (for @Controller)
|                  |     WebMvcEndpointHandlerMapping (for /actuator/*)
+------------------+
     |
     v
+------------------+
| HandlerAdapter   |     Invokes @RequestMapping / @Endpoint methods
+------------------+
     |
     v
+------------------+
| Message          |     Jackson (auto-configured by JacksonAutoConfiguration)
| Converters       |     reads/writes JSON, XML, etc.
+------------------+
     |
     v
+------------------+
| Error Handling   |     BasicErrorController (Whitelabel error page)
|                  |     or custom ErrorController
+------------------+
     |
     v
HTTP Response
```

## 8. Embedded Web Server Factory Pattern

```
                    +---------------------+
                    |  WebServerFactory   |    (marker interface)
                    +---------------------+
                              |
              +---------------+----------------+
              |                                |
+---------------------------+    +----------------------------+
| ServletWebServerFactory   |    | ReactiveWebServerFactory   |
| getWebServer(inits...)    |    | getWebServer(handler)      |
+---------------------------+    +----------------------------+
     |       |       |                |              |
     v       v       v                v              v
+--------+ +------+ +---------+ +---------+   +-------+
| Tomcat | | Jetty| | Under-  | | Tomcat  |   | Netty |
| Servlet| |Servlt| | tow     | | React-  |   | React-|
| Factory| |Factry| | Servlet | | ive     |   | ive   |
|        | |      | | Factory | | Factory |   |Factory|
+--------+ +------+ +---------+ +---------+   +-------+
     |       |       |                |              |
     v       v       v                v              v
+--------+ +------+ +---------+ +---------+   +-------+
| Tomcat | | Jetty| | Under-  | | Tomcat  |   | Netty |
| Web    | | Web  | | tow     | | Web     |   | Web   |
| Server | |Server| | Web     | | Server  |   | Server|
|        | |      | | Server  | |         |   |       |
+--------+ +------+ +---------+ +---------+   +-------+
                         \          |          /
                          \         |         /
                           v        v        v
                    +---------------------+
                    |     WebServer       |
                    | start() / stop()   |
                    | getPort()          |
                    +---------------------+
```

## 9. Auto-Configuration Discovery & Filtering

```
@SpringBootApplication
     |
     +-- @EnableAutoConfiguration
              |
              v
AutoConfigurationImportSelector (DeferredImportSelector)
              |
              v
+-----------------------------------------------+
| 1. SpringFactoriesLoader                      |
|    Scan META-INF/spring/                      |
|    org.springframework.boot.autoconfigure.    |
|    AutoConfiguration.imports                  |
|    -> discovers 100+ candidate classes        |
+-----------------------------------------------+
              |
              v
+-----------------------------------------------+
| 2. Remove duplicates                          |
+-----------------------------------------------+
              |
              v
+-----------------------------------------------+
| 3. Apply exclusions                           |
|    - @EnableAutoConfiguration(exclude=...)    |
|    - spring.autoconfigure.exclude property    |
+-----------------------------------------------+
              |
              v
+-----------------------------------------------+
| 4. Evaluate @Conditional* filters             |
|                                               |
|  @ConditionalOnClass(DataSource.class)        |
|  -> is DataSource on classpath? ----NO----> skip  |
|                                  |            |
|                                 YES           |
|                                  |            |
|  @ConditionalOnMissingBean(DataSource.class)  |
|  -> does user already define one? --YES-> skip|
|                                  |            |
|                                  NO           |
|                                  |            |
|  @ConditionalOnProperty(                      |
|     "spring.datasource.url")                  |
|  -> is property set? -----------NO----> skip  |
|                                  |            |
|                                 YES           |
|                                  v            |
|                           REGISTER BEANS      |
+-----------------------------------------------+
              |
              v
+-----------------------------------------------+
| 5. Order by @AutoConfigureOrder,              |
|    @AutoConfigureBefore, @AutoConfigureAfter  |
+-----------------------------------------------+
              |
              v
       Bean definitions registered
       in ApplicationContext
```

## 10. Configuration Data Loading Pipeline

```
ConfigDataEnvironmentPostProcessor
              |
              v
ConfigDataEnvironment
              |
              v
+-----------------------------------------------+
| 1. Resolve default locations                  |
|    - optional:classpath:/                     |
|    - optional:classpath:/config/              |
|    - optional:file:./                         |
|    - optional:file:./config/                  |
|    - optional:file:./config/*/                |
+-----------------------------------------------+
              |
              v
+-----------------------------------------------+
| 2. ConfigDataLocationResolver                 |
|    StandardConfigDataLocationResolver:        |
|    - Expand location to file references       |
|    - Apply profile-specific variants          |
|      e.g., application-{profile}.yml          |
|    - Support .properties and .yml/.yaml       |
+-----------------------------------------------+
              |
              v
+-----------------------------------------------+
| 3. ConfigDataLoader                           |
|    StandardConfigDataLoader:                  |
|    - Parse YAML using SnakeYAML/SnakeYAML-eng|
|    - Parse .properties using java.util.Props  |
|    - Return ConfigData with PropertySources   |
+-----------------------------------------------+
              |
              v
+-----------------------------------------------+
| 4. Process imports                            |
|    spring.config.import=                      |
|    - configtree:/run/secrets/                 |
|    - optional:file:/etc/app/                  |
|    - configserver:http://config:8888          |
+-----------------------------------------------+
              |
              v
+-----------------------------------------------+
| 5. Activate profiles                          |
|    spring.profiles.active=prod,cloud          |
|    spring.profiles.group.prod=proddb,prodmq   |
+-----------------------------------------------+
              |
              v
     PropertySources added to Environment
     (in precedence order)
```

## 11. Starter Dependency Aggregation

```
+---------------------------+
| spring-boot-starter-web   |  (user adds this one dependency)
+---------------------------+
              |
              +-- spring-boot-starter           (core starter)
              |       +-- spring-boot
              |       +-- spring-boot-autoconfigure
              |       +-- spring-boot-starter-logging
              |       |       +-- logback-classic
              |       |       +-- log4j-to-slf4j
              |       |       +-- jul-to-slf4j
              |       +-- jakarta.annotation-api
              |       +-- spring-core
              |       +-- snakeyaml
              |
              +-- spring-boot-starter-json      (JSON support)
              |       +-- jackson-databind
              |       +-- jackson-datatype-jdk8
              |       +-- jackson-datatype-jsr310
              |       +-- jackson-module-parameter-names
              |
              +-- spring-boot-starter-tomcat    (embedded server)
              |       +-- tomcat-embed-core
              |       +-- tomcat-embed-el
              |       +-- tomcat-embed-websocket
              |
              +-- spring-web                    (Spring MVC)
              +-- spring-webmvc

Each of the 58 starters follows this pattern:
  starter = curated set of transitive dependencies
           + auto-configuration trigger via classpath presence
```

## 12. Health Check Aggregation

```
/actuator/health
       |
       v
HealthEndpoint
       |
       v
HealthEndpointWebExtension
       |
       v
+---------------------------------------------------+
|              StatusAggregator                      |
|  Aggregates: DOWN > OUT_OF_SERVICE > UP > UNKNOWN  |
+---------------------------------------------------+
       |
       +---> DiskSpaceHealthIndicator    -> UP / DOWN
       +---> DataSourceHealthIndicator   -> UP / DOWN
       +---> RedisHealthIndicator        -> UP / DOWN
       +---> MongoHealthIndicator        -> UP / DOWN
       +---> ElasticsearchHealthIndicator-> UP / DOWN
       +---> RabbitHealthIndicator       -> UP / DOWN
       +---> MailHealthIndicator         -> UP / DOWN
       +---> ... (auto-configured based on classpath)
       |
       v
{
  "status": "UP",
  "components": {
    "db":        { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "redis":     { "status": "UP" }
  }
}
```

## 13. Test Support Architecture

```
spring-boot-test-autoconfigure
       |
       +-- @SpringBootTest                   Full integration test
       |      Creates full ApplicationContext
       |      Starts embedded web server (optional)
       |
       +-- @WebMvcTest                       Controller-layer slice test
       |      Only web-layer beans loaded
       |      MockMvc auto-configured
       |
       +-- @DataJpaTest                      JPA repository slice test
       |      Only JPA beans + embedded DB
       |
       +-- @WebFluxTest                      WebFlux slice test
       +-- @JsonTest                         JSON serialization test
       +-- @JdbcTest                         JDBC slice test
       +-- @DataMongoTest                    MongoDB slice test
       +-- @DataRedisTest                    Redis slice test
       +-- @RestClientTest                   REST client test
       +-- @GraphQlTest                      GraphQL slice test
       |
       v
spring-boot-test
       |
       +-- @MockBean / @SpyBean             Mock beans in context
       +-- TestRestTemplate                  HTTP test client
       +-- OutputCaptureExtension            Capture stdout/stderr
       +-- ApplicationContextRunner          Test auto-config in isolation
```

## Module Statistics

| Module | Packages | Key Role |
|--------|----------|----------|
| spring-boot (core) | 34 | Bootstrap, embedded servers, config binding, logging |
| spring-boot-autoconfigure | 57 | Conditional bean registration for 57 technologies |
| spring-boot-actuator | 34 | Health, metrics, endpoints, audit |
| spring-boot-actuator-autoconfigure | — | Auto-configures actuator + metrics exporters + tracing |
| spring-boot-devtools | — | Live reload, remote debugging, restart |
| spring-boot-test | — | Test utilities, mock beans |
| spring-boot-test-autoconfigure | — | Slice test annotations (@WebMvcTest, etc.) |
| spring-boot-docker-compose | — | Docker Compose lifecycle integration |
| spring-boot-starters (58) | — | Curated dependency sets (no code) |
| spring-boot-tools (17) | — | Build plugins, loaders, processors |

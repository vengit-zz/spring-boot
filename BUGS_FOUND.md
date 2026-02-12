# Bugs Found in Spring Boot Codebase

All bugs below have been verified by reading the actual source code. False positives have been eliminated.

---

## Summary

| # | Category | File | Severity |
|---|----------|------|----------|
| 1 | Resource Leak | `SpringBootJoranConfigurator.java:437` | High |
| 2 | Resource Leak | `EmbeddedLdapAutoConfiguration.java:125` | Medium |
| 3 | Resource Leak | `ApplicationHome.java:123` | Low |
| 4 | Resource Leak | `DocumentRoot.java:105` | Low |
| 5 | Concurrency | `BindConverter.java:132-137` | Medium |
| 6 | Concurrency | `SpringIterableConfigurationPropertySource.java:176-180` | Low |
| 7 | Lost Exception Cause | `StringToFileConverter.java:45` | Medium |
| 8 | Lost Exception Cause | `EndpointMBean.java:131` | Medium |
| 9 | Lost Exception Cause | `EndpointMBean.java:134,142` | Medium |
| 10 | Incorrect Exception Wrapping | `AccessLogHttpHandlerFactory.java:111-112` | Medium |

---

## Bug 1 — InputStream Leak in SpringBootJoranConfigurator

**File:** `spring-boot/src/main/java/org/springframework/boot/logging/logback/SpringBootJoranConfigurator.java:437`

**Severity:** High — leaked InputStream on every invocation when file exists

**Code:**
```java
@Override
public void acceptWithException(FileHandler file) throws Exception {
    if (file.exists()) {
        byte[] existingContent = file.getContent().getInputStream().readAllBytes();  // ← LEAK
        if (!Arrays.equals(this.newContent, existingContent)) {
            throw new IllegalStateException(...);
        }
    }
```

**Problem:** The `InputStream` returned by `getContent().getInputStream()` is never closed. `readAllBytes()` consumes the data but does not close the stream. The underlying file handle leaks.

**Fix:** Wrap in try-with-resources:
```java
try (InputStream in = file.getContent().getInputStream()) {
    byte[] existingContent = in.readAllBytes();
    ...
}
```

---

## Bug 2 — InputStream Leak in EmbeddedLdapAutoConfiguration

**File:** `spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/ldap/embedded/EmbeddedLdapAutoConfiguration.java:125`

**Severity:** Medium — leaked InputStream during LDAP server setup

**Code:**
```java
private void setSchema(InMemoryDirectoryServerConfig config, Resource resource) {
    try {
        Schema defaultSchema = Schema.getDefaultStandardSchema();
        Schema schema = Schema.getSchema(resource.getInputStream());  // ← LEAK
        config.setSchema(Schema.mergeSchemas(defaultSchema, schema));
    }
    catch (Exception ex) {
        throw new IllegalStateException("Unable to load schema " + resource.getDescription(), ex);
    }
}
```

**Problem:** The `InputStream` from `resource.getInputStream()` is passed to `Schema.getSchema()` but never explicitly closed. The UnboundID LDAP SDK's `Schema.getSchema(InputStream)` reads from the stream but does not guarantee closure.

**Fix:** Wrap in try-with-resources:
```java
try (InputStream in = resource.getInputStream()) {
    Schema schema = Schema.getSchema(in);
    ...
}
```

---

## Bug 3 — URLConnection Not Closed in ApplicationHome

**File:** `spring-boot/src/main/java/org/springframework/boot/system/ApplicationHome.java:123`

**Severity:** Low — called once at startup

**Code:**
```java
private File findSource(URL location) throws IOException, URISyntaxException {
    URLConnection connection = location.openConnection();  // ← never closed
    if (connection instanceof JarURLConnection jarURLConnection) {
        return getRootJarFile(jarURLConnection.getJarFile());
    }
    return new File(location.toURI());
}
```

**Problem:** The `URLConnection` opened by `location.openConnection()` is never closed. For the non-JAR branch, the connection is abandoned entirely. `URLConnection` can hold native file descriptors and socket resources.

**Fix:** Use try-with-resources or close the connection's input stream in a finally block.

---

## Bug 4 — URLConnection Not Closed in DocumentRoot

**File:** `spring-boot/src/main/java/org/springframework/boot/web/servlet/server/DocumentRoot.java:105`

**Severity:** Low — called once at startup

**Code:**
```java
File getCodeSourceArchive(CodeSource codeSource) {
    try {
        URL location = (codeSource != null) ? codeSource.getLocation() : null;
        if (location == null) {
            return null;
        }
        String path;
        URLConnection connection = location.openConnection();  // ← never closed
        if (connection instanceof JarURLConnection jarURLConnection) {
            path = jarURLConnection.getJarFile().getName();
        }
        else {
            path = location.toURI().getPath();
        }
        ...
    }
    catch (Exception ex) {
        return null;
    }
}
```

**Problem:** Same as Bug 3 — `URLConnection` opened but never closed.

---

## Bug 5 — Race Condition in BindConverter Singleton

**File:** `spring-boot/src/main/java/org/springframework/boot/context/properties/bind/BindConverter.java:132-137`

**Severity:** Medium — data race under JMM; benign in practice (creates duplicate instances)

**Code:**
```java
private static BindConverter sharedInstance;  // ← not volatile

private static BindConverter getSharedInstance() {
    if (sharedInstance == null) {          // ← read without synchronization
        sharedInstance = new BindConverter(null, null);  // ← write without synchronization
    }
    return sharedInstance;
}
```

**Problem:** Classic check-then-act race condition. The `sharedInstance` field is not `volatile` and access is not synchronized. Under the Java Memory Model:
- Multiple threads can each see `null` and create separate instances
- A thread can read a non-null reference to an incompletely constructed `BindConverter`

The class Javadoc says "This class is not thread-safe", but `getSharedInstance()` is a static method returning a shared singleton, so it can be called from multiple threads.

**Fix:** Add `volatile` modifier to the field, or use holder-class idiom:
```java
private static class SharedInstanceHolder {
    static final BindConverter INSTANCE = new BindConverter(null, null);
}
```

---

## Bug 6 — Missing Volatile on Lazy-Initialized Field

**File:** `spring-boot/src/main/java/org/springframework/boot/context/properties/source/SpringIterableConfigurationPropertySource.java:176-180`

**Severity:** Low — configuration property sources are typically initialized on a single thread

**Code:**
```java
private ConfigurationPropertyName[] configurationPropertyNames;  // ← not volatile

private ConfigurationPropertyName[] getConfigurationPropertyNames() {
    if (!isImmutablePropertySource()) {
        return getCache().getConfigurationPropertyNames(getPropertySource().getPropertyNames());
    }
    ConfigurationPropertyName[] configurationPropertyNames = this.configurationPropertyNames;
    if (configurationPropertyNames == null) {
        configurationPropertyNames = getCache()
            .getConfigurationPropertyNames(getPropertySource().getPropertyNames());
        this.configurationPropertyNames = configurationPropertyNames;  // ← write without volatile
    }
    return configurationPropertyNames;
}
```

**Problem:** The racy single-check idiom without `volatile`. A reading thread could see a non-null reference to a partially-initialized array. The local variable copy (`configurationPropertyNames`) prevents re-reading the field, but without `volatile`, there is no happens-before relationship guaranteeing the array contents are visible.

**Fix:** Declare the field `volatile`.

---

## Bug 7 — Lost Exception Cause in StringToFileConverter

**File:** `spring-boot/src/main/java/org/springframework/boot/convert/StringToFileConverter.java:45`

**Severity:** Medium — original IOException stack trace lost

**Code:**
```java
private File getFile(Resource resource) {
    try {
        return resource.getFile();
    }
    catch (IOException ex) {
        throw new IllegalStateException(
            "Could not retrieve file for " + resource + ": " + ex.getMessage());  // ← missing cause
    }
}
```

**Problem:** The caught `IOException` is not passed as the `cause` argument to `IllegalStateException`. The original exception's stack trace and any chained causes are completely lost. This makes debugging file resolution failures significantly harder.

**Fix:**
```java
throw new IllegalStateException(
    "Could not retrieve file for " + resource + ": " + ex.getMessage(), ex);
```

---

## Bug 8 — Lost Exception Cause in EndpointMBean (InvalidEndpointRequestException)

**File:** `spring-boot-actuator/src/main/java/org/springframework/boot/actuate/endpoint/jmx/EndpointMBean.java:131`

**Severity:** Medium — original exception lost during JMX invocations

**Code:**
```java
catch (InvalidEndpointRequestException ex) {
    throw new ReflectionException(
        new IllegalArgumentException(ex.getMessage()),  // ← missing cause
        ex.getMessage());
}
```

**Problem:** The `IllegalArgumentException` wrapping the message does not include `ex` as its cause. The original `InvalidEndpointRequestException` and its stack trace are lost.

**Fix:**
```java
throw new ReflectionException(
    new IllegalArgumentException(ex.getMessage(), ex),
    ex.getMessage());
```

---

## Bug 9 — Lost Exception Cause in EndpointMBean (translateIfNecessary)

**File:** `spring-boot-actuator/src/main/java/org/springframework/boot/actuate/endpoint/jmx/EndpointMBean.java:134,142`

**Severity:** Medium — exception cause chain lost for non-java.* exceptions

**Code:**
```java
catch (Exception ex) {
    throw new MBeanException(translateIfNecessary(ex), ex.getMessage());
}

private Exception translateIfNecessary(Exception exception) {
    if (exception.getClass().getName().startsWith("java.")) {
        return exception;
    }
    return new IllegalStateException(exception.getMessage());  // ← missing cause
}
```

**Problem:** When the exception class is not from `java.*` packages, `translateIfNecessary` creates a new `IllegalStateException` with only the message — the original exception is not passed as the cause. All context about the original failure is lost.

**Fix:**
```java
return new IllegalStateException(exception.getMessage(), exception);
```

---

## Bug 10 — IOException Incorrectly Wrapped in RuntimeException

**File:** `spring-boot/src/main/java/org/springframework/boot/web/embedded/undertow/AccessLogHttpHandlerFactory.java:111-112`

**Severity:** Medium — converts checked exception to unchecked unnecessarily

**Code:**
```java
@Override
public void close() throws IOException {
    try {
        this.accessLogReceiver.close();
        this.worker.shutdown();
        this.worker.awaitTermination(30, TimeUnit.SECONDS);
    }
    catch (IOException ex) {
        throw new RuntimeException(ex);  // ← BUG: method already declares throws IOException
    }
    catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
    }
}
```

**Problem:** The `close()` method declares `throws IOException`, yet when `accessLogReceiver.close()` throws an `IOException`, it is caught and wrapped in a `RuntimeException`. The `IOException` should simply propagate directly since the method signature already allows it. Wrapping it in `RuntimeException` means:
1. Callers cannot catch it as `IOException`
2. The exception type information is lost
3. It bypasses any `IOException`-specific error handling upstream

**Fix:** Remove the `catch (IOException ex)` block entirely, or re-throw:
```java
catch (IOException ex) {
    throw ex;
}
```

---

## Methodology

Each bug was found through systematic pattern-based searches and then **manually verified** by reading the actual source code. The following categories of false positives were identified and excluded:

- **`FileCopyUtils.copy/copyToString/copyToByteArray`** — Spring's `FileCopyUtils` closes streams internally, so callers using it are not resource leaks
- **Annotation metadata casts** — Spring's `AnnotationAttributes` maps always contain the declared types with defaults, so casts from `getAnnotationAttributes()` are safe
- **`this.disable.get() == Boolean.TRUE`** — The `ThreadLocal<Boolean>` is always set with the static constant `Boolean.TRUE`, so identity comparison with `==` is correct
- **`MimeMappings.Mapping` hashCode** — Using only `extension` in hashCode while equals checks both fields does not violate the Java contract (equal objects will have equal hashes)
- **`TestcontainersPropertySource` unmodifiable wrapper bypass** — The mutable backing map modification through the registry lambda is intentional design, allowing dynamic property registration while presenting an unmodifiable view
- **Neo4j `Record.get()`** — The Neo4j driver returns `NullValue` objects rather than null, so direct `.asString()` calls are safe

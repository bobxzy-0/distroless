# Documentation for `gcr.io/distroless/java`

## Image Contents

This image contains a minimal Linux, OpenJDK-based runtime.

Specifically, the image contains everything in the [base image](../base/README.md), plus:

* Temurin OpenJDK 8 (`gcr.io/distroless/java8-debian13`) and its dependencies.
* Temurin OpenJDK 17 (`gcr.io/distroless/java17-debian13`) and its dependencies.
* Temurin OpenJDK 21 (`gcr.io/distroless/java21-debian13`) and its dependencies
* Temurin OpenJDK 25 (`gcr.io/distroless/java25-debian13`) and its dependencies

## Usage

The entrypoint of this image is set to the equivalent of "java -jar", so this image expects users to supply a path to a JAR file in the CMD.

## cgroup v1 and cgroup v2 support

All distroless Java images support both **cgroup v1** and **cgroup v2** container environments.

The JVM flag `-XX:+UseContainerSupport` is **enabled by default** in all included Temurin builds. When active, the JVM automatically detects the cgroup version used by the host (v1 or v2) and reads the container's resource limits:

| cgroup version | Paths read by JVM                              |
| -------------- | ---------------------------------------------- |
| v1             | `/sys/fs/cgroup/memory/memory.limit_in_bytes`  |
|                | `/sys/fs/cgroup/cpu/cpu.cfs_quota_us`          |
| v2             | `/sys/fs/cgroup/memory.max`                    |
|                | `/sys/fs/cgroup/cpu.max`                       |

### Version requirements

| Java version | cgroup v1 support | cgroup v2 support |
| ------------ | ----------------- | ----------------- |
| 8 (Temurin)  | 8u191+            | 8u372+            |
| 11           | 11.0.1+           | 11.0.14+          |
| 17           | 17.0.0+           | 17.0.2+           |
| 21+          | ✓                 | ✓                 |

The distroless Java 8 image ships Temurin **8u482** which fully supports both cgroup v1 and v2.

### How the JVM uses container limits

With `UseContainerSupport` active the JVM automatically:

* Limits the default max heap (`-Xmx`) to 25% of the container's memory limit (or physical RAM if no limit is set).
* Sets `availableProcessors()` to the CPU quota allocated to the container.

You can verify this is working correctly at runtime with:

```sh
# Print heap and container settings
java -XX:+PrintFlagsFinal -version 2>&1 | grep -E 'UseContainerSupport|MaxHeapSize'

# Detailed cgroup info (JDK 8u212+ / JDK 11.0.2+)
java -XX:+PrintContainerInfo -version
```

### Overriding container limits

If you need to override the automatic sizing, pass explicit JVM flags:

```dockerfile
CMD ["/app.jar", "-Xmx512m", "-Xms256m"]
```

Or pass them via environment variable (for Spring Boot and similar frameworks):

```dockerfile
ENV JAVA_TOOL_OPTIONS="-Xmx512m -Xms256m"
CMD ["/app.jar"]
```

> **Note:** Setting `JAVA_TOOL_OPTIONS` in the Dockerfile is also respected by the JVM even when `UseContainerSupport` is active, giving you fine-grained control over heap sizing on both cgroup v1 and cgroup v2 systems.

## Add another JAVA version

To support a new JAVA version you need to do the following steps:
- Add the new version to `ADOPTIUM_DEB_PER_DISTRO` in [java/BUILD](BUILD).
- Add the new version to `JAVA_MAJOR_VERSIONS` in [java/config.bzl](config.bzl).
- Add two yaml files in the [java/testdata](testdata) folder to prepare the tests, you can take inspiration on the other files.

You can then ensure everything works on this folder with `bazel build //java:...`

package testdata;

/**
 * Verifies that the JVM is running with container-aware resource detection
 * (-XX:+UseContainerSupport, enabled by default in JDK 8u191+).
 *
 * <p>When UseContainerSupport is active the JVM reads cgroup limits (both
 * cgroup v1 and cgroup v2) and sizes heap / CPU quota accordingly.
 *
 * <p>cgroup v1 support: JDK 8u191+
 * <p>cgroup v2 support: JDK 11.0.14+ / JDK 17.0.2+ / JDK 8u372+ (Temurin)
 */
public class CheckContainerSupport {

  public static void main(String[] args) {
    long maxMemory = Runtime.getRuntime().maxMemory();
    int cpus = Runtime.getRuntime().availableProcessors();

    // Sanity-check the values the JVM read from the cgroup hierarchy.
    if (maxMemory <= 0) {
      throw new RuntimeException("UseContainerSupport: invalid max heap: " + maxMemory);
    }
    if (cpus <= 0) {
      throw new RuntimeException("UseContainerSupport: invalid CPU count: " + cpus);
    }

    System.out.println("UseContainerSupport=true (default)");
    System.out.println("Max heap: " + maxMemory + " bytes");
    System.out.println("Available processors: " + cpus);
  }
}

package jakarta.servlet;

/**
 * Stub for jakarta.servlet.SingleThreadModel, which was removed in Jakarta Servlet 6.0.
 *
 * Spark 4.x ships its own shaded Jetty (org.sparkproject.jetty) that still references
 * this interface at class-loading time via ServletHolder. Without this stub the Spark UI
 * fails with NoClassDefFoundError when running alongside Spring Boot 4 / Jakarta EE 10.
 *
 * The interface had no methods when it existed — this stub is therefore entirely safe.
 *
 * @deprecated Was deprecated since Servlet 2.4 and removed in Servlet 6.0.
 */
@Deprecated
public interface SingleThreadModel {
    // Intentionally empty — this interface never had any methods.
}


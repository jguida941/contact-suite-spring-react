package contactapp.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Exposes the Spring {@link ApplicationContext} as a static hook.
 *
 * <p>Legacy {@code getInstance()} callers can resolve the actual Spring-managed
 * service even if instrumentation or class reloading resets static fields.
 */
@Component
public final class ApplicationContextProvider implements ApplicationContextAware {

    private static volatile ApplicationContext context;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        context = applicationContext;
    }

    /**
     * @return the active Spring application context, or {@code null} if it has not been created yet
     */
    public static ApplicationContext getContext() {
        return context;
    }

    private ApplicationContextProvider() {
        // Spring only
    }
}

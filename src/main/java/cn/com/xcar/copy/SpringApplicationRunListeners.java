package cn.com.xcar.copy;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * org.springframework.boot.SpringApplicationRunListeners
 * 由于类的权限是默认的不能引用,copy一份
 *
 * @Author lxj
 * @Date 2018-10-25 09:31
 **/
public class SpringApplicationRunListeners {

    private final Log log;
    private final List<SpringApplicationRunListener> listeners;

    public SpringApplicationRunListeners(Log log, Collection<? extends SpringApplicationRunListener> listeners) {
        this.log = log;
        this.listeners = new ArrayList(listeners);
    }

    public void starting() {
        Iterator var1 = this.listeners.iterator();

        while (var1.hasNext()) {
            SpringApplicationRunListener listener = (SpringApplicationRunListener) var1.next();
            listener.starting();
        }

    }

    public void environmentPrepared(ConfigurableEnvironment environment) {
        Iterator var2 = this.listeners.iterator();

        while (var2.hasNext()) {
            SpringApplicationRunListener listener = (SpringApplicationRunListener) var2.next();
            listener.environmentPrepared(environment);
        }

    }

    public void contextPrepared(ConfigurableApplicationContext context) {
        Iterator var2 = this.listeners.iterator();

        while (var2.hasNext()) {
            SpringApplicationRunListener listener = (SpringApplicationRunListener) var2.next();
            listener.contextPrepared(context);
        }

    }

    public void contextLoaded(ConfigurableApplicationContext context) {
        Iterator var2 = this.listeners.iterator();

        while (var2.hasNext()) {
            SpringApplicationRunListener listener = (SpringApplicationRunListener) var2.next();
            listener.contextLoaded(context);
        }

    }

    public void started(ConfigurableApplicationContext context) {
        Iterator var2 = this.listeners.iterator();

        while (var2.hasNext()) {
            SpringApplicationRunListener listener = (SpringApplicationRunListener) var2.next();
            listener.started(context);
        }

    }

    public void running(ConfigurableApplicationContext context) {
        Iterator var2 = this.listeners.iterator();

        while (var2.hasNext()) {
            SpringApplicationRunListener listener = (SpringApplicationRunListener) var2.next();
            listener.running(context);
        }

    }

    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        Iterator var3 = this.listeners.iterator();

        while (var3.hasNext()) {
            SpringApplicationRunListener listener = (SpringApplicationRunListener) var3.next();
            this.callFailedListener(listener, context, exception);
        }

    }

    private void callFailedListener(SpringApplicationRunListener listener, ConfigurableApplicationContext context, Throwable exception) {
        try {
            listener.failed(context, exception);
        } catch (Throwable var6) {
            if (exception == null) {
                ReflectionUtils.rethrowRuntimeException(var6);
            }

            if (this.log.isDebugEnabled()) {
                this.log.error("Error handling failed", var6);
            } else {
                String message = var6.getMessage();
                message = message != null ? message : "no error message";
                this.log.warn("Error handling failed (" + message + ")");
            }
        }

    }

}

package net.montoyo.wd.config.annoconfg.annotation.format;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
    enum Type {
        SERVER,
        CLIENT,
        COMMON
    }
    
    Type type();
    String path() default "";
}

package org.github.gabrielmartins.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command {

    String name();
    String description() default "";
    String usage()       default "";
    String[] aliases()   default {};
    String permission()  default "";
    boolean playerOnly() default false;
    boolean async()      default false;
}

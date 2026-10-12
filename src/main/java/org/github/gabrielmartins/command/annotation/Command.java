package org.github.gabrielmartins.command.annotation;

import org.github.gabrielmartins.command.annotation.platform.Platform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {

    String name();

    Platform platform() default Platform.BOTH;

    String description() default "";

    String usage() default "";

    String[] aliases() default {};

    String permission() default "";
}

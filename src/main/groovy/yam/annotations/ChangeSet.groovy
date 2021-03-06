package yam.annotations

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD])
@interface ChangeSet {

    int sequence() default 1

    String version() default "1.0"

    String author()

    String changeSetConnection()

    String comment() default "Migration Script"

    String dbms() default "mongo"

    boolean runAlways() default false
}
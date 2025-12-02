package bg.energo.phoenix.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a method requires releasing a lock as part of its execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithEntityNotLocked {
    String entityType() default "";
    String entityTypeParam() default "";
    String entityIdParam() default "id";
    String  versionIdParam() default "versionId";
    Class<?> parameterType() default Void.class;
}
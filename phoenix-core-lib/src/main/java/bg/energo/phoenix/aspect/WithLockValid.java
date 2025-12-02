package bg.energo.phoenix.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithLockValid {
    String entityType() default "";
    String entityIdParam()default "id";
    String versionIdParam() default "versionId";
    Class<?> parameterType() default Void.class;
}
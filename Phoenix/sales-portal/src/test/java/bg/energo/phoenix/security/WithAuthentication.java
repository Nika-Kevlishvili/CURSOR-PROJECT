package bg.energo.phoenix.security;

import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithJwtUserSecurityContextFactory.class)
public @interface WithAuthentication {

    String id = "test";
    String DEFAULT_EMAIL = "test@oppa.ge";

    String id() default id;
    String userFirstName() default "";
    String userLastName() default "";
    String userDisplayName() default "";
    String userDepartment() default "";

    String userEmail() default DEFAULT_EMAIL;


    PermissionMapping[] permissions() default {};

}
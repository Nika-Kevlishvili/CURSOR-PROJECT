package bg.energo.phoenix.aspect;

import bg.energo.phoenix.service.lock.LockService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Aspect class that provides functionality to handle locking mechanisms around methods.
 * Ensures proper validation, acquisition, and release of locks for entities before and
 * after the execution of annotated methods. This is done through annotations that specify
 * locking rules and parameters.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class LockAspect {

    private final LockService lockService;

    @Value("${apis.api.token}")
    private String apiToken;

    /**
     * Intercepts methods annotated with {@code @WithLockValid} and checks the validity of
     * a lock for the specified entity before the method execution. This involves resolving
     * the entity type, extracting the entity ID and version ID, and verifying the lock validity.
     *
     * @param joinPoint the join point representing the intercepted method call,
     *                  providing context about the target method and its arguments
     * @param withLockValid the annotation instance containing metadata such as
     *                      entity type, entity ID parameter name, version ID parameter name,
     *                      and parameter type
     */
    @Before("@annotation(withLockValid)")
    public void checkLockValidityBeforeMethod(JoinPoint joinPoint, WithLockValid withLockValid) {
        String entityType = withLockValid.entityType();
        if (entityType.isEmpty()) {
            entityType = resolveEntityType(joinPoint);
        }
        Long entityId = extractEntityId(joinPoint, withLockValid.entityIdParam(), withLockValid.parameterType());
        Long versionId = extractVersionId(joinPoint, withLockValid.versionIdParam(), withLockValid.parameterType());

        lockService.checkLockValidity(entityType, entityId, versionId);
    }

    /**
     * Intercepts methods annotated with {@code @WithEntityNotLocked} and validates that the corresponding
     * entity is not locked before method execution. This involves checking the lock status of the entity
     * specified by its type, ID, and optional version ID.
     *
     * @param joinPoint the join point representing the intercepted method call,
     *                  providing context about the target method and its arguments
     * @param withEntityNotLocked the annotation instance containing metadata such as
     *                             entity type, entity ID parameter name, version ID parameter name,
     *                             and parameter type
     */
    @Before("@annotation(withEntityNotLocked)")
    public void checkLockAvailabilityBeforeMethod(JoinPoint joinPoint, WithEntityNotLocked withEntityNotLocked) {
        String entityType = withEntityNotLocked.entityType();
        if (entityType.isEmpty()) {
            if (withEntityNotLocked.entityTypeParam().isEmpty()) {
                entityType = resolveEntityType(joinPoint);
            } else {
                entityType = (String) extractParameter(joinPoint, withEntityNotLocked.entityTypeParam(), true, String.class);
            }
        }
        Long entityId = extractEntityId(joinPoint, withEntityNotLocked.entityIdParam(), withEntityNotLocked.parameterType());
        Long versionId = extractVersionId(joinPoint, withEntityNotLocked.versionIdParam(), withEntityNotLocked.parameterType());

        lockService.ensureEntityNotLocked(entityType, entityId, versionId);
    }

    /**
     * Releases the lock associated with an entity after a method execution is successfully completed.
     * This method is invoked after returning from methods annotated with {@code @WithLockValid}.
     * The entity type, entity ID, and version ID are resolved based on the provided annotation
     * and method arguments, facilitating the removal of the lock.
     *
     * @param joinPoint the join point representing the intercepted method call,
     *                  providing context about the target method and its arguments
     * @param withLockValid the annotation instance containing metadata such as entity type,
     *                      entity ID parameter name, version ID parameter name, and parameter type
     */
    @AfterReturning("@annotation(withLockValid)")
    public void releaseLockAfterMethod(JoinPoint joinPoint, WithLockValid withLockValid) {
        releaseLockAfter(joinPoint, withLockValid);
    }

    /**
     * Releases the lock associated with an entity after a method execution if an exception
     * is thrown during the method. The entity details such as type, ID, and version ID
     * are resolved based on the provided annotation and method arguments. This method
     * is executed for methods annotated with {@code @WithLockValid}.
     *
     * @param joinPoint the join point representing the intercepted method call,
     *                  providing context about the target method and its arguments
     * @param withLockValid the annotation instance containing metadata such as
     *                      entity type, entity ID parameter name, version ID parameter name,
     *                      and parameter type
     */
    @AfterThrowing("@annotation(withLockValid)")
    public void releaseLockAfterTestMethod(JoinPoint joinPoint, WithLockValid withLockValid) {
        if (apiToken.equals("test")) {
            releaseLockAfter(joinPoint, withLockValid);
        }
    }

    /**
     * Acquires a lock for the specified entity before the execution of a method annotated
     * with {@code @WithLockValid}. This involves determining the entity type, extracting
     * the entity ID and version ID from the method arguments, and ensuring that a lock
     * is established for the given entity context.
     *
     * @param joinPoint the join point representing the intercepted method call, providing
     *                  access to method details and arguments
     * @param withLockValid the annotation instance providing metadata such as entity type,
     *                      entity ID parameter name, version ID parameter name, and parameter type
     */
    @Before("@annotation(withLockValid)")
    public void acquireLockBeforeMethod(JoinPoint joinPoint, WithLockValid withLockValid) {
        if (apiToken.equals("test")){
            String entityType = withLockValid.entityType();
            if (entityType.isEmpty()) {
                entityType = resolveEntityType(joinPoint);
            }
            Long entityId = extractEntityId(joinPoint, withLockValid.entityIdParam(), withLockValid.parameterType());
            Long versionId = extractVersionId(joinPoint, withLockValid.versionIdParam(), withLockValid.parameterType());

            lockService.acquireLock(entityType, entityId, versionId);
        }
    }

    private void releaseLockAfter(JoinPoint joinPoint, WithLockValid withLockValid) {
        String entityType = withLockValid.entityType();
        if (entityType.isEmpty()) {
            entityType = resolveEntityType(joinPoint);
        }
        Long entityId = extractEntityId(joinPoint, withLockValid.entityIdParam(), withLockValid.parameterType());
        Long versionId = extractVersionId(joinPoint, withLockValid.versionIdParam(), withLockValid.parameterType());

        lockService.releaseLock(entityType, entityId, versionId);
    }

    /**
     * Resolves the entity type based on the class-level @RequestMapping annotation of the target object
     * accessed via the provided JoinPoint.
     *
     * @param joinPoint the join point representing the intercepted method call, used to access the target class
     * @return the entity type as a string, derived from the value of the @RequestMapping annotation on the target class
     * @throws IllegalArgumentException if the target class does not have a @RequestMapping annotation or its value is invalid
     */
    private String resolveEntityType(JoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget().getClass();

        RequestMapping requestMapping = targetClass.getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.value().length > 0) {
            String requestMappingPath = requestMapping.value()[0];
            if (requestMappingPath.startsWith("/")) {
                requestMappingPath = requestMappingPath.substring(1);
            }
            return requestMappingPath;
        }

        throw new IllegalArgumentException("Unable to resolve entity type. Ensure @RequestMapping is specified at the class level.");
    }

    /**
     * Extracts the value of an entity ID parameter from the arguments passed in a join point during method interception.
     *
     * @param joinPoint the join point representing the intercepted method call
     * @param entityIdParam the name of the entity ID parameter to extract
     * @param parameterType the type of the parameter to be used for reflection-based extraction
     * @return the entity ID as a Long if the entity ID parameter is found; null otherwise
     */
    private Long extractEntityId(JoinPoint joinPoint, String entityIdParam, Class<?> parameterType) {
        return (Long) extractParameter(joinPoint, entityIdParam, true, parameterType);
    }

    /**
     * Extracts the value of a version ID parameter from the arguments passed in a join point during method interception.
     *
     * @param joinPoint the join point representing the intercepted method call
     * @param versionIdParam the name of the version ID parameter to extract
     * @param parameterType the type of the parameter to be used for reflection-based extraction
     * @return the version ID as a Long if the version ID parameter is found; null otherwise
     */
    private Long extractVersionId(JoinPoint joinPoint, String versionIdParam, Class<?> parameterType) {
        return null;
//        return (Long) extractParameter(joinPoint, versionIdParam, false, parameterType);
    }

    /**
     * Extracts the value of a method parameter or field from the arguments passed
     * in a join point during method interception.
     *
     * @param joinPoint the join point representing the method being intercepted
     * @param parameterName the name of the parameter or field to extract
     * @param isRequired a flag indicating whether the parameter is required; if true,
     *                   an exception is thrown if the parameter is not found or null
     * @param parameterType the expected type of the parameter for reflection-based extraction;
     *                      if Void.class, the parameter type is not used for matching
     * @return the extracted parameter value, or null if the parameter is not found and is not required
     * @throws IllegalArgumentException if the parameter is required but not found or is null
     */
    private Object extractParameter(JoinPoint joinPoint, String parameterName, boolean isRequired, Class<?> parameterType) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // Step 1: Check if a parameterType is specified for reflection-based extraction
        if (parameterType != Void.class) {
            for (Object arg : args) {
                if (parameterType.isInstance(arg)) {
                    Object param = extractFields(arg, parameterName);
                    if (param != null) {
                        return param;
                    }
                }
            }
        }

        // Step 2: If parameterType is not provided, check if parameter name is found in method signature
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(parameterName)) {
                return args[i];
            }
        }

        // Step 3: Throw exception if required parameter is not found or is null
        if (isRequired) {
            throw new IllegalArgumentException("Parameter '" + parameterName + "' not found or is null.");
        }

        return null;
    }

    /**
     * Extracts the value of a specified field from a given object instance using reflection.
     *
     * @param paramInstance the object instance from which the field value needs to be extracted
     * @param fieldName the name of the field to extract
     * @return the value of the specified field if it exists and is accessible, or null if the field is not found
     *         or cannot be accessed
     */
    private Object extractFields(Object paramInstance, String fieldName) {
        try {
            var field = paramInstance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(paramInstance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
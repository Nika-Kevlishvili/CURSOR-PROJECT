package bg.energo.phoenix.security;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Service;


@Slf4j
@Aspect
@Service
@RequiredArgsConstructor
public class PermissionValidationAspect {


    private final PermissionService permissionService;

    @Before("@annotation(permissionValidator)")
    public void validate( PermissionValidator permissionValidator) {
        PermissionMapping[] permissions = permissionValidator.permissions();
        if(!permissionService.checkPermission(permissions)){
            throw new ClientException("You do not have access on this method!",ErrorCode.ACCESS_DENIED);
        }
    }

}

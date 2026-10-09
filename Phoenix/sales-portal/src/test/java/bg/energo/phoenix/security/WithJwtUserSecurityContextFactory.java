package bg.energo.phoenix.security;

import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import bg.energo.common.security.acl.AppModulePermissionsConfiguration;
import bg.energo.common.security.acl.AppPermissionsConfiguration;
import bg.energo.common.security.acl.definitions.AclContextDefinition;
import bg.energo.common.security.acl.definitions.AclPermissionDefinition;
import bg.energo.common.security.acl.enums.AclAccessStatus;
import bg.energo.common.security.acl.enums.AclConfigurationStatus;
import bg.energo.common.security.acl.implementation.AclContextInstance;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.principal.EnergoProPrincipal;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class WithJwtUserSecurityContextFactory implements WithSecurityContextFactory<WithAuthentication> {


    @Value("${app.cfg.applicationId}")
    private UUID appId;
    @Value("${app.cfg.moduleId}")
    private UUID moduleId;

    @Override
    public SecurityContext createSecurityContext(WithAuthentication jwtAuthenticationInfo) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        EnergoProPrincipal energoProPrincipal = createTestAuthentication(jwtAuthenticationInfo);
        context.setAuthentication(energoProPrincipal);
        return context;
    }

    @NotNull
    private EnergoProPrincipal createTestAuthentication(WithAuthentication jwtAuthenticationInfo) {
        PortalUserForApplicationDto portalUserForApplicationDto = new PortalUserForApplicationDto();
        EnergoProPrincipal energoProPrincipal = new EnergoProPrincipal("",portalUserForApplicationDto);
        portalUserForApplicationDto.setId(jwtAuthenticationInfo.id());
//        var contextPermissions =
//                Stream.of(jwtAuthenticationInfo.permissionMappings()).collect(Collectors.toMap(permissionMapping -> permissionMapping.context().getId(),
//                permissionMapping -> Stream.of(permissionMapping.permissionMappings()).map(PermissionEnum::getId).toList()));
//        portalUserForApplicationDto.setContextPermissions(contextPermissions);
//        portalUserForApplicationDto.setId(jwtAuthenticationInfo.id());

        AppPermissionsConfiguration appPermissionsConfiguration = new AppPermissionsConfiguration();
        AppModulePermissionsConfiguration module = new AppModulePermissionsConfiguration();
        module.setModuleId(moduleId);
        module.setAppId(appId);
        module.setStatus(AclConfigurationStatus.ENABLED);
        PermissionMapping[] permissionMappings = jwtAuthenticationInfo.permissions();
        for (PermissionMapping contextMapping : permissionMappings) {
            PermissionContextEnum context = contextMapping.context();
            AclContextDefinition contextDefinition = new AclContextDefinition(
                    context.getId(),
                    context.getDescriptionKey(),
                    context.getAclContextDefinition()
            );

            AclContextInstance instance = module.addContext(contextDefinition, AclAccessStatus.GRANTED);
            PermissionEnum[] permissions  = contextMapping.permissions();
            for (PermissionEnum permission : permissions) {
                instance.addPermission(new AclPermissionDefinition(
                        permission.getId(),
                        permission.getType(),
                        permission.getAclValues(),
                        permission.getTitleKey(),
                        permission.getDescriptionKey()
                ),AclAccessStatus.GRANTED, Arrays.stream(permissions).map(PermissionEnum::getId).collect(Collectors.toSet()),true);
            }

        }
        appPermissionsConfiguration.setAppId(appId);
        appPermissionsConfiguration.addModule(moduleId,module);

        portalUserForApplicationDto.setAppsConfiguration(appPermissionsConfiguration);

        return energoProPrincipal;
    }


}

package bg.energo.phoenix.model.response;


import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import bg.energo.common.security.acl.AppModulePermissionsConfiguration;
import bg.energo.common.security.acl.AppPermissionsConfiguration;
import bg.energo.common.security.acl.enums.AclAccessStatus;
import bg.energo.common.security.acl.implementation.AclContextInstance;
import bg.energo.common.security.acl.implementation.AclUserPermission;
import bg.energo.phoenix.model.principal.EnergoProPrincipal;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
@Data
public class PermissionResponse {
    private String id;
    private Long managerId;
    private String userFirstName;
    private String userLastName;
    private String userDisplayName;
    private String userDepartment;
    private String userEmail;
    private String avatarId;
    private String avatarPath;
    private List<PermissionContext> permissionContexts;

    @Data
    public static class PermissionContext{

        private String id;
        private List<String> permissions;
    }


    public PermissionResponse(EnergoProPrincipal principal,UUID moduleId) {
        PortalUserForApplicationDto applicationUser = principal.getApplicationUser();
        this.id=applicationUser.getId();
        this.userFirstName=applicationUser.getUserFirstName();
        this.userLastName = applicationUser.getUserLastName();
        this.userDisplayName=applicationUser.getUserDisplayName();
        this.userDepartment=applicationUser.getUserDepartment();
        this.userEmail=applicationUser.getUserEmail();
        this.permissionContexts =new ArrayList<>();

        AppPermissionsConfiguration permissionsConfiguration = applicationUser.getAppsConfiguration();
        AppModulePermissionsConfiguration moduleConfiguration = permissionsConfiguration.getModuleConfiguration(moduleId);
        Set<String> contextIds = moduleConfiguration.getContextIds();
        contextIds.forEach(context -> {
            AclContextInstance instance = moduleConfiguration.getContext(context);
            if (instance.getStatus().equals(AclAccessStatus.GRANTED)) {
                PermissionContext permissionContext = new PermissionContext();
                permissionContext.setId(instance.getId());
                this.permissionContexts.add(permissionContext);

                List<String> permissions = new ArrayList<>();
                permissionContext.setPermissions(permissions);
                List<AclUserPermission> verbs = instance.getVerbs();
                verbs.forEach(verb -> {
                    if (verb.getStatus().equals(AclAccessStatus.GRANTED)) {
                        permissions.add(verb.getId());
                    }
                });

            }

        });

    }
}

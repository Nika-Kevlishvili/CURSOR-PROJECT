package bg.energo.phoenix.security;


import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import bg.energo.common.security.acl.AppModulePermissionsConfiguration;
import bg.energo.common.security.acl.AppPermissionsConfiguration;
import bg.energo.common.security.acl.enums.AclAccessStatus;
import bg.energo.common.security.acl.implementation.AclContextInstance;
import bg.energo.common.security.acl.implementation.AclUserPermission;
import bg.energo.common.utils.JsonUtils;
import bg.energo.phoenix.config.AppConfig;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.principal.EnergoProPrincipal;
import bg.energo.phoenix.model.response.PermissionResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final AppConfig appConfig;
    private final AccountManagerRepository accountManagerRepository;

    /**
     * Check if the logged-in user has the given permission in the given context
     *
     * @param permissionMappings the permission mappings to check for the logged-in user
     * @return true if the user has the given permission in the given context, false otherwise
     */
    public boolean checkPermission(PermissionMapping[] permissionMappings) {
        try {
            EnergoProPrincipal principal = (EnergoProPrincipal) SecurityContextHolder.getContext().getAuthentication();

            if (principal == null) {
                return false;
            }
            final PortalUserForApplicationDto appUser = principal.getApplicationUser();

            logAllowedPermissions(permissionMappings, appUser);

            for (PermissionMapping mapping : permissionMappings) {
                if (appUser.isContextGranted(appConfig.getModuleId(), mapping.context().getId())) {
                    for (PermissionEnum permission : mapping.permissions()) {
                        if (appUser.isGranted(appConfig.getModuleId(), mapping.context().getId(), permission.getId())) {
                            return true;
                        }
                    }
                }
            }


            return false;
        } catch (Throwable e) {
            log.error("", e);
            return false;
        }
    }

    private void logAllowedPermissions(PermissionMapping[] permissionMappings, PortalUserForApplicationDto appUser) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALLOWED_PERMISSION_SET ");
        sb.append(appUser.getUserEmail());
        for (PermissionMapping mapping : permissionMappings) {
            sb.append(" Context: %s permissions:".formatted(mapping.context().name()));
            for (PermissionEnum permission : mapping.permissions()) {
                sb.append(" %s,".formatted(permission.name()));
            }
        }
        log.info(sb.toString());
    }


    /**
     * @return the logged-in user id from the security context or null if no user is logged in
     */
    public String getLoggedInUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        EnergoProPrincipal principal = (EnergoProPrincipal) authentication;
        final PortalUserForApplicationDto appUser = principal.getApplicationUser();
        return appUser.getId();
    }


    /**
     * @return currently logged-in user's principal or null if no user is logged in
     */
    public PermissionResponse getUserPermissionResponse() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        EnergoProPrincipal principal = (EnergoProPrincipal) authentication;
        PermissionResponse permissionResponse = new PermissionResponse(principal, appConfig.getModuleId());
        Optional<AccountManager> name = accountManagerRepository.findByUserName(permissionResponse.getId());
        name.ifPresent(accountManager -> permissionResponse.setManagerId(accountManager.getId()));
        return permissionResponse;
    }


    /**
     * Retrieves all permissions from the given context for the currently logged-in user
     *
     * @param contextEnum the context to retrieve permissions from
     * @return list of permissions from the given context for the currently logged-in user
     */
    public List<String> getPermissionsFromContext(PermissionContextEnum contextEnum) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }
        EnergoProPrincipal principal = (EnergoProPrincipal) authentication;
        PortalUserForApplicationDto applicationUser = principal.getApplicationUser();
        AppPermissionsConfiguration permissionsConfiguration = applicationUser.getAppsConfiguration();
        AppModulePermissionsConfiguration moduleConfiguration = permissionsConfiguration.getModuleConfiguration(appConfig.getModuleId());
        AclContextInstance context = moduleConfiguration.getContext(contextEnum.getId());
        if (context == null) {
            return List.of();
        }
        return context.getVerbs()
                .stream()
                .filter(x -> x.getStatus().equals(AclAccessStatus.GRANTED))
                .map(AclUserPermission::getId)
                .toList();
    }


    /**
     * Loops through all permissions in context and checks if any of them is contained in the list of permissions
     *
     * @param permissionContext permission context to check
     * @param permissions       list of permissions to check against the context
     * @return true if any of the permissions is contained in the context, false otherwise
     */
    public boolean permissionContextContainsPermissions(PermissionContextEnum permissionContext, List<PermissionEnum> permissions) {
        List<String> permissionsFromContext = getPermissionsFromContext(permissionContext);
        List<String> prompts = permissions.stream().map(PermissionEnum::getId).toList();
        return permissionsFromContext.stream().anyMatch(prompts::contains);
    }


}

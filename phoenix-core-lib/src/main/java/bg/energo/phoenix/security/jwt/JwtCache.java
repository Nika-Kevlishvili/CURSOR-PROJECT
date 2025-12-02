package bg.energo.phoenix.security.jwt;

import bg.energo.common.backend.jwt.PortalPrincipal;
import bg.energo.common.portal.api.register.AppRegisterResponse;
import bg.energo.common.portal.api.register.RemoteResource;
import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import bg.energo.phoenix.config.AppConfig;
import bg.energo.phoenix.service.portal.ApplicationStateService;
import bg.energo.phoenix.service.portal.PortalResources;
import bg.energo.phoenix.service.portal.PortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class JwtCache {

    private final ApplicationStateService applicationStateService;
    private final PortalService portalService;
    private final AppConfig appConfig;

    @Cacheable(value = "authUserCache",key = "#portalPrincipal.tokenId")
    public PortalUserForApplicationDto getApplicationUser(PortalPrincipal portalPrincipal) {

        AppRegisterResponse registrationResponse = applicationStateService.getPortalRegistrationBundle().getRegistrationResponse();
        RemoteResource queryTokenResource = registrationResponse.getResources().get(PortalResources.QUERY_TOKEN_RESOURCE);
        String sessionId = registrationResponse.getPortalSessionKey();

        return portalService.getCurrentUser(queryTokenResource.getUrl(), portalPrincipal.getJwtToken(),sessionId);
    }
}

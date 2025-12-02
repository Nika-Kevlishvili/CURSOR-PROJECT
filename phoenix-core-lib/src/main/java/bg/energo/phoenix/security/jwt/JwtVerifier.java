package bg.energo.phoenix.security.jwt;

import bg.energo.common.backend.jwt.PortalPrincipal;
import bg.energo.common.jwt.JwtDecodeUtils;
import bg.energo.common.jwt.JwtSession;
import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import bg.energo.common.security.acl.enums.AclUserStatus;
import bg.energo.common.security.exception.PortalSecurityException;
import bg.energo.phoenix.service.portal.ApplicationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtVerifier {

    private final ApplicationStateService applicationStateService;
    private final JwtCache cache;



    public PortalPrincipal verifyToken(final String token) throws PortalSecurityException {
        try {
            final JwtDecodeUtils jwtDecodeUtils = new JwtDecodeUtils(applicationStateService.getPortalRegistrationBundle().getRegistrationResponse().getPublicKey());
            final JwtSession jwtSession = jwtDecodeUtils.decodeJwt(token);

            return new PortalPrincipal(jwtSession.getId(), token, jwtSession.getLanguage(), jwtSession, null);
        } catch (final Exception e) {
            log.error("Exception while verifying token; ", e);
            throw new PortalSecurityException(e.getMessage());
        }
    }


    public PortalUserForApplicationDto verifyTokenAndGetApplicationUser(String token) throws PortalSecurityException {
        PortalPrincipal portalPrincipal = verifyToken(token);
        return cache.getApplicationUser(portalPrincipal);
    }


    public AclUserStatus getUserStatus(PortalUserForApplicationDto portalUserForApplicationDto) {
        return portalUserForApplicationDto.getStatus();
    }
}

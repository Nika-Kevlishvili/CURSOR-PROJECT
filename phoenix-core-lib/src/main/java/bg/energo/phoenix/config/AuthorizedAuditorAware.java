package bg.energo.phoenix.config;

import bg.energo.phoenix.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthorizedAuditorAware implements AuditorAware<String> {

    private final AuthService authService;

    @Override
    public Optional<String> getCurrentAuditor() {
        if(authService==null){
            return Optional.of("system");
        }
        String authorizedUser = authService.getAuthorizedUser();
        if (StringUtils.isEmpty(authorizedUser)) {
            return Optional.of("system");
        }
        return Optional.of(authorizedUser);
    }
}

package bg.energo.phoenix.model.principal;

import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.security.auth.Subject;
import java.io.Serial;
import java.util.Collection;

@NoArgsConstructor
public class EnergoProPrincipal implements Authentication {

    @Serial
    private static final long serialVersionUID = 4789036644294459639L;
    @Getter
    private String token;
    @Getter
    private PortalUserForApplicationDto applicationUser;

    public EnergoProPrincipal(String token, PortalUserForApplicationDto applicationUser) {
        this.token = token;
        this.applicationUser = applicationUser;
    }

    @Override
    public String getName() {
        return token;
    }

    @Override
    public boolean implies(Subject subject) {
        return Authentication.super.implies(subject);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    public String getUserNameAndDisplayName() {
        if (token != null) {
            return "UserName is: %s and Display Name is %s";
        }
        return "Token is not available";
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }
}

package bg.energo.phoenix.service;

import bg.energo.phoenix.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PermissionService permissionService;
    public String getAuthorizedUser(){
        //TODO: Modify to fetch from logged in user (Ping @Irakli for updating tests)
        return permissionService.getLoggedInUserId();
    }
}

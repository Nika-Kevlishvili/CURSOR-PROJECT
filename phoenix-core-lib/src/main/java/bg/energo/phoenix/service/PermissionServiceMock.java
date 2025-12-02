package bg.energo.phoenix.service;

import org.springframework.stereotype.Service;

@Service
public class PermissionServiceMock {

    public boolean hasGDRP() {
        return true;
    }
}

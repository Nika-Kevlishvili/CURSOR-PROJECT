package bg.energo.phoenix.bulgariapost.auth.service;

import java.util.Objects;

import bg.energo.phoenix.bulgariapost.auth.api.BulgariaPostAuthRequest;
import bg.energo.phoenix.bulgariapost.auth.api.BulgariaPostAuthResponse;
import bg.energo.phoenix.bulgariapost.auth.config.BulgariaPostAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulgariaPostAuthService {

    private static final Logger log = LoggerFactory.getLogger(BulgariaPostAuthService.class);
    private static final int MAX_LEN = 256;
    private static final String MSG_INVALID_CREDS = "Грешно потребителско име и/или парола";
    private static final String MSG_INVALID_STATION = "Невалиден код на станцията";
    private static final String MSG_SYSTEM_ERROR = "Системна грешка";

    private final BulgariaPostAuthProperties properties;
    private final BulgariaPostTokenService tokenService;

    public BulgariaPostAuthService(BulgariaPostAuthProperties properties, BulgariaPostTokenService tokenService) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
    }

    public BulgariaPostAuthResponse authenticate(BulgariaPostAuthRequest request) {
        try {
            if (request == null) {
                return BulgariaPostAuthResponse.error(3, MSG_SYSTEM_ERROR);
            }

            final String reqUser = request.getUserName();
            final String reqPass = request.getUserPassword();
            final String postCode = request.getPostCode();

            if (isRequestParamInvalid(reqUser) || isRequestParamInvalid(reqPass)) {
                return BulgariaPostAuthResponse.error(3, MSG_SYSTEM_ERROR);
            }

            if (isRequestParamInvalid(postCode)) {
                return BulgariaPostAuthResponse.error(2, MSG_INVALID_STATION);
            }

            if (!Objects.equals(reqUser, properties.getUsername())
                    || !Objects.equals(reqPass, properties.getPassword())) {
                return BulgariaPostAuthResponse.error(1, MSG_INVALID_CREDS);
            }

            final String token = tokenService.issueToken().token();
            return BulgariaPostAuthResponse.success(token);
        } catch (Exception e) {
            log.warn("BulgariaPost auth failed due to system error", e);
            return BulgariaPostAuthResponse.error(3, MSG_SYSTEM_ERROR);
        }
    }

    private static boolean isRequestParamInvalid(String s) {
        return s == null || s.isEmpty() || s.length() > MAX_LEN;
    }
}


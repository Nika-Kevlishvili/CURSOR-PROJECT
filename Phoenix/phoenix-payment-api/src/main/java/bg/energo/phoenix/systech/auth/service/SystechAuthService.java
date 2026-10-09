package bg.energo.phoenix.systech.auth.service;

import java.util.Objects;

import bg.energo.phoenix.systech.auth.api.SystechAuthRequest;
import bg.energo.phoenix.systech.auth.api.SystechAuthResponse;
import bg.energo.phoenix.systech.auth.config.SystechAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystechAuthService {

    private static final Logger log = LoggerFactory.getLogger(SystechAuthService.class);
    private static final int MAX_LEN = 256;
    private static final String MSG_INVALID_CREDS = "Грешно потребителско име и/или парола";
    private static final String MSG_INVALID_STATION = "Невалиден код на станцията";
    private static final String MSG_SYSTEM_ERROR = "Системна грешка";

    private final SystechAuthProperties properties;
    private final SystechTokenService tokenService;

    public SystechAuthService(SystechAuthProperties properties, SystechTokenService tokenService) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
    }

    public SystechAuthResponse authenticate(SystechAuthRequest request) {
        try {
            if (request == null) {
                return SystechAuthResponse.error(3, MSG_SYSTEM_ERROR);
            }

            final String reqUser = request.getUserName();
            final String reqPass = request.getUserPassword();
            final String postCode = request.getPostCode();

            if (isRequestParamInvalid(reqUser) || isRequestParamInvalid(reqPass)) {
                return SystechAuthResponse.error(3, MSG_SYSTEM_ERROR);
            }

            if (isRequestParamInvalid(postCode)) {
                return SystechAuthResponse.error(2, MSG_INVALID_STATION);
            }

            if (!Objects.equals(reqUser, properties.getUsername())
                    || !Objects.equals(reqPass, properties.getPassword())) {
                return SystechAuthResponse.error(1, MSG_INVALID_CREDS);
            }

            final String token = tokenService.issueToken().token();
            return SystechAuthResponse.success(token);
        } catch (Exception e) {
            log.warn("Systech auth failed due to system error", e);
            return SystechAuthResponse.error(3, MSG_SYSTEM_ERROR);
        }
    }

    private static boolean isRequestParamInvalid(String s) {
        return s == null || s.isEmpty() || s.length() > MAX_LEN;
    }
}

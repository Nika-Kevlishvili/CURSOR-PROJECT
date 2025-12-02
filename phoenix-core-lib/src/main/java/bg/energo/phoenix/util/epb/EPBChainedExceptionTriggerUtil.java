package bg.energo.phoenix.util.epb;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.List;

public class EPBChainedExceptionTriggerUtil {

    /**
     * Throws exception if messages not empty and logs them
     *
     * @param exceptionMessages list of messages to be logged and thrown
     * @param log               logger
     */
    public static void throwExceptionIfRequired(List<String> exceptionMessages, Logger log) throws RuntimeException {
        log.debug("Throwing exceptions if required, exceptions list: [{}]", exceptionMessages);
        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            log.error(StringUtils.join("; ", exceptionMessages));
            StringBuilder sb = new StringBuilder();

            for (String exceptionMessage : exceptionMessages) {
                if (!exceptionMessage.contains(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR)) {
                    sb.append(exceptionMessage);
                    if (exceptionMessage.charAt(exceptionMessage.length() - 1) != ';') {
                        sb.append(";");
                    }
                }
            }

            if (sb.isEmpty()) {
                sb.append("Error: Process Failed");
            }

            throw new ClientException(sb.toString().trim(), ErrorCode.CONFLICT);
        }
    }

    /**
     * Throws exception if messages not empty and logs them
     *
     * @param finalizedMessage  last exception message before throwing exception
     * @param exceptionMessages list of messages to be logged and thrown
     * @param log               logger
     */
    public static void addExceptionAndTrigger(String finalizedMessage, List<String> exceptionMessages, Logger log) throws ClientException {
        exceptionMessages.add(finalizedMessage);

        log.debug("Throwing exceptions, exceptions list: [{}]", exceptionMessages);
        log.error(StringUtils.join("; ", exceptionMessages));
        StringBuilder sb = new StringBuilder();

        for (String exceptionMessage : exceptionMessages) {
            if (!exceptionMessage.contains(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR)) {
                sb.append(exceptionMessage);
                if (exceptionMessage.charAt(exceptionMessage.length() - 1) != ';') {
                    sb.append(";");
                }
            }
        }

        throw new ClientException(sb.toString().trim(), ErrorCode.CONFLICT);
    }
}

package bg.energo.phoenix.util.epb;

import org.apache.commons.lang3.StringUtils;

public class EPBStringUtils {

    /**
     * This is a utility method that converts a string to a query parameter that can be used in a LIKE query.
     *
     * @param prompt the string to be converted
     * @return the converted string
     */
    public static String fromPromptToQueryParameter(String prompt) {
        StringBuilder sb = new StringBuilder("%");
        if (StringUtils.isNotBlank(prompt)) {
            sb.append(prompt.toLowerCase());
        }
        sb.append("%");
        return sb.toString();
    }

}

package bg.energo.phoenix.model.enums.template;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContractTemplateSuffix {
    DATE_AND_TIME_DDMMYYYYHHMM("ddMMyyyyHH:mm"),
    DATE_AND_TIME_DDMMYYYYHHMMSS("ddMMyyyyHH:mm:ss"),
    DATE_AND_TIME_DDMMYYYY_HHMM("ddMMyyyy_HH:mm"),
    DATE_AND_TIME_DDMMYYYY_HHMMSS("ddMMyyyy_HH:mm:ss"),
    DATE_AND_TIME_DDMMYYHHMM("ddMMyyHH:mm"),
    DATE_AND_TIME_DDMMYY_HHMM("ddMMyy_HH:mm"),
    DATE_DDMMYYYY("ddMMyyyy"),
    DATE_DDMMYY("ddMMyy"),
    TIME_HHMM("HH:mm"),
    TIME_HHMMSS("HH:mm:ss"),
    TIMESTAMP("yyyy-MM-dd HH:mm:ss.SSSSSXXX");

    private final String format;
}

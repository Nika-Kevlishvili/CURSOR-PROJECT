package bg.energo.phoenix.util;

public class StringUtil {
    public static String underscoreReplacer(String value){
        return value == null ? null : value.replace("_", "\\_");
    }
}

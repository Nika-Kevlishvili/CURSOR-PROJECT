package bg.energo.phoenix.model.enums.nomenclature;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum SubActivityRegExp {
    BULGARIAN_UPPER_CASE("^[А-Я ]+$", "А-Я"),
    BULGARIAN_LOWER_CASE("^[а-я ]+$", "а-я"),
    ENGLISH_UPPER_CASE("^[A-Z ]+$", "A-Z"),
    ENGLISH_LOWER_CASE("^[a-z ]+$", "a-z"),
    ONLY_NUMBERS("^[0-9]+$", "0-9"),
    ALL("[\\S\\s\\n\\t\\r\\n\\f.]+", "[\\S\\s\\n\\t\\r\\n\\f.]+");

    private final String pattern;
    private final String basePattern;


    /**
     * Returns a pattern that matches all the patterns in the list.
     *
     * @param regExps List of patterns
     * @return A pattern that matches all the patterns in the list
     */
    public static String getPatternByRegExp(List<SubActivityRegExp> regExps) {
        if (regExps.size() == 1) {
            return regExps.get(0).getPattern();
        } else if (regExps.contains(ALL)) {
            return ALL.getPattern();
        } else {
            String pattern = "^[%s ]+$"; // in case of multiple patterns space is allowed too
            StringBuilder basePattern = new StringBuilder();
            for (SubActivityRegExp regExp : regExps) {
                basePattern.append(regExp.getBasePattern());
            }
            return String.format(pattern, basePattern);
        }
    }


    /**
     * @return A pattern that matches phone numbers
     */
    public static String getPhonePattern() {
        return "^[0-9\\-+*]+$";
    }


    /**
     * @return A pattern that matches email addresses
     */
    public static String getEmailPattern() {
        return "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    }


    /**
     * @return A pattern that matches web addresses
     */
    public static String getWebPattern() {
        return "^(https?|ftp):\\/\\/[\\w+_.-]+(\\/[\\w+_.-]+)*$";
    }
}

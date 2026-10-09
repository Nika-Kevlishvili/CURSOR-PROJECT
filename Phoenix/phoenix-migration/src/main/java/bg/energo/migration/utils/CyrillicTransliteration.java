package bg.energo.migration.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CyrillicTransliteration {

    private static final Map<Character, String> CYRILLIC_TO_LATIN_MAP = new HashMap<>() {{
        put('А', "A");
        put('а', "a");
        put('Б', "B");
        put('б', "b");
        put('В', "V");
        put('в', "v");
        put('Г', "G");
        put('г', "g");
        put('Д', "D");
        put('д', "d");
        put('Е', "E");
        put('е', "e");
        put('Ж', "ZH");
        put('ж', "zh");
        put('З', "Z");
        put('з', "z");
        put('И', "I");
        put('и', "i");
        put('Й', "Y");
        put('й', "y");
        put('К', "K");
        put('к', "k");
        put('Л', "L");
        put('л', "l");
        put('М', "M");
        put('м', "m");
        put('Н', "N");
        put('н', "n");
        put('О', "O");
        put('о', "o");
        put('П', "P");
        put('п', "p");
        put('Р', "R");
        put('р', "r");
        put('С', "S");
        put('с', "s");
        put('Т', "T");
        put('т', "t");
        put('У', "U");
        put('у', "u");
        put('Ф', "F");
        put('ф', "f");
        put('Х', "H");
        put('х', "h");
        put('Ц', "TS");
        put('ц', "ts");
        put('Ч', "CH");
        put('ч', "ch");
        put('Ш', "SH");
        put('ш', "sh");
        put('Щ', "SHT");
        put('щ', "SHT");
        put('Ъ', "A");
        put('ъ', "a");
        put('Ь', "Y");
        put('ь', "y");
        put('Ю', "YU");
        put('ю', "yu");
        put('Я', "YA");
        put('я', "ya");
    }};


    public static String transliterateToLatin(String bulgarianText) {
        StringBuilder result = new StringBuilder();
        String input = Optional.ofNullable(bulgarianText).orElse("");

        for (char c : input.toCharArray()) {
            result.append(CYRILLIC_TO_LATIN_MAP.getOrDefault(c, String.valueOf(c)));
        }

        return result.toString();
    }
}

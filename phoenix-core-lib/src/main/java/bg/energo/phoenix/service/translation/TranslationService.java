package bg.energo.phoenix.service.translation;

import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.repository.translation.TranslationRepository;
import lombok.RequiredArgsConstructor;
import org.eclipse.aether.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TranslationService {
    private final TranslationRepository translationRepository;

    private static final Map<Character, String> BULGARIAN_TO_ENGLISH = new HashMap<>();
    private static final Map<String, Character> ENGLISH_TO_BULGARIAN = new HashMap<>();


    static {
        BULGARIAN_TO_ENGLISH.put('А', "A");
        BULGARIAN_TO_ENGLISH.put('а', "a");
        BULGARIAN_TO_ENGLISH.put('Б', "B");
        BULGARIAN_TO_ENGLISH.put('б', "b");
        BULGARIAN_TO_ENGLISH.put('В', "V");
        BULGARIAN_TO_ENGLISH.put('в', "v");
        BULGARIAN_TO_ENGLISH.put('Г', "G");
        BULGARIAN_TO_ENGLISH.put('г', "g");
        BULGARIAN_TO_ENGLISH.put('Д', "D");
        BULGARIAN_TO_ENGLISH.put('д', "d");
        BULGARIAN_TO_ENGLISH.put('Е', "E");
        BULGARIAN_TO_ENGLISH.put('е', "e");
        BULGARIAN_TO_ENGLISH.put('Ж', "Zh");
        BULGARIAN_TO_ENGLISH.put('ж', "zh");
        BULGARIAN_TO_ENGLISH.put('З', "Z");
        BULGARIAN_TO_ENGLISH.put('з', "z");
        BULGARIAN_TO_ENGLISH.put('И', "I");
        BULGARIAN_TO_ENGLISH.put('и', "i");
        BULGARIAN_TO_ENGLISH.put('Й', "Y");
        BULGARIAN_TO_ENGLISH.put('й', "y");
        BULGARIAN_TO_ENGLISH.put('К', "K");
        BULGARIAN_TO_ENGLISH.put('к', "k");
        BULGARIAN_TO_ENGLISH.put('Л', "L");
        BULGARIAN_TO_ENGLISH.put('л', "l");
        BULGARIAN_TO_ENGLISH.put('М', "M");
        BULGARIAN_TO_ENGLISH.put('м', "m");
        BULGARIAN_TO_ENGLISH.put('Н', "N");
        BULGARIAN_TO_ENGLISH.put('н', "n");
        BULGARIAN_TO_ENGLISH.put('О', "O");
        BULGARIAN_TO_ENGLISH.put('о', "o");
        BULGARIAN_TO_ENGLISH.put('П', "P");
        BULGARIAN_TO_ENGLISH.put('п', "p");
        BULGARIAN_TO_ENGLISH.put('Р', "R");
        BULGARIAN_TO_ENGLISH.put('р', "r");
        BULGARIAN_TO_ENGLISH.put('С', "S");
        BULGARIAN_TO_ENGLISH.put('с', "s");
        BULGARIAN_TO_ENGLISH.put('Т', "T");
        BULGARIAN_TO_ENGLISH.put('т', "t");
        BULGARIAN_TO_ENGLISH.put('У', "U");
        BULGARIAN_TO_ENGLISH.put('у', "u");
        BULGARIAN_TO_ENGLISH.put('Ф', "F");
        BULGARIAN_TO_ENGLISH.put('ф', "f");
        BULGARIAN_TO_ENGLISH.put('Х', "H");
        BULGARIAN_TO_ENGLISH.put('х', "h");
        BULGARIAN_TO_ENGLISH.put('Ц', "Ts");
        BULGARIAN_TO_ENGLISH.put('ц', "ts");
        BULGARIAN_TO_ENGLISH.put('Ч', "Ch");
        BULGARIAN_TO_ENGLISH.put('ч', "ch");
        BULGARIAN_TO_ENGLISH.put('Ш', "Sh");
        BULGARIAN_TO_ENGLISH.put('ш', "sh");
        BULGARIAN_TO_ENGLISH.put('Щ', "Sht");
        BULGARIAN_TO_ENGLISH.put('щ', "sht");
        BULGARIAN_TO_ENGLISH.put('Ъ', "A");
        BULGARIAN_TO_ENGLISH.put('ъ', "a");
        BULGARIAN_TO_ENGLISH.put('Ь', "Y");
        BULGARIAN_TO_ENGLISH.put('ь', "y");
        BULGARIAN_TO_ENGLISH.put('Ю', "Yu");
        BULGARIAN_TO_ENGLISH.put('ю', "yu");
        BULGARIAN_TO_ENGLISH.put('Я', "Ya");
        BULGARIAN_TO_ENGLISH.put('я', "ya");


        for (Map.Entry<Character, String> entry : BULGARIAN_TO_ENGLISH.entrySet()) {
            if (entry.getValue().length() == 1 && !entry.getKey().equals('ъ')) {
                ENGLISH_TO_BULGARIAN.put(entry.getValue(), entry.getKey());
            }
        }

        ENGLISH_TO_BULGARIAN.put("Zh", 'Ж');
        ENGLISH_TO_BULGARIAN.put("zh", 'ж');
        ENGLISH_TO_BULGARIAN.put("Ts", 'Ц');
        ENGLISH_TO_BULGARIAN.put("ts", 'ц');
        ENGLISH_TO_BULGARIAN.put("Ch", 'Ч');
        ENGLISH_TO_BULGARIAN.put("ch", 'ч');
        ENGLISH_TO_BULGARIAN.put("Sh", 'Ш');
        ENGLISH_TO_BULGARIAN.put("sh", 'ш');
        ENGLISH_TO_BULGARIAN.put("Sht", 'Щ');
        ENGLISH_TO_BULGARIAN.put("sht", 'щ');
        ENGLISH_TO_BULGARIAN.put("Yu", 'Ю');
        ENGLISH_TO_BULGARIAN.put("yu", 'ю');
        ENGLISH_TO_BULGARIAN.put("Ya", 'Я');
        ENGLISH_TO_BULGARIAN.put("ya", 'я');
    }
    public String translate(String input, Language destLanguage) {
        if(StringUtils.isEmpty(input)){
            return input;
        }
        return translationRepository.translate(input,destLanguage.name());
    }

    public String translateByCharacters(String str,Language destLanguage) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        if(destLanguage.equals(Language.ENGLISH)){
            return translateBulgarianToEnglish(str);
        }
        return translateEnglishToBulgarian(str);
    }

    private  String translateBulgarianToEnglish(String str) {
        StringBuilder array = new StringBuilder();
        boolean lastCharIsUpper = false;
        char prevChar = '\0';

        for (int index = 0; index < str.length(); index++) {
            char currentChar = str.charAt(index);

            if (BULGARIAN_TO_ENGLISH.containsKey(currentChar)) {
                if (lastCharIsUpper && Character.isUpperCase(currentChar)) {
                    int lastAddedLength = BULGARIAN_TO_ENGLISH.get(prevChar).length();
                    array.delete(array.length() - lastAddedLength, array.length());

                    array.append(BULGARIAN_TO_ENGLISH.get(prevChar).toUpperCase());
                    array.append(BULGARIAN_TO_ENGLISH.get(currentChar).toUpperCase());
                } else {
                    array.append(BULGARIAN_TO_ENGLISH.get(currentChar));
                }

                prevChar = currentChar;
                lastCharIsUpper = Character.isUpperCase(currentChar);
            } else {
                array.append(currentChar);
                lastCharIsUpper = false;
            }
        }

        return array.toString();
    }
    private String translateEnglishToBulgarian(String str) {
        StringBuilder result = new StringBuilder();
        int index = 0;

        while (index < str.length()) {
            boolean matched = false;

            for (int len = 3; len >= 1; len--) {
                if (index + len <= str.length()) {
                    String substring = str.substring(index, index + len);
                    if (ENGLISH_TO_BULGARIAN.containsKey(substring)) {
                        result.append(ENGLISH_TO_BULGARIAN.get(substring));
                        index += len;
                        matched = true;
                        break;
                    }

                    if (len > 1) {
                        String lowerCase = substring.toLowerCase();
                        if (ENGLISH_TO_BULGARIAN.containsKey(lowerCase)) {
                            char bulgarianChar = ENGLISH_TO_BULGARIAN.get(lowerCase);
                            if (Character.isUpperCase(substring.charAt(0))) {
                                result.append(Character.toUpperCase(bulgarianChar));
                            } else {
                                result.append(Character.toLowerCase(bulgarianChar));
                            }
                            index += len;
                            matched = true;
                            break;
                        }

                        String upperCase = substring.toUpperCase();
                        if (ENGLISH_TO_BULGARIAN.containsKey(upperCase)) {
                            char bulgarianChar = ENGLISH_TO_BULGARIAN.get(upperCase);
                            result.append(Character.toUpperCase(bulgarianChar));
                            index += len;
                            matched = true;
                            break;
                        }

                        if (len >= 2) {
                            String titleCase = substring.substring(0, 1).toUpperCase() +
                                    substring.substring(1).toLowerCase();
                            if (ENGLISH_TO_BULGARIAN.containsKey(titleCase)) {
                                result.append(ENGLISH_TO_BULGARIAN.get(titleCase));
                                index += len;
                                matched = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!matched) {
                result.append(str.charAt(index));
                index++;
            }
        }

        return result.toString();
    }
}

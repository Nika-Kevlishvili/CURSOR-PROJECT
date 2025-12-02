package bg.energo.phoenix.util.transliteration;

import io.micrometer.common.util.StringUtils;

public abstract class BulgarianTransliterationUtil {
    public static String convertAmountToWords(int numberLv, int numberSt) {
        if (numberLv < 0) {
            numberLv = -numberLv;
        }

        if (numberSt < 0) {
            numberSt = -numberSt;
        }

        String newNumberLv;
        String newNumberSt;

        if (numberLv == 0) {
            newNumberLv = "";
        } else {
            if (numberLv >= 2000 && numberLv <= 2999) {
                newNumberLv = convertToText(numberLv).replaceFirst("два", "две") + " лв.";
            } else {
                newNumberLv = convertToText(numberLv) + " лв.";
            }
        }

        if (newNumberLv.contains("един лева")) {
            newNumberLv = newNumberLv.replaceFirst("един ", "една ");
            newNumberLv = newNumberLv.replaceFirst("една лева", "един лева");
        } else {
            newNumberLv = newNumberLv.replaceFirst("един ", "една ");
        }

        newNumberSt = numberSt == 0 ? "" : "%s %s".formatted(numberToTextSt(numberSt), " ст.");

        if (StringUtils.isBlank(newNumberLv) && StringUtils.isBlank(newNumberSt)) {
            return "нула";
        } else {
            if (!StringUtils.isBlank(newNumberLv) && !StringUtils.isBlank(newNumberSt)) {
                return "%s и %s".formatted(newNumberLv, newNumberSt);
            } else if (!StringUtils.isBlank(newNumberLv)) {
                return newNumberLv;
            } else {
                return newNumberSt;
            }
        }
    }

    private static String convertToText(int n) {
        if (n == 0) {
            return "";
        } else if (n >= 1 && n <= 19) {
            String[] arr = new String[]{
                    "един", "два", "три", "четири", "пет", "шест", "седем", "осем", "девет", "десет", "единадесет",
                    "дванадесет", "тринадесет", "четиринадесет", "петнадесет", "шестнадесет", "седемнадесет", "осемнадесет", "деветнадесет"
            };
            return arr[n - 1];
        } else if (n >= 20 && n <= 99) {
            String[] arr = new String[]{
                    "двадесет", "тридесет", "четиридесет", "петдесет", "шестдесет", "седемдесет", "осемдесет", "деветдесет"
            };
            if ((n % 10) == 0) return arr[n / 10 - 2] + convertToText(n % 10);
            else return arr[n / 10 - 2] + " и " + convertToText(n % 10);
        } else if (n >= 100 && n <= 199) {
            if (n % 100 == 0) return "сто" + convertToText(n % 100);
            if (((n % 100) <= 19) || ((n % 10) == 0)) return "сто и " + convertToText(n % 100);
            else return "сто " + convertToText(n % 100);
        } else if (n >= 200 && n <= 299) {
            if (n % 100 == 0) return "двеста" + convertToText(n % 100);
            if (((n % 100) <= 19) || ((n % 10) == 0)) return "двеста и " + convertToText(n % 100);
            else return "двеста " + convertToText(n % 100);
        } else if (n >= 300 && n <= 399) {
            if (n % 100 == 0) return "триста" + convertToText(n % 100);
            if (((n % 100) <= 19) || ((n % 10) == 0)) return "триста и " + convertToText(n % 100);
            else return "триста " + convertToText(n % 100);
        } else if (n >= 400 && n <= 999) {
            if (n % 100 == 0) return convertToText(n / 100) + "стотин" + convertToText(n % 100);
            if (((n % 100) <= 19) || ((n % 10) == 0))
                return convertToText(n / 100) + "стотин и " + convertToText(n % 100);
            else return convertToText(n / 100) + "стотин " + convertToText(n % 100);
            // 1000 - 1999
        } else if (n >= 1000 && n <= 1999) {
            if (n % 1000 == 0) return "хиляда " + convertToText(n % 1000);
            if (n % 1000 <= 99) {
                if (((n % 100) <= 19) || ((n % 10) == 0)) return "хиляда и " + convertToText(n % 1000);
                else return "хиляда " + convertToText(n % 1000);
            } else {
                if (n % 100 == 0) return "хиляда и " + convertToText(n % 1000);
                else return "хиляда " + convertToText(n % 1000);
            }
            // 2000 - 999,999
        } else {
            if (n % 1000 == 0) return convertToText(n / 1000) + " хиляди " + convertToText(n % 1000);
            if (n % 1000 <= 99) {
                if (((n % 100) <= 19) || ((n % 10) == 0))
                    return convertToText(n / 1000) + " хиляди и " + convertToText(n % 1000);
                else return convertToText(n / 1000) + " хиляди " + convertToText(n % 1000);
            } else {
                if (n % 100 == 0) return convertToText(n / 1000) + " хиляди и " + convertToText(n % 1000);
                else return convertToText(n / 1000) + " хиляди " + convertToText(n % 1000);
            }
        }
    }

    private static String numberToTextSt(int n) {
        if (n == 0) {
            return "";
        } else if (n >= 1 && n <= 19) {
            String[] arr = new String[]{
                    "една", "две", "три", "четири", "пет", "шест", "седем", "осем", "девет", "десет", "единадесет",
                    "дванадесет", "тринадесет", "четиринадесет", "петнадесет", "шестнадесет", "седемнадесет", "осемнадесет", "деветнадесет"
            };
            return arr[n - 1];
        } else {
            String[] arr = new String[]{
                    "двадесет", "тридесет", "четиридесет", "петдесет", "шестдесет", "седемдесет", "осемдесет", "деветдесет"
            };
            if ((n % 10) == 0) return arr[n / 10 - 2] + convertToText(n % 10);
            else return arr[n / 10 - 2] + " и " + convertToText(n % 10);
        }
    }
}

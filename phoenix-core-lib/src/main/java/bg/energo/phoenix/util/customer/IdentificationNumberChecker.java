package bg.energo.phoenix.util.customer;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class IdentificationNumberChecker {


    public static Boolean checkIdentificationNumber(String number) {
        if (number == null || number.equals("")) {
            return false;
        }
        if (number.length()==12){
           return StringUtils.isAlphanumeric(number)&& StringUtils.isNumeric(number.substring(0,8));
        }
        ArrayList<Integer> numbers;
        try {
            numbers = getNumberArrayFromString(number);
        } catch (Exception e) {
            return false;
        }
        switch (numbers.size()) {
            case 9://UIC
                return checkNineDigitUICNumber(numbers);
            case 10://personalNumber
                return personalNumber(numbers);
            case 13://UIC
                return checkThirteenDigitUICNumber(numbers);
        }
        return false;
    }

    public static Boolean checkNineDigitUICNumber(ArrayList<Integer> numbers) {
        int sum = 0;
        int division = 0;
        for (int i = 0; i < numbers.size() - 1; i++) {
            sum += (i + 1) * numbers.get(i);
        }
        division = sum % 11;
        if (division != 10 && division == numbers.get(8)) {
            return true;
        } else if (division == 10) {
            int innerSum = 3 * numbers.get(0) +
                    4 * numbers.get(1) +
                    5 * numbers.get(2) +
                    6 * numbers.get(3) +
                    7 * numbers.get(4) +
                    8 * numbers.get(5) +
                    9 * numbers.get(6) +
                    10 * numbers.get(7);
            int innerDivision = innerSum % 11;
            if (innerDivision != 10 && innerDivision == numbers.get(8)) {
                return true;
            } else if (innerDivision == 10 && numbers.get(8) == 0) {
                return true;
            } else return false;
        }
        return false;
    }

    public static Boolean checkThirteenDigitUICNumber(ArrayList<Integer> numbers) {
        int sum = 2 * numbers.get(8) +
                7 * numbers.get(9) +
                3 * numbers.get(10) +
                5 * numbers.get(11);
        int division = sum % 11;
        if (division != 10 && division == numbers.get(12)) {
            return true;
        } else if (division == 10) {
            int innerSum = 4 * numbers.get(8) +
                    9 * numbers.get(9) +
                    5 * numbers.get(10) +
                    7 * numbers.get(11);
            int innerDivision = innerSum % 11;
            if (innerDivision != 10 && innerDivision == numbers.get(12)) {
                return true;
            }
        }
        return false;
    }

    public static Boolean personalNumber(ArrayList<Integer> numbers) {
        Boolean isValid = false;
        int sum = 2 * numbers.get(0) +
                4 * numbers.get(1) +
                8 * numbers.get(2) +
                5 * numbers.get(3) +
                10 * numbers.get(4) +
                9 * numbers.get(5) +
                7 * numbers.get(6) +
                3 * numbers.get(7) +
                6 * numbers.get(8);
        int division = sum % 11;
        if (division != 10 && division == numbers.get(9)) {
            isValid= true;
        } else if (division == 10 && numbers.get(9) == 0) {
            isValid= true;
        }
        if(isValid){
            return true;
        } else return personalNumberOfForeigner(numbers);
    }

    public static Boolean personalNumberOfForeigner(ArrayList<Integer> numbers) {
        int sum = 21 * numbers.get(0) +
                19 * numbers.get(1) +
                17 * numbers.get(2) +
                13 * numbers.get(3) +
                11 * numbers.get(4) +
                9 * numbers.get(5) +
                7 * numbers.get(6) +
                3 * numbers.get(7) +
                numbers.get(8);
        int division = sum % 10;
        if (division == numbers.get(9)) {
            return true;
        }
        return false;
    }

    public static ArrayList<Integer> getNumberArrayFromString(String number) {
        ArrayList<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < number.length(); i++) {
            numbers.add(Integer.valueOf(String.valueOf(number.charAt(i))));
        }
        return numbers;
    }
}

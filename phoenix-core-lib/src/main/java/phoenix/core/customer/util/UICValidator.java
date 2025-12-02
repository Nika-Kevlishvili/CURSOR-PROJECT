package phoenix.core.customer.util;


import phoenix.core.customer.model.customAnotations.UICDefaultValidator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UICValidator implements ConstraintValidator<UICDefaultValidator, String> {


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(IdentificationNumberChecker.checkIdentificationNumber(value)){
            return true;
        } else return false;
        //Levans Logic
/*        long num;
        try {
           num  = Long.parseLong(value);
        }catch (NumberFormatException e){

            return false;
        }
        int[] digits = new int[value.length()];
        for (int i = value.length()-1; i >=0; i--) {
            int digit =(int) (num % 10);
            digits[i] = digit;
            num = num / 10;
        }
        if (value.length() == 9) {

            int ninthDigit = digits[8];
            int sum = 0;

            for (int i = 0; i < 7; i++) {

                sum += digits[i]*(i+1);

            }
            int rem = sum % 11;

            if (rem != 10) {
                return ninthDigit == rem;
            }
            sum = 0;

            for (int i = 0; i < 7; i++) {
                sum += digits[i] * (3 + i);
            }
            rem = sum % 11;
            if (rem != 10) {
                return ninthDigit == rem;
            }
            return ninthDigit == 0;
        }
        if (value.length() == 13) {
            int sum = 2 * digits[8] + 7 * digits[9] + 3 * digits[10] + 5 * digits[11];
            int rem = sum % 11;
            int thirteenthDigit = digits[12];
            if (rem != 10) {
                return thirteenthDigit == rem;
            }
            sum = 4 * digits[8] + 9 * digits[9] + 5 * digits[10] + 7 * digits[11];
            rem = sum % 11;
            if (rem != 10) {
                return thirteenthDigit == rem;
            }
            return thirteenthDigit == 0;
        }
        if (value.length() == 10) {
            int teenthDigit = digits[9];
            int sum = 2 * digits[0] + 4 * digits[1] + 8 * digits[2] + 5 * digits[3] + 10 * digits[4]
                    + 9 * digits[5] + 7 * digits[6] + 3 * digits[7] + 6 * digits[8];
            int rem = sum % 11;
            if (rem != 10) {
                return teenthDigit == rem;
            }
            return teenthDigit == 0;
        }
        if (value.length() == 12) {
            int sum = 21 * digits[0] + 19 * digits[1] + 17 * digits[2] + 13 * digits[3] + 11 * digits[4]
                    + 9 * digits[5] + 7 * digits[6] + 3 * digits[7] + digits[8];
            int rem = sum % 10;
            return digits[9] == rem;

        }
        return false;*/
    }
}

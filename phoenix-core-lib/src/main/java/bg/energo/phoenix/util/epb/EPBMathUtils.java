package bg.energo.phoenix.util.epb;

import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.Set;

public class EPBMathUtils {

    public static boolean isMathExpressionValid(String expression, Set<String> variables) {
        return new ExpressionBuilder(expression).variables(variables).build().validate(false).isValid();
    }

    public static double evaluateMathExpression(String expression) {
        return new ExpressionBuilder(expression).build().evaluate();
    }

}

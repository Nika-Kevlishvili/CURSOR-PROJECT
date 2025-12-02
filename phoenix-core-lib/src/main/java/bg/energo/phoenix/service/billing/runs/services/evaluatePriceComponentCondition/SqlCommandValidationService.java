package bg.energo.phoenix.service.billing.runs.services.evaluatePriceComponentCondition;

import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class SqlCommandValidationService {
    private static final List<String> SQL_COMMANDS =
            List.of("ALTER", "CREATE", "DELETE", "DROP","LIKE", "EXECUTE", "INSERT", "INTO", "MERGE", "SELECT", "UPDATE", "TRUNCATE");

    public boolean containsSqlCommands(String condition) {
        condition = condition.toUpperCase();
        for (String command : SQL_COMMANDS) {
            if (condition.contains(command)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkWithoutSpacesCondition(String condition) {
        return containsSqlCommands(condition.replace(" ", ""));
    }

    public boolean checkWithoutVariableSymbolCondition(String condition) {
        return containsSqlCommands(condition.replace("$", ""));
    }

    public boolean checkWithoutVariableSymbolAnSpacesCondition(String condition) {
        String result = condition.replace("$", "");
        result = result.replace(" ", "");
        return containsSqlCommands(result);
    }


}

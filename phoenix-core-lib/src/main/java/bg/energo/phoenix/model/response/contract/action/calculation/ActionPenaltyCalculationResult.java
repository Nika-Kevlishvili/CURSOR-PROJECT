package bg.energo.phoenix.model.response.contract.action.calculation;

import java.math.BigDecimal;
import java.util.List;

public record ActionPenaltyCalculationResult(BigDecimal amount, // penalty amount evaluated from the formula
                                             Long currencyId, // currency id of the penalty amount
                                             Boolean isAutomaticClaimSelectedInPenalty, // flag that indicates if the automatic claim is selected in the penalty
                                             List<String> infoErrorMessages) { // list of info error messages

    // enforcing passing informational error messages is intended.
    // This is done to avoid losing the messages when transferring the result through the layers of the application.

    public static ActionPenaltyCalculationResult empty(List<String> infoErrorMessages) {
        return new ActionPenaltyCalculationResult(
                null,
                null,
                null,
                infoErrorMessages
        );
    }

    public static ActionPenaltyCalculationResult empty(String infoErrorMessage) {
        return new ActionPenaltyCalculationResult(
                null,
                null,
                null,
                List.of(infoErrorMessage)
        );
    }

    public static ActionPenaltyCalculationResult empty(List<String> infoErrorMessages, String extraMessage) {
        infoErrorMessages.add(extraMessage);
        return new ActionPenaltyCalculationResult(
                null,
                null,
                null,
                infoErrorMessages
        );
    }

    public boolean isEmpty() {
        return amount == null || currencyId == null;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

}

package bg.energo.phoenix.model.enums.product.term.terms.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TermsListColumns {
    ID("id"),
    NAME("name"),
    NO_INTEREST_ON_OVERDUE_DEBTS("noInterestOnOverdueDebts"),
    AVAILABLE("available"),
    DATE_OF_CREATION("dateOfCreation");

    private final String value;
}

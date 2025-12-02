package bg.energo.phoenix.model.enums.product.term.termsGroup;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TermsGroupListColumns {
    ID("id"),
    NAME("tgd.name"),
    NO_INTEREST_ON_OVERDUE_DEBTS("t.noInterestOnOverdueDebts"),
    STATUS("status"),
    DATE_OF_CREATION("createDate");

    private final String value;
}

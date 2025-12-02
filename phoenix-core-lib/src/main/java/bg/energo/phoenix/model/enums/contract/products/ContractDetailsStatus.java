package bg.energo.phoenix.model.enums.contract.products;

import java.util.List;
import java.util.function.Predicate;

public enum ContractDetailsStatus {
    DRAFT(x -> x.equals(ContractDetailsSubStatus.DRAFT)),
    READY(x -> x.equals(ContractDetailsSubStatus.READY)),
    SIGNED(x -> List.of(
            ContractDetailsSubStatus.SIGNED_BY_CUSTOMER,
            ContractDetailsSubStatus.SIGNED_BY_EPRES,
            ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES,
            ContractDetailsSubStatus.SPECIAL_PROCESSES
    ).contains(x)),
    ENTERED_INTO_FORCE(x -> x.equals(ContractDetailsSubStatus.AWAITING_ACTIVATION)),
    ACTIVE_IN_TERM(x -> List.of(
            ContractDetailsSubStatus.DELIVERY,
            ContractDetailsSubStatus.IN_TERMINATION_BY_EPRES,
            ContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER,
            ContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA
    ).contains(x)),
    ACTIVE_IN_PERPETUITY(x -> List.of(
            ContractDetailsSubStatus.DELIVERY,
            ContractDetailsSubStatus.IN_TERMINATION_BY_EPRES,
            ContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER,
            ContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA
    ).contains(x)),
    TERMINATED(x -> List.of(
            ContractDetailsSubStatus.FROM_CUSTOMER_WITH_NOTICE,
            ContractDetailsSubStatus.FROM_CUSTOMER_WITHOUT_NOTICE,
            ContractDetailsSubStatus.FROM_EPRES_WITH_NOTICE,
            ContractDetailsSubStatus.FROM_EPRES_WITHOUT_NOTICE,
            ContractDetailsSubStatus.BY_MUTUAL_AGREEMENT,
            ContractDetailsSubStatus.NEW_CONTRACT_SIGNED,
            ContractDetailsSubStatus.EXPIRED,
            ContractDetailsSubStatus.DELETED_ID_DECEASED_PERSON,
            ContractDetailsSubStatus.FORCE_MAJEURE,
            ContractDetailsSubStatus.CONTRACT_IS_NOT_ACTIVE,
            ContractDetailsSubStatus.ALL_PODS_ARE_DEACTIVATED
    ).contains(x)),
    CANCELLED(x -> List.of(
            ContractDetailsSubStatus.UNSENT_TO_CUSTOMER,
            ContractDetailsSubStatus.INVALID_DATA,
            ContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_EPRES,
            ContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_CUSTOMER,
            ContractDetailsSubStatus.TEST
    ).contains(x)),
    CHANGED_WITH_AGREEMENT(x -> List.of(
            ContractDetailsSubStatus.UNSENT_TO_CUSTOMER,
            ContractDetailsSubStatus.INVALID_DATA,
            ContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_EPRES,
            ContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_CUSTOMER,
            ContractDetailsSubStatus.TEST
    ).contains(x));

    private final Predicate<ContractDetailsSubStatus> subStatusPredicate;

    ContractDetailsStatus(Predicate<ContractDetailsSubStatus> subStatusPredicate) {
        this.subStatusPredicate = subStatusPredicate;
    }

    public boolean isCorrectSubStatus(ContractDetailsSubStatus status) {
        return subStatusPredicate.test(status);
    }
}

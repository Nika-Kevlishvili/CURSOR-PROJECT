package bg.energo.phoenix.model.enums.contract.service;

import java.util.List;
import java.util.function.Predicate;

public enum ServiceContractDetailStatus {
    DRAFT(x -> x.equals(ServiceContractDetailsSubStatus.DRAFT)),
    READY(x -> x.equals(ServiceContractDetailsSubStatus.READY)),
    SIGNED(x -> List.of
            (ServiceContractDetailsSubStatus.SIGNED_BY_CUSTOMER,
                    ServiceContractDetailsSubStatus.SIGNED_BY_EPRES,
                    ServiceContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES,
                    ServiceContractDetailsSubStatus.SPECIAL_PROCESSES).contains(x)),
    ENTERED_INTO_FORCE(x -> x.equals(ServiceContractDetailsSubStatus.AWAITING_ACTIVATION)),
    ACTIVE_IN_TERM(x -> List.of(
            ServiceContractDetailsSubStatus.DELIVERY,
            ServiceContractDetailsSubStatus.IN_TERMINATION_BY_EPRES,
            ServiceContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER,
            ServiceContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA).contains(x)),
    ACTIVE_IN_PERPETUITY(x -> List.of(
            ServiceContractDetailsSubStatus.DELIVERY,
            ServiceContractDetailsSubStatus.IN_TERMINATION_BY_EPRES,
            ServiceContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER,
            ServiceContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA
    ).contains(x)),
    TERMINATED(x -> List.of(
            ServiceContractDetailsSubStatus.FROM_CUSTOMER_WITH_NOTICE,
            ServiceContractDetailsSubStatus.FROM_CUSTOMER_WITHOUT_NOTICE,
            ServiceContractDetailsSubStatus.FROM_EPRES_WITH_NOTICE,
            ServiceContractDetailsSubStatus.FROM_EPRES_WITHOUT_NOTICE,
            ServiceContractDetailsSubStatus.BY_MUTUAL_AGREEMENT,
            ServiceContractDetailsSubStatus.NEW_CONTRACT_SIGNED,
            ServiceContractDetailsSubStatus.EXPIRED,
            ServiceContractDetailsSubStatus.DELETED_ID_DECEASED_PERSON,
            ServiceContractDetailsSubStatus.FORCE_MAJEURE,
            ServiceContractDetailsSubStatus.CONTRACT_IS_NOT_ACTIVE
    ).contains(x)),
    CANCELLED(x -> List.of(
            ServiceContractDetailsSubStatus.UNSENT_TO_CUSTOMER,
            ServiceContractDetailsSubStatus.INVALID_DATA,
            ServiceContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_EPRES,
            ServiceContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_CUSTOMER,
            ServiceContractDetailsSubStatus.TEST
    ).contains(x)),
    CHANGED_WITH_AGREEMENT(x -> List.of(
            ServiceContractDetailsSubStatus.UNSENT_TO_CUSTOMER,
            ServiceContractDetailsSubStatus.INVALID_DATA,
            ServiceContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_EPRES,
            ServiceContractDetailsSubStatus.REFUSAL_TO_SIGN_BY_CUSTOMER,
            ServiceContractDetailsSubStatus.TEST
    ).contains(x));

    Predicate<ServiceContractDetailsSubStatus> subStatusPredicate;

    ServiceContractDetailStatus(Predicate<ServiceContractDetailsSubStatus> subStatusPredicate) {
        this.subStatusPredicate = subStatusPredicate;
    }

    public boolean isCorrectSubStatus(ServiceContractDetailsSubStatus status) {
        return subStatusPredicate.test(status);
    }
}

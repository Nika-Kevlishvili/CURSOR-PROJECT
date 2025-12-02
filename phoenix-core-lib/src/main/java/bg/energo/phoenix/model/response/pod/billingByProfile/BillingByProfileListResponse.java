package bg.energo.phoenix.model.response.pod.billingByProfile;

import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;

import java.time.LocalDate;

public interface BillingByProfileListResponse {

    Long getId();
    String getPodIdentifier();
    LocalDate getPeriodFrom();
    LocalDate getPeriodTo();
    String getProfileName();
    PeriodType getPeriodType();
    String getInvoiced();
    BillingByProfileStatus getStatus();
    LocalDate getCreateDate();

}

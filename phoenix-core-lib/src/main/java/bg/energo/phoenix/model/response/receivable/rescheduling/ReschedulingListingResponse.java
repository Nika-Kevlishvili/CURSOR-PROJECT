package bg.energo.phoenix.model.response.receivable.rescheduling;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;

import java.time.LocalDate;

public interface ReschedulingListingResponse {
    Long getId();
    String getReschedulingNumber();
    String getCustomer();
    ReschedulingStatus getReschedulingStatus();
    EntityStatus getStatus();
    Integer getNumberOfInstallments();
    Integer getAmountOfTheInstallment();
    Short getInstallmentDueDateOfTheMonth();
    LocalDate getCreationDate();
    Boolean getReversed();
}

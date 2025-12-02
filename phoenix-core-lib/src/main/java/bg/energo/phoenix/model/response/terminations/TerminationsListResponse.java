package bg.energo.phoenix.model.response.terminations;

import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationStatus;

import java.time.LocalDate;

public interface TerminationsListResponse {
    Long getId();

    String getName();

    Boolean getAutoTermination();

    Boolean getNoticeDue();

    String getAvailable();

    LocalDate getCreateDate();

    TerminationStatus getStatus();
}

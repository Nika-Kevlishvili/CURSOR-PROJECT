package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyStatus;

import java.time.LocalDate;

public interface CancellationOfDisconnectionOfThePowerSupplyListingMiddleResponse {
    Long getId();

    String getCancellationNumber();

    LocalDate getCancellationDate();

    String getRequestForDisconnectionNumber();
    Integer getNumberOfPods();

    EntityStatus getEntityStatus();

    CancellationOfDisconnectionOfThePowerSupplyStatus getCancellationStatus();

}

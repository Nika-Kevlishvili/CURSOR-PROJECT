package bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class CancellationOfDisconnectionOfThePowerSupplyResponse {
    private Long id;

    private String number;

    private EntityStatus entityStatus;

    private LocalDate date;

    private CancellationOfDisconnectionOfThePowerSupplyStatus status;
}



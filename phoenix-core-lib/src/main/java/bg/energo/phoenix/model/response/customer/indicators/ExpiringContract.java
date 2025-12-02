package bg.energo.phoenix.model.response.customer.indicators;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpiringContract {

    private String contractNumber;
    private LocalDate contractTermEndDate;
    private String contractId;
    private String contractType;
}
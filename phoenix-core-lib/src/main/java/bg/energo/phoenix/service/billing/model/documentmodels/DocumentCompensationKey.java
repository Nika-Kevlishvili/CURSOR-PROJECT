package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentCompensationKey {
    private String documentNumber;
    private LocalDate documentDate;
    private LocalDate period;
    private String currency;
}

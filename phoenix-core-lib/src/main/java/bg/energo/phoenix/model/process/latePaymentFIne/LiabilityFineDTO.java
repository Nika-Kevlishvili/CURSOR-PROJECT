package bg.energo.phoenix.model.process.latePaymentFIne;

import io.avaje.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityFineDTO {
    private Long id;
    @Nullable
    private BigDecimal lfp;
}

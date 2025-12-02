package bg.energo.phoenix.model.response.priceComponentGroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceComponentGroupVersion {

    private Long version;

    private LocalDate startDate;

    private LocalDate endDate;

}

package bg.energo.phoenix.model.response.priceParameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceParameterDetailsVersionInfo {
    private Long id;
    private String priceParameterPreviewDisplayName;
    private Long version;
    private LocalDateTime createDate;
}

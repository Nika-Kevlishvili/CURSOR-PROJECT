package bg.energo.phoenix.model.request.pod.billingByProfile.data;

import bg.energo.phoenix.model.customAnotations.product.price.ValidFractionalPart;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingByProfileDataUpdateRequest {

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @NotNull(message = "periodFrom-periodFrom must not be null;")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime periodFrom;

    @ValidFractionalPart(value = "${validatedValue}", fieldName = "Value", fraction = 10, nullable = true)
    private BigDecimal value;

    @NotNull(message = "shiftedHour-Shifted hour value must not be null;")
    private Boolean shiftedHour;

}

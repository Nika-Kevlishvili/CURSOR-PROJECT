package bg.energo.phoenix.model.request.product.price.priceParameter;

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
@AllArgsConstructor
@NoArgsConstructor
public class CreatePriceParameterDetailRequest {

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @NotNull(message = "periodFrom-periodFrom must not be null")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime periodFrom;

    //@Min(value = 0,message = "price-Price value must be positive")
    @NotNull(message = "price-Price must not be null")
    @ValidFractionalPart(value = "${validatedValue}", fieldName = "Price", fraction = 10)
    private BigDecimal price;

    @NotNull(message = "shiftedHour-Shifted hour value must not be null")
    private Boolean shiftedHour;

}

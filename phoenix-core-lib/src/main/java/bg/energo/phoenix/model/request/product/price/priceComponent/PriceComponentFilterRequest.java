package bg.energo.phoenix.model.request.product.price.priceComponent;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.price.priceComponent.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@Data
@PromptSymbolReplacer
public class PriceComponentFilterRequest {
    @NotNull(message = "page-Page must not be null")
    @Min(value = 0,message = "page-Page should be positive")
    Integer page;
    @NotNull(message = "size-Size must not be null")
    @Range(min = 1, max = 100, message = "size-size should be between 1 and 100!")
    Integer size;
    String prompt;
    PriceComponentSearchFields searchFields;
    @NotNull(message = "numberType-numberType you can not provide null value explicitly!")
    List<NumberType> numberType = new ArrayList<>();
    @NotNull(message = "valueTypeIds-valueTypeIds you can not provide null value explicitly!")
    List<Long> valueTypeIds = new ArrayList<>();
    @NotNull(message = "priceTypeIds-priceTypeIds you can not provide null value explicitly!")
    List<Long> priceTypeIds = new ArrayList<>();
    @NotNull(message = "availability-availability you can not provide null value explicitly!")
    PriceComponentAvailability availability = PriceComponentAvailability.ALL;
    @NotNull(message = "conditions-conditions you can not provide null value explicitly!")
    List<PriceComponentConditions> conditions = new ArrayList<>();
    @NotNull(message = "sortBy-sortBy you can not provide null value explicitly!")
    PriceComponentSortColumn sortBy=PriceComponentSortColumn.ID;
    @NotNull(message = "sortDirection-sortDirection you can not provide null value explicitly!")
    Sort.Direction sortDirection = Sort.Direction.ASC;
}

package bg.energo.phoenix.model.request.customer.unwantedCustomer;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerFilterField;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerSortField;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * {@link #page} page parameter for pagination
 * {@link #size} page parameter for pagination
 * {@link #prompt} search key for searching in the database
 * {@link #filterField} filter fields for unwanted customer list
 * {@link #reasonId} unwanted customer reason nomenclature id
 * {@link #sortField} sort fields for unwanted customer list
 * {@link #direction} direction for unwanted customer list
 */
@Data
@PromptSymbolReplacer
public class UnwantedCustomerFilterRequest {

    @NotNull(message = "page-Page shouldn't be null;")
    Integer page;

    @NotNull(message = "size-Size shouldn't be null;")
    Integer size;

    String prompt;

    UnwantedCustomerFilterField filterField;

    List<Long> reasonId;

    UnwantedCustomerSortField sortField;

    Sort.Direction direction;

}

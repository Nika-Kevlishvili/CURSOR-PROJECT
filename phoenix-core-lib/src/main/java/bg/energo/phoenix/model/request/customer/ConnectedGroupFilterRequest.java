package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.enums.GCCSortBy;
import bg.energo.phoenix.model.enums.customer.GCCSearchFields;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ConnectedGroupFilterRequest {

    private GCCSearchFields searchField;

    private String searchValue;

    @NotNull(message = "connectionId-Connected Group: Connection Type id can not be null")
    private List<Long> connectionId = new ArrayList<>();

    @Min(value = 0, message = "customerCountFrom-Connected Group: Page size can not be less than 0")
    private Long customerCountFrom;

    @Min(value = 0, message = "customerCountTo-Connected Group: Page size can not be less than 0")
    private Long customerCountTo;

    @NotNull(message = "page-Connected Group: Page can not be null")
    private Integer page = 0;

    @NotNull(message = "size-Connected Group: Page size can not be null")
    @Min(value = 1, message = "size-Connected Group: Page size can not be less than 1")
    private Integer size = 25;

    @NotNull(message = "size-Connected Group: sort by can not be null")
    private GCCSortBy sortBy = GCCSortBy.ID;

    @NotNull(message = "size-Connected Group: sort direction can not be null")
    private Sort.Direction direction = Sort.Direction.DESC;

    @NotNull(message = "size-Connected Group: managersDirection can not be null")
    private Sort.Direction managersDirection = Sort.Direction.DESC;

}

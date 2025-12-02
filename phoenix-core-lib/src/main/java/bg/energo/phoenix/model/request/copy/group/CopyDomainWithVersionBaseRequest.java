package bg.energo.phoenix.model.request.copy.group;

import bg.energo.phoenix.model.enums.product.product.CopyDomainWithVersionBasedRequestFilter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class CopyDomainWithVersionBaseRequest {

    @Size(min = 1, max = 512, message = "prompt-prompt must be between {min} and {max} characters;")
    private String prompt;

    @NotNull(message = "page-page is required;")
    @Min(value = 0)
    private Integer page = 0;

    @NotNull(message = "size-size is required;")
    @Range(min = 1, max = 100)
    private Integer size = 25;

    private CopyDomainWithVersionBasedRequestFilter filter;

}

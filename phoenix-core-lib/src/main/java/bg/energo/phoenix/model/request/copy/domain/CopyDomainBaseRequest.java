package bg.energo.phoenix.model.request.copy.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CopyDomainBaseRequest {

    @Size(min = 1, max = 512, message = "prompt-Prompt does not match the allowed length;")
    private String prompt;

    @NotNull(message = "page-page should not be null;")
    @Min(value = 0)
    private Integer page;

    @NotNull(message = "size-size should not be null;")
    @Range(min = 1, max = 100, message = "page-page size must be between 1_100;")
    private Integer size;

}

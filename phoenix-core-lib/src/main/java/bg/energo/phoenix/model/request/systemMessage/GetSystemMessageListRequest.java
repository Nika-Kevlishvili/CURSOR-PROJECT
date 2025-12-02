package bg.energo.phoenix.model.request.systemMessage;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Param {@link #page} page integer for pagination
 * @Param {@link #size} size of the page for pagination
 * @Param {@link #prompt} search keyword for filtration of the system massage list
 */
@Data
public class GetSystemMessageListRequest {
    @NotNull(message = "page-shouldn't be null")
    Integer page;
    @NotNull(message = "size-shouldn't be null")
    Integer size;
    String prompt;
}

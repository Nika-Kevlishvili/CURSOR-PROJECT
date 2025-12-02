package bg.energo.phoenix.model.request.activity;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.activity.ActivityListColumns;
import bg.energo.phoenix.model.enums.activity.ActivitySearchFields;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class ActivityListRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<Long> subActivityIds;

    private List<String> connectionTypes;

    private ActivityListColumns sortBy;

    private ActivitySearchFields searchBy;

    private Sort.Direction direction;

}

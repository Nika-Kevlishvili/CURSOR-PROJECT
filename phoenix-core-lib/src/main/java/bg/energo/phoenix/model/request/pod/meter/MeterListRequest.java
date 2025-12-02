package bg.energo.phoenix.model.request.pod.meter;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.pod.meter.MeterSearchField;
import bg.energo.phoenix.model.enums.pod.meter.MeterTableColumn;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@PromptSymbolReplacer
@NoArgsConstructor
@AllArgsConstructor
public class MeterListRequest {

    @NotNull(message = "page-Page size must not be null;")
    private int page;

    @NotNull(message = "size-Size must not be null;")
    private int size;

    @Size(min = 1, message = "prompt-Prompt should contain minimum 1 characters;")
    private String prompt;

    private List<Long> gridOperatorIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate installmentFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate installmentTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate removeFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate removeTo;

    private MeterSearchField searchBy;

    private MeterTableColumn sortBy;

    private Sort.Direction sortDirection;

}

package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.service.ServiceConsumptionPurpose;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.list.IndividualServiceOption;
import bg.energo.phoenix.model.enums.product.service.list.ServiceSearchField;
import bg.energo.phoenix.model.enums.product.service.list.ServiceTableColumn;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class ServiceListRequest {

    @NotNull(message = "page-Page size must not be null;")
    private int page;

    @NotNull(message = "size-Size must not be null;")
    private int size;

    @Size(min = 1, message = "prompt-Prompt should contain minimum 1 characters;")
    private String prompt;

    private ServiceSearchField searchBy;

    private ServiceTableColumn sortBy;

    private Sort.Direction sortDirection;

    private List<Long> serviceGroupIds;

    private List<Long> serviceTypeIds;

    private List<ServiceDetailStatus> serviceDetailStatuses;

    private List<String> serviceContractTermNames;

    private List<Long> salesChannelsIds;

    private Boolean globalSalesChannel;

    private List<Long> segmentIds;

    private Boolean globalSegment;

    private Set<ServiceConsumptionPurpose> consumptionPurposes;

    private IndividualServiceOption individualServiceOption;

    private Boolean excludeOldVersions;

}

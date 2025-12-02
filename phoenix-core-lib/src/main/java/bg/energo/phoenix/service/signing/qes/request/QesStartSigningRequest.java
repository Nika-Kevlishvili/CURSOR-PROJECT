package bg.energo.phoenix.service.signing.qes.request;

import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.QesSearchFilter;
import bg.energo.phoenix.model.enums.template.QesSigningStatus;
import bg.energo.phoenix.model.enums.template.QesStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QesStartSigningRequest {
    private List<Long> documentIds;
    private List<Long> excludedIds;
    private boolean allSelected;
    private String sessionId;

    private List<QesStatus> status;
    private List<QesSigningStatus> signingStatuses;
    private List<ContractTemplatePurposes> purposes;
    private List<Long> productIds;
    private List<Long> serviceIds;
    private List<Long> podIds;
    private List<Long> segments;
    private List<Long> saleChannels;
    private List<String> createdBy;
    private LocalDateTime updatedFrom;
    private LocalDateTime updatedTo;
    private QesSearchFilter searchFilter;
    private String prompt;

}

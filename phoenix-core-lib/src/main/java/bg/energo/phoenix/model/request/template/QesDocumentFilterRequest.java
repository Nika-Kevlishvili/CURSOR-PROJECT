package bg.energo.phoenix.model.request.template;

import bg.energo.phoenix.model.enums.template.*;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
@Data
public class QesDocumentFilterRequest {
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
   private Integer page;
   private Integer size;
   private QesSortBy sortBy;
   private Sort.Direction direction;
}

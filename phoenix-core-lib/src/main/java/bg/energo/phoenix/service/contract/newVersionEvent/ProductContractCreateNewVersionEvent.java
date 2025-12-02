package bg.energo.phoenix.service.contract.newVersionEvent;

import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContext;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductContractCreateNewVersionEvent {
    private ProductDetails currentProductDetails;
    private List<String> exceptionMessagesContext;
    private Terms productContractValidTerm;
    private ProductDetails productDetail;
    private Long productRelatedContractId;
    private Integer productRelatedContractVersion;
    private Long productRelatedContractCustomerDetailId;
    private Boolean hasEditLockedPermission;
    private SecurityContext context;

}

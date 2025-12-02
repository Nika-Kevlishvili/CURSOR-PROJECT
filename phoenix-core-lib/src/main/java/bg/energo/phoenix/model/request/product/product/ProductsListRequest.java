package bg.energo.phoenix.model.request.product.product;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.PurposeOfConsumption;
import bg.energo.phoenix.model.enums.product.product.list.ProductListColumns;
import bg.energo.phoenix.model.enums.product.product.list.ProductListIndividualProduct;
import bg.energo.phoenix.model.enums.product.product.list.ProductParameterFilterField;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class ProductsListRequest {
    @NotNull(message = "page-Page shouldn't be null")
    private Integer page;
    @NotNull(message = "size-Size shouldn't be null")
    private Integer size;
    private String prompt;
    private ProductParameterFilterField searchBy;
    private Set<Long> filterProductGroups;
    private List<ProductDetailStatus> status;
    private Set<Long> filterProductTypes;
    private Set<String> filterContractTerms;
    private Set<Long> filterSalesChannels;
    private Set<Long> filterSegments;
    private Set<PurposeOfConsumption> filterConsumptionTypes;
    private ProductListColumns sortBy;
    private ProductListIndividualProduct individualProduct;
    private Boolean globalSalesChannel;
    private Boolean globalSegment;
    private Boolean excludePastVersion;
    private Sort.Direction sortDirection;
}

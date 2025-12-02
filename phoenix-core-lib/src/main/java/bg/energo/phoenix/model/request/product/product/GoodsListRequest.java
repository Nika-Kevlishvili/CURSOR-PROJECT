package bg.energo.phoenix.model.request.product.product;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsSearchField;
import bg.energo.phoenix.model.enums.product.goods.GoodsTableColumn;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class GoodsListRequest {

    @NotNull(message = "page-Page size must not be null")
    private int page;

    @NotNull(message = "size-Size must not be null")
    private int size;

    @Size(min = 1,message = "prompt-Prompt should contain minimum 1 characters")
    private String prompt;

    private GoodsSearchField searchBy;

    private GoodsTableColumn sortBy;

    private Sort.Direction sortDirection;

    private List<GoodsDetailStatus> goodsDetailStatuses;

    private List<Long> groupIds;

    private List<Long> supplierIds;

    private List<Long> salesChannelsIds;

    private List<Long> segmentIds;

    private Boolean globalSalesChannel;

    private Boolean globalSegment;

    private Boolean excludeOldVersions;

}

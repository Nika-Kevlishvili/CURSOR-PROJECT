package bg.energo.phoenix.model.request.template;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.product.product.PurposeOfConsumption;
import bg.energo.phoenix.model.enums.template.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.util.List;

public record TemplateListingRequest(
        @NotNull(message = "page-Page must not be null;")
        Integer page,
        @NotNull(message = "size-Size must not be null;")
        Integer size,
        boolean excludeOldVersions,
        boolean excludeFutureVersions,
        List<ContractTemplateType> types,
        String prompt,
        TemplateSearchBy searchBy,
        List<ContractTemplateFileFormat> outputFileFormats,
        List<ContractTemplatePurposes> templatePurposes,
        List<ContractTemplateSigning> fileSignings,
        List<ContractTemplateLanguage> languages,
        List<CustomerType> customerTypes,
        List<ContractTemplateStatus> statuses,
        List<PurposeOfConsumption> consumptionPurposes,
        Boolean defaultGoodsOrderDocument,
        Boolean defaultGoodsOrderEmail,
        Boolean defaultLatePaymentFineDocument,
        Boolean defaultLatePaymentFineEmail,
        Sort.Direction sortDirection,
        TemplateSortBy sortBy


) {
    public enum TemplateSearchBy {
        ALL,
        NAME,
        ID
    }

    @Getter
    @AllArgsConstructor
    public enum TemplateSortBy {
        NAME("name"),
        TYPE("type"),
        TEMPLATE_PURPOSE("purpose"),
        FILE_SIGNING("fileSignings"),
        CREATION_DATE("createDate"),
        OUTPUT_FILE_FORMAT("outputFileFormats"),
        LANGUAGE("language");

        private final String columnName;
    }
}

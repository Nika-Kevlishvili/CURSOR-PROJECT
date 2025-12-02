package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.entity.nomenclature.product.*;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductTypes;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroup;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroup;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroup;
import bg.energo.phoenix.model.entity.product.product.Product;
import bg.energo.phoenix.model.entity.product.product.ProductFile;
import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroup;
import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductSubObjectShortResponse {
    private Long id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;

    public ProductSubObjectShortResponse(ProductGroups productGroups) {
        if (productGroups != null) {
            this.id = productGroups.getId();
            this.name = productGroups.getName();
        }
    }

    public ProductSubObjectShortResponse(ProductTypes productTypes) {
        if (productTypes != null) {
            this.id = productTypes.getId();
            this.name = productTypes.getName();
        }
    }

    public ProductSubObjectShortResponse(GridOperator gridOperator) {
        if (gridOperator != null) {
            this.id = gridOperator.getId();
            this.name = gridOperator.getName();
        }
    }

    public ProductSubObjectShortResponse(VatRate vatRate) {
        if (vatRate != null) {
            this.id = vatRate.getId();
            this.name = vatRate.getName();
        }
    }

    public ProductSubObjectShortResponse(SalesChannel salesChannel) {
        if (salesChannel != null) {
            this.id = salesChannel.getId();
            this.name = salesChannel.getName();
        }
    }

    public ProductSubObjectShortResponse(SalesArea salesArea) {
        if (salesArea != null) {
            this.id = salesArea.getId();
            this.name = salesArea.getName();
        }
    }

    public ProductSubObjectShortResponse(Segment segment) {
        if (segment != null) {
            this.id = segment.getId();
            this.name = segment.getName();
        }
    }

    public ProductSubObjectShortResponse(ElectricityPriceType electricityPriceType) {
        if (electricityPriceType != null) {
            this.id = electricityPriceType.getId();
            this.name = electricityPriceType.getName();
        }
    }

    public ProductSubObjectShortResponse(Currency currency) {
        if (currency != null) {
            this.id = currency.getId();
            this.name = currency.getName();
        }
    }

    public ProductSubObjectShortResponse(Terms terms) {
        if (terms != null) {
            this.id = terms.getId();
            this.name = terms.getName();
        }
    }

    public ProductSubObjectShortResponse(TermsGroups termsGroups) {
        if (termsGroups != null) {
            this.id = termsGroups.getId();
        }
    }

    public ProductSubObjectShortResponse(Termination termination) {
        if (termination != null) {
            this.id = termination.getId();
            this.name = termination.getName();
        }
    }

    public ProductSubObjectShortResponse(TerminationGroup terminationGroup) {
        if (terminationGroup != null) {
            this.id = terminationGroup.getId();
        }
    }

    public ProductSubObjectShortResponse(Penalty penalty) {
        if (penalty != null) {
            this.id = penalty.getId();
            this.name = penalty.getName();
        }
    }

    public ProductSubObjectShortResponse(PenaltyGroup penaltyGroup) {
        if (penaltyGroup != null) {
            this.id = penaltyGroup.getId();
        }
    }

    public ProductSubObjectShortResponse(PriceComponent priceComponent) {
        if (priceComponent != null) {
            this.id = priceComponent.getId();
            this.name = priceComponent.getName();
        }
    }

    public ProductSubObjectShortResponse(PriceComponentGroup priceComponentGroup) {
        if (priceComponentGroup != null) {
            this.id = priceComponentGroup.getId();
        }
    }

    public ProductSubObjectShortResponse(InterimAdvancePayment interimAdvancePayment) {
        if (interimAdvancePayment != null) {
            this.id = interimAdvancePayment.getId();
            this.name = interimAdvancePayment.getName();
        }
    }

    public ProductSubObjectShortResponse(AdvancedPaymentGroup advancedPaymentGroup) {
        if (advancedPaymentGroup != null) {
            this.id = advancedPaymentGroup.getId();
        }
    }

    public ProductSubObjectShortResponse(Product product) {
        if (product != null) {
            this.id = product.getId();
        }
    }

    public ProductSubObjectShortResponse(EPService epService) {
        if (epService != null) {
            this.id = epService.getId();
        }
    }

    public ProductSubObjectShortResponse(ProductFile productFile) {
        if (productFile != null) {
            this.id = productFile.getId();
            this.name = productFile.getName();
        }
    }
}

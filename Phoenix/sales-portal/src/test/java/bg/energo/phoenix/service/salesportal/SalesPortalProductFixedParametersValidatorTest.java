package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductInterimAndAdvancePayments;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.PaymentType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.ValueType;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductPriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.product.ProductPriceComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalProductFixedParametersValidatorTest {

    private static final Long PRODUCT_ID = 10L;
    private static final Long PRODUCT_VERSION_ID = 1L;
    private static final Long PRODUCT_DETAIL_ID = 100L;

    @Mock private ProductDetailsRepository productDetailsRepository;
    @Mock private ProductContractRepository productContractRepository;
    @Mock private InterimAdvancePaymentRepository interimAdvancePaymentRepository;
    @Mock private InterimAdvancePaymentTermsRepository interimAdvancePaymentTermsRepository;
    @Mock private ProductPriceComponentRepository productPriceComponentRepository;
    @Mock private ProductPriceComponentGroupRepository productPriceComponentGroupRepository;
    @Mock private PriceComponentRepository priceComponentRepository;
    @Mock private PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;

    private SalesPortalProductFixedParametersValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalProductFixedParametersValidator(
                productDetailsRepository,
                productContractRepository,
                interimAdvancePaymentRepository,
                interimAdvancePaymentTermsRepository,
                productPriceComponentRepository,
                productPriceComponentGroupRepository,
                priceComponentRepository,
                priceComponentFormulaVariableRepository
        );
    }

    @Test
    void validate_shouldRejectProductWithUnfilledInterimValueAndRange() {
        ProductDetails productDetails = productDetailsWithInterim(iapWithUnfilledValueAndRange());
        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetails));
        when(productContractRepository.getIapSByProductDetailIdAndCurrentDate(PRODUCT_DETAIL_ID))
                .thenReturn(List.of());
        when(productPriceComponentRepository.findPriceComponentsForProductDetails(PRODUCT_DETAIL_ID))
                .thenReturn(List.of());
        when(productPriceComponentGroupRepository.findByProductDetailsIdAndProductSubObjectStatusIn(
                PRODUCT_DETAIL_ID, List.of(ProductSubObjectStatus.ACTIVE)))
                .thenReturn(List.of());

        List<String> errors = new ArrayList<>();
        validator.validate(PRODUCT_ID, PRODUCT_VERSION_ID, errors);

        assertThat(errors).anyMatch(error -> error.contains("dynamic or unfilled value"));
    }

    @Test
    void validate_shouldAllowInterimValueWithDynamicRangeWhenValueIsFilled() {
        ProductDetails productDetails = productDetailsWithInterim(iapWithFilledValueAndRange());
        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetails));
        when(productContractRepository.getIapSByProductDetailIdAndCurrentDate(PRODUCT_DETAIL_ID))
                .thenReturn(List.of());
        when(productPriceComponentRepository.findPriceComponentsForProductDetails(PRODUCT_DETAIL_ID))
                .thenReturn(List.of());
        when(productPriceComponentGroupRepository.findByProductDetailsIdAndProductSubObjectStatusIn(
                PRODUCT_DETAIL_ID, List.of(ProductSubObjectStatus.ACTIVE)))
                .thenReturn(List.of());

        List<String> errors = new ArrayList<>();
        validator.validate(PRODUCT_ID, PRODUCT_VERSION_ID, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldRejectProductWithUnfilledPriceComponentValue() {
        ProductDetails productDetails = productDetailsWithoutInterim();
        PriceComponent priceComponent = new PriceComponent();
        priceComponent.setId(55L);
        PriceComponentFormulaVariable formulaVariable = new PriceComponentFormulaVariable();
        formulaVariable.setValue(null);

        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetails));
        when(productContractRepository.getIapSByProductDetailIdAndCurrentDate(PRODUCT_DETAIL_ID))
                .thenReturn(List.of());
        when(productPriceComponentRepository.findPriceComponentsForProductDetails(PRODUCT_DETAIL_ID))
                .thenReturn(List.of(priceComponent));
        when(productPriceComponentGroupRepository.findByProductDetailsIdAndProductSubObjectStatusIn(
                PRODUCT_DETAIL_ID, List.of(ProductSubObjectStatus.ACTIVE)))
                .thenReturn(List.of());
        when(priceComponentFormulaVariableRepository.findAllByPriceComponentIdIn(List.of(55L)))
                .thenReturn(List.of(formulaVariable));

        List<String> errors = new ArrayList<>();
        validator.validate(PRODUCT_ID, PRODUCT_VERSION_ID, errors);

        assertThat(errors).anyMatch(error -> error.contains("unfilled value"));
    }

    @Test
    void validate_shouldAllowPriceComponentWithValueAndDynamicRange() {
        ProductDetails productDetails = productDetailsWithoutInterim();
        PriceComponent priceComponent = new PriceComponent();
        priceComponent.setId(55L);
        PriceComponentFormulaVariable formulaVariable = new PriceComponentFormulaVariable();
        formulaVariable.setValue(BigDecimal.TEN);
        formulaVariable.setValueFrom(BigDecimal.ONE);
        formulaVariable.setValueTo(BigDecimal.valueOf(20));

        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetails));
        when(productContractRepository.getIapSByProductDetailIdAndCurrentDate(PRODUCT_DETAIL_ID))
                .thenReturn(List.of());
        when(productPriceComponentRepository.findPriceComponentsForProductDetails(PRODUCT_DETAIL_ID))
                .thenReturn(List.of(priceComponent));
        when(productPriceComponentGroupRepository.findByProductDetailsIdAndProductSubObjectStatusIn(
                PRODUCT_DETAIL_ID, List.of(ProductSubObjectStatus.ACTIVE)))
                .thenReturn(List.of());
        when(priceComponentFormulaVariableRepository.findAllByPriceComponentIdIn(List.of(55L)))
                .thenReturn(List.of(formulaVariable));

        List<String> errors = new ArrayList<>();
        validator.validate(PRODUCT_ID, PRODUCT_VERSION_ID, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldAllowProductWithFixedInterimAndPriceComponentValues() {
        InterimAdvancePayment fixedInterim = new InterimAdvancePayment();
        fixedInterim.setId(1L);
        fixedInterim.setStatus(InterimAdvancePaymentStatus.ACTIVE);
        fixedInterim.setPaymentType(PaymentType.OBLIGATORY);
        fixedInterim.setValueType(ValueType.EXACT_AMOUNT);
        fixedInterim.setValue(BigDecimal.TEN);
        fixedInterim.setMatchTermOfStandardInvoice(true);

        ProductDetails productDetails = productDetailsWithInterim(fixedInterim);
        PriceComponent priceComponent = new PriceComponent();
        priceComponent.setId(55L);
        PriceComponentFormulaVariable formulaVariable = new PriceComponentFormulaVariable();
        formulaVariable.setValue(BigDecimal.ONE);

        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetails));
        when(productContractRepository.getIapSByProductDetailIdAndCurrentDate(PRODUCT_DETAIL_ID))
                .thenReturn(List.of());
        when(productPriceComponentRepository.findPriceComponentsForProductDetails(PRODUCT_DETAIL_ID))
                .thenReturn(List.of(priceComponent));
        when(productPriceComponentGroupRepository.findByProductDetailsIdAndProductSubObjectStatusIn(
                PRODUCT_DETAIL_ID, List.of(ProductSubObjectStatus.ACTIVE)))
                .thenReturn(List.of());
        when(priceComponentFormulaVariableRepository.findAllByPriceComponentIdIn(List.of(55L)))
                .thenReturn(List.of(formulaVariable));

        List<String> errors = new ArrayList<>();
        validator.validate(PRODUCT_ID, PRODUCT_VERSION_ID, errors);

        assertThat(errors).isEmpty();
    }

    private static ProductDetails productDetailsWithoutInterim() {
        ProductDetails productDetails = new ProductDetails();
        productDetails.setId(PRODUCT_DETAIL_ID);
        productDetails.setInterimAndAdvancePayments(List.of());
        return productDetails;
    }

    private static ProductDetails productDetailsWithInterim(InterimAdvancePayment interimAdvancePayment) {
        ProductDetails productDetails = productDetailsWithoutInterim();
        ProductInterimAndAdvancePayments link = new ProductInterimAndAdvancePayments();
        link.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
        link.setInterimAdvancePayment(interimAdvancePayment);
        productDetails.setInterimAndAdvancePayments(List.of(link));
        return productDetails;
    }

    private static InterimAdvancePayment iapWithUnfilledValueAndRange() {
        InterimAdvancePayment interimAdvancePayment = new InterimAdvancePayment();
        interimAdvancePayment.setId(1L);
        interimAdvancePayment.setStatus(InterimAdvancePaymentStatus.ACTIVE);
        interimAdvancePayment.setPaymentType(PaymentType.OBLIGATORY);
        interimAdvancePayment.setValueType(ValueType.EXACT_AMOUNT);
        interimAdvancePayment.setValueFrom(BigDecimal.ONE);
        interimAdvancePayment.setMatchTermOfStandardInvoice(true);
        return interimAdvancePayment;
    }

    private static InterimAdvancePayment iapWithFilledValueAndRange() {
        InterimAdvancePayment interimAdvancePayment = new InterimAdvancePayment();
        interimAdvancePayment.setId(1L);
        interimAdvancePayment.setStatus(InterimAdvancePaymentStatus.ACTIVE);
        interimAdvancePayment.setPaymentType(PaymentType.OBLIGATORY);
        interimAdvancePayment.setValueType(ValueType.EXACT_AMOUNT);
        interimAdvancePayment.setValue(BigDecimal.TEN);
        interimAdvancePayment.setValueFrom(BigDecimal.ONE);
        interimAdvancePayment.setMatchTermOfStandardInvoice(true);
        return interimAdvancePayment;
    }
}

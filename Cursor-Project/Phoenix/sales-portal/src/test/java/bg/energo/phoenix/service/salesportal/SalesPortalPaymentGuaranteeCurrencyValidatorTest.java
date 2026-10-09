package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractUpdateRequest;
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
class SalesPortalPaymentGuaranteeCurrencyValidatorTest {

    private static final Long PRODUCT_ID = 18167L;
    private static final Long PRODUCT_VERSION_ID = 1L;
    private static final Long LEVA_CURRENCY_ID = 1001L;
    private static final Long EURO_CURRENCY_ID = 1002L;

    @Mock
    private ProductDetailsRepository productDetailsRepository;

    private SalesPortalPaymentGuaranteeCurrencyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalPaymentGuaranteeCurrencyValidator(productDetailsRepository);
    }

    @Test
    void validate_shouldRejectMismatchedCashDepositCurrency_whenProductHasFixedCurrency() {
        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetailsWithCurrencies(leva(), leva())));

        SalesPortalContractUpdateRequest request = requestWithCurrencies(
                PaymentGuarantee.CASH_DEPOSIT_AND_BANK,
                EURO_CURRENCY_ID,
                EURO_CURRENCY_ID
        );
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.cashDepositCurrencyId-Cash deposit currency does not match the fixed product currency;",
                "contract.bankGuaranteeCurrencyId-Bank guarantee currency does not match the fixed product currency;"
        );
    }

    @Test
    void validate_shouldAcceptMatchingCurrencies_whenProductHasFixedCurrencies() {
        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetailsWithCurrencies(leva(), leva())));

        SalesPortalContractUpdateRequest request = requestWithCurrencies(
                PaymentGuarantee.CASH_DEPOSIT_AND_BANK,
                LEVA_CURRENCY_ID,
                LEVA_CURRENCY_ID
        );
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldAllowAnyCurrency_whenProductDoesNotDeclareFixedCurrency() {
        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetailsWithCurrencies(null, null)));

        SalesPortalContractUpdateRequest request = requestWithCurrencies(
                PaymentGuarantee.CASH_DEPOSIT_AND_BANK,
                EURO_CURRENCY_ID,
                EURO_CURRENCY_ID
        );
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldValidateOnlyCashDepositCurrency_whenPaymentGuaranteeIsCashDeposit() {
        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetailsWithCurrencies(leva(), leva())));

        SalesPortalContractUpdateRequest request = requestWithCurrencies(
                PaymentGuarantee.CASH_DEPOSIT,
                EURO_CURRENCY_ID,
                LEVA_CURRENCY_ID
        );
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.cashDepositCurrencyId-Cash deposit currency does not match the fixed product currency;"
        );
    }

    @Test
    void validate_shouldValidateOnlyBankGuaranteeCurrency_whenPaymentGuaranteeIsBank() {
        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetailsWithCurrencies(leva(), leva())));

        SalesPortalContractUpdateRequest request = requestWithCurrencies(
                PaymentGuarantee.BANK,
                LEVA_CURRENCY_ID,
                EURO_CURRENCY_ID
        );
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.bankGuaranteeCurrencyId-Bank guarantee currency does not match the fixed product currency;"
        );
    }

    private static SalesPortalContractUpdateRequest requestWithCurrencies(
            PaymentGuarantee paymentGuarantee,
            Long cashDepositCurrencyId,
            Long bankGuaranteeCurrencyId
    ) {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setProductId(PRODUCT_ID);
        request.setProductVersionId(PRODUCT_VERSION_ID);
        request.setPaymentGuarantee(paymentGuarantee);
        request.setCashDepositAmount(BigDecimal.ONE);
        request.setCashDepositCurrencyId(cashDepositCurrencyId);
        request.setBankGuaranteeAmount(BigDecimal.ONE);
        request.setBankGuaranteeCurrencyId(bankGuaranteeCurrencyId);
        return request;
    }

    private static ProductDetails productDetailsWithCurrencies(Currency cashDepositCurrency, Currency bankGuaranteeCurrency) {
        ProductDetails productDetails = new ProductDetails();
        productDetails.setCashDepositCurrency(cashDepositCurrency);
        productDetails.setBankGuaranteeCurrency(bankGuaranteeCurrency);
        return productDetails;
    }

    private static Currency leva() {
        Currency currency = new Currency();
        currency.setId(LEVA_CURRENCY_ID);
        return currency;
    }
}

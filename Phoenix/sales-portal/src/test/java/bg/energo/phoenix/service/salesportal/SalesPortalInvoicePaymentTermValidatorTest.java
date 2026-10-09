package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalInvoicePaymentTermValidatorTest {

    private static final Long PAYMENT_TERM_ID = 20421L;

    @Mock
    private InvoicePaymentTermsRepository invoicePaymentTermsRepository;

    private SalesPortalInvoicePaymentTermValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalInvoicePaymentTermValidator(invoicePaymentTermsRepository);
    }

    @Test
    void validate_shouldRequireValue_whenPaymentTermHasNoFixedValue() {
        InvoicePaymentTerms paymentTerm = paymentTerm(null, 1, 31);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, null);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.invoicePaymentTermValue-Invoice payment term value is mandatory when the selected payment term has no fixed value;"
        );
    }

    @Test
    void validate_shouldAcceptValueWithinRange_whenPaymentTermIsNonFixed() {
        InvoicePaymentTerms paymentTerm = paymentTerm(null, 1, 31);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, 10);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldAcceptZero_whenPaymentTermAllowsZero() {
        InvoicePaymentTerms paymentTerm = paymentTerm(null, 0, 31);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, 0);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldRejectValueAboveAbsoluteMaximum() {
        InvoicePaymentTerms paymentTerm = paymentTerm(null, 0, 9999);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, 10000);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.invoicePaymentTermValue-Invoice payment term value must be between 0 and 9999;"
        );
    }

    @Test
    void validate_shouldRejectNegativeValue() {
        InvoicePaymentTerms paymentTerm = paymentTerm(null, 0, 31);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, -1);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.invoicePaymentTermValue-Invoice payment term value must be between 0 and 9999;"
        );
    }

    @Test
    void validate_shouldRejectValueOutsideRange_whenPaymentTermIsNonFixed() {
        InvoicePaymentTerms paymentTerm = paymentTerm(null, 1, 31);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, 40);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.invoicePaymentTermValue-Invoice payment term value fails range validation;"
        );
    }

    @Test
    void validate_shouldNotRequireValue_whenPaymentTermHasFixedValue() {
        InvoicePaymentTerms paymentTerm = paymentTerm(10, null, null);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, null);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldRejectMismatchedValue_whenPaymentTermHasFixedValue() {
        InvoicePaymentTerms paymentTerm = paymentTerm(10, null, null);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, 15);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.invoicePaymentTermValue-Invoice payment term value does not match the fixed term value;"
        );
    }

    @Test
    void isNonFixedPaymentTerm_shouldTreatNullValueAsNonFixed() {
        assertThat(SalesPortalInvoicePaymentTermValidator.isNonFixedPaymentTerm(paymentTerm(null, null, null)))
                .isTrue();
    }

    @Test
    void validate_shouldNotRequireValue_whenPaymentTermHasValueWithRange() {
        InvoicePaymentTerms paymentTerm = paymentTerm(10, 1, 31);
        when(invoicePaymentTermsRepository.findById(PAYMENT_TERM_ID)).thenReturn(Optional.of(paymentTerm));

        SalesPortalContractUpdateRequest request = requestWithPaymentTerm(PAYMENT_TERM_ID, null);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void isNonFixedPaymentTerm_shouldTreatValueWithRangeAsFixed() {
        assertThat(SalesPortalInvoicePaymentTermValidator.isNonFixedPaymentTerm(paymentTerm(10, 1, 31)))
                .isFalse();
    }

    @Test
    void isWithinAbsoluteBounds_shouldAcceptZeroAnd9999() {
        assertThat(SalesPortalInvoicePaymentTermValidator.isWithinAbsoluteBounds(0)).isTrue();
        assertThat(SalesPortalInvoicePaymentTermValidator.isWithinAbsoluteBounds(9999)).isTrue();
        assertThat(SalesPortalInvoicePaymentTermValidator.isWithinAbsoluteBounds(-1)).isFalse();
        assertThat(SalesPortalInvoicePaymentTermValidator.isWithinAbsoluteBounds(10000)).isFalse();
    }

    private static SalesPortalContractUpdateRequest requestWithPaymentTerm(Long paymentTermId, Integer value) {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setInvoicePaymentTermId(paymentTermId);
        request.setInvoicePaymentTermValue(value);
        return request;
    }

    private static InvoicePaymentTerms paymentTerm(Integer value, Integer valueFrom, Integer valueTo) {
        InvoicePaymentTerms paymentTerm = new InvoicePaymentTerms();
        paymentTerm.setId(PAYMENT_TERM_ID);
        paymentTerm.setStatus(PaymentTermStatus.ACTIVE);
        paymentTerm.setValue(value);
        paymentTerm.setValueFrom(valueFrom);
        paymentTerm.setValueTo(valueTo);
        return paymentTerm;
    }
}

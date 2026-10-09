package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.entity.product.product.ProductContractTerms;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.product.ProductTermPeriodType;
import bg.energo.phoenix.model.enums.product.product.ProductTermType;
import bg.energo.phoenix.repository.product.product.ProductContractTermRepository;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalContractTermValidatorTest {

    private static final Long CONTRACT_TERM_ID = 18353L;

    @Mock
    private ProductContractTermRepository productContractTermRepository;

    private SalesPortalContractTermValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalContractTermValidator(productContractTermRepository);
    }

    @Test
    void validate_shouldRequireEndDate_whenContractTermTypeIsCertainDate() {
        when(productContractTermRepository.findById(CONTRACT_TERM_ID))
                .thenReturn(Optional.of(contractTerm(ProductTermPeriodType.CERTAIN_DATE)));

        SalesPortalContractUpdateRequest request = requestWithContractTerm(CONTRACT_TERM_ID, null);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.contractTermEndDate-Contract term end date is mandatory when the selected contract term type is certain date;"
        );
    }

    @Test
    void validate_shouldAcceptEndDate_whenContractTermTypeIsCertainDate() {
        when(productContractTermRepository.findById(CONTRACT_TERM_ID))
                .thenReturn(Optional.of(contractTerm(ProductTermPeriodType.CERTAIN_DATE)));

        LocalDate endDate = LocalDate.of(2027, 12, 31);
        SalesPortalContractUpdateRequest request = requestWithContractTerm(CONTRACT_TERM_ID, endDate);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldNotRequireEndDate_whenContractTermTypeIsPeriod() {
        when(productContractTermRepository.findById(CONTRACT_TERM_ID))
                .thenReturn(Optional.of(contractTerm(ProductTermPeriodType.PERIOD)));

        SalesPortalContractUpdateRequest request = requestWithContractTerm(CONTRACT_TERM_ID, null);
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldRejectInvalidContractTermId() {
        when(productContractTermRepository.findById(CONTRACT_TERM_ID)).thenReturn(Optional.empty());

        SalesPortalContractUpdateRequest request = requestWithContractTerm(CONTRACT_TERM_ID, LocalDate.of(2027, 1, 1));
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly("contract.contractTermId-Selected contract term is not valid;");
    }

    @Test
    void requiresContractTermEndDate_shouldBeTrueOnlyForCertainDate() {
        assertThat(SalesPortalContractTermValidator.requiresContractTermEndDate(
                contractTerm(ProductTermPeriodType.CERTAIN_DATE))).isTrue();
        assertThat(SalesPortalContractTermValidator.requiresContractTermEndDate(
                contractTerm(ProductTermPeriodType.PERIOD))).isFalse();
        assertThat(SalesPortalContractTermValidator.requiresContractTermEndDate(
                contractTerm(ProductTermPeriodType.WITHOUT_TERM))).isFalse();
    }

    private static SalesPortalContractUpdateRequest requestWithContractTerm(Long contractTermId, LocalDate endDate) {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setContractTermId(contractTermId);
        request.setContractTermEndDate(endDate);
        return request;
    }

    private static ProductContractTerms contractTerm(ProductTermPeriodType periodType) {
        ProductContractTerms contractTerm = new ProductContractTerms();
        contractTerm.setId(CONTRACT_TERM_ID);
        contractTerm.setStatus(ProductSubObjectStatus.ACTIVE);
        contractTerm.setPeriodType(periodType);
        contractTerm.setType(ProductTermType.DAY_DAYS);
        contractTerm.setValue(12);
        return contractTerm;
    }
}

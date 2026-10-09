package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponseImpl;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractResponse;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesPortalSigningDateValidatorTest {

    private SalesPortalSigningDateValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalSigningDateValidator();
    }

    @Test
    void validate_shouldAcceptSigningDateEqualToExactSupplyActivationDate() {
        SalesPortalContractUpdateRequest request = request(
                LocalDate.of(2026, 7, 8),
                SupplyActivation.EXACT_DATE,
                LocalDate.of(2026, 7, 8)
        );

        List<String> errors = new ArrayList<>();
        validator.validate(request, new ProductContractResponse(), errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldAcceptSigningDateBeforeExactSupplyActivationDate() {
        SalesPortalContractUpdateRequest request = request(
                LocalDate.of(2026, 6, 1),
                SupplyActivation.EXACT_DATE,
                LocalDate.of(2026, 7, 8)
        );

        List<String> errors = new ArrayList<>();
        validator.validate(request, new ProductContractResponse(), errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldRejectSigningDateAfterExactSupplyActivationDate() {
        SalesPortalContractUpdateRequest request = request(
                LocalDate.of(2026, 8, 1),
                SupplyActivation.EXACT_DATE,
                LocalDate.of(2026, 7, 8)
        );

        List<String> errors = new ArrayList<>();
        validator.validate(request, new ProductContractResponse(), errors);

        assertThat(errors).containsExactly(
                "contract.signingDate-Signing date should be less or equal to pod activation date;"
        );
    }

    @Test
    void validate_shouldUseFirstDayOfMonthAfterSigningMonthForFirstDayOfMonthActivation() {
        SalesPortalContractUpdateRequest request = request(
                LocalDate.of(2026, 6, 18),
                SupplyActivation.FIRST_DAY_OF_MONTH,
                null
        );

        List<String> errors = new ArrayList<>();
        validator.validate(request, new ProductContractResponse(), errors);

        assertThat(errors).isEmpty();
        assertThat(SalesPortalSigningDateValidator.resolveSupplyActivationDate(request))
                .isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void validate_shouldRejectSigningDateAfterExistingPodActivationDate() {
        SalesPortalContractUpdateRequest request = request(
                LocalDate.of(2026, 6, 20),
                SupplyActivation.MANUAL,
                null
        );

        ContractPodsResponseImpl pod = new ContractPodsResponseImpl(
                null, null, null, null, null, null, null, null, null,
                null, null, null,
                LocalDate.of(2026, 6, 15), null,
                null, null, null, null, null, null, null
        );

        ProductContractResponse existingContract = new ProductContractResponse();
        existingContract.setContractPodsResponses(List.of(pod));

        List<String> errors = new ArrayList<>();
        validator.validate(request, existingContract, errors);

        assertThat(errors).containsExactly(
                "contract.signingDate-Signing date should be less or equal to pod activation date;"
        );
    }

    private static SalesPortalContractUpdateRequest request(
            LocalDate signingDate,
            SupplyActivation supplyActivation,
            LocalDate supplyActivationDate
    ) {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setSigningDate(signingDate);
        request.setSupplyActivationAfterContractResigning(supplyActivation);
        request.setSupplyActivationAfterContractResigningDate(supplyActivationDate);
        return request;
    }
}

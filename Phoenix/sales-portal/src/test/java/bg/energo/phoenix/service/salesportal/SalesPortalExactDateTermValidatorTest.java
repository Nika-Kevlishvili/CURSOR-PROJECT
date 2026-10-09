package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesPortalExactDateTermValidatorTest {

    private SalesPortalExactDateTermValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalExactDateTermValidator();
    }

    @Test
    void validate_shouldRequireStartOfInitialTermDate_whenTermTypeIsExactDate() {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setStartOfInitialTerm(StartOfContractInitialTerm.EXACT_DATE);
        request.setStartOfInitialTermDate(null);
        request.setSupplyActivationAfterContractResigning(SupplyActivation.MANUAL);

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.startOfInitialTermDate-Start of initial term date is mandatory when the selected term type is exact date;"
        );
    }

    @Test
    void validate_shouldRequireSupplyActivationDate_whenActivationTypeIsExactDate() {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setStartOfInitialTerm(StartOfContractInitialTerm.SIGNING);
        request.setSupplyActivationAfterContractResigning(SupplyActivation.EXACT_DATE);
        request.setSupplyActivationAfterContractResigningDate(null);

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.supplyActivationAfterContractResigningDate-Supply activation date is mandatory when the selected activation type is exact date;"
        );
    }

    @Test
    void validate_shouldRejectBothMissingDates_whenBothTypesAreExactDate() {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setStartOfInitialTerm(StartOfContractInitialTerm.EXACT_DATE);
        request.setStartOfInitialTermDate(null);
        request.setSupplyActivationAfterContractResigning(SupplyActivation.EXACT_DATE);
        request.setSupplyActivationAfterContractResigningDate(null);

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.startOfInitialTermDate-Start of initial term date is mandatory when the selected term type is exact date;",
                "contract.supplyActivationAfterContractResigningDate-Supply activation date is mandatory when the selected activation type is exact date;"
        );
    }

    @Test
    void validate_shouldAcceptDates_whenExactDateTypesAreSelected() {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setStartOfInitialTerm(StartOfContractInitialTerm.EXACT_DATE);
        request.setStartOfInitialTermDate(LocalDate.of(2026, 6, 17));
        request.setSupplyActivationAfterContractResigning(SupplyActivation.EXACT_DATE);
        request.setSupplyActivationAfterContractResigningDate(LocalDate.of(2026, 7, 8));

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldNotRequireDates_whenNonExactDateTypesAreSelected() {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setStartOfInitialTerm(StartOfContractInitialTerm.SIGNING);
        request.setStartOfInitialTermDate(null);
        request.setSupplyActivationAfterContractResigning(SupplyActivation.MANUAL);
        request.setSupplyActivationAfterContractResigningDate(null);

        List<String> errors = new ArrayList<>();
        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void requiresStartOfInitialTermDate_shouldBeTrueOnlyForExactDate() {
        assertThat(SalesPortalExactDateTermValidator.requiresStartOfInitialTermDate(
                StartOfContractInitialTerm.EXACT_DATE)).isTrue();
        assertThat(SalesPortalExactDateTermValidator.requiresStartOfInitialTermDate(
                StartOfContractInitialTerm.SIGNING)).isFalse();
    }

    @Test
    void requiresSupplyActivationDate_shouldBeTrueOnlyForExactDate() {
        assertThat(SalesPortalExactDateTermValidator.requiresSupplyActivationDate(
                SupplyActivation.EXACT_DATE)).isTrue();
        assertThat(SalesPortalExactDateTermValidator.requiresSupplyActivationDate(
                SupplyActivation.MANUAL)).isFalse();
    }
}

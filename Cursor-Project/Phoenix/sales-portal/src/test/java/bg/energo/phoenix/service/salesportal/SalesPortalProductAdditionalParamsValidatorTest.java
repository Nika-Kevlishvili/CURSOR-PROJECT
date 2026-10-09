package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractAdditionalParamsResponse;
import bg.energo.phoenix.repository.product.product.ProductAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
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
class SalesPortalProductAdditionalParamsValidatorTest {

    private static final Long PRODUCT_ID = 18018L;
    private static final Long PRODUCT_VERSION_ID = 1L;
    private static final Long PRODUCT_DETAIL_ID = 500L;
    private static final Long NON_FIXED_PARAM_ID = 9001L;
    private static final Long FIXED_PARAM_ID = 9002L;

    @Mock
    private ProductDetailsRepository productDetailsRepository;
    @Mock
    private ProductAdditionalParamsRepository productAdditionalParamsRepository;

    private SalesPortalProductAdditionalParamsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SalesPortalProductAdditionalParamsValidator(
                productDetailsRepository,
                productAdditionalParamsRepository
        );
    }

    @Test
    void validate_shouldRequireValue_whenProductAdditionalParamHasNoFixedValue() {
        stubProductWithParams(
                productParam(NON_FIXED_PARAM_ID, "Dynamic label", null),
                productParam(FIXED_PARAM_ID, "Fixed label", "fixed-value")
        );

        SalesPortalContractUpdateRequest request = requestWithParams();
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.productAdditionalParams-Additional parameter value is mandatory for parameter id %s when the product parameter has no fixed value;"
                        .formatted(NON_FIXED_PARAM_ID)
        );
    }

    @Test
    void validate_shouldAcceptProvidedValue_whenProductAdditionalParamHasNoFixedValue() {
        stubProductWithParams(productParam(NON_FIXED_PARAM_ID, "Dynamic label", null));

        SalesPortalContractUpdateRequest request = requestWithParams(
                paramUpdate(NON_FIXED_PARAM_ID, "contract-value")
        );
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void validate_shouldRejectMismatchedValue_whenProductAdditionalParamIsFixed() {
        stubProductWithParams(productParam(FIXED_PARAM_ID, "Fixed label", "fixed-value"));

        SalesPortalContractUpdateRequest request = requestWithParams(
                paramUpdate(FIXED_PARAM_ID, "other-value")
        );
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).containsExactly(
                "contract.productAdditionalParams[0]-Additional parameter value does not match the fixed product value;"
        );
    }

    @Test
    void validate_shouldAllowOmittingFixedProductAdditionalParam() {
        stubProductWithParams(productParam(FIXED_PARAM_ID, "Fixed label", "fixed-value"));

        SalesPortalContractUpdateRequest request = requestWithParams();
        List<String> errors = new ArrayList<>();

        validator.validate(request, errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void requiresContractValue_shouldBeTrueOnlyWhenLabelExistsAndProductValueMissing() {
        assertThat(SalesPortalProductAdditionalParamsValidator.requiresContractValue(
                productParam(NON_FIXED_PARAM_ID, "Label", null))).isTrue();
        assertThat(SalesPortalProductAdditionalParamsValidator.requiresContractValue(
                productParam(FIXED_PARAM_ID, "Label", "value"))).isFalse();
    }

    private void stubProductWithParams(ProductContractAdditionalParamsResponse... params) {
        ProductDetails productDetails = new ProductDetails();
        productDetails.setId(PRODUCT_DETAIL_ID);
        when(productDetailsRepository.findByProductIdAndVersion(PRODUCT_ID, PRODUCT_VERSION_ID))
                .thenReturn(Optional.of(productDetails));
        when(productAdditionalParamsRepository.findProductFilledAdditionalParamsByProductDetailId(PRODUCT_DETAIL_ID))
                .thenReturn(List.of(params));
    }

    private static SalesPortalContractUpdateRequest requestWithParams(
            SalesPortalContractUpdateRequest.ProductAdditionalParamUpdate... params
    ) {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setProductId(PRODUCT_ID);
        request.setProductVersionId(PRODUCT_VERSION_ID);
        request.setProductAdditionalParams(List.of(params));
        return request;
    }

    private static SalesPortalContractUpdateRequest.ProductAdditionalParamUpdate paramUpdate(Long id, String value) {
        SalesPortalContractUpdateRequest.ProductAdditionalParamUpdate update =
                new SalesPortalContractUpdateRequest.ProductAdditionalParamUpdate();
        update.setId(id);
        update.setValue(value);
        return update;
    }

    private static ProductContractAdditionalParamsResponse productParam(Long id, String label, String value) {
        return new ProductContractAdditionalParamsResponse(id, 1L, label, value);
    }
}

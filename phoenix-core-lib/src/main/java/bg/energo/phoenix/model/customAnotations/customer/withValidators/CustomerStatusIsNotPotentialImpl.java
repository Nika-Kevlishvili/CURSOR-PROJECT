package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.CreateCustomerRequest;
import bg.energo.phoenix.model.request.customer.ForeignAddressData;
import bg.energo.phoenix.model.request.customer.LocalAddressData;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
    If Customer Status is not potential following fields are mandatory: Segments; Address;  Country;
    Region; Municipality; Populated Place; District; Zip Code;
    Additionally if customer type is LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY
    following fields are mandatory too: Form of Ownership; Economic Branch; Main Subject of Activity;
 */
public class CustomerStatusIsNotPotentialImpl
        implements ConstraintValidator<CustomerStatusIsNotPotential, CreateCustomerRequest> {

    @Override
    public boolean isValid(CreateCustomerRequest createCustomerRequest, ConstraintValidatorContext context) {
        if (createCustomerRequest.getCustomerDetailStatus() == null) {
            return false;
        }
        if (createCustomerRequest.getCustomerType() == null) {
            return false;
        }
        boolean correct = true;
        StringBuilder stringBuilder = new StringBuilder();
        context.disableDefaultConstraintViolation();
        if (createCustomerRequest.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL) {
            if ((createCustomerRequest.getBusinessActivity() != null && createCustomerRequest.getBusinessActivity())
                    || createCustomerRequest.getCustomerType() == CustomerType.LEGAL_ENTITY) {

                if (createCustomerRequest.getOwnershipFormId() == null) {
                    stringBuilder.append("ownershipFormId-Form of Ownership is required;");
                    correct = false;
                }

                if (createCustomerRequest.getEconomicBranchId() == null) {
                    stringBuilder.append("economicBranchId-Economic Branch is required;");
                    correct = false;
                }

                if (createCustomerRequest.getMainSubjectOfActivity() == null || createCustomerRequest.getMainSubjectOfActivity().isBlank()) {
                    stringBuilder.append("mainSubjectOfActivity-Main Subject of Activity is required;");
                    correct = false;
                }
            }

            if (createCustomerRequest.getSegmentIds() == null || createCustomerRequest.getSegmentIds().isEmpty()) {
                stringBuilder.append("segmentIds-Minimum one segment is required;");
                correct = false;
            }

            if (createCustomerRequest.getAddress() != null) {
                if (createCustomerRequest.getAddress().getForeign() != null
                        && createCustomerRequest.getAddress().getForeign()) {
                    ForeignAddressData foreignAddressData = createCustomerRequest.getAddress().getForeignAddressData();
                    if (foreignAddressData == null) {
                        stringBuilder.append("address.foreignAddressData-Foreign address data is required;");
                        correct = false;
                    } else {
                        if (foreignAddressData.getCountryId() == null) {
                            stringBuilder.append("address.foreignAddressData.countryId-Country is required;");
                            correct = false;
                        }

                        if (foreignAddressData.getRegion() == null || foreignAddressData.getRegion().isBlank()) {
                            stringBuilder.append("address.foreignAddressData.region-Region is required;");
                            correct = false;
                        }

                        if (foreignAddressData.getMunicipality() == null || foreignAddressData.getMunicipality().isBlank()) {
                            stringBuilder.append("address.foreignAddressData.municipality-Municipality is required;");
                            correct = false;
                        }

                        if (foreignAddressData.getPopulatedPlace() == null || foreignAddressData.getPopulatedPlace().isBlank()) {
                            stringBuilder.append("address.foreignAddressData.populatedPlace-Populated Place is required;");
                            correct = false;
                        }

                        if (foreignAddressData.getZipCode() == null || foreignAddressData.getZipCode().isBlank()) {
                            stringBuilder.append("address.foreignAddressData.zipCode-ZIP Code is required;");
                            correct = false;
                        }

                    }
                } else {
                    LocalAddressData localAddressData = createCustomerRequest.getAddress().getLocalAddressData();
                    if (localAddressData == null) {
                        stringBuilder.append("address.localAddressData-Local address data is required;");
                        correct = false;
                    } else {
                        if (localAddressData.getCountryId() == null) {
                            stringBuilder.append("address.localAddressData.countryId-Country is required;");
                            correct = false;
                        }

                        if (localAddressData.getRegionId() == null) {
                            stringBuilder.append("address.localAddressData.regionId-Region is required;");
                            correct = false;
                        }

                        if (localAddressData.getMunicipalityId() == null) {
                            stringBuilder.append("address.localAddressData.municipalityId-Municipality is required;");
                            correct = false;
                        }

                        if (localAddressData.getPopulatedPlaceId() == null) {
                            stringBuilder.append("address.localAddressData.populatedPlaceId-Populated Place is required;");
                            correct = false;
                        }

                        if (localAddressData.getZipCodeId() == null) {
                            stringBuilder.append("address.localAddressData.zipCodeId-ZIP Code is required;");
                            correct = false;
                        }
                    }
                }
            }
        }
        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return correct;
    }


}

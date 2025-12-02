package phoenix.core.customer.model.customAnotations.customer.withValidators;

import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.CreateCustomerRequest;
import phoenix.core.customer.model.request.ForeignAddressData;
import phoenix.core.customer.model.request.LocalAddressData;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
            if (createCustomerRequest.getCustomerType() == CustomerType.PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY
                    || createCustomerRequest.getCustomerType() == CustomerType.LEGAL_ENTITY) {

                if (createCustomerRequest.getOwnershipFormId() == null) {
                    stringBuilder.append("Form of Ownership is required; ");
                    correct = false;
                }

                if (createCustomerRequest.getEconomicBranchId() == null) {
                    stringBuilder.append("Economic Branch is required; ");
                    correct = false;
                }

                if (createCustomerRequest.getMainSubjectOfActivity() == null) {
                    stringBuilder.append("Main Subject of Activity is required; ");
                    correct = false;
                }
            }

            if (createCustomerRequest.getSegmentIds() == null || createCustomerRequest.getSegmentIds().isEmpty()) {
                stringBuilder.append("Minimum one segment is required; ");
                correct = false;
            }

            if (createCustomerRequest.getAddress() != null) {
                if (createCustomerRequest.getAddress().getForeign() != null
                        && createCustomerRequest.getAddress().getForeign()) {
                    ForeignAddressData foreignAddressData = createCustomerRequest.getAddress().getForeignAddressData();
                    if (foreignAddressData == null) {
                        stringBuilder.append("Foreign address data is required; ");
                        correct = false;
                    } else {
                        if (foreignAddressData.getCountryId() == null) {
                            stringBuilder.append("Country is required; ");
                            correct = false;
                        }

                        if (foreignAddressData.getRegion() == null) {
                            stringBuilder.append("Region is required; ");
                            correct = false;
                        }

                        if (foreignAddressData.getMunicipality() == null) {
                            stringBuilder.append("Municipality is required; ");
                            correct = false;
                        }

                        if (foreignAddressData.getPopulatedPlace() == null) {
                            stringBuilder.append("Populated Place is required; ");
                            correct = false;
                        }

                        if (foreignAddressData.getZipCode() == null) {
                            stringBuilder.append("ZIP Code is required; ");
                            correct = false;
                        }

                        if (foreignAddressData.getDistrict() == null) {
                            stringBuilder.append("District is required; ");
                            correct = false;
                        }
                    }
                } else {
                    LocalAddressData localAddressData = createCustomerRequest.getAddress().getLocalAddressData();
                    if (localAddressData == null) {
                        stringBuilder.append("Local address data is required; ");
                        correct = false;
                    } else {
                        if (localAddressData.getCountryId() == null) {
                            stringBuilder.append("Country is required; ");
                            correct = false;
                        }

                        if (localAddressData.getRegionId() == null) {
                            stringBuilder.append("Region is required; ");
                            correct = false;
                        }

                        if (localAddressData.getMunicipalityId() == null) {
                            stringBuilder.append("Municipality is required; ");
                            correct = false;
                        }

                        if (localAddressData.getPopulatedPlaceId() == null) {
                            stringBuilder.append("Populated Place is required; ");
                            correct = false;
                        }

                        if (localAddressData.getZipCodeId() == null) {
                            stringBuilder.append("ZIP Code is required; ");
                            correct = false;
                        }

                        if (localAddressData.getDistrictId() == null) {
                            stringBuilder.append("District is required; ");
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

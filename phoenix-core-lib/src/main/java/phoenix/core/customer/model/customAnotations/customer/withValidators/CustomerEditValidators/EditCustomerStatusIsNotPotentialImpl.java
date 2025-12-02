package phoenix.core.customer.model.customAnotations.customer.withValidators.CustomerEditValidators;

import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.request.EditCustomerRequest;
import phoenix.core.customer.model.request.ForeignAddressData;
import phoenix.core.customer.model.request.LocalAddressData;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EditCustomerStatusIsNotPotentialImpl
        implements ConstraintValidator<EditCustomerStatusIsNotPotential, EditCustomerRequest> {
    @Override
    public boolean isValid(EditCustomerRequest editCustomerRequest, ConstraintValidatorContext context) {
        if (editCustomerRequest.getCustomerDetailStatus() == null) {
            return false;
        }
        if (editCustomerRequest.getCustomerType() == null) {
            return false;
        }
        boolean correct = true;
        StringBuilder stringBuilder = new StringBuilder();
        context.disableDefaultConstraintViolation();
        if (editCustomerRequest.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL) {
            if (editCustomerRequest.getCustomerType() == CustomerType.PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY
                    || editCustomerRequest.getCustomerType() == CustomerType.LEGAL_ENTITY) {

                if (editCustomerRequest.getOwnershipFormId() == null) {
                    stringBuilder.append("Form of Ownership is required; ");
                    correct = false;
                }

                if (editCustomerRequest.getEconomicBranchId() == null) {
                    stringBuilder.append("Economic Branch is required; ");
                    correct = false;
                }

                if (editCustomerRequest.getMainSubjectOfActivity() == null) {
                    stringBuilder.append("Main Subject of Activity is required; ");
                    correct = false;
                }
            }

            if (editCustomerRequest.getSegmentIds() == null || editCustomerRequest.getSegmentIds().isEmpty()) {
                stringBuilder.append("Minimum one segment is required; ");
                correct = false;
            }

            if (editCustomerRequest.getAddress() != null) {
                if (editCustomerRequest.getAddress().getForeign() != null
                        && editCustomerRequest.getAddress().getForeign()) {
                    ForeignAddressData foreignAddressData = editCustomerRequest.getAddress().getForeignAddressData();
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
                    LocalAddressData localAddressData = editCustomerRequest.getAddress().getLocalAddressData();
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

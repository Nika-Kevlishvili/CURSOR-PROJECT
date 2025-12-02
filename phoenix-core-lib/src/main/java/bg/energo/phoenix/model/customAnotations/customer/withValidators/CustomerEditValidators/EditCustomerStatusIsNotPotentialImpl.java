package bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators;

import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import bg.energo.phoenix.model.request.customer.ForeignAddressData;
import bg.energo.phoenix.model.request.customer.LocalAddressData;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
            if ((editCustomerRequest.getBusinessActivity() != null && editCustomerRequest.getBusinessActivity())
                    || editCustomerRequest.getCustomerType() == CustomerType.LEGAL_ENTITY) {

                if (editCustomerRequest.getOwnershipFormId() == null) {
                    stringBuilder.append("ownershipFormId-Form of Ownership is required;");
                    correct = false;
                }

                if (editCustomerRequest.getEconomicBranchId() == null) {
                    stringBuilder.append("economicBranchId-Economic Branch is required;");
                    correct = false;
                }

                if (editCustomerRequest.getMainSubjectOfActivity() == null || editCustomerRequest.getMainSubjectOfActivity().isBlank()) {
                    stringBuilder.append("mainSubjectOfActivity-Main Subject of Activity is required;");
                    correct = false;
                }
            }

            if (editCustomerRequest.getSegmentIds() == null || editCustomerRequest.getSegmentIds().isEmpty()) {
                stringBuilder.append("segmentIds-Minimum one segment is required;");
                correct = false;
            }

            if (editCustomerRequest.getAddress() != null) {
                if (editCustomerRequest.getAddress().getForeign() != null
                        && editCustomerRequest.getAddress().getForeign()) {
                    ForeignAddressData foreignAddressData = editCustomerRequest.getAddress().getForeignAddressData();
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
                    LocalAddressData localAddressData = editCustomerRequest.getAddress().getLocalAddressData();
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

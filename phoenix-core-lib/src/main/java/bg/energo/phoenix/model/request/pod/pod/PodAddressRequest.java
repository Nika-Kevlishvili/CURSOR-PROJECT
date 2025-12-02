package bg.energo.phoenix.model.request.pod.pod;

import bg.energo.phoenix.model.customAnotations.customer.manager.AddressFieldValidator;
import bg.energo.phoenix.model.entity.pod.pod.PodContractResponse;
import jakarta.validation.Valid;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;

@Data
public class PodAddressRequest {
    private Boolean foreign;

    @Valid
    private PODForeignAddressData foreignAddressData;
    @Valid
    private PODLocalAddressData localAddressData;
    @AddressFieldValidator(value = "addressRequest.number")
    @Length(min = 1, max = 32, message = "addressRequest.number-number length must be between {min} and {max};")
    private String number;
    @AddressFieldValidator(value = "addressRequest.additionalInformation")
    @Length(min = 1, max = 512, message = "addressRequest.additionalInformation-additionalInformation length must be between {min} and {max};")
    private String additionalInformation;
    @AddressFieldValidator(value = "addressRequest.block")
    @Length(min = 1, max = 128, message = "addressRequest.block-block length must be between {min} and {max};")
    private String block;
    @AddressFieldValidator(value = "addressRequest.entrance")
    @Length(min = 1, max = 32, message = "addressRequest.entrance-entrance length must be between {min} and {max};")
    private String entrance;
    @AddressFieldValidator(value = "addressRequest.floor")
    @Length(min = 1, max = 16, message = "addressRequest.floor-floor length must be between {min} and {max};")
    private String floor;
    @AddressFieldValidator(value = "addressRequest.apartment")
    @Length(min = 1, max = 32, message = "addressRequest.apartment-apartment length must be between {min} and {max};")
    private String apartment;
    @AddressFieldValidator(value = "addressRequest.mailbox")
    @Length(min = 1, max = 32, message = "addressRequest.mailbox-mailbox length must be between {min} and {max};")
    private String mailbox;

    public boolean equalsResponse(PodContractResponse that) {
        if (!Objects.equals(foreign, that.getForeign())) return false;
        if (Boolean.TRUE.equals(foreign)) {
            if (!Objects.equals(foreignAddressData.getCountryId(), that.getCountry().getId())) return false;
            if (!Objects.equals(foreignAddressData.getRegion(), that.getRegionForeign())) return false;
            if (!Objects.equals(foreignAddressData.getMunicipality(), that.getMunicipalityForeign())) return false;
            if (!Objects.equals(foreignAddressData.getPopulatedPlace(), that.getPopulatedPlaceForeign())) return false;
            if (!Objects.equals(foreignAddressData.getZipCode(), that.getZipCodeForeign())) return false;
            if (!Objects.equals(foreignAddressData.getDistrict(), that.getDistrictForeign())) return false;
            if (!Objects.equals(foreignAddressData.getResidentialArea(), that.getResidentialAreaForeign()))
                return false;
            if (!Objects.equals(foreignAddressData.getResidentialAreaType(), that.getResidentialAreaTypeForeign()))
                return false;
            if (!Objects.equals(foreignAddressData.getStreetType(), that.getStreetTypeForeign())) return false;
            if (!Objects.equals(foreignAddressData.getStreet(), that.getStreetForeign())) return false;
        } else {
            if (!Objects.equals(localAddressData.getCountryId(), that.getCountry().getId())) return false;
            if (!Objects.equals(localAddressData.getRegionId(), that.getRegion().getId())) return false;
            if (!Objects.equals(localAddressData.getMunicipalityId(), that.getMunicipality().getId())) return false;
            if (!Objects.equals(localAddressData.getPopulatedPlaceId(), that.getPopulatedPlace().getId())) return false;
            if (!Objects.equals(localAddressData.getZipCodeId(), that.getZipCode() == null ? null : that.getZipCode().getId()))
                return false;
            if (!Objects.equals(localAddressData.getDistrictId(), that.getDistrict() == null ? null : that.getDistrict().getId()))
                return false;
            if (!Objects.equals(localAddressData.getResidentialAreaId(), that.getResidentialArea() == null ? null : that.getResidentialArea().getId()))
                return false;
            if (!Objects.equals(localAddressData.getStreetId(), that.getStreets() == null ? null : that.getStreets().getId()))
                return false;
        }

        if (!Objects.equals(number, that.getStreetNumber())) return false;
        if (!Objects.equals(additionalInformation, that.getAddressAdditionalInfo())) return false;
        if (!Objects.equals(block, that.getBlock())) return false;
        if (!Objects.equals(entrance, that.getEntrance())) return false;
        if (!Objects.equals(floor, that.getFloor())) return false;
        if (!Objects.equals(apartment, that.getApartment())) return false;
        return Objects.equals(mailbox, that.getMailbox());
    }

}

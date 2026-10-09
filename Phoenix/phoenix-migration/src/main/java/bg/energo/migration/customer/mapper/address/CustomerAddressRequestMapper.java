package bg.energo.migration.customer.mapper.address;

import bg.energo.migration.customer.entity.CustomerGeneralData;
import bg.energo.migration.integration.phoenix.customer.model.request.address.CustomerAddressRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.address.foreignaddressdata.ForeignAddressData;
import bg.energo.migration.integration.phoenix.customer.model.request.address.localaddressdata.LocalAddressData;
import org.springframework.stereotype.Service;

@Service
public class CustomerAddressRequestMapper {

    public CustomerAddressRequest map(CustomerGeneralData customerGeneralData) {

        return CustomerAddressRequest
                .builder()
                .foreign(customerGeneralData.getForeignAddress())
                .localAddressData(mapLocalAddressData(customerGeneralData))
                .foreignAddressData(mapForeignAddressData(customerGeneralData))
                .number(customerGeneralData.getStreetNumber())
                .block(customerGeneralData.getBlock())
                .apartment(customerGeneralData.getApartment())
                .build();
    }

    private ForeignAddressData mapForeignAddressData(CustomerGeneralData customerGeneralData) {
        if(!customerGeneralData.getForeignAddress()){
            return null;
        }
        return ForeignAddressData
                .builder()
                .countryId(customerGeneralData.getCountryId())
                .region(customerGeneralData.getRegionForeign())
                .municipality(customerGeneralData.getMunicipalityForeign())
                .populatedPlace(customerGeneralData.getPopulatedPlaceForeign())
                .zipCode(customerGeneralData.getZipCodeForeign())
                .district(customerGeneralData.getDistrictForeign())
                .street(customerGeneralData.getStreetForeign())
                .residentialArea(customerGeneralData.getResidentialAreaForeign())
                .streetType(customerGeneralData.getStreetType())
                .build();
    }

    private LocalAddressData mapLocalAddressData(CustomerGeneralData customerGeneralData) {
               if(customerGeneralData.getForeignAddress()){
                   return null;
               }
        return LocalAddressData
                .builder()
                .countryId(customerGeneralData.getCountryId())
                .regionId(customerGeneralData.getRegionId())
                .municipalityId(customerGeneralData.getMunicipalityId())
                .populatedPlaceId(customerGeneralData.getPopulatedPlaceId())
                .zipCodeId(customerGeneralData.getZipCodeId())
                .streetId(customerGeneralData.getStreetId())
                .streetType(customerGeneralData.getStreetType())
                .build();
    }
}

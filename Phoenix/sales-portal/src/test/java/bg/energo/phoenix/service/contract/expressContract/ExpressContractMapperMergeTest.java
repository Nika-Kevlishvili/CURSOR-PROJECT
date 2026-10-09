package bg.energo.phoenix.service.contract.expressContract;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.contract.express.ExpressContractBusinessCustomer;
import bg.energo.phoenix.model.request.contract.express.ExpressContractPrivateCustomer;
import bg.energo.phoenix.model.request.customer.CustomerAddressRequest;
import bg.energo.phoenix.model.request.customer.LocalAddressData;
import bg.energo.phoenix.model.response.contract.express.ExpressContractCustomerShortResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressContractMapperMergeTest {

    @Test
    void mergePrivateCustomerDetails_fillsOmittedNames_preservesProvidedKyc() {
        ExpressContractPrivateCustomer partial = new ExpressContractPrivateCustomer();
        partial.setKycPassed(false);

        ExpressContractPrivateCustomer fromDb = new ExpressContractPrivateCustomer();
        fromDb.setFirstName("ИВАН");
        fromDb.setFirstNameTranslated("IVAN");
        fromDb.setLastName("ИВАНОВ");
        fromDb.setLastNameTranslated("IVANOV");
        fromDb.setMiddleName("ПЕТРОВ");
        fromDb.setMiddleNameTranslated("PETROV");
        fromDb.setBirthDate("1990-01-01");
        fromDb.setKycPassed(true);
        fromDb.setKycExpirationDate(LocalDate.now().plusYears(1));

        ExpressContractPrivateCustomer merged = ExpressContractMapper.mergePrivateCustomerDetails(partial, fromDb);

        assertThat(merged.getFirstName()).isEqualTo("ИВАН");
        assertThat(merged.getFirstNameTranslated()).isEqualTo("IVAN");
        assertThat(merged.getLastName()).isEqualTo("ИВАНОВ");
        assertThat(merged.getLastNameTranslated()).isEqualTo("IVANOV");
        assertThat(merged.getMiddleName()).isEqualTo("ПЕТРОВ");
        assertThat(merged.getBirthDate()).isEqualTo("1990-01-01");
        assertThat(merged.getKycPassed()).isFalse();
        assertThat(merged.getKycExpirationDate()).isNull();
    }

    @Test
    void mergePrivateCustomerDetails_preservesProvidedNameOverrides() {
        ExpressContractPrivateCustomer partial = new ExpressContractPrivateCustomer();
        partial.setFirstName("NEWNAME");
        partial.setKycPassed(true);

        ExpressContractPrivateCustomer fromDb = new ExpressContractPrivateCustomer();
        fromDb.setFirstName("ИВАН");
        fromDb.setFirstNameTranslated("IVAN");
        fromDb.setLastName("ИВАНОВ");
        fromDb.setLastNameTranslated("IVANOV");
        fromDb.setKycPassed(true);
        fromDb.setKycExpirationDate(LocalDate.of(2030, 1, 1));

        ExpressContractPrivateCustomer merged = ExpressContractMapper.mergePrivateCustomerDetails(partial, fromDb);

        assertThat(merged.getFirstName()).isEqualTo("NEWNAME");
        assertThat(merged.getFirstNameTranslated()).isEqualTo("IVAN");
        assertThat(merged.getLastName()).isEqualTo("ИВАНОВ");
        assertThat(merged.getKycPassed()).isTrue();
        assertThat(merged.getKycExpirationDate()).isEqualTo(LocalDate.of(2030, 1, 1));
    }

    @Test
    void mergePrivateCustomerDetails_replacesNullTargetWithSource() {
        ExpressContractPrivateCustomer fromDb = new ExpressContractPrivateCustomer();
        fromDb.setFirstName("ИВАН");

        assertThat(ExpressContractMapper.mergePrivateCustomerDetails(null, fromDb)).isSameAs(fromDb);
    }

    @Test
    void toPrivateCustomerDetails_readsNamesFromEntities() {
        Customer customer = new Customer();
        customer.setCustomerType(CustomerType.PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY);
        customer.setBirthDate(LocalDate.of(1985, 5, 5));
        customer.setKycPassed(false);

        CustomerDetails details = new CustomerDetails();
        details.setName("МАРИЯ");
        details.setNameTransl("MARIA");
        details.setLastName("ПЕТРОВА");
        details.setLastNameTransl("PETROVA");

        ExpressContractPrivateCustomer mapped = ExpressContractMapper.toPrivateCustomerDetails(customer, details);

        assertThat(mapped.getFirstName()).isEqualTo("МАРИЯ");
        assertThat(mapped.getFirstNameTranslated()).isEqualTo("MARIA");
        assertThat(mapped.getLastName()).isEqualTo("ПЕТРОВА");
        assertThat(mapped.getLastNameTranslated()).isEqualTo("PETROVA");
        assertThat(mapped.getBirthDate()).isEqualTo("1985-05-05");
        assertThat(mapped.getKycPassed()).isFalse();
    }

    @Test
    void mergeBusinessCustomerDetails_fillsOmittedName() {
        ExpressContractBusinessCustomer partial = new ExpressContractBusinessCustomer();
        partial.setProcurementLaw(true);

        ExpressContractCustomerShortResponse shortResponse = new ExpressContractCustomerShortResponse();
        shortResponse.setCustomerType(CustomerType.LEGAL_ENTITY);
        shortResponse.setName("ACME EOOD");
        shortResponse.setNameTransl("ACME EOOD");
        shortResponse.setPublicProcurementLaw(false);

        ExpressContractBusinessCustomer fromDb = ExpressContractMapper.toBusinessCustomerDetails(shortResponse);
        ExpressContractBusinessCustomer merged = ExpressContractMapper.mergeBusinessCustomerDetails(partial, fromDb);

        assertThat(merged.getName()).isEqualTo("ACME EOOD");
        assertThat(merged.getNameTranslated()).isEqualTo("ACME EOOD");
        assertThat(merged.getProcurementLaw()).isTrue();
    }

    @Test
    void mergeCustomerAddressRequest_fillsOmittedLocalAddress_preservesProvidedNumber() {
        CustomerAddressRequest partial = new CustomerAddressRequest();
        partial.setForeign(false);
        partial.setNumber("12A");

        CustomerAddressRequest fromDb = new CustomerAddressRequest();
        fromDb.setForeign(false);
        fromDb.setNumber("99");
        fromDb.setBlock("B");
        LocalAddressData local = new LocalAddressData();
        local.setCountryId(3001L);
        local.setPopulatedPlaceId(3002L);
        local.setZipCodeId(3003L);
        fromDb.setLocalAddressData(local);

        CustomerAddressRequest merged = ExpressContractMapper.mergeCustomerAddressRequest(partial, fromDb);

        assertThat(merged.getForeign()).isFalse();
        assertThat(merged.getNumber()).isEqualTo("12A");
        assertThat(merged.getBlock()).isEqualTo("B");
        assertThat(merged.getLocalAddressData().getCountryId()).isEqualTo(3001L);
        assertThat(merged.getLocalAddressData().getPopulatedPlaceId()).isEqualTo(3002L);
        assertThat(merged.getForeignAddressData()).isNull();
    }
}

package bg.energo.migration.customer.mapper.communication;

import bg.energo.migration.customer.entity.CustomerGeneralData;
import bg.energo.migration.customer.entity.communication.CustomerCommunicationView;
import bg.energo.migration.customer.entity.communication.contactpurpose.CustomerCommContactPurposeView;
import bg.energo.migration.customer.entity.communication.contacts.CustomerCommContactView;
import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerCommContactTypes;
import bg.energo.migration.integration.phoenix.customer.model.request.address.foreignaddressdata.ForeignAddressData;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.CreateCustomerCommunicationsRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.commaddress.CustomerCommAddressRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.commaddress.CustomerCommForeignAddressData;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.commaddress.CustomerCommLocalAddressData;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.communicationcontacts.CreateCommunicationContactRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.contactpurposes.CreateContactPurposeRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerCommunicationDataMapper {

    public CreateCustomerCommunicationsRequest map(
            CustomerCommunicationView customerCommunicationView,
            List<CustomerCommContactView> customerCommContactViewList,
            List<CustomerCommContactPurposeView> customerCommContactPurposeViews
    ) {

        List<CreateContactPurposeRequest> contactPurposes = customerCommContactPurposeViews
                .stream()
                .map(this::mapToContactPurpose)
                .toList();

        List<CreateCommunicationContactRequest> communicationContacts = new ArrayList<>();
        for(CustomerCommContactView contact : customerCommContactViewList){
            List<CustomerCommContactView> splitedContact = splitContactIfNecessary(contact);
            for(CustomerCommContactView splited : splitedContact){
                if (communicationContacts.stream().anyMatch(f -> f.getContactValue().equals(splited.getContactValue()))) {
                    continue;
                }
                communicationContacts.add(CreateCommunicationContactRequest
                        .builder()
                        .sendSms(splited.getSendSms())
                        .status(splited.getStatus())
                        .contactType(splited.getContactType())
                        .contactValue(splited.getContactValue())
                        .build());
            }
        }
        setSendSMSForMobile(communicationContacts);


        return CreateCustomerCommunicationsRequest
                .builder()
                .contactTypeName(
                        customerCommunicationView != null ? customerCommunicationView.getContactTypeName() : null
                )
                .address(mapToCustomerCommAddressRequest(customerCommunicationView))
                .status(
                        customerCommunicationView != null ? customerCommunicationView.getStatus() : null
                )
                .contactPurposes(contactPurposes)
                .communicationContacts(communicationContacts)
                .build();
    }

    private CreateContactPurposeRequest mapToContactPurpose(
            CustomerCommContactPurposeView customerCommContactPurposeView
    ) {
        return CreateContactPurposeRequest
                .builder()
                .contactPurposeId(customerCommContactPurposeView.getId())
                .status(customerCommContactPurposeView.getStatus())
                .build();
    }

    private CreateCommunicationContactRequest mapToCommunicationContact(
            CustomerCommContactView customerCommContactView
    ) {
        return CreateCommunicationContactRequest
                .builder()
                .sendSms(customerCommContactView.getSendSms())
                .status(customerCommContactView.getStatus())
                .contactType(customerCommContactView.getContactType())
                .contactValue(customerCommContactView.getContactValue())
                .build();
    }

    private List<CreateCommunicationContactRequest> setSendSMSForMobile(List<CreateCommunicationContactRequest> communicationContacts){
        List<CreateCommunicationContactRequest> mobileContact = communicationContacts.stream()
                .filter(contact -> contact.getContactType() == CustomerCommContactTypes.MOBILE_NUMBER && contact.getSendSms())
                .toList();
        if(mobileContact.size() > 1){
            mobileContact.forEach(contact -> contact.setSendSms(false));
            mobileContact.get(0).setSendSms(true);
        }
        return communicationContacts;
    }

    private CustomerCommAddressRequest mapToCustomerCommAddressRequest(
            CustomerCommunicationView customerCommunicationView
    ) {
        if (customerCommunicationView == null) return null;

        return CustomerCommAddressRequest
                .builder()
                .foreign(customerCommunicationView.getForeignAddress())
                .localAddressData(
                        mapToCustomerCommLocalAddressData(
                                customerCommunicationView
                        )
                )
                .foreignAddressData(mapToCustomerCommForeignAddressData(customerCommunicationView))
                .number(
                        customerCommunicationView.getStreetNumber()
                )
                .block(customerCommunicationView.getBlock())
                .apartment(customerCommunicationView.getApartment())
                .build();
    }

    private CustomerCommForeignAddressData mapToCustomerCommForeignAddressData( CustomerCommunicationView customerCommunicationView) {
        if(!customerCommunicationView.getForeignAddress()){
            return null;
        }
        return CustomerCommForeignAddressData
                .builder()
                .countryId(customerCommunicationView.getCountryId())
                .region(customerCommunicationView.getRegionForeign())
                .municipality(customerCommunicationView.getMunicipalityForeign())
                .populatedPlace(customerCommunicationView.getPopulatedPlaceForeign())
                .zipCode(customerCommunicationView.getZipCodeForeign())
                .district(customerCommunicationView.getDistrictForeign())
                .street(customerCommunicationView.getStreetForeign())
                .residentialArea(customerCommunicationView.getResidentialAreaForeign())
                .streetType(customerCommunicationView.getStreetType())
                .build();
    }

    private CustomerCommLocalAddressData mapToCustomerCommLocalAddressData(
            CustomerCommunicationView customerCommunicationView
    ) {
        if(customerCommunicationView.getForeignAddress()){
            return null;
        }
        return CustomerCommLocalAddressData
                .builder()
                .countryId(customerCommunicationView.getCountryId())
                .regionId(customerCommunicationView.getRegionId())
                .municipalityId(customerCommunicationView.getMunicipalityId())
                .populatedPlaceId(customerCommunicationView.getPopulatedPlaceId())
                .zipCodeId(customerCommunicationView.getZipCodeId())
                .streetId(customerCommunicationView.getStreetId())
                .streetType(customerCommunicationView.getStreetType())
                .build();
    }

    private List<CustomerCommContactView> splitContactIfNecessary(CustomerCommContactView contact){
        List<CustomerCommContactView> contacts = new ArrayList<>();
        if(contact.getContactValue() != null){
            contact.setContactValue(contact.getContactValue().replaceAll(" ",""));
        }

        switch (contact.getContactType()) {
            case MOBILE_NUMBER, LANDLINE_PHONE, CALL_CENTER, FAX ->{
                if(contact.getContactValue().contains(";") || contact.getContactValue().contains("/")){
                    contacts.addAll(splitContactNumber(contact));
                }else{
                    CustomerCommContactView contactData = new CustomerCommContactView();
                    contactData.setContactType(contact.getContactType());
                    contactData.setContactValue(contact.getContactValue());
                    contactData.setStatus(contact.getStatus());
                    contactData.setSendSms(contact.getSendSms());
                    contactData.setCustomerId(contact.getCustomerId());

                    contacts.add(contactData);

                }
            }
            case EMAIL ->{
                if(contact.getContactValue().contains(";")){
                    contacts.addAll(splitEmailWithSemicolon(contact));
                }else{
                    CustomerCommContactView contactData = new CustomerCommContactView();
                    contactData.setContactType(contact.getContactType());
                    contactData.setContactValue(contact.getContactValue());
                    contactData.setStatus(contact.getStatus());
                    contactData.setSendSms(contact.getSendSms());
                    contactData.setCustomerId(contact.getCustomerId());
                    contacts.add(contactData);
                }
            }
            default -> {
                contacts.add(contact);
            }
        }
        return contacts;
    }

    private List<CustomerCommContactView> splitContactNumber(CustomerCommContactView contact){
        List<CustomerCommContactView> numberContacts = new ArrayList<>();
        ArrayList<String> contacts = new ArrayList<>();
        String contactValue = contact.getContactValue();
        if(contactValue.contains("(") && contactValue.contains(")")){
            contactValue = contactValue.replaceAll("[()]", "");
        }
        if(contactValue.contains(";")){
            contacts.addAll(List.of(contactValue.split(";")));
        }
        if(contactValue.contains("/")){
            contacts.addAll(List.of(contactValue.split("/")));
        }
        for(String contactNumber : contacts){
            CustomerCommContactView contactData = new CustomerCommContactView();
            contactData.setContactType(contact.getContactType());
            contactData.setContactValue(contactNumber);
            contactData.setStatus(contact.getStatus());
            contactData.setSendSms(contact.getSendSms());
            contactData.setCustomerId(contact.getCustomerId());
            numberContacts.add(contactData);
        }
        return numberContacts;
    }

    private List<CustomerCommContactView> splitEmailWithSemicolon(CustomerCommContactView contact){
        List<CustomerCommContactView> emailContacts = new ArrayList<>();
        String[] emails = contact.getContactValue().split(";");
        for(String email : emails){
            CustomerCommContactView contactData = new CustomerCommContactView();
            contactData.setContactType(CustomerCommContactTypes.EMAIL);
            contactData.setContactValue(email);
            contactData.setStatus(contact.getStatus());
            contactData.setSendSms(contact.getSendSms());
            contactData.setCustomerId(contact.getCustomerId());
            emailContacts.add(contactData);
        }
        return emailContacts;
    }

}
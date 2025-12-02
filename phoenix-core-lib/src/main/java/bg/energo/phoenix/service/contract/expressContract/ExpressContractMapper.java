package bg.energo.phoenix.service.contract.expressContract;

import bg.energo.phoenix.model.enums.contract.express.ExpressCommunicationTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCommunicationContactRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractCommunicationsRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractManagerRequest;
import bg.energo.phoenix.model.request.customer.communicationData.CreateCustomerCommunicationsRequest;
import bg.energo.phoenix.model.request.customer.communicationData.EditCustomerCommunicationsRequest;
import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.CreateCommunicationContactRequest;
import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.EditCommunicationContactRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.CreateContactPurposeRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.EditContactPurposeRequest;
import bg.energo.phoenix.model.request.customer.manager.CreateManagerRequest;
import bg.energo.phoenix.model.request.customer.manager.EditManagerRequest;

import java.util.List;

public class ExpressContractMapper {

    public static CreateManagerRequest createManagerRequest(ExpressContractManagerRequest request) {
        CreateManagerRequest createManagerRequest = new CreateManagerRequest();
        createManagerRequest.setTitleId(request.getTitleId());
        createManagerRequest.setName(request.getName());
        createManagerRequest.setMiddleName(request.getMiddleName());
        createManagerRequest.setSurname(request.getSurname());
        createManagerRequest.setPersonalNumber(request.getPersonalNumber());
        createManagerRequest.setStatus(Status.ACTIVE);
        createManagerRequest.setJobPosition(request.getJobPosition());
        createManagerRequest.setRepresentationMethodId(request.getRepresentationMethodId());
        return createManagerRequest;
    }

    public static EditManagerRequest editManagerRequest(ExpressContractManagerRequest request) {
        EditManagerRequest editManagerRequest = new EditManagerRequest();
        editManagerRequest.setId(request.getId());
        editManagerRequest.setTitleId(request.getTitleId());
        editManagerRequest.setName(request.getName());
        editManagerRequest.setMiddleName(request.getMiddleName());
        editManagerRequest.setSurname(request.getSurname());
        editManagerRequest.setStatus(Status.ACTIVE);
        editManagerRequest.setPersonalNumber(request.getPersonalNumber());
        editManagerRequest.setJobPosition(request.getJobPosition());
        editManagerRequest.setRepresentationMethodId(request.getRepresentationMethodId());
        return editManagerRequest;
    }

    public static CreateCustomerCommunicationsRequest createCommunications(ExpressContractCommunicationsRequest request, Long billingId, Long communicationId) {
        CreateCustomerCommunicationsRequest communications = new CreateCustomerCommunicationsRequest();
        communications.setStatus(Status.ACTIVE);
        communications.setAddress(request.getAddress());
        communications.setContactTypeName(request.getCommunicationTypes().getAddress());
        CreateContactPurposeRequest purposeRequest = new CreateContactPurposeRequest();
        //Todo Check logic with megi
        if(request.getCommunicationTypes().equals(ExpressCommunicationTypes.INVOICE_ISSUANCE)){
            purposeRequest.setContactPurposeId(billingId);
        } else if (request.getCommunicationTypes().equals(ExpressCommunicationTypes.CONTRACT_COMMUNICATION)) {
            purposeRequest.setContactPurposeId(communicationId);
        }
        purposeRequest.setStatus(Status.ACTIVE);
        communications.setContactPurposes(List.of(purposeRequest));

        List<CreateCommunicationContactRequest> contactRequests = request.getContactRequests().stream().map(ExpressContractMapper::createCommunicationsContact).toList();

        communications.setCommunicationContacts(contactRequests);
        return communications;
    }

    private static CreateCommunicationContactRequest createCommunicationsContact(ExpressContractCommunicationContactRequest x) {
        CreateCommunicationContactRequest createCommunicationContactRequest = new CreateCommunicationContactRequest();
        createCommunicationContactRequest.setContactType(x.getContactType());
        createCommunicationContactRequest.setContactValue(x.getContactValue());
        createCommunicationContactRequest.setSendSms(false);
        createCommunicationContactRequest.setStatus(Status.ACTIVE);
        return createCommunicationContactRequest;
    }

    private static EditCommunicationContactRequest editCommunicationsContact(ExpressContractCommunicationContactRequest x) {
        EditCommunicationContactRequest createCommunicationContactRequest = new EditCommunicationContactRequest();
        createCommunicationContactRequest.setContactType(x.getContactType());
        createCommunicationContactRequest.setContactValue(x.getContactValue());
        createCommunicationContactRequest.setSendSms(false);
        createCommunicationContactRequest.setStatus(Status.ACTIVE);
        return createCommunicationContactRequest;
    }

    public static EditCustomerCommunicationsRequest editCommunications(ExpressContractCommunicationsRequest request, Long billingId, Long communicationId){
        EditCustomerCommunicationsRequest communications = new EditCustomerCommunicationsRequest();
        communications.setStatus(Status.ACTIVE);
        communications.setAddress(request.getAddress());
        communications.setContactTypeName(request.getCommunicationTypes().getAddress());
        communications.setId(request.getId());

        EditContactPurposeRequest purposeRequest = new EditContactPurposeRequest();
        if(request.getCommunicationTypes().equals(ExpressCommunicationTypes.INVOICE_ISSUANCE)){
            purposeRequest.setContactPurposeId(billingId);
        } else if (request.getCommunicationTypes().equals(ExpressCommunicationTypes.CONTRACT_COMMUNICATION)) {
            purposeRequest.setContactPurposeId(communicationId);
        }
        purposeRequest.setStatus(Status.ACTIVE);
        communications.setContactPurposes(List.of(purposeRequest));

        List<EditCommunicationContactRequest> contactRequests = request.getContactRequests().stream().map(ExpressContractMapper::editCommunicationsContact).toList();

        communications.setCommunicationContacts(contactRequests);
        return communications;
    }

}

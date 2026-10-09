package bg.energo.migration.customer.mapper;

import bg.energo.migration.customer.entity.CustomerGeneralData;
import bg.energo.migration.customer.entity.accountmanager.CustomerAccountManagerView;
import bg.energo.migration.customer.entity.communication.CustomerCommunicationView;
import bg.energo.migration.customer.entity.communication.contactpurpose.CustomerCommContactPurposeView;
import bg.energo.migration.customer.entity.communication.contacts.CustomerCommContactView;
import bg.energo.migration.customer.entity.manager.CustomerManagerView;
import bg.energo.migration.customer.entity.segment.CustomerSegmentView;
import bg.energo.migration.customer.mapper.accountmanager.CustomerAccountManagerRequestMapper;
import bg.energo.migration.customer.mapper.address.CustomerAddressRequestMapper;
import bg.energo.migration.customer.mapper.bankingdetails.CustomerBankingDetailsMapper;
import bg.energo.migration.customer.mapper.businesscustomerdetails.CustomerBusinessDetailsMapper;
import bg.energo.migration.customer.mapper.communication.CustomerCommunicationDataMapper;
import bg.energo.migration.customer.mapper.manager.CustomerManagerMapper;
import bg.energo.migration.customer.mapper.privatecustomerdetails.PrivateCustomerDetailsMapper;
import bg.energo.migration.customer.mapper.segment.CustomerSegmentMapper;
import bg.energo.migration.customer.models.CustomerManagerViewMiddleResponse;
import bg.energo.migration.customer.models.CustomerManagerViewResponse;
import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerType;
import bg.energo.migration.integration.phoenix.customer.model.request.CreateCustomerRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.businesscustomerdetails.BusinessCustomerDetails;
import bg.energo.migration.integration.phoenix.customer.model.request.privatecustomerdetails.PrivateCustomerDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerRequestMapper {
    private final CustomerSegmentMapper customerSegmentMapper;
    private final CustomerManagerMapper customerManagerMapper;
    private final CustomerAddressRequestMapper customerAddressRequestMapper;
    private final CustomerBankingDetailsMapper customerBankingDetailsMapper;
    private final PrivateCustomerDetailsMapper privateCustomerDetailsMapper;
    private final CustomerBusinessDetailsMapper customerBusinessDetailsMapper;
    private final CustomerCommunicationDataMapper customerCommunicationDataMapper;
    private final CustomerAccountManagerRequestMapper customerAccountManagerRequestMapper;


    public CreateCustomerRequest map(
            CustomerGeneralData customerGeneralData,
            CustomerCommunicationView customerCommunicationView,
            List<CustomerAccountManagerView> accountManagers,
            List<CustomerSegmentView> segments,
            List<CustomerManagerViewResponse> managers,
            List<CustomerCommContactPurposeView> purposes,
            List<CustomerCommContactView> commContacts
    ) {
        return CreateCustomerRequest
                .builder()
                .customerType(customerGeneralData.getCustomerType())
                .businessActivity(mapBusinessActivity(customerGeneralData))
                .customerIdentifier(customerGeneralData.getCustomerIdentifier())
                .foreign(customerGeneralData.getForeignEntityPerson())
                .marketingConsent(customerGeneralData.getMarketingCommConsent())
                .preferCommunicationInEnglish(customerGeneralData.getPreferCommunicationInEnglish())
                .oldCustomerNumber(customerGeneralData.getOldCustomerNumbers())
                .vatNumber(customerGeneralData.getVatNumber())
                .customerDetailStatus(customerGeneralData.getCustomerDetailStatus())
                .ownershipFormId(customerGeneralData.getOwnershipFormId())
                .economicBranchId(Boolean.FALSE.equals(mapBusinessActivity(customerGeneralData)) ? null : customerGeneralData.getEconomicBranchCiId())
                .mainSubjectOfActivity(Boolean.FALSE.equals(mapBusinessActivity(customerGeneralData)) ? null : customerGeneralData.getMainsActivitySubject())
                .privateCustomerDetails(mapPrivateCustomerDetails(customerGeneralData))
                .businessCustomerDetails(mapBusinessCustomerDetails(customerGeneralData))
                .address(customerAddressRequestMapper.map(customerGeneralData))
                .bankingDetails(customerBankingDetailsMapper.map(customerGeneralData))
                .accountManagers(customerAccountManagerRequestMapper.map(accountManagers))
                .segmentIds(customerSegmentMapper.map(segments))
                .managers(customerManagerMapper.map(managers))
                .communicationData(
                        List.of(customerCommunicationDataMapper
                                .map(
                                        customerCommunicationView,
                                        commContacts,
                                        purposes
                                )
                        )
                )
                .build();
    }

    private PrivateCustomerDetails mapPrivateCustomerDetails(CustomerGeneralData customerGeneralData) {
        return CustomerType.PRIVATE_CUSTOMER.equals(customerGeneralData.getCustomerType())
                ? privateCustomerDetailsMapper.map(customerGeneralData)
                : null;
    }

    private BusinessCustomerDetails mapBusinessCustomerDetails(CustomerGeneralData customerGeneralData) {
        return (CustomerType.LEGAL_ENTITY.equals(customerGeneralData.getCustomerType())
                ||
                (customerGeneralData.getBusinessActivity() != null && customerGeneralData.getBusinessActivity()))
                ? customerBusinessDetailsMapper.map(customerGeneralData)
                : null;
    }

    private Boolean mapBusinessActivity(CustomerGeneralData customerGeneralData) {
        if(CustomerType.LEGAL_ENTITY.equals(customerGeneralData.getCustomerType())) {
            return null;
        }
        if(customerGeneralData.getBusinessActivity() == null) {
            return false;
        }
        return customerGeneralData.getBusinessActivity();
    }
}

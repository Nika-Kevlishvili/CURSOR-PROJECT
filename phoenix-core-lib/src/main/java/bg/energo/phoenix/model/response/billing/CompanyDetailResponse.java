package bg.energo.phoenix.model.response.billing;

import bg.energo.phoenix.model.entity.billing.companyDetails.*;
import bg.energo.phoenix.model.request.billing.companyDetails.CompanyBankDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.CompanyCommunicationChannelDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.CompanyDetailedParameterDTO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailResponse {

    private Long id;
    private Long companyId;
    private String uic;
    private String vatNumber;
    private String numberUnderExciseDutiesTaxWhAct;
    private String name;
    private String nameTranslated;
    private String managementAddress;
    private String managementAddressTranslated;
    private List<CompanyDetailedParameterDTO> companyCommunicationAddressList;
    private List<CompanyDetailedParameterDTO> companyManagerList;
    private List<CompanyCommunicationChannelDTO> telephoneList;
    private List<CompanyCommunicationChannelDTO> emailList;
    private List<CompanyBankDTO> bankList;
    private List<CompanyDetailedParameterDTO> companyInvoiceIssuePlaceList;
    private List<CompanyDetailedParameterDTO> companyInvoiceCompilerList;
    private List<CompanyDetailVersionsResponse> companyDetailVersionsResponseList;
    private Long versionId;
    private CompanyDetailFileResponse companyDetailFileResponse;


    public static CompanyDetailResponse toCompanyDetailResponse(CompanyDetails companyDetails,
                                                                List<CompanyCommunicationAddress> companyCommunicationAddress,
                                                                List<CompanyManager> companyManagerList,
                                                                List<CompanyTelephone> telephoneList,
                                                                List<CompanyEmail> emailList,
                                                                List<CompanyBankDTO> bankList,
                                                                List<CompanyInvoiceIssuePlace> companyInvoiceIssuePlaceList,
                                                                List<CompanyInvoiceCompiler> companyInvoiceCompilerList,
                                                                List<CompanyDetailVersionsResponse> companyDetailVersionsResponseList,
                                                                Long companyId,
                                                                Long versionId,
                                                                CompanyDetailFileResponse companyDetailFileResponse
    ) {
        return CompanyDetailResponse.builder()
                .id(companyDetails.getId())
                .uic(companyDetails.getIdentifier())
                .vatNumber(companyDetails.getVatNumber())
                .numberUnderExciseDutiesTaxWhAct(companyDetails.getNumberUnderExciseDutiesTaxWhAct())
                .name(companyDetails.getName())
                .nameTranslated(companyDetails.getNameTranslated())
                .managementAddress(companyDetails.getManagementAddress())
                .managementAddressTranslated(companyDetails.getManagementAddressTranslated())
                .companyCommunicationAddressList(CompanyDetailedParameterDTO.fromCompanyCommunicationAddressList(companyCommunicationAddress))
                .companyManagerList(CompanyDetailedParameterDTO.fromCompanyManagerList(companyManagerList))
                .telephoneList(CompanyCommunicationChannelDTO.fromCompanyTelephoneList(telephoneList))
                .emailList(CompanyCommunicationChannelDTO.fromCompanyEmailList(emailList))
                .bankList(bankList)
                .companyInvoiceIssuePlaceList(CompanyDetailedParameterDTO.fromCompanyInvoiceIssuePlaceList(companyInvoiceIssuePlaceList))
                .companyInvoiceCompilerList(CompanyDetailedParameterDTO.fromCompanyInvoiceCompilerList(companyInvoiceCompilerList))
                .companyDetailVersionsResponseList(companyDetailVersionsResponseList)
                .companyId(companyId)
                .versionId(versionId)
                .companyDetailFileResponse(companyDetailFileResponse)
                .build();
    }
}

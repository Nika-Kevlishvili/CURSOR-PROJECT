package bg.energo.phoenix.model.request.billing.companyDetails;

import bg.energo.phoenix.model.customAnotations.billing.CompanyCommunicationChannelValidator;
import bg.energo.phoenix.model.customAnotations.billing.CompanyDetailedParameterValidator;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyBankDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyCommunicationChannelDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyDetailedParameterDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class CreateCompanyRequest {

    @NotNull(message = "uic-is required;")
    @NotBlank(message = "uic-should not be Blank;")
    @Length(min = 1, max = 32, message = "uic-length should be between {min} and {max};")
    private String uic;

    @NotNull(message = "vatNumber-is required;")
    @Size(max = 32, message = "vatNumber-length should be {min};")
    private String vatNumber;

    @Size(min = 1, max = 512, message = "numberUnderExciseDutiesTaxWhAct-length should be between {min} and {max};")
    private String numberUnderExciseDutiesTaxWhAct;

    @NotNull(message = "name-is required;")
    @NotBlank(message = "name-should not be Blank;")
    @Length(min = 1, max = 2048, message = "name-length should be between {min} and {max};")
    private String name;

    @NotNull(message = "nameTranslated-is required;")
    @NotBlank(message = "nameTranslated-should not be Blank;")
    @Length(min = 1, max = 2048, message = "nameTranslated-length should be between {min} and {max};")
    private String nameTranslated;

    @NotNull(message = "managementAddress-is required;")
    @NotBlank(message = "managementAddress-should not be Blank;")
    @Length(min = 1, max = 2048, message = "managementAddress-length should be between {min} and {max};")
    private String managementAddress;

    @NotNull(message = "managementAddressTranslated-is required;")
    @NotBlank(message = "managementAddressTranslated-should not be Blank;")
    @Length(min = 1, max = 2048, message = "managementAddressTranslated-length should be between {min} and {max};")
    private String managementAddressTranslated;

    @NotEmpty(message = "companyCommunicationAddressList-at least 1 record is required;")
    @CompanyDetailedParameterValidator(
            fieldPath = "companyCommunicationAddressList",
            maxSize = 2048)
    private List<BaseCompanyDetailedParameterDTO> companyCommunicationAddressList;

    @NotEmpty(message = "companyManagerList-at least 1 record is required;")
    @CompanyDetailedParameterValidator(
            fieldPath = "companyManagerList",
            maxSize = 2048)
    private List<BaseCompanyDetailedParameterDTO> companyManagerList;

    @NotEmpty(message = "telephoneList-at least 1 record is required;")
    @CompanyCommunicationChannelValidator(
            fieldPath = "telephoneList",
            maxSize = 2048)
    private List<BaseCompanyCommunicationChannelDTO> telephoneList;

    @NotEmpty(message = "emailList-at least 1 record is required;")
    @CompanyCommunicationChannelValidator(
            fieldPath = "emailList",
            maxSize = 2048)
    private List<BaseCompanyCommunicationChannelDTO> emailList;

    private List<@Valid BaseCompanyBankDTO> bankList;

    @NotEmpty(message = "companyInvoiceIssuePlaceList-at least 1 record is required;")
    @CompanyDetailedParameterValidator(
            fieldPath = "companyInvoiceIssuePlaceList",
            maxSize = 512)
    private List<BaseCompanyDetailedParameterDTO> companyInvoiceIssuePlaceList;

    @NotEmpty(message = "companyInvoiceCompilerList-at least 1 record is required;")
    @CompanyDetailedParameterValidator(
            fieldPath = "companyInvoiceCompilerList",
            maxSize = 512)
    private List<BaseCompanyDetailedParameterDTO> companyInvoiceCompilerList;

    private Long logoId;

}

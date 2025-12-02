package bg.energo.phoenix.model.request.billing.companyDetails;

import bg.energo.phoenix.model.customAnotations.billing.CompanyCommunicationChannelValidator;
import bg.energo.phoenix.model.customAnotations.billing.CompanyDetailedParameterValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.List;

@Data
public class EditCompanyRequest {
    @NotNull(message = "uic-is required;")
    @NotBlank(message = "uic-should not be Blank;")
    @Length(min = 1, max = 32, message = "uic-length should be between {min} and {max};")
    private String uic;

    @NotNull(message = "vatNumber-is required;")
    @Length(max = 32, message = "vatNumber-length should be between {min} and {max};")
    private String vatNumber;

    @Length(min = 1, max = 512, message = "numberUnderExciseDutiesTaxWhAct-length should be between {min} and {max};")
    @Pattern(regexp = "^[0-9A-Я]+$", message = "numberUnderExciseDutiesTaxWhAct-numberUnderExciseDutiesTaxWhAct does not match regex;")
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

    @CompanyDetailedParameterValidator(
            fieldPath = "companyCommunicationAddressList",
            maxSize = 2048)
    private List<CompanyDetailedParameterDTO> companyCommunicationAddressList;

    @CompanyDetailedParameterValidator(
            fieldPath = "companyManagerList",
            maxSize = 2048)
    private List<CompanyDetailedParameterDTO> companyManagerList;

    @CompanyCommunicationChannelValidator(
            fieldPath = "telephoneList",
            maxSize = 2048)
    private List<CompanyCommunicationChannelDTO> telephoneList;

    @CompanyCommunicationChannelValidator(
            fieldPath = "emailList",
            maxSize = 2048)
    private List<CompanyCommunicationChannelDTO> emailList;

    private List<@Valid CompanyBankDTO> bankList;

    @CompanyDetailedParameterValidator(
            fieldPath = "companyInvoiceIssuePlaceList",
            maxSize = 512)
    private List<CompanyDetailedParameterDTO> companyInvoiceIssuePlaceList;

    @CompanyDetailedParameterValidator(
            fieldPath = "companyInvoiceCompilerList",
            maxSize = 512)
    private List<CompanyDetailedParameterDTO> companyInvoiceCompilerList;

    private Long logoId;

    private LocalDate versionStartDate;

}

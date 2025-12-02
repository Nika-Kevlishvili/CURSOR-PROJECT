package bg.energo.phoenix.model.request.billing.companyDetails;

import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyCommunicationAddress;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyInvoiceCompiler;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyInvoiceIssuePlace;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyManager;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyDetailedParameterDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CompanyDetailedParameterDTO extends BaseCompanyDetailedParameterDTO {
    private Long id;

    public static CompanyDetailedParameterDTO toDTO(String parameter, String parameterTranslated, Long id) {
        CompanyDetailedParameterDTO companyDetailedParameterDTO = new CompanyDetailedParameterDTO();
        companyDetailedParameterDTO.setParameter(parameter);
        companyDetailedParameterDTO.setParameterTranslated(parameterTranslated);
        companyDetailedParameterDTO.setId(id);
        return companyDetailedParameterDTO;
    }

    public static List<CompanyDetailedParameterDTO> fromCompanyCommunicationAddressList(List<CompanyCommunicationAddress> dbObjList) {
        List<CompanyDetailedParameterDTO> dtoList = new ArrayList<>();
        for (CompanyCommunicationAddress dbObj : dbObjList) {
            dtoList.add(toDTO(dbObj.getAddress(), dbObj.getAddressTranslated(), dbObj.getId()));
        }
        return dtoList;
    }

    public static List<CompanyDetailedParameterDTO> fromCompanyManagerList(List<CompanyManager> dbObjList) {
        List<CompanyDetailedParameterDTO> dtoList = new ArrayList<>();
        for (CompanyManager dbObj : dbObjList) {
            dtoList.add(toDTO(dbObj.getManager(), dbObj.getManagerTranslated(), dbObj.getId()));
        }
        return dtoList;
    }

    public static List<CompanyDetailedParameterDTO> fromCompanyInvoiceIssuePlaceList(List<CompanyInvoiceIssuePlace> dbObjList) {
        List<CompanyDetailedParameterDTO> dtoList = new ArrayList<>();
        for (CompanyInvoiceIssuePlace dbObj : dbObjList) {
            dtoList.add(toDTO(dbObj.getInvoiceIssuePlace(), dbObj.getInvoiceIssuePlaceTranslated(), dbObj.getId()));
        }
        return dtoList;
    }

    public static List<CompanyDetailedParameterDTO> fromCompanyInvoiceCompilerList(List<CompanyInvoiceCompiler> dbObjList) {
        List<CompanyDetailedParameterDTO> dtoList = new ArrayList<>();
        for (CompanyInvoiceCompiler dbObj : dbObjList) {
            dtoList.add(toDTO(dbObj.getInvoiceCompiler(), dbObj.getInvoiceCompilerTranslated(), dbObj.getId()));
        }
        return dtoList;
    }

}

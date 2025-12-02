package bg.energo.phoenix.service.billing.model.persistance;

public interface CompanyDetailedInformationModel {
    Long getCompanyDetailId();

    String getCompanyAddressList();

    String getCompanyAddressTrslList();

    String getCompanyManagerList();

    String getCompanyManagerTrslList();

    String getCompanyEmailList();

    String getCompanyPhoneList();

    String getCompanyBankList();

    String getCompanyBicList();

    String getCompanyIbanList();

    String getCompanyIssuingPlaceList();

    String getCompanyIssuingPlaceTrslList();

    String getCompanyComplierList();

    String getCompanyComplierTrslList();

    String getCompanyUIC();

    String getCompanyVATNumber();

    String getCompanyExciseNumber();

    String getCompanyName();

    String getCompanyNameTrsl();

    String getCompanyHeadquarterAddress();

    String getCompanyHeadquarterAddressTrsl();

}

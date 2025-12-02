package bg.energo.phoenix.service.billing.model.impl;

import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.util.epb.EPBListUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class CompanyDetailedInformationModelImpl {
    public List<String> CompanyAddressList = new ArrayList<>();
    public List<String> CompanyAddressTrslList = new ArrayList<>();
    public List<String> CompanyManagerList = new ArrayList<>();
    public List<String> CompanyManagerTrslList = new ArrayList<>();
    public List<String> CompanyEmailList = new ArrayList<>();
    public List<String> CompanyPhoneList = new ArrayList<>();
    public List<String> CompanyBankList = new ArrayList<>();
    public List<String> CompanyBicList = new ArrayList<>();
    public List<String> CompanyIbanList = new ArrayList<>();
    public List<String> CompanyIssuingPlaceList = new ArrayList<>();
    public List<String> CompanyIssuingPlaceTrslList = new ArrayList<>();
    public List<String> CompanyComplierList = new ArrayList<>();
    public List<String> CompanyComplierTrslList = new ArrayList<>();
    public String CompanyUIC;
    public String CompanyVATNumber;
    public String CompanyExciseNumber;
    public String CompanyName;
    public String CompanyNameTrsl;
    public String CompanyHeadquarterAddress;
    public String CompanyHeadquarterAddressTrsl;
    public byte[] CompanyLogo = new byte[]{};

    public void fillCompanyDetailedInformation(CompanyDetailedInformationModel model) {
        this.CompanyAddressList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyAddressList());
        this.CompanyAddressTrslList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyAddressTrslList());
        this.CompanyManagerList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyManagerList());
        this.CompanyManagerTrslList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyManagerTrslList());
        this.CompanyEmailList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyEmailList());
        this.CompanyPhoneList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyPhoneList());
        this.CompanyBankList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyBankList());
        this.CompanyBicList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyBicList());
        this.CompanyIbanList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyIbanList());
        this.CompanyIssuingPlaceList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyIssuingPlaceList());
        this.CompanyIssuingPlaceTrslList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyIssuingPlaceTrslList());
        this.CompanyComplierList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyComplierList());
        this.CompanyComplierTrslList = EPBListUtils.convertDBStringArrayIntoListString(model.getCompanyComplierTrslList());
        this.CompanyUIC = model.getCompanyUIC();
        this.CompanyVATNumber = model.getCompanyVATNumber();
        this.CompanyExciseNumber = model.getCompanyExciseNumber();
        this.CompanyName = model.getCompanyName();
        this.CompanyNameTrsl = model.getCompanyNameTrsl();
        this.CompanyHeadquarterAddress = model.getCompanyHeadquarterAddress();
        this.CompanyHeadquarterAddressTrsl = model.getCompanyHeadquarterAddressTrsl();
    }
}

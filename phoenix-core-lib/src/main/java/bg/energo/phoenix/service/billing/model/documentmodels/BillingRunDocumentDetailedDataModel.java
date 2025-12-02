package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class BillingRunDocumentDetailedDataModel {
    @Getter
    public String PODID;
    public String PODAdditionalID;
    public String PODName;
    public String PODAddressComb;
    public String PODPlace;
    public String PODZip;
    public String Profile;
    public String SLP;
    public List<BillingRunDocumentDetailedDataPriceComponentScale> TablePCScales = new ArrayList<>();
    public List<BillingRunDocumentDetailedDataPriceComponentProfileModel> TablePCProfiles = new ArrayList<>();
    public List<BillingRunExcludedPriceComponentSummaryModel> TableExcludedPC = new ArrayList<>();
    public List<BillingRunDocumentDetailedDataScaleDataModel> TableScalesData = new ArrayList<>();
    public List<BillingRunDocumentDetailedDataCompensations> TableCompensations=new ArrayList<>();
}

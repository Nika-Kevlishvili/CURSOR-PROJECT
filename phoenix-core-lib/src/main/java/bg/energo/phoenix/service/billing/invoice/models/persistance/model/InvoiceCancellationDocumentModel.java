package bg.energo.phoenix.service.billing.invoice.models.persistance.model;

import bg.energo.phoenix.service.billing.invoice.models.persistance.extractor.InvoiceCancellationDocumentModelExtractor;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceCancellationDocumentModel extends CompanyDetailedInformationModelImpl {
    public String CustomerNameComb;
    public String CustomerNameCombTrsl;
    public String CustomerIdentifer;
    public String CustomerVat;
    public String CustomerNumber;
    public String CustomerAddressComb;
    public String CustomerPopulatedPlace;
    public String CustomerZip;
    public String CustomerDistrict;
    public String CustomerQuarterRaType;
    public String CustomerQuarterRaName;
    public String CustomerStrBlvdType;
    public String CustomerStrBlvdName;
    public String CustomerStrBlvdNumber;
    public String CustomerBlock;
    public String CustomerEntrance;
    public String CustomerFloor;
    public String CustomerApartment;
    public String CustomerAdditionalInfo;
    public List<String> CustomerSegments = new ArrayList<>();
    public List<InvoiceCancellationDocumentManagerModel> Managers = new ArrayList<>();
    public String DocumentNumber;
    public String DocumentPrefix;
    public LocalDate DocumentDate;
    public String CanceledDocumentType;
    public String CanceledDocumentNumber;
    public String CanceledDocumentPrefix;
    public LocalDate CanceledDocumentDate;

    public void fillCancellationDocumentDetails(InvoiceCancellationDocumentModelExtractor extractor) {
        this.CustomerNameComb = extractor.getCustomerNameComb();
        this.CustomerNameCombTrsl = extractor.getCustomerNameCombTrsl();
        this.CustomerIdentifer = extractor.getCustomerIdentifer();
        this.CustomerVat = extractor.getCustomerVat();
        this.CustomerNumber = extractor.getCustomerNumber();
        this.CustomerAddressComb = extractor.getCustomerAddressComb();
        this.CustomerPopulatedPlace = extractor.getCustomerPopulatedPlace();
        this.CustomerZip = extractor.getCustomerZip();
        this.CustomerDistrict = extractor.getCustomerDistrict();
        this.CustomerQuarterRaType = extractor.getCustomerQuarterRaType();
        this.CustomerQuarterRaName = extractor.getCustomerQuarterRaName();
        this.CustomerStrBlvdType = extractor.getCustomerStrBlvdType();
        this.CustomerStrBlvdName = extractor.getCustomerStrBlvdName();
        this.CustomerStrBlvdNumber = extractor.getCustomerStrBlvdNumber();
        this.CustomerBlock = extractor.getCustomerBlock();
        this.CustomerEntrance = extractor.getCustomerEntrance();
        this.CustomerFloor = extractor.getCustomerFloor();
        this.CustomerApartment = extractor.getCustomerApartment();
        this.CustomerAdditionalInfo = extractor.getCustomerAdditionalInfo();
        String customerSegments = extractor.getCustomerSegments();
        this.CustomerSegments = StringUtils.isBlank(customerSegments) ? new ArrayList<>() : EPBListUtils.convertDBStringArrayIntoListString(customerSegments);
        String managers = extractor.getManagers();
        this.Managers = StringUtils.isBlank(managers) ? new ArrayList<>() : EPBJsonUtils.readList(managers, InvoiceCancellationDocumentManagerModel.class);
        this.DocumentNumber = extractor.getDocumentNumber();
        this.DocumentPrefix = extractor.getDocumentPrefix();
        this.DocumentDate = extractor.getDocumentDate();
        this.CanceledDocumentType = extractor.getCanceledDocumentType();
        this.CanceledDocumentNumber = extractor.getCanceledDocumentNumber();
        this.CanceledDocumentPrefix = extractor.getCanceledDocumentPrefix();
        this.CanceledDocumentDate = extractor.getDocumentDate();
    }
}

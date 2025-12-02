package bg.energo.phoenix.model.response.terms.copy;

import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.enums.product.term.terms.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TermsCopyResponse {
    private Long id;
    private String name;
    private Integer contractDeliveryActivationValue;
    private ContractDeliveryActivationType contractDeliveryActivationType;
    private Boolean contractDeliveryActivationAutoTermination;
    private Integer resigningDeadlineValue;
    private ResigningDeadlineType resigningDeadlineType;
    private List<SupplyActivation> supplyActivations;
    private Integer supplyActivationExactDateStartDay;
    private Integer generalNoticePeriodValue;
    private GeneralNoticePeriodType generalNoticePeriodType;
    private Integer noticeTermPeriodValue;
    private NoticeTermPeriodType noticeTermPeriodType;
    private Integer noticeTermDisconnectionPeriodValue;
    private NoticeTermDisconnectionPeriodType noticeTermDisconnectionPeriodType;
    private List<ContractEntryIntoForce> contractEntryIntoForces;
    private Integer contractEntryIntoForceFromExactDayOfMonthStartDay;
    private Boolean noInterestOnOverdueDebts;
    private TermStatus termStatus;
    private Long groupDetailsId;
    private Boolean isUsedInProduct;
    private Boolean isUsedInGroup;
    private Boolean isUsedInService;
    private Long productId;
    private List<StartOfContractInitialTerm> startsOfContractInitialTerms;
    private Integer startDayOfInitialContractTerm;
    private Integer firstDayOfTheMonthOfInitialContractTerm;

    private List<WaitForOldContractTermToExpire> waitForOldContractTermToExpires;

    List<InvoicePaymentTermsCopyResponse> invoicePaymentTerms;


    public TermsCopyResponse(Terms terms, List<InvoicePaymentTermsCopyResponse> invoicePaymentTerms) {
        this.id = terms.getId();
        this.name = terms.getName();
        this.contractDeliveryActivationValue = terms.getContractDeliveryActivationValue();
        this.contractDeliveryActivationType = terms.getContractDeliveryActivationType();
        this.contractDeliveryActivationAutoTermination = terms.getContractDeliveryActivationAutoTermination();
        this.resigningDeadlineValue = terms.getResigningDeadlineValue();
        this.resigningDeadlineType = terms.getResigningDeadlineType();
        this.contractEntryIntoForces = terms.getContractEntryIntoForces();
        this.supplyActivationExactDateStartDay = terms.getSupplyActivationExactDateStartDay();
        this.generalNoticePeriodValue = terms.getGeneralNoticePeriodValue();
        this.generalNoticePeriodType = terms.getGeneralNoticePeriodType();
        this.noticeTermPeriodValue = terms.getNoticeTermPeriodValue();
        this.noticeTermPeriodType = terms.getNoticeTermPeriodType();
        this.noticeTermDisconnectionPeriodValue = terms.getNoticeTermDisconnectionPeriodValue();
        this.noticeTermDisconnectionPeriodType = terms.getNoticeTermDisconnectionPeriodType();
        this.supplyActivations = terms.getSupplyActivations();
        this.contractEntryIntoForceFromExactDayOfMonthStartDay = terms.getContractEntryIntoForceFromExactDayOfMonthStartDay();
        this.noInterestOnOverdueDebts = terms.getNoInterestOnOverdueDebts();
        this.termStatus = terms.getStatus();
        this.groupDetailsId = terms.getGroupDetailId();
        this.invoicePaymentTerms = invoicePaymentTerms;
        this.isUsedInService = false;
        this.isUsedInGroup = terms.getGroupDetailId() != null;
        this.isUsedInProduct = false; //TODO PRODUCT add: this.getProductId() == null; this line when product is done
        this.productId = null; //TODO PRODUCT add: terms.getProductId(); this line when product is done
        this.startsOfContractInitialTerms = terms.getStartsOfContractInitialTerms();
        this.startDayOfInitialContractTerm = terms.getStartDayOfInitialContractTerm();
        this.waitForOldContractTermToExpires = terms.getWaitForOldContractTermToExpires();
        this.firstDayOfTheMonthOfInitialContractTerm=terms.getFirstDayOfTheMonthOfInitialContractTerm();
    }
}

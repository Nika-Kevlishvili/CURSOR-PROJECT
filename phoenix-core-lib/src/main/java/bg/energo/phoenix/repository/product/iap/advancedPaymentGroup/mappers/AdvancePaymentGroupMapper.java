package bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.mappers;

import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroup;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupAdvancedPayments;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetails;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup.AdvancedPaymentGroupEditRequest;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.AdvancedPaymentGroupVersionResponse;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.AdvancedPaymentGroupViewResponse;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.AdvancedPaymentSimpleInfoResponse;
import lombok.Builder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Builder
public class AdvancePaymentGroupMapper {
    public AdvancedPaymentGroupViewResponse toAdvancedPaymentGroupViewResponse(AdvancedPaymentGroup advancedPaymentGroup,
                                                                               AdvancedPaymentGroupDetails advancedPaymentGroupDetails,
                                                                               List<AdvancedPaymentGroupVersionResponse> versionList,
                                                                               List<AdvancedPaymentSimpleInfoResponse> advancedPaymentSimpleInfoResponse) {
        AdvancedPaymentGroupViewResponse build = AdvancedPaymentGroupViewResponse.builder()
                .id(advancedPaymentGroup.getId())
                .status(advancedPaymentGroup.getStatus())
                .name(advancedPaymentGroupDetails.getName())
                .startDate(advancedPaymentGroupDetails.getStartDate())
                .advancedPaymentGroupId(advancedPaymentGroupDetails.getAdvancedPaymentGroupId())
                .advancedPayments(advancedPaymentSimpleInfoResponse)
                .versionId(advancedPaymentGroupDetails.getVersionId())
                .versions(versionList)
                .build();
        return build;
    }

    public AdvancedPaymentGroupVersionResponse versionsMap(AdvancedPaymentGroupDetails advancedPaymentGroupDetails) {
        return AdvancedPaymentGroupVersionResponse.builder()
                .id(advancedPaymentGroupDetails.getVersionId())
                .name(advancedPaymentGroupDetails.getName())
                .startDate(advancedPaymentGroupDetails.getStartDate())
                .endDate(advancedPaymentGroupDetails.getEndDate())
                .creationDate(advancedPaymentGroupDetails.getCreateDate())
                .build();
    }


    public AdvancedPaymentGroupAdvancedPayments createAdvPayGroupAdvPay(AdvancedPaymentGroupDetails advancedPaymentGroupDetails, Long id) {
        return AdvancedPaymentGroupAdvancedPayments.builder()
                .advancePaymentGroupDetailId(advancedPaymentGroupDetails.getId())
                .advancePaymentId(id)
                .status(AdvancedPaymentGroupStatus.ACTIVE)
                .build();
    }

    public AdvancedPaymentGroupDetails advancedGroupDetailsFromRequest(AdvancedPaymentGroupEditRequest request, AdvancedPaymentGroup advancedPaymentGroup, long versionId) {
        return AdvancedPaymentGroupDetails.builder()
                .advancedPaymentGroupId(advancedPaymentGroup.getId())
                .versionId(versionId)
                .startDate(request.getStartDate())
                .name(request.getName())
                .build();
    }

    public InterimAdvancePayment copyInterimAdvancePayment(InterimAdvancePayment original, PriceComponent priceComponent) {
        InterimAdvancePayment copy = new InterimAdvancePayment();
        copy.setName(original.getName());
        copy.setValueType(original.getValueType());
        copy.setValue(original.getValue());
        copy.setValueFrom(original.getValueFrom());
        copy.setValueTo(original.getValueTo());
        copy.setCurrency(original.getCurrency());
        copy.setDateOfIssueType(original.getDateOfIssueType());
        copy.setDateOfIssueValue(original.getDateOfIssueValue());
        copy.setDateOfIssueValueFrom(original.getDateOfIssueValueFrom());
        copy.setDateOfIssueValueTo(original.getDateOfIssueValueTo());
        copy.setPriceComponent(priceComponent);
        copy.setPaymentType(original.getPaymentType());
        copy.setPeriodType(original.getPeriodType());
        copy.setMatchTermOfStandardInvoice(original.getMatchTermOfStandardInvoice());
        copy.setNoInterestOnOverdueDebts(original.getNoInterestOnOverdueDebts());
        copy.setYearRound(original.getYearRound());
        copy.setIssuingForTheMonthToCurrent(original.getIssuingForTheMonthToCurrent());
        copy.setDeductionFrom(original.getDeductionFrom());
        copy.setStatus(original.getStatus());
        copy.setGroupDetailId(original.getGroupDetailId());
        return copy;
    }


    public AdvancedPaymentGroupAdvancedPayments createAdvancedPaymentGroupAdvancedPayments(AdvancedPaymentGroupDetails advancedPaymentGroupDetails, Long advancePaymentId) {
        return AdvancedPaymentGroupAdvancedPayments.builder()
                .advancePaymentGroupDetailId(advancedPaymentGroupDetails.getId())
                .advancePaymentId(advancePaymentId)
                .status(AdvancedPaymentGroupStatus.ACTIVE)
                .build();
    }

    public AdvancedPaymentGroupDetails fromDetails(AdvancedPaymentGroupDetails details, Long id) {
        return AdvancedPaymentGroupDetails.builder()
                .name(details.getName())
                .advancedPaymentGroupId(id)
                .startDate(details.getStartDate())
                .versionId(1L)
                .build();
    }

    public InterimAdvancePaymentTerms copyTerms(InterimAdvancePaymentTerms terms) {
        InterimAdvancePaymentTerms response = new InterimAdvancePaymentTerms();
        response.setCalendar(terms.getCalendar());
        response.setName(terms.getName());
        response.setValue(terms.getValue());
        response.setStatus(terms.getStatus());
        response.setCalendarType(terms.getCalendarType());
        response.setDueDateChange(terms.getDueDateChange());
        response.setValueFrom(terms.getValueFrom());
        response.setValueTo(terms.getValueTo());
        response.setExcludeWeekends(terms.getExcludeWeekends());
        response.setExcludeHolidays(terms.getExcludeHolidays());
        response.setDueDateChange(terms.getDueDateChange());
        return response;
    }
}

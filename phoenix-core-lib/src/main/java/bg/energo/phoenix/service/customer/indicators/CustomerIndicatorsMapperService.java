package bg.energo.phoenix.service.customer.indicators;

import bg.energo.phoenix.model.entity.customer.indicators.CustomerIndicators;
import bg.energo.phoenix.model.response.customer.indicators.CustomerIndicatorsResponse;
import bg.energo.phoenix.model.response.customer.indicators.ExpiringContract;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerIndicatorsMapperService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CustomerIndicatorsResponse toResponse(CustomerIndicators entity) {
        if (entity == null) {
            return null;
        }

        return CustomerIndicatorsResponse.builder()
                .customerId(entity.getCustomerId())
                .numberOfActiveContracts(entity.getNumberOfActiveContracts())
                .expiringContractsNext3Months(entity.getExpiringContractsNext3Months())
                .listOfExpiringContractsNext3Months(parseContracts(entity.getListOfExpiringContractsNext3Months()))

                .paymentChannelName(entity.getPaymentChannelName())
                .communicationChannel(entity.getCommunicationChannel())

                .currentOverdueLiabilities(entity.getCurrentOverdueLiabilities())
                .currentReceivables(entity.getCurrentReceivables())
                .totalInvoicedAmount(entity.getTotalInvoicedAmount())
                .averagePaymentDay(entity.getAveragePaymentDay())

                .activeRescheduling(entity.getActiveRescheduling())
                .overdueLiabilities12Month(entity.getOverdueLiabilities12Month())
                .overdueLiabilities24Month(entity.getOverdueLiabilities24Month())
                .overdueLiabilities12MonthWithData(entity.getOverdueLiabilities12MonthWithData())
                .overdueLiabilities24MonthWithData(entity.getOverdueLiabilities24MonthWithData())

                .warnings12Month(entity.getWarnings12Month())
                .warnings24Month(entity.getWarnings24Month())
                .warnings12MonthWithData(entity.getWarnings12MonthWithData())
                .warnings24MonthWithData(entity.getWarnings24MonthWithData())

                .disconnection12Month(entity.getDisconnection12Month())
                .disconnection24Month(entity.getDisconnection24Month())
                .disconnection12MonthWithData(entity.getDisconnection12MonthWithData())
                .disconnection24MonthWithData(entity.getDisconnection24MonthWithData())

                .pavLast12Months(entity.getPavLast12Months())
                .pavSinceContractStart(entity.getPavSinceContractStart())
                .pavAgreed(entity.getPavAgreed())
                .deviationLast12Months(entity.getDeviationLast12Months())
                .deviationSinceContractStart(entity.getDeviationSinceContractStart())

                .avgDailyLast12Months(entity.getAvgDailyLast12Months())
                .avgMonthlyLast12Months(entity.getAvgMonthlyLast12Months())
                .maxMonthlyLast12Months(entity.getMaxMonthlyLast12Months())

                .avgDailySinceContractStart(entity.getAvgDailySinceContractStart())
                .avgMonthlySinceContractStart(entity.getAvgMonthlySinceContractStart())
                .maxMonthlySinceContractStart(entity.getMaxMonthlySinceContractStart())

                .avgInvoicedLast12Months(entity.getAvgInvoicedLast12Months())
                .maxInvoicedLast12Months(entity.getMaxInvoicedLast12Months())

                .avgInvoicedSinceContractStart(entity.getAvgInvoicedSinceContractStart())
                .maxInvoicedSinceContractStart(entity.getMaxInvoicedSinceContractStart())

                .currentLiabilities(entity.getCurrentLiabilities())
                .assessmentDate(entity.getAssessmentDate())
                .currentRiskAssessment(entity.getCurrentRiskAssessment())
                .numberOfLawsuits(entity.getNumberOfLawsuits())
                .build();
    }

    public static List<ExpiringContract> parseContracts(String contractsList) {
        List<ExpiringContract> contracts = new ArrayList<>();

        if (contractsList == null || contractsList.trim().isEmpty()) {
            return contracts;
        }

        String[] contractEntries = contractsList.split(",");
        for (String contractEntry : contractEntries) {
            String[] parts = contractEntry.split("/");

            if (parts.length == 4) {
                ExpiringContract contract = ExpiringContract.builder()
                        .contractNumber(parts[0].trim())
                        .contractTermEndDate(LocalDate.parse(parts[1].trim(), DATE_FORMATTER))
                        .contractId(parts[2].trim())
                        .contractType(parts[3].trim())
                        .build();

                contracts.add(contract);
            }
        }

        return contracts;
    }
}
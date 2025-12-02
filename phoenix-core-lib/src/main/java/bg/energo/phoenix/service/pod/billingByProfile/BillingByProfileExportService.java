package bg.energo.phoenix.service.pod.billingByProfile;

import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingByProfile;
import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingDataByProfile;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingByProfileRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingDataByProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static bg.energo.phoenix.util.epb.EPBExportUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingByProfileExportService {
    private final BillingByProfileRepository billingByProfileRepository;
    private final BillingDataByProfileRepository billingDataByProfileRepository;

    @Transactional
    public byte[] exportFile(
            Long billingByProfileId,
            LocalDateTime periodFrom,
            LocalDateTime periodTo
    ) {
        log.info("Export file for billing by profile with id: {}", billingByProfileId);

        Optional<BillingByProfile> billingByProfileOptional = billingByProfileRepository.findById(
                billingByProfileId
        );
        if (billingByProfileOptional.isEmpty()) {
            throw new IllegalArgumentException("Billing by profile with id:" + billingByProfileId + "not found");
        }
        BillingByProfile billingByProfile = billingByProfileOptional.get();

        return getData(billingByProfile, periodFrom, periodTo, billingByProfile.getPeriodType());

    }

    private byte[] getData(BillingByProfile billingByProfile,
                           LocalDateTime periodFrom, LocalDateTime periodTo, PeriodType periodType) {
        Long billingByProfileId = billingByProfile.getId();
        List<BillingDataByProfile> billingDataByProfileList = billingDataByProfileRepository
                .fetchBillingDataWithinPeriod(
                        billingByProfileId, periodFrom, periodTo
                );

        if (billingDataByProfileList.isEmpty()) {
            throw new IllegalArgumentException("No billing data found for profile ID: " + billingByProfileId);
        }
        DateTimeFormatter dateFormatter;
        String periodHeader;
        switch (billingByProfile.getPeriodType()) {
            case FIFTEEN_MINUTES -> {
                dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                periodHeader = "PERIOD \n" +
                        "DD.MM.YYYY HH:MM";
            }
            case ONE_HOUR -> {
                dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                periodHeader = "PERIOD\n" +
                        "DD.MM.YYYY HH:MM";
            }
            case ONE_DAY -> {
                dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                periodHeader = "PERIOD\n" +
                        "DD.MM.YYYY";
            }
            case ONE_MONTH -> {
                dateFormatter = DateTimeFormatter.ofPattern("MM.yyyy");
                periodHeader = "PERIOD\n" +
                        "MM.YYYY";
            }
            default -> throw new IllegalArgumentException(
                    "There is a problem while generating data file for Billing by profile with id:"
                            + billingByProfileId
            );

        }
        return generateExcelFile(
                billingDataByProfileList,
                billingByProfile,
                dateFormatter,
                periodHeader,
                periodFrom,
                periodTo,
                periodType
        );

    }

    private byte[] generateExcelFile(
            List<BillingDataByProfile> billingDataByProfileList,
            BillingByProfile billingByProfile,
            DateTimeFormatter dateFormatter,
            String periodHeader,
            LocalDateTime periodFromm ,
            LocalDateTime periodTo,
            PeriodType periodType) {
        try (
                SXSSFWorkbook workbook = new SXSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Billing Data");

            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue(periodHeader);
            headerRow.createCell(1).setCellValue("VALUE\n" +
                    "(use D for delete)");

            int rowIndex = 1;
            for (BillingDataByProfile billingData : fillMissingIntervals(billingDataByProfileList, periodFromm, periodTo, periodType, billingByProfile.getTimeZone())) {
                Row dataRow = sheet.createRow(rowIndex++);
                String shiftedSymbol = billingData.getIsShiftedHour() ? "*" : "";
                // Set the "period" cell (mapped from periodFrom)
                dataRow.createCell(0).setCellValue(
                        billingData
//                                .getPeriodTo()
                                .getPeriodFrom()
                                .format(dateFormatter)
                                + shiftedSymbol
                );

                // Set the "value" cell (მაპპედ ფრომ ვალუე)
                String billingValue = billingData.getValue() == null ? "" : billingData.getValue().toPlainString();
                dataRow.createCell(1).setCellValue(billingValue);
            }

            workbook.write(outputStream);
            log.info("Export completed successfully for profile ID: {}", billingByProfile.getProfileId());

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate Excel file for billing data", e);
            throw new RuntimeException("Failed to generate Excel file for billing data", e);
        }

    }

    /**
     * Fills in missing intervals of billing data within a specified time period, ensuring
     * all time intervals are accounted for based on the provided period type and time zone.
     *
     * @param priceParameterDataList the list of billing data, each containing a timestamp
     *                               and associated details
     * @param periodFrom the starting timestamp of the time period to be processed
     * @param periodTo the ending timestamp of the time period to be processed
     * @param periodType the type of time interval (e.g., hourly, daily) used to fill missing data
     * @param timeZone the time zone that affects interval adjustments for shifted hours
     * @return a list of billing data, with missing intervals filled as per the specified period type and time zone
     */
    private List<BillingDataByProfile> fillMissingIntervals(
            List<BillingDataByProfile> priceParameterDataList,
            LocalDateTime periodFrom,
            LocalDateTime periodTo,
            PeriodType periodType,
            TimeZone timeZone
    ) {
        TreeMap<LocalDateTime, BillingDataByProfile> mappedData = new TreeMap<>();
        TreeMap<LocalDateTime, BillingDataByProfile> shifted = new TreeMap<>();

        for (BillingDataByProfile detail : priceParameterDataList) {
            if (detail.getIsShiftedHour()) {
                shifted.putIfAbsent(detail.getPeriodFrom(), detail);
            } else {
                mappedData.putIfAbsent(detail.getPeriodFrom(), detail);
            }
        }

        Duration step = getIntervalDuration(periodType);

        List<BillingDataByProfile> completeDataList = new ArrayList<>();
        for (LocalDateTime current = periodFrom; !current.isAfter(periodTo); current = adjustStep(current, step)) {
            fillBillingData(mappedData, completeDataList, current, false, step);
            if (/*timeZone.*/canBeShiftedHour(current, timeZone)) {
                fillBillingData(shifted, completeDataList, current, true, step);
            }
        }
        return completeDataList;
    }

    public static void fillBillingData(
            TreeMap<LocalDateTime, BillingDataByProfile> mappedData,
            List<BillingDataByProfile> completeDataList,
            LocalDateTime current,
            boolean isShiftedHour,
            Duration step
    ) {
        fillCompleteDataList(mappedData, completeDataList, current, () -> {
            BillingDataByProfile emptyDetail = new BillingDataByProfile();
//            emptyDetail.setPeriodTo(adjustStep(current, step));
            emptyDetail.setPeriodFrom(current);
            emptyDetail.setIsShiftedHour(isShiftedHour);
            return emptyDetail;
        });
    }
}

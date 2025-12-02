package bg.energo.phoenix.service.product.price.priceParameter;

import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameter;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetailInfo;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetails;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailsRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceParameterExportService {
    private final PriceParameterRepository priceParameterRepository;
    private final PriceParameterDetailsRepository priceParameterDetailsRepository;
    private final PriceParameterDetailInfoRepository priceParameterDetailInfoRepository;

    @Transactional
    public byte[] exportFile(
            Long priceParameterId,
            Long priceParameterDetailsVersionId,
            LocalDateTime periodFrom,
            LocalDateTime periodTo
    ) {
        log.info(
                "Export file for price parameter with id: {} detail: {}",
                priceParameterId,
                priceParameterDetailsVersionId
        );

        Optional<PriceParameter> priceParameterOptional = priceParameterRepository.findById(priceParameterId);
        if (priceParameterOptional.isEmpty()) {
            throw new IllegalArgumentException("Price parameter with id:" + priceParameterId + "not found");
        }
        PriceParameter priceParameter = priceParameterOptional.get();

        Optional<PriceParameterDetails> priceParameterDetailsOptional = priceParameterDetailsRepository
                .findByPriceParameterIdAndVersionId(
                        priceParameterId,
                        priceParameterDetailsVersionId
                );

        if (priceParameterDetailsOptional.isEmpty()) {
            throw new IllegalArgumentException(
                    "Price parameter with id:" + priceParameterId + "and version:"
                            + priceParameterDetailsVersionId + "not found"
            );
        }
        PriceParameterDetails priceParameterDetails = priceParameterDetailsOptional.get();

        return getData(priceParameter, priceParameterDetails, periodFrom, periodTo, priceParameter.getPeriodType());
    }

    private byte[] getData(
            PriceParameter priceParameter,
            PriceParameterDetails priceParameterDetails,
            LocalDateTime periodFrom, LocalDateTime periodTo, PeriodType periodType) {
        Long priceParameterDetailsId = priceParameterDetails.getId();
        List<PriceParameterDetailInfo> priceParameterDataList = priceParameterDetailInfoRepository
                .findPriceParameterDetailInfoWithinPeriod(
                        priceParameterDetailsId, periodFrom, periodTo
                );

        if (priceParameterDataList.isEmpty()) {
            throw new IllegalArgumentException("No Price parameter data found for price parameter ID: %s, and price parameter version: %s."
                    .formatted(priceParameter.getId(), priceParameterDetails.getVersionId()));
        }
        DateTimeFormatter dateFormatter;
        String periodHeader;
        switch (priceParameter.getPeriodType()) {
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
                    "There is a problem while generating data file for Price parameter with id:"
                            + priceParameter.getId()
                            + "and detail:"
                            + priceParameterDetailsId
            );

        }
        return generateExcelFile(
                priceParameterDataList,
                priceParameter,
                priceParameterDetailsId,
                dateFormatter,
                periodHeader,
                periodFrom,
                periodTo,
                periodType
        );

    }

    private byte[] generateExcelFile(
            List<PriceParameterDetailInfo> priceParameterDataList,
            PriceParameter priceParameter,
            Long priceParameterDetailsId,
            DateTimeFormatter dateFormatter,
            String periodHeader,
            LocalDateTime periodFrom,
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
            for (PriceParameterDetailInfo priceParameterDetailInfo : fillMissingIntervals(priceParameterDataList, periodFrom, periodTo, periodType, priceParameter.getTimeZone())) {
                Row dataRow = sheet.createRow(rowIndex++);
                String shiftedSymbol = priceParameterDetailInfo.getIsShiftedHour() ? "*" : "";
                // Set the "period" cell (mapped from periodFrom)
                dataRow.createCell(0).setCellValue(
                        priceParameterDetailInfo
//                                .getPeriodTo()
                                .getPeriodFrom()
                                .format(dateFormatter)
                                + shiftedSymbol
                );

                // Set the "value" cell
                String priceValue = priceParameterDetailInfo.getPrice() == null ? "" : priceParameterDetailInfo.getPrice().toPlainString();
                dataRow.createCell(1).setCellValue(priceValue);
            }

            workbook.write(outputStream);
            log.info(
                    "Export completed successfully for Price parameter with id:{} and detail:{} ",
                    priceParameter.getId(),
                    priceParameterDetailsId
            );

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate Excel file for price parameter", e);
            throw new RuntimeException("Failed to generate Excel file for price parameter", e);
        }

    }

    /**
     * Fills in the missing intervals within the specified range using the provided price parameter data.
     * The method ensures that data is populated for all intervals defined by the period type between
     * the given start and end dates. It handles shifted hour adjustments based on the provided time zone.
     *
     * @param priceParameterDataList a list of {@link PriceParameterDetailInfo} containing existing price parameter data
     * @param periodFrom the start date and time of the interval range to be filled
     * @param periodTo the end date and time of the interval range to be filled
     * @param periodType the type of period used to define the intervals (e.g., hourly, daily)
     * @param timeZone the time zone information used for adjusting shifted hours, if applicable
     * @return a list of {@link PriceParameterDetailInfo} with filled intervals based on the given period type and range
     */
    private List<PriceParameterDetailInfo> fillMissingIntervals(
            List<PriceParameterDetailInfo> priceParameterDataList,
            LocalDateTime periodFrom,
            LocalDateTime periodTo,
            PeriodType periodType,
            TimeZone timeZone
    ) {

        TreeMap<LocalDateTime, PriceParameterDetailInfo> mappedData = new TreeMap<>();
        TreeMap<LocalDateTime, PriceParameterDetailInfo> shifted = new TreeMap<>();

        for (PriceParameterDetailInfo detail : priceParameterDataList) {
            if (detail.getIsShiftedHour()) {
                shifted.putIfAbsent(detail.getPeriodFrom(), detail);
            } else {
                mappedData.putIfAbsent(detail.getPeriodFrom(), detail);
            }
        }

        Duration step = getIntervalDuration(periodType);

        List<PriceParameterDetailInfo> completeDataList = new ArrayList<>();
        for (LocalDateTime current = periodFrom; !current.isAfter(periodTo); current = adjustStep(current, step)) {
            fillPriceParameterData(mappedData, completeDataList, current, false, step);
            if (/*timeZone.*/canBeShiftedHour(current, timeZone)){
                fillPriceParameterData(shifted, completeDataList, current, true, step);
            }
        }
        return completeDataList;
    }

    public static void fillPriceParameterData(
            TreeMap<LocalDateTime, PriceParameterDetailInfo> mappedData,
            List<PriceParameterDetailInfo> completeDataList,
            LocalDateTime current,
            boolean isShiftedHour,
            Duration step
    ) {
        fillCompleteDataList(mappedData, completeDataList, current, () -> {
            PriceParameterDetailInfo emptyDetail = new PriceParameterDetailInfo();
//            emptyDetail.setPeriodTo(adjustStep(current, step));
            emptyDetail.setPeriodFrom(current);
            emptyDetail.setIsShiftedHour(isShiftedHour);
            return emptyDetail;
        });
    }
}

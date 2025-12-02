package bg.energo.phoenix.service.pod.billingByProfile;

import bg.energo.common.utils.JsonUtils;
import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.entity.nomenclature.pod.Profiles;
import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingByProfile;
import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingDataByProfile;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileTableColumn;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.product.service.list.ServiceSearchField;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import bg.energo.phoenix.model.request.pod.billingByProfile.BillingByProfileCreateRequest;
import bg.energo.phoenix.model.request.pod.billingByProfile.BillingByProfileListRequest;
import bg.energo.phoenix.model.request.pod.billingByProfile.BillingByProfilePreviewRequest;
import bg.energo.phoenix.model.request.pod.billingByProfile.BillingByProfileUpdateRequest;
import bg.energo.phoenix.model.request.pod.billingByProfile.data.BillingByProfileDataCreateRequest;
import bg.energo.phoenix.model.request.pod.billingByProfile.data.BillingByProfileDataJson;
import bg.energo.phoenix.model.request.pod.billingByProfile.data.BillingByProfileDataUpdateRequest;
import bg.energo.phoenix.model.response.pod.billingByProfile.BillingByProfileDataResponse;
import bg.energo.phoenix.model.response.pod.billingByProfile.BillingByProfileListResponse;
import bg.energo.phoenix.model.response.pod.billingByProfile.BillingByProfilePreviewResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.massImport.ExcelHelper;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.nomenclature.pod.ProfilesRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingByProfileRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingDataByProfileRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.mi.ExcelMapperForMIFields;
import io.micrometer.core.instrument.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static bg.energo.phoenix.exception.ErrorCode.*;
import static bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus.DELETED;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingByProfileService {
    private final BillingByProfileRepository billingByProfileRepository;
    private final BillingDataByProfileRepository billingDataByProfileRepository;
    private final BillingByProfileMapper billingByProfileMapper;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final ProfilesRepository profilesRepository;

    private final PermissionService permissionService;
    private final ExcelMapperForMIFields excelMapper;
    private final TemplateRepository templateRepository;
    private final FileService fileService;

    /**
     * Creates billing by profile and its data entries.
     *
     * @param request {@link BillingByProfileCreateRequest} object containing the request data
     * @return the id of the created billing by profile
     */
    @Transactional
    public Long create(BillingByProfileCreateRequest request) {
        log.debug("Creating billing by profile with identifier: {}", request.getIdentifier());

        PointOfDelivery pod = pointOfDeliveryRepository
                .findByIdentifierAndStatus(request.getIdentifier(), PodStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Active point of delivery with identifier: %s does not exist in the system;".formatted(request.getIdentifier())));

        Profiles profiles = profilesRepository
                .findByIdAndStatus(request.getProfileId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("profileId-Active profile with id: %s does not exist in the system;".formatted(request.getProfileId())));

        List<BillingByProfile> billingByProfiles = billingByProfileRepository.findByPodIdAndProfileIdAndPeriodFromAndPeriodTo(
                pod.getId(),
                profiles.getId(),
                request.getPeriodFrom(),
                request.getPeriodTo()
        );

        boolean anyBillingByProfileIsDifferentDimensionInSystem = billingByProfiles
                .stream()
                .anyMatch(bbp -> !Objects.equals(bbp.getPeriodType(), request.getPeriodType()));

        if (anyBillingByProfileIsDifferentDimensionInSystem) {
            log.debug("Billing data by profile with same pod, profile and periods found in system with different dimension");
            throw new ClientException("Billing data by profile with same pod, profile and periods found in system with different dimension", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (!request.isWarningAcceptedByUser()) {
            checkWarnings(request, pod);
        }

        BillingByProfile billingByProfile = billingByProfileMapper.fromRequestToBillingByProfileEntity(request, profiles, pod);
        billingByProfileRepository.saveAndFlush(billingByProfile);

        List<BillingDataByProfile> tempList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(request.getEntries())) {
            for (int i = 0; i < request.getEntries().size(); i++) {
                BillingByProfileDataCreateRequest entry = request.getEntries().get(i);
                validateBillingDataByProfile(request, entry, billingByProfile.getTimeZone(), i);

                LocalDateTime periodFrom = calculatePeriodFrom(entry.getPeriodFrom(), request.getPeriodType());
                LocalDateTime periodTo = calculatePeriodTo(periodFrom, request.getPeriodType());
                BillingDataByProfile data = billingByProfileMapper.fromRequestToBillingDataByProfileEntity(
                        periodFrom,
                        periodTo,
                        entry.getValue(),
                        billingByProfile.getId(),
                        entry.getShiftedHour()
                );
                tempList.add(data);
            }
            billingDataByProfileRepository.saveAll(tempList);
        }

        return billingByProfile.getId();
    }

    private void checkWarnings(BillingByProfileCreateRequest request, PointOfDelivery pod) {
        StringBuilder warningMessage = new StringBuilder();
        if (billingByProfileRepository.isBillingOverlappingForProfileAndPodInPeriod(pod.getId(), request.getProfileId(), request.getPeriodFrom(), request.getPeriodTo())) {
            log.error("The billing data with same POD (ID: {}) and time period ({}:{}) already exists;",
                    pod.getId(), request.getPeriodFrom().toLocalDate().toString(), request.getPeriodTo().toLocalDate().toString());
            warningMessage.append("The billing data with same POD and overlapping time period already exists.");
        }

        if (pointOfDeliveryDetailsRepository.hasPodMeasurementTypeSLP(pod.getId())) {
            log.error("The billing data type does not match POD’s measurement type;");
            warningMessage.append("The billing data type does not match POD’s measurement type.");
        }

        if (!warningMessage.isEmpty()) {
            warningMessage.insert(0, "%s-".formatted(EPBFinalFields.WARNING_MESSAGE_INDICATOR));
            throw new ClientException(warningMessage.toString(), CONFLICT);
        }
    }


    /**
     * Updates billing by profile and its data entries.
     *
     * @param request     {@link BillingByProfileCreateRequest} object containing the request data
     * @param dataRequest {@link BillingByProfileDataCreateRequest} object containing the request entries data
     * @param index       index of the data entry
     */
    private void validateBillingDataByProfile(BillingByProfileCreateRequest request, BillingByProfileDataCreateRequest dataRequest, TimeZone timeZone, int index) {
        validatePeriodFromFormat(dataRequest.getPeriodFrom(), request.getPeriodType(), index);

        if (timeZone != null) {
            if (dataRequest.getShiftedHour()) {
                validateShiftedHourFormat(dataRequest.getPeriodFrom(), timeZone, index);
            }
        }
    }

    /**
     * Validates the format of the starting time for the given {@code periodFrom} and {@code periodType}.
     *
     * @param periodFrom the starting time to validate
     * @param periodType the type of the period, either {@link PeriodType#FIFTEEN_MINUTES} or {@link PeriodType#ONE_HOUR}
     * @throws ClientException if the period from format is invalid
     */
    private void validatePeriodFromFormat(LocalDateTime periodFrom, PeriodType periodType, int index) {
        int minute = periodFrom.getMinute();

        if (periodType.equals(PeriodType.FIFTEEN_MINUTES)) {
            if (!(minute == 15 || minute == 30 || minute == 45 || minute == 0)) {
                log.error("Invalid starting time for fifteen minutes time period.");
                throw new ClientException("entries[%s].periodFrom-Invalid starting time for fifteen minutes time period;".formatted(index), ILLEGAL_ARGUMENTS_PROVIDED);
            }
        } else if (periodType.equals(PeriodType.ONE_HOUR)) {
            if (minute != 0) {
                log.error("Invalid starting time for one hour time period.");
                throw new ClientException("entries[%s].periodFrom-Invalid starting time for one hour time period;".formatted(index), ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (periodFrom.getYear() < 1990) {
            log.error("Selected periodFrom should not be before the year 1990;");
            throw new ClientException("entries[%s].periodFrom-Selected periodFrom should not be before the year 1990;".formatted(index), ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    /**
     * Validates the format of the shifted hour for the gtrueiven {@code periodFrom} and {@code timeZone}.
     *
     * @param periodFrom the starting time to validate
     * @param timeZone   the time zone to validate against, either {@link TimeZone#CET} or another time zone
     * @throws ClientException if the shifted hour format is invalid
     */
    private void validateShiftedHourFormat(LocalDateTime periodFrom, TimeZone timeZone, int index) {
        int hour = periodFrom.getHour();
        int minute = periodFrom.getMinute();

        if (timeZone.equals(TimeZone.CET)) {
            if (!(hour == 3 && (minute == 0 || minute == 15 || minute == 30 || minute == 45))) {
                log.error("Time can't be shifted;");
                throw new ClientException("entries[%s].shiftedHour-Time can't be shifted when [periodFrom] field is: %s and [timezone] is: %s;"
                        .formatted(index, periodFrom.toString(), timeZone.name()), ILLEGAL_ARGUMENTS_PROVIDED);
            }
        } else {
            if (!(hour == 4 && (minute == 0 || minute == 15 || minute == 30 || minute == 45))) {
                throw new ClientException("entries[%s].shiftedHour-Time can't be shifted when [periodFrom] field is: %s and [timezone] is: %s;"
                        .formatted(index, periodFrom.toString(), timeZone.name()), ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
    }


    /**
     * Formats the start date of a period based on the start date and period type.
     *
     * @param periodFrom the start date of the period
     * @param periodType the type of the period, could be one of: {@link PeriodType#FIFTEEN_MINUTES}, {@link PeriodType#ONE_HOUR}, {@link PeriodType#ONE_DAY}, {@link PeriodType#ONE_MONTH}
     * @return the start date of the period
     */
    private LocalDateTime calculatePeriodFrom(LocalDateTime periodFrom, PeriodType periodType) {
        switch (periodType) {
            case FIFTEEN_MINUTES -> periodFrom = periodFrom.withSecond(0).truncatedTo(ChronoUnit.SECONDS);
            case ONE_HOUR -> periodFrom = periodFrom.withMinute(0).withSecond(0).truncatedTo(ChronoUnit.SECONDS);
            case ONE_DAY ->
                    periodFrom = periodFrom.withHour(0).withMinute(0).withSecond(0).truncatedTo(ChronoUnit.SECONDS);
            case ONE_MONTH ->
                    periodFrom = periodFrom.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).truncatedTo(ChronoUnit.SECONDS);
        }
        return periodFrom;
    }


    /**
     * Calculates the end date of a period based on the start date and period type.
     *
     * @param periodFrom the start date of the period
     * @param periodType the type of the period, could be one of: {@link PeriodType#FIFTEEN_MINUTES}, {@link PeriodType#ONE_HOUR}, {@link PeriodType#ONE_DAY}, {@link PeriodType#ONE_MONTH}
     * @return the end date of the period
     * @throws ClientException if the provided periodType is unexpected or the resulting periodTo is after December 31st, 2090
     */
    private LocalDateTime calculatePeriodTo(LocalDateTime periodFrom, PeriodType periodType) {
        LocalDateTime periodTo;
        switch (periodType) {
            case FIFTEEN_MINUTES -> periodTo = periodFrom.plusMinutes(15L);
            case ONE_HOUR -> periodTo = periodFrom.plusHours(1L);
            case ONE_DAY -> periodTo = periodFrom.plusDays(1L);
            case ONE_MONTH -> periodTo = periodFrom.plusMonths(1L);
            default -> {
                log.error("periodType-Unexpected value for periodType: " + periodType);
                throw new ClientException("periodType-Unexpected value for periodType: " + periodType, ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (periodTo.isAfter(LocalDateTime.of(2090, 12, 31, 0, 0))) {
            log.error("Provided period range should end before 31.12.2090;");
            throw new ClientException("periodTo-Provided period should end before 31.12.2090;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        return periodTo;
    }


    /**
     * Updates billing by profile data. Only entries can be updated.
     *
     * @param id      ID of the {@link BillingByProfile} to be updated
     * @param request {@link BillingByProfileUpdateRequest} containing the data
     * @return ID of the updated {@link BillingByProfile}
     */
    @Transactional
    public Long update(Long id, BillingByProfileUpdateRequest request) {
        log.debug("Updating billing by profile with id: {} and request: {}", id, request);

        BillingByProfile billingByProfile = billingByProfileRepository
                .findByIdAndStatus(id, ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active billing by profile with id: %s not found;".formatted(id)));

        if (Boolean.TRUE.equals(billingByProfile.getInvoiced())) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.BILLING_BY_SCALES, List.of(PermissionEnum.BILLING_BY_SCALES_EDIT_LOCKED))) {
                throw new ClientException("id -[id] Can't edit because it is connected to invoice", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

        if (CollectionUtils.isEmpty(request.getEntries())) {
            return id;
        }

        // only modified (added/created/deleted) entries will be provided here (for a 1-day time frame)
        List<BillingByProfileDataUpdateRequest> entries = request.getEntries();
        if (CollectionUtils.isNotEmpty(entries)) {

            // sort entries by periodFrom ascending
            entries.sort(Comparator.comparing(BillingByProfileDataUpdateRequest::getPeriodFrom));

            LocalDateTime endOfDayForPeriodTo = LocalDateTime.of(billingByProfile.getPeriodTo().toLocalDate(), LocalTime.MAX);

            // validate if entries-PeriodFrom is between billingByProfile.periodFrom and billingByProfile.periodTo
            if (entries.get(0).getPeriodFrom().isBefore(billingByProfile.getPeriodFrom())
                    || entries.get(entries.size() - 1).getPeriodFrom().isAfter(endOfDayForPeriodTo)) {
                log.error("entries-PeriodFrom must be between billingByProfile.periodFrom and billingByProfile.periodTo;");
                throw new IllegalArgumentsProvidedException("entries-PeriodFrom must be between billingByProfile.periodFrom and billingByProfile.periodTo;");
            }
            List<BillingByProfileDataJson> list = new ArrayList<>();
            List<LocalDateTime> datesForDelete = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                BillingByProfileDataUpdateRequest entry = entries.get(i);
                if (entry.getValue() == null) {
                    datesForDelete.add(entry.getPeriodFrom());
                    continue;
                }
                validatePeriodFromFormat(entry.getPeriodFrom(), billingByProfile.getPeriodType(), i);
                if (entry.getShiftedHour()) {
                    validateShiftedHourFormat(entry.getPeriodFrom(), billingByProfile.getTimeZone(), i);
                }

                LocalDateTime periodFrom = calculatePeriodFrom(entry.getPeriodFrom(), billingByProfile.getPeriodType());
                LocalDateTime periodTo = calculatePeriodTo(periodFrom, billingByProfile.getPeriodType());
                BillingByProfileDataJson dataJson = new BillingByProfileDataJson(periodFrom, periodTo, entry.getValue(), entry.getShiftedHour());
                list.add(dataJson);
            }
            billingDataByProfileRepository.deleteAllNotPassedValues(id, datesForDelete);
            billingDataByProfileRepository.insertOrUpdateBatch(id, permissionService.getLoggedInUserId(), JsonUtils.toJson(list));
        }

        return id;
    }


    /**
     * Deletes a billing by profile if all validations pass.
     *
     * @param id the id of the billing by profile to delete
     * @return the id of the deleted billing by profile
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting billing by profile with id: {}", id);

        BillingByProfile billingByProfile = billingByProfileRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Billing by profile with id: %s not found;".formatted(id)));

        if (Boolean.TRUE.equals(billingByProfile.getInvoiced())) {
            throw new ClientException("id -[id] Can't delete because it is connected to invoice", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (billingByProfile.getStatus().equals(DELETED)) {
            log.error("Billing by profile with id: {} is already deleted;", id);
            throw new OperationNotAllowedException("id-Billing by profile with id: %s is already deleted;".formatted(id));
        }

        // TODO: 6/26/23 add the rest of the validations

        billingByProfile.setStatus(DELETED);
        billingByProfileRepository.save(billingByProfile);

        return id;
    }


    /**
     * Retrieves {@link BillingByProfile} and {@link BillingDataByProfile} information for preview
     *
     * @param id ID of the {@link BillingByProfile} object
     * @return {@link BillingByProfilePreviewResponse}
     */
    public BillingByProfilePreviewResponse preview(Long id, BillingByProfilePreviewRequest request) {
        log.debug("Retrieving detailed info for Billing By Profile with ID: {}", id);

        if (request.getPeriodTo().isBefore(request.getPeriodFrom())) {
            log.error("Period To is before Period From");
            throw new IllegalArgumentsProvidedException("periodFrom-Period From is before Period To;");
        }

        // fetch response with detailed information
        BillingByProfilePreviewResponse response = billingByProfileRepository
                .previewByIdAndStatusIn(id, List.of(ACTIVE, DELETED))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Billing by profile with ID %s not found;".formatted(id)));

        List<BillingByProfileDataResponse> dataResponses;
        if (response.getPeriodType().equals(PeriodType.ONE_MONTH)) {
            long hoursOffset = getTimeOffsetInHours();

            dataResponses = billingDataByProfileRepository
                    .findResponseByBillingByProfileIdAndPeriodFromAndPeriodTo(
                            id,
                            request.getPeriodFrom().with(TemporalAdjusters.firstDayOfMonth()).plusHours(hoursOffset),
                            request.getPeriodTo().with(TemporalAdjusters.firstDayOfNextMonth()).plusHours(hoursOffset)
                    );
        } else {
            dataResponses = billingDataByProfileRepository.findResponseByBillingByProfileIdAndPeriodFromAndPeriodTo(id, request.getPeriodFrom(), request.getPeriodTo());
        }

        response.setEntries(dataResponses);

        return response;
    }


    /**
     * Returns a page of {@link BillingByProfileListResponse} objects based on the provided {@link BillingByProfileListRequest}
     *
     * @param request {@link BillingByProfileListRequest} containing the search criteria
     * @return {@link Page} of {@link BillingByProfileListResponse} objects
     */
    public Page<BillingByProfileListResponse> list(BillingByProfileListRequest request) {
        log.debug("Retrieving billing by profile list with request: {}", request);

        String sortBy = BillingByProfileTableColumn.BBP_DATE_OF_CREATION.getValue();
        if (request.getSortBy() != null && StringUtils.isNotEmpty(request.getSortBy().getValue())) {
            sortBy = request.getSortBy().getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String searchBy = ServiceSearchField.ALL.getValue();
        if (request.getSearchBy() != null
                && StringUtils.isNotEmpty(request.getSearchBy().name())
                && StringUtils.isNotEmpty(request.getPrompt())) {
            searchBy = request.getSearchBy().name();
        }

        return billingByProfileRepository
                .list(
                        searchBy,
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        getBillingByProfileStatuses().stream().map(Enum::name).toList(),
                        CollectionUtils.isEmpty(request.getGridOperatorIds()) ? new ArrayList<>() : request.getGridOperatorIds(),
                        CollectionUtils.isEmpty(request.getProfileIds()) ? new ArrayList<>() : request.getProfileIds(),
                        request.getDateFromStartingWith(),
                        request.getDateFromEndingWith(),
                        request.getDateToStartingWith(),
                        request.getDateToEndingWith(),
                        request.getPeriodType() == null ? null : request.getPeriodType().name(),
                        request.getInvoiced() == null ? null : request.getInvoiced().toString(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(new Sort.Order(sortDirection, sortBy))
                        )
                );
    }


    /**
     * @return a list of {@link BillingByProfileStatus} objects based on the permissions of the user
     */
    private List<BillingByProfileStatus> getBillingByProfileStatuses() {
        List<BillingByProfileStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.BILLING_BY_PROFILE, List.of(PermissionEnum.BILLING_BY_PROFILE_VIEW_BASIC))) {
            statuses.add(ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.BILLING_BY_PROFILE, List.of(PermissionEnum.BILLING_BY_PROFILE_VIEW_DELETED))) {
            statuses.add(DELETED);
        }

        return statuses;
    }


    @Transactional
    public void importFile(MultipartFile file, PeriodType periodType, Long billingByProfileId) throws InterruptedException {
        excelMapper.validateFileFormat(file);
        excelMapper.validateFileContent(file, getTemplate(periodType), periodType);
        mapAndSaveToDB(file, periodType, billingByProfileId);
    }


    private void mapAndSaveToDB(MultipartFile file, PeriodType periodType, Long billingByProfileId) throws InterruptedException {
        BillingByProfile billingByProfile = getBillingByProfile(billingByProfileId, periodType);

        Map<String, BillingByProfileImportHelper> tempMap = new HashMap<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.isXLS(file) ? new HSSFWorkbook(is) : new XSSFWorkbook(is)) {
            Sheet firstSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = firstSheet.iterator();
            if (iterator.hasNext()) iterator.next();

            while (iterator.hasNext()) {
                Row row = iterator.next();
                BillingByProfileImportHelper helper = new BillingByProfileImportHelper();

                String periodFrom = excelMapper.getPeriodFrom(row, 0, periodType, billingByProfile.getTimeZone(), helper);
                BigDecimal price = excelMapper.getPriceValue(1, row);
                LocalDateTime periodFromDate;
                if (periodFrom.contains("*")) {
                    periodFromDate = LocalDateTime.parse(periodFrom.substring(0, helper.getTime().indexOf('*')).trim());
                } else {
                    periodFromDate = LocalDateTime.parse(periodFrom);
                }
                // validate if entry period is in range of billing by profile period
                validateDateRanges(periodFromDate, billingByProfile.getPeriodFrom(), billingByProfile.getPeriodTo());
                helper.setValue(price == null ? BigDecimal.valueOf(Long.MIN_VALUE) : price);
                helper.setTime(row.getCell(0).getStringCellValue());
                helper.setRowNum(row.getRowNum());
                tempMap.put(periodFrom, helper);
            }
        } catch (IOException e) {
            log.error("Error happened while reading import file content", e);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        List<String> mapKeys = new ArrayList<>(tempMap.keySet());
        List<List<String>> partitions = ListUtils.partition(mapKeys, 50);
        List<Callable<Integer>> callableTasks = new ArrayList<>();

        for (List<String> partition : partitions) {
            List<BillingDataByProfile> billingDataByProfileToSave = new ArrayList<>();
            List<BillingDataByProfile> billingDataByProfileToSaveToDelete = new ArrayList<>();
            Callable<Integer> callableTask = () -> {
                for (String currentString : partition) {
                    BillingByProfileImportHelper helper = tempMap.get(currentString);
                    LocalDateTime current;
                    if (currentString.contains("*")) {
                        current = LocalDateTime.parse(currentString.substring(0, helper.getTime().indexOf('*')).trim());
                    } else {
                        current = LocalDateTime.parse(currentString);
                    }
                    LocalDateTime periodTo = excelMapper.getPeriodTo(current, periodType, helper.getRowNum() + 1);
                    BigDecimal price = helper.getValue();
                    long timeOffsetInHours = getTimeOffsetInHours();

                    Optional<BillingDataByProfile> billingDataByProfileOptional =
                            billingDataByProfileRepository
                                    .findByBillingDataIdAndPeriodAndIsShiftedHour(billingByProfile.getId(), current.plusHours(timeOffsetInHours), periodTo.plusHours(timeOffsetInHours), helper.getTime().contains("*"));

                    if (billingDataByProfileOptional.isEmpty()) {
                        if (price != null && !price.equals(BigDecimal.valueOf(Long.MIN_VALUE)) && !price.equals(BigDecimal.valueOf(Long.MAX_VALUE))) {
                            BillingDataByProfile details = createBillingDataByProfile(
                                    current, periodTo, price, billingByProfile.getId(), helper.getTime().contains("*"));
                            billingDataByProfileToSave.add(details);
                        }
                    } else {
                        BillingDataByProfile oldPriceParameterDetailInfo = billingDataByProfileOptional.get();
                        if (price != null) {
                            if (!price.equals(BigDecimal.valueOf(Long.MIN_VALUE)) && !price.equals(BigDecimal.valueOf(Long.MAX_VALUE))) {
                                oldPriceParameterDetailInfo.setValue(price);
                                billingDataByProfileToSave.add(oldPriceParameterDetailInfo);

                            } else if (price.equals(BigDecimal.valueOf(Long.MAX_VALUE))) {
                                billingDataByProfileToSaveToDelete.add(oldPriceParameterDetailInfo);
                            } else if (price.equals(BigDecimal.valueOf(Long.MIN_VALUE))) {
                                billingDataByProfileToSave.add(oldPriceParameterDetailInfo);
                            }
                        }
                    }
                }
                billingDataByProfileRepository.saveAll(billingDataByProfileToSave);
                billingDataByProfileRepository.deleteAll(billingDataByProfileToSaveToDelete);
                return 1;
            };
            callableTasks.add(callableTask);
        }
        threadPool.invokeAll(callableTasks);
    }

    private BillingDataByProfile createBillingDataByProfile(LocalDateTime periodFrom, LocalDateTime periodTo,
                                                            BigDecimal value, Long billingByProfileId, boolean isShiftedHour) {
        BillingDataByProfile info = new BillingDataByProfile();
        info.setPeriodFrom(periodFrom);
        info.setPeriodTo(periodTo);
        info.setValue(value);
        info.setBillingByProfileId(billingByProfileId);
        info.setIsShiftedHour(isShiftedHour);
        return info;
    }

    private void validateDateRanges(LocalDateTime entryPeriodFrom,
                                    LocalDateTime billingByProfilePeriodFrom,
                                    LocalDateTime billingByProfilePeriodTo) {
        LocalDateTime endOfDayForPeriodTo = LocalDateTime.of(billingByProfilePeriodTo.toLocalDate(), LocalTime.MAX);
        if (entryPeriodFrom.isBefore(billingByProfilePeriodFrom) || entryPeriodFrom.isAfter(endOfDayForPeriodTo)) {
            log.error("Error happened while processing mass import template. Entry period is not in range of billing by profile period.");
            throw new ClientException("Error happened while processing mass import template. Entry period is not in range of billing by profile period.", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    private BillingByProfile getBillingByProfile(Long billingByProfileId, PeriodType periodType) {
        BillingByProfile billingByProfile = billingByProfileRepository.findByIdAndStatus(billingByProfileId, ACTIVE).orElseThrow(() -> {
            log.error("BillingByProfile with id: " + billingByProfileId + " was not found");
            return new ClientException("BillingByProfile with id: " + billingByProfileId + " was not found", DOMAIN_ENTITY_NOT_FOUND);
        });

        if (!billingByProfile.getPeriodType().equals(periodType)) {
            log.error("Error happened while processing mass import template. BillingByProfile with id: " + billingByProfileId + " has " + billingByProfile.getPeriodType() + " period type.");
            throw new ClientException("Error happened while processing mass import template.BillingByProfile with id: " + billingByProfileId + " has " + billingByProfile.getPeriodType() + " period type.", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        return billingByProfile;
    }


    public byte[] getTemplate(PeriodType periodType) {
        try {
            var templatePath = templateRepository.findById("PRICE_PARAMETER_" + periodType.toString()).orElseThrow(() -> new DomainEntityNotFoundException("Unable to find template path for mass import process"));
            log.info("template path ->>>> :" + templatePath.getFileUrl());
            return fileService.downloadFile(templatePath.getFileUrl()).getByteArray();
        } catch (Exception exception) {
            log.error("Could not fetch mass import template", exception);
            throw new ClientException("Could not fetch mass import template", APPLICATION_ERROR);
        }
    }

    private long getTimeOffsetInHours() {
        return (long) TimeUtils
                .secondsToUnit(
                        ZoneId.systemDefault()
                                .getRules()
                                .getOffset(Instant.now())
                                .getTotalSeconds(),
                        TimeUnit.HOURS);
    }
}

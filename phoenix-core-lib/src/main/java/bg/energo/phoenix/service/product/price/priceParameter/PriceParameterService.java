package bg.energo.phoenix.service.product.price.priceParameter;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameter;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetailInfo;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetails;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceListColumns;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterFilterField;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterFilterPriceType;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import bg.energo.phoenix.model.request.product.price.priceParameter.*;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterDetailInfoPreviewResponse;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterListingResponse;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterPreviewResponse;
import bg.energo.phoenix.model.response.priceParameter.PriceParameterResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.massImport.ExcelHelper;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailsRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.mi.ExcelMapperForMIFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static bg.energo.phoenix.exception.ErrorCode.*;
import static bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterFilterPriceType.ALL;
import static bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus.DELETED;
import static bg.energo.phoenix.permissions.PermissionEnum.PRICE_PARAMETER_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.PRICE_PARAMETER_VIEW_DELETED;

@Slf4j
@RequiredArgsConstructor
@Service
public class PriceParameterService {
    private final PriceComponentRepository priceComponentRepository;
    private final ExcelMapperForMIFields excelMapper;
    private final TemplateRepository templateRepository;
    private final PriceParameterRepository priceParameterRepository;
    private final PriceParameterDetailsRepository priceParameterDetailsRepository;
    private final PriceParameterDetailInfoRepository priceParameterDetailInfoRepository;
    private final FileService fileService;
    private final PermissionService permissionService;

    @Transactional
    public Long create(CreatePriceParameterRequest request) {
        log.info("Creating price parameter, request: {}", request);

        validatePriceParamName(request.getName()
                                      .trim());

        PriceParameter priceParameter = createPriceParameter(request);
        PriceParameterDetails priceParameterDetails = createPriceParameterDetails(request, priceParameter);

        List<PriceParameterDetailInfo> tempInfos = new ArrayList<>();
        for (CreatePriceParameterDetailRequest detail : request.getPriceParameterDetails()) {
            validatePriceParamDetailInfo(request, detail);
            createPriceParameterDetailInfo(tempInfos, detail, request.getPeriodType(), priceParameterDetails.getId());
        }
        priceParameterDetailInfoRepository.saveAll(tempInfos);

        return priceParameter.getId();
    }

    /**
     * Validates that the given price parameter name is unique.
     *
     * @param name the name of the price parameter to validate
     * @throws ClientException if the name is not unique
     */
    private void validatePriceParamName(String name) {
        List<PriceParameterDetails> priceParamOptional = priceParameterDetailsRepository
                .findAllByNameAndPriceParameterStatus(name, List.of(ACTIVE));
        if (!priceParamOptional.isEmpty()) {
            log.error("Price parameter name must be unique;");
            throw new ClientException("name-Price parameter name must be unique;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    /**
     * Validates the price parameter detail information contained in the given {@link CreatePriceParameterRequest}
     * and {@link CreatePriceParameterDetailRequest}.
     *
     * @param request the request containing the price parameter information
     * @param detail  the request containing the price parameter detail information
     */
    private void validatePriceParamDetailInfo(CreatePriceParameterRequest request, CreatePriceParameterDetailRequest detail) {
        validatePeriodFromFormat(detail.getPeriodFrom(), request.getPeriodType());
        if (detail.getShiftedHour()) {
            validateShiftedHourFormat(detail.getPeriodFrom(), request.getTimeZone());
        }
    }

    /**
     * Validates the format of the starting time for the given {@code periodFrom} and {@code periodType}.
     *
     * @param periodFrom the starting time to validate
     * @param periodType the type of the period, either {@link PeriodType#FIFTEEN_MINUTES} or {@link PeriodType#ONE_HOUR}
     * @throws ClientException if the period from format is invalid
     */
    private void validatePeriodFromFormat(LocalDateTime periodFrom, PeriodType periodType) {
        int minute = periodFrom.getMinute();

        if (periodType.equals(PeriodType.FIFTEEN_MINUTES)) {
            if (!(minute == 15 || minute == 30 || minute == 45 || minute == 0)) {
                log.error("Invalid starting time for fifteen minutes time period.");
                throw new ClientException(
                        "periodFrom-Invalid starting time for fifteen minutes time period;",
                        ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
        } else if (periodType.equals(PeriodType.ONE_HOUR)) {
            if (minute != 0) {
                log.error("Invalid starting time for one hour time period.");
                throw new ClientException("periodFrom-Invalid starting time for one hour time period;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        if (periodFrom.getYear() < 1990) {
            log.error("Selected periodFrom should not be before the year 1990;");
            throw new ClientException(
                    "periodFrom-Selected periodFrom should not be before the year 1990;",
                    ILLEGAL_ARGUMENTS_PROVIDED
            );
        }
    }

    /**
     * Validates the format of the shifted hour for the given {@code periodFrom} and {@code timeZone}.
     *
     * @param periodFrom the starting time to validate
     * @param timeZone   the time zone to validate against, either {@link TimeZone#CET} or another time zone
     * @throws ClientException if the shifted hour format is invalid
     */
    private void validateShiftedHourFormat(LocalDateTime periodFrom, TimeZone timeZone) {
        int hour = periodFrom.getHour();
        int minute = periodFrom.getMinute();

        if (timeZone.equals(TimeZone.CET)) {
            if (!(hour == 3 && (minute == 0 || minute == 15 || minute == 30 || minute == 45))) {
                log.error("Time can't be shifted;");
                throw new ClientException(
                        "Time can't be shifted when [periodFrom] field is: %s and [timezone] is: %s;"
                                .formatted(periodFrom.toString(), timeZone.name()), ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
        } else {
            if (!(hour == 4 && (minute == 0 || minute == 15 || minute == 30 || minute == 45))) {
                throw new ClientException(
                        "Time can't be shifted when [periodFrom] field is: %s and [timezone] is: %s;"
                                .formatted(periodFrom.toString(), timeZone.name()), ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
        }
    }

    /**
     * Creates a new price parameter with the given {@code CreatePriceParameterRequest}.
     *
     * @param request the request containing the information for the price parameter
     * @return the newly created {@link PriceParameter}
     */
    private PriceParameter createPriceParameter(CreatePriceParameterRequest request) {
        PriceParameter priceParameter = new PriceParameter();
        priceParameter.setStatus(ACTIVE);
        priceParameter.setPeriodType(request.getPeriodType());
        priceParameter.setTimeZone(request.getTimeZone());
        return priceParameterRepository.saveAndFlush(priceParameter);
    }

    /**
     * Creates a new price parameter details object for the given {@link CreatePriceParameterRequest} and {@link PriceParameter}.
     *
     * @param request        the request containing the information for the price parameter details
     * @param priceParameter the price parameter to associate the details with
     * @return the newly created price {@link PriceParameterDetails}
     */
    private PriceParameterDetails createPriceParameterDetails(CreatePriceParameterRequest request, PriceParameter priceParameter) {
        PriceParameterDetails priceParameterDetails = new PriceParameterDetails();
        priceParameterDetails.setName(request.getName()
                                             .trim());
        priceParameterDetails.setVersionId(1L);
        priceParameterDetails.setPriceParameterId(priceParameter.getId());
        priceParameterDetailsRepository.save(priceParameterDetails);

        priceParameter.setLastPriceParameterDetailId(priceParameterDetails.getId());
        priceParameterRepository.save(priceParameter);

        return priceParameterDetails;
    }

    /**
     * Creates a {@link PriceParameterDetailInfo} object and adds it to the given temporary list.
     *
     * @param tempInfos               the list of temporary {@link PriceParameterDetailInfo} objects to add the new object to
     * @param request                 the {@link CreatePriceParameterDetailRequest} object containing the details for the new object
     * @param periodType              the {@link PeriodType} enum value indicating the period type of the new object
     * @param priceParameterDetailsId the ID of the {@link PriceParameterDetails} object to associate with the new object
     */
    private void createPriceParameterDetailInfo(
            List<PriceParameterDetailInfo> tempInfos,
            CreatePriceParameterDetailRequest request,
            PeriodType periodType,
            Long priceParameterDetailsId
    ) {
        PriceParameterDetailInfo detailInfo = new PriceParameterDetailInfo();
        detailInfo.setPrice(request.getPrice());
        detailInfo.setPeriodFrom(calculatePeriodFrom(request.getPeriodFrom(), periodType));
        detailInfo.setPeriodTo(calculatePeriodTo(detailInfo.getPeriodFrom(), periodType));
        detailInfo.setIsShiftedHour(request.getShiftedHour());
        detailInfo.setPriceParameterDetailId(priceParameterDetailsId);
        tempInfos.add(detailInfo);
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
            case FIFTEEN_MINUTES -> periodFrom = periodFrom.withSecond(0)
                                                           .truncatedTo(ChronoUnit.SECONDS);
            case ONE_HOUR -> periodFrom = periodFrom.withMinute(0)
                                                    .withSecond(0)
                                                    .truncatedTo(ChronoUnit.SECONDS);
            case ONE_DAY -> periodFrom = periodFrom.withHour(0)
                                                   .withMinute(0)
                                                   .withSecond(0)
                                                   .truncatedTo(ChronoUnit.SECONDS);
            case ONE_MONTH -> periodFrom = periodFrom.withDayOfMonth(1)
                                                     .withHour(0)
                                                     .withMinute(0)
                                                     .withSecond(0)
                                                     .truncatedTo(ChronoUnit.SECONDS);
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
            throw new ClientException("periodFrom-Provided period should end before 31.12.2090;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        return periodTo;
    }

    @Transactional
    public void importFile(MultipartFile file, PeriodType periodType, Long priceParameterId, Long priceParameterDetailsVersionId)
    throws InterruptedException {
        excelMapper.validateFileFormat(file);
        excelMapper.validateFileContent(file, getTemplate(periodType), periodType);
        mapAndSaveToDB(file, periodType, priceParameterId, priceParameterDetailsVersionId);
    }

    private void mapAndSaveToDB(MultipartFile file, PeriodType periodType, Long priceParameterId, Long priceParameterDetailsVersionId)
    throws InterruptedException {
        PriceParameter priceParameter = getPriceParameter(priceParameterId, periodType);
        PriceParameterDetails priceParameterDetails = getPriceParameterDetails(priceParameterId, priceParameterDetailsVersionId);
        Map<String, PriceParameterImportHelper> tempMap = new HashMap<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.isXLS(file) ? new HSSFWorkbook(is) : new XSSFWorkbook(
                is)) {
            Sheet firstSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = firstSheet.iterator();
            if (iterator.hasNext()) iterator.next();
            while (iterator.hasNext()) {
                Row row = iterator.next();

                PriceParameterImportHelper helper = new PriceParameterImportHelper();
                String periodFrom = excelMapper.getPeriodFrom(row, 0, periodType, priceParameter.getTimeZone(), helper);
                LocalDateTime periodFromParsed;
                if (periodFrom.contains("*")) {
                    periodFromParsed = LocalDateTime.parse(
                            periodFrom.substring(
                                              0,
                                              helper.getTime()
                                                    .indexOf('*')
                                      )
                                      .trim()
                    );
                } else {
                    periodFromParsed = LocalDateTime.parse(periodFrom);
                }
                excelMapper.getPeriodTo(periodFromParsed, periodType, row.getRowNum() + 1);
                BigDecimal price = excelMapper.getPriceValue(1, row);

                helper.setPrice(price == null ? BigDecimal.valueOf(Long.MIN_VALUE) : price);
                helper.setTime(row.getCell(0)
                                  .getStringCellValue());
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
            List<PriceParameterDetailInfo> priceParameterDetailInfosToSave = new ArrayList<>();
            List<PriceParameterDetailInfo> priceParameterDetailInfosToDelete = new ArrayList<>();
            Callable<Integer> callableTask = () -> {
                for (String currentString : partition) {
                    PriceParameterImportHelper helper = tempMap.get(currentString);
                    boolean isShiftedHour = helper.getTime()
                                                  .contains("*");
                    LocalDateTime current;
                    if (isShiftedHour) {
                        current = LocalDateTime.parse(currentString.substring(
                                                                           0,
                                                                           helper.getTime()
                                                                                 .indexOf('*')
                                                                   )
                                                                   .trim());
                    } else {
                        current = LocalDateTime.parse(currentString);
                    }

                    LocalDateTime periodTo = excelMapper.getPeriodTo(current, periodType, helper.getRowNum() + 1);
                    BigDecimal price = helper.getPrice();

                    Optional<PriceParameterDetailInfo> priceParameterDetailInfo = priceParameterDetailInfoRepository.
                            findByPriceParameterDetailsIdAndPeriodAndIsShiftedHour(
                                    priceParameterDetails.getId(),
                                    current,
                                    periodTo,
                                    isShiftedHour
                            );

                    if (priceParameterDetailInfo.isEmpty()) {
                        if (price != null && !price.equals(BigDecimal.valueOf(Long.MIN_VALUE)) && !price.equals(BigDecimal.valueOf(Long.MAX_VALUE))) {
                            PriceParameterDetailInfo details = createPriceParameterDetailInfo(
                                    current, periodTo, price, priceParameterDetails.getId(), isShiftedHour);
                            priceParameterDetailInfosToSave.add(details);

                        }
                    } else {
                        PriceParameterDetailInfo oldPriceParameterDetailInfo = priceParameterDetailInfo.get();
                        if (price != null) {
                            if (!price.equals(BigDecimal.valueOf(Long.MIN_VALUE)) && !price.equals(BigDecimal.valueOf(Long.MAX_VALUE))) {
                                oldPriceParameterDetailInfo.setPrice(price);
                                priceParameterDetailInfosToSave.add(oldPriceParameterDetailInfo);
                            } else if (price.equals(BigDecimal.valueOf(Long.MAX_VALUE))) {
                                priceParameterDetailInfosToDelete.add(oldPriceParameterDetailInfo);
                            } else if (price.equals(BigDecimal.valueOf(Long.MIN_VALUE))) {
                                priceParameterDetailInfosToSave.add(oldPriceParameterDetailInfo);
                            }
                        }
                    }
                }
                priceParameterDetailInfoRepository.saveAll(priceParameterDetailInfosToSave);
                priceParameterDetailInfoRepository.deleteAll(priceParameterDetailInfosToDelete);
                return 1;
            };
            callableTasks.add(callableTask);
        }
        threadPool.invokeAll(callableTasks);
    }


    private PriceParameter getPriceParameter(Long priceParameterId, PeriodType periodType) {
        PriceParameter priceParameter = priceParameterRepository.findByIdAndStatus(priceParameterId, ACTIVE)
                                                                .orElseThrow(() -> {
                                                                    log.error("PriceParameter with id: " + priceParameterId + " was not found");
                                                                    return new ClientException(
                                                                            "PriceParameter with id: " + priceParameterId + " was not found",
                                                                            DOMAIN_ENTITY_NOT_FOUND
                                                                    );
                                                                });

        if (!priceParameter.getPeriodType()
                           .equals(periodType)) {
            log.error("Error happened while processing mass import template.PriceParameter with id: " + priceParameterId + " has " + priceParameter.getPeriodType() + " period type.");
            throw new ClientException(
                    "Error happened while processing mass import template.PriceParameter with id: " + priceParameterId + " has " + priceParameter.getPeriodType() + " period type.",
                    ILLEGAL_ARGUMENTS_PROVIDED
            );
        }
        return priceParameter;
    }

    private PriceParameterDetails getPriceParameterDetails(Long priceParameterId, Long priceParameterDetailsVersionId) {
        return priceParameterDetailsRepository.findByPriceParameterIdAndVersionId(
                                                      priceParameterId, priceParameterDetailsVersionId)
                                              .orElseThrow(() -> {
                                                  log.error("PriceParameterDetails with priceParameterId: " + priceParameterId + " and versionId: " + priceParameterDetailsVersionId + " was not found");
                                                  return new ClientException(
                                                          "PriceParameterDetails with priceParameterId: " + priceParameterId + " and versionId: " + priceParameterDetailsVersionId + " was not found",
                                                          DOMAIN_ENTITY_NOT_FOUND
                                                  );
                                              });
    }


    private PriceParameterDetailInfo createPriceParameterDetailInfo(
            LocalDateTime periodFrom, LocalDateTime periodTo,
            BigDecimal price, Long priceParameterDetailsId, boolean isShiftedHour
    ) {
        PriceParameterDetailInfo info = new PriceParameterDetailInfo();
        info.setPeriodFrom(periodFrom);
        info.setPeriodTo(periodTo);
        info.setPrice(price);
        info.setPriceParameterDetailId(priceParameterDetailsId);
        info.setIsShiftedHour(isShiftedHour);
        return info;
    }


    public byte[] getTemplate(PeriodType periodType) {
        try {
            var templatePath = templateRepository.findById("PRICE_PARAMETER_" + periodType.toString())
                                                 .orElseThrow(() -> new DomainEntityNotFoundException(
                                                         "Unable to find template path for mass import process"));
            log.info("template path aris ->>>> :" + templatePath.getFileUrl());
            return fileService.downloadFile(templatePath.getFileUrl())
                              .getByteArray();
        } catch (Exception exception) {
            log.error("Could not fetch mass import template", exception);
            throw new ClientException("Could not fetch mass import template", APPLICATION_ERROR);
        }
    }


    public PriceParameterResponse delete(Long id) {
        log.debug(String.format("Requested delete Price Parameter with id [%s]", id));
        PriceParameter priceParameter = priceParameterRepository.findById(id)
                                                                .orElseThrow(() -> new DomainEntityNotFoundException(String.format(
                                                                        "id-Price Parameter with presented id [%s] does not exists;",
                                                                        id
                                                                )));

        // TODO: 14.03.23 check if this price parameter is connected with penalty
        // priceParameterRepository.checkPriceParameterConnectionsWithPriceComponents(priceParameter);
        List<PriceComponent> priceComponentList =
                priceComponentRepository.findByPriceFormulaContaining("%s%s%s".formatted("$", priceParameter.getId(), "$"));
        if (CollectionUtils.isNotEmpty(priceComponentList)) {
            throw new ClientException("You can’t delete the Price Parameter because it is connected to the Price Component", CONFLICT);
        }
        if (priceParameter.getStatus()
                          .equals(DELETED)) {
            log.error(String.format("Price Parameter with id [%s] already deleted, throwing client exception", id));
            throw new ClientException(
                    String.format("id-Price Parameter with presented id [%s] already deleted;", id),
                    APPLICATION_ERROR
            );
        }

        priceParameter.setStatus(DELETED);
        priceParameterRepository.save(priceParameter);

        return new PriceParameterResponse(priceParameter);
    }


    /**
     * Returns a page of price parameters matching the given filtering parameters.
     *
     * @param request request containing the filtering parameters
     * @return a page of price parameters matching the given filtering parameters
     */
    public Page<PriceParameterListingResponse> list(PriceParameterFilterRequest request) {
        log.debug("Requested list for Price Parameter with request: {}", request);
        Sort.Order order = new Sort.Order(checkColumnDirection(request), checkSortField(request));
        return priceParameterRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchField(request),
                getPriceParameterTypes(request),
                getStatusesByPermission(),
                String.valueOf(request.isExcludeOldVersions()),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))
        );
    }


    /**
     * Returns active price components that should be used in price component creation.
     *
     * @param request request containing the filtering parameters
     * @return a page of price parameters matching the given filtering parameters
     */
    public Page<PriceParameterListingResponse> fetchPriceParametersForPriceComponent(PriceParameterFilterRequest request) {
        log.debug("Requested list for Price Parameter with request: {}", request);
        Sort.Order order = new Sort.Order(checkColumnDirection(request), checkSortField(request));
        return priceParameterRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchField(request),
                getPriceParameterTypes(request),
                List.of(ACTIVE),
                String.valueOf(request.isExcludeOldVersions()), PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))
        );
    }


    /**
     * @return a list of price parameter statuses that should be shown in the list based on user permissions
     */
    private List<PriceParameterStatus> getStatusesByPermission() {
        List<PriceParameterStatus> statuses = new ArrayList<>();

        if (permissionService.permissionContextContainsPermissions(
                PermissionContextEnum.PRICE_PARAMETER,
                List.of(PRICE_PARAMETER_VIEW_DELETED)
        )) {
            statuses.add(DELETED);
        }

        if (permissionService.permissionContextContainsPermissions(
                PermissionContextEnum.PRICE_PARAMETER,
                List.of(PRICE_PARAMETER_VIEW_BASIC)
        )) {
            statuses.add(ACTIVE);
        }

        return statuses;
    }

    private Sort.Direction checkColumnDirection(PriceParameterFilterRequest request) {
        if (request.getColumnDirection() == null) {
            return Sort.Direction.ASC;
        } else {
            return request.getColumnDirection();
        }
    }

    private List<PeriodType> getPriceParameterTypes(PriceParameterFilterRequest request) {
        List<PeriodType> periodTypes = null;
        List<PriceParameterFilterPriceType> priceParameterTypes = request.getPriceParameterTypes();
        if (priceParameterTypes != null) {
            for (PriceParameterFilterPriceType item : priceParameterTypes) {
                periodTypes = new ArrayList<>();
                if (priceParameterTypes.contains(ALL)) {
                    periodTypes = List.of(
                            PeriodType.FIFTEEN_MINUTES,
                            PeriodType.ONE_HOUR,
                            PeriodType.ONE_DAY,
                            PeriodType.ONE_MONTH
                    );
                    break;
                } else {
                    periodTypes.add(PeriodType.valueOf(item.name()));
                }
            }
        }
        return periodTypes;
    }

    private String getSearchField(PriceParameterFilterRequest request) {
        String searchField;
        if (request.getFilterField() != null) {
            searchField = request.getFilterField()
                                 .getValue();
        } else {
            searchField = PriceParameterFilterField.ALL.getValue();
        }
        return searchField;
    }


    private String checkSortField(PriceParameterFilterRequest request) {
        if (request.getPriceListColumns() == null) {
            return PriceListColumns.ID.getValue();
        } else {
            return request.getPriceListColumns()
                          .getValue();
        }
    }

    public PriceParameterPreviewResponse preview(Long id, PriceParameterDetailInfoPreviewRequest request) {
        log.debug(
                "Requested preview for Price Parameter with id:[{}], version:[{}], periodFrom:[{}], periodTo:[{}]",
                id,
                request.getVersion(),
                request.getPeriodFrom(),
                request.getPeriodTo()
        );

        if (request.getPeriodTo()
                   .isBefore(request.getPeriodFrom())) {
            log.error("Period To is before Period From");
            throw new ClientException("periodFrom-Period From is before Period To", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long requiredVersionId = request.getVersion() == null ? priceParameterDetailsRepository
                .findFirstByPriceParameterId(id, Sort.by(Sort.Direction.DESC, "versionId"))
                .orElseThrow(() ->
                                     new ClientException(
                                             "id-Active Price Parameter Detail with this id not found",
                                             DOMAIN_ENTITY_NOT_FOUND
                                     ))
                .getVersionId()
                : request.getVersion();

        PriceParameterPreviewResponse priceParameterPreviewResponse =
                priceParameterRepository.findByIdVersionAndStatusMapToPreviewResponse(id, requiredVersionId, List.of(ACTIVE, DELETED))
                                        .orElseThrow(() ->
                                                             new DomainEntityNotFoundException(
                                                                     "id-Active Price Parameter with presented id or version not found"));
        log.debug("Requested Price Parameter found: [{}]", priceParameterPreviewResponse.toString());

        priceParameterPreviewResponse.setPriceParameterDetailsVersionInfos(
                priceParameterDetailsRepository.getPriceParameterExistingVersions(id));

        log.debug(
                "Searching assigned Price Parameter Detail Infos for Price Parameter with id:[{}], version:[{}], periodFrom:[{}], periodTo:[{}]",
                id,
                request.getVersion(),
                request.getPeriodFrom(),
                request.getPeriodTo()
        );
        List<PriceParameterDetailInfoPreviewResponse> priceParameterDetailInfoPreviewResponses =
                priceParameterDetailInfoRepository
                        .findByPeriodFromAndPeriodTo(id, requiredVersionId, request.getPeriodFrom(), request.getPeriodTo());
        log.debug(
                "Price Parameter Detail Info Query finished, total entities count: [{}]",
                priceParameterDetailInfoPreviewResponses.size()
        );

        priceParameterPreviewResponse.setPriceParameterDetailInfos(priceParameterDetailInfoPreviewResponses);

        return priceParameterPreviewResponse;
    }


    @Transactional
    public void updatePriceParameter(Long id, PriceParameterUpdateRequest request) {
        PriceParameter priceParameter = priceParameterRepository
                .findByIdAndStatus(id, PriceParameterStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Price parameter not found;"));

        PriceParameterDetails priceParameterDetails;
        if (request.getVersionId() == null) {
            priceParameterDetails = priceParameterDetailsRepository
                    .findById(priceParameter.getLastPriceParameterDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Price parameter detail not found;"));
        } else {
            priceParameterDetails = priceParameterDetailsRepository
                    .findByPriceParameterIdAndVersionId(id, request.getVersionId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Price parameter detail not found;"));
        }

        validatePriceParameterNameForUpdate(id, request.getName());
        if (checkIsLocked(priceParameter) && !hasEditLockedPermission()) {
            log.error("You can not edit this Price parameter;");
            throw new ClientException("id-You can not edit this Price parameter;", ACCESS_DENIED);
        }

        if (request.isNewVersion()) {
            Long versionId = priceParameterDetailsRepository.findLatestByParameterId(id);
            PriceParameterDetails newVersion = priceParameterDetailsRepository.save(
                    new PriceParameterDetails(
                            request.getName(),
                            versionId + 1,
                            priceParameter.getId()
                    )
            );
            List<PriceParameterDetailInfo> sourceVersionEntries = priceParameterDetailInfoRepository.findAllByPriceParameterDetailId(
                    priceParameterDetails.getId());
            List<PriceParameterDetailInfo> newVersionEntries = sourceVersionEntries
                    .stream()
                    .map(info -> info.deepClone(info, newVersion.getId()))
                    .toList();
            priceParameterDetailInfoRepository.saveAll(newVersionEntries);
            priceParameterDetails = newVersion;
            priceParameter.setLastPriceParameterDetailId(newVersion.getId());
            priceParameterRepository.save(priceParameter);
        }

        updatePriceDetailsInfo(priceParameterDetails.getId(), request.getPriceParameterDetails(), priceParameter.getPeriodType());
        priceParameterDetails.setName(request.getName());
        priceParameterDetailsRepository.save(priceParameterDetails);
    }

    private void updatePriceDetailsInfo(Long detailsId, List<PriceParameterTimeRequest> timeRequests, PeriodType type) {
        if (CollectionUtils.isEmpty(timeRequests)) {
            return;
        }
        timeRequests.forEach(timeRequest -> {
            PriceParameterDetailInfo info = priceParameterDetailInfoRepository
                    .findByDetailIdAndPeriodAndIsShifted(detailsId, timeRequest.getPeriodFrom(), timeRequest.isShiftedHour())
                    .orElseGet(() -> new PriceParameterDetailInfo(
                            timeRequest.getPeriodFrom(),
                            detailsId,
                            timeRequest.isShiftedHour(),
                            type
                    ));
            BigDecimal price = timeRequest.getPrice();
            if (price == null) {
                priceParameterDetailInfoRepository.delete(info);
            } else {
                info.setPrice(price);
                info.setIsShiftedHour(timeRequest.isShiftedHour());
                priceParameterDetailInfoRepository.save(info);
            }
        });
    }

    private boolean checkIsLocked(PriceParameter priceParameter) {
        return false;
    }

    private boolean hasEditLockedPermission() {
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.PRICE_PARAMETER);
        return context.stream()
                      .anyMatch(x -> x.equals(PermissionEnum.PRICE_PARAMETER_EDIT_LOCKED.getId()));
    }

    private void validatePriceParameterNameForUpdate(Long parameterId, String name) {
        List<PriceParameterDetails> parameterDetails = priceParameterDetailsRepository.findByNameAndNotParameterId(
                name,
                parameterId,
                List.of(ACTIVE)
        );
        if (!parameterDetails.isEmpty()) {
            throw new ClientException("name-Name already exists!", CONFLICT);
        }
    }
}

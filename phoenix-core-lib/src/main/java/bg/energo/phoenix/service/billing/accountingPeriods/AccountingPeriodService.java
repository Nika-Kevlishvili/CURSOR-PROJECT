package bg.energo.phoenix.service.billing.accountingPeriods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountPeriodFileGenerationStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodSearchByEnums;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.request.billing.accountingPeriod.AccountingPeriodListColumns;
import bg.energo.phoenix.model.request.billing.accountingPeriod.AccountingPeriodRequest;
import bg.energo.phoenix.model.request.billing.accountingPeriod.AccountingPeriodsListingRequest;
import bg.energo.phoenix.model.request.billing.accountingPeriod.AvailableAccountingPeriodListRequest;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsListingResponse;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsPreviewResponse;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsChangeHistoryRepository;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.accountingPeriods.files.AccountingPeriodSapReportRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static bg.energo.phoenix.model.enums.billing.billings.BillingStatus.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountingPeriodService {
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final BillingRunRepository billingRunRepository;
    private final AccountingPeriodsChangeHistoryRepository accountingPeriodsChangeHistoryRepository;
    private final AccountingPeriodReportService accountingPeriodReportService;
    private final AccountingPeriodSapReportRepository accountingPeriodSapReportRepository;


    /**
     * Creates a new accounting period in the system.
     *
     * @param accountingPeriods The accounting period entity to be created.
     * @throws OperationNotAllowedException If an accounting period with the same name already exists.
     */
    @Transactional
    public void create(AccountingPeriods accountingPeriods) {
        if (accountingPeriodsRepository.countAccountingPeriodsByName(accountingPeriods.getName()) > 0) {
            log.error("name-Accounting Period with the same name already exists;");
            throw new OperationNotAllowedException("name-Accounting Period with the same name already exists;");
        }
        accountingPeriodsRepository.save(accountingPeriods);
    }

    /**
     * Edits an existing accounting period in the system.
     *
     * @param id      The ID of the accounting period to be edited.
     * @param request The request object containing the updated details for the accounting period.
     * @return The updated accounting period response.
     * @throws DomainEntityNotFoundException If the accounting period with the given ID is not found.
     * @throws OperationNotAllowedException  If the accounting period cannot be edited due to its status or the status of associated billing runs.
     * @throws ClientException               If the accounting period is already in the requested status.
     */
    @Transactional
    public AccountingPeriodsResponse edit(Long id, AccountingPeriodRequest request) {
        log.debug("Editing accounting period: {}, with ID: {}", request.toString(), id);

        AccountingPeriods accountingPeriods = accountingPeriodsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Accounting period with given id %s not found!;", id)));

        if (!accountingPeriods.getStatus().equals(request.getStatus())) {
            Optional<List<BillingRun>> billingRunOptional = billingRunRepository.findBillingRunByAccountingPeriod(id, List.of(
                    IN_PROGRESS_DRAFT,
                    DRAFT,
                    IN_PROGRESS_GENERATION,
                    GENERATED,
                    IN_PROGRESS_ACCOUNTING,
                    PAUSED
            ));
            if (billingRunOptional.isPresent() && CollectionUtils.isNotEmpty(billingRunOptional.get())) {
                log.error("Cannot edit this Accounting period because of its status in billing run");
                throw new OperationNotAllowedException("accountingPeriod-Cannot edit this Accounting period because of its status in billing run;");
            }
        } else {
            throw new ClientException("Accounting period is already %s".formatted(request.getStatus().equals(AccountingPeriodStatus.OPEN) ? "opened" : "closed"), ErrorCode.CONFLICT);
        }

        if (LocalDateTime.now().isBefore(accountingPeriods.getEndDate()) || LocalDateTime.now().isEqual(accountingPeriods.getEndDate())) {
            log.error("Cannot close the accounting period until its end date");
            throw new OperationNotAllowedException("accountingPeriod-Cannot close the accounting period until its end date;");
        }
        AccountingPeriods period;
        if (request.getStatus() == AccountingPeriodStatus.CLOSED) {
            AccountPeriodFileGenerationStatus status = accountingPeriods.getFileGenerationStatus();
            accountingPeriods.setFileGenerationStatus(AccountPeriodFileGenerationStatus.IN_PROGRESS);
            period = accountingPeriodsRepository.saveAndFlush(accountingPeriods);

            CompletableFuture.runAsync(() -> generateReportsAndUpdateStatus(period, status));
        } else {
            accountingPeriods.setStatus(request.getStatus());
            accountingPeriods.setFileGenerationStatus(AccountPeriodFileGenerationStatus.INITIAL);
            period = accountingPeriodsRepository.saveAndFlush(accountingPeriods);
        }
        return new AccountingPeriodsResponse(period);
    }

    /**
     * Generates the VAT Dairy and Excel reports for the specified accounting period, and updates the status of the accounting period based on the success or failure of the report generation.
     *
     * @param periods The accounting period for which the reports should be generated.
     * @param status  The current file generation status of the accounting period.
     */
    private void generateReportsAndUpdateStatus(AccountingPeriods periods, AccountPeriodFileGenerationStatus status) {
        CompletableFuture<Boolean> vatDairyFuture = CompletableFuture.supplyAsync(() -> {
            try {
                accountingPeriodReportService.generateVatDairy(periods.getId(), status);
                return true;
            } catch (Exception e) {
                log.error("Error generating VAT Dairy report for accounting period {}: {}", periods.getId(), e.getMessage());
                return false;
            }
        });

        CompletableFuture<Boolean> excelFuture = CompletableFuture.supplyAsync(() -> {
            try {
                accountingPeriodReportService.generateExcel(periods.getId(), status);
                return true;
            } catch (Exception e) {
                log.error("Error generating Excel report for accounting period {}: {}", periods.getId(), e.getMessage());
                return false;
            }
        });

        CompletableFuture.allOf(vatDairyFuture, excelFuture).thenRun(() -> {
            try {
                boolean vatDairySuccess = vatDairyFuture.get();
                boolean excelSuccess = excelFuture.get();

                if (vatDairySuccess && excelSuccess) {
                    periods.setStatus(AccountingPeriodStatus.CLOSED);
                    periods.setFileGenerationStatus(AccountPeriodFileGenerationStatus.COMPLETED);
                } else {
                    periods.setFileGenerationStatus(AccountPeriodFileGenerationStatus.FAILED);
                }
                accountingPeriodsRepository.save(periods);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error while checking report generation results: {}", e.getMessage());
                periods.setFileGenerationStatus(AccountPeriodFileGenerationStatus.FAILED);
                accountingPeriodsRepository.save(periods);
            }
        });
    }

    /**
     * Lists accounting periods based on the provided request parameters.
     *
     * @param request The request parameters for filtering and sorting the accounting periods.
     * @return A page of {@link AccountingPeriodsListingResponse} objects representing the filtered and sorted accounting periods.
     */
    @Transactional
    public Page<AccountingPeriodsListingResponse> list(AccountingPeriodsListingRequest request) {
        Sort.Order order = new Sort.Order(request.getDirection(), checkSortField(request));
        return accountingPeriodsRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getStatusString(request),
                request.getStartDateFrom(),
                request.getStartDateTo(),
                request.getEndDateFrom(),
                request.getEndDateTo(),
                request.getClosedOnDateFrom(),
                request.getClosedOnDateTo(),
                getSearchByEnum(request),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))).map(AccountingPeriodsListingResponse::new);
    }

    /**
     * Retrieves a preview of an accounting period based on the provided ID.
     *
     * @param id The ID of the accounting period to preview.
     * @return An {@link AccountingPeriodsPreviewResponse} containing the details of the accounting period, including its history, name, start and end dates, status, associated files, and file generation status.
     * @throws DomainEntityNotFoundException if the accounting period with the given ID is not found.
     */
    @Transactional
    public AccountingPeriodsPreviewResponse preview(Long id) {
        AccountingPeriods accountingPeriods = accountingPeriodsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Accounting period with given id %s not found!;", id)));

        return AccountingPeriodsPreviewResponse.builder()
                .id(id)
                .historyResponse(accountingPeriodsChangeHistoryRepository.getHistory(id))
                .name(accountingPeriods.getName())
                .startDate(accountingPeriods.getStartDate())
                .endDate(accountingPeriods.getEndDate())
                .status(accountingPeriods.getStatus())
                .files(accountingPeriodSapReportRepository.findByAccountPeriodId(id))
                .fileGenerationStatus(accountingPeriods.getFileGenerationStatus())
                .build();
    }

    /**
     * Retrieves a page of available accounting periods based on the provided request parameters.
     *
     * @param request The request parameters for filtering and paging the available accounting periods.
     * @return A page of {@link AccountingPeriodsResponse} objects representing the available accounting periods.
     */
    public Page<AccountingPeriodsResponse> available(AvailableAccountingPeriodListRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return accountingPeriodsRepository.getAvailable(EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()), pageable);
    }

    /**
     * Checks if the provided accounting period ID is available for a billing run.
     *
     * @param id The ID of the accounting period to check.
     * @return An {@link Optional} containing the {@link AccountingPeriodsResponse} if the accounting period is available for billing, or an empty {@link Optional} if it is not available.
     */
    public Optional<AccountingPeriodsResponse> checkAvailableIdForBillingRun(Long id) {
        return accountingPeriodsRepository.availableIdForBilling(id);
    }

    /**
     * Retrieves the sort field to be used for listing accounting periods based on the provided request.
     *
     * @param request The request containing the sort field information.
     * @return The sort field to be used for the listing, or the default "NAME" field if no sort field is provided in the request.
     */
    private String checkSortField(AccountingPeriodsListingRequest request) {
        if (request.getSortBy() == null) {
            return AccountingPeriodListColumns.NAME.getValue();
        } else return request.getSortBy().getValue();
    }

    /**
     * Retrieves the search field to be used for listing accounting periods based on the provided request.
     *
     * @param request The request containing the search field information.
     * @return The search field to be used for the listing, or the default "ALL" field if no search field is provided in the request.
     */
    private String getSearchByEnum(AccountingPeriodsListingRequest request) {
        String searchByField;
        if (request.getSearchBy() != null) {
            searchByField = request.getSearchBy().getValue();
        } else searchByField = AccountingPeriodSearchByEnums.ALL.getValue();
        return searchByField;
    }

    /**
     * Retrieves the status string to be used for listing accounting periods based on the provided request.
     *
     * @param request The request containing the status information.
     * @return The status string to be used for the listing, or `null` if no status is provided in the request.
     */
    private String getStatusString(AccountingPeriodsListingRequest request) {
        if (request.getStatus() == null) {
            return null;
        } else return request.getStatus().getValue();
    }

}

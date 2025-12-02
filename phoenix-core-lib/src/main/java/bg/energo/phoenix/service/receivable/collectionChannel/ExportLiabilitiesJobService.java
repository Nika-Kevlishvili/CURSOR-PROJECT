package bg.energo.phoenix.service.receivable.collectionChannel;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Holiday;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.term.terms.HolidayStatus;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.TypeOfFile;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
import bg.energo.phoenix.repository.nomenclature.address.ResidentialAreaRepository;
import bg.energo.phoenix.repository.nomenclature.address.StreetRepository;
import bg.energo.phoenix.repository.nomenclature.address.ZipCodeRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.HolidaysRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.RRuleUtil;
import bg.energo.phoenix.util.term.PaymentTermUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
//@Profile({"dev","test"})
@RequiredArgsConstructor
//@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ExportLiabilitiesJobService {
    private final CollectionChannelRepository collectionChannelRepository;
    private final FileService fileService;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final CalendarRepository calendarRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final HolidaysRepository holidaysRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final ZipCodeRepository zipCodeRepository;
    private final StreetRepository streetRepository;
    private final ResidentialAreaRepository residentialAreaRepository;
    private final BankRepository bankRepository;

    @Transactional
//    @Scheduled(cron = "${collection.channel.job.hourly.cron}")
    public void executeHourly() {
        log.debug("starting hourly job ");
        LocalDate processStartDate = LocalDate.now();
        List<CollectionChannel> collectionChannels = collectionChannelRepository.findByTypeAndStatus(CollectionChannelType.OFFLINE, EntityStatus.ACTIVE).stream()
                .filter(cc-> {
                    if(cc.getDataSendingSchedule()==null) {
                        log.debug("RRULE in database is null");
                        return false;
                    }
                    boolean rruleValid = validateRRULE(cc.getDataSendingSchedule());
                    if(!rruleValid) {
                        log.debug("RRULE is not valid for collection channel: {}", cc.getId());
                    }

                    return rruleValid;
                })
                .toList();
        log.debug("Filtered collection channels {}", collectionChannels);

        for(CollectionChannel collectionChannel: collectionChannels) {
            log.debug("processing collection channel with id {}",collectionChannel.getId());

            List<CustomerLiability> customerLiabilities = filterCustomerLiabilities(collectionChannel, processStartDate);
            processFiles(collectionChannel, customerLiabilities, processStartDate);
        }
    }

    private void processFiles(CollectionChannel collectionChannel, List<CustomerLiability> customerLiabilities, LocalDate processStartDate) {
        if (customerLiabilities.isEmpty()) {
            log.info("No customer liabilities to process for collection channel {}", collectionChannel.getId());
            return;
        }

        String fileContent;
        String fileName;
        if (collectionChannel.getTypeOfFile().equals(TypeOfFile.BANK_PARTNER)) {
            fileContent = generateBankPartnerFile(customerLiabilities, processStartDate);
            fileName = generateFileName(collectionChannel, "bank");
        } else {
            fileContent = generatePaymentPartnerFile(customerLiabilities);
            fileName = generateFileName(collectionChannel, "payment");
        }

        // Upload file if folder is specified
        if (StringUtils.isNotBlank(collectionChannel.getFolderForFileSending())) {
            uploadFile(collectionChannel.getFolderForFileSending(), fileName, fileContent);
        }

        // TODO: implement sending email with file
//        if (StringUtils.isNotBlank(collectionChannel.getEmailForFileSending())) {
//            sendEmail(collectionChannel.getEmailForFileSending(), fileName, fileContent);
//        }

        // Update liabilities end date of waiting payment if number of working days is specified
        if (collectionChannel.getNumberOfWorkingDays() != null) {
            updateLiabilitiesEndDateOfWaitingPayment(collectionChannel.getNumberOfWorkingDays(), collectionChannel.getCalendarId(), customerLiabilities, processStartDate);
        }
    }

    private String generateBankPartnerFile(List<CustomerLiability> liabilities, LocalDate processStartDate) {
        return liabilities.stream()
                .map(liability -> generateBankPartnerRow(liability, processStartDate))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n"));
    }

    private String generatePaymentPartnerFile(List<CustomerLiability> liabilities) {
        return liabilities.stream()
                .map(this::generatePaymentPartnerRow)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n"));
    }

    private Optional<String> generateBankPartnerRow(CustomerLiability liability, LocalDate processStartDate) {
        try {
            Customer customer = customerRepository.findById(liability.getCustomerId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found with given id: %s".formatted(liability.getCustomerId())));
            CustomerDetails customerDetails = customerDetailsRepository.findById(customer.getLastCustomerDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer detail not found with given id: %s".formatted(customer.getLastCustomerDetailId())));

            StringBuilder row = new StringBuilder();
            row.append("P02,");
            row.append(processStartDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))).append(",");
            row.append(String.format("%014d,", liability.getCurrentAmount().multiply(BigDecimal.valueOf(100)).longValue()));

            String energoProBic = "";
            String energoProIban = "";
            String invoiceNumber = "";

            if (liability.getInvoiceId() != null) {
                Invoice invoice = invoiceRepository.findById(liability.getInvoiceId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with id: %s".formatted(liability.getInvoiceId())));
                energoProBic = getBankBic(invoice.getBankId());
                energoProIban = invoice.getIban();
                invoiceNumber = invoice.getInvoiceNumber();
            }

            row.append(energoProBic).append(",");
            row.append(energoProIban).append(",");

            row.append(customerDetails.getBank() != null ? customerDetails.getBank().getBic() : "").append(",");
            row.append(customerDetails.getIban()).append(",");
            row.append(padField(customerDetails.getName(), 35)).append(",");
            row.append(padField(String.format("%s %.2f", customer.getCustomerNumber(), liability.getCurrentAmount()), 35)).append(",");
            row.append(padField(invoiceNumber + (invoiceNumber.isEmpty() ? "" : " ") + "ОБЕКТ0000000000", 35)).append(",");
            row.append("BISERA,");
            row.append("SHA,");
            row.append("L,");
            row.append(",,");
            row.append(customerDetails.getForeignEntityPerson() ? "F," : "L,");
            row.append(",,,,");

            return Optional.of(row.toString());
        }  catch (Exception e) {
            log.info("Skipping row due to: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> generatePaymentPartnerRow(CustomerLiability liability) {
        try {
            Customer customer = customerRepository.findById(liability.getCustomerId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found with given id: %s".formatted(liability.getCustomerId())));
            CustomerDetails customerDetails = customerDetailsRepository.findById(customer.getLastCustomerDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer detail not found with given id: %s".formatted(customer.getLastCustomerDetailId())));

            StringBuilder row = new StringBuilder();
            row.append(padField(customer.getCustomerNumber().toString(), 16));

            String fullName = customerDetails.getName();
            String firstPart = padField(fullName.length() > 29 ? fullName.substring(0, 29) : fullName, 30);
            String secondPart = padField(fullName.length() > 29 ? fullName.substring(29) : "", 30);

            row.append(firstPart);
            row.append(secondPart);
            row.append(firstPart);
            row.append(secondPart);

            row.append(padField("", 30));
            row.append(padField(customer.getIdentifier(), 20));
            row.append(padField("", 12));

            // Handle invoice and POD related fields
            if (liability.getInvoiceId() != null) {
                Invoice invoice = invoiceRepository.findById(liability.getInvoiceId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found with id: %s".formatted(liability.getInvoiceId())));
                List<PointOfDelivery> pods = pointOfDeliveryRepository.findDistinctPodsByInvoiceId(invoice.getId());
                boolean hasMultiplePods = pods.size() > 1;
                PointOfDelivery pod = hasMultiplePods ? null : pods.isEmpty() ? null : pods.get(0);

                if (!hasMultiplePods && pod != null) {
                    PointOfDeliveryDetails podDetails = pointOfDeliveryDetailsRepository.findById(pod.getLastPodDetailId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Pod detail not found with given id: %s".formatted(pod.getLastPodDetailId())));
                    row.append(padField(getPopulatedPlace(podDetails), 30));
                    row.append(padField(getZipCode(podDetails), 10));
                    row.append(padField(getStreetOrResidentialArea(podDetails), 40));
                    row.append(padField(podDetails.getStreetNumber(), 6));
                    row.append(padField((podDetails.getBlock() + " " + podDetails.getEntrance()).trim(), 6));
                    row.append(padField(podDetails.getApartment(), 8));
                } else {
                    row.append(padField("", 30 + 10 + 40 + 6 + 6 + 8)); // Empty POD fields if multiple PODs or no POD
                }

                row.append(padField(liability.getContractBillingGroupId() != null ? liability.getContractBillingGroupId().toString() : "", 30));
                row.append(padField("", 10));
                row.append(padField(invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber().substring(0, Math.min(3, invoice.getInvoiceNumber().length())) : "", 3));
                row.append(padField(invoice.getInvoiceNumber() != null && invoice.getInvoiceNumber().length() > 3 ? invoice.getInvoiceNumber().substring(3) : "", 10));
                row.append(padField(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "", 10));
                row.append(padField(invoice.getPaymentDeadline() != null ? invoice.getPaymentDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "", 10));
            } else {
                // If there's no invoice, fill all invoice and POD related fields with blank spaces
                row.append(padField("", 30 + 10 + 40 + 6 + 6 + 8)); // POD fields
                row.append(padField(liability.getContractBillingGroupId() != null ? liability.getContractBillingGroupId().toString() : "", 30));
                row.append(padField("", 10));
                row.append(padField("", 3 + 10 + 10 + 10)); // Invoice number, date, and payment deadline fields
            }

            row.append(padField(String.format("%.2f", liability.getInitialAmount()), 16));
            row.append(padField(String.format("%.2f", liability.getCurrentAmount()), 16));
            row.append(padField("12345", 16));

            return Optional.of(row.toString());
        } catch (Exception e) {
            log.info("Skipping row due to: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String padField(String value, int length) {
        if (value == null) {
            value = "";
        }
        if (value.length() > length) {
            return value.substring(0, length);
        }
        return String.format("%-" + length + "s", value);
    }

    private String generateFileName(CollectionChannel collectionChannel, String fileType) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%s_%s_%s.txt", collectionChannel.getName(), fileType, now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
    }

    private void uploadFile(String folderPath, String fileName, String content) {
        try {
            File tempFile = File.createTempFile("temp", ".txt");
            FileWriter writer = new FileWriter(tempFile);
            writer.write(content);
            writer.close();

            fileService.uploadFile(tempFile, folderPath, fileName);
        } catch (IOException e) {
            log.error("Error creating temporary file", e);
        }
    }

    private void sendEmail(String emailAddress, String fileName, String fileContent) {
        if (StringUtils.isNotBlank(emailAddress)) {
            log.info("Sending email with file {} to {}", fileName, emailAddress);
        }
    }

    private void updateLiabilitiesEndDateOfWaitingPayment(Integer numberOfWorkingDays, Long calendarId, List<CustomerLiability> liabilities, LocalDate processStartDate) {
        LocalDate endDate = calculateDeadline(numberOfWorkingDays, processStartDate, calendarId);

        for (CustomerLiability liability : liabilities) {
            liability.setEndDateOfWaitingPayment(endDate);
        }

        customerLiabilityRepository.saveAll(liabilities);
    }

    private boolean validateRRULE(String RRULE) {
        RecurrenceRule recurrenceRule = RRuleUtil.validRecurrenceRule(RRULE);
        if (recurrenceRule == null) {
            return false;
        }
        return RRuleUtil.periodMatchesRRule(recurrenceRule, LocalDate.now());
    }

    private List<CustomerLiability> filterCustomerLiabilities(CollectionChannel collectionChannel, LocalDate processStartDate) {
        List<Long> customerLiabilityIdsByCollectionChannel = customerLiabilityRepository.findLiabilitiesByCollectionChannelId(collectionChannel.getId(), processStartDate);
        return customerLiabilityRepository.findAllById(customerLiabilityIdsByCollectionChannel);
    }

    private LocalDate calculateDeadline(Integer days, LocalDate endDate, Long calendarId) {
        Calendar calendar = calendarRepository.findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Calendar not found with id: %s".formatted(calendarId)));

        List<DayOfWeek> weekends = Arrays.stream(
                        Objects.requireNonNullElse(calendar.getWeekends(), "")
                                .split(";")
                )
                .filter(StringUtils::isNotBlank)
                .map(DayOfWeek::valueOf)
                .toList();
        List<Holiday> holidays = holidaysRepository.findAllByCalendarIdAndHolidayStatus(calendarId, List.of(HolidayStatus.ACTIVE));

        return PaymentTermUtils.calculateDeadlineForCalendarAndWorkingDays(days, endDate, weekends, holidays);
    }


    private String getPopulatedPlace(PointOfDeliveryDetails podDetails) {
        if (podDetails.getPopulatedPlaceId() != null) {
            return populatedPlaceRepository.findPopulatedPlaceNameById(podDetails.getPopulatedPlaceId()).orElse("");
        }

        return podDetails.getPopulatedPlaceForeign() != null ? podDetails.getPopulatedPlaceForeign() : "";
    }

    private String getZipCode(PointOfDeliveryDetails podDetails) {
        if (podDetails.getZipCodeId() != null) {
            return zipCodeRepository.findZipCodeById(podDetails.getZipCodeId()).orElse("");
        }

        return podDetails.getZipCodeForeign() != null ? podDetails.getZipCodeForeign() : "";
    }

    private String getStreetOrResidentialArea(PointOfDeliveryDetails podDetails) {
        if (podDetails.getStreetId() != null) {
            return streetRepository.findStreetNameById(podDetails.getStreetId())
                    .orElse("");
        } else if (podDetails.getResidentialAreaId() != null) {
            return residentialAreaRepository.findResidentialAreaNameById(podDetails.getResidentialAreaId())
                    .orElse("");
        } else if (podDetails.getStreetForeign() != null) {
            return podDetails.getStreetForeign();
        } else if (podDetails.getResidentialAreaForeign() != null) {
            return podDetails.getResidentialAreaForeign();
        }

        return "";
    }

    private String getBankBic(Long bankId) {
        return Optional.ofNullable(bankId)
                .flatMap(bankRepository::findById)
                .map(Bank::getBic)
                .orElse("");
    }

}

package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.payment.OutgoingDocumentType;
import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;

/**
 * The `BankPartnerMapper` class is responsible for parsing and mapping bank partner records from a file to a format that can be used by the application.
 *
 * The class provides the following functionality:
 * - Finding record delimiters in the bank partner file
 * - Parsing individual bank partner records
 * - Mapping the parsed record data to a `BankPartnerRecord` object
 * - Mapping the `BankPartnerRecord` to a `CreatePaymentRequest` object
 * - Parsing metadata from the bank partner file
 * - Validating the structure of the bank partner file
 * - Parsing the entire bank partner file and returning a list of parsed records
 *
 * The class uses several repositories to fetch data required for the mapping and validation processes, such as the `CurrencyRepository`, `CustomerRepository`, `AccountingPeriodsRepository`, and `InvoiceRepository`.
 */
@Component
@RequiredArgsConstructor
public class BankPartnerMapper {

    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Finds the record delimiters in the given bank partner file.
     *
     * The method splits the file into lines and searches for lines starting with ":61:".
     * The indices of these lines are added to the `delimiters` list, which also includes
     * the index of the last line (the end of the file).
     *
     * @param bankPartnerFile the bank partner file to search for record delimiters
     * @return a list of indices representing the record delimiters in the file
     */
    public List<Integer> findRecordDelimiters(String bankPartnerFile) {
        List<Integer> delimiters = new ArrayList<>();
        String[] lines = bankPartnerFile.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(":61:")) {
                delimiters.add(i);
            }
        }

        delimiters.add(lines.length); // Add the end of file as the last delimiter
        return delimiters;
    }

    /**
     * Parses a bank partner record and extracts the relevant data fields.
     *
     * The record is expected to be in a specific format, with fields starting with a colon (e.g. ":61:", ":86:").
     * The method will extract the values for the "61" and "86" fields and return them in a map.
     *
     * @param record the bank partner record to be parsed
     * @return a map containing the extracted "61" and "86" field values
     */
    public Map<String, String> parseBankPartnerRecord(String record) {
        Map<String, String> recordData = new HashMap<>();
        String[] lines = record.split("\\r?\\n");
        StringBuilder field61Builder = new StringBuilder();
        StringBuilder field86Builder = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith(":61:")) {
                field61Builder.append(line.substring(4)).append("\n");
            } else if (line.startsWith(":86:") || (!line.startsWith(":") && !field86Builder.isEmpty())) {
                field86Builder.append(line.startsWith(":86:") ? line.substring(4) : line).append("\n");
            }
        }

        if (!field61Builder.isEmpty()) {
            recordData.put("61", field61Builder.toString().trim());
        }
        if (!field86Builder.isEmpty()) {
            recordData.put("86", field86Builder.toString().trim());
        }

        return recordData;
    }

    /**
     * Maps a record data map to a BankPartnerRecord instance.
     *
     * @param recordData The map containing the record data.
     * @param accountIdentification The account identification.
     * @param currency The currency.
     * @param errorMessages The list to store any error messages encountered during mapping.
     * @return The BankPartnerRecord instance.
     */
    public BankPartnerRecord mapToBankPartnerRecord(Map<String, String> recordData, String accountIdentification, String currency, List<String> errorMessages) {
        BankPartnerRecord record = new BankPartnerRecord();

        // Set account identification and currency from metadata
        record.setAccountIdentification(accountIdentification);
        record.setCurrency(currency);

        // Parse :61: - Payment date, Amount, Reference
        String field61 = recordData.get("61");
        if (field61 == null || field61.isEmpty()) {
            errorMessages.add("Error: :61: field is empty or missing.");
            return record;
        }

        if (field61.length() < 6) {
            errorMessages.add("Error: :61: field does not contain payment date.");
            return record;
        }

        try {
            record.setPaymentDate(LocalDate.parse(field61.substring(0, 6), DateTimeFormatter.ofPattern("yyMMdd")));
        } catch (DateTimeParseException e) {
            errorMessages.add("Error: Invalid payment date format in :61: field.");
            return record;
        }

        int cIndex = field61.indexOf('C');
        int nIndex = field61.indexOf('N');

        if (cIndex == -1 || nIndex == -1 || cIndex >= nIndex) {
            errorMessages.add("Error: :61: field does not contain valid amount information.");
            return record;
        }

        try {
            record.setAmount(new BigDecimal(field61.substring(cIndex + 1, nIndex).replace(",", ".")));
        } catch (NumberFormatException e) {
            errorMessages.add("Error: Invalid amount format in :61: field.");
            return record;
        }

        if (nIndex == field61.length() - 1) {
            errorMessages.add("Error: :61: field does not contain reference information.");
            return record;
        }

        record.setReference(field61.substring(nIndex + 1).trim());

        // Parse :86: - Invoice prefix, Invoice number, Customer number
        String field86 = recordData.get("86");
        if (field86 == null || field86.isEmpty()) {
            errorMessages.add("Error: :86: field is empty or missing.");
            return record;
        }

        int energiaIndex = field86.indexOf("ЕНЕРГИЯ");
        if (energiaIndex == -1) {
            errorMessages.add("Error: 'ЕНЕРГИЯ' not found in :86: field.");
            return record;
        }

        try {
            String invoiceInfo = field86.substring(energiaIndex + "ЕНЕРГИЯ".length()).trim();
            if (invoiceInfo.length() < 13) {
                errorMessages.add("Error: Invoice information in :86: field is too short.");
                return record;
            }
            record.setInvoicePrefix(invoiceInfo.substring(0, 3));
            record.setInvoiceNumber(invoiceInfo.substring(4, 14));
        } catch (IndexOutOfBoundsException e) {
            errorMessages.add("Error: Invalid invoice information format in :86: field.");
            return record;
        }

        // Find customer number
        int plusIndex = field86.indexOf('+', energiaIndex);
        if (plusIndex == -1) {
            errorMessages.add("Error: '+' not found after 'ЕНЕРГИЯ' in :86: field.");
            return record;
        }

        int spaceIndex = field86.indexOf(' ', plusIndex);
        if (spaceIndex == -1) {
            errorMessages.add("Error: Space not found after '+' in :86: field.");
            return record;
        }

        String customerInfo = field86.substring(spaceIndex + 1);
        Matcher matcher = java.util.regex.Pattern.compile("\\d{10}").matcher(customerInfo);
        if (matcher.find()) {
            try {
                record.setCustomerNumber(Long.parseLong(matcher.group()));
            } catch (NumberFormatException e) {
                errorMessages.add("Error: Failed to parse customer number in :86: field.");
            }
        } else {
            errorMessages.add("Error: Customer number not found in the expected format in :86: field.");
        }

        return record;
    }

    /**
     * Maps a BankPartnerRecord to a CreatePaymentRequest.
     *
     * @param bankPartnerRecord the BankPartnerRecord to map from
     * @param paymentDate the payment date to use
     * @param errorMessages a list to add any error messages to
     * @param paymentPackageId the ID of the payment package
     * @return the created CreatePaymentRequest
     */
    public CreatePaymentRequest mapToPaymentRequest(BankPartnerRecord bankPartnerRecord, LocalDate paymentDate, List<String> errorMessages, Long paymentPackageId) {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        Optional<CacheObject> CurrencyOptional = currencyRepository.getCacheObjectByNameAndStatusIn(bankPartnerRecord.getCurrency(), List.of(NomenclatureItemStatus.ACTIVE));
        if(CurrencyOptional.isEmpty())
            errorMessages.add("Currency not found;");

        Long customerId = null;

        Optional<CacheObject> customerOptional = customerRepository.findCacheObjectByCustomerNumber(bankPartnerRecord.getCustomerNumber(), CustomerStatus.ACTIVE);
        if(customerOptional.isEmpty()) {
            Optional<CacheObject> hardcodedCustomer = customerRepository.findHardcodedCustomer();
            if(hardcodedCustomer.isEmpty()) {
                Random random = new Random();
                String generated = String.format("%08d", random.nextInt(100000000));
                customerId = customerRepository.insertHardCodedCustomer(Long.parseLong("60" + generated));
                customerRepository.setDetailIdToHardcodedCustomer(customerId);
            } else {
                customerId = hardcodedCustomer.get().getId();
            }
        } else {
            customerId = customerOptional.get().getId();
        }

        LocalDateTime dateTime = paymentDate.atStartOfDay();
        Optional<CacheObject> accountingPeriodsOptional = accountingPeriodsRepository.findAccountingPeriodsByDate(dateTime);
        if(accountingPeriodsOptional.isEmpty())
            errorMessages.add("Account period not found;");

        Optional<CacheObject> invoiceOptional = invoiceRepository.findByInvoiceNumber(bankPartnerRecord.getInvoicePrefix() + "-" + bankPartnerRecord.getInvoiceNumber());
        if(invoiceOptional.isEmpty())
            errorMessages.add("Invoice not found!");

        paymentRequest.setPaymentDate(bankPartnerRecord.getPaymentDate());
        paymentRequest.setPaymentInfo(bankPartnerRecord.getAccountIdentification());
        paymentRequest.setInitialAmount(bankPartnerRecord.getAmount());
        paymentRequest.setCurrencyId(CurrencyOptional.map(CacheObject::getId).orElse(null));
        paymentRequest.setCollectionChannelId(bankPartnerRecord.getCollectionChannelId());
        paymentRequest.setPaymentPackageId(paymentPackageId);
        paymentRequest.setAccountPeriodId(accountingPeriodsOptional.map(CacheObject::getId).orElse(null));
        paymentRequest.setCustomerId(customerId);
        paymentRequest.setInvoiceId(invoiceOptional.map(CacheObject::getId).orElse(null));
        paymentRequest.setOutgoingDocumentType(OutgoingDocumentType.INVOICE);
        return paymentRequest;
    }

    /**
     * Parses the metadata from a bank partner file, including the account identification and currency.
     * If any errors are encountered during parsing, they are added to the provided errorMessages list.
     *
     * @param bankPartnerFile the contents of the bank partner file to parse
     * @param errorMessages a list to store any error messages encountered during parsing
     * @return a map containing the parsed metadata, such as the account identification and currency
     */
    public Map<String, String> parseMetadata(String bankPartnerFile, List<String> errorMessages) {
        Map<String, String> metadata = new HashMap<>();
        String[] lines = bankPartnerFile.split("\\r?\\n");
        boolean found25 = false;
        boolean found60F = false;

        for (String line : lines) {
            if (line.startsWith(":25:")) {
                if (found25) {
                    errorMessages.add("Error: More than one :25: initial found in the file.");
                }
                found25 = true;
                String accountIdentification = line.substring(4).trim();
                if (accountIdentification.isEmpty()) {
                    errorMessages.add("Error: Empty account identification in :25: field.");
                } else {
                    metadata.put("accountIdentification", accountIdentification);
                }
            } else if (line.startsWith(":60F:")) {
                if (found60F) {
                    errorMessages.add("Error: More than one :60F: initial found in the file.");
                }
                found60F = true;
                if (line.length() <= 5) {
                    errorMessages.add("Error: Empty or invalid :60F: field.");
                } else {
                    String currency = line.substring(5).trim();
                    if (currency.contains("BGN")) {
                        metadata.put("currency", "BGN");
                    } else if (currency.contains("EUR")) {
                        metadata.put("currency", "EUR");
                    } else {
                        errorMessages.add("Error: Invalid currency in :60F: field. Expected BGN or EUR.");
                    }
                }
            } else if (line.startsWith(":61:")) {
                break;
            }
        }

        if (!found25) {
            errorMessages.add("Error: No :25: initial found in the file.");
        }
        if (!found60F) {
            errorMessages.add("Error: No :60F: initial found in the file.");
        }

        return metadata;
    }

    /**
     * Validates the structure of a bank partner file, checking for the presence and order of specific file markers.
     *
     * @param bankPartnerFile The contents of the bank partner file as a string.
     * @param errorMessages A list to store any error messages found during validation.
     */
    public void validateBankPartnerFileStructure(String bankPartnerFile, List<String> errorMessages) {
        String[] lines = bankPartnerFile.split("\\r?\\n");
        boolean found25 = false;
        boolean found60F = false;
        int count61 = 0;
        int count86 = 0;
        boolean lastWas61 = false;

        for (String line : lines) {
            if (line.startsWith(":25:")) {
                if (found25) {
                    errorMessages.add("Error: More than one :25: initial found in the file.");
                }
                found25 = true;
            } else if (line.startsWith(":60F:")) {
                if (found60F) {
                    errorMessages.add("Error: More than one :60F: initial found in the file.");
                }
                found60F = true;
            } else if (line.startsWith(":61:")) {
                count61++;
                if (!lastWas61 && count61 != count86 + 1) {
                    errorMessages.add("Error: :61: and :86: initials are not in the correct order.");
                }
                lastWas61 = true;
            } else if (line.startsWith(":86:")) {
                count86++;
                if (!lastWas61) {
                    errorMessages.add("Error: :86: initial found without a preceding :61: initial.");
                }
                lastWas61 = false;
            }
        }

        if (!found25) {
            errorMessages.add("Error: No :25: initial found in the file.");
        }
        if (!found60F) {
            errorMessages.add("Error: No :60F: initial found in the file.");
        }
        if (count61 != count86) {
            errorMessages.add("Error: Number of :61: initials does not match number of :86: initials.");
        }
    }

    /**
     * Parses a bank partner file and returns a list of parsed data as maps.
     *
     * @param txtFile         the bank partner file as a string
     * @param delimiters      the list of line indices that delimit each record in the file
     * @param errorMessages   a list to store any error messages encountered during parsing
     * @return a list of maps, where each map represents a parsed record from the file
     */
    public List<Map<String, String>> parseBankPartnerFile(String txtFile, List<Integer> delimiters, List<String> errorMessages) {
        validateBankPartnerFileStructure(txtFile, errorMessages);
        if (!errorMessages.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, String>> parsedData = new ArrayList<>();
        String[] lines = txtFile.split("\\r?\\n");

        for (int i = 0; i < delimiters.size() - 1; i++) {
            int start = delimiters.get(i);
            int end = delimiters.get(i + 1);
            String record = String.join("\n", Arrays.copyOfRange(lines, start, end));
            Map<String, String> recordData = parseBankPartnerRecord(record);
            parsedData.add(recordData);
        }

        return parsedData;
    }
}

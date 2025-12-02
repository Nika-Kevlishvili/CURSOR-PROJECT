package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForCustomer;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.receivable.payment.OutgoingDocumentType;
import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

/**
 * The PaymentPartnerMapper class is responsible for mapping payment partner data from a file
 * to domain objects and creating payment requests.
 */
@Component
@RequiredArgsConstructor
public class PaymentPartnerMapper {

    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final InvoiceRepository invoiceRepository;
    /**
     * Parses a payment partner file and returns a list of maps containing the parsed data.
     *
     * @param paymentPartnerFile the payment partner file content as a string
     * @return a list of maps containing the parsed data
     */
    public List<Map<String, String>> parsePaymentPartnerFile(String paymentPartnerFile) {
        List<Map<String, String>> parsedData = new ArrayList<>();

        String[] lines = paymentPartnerFile.split("\\r?\\n");
        for (int i = 0;i<lines.length;i++) {
            System.out.println("Length line on row " + i + ", " + lines[i].length());
            if(lines[i].length()!=69) {
                throw new ClientException("Incorrect file structure provided,row length is not valid on row " + i, ILLEGAL_ARGUMENTS_PROVIDED);            }
            Map<String, String> recordData = new HashMap<>();
            String line = lines[i];
            recordData.put("customerNumber", line.substring(0, 16).trim());
            recordData.put("customerIdentifier", line.substring(16, 26).trim());
            recordData.put("billingGroupNumber", line.substring(26, 46).trim());
            recordData.put("invoicePrefix", line.substring(46, 49).trim());
            recordData.put("invoiceNumber", line.substring(49, 59).trim());
            recordData.put("amount", line.substring(59, 69).trim().replace(",", "."));
            parsedData.add(recordData);
        }

        return parsedData;
    }
    /**
     * Maps the record data from a map to a PaymentPartnerRecord object.
     *
     * @param recordData   the record data as a map
     * @param errorMessages a list to store any error messages encountered during mapping
     * @return a PaymentPartnerRecord object
     */
    public PaymentPartnerRecord mapToPaymentPartnerRecord(Map<String, String> recordData,List<String> errorMessages) {
        PaymentPartnerRecord paymentPartnerRecord = new PaymentPartnerRecord();
        try {
            paymentPartnerRecord.setCustomerNumber(Long.parseLong(recordData.get("customerNumber")));
        }catch (Exception e) {
            errorMessages.add("Invalid format of customer number!;");
        }
        paymentPartnerRecord.setCustomerIdentifier(recordData.get("customerIdentifier"));
        paymentPartnerRecord.setBillingGroupNumber(recordData.get("billingGroupNumber"));
        paymentPartnerRecord.setInvoicePrefix(recordData.get("invoicePrefix"));
        paymentPartnerRecord.setInvoiceNumber(recordData.get("invoiceNumber"));
        try {
            paymentPartnerRecord.setAmount(new BigDecimal(recordData.get("amount")));
        }catch (Exception e) {
            errorMessages.add("Invalid format of amount!;");
            return paymentPartnerRecord;
        }
        String amount = recordData.get("amount");
        if(amount.isBlank()) {
            errorMessages.add("Amount is empty!;");
        } else {
            if(amount.contains(".")) {
                if(amount.charAt(amount.length()-1)=='.') {
                    errorMessages.add("Invalid number format,digits are mandatory after dot;");
                }
                if((amount.length()-1)-amount.indexOf(".")>2) {
                    errorMessages.add("invalid number format,only two digits are allowed after dot;");
                }
            }
        }
        return paymentPartnerRecord;
    }
    /**
     * Maps a PaymentPartnerRecord object and payment date to a CreatePaymentRequest object.
     *
     * @param paymentPartnerRecord the PaymentPartnerRecord object
     * @param paymentDate          the payment date
     * @param errorMessages        a list to store any error messages encountered during mapping
     * @param paymentPackageId     the payment package ID
     * @return a CreatePaymentRequest object
     */
    public CreatePaymentRequest mapToPaymentRequest(PaymentPartnerRecord paymentPartnerRecord, LocalDate paymentDate,List<String> errorMessages,Long paymentPackageId,Boolean currencyFromCollectionChannel,CollectionChannel collectionChannel) {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        if(currencyFromCollectionChannel) {
            paymentRequest.setCurrencyId(collectionChannel.getCurrencyId());
        } else {
            Optional<CacheObject> defaultCurrencyOptional = currencyRepository.findByDefaultSelectionTrueCache();
            if(defaultCurrencyOptional.isEmpty()) {
                errorMessages.add("Default currency not found;");
            }
            paymentRequest.setCurrencyId(defaultCurrencyOptional.map(CacheObject::getId).orElse(null));
        }
        Optional<CacheObjectForCustomer> customerOptional = customerRepository.findCacheObjectCustomerNumber(paymentPartnerRecord.getCustomerIdentifier(), CustomerStatus.ACTIVE);
        Long customerId = null;
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
            if(!customerOptional.get().getCustomerNumber().equals(paymentPartnerRecord.getCustomerNumber())) {
                errorMessages.add("Customer number mismatch!;");
            } else {
                customerId = customerOptional.get().getId();
            }
            if(paymentPartnerRecord.getBillingGroupNumber()!=null && !paymentPartnerRecord.getBillingGroupNumber().isBlank()) {
                Optional<CacheObject> contractBillingGroupOptional = contractBillingGroupRepository.findCacheObjectByNameAndCustomerId(customerOptional.get().getId(),paymentPartnerRecord.getBillingGroupNumber(), EntityStatus.ACTIVE);
                contractBillingGroupOptional.ifPresent(cacheObject -> paymentRequest.setContractBillingGroupId(cacheObject.getId()));
            }
        }
        LocalDateTime dateTime = paymentDate.atStartOfDay();
        Optional<CacheObject> accountingPeriodsOptional = accountingPeriodsRepository.findAccountingPeriodsByDate(dateTime);
        if(accountingPeriodsOptional.isEmpty()) {
            errorMessages.add("Account period not found;");
        }


        Optional<CacheObject> invoiceOptional = invoiceRepository.findByInvoiceNumber(paymentPartnerRecord.getInvoicePrefix() + "-" + paymentPartnerRecord.getInvoiceNumber());

        if(invoiceOptional.isEmpty()) {
            errorMessages.add("Invoice not found!");
        }

        paymentRequest.setPaymentDate(paymentDate);
        paymentRequest.setInitialAmount(paymentPartnerRecord.getAmount());
        paymentRequest.setCollectionChannelId(paymentPartnerRecord.getCollectionChannelId());
        paymentRequest.setPaymentPackageId(paymentPackageId);
        paymentRequest.setAccountPeriodId(accountingPeriodsOptional.map(CacheObject::getId).orElse(null));
        paymentRequest.setCustomerId(customerId);
        paymentRequest.setInvoiceId(invoiceOptional.map(CacheObject::getId).orElse(null));
        paymentRequest.setOutgoingDocumentType(OutgoingDocumentType.INVOICE);
        return paymentRequest;
    }
}
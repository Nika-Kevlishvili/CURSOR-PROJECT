package bg.energo.phoenix.service.billing.invoice;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceFileExport;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceResponseExport;
import bg.energo.phoenix.repository.billing.invoice.InvoiceFileExportRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.ByteMultiPartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceFileService {


    private final FileService fileService;
    private final InvoiceFileExportRepository fileExportRepository;

    @Value("${invoice.export.file.path}")
    private String uploadPath;

    public void saveInvoiceCsv(Long billingRunId, Collection<InvoiceResponseExport> invoiceResponses, int maxVatRateSize) {
        StringWriter stringWriter = new StringWriter();

        String[] vatRateNames = new String[maxVatRateSize * 3];
        int itr = 1;
        for (int i = 0; i < vatRateNames.length; i += 3) {
            vatRateNames[i] = "vatRatePercent_%s".formatted(itr);
            vatRateNames[i + 1] = "amountExcludingVat_%s".formatted(itr);
            vatRateNames[i + 2] = "valueOfVat_%s".formatted(itr);
            itr++;
        }

        String[] concatenate = concatenate(vatRateNames, new String[]{"Invoice number",
                        "Creation date",
                        "Status",
                        "Status modify date",
                        "Document type",
                        "Customer identifier",
                        "Customer name",
                        "Tax event date",
                        "Payment Deadline",
                        "Invoice Type",
                        "Contract Order",
                        "Billing group",
                        "Receipt of an invoice number",
                        "Receipt of an invoice name",
                        "Communication for billing",
                        "Product/Service",
                        "Meter reading period from",
                        "Meter reading period to",
                        "Number of income account",
                        "Basis for issuing",
                        "Cost center controlling order",
                        "Applicable interest rate"},
                new String[]{"Currency name",
                        "Total amount including vat",
                        "Total amount including vat in alt currency",
                        "Direct Debit",
                        "Bank",
                        "Bic",
                        "Bank account",
                        "Issuer",
                        "Liability customer receivable",
                        "Accounting Period Name",
                        "Template",
                        "Current Compensation",
                        "Debit/Credit Notes"});
        CSVFormat csvFormat = CSVFormat.DEFAULT
                .builder()
                .setHeader(
                        concatenate
                )
                .build();

        try (CSVPrinter printer = new CSVPrinter(stringWriter, csvFormat)) {
            for (InvoiceResponseExport res : invoiceResponses) {
                Object[] recordList = createRecordList(res);
                printer.printRecord(recordList);
            }
        } catch (IOException e) {
            log.error("Error while writing in csv file;");
        }
        String randomName = UUID.randomUUID().toString();
        ByteMultiPartFile multiPartFile = new ByteMultiPartFile(randomName, stringWriter.toString().getBytes(StandardCharsets.UTF_8));

        String path = fileService.uploadFile(multiPartFile, uploadPath, randomName);

        Optional<InvoiceFileExport> invoiceFileByBillingRun = fileExportRepository.findInvoiceFIleByBillingRun(billingRunId);
        InvoiceFileExport invoiceFileExport;
        String fileName = "%s.csv".formatted(path);
        if (invoiceFileByBillingRun.isEmpty()) {
            invoiceFileExport = new InvoiceFileExport(billingRunId, path, fileName);
        } else {
            invoiceFileExport = invoiceFileByBillingRun.get();
            invoiceFileExport.setExcelPath(path);
            invoiceFileExport.setFileName(fileName);
        }
        fileExportRepository.save(invoiceFileExport);
    }

    private Object[] createRecordList(InvoiceResponseExport res) {
        Object[] beforeVats = {res.getInvoiceNumber(),
                res.getCreationDate(),
                res.getInvoiceStatus(),
                res.getStatusModifyDate(),
                res.getDocumentType(),
                res.getCustomerIdentifier(),
                res.getCustomerName(),
                res.getTaxEventDate(),
                res.getPaymentDeadLine(),
                res.getInvoiceType(),
                res.getContractOrder(),
                res.getBillingGroupName(),
                res.getRecipientOfInvoiceNumber(),
                res.getRecipientOfInvoiceName(),
                res.getCommForBilling(),
                res.getProductServiceName(),
                res.getMeterReadingFrom(),
                res.getMeterReadingTo(),
                res.getNumberOfIncomeAccount(),
                res.getBasisForIssuing(),
                res.getCostCenterControllingOrder(),
                res.getInterestRate()};
        Object[] vats = res.getVatRateResponses()
                .stream()
                .flatMap(x -> Arrays.stream(
                        new BigDecimal[]{
                                x.getVatRatePercent(),
                                x.getAmountExcludingVat(),
                                x.getValueOfVat()
                        }))
                .toArray();
        Object[] afterVats = {res.getCurrencyName(),
                res.getTotalAmountIncludingVat(),
                res.getTotalAmountIncludingVatInOtherCurrency(),
                res.getDirectDebit(),
                res.getBank(),
                res.getBic(),
                res.getBankAccount(),
                res.getIssuer(),
                res.getLiabilityCustomerReceivable(),
                res.getAccountingPeriodName(),
                res.getTemplateName(),
                "\"%s\"".formatted(res.getCompensation()),
                "\"%s\"".formatted(res.getDebitCreditNotes())};
        return concatenateResponse(vats, beforeVats, afterVats);
    }

    public static String[] concatenate(String[] vatRates, String[] beforeVats, String[] afterVats) {
        int vatRatesLength = vatRates.length;
        int beforeVatLength = beforeVats.length;
        int afterVatsLength = afterVats.length;
        String[] result = new String[vatRatesLength + beforeVatLength + afterVatsLength];
        System.arraycopy(beforeVats, 0, result, 0, beforeVatLength);
        System.arraycopy(vatRates, 0, result, beforeVatLength, vatRatesLength);
        System.arraycopy(afterVats, 0, result, vatRatesLength + beforeVatLength, afterVatsLength);
        return result;
    }

    public static Object[] concatenateResponse(Object[] vatRates, Object[] beforeVats, Object[] afterVats) {
        int vatRatesLength = vatRates.length;
        int beforeVatLength = beforeVats.length;
        int afterVatsLength = afterVats.length;
        Object[] result = new Object[vatRatesLength + beforeVatLength + afterVatsLength];
        System.arraycopy(beforeVats, 0, result, 0, beforeVatLength);
        System.arraycopy(vatRates, 0, result, beforeVatLength, vatRatesLength);
        System.arraycopy(afterVats, 0, result, vatRatesLength + beforeVatLength, afterVatsLength);
        return result;
    }

    public Pair<String, ByteArrayResource> fetchInvoice(Long billingRunId) {
        Optional<InvoiceFileExport> invoiceFileExport = fileExportRepository.findInvoiceFIleByBillingRun(billingRunId);
        if (invoiceFileExport.isEmpty()) {
            return null;
        }
        InvoiceFileExport file = invoiceFileExport.get();
        ByteArrayResource byteArrayResource = fileService.downloadFile(file.getExcelPath());
        if (byteArrayResource == null) {
            throw new DomainEntityNotFoundException("Billing run file was not generated!:");
        }
        return Pair.of(file.getFileName(), byteArrayResource);
    }
}

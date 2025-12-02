package bg.energo.phoenix.service.billing.invoice.numberGeneration;

import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.service.billing.invoice.InvoicePrefixService;
import bg.energo.phoenix.service.billing.invoice.enums.InvoiceObjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus.REAL;

@RequiredArgsConstructor
@Service
@Slf4j
/**
 * Service is used for filling invoice numbers use fillInvoiceNumber methods.
 */
public class InvoiceNumberService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePrefixService invoicePrefixService;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private Integer jobInterval = 3;

    /**
     * Fills the invoice number for the given invoice.
     *
     * @param invoice The invoice to fill the number for.
     */
    @Transactional
    public void fillInvoiceNumber(Invoice invoice) {
        saveInvoiceNumber(invoice);
    }

    /**
     * Fills the invoice number for the given invoice.
     *
     * @param invoice The invoice to fill the number for.
     */

    @Transactional
    public void fillInvoiceNumber(Set<Long> invoice) {
        fillInvoiceNumber(invoiceRepository.findAllById(invoice));
    }


    /**
     * Fills the invoice number for the given set of invoices.
     *
     * @param invoices The invoices to fill the number for.
     */
    @Transactional
    public void fillInvoiceNumber(Iterable<Invoice> invoices) {
        for (Invoice invoice : invoices) {
            saveInvoiceNumber(invoice);
        }
    }

    private void saveInvoiceNumber(Invoice invoice) {
        PrefixType prefixType = invoiceRepository.findPrefixTypeForInvoice(invoice.getId()).orElse(null);
        boolean isOrder = !(invoice.getInvoiceStatus().equals(REAL) && !invoice.getInvoiceDocumentType().equals(InvoiceDocumentType.PROFORMA_INVOICE)) && List.of(InvoiceObjectType.GOODS_ORDER, InvoiceObjectType.SERVICE_ORDER).contains(InvoiceObjectType.defineInvoiceObjectType(invoice));
        if (isOrder) {
            processOrderInvoices(invoice, prefixType);
        } else {
            processBillingInvoices(invoice, prefixType);
        }
    }


    private void processOrderInvoices(Invoice invoice, PrefixType prefixType) {

        String oldInvoiceNumberFull = invoice.getInvoiceNumber();
        InvoiceObjectType invoiceObjectType = InvoiceObjectType.defineInvoiceObjectType(invoice);

        if (oldInvoiceNumberFull != null) {
            if ((invoice.getInvoiceStatus().equals(REAL) || invoice.getInvoiceStatus().equals(InvoiceStatus.DRAFT_GENERATED)) && oldInvoiceNumberFull.charAt(oldInvoiceNumberFull.indexOf('-') - 1) != 'D') {
                return;
            } else if (!(invoice.getInvoiceStatus().equals(REAL) || invoice.getInvoiceStatus().equals(InvoiceStatus.DRAFT_GENERATED))) {
                return;
            }
        }
        InvoiceType invoiceType = invoice.getInvoiceType();
        InvoiceDocumentType invoiceDocumentType = invoice.getInvoiceDocumentType();
        String invoicePrefix = invoicePrefixService.getInvoiceNumber(invoiceObjectType, invoiceType, invoiceDocumentType, prefixType);
        String invoiceNumber;
        InvoiceStatus invoiceStatus = invoice.getInvoiceStatus();
        if (invoiceStatus.equals(REAL) || invoiceStatus.equals(InvoiceStatus.DRAFT_GENERATED)) {
            invoiceNumber = invoiceNumberGenerator.generateNumber(NumberType.REAL_PROFORMA);
        } else {
            invoiceNumber = invoiceNumberGenerator.generateNumber(NumberType.DRAFT_PROFORMA);
        }

        String finalInvoiceNumber = "%s%s-%s".formatted(invoicePrefix,
                invoiceStatus.equals(InvoiceStatus.DRAFT) ? "D" : "",
                invoiceNumber);
        invoice.setInvoiceNumber(finalInvoiceNumber);

        invoiceRepository.save(invoice);
    }

    private void processBillingInvoices(Invoice invoice, PrefixType prefixType) {


        String oldInvoiceNumberFull = invoice.getInvoiceNumber();
        InvoiceObjectType invoiceObjectType = InvoiceObjectType.defineInvoiceObjectType(invoice);
        if (oldInvoiceNumberFull != null) {
            if ((invoice.getInvoiceStatus().equals(REAL) || invoice.getInvoiceStatus().equals(InvoiceStatus.DRAFT_GENERATED)) && oldInvoiceNumberFull.charAt(oldInvoiceNumberFull.indexOf('-') - 1) != 'D' && !invoiceObjectType.equals(InvoiceObjectType.GOODS_ORDER) && !invoiceObjectType.equals(InvoiceObjectType.SERVICE_ORDER)) {
                return;
            } else if (!(invoice.getInvoiceStatus().equals(REAL) || invoice.getInvoiceStatus().equals(InvoiceStatus.DRAFT_GENERATED))) {
                return;
            }
        }
        InvoiceType invoiceType = invoice.getInvoiceType();
        InvoiceDocumentType invoiceDocumentType = invoice.getInvoiceDocumentType();
        String invoicePrefix = invoicePrefixService.getInvoiceNumber(invoiceObjectType, invoiceType, invoiceDocumentType, prefixType);
        String invoiceNumber;
        InvoiceStatus invoiceStatus = invoice.getInvoiceStatus();
        if (invoiceStatus.equals(REAL) || invoiceStatus.equals(InvoiceStatus.DRAFT_GENERATED)) {
            invoiceNumber = invoiceNumberGenerator.generateNumber(NumberType.REAL);
        } else {
            invoiceNumber = invoiceNumberGenerator.generateNumber(NumberType.DRAFT_INVOICE);

        }

        String finalInvoiceNumber = "%s%s-%s".formatted(invoicePrefix,
                invoiceStatus.equals(InvoiceStatus.DRAFT) ? "D" : "",
                invoiceNumber);
        invoice.setInvoiceNumber(finalInvoiceNumber);

        invoiceRepository.save(invoice);
    }

    @Transactional
    /**
     * This method finds and saves missed number to be later used in number generation
     * runs as job in certain interval
     */
    public void saveMissedNumbers() {
        List<InvoiceNumberDto> missedInvoiceNumbers = invoiceRepository.getMissedInvoiceNumbers(LocalDateTime.now().minusHours(jobInterval));
        for (InvoiceNumberDto missedInvoiceNumber : missedInvoiceNumbers) {
            invoiceNumberGenerator.save(new InvoiceNumberTable(null, missedInvoiceNumber.getNumber(), missedInvoiceNumber.getNumberType()));
        }
    }
}

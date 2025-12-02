package bg.energo.phoenix.service.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.service.billing.invoice.enums.InvoiceObjectType;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceCreationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus.REAL;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventListener {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePrefixService invoicePrefixService;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Generates invoice numbers after successful commit;
     */
    public void saveInvoiceNumber(InvoiceCreationEvent event) {

        Set<Long> invoices = event.getInvoices();
        List<Object[]> invoiceList = invoiceRepository.findAllWithPrefixByInvoiceIds(invoices);
        log.debug("InvoiceNumbers {}", invoices);
        Map<Boolean, List<Object[]>> groupedBaseOnObjectType = invoiceList
                .stream()
                .collect(Collectors.groupingBy(x -> !(((Invoice) x[0]).getInvoiceStatus().equals(REAL) && !((Invoice) x[0]).getInvoiceDocumentType().equals(InvoiceDocumentType.PROFORMA_INVOICE)) && List.of(InvoiceObjectType.GOODS_ORDER, InvoiceObjectType.SERVICE_ORDER).contains(InvoiceObjectType.defineInvoiceObjectType((Invoice) x[0]))));
        groupedBaseOnObjectType.forEach((key, value) -> {
            if (key.equals(Boolean.TRUE)) {
                processOrderInvoices(value);
            } else {
                processBillingInvoices(value);
            }
        });
    }


    public void processOrderInvoices(List<Object[]> invoiceList) {
        List<String> draftInvoiceNumbers = invoiceRepository.findAvailableDraftProformaInvoiceNumbers();
        for (int i = 0; i < invoiceList.size(); i++) {
            Invoice invoice = (Invoice) invoiceList.get(i)[0];
            PrefixType prefixType = (PrefixType) invoiceList.get(i)[1];
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
                invoiceNumber = String.format("%010d", invoiceRepository.getRealProformaNextSequenceValue());
            } else {
                if (!draftInvoiceNumbers.isEmpty()) {
                    String tempNumber = draftInvoiceNumbers.remove(0);
                    invoiceNumber = tempNumber.substring(tempNumber.indexOf('-') + 1);
                } else {
                    invoiceNumber = String.format("%09d", invoiceRepository.getDraftProformaNextSequenceValue());
                }

            }

            String finalInvoiceNumber = "%s%s-%s".formatted(invoicePrefix,
                    invoiceStatus.equals(InvoiceStatus.DRAFT) ? "D" : "",
                    invoiceNumber);
            invoice.setInvoiceNumber(finalInvoiceNumber);
        }
        invoiceRepository.saveAllAndFlush(invoiceList.stream().map(obj -> (Invoice) obj[0]).toList());
    }

    public void processBillingInvoices(List<Object[]> invoiceList) {
        List<String> draftInvoiceNumbers = invoiceRepository.findAvailableDraftInvoiceNumbers();
        log.debug("draftInvoiceNumbers{}", draftInvoiceNumbers);
        for (int i = 0; i < invoiceList.size(); i++) {
            Invoice invoice = (Invoice) invoiceList.get(i)[0];
            PrefixType prefixType = (PrefixType) invoiceList.get(i)[1];
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
                invoiceNumber = String.format("%010d", invoiceRepository.getRealNextSequenceValue());
            } else {
                if (!draftInvoiceNumbers.isEmpty()) {
                    String tempNumber = draftInvoiceNumbers.remove(0);
                    log.debug("draftInvoiceNumber {}", tempNumber);
                    invoiceNumber = tempNumber.substring(tempNumber.indexOf('-') + 1);
                } else {
                    invoiceNumber = String.format("%09d", invoiceRepository.getDraftNextSequenceValue());
                }

            }

            String finalInvoiceNumber = "%s%s-%s".formatted(invoicePrefix,
                    invoiceStatus.equals(InvoiceStatus.DRAFT) ? "D" : "",
                    invoiceNumber);
            invoice.setInvoiceNumber(finalInvoiceNumber);
        }
        invoiceRepository.saveAll(invoiceList.stream().map(obj -> (Invoice) obj[0]).toList());
    }
}

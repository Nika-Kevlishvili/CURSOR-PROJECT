package bg.energo.phoenix.service.billing.invoice;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
import bg.energo.phoenix.service.billing.invoice.enums.InvoiceObjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePrefixService {

    private final PrefixRepository prefixRepository;


    @Value("${invoice.prefix.productInterim}")
    private Long productInterim;
    @Value("${invoice.prefix.productDebit}")
    private Long productDebit;
    @Value("${invoice.prefix.productCredit}")
    private Long productCredit;
    @Value("${invoice.prefix.productInvoice}")
    private Long productInvoice;

    @Value("${invoice.prefix.serviceInterim}")
    private Long serviceInterim;
    @Value("${invoice.prefix.serviceGoodsDebit}")
    private Long serviceGoodsDebit;
    @Value("${invoice.prefix.serviceGoodsCredit}")
    private Long serviceGoodsCredit;
    @Value("${invoice.prefix.serviceGoodsInvoice}")
    private Long serviceGoodsInvoice;

    public String getInvoiceNumber(InvoiceObjectType invoiceObjectType, InvoiceType invoiceType, InvoiceDocumentType documentType, PrefixType prefixType) {
        if (invoiceObjectType == null || invoiceType == null) {
            return "Unknown";
        }

        if (invoiceObjectType.equals(InvoiceObjectType.PRODUCT_CONTRACT)) {
            if (invoiceType.equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)) {
                return getSuffixById(productInterim);
            }
            if(invoiceType.equals(InvoiceType.RECONNECTION)){
                return getSuffixById(serviceGoodsInvoice);
            }
            if (documentType.equals(InvoiceDocumentType.DEBIT_NOTE)) {
                return getSuffixById(productDebit);
            }
            if (documentType.equals(InvoiceDocumentType.CREDIT_NOTE)) {
                return getSuffixById(productCredit);
            }


            return getSuffixById(productInvoice);

        } else {


            if (invoiceType.equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)) {
                if (prefixType == null) {
                    return getSuffixById(serviceInterim);
                }
            }

            if (documentType.equals(InvoiceDocumentType.INVOICE) || documentType.equals(InvoiceDocumentType.PROFORMA_INVOICE)) {
                if (Objects.nonNull(prefixType)) {
                    switch (prefixType) {
                        case GOODS, SERVICE -> {
                            if (invoiceType.equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)) {
                                return getSuffixById(serviceInterim);
                            } else {
                                return getSuffixById(serviceGoodsInvoice);
                            }
                        }
                        case PRODUCT -> {
                            if (invoiceType.equals(InvoiceType.INTERIM_AND_ADVANCE_PAYMENT)) {
                                return getSuffixById(productInterim);
                            } else {
                                return getSuffixById(productInvoice);
                            }

                        }
                    }
                }
                return getSuffixById(serviceGoodsInvoice);
            }
            if (documentType.equals(InvoiceDocumentType.DEBIT_NOTE)) {
                return getSuffixById(serviceGoodsDebit);
            }
            if (documentType.equals(InvoiceDocumentType.CREDIT_NOTE)) {
                return getSuffixById(serviceGoodsCredit);
            }
        }
        return "Unknown";
    }

    private String getSuffixById(Long id) {
        Prefix prefix = prefixRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("prefix not found!;"));
        return prefix.getName();
    }
}

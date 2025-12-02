package bg.energo.phoenix.service.billing.invoice.numberGeneration;

import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberTableRepository invoiceNumberTableRepository;




    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateNumber(NumberType numberType){

        Optional<InvoiceNumberTable> firstByNumberType = invoiceNumberTableRepository.findFirstByNumberType(numberType);
        if(firstByNumberType.isPresent()) {
            InvoiceNumberTable invoiceNumberTable = firstByNumberType.get();
            invoiceNumberTableRepository.deleteById(invoiceNumberTable.getId());
            return invoiceNumberTable.getInvoiceNumber();
        }
        return switch (numberType){
            case DRAFT_INVOICE -> String.format("%09d", invoiceRepository.getDraftNextSequenceValue());
            case DRAFT_PROFORMA -> String.format("%09d", invoiceRepository.getDraftProformaNextSequenceValue());
            case REAL -> String.format("%010d", invoiceRepository.getRealNextSequenceValue());
            case REAL_PROFORMA -> String.format("%010d", invoiceRepository.getRealProformaNextSequenceValue());
        };
    }

    public void save(InvoiceNumberTable invoiceNumberTable) {
        invoiceNumberTableRepository.save(invoiceNumberTable);
    }
}

package bg.energo.phoenix.util.contract;

import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static bg.energo.phoenix.util.epb.EPBFinalFields.CONTRACT_NUMBER_PREFIX;

@Service
@RequiredArgsConstructor
public class ContractUtils {
    private final ProductContractRepository productContractRepository;

    /**
     * Returns the next contract number in the system in the format PREFYYMMXXXXXX,
     * where PREF is the corresponding prefix for the product and service contracts (EPES),
     * YYMM is the current year and month, and XXXXXX is the serial number of the contract for the month.
     * The serial number counter is being reset once in a defined period of time (1 month at the moment).
     */
    public String getNextContractNumber() {
        String nextSequenceValue = productContractRepository.getNextSequenceValue();
        String datePadding = DateTimeFormatter.ofPattern("yyMM").format(LocalDate.now());
        String numberPadding = "0".repeat(Math.max(0, 6 - nextSequenceValue.length()));
        return "%s%s%s%s".formatted(CONTRACT_NUMBER_PREFIX, datePadding, numberPadding, nextSequenceValue);
    }


    /**
     * Resets the contract serial number sequence.
     */
    public void resetContractNumberSequence() {
        productContractRepository.resetContractNumberSequence();
    }
}

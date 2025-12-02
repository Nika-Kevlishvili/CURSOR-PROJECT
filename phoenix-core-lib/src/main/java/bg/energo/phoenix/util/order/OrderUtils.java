package bg.energo.phoenix.util.order;

import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrderUtils {
    private final ServiceOrderRepository serviceOrderRepository;

    /**
     * Returns the next order number in the system in the format PREFYYMMXXXXXX,
     * where PREF is the corresponding prefix for the service and goods orders (ORES),
     * YYMM is the current year and month, and XXXXXX is the serial number of the order for the month.
     * The serial number counter is being reset once in a defined period of time (1 month at the moment).
     */
    public String getNextOrderNumber() {
        Long nextOrderNumber = serviceOrderRepository.getNextOrderNumber();
        String datePadding = DateTimeFormatter.ofPattern("yyMM").format(LocalDate.now());
        String numberPadding = "0".repeat(Math.max(0, 6 - nextOrderNumber.toString().length()));
        return "%s%s%s%s".formatted(EPBFinalFields.ORDER_NUMBER_PREFIX, datePadding, numberPadding, nextOrderNumber);
    }


    /**
     * Resets the order serial number sequence.
     */
    public void resetOrderNumberSequence() {
        serviceOrderRepository.resetOrderNumberSequence();
    }

}

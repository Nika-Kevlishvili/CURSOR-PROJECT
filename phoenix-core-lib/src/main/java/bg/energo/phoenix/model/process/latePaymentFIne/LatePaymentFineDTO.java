package bg.energo.phoenix.model.process.latePaymentFIne;

import com.fasterxml.jackson.databind.JsonNode;

public interface LatePaymentFineDTO {
     Long getId();
     JsonNode getLfp();
}

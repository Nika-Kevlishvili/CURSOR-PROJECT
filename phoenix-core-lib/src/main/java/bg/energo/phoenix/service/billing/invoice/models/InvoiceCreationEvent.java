package bg.energo.phoenix.service.billing.invoice.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvoiceCreationEvent {

   private Set<Long> invoices;

}

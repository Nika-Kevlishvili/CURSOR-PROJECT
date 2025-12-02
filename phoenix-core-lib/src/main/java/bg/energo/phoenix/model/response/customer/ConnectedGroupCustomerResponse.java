package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.Data;

@Data
public class ConnectedGroupCustomerResponse {

    private Long customerId;
    private String customerName;
    private String identifier;

    public ConnectedGroupCustomerResponse(Long id, String personalNumber,CustomerType type,String name, String middleName, String lastName,String legalFormName) {
        this.customerId = id;
        this.identifier = personalNumber;
        if(type.equals(CustomerType.LEGAL_ENTITY)){
            this.customerName=name;
        }else {
            this.customerName=String.format("%s %s %s",name,middleName==null? "": middleName,lastName);
        }
        if(legalFormName!=null){
            this.customerName="%s %s".formatted(customerName,legalFormName);
        }
    }
}

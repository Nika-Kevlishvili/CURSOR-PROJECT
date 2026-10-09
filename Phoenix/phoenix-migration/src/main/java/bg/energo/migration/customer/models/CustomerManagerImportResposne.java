package bg.energo.migration.customer.models;

import lombok.Data;

@Data
public class CustomerManagerImportResposne {
    private String customerIdentifier;
    private String title1;
    private String name1;
    private String middlename1;
    private String surname1;
    private String position1;
    private String representation1;
    private String personalNumber1;
    private String title2;
    private String name2;
    private String middlename2;
    private String surname2;
    private String position2;
    private String representation2;
    private String title3;
    private String name3;
    private String middlename3;
    private String surname3;
    private String position3;
    private String representation3;

    public CustomerManagerImportResposne(CustomerManagerImportMiddleResponse manager) {
        this.customerIdentifier = manager.getCustomerIdentifier();
        this.title1 = manager.getTitle1();
        this.name1 = manager.getName1();
        this.middlename1 = manager.getMiddlename1();
        this.surname1 = manager.getSurname1();
        this.position1 = manager.getPosition1();
        this.representation1 = manager.getRepresentation1();
        this.personalNumber1 = manager.getPersonalNumber1();
        this.title2 = manager.getTitle2();
        this.name2 = manager.getName2();
        this.middlename2 = manager.getMiddlename2();
        this.surname2 = manager.getSurname2();
        this.position2 = manager.getPosition2();
        this.representation2 = manager.getRepresentation2();
        this.title3 = manager.getTitle3();
        this.name3 = manager.getName3();
        this.surname3 = manager.getSurname3();
        this.middlename3 = manager.getMiddlename3();
        this.position3 = manager.getPosition3();
        this.representation3 = manager.getRepresentation3();
    }
}

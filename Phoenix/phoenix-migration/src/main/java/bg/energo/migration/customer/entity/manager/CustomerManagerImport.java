package bg.energo.migration.customer.entity.manager;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

@Data
@Entity
@Immutable
@Table(name = "managers_for_import_tab1", schema = "customer_migration")
public class CustomerManagerImport {

    @Id
    private Long id;

    @Column(name = "Customer")
    private String customerId;

    @Column(name = "Title-Manger1")
    private String titleName1;

    @Column(name = "Name-Manger1")
    private String name1;

    @Column(name = "Position-Manger1")
    private String jobPosition1;

    @Column(name = "Representation-Manager1")
    private String representationMethod1;

    @Column(name = "Personalnumber-Manager1")
    private String personalNumber1;

    @Column(name = "Title-Manger2")
    private String titleName2;

    @Column(name = "Name-Manger2")
    private String name2;

    @Column(name = "Position-Manger2")
    private String jobPosition2;

    @Column(name = "Representation-Manager2")
    private String representationMethod2;

    @Column(name = "Title-Manger3")
    private String titleName3;

    @Column(name = "Name-Manger3")
    private String name3;

    @Column(name = "Position-Manger3")
    private String jobPosition3;

    @Column(name = "Representation-Manager3")
    private String representationMethod3;
}

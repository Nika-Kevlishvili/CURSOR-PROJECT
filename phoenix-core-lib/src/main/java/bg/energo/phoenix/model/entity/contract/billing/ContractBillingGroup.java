package bg.energo.phoenix.model.entity.contract.billing;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.billing.BillingGroupSendingInvoice;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupNumberWrapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contract_billing_groups",schema = "product_contract")

@SqlResultSetMapping(
        name = "product_contract.groupNumberMapping",
        classes = {
                @ConstructorResult(targetClass = BillingGroupNumberWrapper.class,
                        columns = {@ColumnResult(name = "number")}
                )}
)
@NamedNativeQuery(
        name = "product_contract.group_number_calculator",
        query = """
            select cbg.group_number as number
            from
            product_contract.contract_billing_groups cbg
            where
            cbg.contract_id in
            (select c.id from product_contract.contracts c
            join
            product_contract.contract_details cd
            on cd.contract_id =  c.id
            where cd.customer_detail_id in(
            select cd.id from customer.customer_details cd where cd.customer_id in
            (select
             (select customer_id from customer.customer_details cdd where cdd.id =  cd.customer_detail_id) as customer_id
            from product_contract.contract_details cd
            where cd.contract_id = :contractId )))
            and cbg.status='ACTIVE'
            order by number
""",
        resultSetMapping = "product_contract.groupNumberMapping"
)
public class ContractBillingGroup extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "contract_billing_groups_id_seq",
            sequenceName = "product_contract.contract_billing_groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contract_billing_groups_id_seq"
    )
    private Long id;

    @Column(name = "group_number")
    private String groupNumber;

    @Column(name = "sending_invoice")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingGroupSendingInvoice sendingInvoice;
    
    @Column(name = "separate_invoice_for_each_pod")
    private Boolean separateInvoiceForEachPod;
    
    @Column(name = "direct_debit")
    private Boolean directDebit;
    
    @Column(name = "bank_id")
    private Long bankId;
    
    @Column(name = "iban")
    private String iban;
    
    @Column(name = "alt_invoice_recipient_customer_detail_id")
    private Long alternativeRecipientCustomerDetailId;
    
    @Column(name = "customer_communication_id_for_billing")
    private Long billingCustomerCommunicationId;
    
    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}

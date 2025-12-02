package bg.energo.phoenix.model.entity.template;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.product.product.PurposeOfConsumption;
import bg.energo.phoenix.model.enums.template.*;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "template_details", schema = "template")
public class ContractTemplateDetail extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_details_id_gen")
    @SequenceGenerator(name = "template_details_id_gen", schema = "template", sequenceName = "template_details_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "template_id")
    private Long templateId;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Size(max = 1024)
    @NotNull
    @Column(name = "name", nullable = false, length = 1024)
    private String name;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "template.template_file_name"
            )
    )
    @Column(name = "file_name", columnDefinition = "template.template_file_name[]")
    private List<ContractTemplateFileName> fileName;

    @Column(name = "output_file_format", columnDefinition = "template.template_output_file_format[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "template.template_output_file_format"
            )
    )
    private List<ContractTemplateFileFormat> outputFileFormat;

    @Column(name = "file_name_sufix")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractTemplateSuffix fileNameSuffix;

    @Column(name = "file_signing", columnDefinition = "template.template_file_signing[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "template.template_file_signing"
            )
    )
    private List<ContractTemplateSigning> fileSigning;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractTemplateType templateType;

    @Column(name = "subject")
    private String subject;

    @Column(name = "file")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractTemplateFile file;

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractTemplateLanguage language;

    @Column(name = "customer_type", columnDefinition = "template.template_customer_type[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "template.template_customer_type"
            )
    )
    private List<CustomerType> customerType;

    @Column(name = "purpose_of_consumption", columnDefinition = "template.template_purpose_of_consumption[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "template.template_purpose_of_consumption"
            )
    )
    private List<PurposeOfConsumption> consumptionPurposes;

    @Column(name = "file_name_prefix")
    private String fileNamePrefix;

    @Column(name = "template_file_id")
    private Long templateFileId;

    @Column(name = "version")
    private Integer version;

}
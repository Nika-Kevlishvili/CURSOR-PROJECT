package bg.energo.phoenix.process.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "template", schema = "process_management")

@Data
public class Template {
    @Id
    private String templateName;
    private String fileUrl;
}

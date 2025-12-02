package bg.energo.phoenix.process.repository;

import bg.energo.phoenix.process.model.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, String> {
}

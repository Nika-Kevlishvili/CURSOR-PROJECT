package bg.energo.phoenix.repository;

import bg.energo.phoenix.model.entity.EasyPayVerifyDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EasyPayVerifyDetailsRepository extends JpaRepository<EasyPayVerifyDetailsEntity, Long> {
}

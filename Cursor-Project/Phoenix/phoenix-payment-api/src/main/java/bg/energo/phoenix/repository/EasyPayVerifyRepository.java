package bg.energo.phoenix.repository;

import bg.energo.phoenix.model.entity.EasyPayVerifyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EasyPayVerifyRepository extends JpaRepository<EasyPayVerifyEntity, Long> {
}

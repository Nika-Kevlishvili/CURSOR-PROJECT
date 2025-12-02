package bg.energo.phoenix.service.signing.qes.repositories;

import bg.energo.phoenix.service.signing.qes.entities.QesAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QesAliasRepository extends JpaRepository<QesAlias, String> {
}

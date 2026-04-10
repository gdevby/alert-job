package by.gdev.alert.job.core.repository;

import by.gdev.alert.job.core.model.db.UserSiteCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSiteCredentialRepository extends JpaRepository<UserSiteCredential, Long> {

    List<UserSiteCredential> findByUserUuid(String userUuid);

    Optional<UserSiteCredential> findByUserUuidAndSiteIdAndModuleId(
            String userUuid,
            Long siteId,
            Long moduleId
    );
}

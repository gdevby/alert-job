package by.gdev.alert.job.core.repository.ai;

import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSiteCredentialRepository extends JpaRepository<UserSiteCredential, Long> {
    List<UserSiteCredential> findByUserUuid(String userUuid);
    List<UserSiteCredential> findByUserUuidAndSiteId(String userUuid, Long siteId);
    Optional<UserSiteCredential> findByUserUuidAndSiteIdAndName(String userUuid, Long siteId, String name);
}
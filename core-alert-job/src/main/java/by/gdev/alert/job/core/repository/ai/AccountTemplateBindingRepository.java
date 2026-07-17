package by.gdev.alert.job.core.repository.ai;

import by.gdev.alert.job.core.model.db.ai.AccountTemplateBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountTemplateBindingRepository extends JpaRepository<AccountTemplateBinding, Long> {

    List<AccountTemplateBinding> findAllByModuleId(Long moduleId);

    boolean existsByModuleIdAndAccountIdAndTemplateId(Long moduleId, Long accountId, Long templateId);

    boolean existsByModuleIdAndAccountIdAndTemplateIdAndPromtId(Long moduleId, Long accountId, Long templateId, Long promtId);

    Optional<AccountTemplateBinding> findByModuleIdAndAccountIdAndActiveTrue(Long moduleId, Long accountId);

    List<AccountTemplateBinding> findByAccountId(Long accountId);

    // Найти все биндинги по модулю и списку аккаунтов
    List<AccountTemplateBinding> findByModuleIdAndAccountIdIn(Long moduleId, List<Long> accountIds);
}

package by.gdev.alert.job.llm.service.template;

import by.gdev.alert.job.llm.domain.LlmUser;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.repository.LlmUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LlmUserService {

    private final LlmUserRepository userRepository;

    @Transactional
    public LlmUser getOrCreateUser(String uuid) {
        return userRepository.findByUuid(uuid)
                .orElseGet(() -> {
                    LlmUser u = new LlmUser();
                    u.setUuid(uuid);
                    return userRepository.save(u);
                });
    }

    @Transactional
    public LlmUser saveUser(AiAppUserDTO dto) {
        if (dto == null || dto.getUuid() == null) {
            return null;
        }

        return userRepository.findByUuid(dto.getUuid())
                .orElseGet(() -> {
                    LlmUser u = new LlmUser();
                    u.setUuid(dto.getUuid());
                    return userRepository.save(u);
                });
    }



    public LlmUser getByUuid(String uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("LlmUser not found: " + uuid));
    }
}

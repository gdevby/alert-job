package by.gdev.alert.job.llm.service.template;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.LlmUser;
import by.gdev.alert.job.llm.domain.dto.template.CreateTemplateRequest;
import by.gdev.alert.job.llm.repository.AiReplyTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReplyTemplateService {

    private final AiReplyTemplateRepository templateRepository;
    private final LlmUserService userService;

    public AiReplyTemplate getTemplateForUserAndModule(String userUuid, Long moduleId) {
        LlmUser user = userService.getOrCreateUser(userUuid);
        return templateRepository.findByUserAndModuleId(user, moduleId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AiReplyTemplate> getTemplatesByUser(String userUuid) {
        LlmUser user = userService.getOrCreateUser(userUuid);
        return templateRepository.findByUser(user);
    }

    @Transactional
    public AiReplyTemplate createOrUpdateTemplate(CreateTemplateRequest req) {

        if (req.getUserUuid() == null || req.getModuleId() == null) {
            throw new IllegalArgumentException("userUuid and moduleId are required");
        }

        LlmUser user = userService.getOrCreateUser(req.getUserUuid());

        AiReplyTemplate template = templateRepository
                .findByUserAndModuleId(user, req.getModuleId())
                .orElseGet(AiReplyTemplate::new);

        template.setUser(user);
        template.setModuleId(req.getModuleId());
        template.setHtmlTemplate(req.getHtmlTemplate());

        return templateRepository.save(template);
    }
}

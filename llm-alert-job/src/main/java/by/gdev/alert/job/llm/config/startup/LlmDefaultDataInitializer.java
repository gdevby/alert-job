package by.gdev.alert.job.llm.config.startup;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import by.gdev.alert.job.llm.repository.AiReplyTemplateRepository;
import by.gdev.alert.job.llm.repository.promt.AiPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * Создаёт системные DEFAULT_PROMPT и DEFAULT_TEMPLATE при первом старте.
 * Единственный источник текстов — {@code prompts/default-system-prompt.txt}
 * и {@code prompts/default-reply-template.txt}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LlmDefaultDataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PROMPT_NAME = "DEFAULT_PROMPT";
    private static final String DEFAULT_TEMPLATE_NAME = "DEFAULT_TEMPLATE";
    private static final String DEFAULT_PROMPT_RESOURCE = "prompts/default-system-prompt.txt";
    private static final String DEFAULT_TEMPLATE_RESOURCE = "prompts/default-reply-template.txt";

    private final AiPromptRepository aiPromptRepository;
    private final AiReplyTemplateRepository templateRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedDefaultPrompt();
        seedDefaultTemplate();
    }

    private void seedDefaultPrompt() throws Exception {
        if (aiPromptRepository.findByName(DEFAULT_PROMPT_NAME).isPresent()) {
            return;
        }
        AiPrompt prompt = AiPrompt.builder()
                .type(AiPromptType.DEFAULT)
                .name(DEFAULT_PROMPT_NAME)
                .promptText(readClasspathResource(DEFAULT_PROMPT_RESOURCE))
                .version(1)
                .build();
        aiPromptRepository.save(prompt);
        log.info("Создан системный промт {}", DEFAULT_PROMPT_NAME);
    }

    private void seedDefaultTemplate() throws Exception {
        if (!templateRepository.findByName(DEFAULT_TEMPLATE_NAME).isEmpty()) {
            return;
        }
        AiReplyTemplate template = new AiReplyTemplate();
        template.setName(DEFAULT_TEMPLATE_NAME);
        template.setText(readClasspathResource(DEFAULT_TEMPLATE_RESOURCE));
        templateRepository.save(template);
        log.info("Создан системный шаблон {}", DEFAULT_TEMPLATE_NAME);
    }

    private String readClasspathResource(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}

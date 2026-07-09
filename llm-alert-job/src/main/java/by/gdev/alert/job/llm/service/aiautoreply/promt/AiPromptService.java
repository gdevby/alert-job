package by.gdev.alert.job.llm.service.aiautoreply.promt;

import by.gdev.alert.job.llm.domain.LlmUser;
import by.gdev.alert.job.llm.domain.dto.promt.AiPromptDto;
import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import by.gdev.alert.job.llm.repository.promt.AiPromptRepository;
import by.gdev.alert.job.llm.service.template.LlmUserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final AiPromptRepository aiPromptRepository;
    private final LlmUserService userService;

    /**
     * Создать или обновить промт пользователя.
     */
    @Transactional
    public AiPromptDto createOrUpdatePrompt(String userUuid, String name, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст промта не должен быть пустым");
        }

        LlmUser user = userService.getOrCreateUser(userUuid);

        // Ищем существующий промт пользователя с таким именем
        Optional<AiPrompt> existingOpt = aiPromptRepository.findByUserAndName(user, name).stream().findFirst();

        AiPrompt prompt;
        if (existingOpt.isPresent()) {
            prompt = existingOpt.get();
            prompt.setPromptText(text);
            prompt.setVersion(prompt.getVersion() + 1);
        } else {
            prompt = AiPrompt.builder()
                    .name(name)
                    .promptText(text)
                    .version(1)
                    .user(user)
                    .type(AiPromptType.CUSTOM)
                    .build();
        }
        AiPrompt saved = aiPromptRepository.save(prompt);
        return toDto(saved);
    }

    /**
     * Получить промт по ID.
     */
    @Transactional(readOnly = true)
    public AiPromptDto getPromptByIdOrDefault(String uuid, Long id) {
        // Проверяем существование пользователя
        if (!userService.existsByUuid(uuid)) {
            return null; // пользователь не найден
        }

        Optional<AiPrompt> promptOpt = aiPromptRepository.findById(id);
        if (promptOpt.isPresent()) {
            AiPrompt prompt = promptOpt.get();
            // Если промт глобальный или принадлежит пользователю – возвращаем DTO
            if (prompt.getUser() == null || prompt.getUser().getUuid().equals(uuid)) {
                return toDto(prompt);
            }
        }

        // Если промт не найден или не доступен – возвращаем дефолтный DTO
        return toDto(getDefaultPromptEntity());
    }


    public AiPrompt getDefaultPromptEntity() {
        return aiPromptRepository.findByName("DEFAULT_PROMPT")
                .orElseThrow(() -> new IllegalStateException("DEFAULT_PROMPT not found"));
    }

    @Transactional
    public void deletePrompt(String uuid, Long id) {
        // 1. Находим промт
        AiPrompt prompt = aiPromptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Промт не найден для id: " + id));

        // 2. Запрещаем удаление системного DEFAULT_PROMPT
        if (prompt.getType() == AiPromptType.DEFAULT) {
            throw new IllegalArgumentException("Нельзя удалить системный промт по умолчанию");
        }

        // 3. Проверяем, что промт принадлежит пользователю
        if (prompt.getUser() == null || !prompt.getUser().getUuid().equals(uuid)) {
            throw new IllegalArgumentException("У вас нет прав на удаление этого промта");
        }

        // 4. Удаляем
        aiPromptRepository.delete(prompt);
    }

    /**
     * Возвращает список DTO промтов пользователя.
     */
    @Transactional(readOnly = true)
    public List<AiPromptDto> getAllPromptDtos(String userUuid) {
        LlmUser user = userService.getOrCreateUser(userUuid);
        List<AiPromptDto> dtos = aiPromptRepository.findByUser(user).stream()
                .map(this::toDto) // используем отдельный метод
                .collect(Collectors.toCollection(ArrayList::new));

        // Добавляем дефолтный промт, если его ещё нет
        try {
            AiPrompt defaultPrompt = getDefaultPromptEntity();
            boolean alreadyExists = dtos.stream()
                    .anyMatch(dto -> dto.getId().equals(defaultPrompt.getId()));
            if (!alreadyExists) {
                dtos.add(toDto(defaultPrompt));
            }
        } catch (IllegalStateException e) {
            // Дефолтный промт не найден – просто продолжаем
        }

        return dtos;
    }



    public boolean existsById(String uuid, Long id) {
        if (id == null) return false;
        Optional<AiPrompt> promptOpt = aiPromptRepository.findById(id);
        if (promptOpt.isEmpty()) return false;
        AiPrompt prompt = promptOpt.get();
        return prompt.getUser() == null || prompt.getUser().getUuid().equals(uuid);
    }
    /**
     * Преобразует сущность AiPrompt в DTO.
     */
    private AiPromptDto toDto(AiPrompt prompt) {
        return AiPromptDto.builder()
                .id(prompt.getId())
                .name(prompt.getName())
                .text(prompt.getPromptText()) 
                .version(prompt.getVersion())
                .createdAt(prompt.getCreatedAt())
                .updatedAt(prompt.getUpdatedAt())
                .build();
    }

}

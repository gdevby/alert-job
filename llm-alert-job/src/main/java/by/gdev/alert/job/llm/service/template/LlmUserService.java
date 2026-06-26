package by.gdev.alert.job.llm.service.template;

import by.gdev.alert.job.llm.client.CoreClient;
import by.gdev.alert.job.llm.domain.LlmUser;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.repository.LlmUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для работы с пользователями LLM‑модуля.
 * <p>
 * Отвечает за:
 * <ul>
 *     <li>проверку существования пользователя в CORE‑сервисе;</li>
 *     <li>создание локальной записи пользователя при необходимости;</li>
 *     <li>поиск пользователей по UUID;</li>
 *     <li>синхронизацию данных между CORE и LLM‑модулем.</li>
 * </ul>
 * <p>
 * Локальная таблица хранит только UUID, а все остальные данные остаются в CORE.
 */
@Service
@RequiredArgsConstructor
public class LlmUserService {

    /**
     * Клиент для взаимодействия с CORE‑модулем.
     */
    private final CoreClient coreClient;

    /**
     * Репозиторий для работы с локальными пользователями LLM‑модуля.
     */
    private final LlmUserRepository userRepository;

    /**
     * Возвращает локального пользователя по UUID или создаёт нового,
     * предварительно проверив его существование в CORE‑сервисе.
     *
     * @param uuid уникальный идентификатор пользователя
     * @return существующий или вновь созданный пользователь
     * @throws RuntimeException если пользователь отсутствует в CORE
     */
    @Transactional
    public LlmUser getOrCreateUser(String uuid) {
        AiAppUserDTO coreUser = coreClient.getUserByUuid(uuid);
        if (coreUser == null) {
            throw new RuntimeException("User does not exist in CORE: " + uuid);
        }

        return userRepository.findByUuid(uuid)
                .orElseGet(() -> {
                    LlmUser u = new LlmUser();
                    u.setUuid(uuid);
                    return userRepository.save(u);
                });
    }

    /**
     * Сохраняет пользователя на основе данных из CORE.
     * Если пользователь уже существует — возвращает существующего.
     *
     * @param dto DTO пользователя из CORE
     * @return сохранённый или найденный пользователь, либо null если DTO некорректен
     */
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

    /**
     * Возвращает пользователя по UUID.
     *
     * @param uuid уникальный идентификатор пользователя
     * @return найденный пользователь
     * @throws RuntimeException если пользователь отсутствует в локальной базе
     */
    public LlmUser getByUuid(String uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("LlmUser not found: " + uuid));
    }
}

package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.AppUserDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.service.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class AppUserController {

    private final AppUserService userService;

    @GetMapping("/{uuid}")
    public ResponseEntity<?> getUserByUuid(@PathVariable String uuid) {
        try {
            AppUser user = userService.findByUuid(uuid)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            AppUserDTO dto = new AppUserDTO();
            dto.setUuid(user.getUuid());
            dto.setEmail(user.getEmail());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Ошибка при получении пользователя в CORE {}", uuid, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}


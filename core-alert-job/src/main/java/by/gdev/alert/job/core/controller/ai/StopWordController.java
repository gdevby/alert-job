package by.gdev.alert.job.core.controller.ai;

import by.gdev.alert.job.core.model.ai.AiStopWordDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/stop-words")
@RequiredArgsConstructor
@Tag(name = "Стоп-слова", description = "Управление стоп-словами при автоответе на заказы")
public class StopWordController {


    @GetMapping
    @Operation(summary = "Получить все стоп-слова")
    public ResponseEntity<List<AiStopWordDTO>> getAll() {
        return ResponseEntity.ok(null);
    }

    @PostMapping
    @Operation(summary = "Добавить стоп-слово")
    public ResponseEntity<AiStopWordDTO> add(@RequestBody AiStopWordDTO request) {
        try {
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            log.error("Ошибка добавления стоп-слова: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить стоп-слово по ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка удаления стоп-слова: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/by-word")
    @Operation(summary = "Удалить стоп-слово по тексту")
    public ResponseEntity<Void> deleteByWord(@RequestParam String word) {
        try {
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка удаления стоп-слова: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
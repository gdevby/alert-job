package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.binding.dto.BindingResponse;
import by.gdev.alert.job.core.model.db.ai.AccountTemplateBinding;
import by.gdev.alert.job.core.service.ai.AccountTemplateBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bindings")
@RequiredArgsConstructor
public class AccountTemplateBindingController {

    private final AccountTemplateBindingService service;

    @PostMapping
    public ResponseEntity<AccountTemplateBinding> create(
            @RequestParam Long moduleId,
            @RequestParam Long accountId,
            @RequestParam Long templateId,
            @RequestParam(defaultValue = "true") boolean active
    ) {
        return ResponseEntity.ok(
                service.create(moduleId, accountId, templateId, active)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountTemplateBinding> update(
            @PathVariable Long id,
            @RequestParam Long moduleId,
            @RequestParam Long accountId,
            @RequestParam Long templateId,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(
                service.update(id, moduleId, accountId, templateId, active)
        );
    }

    @GetMapping("/{moduleId}")
    public ResponseEntity<List<AccountTemplateBinding>> getByModule(@PathVariable Long moduleId) {
        return ResponseEntity.ok(service.getByModule(moduleId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<AccountTemplateBinding> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<AccountTemplateBinding> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    @PostMapping("/{id}/active")
    public ResponseEntity<AccountTemplateBinding> setActive(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @GetMapping("/user/{uuid}/all")
    public ResponseEntity<?> getAllBindingsForUser(@PathVariable String uuid) {
        try {
            List<BindingResponse> result = service.getBindingsForUser(uuid);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}

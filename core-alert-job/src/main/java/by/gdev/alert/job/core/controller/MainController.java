package by.gdev.alert.job.core.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import by.gdev.alert.job.core.model.AlertTime;
import by.gdev.alert.job.core.model.AppUserDTO;
import by.gdev.alert.job.core.model.Modules;
import by.gdev.alert.job.core.model.OrderModulesDTO;
import by.gdev.alert.job.core.model.Source;
import by.gdev.alert.job.core.model.SourceDTO;
import by.gdev.alert.job.core.model.UserAlertTimeDTO;
import by.gdev.alert.job.core.service.CoreService;
import by.gdev.common.model.HeaderName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/api/")
@RequiredArgsConstructor
@Validated
public class MainController {

    private final CoreService coreService;

    @PostMapping("user/authentication")
    public Mono<ResponseEntity<String>> userAuthentication(
	    @RequestHeader(required = false, name = HeaderName.UUID_USER_HEADER) String uuid,
	    @RequestHeader(required = false, name = HeaderName.EMAIL_USER_HEADER) String mail) {
	return coreService.authentication(uuid, mail);
    }

    @PostMapping("/test-message")
    public ResponseEntity<Mono<Void>> testMailMessage(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
	return ResponseEntity.ok(coreService.sendTestMessageOnMai(uuid));
    }

    @PatchMapping("user/alerts")
    public ResponseEntity<Mono<Boolean>> diactivateAlerts(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @RequestParam("status") boolean status) {
	return ResponseEntity.ok(coreService.changeAlertStatus(uuid, status));
    }

    @PatchMapping("user/alerts/type")
    public ResponseEntity<Mono<Boolean>> alertType(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @RequestParam("default_send") boolean defaultSend) {
	return ResponseEntity.ok(coreService.changeDefaultSendType(uuid, defaultSend));
    }

    @PatchMapping("user/telegram")
    public ResponseEntity<Mono<Void>> addUserTelegram(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @RequestParam("telegram_id") Long telegramId) {
	return ResponseEntity.ok(coreService.changeUserTelegram(uuid, telegramId));
    }

    @PatchMapping("user/alert-time")
    public ResponseEntity<Mono<UserAlertTimeDTO>> createAlertTime(
	    @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @RequestBody AlertTime alertTime) {
	return ResponseEntity.ok(coreService.addAlertTime(uuid, alertTime));
    }

    @DeleteMapping("user/alert-time/{id}")
    public Mono<ResponseEntity<Void>> deleteAlertTime(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @PathVariable("id") Long id) {
	return coreService.removeAlertTime(uuid, id);
    }

    @GetMapping("user/alert-info")
    public ResponseEntity<Mono<AppUserDTO>> getUserAlertInfo(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
	return ResponseEntity.ok(coreService.showUserAlertInfo(uuid));
    }

    @GetMapping("user/order-module")
    public ResponseEntity<Flux<OrderModulesDTO>> getOrderModules(
	    @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
	return ResponseEntity.ok(coreService.showOrderModules(uuid));
    }

    @PostMapping("user/order-module")
    public ResponseEntity<Mono<OrderModulesDTO>> addOrderModules(
	    @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @RequestBody Modules modules) {
	return ResponseEntity.ok(coreService.createOrderModules(uuid, modules));
    }

    @PatchMapping("user/order-module/{id}")
    public ResponseEntity<Mono<OrderModulesDTO>> changeOrderModulew(
	    @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @PathVariable("id") Long moduleId,
	    @RequestBody Modules modules) {
	return ResponseEntity.ok(coreService.updateOrderModules(uuid, moduleId, modules));
    }

    @DeleteMapping("user/order-module/{id}")
    public Mono<ResponseEntity<Void>> deleteOrderModules(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @PathVariable("id") Long id) {
	return coreService.removeOrderModules(uuid, id);
    }

    @GetMapping("user/module/{id}/source")
    public ResponseEntity<Flux<SourceDTO>> getSouceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @PathVariable("id") Long id) {
	return ResponseEntity.ok(coreService.showSourceSite(uuid, id));
    }

    @PostMapping("user/module/{id}/source")
    public Mono<ResponseEntity<SourceSiteDTO>> addSouceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @PathVariable("id") Long id, @RequestBody @Valid Source source) {
	return coreService.createSourceSite(uuid, id, source);
    }

    @DeleteMapping("user/module/{id}/source/{source_id}")
    public Mono<ResponseEntity<Void>> deleteSourceSite(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @PathVariable("id") Long id, @PathVariable("source_id") Long sourceId) {
	return coreService.removeSourceSite(uuid, id, sourceId);
    }

    @GetMapping("user/module/{id}/true-filter/orders")
    public ResponseEntity<Flux<OrderDTO>> getPartitioningOnTrueOrders(
	    @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @PathVariable("id") Long moduleId) {
	return ResponseEntity.ok(coreService.showTrueFilterOrders(uuid, moduleId));
    }

    @GetMapping("user/module/{id}/false-filter/orders")
    public ResponseEntity<Flux<OrderDTO>> getPartitioningOrders(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
	    @PathVariable("id") Long moduleId) {
	return ResponseEntity.ok(coreService.showFalseFilterOrders(uuid, moduleId));
    }

}
package by.gdev.alert.job.core.filter;

import by.gdev.alert.job.core.service.AppUserService;
import by.gdev.common.model.HeaderName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class UserIpFilter implements WebFilter {

    private final AppUserService appUserService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String uuid = exchange.getRequest().getHeaders().getFirst(HeaderName.UUID_USER_HEADER);
        if (uuid != null) {
            String ip = getClientIp(exchange);
            if (ip != null && !ip.equals("unknown") && !ip.equals("127.0.0.1") && !ip.equals("0:0:0:0:0:0:0:1")) {
                log.debug("Determined IP for user {}: {}", uuid, ip);
                Mono.fromRunnable(() -> appUserService.updateUserIpAndCountry(uuid, ip))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                null,
                                e -> log.error("Failed to update IP/country for user {}: {}", uuid, e.getMessage())
                        );
            } else {
                log.debug("Skipping IP update for user {} because IP is local or unknown", uuid);
            }
        }
        return chain.filter(exchange);
    }

    private String getClientIp(ServerWebExchange exchange) {
        var request = exchange.getRequest();
        var headers = request.getHeaders();

        String ip = normalizeIp(headers.getFirst("X-Real-IP"));
        if (isValidIp(ip)) {
            return ip;
        }

        ip = firstValidFromXff(headers.getFirst("X-Forwarded-For"));
        if (isValidIp(ip)) {
            return ip;
        }

        ip = parseForwardedFor(headers.getFirst("Forwarded"));
        if (isValidIp(ip)) {
            return ip;
        }

        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            if (remoteAddress.getAddress() != null) {
                ip = normalizeIp(remoteAddress.getAddress().getHostAddress());
            } else {
                String host = remoteAddress.getHostString();
                if (host != null && !host.isBlank() && !"<unresolved>".equals(host)) {
                    ip = normalizeIp(host);
                }
            }
            if (isValidIp(ip)) {
                return ip;
            }
        }

        log.debug("Could not determine client IP from headers or remote address");
        return null;
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank()
                && !"unknown".equalsIgnoreCase(ip)
                && !"-".equals(ip);
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return ip;
        }
        ip = ip.trim();
        if (ip.startsWith("\"") && ip.endsWith("\"")) {
            ip = ip.substring(1, ip.length() - 1);
        }
        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf(']'));
        } else if (ip.chars().filter(ch -> ch == ':').count() == 1 && ip.contains(".")) {
            ip = ip.substring(0, ip.indexOf(':'));
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        if (ip.startsWith("::ffff:")) {
            return ip.substring(7);
        }
        return ip;
    }

    private static String firstValidFromXff(String xff) {
        if (xff == null || xff.isBlank()) {
            return null;
        }
        for (String part : xff.split(",")) {
            String ip = normalizeIp(part.trim());
            if (isValidIp(ip)) {
                return ip;
            }
        }
        return null;
    }

    private static String parseForwardedFor(String forwarded) {
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }
        for (String element : forwarded.split(",")) {
            for (String param : element.split(";")) {
                String trimmed = param.trim();
                if (trimmed.toLowerCase().startsWith("for=")) {
                    String value = trimmed.substring(4).trim();
                    String ip = normalizeIp(value);
                    if (isValidIp(ip)) {
                        return ip;
                    }
                }
            }
        }
        return null;
    }
}

package by.gdev.gateway.filter;

import java.util.Objects;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import by.gdev.common.model.HeaderName;
import by.gdev.common.model.UsernameInfo;
import by.gdev.gateway.util.FilterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Order(1)
@Component
@Slf4j
@RequiredArgsConstructor
public class AddedUserInfo implements GlobalFilter {

	private final FilterUtils filterUtils;

	private UsernameInfo getUsername(HttpHeaders requestHeaders) {
		if (filterUtils.getAuthToken(requestHeaders) != null) {
			UsernameInfo u = new UsernameInfo();
			String authToken = filterUtils.getAuthToken(requestHeaders).replace("Bearer ", "");
			JSONObject jsonObj = decodeJWT(authToken);
			try {
				u.setUsername(jsonObj.getString("preferred_username"));
				u.setUuid(jsonObj.getString("sub"));
				u.setEmail(jsonObj.getString("email"));
				return u;
			} catch (Exception e) {
				log.warn(e.getMessage());
			}
		}
		return null;
	}

	private JSONObject decodeJWT(String JWTToken) {
		String[] split_string = JWTToken.split("\\.");
		String base64EncodedBody = split_string[1];
		Base64 base64Url = new Base64(true);
		String body = new String(base64Url.decode(base64EncodedBody));
		JSONObject jsonObj = new JSONObject(body);
		return jsonObj;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String jwt = filterUtils.getAuthToken(exchange.getRequest().getHeaders());
		if (Objects.nonNull(jwt)) {
			UsernameInfo u = getUsername(exchange.getRequest().getHeaders());
			exchange = filterUtils.setRequestHeader(exchange, HeaderName.USERNAME_HEADER, u.getUsername());
			exchange = filterUtils.setRequestHeader(exchange, HeaderName.UUID_USER_HEADER, u.getUuid());
			exchange = filterUtils.setRequestHeader(exchange, HeaderName.EMAIL_USER_HEADER, u.getEmail());
		}
		return chain.filter(exchange);
	}
}

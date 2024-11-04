package by.gdev.gateway.filter;

import by.gdev.common.model.HeaderName;
import by.gdev.common.model.UsernameInfo;
import by.gdev.gateway.util.FilterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
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
		Mono<Void> monoFilter = ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication)
				.flatMap(authentication -> {

					Object principal = authentication.getPrincipal();
					DefaultOidcUser user = (DefaultOidcUser) principal;

					ServerWebExchange exchangeWithHeaders = filterUtils.setRequestHeader(exchange, HeaderName.USERNAME_HEADER, user.getUserInfo().getPreferredUsername());
					exchangeWithHeaders = filterUtils.setRequestHeader(exchangeWithHeaders, HeaderName.UUID_USER_HEADER, user.getUserInfo().getSubject());
					exchangeWithHeaders = filterUtils.setRequestHeader(exchangeWithHeaders, HeaderName.EMAIL_USER_HEADER, user.getUserInfo().getEmail());

					return chain.filter(exchangeWithHeaders);
				})
				.switchIfEmpty(chain.filter(exchange));

		return  monoFilter;
	}
}

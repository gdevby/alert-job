package by.gdev.gateway.util;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import by.gdev.common.model.HeaderName;

@Component
public class FilterUtils {

	public String getAuthToken(HttpHeaders requestHeaders) {
		if (requestHeaders.get(HeaderName.AUTH_TOKEN_HEADER) != null) {
			List<String> header = requestHeaders.get(HeaderName.AUTH_TOKEN_HEADER);
			return header.stream().findFirst().get();
		} else {
			return null;
		}
	}

	public ServerWebExchange setRequestHeader(ServerWebExchange exchange, String name, String value) {
		return exchange.mutate().request(exchange.getRequest().mutate().header(name, value).build()).build();
	}
}

package by.gdev.common.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Проверка пользовательского адреса webhook.
 *
 * Адрес приходит от пользователя, а запрос по нему делает notification-модуль,
 * который стоит внутри docker-сети рядом с core, keycloak и базой. Без проверки
 * это SSRF: пользователь указал бы http://core:8017/... и заставил бы сервис
 * ходить по внутренним адресам от своего имени. Поэтому режем всё, что не
 * http/https, и все адреса, ведущие внутрь периметра.
 */
public final class WebhookUrlValidator {

	private WebhookUrlValidator() {
	}

	public static void validate(String url) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("webhook url is empty");
		}
		if (url.length() > 512) {
			throw new IllegalArgumentException("webhook url is too long");
		}

		URI uri;
		try {
			uri = new URI(url.trim());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("webhook url is malformed");
		}

		String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
		if (!scheme.equals("http") && !scheme.equals("https")) {
			throw new IllegalArgumentException("only http and https are allowed");
		}

		String host = uri.getHost();
		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("webhook url has no host");
		}

		InetAddress[] addresses;
		try {
			addresses = InetAddress.getAllByName(host);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("webhook host is not resolvable");
		}

		for (InetAddress address : addresses) {
			if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
					|| address.isSiteLocalAddress() || address.isMulticastAddress()) {
				throw new IllegalArgumentException("webhook host points to an internal address");
			}
		}
	}

	public static boolean isValid(String url) {
		try {
			validate(url);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}

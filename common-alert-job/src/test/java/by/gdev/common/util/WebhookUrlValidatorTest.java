package by.gdev.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WebhookUrlValidatorTest {

	@Test
	void publicHttpsUrlIsAllowed() {
		assertTrue(WebhookUrlValidator.isValid("https://example.com/hooks/alert-job"));
	}

	@Test
	void nonHttpSchemesAreRejected() {
		assertFalse(WebhookUrlValidator.isValid("ftp://example.com/hook"));
		assertFalse(WebhookUrlValidator.isValid("file:///etc/passwd"));
	}

	@Test
	void internalAddressesAreRejected() {
		assertFalse(WebhookUrlValidator.isValid("http://127.0.0.1:8017/api/orders"));
		assertFalse(WebhookUrlValidator.isValid("http://localhost/hook"));
		assertFalse(WebhookUrlValidator.isValid("http://10.0.0.5/hook"));
		assertFalse(WebhookUrlValidator.isValid("http://192.168.1.10/hook"));
		assertFalse(WebhookUrlValidator.isValid("http://169.254.169.254/latest/meta-data"));
	}

	@Test
	void emptyUrlIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate(""));
		assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate(null));
	}
}

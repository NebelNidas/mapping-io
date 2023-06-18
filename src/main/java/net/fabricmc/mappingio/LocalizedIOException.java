package net.fabricmc.mappingio;

import java.io.IOException;

public class LocalizedIOException extends IOException {
	public LocalizedIOException(String errorKey, Object... parameters) {
		super(I18n.translate("error." + errorKey, parameters));
	}

	public LocalizedIOException(Throwable cause, String errorKey, Object... parameters) {
		super(I18n.translate("error." + errorKey, parameters), cause);
	}
}

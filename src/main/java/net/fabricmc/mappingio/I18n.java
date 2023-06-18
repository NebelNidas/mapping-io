/*
 * Copyright (c) 2023 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.mappingio;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class I18n {
	public enum Locale {
		EN_US("en_us");

		public final String id;

		Locale(String id) {
			this.id = id;
		}
	}

	private static ResourceBundle localizedMessagesBundle;
	private static ResourceBundle fallbackMessagesBundle;

	private static ResourceBundle load(Locale locale) {
		ResourceBundle resBundle;
		String resName = String.format("i18n/%s.properties", locale.id);
		URL resUrl = I18n.class.getResource(resName);

		if (resUrl == null) {
			throw new RuntimeException("Locale resource not found: " + resName);
		}

		try (Reader reader = new InputStreamReader(resUrl.openStream(), StandardCharsets.UTF_8)) {
			resBundle = new PropertyResourceBundle(reader);
			return resBundle;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load " + resName, e);
		}
	}

	public static void setLocale(Locale locale) {
		localizedMessagesBundle = load(locale);
	}

	public static String translate(String key) {
		if (fallbackMessagesBundle == null) {
			fallbackMessagesBundle = load(Locale.EN_US);
		}

		try {
			return localizedMessagesBundle.getString(key);
		} catch (Exception e) {
			try {
				return fallbackMessagesBundle.getString(key);
			} catch (Exception e2) {
				return key;
			}
		}
	}

	public static String translate(String key, Object... parameters) {
		return String.format(translate(key), parameters);
	}

	private I18n() {
	}
}

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

public class LocalizedIOException extends IOException {
	public LocalizedIOException(String errorKey, Object... parameters) {
		super(I18n.translate("error." + errorKey, parameters));
	}

	public LocalizedIOException(Throwable cause, String errorKey, Object... parameters) {
		super(I18n.translate("error." + errorKey, parameters), cause);
	}
}

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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;

public final class TestHelper {
	public static Path getResource(String slashPrefixedResourcePath) {
		try {
			return Paths.get(TestHelper.class.getResource(slashPrefixedResourcePath).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static void writeToDir(MappingTree tree, MappingFormat format, Path dir) throws IOException {
		MappingWriter writer = MappingWriter.create(dir.resolve(format.name() + "." + format.fileExt), format);
		tree.accept(writer);
		writer.close();
	}
}
/*
 * Copyright (c) 2021 FabricMC
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

package net.fabricmc.mappingio.format.tiny;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.ColumnFileReader;
import net.fabricmc.mappingio.format.MappingFormat;

public final class Tiny2DirReader {
		public static void read(Path dir, MappingVisitor visitor) throws IOException {
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().endsWith("." + MappingFormat.TINY_2.fileExt)) {
					Tiny2Reader.read(Files.newBufferedReader(file), visitor);
				}

				return FileVisitResult.CONTINUE;
			}
		});
		visitor.visitEnd();
	}

	public static List<String> getNamespaces(Path dir, Reader reader, boolean fullCertainty) throws IOException {
		return getNamespaces(dir, new ColumnFileReader(reader, '\t'), fullCertainty);
	}

	private static List<String> getNamespaces(Path dir, ColumnFileReader reader, boolean fullCertainty) throws IOException {
		List<String> namespaces = new ArrayList<>();

		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().endsWith("." + MappingFormat.TINY_2.fileExt)) {
					if (namespaces.isEmpty()) {
						namespaces.addAll(Tiny2Reader.getNamespaces(Files.newBufferedReader(file)));
						if (!fullCertainty) {
							return FileVisitResult.TERMINATE;
						}
					} else {
						if (!Tiny2Reader.getNamespaces(Files.newBufferedReader(file)).equals(namespaces)) {
							throw new IOException("Some tiny v2 files in the supplied directory provide different namespaces!");
						}
					}
				}

				return FileVisitResult.CONTINUE;
			}
		});

		return namespaces;
	}
}

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

package net.fabricmc.mappingio.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.NopMappingVisitor;
import net.fabricmc.mappingio.format.ErrorCollector;
import net.fabricmc.mappingio.format.ErrorCollector.ParsingError;
import net.fabricmc.mappingio.format.ErrorCollector.Severity;
import net.fabricmc.mappingio.format.MappingFormat;

public class InvalidContentReadTest {
	private static final String tinyHeader = "v1	source	target\n";
	private static final String tiny2Header = "tiny	2	0	source	target\n";

	@Test
	public void tinyFile() throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;

		checkThrows(" ", format);
		checkWorks(tinyHeader, format);

		checkTinyLine(MappedElementKind.CLASS);
		checkTinyLine(MappedElementKind.FIELD);
		checkTinyLine(MappedElementKind.METHOD);
	}

	private void checkTinyLine(MappedElementKind kind) throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;
		String prefix = tinyHeader + kind.name();

		// No source/target
		checkError(prefix, format);

		// Tabs for separation
		prefix += "\t";
		checkError(prefix, format);
		prefix += "src";
		checkError(prefix, format);

		if (kind == MappedElementKind.FIELD || kind == MappedElementKind.METHOD) {
			prefix += "\t";
			checkError(prefix, format);

			prefix += kind == MappedElementKind.FIELD ? "I" : "()V";
			checkError(prefix, format);

			prefix += "\tsrc";
			checkError(prefix, format);
		}

		checkWorks(prefix += "\t", format);
		checkWorks(prefix += "dst", format);
	}

	@Test
	public void tinyV2File() throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;

		checkThrows(" ", format);
		checkWorks(tiny2Header, format);
		checkError(tiny2Header + "\t", format); // missing property key

		checkTiny2Line(MappedElementKind.CLASS);
		checkTiny2Line(MappedElementKind.FIELD);
		checkTiny2Line(MappedElementKind.METHOD);
		checkTiny2Line(MappedElementKind.METHOD_ARG);
		checkTiny2Line(MappedElementKind.METHOD_VAR);
	}

	private void checkTiny2Line(MappedElementKind kind) throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;
		String prefix = tiny2Header + "c";

		if (kind != MappedElementKind.CLASS) {
			prefix += "\tsrc\t\n\t" + (kind == MappedElementKind.FIELD ? "f" : "m");
		}

		// No source/target
		checkError(prefix, format);

		// Tabs for separation
		if (kind != MappedElementKind.CLASS) {
			checkError(prefix, format);
			prefix += "\t";
			checkError(prefix, format);

			prefix += kind == MappedElementKind.FIELD ? "I" : "()V";
			checkError(prefix, format);
		}

		prefix += "\t";
		checkError(prefix, format);
		prefix += "src";

		checkWarning(prefix, format);
		checkWorks(prefix += "\t", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		if (kind == MappedElementKind.METHOD_ARG) {
			checkWarning(prefix += "\t\tp", format);
			checkError(prefix += "\t", format);
			checkError(prefix + "\t", format);
			checkWarning(prefix + "-1", format);
			checkWarning(prefix += "0", format);
			checkWarning(prefix += "\t", format);
			checkWorks(prefix + "\t", format);
			checkWarning(prefix += "src", format);
			checkWorks(prefix += "\t", format);
			checkWorks(prefix += "dst", format);
		} else if (kind == MappedElementKind.METHOD_VAR) {
			checkWarning(prefix += "\t\tv", format);
			checkError(prefix += "\t", format);
			checkError(prefix + "\t", format);
			checkWarning(prefix + "-1", format);
			checkWarning(prefix += "0", format);
			checkError(prefix += "\t", format);
			checkWarning(prefix + "-1", format);
			checkWarning(prefix += "0", format);
			checkWorks(prefix + "\t-1\t\t", format);
			checkError(prefix += "\t", format);
			checkWarning(prefix + "-2\t\t", format);
			checkWarning(prefix += "-1", format);
			checkWarning(prefix += "\t", format);
			checkWorks(prefix + "\t", format);
			checkWarning(prefix += "src", format);
			checkWorks(prefix += "\t", format);
			checkWorks(prefix += "dst", format);
		}
	}

	@Test
	public void enigmaFile() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_FILE;
		String prefix = "CLASS";

		checkError(prefix, format);
		checkError(prefix += " ", format);
		checkWorks(prefix += "src", format);
		checkWorks(prefix += "\n", format);
		checkError(prefix += "\tMETHOD", format);

		checkError(prefix + " ", format);
		checkWarning(prefix += " src", format);
		checkWarning(prefix + " ", format);
		checkError(prefix += " ()V\n\t\tARG", format);

		checkError(prefix += " ", format);
		checkError(prefix + " ", format);
		checkError(prefix + "src", format);
		checkWorks(prefix += "0", format);
		checkWarning(prefix += " ", format);
	}

	@Test
	public void srgFile() throws Exception {
		MappingFormat format = MappingFormat.SRG_FILE;

		checkSrgLine(MappedElementKind.CLASS, format);
		checkSrgLine(MappedElementKind.FIELD, format);
		checkSrgLine(MappedElementKind.METHOD, format);
	}

	@Test
	public void xsrgFile() throws Exception {
		MappingFormat format = MappingFormat.XSRG_FILE;

		checkSrgLine(MappedElementKind.CLASS, format);
		checkSrgLine(MappedElementKind.FIELD, format);
		checkSrgLine(MappedElementKind.METHOD, format);
	}

	private void checkSrgLine(MappedElementKind kind, MappingFormat format) throws Exception {
		String prefix;

		if (kind == MappedElementKind.CLASS) {
			prefix = "CL:";
		} else {
			prefix = (kind == MappedElementKind.FIELD ? "FD:" : "MD:");
		}

		// No source/target
		checkError(prefix, format);

		// Spaces for separation
		prefix += " ";
		checkError(prefix, format);
		prefix += "src";
		check(prefix, format, true, kind == MappedElementKind.CLASS ? Severity.WARNING : Severity.ERROR);
		String suffix = "";

		if (kind != MappedElementKind.CLASS) {
			prefix += "/";
			checkError(prefix, format);

			prefix += "src";
			checkWarning(prefix, format);

			if (kind == MappedElementKind.METHOD || format == MappingFormat.XSRG_FILE) {
				prefix += " ";
				checkWarning(prefix, format);

				prefix += kind == MappedElementKind.FIELD ? "I" : "()V";
				checkWarning(prefix, format);
			}

			prefix += " dst/";
			checkWarning(prefix, format);

			if (kind == MappedElementKind.METHOD) {
				suffix += " ()V";
			} else if (format == MappingFormat.XSRG_FILE) {
				suffix += " I";
			}
		} else {
			prefix += " ";
		}

		checkWarning(prefix + "" + suffix, format);
		checkWorks(prefix + "dst" + suffix, format);
	}

	private void checkThrows(String fileContent, MappingFormat format) throws Exception {
		assertThrows(IOException.class, () -> checkWorks(fileContent, format));
	}

	private void check(String fileContent, MappingFormat format, boolean shouldError, @Nullable Severity expectedSeverity) throws Exception {
		if (!shouldError) {
			checkWorks(fileContent, format);
		} else {
			checkSeverity(fileContent, format, expectedSeverity);
		}
	}

	private void checkWorks(String fileContent, MappingFormat format) throws Exception {
		checkSeverity(fileContent, format, null);
	}

	private void checkInfo(String fileContent, MappingFormat format) throws Exception {
		checkSeverity(fileContent, format, Severity.INFO);
	}

	private void checkWarning(String fileContent, MappingFormat format) throws Exception {
		checkSeverity(fileContent, format, Severity.WARNING);
	}

	private void checkError(String fileContent, MappingFormat format) throws Exception {
		checkSeverity(fileContent, format, Severity.ERROR);
	}

	private void checkSeverity(String fileContent, MappingFormat format, @Nullable Severity expectedSeverity) throws Exception {
		List<ParsingError> errors = read(fileContent, format);

		if (expectedSeverity != null) {
			assertFalse(errors.isEmpty(), "Expected error not found");
			assertEquals(expectedSeverity, errors.get(0).getSeverity(), "Wrong error severity");
		} else {
			assertEquals(0, errors.size(), "Unexpected error found");
		}
	}

	private List<ParsingError> read(String fileContent, MappingFormat format) throws Exception {
		ErrorCollector errorCollector = ErrorCollector.create();
		MappingReader.read(new StringReader(fileContent), format, new NopMappingVisitor(true), errorCollector);
		return errorCollector.getErrors();
	}
}

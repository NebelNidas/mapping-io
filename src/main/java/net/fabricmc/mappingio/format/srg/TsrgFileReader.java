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

package net.fabricmc.mappingio.format.srg;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.ColumnFileReader;
import net.fabricmc.mappingio.format.ErrorSink;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.ParsingError.Severity;

/**
 * {@linkplain MappingFormat#CSRG_FILE CSRG file},
 * {@linkplain MappingFormat#TSRG_FILE TSRG file} and
 * {@linkplain MappingFormat#TSRG_2_FILE TSRG2 file} reader.
 *
 * <p>Crashes if a second visit pass is requested without
 * {@link MappingFlag#NEEDS_MULTIPLE_PASSES} having been passed beforehand.
 */
public final class TsrgFileReader {
	private TsrgFileReader() {
	}

	public static List<String> getNamespaces(Reader reader) throws IOException {
		return getNamespaces(new ColumnFileReader(reader, '\t', ' '));
	}

	private static List<String> getNamespaces(ColumnFileReader reader) throws IOException {
		if (reader.nextCol("tsrg2")) { // tsrg2 magic
			List<String> ret = new ArrayList<>();
			String ns;

			while ((ns = reader.nextCol()) != null) {
				ret.add(ns);
			}

			return ret;
		} else { // assume tsrg1
			return Arrays.asList(MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK);
		}
	}

	@Deprecated
	public static void read(Reader reader, MappingVisitor visitor) throws IOException {
		read(reader, MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor);
	}

	public static void read(Reader reader, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		read(new ColumnFileReader(reader, '\t', ' '), MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor, errorSink);
	}

	@Deprecated
	public static void read(Reader reader, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
		read(new ColumnFileReader(reader, '\t', ' '), sourceNs, targetNs, visitor, ErrorSink.throwingOnSeverity(Severity.WARNING));
	}

	public static void read(ColumnFileReader reader, String sourceNs, String targetNs, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		MappingFormat format = reader.nextCol("tsrg2") ? format = MappingFormat.TSRG_2_FILE : MappingFormat.TSRG_FILE;
		String srcNamespace;
		List<String> dstNamespaces;

		if (format == MappingFormat.TSRG_2_FILE) {
			srcNamespace = reader.nextCol();
			dstNamespaces = new ArrayList<>();
			String dstNamespace;

			while ((dstNamespace = reader.nextCol()) != null) {
				dstNamespaces.add(dstNamespace);
			}

			reader.nextLine(0);
		} else {
			srcNamespace = sourceNs;
			dstNamespaces = Collections.singletonList(targetNs);
		}

		if (visitor.getFlags().contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			reader.mark();
		}

		int dstNsCount = dstNamespaces.size();
		List<String> nameTmp = dstNamespaces.size() > 1 ? new ArrayList<>(dstNamespaces.size() - 1) : null;

		for (;;) {
			if (visitor.visitHeader()) {
				visitor.visitNamespaces(srcNamespace, dstNamespaces);
			}

			if (visitor.visitContent()) {
				String lastClass = null;
				boolean visitLastClass = false; // Only used for CSRG

				do {
					if (reader.hasExtraIndents()) continue;
					reader.mark();
					String line = reader.nextCols(false);

					if ((line == null || line.isEmpty()) && reader.isAtEof()) {
						reader.discardMark();
						continue;
					}

					reader.reset();
					reader.discardMark();
					String[] parts = line.split("((?<= )|(?= ))"); // Split on spaces, but keep them

					if (format != MappingFormat.TSRG_2_FILE && parts.length >= 4 && !parts[3].startsWith("#")) { // CSRG
						format = MappingFormat.CSRG_FILE;
						String clsName = parts[0];

						if (clsName.isEmpty()) {
							errorSink.addError("missing class-name-a in line "+reader.getLineNumber());
							continue;
						}

						if (!clsName.equals(lastClass)) {
							lastClass = clsName;
							visitLastClass = visitor.visitClass(clsName) && visitor.visitElementContent(MappedElementKind.CLASS);
						}

						if (!visitLastClass) continue;
						String dstName;

						if (parts.length >= 6 && !parts[5].startsWith("#")) { // method
							dstName = parts.length == 6 ? null : parts[6];

							if (dstName == null || dstName.isEmpty() || dstName.startsWith("#")) {
								errorSink.addWarning("missing method-name-b in line "+reader.getLineNumber());
								dstName = null;
							}

							if (visitor.visitMethod(parts[2], parts[4])) {
								visitor.visitDstName(MappedElementKind.METHOD, 0, dstName);
							}

							continue;
						} else if (parts.length >= 4) { // field
							dstName = parts.length == 4 ? null : parts[4];

							if (dstName == null || dstName.isEmpty() || dstName.startsWith("#")) {
								errorSink.addError("missing field-name-b in line "+reader.getLineNumber());
								dstName = null;
							}

							if (visitor.visitField(parts[2], null)) {
								visitor.visitDstName(MappedElementKind.FIELD, 0, dstName);
							}

							continue;
						}

						errorSink.addError("invalid CSRG line: "+line);
						continue;
					}

					String srcName = reader.nextCol();
					if (srcName == null && reader.isAtEof()) continue;

					if (srcName == null || srcName.isEmpty()) {
						errorSink.addError("missing class-/package-name-a in line "+reader.getLineNumber());
						continue;
					} else if (srcName.endsWith("/")) {
						errorSink.addWarning("encountered package mapping in line "+reader.getLineNumber()+" which isn't supported yet, ignoring");
						continue;
					}

					if (!srcName.equals(lastClass)) {
						lastClass = srcName;
						visitLastClass = visitor.visitClass(srcName);

						if (visitLastClass) {
							visitLastClass = readClass(reader, format == MappingFormat.TSRG_2_FILE, dstNsCount, nameTmp, visitor, errorSink);
						}
					}
				} while (reader.nextLine(0));
			}

			if (visitor.visitEnd()) break;

			int markIdx = reader.reset();
			assert markIdx == 1;
		}
	}

	private static boolean readClass(ColumnFileReader reader, boolean isTsrg2, int dstNsCount, List<String> nameTmp, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		readDstNames(reader, MappedElementKind.CLASS, 0, dstNsCount, visitor, errorSink);
		if (!visitor.visitElementContent(MappedElementKind.CLASS)) return false;

		lineLoop: while (reader.nextLine(1)) {
			if (reader.hasExtraIndents()) continue;

			String srcName = reader.nextCol();

			if (srcName == null || srcName.isEmpty()) {
				errorSink.addError("missing name-a in line "+reader.getLineNumber());
				continue;
			}

			String arg = reader.nextCol();

			if (arg == null) {
				errorSink.addError("missing desc/name-b in line "+reader.getLineNumber() + ", skipping element due to ambiguity regarding its kind");
				continue;
			}

			if (arg.startsWith("(")) { // method: <nameA> <descA> <names>...
				if (visitor.visitMethod(srcName, arg)) {
					readMethod(reader, dstNsCount, visitor, errorSink);
				}
			} else if (!isTsrg2) { // tsrg1 field, never has a desc: <nameA> <names>...
				if (visitor.visitField(srcName, null)) {
					if (arg.isEmpty()) {
						errorSink.addWarning("missing field-name-b in line "+reader.getLineNumber());
					} else {
						visitor.visitDstName(MappedElementKind.FIELD, 0, arg);
					}

					readElement(reader, MappedElementKind.FIELD, 1, dstNsCount, visitor, errorSink);
				}
			} else { // tsrg2 field, may have desc
				for (int i = 0; i < dstNsCount - 1; i++) {
					String name = reader.nextCol();

					if (name == null) {
						errorSink.addError("missing name columns in line "+reader.getLineNumber());
						continue lineLoop;
					} else if (name.isEmpty()) {
						errorSink.addWarning("missing destination name in line "+reader.getLineNumber());
					}

					nameTmp.add(name);
				}

				String lastName = reader.nextCol();
				int offset;
				String desc;

				if (lastName == null) { // no desc, arg is first dst name, nameTmp starts with 2nd dst name: <nameA> <names>...
					offset = 1;
					desc = null;
				} else { // arg is desc, nameTmp starts with 1st dst name: <nameA> <descA> <names>...
					offset = 0;
					desc = arg;

					if (desc.isEmpty()) {
						errorSink.addWarning("empty field desc in line "+reader.getLineNumber());
						desc = null;
					}
				}

				if (visitor.visitField(srcName, desc)) {
					// first name without desc
					if (lastName == null && !arg.isEmpty()) visitor.visitDstName(MappedElementKind.FIELD, 0, arg);

					// middle names
					for (int i = 0; i < dstNsCount - 1; i++) {
						String name = nameTmp.get(i);
						if (!name.isEmpty()) visitor.visitDstName(MappedElementKind.FIELD, i + offset, name);
					}

					// last name with desc
					if (lastName != null && !lastName.isEmpty()) visitor.visitDstName(MappedElementKind.FIELD, dstNsCount - 1, lastName);

					visitor.visitElementContent(MappedElementKind.FIELD);
				}

				if (nameTmp != null) nameTmp.clear();
			}
		}

		return true;
	}

	private static void readMethod(ColumnFileReader reader, int dstNsCount, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		readDstNames(reader, MappedElementKind.METHOD, 0, dstNsCount, visitor, errorSink);
		if (!visitor.visitElementContent(MappedElementKind.METHOD)) return;

		while (reader.nextLine(2)) {
			if (reader.hasExtraIndents()) continue;

			if (reader.nextCol("static")) {
				// method is static
			} else {
				int lvIndex = -1;

				try {
					lvIndex = reader.nextIntCol(false);
				} catch (NumberFormatException e) {
					// lvIndex remains -1, handled below
				}

				if (lvIndex < 0) {
					errorSink.addWarning("missing/invalid parameter lv-index in line "+reader.getLineNumber());
					lvIndex = -1;
				}

				String srcName = reader.nextCol();

				if (srcName == null) {
					errorSink.addWarning("missing var-name-a column in line "+reader.getLineNumber());
				}

				if (srcName.isEmpty()) srcName = null;

				if (visitor.visitMethodArg(-1, lvIndex, srcName)) {
					readElement(reader, MappedElementKind.METHOD_ARG, 0, dstNsCount, visitor, errorSink);
				}
			}
		}
	}

	private static void readElement(ColumnFileReader reader, MappedElementKind kind, int dstNsOffset, int dstNsCount, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		readDstNames(reader, kind, dstNsOffset, dstNsCount, visitor, errorSink);
		visitor.visitElementContent(kind);
	}

	private static void readDstNames(ColumnFileReader reader, MappedElementKind subjectKind, int dstNsOffset, int dstNsCount, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		for (int dstNs = dstNsOffset; dstNs < dstNsCount; dstNs++) {
			String name = reader.nextCol();

			if (name == null) {
				errorSink.addError("missing name columns in line "+reader.getLineNumber());
				break;
			}

			if (name.isEmpty()) {
				errorSink.addWarning("missing destination name in line "+reader.getLineNumber());
				continue;
			}

			visitor.visitDstName(subjectKind, dstNs, name);
		}
	}
}

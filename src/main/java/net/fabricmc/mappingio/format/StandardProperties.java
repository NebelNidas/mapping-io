package net.fabricmc.mappingio.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;

public final class StandardProperties {
	private StandardProperties() {
	}

	public static Set<StandardProperty> values() {
		return Collections.unmodifiableSet(values);
	}

	public static StandardProperty getByName(String name) {
		return valuesByName.get(name);
	}

	@ApiStatus.Internal
	public static StandardProperty getById(String id) {
		return valuesById.get(id);
	}

	public static final StandardProperty NEXT_INTERMEDIARY_CLASS;
	public static final StandardProperty NEXT_INTERMEDIARY_FIELD;
	public static final StandardProperty NEXT_INTERMEDIARY_METHOD;
	public static final StandardProperty NEXT_INTERMEDIARY_COMPONENT;
	public static final StandardProperty MISSING_LVT_INDICES;
	public static final StandardProperty ESCAPED_NAMES;
	private static final Set<StandardProperty> values = new HashSet<>();
	private static final Map<String, StandardProperty> valuesByName = new HashMap<>();
	private static final Map<String, StandardProperty> valuesById = new HashMap<>();

	static {
		NEXT_INTERMEDIARY_CLASS = register(
				"next-intermediary-class",
				new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER class");
					put(MappingFormat.TINY_2_FILE, "next-intermediary-class");
				}});
		NEXT_INTERMEDIARY_FIELD = register(
				"next-intermediary-field",
				new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER field");
					put(MappingFormat.TINY_2_FILE, "next-intermediary-field");
				}});
		NEXT_INTERMEDIARY_METHOD = register(
				"next-intermediary-method",
				new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER method");
					put(MappingFormat.TINY_2_FILE, "next-intermediary-method");
				}});
		NEXT_INTERMEDIARY_COMPONENT = register(
				"next-intermediary-component",
				new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER component");
					put(MappingFormat.TINY_2_FILE, "next-intermediary-component");
				}});
		MISSING_LVT_INDICES = register(
				"missing-lvt-indices",
				new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_2_FILE, "missing-lvt-indices");
				}});
		ESCAPED_NAMES = register(
				"escaped-names",
				new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_2_FILE, "escaped-names");
				}});
	}

	private static StandardProperty register(String id, Map<MappingFormat, String> nameByFormat) {
		StandardProperty ret = new StandardPropertyImpl(id, nameByFormat);
		values.add(ret);
		valuesById.put(id, ret);

		for (String name : nameByFormat.values()) {
			valuesByName.putIfAbsent(name, ret);
		}

		return ret;
	}

	private static class StandardPropertyImpl implements StandardProperty {
		StandardPropertyImpl(String id, Map<MappingFormat, String> nameByFormat) {
			this.id = id;
			this.nameByFormat = nameByFormat;
		}

		@Override
		public Set<MappingFormat> getApplicableFormats() {
			return nameByFormat.keySet();
		}

		@Override
		public boolean isApplicableTo(MappingFormat format) {
			return nameByFormat.containsKey(format);
		}

		@Override
		public String getNameFor(MappingFormat format) {
			return nameByFormat.get(format);
		}

		@Override
		public String getId() {
			return id;
		}

		private final String id;
		private final Map<MappingFormat, String> nameByFormat;
	}
}
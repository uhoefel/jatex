package eu.hoefel.jatex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Record for describing a LaTeX preamble entry that may or may not be
 * updateable.
 * 
 * @param cmd        the command, e.g. "\\pgfplotsset"
 * @param options    the options as key-value pairs. Use null or an empty string
 *                   for the value if you have an option that does not need a
 *                   value
 * @param standalone true if this preamble entry should not be changed even if
 *                   the command would have a subsequent change in a list of
 *                   preamble entries given to {@link #cleanup(List)}, false if
 *                   the line may be updated
 * 
 * @author Udo Hoefel
 */
public record LatexPreambleEntry(String cmd, Map<String, String> options, boolean standalone) {

	/**
	 * Creates the information for a LaTeX preamble entry. No options are used and
	 * the entry will be a standalone entry.
	 * 
	 * @param cmd the command, e.g. "\\pgfplotsset"
	 */
	public LatexPreambleEntry(String cmd) {
		this(cmd, true);
	}

	/**
	 * Creates the information for a LaTeX preamble entry. The entry will be a
	 * standalone entry.
	 * 
	 * @param cmd    the command, e.g. "\\pgfplotsset"
	 * @param option the command option. If you need a key-value option use
	 *               {@link #LatexPreambleEntry(String, Map)}.
	 */
	public LatexPreambleEntry(String cmd, String option) {
		this(cmd, Map.of(option, ""), true);
	}

	/**
	 * Creates the information for a LaTeX preamble entry. The entry will be a
	 * standalone entry.
	 * 
	 * @param cmd     the command, e.g. "\\pgfplotsset"
	 * @param options the options as key-value pairs. Use null or an empty string
	 *                for the value if you have an option that does not need a value
	 */
	public LatexPreambleEntry(String cmd, Map<String, String> options) {
		this(cmd, options, true);
	}

	/**
	 * Creates the information for a LaTeX preamble entry. No options are used.
	 * 
	 * @param cmd        the command, e.g. "\\pgfplotsset"
	 * @param standalone true if this preamble entry should not be changed even if
	 *                   the command would have a subsequent change in a list of
	 *                   preamble entries given to {@link #cleanup(List)}, false if
	 *                   the line may be updated
	 */
	public LatexPreambleEntry(String cmd, boolean standalone) {
		this(cmd, Map.of(), standalone);
	}

	/**
	 * Creates the information for a LaTeX preamble entry.
	 * 
	 * @param cmd        the command, e.g. "\\pgfplotsset"
	 * @param options    the options as key-value pairs. Use null or an empty string
	 *                   for the value if you have an option that does not need a
	 *                   value
	 * @param standalone true if this preamble entry should not be changed even if
	 *                   the command would have a subsequent change in a list of
	 *                   preamble entries given to {@link #cleanup(List)}, false if
	 *                   the line may be updated
	 */
	public LatexPreambleEntry(String cmd, Map<String, String> options, boolean standalone) {
		Objects.nonNull(cmd);
		this.cmd = cmd;
		if (options == null || options.isEmpty()) {
			this.options = Collections.emptyNavigableMap();
		} else {
			this.options = Map.copyOf(options);
		}
		this.standalone = standalone;
	}

	/**
	 * Gets the formatted preamble line, including all the options appropriately put
	 * into the typical LaTeX format ("{a=b,c,d=e}").
	 * 
	 * @return the formatted preamble line
	 */
	public String preambleLine() {
		StringBuilder sb = new StringBuilder(cmd);
		if (!cmd.startsWith("%") && !cmd.isBlank() && !options.isEmpty()) {
			sb.append("{");
			for (Entry<String, String> option : options.entrySet()) {
				String key = option.getKey();
				if (key != null && !key.isBlank()) {
					sb.append(key);
					String value = option.getValue();
					if (Latex.STRING_IS_NOT_BLANK.test(value)) {
						sb.append("=");
						sb.append(value);
					}
					sb.append(",");
				}
			}
			sb.append("}");
		}
		return sb.toString();
	}

	/**
	 * Clean up the list of preamble entries by merging non-standalone duplicates.
	 * 
	 * @param entries a list of preamble entries with potential non-standalone
	 *                duplicates
	 * @return the list of preamble entries with no non-standalone duplicates.
	 */
	public static final List<LatexPreambleEntry> cleanup(List<LatexPreambleEntry> entries) {
		List<LatexPreambleEntry> cleanedEntries = new ArrayList<>();
		for (LatexPreambleEntry entry : entries) {
			if (entry.standalone()) {
				cleanedEntries.add(entry);
			} else {
				// go through all entries and try to find one that matches and is not standalone
				cleanedEntries.stream()
								.filter(e -> e.cmd().equals(entry.cmd()) && !e.standalone())
								.findFirst()
								.ifPresentOrElse(updateEntry(cleanedEntries, entry), () -> cleanedEntries.add(entry));
			}
		}
		return cleanedEntries;
	}

	/**
	 * Updates the specified entry in the list of entries.
	 * 
	 * @param entries         the list of entries
	 * @param additionalEntry the entry to update
	 * @return the consumer that updates the specified package in given list of
	 *         packages
	 */
	private static final Consumer<LatexPreambleEntry> updateEntry(List<LatexPreambleEntry> entries, LatexPreambleEntry additionalEntry) {
		return e -> {
			Map<String, String> m = new HashMap<>();
			m.putAll(e.options());
			m.putAll(additionalEntry.options());
			entries.set(entries.indexOf(e), new LatexPreambleEntry(e.cmd(), Map.copyOf(m), e.standalone()));
		};
	}
}

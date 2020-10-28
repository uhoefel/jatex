package eu.hoefel.jatex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class for handling tables to be included in {@link Latex} documents.
 * 
 * @author Udo Hoefel
 */
public final class Table implements Texable {

	/** The namespace used for tables in labels. */
	public static final String LABEL_NAMESPACE = "tab:";

	private String format;
	private int numCols = 0;
	private int numRows = 0;
	private int definedNumCols;
	private boolean useTable;
	private String label = null;
	private String caption = null;
	private String captionShort = null;
	private TableEnvironment env;
	private Map<Integer, Boolean> midRuleExtra = new HashMap<>();
	private int endHead = -1;
	private boolean centering;
	private String position = null;

	private Map<Integer, Map<Integer, String>> entry = new HashMap<>();
	private Map<Integer, Map<Integer, Boolean>> active = new HashMap<>();
	private Map<Integer, Map<Integer, Boolean>> useAmpersand = new HashMap<>();
	private Map<Integer, Map<Integer, Integer>> width = new HashMap<>();
	private Map<Integer, Map<Integer, String>> color = new HashMap<>();

	private List<LatexPackage> packages = new ArrayList<>();
	private List<LatexPreambleEntry> preambleExtras = new ArrayList<>();
	
	/**
	 * The available table environments.
	 * 
	 * @author Udo Hoefel
	 */
	public enum TableEnvironment {

		/**
		 * Used for normal tables (shorter than one page, otherwise use
		 * {@link #LONGTABLE}).
		 */
		TABULAR,

		/**
		 * When you have a table that spans more than one page, the longtable
		 * environment is the right choice. It allows you to specify the column headings
		 * such that it prints them on each page (see {@link Table#endHead(int)}).
		 */
		LONGTABLE
	}

	/** Constructor that uses some defaults (needed packages, environment etc.). */
	public Table() {
		centering();
		usePackages(new LatexPackage("booktabs"), new LatexPackage("longtable"), new LatexPackage("caption"), new LatexPackage("multirow"));
		usePackageWithOptions("xcolor", Map.of("table", ""));
		floating(true);
		environment(TableEnvironment.TABULAR);
	}

	/**
	 * Allows to choose the environment to use.
	 * 
	 * @param env the environment to use
	 * @see TableEnvironment
	 * @return the Table object
	 */
	public Table environment(TableEnvironment env) {
		this.env = env;
		return this;
	}

	/**
	 * Gets the environment used for this Table object.
	 * 
	 * @return the (main) environment for this Table
	 */
	public TableEnvironment getEnvironment() {
		return env;
	}

	/**
	 * Specify (floating) table positioning options. Allowed inputs:
	 * <ul>
	 * <li>! - really try to place figure where you specify
	 * <li>h - try to place the figure "here"
	 * <li>t - try to place the figure at the top
	 * <li>b - try to place the figure at the bottom
	 * <li>p - try to place the figure at a separate page
	 * </ul>
	 * 
	 * @param position the position
	 * @return this Table object
	 */
	public Table position(String position) {
		if (!"!".equals(position) 
				&& !"h".equals(position) 
				&& !"t".equals(position) 
				&& !"b".equals(position)
				&& !"p".equals(position)) {
			throw new IllegalArgumentException("Unknown position argument for table");
		}
		this.position = position;
		return this;
	}

	/** Calculates the requested number of rows and columns. */
	private void calculateNumRowsCols() {
		numCols = 0;
		numRows = 0;
		for (var col : active.entrySet()) {
			for (var row : col.getValue().entrySet()) {
				if (Boolean.TRUE.equals(row.getValue())) {
					numCols = Math.max(numCols, col.getKey());
					numRows = Math.max(numRows, row.getKey());
				}
			}
		}

		// we add 1 because we start indexing at 0
		numCols++;
		numRows++;

		if (numCols > definedNumCols) {
			throw new IllegalArgumentException("More columns requested than defined: " + numCols + " > " + definedNumCols);
		}

		// fill the remaining cells
		for (int i = 0; i < numCols; i++) {
			boolean val = (i != numCols - 1);
			for (int j = 0; j < numRows; j++) {
				Map<Integer, Boolean> innerMap = useAmpersand.getOrDefault(i, new HashMap<>());
				innerMap.putIfAbsent(j, val);
				useAmpersand.put(i, innerMap);
			}
		}

		// calculate the correct width
		int colorCommandLength = "{\\cellcolor{}}".length();
		for (int i = 0; i < numCols; i++) {
			if (entry.containsKey(i)) {
				int max = 0;
				for (Entry<Integer, String> w : entry.get(i).entrySet()) {
					int colorlength = 0;
					if (color.containsKey(i) && color.get(i).containsKey(w.getKey())) {
						colorlength = colorCommandLength + color.get(i).get(w.getKey()).length();
					}
					max = Math.max(max, w.getValue().length() + colorlength);
				}
				for (int j = 0; j < numRows; j++) {
					int w = 0;
					if (entry.get(i).containsKey(j)) {
						w += entry.get(i).get(j).length();
					}
					if (color.containsKey(i) && color.get(i).containsKey(j)) {
						w += colorCommandLength + color.get(i).get(j).length();
					}
					Map<Integer, Integer> innerMap = width.getOrDefault(i, new HashMap<>());
					innerMap.put(j, max - w);
					width.put(i, innerMap);
				}
			} else {
				for (int j = 0; j < numRows; j++) {
					Map<Integer, Integer> innerMap = width.getOrDefault(i, new HashMap<>());
					innerMap.put(j, 0);
					width.put(i, innerMap);
				}
			}
		}
	}

	/**
	 * Sets a table entry. The indices start at 0.
	 * 
	 * @param col   the column
	 * @param row   the row
	 * @param entry the String to be written to that cell
	 * @return this Table object
	 */
	public Table entry(int col, int row, String entry) {
		this.entry.computeIfAbsent(col, HashMap::new).put(row, entry);
		activateCells(col, row, entry);
		return this;
	}

	/**
	 * Sets a table entry color.
	 * 
	 * @param col   the column
	 * @param row   the row
	 * @param color the color of the cell
	 * @return the Table object
	 */
	public Table color(int col, int row, String color) {
		this.color.computeIfAbsent(col, HashMap::new).put(row, color);
		activateCells(col, row, color);
		return this;
	}

	/**
	 * Activates the cell appropriate cells. Should be able to handle
	 * multirow/multicolumn commands in the String.
	 * 
	 * @param col the column
	 * @param row the row
	 * @param str the String to check
	 */
	private void activateCells(int col, int row, String str) {
		// this is the obvious part...
		active.computeIfAbsent(col, HashMap::new).put(row, true);

		// here we look for multirow/-column
		String multirow = "\\multirow{";
		if (str.contains(multirow)) {
			int start = str.indexOf(multirow) + multirow.length();
			int end = str.indexOf('}', start);
			int addrows = Integer.parseInt(str.substring(start, end));
	
			for (int i = 0; i < addrows; i++) {
				active.computeIfAbsent(col, HashMap::new).put(row + i, true);
				useAmpersand.computeIfAbsent(col, HashMap::new).put(row + i, true);
			}
		}

		String multicolumn = "\\multicolumn{";
		if (str.contains(multicolumn)) {
			int start = str.indexOf(multicolumn) + multicolumn.length();
			int end = str.indexOf('}', start);
			int addcols = Integer.parseInt(str.substring(start, end));
	
			for (int i = 0; i < addcols; i++) {
				active.computeIfAbsent(col + i, HashMap::new).put(row, true);
				useAmpersand.computeIfAbsent(col + i, HashMap::new).put(row, false);
			}
		}
	}

	/**
	 * Sets multiple column values for a specific column.
	 * 
	 * @param row   the row for which you want to specify multiple column entries
	 * @param cells the cell entries, each separate String is for another column
	 * @return this Table object
	 */
	public Table row(int row, String... cells) {
		for (int i = 0; i < cells.length; i++) {
			entry(i, row, cells[i]);
		}
		return this;
	}

	/**
	 * Sets multiple row values for a specific column.
	 * 
	 * @param col   the column for which you want to specify multiple row entries
	 * @param cells the cell entries, each separate String is for another row
	 * @return this Table object
	 */
	public Table column(int col, String... cells) {
		for (int i = 0; i < cells.length; i++) {
			entry(col, i, cells[i]);
		}
		return this;
	}

	/**
	 * Sets multiple row values for a specific column starting at the specified row.
	 * 
	 * @param col   the column for which you want to specify multiple row entries
	 * @param row   the row to start the column entries
	 * @param cells the cell entries, each separate String is for another row
	 * @return this Table object
	 */
	public Table column(int col, int row, String... cells) {
		for (int i = 0; i < cells.length; i++) {
			entry(col, row + i, cells[i]);
		}
		return this;
	}

	/**
	 * Gets whether this table is centered.
	 * 
	 * @return true if centered
	 */
	public boolean isCentering() {
		return centering;
	}

	/**
	 * Sets whether this table should be centered.
	 * 
	 * @param centering true if you want the table centered
	 * @return this Table object
	 */
	public Table centering(boolean centering) {
		this.centering = centering;
		return this;
	}

	/**
	 * Centers the table.
	 * 
	 * @return the Table object
	 */
	public Table centering() {
		return centering(true);
	}

	/**
	 * Sets a midrule after the given row.
	 * 
	 * @param row the row
	 * @return the Table object
	 */
	public Table midrule(int row) {
		midRuleExtra.put(row, true);
		return this;
	}

	/**
	 * Ends the head after the given row. Requires {@link TableEnvironment#LONGTABLE} (can be set
	 * via {@link Table#environment(TableEnvironment)}).
	 * 
	 * @param row the row
	 * @return the Table object
	 */
	public Table endHead(int row) {
		endHead = row;
		return this;
	}

	/**
	 * Sets the formats to be used for the table, for example "l","c","c","c" for 4
	 * columns (the first left aligned, and 3 centered). The column descriptors
	 * <em>need</em> to be put in separately for each column.
	 * 
	 * @param formats the table format
	 * @return the Table object
	 */
	public Table format(String... formats) {
		String[] formatsTrimmed = new String[formats.length];
		for (int i = 0; i < formatsTrimmed.length; i++) {
			formatsTrimmed[i] = formats[i].trim();
		}
		format = String.join(" ", formatsTrimmed);
		definedNumCols = formatsTrimmed.length;
		return this;
	}

	/**
	 * Sets whether the table should float.
	 * 
	 * @param floating true if table should float, otherwise false
	 * @return the Table object
	 */
	public Table floating(boolean floating) {
		useTable = floating;
		return this;
	}

	/**
	 * True if the table is floating, otherwise false.
	 * 
	 * @return true if floating
	 */
	public boolean isFloating() {
		return useTable;
	}

	/**
	 * Gets the caption of the table.
	 * 
	 * @return the caption
	 */
	public String getCaption() {
		return caption;
	}

	/**
	 * Sets the caption of the table.
	 * 
	 * @param caption the caption
	 * @return this Table object
	 */
	public Table caption(String caption) {
		this.caption = caption;
		return this;
	}

	/**
	 * Gets the short form of the caption.
	 * 
	 * @return the short caption
	 */
	public String getCaptionShort() {
		return captionShort;
	}

	/**
	 * Sets the short form of the caption, for example for the table of tables.
	 * 
	 * @param captionShort the short form
	 * @return this Table object
	 */
	public Table captionShort(String captionShort) {
		this.captionShort = captionShort;
		return this;
	}

	/**
	 * Gets the full label for this table without the internal namespace prefix:
	 * {@value #LABEL_NAMESPACE}.
	 * 
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the label for this table. Internally added is the namespace prefix:
	 * {@value #LABEL_NAMESPACE}.
	 * 
	 * @param label the label
	 * @return this Table object
	 */
	public Table label(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Gets the LaTeX command needed for spanning rows:
	 * \multirow{''num_rows''}{''width''}{''contents''}. The arguments are pretty
	 * simple to deduce (* for the width means the content's natural width).
	 * 
	 * @param numRows the number of rows to span
	 * @param width   the width
	 * @param content the content
	 * @return the multirow command
	 */
	public static String multirow(int numRows, String width, String content) {
		return "\\multirow{%d}{%s}{%s}".formatted(numRows, width, content);
	}

	/**
	 * Gets the LaTeX command needed for spanning multiple rows, with natural width
	 * of the content used as the width.
	 * 
	 * @param numRows the number of rows to span
	 * @param content the content
	 * @return the multirow command
	 * 
	 * @see #multirow(int, String, String)
	 */
	public static String multirow(int numRows, String content) {
		return "\\multirow{%d}{*}{%s}".formatted(numRows, content);
	}

	/**
	 * Gets the LaTeX command needed for spanning multiple columns:
	 * \multicolumn{numCols}{alignment}{contents}. numCols is the number of
	 * subsequent columns to merge; alignment is either l, c, r, or to have text
	 * wrapping specify a width p{5.0cm}. And contents is simply the actual data you
	 * want to be contained within that cell.
	 * 
	 * @param numCols   the number of columns to span
	 * @param alignment the alignment for the content
	 * @param content   the content
	 * @return the multicolumn command
	 */
	public static String multicolumn(int numCols, String alignment, String content) {
		return  "\\multicolumn{%d}{%s}{%s}".formatted(numCols, alignment, content);
	}

	/**
	 * Indicates to {@link Latex} that these packages and options are needed.
	 * 
	 * @param packages the needed packages
	 * @return the Table object
	 */
	public Table usePackages(LatexPackage... packages) {
		this.packages.addAll(List.of(packages));
		return this;
	}

	/**
	 * Adds options to a package.
	 * 
	 * @param packageName the package to add the option to
	 * @param options     the options
	 * @return the Table object
	 */
	public Table usePackageWithOptions(String packageName, Map<String, String> options) {
		packages.add(new LatexPackage(packageName, options));
		return this;
	}

	@Override
	public List<LatexPackage> neededPackages() {
		return packages;
	}

	@Override
	public List<LatexPreambleEntry> preambleExtras() {
		return preambleExtras;
	}

	@Override
	public List<String> latexCode() {
		// to make sure the user didn't put garbage in
		if (!isFloating()) label(null);

		calculateNumRowsCols();

		List<String> ret = new ArrayList<>();
		int n = 1;

		if (isFloating()) {
			ret.add(Latex.indent(n) + "\\begin{table}[" + (position != null ? position : "") + "]%");
			if (centering) ret.add(Latex.indent(n) + "\\centering");
			if (caption != null) {
				ret.add((Latex.indent(n + 1) + "\\caption"
						+ (captionShort != null ? "[" + captionShort + "]" : "") + "{" + caption
						+ "}%"));
			}
			if (label != null) ret.add("%s\\label{%s%s}%%".formatted(Latex.indent(n + 1), LABEL_NAMESPACE, label));
			n++;
		} else {
			if (centering) ret.add(Latex.indent(n) + "{\\centering");
		}
		ret.add(Latex.indent(n) + "\\begin{" + env.name().toLowerCase(Locale.ENGLISH) + "}{" + format + "}\\toprule");

		for (int i = 0; i < numRows; i++) {
			StringBuilder str = new StringBuilder();
			str.append(Latex.indent(n + 1));
			for (int j = 0; j < numCols; j++) {
				
				if (color.containsKey(j) && color.get(j).containsKey(i)) {
					str.append("{\\cellcolor{" + color.get(j).get(i) + "}}");
				}

				if (entry.containsKey(j) && entry.get(j).containsKey(i)) {
					str.append(entry.get(j).get(i));
				}

				str.append(" ".repeat(width.get(j).get(i)));

				if (Boolean.TRUE.equals(useAmpersand.get(j).get(i))) {
					str.append(" & ");
				}
			}

			str.append(" \\tabularnewline" 
					+ (midRuleExtra.containsKey(i) ? "\\midrule" : "")
					+ (i == endHead && env == TableEnvironment.LONGTABLE ? "\\endhead" : "")
					+ (i == numRows - 1 ? "\\bottomrule" : ""));

			ret.add(str.toString());
		}

		ret.add(Latex.indent(n) + "\\end{" + env.name().toLowerCase(Locale.ENGLISH) + "}");

		if (isFloating()) {
			ret.add(Latex.indent(n - 1) + "\\end{table}");
		} else if (centering) {
			 ret.add(Latex.indent(n) + "}");
		}

		return ret;
	}
}

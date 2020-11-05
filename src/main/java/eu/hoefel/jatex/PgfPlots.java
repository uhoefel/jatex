package eu.hoefel.jatex;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import eu.hoefel.utils.Strings;
import eu.hoefel.utils.Types;

/**
 * Class for handling (pretty) complex pgfplots. Intended to be used in
 * combination with {@link Tikz#Tikz()}.
 * 
 * @author Udo Hoefel
 */
public class PgfPlots implements Texable {

	private Map<String, String> options = new HashMap<>();
	private List<String> lines = new ArrayList<>();
	private List<LatexPackage> packages = new ArrayList<>();
	private List<LatexPreambleEntry> preambleEntries = new ArrayList<>();
	private int estimatedNumRows = 0;

	/** Constructor that uses defaults (needed packages and settings). */
	public PgfPlots() {
		usePackages(new LatexPackage("pgfplots"));
		compat("newest");
	}

	/**
	 * Copy constructor.
	 * 
	 * @param plot the Plot to deep copy
	 */
	public PgfPlots(PgfPlots plot) {
		options = new HashMap<>(plot.options);
		lines = new ArrayList<>(plot.lines);
		packages.addAll(plot.packages);
		preambleEntries.addAll(plot.preambleEntries);
	}

	/**
	 * Adds a line of code.
	 * 
	 * @param line the line of code
	 * @return the Plot object
	 */
	public PgfPlots add(String line) {
		lines.add(line);
		return this;
	}

	/**
	 * Adds options to the axis environment.
	 * 
	 * @param options the options
	 * @return the Plot object
	 */
	public PgfPlots addOptions(Map<String, String> options) {
		this.options.putAll(options);
		return this;
	}

	/**
	 * Activates the major grid.
	 * 
	 * @return the Plot object
	 */
	public PgfPlots grid() {
		addOptions(Map.of("grid", "major"));
		return this;
	}

	/**
	 * Adds an x axis label.
	 * 
	 * @param label the x axis label
	 * @return the Plot object
	 */
	public PgfPlots xlabel(String label) {
		addOptions(Map.of("xlabel", label));
		return this;
	}

	/**
	 * Adds a y axis label.
	 * 
	 * @param label the y axis label
	 * @return the Plot object
	 */
	public PgfPlots ylabel(String label) {
		addOptions(Map.of("ylabel", label));
		return this;
	}

	/**
	 * Adds a colorbar label.
	 * 
	 * @param label the colorbar label
	 * @return the Plot object
	 */
	public PgfPlots clabel(String label) {
		addOptions(Map.of("colorbar style", "{ylabel={%s}}".formatted(label)));
		return this;
	}

	/**
	 * Adds a title.
	 * 
	 * @param title the title
	 * @return the Plot object
	 */
	public PgfPlots title(String title) {
		addOptions(Map.of("title", "{%s}".formatted(title)));
		return this;
	}

	/**
	 * Plots data from a file, formulas or 2D/3D arrays. If you want to plot from a
	 * file but just want to save the script this will fail, as the existence of the
	 * file has to be checked.
	 * 
	 * @param <T>     the data type
	 * @param input   either the filename, the formula (e.g. cos(deg(x))*x^2) or the
	 *                data to plot
	 * @param legend  the legend entry
	 * @return the Plot object
	 */
	public <T> PgfPlots plot(T input, String legend) {
		return plot(input, legend, Map.of());
	}

	/**
	 * Plots data from a file, formulas or 2D/3D arrays. If you want to plot from a
	 * file but just want to save the script this will fail, as the existence of the
	 * file has to be checked.
	 * 
	 * @param <T>     the data type
	 * @param input   either the filename, the formula (e.g. cos(deg(x))*x^2) or the
	 *                data to plot
	 * @param legend  the legend entry
	 * @param options the options for this plot (<em>not</em> the axis environment!)
	 * @return the Plot object
	 */
	public <T> PgfPlots plot(T input, String legend, Map<String, String> options) {
		boolean hasOptions = options != null && !options.isEmpty();
		String formattedOptions = hasOptions ?  "+" + Latex.toOptions(options) + " ": "";

		if (input instanceof String s) {
			// we need to be slightly clever to find out whether the user wants to plot a file or a formula
			// this will not work if the script is just saved for future use without having the files in place
			String file = new File(s).isFile() ? " file " : " ";

			add("\\addplot " + formattedOptions + file + "{" + input + "};");
		} else if (input.getClass().isArray() && Types.dimension(input.getClass()) == 2 && Array.getLength(input) == 2) {
			add("\\addplot " + formattedOptions + "coordinates {");
			for (int i = 0; i < Array.getLength(Array.get(input, 0)); i++) {
				add(Latex.indent(1) + "(" + Array.get(Array.get(input, 0), i) + "," + Array.get(Array.get(input, 1), i) + ")");
			}
			add("};");
		} else if (input.getClass().isArray() && Types.dimension(input.getClass()) == 2 && Array.getLength(input) == 3) {
			add("\\addplot3 " + formattedOptions + "coordinates {");
			Set<Object> elems = new HashSet<>();
			for (int i = 0; i < Array.getLength(Array.get(input, 0)); i++) {
				add(Latex.indent(1) + "(" + Array.get(Array.get(input, 0), i) + "," + Array.get(Array.get(input, 1), i) + "," + Array.get(Array.get(input, 2), i) + ")");
				elems.add(Array.get(Array.get(input, 0), i));
			}
			add("};");
			estimatedNumRows = elems.size();
		} else {
			throw new IllegalArgumentException("Only 2D and 3D arrays are supported");
		}
		
		if (legend != null) add("\\addlegendentry{%s};".formatted(legend));
		return this;
	}

	/**
	 * Plots data as a contour plot.
	 * 
	 * @param x       the x values
	 * @param y       the y values
	 * @param z       the z values
	 * @param options the options for this plot (<em>not</em> the axis
	 *                environment!), use e.g. "contour filled={number=20}" to change
	 *                the number of levels
	 * @return the Plot object
	 */
	public PgfPlots contour(double[] x, double[] y, double[][] z, Map<String, String> options) {
		addOptions(Map.of("view", "{0}{90}", "colorbar", ""));

		String[] ssvs = new String[x.length * y.length];
		double[] zFlat = flatten(z);
		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < y.length; j++) {
				ssvs[i * y.length + j] = x[i] + " " + y[j] + " " + zFlat[i * y.length + j];
			}
		}

		boolean hasNumber  = Stream.of(options).anyMatch(s -> s.containsKey("contour filled") && s.get("contour filled").contains("{number="));
		boolean hasSamples = Stream.of(options).anyMatch(s -> s.containsKey("samples"));
		boolean hasShader  = Stream.of(options).anyMatch(s -> s.containsKey("shader"));
		
		StringBuilder concatenatedUserOptions = new StringBuilder();
		for (Entry<String, String> option : options.entrySet()) {
			concatenatedUserOptions.append(option.getKey() + (Latex.STRING_IS_NOT_BLANK.test(option.getValue()) ? "=" + option.getValue() : "") + ",");
		}
		
		add("\\addplot3[surf,"
				+ "mesh/rows=" + x.length + ","
				+ "mesh/cols=" + y.length + "," 
				+ concatenatedUserOptions.toString()
				+ (!hasNumber  ? "contour filled={number=7}," : "")
				+ (!hasSamples ? "samples=150," : "")
				+ (!hasShader  ? "shader=interp," : "")
				+ "] table {");
		add(Latex.indent(1) + "X Y Z");
		for (String ssv : ssvs) {
			add(Latex.indent(1) + ssv);
		}
		add("};");
		return this;
	}

	/**
	 * Flattens a 2D double array into 1D. The returned data is stored row by row
	 * from {@code a}. This method only works for rectangular arrays.
	 * 
	 * @param a the 2D array
	 * @return the flattened array
	 */
	private static final double[] flatten(double[][] a) {
		int size = a.length * a[0].length;
		double[] flat = new double[size];

		int index = 0;
		for (int i = 0; i < a.length; i++) {
			int numNew = a[i].length;
			System.arraycopy(a[i], 0, flat, index, numNew);
			index += numNew;
		}
		return flat;
	}

	/**
	 * Convenience method for plotting directly via the standalone LaTeX
	 * documentclass.
	 * 
	 * @param filepath the path to save to
	 * @return the error code, i.e. 0 if the execution terminated normally and &gt;1
	 *         if an error occurred
	 */
	public int exec(String filepath) {
		Latex tex = new Latex();
		tex.compiler(TexCompiler.LUALATEX);
		
		String folder = new File(filepath).getParent();
		String file = new File(filepath).getName();
		
		tex.folder(folder);
		tex.filename(file);
		tex.repeat(1);
		tex.clean(true);
		
		tex.documentclassWithOptions("standalone", Map.of("tikz", ""));
		
		PgfPlots plot = new PgfPlots(this);
		plot.pgfplotslibraries("colormaps","colorbrewer");
		plot.addOptions(Map.of("axis on top", "",
				"axis background/.style", "{fill=white}",
				"samples", "100",
				"legend cell align", "left"));
		
		if (estimatedNumRows != 0) {
			// try with guessed number of rows
			plot.addOptions(Map.of("mesh/cols", Integer.toString(estimatedNumRows)));
		}

		// pgfplotsset needs the arguments ordered, sigh
		// make sure we have the cycle list first
		plot.preambleEntries.add(new LatexPreambleEntry("\\pgfplotsset", Map.of("cycle list/Dark2-8", "")));
		plot.preambleEntries.add(new LatexPreambleEntry("\\pgfplotsset", Map.of("cycle multiindex* list", "{mark list*\\nextlist Dark2-8\\nextlist}",
																		"colormap/viridis", "")));
		tex.add(Tikz.of(plot));
		tex.save(true);
		
		if (tex.isExecutable()) {
			return tex.exec();
		} else {
			throw new IllegalStateException("Seems like " + tex.getCompiler() + " is not accessible from Java. "
					+ "Please make sure that it is installed and on the PATH environment variable.");
		}
	}

	/**
	 * Plots data from a file, formulas or 2D/3D arrays.
	 * 
	 * @param <T>    the data type
	 * @param input  either the filename, the formula (e.g. cos(deg(x))*x^2) or the
	 *               data to plot
	 * @param legend the legend entry
	 * @return a new Plot object
	 */
	public static final <T> PgfPlots of(T input, String legend) {
		return new PgfPlots().plot(input, legend, Map.of());
	}

	/**
	 * Plots data from a file, formulas or 2D/3D arrays.
	 * 
	 * @param <T>     the data type
	 * @param input   either the filename, the formula (e.g. cos(deg(x))*x^2) or the
	 *                data to plot
	 * @param legend  the legend entry
	 * @param options the options for this plot (<em>not</em> the axis environment!)
	 * @return a new Plot object
	 */
	public static final <T> PgfPlots of(T input, String legend, Map<String, String> options) {
		return new PgfPlots().plot(input, legend, options);
	}

	/**
	 * Plots filled contours.
	 * 
	 * @param x       the x values
	 * @param y       the y values
	 * @param z       the z values
	 * @param options the options for this plot (<em>not</em> the axis
	 *                environment!), use e.g. Map.of("contour filled",
	 *                "{number=20}") to change the number of levels
	 * @return the Plot object
	 */
	public static final PgfPlots contourOf(double[] x, double[] y, double[][] z, Map<String, String> options) {
		return new PgfPlots().contour(x, y, z, options);
	}

	/**
	 * Requests these tikzlibraries to be loaded.
	 * 
	 * @param libraries the tikzlibraries
	 * @return the Plot object
	 */
	public PgfPlots tikzlibraries(String... libraries) {
		preambleEntries.add(new LatexPreambleEntry("\\usetikzlibrary", Strings.mapOf(libraries), false));
		return this;
	}

	/**
	 * Requests these pgflibraries to be loaded.
	 * 
	 * @param libraries the pgflibraries
	 * @return the Plot object
	 */
	public PgfPlots pgflibraries(String... libraries) {
		preambleEntries.add(new LatexPreambleEntry("\\usepgflibrary", Strings.mapOf(libraries), false));
		return this;
	}

	/**
	 * Requests these pgfplotslibraries to be loaded.
	 * 
	 * @param libraries the pgfplotslibraries
	 * @return the Plot object
	 */
	public PgfPlots pgfplotslibraries(String... libraries) {
		preambleEntries.add(new LatexPreambleEntry("\\usepgfplotslibrary", Strings.mapOf(libraries), false));
		return this;
	}

	/**
	 * Requests these gdlibraries to be loaded.
	 * 
	 * @param libraries the gdlibraries
	 * @return the Plot object
	 */
	public PgfPlots gdlibraries(String... libraries) {
		preambleEntries.add(new LatexPreambleEntry("\\usegdlibrary", Strings.mapOf(libraries), false));
		return this;
	}

	/**
	 * Sets the compatibility mode, either a version like e.g. "1.15" or "newest".
	 * 
	 * @param compat the compatibility mode
	 * @return the Plot object
	 */
	public PgfPlots compat(String compat) {
		preambleEntries.add(new LatexPreambleEntry("\\pgfplotsset", Map.of("compat", compat), false));
		return this;
	}

	/**
	 * Indicates to {@link Latex} that these packages and options are needed.
	 * 
	 * @param packages the needed packages
	 * @return the Plot object
	 */
	public PgfPlots usePackages(LatexPackage... packages) {
		this.packages.addAll(List.of(packages));
		return this;
	}

	/**
	 * Adds options to a package.
	 * 
	 * @param packageName the package to add the option to
	 * @param options     the options
	 * @return the Plot object
	 */
	public PgfPlots usePackageWithOptions(String packageName, Map<String, String> options) {
		packages.add(new LatexPackage(packageName, options));
		return this;
	}

	@Override
	public List<LatexPackage> neededPackages() {
		return packages;
	}

	@Override
	public List<LatexPreambleEntry> preambleExtras() {
		return preambleEntries;
	}

	@Override
	public List<String> latexCode() {
		List<String> ret = new ArrayList<>();
		ret.add("\\begin{axis}");
		ret.add(Latex.indent(1) + "[");
		for (Entry<String, String> option : options.entrySet()) {
			ret.add(Latex.indent(2) + option.getKey() + (Latex.STRING_IS_NOT_BLANK.test(option.getValue()) ? "=" + option.getValue() : "") + ",");
		}
		ret.add(Latex.indent(1) + "]");
		ret.add("");
		for (String line : lines) {
			ret.add(Latex.indent(1) + line);
		}
		ret.add("\\end{axis}");
		return ret;
	}
}

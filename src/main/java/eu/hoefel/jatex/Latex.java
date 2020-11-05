package eu.hoefel.jatex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.hoefel.jatex.Equation.EquationEnvironment;
import eu.hoefel.jatex.Figure.FigureEnvironment;
import eu.hoefel.utils.IOs;

/**
 * Class for the construction of
 * <a href="https://www.latex-project.org/">LaTeX</a> documents and their
 * compilation.
 * 
 * @author Udo Hoefel
 */
public final class Latex {
	
	/** The namespace used for sections/chapters/etc. in labels. */
	public static final String LABEL_NAMESPACE = "sec:";

	/** Indicates a line break. */
	private static final String LINE_BREAK = "\n";

	/**
	 * This preamble entry represents an empty line (i.e., the command to write is
	 * just an empty string).
	 */
	public static final LatexPreambleEntry EMPTY_LINE = new LatexPreambleEntry("", true);

	/** This preamble entry can be used to indicate a minor separation in the tex file. */
	public static final LatexPreambleEntry MINOR_SEPARATOR = new LatexPreambleEntry("% ----------------", true);

	/** This preamble entry can be used to indicate a major separation in the tex file. */
	public static final LatexPreambleEntry MAJOR_SEPARATOR = new LatexPreambleEntry("% ================", true);

	static final Predicate<String> STRING_IS_NOT_BLANK = s -> s != null && !s.isBlank();
	private static final Logger logger = Logger.getLogger(Latex.class.getName());

	private TexCompiler compiler = TexCompiler.LUALATEX;
	
	private List<LatexPackage> requiredPackages = new ArrayList<>();
	private String documentclass;
	private NavigableMap<String, String> documentclassOptions = new TreeMap<>();
	private List<LatexPackage> packages = new ArrayList<>();
	private List<LatexPreambleEntry> preambleEntries = new ArrayList<>();
	private List<String> body = new ArrayList<>();

	private boolean clean;

	/** known default helper file extensions */
	private static final Set<String> DEFAULT_EXTS_FOR_CLEANING = Set.of("aux", "bbl", "log");

	/** known helper file extensions, including user set ones. */
	private final Set<String> exts = new HashSet<>();
	private String folder = "";
	private String name = null;
	private int numRepeat = 3;
	private StringBuilder str = new StringBuilder();
	private boolean bibliography;
	private String bibfile = null;
	private List<String> envs = new ArrayList<>();

	private boolean compilerSet = false;
	private boolean folderSet = false;
	private boolean nameSet = false;
	private boolean repeatSet = false;
	private boolean documentclassSet = false;
	private boolean cleanSet = false;
	private boolean colorSet = false;
	private boolean headerSet;
	private boolean footerSet;
	private boolean maketitleSet;
	private boolean titleheadSet;
	private boolean subjectSet;
	private boolean titleSet;
	private boolean subtitleSet;
	private boolean authorSet;
	private boolean dateSet;
	private boolean publisherSet;
	private boolean extratitleSet;
	private boolean uppertitlebackSet;
	private boolean lowertitlebackSet;
	private boolean dedicationSet;
	private boolean bibliographySet;
	private boolean bibfileSet;

	private boolean maketitle;
	private String titlehead;
	private String subject;
	private String title;
	private String subtitle;
	private String author;
	private String date;
	private String publisher;
	private String extratitle;
	private String uppertitleback;
	private String lowertitleback;
	private String dedication;
	private String[] colorScheme = { "black", "black" };
	private final String[] header = new String[6];
	private final String[] footer = new String[6];

	/** Constructor which does not use any defaults. */
	public Latex() {
		// nothing here
	}

	/**
	 * Copy constructor. Can be used to easily set defaults.
	 * 
	 * @param tex the LaTeX thing to copy in a new instance
	 */
	public Latex(Latex tex) {
		add(tex);
	}

	/**
	 * Gets the given number of indentation strings.
	 * 
	 * @param n the indentation level
	 * @return n times the indentation string
	 */
	public static final String indent(int n) {
		return " ".repeat(4 * n);
	}

	/**
	 * Intended for inline usage to cite some reference given in an external bib
	 * file via {@link Latex#bibfile(String)}.
	 * 
	 * @param cite the bib key
	 * @return the LaTeX object
	 */
	public static final String cite(String cite) {
		return "\\cite{" + cite + "}";
	}

	/**
	 * Intended for inline use of references. Uses cleveref under the hood, so
	 * automatically prepends equation, table etc.
	 * 
	 * @param ref the full ref, e.g. "eq:MaxwellEquation" or multiple refs like
	 *            "tab:first,tab:second"
	 * @return the latex command
	 */
	public static final String ref(String ref) {
		return "\\cref{" + ref + "}";
	}

	/**
	 * Intended for inline use of quotes. Automatically handles correct numbers of
	 * quote signs if quotes are nested.
	 * 
	 * @param quote the quote
	 * @return the latex command
	 */
	public static final String quote(String quote) {
		return "\\enquote{" + quote + "}";
	}

	/**
	 * Creates the options in the typical LaTeX layout (i.e. "[a=b,c,d=e,...]").
	 * 
	 * @param options the options
	 * @return the options formatted for LaTeX
	 */
	static final String toOptions(Map<String, String> options) {
		return "[" + new TreeMap<>(options).entrySet()
											.stream()
											.map(o -> o.getKey() + (STRING_IS_NOT_BLANK.test(o.getValue()) ? "=" + o.getValue() : ""))
											.collect(Collectors.joining(",")) + "]";
	}

	/**
	 * Gets a class with some basic default settings.
	 * 
	 * @return the default settings
	 */
	public static final Latex standard() {
		Latex tex = new Latex();
		tex.compiler(TexCompiler.LUALATEX);
		tex.folder(System.getProperty("user.dir") + "/LaTeX/");
		tex.repeat(3);
		
		tex.colorScheme("red!31.372549019!black", "green!31.372549019!black");
		tex.leftFooter(null, "\\pagemark");
		tex.rightFooter("\\pagemark", null);
		tex.leftHeader(null, "\\leftmark");
		tex.rightHeader("\\rightmark", null);

		tex.documentclassWithOptions("scrartcl", Map.ofEntries(Map.entry("a4paper", ""),
															Map.entry("DIV", "calc"), // calculate a reasonable layout
															Map.entry("BCOR", "8mm"), // binding correction of 8mm
															Map.entry("headinclude", ""), // if we have a header, include it in the layout calculation
															Map.entry("bibliography", "totoc"), // add the bibliograhy to the table of contents
															Map.entry("listof", "totoc"), // add the list of tables/figures to the table of contents
															Map.entry("index", "totoc"), // add the index (not implemented in JaTeX) to the table of contents
															Map.entry("english", ""),
															Map.entry("oneside", ""),
															Map.entry("12pt", ""),
															Map.entry("version", "last"),
															Map.entry("captions", "tableheading"))); // tables should have the heading ALWAYS above them

		tex.usePackages("csquotes",
						"babel",
						"amsmath",
						"fontspec",
						"unicode-math",
						"microtype",
						"selnolig", // breaks wrong ligatures, requires lualatex
						"siunitx",
						"booktabs",
						"xcolor",
						"colortbl",
						"hyperref",
						"cleveref",
						"bookmark",
						"scrlayer-scrpage");

		tex.usePackageWithOptions("unicode-math", Map.of("math-style", "ISO", "bold-style", "ISO", "nabla", "upright"));
		tex.usePackageWithOption("amsmath", "fleqn");

		tex.usePackageWithOptions("csquotes", Map.of("strict", "true", // report warnings as errors
											"autostyle", "true")); // looks for the babel/polyglossia language and uses that

		tex.usePackageWithOptions("babel", Map.of("main", "english"));
		tex.usePackageWithOption("xcolor", "table");
		tex.usePackageWithOption("cleveref", "noabbrev");

		tex.usePackageWithOptions("caption", Map.of("format", "plain", // usual layout, resulting in Figure X:... respectively Table X:...
											"indention", "1em", // how much is the second line indented?
											"labelfont", "{color=" + tex.getColor1() + ",small,sf,bf}", // adjust the labelfont, size, bold, sans serif, italic... color (good-ooking e.g. RoyalBlue!50!black)
											"textfont", "{color={black},small}", // font=footnotesize for smaller font
											"width", "0.925\\textwidth")); // width of the caption

		tex.usePackageWithOptions("microtype", Map.of("activate", "{true,nocompatibility}", // activate protrusion and expansion
												"final", "", // enable microtype; use "draft" to disable
												"tracking", "true",
												"factor", "1100", // add 10% to the protrusion amount (default is 1000)
												"stretch", "10", // reduce stretchability (default is 20)
												"shrink", "")); // reduce shrinkability (default is 20)

		tex.usePackageWithOptions("siunitx", Map.of("locale", "UK",
											"separate-uncertainty", "",// uncertainty of the format (x \pm y) unit
											"per-mode", "symbol-or-fraction")); // use a fraction to separate units if \per is used 

		tex.usePackageWithOptions("siunitx", Map.of("locale", "UK"));
		tex.usePackageWithOptions("hyperref", Map.of("pdfpagemode", "UseOutlines", "pdfencoding", "unicode", "bookmarksopenlevel", "0"));
		tex.usePackageWithOption("bookmark", "open");
		tex.usePackageWithOption("scrlayer-scrpage", "automark");

		// =====
		tex.addToPreamble(new LatexPreambleEntry("\\setmainfont{Latin Modern Roman}"));
		
		// some floating environment black magic
		// for a detailed explanation see https://tex.stackexchange.com/questions/39017/how-to-influence-the-position-of-float-environments-like-figure-and-table-in-lat
		// there are a couple of more parameters described there, which I have no experience with, so I don't mess with them 
		// N.B.: floatpagefraction MUST be less than topfraction
		tex.addToPreamble(new LatexPreambleEntry("\\setcounter{totalnumber}{4}"));  // (default 3) is the maximum number of floats on a text (!) page)
		tex.addToPreamble(new LatexPreambleEntry("\\setcounter{topnumber}{2}"));    // (default 2) is the maximum number of floats in the top area
		tex.addToPreamble(new LatexPreambleEntry("\\setcounter{bottomnumber}{2}")); // (default 1) is the maximum number of floats in the bottom area
		tex.addToPreamble(new LatexPreambleEntry("\\setcounter{dbltopnumber}{2}")); // (default 2) is the maximum number of full sized floats in two-column mode going above the text columns
		
		tex.addToPreamble(new LatexPreambleEntry("\\renewcommand{\\topfraction}{0.9}"));       // (default 0.7) maximum size of the top area
		tex.addToPreamble(new LatexPreambleEntry("\\renewcommand{\\bottomfraction}{0.5}"));    // (default 0.3) maximum size of the bottom area
		tex.addToPreamble(new LatexPreambleEntry("\\renewcommand{\\floatpagefraction}{0.8}")); // (default 0.7) maximum size of the top area for double-column floats
		tex.addToPreamble(new LatexPreambleEntry("\\renewcommand{\\textfraction}{0.1}"));      // (default 0.2) minimum size of the text area, i.e., the area that must not be occupied by floats
		tex.addToPreamble(new LatexPreambleEntry("\\renewcommand{\\dbltopfraction}{0.9}"));    // fit big float above 2-col. text
		tex.addToPreamble(new LatexPreambleEntry("\\renewcommand{\\dblfloatpagefraction}{0.8}"));

		// we use fontspec in the defaults anyway, so this should be fine. We don't want oldstyle numbers in tables.
		tex.addToPreamble(new LatexPreambleEntry("\\AtBeginEnvironment{tabular}", "\\addfontfeatures{Numbers={Monospaced}}"));

		// adds additional space between small capitals (should always be done)
		tex.addToPreamble(new LatexPreambleEntry("\\SetTracking{encoding={*}, shape=sc}{40}"));

		// Disable ligatures in \textsc{} environment
		tex.addToPreamble(new LatexPreambleEntry("\\DisableLigatures[ff,ffi,fj,fi]", Map.of("encoding", "*", "family", "sc*")));

		// make outermost bookmarks bold
		tex.addToPreamble(new LatexPreambleEntry("\\bookmarksetup", Map.of("addtohook", "{\\ifnum\\bookmarkget{level}<1 \\bookmarksetup{bold}\\fi}")));
		
		// ---------
		// from Will Robertson, prevents linebreaks before citations
		// Note that we mark each of the lines as standalone, to keep them together
		tex.addToPreamble(new LatexPreambleEntry("\\def\\nobreakbefore{\\relax\\ifvmode\\else\\ifhmode\\ifdim\\lastskip > 0pt\\relax\\unskip\\nobreakspace\\fi\\fi\\fi}", true));
		tex.addToPreamble(new LatexPreambleEntry("\\let\\oldcite\\cite", true));
		tex.addToPreamble(new LatexPreambleEntry("\\renewcommand\\cite{\\nobreakbefore\\oldcite}", true));
		// ---------
			
		// =====
		return tex;
	}

	/**
	 * Copy the current LaTeX object.
	 * 
	 * @return the copy
	 */
	public Latex copy() {
		return new Latex(this);
	}

	/**
	 * Gets the main color of the color scheme.
	 * 
	 * @return the main color scheme color, or null if not set
	 */
	public String getColor1() {
		if (colorScheme != null) {
			return colorScheme[0];
		}
		return null;
	}

	/**
	 * Gets the support color of the color scheme.
	 * 
	 * @return the support color scheme color, or null if not set
	 */
	public String getColor2() {
		if (colorScheme != null) {
			return colorScheme[1];
		}
		return null;
	}

	/**
	 * Sets the color scheme.
	 * 
	 * @param color1 the main color
	 * @param color2 the support color
	 * @return the LaTeX object
	 */
	public Latex colorScheme(String color1, String color2) {
		if (color1 == null && color2 == null) {
			colorScheme = null;
			colorSet = false;
		} else {
			if (color1 == null) color1 = "black";
			if (color2 == null) color2 = "black";
			colorScheme = new String[] { color1, color2 };
			colorSet = true;
		}
		return this;
	}

	/**
	 * Saves the LaTeX document <em>without executing it</em>.
	 * 
	 * @param showPath if true, print the file name to the logger
	 * @return the file name to which the LaTeX document got saved
	 */
	public String save(boolean showPath) {
		build();

		String fileName = null;
		IOs.mkdir(folder);
		
		if (name == null || name.isBlank()) {
			try {
				File f = File.createTempFile("tex", ".tex", Paths.get(folder).toFile());
				fileName = f.getAbsolutePath();
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Failed to save file", e);
			}
		} else {
			fileName = folder + name;
		}

		if (fileName == null) {
			throw new IllegalStateException("It appears no valid file name was being constructed."
					+ "Most likely a temporary file could not be generated.");
		}

		IOs.writeToFile(new File(fileName), str.toString());

		if (showPath) logger.log(Level.INFO, "File saved to: {0}", fileName);
		return fileName;
	}

	/**
	 * Saves the LaTeX document <em>without executing it</em>.
	 * 
	 * @return the file name to which the LaTeX document got saved
	 */
	public String save() {
		return save(false);
	}

	@Override
	public String toString() {
		build();
		return str.toString();
	}

	/** Prints the current state of the LaTeX document to the logger. */
	public void show() {
		logger.severe(this::toString);
	}

	/**
	 * Tries to determine whether LaTeX can be executed in the current environment.
	 * This is done by calling "latex --version" (or xetex, lualatex, ...), and, if
	 * it succeeds (should) return an error code of 0, otherwise an error code
	 * &gt;0, so this should be platform independent.
	 * 
	 * @return true if LaTeX can be executed, otherwise false
	 */
	public boolean isExecutable() {
		return isExecutable(compiler);
	}

	/**
	 * Tries to determine whether LaTeX can be executed in the current environment.
	 * This is done by calling "latex --version" (or xetex, lualatex, ...), and, if
	 * it succeeds (should) return an error code of 0, otherwise an error code
	 * &gt;0, so this should be platform independent.
	 * 
	 * @param compiler the compiler to check
	 * 
	 * @return true if LaTeX can be executed, otherwise false
	 */
	public static boolean isExecutable(TexCompiler compiler) {
		ProcessBuilder texpb = new ProcessBuilder(compiler.executableName(), "--version");
		try {
			return texpb.start().waitFor() == 0;
		} catch (IOException ioe) {
			return false;
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ie);
		}
	}

	/**
	 * Compiles the LaTeX document.
	 * 
	 * @return the error code, i.e. 0 if the execution terminated normally and &gt;1
	 *         if an error occurred
	 */
	public int exec() {
		if (compiler == TexCompiler.LUALATEX) {
			if (!hasPackage("fontspec")) {
				logger.info("You are using lualatex without the fontspec package. "
						+ "Proceeding without it.");
			}
			if (!hasPackage("unicode-math")) {
				logger.info("You are using lualatex without the unicode-math package. "
						+ "Proceeding without it.");
			}
		}
		
		String fileName = save(false);

		int errorCode = 0;
		for (int i = 0; i < numRepeat; i++) {
			ProcessBuilder texpb = new ProcessBuilder(
					compiler.executableName(), 
					"--output-directory=" + folder, 
					"--enable-write18", 
					"--interaction=nonstopmode", 
					"-halt-on-error",
					fileName
					);

			if (logger.isLoggable(Level.FINE)) texpb.inheritIO();

			try {
				Process texp = texpb.start();
				if (!logger.isLoggable(Level.FINE)) {
					// make sure we don't have a fatal error in the stream and print if there was one
					try (BufferedReader br = new BufferedReader(new InputStreamReader(texp.getInputStream()))) {
						List<String> lines = br.lines().collect(Collectors.toList());
						for (String line : lines) {
							if (line != null && line.contains("Fatal error occurred")) {
								logger.severe(lines.stream().filter(Objects::nonNull).collect(Collectors.joining(System.lineSeparator())));
								break;
							}
						}
					}
				}
				errorCode = Math.max(errorCode, texp.waitFor());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(ie);
			}

			if (bibliography && bibfile != null) {
				String file = name;
				if (name.endsWith(".tex")) file = file.substring(0, name.length() - 4);
				
				ProcessBuilder bibpb = new ProcessBuilder("biber", "--output-directory=" + folder, file);

				if (logger.isLoggable(Level.FINE)) bibpb.inheritIO();

				try {
					Process bibp = bibpb.start();
					if (!logger.isLoggable(Level.FINE)) {
						bibp.getInputStream().close();
						bibp.getErrorStream().close();
					}
					bibp.waitFor();
				} catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(ie);
				}
			}
		}

		if (clean) {
			String fileNameWithoutExt;
			if (fileName.endsWith(".tex")) {
				fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
			} else {
				// we assume it doesn't have a file ending
				fileNameWithoutExt = fileName;
			}

			for (String ext : exts) {
				File file = new File(fileNameWithoutExt + "." + ext);
				if (file.isFile()) {
					try {
						Files.delete(file.toPath());
						logger.info(() -> "Successfully deleted %s".formatted(file.toPath()));
					} catch (IOException e) {
						logger.severe(() -> "Unable to delete %s, %s".formatted(file.getAbsolutePath(), e.getMessage()));
					}
				}
			}
		}
		return errorCode;
	}

	/**
	 * Sets the filename of the LaTeX document, so it should end in ".tex".
	 * 
	 * @param name the filename
	 * @return the LaTeX object
	 */
	public Latex filename(String name) {
		this.name = name;
		nameSet = true;
		return this;
	}

	/**
	 * If true, delete helper files after the execution (including the tex file!).
	 * 
	 * @param clean true if helper files should be deleted
	 * @param exts  the additional extensions to remove, e.g. "tex" if you want to
	 *              have the tex file removed after generating the pdf as well
	 * @return the LaTeX object
	 */
	public Latex clean(boolean clean, String... exts) {
		this.clean = clean;
		this.exts.retainAll(DEFAULT_EXTS_FOR_CLEANING);
		Collections.addAll(this.exts, exts);
		cleanSet = true;
		return this;
	}

	/** Builds the String to be written to the LaTeX document. */
	private void build() {
		sanityChecks();
		str = new StringBuilder();
		buildPreamble();
		buildBody();
		createDocumentEnd();
	}

	/** Does some sanity checks prior to building the tex file. */
	private void sanityChecks() {
		if (!envs.isEmpty()) {
			logger.log(Level.SEVERE, "Seems like you opened environment(s) that at least were not closed via endEnv(). "
					+ "The environment in question is/are {0}."
					+ " I'll try to continue, as you might have closed the environment directly via add().",
					envs.toString());
		}

		// make sure we only build the titlepage if we have a compatible komascript
		// class or a standard class extended with scrextend
		if (!List.of("scrbook", "scrreprt", "scrartcl").contains(getDocumentclass())) {
			if (List.of("article", "book", "report", "letter").contains(getDocumentclass())) {
				usePackageWithOptions("scrextend", Map.of("extendedfeature", "title"));
			} else {
				maketitle(false);
			}
		}
	}

	/**
	 * Sets the compiler to use.
	 * 
	 * @param compiler the compiler
	 * @return the LaTeX object
	 */
	public Latex compiler(TexCompiler compiler) {
		this.compiler = compiler;
		return this;
	}

	/**
	 * Sets the folder to save the LaTeX stuff to.
	 * 
	 * @param folder the folder
	 * @return the LaTeX object
	 */
	public Latex folder(String folder) {
		if (!folder.endsWith("/")) folder = folder + "/";
		this.folder = folder.replace("\\", "/");
		folderSet = true;
		return this;
	}

	/**
	 * Adds &#92;usepackage and guarantees that the packages are loaded.
	 * 
	 * @param packages the packages
	 * @return the LaTeX object
	 */
	public Latex usePackages(String... packages) {
		for (String pckg : packages) {
			this.packages.add(new LatexPackage(pckg));
		}
		return this;
	}

	/**
	 * Removes the packages and all related options.
	 * 
	 * @param packages the packages
	 * @return the LaTeX object
	 */
	public Latex removePackages(String... packages) {
		List<String> packageNamesToBeRemoved = List.of(packages);
		this.packages.removeIf(p -> packageNamesToBeRemoved.contains(p.name()));
		return this;
	}

	/**
	 * Checks whether the package is in the list of packages to be loaded.
	 * 
	 * @param name the package
	 * @return the LaTeX object
	 */
	private boolean hasPackage(String name) {
		return packages.stream().anyMatch(p -> p.name().equals(name));
	}

	/**
	 * Indicates to {@link Latex} that these packages are needed.
	 * 
	 * @param packages the needed packages
	 * @return the LaTeX object
	 */
	public Latex usePackages(LatexPackage... packages) {
		this.packages.addAll(List.of(packages));
		LatexPackage.cleanup(this.packages);
		return this;
	}

	/**
	 * The number of times the compiler is called on the LaTeX document. By default
	 * 3.
	 * 
	 * @param numRepeat the number of compiler calls
	 * @return the LaTeX object
	 */
	public Latex repeat(int numRepeat) {
		this.numRepeat = numRepeat;
		repeatSet = true;
		return this;
	}

	/**
	 * Adds options to a package.
	 * 
	 * @param packageName the package to add the option to
	 * @param options     the options
	 * @return the LaTeX object
	 */
	public Latex usePackageWithOptions(String packageName, Map<String, String> options) {
		packages.add(new LatexPackage(packageName, options));
		return this;
	}

	/**
	 * Adds an option to a package.
	 * 
	 * @param packageName the package to add the option to
	 * @param option      the option
	 * @return the LaTeX object
	 */
	public Latex usePackageWithOption(String packageName, String option) {
		packages.add(new LatexPackage(packageName, option));
		return this;
	}

	/**
	 * Adds \RequirePackage.
	 * 
	 * @param packageName the package
	 * @return the LaTeX object
	 */
	public Latex requirePackage(String packageName) {
		requiredPackages.add(new LatexPackage(packageName));
		return this;
	}

	/**
	 * Adds an option to a required package.
	 * 
	 * @param packageName the required package to add the option to
	 * @param optionKey   the option
	 * @return the LaTeX object
	 */
	public Latex requirePackageWithOption(String packageName, String optionKey) {
		requirePackageWithOptions(packageName, Map.of(optionKey, ""));
		return this;
	}

	/**
	 * Adds an option to a required package.
	 * 
	 * @param packageName the required package to add the option to
	 * @param options     the options
	 * @return the LaTeX object
	 */
	public Latex requirePackageWithOptions(String packageName, Map<String, String> options) {
		requiredPackages.add(new LatexPackage(packageName, options));
		return this;
	}

	/**
	 * The documentclass to use.
	 * 
	 * @param documentclass the documentclass
	 * @return the LaTeX object
	 */
	public Latex documentclass(String documentclass) {
		this.documentclass = documentclass;
		documentclassSet = true;
		return this;
	}

	/**
	 * Adds the documentclass with options.
	 * 
	 * @param documentclass the documentclass
	 * @param options the options to add
	 * @return the LaTeX object
	 */
	public Latex documentclassWithOptions(String documentclass, Map<String, String> options) {
		documentclass(documentclass);
		documentclassOptions.putAll(options);
		return this;
	}

	/**
	 * Adds a line to the preamble. This is a thin wrapper around
	 * {@link LatexPreambleEntry}. The written preamble command will be a standalone
	 * line, i.e. it cannot be changed afterwards.
	 * 
	 * @param line the line to add
	 * @return the LaTeX object
	 */
	public Latex addToPreamble(String line) {
		if (line != null) preambleEntries.add(new LatexPreambleEntry(line, true));
		return this;
	}

	/**
	 * Adds the specified preamble entries.
	 * 
	 * @param entries the preamble entries
	 * @return the LaTeX object
	 */
	public Latex addToPreamble(LatexPreambleEntry... entries) {
		preambleEntries.addAll(List.of(entries));
		return this;
	}

	/**
	 * Removes a line from the preamble. If it occurs multiple times, all of the
	 * occurrences will be removed. You need to have an exact match of the
	 * {@link LatexPreambleEntry#cmd() command} to delete a line. Note that options
	 * and whether the line is a standalone line do not matter for the check whether
	 * to remove a preamble entry.
	 * 
	 * @param line the line to remove
	 * @return the LaTeX object
	 */
	public Latex removeFromPreamble(String line) {
		preambleEntries.removeIf(entry -> entry.cmd().equals(line));
		return this;
	}

	/**
	 * Checks whether a specific line is contained in the list of lines to be added
	 * to the preamble.
	 * 
	 * @param line the line to check for
	 * @return the LaTeX object
	 */
	private boolean hasPreambleEntry(String line) {
		return line != null && preambleEntries.stream()
												.map(LatexPreambleEntry::cmd)
												.anyMatch(line::equals);
	}

	/**
	 * Adds a line of code to the body of the LaTeX document.
	 * 
	 * @param code the line of code
	 * @return the LaTeX object
	 */
	public Latex add(String code) {
		body.add(indent(1) + code);
		return this;
	}

	/** Ends the LaTeX document. */
	private void createDocumentEnd() {
		str.append("\\end{document}" + LINE_BREAK);
	}

	/**
	 * Appends the given strings to the specified StringBuilder.
	 * 
	 * @param sb   the StringBuilder that gets appended to
	 * @param strs the strings to append
	 * @return the StringBuilder with the appended strings
	 */
	private static final StringBuilder append(StringBuilder sb, String... strs) {
		for (String string : strs) sb.append(string);
		return sb;
	}

	/**
	 * Creates the whole preamble of the LaTeX document, including documentclass,
	 * package loading and user settings/defs.
	 */
	private void buildPreamble() {
		StringBuilder requirePackage = append(new StringBuilder(), MAJOR_SEPARATOR.cmd(), LINE_BREAK);
		append(requirePackage, "% Required packages", LINE_BREAK, MAJOR_SEPARATOR.cmd(), LINE_BREAK);
		
		requiredPackages = LatexPackage.cleanup(requiredPackages);
		for (LatexPackage p : requiredPackages) {
			requirePackage.append("\\RequirePackage");
			if (!p.options().isEmpty()) {
				requirePackage.append(toOptions(p.options()));
			}
			append(requirePackage, "{", p.name(), "}", LINE_BREAK);
		}
		append(requirePackage, MAJOR_SEPARATOR.cmd(), LINE_BREAK.repeat(2));

		StringBuilder documentclassLine = new StringBuilder("\\documentclass");
		if (!documentclassOptions.isEmpty()) {
			documentclassLine.append(toOptions(documentclassOptions));
		}
		documentclassLine.append("{" + documentclass + "}" + LINE_BREAK.repeat(2));

		StringBuilder packageImports = append(new StringBuilder(), MAJOR_SEPARATOR.cmd(), LINE_BREAK);
		append(packageImports, "% packages", LINE_BREAK, MAJOR_SEPARATOR.cmd(), LINE_BREAK);
		
		packages = LatexPackage.cleanup(packages);
		for (LatexPackage p : packages) {
			packageImports.append("\\usepackage");
			if (!p.options().isEmpty()) {
				packageImports.append(toOptions(p.options()));
			}
			append(packageImports, "{", p.name(), "}", LINE_BREAK);
		}
		append(packageImports, MAJOR_SEPARATOR.cmd(), LINE_BREAK.repeat(2));

		append(str, "% !TEX program = ", compiler.toString().toLowerCase(), LINE_BREAK);
		if (bibliography) append(str, "% !BIB program = biber", LINE_BREAK);
		append(str, "% !TEX encoding = UTF-8 Unicode", LINE_BREAK.repeat(2));

		if (!requiredPackages.isEmpty()) str.append(requirePackage.toString());
		str.append(documentclassLine.toString());
		if (!packages.isEmpty()) str.append(packageImports.toString());
		if (!preambleEntries.isEmpty()) str.append(buildUserSettingsAndDefs());
		
		if (maketitle) {
			// titlepage
			append(str, MAJOR_SEPARATOR.cmd(), "% titlepage", LINE_BREAK, MAJOR_SEPARATOR.cmd(), LINE_BREAK);
			if (titlehead != null)      append(str,"\\titlehead{", titlehead, "}", LINE_BREAK);
			if (subject != null)        append(str, "\\subject{", subject, "}", LINE_BREAK);
			if (title != null)          append(str, "\\title{\\color{", getColor1(), "}", title, "}", LINE_BREAK);
			if (subtitle != null)       append(str, "\\subtitle{\\color{", getColor1(), "}", subtitle, "}", LINE_BREAK);
			if (author != null)         append(str, "\\author{", author, "}", LINE_BREAK);
			if (date != null)           append(str, "\\date{", date, "}", LINE_BREAK);
			if (publisher != null)      append(str, "\\publishers{", publisher, "}", LINE_BREAK);
			if (extratitle != null)     append(str, "\\extratitle{", extratitle, "}", LINE_BREAK);
			if (uppertitleback != null) append(str, "\\uppertitleback{", uppertitleback, "}", LINE_BREAK);
			if (lowertitleback != null) append(str, "\\lowertitleback{", lowertitleback, "}", LINE_BREAK);
			if (dedication != null)     append(str, "\\dedication{", dedication, "}", LINE_BREAK);
			append(str, MAJOR_SEPARATOR.cmd(), LINE_BREAK.repeat(2));
		}
		append(str, "\\begin{document}", LINE_BREAK);
	}

	/**
	 * Builds the part of the preamble that handles the non-LaTeX-standard
	 * settings/definitions.
	 * 
	 * @return the user settings/definitions
	 */
	private String buildUserSettingsAndDefs() {
		addDefaultPreambleEntries();
		LatexPreambleEntry.cleanup(preambleEntries);
		
		// these come at the very end
		preambleEntries.add(MAJOR_SEPARATOR);
		preambleEntries.add(EMPTY_LINE);
		
		// we have another empty line here, as the building of the return stream below
		// collects the preamble entries via a joining collector, i.e. the last preamble
		// entry does not get a linebreak at the end
		preambleEntries.add(EMPTY_LINE);

		return preambleEntries.stream()
								.map(LatexPreambleEntry::preambleLine)
								.collect(Collectors.joining(LINE_BREAK));
	}

	/** Prepends default settings to the preamble entries. */
	private void addDefaultPreambleEntries() {
		List<LatexPreambleEntry> nspe = new ArrayList<>();
		
		// this adds the settings/user def info above
		nspe.add(MAJOR_SEPARATOR);
		nspe.add(new LatexPreambleEntry("% settings/user defs"));
		nspe.add(MAJOR_SEPARATOR);
		
		// put bib file at the very top
		boolean addBibResource = packages.stream().anyMatch(p -> p.name().equals("biblatex")) && bibfile != null;
		if (addBibResource) {
			nspe.add(new LatexPreambleEntry("\\addbibresource", Map.of(bibfile + ".bib", ""), false));
			nspe.add(EMPTY_LINE);
		}

		// tikz/pgf imports come pretty much at the top
		boolean tikzLibraryLoaded = false;
		if (preambleEntries.stream().map(LatexPreambleEntry::cmd).anyMatch("usegdlibrary"::equals)) {
			tikzLibraryLoaded = nspe.add(new LatexPreambleEntry("usegdlibrary", false));
		}
		if (preambleEntries.stream().map(LatexPreambleEntry::cmd).anyMatch("usepgfplotslibrary"::equals)) {
			tikzLibraryLoaded = nspe.add(new LatexPreambleEntry("usepgfplotslibrary", false));
		}
		if (preambleEntries.stream().map(LatexPreambleEntry::cmd).anyMatch("usepgflibrary"::equals)) {
			tikzLibraryLoaded = nspe.add(new LatexPreambleEntry("usepgflibrary", false));
		}
		if (preambleEntries.stream().map(LatexPreambleEntry::cmd).anyMatch("usetikzlibrary"::equals)) {
			tikzLibraryLoaded = nspe.add(new LatexPreambleEntry("usetikzlibrary", false));
		}
		if (tikzLibraryLoaded) {
			nspe.add(EMPTY_LINE);
		}

		// header/footer
		if (hasPackage("scrlayer-scrpage")) {
			boolean hasHeaderInfo = Stream.of(header).anyMatch(Objects::nonNull);
			if (hasHeaderInfo) {
				// remove old footer formatting
				nspe.add(new LatexPreambleEntry("\\clearpairofpagestyles", true));
			}
			if (header[0] != null) {
				// what to write to the left head on even pages
				nspe.add(new LatexPreambleEntry("\\lehead", Map.of(header[0], ""), false));
			}
			if (header[1] != null) {
				// what to write to the left head on odd pages
				nspe.add(new LatexPreambleEntry("\\lohead", Map.of(header[1], ""), false));
			}
			if (header[2] != null) {
				// what to write to the center foot on even pages
				nspe.add(new LatexPreambleEntry("\\cehead", Map.of(header[2], ""), false));
			}
			if (header[3] != null) {
				// what to write to the right foot on odd pages
				nspe.add(new LatexPreambleEntry("\\cohead", Map.of(header[3], ""), false));
			}
			if (header[4] != null) {
				// what to write to the right foot on even pages
				nspe.add(new LatexPreambleEntry("\\rehead", Map.of(header[4], ""), false));
			}
			if (header[5] != null) {
				// what to write to the right foot on odd pages
				nspe.add(new LatexPreambleEntry("\\rohead", Map.of(header[5], ""), false));
			}
			if (hasHeaderInfo) nspe.add(EMPTY_LINE);

			boolean hasFooterInfo = Stream.of(footer).anyMatch(Objects::nonNull);
			if (hasFooterInfo) {
				// remove old header formatting
				nspe.add(new LatexPreambleEntry("\\clearmainofpairofpagestyles"));
			}
			if (footer[0] != null) {
				// what to write to the left foot on even pages
				nspe.add(new LatexPreambleEntry("\\lefoot", Map.of(footer[0], ""), false));
			}
			if (footer[1] != null) {
				// what to write to the left foot on odd pages
				nspe.add(new LatexPreambleEntry("\\lofoot", Map.of(footer[1], ""), false));
			}
			if (footer[2] != null) {
				// what to write to the center foot on even pages
				nspe.add(new LatexPreambleEntry("\\cefoot", Map.of(footer[2], ""), false));
			}
			if (footer[3] != null) {
				// what to write to the center foot on odd pages
				nspe.add(new LatexPreambleEntry("\\cofoot", Map.of(footer[3], ""), false));
			}
			if (footer[4] != null) {
				// what to write to the right foot on even pages
				nspe.add(new LatexPreambleEntry("\\refoot", Map.of(footer[4], ""), false));
			}
			if (footer[5] != null) {
				// what to write to the right foot on odd pages
				nspe.add(new LatexPreambleEntry("\\rofoot", Map.of(footer[5], ""), false));
			}
			if (hasFooterInfo) nspe.add(EMPTY_LINE);
		}
		
		if (colorScheme != null) {
			if (hasPackage("caption")) {
				nspe.add(new LatexPreambleEntry("\\captionsetup", Map.of("labelfont+", "{color={" + getColor1() + "}}"), false));
			}
			if (hasPackage("subcaption")) {
				nspe.add(new LatexPreambleEntry("\\captionsetup[sub]", Map.of("labelfont+", "{color={" + getColor1() + "}}"), false));
			}
			if (hasPackage("caption") || hasPackage("subcaption")) {
				nspe.add(EMPTY_LINE);
			}

			if (("scrartcl".equals(getDocumentclass()) 
					|| "scrreprt".equals(getDocumentclass())
					|| "scrbook".equals(getDocumentclass()))) {

				nspe.add(new LatexPreambleEntry("\\addtokomafont{pagehead}", Map.of("\\color{" + getColor1() + "}", ""), false));
				nspe.add(new LatexPreambleEntry("\\addtokomafont{footnoterule}", Map.of("\\color{" + getColor2() + "}", ""), false));
				nspe.add(new LatexPreambleEntry("\\addtokomafont{pagenumber}", Map.of("\\color{" + getColor2() + "}", ""), false));
				
				if ("scrbook".equals(getDocumentclass())) {
					nspe.add(new LatexPreambleEntry("\\addtokomafont{part}", Map.of("\\color{" + getColor1() + "}", ""), false));
				}
				if ("scrbook".equals(getDocumentclass()) || "scrreprt".equals(getDocumentclass())) {
					nspe.add(new LatexPreambleEntry("\\addtokomafont{chapter}", Map.of("\\color{" + getColor1() + "}", ""), false));
				}
				nspe.add(new LatexPreambleEntry("\\addtokomafont{section}", Map.of("\\color{" + getColor1() + "}", ""), false));
				nspe.add(new LatexPreambleEntry("\\addtokomafont{subsection}", Map.of("\\color{" + getColor1() + "}", ""), false));
				nspe.add(new LatexPreambleEntry("\\addtokomafont{subsubsection}", Map.of("\\color{" + getColor1() + "}", ""), false));
				nspe.add(EMPTY_LINE);
			}
		}

		preambleEntries.addAll(0, nspe);
	}

	/**
	 * Sets the behavior of the left footer on odd respectively even pages.
	 * 
	 * @param odd  the commands used on an odd page
	 * @param even the commands used on an even page
	 * @return the LaTeX object
	 */
	public Latex leftFooter(String odd, String even) {
		footer[0] = even;
		footer[1] = odd;
		footerSet = true;
		return usePackages("scrlayer-scrpage");
	}

	/**
	 * Sets the behavior of the center footer on odd respectively even pages.
	 * 
	 * @param odd  the commands used on an odd page
	 * @param even the commands used on an even page
	 * @return the LaTeX object
	 */
	public Latex centerFooter(String odd, String even) {
		footer[2] = even;
		footer[3] = odd;
		footerSet = true;
		return usePackages("scrlayer-scrpage");
	}

	/**
	 * Sets the behavior of the right footer on odd respectively even pages.
	 * 
	 * @param odd  the commands used on an odd page
	 * @param even the commands used on an even page
	 * @return the LaTeX object
	 */
	public Latex rightFooter(String odd, String even) {
		footer[4] = even;
		footer[5] = odd;
		footerSet = true;
		return usePackages("scrlayer-scrpage");
	}

	/**
	 * Sets the behavior of the left header on odd respectively even pages.
	 * 
	 * @param odd  the commands used on an odd page
	 * @param even the commands used on an even page
	 * @return the LaTeX object
	 */
	public Latex leftHeader(String odd, String even) {
		header[0] = even;
		header[1] = odd;
		headerSet = true;
		return usePackages("scrlayer-scrpage");
	}

	/**
	 * Sets the behavior of the center header on odd respectively even pages.
	 * 
	 * @param odd  the commands used on an odd page
	 * @param even the commands used on an even page
	 * @return the LaTeX object
	 */
	public Latex centerHeader(String odd, String even) {
		header[2] = even;
		header[3] = odd;
		headerSet = true;
		return usePackages("scrlayer-scrpage");
	}

	/**
	 * Sets the behavior of the right header on odd respectively even pages.
	 * 
	 * @param odd  the commands used on an odd page
	 * @param even the commands used on an even page
	 * @return the LaTeX object
	 */
	public Latex rightHeader(String odd, String even) {
		header[4] = even;
		header[5] = odd;
		headerSet = true;
		return usePackages("scrlayer-scrpage");
	}

	/**
	 * Gets the entries used for the header behavior.
	 * 
	 * @return the header behavior entries
	 */
	private String[] getHeader() {
		return header.clone();
	}

	/**
	 * Gets the entries used for the footer behavior.
	 * 
	 * @return the footer behavior entries
	 */
	private String[] getFooter() {
		return footer.clone();
	}

	/**
	 * Escapes LaTeX chars.
	 * 
	 * @param input the String
	 * @return the String with escaped chars
	 */
	public static final String escapeAllChars(String input) {
		return input.replace("\\", "\\textbackslash ")
					.replace("#", "\\#")
					.replace("$", "\\$")
					.replace("%", "\\%")
					.replace("_", "\\_")
					.replace("&", "\\&")
					.replace("{", "\\{")
					.replace("}", "\\}")
					.replace("~", "\\textasciitilde ")
					.replace("^", "\\textasciicircum ");
	}

	/**
	 * Escapes LaTeX chars if no inline math mode is detected. This could be made
	 * more fancy, but should work pretty well in most cases.
	 * 
	 * @param input the String
	 * @return the String with escaped chars
	 */
	public static final String escapeChars(String input) {
		// we try to figure out if we are in (inline) math mode
		StringBuilder ret = new StringBuilder();
		boolean mathmodeActive = false;
		for (int i = 0; i < input.length(); i++) {
			char str = input.charAt(i);
			if (mathmodeActive) {
				ret.append(str);
				if (str == '$') mathmodeActive ^= true;
			} else {
				if (str == '$') mathmodeActive ^= true;
				ret.append(mathmodeActive ? str : escapeAllChars(Character.toString(str)));
			}
		}
		return ret.toString();
	}

	/** Creates the body of the LaTeX document. */
	private void buildBody() {
		if (hasPackage("scrlayer-scrpage") && maketitle) {
			append(str, indent(1), "\\pagestyle{empty}", LINE_BREAK);
			append(str, indent(1), "\\maketitle[-1]", LINE_BREAK);
			append(str, indent(1), "\\pagestyle{scrheadings}", LINE_BREAK.repeat(2));
		}
		str.append(String.join(LINE_BREAK, body) + LINE_BREAK);
	}

	/**
	 * Add another LaTeX class (or multiple), override entries from the current
	 * class, if given. Append where possible.
	 * 
	 * @param texs the LaTeX classes to add
	 * @return the LaTeX object
	 */
	public Latex add(Latex... texs) {
		for (Latex tex : texs) {
			if (tex.compilerSet) compiler(tex.getCompiler());
			if (tex.folderSet) folder(tex.getFolder());
			if (tex.nameSet) filename(tex.getFilename());
			if (tex.repeatSet) repeat(tex.getRepeat());
			if (tex.cleanSet) clean(tex.clean, tex.exts.toArray(String[]::new));
			if (tex.colorSet) colorScheme(tex.getColor1(), tex.getColor2());
			if (tex.headerSet) {
				leftHeader(  tex.getHeader()[1], tex.getHeader()[0]);
				centerHeader(tex.getHeader()[3], tex.getHeader()[2]);
				rightHeader( tex.getHeader()[5], tex.getHeader()[4]);
			}
			if (tex.footerSet) {
				leftFooter(  tex.getFooter()[1], tex.getFooter()[0]);
				centerFooter(tex.getFooter()[3], tex.getFooter()[2]);
				rightFooter( tex.getFooter()[5], tex.getFooter()[4]);
			}
			if (tex.maketitleSet) maketitle(tex.maketitle);
			if (tex.titleheadSet) titlehead(tex.titlehead);
			if (tex.subjectSet) subject(tex.subject);
			if (tex.titleSet) title(tex.title);
			if (tex.subtitleSet) subtitle(tex.subtitle);
			if (tex.authorSet) authors(tex.author.split("\\and "));
			if (tex.dateSet) date(tex.date);
			if (tex.publisherSet) publisher(tex.publisher);
			if (tex.extratitleSet) extratitle(tex.extratitle);
			if (tex.uppertitlebackSet) uppertitleback(tex.uppertitleback);
			if (tex.lowertitlebackSet) lowertitleback(tex.lowertitleback);
			if (tex.dedicationSet) dedication(tex.dedication);
			if (tex.bibliographySet) bib(tex.bibliography);
			if (tex.bibfileSet) bibfile(tex.bibfile);

			requiredPackages.addAll(tex.packages());

			if (tex.documentclassSet) {
				documentclass(tex.getDocumentclass());
			}

			documentclassOptions.putAll(tex.documentclassOptions());
			packages.addAll(tex.packages());
			preambleEntries.addAll(LatexPreambleEntry.cleanup(tex.preambleEntries));
			body.addAll(tex.body);
		}
		
		LatexPackage.cleanup(requiredPackages);
		LatexPackage.cleanup(packages);
		LatexPreambleEntry.cleanup(preambleEntries);
		return this;
	}

	/**
	 * Gets the documentclass options.
	 * 
	 * @return the documentclass options
	 */
	private Map<String, String> documentclassOptions() {
		return documentclassOptions;
	}

	/**
	 * Gets the packages.
	 * 
	 * @return the packages
	 */
	private List<LatexPackage> packages() {
		return List.copyOf(LatexPackage.cleanup(packages));
	}

	/**
	 * Gets the folder to save the tex file in.
	 * 
	 * @return the folder
	 */
	public String getFolder() {
		return folder;
	}

	/**
	 * Gets the file name of the tex file to be produced.
	 * 
	 * @return the file name
	 */
	public String getFilename() {
		return name;
	}

	/**
	 * Gets the current compiler.
	 * 
	 * @return the compiler
	 */
	public TexCompiler getCompiler() {
		return compiler;
	}

	/**
	 * Gets the number of repeated compiler calls on the tex file.
	 * 
	 * @return the number of calls of the tex file
	 */
	public int getRepeat() {
		return numRepeat;
	}

	/**
	 * Gets the current documentclass.
	 * 
	 * @return the documentclass
	 */
	public String getDocumentclass() {
		return documentclass;
	}

	/**
	 * Adds a figure via {@literal \}includegraphics with the given width, caption
	 * and label.
	 * 
	 * @param path    the path of the graphic to include
	 * @param width   the aimed for width of the graphic
	 * @param caption the caption
	 * @param label   the label of the graphic
	 * @return the LaTeX object
	 */
	public Latex addFigure(String path, String width, String caption, String label) {
		return add(new Figure().mode(FigureEnvironment.FIGURE)
								.centering(true)
								.width(width)
								.path(path)
								.caption(caption)
								.label(label));
	}

	/**
	 * Adds a figure via {@literal \}includegraphics with the given caption.
	 * 
	 * @param path    the path of the graphic to include
	 * @param caption the caption
	 * @return the LaTeX object
	 */
	public Latex addFigure(String path, String caption) {
		return addFigure(path, "", caption, "");
	}

	/**
	 * Adds information from a texable. This can be used from outside the JaTeX
	 * project to create own classes that implement the {@link Texable} interface
	 * and parse them correctly and consistently into LaTeX.
	 * 
	 * @param texable the texable containing information about the required
	 *                packages, package options, package incompatibilities, extra
	 *                preamble lines and the lines of code to be added to the LaTeX
	 *                document
	 * @return the LaTeX object
	 */
	public Latex add(Texable... texable) {
		for (Texable tex : texable) {
			// we start with the code to make sure all packages are loaded
			body.addAll(tex.latexCode());

			packages.addAll(tex.neededPackages());
			preambleEntries.addAll(tex.preambleExtras());
		}
		requiredPackages = LatexPackage.cleanup(requiredPackages, true);
		packages = LatexPackage.cleanup(packages, true);
		
		List<LatexPackage> allPackagesToLoad = new ArrayList<>(packages);
		allPackagesToLoad.addAll(requiredPackages);
		LatexPackage.checkForIncompatiblePackages(requiredPackages);
		
		preambleEntries = LatexPreambleEntry.cleanup(preambleEntries);
		return this;
	}

	/**
	 * Adds the given tikzlibraries.
	 * 
	 * @param libraries the tikzlibraries, e.g. "shpaes.misc"
	 * @return the LaTeX object
	 */
	public Latex tikzlibraries(String... libraries) {
		if (libraries != null && libraries.length > 0) {
			usePackages("tikz");
			for (String library : libraries) {
				addToPreamble(new LatexPreambleEntry("\\usetikzlibrary", library));
			}
		}
		return this;
	}

	/**
	 * Adds the given pgflibraries.
	 * 
	 * @param libraries the pgflibraries, e.g. "plothandlers"
	 * @return the LaTeX object
	 */
	public Latex pgflibraries(String... libraries) {
		if (libraries != null && libraries.length > 0) {
			usePackages("pgf");
			for (String library : libraries) {
				addToPreamble(new LatexPreambleEntry("\\usepgflibrary", library));
			}
		}
		return this;
	}

	/**
	 * Adds the given pgfplotslibraries.
	 * 
	 * @param libraries the pgfplotslibraries, e.g. "external"
	 * @return the LaTeX object
	 */
	public Latex pgfplotslibraries(String... libraries) {
		if (libraries != null && libraries.length > 0) {
			usePackages("pgfplots");
			for (String library : libraries) {
				addToPreamble(new LatexPreambleEntry("\\usepgfplotslibrary", library));
			}
		}
		return this;
	}

	/**
	 * Adds the given gdlibraries.
	 * 
	 * @param libraries the gdlibraries, e.g. "layered"
	 * @return the LaTeX object
	 */
	public Latex gdlibraries(String... libraries) {
		if (libraries != null && libraries.length > 0) {
			usePackages("tikz");
			for (String library : libraries) {
				addToPreamble(new LatexPreambleEntry("\\usegdlibrary", library));
			}
		}
		return this;
	}

	/**
	 * Use the automatic externalization of tikz/pgf graphics, and save the
	 * corresponding files in the specified folder. This will not work if you change
	 * the compiler or the main output folder afterwards.
	 * 
	 * @param folder the folder to save the created graphics to
	 * @return the LaTeX object
	 */
	public Latex externalize(String folder) {
		if (folder == null) throw new IllegalArgumentException("null is not a valid tikzexternalization folder!");
		usePackages("tikz", "shellesc");
		tikzlibraries("external");
		
		if (!folder.isEmpty()) {
			folder = folder.replace("\\", "/");
			if (!folder.endsWith("/")) folder = folder + "/";

			if (Paths.get(folder).isAbsolute()) {
				IOs.mkdir(folder);
			} else {
				IOs.mkdir(this.folder + folder);
			}
		}
		
		addToPreamble("\\tikzexternalize[" + (folder.isBlank() ? "" : "prefix=" + folder) + "]");
		addToPreamble("\\tikzset{external/system call={" + compiler.name().toLowerCase(Locale.ENGLISH) + " --output-directory=" + this.folder
				+ " \\tikzexternalcheckshellescape --enable-write18 -halt-on-error -interaction=batchmode -jobname \"\\image\" \"\\texsource\"}}");
		return this;
	}

	/**
	 * Use the automatic externalization of tikz/pgf graphics, and save the
	 * corresponding files in a default folder.
	 * 
	 * @return the LaTeX object
	 */
	public Latex externalize() {
		return externalize("tikz");
	}

	/**
	 * Plots the given data.
	 * 
	 * @param         <T> the data type
	 * @param caption the caption of the plot
	 * @param data    the data to plot
	 * @param legend  the legend corresponding to the data
	 * @param options the options for the plot (not the axis environment!)
	 * @return the LaTeX object
	 */
	public <T> Latex plotData(String caption, T data, String legend, Map<String, String> options) {
		return plotData(PgfPlots.of(data, legend, options), caption);
	}

	/**
	 * Adds the given Plot.
	 * 
	 * @param plot    the Plot to add
	 * @param caption the caption
	 * @return the LaTeX object
	 */
	public Latex plotData(PgfPlots plot, String caption) {
		return add(Figure.in(FigureEnvironment.FIGURE)
							.centering(true)
							.caption(caption)
							.tikz(Tikz.of(plot)));
	}

	/**
	 * Adds the given equation.
	 * 
	 * @param eq the equation (multiple lines)
	 * @return the LaTeX object
	 */
	public Latex equation(String... eq) {
		return add(new Equation().add(eq));
	}

	/**
	 * Adds the given equation.
	 * 
	 * @param label the label
	 * @param eq    the equation (multiple lines)
	 * @return the LaTeX object
	 */
	public Latex labeledEquation(String label, String... eq) {
		return add(new Equation().add(eq).label(label));
	}

	/**
	 * Adds the given equation.
	 * 
	 * @param env   the math environment to use
	 * @param label the label
	 * @param eq    the equation (multiple lines)
	 * @return the LaTeX object
	 */
	public Latex equation(EquationEnvironment env, String label, String... eq) {
		return add(new Equation().environment(env).add(eq).label(label));
	}

	/**
	 * Determines whether to use the bibliography. Do note that you need the biber
	 * executable in your system or user environment variables!
	 * 
	 * @param bibliography true if you want to use the bibliography
	 * @return the LaTeX object
	 */
	public Latex bib(boolean bibliography) {
		// set to a value greater than 0 to allow url breaking after lowercase (lc),
		// uppercase (uc) and numerical (num) values
		String biburllc  = "\\setcounter{biburllcpenalty}{7000}";
		String biburluc  = "\\setcounter{biburlucpenalty}{7000}";
		String biburlnum = "\\setcounter{biburlnumpenalty}{7000}";

		if (bibliography) {
			usePackages("biblatex");
			usePackageWithOptions("biblatex", Map.of("backend", "biber",
											"hyperref", "true",
											"language", "english",
											"style", "numeric-comp",
											"maxbibnames", "5",
											"sortlocale", "en"));

			if (!hasPreambleEntry(biburllc))  addToPreamble(biburllc);
			if (!hasPreambleEntry(biburluc))  addToPreamble(biburluc);
			if (!hasPreambleEntry(biburlnum)) addToPreamble(biburlnum);
		} else {
			removePackages("biblatex");
			removeFromPreamble(biburllc);
			removeFromPreamble(biburluc);
			removeFromPreamble(biburlnum);
		}
		this.bibliography = bibliography;
		bibliographySet = true;
		return this;
	}

	/**
	 * Specifies the bibfile and activates biblatex (see
	 * {@link Latex#bib(boolean)}), if not already done.
	 * 
	 * @param bibfile the path to the bibfile <em>relative</em> to the folder
	 *                specified in {@link Latex#folder(String)}
	 * @return the LaTeX object
	 */
	public Latex bibfile(String bibfile) {
		this.bibfile = bibfile;
		bibfileSet = true;
		return bib(true);
	}

	/**
	 * Adds a bibliography. Needs a known bib file, set via
	 * {@link Latex#bibfile(String)}.
	 * 
	 * @return the LaTeX object
	 */
	public Latex bib() {
		add("\\printbibliography");
		return this;
	}

	/**
	 * Adds a table of contents.
	 * 
	 * @return the LaTeX object
	 */
	public Latex toc() {
		return add(deactivateProtrusion("\\tableofcontents"));
	}

	/**
	 * Adds a list of figures.
	 * 
	 * @return the LaTeX object
	 */
	public Latex lof() {
		return add(deactivateProtrusion("\\listoffigures"));
	}

	/**
	 * Adds a list of tables.
	 * 
	 * @return the LaTeX object
	 */
	public Latex lot() {
		return add(deactivateProtrusion("\\listoftables"));
	}

	/**
	 * Deactivated protrusion for the given code.
	 * 
	 * @param code the code
	 * @return the code with disabled microtype protrusion
	 */
	private String deactivateProtrusion(String code) {
		boolean microtype = hasPackage("microtype");
		// if we have microtype we disable protrusion for the encapsulated string
		return (microtype ? "{\\microtypesetup{protrusion=false}" : "") + code + (microtype ? "}" : "");
	}

	/**
	 * Starts a new environment via <code>\begin{env}</code>. Should be closed via
	 * {@link Latex#endEnv(String)}, although it will not break if you close it
	 * differently.
	 * 
	 * @param env the environment to begin
	 * @return the LaTeX object
	 */
	public Latex beginEnv(String env) {
		envs.add(0, env);
		return add("\\begin{" + env + "}%");
	}

	/**
	 * Ends an environment via <code>\end{env}</code>. Should have been opened via
	 * {@link Latex#beginEnv(String)}, although it will not break if you opened it
	 * differently.
	 * 
	 * @param env the environment to end
	 * @return the LaTeX object
	 */
	public Latex endEnv(String env) {
		boolean found = false;
		for (String openEnv : envs) {
			if (openEnv.equals(env)) {
				envs.remove(0);
				found = true;
				break;
			}
		}
		if (!found) {
			logger.log(Level.WARNING, "Seems like you closed an environment "
					+ "that at least was not opened via beginEnv(). "
					+ "The env in question is {0}. "
					+ "I'll try to continue, as you might have opened the "
					+ "environment directly via add().", env);
		}
		return add("\\end{" + env + "}%");
	}

	/**
	 * If true, try to make a title page.
	 * 
	 * @param maketitle true if you want a titlepage
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex maketitle(boolean maketitle) {
		this.maketitle = maketitle;
		maketitleSet = true;
		return this;
	}

	/**
	 * Try to make a titlepage.
	 * 
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex maketitle() {
		return maketitle(true);
	}

	/**
	 * Creates a head for the main title page. The user given formatted text will be
	 * displayed across the whole textwidth.
	 * 
	 * @param titlehead the head for the main title page
	 * @return the LaTeX object
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex titlehead(String titlehead) {
		this.titlehead = titlehead;
		titleheadSet = true;
		return maketitle();
	}

	/**
	 * The subject will be put centered immediately above the title.
	 * 
	 * @param subject the document subject
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex subject(String subject) {
		usePackageWithOptions("hyperref", Map.of("pdfsubject", "{" + subject + "}"));
		this.subject = subject;
		subjectSet = true;
		return maketitle();
	}

	/**
	 * The title is centered (horizontally) on the main title page.
	 * 
	 * @param title the title
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex title(String title) {
		usePackageWithOptions("hyperref", Map.of("pdftitle", "{" + title + "}"));
		this.title = title;
		titleSet = true;
		return maketitle();
	}

	/**
	 * The subtitle is put slightly below the title.
	 * 
	 * @param subtitle the subtitle
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex subtitle(String subtitle) {
		this.subtitle = subtitle;
		subtitleSet = true;
		return maketitle();
	}

	/**
	 * If you have multiple authors they will be joined via "\\and" between them.
	 * If you have annotations for an author, you can use "\\thanks{annotation...}"
	 * for further information put in a footnote.
	 * 
	 * @param authors the author(s)
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex authors(String... authors) {
		this.author = String.join("\\and ", authors);
		String pdfClean = author.replace("\\and ", ",").replace("\\and", ",");
		String thanks = "\\thanks{";
		while (pdfClean.contains(thanks)) {
			int i = pdfClean.indexOf(thanks);
			String tmp = pdfClean.substring(i + thanks.length());
			int openBrackets = 1;
			for (int j = 0; j < tmp.length(); j++) {
				if (Character.toString(tmp.charAt(j)).equals("{")) openBrackets++;
				if (Character.toString(tmp.charAt(j)).equals("}")) openBrackets--;
				if (openBrackets == 0) {
					pdfClean = pdfClean.replace(pdfClean.substring(i).substring(0, j + thanks.length() + 1), "");
					break;
				}
			}
		}

		usePackageWithOptions("hyperref", Map.of("pdfauthor", "{" + pdfClean + "}"));
		authorSet = true;
		return maketitle();
	}

	/**
	 * The date shown on the titlepage. If you want the current day use "\\today".
	 * 
	 * @param date the date
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex date(String date) {
		this.date = date;
		dateSet = true;
		return maketitle();
	}

	/**
	 * A field at the end of the main title page. Intended for the publisher name,
	 * but it might very well be used for different content, if it deems
	 * appropriate. Be aware that below this field the potentially existing
	 * "\\thanks{}" entries from {@link Latex#authors(String...)} are shown.
	 * 
	 * @param publisher the publisher name (or something similar)
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex publisher(String publisher) {
		this.publisher = publisher;
		publisherSet = true;
		return maketitle();
	}

	/**
	 * The so-called "dirt title" ("Schmutztitel" in german) is usually used
	 * nowadays to display publishing company information, ISBN numbers etc. No
	 * specific formatting. Background information: the name stems from the
	 * beginnings of book printing, as books did not usually have a cover for
	 * protection back then, the first page of the page was the only protection.
	 * This page often had a short title on it, and, of course, got often dirty,
	 * thus the name.
	 * 
	 * @param extratitle the "dirt title"
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex extratitle(String extratitle) {
		this.extratitle = extratitle;
		extratitleSet = true;
		return maketitle();
	}

	/**
	 * In standard double-sided print the back side of the title page is usually
	 * empty. This commands put the user given text at the upper part of the back
	 * side of the title page.
	 * 
	 * @param uppertitleback the text to put on the upper back part of the title
	 *                       page
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #lowertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex uppertitleback(String uppertitleback) {
		this.uppertitleback = uppertitleback;
		uppertitlebackSet = true;
		return maketitle();
	}

	/**
	 * In standard double-sided print the back side of the title page is usually
	 * empty. This commands put the user given text at the upper part of the back
	 * side of the title page. Can be used for copyright information or the like.
	 * 
	 * @param lowertitleback the text to put on the lower back part of the title
	 *                       page
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #dedication(String)
	 */
	public Latex lowertitleback(String lowertitleback) {
		this.lowertitleback = lowertitleback;
		lowertitlebackSet = true;
		return maketitle();
	}

	/**
	 * Puts the given text centered on a separate dedication page.
	 * 
	 * @param dedication the dedication for the document
	 * @return the LaTeX object
	 * @see #titlehead(String)
	 * @see #subject(String)
	 * @see #title(String)
	 * @see #subtitle(String)
	 * @see #authors(String...)
	 * @see #date(String)
	 * @see #publisher(String)
	 * @see #extratitle(String)
	 * @see #uppertitleback(String)
	 * @see #lowertitleback(String)
	 */
	public Latex dedication(String dedication) {
		this.dedication = dedication;
		dedicationSet = true;
		return maketitle();
	}

	/**
	 * Adds a chapter title.
	 * 
	 * @param chapter the chapter title
	 * @param label   the label of the chapter, gets {@value #LABEL_NAMESPACE} prefixed
	 * @return the LaTeX object
	 */
	public Latex chapter(String chapter, String label) {
		return add("\\chapter{" + chapter + "}" + (STRING_IS_NOT_BLANK.test(label) ? "\\label{" + LABEL_NAMESPACE + label + "}" : ""));
	}

	/**
	 * Adds a chapter title.
	 * 
	 * @param chapter the chapter title
	 * @return the LaTeX object
	 */
	public Latex chapter(String chapter) {
		return chapter(chapter, null);
	}

	/**
	 * Adds a section title.
	 * 
	 * @param section the section title
	 * @param label   the label of the section, gets {@value #LABEL_NAMESPACE} prefixed
	 * @return the LaTeX object
	 */
	public Latex section(String section, String label) {
		return add("\\section{" + section + "}" + (STRING_IS_NOT_BLANK.test(label) ? "\\label{" + LABEL_NAMESPACE + label + "}" : ""));
	}

	/**
	 * Adds a section title.
	 * 
	 * @param section the section title
	 * @return the LaTeX object
	 */
	public Latex section(String section) {
		return section(section, null);
	}

	/**
	 * Adds a subsection title.
	 * 
	 * @param subsection the subsection title
	 * @param label      the label of the subsection, gets {@value #LABEL_NAMESPACE} prefixed
	 * @return the LaTeX object
	 */
	public Latex subsection(String subsection, String label) {
		return add("\\subsection{" + subsection + "}" + (STRING_IS_NOT_BLANK.test(label) ? "\\label{" + LABEL_NAMESPACE + label + "}" : ""));
	}

	/**
	 * Adds a subsection title.
	 * 
	 * @param subsection the subsection title
	 * @return the LaTeX object
	 */
	public Latex subsection(String subsection) {
		return subsection(subsection, null);
	}

	/**
	 * Adds a subsubsection title.
	 * 
	 * @param subsubsection the subsubsection title
	 * @param label         the label of the subsubsection, gets {@value #LABEL_NAMESPACE} prefixed
	 * @return the LaTeX object
	 */
	public Latex subsubsection(String subsubsection, String label) {
		return add("\\subsubsection{" + subsubsection + "}" + (STRING_IS_NOT_BLANK.test(label) ? "\\label{" + LABEL_NAMESPACE + label + "}" : ""));
	}

	/**
	 * Adds a subsubsection title.
	 * 
	 * @param subsubsection the subsubsection title
	 * @return the LaTeX object
	 */
	public Latex subsubsection(String subsubsection) {
		return subsubsection(subsubsection, null);
	}
}

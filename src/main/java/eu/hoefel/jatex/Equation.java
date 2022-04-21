package eu.hoefel.jatex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class for handling all sorts of equations.
 * 
 * @author Udo Hoefel
 */
public final class Equation implements Texable {

    /** The namespace used for equations in labels. */
    public static final String LABEL_NAMESPACE = "eq:";

    private List<Object> input = new ArrayList<>();
    private List<Boolean> endLines = new ArrayList<>();
    private List<LatexPackage> packages = new ArrayList<>();
    private List<LatexPreambleEntry> preambleExtras = new ArrayList<>();

    /** The environment in which to put the equation. */
    private EquationEnvironment env;
    private boolean isStarred;
    private boolean useSubequations;
    private String label = null;

    /** True if this equation is the outermost one. */
    private boolean outermost = true;

    /**
     * Only relevant for {@link EquationEnvironment#ALIGNAT} and {@link EquationEnvironment#ALIGNEDAT}. Needs to
     * be set explicitly via {@link Equation#equationColumns(int)}.
     */
    private int equationColumns = 0;

    /**
     * The available math environments.
     * 
     * @see <a href="http://mirrors.ctan.org/macros/latex/required/amsmath/amsldoc.pdf">AMS User Guide</a>
     * @author Udo Hoefel
     */
    public enum EquationEnvironment {

        /** The equation environment is for a single equation. */
        EQUATION(true, false, false),

        /**
         * The align environment is used for two or more equations when vertical
         * alignment is desired; usually binary relations such as equal signs are
         * aligned. To have several equation columns side-by-side, use extra ampersands
         * to separate the columns. For example use something along the lines of:
         * <p>
         * <code>
         * eq.add("x&amp;=y       &amp; X&amp;=Y       &amp; a&amp;=b+c");<br>
         * eq.add("x'&amp;=y'     &amp; X'&amp;=Y'     &amp; a'&amp;=b");<br>
         * eq.add("x+x'&amp;=y+y' &amp; X+X'&amp;=Y+Y' &amp; a'b&amp;=c'b");<br>
         * </code>
         * <p>
         * Line-by-line annotations on an equation can be done by judicious application
         * of \text inside an align environment:
         * <p>
         * <code>
         * eq.add("x&amp; = y_1-y_2+y_3-y_5+y_8-\dots &amp;&amp; \text{by Theorem 1}");<br>
         * eq.add("&amp; = y'\circ y^* &amp;&amp; \text{by whatever}");<br>
         * eq.add("&amp; = y(0) y' &amp;&amp; \text{by Axiom 1.}");<br>
         * </code>
         */
        ALIGN(true, false, false),

        /**
         * Similar to {@link #ALIGN}, except that the total width is the actual width of
         * the contents; thus it can be used as a component in a containing expression.
         * For example:
         * <p>
         * <code>
         * \begin{equation*}<br>
         * \left.\begin{aligned}<br>
         * B'&amp;=-\partial\times E,\\<br>
         * E'&amp;=\partial\times B - 4\pi j,<br>
         * \end{aligned}<br>
         * \right\} \qquad \text{Maxwell's equations}<br>
         * \end{equation*}
         * </code>
         * 
         * @see #ALIGN
         */
        ALIGNED(false, true, false),

        /**
         * The gather environment is used for a group of consecutive equations when
         * there is no alignment desired among them; each one is centered separately
         * within the text width. Equations inside gather are separated by each calling
         * {@link Equation#add(String)}. Any equation in a gather may consist of another
         * Equation with the {@link #SPLIT} environment.
         */
        GATHER(true, false, false),

        /**
         * Similar to {@link #GATHER}, except that the total width is the actual width
         * of the contents; thus it can be used as a component in a containing
         * expression. An example is shown for {@link #ALIGNED}.
         * 
         * @see #GATHER
         * @see #ALIGNED
         */
        GATHERED(false, true, false),

        /**
         * A variant environment to {@link #ALIGN}. Allows the horizontal space between
         * equations to be explicitly specified. This environment takes one argument,
         * the number of "equation columns": count the maximum number of &amp;s
         * (ampersands) in any row, add 1 and divide by 2. The "equation columns" have
         * to be set explicitly via {@link Equation#equationColumns(int)}.
         * 
         * @see #ALIGN
         * @see Equation#equationColumns(int)
         */
        ALIGNAT(true, false, true),

        /**
         * Similar to {@link #ALIGNAT}, except that the total width is the actual width
         * of the contents; thus it can be used as a component in a containing
         * expression. An example is shown for {@link #ALIGNED}. The "equation columns"
         * have to be set explicitly via {@link Equation#equationColumns(int)}.
         * 
         * @see #ALIGNAT
         * @see #ALIGNED
         */
        ALIGNEDAT(false, true, true),

        /**
         * Cases constructions are common in mathematics. You can use it like this:
         * <p>
         * <code>
         * eq2 = Equation.eq().env(.cases);<br>
         * eq2.add("0&amp; \text{if $r-j$ is odd},");<br>
         * eq2.add("r!\,(-1)^{(r-j)/2}&amp; \text{if $r-j$ is even}.");<br>
         * eq.add(eq2);
         * </code>
         */
        CASES(false, true, false),

        /**
         * In a flalign environment the fields alternately flush right and then
         * flushleft. Ampersands are used to separate the flushing regions. Example:
         * <p>
         * <code>
         * &nbsp;&nbsp;&amp; RPC &amp; {}={} &amp; A+B\tilde{f} +C x &amp;&nbsp;&nbsp;&nbsp;<br>
         *   &gt; &nbsp;&nbsp;&lt;&lt;&lt;&nbsp;&nbsp;&nbsp;&gt;&gt;&gt;&gt;&gt;&nbsp;&nbsp;&nbsp;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&nbsp;&nbsp;&nbsp;&gt;
         * </code>
         * <p>
         * The {@code<>} indicate herein the direction in which the flushing takes
         * place.
         */
        FLALIGN(true, false, false),

        /**
         * The multline environment is a variation of the {@link #EQUATION} environment
         * used for equations that don't fit on a single line. The first line of a
         * multline will be at the left margin and the last line at the right margin,
         * except for an indention on both sides in the amount of \multlinegap . Any
         * additional lines in between will be centered independently within the display
         * width (unless the fleqn option is passed to the amsmath package). Like
         * {@link #EQUATION} , multline has only a single equation number (thus, none of
         * the individual lines should be marked with \notag). The equation number is
         * placed on the last line (reqno option for the amsmath package) or first line
         * (leqno option for the amsmath package); vertical centering as for
         * {@link #SPLIT} is not supported by multline .
         */
        MULTLINE(true, false, false),

        /**
         * Like {@link #MULTLINE}, the split environment is for single equations that
         * are too long to fit on one line and hence must be split into multiple lines.
         * Unlike {@link #MULTLINE}, however, the split environment provides for
         * alignment among the split lines, using &amp; to mark alignment points. Unlike
         * the other amsmath equation structures, the split environment provides no
         * numbering, because it is intended to be used only inside some other displayed
         * equation structure, usually an {@link #EQUATION}, {@link #ALIGN}, or
         * {@link #GATHER}, which provides the numbering. The split structure should
         * constitute the entire body of the enclosing structure, apart from commands
         * like \label that produce no visible material.
         */
        SPLIT(false, true, false);

        private boolean canBeStarred;
        private boolean mathModeOnly;
        private boolean hasEquationColumns;

        /**
         * Constructor for the equation environments.
         * 
         * @param canBeStarred       true if the environment can be starred
         * @param mathModeOnly       true if the environment can only be used inside
         *                           another math environment
         * @param hasEquationColumns true if the environment has "equation columns"
         *                           (alignat and alignedat)
         */
        EquationEnvironment(boolean canBeStarred, boolean mathModeOnly, boolean hasEquationColumns) {
            this.canBeStarred = canBeStarred;
            this.mathModeOnly = mathModeOnly;
            this.hasEquationColumns = hasEquationColumns;
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        /**
         * True if this math environment can be used in the starred version (and thus
         * prevents the use of numbers to label the equation(s), e.g. (3))
         * 
         * @return true if environment can be starred
         */
        public boolean canBeStarred() {
            return canBeStarred;
        }

        /**
         * True if this math environment can only be used inside another math
         * environment.
         * 
         * @return true if the environment can only be used inside another math
         *         environment
         */
        public boolean mathModeOnly() {
            return mathModeOnly;
        }

        /**
         * True if the environment defines "equation columns".
         * 
         * @return true if the environment defines "equation columns"
         */
        public boolean hasEquationColumns() {
            return hasEquationColumns;
        }
    }

    /** Constructor with default required packages. */
    public Equation() {
        packages.add(new LatexPackage("amsmath", "breqn", Equation.class));
        environment(EquationEnvironment.EQUATION, false);
    }

    /**
     * Gets the environment used for this Equation.
     * 
     * @return the (main) environment for this Equation
     */
    public EquationEnvironment getEnvironment() {
        return env;
    }

    /**
     * Sets the environment used for this Equation.
     * 
     * @param env       the (main) environment for this Equation
     * @param isStarred determines whether the starred version of the environment is
     *                  used (starred means no numbering of equations)
     * @return the Equation object
     */
    public Equation environment(EquationEnvironment env, boolean isStarred) {
        this.env = env;
        if (env.canBeStarred()) {
            this.isStarred = isStarred;
        } else {
            this.isStarred = false;
        }
        return this;
    }

    /**
     * Sets the environment used for this Equation object (unstarred version).
     * 
     * @param env the (main) environment for this Equation
     * @return the Equation object
     */
    public Equation environment(EquationEnvironment env) {
        return environment(env, false);
    }

    /**
     * Gets whether the starred or unstarred version of the
     * {@link EquationEnvironment} is used.
     * 
     * @return true if the starred version of the environment is used (starred means
     *         no numbering of equations)
     */
    public boolean isStarred() {
        return isStarred;
    }

    /**
     * Determines if the starred or unstarred version of the
     * {@link EquationEnvironment} is used.
     * 
     * @param isStarred determines whether the starred version of the environment is
     *                  used (starred means no numbering of equations)
     * @return the Equation object
     */
    public Equation starred(boolean isStarred) {
        this.isStarred = isStarred;
        return this;
    }

    /**
     * Gets the label for this equation, without {@value #LABEL_NAMESPACE} prepended
     * (which is prepended internally).
     * 
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label for this equation. {@value #LABEL_NAMESPACE} is prepended
     * internally.
     * 
     * @param label the label
     * @return the Equation object
     */
    public Equation label(String label) {
        this.label = label;
        return this;
    }

    /**
     * Specifies the number of "equation columns", only relevant for
     * {@link EquationEnvironment#ALIGNAT} and
     * {@link EquationEnvironment#ALIGNEDAT}.
     * 
     * @param columns the number of "equation columns"
     * @return the Equation object
     * @see EquationEnvironment#ALIGNAT
     * @see EquationEnvironment#ALIGNEDAT
     */
    public Equation equationColumns(int columns) {
        equationColumns = columns;
        return this;
    }

    /**
     * Add a line of code for the equation.
     * 
     * @param line    the line to add
     * @param endLine if true end this line, else don't
     * @return the Equation object
     */
    public Equation add(String line, boolean endLine) {
        input.add(line);
        endLines.add(endLine);
        return this;
    }

    /**
     * Add a line of code for the equation. The line is automatically ended.
     * 
     * @param line the line to add
     * @return the Equation object
     */
    public Equation add(String line) {
        return add(line, true);
    }

    /**
     * Add an equation to the equation.
     * 
     * @param eq      the equation to add
     * @param endLine if true end this line, else don't
     * @return the Equation object
     */
    public Equation add(Equation eq, boolean endLine) {
        if (!eq.getEnvironment().mathModeOnly()) {
            throw new IllegalArgumentException("Cannot use a " + eq.env + " environment inside a " + env
                    + " environment. Maybe you wanted to use parse(Equation eq) instead of add(Equation eq)?");
        }
        input.add(eq);
        endLines.add(endLine);
        return this;
    }

    /**
     * Add an equation to the equation. The line is automatically ended.
     * 
     * @param eq the equation to add
     * @return the Equation object
     */
    public Equation add(Equation eq) {
        return add(eq, true);
    }

    /**
     * Parses the Equation object and replaces most info, like the data previously
     * added via add(...). If you want to <em>add</em> an Equation, please use
     * {@link #add(Equation, boolean)} or {@link #add(Equation)}.
     * 
     * @param eq the Equation to parse
     * @return the Equation object
     */
    public Equation parse(Equation eq) {
        environment(eq.getEnvironment(), eq.isStarred());
        useSubequations(eq.useSubequations);
        label(eq.getLabel());

        packages.addAll(eq.neededPackages());
        preambleExtras.addAll(eq.preambleExtras());
        input = eq.input;

        return this;
    }

    /**
     * Adds a line of text to the equation.
     * 
     * @param text the text to add
     * @return the Equation object
     */
    public Equation intertext(String text) {
        add("\\intertext{%s}".formatted(text));
        return this;
    }

    /**
     * Adds lines of code to the equation.
     * 
     * @param lines the lines of code to add
     * @return the Equation object
     */
    public Equation add(String... lines) {
        for (String line : lines) add(line, true);
        return this;
    }

    /**
     * Puts the whole equation inside a subequations environment if true, changing
     * the numbering: Causes all numbered equations within that part of the document
     * to be numbered (4.9a) (4.9b) (4.9c) ..., if the preceding numbered equation
     * was (4.8).
     * 
     * @param useSubequations if true use the subequations environment
     * @return the Equation object
     */
    public Equation useSubequations(boolean useSubequations) {
        this.useSubequations = useSubequations;
        return this;
    }

    /**
     * Puts the whole equation inside a subequations environment, changing the
     * numbering: Causes all numbered equations within that part of the document to
     * be numbered (4.9a) (4.9b) (4.9c) ..., if the preceding numbered equation was
     * (4.8).
     * 
     * @return the Equation object
     */
    public Equation useSubequations() {
        return useSubequations(true);
    }

    /**
     * Indicates to {@link Latex} that these packages and options are needed.
     * 
     * @param packages the needed packages
     * @return the Equation object
     */
    public Equation usePackages(LatexPackage... packages) {
        this.packages.addAll(List.of(packages));
        return this;
    }

    /**
     * Adds options to a package.
     * 
     * @param packageName the package to add the option to
     * @param options     the options
     * @return the Equation object
     */
    public Equation usePackageWithOptions(String packageName, Map<String, String> options) {
        packages.add(new LatexPackage(packageName, options));
        return this;
    }

    @Override
    public List<LatexPackage> neededPackages() {
        return List.copyOf(packages);
    }

    @Override
    public List<LatexPreambleEntry> preambleExtras() {
        return preambleExtras;
    }

    @Override
    public List<String> latexCode() {
        // to make sure the user didn't put garbage in (starred environments where they
        // cannot exist)
        environment(env, isStarred);
        if (isStarred) label(null);
        if (equationColumns <= 0 && (env == EquationEnvironment.ALIGNAT || env == EquationEnvironment.ALIGNEDAT)) {
            throw new IllegalArgumentException(
                    "You did not specifiy a valid number of columns (specified: " + equationColumns + ") for " + env.toString());
        }
        // ======

        List<String> ret = new ArrayList<>();
        int n = 1;
        if (useSubequations && outermost) {
            ret.add(Latex.indent(n) + "\\begin{subequations}"
                    + (label != null ? "\\label{" + LABEL_NAMESPACE + label + "}" : ""));
            n++;
        }

        if ((env.mathModeOnly() && !outermost) || outermost) {
            ret.add(Latex.indent(n) + "\\begin{" + env.toString() + (isStarred ? "*" : "") + "}"
                    + (env.hasEquationColumns() ? "{" + equationColumns + "}" : "") + "%");
        }

        // ======
        // this is required for pretty formatting the tex file (the labels resp. \\
        // aligned)
        List<Integer> spaces = new ArrayList<>();
        for (Object obj : input) {
            if (obj instanceof Equation) {
                spaces.add(0);
            } else if (obj instanceof String s) {
                spaces.add((Latex.indent(n + 1) + s).length());
            }
        }
        int correctLength = 0;
        correctLength = spaces.stream().mapToInt(Integer::intValue).max().getAsInt() + 1;
        List<String> spaceCorrection = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            Object obj = input.get(i);
            if (obj instanceof Equation) {
                spaceCorrection.add("");
            } else if (obj instanceof String s) {
                spaceCorrection.add(" ".repeat(correctLength - spaces.get(i)));
            }
        }
        // ======

        int labelIndex = 0;
        for (int i = 0; i < input.size(); i++) {
            boolean useLabel = ((endLines.get(i) && i != input.size() - 1) || (i == input.size() - 1))
                    && label != null
                    && !env.mathModeOnly();

            Object obj = input.get(i);
            String currLabel = "\\label{%s%s-%d}".formatted(LABEL_NAMESPACE, label, labelIndex);

            if (obj instanceof Equation eq) {
                eq.useSubequations(false).outermost = false;

                packages.addAll(eq.neededPackages());
                preambleExtras.addAll(eq.preambleExtras());

                for (String line : eq.latexCode()) {
                    ret.add(Latex.indent(n) + line);
                }
                if (useLabel) {
                    ret.add(Latex.indent(n + 1) + currLabel);
                    labelIndex++;
                }
            } else if (obj instanceof String s) {
                String str = s.replace("\t", "").trim();
                if (str.startsWith("\\intertext") && str.endsWith("}")) {
                    useLabel = false;
                    endLines.set(i, false);
                }

                ret.add(Latex.indent(n + 1) 
                        + s
                        + spaceCorrection.get(i)
                        + (useLabel ? currLabel : "") 
                        + (Boolean.TRUE.equals(endLines.get(i)) && i != input.size() - 1 ? "\\\\%" : ""));

                if (useLabel) labelIndex++;
            }
        }

        if ((env.mathModeOnly() && !outermost) || outermost) {
            ret.add(Latex.indent(n) + "\\end{" + env.toString() + (isStarred ? "*" : "") + "}%");
        }
        if (useSubequations && outermost) ret.add(Latex.indent(n - 1) + "\\end{subequations}");
        return ret;
    }
}

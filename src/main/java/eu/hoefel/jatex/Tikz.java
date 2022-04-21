package eu.hoefel.jatex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import eu.hoefel.utils.Strings;

/**
 * Class for handling tikzpictures to be included in {@link Latex}
 * documents.
 * 
 * @author Udo Hoefel
 * 
 * @see Figure#tikz(Tikz)
 */
public final class Tikz implements Texable {

    private List<String> options = new ArrayList<>();
    private List<String> lines = new ArrayList<>();
    private String fname = null;
    private List<LatexPackage> packages = new ArrayList<>();
    private List<LatexPreambleEntry> preambleEntries = new ArrayList<>();

    /** Constructor that uses some defaults (needed packages). */
    public Tikz() {
        usePackages(new LatexPackage("tikz"), new LatexPackage("pgf"));
    }

    /**
     * Puts the Plot in a tikzpicture.
     * 
     * @param plot the Plot
     * @return the Tikz object
     */
    public Tikz plot(PgfPlots plot) {
        packages.addAll(plot.neededPackages());
        preambleEntries.addAll(plot.preambleExtras());
        lines.addAll(plot.latexCode());
        return this;
    }

    /**
     * The filename to externalize the tikzpicture to, if
     * {@link Latex#externalize(String)} or {@link Latex#externalize()} is used.
     * 
     * @param fname the filename
     * @return the Tikz object
     */
    public Tikz filename(String fname) {
        this.fname = fname;
        return this;
    }

    /**
     * Adds lines of code to the tikzpicture.
     * 
     * @param lines the lines of code
     * @return the Tikz object
     */
    public Tikz add(String... lines) {
        Collections.addAll(this.lines, lines);
        return this;
    }

    /**
     * Adds options to the tikzpicture.
     * 
     * @param options the tikzpicture options
     * @return the Tikz object
     */
    public Tikz addOptions(String... options) {
        for (String option : options) {
            if (!this.options.contains(option)) this.options.add(option);
        }
        return this;
    }

    /**
     * Defines a node.
     * 
     * @param name    the name of the node
     * @param label   the label for the node
     * @param options the node options
     * @return the Tikz object
     */
    public Tikz node(String name, String label, String... options) {
        return node(name, null, label, options);
    }

    /**
     * Defines a node.
     * 
     * @param name    the name of the node
     * @param at      the coordinates of the ndoe
     * @param label   the label for the node
     * @param options the node options
     * @return the Tikz object
     */
    public Tikz node(String name, String at, String label, String... options) {
        add("\\node[%s] %s(%s) {%s};".formatted(String.join(",", options), (Latex.STRING_IS_NOT_BLANK.test(at) ? "at (" + at + ") " : ""), name, label));
        return this;
    }

    /**
     * Draws the specified commands.
     * 
     * @param cmd     the commands that should be drawn, e.g. "(0,0) -- (1,1)"
     * @param options the draw options
     * @return the Tikz object
     */
    public Tikz draw(String cmd, String... options) {
        add("\\draw [%s] %s;".formatted(String.join(",", options), cmd));
        return this;
    }

    /**
     * Defines a path.
     * 
     * @param cmd     the commands that describe the path, e.g. "(0,0) -- (1,1)"
     * @param options the path options
     * @return the Tikz object
     */
    public Tikz path(String cmd, String... options) {
        add("\\path [%s] %s;".formatted(String.join(",", options), cmd));
        return this;
    }

    /**
     * Fills the shape defined by the given commands.
     * 
     * @param cmd     the commands that should be filled, e.g. "(0,0) -- (1,1) --
     *                (1,0) -- cycle"
     * @param options the fill options
     * @return the Tikz object
     */
    public Tikz fill(String cmd, String... options) {
        add("\\fill [%s] %s;".formatted(String.join(",", options), cmd));
        return this;
    }

    /**
     * Fills and draws the shape defined by the given commands.
     * 
     * @param cmd     the commands that should be filled and drawn, e.g. "(0,0) --
     *                (1,1) -- (1,0) -- cycle"
     * @param options the filldraw options
     * @return the Tikz object
     */
    public Tikz filldraw(String cmd, String... options) {
        add("\\filldraw [%s] %s;".formatted(String.join(",", options), cmd));
        return this;
    }

    /**
     * Creates a Tikz picture of the given plot.
     * 
     * @param plot the Plot to be contained in the Tikz picture
     * @return the Tikz object
     */
    public static Tikz of(PgfPlots plot) {
        return new Tikz().plot(plot);
    }

    /**
     * Requests these tikzlibraries to be loaded.
     * 
     * @param libraries the tikzlibraries
     * @return the Tikz object
     */
    public Tikz tikzlibraries(String... libraries) {
        preambleEntries.add(new LatexPreambleEntry("\\usetikzlibrary", Strings.mapOf(libraries), false));
        return this;
    }

    /**
     * Requests these pgflibraries to be loaded.
     * 
     * @param libraries the pgflibraries
     * @return the Tikz object
     */
    public Tikz pgflibraries(String... libraries) {
        preambleEntries.add(new LatexPreambleEntry("\\usepgflibrary", Strings.mapOf(libraries), false));
        return this;
    }

    /**
     * Requests these pgfplotslibraries to be loaded.
     * 
     * @param libraries the pgfplotslibraries
     * @return the Tikz object
     */
    public Tikz pgfplotslibraries(String... libraries) {
        preambleEntries.add(new LatexPreambleEntry("\\usepgfplotslibrary", Strings.mapOf(libraries), false));
        return this;
    }

    /**
     * Requests these gdlibraries to be loaded.
     * 
     * @param libraries the gdlibraries
     * @return the Tikz object
     */
    public Tikz gdlibraries(String... libraries) {
        preambleEntries.add(new LatexPreambleEntry("\\usegdlibrary", Strings.mapOf(libraries), false));
        return this;
    }

    /**
     * Indicates to {@link Latex} that these packages and options are needed.
     * 
     * @param packages the needed packages
     * @return the Tikz object
     */
    public Tikz usePackages(LatexPackage... packages) {
        this.packages.addAll(List.of(packages));
        return this;
    }

    /**
     * Adds options to a package.
     * 
     * @param name    the package to add the option to
     * @param options the options
     * @return the Tikz object
     */
    public Tikz usePackageWithOptions(String name, Map<String, String> options) {
        packages.add(new LatexPackage(name, options));
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
        int outer = 1;

        List<String> ret = new ArrayList<>();
        if (fname != null) {
            ret.add(Latex.indent(outer) + "\\tikzsetnextfilename{" + fname + "}");
        }
        ret.add(Latex.indent(outer) + "\\begin{tikzpicture}");
        ret.add(Latex.indent(outer + 1) + "[");
        for (String option : options) {
            ret.add(Latex.indent(outer + 2) + option + ",");
        }
        ret.add(Latex.indent(outer + 1) + "]");
        ret.add("");
        for (String line : lines) {
            ret.add(Latex.indent(outer + 1) + line);
        }
        ret.add(Latex.indent(outer) + "\\end{tikzpicture}");

        return ret;
    }
}

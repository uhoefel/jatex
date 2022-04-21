package eu.hoefel.jatex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for handling more complex figures to be included in {@link Latex}
 * documents.
 * 
 * @author <a href="mailto:udo.hoefel@ipp.mpg.de">Udo Hoefel</a>
 * 
 * @see Latex#add(Texable...)
 */
public final class Figure implements Texable {

    /** The namespace used for figures in labels. */
    public static final String LABEL_NAMESPACE = "fig:";

    private String caption = "";
    private String captionShort = "";
    private boolean centering = true;
    private Tikz tikz;
    private String width = "";
    private String height = "";
    private String scale = "";
    private String position = "";
    private String breadth = "";
    private String path = "";
    private String label = null;
    private FigureEnvironment mode = FigureEnvironment.FIGURE;
    private FigureContent content = FigureContent.INCLUDEGRAPHICS;
    private Integer indentation = null;
    private Figure[] figs;
    private boolean isSubfigure;
    private List<String> postSubFigureCode = new ArrayList<>();
    private String[] subfigureWidths;

    private List<LatexPackage> packages = new ArrayList<>();
    private List<LatexPreambleEntry> preambleExtras = new ArrayList<>();

    /**
     * The possible type of a figure.
     * 
     * @author <a href="mailto:udo.hoefel@ipp.mpg.de">Udo Hoefel</a>
     */
    public enum FigureEnvironment {

        /** Standard <code>\begin{figure}...\end{figure}</code>. */
        FIGURE,

        /**
         * This type is used for floating text around pictures and requires two
         * additional parameters, position and breadth.<br>
         * <code>\begin{wrapfigure}{position}{breadth}...\end{wrapfigure}</code>.
         * 
         * @see Figure#position(String)
         * @see Figure#breadth(String)
         */
        WRAPFIGURE
    }

    /**
     * The possible contents of a figure.
     * 
     * @author <a href="mailto:udo.hoefel@ipp.mpg.de">Udo Hoefel</a>
     */
    private enum FigureContent {

        /**
         * Use the standard<br>
         * <code>\includegraphics[‹width›,‹height›,‹scale›]{‹path›}</code>.
         * 
         * @see Figure#width(String)
         * @see Figure#height(String)
         * @see Figure#scale(String)
         * @see Figure#path(String)
         */
        INCLUDEGRAPHICS,

        /**
         * Use the figure to set a tikzpicture.
         * 
         * @see Tikz
         */
        TIKZPICTURE
    }

    /**
     * Constructor that uses some defaults (needed packages, figure mode,
     * tikzpicture content, centered).
     */
    public Figure() {
        packages.add(new LatexPackage("caption"));
        centering(true);
        mode(FigureEnvironment.FIGURE);
        content = FigureContent.INCLUDEGRAPHICS;
    }

    /**
     * Gets the caption of the figure.
     * 
     * @return the caption
     */
    public String getCaption() {
        return caption;
    }

    /**
     * Sets the caption of the figure.
     * 
     * @param caption the caption
     * @return this Figure object
     */
    public Figure caption(String caption) {
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
     * Sets the short form of the caption, for example for the table of figures.
     * 
     * @param captionShort the short form
     * @return this Figure object
     */
    public Figure captionShort(String captionShort) {
        this.captionShort = captionShort;
        return this;
    }

    /**
     * Gets the label for this figure, without the {@value #LABEL_NAMESPACE}
     * attached.
     * 
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label for this figure and all eventually existing subfigures (sic!).
     * Will get {@value #LABEL_NAMESPACE} prepended.
     * 
     * @param label the label
     * @return this Figure object
     */
    public Figure label(String label) {
        this.label = label;
        return this;
    }

    /**
     * Gets whether this figures content is centered.
     * 
     * @return true if centered
     */
    public boolean isCentering() {
        return centering;
    }

    /**
     * Sets whether this figure should be centered.
     * 
     * @param centering true if you want the content centered
     * @return this Figure object
     */
    public Figure centering(boolean centering) {
        this.centering = centering;
        return this;
    }

    /**
     * Gets the tikzpicture.
     * 
     * @return the tikzpicture if set, otherwise null
     * @see Tikz
     */
    public Tikz getTikz() {
        return tikz;
    }

    /**
     * Sets a tikzpicture.
     * 
     * @param tikz the tikzpicture
     * @return this Figure object
     * @see Tikz
     */
    public Figure tikz(Tikz tikz) {
        content = FigureContent.TIKZPICTURE;
        packages.addAll(tikz.neededPackages());
        preambleExtras.addAll(tikz.preambleExtras());
        this.tikz = tikz;
        return this;
    }

    /**
     * Gets the aimed for width of the graphic to be included.
     * 
     * @return the width
     */
    public String getWidth() {
        return width;
    }

    /**
     * Sets the aimed for width of the graphic to be included.
     * 
     * @param width the width
     * @return this Figure object
     */
    public Figure width(String width) {
        this.width = width;
        scale = "";
        return this;
    }

    /**
     * Gets the aimed for height of the graphic to be included.
     * 
     * @return the height
     */
    public String getHeight() {
        return height;
    }

    /**
     * Sets the aimed for height of the graphic to be included.
     * 
     * @param height the height
     * @return this Figure object
     */
    public Figure height(String height) {
        this.height = height;
        scale = "";
        return this;
    }

    /**
     * Gets the aimed for scale of the graphic to be included.
     * 
     * @return the scale
     */
    public String getScale() {
        return scale;
    }

    /**
     * Sets the aimed for scale of the graphic to be included.
     * 
     * @param scale the scale
     * @return this Figure object
     */
    public Figure scale(String scale) {
        this.scale = scale;
        width = "";
        height = "";
        return this;
    }

    /**
     * Gets the position of the wrapfigure
     * 
     * @return the position
     * @see #position(String)
     * @see FigureEnvironment#WRAPFIGURE
     */
    public String getPosition() {
        return position;
    }

    /**
     * Can be used for figure and wrapfigures, however, with different allowed
     * inputs. For the figure case (best use none of them!): Allowed inputs:
     * <ul>
     * <li>! - really try to place figure where you specify
     * <li>h - try to place the figure "here"
     * <li>t - try to place the figure at the top
     * <li>b - try to place the figure at the bottom
     * <li>p - try to place the figure at a separate page
     * </ul>
     * 
     * For wrapfig the valid codes are different from regular figures. They come in
     * pairs: an uppercase version which allows the figure to float, and a lowercase
     * version that puts the figure <em>exactly here</em>.
     * <ul>
     * <li>r R - the right side of the text
     * <li>l L - the left side of the text
     * <li>i I - the inside edge - near the binding (if twosided document)
     * <li>o O - the outside edge - far from the binding
     * </ul>
     * 
     * @param position the position
     * @return this Figure object
     * @see FigureEnvironment#WRAPFIGURE
     */
    public Figure position(String position) {
        if (mode == FigureEnvironment.FIGURE 
                && (!"!".equals(position) 
                        && !"h".equals(position) 
                        && !"t".equals(position)
                        && !"b".equals(position) 
                        && !"p".equals(position))) {
            throw new IllegalArgumentException("Unknown position argument for figure");
        }
        if (mode == FigureEnvironment.WRAPFIGURE 
                && (!"r".equalsIgnoreCase(position)
                        && !"l".equalsIgnoreCase(position)
                        && !"i".equalsIgnoreCase(position)
                        && !"o".equalsIgnoreCase(position))) {
            throw new IllegalArgumentException("Unknown position argument for wrapfigure");
        }
        this.position = position;
        return this;
    }

    /**
     * Gets the width of the wrapfigure/subfigure.
     * 
     * @return the wrapfigure/subfigure width
     * @see FigureEnvironment#WRAPFIGURE
     */
    public String getBreadth() {
        return breadth;
    }

    /**
     * Sets the width of the wrapfigure/subfigure.
     * 
     * @param breadth the wrapfigure/subfigure width
     * @return this Figure object
     * @see FigureEnvironment#WRAPFIGURE
     */
    public Figure breadth(String breadth) {
        this.breadth = breadth;
        return this;
    }

    /**
     * Gets the path from which the graphic is loaded.
     * 
     * @return the graphic path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path from which the graphic is loaded.
     * 
     * @param path the graphic path
     * @return this Figure object
     */
    public Figure path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Gets the type of the figure.
     * 
     * @return the mode
     * @see FigureEnvironment
     */
    public FigureEnvironment getMode() {
        return mode;
    }

    /**
     * Sets the type of the figure.
     * 
     * @param mode the mode
     * @return this Figure object
     * @see FigureEnvironment
     */
    public Figure mode(FigureEnvironment mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Gets the aimed for size of the figure to be included.
     * 
     * @return the size
     */
    public String getSize() {
        String ret = "[";
        if (!"".equals(width)) {
            ret += "width=" + width;
        }
        if (!"".equals(height)) {
            ret += "height=" + height;
        }
        if (!"".equals(scale)) {
            ret += "scale=" + scale;
        }
        ret += "]";

        if ("[]".equals(ret)) {
            ret = "";
        }
        return ret;
    }

    /**
     * Sets one or multiple subfigures.
     * 
     * @param figs the figures to be included as subfigures
     * @return this Figure object
     */
    public Figure subfigures(Figure... figs) {
        for (Figure fig : figs) fig.isSubfigure = true;
        this.figs = figs;
        return this;
    }

    /**
     * Gets the subfigures.
     * 
     * @return the figures to be included as subfigures
     */
    public Figure[] getSubfigures() {
        return figs;
    }

    /**
     * Gets whether this figure is a subfigure.
     * 
     * @return true if this figure is a subfigure
     */
    public boolean isSubfigure() {
        return isSubfigure;
    }

    /**
     * Sets the indentation level.
     * 
     * @param indentation the number of indentations
     * @return this Figure object
     */
    public Figure indentationLevel(Integer indentation) {
        this.indentation = indentation;
        return this;
    }

    /**
     * Gets the indentation level.
     * 
     * @return the number of indentations
     */
    public Integer getIndentation() {
        return indentation;
    }

    /**
     * Adds code to be written after each subfigure. Length needs to be one shorter
     * than the number of subfigures, as the last one does not allow code to be
     * written after it.
     * 
     * @param code the code to add after each (except the last) subfigure
     * @return this Figure object
     */
    public Figure postSubFigureCode(String... code) {
        postSubFigureCode.clear();
        Collections.addAll(postSubFigureCode, code);
        return this;
    }

    /**
     * Gets the code to be written after each subfigure, except the last one.
     * 
     * @return the post subfigure code
     */
    private List<String> getPostSubFigureCodes() {
        if (postSubFigureCode.isEmpty()) {
            return postSubFigureCode;
        }

        if (postSubFigureCode.size() != figs.length - 1) {
            throw new IllegalStateException("# subfigures != # (post subfigures codes) - 1, with " + (figs.length - 1)
                    + "!= " + postSubFigureCode.size() + "");
        }
        return postSubFigureCode;
    }

    /**
     * Sets the widths of the subfigures.
     * 
     * @param widths the widths
     * @return this Figure object
     */
    public Figure subfigureWidths(String... widths) {
        subfigureWidths = widths;
        return this;
    }

    /**
     * Gets the widths of the subfigures.
     * 
     * @return the widths of each subfigure
     */
    public String[] getSubfigureWidths() {
        return subfigureWidths;
    }

    /**
     * Gets a new Figure in the specified environment.
     * 
     * @param env the environment to be used
     * @return a new Figure
     */
    public static Figure in(FigureEnvironment env) {
        return new Figure().mode(env);
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
        List<String> ret = new ArrayList<>();
        int n = 1;
        if (getIndentation() != null) {
            n = getIndentation();
        }

        switch (mode) {
            case FIGURE -> {
                String pos = "";
                if (!getPosition().equals("")) {
                    pos += "[" + getPosition() + "]";
                }
                if (isSubfigure()) {
                    ret.add(Latex.indent(n) + "\\begin{subfigure}" + pos + "{" + getBreadth() + "}" + "%");
                } else {
                    ret.add(Latex.indent(n) + "\\begin{figure}" + pos + "%");
                }
            }
            case WRAPFIGURE -> {
                packages.add(new LatexPackage("wrapfig"));
                ret.add(Latex.indent(n) + "\\begin{wrapfigure}{" + getPosition() + "}{" + getBreadth() + "}%");
            }
        }

        if (isCentering()) {
            ret.add(Latex.indent(n + 1) + "\\centering%");
        }

        Figure[] subFigs = getSubfigures();
        if (subFigs != null && subFigs.length > 0) {
            packages.add(new LatexPackage("subcaption", Map.of("hypcap", "true"), Map.of("subfig", Set.of(Figure.class))));
            packages.add(new LatexPackage("caption", Map.of("hypcap", "true")));

            List<String> postSubfigCodes = getPostSubFigureCodes();
            for (int i = 0; i < subFigs.length; i++) {
                packages.addAll(subFigs[i].neededPackages());
                preambleExtras.addAll(subFigs[i].preambleExtras());

                subFigs[i].mode(FigureEnvironment.FIGURE);
                subFigs[i].indentationLevel(n + 1);
                if (getLabel() != null) {
                    subFigs[i].label(getLabel() + "-" + i);
                }

                ret.addAll(subFigs[i].latexCode());
                if (i != subFigs.length - 1 && i < postSubfigCodes.size()) {
                    ret.add(Latex.indent(n + 1) + postSubfigCodes.get(i) + "%");
                }
            }
        } else {
            switch (content) {
                case INCLUDEGRAPHICS -> ret.add(Latex.indent(n + 1) + "\\includegraphics" + getSize() + "{" + getPath() + "}%");
                case TIKZPICTURE -> {
                    packages.addAll(getTikz().neededPackages());
                    preambleExtras.addAll( getTikz().preambleExtras());
                    List<String> code = getTikz().latexCode();
                    if (code != null) {
                        for (int i = 0; i < code.size(); i++) {
                            if (i == 0 || i == code.size() - 1) {
                                ret.add(Latex.indent(n + 1) + code.get(i) + "%");
                            } else {
                                ret.add(Latex.indent(n + 2) + code.get(i) + "%");
                            }
                        }
                    }
                }
            }
        }

        String fullCaption = Latex.indent(n + 1) + "\\caption";
        if (!getCaptionShort().equals("")) {
            fullCaption += "[" + getCaptionShort() + "]";
        }
        if (!getCaption().equals("")) {
            fullCaption += "{" + getCaption() + "}";
            ret.add(fullCaption + "%");
        }

        if (getLabel() != null) {
            ret.add(Latex.indent(n + 1) + "\\label{" + LABEL_NAMESPACE + getLabel() + "}%");
        }

        switch (mode) {
            case FIGURE     -> ret.add(Latex.indent(n) + "\\end{" + (isSubfigure() ? "sub" : "") + "figure}%");
            case WRAPFIGURE -> ret.add(Latex.indent(n) + "\\end{wrapfigure}%");
        }
        return ret;
    }
}

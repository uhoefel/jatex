package eu.hoefel.jatex;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.hoefel.jatex.Equation.EquationEnvironment;
import eu.hoefel.jatex.Figure.FigureEnvironment;
import eu.hoefel.jatex.Table.TableEnvironment;

/**
 * Examples showing some of the usecases of JateX.
 * 
 * @author Udo Hoefel
 */
class IntegrationTests {

    /**
     * Gets {@code n} linearly spaced steps from {@code x0} to {@code x1}.
     * 
     * @param x0 the start value
     * @param x1 the end value, inclusive
     * @param n  the number of steps
     * @return n steps from x0 to x1
     */
    private static final double[] linSpace(double x0, double x1, int n) {
        if (n == 1) return new double[] { (x0 + x1) / 2 };

        double[] f = new double[n];
        double dx = (x1 - x0) / (n - 1.0);
        for (int i = 0; i < n; i++) {
            f[i] = x0 + i*dx;
        }
        return f;
    }

    /**
     * Example showing two plots that do get externalized as well.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void plotExample1(@TempDir Path folder) {
        double[][] data1 = { linSpace(0, 1, 10), linSpace(-5,  5, 10) };
        double[][] data2 = { linSpace(0, 1, 10), linSpace( 5, -5, 10) };

        Latex tex = Latex.standard();
        tex.folder(folder.toString());
        tex.filename("plotExample1.tex");
        tex.documentclass("scrartcl");
        tex.plotData("testplot", data1, "some values", Map.of("xlabel", "$x$ values", "ylabel", "$y$ values"));
        tex.plotData("testplot", data2, "some values", Map.of("grid", "major"));
        tex.externalize();

        assertEquals(0, tex.exec());
    }

    /**
     * Example showing two datasets in one plot.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void plotExample2(@TempDir Path folder) {
        double[][] data1 = { linSpace(0, 1, 10), linSpace(-5, 5, 10) };
        double[][] data2 = { linSpace(0, 1, 10), linSpace(5, -5, 10) };
        double[][] data3d = { { 1,2,3,1,2,3,1,2,3 }, { 4,4,4,5,5,5,6,6,6 }, { 1,4,3,9,3,6,11,8,9 } };

        PgfPlots p1 = PgfPlots.of(data1, "blab", Map.of("dashed", ""))
                              .grid()
                              .plot(data2, "blu")
                              .plot("cos(deg(x))*x^2", "analytical", Map.of("mark", "none"))
                              .xlabel("$\\int x^\\alpha$ values")
                              .addOptions(Map.of("ylabel", "$y$ values"));
        assertEquals(0, p1.exec(folder + "plotExample2/plotExample2-2d.tex"));

        p1 = PgfPlots.of(data3d, "blab", Map.of("surf", ""))
                     .xlabel("$\\int x^\\alpha$ values")
                     .addOptions(Map.of("ylabel", "$y$ values"));
        assertEquals(0, p1.exec(folder + "plotExample2/plotExample2-3d.tex"));
    }

    /**
     * Example showing how to include an already existing picture.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void plotExample3(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "plotExample3");
        tex.filename("plotExample3.tex");
        tex.usePackageWithOption("graphicx", "demo");
        tex.addFigure("bla/123/3p.pdf", "testcapt.");

        assertEquals(0, tex.exec());
    }

    /**
     * Example showing how to include an already existing picture with a specific
     * width and label.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void plotExample4(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "plotExample4");
        tex.filename("plotExample4.tex");
        tex.documentclass("scrartcl");
        tex.usePackageWithOption("graphicx", "demo");
        tex.addFigure("bla/123/3p.pdf", "0.8\\textwidth", "testcapt.", "bla");

        assertEquals(0, tex.exec());
    }

    /**
     * Save a simple example of an equation environment.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void equationExample1(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "equationExample1");
        tex.filename("equationExample1.tex");
        tex.documentclass("article");
        tex.equation("x=y");

        assertEquals(0, tex.exec());
    }

    /**
     * Saves a pdf with an example of an align environment.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void equationExample2(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "equationExample2");
        tex.filename("equationExample2.tex");
        tex.documentclass("article");
        tex.add(new Equation().environment(EquationEnvironment.ALIGN)
                              .add("x &= y", "z &= \\int\\xi_i\\pi(x)\\,\\mathrm{d}x")
                              .label("boltzmann"));

        assertEquals(0, tex.exec());
    }

    /**
     * Creates an example pdf with a cases environment nested inside a starred align
     * environment (so no numbering and labels are useless).
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void equationExample3(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "equationExample3");
        tex.filename("equationExample3.tex");
        tex.documentclass("article");
        Equation eq = new Equation().environment(EquationEnvironment.CASES)
                                    .add("0& \\text{if $r-j$ is odd}",
                                            "r!\\,(-1)^{(r-j)/2}& \\text{if $r-j$ is even}.");
        tex.add(new Equation().environment(EquationEnvironment.ALIGN, true)
                              .add("z &= \\pi", false)
                              .add(eq));

        assertEquals(0, tex.exec());
    }

    /**
     * Creates a complex example pdf: A case nested in another case used within an
     * align environment.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void equationExample4(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "equationExample4");
        tex.filename("equationExample4.tex");
        tex.documentclass("article");
        Equation subeq1 = new Equation().environment(EquationEnvironment.CASES)
                                        .add("0 & \\text{if $r-j$ is odd}", true)
                                        .add("a & \\text{if $a=b$}", false);
        Equation subeq2 = new Equation().environment(EquationEnvironment.CASES)
                                        .add("u & \\text{if bla}", "o & \\sum\\xi_i");
        subeq1.add(subeq2);
        tex.add(new Equation().environment(EquationEnvironment.ALIGN, false)
                              .add("z &= \\pi", false)
                              .add(subeq1)
                              .label("maxwell"));

        assertEquals(0, tex.exec());
    }

    /**
     * Creates an example pdf in which the align environment is used in a
     * subequations environment, thus the numbering is (1a) (1b)... instead of (1)
     * (2)...
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void equationExample5(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "equationExample5");
        tex.filename("equationExample5.tex");
        tex.add(new Equation().environment(EquationEnvironment.ALIGN)
                              .add("x &= y", "z &= \\pi")
                              .label("boltzmann")
                              .useSubequations());

        assertEquals(0, tex.exec());
    }

    /**
     * Creates an example pdf with a simple, only partially filled table.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void tableExample1(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "tableExample1");
        tex.filename("tableExample1.tex");
        tex.add(new Table().format("c","l","l","l")
                           .midrule(0)
                           .entry(2, 3, "test")
                           .label("bla")
                           .caption("caption"));

        assertEquals(0, tex.exec());
    }

    /**
     * Creates an example pdf similar to the one created by
     * {@link #tableExample1(Path)}, except that the table is not centered.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void tableExample2(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "tableExample2");
        tex.filename("tableExample2.tex");
        tex.add(new Table().centering(false)
                           .format("c","l","l","l")
                           .midrule(0)
                           .entry(2, 3, "test")
                           .label("bla")
                           .caption("caption"));

        assertEquals(0, tex.exec());
    }

    /**
     * Creates an example pdf of a table that is filled a bit more, the top row is
     * specified in one method.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void tableExample3(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "tableExample3");
        tex.filename("tableExample3.tex");
        tex.add(new Table().format("c","l","l","l")
                           .midrule(0)
                           .row(0, "$x$", "y", "$T_\\mathrm{e}$ in keV")
                           .entry(2, 3, "test")
                           .color(2, 3, "red")
                           .label("bla")
                           .caption("caption"));

        assertEquals(0, tex.exec());
    }

    /**
     * Creates an example pdf with a longtable environment (so could go beyond 1
     * page) and coloring in one cell.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void tableExample4(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "tableExample4");
        tex.filename("tableExample4.tex");
        tex.add(new Table().environment(TableEnvironment.LONGTABLE)
                           .format("c","l","l","l")
                           .midrule(0)
                           .row(0, "$x$", "y", "$T_\\mathrm{e}$ in keV")
                           .entry(2, 3, "test")
                           .color(2, 2, "red")
                           .label("bla")
                           .caption("caption"));

        assertEquals(0, tex.exec());
    }

    /**
     * Creates an example pdf and shows how one can easily fill the rows/columns.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void tableExample5(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "tableExample5");
        tex.filename("tableExample5.tex");

        Table table = new Table();
        table.format("l", "c");

        for (int i = 0; i < 5; i++) {
            table.row(i, "A Entry" + i, "B Entry" + i);
            table.color(0, i, "red");
        }

        tex.add(table);

        assertEquals(0, tex.exec());
    }

    /**
     * Creates a figure and a wrapfigure.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void figureExample1(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "figureExample1");
        tex.filename("figureExample1.tex");

        tex.usePackageWithOption("graphicx", "demo");
        tex.usePackages("lipsum");

        Figure fig = Figure.in(FigureEnvironment.WRAPFIGURE)
                           .tikz(new Tikz())
                           .position("l")
                           .breadth("3cm")
                           .caption("aha")
                           .captionShort("lala")
                           .label("fgh")
                           .height("2cm")
                           .scale("0.1");

        tex.add(fig);
        tex.add("\\lipsum");
        tex.add(Figure.in(FigureEnvironment.FIGURE)
                      .path("path/to/file")
                      .caption("blabla")
                      .label("nameofpic")
                      .scale("0.1"));

        assertEquals(0, tex.exec());
    }

    /**
     * Creates a figure with two subfigures.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void figureExample2(@TempDir Path folder) {
        Latex tex = Latex.standard()
                        .folder(folder + "figureExample2")
                        .filename("figureExample2.tex");

        Figure fig = Figure.in(FigureEnvironment.FIGURE)
                        .tikz(new Tikz())
                        .position("h")
                        .breadth("3cm")
                        .caption("aha")
                        .captionShort("lala")
                        .label("fgh")
                        .height("2cm")
                        .scale("0.1");

        Figure fig2 = Figure.in(FigureEnvironment.FIGURE)
                        .tikz(new Tikz())
                        .position("h")
                        .breadth("3cm")
                        .caption("afgsw")
                        .captionShort("laela")
                        .label("fgefh")
                        .height("2cm");

        fig.subfigures(fig2, fig2);
        fig.postSubFigureCode("\\hfill");

        tex.add(fig);

        assertEquals(0, tex.exec());
    }

    /**
     * Adds a LaTeX document to another LaTeX document, where tex2 is parsed into
     * tex.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void latexExample1(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "latexExample1");
        tex.filename("latexExample1.tex");

        Latex tex2 = new Latex(); // no defaults for this one!
        tex2.add("This is a test");
        tex.add(tex2);

        assertEquals(0, tex.exec());
    }

    /**
     * Shows how to use bibliographies and citations.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void latexExample2(@TempDir Path folder) {
        String bibContent = """
                            @Book{Hartfuss2013,
                              author    = {Hartfu{\ss}, H. J. and Geist, T.},
                              title     = {Fusion Plasma Diagnostics with mm-Waves: An Introduction},
                              year      = {2013},
                              date      = {2013-09},
                              series    = {EBL-Schweitzer},
                              publisher = {Wiley},
                              isbn      = {9783527676248},
                              doi       = {10.1002/9783527676279},
                            } 
                            """;

        Path bibfile = assertDoesNotThrow(() -> Files.createTempFile(folder, "", ""));
        assertDoesNotThrow(() -> Files.write(bibfile, bibContent.getBytes(StandardCharsets.UTF_8)));

        Latex tex = Latex.standard();
        tex.folder(folder + "latexExample2");
        tex.filename("latexExample2.tex");
        tex.documentclass("article");
        tex.bibfile(bibfile.getFileName().toString());
        tex.add(Latex.cite("Hartfuss2013"));
        tex.bib();

        assertEquals(0, tex.exec());
    }

    /**
     * Shows an automatically generated title page.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void latexExample3(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "latexExample3");
        tex.filename("latexExample3.tex");
        tex.titlehead("Titlehead");
        tex.subject("subject");
        tex.title("Testtitle");
        tex.subtitle("Subtitle");
        tex.authors("John Doe", "Jane Doe\\thanks{some grant thing}");
        tex.date("\\today");
        tex.publisher("eseed");
        tex.extratitle("extratitle");
        tex.uppertitleback("uppertitleback");
        tex.lowertitleback("lowertitleback");
        tex.dedication("Dedicated to something");

        assertEquals(0, tex.exec());
    }

    /**
     * Shows an automatically generated title page for a typically double sided
     * class.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void latexExample4(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "latexExample4");
        tex.filename("latexExample4.tex");
        tex.documentclass("scrbook");
        tex.titlehead("Titlehead");
        tex.subject("subject");
        tex.title("Testtitle");
        tex.subtitle("Subtitle");
        tex.authors("John Doe", "Jane Doe\\thanks{some grant thing}");
        tex.date("\\today");
        tex.publisher("eseed");
        tex.extratitle("extratitle");
        tex.uppertitleback("uppertitleback");
        tex.lowertitleback("lowertitleback");
        tex.dedication("Dedicated to something");

        assertEquals(0, tex.exec());
    }

    /**
     * Shows an automatically generated title page for a non-KOMA script class, so
     * this is a bit work-aroundish, but still the cleanest way to get it
     * consistent, I think.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void latexExample5(@TempDir Path folder) {
        Latex tex = Latex.standard()
                         .folder(folder + "latexExample5")
                         .filename("latexExample5.tex")
                         .documentclass("book")
                         .titlehead("Titlehead")
                         .subject("subject")
                         .title("Testtitle")
                         .subtitle("Subtitle")
                         .authors("John Doe", "Jane Doe\\thanks{some grant thing}")
                         .date("\\today")
                         .publisher("eseed")
                         .extratitle("extratitle")
                         .uppertitleback("uppertitleback")
                         .lowertitleback("lowertitleback")
                         .dedication("Dedicated to something");

        assertEquals(0, tex.exec());
    }

    /**
     * Shows a quite complex document.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void latexExample6(@TempDir Path folder) {
        Latex tex = Latex.standard();
        tex.folder(folder + "latexExample6");
        tex.filename("latexExample6.tex");
        tex.titlehead("Titlehead");
        tex.usePackages("lipsum");
        tex.usePackageWithOption("graphicx", "demo");
        tex.subject("subject");
        tex.title("Testtitle");
        tex.subtitle("Subtitle");
        tex.authors("John Doe", "Jane Doe\\thanks{some grant thing}");
        tex.date("\\today");
        tex.publisher("eseed");
        tex.extratitle("extratitle");
        tex.uppertitleback("uppertitleback");
        tex.lowertitleback("lowertitleback");
        tex.dedication("Dedicated to something");
        tex.section("some section");
        tex.add("\\lipsum");
        tex.subsection("some subsection");
        tex.plotData("testplot", new double[][] { linSpace(0, 1, 10), linSpace(-5, 5, 10) },
                "some values", Map.of("xlabel", "$x$ values", "ylabel", "$y$ values"));
        tex.add("\\lipsum");
        tex.subsubsection("subsubsection bla");

        tex.add("\\lipsum");
        Figure fig = new Figure();
        fig.mode(FigureEnvironment.FIGURE);
        Tikz tikz = new Tikz();
        fig.tikz(tikz);
        fig.position("h");
        fig.breadth("3cm");
        fig.caption("aha");
        fig.captionShort("lala");
        fig.label("fgh");
        fig.height("2cm");
        fig.scale("0.1");

        Figure fig2 = new Figure();
        fig2.mode(FigureEnvironment.FIGURE);
        fig2.path("path/to/file");
        fig2.position("h");
        fig2.breadth("0.49\\textwidth");
        fig2.caption("afgsw");
        fig2.captionShort("laela");
        fig2.label("fgefh");
        fig2.height("2cm");

        fig.subfigures(fig2, fig2);
        fig.postSubFigureCode("\\hfill");

        tex.add(fig);
        tex.add("\\lipsum");

        tex.section("nfkaub");
        tex.add("\\lipsum");

        assertEquals(0, tex.exec());
    }

    /**
     * Complex document intended foe examples with custom defaults (but that part is
     * commented out).
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void latexExample7(@TempDir Path folder) {
        Latex tex = Latex.standard();

        tex.folder(folder + "latexExample7");
        tex.filename("latexExample7.tex");

        tex.documentclass("scrbook");

        tex.usePackages("lipsum");
        tex.usePackageWithOption("graphicx", "demo");
        tex.chapter("some chapter", "chap1");
        tex.section("some section");
        tex.add("\\lipsum");
        tex.subsection("some subsection");
        tex.plotData("testplot", new double[][] { linSpace(0, 1, 10), linSpace(-5, 5, 10) },
                "some values", Map.of("xlabel", "$x$ values", "ylabel", "$y$ values"));
        tex.add("\\lipsum");
        tex.subsubsection("subsubsection bla");

        tex.add("\\lipsum");
        Figure fig = new Figure();
        fig.mode(FigureEnvironment.FIGURE);
        Tikz tikz = new Tikz();
        fig.tikz(tikz);
        fig.position("h");
        fig.breadth("3cm");
        fig.caption("aha");
        fig.captionShort("lala");
        fig.label("fgh");
        fig.height("2cm");
        fig.scale("0.1");

        Figure fig2 = new Figure();
        fig2.mode(FigureEnvironment.FIGURE);
        fig2.path("path/to/file");
        fig2.position("h");
        fig2.breadth("0.49\\textwidth");
        fig2.caption("afgsw");
        fig2.captionShort("laela");
        fig2.label("fgefh");
        fig2.height("2cm");

        fig.subfigures(fig2, fig2);
        fig.postSubFigureCode("\\hfill");

        tex.add(fig);
        tex.add("\\lipsum");

        tex.section("nfkaub");
        tex.add("\\lipsum");
        tex.add(Latex.ref("sec:chap1"));

        assertEquals(0, tex.exec());
    }
}

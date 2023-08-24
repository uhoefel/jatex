package eu.hoefel.jatex.letter;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Map;
import java.util.Objects;

import eu.hoefel.jatex.Latex;
import eu.hoefel.jatex.LatexPackage;
import eu.hoefel.jatex.TexCompiler;
import eu.hoefel.jatex.Texable;

/**
 * Class for easily writing letters using KOMA.
 * 
 * @author Udo Hoefel
 */
public final class KomaLetter {
    private Texable user;
    private boolean showbank = true;
    private boolean smaller = true;
    private boolean clean = true;
    private String[] cleanupFileExtensions = new String[0];
    private String foldmarks = "TBMPL";
    private String toName;
    private String toStreet;
    private String toCity;
    private String toExtra;
    private String language;

    private String title;
    private String subject;
    private String yourRef;
    private String myRef;
    private String yourMail;
    private String customer;
    private String invoice;
    private LocalDate date;
    private String opening;
    private String closing;
    private String ps;
    private String[] encl;
    private String[] cc;

    private String[] body;

    private Path file;

    /** Hiding any public constructor. */
    private KomaLetter() {
        //no-op
    }

    private Latex setup() {
        return new Latex()
            .compiler(TexCompiler.LUALATEX)
            .documentclassWithOptions("scrlttr2", Map.of("version", "last"))

            .usePackages("babel")
    
            .usePackageWithOptions("microtype",
                    Map.of("activate", "{true,nocompatibility}",
                            "final", "",
                            "tracking", "true"))

            .usePackageWithOptions("siunitx",
                    Map.of("locale", "DE",
                           "separate-uncertainty", "",
                           "per-mode", "fraction"))

            .usePackageWithOptions("csquotes",
                    Map.of("strict", "true",
                            "autostyle", "true",
                            "german", "guillemets"))

            .usePackages("xcolor",
                        "datetime2",
                        "selnolig",
                        "phonenumbers",
                        "marvosym",
                        "ifthen")

            .usePackageWithOption("hyperref", "hidelinks")

            .addToPreamble(Latex.MINOR_SEPARATOR)
            .addToPreamble("% layout definitions")
            .addToPreamble(Latex.MINOR_SEPARATOR)

            .addToPreamble("\\KOMAoptions{fromphone=on,fromrule=aftername,fromemail=on,foldmarks="+(foldmarks != null ? foldmarks : "off")+"}")
            .addToPreamble("\\SetTracking{encoding={*},shape=sc}{40}")
            .addToPreamble(Latex.EMPTY_LINE)
            .addToPreamble("\\newboolean{showbank}")
            .addToPreamble("\\setboolean{showbank}{false}")
            .addToPreamble("\\newboolean{smaller}")
            .addToPreamble("\\setboolean{smaller}{false}")
            .addToPreamble("\\setkomafont{fromname}{\\scshape \\LARGE} ")
            .addToPreamble("\\setkomafont{backaddress}{\\mdseries}")
            .addToPreamble(Latex.EMPTY_LINE)
            .addToPreamble("\\makeatletter")
            .addToPreamble("\\newcommand{\\layout}{\\ifthenelse{\\boolean{smaller}}{")
            .addToPreamble(Latex.indent(1) + "\\@setplength{firstheadvpos}{17mm}%")
            .addToPreamble(Latex.indent(1) + "\\@setplength{firstfootvpos}{275mm}% Abstand des Footers von oben")
            .addToPreamble(Latex.indent(1) + "\\@setplength{locwidth}{70mm}% Breite des Locationfeldes")
            .addToPreamble(Latex.indent(1) + "\\@setplength{locvpos}{55mm}% Abstand des Locationfeldes von oben")
            .addToPreamble(Latex.indent(1) + "\\@setplength{foldmarkhpos}{6.5mm}%")
            .addToPreamble(Latex.indent(1) + "}{}}")
            .addToPreamble("\\makeatother%")
            .addToPreamble(Latex.EMPTY_LINE)
            .addToPreamble("\\setkomavar{firsthead}{%")
            .addToPreamble(Latex.indent(1) + "\\usekomavar{fromlogo}\\hfill\\scshape\\LARGE\\usekomavar{fromname}\\\\")
            .addToPreamble(Latex.indent(1) + "\\rule[3pt]{\\textwidth}{.4pt}%")
            .addToPreamble("}")
            .addToPreamble(Latex.EMPTY_LINE)
            .addToPreamble("\\setkomavar{firstfoot}{\\footnotesize%")
            .addToPreamble(Latex.indent(1) + "\\rule[3pt]{\\textwidth}{.4pt}\\\\")
            .addToPreamble(Latex.indent(1) + "\\begin{tabular}[t]{l@{}}%")
            .addToPreamble(Latex.indent(2) + "\\usekomavar{fromname}\\\\")
            .addToPreamble(Latex.indent(2) + "\\usekomavar{fromaddress}\\\\")
            .addToPreamble(Latex.indent(1) + "\\end{tabular}%")
            .addToPreamble(Latex.indent(1) + "\\hfill")
            .addToPreamble(Latex.indent(1) + "\\begin{tabular}[t]{l@{}}%")
            .addToPreamble(Latex.indent(2) + "\\usekomavar{fromphone}\\\\")
            .addToPreamble(Latex.indent(2) + "\\usekomavar{fromemail}\\\\")
            .addToPreamble(Latex.indent(1) + "\\end{tabular}%")
            .addToPreamble(Latex.indent(1) + "\\ifthenelse{\\boolean{showbank}}{\\ifkomavarempty{frombank}{}{%")
            .addToPreamble(Latex.indent(2) + "\\hfill")
            .addToPreamble(Latex.indent(2) + "\\begin{tabular}[t]{r@{}}%")
            .addToPreamble(Latex.indent(3) + "\\usekomavar{frombank}")
            .addToPreamble(Latex.indent(2) + "\\end{tabular}%")
            .addToPreamble(Latex.indent(2) + "}}{}%")
            .addToPreamble("}")
            .addToPreamble(Latex.EMPTY_LINE)
            .addToPreamble("\\renewcommand*{\\raggedsignature}{\\raggedright}")
            .addToPreamble(Latex.MINOR_SEPARATOR)
            .addToPreamble(Latex.EMPTY_LINE);
    }

    /**
     * Creates a new KomaLetter bound to file.
     * 
     * @param file the file to save to
     * @return the new KomaLetter
     * @deprecated Use {@link #as(Path)}
     */
    @Deprecated(forRemoval = true, since = "1.3.2")
    public static KomaLetter as(File file) {
        return new KomaLetter().file(file);
    }

    /**
     * Creates a new KomaLetter bound to file.
     * 
     * @param file the file to save to
     * @return the new KomaLetter
     */
    public static KomaLetter as(Path file) {
        return new KomaLetter().file(file);
    }

    /**
     * Creates a new KomaLetter bound to file.
     * 
     * @param file the file to save to
     * @return the new KomaLetter
     */
    public static KomaLetter as(String file) {
        return KomaLetter.as(new File(file));
    }

    /**
     * Creates a new KomaLetter bound to file.
     * 
     * @param file the file to save to
     * @return the new KomaLetter
     * @deprecated Use {@link #file(Path)}
     */
    @Deprecated(forRemoval = true, since = "1.3.2")
    public KomaLetter file(File file) {
        this.file = file == null ? null : file.toPath();
        return this;
    }

    /**
     * Sets the file to save to.
     * 
     * @param file the file to save to
     * @return the current KOMA letter instance
     */
    public KomaLetter file(Path file) {
        this.file = file;
        return this;
    }

    /**
     * Whether to clean files with given file extensions. If no file extensions are given, a default set of them will be removed.
     * 
     * @param clean {@code true} if helper files should be deleted
     * @param exts  the additional extensions to remove, e.g. {@code "tex"} if you
     *              want to have the tex file removed after generating the pdf
     * @return the new KomaLetter
     */
    public KomaLetter clean(boolean clean, String... exts) {
        this.clean = clean;
        this.cleanupFileExtensions = Objects.requireNonNullElse(exts, new String[0]);
        return this;
    }

    /**
     * Sets the title of the letter.
     * 
     * @param title the title
     * @return the current KOMA letter instance
     */
    public KomaLetter title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the subject of the letter.
     * 
     * @param subject the subject
     * @return the current KOMA letter instance
     */
    public KomaLetter subject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Allows to put in user specific definitions, like an IBAN.
     * 
     * @param user the class with defaults
     * @return the current KOMA letter instance
     */
    public KomaLetter user(Texable user) {
        this.user = user;
        return this;
    }

    /**
     *Sets whether to show the bank information.
     * 
     * @param showbank if true, show the bank info
     * @return the current KomaLetter
     * @deprecated Wrong name, use {@link #showBank(boolean)}
     */
    @Deprecated(forRemoval = true, since = "1.3.4")
    public KomaLetter showbank(boolean showbank) {
        return showBank(showbank);
    }

    /**
     *Sets whether to show the bank information.
     * 
     * @param showBank if true, show the bank info
     * @return the current KomaLetter
     */
    public KomaLetter showBank(boolean showBank) {
        this.showbank = showBank;
        return this;
    }

    /**
     *Sets whether to show foldmarks, and which. See the corresponding chapter in the KOMAScript manual.
     * Default is {@code 'TBMPL'}.
     * <p>
     * To deactivate the fold marks use {@code null}.
     * 
     * @param foldmarks if true, show foldmarks
     * @return the current KomaLetter
     */
    public KomaLetter foldmarks(String foldmarks) {
        this.foldmarks = foldmarks;
        return this;
    }

    /**
     * Sets the language of the letter (this language is used for babel, so e.g. for
     * hyphenation). Note that for german you should use "ngerman".
     * 
     * @param language the language
     * @return the current KOMA letter instance
     */
    public KomaLetter language(String language) {
        this.language = language;
        return this;
    }

    /**
     * Adds the body of the letter.
     * 
     * @param body the letter body, each string corresponds to one paragraph
     * @return the current KOMA letter instance
     */
    public KomaLetter write(String... body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the recipient's name.
     * 
     * @param toName the name of the recipient
     * @return the current KOMA letter instance
     */
    public KomaLetter toName(String toName) {
        this.toName = toName;
        return this;
    }

    /**
     * Sets the street name and number of the recipient's address.
     * 
     * @param toStreet the street
     * @return the current KOMA letter instance
     */
    public KomaLetter toStreet(String toStreet) {
        this.toStreet = toStreet;
        return this;
    }

    /**
     * Sets the city name and potentially the postal code of the recipient's address.
     * 
     * @param toCity the city, may include postal code
     * @return the current KOMA letter instance
     */
    public KomaLetter toCity(String toCity) {
        this.toCity = toCity;
        return this;
    }

    /**
     * Sets extra recipient address information.
     * 
     * @param toExtra additional recipient address information
     * @return the current KOMA letter instance
     */
    public KomaLetter toExtra(String toExtra) {
        this.toExtra = toExtra;
        return this;
    }

    /**
     * Sets the reference of the recipient.
     * 
     * @param yourRef the reference of the recipient
     * @return the current KOMA letter instance
     */
    public KomaLetter yourRef(String yourRef) {
        this.yourRef = yourRef;
        return this;
    }

    /**
     * Sets your reference.
     * 
     * @param myRef your reference
     * @return the current KOMA letter instance
     */
    public KomaLetter myRef(String myRef) {
        this.myRef = myRef;
        return this;
    }

    /**
     * Sets the date of the reference letter that the recipient of the current mail sent previously.
     * 
     * @param date the date of the reference letter from the recipient 
     * @return the current KOMA letter instance
     */
    public KomaLetter yourMail(String date) {
        this.yourMail = date;
        return this;
    }

    /**
     * Sets the customer number.
     * 
     * @param customer the customer number
     * @return the current KOMA letter instance
     */
    public KomaLetter customer(String customer) {
        this.customer = customer;
        return this;
    }

    /**
     * Sets the invoice number.
     * 
     * @param invoice the invoice number
     * @return the current KOMA letter instance
     */
    public KomaLetter invoice(String invoice) {
        this.invoice = invoice;
        return this;
    }

    /**
     * Sets the date of the letter. Not necessary if the date of today should be used.
     * 
     * @param date the letter date
     * @return the current KOMA letter instance
     * @deprecated Use {@link #date(LocalDate)}
     */
    @Deprecated(forRemoval = true, since = "1.3.2")
    public KomaLetter date(Calendar date) {
        this.date = date == null ? null : LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return this;
    }

    /**
     * Sets the date of the letter. Not necessary if the date of today should be used.
     * 
     * @param date the letter date
     * @return the current KOMA letter instance
     */
    public KomaLetter date(LocalDate date) {
        this.date = date;
        return this;
    }

    /**
     * Adds the opening line, typically something along the lines of "Dear XYZ".
     * 
     * @param opening the letter opening
     * @return the current KOMA letter instance
     */
    public KomaLetter opening(String opening) {
        this.opening = opening;
        return this;
    }

    /**
     * Defines the closing of the letter, typically something along the lines of
     * "Best regards", "Yours faithfully" etc.
     * 
     * @param closing the letter closing
     * @return the current KOMA letter instance
     */
    public KomaLetter closing(String closing) {
        this.closing = closing;
        return this;
    }

    /**
     * Adds a postscriptum.
     * 
     * @param ps the postscriptum
     * @return the current KOMA letter instance
     */
    public KomaLetter ps(String ps) {
        this.ps = ps;
        return this;
    }

    /**
     * Sets which things, typically other documents, are enclosed.
     * 
     * @param encl the enclosed things
     * @return the current KOMA letter instance
     */
    public KomaLetter encl(String... encl) {
        this.encl = encl;
        return this;
    }

    /**
     * Sets the people intended to receive a copy.
     * 
     * @param cc the people intended to receive a copy
     * @return the current KOMA letter instance
     */
    public KomaLetter cc(String... cc) {
        this.cc = cc;
        return this;
    }

    /**
     * Generates the letter corresponding to the LaTeX content.
     * 
     * @return the error code, i.e. 0 if the execution terminated normally and &gt;1
     *         if an error occurred
     */
    public int exec() {
        if (date == null) {
            date = LocalDate.now();
        }

        String address = (toStreet == null ? "" : toStreet + "\\\\")
                + (toCity == null ? "" : toCity + "\\\\")
                + (toExtra == null ? "" : toExtra);

        String locale = user.neededPackages().stream()
                            .filter(p -> p.name().equals("babel"))
                            .findFirst()
                            .orElse(new LatexPackage("babel")) // package without the option we are interested in
                            .options()
                            .getOrDefault("main", "english");

        if (language != null) {
            locale = language;
        }

        Latex tex = setup().folder(file.getParent().toString())
                           .filename(file.getFileName().toString());

        if (user != null) {
            tex.add(user);
        }

        tex.usePackageWithOptions("babel", Map.of("main", locale))
           .addToPreamble(Latex.MINOR_SEPARATOR)
           .addToPreamble("% mail specific settings")
           .addToPreamble(Latex.MINOR_SEPARATOR)
           .addToPreamble("\\setboolean{showbank}{" + showbank + "}")
           .addToPreamble("\\setboolean{smaller}{" + smaller + "}")
           .addToPreamble(Latex.EMPTY_LINE)
           .addToPreamble("\\setkomavar{toname}{" + (toName == null ? "" : toName) + "}")
           .addToPreamble("\\setkomavar{toaddress}{" + address + "}")
           .addToPreamble(Latex.EMPTY_LINE)
           .addToPreamble("\\setkomavar{title}{" + (title == null ? "" : title) + "}")
           .addToPreamble("\\setkomavar{subject}{" + (subject == null ? "" : subject) + "}")
           .addToPreamble("\\setkomavar{yourref}{" + (yourRef == null ? "" : yourRef) + "}")
           .addToPreamble("\\setkomavar{yourmail}{" + (yourMail == null ? "" : yourMail) + "}")
           .addToPreamble("\\setkomavar{myref}{" + (myRef == null ? "" : myRef) + "}")
           .addToPreamble("\\setkomavar{customer}{" + (customer == null ? "" : customer) + "}")
           .addToPreamble("\\setkomavar{invoice}{" + (invoice == null ? "" : invoice) + "}")
           .addToPreamble("\\setkomavar{date}{\\DTMdisplaydate{"+date.getYear()+"}{"+(date.getMonthValue())+"}{"+date.getDayOfMonth()+"}{-1}}")
           .addToPreamble(Latex.MINOR_SEPARATOR)
           .addToPreamble(Latex.EMPTY_LINE)
           .addToPreamble("\\layout")

           .add("\\begin{letter}{}");

        if (opening != null) {
            tex.add(Latex.indent(1) + "\\opening{" + opening + "}");
        }

        if (body != null) { 
            for (String line : body) {
                tex.add(Latex.indent(1) + line);
                tex.add("");
            }
        }

        if (closing != null) {
            tex.add(Latex.indent(1) + "\\closing{" + closing + "}");
        }

        if (ps != null) {
            tex.add(Latex.indent(1) + "\\ps " + ps);
        }

        if (encl != null) {
            tex.add(Latex.indent(1) + "\\encl{");
            for (String line : encl) {
                tex.add(Latex.indent(2) + line + "\\\\");
            }
            tex.add(Latex.indent(1) + "}");
        }

        if (cc != null) {
            tex.add(Latex.indent(1) + "\\cc{");
            for (String line : cc) {
                tex.add(Latex.indent(2) + line + "\\\\");
            }
            tex.add(Latex.indent(1) + "}");
        }

        tex.add("\\end{letter}");
        tex.add("");

        tex.clean(clean, cleanupFileExtensions);

        return tex.exec();
    }
}

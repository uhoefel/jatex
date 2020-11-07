package eu.hoefel.jatex.letter;

import java.util.List;
import java.util.Map;

import eu.hoefel.jatex.Latex;
import eu.hoefel.jatex.LatexPackage;
import eu.hoefel.jatex.LatexPreambleEntry;
import eu.hoefel.jatex.Texable;

/**
 * Gets the example of user specific settings for a virtual John Doe of the
 * {@link KomaLetter} class.
 * 
 * @author Udo Hoefel
 */
class JohnDoe implements Texable {

	/** Private constructor, one should use the static method, {@link #defaults()}. */
	private JohnDoe() {
		// private as there can be only one :-)
	}

	/**
	 * The default settings for a {@link KomaLetter} for John Doe.
	 * 
	 * @return the {@link KomaLetter} defaults
	 */
	public static Texable defaults() {
		return new JohnDoe();
	}

	@Override
	public List<LatexPackage> neededPackages() {
		return List.of(new LatexPackage("babel", Map.of("main", "english")),
						new LatexPackage("fontspec"),
						new LatexPackage("phonenumbers"));
	}

	@Override
	public List<LatexPreambleEntry> preambleExtras() {
		return List.of(
			Latex.MINOR_SEPARATOR,
			new LatexPreambleEntry("% font settings"),
			Latex.MINOR_SEPARATOR,
			new LatexPreambleEntry("\\IfFontExistsTF{Minion Pro}{\\setmainfont[Numbers={OldStyle,Proportional},Ligatures={Common,TeX},SmallCapsFeatures={Renderer=Basic}]{Minion Pro}}{}"),
			new LatexPreambleEntry("\\IfFontExistsTF{Myriad Pro}{\\setsansfont[Numbers={OldStyle,Proportional},Ligatures={Common,TeX},SmallCapsFeatures={Renderer=Basic}]{Myriad Pro}}{}"),
			new LatexPreambleEntry("\\IfFontExistsTF{Consolas}{\\setmonofont[Scale=MatchLowercase]{Consolas}}{}"),
			Latex.MINOR_SEPARATOR,
			Latex.EMPTY_LINE,
			Latex.MINOR_SEPARATOR,
			new LatexPreambleEntry("% personal info"),
			Latex.MINOR_SEPARATOR,
			new LatexPreambleEntry("\\setkomavar{fromname}{John Doe}"),
			new LatexPreambleEntry("\\setkomavar{fromaddress}{Einestraße 1 № 12\\\\D-12345 Stuttgart}"),
			new LatexPreambleEntry("\\setkomavar{fromphone}{\\Mobilefone~\\phonenumber[country=DE,foreign=international,link=on]{015112345678}}"),
			new LatexPreambleEntry("\\setkomavar{fromemail}{\\Letter~john.doe@mail.com}"),
			new LatexPreambleEntry("\\setkomavar{frombank}{Volksbank Stuttgart\\\\IBAN:~\\textsc{de}12\\,3456\\,7890\\,1234\\,5678\\,90}"),
			new LatexPreambleEntry("\\setkomavar{backaddressseparator}{ -- }"),
			new LatexPreambleEntry("\\setkomavar{place}{Stuttgart}"),
			Latex.MINOR_SEPARATOR,
			Latex.EMPTY_LINE
		);
	}

	@Override
	public List<String> latexCode() {
		return List.of();
	}
}

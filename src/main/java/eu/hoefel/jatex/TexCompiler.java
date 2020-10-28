package eu.hoefel.jatex;

import java.util.Locale;

/**
 * Enums for the available LaTeX compilers.
 * 
 * @author <a href="mailto:udo.hoefel@ipp.mpg.de">Udo Hoefel</a>
 */
public enum TexCompiler {
	/** Use latex. */
	LATEX,

	/** Use pdflatex. */
	PDFLATEX,

	/** Use xetex. */
	XETEX,

	/**
	 * Use lualatex. This will be the successor of pdflatex, so you probably should
	 * use this.
	 */
	LUALATEX;

	/**
	 * Gets the name of the executable. This might differ from the {@link #name()}
	 * as some operating systems are case sensitive, while others are not.
	 * 
	 * @return the executable name
	 */
	public String executableName() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}

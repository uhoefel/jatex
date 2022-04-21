package eu.hoefel.jatex;

import java.util.List;

/**
 * Interface to be implemented by each class that should be understood by the
 * main {@link Latex} class.
 * 
 * @author Udo Hoefel
 */
public interface Texable {

    /**
     * Gets the needed packages, their options and indicates incompatibilities. Note
     * that the list of packages may contain duplicates. If you want to make sure
     * there are no duplicates call {@link LatexPackage#cleanup(List, boolean)}) on
     * the returned list.
     * 
     * @return the packages needed by this texable
     */
    public List<LatexPackage> neededPackages();

    /**
     * Gets the preamble entries, like e.g. values set via \pgfplotsset{}. Note that
     * the list of preamble entries may contain duplicates. If you want to make sure
     * there are no non-standalone duplicates call
     * {@link LatexPreambleEntry#cleanup(List)}) on the returned list.
     * 
     * @return the preamble extras needed by this texable
     */
    public List<LatexPreambleEntry> preambleExtras();

    /**
     * Gets the lines of LaTeX code, typically to be used in the body of a LaTeX
     * document.
     * 
     * @return the lines of code
     */
    public List<String> latexCode();
}

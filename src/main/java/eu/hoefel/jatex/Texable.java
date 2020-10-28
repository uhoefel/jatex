/*
 * JaTeX - A project to generate and run LaTeX code from within Java.
 * Copyright © 2018 Udo Höfel (udo.hoefel@arcor.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

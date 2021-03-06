/**
 * This module provides support for programmatic (generic) LaTeX document
 * creation, such as used for lab reports or letters.
 * <p>
 * The {@link eu.hoefel.jatex} is the package containing the base classes needed
 * for the document generation, as well as some convenience classes wrapping
 * often used LaTeX features, like e.g. {@link eu.hoefel.jatex.Table tables} and
 * {@link eu.hoefel.jatex.Equation equations}.
 * <p>
 * The {@link eu.hoefel.jatex.letter} is the package containing convenience
 * classes for the generation of letters. It depends on the
 * {@link eu.hoefel.jatex} package.
 * <p>
 * The {@link eu.hoefel.jatex.junit} is the package containing classes helping
 * with writing JUnit tests.
 * 
 * @author Udo Hoefel
 */
module eu.hoefel.jatex {
	exports eu.hoefel.jatex;
	exports eu.hoefel.jatex.letter;
	exports eu.hoefel.jatex.junit;
	
	opens eu.hoefel.jatex to org.junit.platform.commons;
	opens eu.hoefel.jatex.letter to org.junit.platform.commons;
	opens eu.hoefel.jatex.junit to org.junit.platform.commons;

	requires java.logging;
	requires java.base;
	requires org.junit.jupiter.api;
	requires eu.hoefel.utils;
}
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Test the main LaTeX class.
 * 
 * @author Udo Hoefel
 */
@SuppressWarnings("javadoc")
class LatexTests {

	@Test
	@DisplayName("Testing constructors")
	void constructor() {
		assertDoesNotThrow(() -> Latex.standard().show());
	}

	@Test
	@DisplayName("Testing executability")
	void executable() {
		assertTrue(Latex.isExecutable(TexCompiler.LUALATEX));
	}

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
//	@EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
	void plotExample1(@TempDir Path folder) {
		double[][] data1 = new double[][] { linSpace(0, 1, 10), linSpace(-5,  5, 10) };
		double[][] data2 = new double[][] { linSpace(0, 1, 10), linSpace( 5, -5, 10) };

		Latex tex = Latex.standard();
		tex.folder(folder.toString());
		tex.filename("plotExample1.tex");
		tex.documentclass("scrartcl");
		tex.plotData("testplot", data1, "some values", Map.of("xlabel", "$x$ values", "ylabel", "$y$ values"));
		tex.plotData("testplot", data2, "some values", Map.of("grid", "major"));
		tex.externalize();
		assertEquals(0, tex.exec());
	}
}

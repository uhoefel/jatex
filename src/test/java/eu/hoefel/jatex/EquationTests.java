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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import eu.hoefel.jatex.Equation.EquationEnvironment;

/**
 * Tests for the Equation class.
 * 
 * @author Udo Hoefel
 */
@SuppressWarnings("javadoc")
class EquationTests {

	@ParameterizedTest(name = "Testing getter/setter for environments")
	@EnumSource(EquationEnvironment.class)
	void environments(EquationEnvironment env) {
		Equation eq = new Equation().environment(env);
		assertEquals(env, eq.getEnvironment());
	}

	@Test
	@DisplayName("Testing labeling")
	void label() {
		String label = "testlabel";
		String expected = "\\label{%s%s-0}".formatted(Equation.LABEL_NAMESPACE, label);

		Equation eq = new Equation();
		eq.environment(EquationEnvironment.ALIGN);
		eq.label(label);
		eq.add("E &= mc^2");
		assertTrue(String.join("\n", eq.latexCode()).contains(expected));
		eq.label(null);
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.ALIGNAT);
		eq.label(label);
		eq.add("E &= mc^2");
		eq.equationColumns(2);
		assertTrue(String.join("\n", eq.latexCode()).contains(expected));
		eq.label(null);
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		// cannot have labels
		eq = new Equation();
		eq.environment(EquationEnvironment.ALIGNED);
		eq.label(label);
		eq.add("E &= mc^2");
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.ALIGNEDAT);
		eq.label(label);
		eq.add("E &= mc^2");
		eq.equationColumns(2);
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.CASES);
		eq.label(label);
		eq.add("E &= mc^2");
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.EQUATION);
		eq.label(label);
		eq.add("E &= mc^2");
		assertTrue(String.join("\n", eq.latexCode()).contains(expected));
		eq.label(null);
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.FLALIGN);
		eq.label(label);
		eq.add("E &= mc^2");
		assertTrue(String.join("\n", eq.latexCode()).contains(expected));
		eq.label(null);
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.GATHER);
		eq.label(label);
		eq.add("E &= mc^2");
		assertTrue(String.join("\n", eq.latexCode()).contains(expected));
		eq.label(null);
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.GATHERED);
		eq.label(label);
		eq.add("E &= mc^2");
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.MULTLINE);
		eq.label(label);
		eq.add("E &= mc^2");
		assertTrue(String.join("\n", eq.latexCode()).contains(expected));
		eq.label(null);
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));

		eq = new Equation();
		eq.environment(EquationEnvironment.SPLIT);
		eq.label(label);
		eq.add("E &= mc^2");
		assertFalse(String.join("\n", eq.latexCode()).contains(expected));
	}

	@Test
	@DisplayName("Testing parsing")
	void parse() {
		String expectedEq = "a &= b";
		String expectedLabel = "different smth";
		
		Equation eq = new Equation();
		eq.environment(EquationEnvironment.EQUATION);
		eq.label("label");
		eq.add("E = mc^2");
		
		Equation eq2 = new Equation();
		eq2.environment(EquationEnvironment.ALIGN);
		eq2.label(expectedLabel);
		eq2.add(expectedEq);
		
		eq.parse(eq2);
		
		assertEquals(EquationEnvironment.ALIGN, eq.getEnvironment());
		assertEquals(expectedLabel, eq.getLabel());
		assertTrue(String.join("\n", eq.latexCode()).contains(expectedEq));
	}

	@Test
	@DisplayName("Testing inline equation")
	void addInlineEq() {
		List<String> expected = List.of(
				"    \\begin{equation}%",
				"        E = mc^2 ", 
				"        \\begin{cases}%",
				"            0 & \\text{if $r-j$ is odd}, \\\\%",
				"            1 & \\text{else},            ",
				"        \\end{cases}%",
				"        +\\xi     \\label{eq:label-0}",
				"    \\end{equation}%"
		);

		Equation eq = new Equation();
		eq.environment(EquationEnvironment.EQUATION);
		eq.label("label");
		eq.add("E = mc^2", false);
		
		Equation eq2 = new Equation();
		eq2.environment(EquationEnvironment.CASES);
		eq2.add("0 & \\text{if $r-j$ is odd},", "1 & \\text{else},");

		eq.add(eq2, false);
		eq.add("+\\xi");
		
		assertEquals(expected, eq.latexCode());
		
		eq = new Equation();
		eq.environment(EquationEnvironment.EQUATION);
		eq.label("label");
		eq.add("E = mc^2", false);

		eq2 = new Equation();
		eq2.environment(EquationEnvironment.EQUATION);
		eq2.add("0 & \\text{if $r-j$ is odd},", "1 & \\text{else},");
		
		Equation eq3 = eq;
		Equation eq4 = eq2;

		assertThrows(RuntimeException.class, ()->{ eq3.add(eq4, false); });

		expected = List.of(
				"    \\begin{equation}%",
				"        E = mc^2 ", 
				"        \\begin{cases}%",
				"            0 & \\text{if $r-j$ is odd}, \\\\%",
				"            1 & \\text{else},            ",
				"        \\end{cases}%",
				"        \\label{eq:label-0}",
				"    \\end{equation}%"
		);

		eq = new Equation();
		eq.environment(EquationEnvironment.EQUATION);
		eq.label("label");
		eq.add("E = mc^2", false);
		
		eq2 = new Equation();
		eq2.environment(EquationEnvironment.CASES);
		eq2.add("0 & \\text{if $r-j$ is odd},", "1 & \\text{else},");

		eq.add(eq2);

		assertEquals(expected, eq.latexCode());
	}

	@Test
	@DisplayName("Testing intertext")
	void addIntertext() {
		List<String> expected = List.of(
				"    \\begin{align}%",
				"        E &= mc^2                          \\label{eq:label-0}\\\\%", 
				"        \\intertext{Some intermediate text} ",
				"        &=\\xi                              \\label{eq:label-1}",
				"    \\end{align}%"
		);
		
		Equation eq = new Equation();
		eq.environment(EquationEnvironment.ALIGN);
		eq.label("label");
		eq.add("E &= mc^2");
		eq.intertext("Some intermediate text");
		eq.add("&=\\xi");

		assertEquals(expected, eq.latexCode());
	}

	@Test
	@DisplayName("Testing subequations")
	void subequation() {
		
		List<String> expected = List.of(
				"    \\begin{subequations}\\label{eq:label}",
				"        \\begin{align}%",
				"            E &= mc^2                          \\label{eq:label-0}\\\\%", 
				"            \\intertext{Some intermediate text} ",
				"            &=\\xi                              \\label{eq:label-1}",
				"        \\end{align}%",
				"    \\end{subequations}"
		);

		Equation eq = new Equation();
		eq.environment(EquationEnvironment.ALIGN);
		eq.label("label");
		eq.useSubequations();
		eq.add("E &= mc^2");
		eq.intertext("Some intermediate text");
		eq.add("&=\\xi");

		assertEquals(expected, eq.latexCode());

		expected = List.of(
				"    \\begin{subequations}",
				"        \\begin{align}%",
				"            E &= mc^2                          \\\\%", 
				"            \\intertext{Some intermediate text} ",
				"            &=\\xi                              ",
				"        \\end{align}%",
				"    \\end{subequations}"
		);

		eq = new Equation();
		eq.environment(EquationEnvironment.ALIGN);
		eq.label(null);
		eq.useSubequations();
		eq.add("E &= mc^2");
		eq.intertext("Some intermediate text");
		eq.add("&=\\xi");

		assertEquals(expected, eq.latexCode());
	}

	@Test
	@DisplayName("Testing starred versions")
	void star() {
		List<String> expected = List.of(
				"    \\begin{align*}%",
				"        E &= mc^2                          \\\\%", 
				"        \\intertext{Some intermediate text} ",
				"        &=\\xi                              ",
				"    \\end{align*}%"
		);

		Equation eq = new Equation();
		eq.environment(EquationEnvironment.ALIGN, true);
		eq.label("label");
		eq.add("E &= mc^2");
		eq.intertext("Some intermediate text");
		eq.add("&=\\xi");

		assertEquals(expected, eq.latexCode());

		eq = new Equation();
		eq.environment(EquationEnvironment.ALIGN);
		eq.starred(true);
		eq.label("label");
		eq.add("E &= mc^2");
		eq.intertext("Some intermediate text");
		eq.add("&=\\xi");

		assertEquals(expected, eq.latexCode());
	}

	@Test
	@DisplayName("Testing equation columns")
	void equationColumns() {
		Equation eq = new Equation();
		eq.environment(EquationEnvironment.ALIGNAT, true);
		eq.label("label");
		eq.equationColumns(-2);
		eq.add("E &= mc^2");
		eq.add("&=\\xi");
		
		// effectively final
		Equation eq2 = eq;

		assertThrows(RuntimeException.class, () -> { eq2.latexCode(); });

		// effectively final
		Equation eq3 = eq.environment(EquationEnvironment.ALIGNEDAT);

		assertThrows(RuntimeException.class, () -> { eq3.latexCode(); });
	}

	@DisplayName("Testing latex base class communication parts")
	@Test
	void baseCommunication() {
		Equation eq = new Equation();
		eq.usePackages(new LatexPackage("a"), new LatexPackage("b"));
		eq.usePackages(new LatexPackage("a")); // shouldn't change anything
		eq.usePackageWithOptions("c", Map.of("key", "val"));
		
		List<LatexPackage> needed = LatexPackage.cleanup(eq.neededPackages());
		assertTrue(() -> needed.stream().anyMatch(p -> "a".equals(p.name())));
		assertTrue(() -> needed.stream().anyMatch(p -> "b".equals(p.name())));
		assertTrue(() -> needed.stream().anyMatch(p -> "c".equals(p.name())));

		LatexPackage pckg = needed.stream().filter(p -> "c".equals(p.name())).findFirst().get();
		assertTrue(pckg.options().containsKey("key"));
		assertEquals("val", pckg.options().get("key"));

		eq.usePackageWithOptions("c", Map.of("key", "val2"));
		pckg = LatexPackage.cleanup(eq.neededPackages()).stream().filter(p -> "c".equals(p.name())).findFirst().get();
		assertTrue(pckg.options().containsKey("key"));
		assertEquals("val2", pckg.options().get("key"));
	}
}

package eu.hoefel.jatex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LatexPackage}.
 * 
 * @author Udo Hoefel
 */
@SuppressWarnings("javadoc")
class LatexPackageTests {

    @DisplayName("Checking package incompatibility warnings")
    @Test
    void testIncompatibility() {
        assertTrue(LatexPackage.checkForIncompatiblePackages(List.of(new LatexPackage("a", "b", LatexPackageTests.class), new LatexPackage("b"))));
        assertFalse(LatexPackage.checkForIncompatiblePackages(List.of(new LatexPackage("a", "b", LatexPackageTests.class), new LatexPackage("c"))));
    }
}

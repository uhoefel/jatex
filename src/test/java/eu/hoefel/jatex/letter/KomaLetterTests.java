package eu.hoefel.jatex.letter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import eu.hoefel.jatex.EnabledIfLatexExecutable;
import eu.hoefel.jatex.TexCompiler;

/**
 * Basic tests for letters.
 * 
 * @author Udo Hoefel
 */
class KomaLetterTests {

    /**
     * Example showcase letter.
     * 
     * @param folder the temporary folder to save the file to
     */
    @Test
    @EnabledIfLatexExecutable(compiler = TexCompiler.LUALATEX)
    void testLetter(@TempDir Path folder) {
        Path file = Paths.get(folder.toString(), "letter.tex");
        KomaLetter letter = KomaLetter.as(file.toString())
                                .language("ngerman")
                                .user(JohnDoe.defaults()) // fill in specifics for yourself
                                .toName("Mr. Bob Doe")
                                .cc("Jane Doe")
                                .toStreet("Wendelsteinstraße 1")
                                .toCity("D-12345 Entenhausen")
                                .toExtra("some extra info")
                                .yourMail("1970-01-01")
                                .yourRef("yourRef")
                                .myRef("myRef")
                                .invoice("123")
                                .customer("987")
                                .title("Title")
                                .subject("subject")
                                .opening("Dear Testreader,")
                                .write("This letter is not really a letter. I just test for example if the paragraph building works.",
                                        "Let's see\\ldots")
                                .closing("Mit freundlichen Grüßen,")
                                .ps("Here some postscriptum text")
                                .encl("Document1.pdf", "Document2.pdf");

        assertEquals(0, letter.exec());

        // does the tex file exist?
        assertTrue(Files.exists(file));

        String[] filenameParts = file.toFile().getName().split("\\.");
        File pdf = new File(file.toFile().getParentFile(), String.join(".", filenameParts[0], "pdf"));
        assertTrue(pdf.exists());
        assertTrue(pdf.isFile());
        assertFalse(pdf.isDirectory());
        assertTrue(pdf.length() > 0);
    }
}

package eu.hoefel.jatex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/** Annotation to check if a specific LaTeX compiler is executable. */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(LatexExecutableCondition.class)
public @interface EnabledIfLatexExecutable {

    /**
     * The compiler that is required to be executable.
     * 
     * @return the LaTeX compiler
     */
    TexCompiler compiler();
}
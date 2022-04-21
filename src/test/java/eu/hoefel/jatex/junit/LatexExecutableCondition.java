package eu.hoefel.jatex.junit;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.reflect.AnnotatedElement;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import eu.hoefel.jatex.Latex;

/**
 * The condition that tests whether a specified LaTeX compiler can be executed.
 * 
 * @author Udo Hoefel
 */
class LatexExecutableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        AnnotatedElement element = context.getElement().orElseThrow(IllegalStateException::new);
        return findAnnotation(element, EnabledIfLatexExecutable.class)
                .map(annotation -> disableIfUnreachable(annotation, element))
                .orElse(ConditionEvaluationResult.disabled("@EnabledIfReachable not used"));
    }

    /**
     * Determines whether the compiler is executable.
     * 
     * @param annotation the annotation
     * @param element    the annotated element
     * @return enabled if the compiler is executable, else disabled
     */
    private ConditionEvaluationResult disableIfUnreachable(EnabledIfLatexExecutable annotation, AnnotatedElement element) {
        if (Latex.isExecutable(annotation.compiler())) {
            return ConditionEvaluationResult.enabled("Enabled %s as %s is executable".formatted(element, annotation.compiler()));
        }
        return ConditionEvaluationResult.disabled("Disabled %s as %s is not executable".formatted(element, annotation.compiler()));
    }
}
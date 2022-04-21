package eu.hoefel.jatex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.logging.Logger;

/**
 * Record for describing a LaTeX package with the information needed to load it
 * appropriately.
 * 
 * @param name                 the package name, e.g. "babel"
 * @param options              the options as key-value pairs. Use null or an
 *                             empty string for the value if you have an option
 *                             that does not need a value, e.g. "open" for the
 *                             package "bookmark"
 * @param incompatiblePackages the packages with which the package is
 *                             incompatible. The accompanying set contains the
 *                             classes that request the current package to be
 *                             loaded to help with debugging.
 * 
 * @author Udo Hoefel
 */
public record LatexPackage(String name, Map<String, String> options, Map<String, Set<Class<?>>> incompatiblePackages) {

    private static final Logger logger = Logger.getLogger(LatexPackage.class.getName());

    /**
     * Creates the information of a LaTeX package needed to load it appropriately.
     * No options are used and no incompatibilities are registered.
     * 
     * @param name the package name, e.g. "babel"
     */
    public LatexPackage(String name) {
        this(name, (Map<String, String>) null, null);
    }

    /**
     * Creates the information of a LaTeX package needed to load it appropriately.
     * No incompatibilities are registered.
     * 
     * @param name   the package name, e.g. "babel"
     * @param option the package option, e.g. "open" for the package "bookmark". If
     *               you need a key-value option use
     *               {@link #LatexPackage(String, Map)}.
     */
    public LatexPackage(String name, String option) {
        this(name, Map.of(option, ""), null);
    }

    /**
     * Creates the information of a LaTeX package needed to load it appropriately.
     * No incompatibilities are registered.
     * 
     * @param name    the package name, e.g. "babel"
     * @param options the options as key-value pairs. Use null or an empty string
     *                for the value if you have an option that does not need a
     *                value, e.g. "open" for the package "bookmark"
     */
    public LatexPackage(String name, Map<String, String> options) {
        this(name, options, null);
    }

    /**
     * Creates the information of a LaTeX package needed to load it appropriately.
     * No options are used.
     * 
     * @param name                     the package name, e.g. "babel"
     * @param incompatiblePackage      the package with which the package is
     *                                 incompatible
     * @param classThatRequestsPackage the accompanying class that request the
     *                                 current package to be loaded to help with
     *                                 debugging
     */
    public LatexPackage(String name, String incompatiblePackage, Class<?> classThatRequestsPackage) {
        this(name, null, Map.of(incompatiblePackage, Set.of(classThatRequestsPackage)));
    }

    /**
     * Creates the information of a LaTeX package needed to load it appropriately.
     * 
     * @param name                 the package name, e.g. "babel"
     * @param options              the options as key-value pairs. Use null or an
     *                             empty string for the value if you have an option
     *                             that does not need a value, e.g. "open" for the
     *                             package "bookmark"
     * @param incompatiblePackages the packages with which the package is
     *                             incompatible. The accompanying set contains the
     *                             classes that request the current package to be
     *                             loaded to help with debugging.
     */
    public LatexPackage(String name, Map<String, String> options, Map<String, Set<Class<?>>> incompatiblePackages) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Package name may not be null/blank!");
        }

        this.name = name;
        if (options == null || options.isEmpty()) {
            this.options = Collections.emptyNavigableMap();
        } else {
            this.options = Map.copyOf(options);
        }

        if (incompatiblePackages == null || incompatiblePackages.isEmpty()) {
            this.incompatiblePackages = Collections.emptyNavigableMap();
        } else {
            Map<String, Set<Class<?>>> ip = new HashMap<>();
            for (Entry<String, Set<Class<?>>> incompatiblePackage : incompatiblePackages.entrySet()) {
                ip.put(incompatiblePackage.getKey(), Set.copyOf(incompatiblePackage.getValue()));
            }
            this.incompatiblePackages = Map.copyOf(ip);
        }
    }

    /**
     * Provides a simple and rough check whether you load incompatible packages.
     * This will not cover all cases and is only meant as a rough check.
     * 
     * @param packages the packages to check
     * @return true if probably incompatible packages have been found
     */
    public static final boolean checkForIncompatiblePackages(List<LatexPackage> packages) {
        boolean probablyIncompatible = false;
        for (LatexPackage pckg : packages) {
            for (LatexPackage p : packages) {
                if (pckg.incompatiblePackages().containsKey(p.name())) {
                    logger.warning(() -> String.format(Locale.ENGLISH,
                            "Probably incompatible packages found. "
                                    + "You used these classes: %s, which need the %s package, "
                                    + "which is (probably) incompatible with the %s package "
                                    + "that is used in your main document. I'll try to continue, "
                                    + "but this may very well break.",
                            pckg.incompatiblePackages().get(p.name()), p.name(), pckg.name()));
                    probablyIncompatible = true;
                }
            }
        }
        return probablyIncompatible;
    }

    /**
     * Cleans up the package list by merging duplicates. Override previously set
     * options.
     * 
     * @param packages the package list to remove duplicates from
     * @return the list of (unique) packages
     */
    public static final List<LatexPackage> cleanup(List<LatexPackage> packages) {
        return cleanup(packages, false);
    }

    /**
     * Cleans up the package list by merging duplicates.
     * 
     * @param packages       the package list to remove duplicates from
     * @param firstDominates if true, do not override options from subsequent
     *                       occurrences of the same package, but append where
     *                       possible. If false, override previously set options.
     * @return the list of (unique) packages
     */
    public static final List<LatexPackage> cleanup(List<LatexPackage> packages, boolean firstDominates) {
        LinkedHashMap<String, LatexPackage> cleanedPckgs = new LinkedHashMap<>();
        for (LatexPackage pckg : packages) {
            cleanedPckgs.putIfAbsent(pckg.name(), pckg);

            Map<String, String> o = new HashMap<>();
            Map<String, Set<Class<?>>> ip = new HashMap<>();

            if (firstDominates) {
                fill(cleanedPckgs, pckg, o::putIfAbsent, ip);
            } else {
                fill(cleanedPckgs, pckg, o::put, ip);
            }

            cleanedPckgs.put(pckg.name(), new LatexPackage(pckg.name(), o, ip));
        }
        return new ArrayList<>(cleanedPckgs.values());
    }

    /**
     * Fills the list of options and incompatible packages with the data from the
     * list of cleaned packages and the specified package.
     * 
     * @param cleanedPckgs      the (partially filled) list of cleaned packages
     * @param pckg              the package to extract the data for
     * @param options           the function used for filling an option map
     * @param incompatibilities the map to put in incompatibilities for the
     *                          specified package
     */
    private static final void fill(Map<String, LatexPackage> cleanedPckgs, LatexPackage pckg,
            BinaryOperator<String> options, Map<String, Set<Class<?>>> incompatibilities) {

        // put in already existing options
        for (Entry<String, String> option : cleanedPckgs.get(pckg.name()).options().entrySet()) {
            options.apply(option.getKey(), option.getValue());
        }

        // put in options from pckg
        for (Entry<String, String> option : pckg.options().entrySet()) {
            options.apply(option.getKey(), option.getValue());
        }

        // put in all incompatible classes
        LatexPackage p = cleanedPckgs.computeIfAbsent(pckg.name(), s -> pckg);
        for (var ip : p.incompatiblePackages().entrySet()) {
            Set<Class<?>> incompatibleClasses = incompatibilities.computeIfAbsent(ip.getKey(), s -> new HashSet<>(ip.getValue()));
            incompatibleClasses.addAll(ip.getValue());
        }
    }
}

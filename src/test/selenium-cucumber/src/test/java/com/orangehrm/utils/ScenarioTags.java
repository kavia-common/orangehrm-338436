package com.orangehrm.utils;

import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * Utility for classifying Cucumber scenarios by their tags.
 *
 * <h3>Flow name: ScenarioClassificationFlow</h3>
 * <p>Centralises tag-based scenario classification logic so that Hooks,
 * step definitions, and runners all use the same criteria. Prevents
 * scattered tag-matching conditionals.</p>
 *
 * <h3>Tag taxonomy</h3>
 * <ul>
 *   <li>{@code @smoke}, {@code @regression} — test suite scopes</li>
 *   <li>{@code @login}, {@code @ui}, {@code @critical} — UI test markers</li>
 *   <li>{@code @database}, {@code @db-validation} — database-only markers</li>
 *   <li>{@code @flaky} — known flaky tests (may be skipped or retried)</li>
 *   <li>{@code @sequential} — must not run in parallel</li>
 *   <li>{@code @wip} — work-in-progress, excluded from CI</li>
 * </ul>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Inputs:</b> Cucumber {@link Scenario} object.</li>
 *   <li><b>Outputs:</b> boolean classification results.</li>
 *   <li><b>Errors:</b> none — all methods are safe and return false on null input.</li>
 *   <li><b>Side-effects:</b> none.</li>
 * </ul>
 */
// PUBLIC_INTERFACE
public final class ScenarioTags {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioTags.class);

    /** Tags that indicate a scenario interacts with the database only. */
    private static final Set<String> DB_TAGS = Set.of("@database", "@db-validation");

    /** Tags that indicate a scenario uses the browser UI. */
    private static final Set<String> UI_TAGS = Set.of("@ui", "@login", "@regression", "@critical", "@smoke");

    /** Tag for known flaky scenarios. */
    private static final String FLAKY_TAG = "@flaky";

    /** Tag for scenarios that must run sequentially. */
    private static final String SEQUENTIAL_TAG = "@sequential";

    /** Tag for work-in-progress scenarios excluded from CI runs. */
    private static final String WIP_TAG = "@wip";

    private ScenarioTags() {
        // Utility class — no instantiation
    }

    /**
     * Check if the scenario is tagged as a database-only scenario
     * (has a DB tag but no UI tag).
     *
     * @param scenario the Cucumber scenario
     * @return true if the scenario is database-only
     */
    // PUBLIC_INTERFACE
    public static boolean isDatabaseOnly(Scenario scenario) {
        if (scenario == null) {
            return false;
        }
        Collection<String> tags = scenario.getSourceTagNames();
        boolean hasDbTag = tags.stream().anyMatch(t -> DB_TAGS.contains(t.toLowerCase()));
        boolean hasUiTag = tags.stream().anyMatch(t -> UI_TAGS.contains(t.toLowerCase()));
        return hasDbTag && !hasUiTag;
    }

    /**
     * Check if the scenario is tagged as flaky.
     *
     * @param scenario the Cucumber scenario
     * @return true if the scenario has the @flaky tag
     */
    // PUBLIC_INTERFACE
    public static boolean isFlaky(Scenario scenario) {
        if (scenario == null) {
            return false;
        }
        return scenario.getSourceTagNames().stream()
                .anyMatch(t -> t.equalsIgnoreCase(FLAKY_TAG));
    }

    /**
     * Check if the scenario must run sequentially (not in parallel).
     *
     * @param scenario the Cucumber scenario
     * @return true if the scenario has the @sequential tag
     */
    // PUBLIC_INTERFACE
    public static boolean isSequential(Scenario scenario) {
        if (scenario == null) {
            return false;
        }
        return scenario.getSourceTagNames().stream()
                .anyMatch(t -> t.equalsIgnoreCase(SEQUENTIAL_TAG));
    }

    /**
     * Check if the scenario is work-in-progress and should be excluded from CI.
     *
     * @param scenario the Cucumber scenario
     * @return true if the scenario has the @wip tag
     */
    // PUBLIC_INTERFACE
    public static boolean isWip(Scenario scenario) {
        if (scenario == null) {
            return false;
        }
        return scenario.getSourceTagNames().stream()
                .anyMatch(t -> t.equalsIgnoreCase(WIP_TAG));
    }

    /**
     * Check if the scenario requires a browser (UI interaction).
     *
     * @param scenario the Cucumber scenario
     * @return true if the scenario has any UI-related tag or is not database-only
     */
    // PUBLIC_INTERFACE
    public static boolean requiresBrowser(Scenario scenario) {
        if (scenario == null) {
            return true; // Default to requiring browser
        }
        return !isDatabaseOnly(scenario);
    }
}

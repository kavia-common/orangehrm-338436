package com.orangehrm.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Sequential test runner for scenarios that must not execute in parallel.
 *
 * <h3>Flow name: SequentialTestRunnerFlow</h3>
 * <p>Executes only scenarios tagged {@code @sequential} — typically database
 * validation scenarios that use hardcoded IDs or shared database state.
 * These are excluded from {@link TestRunner} to prevent data collisions
 * during parallel execution.</p>
 *
 * <h3>Why a separate runner?</h3>
 * <p>Cucumber's native parallel execution operates at the scenario level
 * within a single runner. By isolating {@code @sequential} scenarios into
 * their own runner class, Maven Surefire can execute them in a dedicated
 * phase/fork without interference from parallel UI scenarios.</p>
 *
 * <h3>Surefire integration</h3>
 * <p>This runner is included in the Surefire configuration and runs in the
 * same JVM but after the parallel runner completes (via Surefire's serial
 * test class execution). The {@code cucumber.execution.parallel.enabled}
 * property is overridden to {@code false} via the {@code @CucumberOptions}
 * to ensure these scenarios run one-at-a-time regardless of the global
 * parallel setting.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn test                           # runs both parallel + sequential
 *   mvn test -Dtest=SequentialTestRunner  # run sequential tests only
 * </pre>
 */
// PUBLIC_INTERFACE
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.orangehrm.steps", "com.orangehrm.hooks", "com.orangehrm.db"},
        plugin = {
            "pretty",
            "html:target/cucumber-reports/cucumber-sequential.html",
            "json:target/cucumber-reports/cucumber-sequential.json"
        },
        // Only run sequential scenarios (database validation, etc.)
        tags = "@sequential and not @flaky and not @wip",
        monochrome = true,
        publish = false
)
public class SequentialTestRunner {
    // This class remains empty; is used to invoke the Cucumber runner
}

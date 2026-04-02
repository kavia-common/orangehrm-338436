package com.orangehrm.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Main test runner for Cucumber + JUnit — parallel-safe UI scenarios.
 *
 * <h3>Flow name: ParallelTestRunnerFlow</h3>
 * <p>Executes all feature files under {@code src/test/resources/features},
 * excluding scenarios tagged {@code @flaky}, {@code @wip}, or
 * {@code @sequential}. Sequential scenarios (e.g. database validation)
 * are handled by {@link SequentialTestRunner} to prevent data conflicts
 * during parallel execution.</p>
 *
 * <h3>Parallel execution model</h3>
 * <p>Cucumber 7+ native parallelism is configured via
 * {@code cucumber.properties} (thread-per-scenario). Combined with:</p>
 * <ul>
 *   <li>{@link com.orangehrm.driver.DriverFactory} — ThreadLocal WebDriver</li>
 *   <li>PicoContainer DI — fresh step/hook instances per scenario</li>
 *   <li>{@code @sequential} tag exclusion — DB scenarios run separately</li>
 * </ul>
 *
 * <h3>Report isolation</h3>
 * <p>Each runner writes to separate report files under
 * {@code target/cucumber-reports/} to avoid file contention.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn test                                          # parallel (default)
 *   mvn test -Dcucumber.execution.parallel.enabled=false  # sequential
 *   mvn test -Dcucumber.execution.parallel.config.fixed.parallelism=4  # 4 threads
 * </pre>
 */
// PUBLIC_INTERFACE
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.orangehrm.steps", "com.orangehrm.hooks", "com.orangehrm.db"},
        plugin = {
            "pretty",
            "html:target/cucumber-reports/cucumber-parallel.html",
            "json:target/cucumber-reports/cucumber-parallel.json",
            "timeline:target/cucumber-reports/timeline"
        },
        // Exclude flaky, wip, AND sequential (DB) scenarios from parallel run
        tags = "not @flaky and not @wip and not @sequential",
        monochrome = true,
        publish = false
)
public class TestRunner {
    // This class remains empty; is used to invoke the Cucumber runner
}

package com.orangehrm.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

/**
 * PUBLIC_INTERFACE
 * CucumberTestRunner is the main entry point for executing Cucumber BDD tests.
 *
 * <p>This runner uses JUnit 5 Platform Suite to discover and execute
 * Cucumber feature files. It is configured to:
 * <ul>
 *   <li>Scan for feature files under {@code src/test/resources/features}</li>
 *   <li>Find step definitions in {@code com.orangehrm.stepdefinitions} package</li>
 *   <li>Generate pretty console output and HTML reports</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   mvn test                           # Run all scenarios
 *   mvn test -Dcucumber.filter.tags="@smoke"  # Run only smoke tests
 *   mvn test -Pheadless                # Run in headless browser mode
 * </pre>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.orangehrm.stepdefinitions")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:target/cucumber-reports/cucumber-report.html, json:target/cucumber-reports/cucumber-report.json"
)
public class CucumberTestRunner {
    // This class serves as the Cucumber test suite entry point.
    // All configuration is provided via annotations above.
    // Step definitions are automatically discovered from the glue package.
}

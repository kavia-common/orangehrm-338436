package com.orangehrm.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Main test runner for Cucumber + JUnit.
 * Executes all feature files under src/test/resources/features.
 */
// PUBLIC_INTERFACE
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.orangehrm.steps", "com.orangehrm.hooks", "com.orangehrm.db"},
        plugin = {
            "pretty",
            "html:target/cucumber-reports/cucumber.html",
            "json:target/cucumber-reports/cucumber.json"
        },
        monochrome = true,
        publish = true
)
public class TestRunner {
    // This class remains empty; is used to invoke the Cucumber runner
}

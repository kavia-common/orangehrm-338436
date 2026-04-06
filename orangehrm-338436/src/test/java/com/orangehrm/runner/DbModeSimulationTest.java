package com.orangehrm.runner;

import com.orangehrm.config.EnvironmentClassifier;
import com.orangehrm.config.EnvironmentClassifier.ClassificationResult;
import com.orangehrm.config.EnvironmentClassifier.EnvironmentState;
import com.orangehrm.stepdefinitions.HealthCheckHook;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PUBLIC_INTERFACE
 * DbModeSimulationTest verifies the EnvironmentClassifier and HealthCheckHook
 * behavior under simulated DB-ON and DB-OFF conditions.
 *
 * <p>These tests do NOT require a running OrangeHRM instance or database.
 * They exercise the simulation/classification logic path to verify
 * deterministic, consistent behavior.</p>
 */
@Tag("simulation")
@DisplayName("DB-ON vs DB-OFF Simulation Tests")
public class DbModeSimulationTest {

    @BeforeEach
    void setUp() {
        System.clearProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY);
        HealthCheckHook.resetCache();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY);
        HealthCheckHook.resetCache();
    }

    @Test
    @DisplayName("DB-ON simulation should classify as SIMULATED_DB_ON")
    void testDbOnSimulationClassifiesCorrectly() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "ON");
        ClassificationResult result = EnvironmentClassifier.classify();
        assertNotNull(result, "Classification result should not be null");
        assertEquals(EnvironmentState.SIMULATED_DB_ON, result.getState());
        assertTrue(result.canRunTests());
    }

    @Test
    @DisplayName("DB-ON simulation should be consistent across 3 runs")
    void testDbOnSimulationConsistency() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "ON");
        for (int i = 1; i <= 3; i++) {
            HealthCheckHook.resetCache();
            ClassificationResult result = EnvironmentClassifier.classify();
            assertEquals(EnvironmentState.SIMULATED_DB_ON, result.getState(),
                "Run " + i + ": DB-ON simulation should be consistent");
            assertTrue(result.canRunTests(), "Run " + i + ": DB-ON should allow tests");
            assertFalse(result.getDiagnostics().isEmpty(), "Run " + i + ": Should have diagnostics");
        }
    }

    @Test
    @DisplayName("DB-ON simulation should include simulation mode in diagnostics")
    void testDbOnDiagnosticsContainSimulationInfo() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "ON");
        ClassificationResult result = EnvironmentClassifier.classify();
        String allDiags = String.join(" ", result.getDiagnostics());
        assertTrue(allDiags.contains("SIMULATION"), "Diagnostics should mention SIMULATION mode");
    }

    @Test
    @DisplayName("DB-OFF simulation should classify as SIMULATED_DB_OFF")
    void testDbOffSimulationClassifiesCorrectly() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "OFF");
        ClassificationResult result = EnvironmentClassifier.classify();
        assertNotNull(result, "Classification result should not be null");
        assertEquals(EnvironmentState.SIMULATED_DB_OFF, result.getState());
        assertFalse(result.canRunTests());
    }

    @Test
    @DisplayName("DB-OFF simulation should be consistent across 3 runs")
    void testDbOffSimulationConsistency() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "OFF");
        for (int i = 1; i <= 3; i++) {
            HealthCheckHook.resetCache();
            ClassificationResult result = EnvironmentClassifier.classify();
            assertEquals(EnvironmentState.SIMULATED_DB_OFF, result.getState(),
                "Run " + i + ": DB-OFF simulation should be consistent");
            assertFalse(result.canRunTests(), "Run " + i + ": DB-OFF should NOT allow tests");
        }
    }

    @Test
    @DisplayName("DB-OFF simulation should indicate DB-down in diagnostics")
    void testDbOffDiagnosticsContainDbDownInfo() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "OFF");
        ClassificationResult result = EnvironmentClassifier.classify();
        String allDiags = String.join(" ", result.getDiagnostics()).toLowerCase();
        assertTrue(allDiags.contains("db") || allDiags.contains("down") || allDiags.contains("uninitialized"),
            "Diagnostics should mention DB down/uninitialized");
    }

    @Test
    @DisplayName("DB-ON and DB-OFF should produce different states")
    void testModesProduceDifferentStates() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "ON");
        HealthCheckHook.resetCache();
        ClassificationResult dbOnResult = EnvironmentClassifier.classify();

        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "OFF");
        HealthCheckHook.resetCache();
        ClassificationResult dbOffResult = EnvironmentClassifier.classify();

        assertNotEquals(dbOnResult.getState(), dbOffResult.getState());
        assertNotEquals(dbOnResult.canRunTests(), dbOffResult.canRunTests());
    }

    @Test
    @DisplayName("isSimulationMode should return true when flag is set")
    void testIsSimulationModeReturnsTrue() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "ON");
        assertTrue(EnvironmentClassifier.isSimulationMode());
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "OFF");
        assertTrue(EnvironmentClassifier.isSimulationMode());
    }

    @Test
    @DisplayName("isSimulationMode should return false when flag is not set")
    void testIsSimulationModeReturnsFalse() {
        System.clearProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY);
        assertFalse(EnvironmentClassifier.isSimulationMode());
    }

    @Test
    @DisplayName("formatSummary should produce non-empty output")
    void testFormatSummaryProducesOutput() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "ON");
        ClassificationResult result = EnvironmentClassifier.classify();
        String summary = EnvironmentClassifier.formatSummary(result);
        assertNotNull(summary);
        assertFalse(summary.isEmpty());
        assertTrue(summary.contains("ENVIRONMENT CLASSIFICATION RESULT"));
    }

    @Test
    @DisplayName("Full simulation runner should produce report")
    void testFullSimulationRunnerProducesReport() {
        DbModeSimulationRunner runner = new DbModeSimulationRunner();
        String report = runner.runFullSimulation();
        assertNotNull(report);
        assertFalse(report.isEmpty());
        assertTrue(report.contains("DB-ON"));
        assertTrue(report.contains("DB-OFF"));
        assertTrue(report.contains("Stability Analysis"));
        assertTrue(report.contains("Reliability Recommendations"));
    }

    @Test
    @DisplayName("Unknown simulation flag value should fall through gracefully")
    void testUnknownSimulationFlagValue() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "INVALID");
        ClassificationResult result = EnvironmentClassifier.classify();
        assertNotNull(result, "Result should not be null even with invalid flag");
    }

    @Test
    @DisplayName("Classification result should have non-negative duration")
    void testClassificationDurationIsNonNegative() {
        System.setProperty(EnvironmentClassifier.SIMULATE_DB_STATUS_KEY, "ON");
        ClassificationResult result = EnvironmentClassifier.classify();
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    @DisplayName("Mask utility should mask sensitive values correctly")
    void testMaskUtility() {
        assertEquals("****", com.orangehrm.config.TestConfig.mask(null));
        assertEquals("****", com.orangehrm.config.TestConfig.mask(""));
        assertEquals("****", com.orangehrm.config.TestConfig.mask("ab"));
        assertEquals("A***n", com.orangehrm.config.TestConfig.mask("Admin"));
        assertEquals("a******3", com.orangehrm.config.TestConfig.mask("admin123"));
    }
}

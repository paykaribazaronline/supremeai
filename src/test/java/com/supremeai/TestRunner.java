
package com.supremeai;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test suite for running all tests in the SupremeAI project.
 * This suite can be used to run all tests at once for comprehensive coverage.
 */
@Suite
@SuiteDisplayName("SupremeAI Test Suite")
@SelectPackages({
    "com.supremeai.service",
    "com.supremeai.controller",
    "com.supremeai.security",
    "com.supremeai.ml",
    "com.supremeai.agentorchestration",
    "com.supremeai.selfhealing",
    "com.supremeai.provider",
    "com.supremeai.integration"
})
public class TestRunner {
    // This class serves as a test suite runner
    // All tests in the specified packages will be executed when this suite is run
}

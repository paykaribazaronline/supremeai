package com.supremeai.learning;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for LearningModeControl. Tests mode switching, permission checks, and emergency pause
 * behavior.
 */
class LearningModeControlTest {

  @Test
  void testGetCurrentMode_defaultIsBalanced() {
    LearningModeControl control = new LearningModeControl();
    assertEquals(LearningModeControl.LearningMode.BALANCED, control.getCurrentMode());
  }

  @Test
  void testSetMode_changesMode() {
    LearningModeControl control = new LearningModeControl();

    control.setMode(LearningModeControl.LearningMode.AGGRESSIVE);
    assertEquals(LearningModeControl.LearningMode.AGGRESSIVE, control.getCurrentMode());

    control.setMode(LearningModeControl.LearningMode.MANUAL);
    assertEquals(LearningModeControl.LearningMode.MANUAL, control.getCurrentMode());

    control.setMode(LearningModeControl.LearningMode.PAUSED);
    assertEquals(LearningModeControl.LearningMode.PAUSED, control.getCurrentMode());
  }

  @Test
  void testIsScrapingAllowed_aggressiveMode() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.AGGRESSIVE);
    assertTrue(control.isScrapingAllowed());
  }

  @Test
  void testIsScrapingAllowed_balancedMode() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.BALANCED);
    assertTrue(control.isScrapingAllowed());
  }

  @Test
  void testIsScrapingAllowed_manualMode() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.MANUAL);
    assertFalse(control.isScrapingAllowed());
  }

  @Test
  void testIsScrapingAllowed_pausedMode() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.PAUSED);
    assertFalse(control.isScrapingAllowed());
  }

  @Test
  void testIsAutoApprovalAllowed_aggressiveModeOnly() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.AGGRESSIVE);
    assertTrue(control.isAutoApprovalAllowed());

    control.setMode(LearningModeControl.LearningMode.BALANCED);
    assertFalse(control.isAutoApprovalAllowed());

    control.setMode(LearningModeControl.LearningMode.MANUAL);
    assertFalse(control.isAutoApprovalAllowed());
  }

  @Test
  void testIsLearningAllowed_aggressiveAndBalanced() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.AGGRESSIVE);
    assertTrue(control.isLearningAllowed());
    control.setMode(LearningModeControl.LearningMode.BALANCED);
    assertTrue(control.isLearningAllowed());
  }

  @Test
  void testIsLearningAllowed_manualAndPaused() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.MANUAL);
    assertFalse(control.isLearningAllowed());
    control.setMode(LearningModeControl.LearningMode.PAUSED);
    assertFalse(control.isLearningAllowed());
  }

  @Test
  void testEmergencyPause_overridesAllModes() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.AGGRESSIVE);
    assertTrue(control.isLearningAllowed());

    control.emergencyPause();
    assertTrue(control.isEmergencyPaused());
    assertEquals(LearningModeControl.LearningMode.PAUSED, control.getCurrentMode());
    assertFalse(control.isLearningAllowed());
    assertFalse(control.isScrapingAllowed());
  }

  @Test
  void testResumeFromPause_restoresPreviousMode() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.AGGRESSIVE);
    control.emergencyPause();
    assertEquals(LearningModeControl.LearningMode.PAUSED, control.getCurrentMode());

    control.resumeFromPause();
    assertFalse(control.isEmergencyPaused());
    assertEquals(LearningModeControl.LearningMode.AGGRESSIVE, control.getCurrentMode());
    assertTrue(control.isLearningAllowed());
  }

  @Test
  void testEmergencyPause_doubleCall_isIdempotent() {
    LearningModeControl control = new LearningModeControl();
    control.emergencyPause();
    control.emergencyPause(); // Call again
    assertTrue(control.isEmergencyPaused());
    assertEquals(LearningModeControl.LearningMode.PAUSED, control.getCurrentMode());
  }

  @Test
  void testAllowManualTrigger_manualAndBalancedModes() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.MANUAL);
    assertTrue(control.allowManualTrigger());
    control.setMode(LearningModeControl.LearningMode.BALANCED);
    assertTrue(control.allowManualTrigger());
  }

  @Test
  void testAllowManualTrigger_aggressiveAndPaused() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.AGGRESSIVE);
    assertFalse(control.allowManualTrigger());
    control.setMode(LearningModeControl.LearningMode.PAUSED);
    assertFalse(control.allowManualTrigger());
  }

  @Test
  void testEmergencyPause_overridesManualTrigger() {
    LearningModeControl control = new LearningModeControl();
    control.setMode(LearningModeControl.LearningMode.MANUAL);
    assertTrue(control.allowManualTrigger());

    control.emergencyPause();
    assertFalse(control.allowManualTrigger());
  }

  @Test
  void testModeTransitionSequence() {
    LearningModeControl control = new LearningModeControl();

    // BALANCED -> AGGRESSIVE -> MANUAL -> PAUSED -> BALANCED
    control.setMode(LearningModeControl.LearningMode.BALANCED);
    assertEquals(LearningModeControl.LearningMode.BALANCED, control.getCurrentMode());

    control.setMode(LearningModeControl.LearningMode.AGGRESSIVE);
    assertEquals(LearningModeControl.LearningMode.AGGRESSIVE, control.getCurrentMode());

    control.setMode(LearningModeControl.LearningMode.MANUAL);
    assertEquals(LearningModeControl.LearningMode.MANUAL, control.getCurrentMode());

    control.setMode(LearningModeControl.LearningMode.PAUSED);
    assertEquals(LearningModeControl.LearningMode.PAUSED, control.getCurrentMode());

    control.setMode(LearningModeControl.LearningMode.BALANCED);
    assertEquals(LearningModeControl.LearningMode.BALANCED, control.getCurrentMode());
  }
}

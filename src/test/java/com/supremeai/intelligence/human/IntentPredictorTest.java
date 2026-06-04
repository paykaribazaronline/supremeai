package com.supremeai.intelligence.human;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for IntentPredictor. Tests next-move prediction based on code context and partial
 * prompts.
 */
class IntentPredictorTest {

  private final IntentPredictor predictor = new IntentPredictor();

  @Test
  void testPredictNextMove_restControllerWithSave() {
    String currentCode = "@RestController\npublic class UserController {}";
    String partialPrompt = "save";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNotNull(prediction);
    assertTrue(prediction.contains("POST endpoint"));
    assertTrue(prediction.contains("save"));
  }

  @Test
  void testPredictNextMove_repositoryInterface() {
    String currentCode = "public interface UserRepository extends JpaRepository<User, Long> {}";
    String partialPrompt = "i need service";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNotNull(prediction);
    assertTrue(prediction.contains("Service"));
    assertTrue(prediction.contains("CRUD"));
  }

  @Test
  void testPredictNextMove_insufficientContext() {
    String currentCode = "public class SimpleClass {}";
    String partialPrompt = "do something";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNull(prediction);
  }

  @Test
  void testPredictNextMove_restControllerWithoutSaveKeyword() {
    String currentCode = "@RestController\npublic class UserController {}";
    String partialPrompt = "get all users";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNull(prediction);
  }

  @Test
  void testPredictNextMove_repositoryWithoutServiceMention() {
    String currentCode = "public interface UserRepository extends JpaRepository<User, Long> {}";
    String partialPrompt = "test";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNull(prediction);
  }

  @Test
  void testPredictNextMove_caseInsensitivePartialPrompt() {
    String currentCode = "@RestController\npublic class UserController {}";
    String partialPrompt = "SAVE";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNotNull(prediction);
  }

  @Test
  void testPredictNextMove_codeWithoutAnnotations() {
    String currentCode = "public class Service {}";
    String partialPrompt = "something";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNull(prediction);
  }

  @Test
  void testPredictNextMove_emptyCode() {
    String currentCode = "";
    String partialPrompt = "save";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNull(prediction);
  }

  @Test
  void testPredictNextMove_nullCode() {
    String currentCode = null;
    String partialPrompt = "save";

    assertThrows(
        NullPointerException.class,
        () -> {
          predictor.predictNextMove(currentCode, partialPrompt);
        });
  }

  @Test
  void testPredictNextMove_entityRepository() {
    String currentCode =
        "public interface ProductRepository extends JpaRepository<Product, Long> {}";
    String partialPrompt = "i need a service";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNotNull(prediction);
    assertTrue(prediction.contains("Service"));
  }

  @Test
  void testPredictNextMove_predictionContent() {
    String currentCode = "@RestController\npublic class OrderController {}";
    String partialPrompt = "save order";

    String prediction = predictor.predictNextMove(currentCode, partialPrompt);

    assertNotNull(prediction);
    assertTrue(prediction.contains("Controller") || prediction.contains("layer"));
  }
}

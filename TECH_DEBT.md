# Technical Debt Tracking

This document tracks technical debt, known bugs, and workarounds within the SupremeAI 2.0 repository. Items should be addressed in future sprints.

## Identified Issues

### BUG-001: Legacy Tools Mocking Non-existent Router Method
* **Location**: 
  - ackend/tests/tools/test_game_dev_agent.py
  - ackend/tests/tools/test_image_to_code_react.py
  - ackend/tests/tools/test_legal_agent.py
* **Severity**: Low
* **Description**: These test suites were recently discovered after being moved from 	ools/ to 	ests/tools/. They attempt to mock _get_model_router on agents that do not possess this method, causing AttributeErrors during pytest execution.
* **Workaround**: Tests have been temporarily marked with @pytest.mark.skip. They need to be rewritten to correctly mock core.llm.llm_gateway.acompletion instead of the non-existent router method.

### BUG-002: Missing Coverage Mock Failures
* **Location**: ackend/tests/core/test_core_missing_coverage.py
* **Severity**: Low
* **Description**: Legacy tests targeting LLMGateway edge cases fail due to outdated mock references following the core module restructure.
* **Workaround**: Tests are temporarily skipped. Mocks need to be updated to target the correct new submodule paths.

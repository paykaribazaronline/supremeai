# 📄 ফাইল: backend/API-swagger.yaml

**প্রকার:** .yaml  
**সাইজ:** 248,567 বাইট  
**আপডেট:** 2026-07-11T18:21:34.894469

---

## কোড

```yaml
openapi: 3.1.0
info:
  title: SupremeAI 2.0 (Production Ready)
  description: Multi-cloud AI orchestration platform with zero-cost edge computing.
  version: 2.0.0
paths:
  /health:
    get:
      summary: Health Check
      operationId: health_check_health_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /actuator/health:
    get:
      summary: Actuator Health
      operationId: actuator_health_actuator_health_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                additionalProperties:
                  type: string
                type: object
                title: Response Actuator Health Actuator Health Get
  /api/admin/login:
    post:
      summary: Admin Login
      operationId: admin_login_api_admin_login_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AdminLoginRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/verify:
    post:
      summary: Admin Verify
      operationId: admin_verify_api_admin_verify_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AdminVerifyRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/firebase-login:
    post:
      summary: Admin Firebase Login
      operationId: admin_firebase_login_api_admin_firebase_login_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AdminFirebaseLoginRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/firebase-totp-setup:
    post:
      summary: Admin Firebase Totp Setup
      operationId: admin_firebase_totp_setup_api_admin_firebase_totp_setup_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AdminFirebaseTotpSetupRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/firebase-totp-verify:
    post:
      summary: Admin Firebase Totp Verify
      operationId: admin_firebase_totp_verify_api_admin_firebase_totp_verify_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AdminFirebaseTotpVerifyRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin/cloud-distribution:
    get:
      summary: Cloud Distribution
      operationId: cloud_distribution_admin_cloud_distribution_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /admin/free-tier-status:
    get:
      summary: Free Tier Status
      operationId: free_tier_status_admin_free_tier_status_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /admin/free-tier-status/{provider}:
    get:
      summary: Free Tier Provider Status
      operationId: free_tier_provider_status_admin_free_tier_status__provider__get
      parameters:
      - name: provider
        in: path
        required: true
        schema:
          type: string
          title: Provider
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin/free-tier-pause/{provider}:
    post:
      summary: Free Tier Pause Provider
      operationId: free_tier_pause_provider_admin_free_tier_pause__provider__post
      parameters:
      - name: provider
        in: path
        required: true
        schema:
          type: string
          title: Provider
      requestBody:
        content:
          application/json:
            schema:
              type: object
              additionalProperties: true
              default:
                seconds: 60
              title: Payload
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin/free-tier-override/{provider}:
    post:
      summary: Free Tier Override Limits
      operationId: free_tier_override_limits_admin_free_tier_override__provider__post
      parameters:
      - name: provider
        in: path
        required: true
        schema:
          type: string
          title: Provider
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              additionalProperties: true
              title: Payload
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin/token-budget-stats:
    get:
      summary: Token Budget Stats
      operationId: token_budget_stats_admin_token_budget_stats_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /gcp/health:
    get:
      summary: Gcp Health
      operationId: gcp_health_gcp_health_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /gcp/verification-queue/stats:
    get:
      summary: Gcp Verification Queue Stats
      operationId: gcp_verification_queue_stats_gcp_verification_queue_stats_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /gcp/pubsub/stats:
    get:
      summary: Gcp Pubsub Stats
      operationId: gcp_pubsub_stats_gcp_pubsub_stats_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /admin/rules:
    get:
      summary: Get Admin Rules
      operationId: get_admin_rules_admin_rules_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
    post:
      summary: Post Admin Rules
      operationId: post_admin_rules_admin_rules_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties: true
              type: object
              title: Payload
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /skills:
    get:
      summary: Get Skills
      operationId: get_skills_skills_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /memory/checkpoint:
    post:
      tags:
      - memory
      summary: Save Checkpoint
      operationId: save_checkpoint_memory_checkpoint_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CheckpointSaveRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CheckpointResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /memory/checkpoint/{task_id}:
    get:
      tags:
      - memory
      summary: Load Checkpoint
      operationId: load_checkpoint_memory_checkpoint__task_id__get
      parameters:
      - name: task_id
        in: path
        required: true
        schema:
          type: string
          title: Task Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                anyOf:
                - $ref: '#/components/schemas/CheckpointResponse'
                - type: 'null'
                title: Response Load Checkpoint Memory Checkpoint  Task Id  Get
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    delete:
      tags:
      - memory
      summary: Clear Checkpoint
      operationId: clear_checkpoint_memory_checkpoint__task_id__delete
      parameters:
      - name: task_id
        in: path
        required: true
        schema:
          type: string
          title: Task Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /memory/checkpoints:
    get:
      tags:
      - memory
      summary: List Checkpoints
      operationId: list_checkpoints_memory_checkpoints_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                items:
                  additionalProperties: true
                  type: object
                type: array
                title: Response List Checkpoints Memory Checkpoints Get
  /memory/chunk:
    post:
      tags:
      - memory
      summary: Chunk Text
      operationId: chunk_text_memory_chunk_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ChunkRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ChunkResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /memory/context:
    post:
      tags:
      - memory
      summary: Build Context
      operationId: build_context_memory_context_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ContextRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ContextResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /memory/recall:
    get:
      tags:
      - memory
      summary: Recall Memory
      operationId: recall_memory_memory_recall_get
      parameters:
      - name: session_id
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Session Id
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          default: 20
          title: Limit
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                type: array
                items:
                  type: object
                  additionalProperties: true
                title: Response Recall Memory Memory Recall Get
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    delete:
      tags:
      - memory
      summary: Clear Memory
      operationId: clear_memory_memory_recall_delete
      parameters:
      - name: session_id
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Session Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/chat/completion:
    post:
      summary: Get Completion
      operationId: get_completion_api_chat_completion_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CompletionRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CompletionResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/chat/stream:
    post:
      summary: Stream Chat
      operationId: stream_chat_api_chat_stream_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ChatStreamRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /task/execute:
    post:
      summary: Execute Task
      operationId: execute_task_task_execute_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TaskRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/task/stream:
    get:
      summary: Task Stream
      operationId: task_stream_api_task_stream_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/chat/prompt-action:
    post:
      summary: Prompt Action
      operationId: prompt_action_api_chat_prompt_action_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ActionStreamRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/markdown/export:
    post:
      tags:
      - markdown
      summary: Export Markdown
      operationId: export_markdown_api_v1_markdown_export_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MarkdownExportRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/markdown/export/{job_id}/status:
    get:
      tags:
      - markdown
      summary: Get Job Status
      operationId: get_job_status_api_v1_markdown_export__job_id__status_get
      parameters:
      - name: job_id
        in: path
        required: true
        schema:
          type: string
          title: Job Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/markdown/export/{job_id}/download:
    get:
      tags:
      - markdown
      summary: Download Markdown
      operationId: download_markdown_api_v1_markdown_export__job_id__download_get
      parameters:
      - name: job_id
        in: path
        required: true
        schema:
          type: string
          title: Job Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/markdown/compare:
    post:
      tags:
      - markdown
      summary: Compare Ranges
      operationId: compare_ranges_api_v1_markdown_compare_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CompareRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/markdown/share:
    post:
      tags:
      - markdown
      summary: Share To Ai
      operationId: share_to_ai_api_v1_markdown_share_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ShareRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/markdown/export/history:
    get:
      tags:
      - markdown
      summary: Get History
      operationId: get_history_api_v1_markdown_export_history_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/simulator/profile:
    get:
      tags:
      - simulator
      summary: Get Profile
      operationId: get_profile_api_simulator_profile_get
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - simulator
      summary: Update Profile
      operationId: update_profile_api_simulator_profile_post
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProfileUpdateRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/simulator/install:
    post:
      tags:
      - simulator
      summary: Install App
      operationId: install_app_api_simulator_install_post
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/api__routes__simulator__InstallRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/simulator/install/{appId}:
    delete:
      tags:
      - simulator
      summary: Uninstall App
      operationId: uninstall_app_api_simulator_install__appId__delete
      parameters:
      - name: appId
        in: path
        required: true
        schema:
          type: string
          title: Appid
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/simulator/installed:
    get:
      tags:
      - simulator
      summary: Get Installed Apps
      operationId: get_installed_apps_api_simulator_installed_get
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/simulator/session/start:
    post:
      tags:
      - simulator
      summary: Start Session
      operationId: start_session_api_simulator_session_start_post
      parameters:
      - name: appId
        in: query
        required: true
        schema:
          type: string
          title: Appid
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/simulator/session/stop:
    post:
      tags:
      - simulator
      summary: Stop Session
      operationId: stop_session_api_simulator_session_stop_post
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/simulator/session/status:
    get:
      tags:
      - simulator
      summary: Get Session Status
      operationId: get_session_status_api_simulator_session_status_get
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/simulator/devices:
    get:
      tags:
      - simulator
      summary: Get Available Devices
      operationId: get_available_devices_api_simulator_devices_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/simulator/admin/usage:
    get:
      tags:
      - simulator
      summary: Get All Usage
      operationId: get_all_usage_api_simulator_admin_usage_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/simulator/admin/set-quota/{userId}:
    post:
      tags:
      - simulator
      summary: Admin Set Quota
      operationId: admin_set_quota_api_simulator_admin_set_quota__userId__post
      parameters:
      - name: userId
        in: path
        required: true
        schema:
          type: string
          title: Userid
      - name: quota
        in: query
        required: true
        schema:
          type: integer
          title: Quota
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/site-actions/:
    get:
      tags:
      - Site Actions Registry
      summary: List Site Actions
      operationId: list_site_actions_api_admin_site_actions__get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
    post:
      tags:
      - Site Actions Registry
      summary: Create Site Action
      operationId: create_site_action_api_admin_site_actions__post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SiteActionIn'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/site-actions/{action_id}:
    put:
      tags:
      - Site Actions Registry
      summary: Update Site Action
      operationId: update_site_action_api_admin_site_actions__action_id__put
      parameters:
      - name: action_id
        in: path
        required: true
        schema:
          type: integer
          title: Action Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SiteActionIn'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    delete:
      tags:
      - Site Actions Registry
      summary: Delete Site Action
      operationId: delete_site_action_api_admin_site_actions__action_id__delete
      parameters:
      - name: action_id
        in: path
        required: true
        schema:
          type: integer
          title: Action Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/site-actions/test:
    post:
      tags:
      - Site Actions Registry
      summary: Test Selector
      description: 'Dry-Run DOM Test endpoint.

        In production, this proxies a CDP command to the live headless instance.

        For now, it simulates a visual hit.'
      operationId: test_selector_api_admin_site_actions_test_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TestSelectorRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/llm/providers:
    get:
      tags:
      - LLM Gateway
      summary: List Providers
      operationId: list_providers_api_admin_llm_providers_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/admin/llm/router:
    get:
      tags:
      - LLM Gateway
      summary: Get Router State
      operationId: get_router_state_api_admin_llm_router_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/admin/llm/router/override:
    post:
      tags:
      - LLM Gateway
      summary: Set Router Override
      operationId: set_router_override_api_admin_llm_router_override_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RouterOverride'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/llm/rules:
    get:
      tags:
      - LLM Gateway
      summary: Get System Rules
      operationId: get_system_rules_api_admin_llm_rules_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
    post:
      tags:
      - LLM Gateway
      summary: Save System Rules
      operationId: save_system_rules_api_admin_llm_rules_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RulesPayload'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/status:
    get:
      tags:
      - browser
      summary: Get Status
      operationId: get_status_api_browser_surf_status_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/surf/start:
    post:
      tags:
      - browser
      summary: Start Surf
      operationId: start_surf_api_browser_surf_start_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/surf/stop:
    post:
      tags:
      - browser
      summary: Stop Surf
      operationId: stop_surf_api_browser_surf_stop_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/activity/recent:
    get:
      tags:
      - browser
      summary: Get Recent Activity
      operationId: get_recent_activity_api_browser_activity_recent_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/credentials:
    get:
      tags:
      - browser
      summary: Get Credentials
      operationId: get_credentials_api_browser_credentials_get
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - browser
      summary: Save Credential
      operationId: save_credential_api_browser_credentials_post
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CredentialRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/credentials/{id}:
    delete:
      tags:
      - browser
      summary: Delete Credential
      operationId: delete_credential_api_browser_credentials__id__delete
      parameters:
      - name: credential_id
        in: query
        required: true
        schema:
          type: string
          title: Credential Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/resume:
    post:
      tags:
      - browser
      summary: Resume Surf
      operationId: resume_surf_api_browser_surf_resume_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties:
                type: string
              type: object
              title: Body
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/skip-auth:
    post:
      tags:
      - browser
      summary: Skip Auth
      operationId: skip_auth_api_browser_surf_skip_auth_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties:
                type: string
              type: object
              title: Body
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/pause-manual:
    post:
      tags:
      - browser
      summary: Pause Manual
      operationId: pause_manual_api_browser_surf_pause_manual_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties:
                type: string
              type: object
              title: Body
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/paused-state:
    get:
      tags:
      - browser
      summary: Get Paused State
      operationId: get_paused_state_api_browser_surf_paused_state_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/urls/allowed:
    get:
      tags:
      - browser
      summary: Get Allowed Urls
      operationId: get_allowed_urls_api_browser_urls_allowed_get
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - browser
      summary: Add Allowed Url
      operationId: add_allowed_url_api_browser_urls_allowed_post
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UrlPermissionRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/urls/denied:
    get:
      tags:
      - browser
      summary: Get Denied Urls
      operationId: get_denied_urls_api_browser_urls_denied_get
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - browser
      summary: Add Denied Url
      operationId: add_denied_url_api_browser_urls_denied_post
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UrlPermissionRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/urls/allowAll:
    post:
      tags:
      - browser
      summary: Allow All Urls
      operationId: allow_all_urls_api_browser_urls_allowAll_post
      parameters:
      - name: userId
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Userid
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/urls/{id}:
    delete:
      tags:
      - browser
      summary: Delete Url
      operationId: delete_url_api_browser_urls__id__delete
      parameters:
      - name: url_id
        in: query
        required: true
        schema:
          type: string
          title: Url Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/urls/requests:
    get:
      tags:
      - browser
      summary: Get Requests
      operationId: get_requests_api_browser_urls_requests_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/urls/requests/{id}/decision:
    post:
      tags:
      - browser
      summary: Decision
      operationId: decision_api_browser_urls_requests__id__decision_post
      parameters:
      - name: request_id
        in: query
        required: true
        schema:
          type: string
          title: Request Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DecisionRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/system-learning:
    get:
      tags:
      - browser
      summary: Get System Learning
      operationId: get_system_learning_api_browser_system_learning_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/system-learning/toggle:
    post:
      tags:
      - browser
      summary: Toggle Learning
      operationId: toggle_learning_api_browser_system_learning_toggle_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties:
                type: boolean
              type: object
              title: Body
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/tasks:
    get:
      tags:
      - browser
      summary: Get Tasks
      operationId: get_tasks_api_browser_tasks_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
    post:
      tags:
      - browser
      summary: Create Task
      operationId: create_task_api_browser_tasks_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GoalRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/tasks/{id}/circuit-open:
    post:
      tags:
      - browser
      summary: Set Task Circuit Open
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u099F\u09BE\u09B8\u09CD\u0995\u099F\u09BF \u09B8\u09BE\u09B0\u09CD\
        \u0995\u09BF\u099F \u09AC\u09CD\u09B0\u09C7\u0995\u09BE\u09B0 \u09B8\u09CD\
        \u099F\u09C7\u099F\u09C7 \u09B8\u09C7\u099F \u0995\u09B0\u09C7 \u2014 UI \u09A4\
        \u09C7 \u09B2\u09BE\u09B2 \u09B8\u09A4\u09B0\u09CD\u0995-\u0986\u09AD\u09BE\
        \ \u09A6\u09C7\u0996\u09BE\u09A8\u09CB\u09B0 \u099C\u09A8\u09CD\u09AF"
      operationId: set_task_circuit_open_api_browser_tasks__id__circuit_open_post
      parameters:
      - name: task_id
        in: query
        required: true
        schema:
          type: string
          title: Task Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/tasks/{id}/complete:
    post:
      tags:
      - browser
      summary: Set Task Complete
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u099F\u09BE\u09B8\u09CD\u0995 \u09B8\u09AB\u09B2\u09AD\u09BE\u09AC\
        \u09C7 \u09B8\u09AE\u09CD\u09AA\u09A8\u09CD\u09A8 \u09B9\u09B2\u09C7 \u0995\
        \u09B2 \u0995\u09B0\u09C1\u09A8"
      operationId: set_task_complete_api_browser_tasks__id__complete_post
      parameters:
      - name: task_id
        in: query
        required: true
        schema:
          type: string
          title: Task Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/tasks/{id}/fail:
    post:
      tags:
      - browser
      summary: Set Task Failed
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u099F\u09BE\u09B8\u09CD\u0995 \u09AC\u09CD\u09AF\u09B0\u09CD\u09A5\
        \ \u09B9\u09B2\u09C7 \u0995\u09B2 \u0995\u09B0\u09C1\u09A8"
      operationId: set_task_failed_api_browser_tasks__id__fail_post
      parameters:
      - name: task_id
        in: query
        required: true
        schema:
          type: string
          title: Task Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/tasks/{id}:
    delete:
      tags:
      - browser
      summary: Delete Task
      operationId: delete_task_api_browser_tasks__id__delete
      parameters:
      - name: task_id
        in: query
        required: true
        schema:
          type: string
          title: Task Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/tasks/{id}/findings:
    get:
      tags:
      - browser
      summary: Get Findings
      operationId: get_findings_api_browser_tasks__id__findings_get
      parameters:
      - name: task_id
        in: query
        required: true
        schema:
          type: string
          title: Task Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/findings:
    post:
      tags:
      - browser
      summary: Add Finding
      operationId: add_finding_api_browser_findings_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties: true
              type: object
              title: Finding
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/screenshot:
    get:
      tags:
      - browser
      summary: Get Screenshot
      operationId: get_screenshot_api_browser_surf_screenshot_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/surf/navigate:
    post:
      tags:
      - browser
      summary: Navigate
      operationId: navigate_api_browser_surf_navigate_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/NavigateRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/click:
    post:
      tags:
      - browser
      summary: Click
      operationId: click_api_browser_surf_click_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ClickRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/fill:
    post:
      tags:
      - browser
      summary: Fill
      operationId: fill_api_browser_surf_fill_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/FillRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/click-at:
    post:
      tags:
      - browser
      summary: Click At
      operationId: click_at_api_browser_surf_click_at_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ClickAtRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/type-key:
    post:
      tags:
      - browser
      summary: Type Key
      operationId: type_key_api_browser_surf_type_key_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/KeyRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/surf/accessibility:
    get:
      tags:
      - browser
      summary: Get Accessibility Tree
      operationId: get_accessibility_tree_api_browser_surf_accessibility_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/browser/simulate-activity:
    post:
      tags:
      - browser
      summary: Simulate Activity
      operationId: simulate_activity_api_browser_simulate_activity_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties:
                type: string
              type: object
              title: Body
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/tasks/{id}/step:
    post:
      tags:
      - browser
      summary: Execute Step
      operationId: execute_step_api_browser_tasks__id__step_post
      parameters:
      - name: task_id
        in: query
        required: true
        schema:
          type: string
          title: Task Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/sessions:
    get:
      tags:
      - browser
      summary: List Sessions
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u09B8\u09AC \u09B8\u09C7\u09B6\u09A8 \u09A4\u09BE\u09B2\u09BF\u0995\
        \u09BE \u09B0\u09BF\u099F\u09BE\u09B0\u09CD\u09A8 \u0995\u09B0\u09C7"
      operationId: list_sessions_api_browser_sessions_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
    post:
      tags:
      - browser
      summary: Create Session
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u09A8\u09A4\u09C1\u09A8 \u09B8\u09C7\u09B6\u09A8 \u09A4\u09C8\u09B0\
        \u09BF \u0995\u09B0\u09C7"
      operationId: create_session_api_browser_sessions_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SessionIn'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/sessions/{session_id}:
    get:
      tags:
      - browser
      summary: Get Session
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u09A8\u09BF\u09B0\u09CD\u09A6\u09BF\u09B7\u09CD\u099F \u09B8\u09C7\
        \u09B6\u09A8 \u09B0\u09BF\u099F\u09BE\u09B0\u09CD\u09A8 \u0995\u09B0\u09C7"
      operationId: get_session_api_browser_sessions__session_id__get
      parameters:
      - name: session_id
        in: path
        required: true
        schema:
          type: string
          title: Session Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    put:
      tags:
      - browser
      summary: Update Session
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u09AC\u09BF\u09A6\u09CD\u09AF\u09AE\u09BE\u09A8 \u09B8\u09C7\u09B6\
        \u09A8 \u0986\u09AA\u09A1\u09C7\u099F \u0995\u09B0\u09C7"
      operationId: update_session_api_browser_sessions__session_id__put
      parameters:
      - name: session_id
        in: path
        required: true
        schema:
          type: string
          title: Session Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SessionIn'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    delete:
      tags:
      - browser
      summary: Delete Session
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u09B8\u09C7\u09B6\u09A8 \u09AE\u09C1\u099B\u09C7 \u09AB\u09C7\u09B2\
        \u09C7"
      operationId: delete_session_api_browser_sessions__session_id__delete
      parameters:
      - name: session_id
        in: path
        required: true
        schema:
          type: string
          title: Session Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/browser/browse:
    post:
      tags:
      - browser
      summary: Browse
      description: Navigate to a URL and perform browser actions (Admin Only).
      operationId: browse_api_browser_browse_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BrowseRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
      security:
      - HTTPBearer: []
  /api/browser/extract:
    post:
      tags:
      - browser
      summary: Extract
      description: Fetch page and extract structured data with AI (Admin Only).
      operationId: extract_api_browser_extract_post
      security:
      - HTTPBearer: []
      parameters:
      - name: url
        in: query
        required: true
        schema:
          type: string
          title: Url
      - name: extraction_prompt
        in: query
        required: true
        schema:
          type: string
          title: Extraction Prompt
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/stream/chat:
    post:
      tags:
      - stream
      summary: Stream Chat
      operationId: stream_chat_api_stream_chat_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/StreamRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/media/generate-upload-url:
    post:
      tags:
      - media
      summary: Get Upload Url
      operationId: get_upload_url_api_v1_media_generate_upload_url_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UploadRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/graph/skills:
    get:
      tags:
      - knowledge-graph
      summary: Get Skill Graph
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u09AB\u09CD\u09B0\u09A8\u09CD\u099F\u098F\u09A8\u09CD\u09A1\u09C7\
        \ \u09AD\u09BF\u099C\u09CD\u09AF\u09C1\u09AF\u09BC\u09BE\u09B2\u09BE\u0987\
        \u099C \u0995\u09B0\u09BE\u09B0 \u099C\u09A8\u09CD\u09AF \u0997\u09CD\u09B0\
        \u09BE\u09AB\u09C7\u09B0 \u09B8\u09AE\u09B8\u09CD\u09A4 \u09A8\u09CB\u09A1\
        \ \u098F\u09AC\u0982 \u09B0\u09BF\u09B2\u09C7\u09B6\u09A8\u09B6\u09BF\u09AA\
        \ (Edges) \u09AB\u09C7\u099A \u0995\u09B0\u09AC\u09C7\u0964\n\u098F\u099F\u09BF\
        \ React Flow \u09AC\u09BE D3.js \u098F\u09B0 \u0989\u09AA\u09AF\u09CB\u0997\
        \u09C0 \u09A1\u09C7\u099F\u09BE \u09B8\u09CD\u099F\u09CD\u09B0\u09BE\u0995\
        \u099A\u09BE\u09B0 \u09B0\u09BF\u099F\u09BE\u09B0\u09CD\u09A8 \u0995\u09B0\
        \u09C7\u0964"
      operationId: get_skill_graph_api_v1_graph_skills_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                additionalProperties:
                  items:
                    additionalProperties: true
                    type: object
                  type: array
                type: object
                title: Response Get Skill Graph Api V1 Graph Skills Get
      security:
      - OAuth2PasswordBearer: []
  /api/v1/graph/path:
    get:
      tags:
      - knowledge-graph
      summary: Get Learning Path
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u09A6\u09C1\u099F\u09BF \u09B8\u09CD\u0995\u09BF\u09B2\u09C7\u09B0\
        \ \u09AE\u09A7\u09CD\u09AF\u09C7 \u0985\u09AA\u09CD\u099F\u09BF\u09AE\u09BE\
        \u0987\u099C\u09A1 \u09B2\u09BE\u09B0\u09CD\u09A8\u09BF\u0982 \u09AA\u09BE\
        \u09A5 \u09AC\u09C7\u09B0 \u0995\u09B0\u09AC\u09C7\u0964"
      operationId: get_learning_path_api_v1_graph_path_get
      security:
      - OAuth2PasswordBearer: []
      parameters:
      - name: start_skill
        in: query
        required: true
        schema:
          type: string
          description: Starting skill name
          title: Start Skill
        description: Starting skill name
      - name: end_skill
        in: query
        required: true
        schema:
          type: string
          description: Target skill name
          title: End Skill
        description: Target skill name
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/knowledge/seed:
    post:
      tags:
      - knowledge
      summary: Index Seed Data
      operationId: index_seed_data_api_knowledge_seed_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/knowledge/search:
    get:
      tags:
      - knowledge
      summary: Search Knowledge
      operationId: search_knowledge_api_knowledge_search_get
      parameters:
      - name: q
        in: query
        required: true
        schema:
          type: string
          title: Q
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          default: 5
          title: Limit
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/KnowledgeSearchResult'
                title: Response Search Knowledge Api Knowledge Search Get
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - knowledge
      summary: Knowledge Search
      operationId: knowledge_search_api_knowledge_search_post
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/KnowledgeSearchRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/KnowledgeSearchResult'
                title: Response Knowledge Search Api Knowledge Search Post
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /marketplace/search:
    post:
      tags:
      - marketplace
      summary: Search Skills
      operationId: search_skills_marketplace_search_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SearchRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                items:
                  $ref: '#/components/schemas/SkillResponse'
                type: array
                title: Response Search Skills Marketplace Search Post
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /marketplace/install:
    post:
      tags:
      - marketplace
      summary: Install Skill
      operationId: install_skill_marketplace_install_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/api__routes__marketplace__InstallRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                additionalProperties: true
                type: object
                title: Response Install Skill Marketplace Install Post
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/auth/login:
    post:
      tags:
      - auth
      summary: Login
      operationId: login_api_v1_auth_login_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TokenResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/auth/me:
    get:
      tags:
      - auth
      summary: Me
      operationId: me_api_v1_auth_me_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/MeResponse'
      security:
      - OAuth2PasswordBearer: []
  /api/v1/onboarding/onboarding/complete:
    post:
      tags:
      - onboarding
      summary: Complete Onboarding
      description: 'Complete user onboarding:

        1. Validate API key against provider

        2. Save user preferences (theme, model, language)

        3. Return readiness status'
      operationId: complete_onboarding_api_v1_onboarding_onboarding_complete_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/OnboardingPayload'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OnboardingResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/onboarding/onboarding/status/{user_id}:
    get:
      tags:
      - onboarding
      summary: Get Onboarding Status
      description: Check if a user has completed onboarding.
      operationId: get_onboarding_status_api_v1_onboarding_onboarding_status__user_id__get
      parameters:
      - name: user_id
        in: path
        required: true
        schema:
          type: string
          title: User Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                type: object
                additionalProperties: true
                title: Response Get Onboarding Status Api V1 Onboarding Onboarding
                  Status  User Id  Get
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/onboarding/onboarding/reset/{user_id}:
    delete:
      tags:
      - onboarding
      summary: Reset Onboarding
      description: Reset onboarding state (for testing/support).
      operationId: reset_onboarding_api_v1_onboarding_onboarding_reset__user_id__delete
      parameters:
      - name: user_id
        in: path
        required: true
        schema:
          type: string
          title: User Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                type: object
                additionalProperties:
                  type: string
                title: Response Reset Onboarding Api V1 Onboarding Onboarding Reset  User
                  Id  Delete
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/evolution/api/evolution/logs:
    get:
      tags:
      - self-evolution-engine
      summary: Get Evolution Logs
      operationId: get_evolution_logs_api_v1_evolution_api_evolution_logs_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /api/v1/evolution/api/evolution/forge:
    post:
      tags:
      - self-evolution-engine
      summary: Forge Dynamic Skill
      description: On-the-fly AI Skill Generation and Sandbox Deployed Gate.
      operationId: forge_dynamic_skill_api_v1_evolution_api_evolution_forge_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/EvolutionRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/evolution/api/evolution/swarm-graph:
    get:
      tags:
      - self-evolution-engine
      summary: Get Swarm Graph
      operationId: get_swarm_graph_api_v1_evolution_api_evolution_swarm_graph_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/v1/evolution/api/evolution/quarantine:
    post:
      tags:
      - self-evolution-engine
      summary: Quarantine Skill
      operationId: quarantine_skill_api_v1_evolution_api_evolution_quarantine_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/QuarantineRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
      security:
      - HTTPBearer: []
  /api/v1/evolution/api/evolution/proposals:
    get:
      tags:
      - self-evolution-engine
      summary: List Proposals
      description: List all pending AI code proposals for admin review.
      operationId: list_proposals_api_v1_evolution_api_evolution_proposals_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /api/v1/evolution/api/evolution/proposals/{proposal_id}/approve:
    post:
      tags:
      - self-evolution-engine
      summary: Approve Proposal
      description: Manually approve a proposal after security review.
      operationId: approve_proposal_api_v1_evolution_api_evolution_proposals__proposal_id__approve_post
      security:
      - HTTPBearer: []
      parameters:
      - name: proposal_id
        in: path
        required: true
        schema:
          type: string
          title: Proposal Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin-api/logs/stream:
    get:
      tags:
      - admin-dashboard
      summary: Logs Stream
      operationId: logs_stream_admin_api_logs_stream_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/costs:
    get:
      tags:
      - admin-dashboard
      summary: Get Costs
      description: Real-time Cost/budget metrics from CostAuditor.
      operationId: get_costs_admin_api_costs_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/health-map:
    get:
      tags:
      - admin-dashboard
      summary: Get Health Map
      operationId: get_health_map_admin_api_health_map_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/users:
    get:
      tags:
      - admin-dashboard
      summary: Get Users
      operationId: get_users_admin_api_users_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
    post:
      tags:
      - admin-dashboard
      summary: Create User
      operationId: create_user_admin_api_users_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserUpdate'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
      security:
      - HTTPBearer: []
  /admin-api/users/{username}:
    delete:
      tags:
      - admin-dashboard
      summary: Delete User
      operationId: delete_user_admin_api_users__username__delete
      security:
      - HTTPBearer: []
      parameters:
      - name: username
        in: path
        required: true
        schema:
          type: string
          title: Username
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin-api/deploy:
    post:
      tags:
      - admin-dashboard
      summary: Trigger Deploy
      operationId: trigger_deploy_admin_api_deploy_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/metrics:
    get:
      tags:
      - admin-dashboard
      summary: Get Metrics
      operationId: get_metrics_admin_api_metrics_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/providers:
    get:
      tags:
      - admin-dashboard
      summary: Get Providers
      operationId: get_providers_admin_api_providers_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/model-router:
    get:
      tags:
      - admin-dashboard
      summary: Get Model Router
      operationId: get_model_router_admin_api_model_router_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/model-router/override:
    post:
      tags:
      - admin-dashboard
      summary: Set Router Override
      operationId: set_router_override_admin_api_model_router_override_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RouterOverrideRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
      security:
      - HTTPBearer: []
  /admin-api/codebase/export:
    get:
      tags:
      - admin-dashboard
      summary: Get Codebase Export
      operationId: get_codebase_export_admin_api_codebase_export_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/cost-caps:
    get:
      tags:
      - admin-dashboard
      summary: Get Cost Caps
      operationId: get_cost_caps_admin_api_cost_caps_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
    post:
      tags:
      - admin-dashboard
      summary: Update Cost Caps
      operationId: update_cost_caps_admin_api_cost_caps_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties: true
              type: object
              title: Payload
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
      security:
      - HTTPBearer: []
  /admin-api/users/impersonate/{username}:
    post:
      tags:
      - admin-dashboard
      summary: Impersonate User
      operationId: impersonate_user_admin_api_users_impersonate__username__post
      security:
      - HTTPBearer: []
      parameters:
      - name: username
        in: path
        required: true
        schema:
          type: string
          title: Username
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin-api/emergency-deploy:
    post:
      tags:
      - admin-dashboard
      summary: Emergency Deploy
      operationId: emergency_deploy_admin_api_emergency_deploy_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/backup:
    post:
      tags:
      - admin-dashboard
      summary: Trigger Backup
      operationId: trigger_backup_admin_api_backup_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/data-export:
    get:
      tags:
      - admin-dashboard
      summary: Get Full Data Export
      operationId: get_full_data_export_admin_api_data_export_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/security-scan:
    get:
      tags:
      - admin-dashboard
      summary: Run Security Scan
      operationId: run_security_scan_admin_api_security_scan_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
      security:
      - HTTPBearer: []
  /admin-api/gate/override:
    post:
      tags:
      - admin-dashboard
      summary: Execute Manual Gate Override
      description: 'God-Mode Admin Override Gateway.

        Manually bypasses or forces the autonomous deployment gate status.

        Directly affects CI/CD Cloud Build pipelines.'
      operationId: execute_manual_gate_override_admin_api_gate_override_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GateOverridePayload'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
      security:
      - HTTPBearer: []
  /admin-api/ci-logs:
    get:
      tags:
      - admin-dashboard
      summary: Get Ci Logs
      operationId: get_ci_logs_admin_api_ci_logs_get
      security:
      - HTTPBearer: []
      parameters:
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          default: 20
          title: Limit
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin-api/ci-report:
    post:
      tags:
      - admin-dashboard
      summary: Receive Ci Report
      description: 'Receives and stores a structured CI/CD report from a GitHub Actions
        workflow.

        This endpoint is protected by a constitutional rule.'
      operationId: receive_ci_report_admin_api_ci_report_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CIReportPayload'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
      security:
      - HTTPBearer: []
  /admin-api/events:
    get:
      tags:
      - admin-dashboard
      summary: Get Events
      operationId: get_events_admin_api_events_get
      security:
      - HTTPBearer: []
      parameters:
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          maximum: 200
          minimum: 1
          default: 50
          title: Limit
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /admin-api/reports:
    get:
      tags:
      - admin-dashboard
      summary: List Reports
      operationId: list_reports_admin_api_reports_get
      security:
      - HTTPBearer: []
      parameters:
      - name: report_name
        in: query
        required: false
        schema:
          type: string
          title: Report Name
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /integrations/email/gmail:
    post:
      tags:
      - email
      summary: Gmail Auth
      operationId: gmail_auth_integrations_email_gmail_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GmailAuthRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /integrations/email/imap:
    post:
      tags:
      - email
      summary: Imap Auth
      operationId: imap_auth_integrations_email_imap_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ImapAuthRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /github/connect:
    post:
      tags:
      - github
      summary: Connect Repo
      operationId: connect_repo_github_connect_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ConnectRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /github/improve:
    post:
      tags:
      - github
      summary: Improve Repo
      operationId: improve_repo_github_improve_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ImproveRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /github/push:
    post:
      tags:
      - github
      summary: Push Improvements
      operationId: push_improvements_github_push_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PushRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /github/discover:
    post:
      tags:
      - github
      summary: Discover Repos
      operationId: discover_repos_github_discover_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DiscoverRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /github/implement:
    post:
      tags:
      - github
      summary: Implement Repo
      operationId: implement_repo_github_implement_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ImplementRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /internal/run-daily-evolution:
    post:
      summary: Run Daily Evolution
      operationId: run_daily_evolution_internal_run_daily_evolution_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RunEvolutionRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /config/{key}:
    get:
      tags:
      - config
      summary: Get Config
      operationId: get_config_config__key__get
      security:
      - HTTPBearer: []
      parameters:
      - name: key
        in: path
        required: true
        schema:
          type: string
          title: Key
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    put:
      tags:
      - config
      summary: Update Config
      operationId: update_config_config__key__put
      security:
      - HTTPBearer: []
      parameters:
      - name: key
        in: path
        required: true
        schema:
          type: string
          title: Key
      - name: category
        in: query
        required: false
        schema:
          anyOf:
          - type: string
          - type: 'null'
          title: Category
      - name: description
        in: query
        required: false
        schema:
          anyOf:
          - type: string
          - type: 'null'
          title: Description
      requestBody:
        required: true
        content:
          application/json:
            schema:
              title: Value
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /config/category/{category}:
    get:
      tags:
      - config
      summary: Get Configs By Category
      operationId: get_configs_by_category_config_category__category__get
      security:
      - HTTPBearer: []
      parameters:
      - name: category
        in: path
        required: true
        schema:
          type: string
          title: Category
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /repos/:
    get:
      tags:
      - repos
      summary: List Repos
      operationId: list_repos_repos__get
      parameters:
      - name: category
        in: query
        required: false
        schema:
          anyOf:
          - type: string
          - type: 'null'
          title: Category
      - name: priority
        in: query
        required: false
        schema:
          anyOf:
          - type: string
          - type: 'null'
          title: Priority
      - name: status
        in: query
        required: false
        schema:
          type: string
          default: active
          title: Status
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          maximum: 200
          default: 50
          title: Limit
      - name: offset
        in: query
        required: false
        schema:
          type: integer
          default: 0
          title: Offset
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - repos
      summary: Create Repo
      operationId: create_repo_repos__post
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RepoCreate'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /repos/{repo_id}:
    patch:
      tags:
      - repos
      summary: Update Repo
      operationId: update_repo_repos__repo_id__patch
      parameters:
      - name: repo_id
        in: path
        required: true
        schema:
          type: string
          title: Repo Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RepoUpdate'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    delete:
      tags:
      - repos
      summary: Delete Repo
      operationId: delete_repo_repos__repo_id__delete
      parameters:
      - name: repo_id
        in: path
        required: true
        schema:
          type: string
          title: Repo Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /tools/smell-check:
    post:
      tags:
      - tools-ops
      summary: Smell Check
      operationId: smell_check_tools_smell_check_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SmellCheckRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SmellCheckResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /tools/vulnerability-check:
    post:
      tags:
      - tools-ops
      summary: Vulnerability Check
      operationId: vulnerability_check_tools_vulnerability_check_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/VulnCheckRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/VulnCheckResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /tools/skills/recommend:
    post:
      tags:
      - tools-ops
      summary: Recommend Skills
      operationId: recommend_skills_tools_skills_recommend_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SkillRecRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SkillRecResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /tools/domain/adapt:
    post:
      tags:
      - tools-ops
      summary: Domain Adapt
      operationId: domain_adapt_tools_domain_adapt_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DomainAdaptRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DomainAdaptResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /tools/deploy/compose:
    post:
      tags:
      - tools-ops
      summary: Deploy Compose
      operationId: deploy_compose_tools_deploy_compose_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DeployComposeRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DeployResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /tools/deploy/helm:
    post:
      tags:
      - tools-ops
      summary: Deploy Helm
      operationId: deploy_helm_tools_deploy_helm_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DeployHelmRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DeployResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/legal/analyze:
    post:
      tags:
      - specialized-agents
      summary: Legal Analyze
      operationId: legal_analyze_agents_legal_analyze_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LegalAnalysisRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/medical/symptoms:
    post:
      tags:
      - specialized-agents
      summary: Medical Symptoms
      operationId: medical_symptoms_agents_medical_symptoms_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SymptomRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/medical/drug-interactions:
    post:
      tags:
      - specialized-agents
      summary: Medical Drug Interactions
      operationId: medical_drug_interactions_agents_medical_drug_interactions_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DrugInteractionRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/trading/analyze:
    post:
      tags:
      - specialized-agents
      summary: Trading Analyze
      operationId: trading_analyze_agents_trading_analyze_post
      parameters:
      - name: symbol
        in: query
        required: true
        schema:
          type: string
          title: Symbol
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/trading/buy:
    post:
      tags:
      - specialized-agents
      summary: Trading Buy
      operationId: trading_buy_agents_trading_buy_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TradeRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/trading/sell:
    post:
      tags:
      - specialized-agents
      summary: Trading Sell
      operationId: trading_sell_agents_trading_sell_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TradeRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/trading/portfolio:
    get:
      tags:
      - specialized-agents
      summary: Trading Portfolio
      operationId: trading_portfolio_agents_trading_portfolio_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /agents/research/search:
    post:
      tags:
      - specialized-agents
      summary: Research Search
      operationId: research_search_agents_research_search_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ResearchRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/research/summarize:
    post:
      tags:
      - specialized-agents
      summary: Research Summarize
      operationId: research_summarize_agents_research_summarize_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SummarizeRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /agents/research/cite:
    post:
      tags:
      - specialized-agents
      summary: Research Cite
      operationId: research_cite_agents_research_cite_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SummarizeRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/rules:
    post:
      tags:
      - Admin Control Center
      summary: Update Constitutional Rule
      description: Update God.py constitutional rules directly from the Command Center
        UI
      operationId: update_constitutional_rule_api_admin_rules_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RuleUpdate'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/actions/{action_type}:
    post:
      tags:
      - Admin Control Center
      summary: Trigger Quick Action
      description: Trigger 1-click Quick Actions from Dashboard
      operationId: trigger_quick_action_api_admin_actions__action_type__post
      parameters:
      - name: action_type
        in: path
        required: true
        schema:
          type: string
          title: Action Type
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/fixes:
    get:
      tags:
      - Admin Control Center
      summary: Get Fixes
      description: Fetch all fixes for a tenant with a specific status.
      operationId: get_fixes_api_admin_fixes_get
      parameters:
      - name: tenant_id
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Tenant Id
      - name: status
        in: query
        required: false
        schema:
          type: string
          default: pending_review
          title: Status
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/fixes/{fix_id}/approve:
    post:
      tags:
      - Admin Control Center
      summary: Approve Fix
      description: Approve a pending fix.
      operationId: approve_fix_api_admin_fixes__fix_id__approve_post
      parameters:
      - name: fix_id
        in: path
        required: true
        schema:
          type: string
          title: Fix Id
      - name: tenant_id
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Tenant Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/fixes/{fix_id}/reject:
    post:
      tags:
      - Admin Control Center
      summary: Reject Fix
      description: Reject a pending fix.
      operationId: reject_fix_api_admin_fixes__fix_id__reject_post
      parameters:
      - name: fix_id
        in: path
        required: true
        schema:
          type: string
          title: Fix Id
      - name: tenant_id
        in: query
        required: false
        schema:
          type: string
          default: default
          title: Tenant Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /tools/:
    get:
      tags:
      - tools
      summary: List Tools
      operationId: list_tools_tools__get
      parameters:
      - name: category
        in: query
        required: false
        schema:
          anyOf:
          - type: string
          - type: 'null'
          title: Category
      - name: status
        in: query
        required: false
        schema:
          type: string
          default: active
          title: Status
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          maximum: 200
          default: 50
          title: Limit
      - name: offset
        in: query
        required: false
        schema:
          type: integer
          default: 0
          title: Offset
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - tools
      summary: Create Tool
      operationId: create_tool_tools__post
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ToolCreate'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /tools/{tool_id}:
    patch:
      tags:
      - tools
      summary: Update Tool
      operationId: update_tool_tools__tool_id__patch
      parameters:
      - name: tool_id
        in: path
        required: true
        schema:
          type: string
          title: Tool Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ToolUpdate'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    delete:
      tags:
      - tools
      summary: Delete Tool
      operationId: delete_tool_tools__tool_id__delete
      parameters:
      - name: tool_id
        in: path
        required: true
        schema:
          type: string
          title: Tool Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /preferences/:
    get:
      tags:
      - preferences
      summary: Get Preferences
      operationId: get_preferences_preferences__get
      parameters:
      - name: user_id
        in: query
        required: false
        schema:
          type: string
          default: default
          title: User Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - preferences
      summary: Upsert Preferences
      operationId: upsert_preferences_preferences__post
      parameters:
      - name: user_id
        in: query
        required: false
        schema:
          type: string
          default: default
          title: User Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PreferenceUpdate'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /preferences/{user_id}/stream:
    get:
      tags:
      - preferences
      summary: Stream Preferences
      description: SSE endpoint to listen for real-time theme and preference updates
        for a specific user.
      operationId: stream_preferences_preferences__user_id__stream_get
      parameters:
      - name: user_id
        in: path
        required: true
        schema:
          type: string
          title: User Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /metrics/usage/:
    get:
      tags:
      - usage-metrics
      summary: Get Usage Metrics
      operationId: get_usage_metrics_metrics_usage__get
      parameters:
      - name: start
        in: query
        required: false
        schema:
          anyOf:
          - type: string
          - type: 'null'
          title: Start
      - name: end
        in: query
        required: false
        schema:
          anyOf:
          - type: string
          - type: 'null'
          title: End
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          maximum: 365
          default: 30
          title: Limit
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - usage-metrics
      summary: Upsert Usage Metric
      operationId: upsert_usage_metric_metrics_usage__post
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UsageMetricUpsert'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /auth/sso/oidc/discovery:
    post:
      tags:
      - sso
      summary: Oidc Discovery
      operationId: oidc_discovery_auth_sso_oidc_discovery_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/OIDCDiscoveryRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OIDCLoginResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /auth/sso/oidc/{provider}/authorize:
    post:
      tags:
      - sso
      summary: Oidc Provider Authorize
      operationId: oidc_provider_authorize_auth_sso_oidc__provider__authorize_post
      parameters:
      - name: provider
        in: path
        required: true
        schema:
          type: string
          title: Provider
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProviderSSORequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OIDCLoginResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /auth/sso/oidc/{provider}/callback:
    post:
      tags:
      - sso
      summary: Oidc Provider Callback
      operationId: oidc_provider_callback_auth_sso_oidc__provider__callback_post
      parameters:
      - name: provider
        in: path
        required: true
        schema:
          type: string
          title: Provider
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/OIDCCallbackRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SSOLoginResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /auth/sso/oidc/{provider}/logout:
    get:
      tags:
      - sso
      summary: Oidc Logout
      operationId: oidc_logout_auth_sso_oidc__provider__logout_get
      parameters:
      - name: provider
        in: path
        required: true
        schema:
          type: string
          title: Provider
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /auth/sso/saml:
    post:
      tags:
      - sso
      summary: Saml Login
      operationId: saml_login_auth_sso_saml_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SAMLAssertionRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SSOLoginResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /auth/sso/metadata:
    get:
      tags:
      - sso
      summary: Sso Metadata
      operationId: sso_metadata_auth_sso_metadata_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/health/agents:
    post:
      summary: Get Agents Health
      operationId: get_agents_health_api_health_agents_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/HealthRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/create:
    post:
      tags:
      - api-keys
      summary: Create Key
      operationId: create_key_api_api_keys_create_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateAPIKeyRequest'
        required: true
      responses:
        '201':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/:
    get:
      tags:
      - api-keys
      summary: List User Keys
      operationId: list_user_keys_api_api_keys__get
      parameters:
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          default: 50
          title: Limit
      - name: offset
        in: query
        required: false
        schema:
          type: integer
          default: 0
          title: Offset
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/all:
    get:
      tags:
      - api-keys
      summary: List All Keys
      operationId: list_all_keys_api_api_keys_all_get
      parameters:
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          default: 100
          title: Limit
      - name: offset
        in: query
        required: false
        schema:
          type: integer
          default: 0
          title: Offset
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/{key_id}:
    get:
      tags:
      - api-keys
      summary: Get Key
      operationId: get_key_api_api_keys__key_id__get
      parameters:
      - name: key_id
        in: path
        required: true
        schema:
          type: integer
          title: Key Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    delete:
      tags:
      - api-keys
      summary: Delete Key
      operationId: delete_key_api_api_keys__key_id__delete
      parameters:
      - name: key_id
        in: path
        required: true
        schema:
          type: integer
          title: Key Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/{key_id}/revoke:
    post:
      tags:
      - api-keys
      summary: Revoke Key
      operationId: revoke_key_api_api_keys__key_id__revoke_post
      parameters:
      - name: key_id
        in: path
        required: true
        schema:
          type: integer
          title: Key Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/{key_id}/rotate:
    post:
      tags:
      - api-keys
      summary: Rotate Key
      operationId: rotate_key_api_api_keys__key_id__rotate_post
      parameters:
      - name: key_id
        in: path
        required: true
        schema:
          type: integer
          title: Key Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RotateAPIKeyRequest'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/{key_id}/usage:
    get:
      tags:
      - api-keys
      summary: Key Usage
      operationId: key_usage_api_api_keys__key_id__usage_get
      parameters:
      - name: key_id
        in: path
        required: true
        schema:
          type: integer
          title: Key Id
      - name: limit
        in: query
        required: false
        schema:
          type: integer
          default: 100
          title: Limit
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - api-keys
      summary: Record Usage Hook
      operationId: record_usage_hook_api_api_keys__key_id__usage_post
      parameters:
      - name: key_id
        in: path
        required: true
        schema:
          type: integer
          title: Key Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              additionalProperties: true
              title: Payload
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/{key_id}/stats:
    get:
      tags:
      - api-keys
      summary: Key Stats
      operationId: key_stats_api_api_keys__key_id__stats_get
      parameters:
      - name: key_id
        in: path
        required: true
        schema:
          type: integer
          title: Key Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/{key_id}/admin/quota-alert:
    get:
      tags:
      - api-keys
      summary: Quota Alert
      operationId: quota_alert_api_api_keys__key_id__admin_quota_alert_get
      parameters:
      - name: key_id
        in: path
        required: true
        schema:
          type: integer
          title: Key Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/api-keys/admin/bulk-delete:
    post:
      tags:
      - api-keys
      summary: Bulk Delete
      operationId: bulk_delete_api_api_keys_admin_bulk_delete_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BulkDeleteRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/ci/webhook:
    post:
      tags:
      - ci
      summary: Ci Webhook
      operationId: ci_webhook_api_ci_webhook_post
      parameters:
      - name: X-CI-Webhook-Secret
        in: header
        required: true
        schema:
          type: string
          title: X-Ci-Webhook-Secret
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CIReportPayload'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/workspace/task/execute:
    post:
      tags:
      - Supreme Workspace Tasks
      summary: Execute Task
      description: 'Handles user prompts from the Vanilla JS Customer Dashboard.

        Integrates Redis rate limiting, RAM conversation history, and Supabase persistent
        storage.'
      operationId: execute_task_api_v1_workspace_task_execute_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TaskPayload'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/workspace/task/quota:
    get:
      tags:
      - Supreme Workspace Tasks
      summary: Get Quota
      description: Fetch the current token quota from Redis for the UI.
      operationId: get_quota_api_v1_workspace_task_quota_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/v1/agent/execute:
    post:
      summary: Execute Agent Command
      operationId: execute_agent_command_api_v1_agent_execute_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/WorkspaceCommand'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/agent/learn:
    post:
      summary: Commit To Memory
      description: "\u09B6\u09C1\u09A7\u09C1\u09AE\u09BE\u09A4\u09CD\u09B0 \u09AD\u09C7\
        \u09B0\u09BF\u09AB\u09BE\u09DF\u09C7\u09A1 \u098F\u09AC\u0982 \u0995\u09BE\
        \u099C \u0995\u09B0\u09BE \u0995\u09CB\u09A1\u0997\u09C1\u09B2\u09CB\u0987\
        \ \u09AE\u09C7\u09AE\u09CB\u09B0\u09BF \u09AD\u09B2\u09CD\u099F\u09C7 \u09B8\
        \u09C7\u09AD \u09B9\u09AC\u09C7\u0964"
      operationId: commit_to_memory_api_v1_agent_learn_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LearnRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/agent/github/pr:
    post:
      summary: Trigger Github Pr
      operationId: trigger_github_pr_api_v1_agent_github_pr_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PRRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/integrations/github/link:
    get:
      summary: Link Github
      description: "\u0987\u0989\u099C\u09BE\u09B0\u0995\u09C7 GitHub OAuth \u09B2\
        \u0997\u0987\u09A8 \u09AA\u09C7\u0987\u099C\u09C7 \u09B0\u09BF\u09A1\u09BE\
        \u0987\u09B0\u09C7\u0995\u09CD\u099F \u0995\u09B0\u09C7\u0964\nredirect_uri\
        \ \u098F\u0996\u09A8 \u09A1\u09BE\u09AF\u09BC\u09A8\u09BE\u09AE\u09BF\u0995\
        \ \u2014 settings.frontend_base_url \u09A5\u09C7\u0995\u09C7 \u09A8\u09C7\u0993\
        \u09AF\u09BC\u09BE \u09B9\u09AF\u09BC\u0964"
      operationId: link_github_api_v1_integrations_github_link_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/v1/integrations/github/callback:
    get:
      summary: Github Callback
      description: "GitHub OAuth \u0995\u09B2\u09AC\u09CD\u09AF\u09BE\u0995 \u09B9\
        \u09CD\u09AF\u09BE\u09A8\u09CD\u09A1\u09B2\u09BE\u09B0\u0964\n\u0995\u09CB\
        \u09A1 \u098F\u0995\u09CD\u09B8\u099A\u09C7\u099E\u09CD\u099C \u0995\u09B0\
        \u09C7 access_token \u09A8\u09C7\u09AF\u09BC, \u098F\u09A8\u0995\u09CD\u09B0\
        \u09BF\u09AA\u09CD\u099F \u0995\u09B0\u09C7, \u098F\u09AC\u0982 DB-\u09A4\u09C7\
        \ \u09B8\u0982\u09B0\u0995\u09CD\u09B7\u09A3 \u0995\u09B0\u09C7\u0964"
      operationId: github_callback_api_v1_integrations_github_callback_get
      parameters:
      - name: code
        in: query
        required: true
        schema:
          type: string
          title: Code
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/config/public:
    get:
      tags:
      - public_config
      summary: Get Public Config
      operationId: get_public_config_api_config_public_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PublicConfigResponse'
  /api/admin/traffic/live:
    get:
      tags:
      - traffic
      summary: Get Live Traffic
      description: "\u09AC\u09BE\u0982\u09B2\u09BE \u09AE\u09A8\u09CD\u09A4\u09AC\u09CD\
        \u09AF: \u09B0\u09BF\u09DF\u09C7\u09B2-\u099F\u09BE\u0987\u09AE \u099F\u09CD\
        \u09B0\u09BE\u09AB\u09BF\u0995, p95 \u09B2\u09CD\u09AF\u09BE\u099F\u09C7\u09A8\
        \u09CD\u09B8\u09BF \u098F\u09AC\u0982 \u0985\u09CD\u09AF\u09BE\u09B0\u09B0\
        \ \u09B0\u09C7\u099F\u0964\n\u098F\u099F\u09BF \u09B8\u09CD\u099F\u09C1\u09A1\
        \u09BF\u0993 \u0995\u09CD\u09B2\u09BE\u09DF\u09C7\u09A8\u09CD\u099F \u09AC\
        \u09BE \u09AB\u09CD\u09B2\u09BE\u099F\u09BE\u09B0 \u09A1\u09CD\u09AF\u09BE\
        \u09B6\u09AC\u09CB\u09B0\u09CD\u09A1\u09C7 \u09B2\u09BE\u0987\u09AD \u09B8\
        \u09CD\u099F\u09CD\u09B0\u09BF\u09AE\u09BF\u0982 \u098F\u09B0 \u099C\u09A8\
        \u09CD\u09AF \u09AC\u09CD\u09AF\u09AC\u09B9\u09BE\u09B0 \u09B9\u09AC\u09C7\
        \u0964"
      operationId: get_live_traffic_api_admin_traffic_live_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                additionalProperties: true
                type: object
                title: Response Get Live Traffic Api Admin Traffic Live Get
  /api/v1/stream:
    get:
      tags:
      - Swarm
      summary: Stream Swarm Health
      description: 'SSE Endpoint for Real-time Swarm Health & Logs.

        URL: /api/v1/swarm/stream'
      operationId: stream_swarm_health_api_v1_stream_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/v1/telemetry/patch-result:
    post:
      tags:
      - Swarm
      summary: Record Patch Telemetry
      description: Receives telemetry on whether the user accepted, rejected, or modified
        the Swarm's proposed fix.
      operationId: record_patch_telemetry_api_v1_telemetry_patch_result_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PatchTelemetry'
        required: true
      responses:
        '202':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/execute-healing:
    post:
      tags:
      - Swarm
      summary: Execute Healing
      description: 'Agent-in-the-Loop endpoint to self-heal code errors from VS Code
        Extension.

        Rate limited to 5 requests per minute per IP to prevent LLM cost spikes.'
      operationId: execute_healing_api_v1_execute_healing_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SelfHealingRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/forge:
    post:
      tags:
      - Swarm
      summary: Save Forge Swarm
      description: Saves the visual swarm layout (nodes and edges) from the Evolution
        Forge.
      operationId: save_forge_swarm_api_v1_forge_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ForgePayload'
        required: true
      responses:
        '201':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/v1/forge/{flow_id}/execute:
    post:
      tags:
      - Swarm
      summary: Execute Forge Flow
      operationId: execute_forge_flow_api_v1_forge__flow_id__execute_post
      parameters:
      - name: flow_id
        in: path
        required: true
        schema:
          type: string
          title: Flow Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ForgePayload'
      responses:
        '202':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /orchestrator/status:
    get:
      tags:
      - orchestrator
      summary: Get Status
      operationId: get_status_orchestrator_status_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /orchestrator/tick:
    post:
      tags:
      - orchestrator
      summary: Trigger Tick
      description: Webhook for Google Cloud Scheduler to trigger the orchestrator
        periodically.
      operationId: trigger_tick_orchestrator_tick_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /tools/image-to-code:
    post:
      tags:
      - tools
      - image-to-code
      summary: Api Image To Code
      operationId: api_image_to_code_tools_image_to_code_post
      requestBody:
        content:
          multipart/form-data:
            schema:
              $ref: '#/components/schemas/Body_api_image_to_code_tools_image_to_code_post'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/voice/process-audio:
    post:
      tags:
      - voice-coder
      summary: Process Audio
      description: Upload an audio file and get code generated from it.
      operationId: process_audio_api_voice_process_audio_post
      requestBody:
        content:
          multipart/form-data:
            schema:
              $ref: '#/components/schemas/Body_process_audio_api_voice_process_audio_post'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/style/learn:
    post:
      tags:
      - style-learner
      summary: Learn Style
      description: Extract and persist coding style from a repository path.
      operationId: learn_style_api_style_learn_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/StyleRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/style/prompt:
    get:
      tags:
      - style-learner
      summary: Get Style Prompt
      description: Get a style-injection prompt for the given repo and language.
      operationId: get_style_prompt_api_style_prompt_get
      parameters:
      - name: repo_path
        in: query
        required: true
        schema:
          type: string
          title: Repo Path
      - name: language
        in: query
        required: false
        schema:
          type: string
          default: python
          title: Language
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/diagram/generate:
    post:
      tags:
      - diagram-to-architecture
      summary: Generate From Diagram
      description: Upload a diagram image and get infrastructure-as-code.
      operationId: generate_from_diagram_api_diagram_generate_post
      requestBody:
        content:
          multipart/form-data:
            schema:
              $ref: '#/components/schemas/Body_generate_from_diagram_api_diagram_generate_post'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/diagram/api-spec:
    post:
      tags:
      - diagram-to-architecture
      summary: Generate Api Spec
      description: Upload sequence diagram and get OpenAPI spec.
      operationId: generate_api_spec_api_diagram_api_spec_post
      requestBody:
        content:
          multipart/form-data:
            schema:
              $ref: '#/components/schemas/Body_generate_api_spec_api_diagram_api_spec_post'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/pair/solve:
    post:
      tags:
      - ai-pair-programmer
      summary: Solve Issue
      operationId: solve_issue_api_pair_solve_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/IssueRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/pair/review:
    post:
      tags:
      - ai-pair-programmer
      summary: Review Code
      operationId: review_code_api_pair_review_post
      requestBody:
        content:
          application/json:
            schema:
              additionalProperties: true
              type: object
              title: Payload
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/codeflow/analyze:
    post:
      tags:
      - codeflow
      summary: Analyze
      operationId: analyze_api_codeflow_analyze_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CodeFlowRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CodeFlowResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/feedback/ingest:
    post:
      tags:
      - feedback
      summary: Ingest
      operationId: ingest_api_feedback_ingest_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/FeedbackEvent'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FeedbackResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/tts/synthesize:
    post:
      tags:
      - multilingual-tts
      summary: Synthesize Text
      description: Convert text to speech. Supports 29 languages. Auto-detects language.
      operationId: synthesize_text_api_tts_synthesize_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TTSRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TTSResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/tts/audio/{filename}:
    get:
      tags:
      - multilingual-tts
      summary: Get Audio
      description: Serve generated audio file.
      operationId: get_audio_api_tts_audio__filename__get
      parameters:
      - name: filename
        in: path
        required: true
        schema:
          type: string
          title: Filename
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/tts/languages:
    get:
      tags:
      - multilingual-tts
      summary: List Languages
      description: List all supported languages.
      operationId: list_languages_api_tts_languages_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/tts/voices:
    get:
      tags:
      - multilingual-tts
      summary: List Voices
      description: List available ElevenLabs voices (requires API key).
      operationId: list_voices_api_tts_voices_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/tts/cache:
    delete:
      tags:
      - multilingual-tts
      summary: Clear Cache
      description: Clear TTS audio cache.
      operationId: clear_cache_api_tts_cache_delete
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/voice/stream_audio:
    get:
      summary: Stream Audio
      description: 'Stream TTS audio bytes in real-time for the given text.

        Uses ElevenLabs for primary synthesis (if API key configured) with fallback
        to edge-tts.'
      operationId: stream_audio_api_voice_stream_audio_get
      parameters:
      - name: text
        in: query
        required: false
        schema:
          type: string
          default: ''
          title: Text
      - name: voice
        in: query
        required: false
        schema:
          anyOf:
          - type: string
          - type: 'null'
          title: Voice
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/comment-ai/handle-comment:
    post:
      tags:
      - comment-thread-ai
      summary: Handle Comment
      description: "Handle a PR review comment \u2014 propose fix and optionally auto-reply."
      operationId: handle_comment_api_comment_ai_handle_comment_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PRCommentPayload'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/comment-ai/summarize:
    post:
      tags:
      - comment-thread-ai
      summary: Summarize Thread
      description: Summarize a GitHub PR/issue comment thread with AI.
      operationId: summarize_thread_api_comment_ai_summarize_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ThreadSummaryRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/comment-ai/stale-prs/{owner}/{repo}:
    get:
      tags:
      - comment-thread-ai
      summary: Detect Stale
      description: Find PRs with no activity in N days.
      operationId: detect_stale_api_comment_ai_stale_prs__owner___repo__get
      parameters:
      - name: owner
        in: path
        required: true
        schema:
          type: string
          title: Owner
      - name: repo
        in: path
        required: true
        schema:
          type: string
          title: Repo
      - name: days
        in: query
        required: false
        schema:
          type: integer
          default: 7
          title: Days
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/comment-ai/webhook:
    post:
      tags:
      - comment-thread-ai
      summary: Github Webhook
      description: GitHub webhook receiver for PR comment events.
      operationId: github_webhook_api_comment_ai_webhook_post
      parameters:
      - name: x-github-event
        in: header
        required: false
        schema:
          type: string
          default: ping
          title: X-Github-Event
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/test-gen/generate:
    post:
      tags:
      - auto-test-generator
      summary: Generate Tests
      description: Generate unit tests for submitted source code.
      operationId: generate_tests_api_test_gen_generate_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TestGenRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TestGenResponse'
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/test-gen/generate-file:
    post:
      tags:
      - auto-test-generator
      summary: Generate From File
      description: Upload a source file and get back a test file.
      operationId: generate_from_file_api_test_gen_generate_file_post
      requestBody:
        content:
          multipart/form-data:
            schema:
              $ref: '#/components/schemas/Body_generate_from_file_api_test_gen_generate_file_post'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/test-gen/batch:
    post:
      tags:
      - auto-test-generator
      summary: Batch Generate
      description: Generate tests for multiple source file paths (server-side paths).
      operationId: batch_generate_api_test_gen_batch_post
      requestBody:
        content:
          application/json:
            schema:
              items:
                type: string
              type: array
              title: Paths
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/test-gen/supported-stacks:
    get:
      tags:
      - auto-test-generator
      summary: Supported Stacks
      operationId: supported_stacks_api_test_gen_supported_stacks_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/admin/tenant-limits:
    get:
      tags:
      - tenant-admin
      summary: List Tenants
      description: List all tenants with their rate limits and (optionally) live usage.
      operationId: list_tenants_api_admin_tenant_limits_get
      parameters:
      - name: include_usage
        in: query
        required: false
        schema:
          type: boolean
          default: true
          title: Include Usage
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    post:
      tags:
      - tenant-admin
      summary: Create Tenant
      description: Create a new tenant with rate limits.
      operationId: create_tenant_api_admin_tenant_limits_post
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TenantLimitCreate'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/tenant-limits/{tenant_id}:
    get:
      tags:
      - tenant-admin
      summary: Get Tenant
      description: Get a single tenant's limits and usage.
      operationId: get_tenant_api_admin_tenant_limits__tenant_id__get
      parameters:
      - name: tenant_id
        in: path
        required: true
        schema:
          type: string
          title: Tenant Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    put:
      tags:
      - tenant-admin
      summary: Update Tenant
      description: Update a tenant's rate limits or billing tier.
      operationId: update_tenant_api_admin_tenant_limits__tenant_id__put
      parameters:
      - name: tenant_id
        in: path
        required: true
        schema:
          type: string
          title: Tenant Id
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TenantLimitUpdate'
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
    delete:
      tags:
      - tenant-admin
      summary: Delete Tenant
      description: Remove a tenant.
      operationId: delete_tenant_api_admin_tenant_limits__tenant_id__delete
      parameters:
      - name: tenant_id
        in: path
        required: true
        schema:
          type: string
          title: Tenant Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/tenant-limits/{tenant_id}/usage:
    get:
      tags:
      - tenant-admin
      summary: Get Usage
      description: Get live usage stats for a tenant.
      operationId: get_usage_api_admin_tenant_limits__tenant_id__usage_get
      parameters:
      - name: tenant_id
        in: path
        required: true
        schema:
          type: string
          title: Tenant Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/tenant-limits/{tenant_id}/reset-usage:
    post:
      tags:
      - tenant-admin
      summary: Reset Usage
      description: Reset today's request/token counters for a tenant (Redis).
      operationId: reset_usage_api_admin_tenant_limits__tenant_id__reset_usage_post
      parameters:
      - name: tenant_id
        in: path
        required: true
        schema:
          type: string
          title: Tenant Id
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/tenant-limits/tiers/defaults:
    get:
      tags:
      - tenant-admin
      summary: Get Tier Defaults
      description: Return the default limits for each billing tier.
      operationId: get_tier_defaults_api_admin_tenant_limits_tiers_defaults_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/mobile/bff/orchestrate:
    post:
      tags:
      - mobile-bff
      summary: Proxy Mobile Ai Request
      description: 'BFF (Backend for Frontend) Router for Flutter Mobile Client.

        Eliminates the need for hardcoded API keys in the mobile source code.'
      operationId: proxy_mobile_ai_request_api_mobile_bff_orchestrate_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MobileChatRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/billing/wallet:
    get:
      tags:
      - Billing & Credit Wallet
      summary: Get Wallet Balance
      operationId: get_wallet_balance_api_billing_wallet_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/billing/history:
    get:
      tags:
      - Billing & Credit Wallet
      summary: Get Transaction History
      operationId: get_transaction_history_api_billing_history_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/billing/add-funds:
    post:
      tags:
      - Billing & Credit Wallet
      summary: Add Funds
      operationId: add_funds_api_billing_add_funds_post
      parameters:
      - name: amount
        in: query
        required: true
        schema:
          type: number
          title: Amount
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/billing/plans:
    get:
      tags:
      - Billing & Credit Wallet
      summary: Get Subscription Plans
      operationId: get_subscription_plans_api_billing_plans_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/billing/checkout:
    post:
      tags:
      - Billing & Credit Wallet
      summary: Create Checkout Session
      operationId: create_checkout_session_api_billing_checkout_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/api__routes__billing_api__CheckoutRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/billing/webhook/stripe:
    post:
      tags:
      - Billing & Credit Wallet
      summary: Stripe Webhook
      description: Zero-Gap Stripe Webhook with strict signature validation and atomic
        DB updates.
      operationId: stripe_webhook_api_billing_webhook_stripe_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/billing/webhook/sslcommerz:
    post:
      tags:
      - Billing & Credit Wallet
      summary: Sslcommerz Webhook Listener
      description: Asynchronously processes local currency MFS payments success logs
        from SSLCommerz.
      operationId: sslcommerz_webhook_listener_api_billing_webhook_sslcommerz_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/admin/metrics/dashboard:
    get:
      tags:
      - infrastructure-metrics
      summary: Get Admin Metrics Dashboard
      description: 'Secure Admin Metrics Endpoint.

        Feeds real-time infrastructure savings data directly to the Studio Client.'
      operationId: supreme_admin_metrics_dashboard
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/admin/metrics/trigger-nightly-chaos:
    post:
      tags:
      - infrastructure-metrics
      summary: Trigger Nightly Chaos
      description: 'Secure Webhook Target for Google Cloud Scheduler.

        Triggers autonomous self-testing and loops it into the deployment gate.'
      operationId: supreme_trigger_nightly_chaos
      parameters:
      - name: x-chaos-key
        in: header
        required: false
        schema:
          type: string
          title: X-Chaos-Key
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/cloud-mesh/kill-switch:
    post:
      tags:
      - cloud-mesh
      summary: Kill Switch
      description: 'Instantly drops traffic to 0% for the targeted node and redirects
        to failover nodes.

        Used during severe infrastructure degradation.'
      operationId: kill_switch_api_admin_cloud_mesh_kill_switch_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CloudNodeTarget'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/cloud-mesh/defcon:
    post:
      tags:
      - cloud-mesh
      summary: Set Defcon
      description: 'Elevates system security level. DEFCON 1 enables strict WAF rules
        and puts

        the system into maintenance mode, locking out non-admin traffic.'
      operationId: set_defcon_api_admin_cloud_mesh_defcon_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DefconPayload'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/admin/cloud-mesh/purge-cache:
    post:
      tags:
      - cloud-mesh
      summary: Purge Cache
      description: Clears the global semantic cache (Upstash/Cloudflare) to force
        fresh AI generations.
      operationId: purge_cache_api_admin_cloud_mesh_purge_cache_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /api/admin/cloud-mesh/rotate-keys:
    post:
      tags:
      - cloud-mesh
      summary: Rotate Keys
      description: Auto-rotates API keys for a specific provider if rate limits are
        exhausted.
      operationId: rotate_keys_api_admin_cloud_mesh_rotate_keys_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CloudNodeTarget'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /api/dashboard/stream:
    get:
      tags:
      - Events
      summary: Dashboard Stream
      description: 'SSE endpoint for dashboard metrics and events.

        Yields data when published to ''dashboard_events'' channel.

        Maintains connection with a 20s heartbeat.'
      operationId: dashboard_stream_api_dashboard_stream_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /payments/plans:
    get:
      tags:
      - payments
      summary: Get Subscription Plans
      operationId: get_subscription_plans_payments_plans_get
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
  /payments/checkout:
    post:
      tags:
      - payments
      summary: Create Checkout Session
      operationId: create_checkout_session_payments_checkout_post
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/api__routes__payments__CheckoutRequest'
        required: true
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
        '422':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HTTPValidationError'
  /payments/webhook:
    post:
      tags:
      - payments
      summary: Stripe Webhook
      operationId: stripe_webhook_payments_webhook_post
      responses:
        '200':
          description: Successful Response
          content:
            application/json:
              schema: {}
components:
  schemas:
    ActionStreamRequest:
      properties:
        message:
          type: string
          title: Message
        messages:
          anyOf:
          - items:
              additionalProperties: true
              type: object
            type: array
          - type: 'null'
          title: Messages
      type: object
      required:
      - message
      title: ActionStreamRequest
    AdminFirebaseLoginRequest:
      properties:
        id_token:
          type: string
          title: Id Token
          description: Firebase ID token
      type: object
      required:
      - id_token
      title: AdminFirebaseLoginRequest
    AdminFirebaseTotpSetupRequest:
      properties:
        id_token:
          type: string
          title: Id Token
          description: Firebase ID token
      type: object
      required:
      - id_token
      title: AdminFirebaseTotpSetupRequest
    AdminFirebaseTotpVerifyRequest:
      properties:
        id_token:
          type: string
          title: Id Token
          description: Firebase ID token
        otp:
          type: string
          title: Otp
          description: TOTP MFA OTP code
      type: object
      required:
      - id_token
      - otp
      title: AdminFirebaseTotpVerifyRequest
    AdminLoginRequest:
      properties:
        password:
          type: string
          title: Password
          description: Admin password
      type: object
      required:
      - password
      title: AdminLoginRequest
    AdminVerifyRequest:
      properties:
        password:
          type: string
          title: Password
          description: Admin password
        otp:
          type: string
          title: Otp
          description: TOTP MFA OTP code
      type: object
      required:
      - password
      - otp
      title: AdminVerifyRequest
    Body_api_image_to_code_tools_image_to_code_post:
      properties:
        file:
          type: string
          contentMediaType: application/octet-stream
          title: File
        framework:
          type: string
          title: Framework
          default: react
        styling:
          type: string
          title: Styling
          default: tailwind
      type: object
      required:
      - file
      title: Body_api_image_to_code_tools_image_to_code_post
    Body_generate_api_spec_api_diagram_api_spec_post:
      properties:
        file:
          type: string
          contentMediaType: application/octet-stream
          title: File
      type: object
      required:
      - file
      title: Body_generate_api_spec_api_diagram_api_spec_post
    Body_generate_from_diagram_api_diagram_generate_post:
      properties:
        file:
          type: string
          contentMediaType: application/octet-stream
          title: File
        provider:
          type: string
          title: Provider
          default: aws
        iac_tool:
          type: string
          title: Iac Tool
          default: terraform
      type: object
      required:
      - file
      title: Body_generate_from_diagram_api_diagram_generate_post
    Body_generate_from_file_api_test_gen_generate_file_post:
      properties:
        file:
          type: string
          contentMediaType: application/octet-stream
          title: File
      type: object
      required:
      - file
      title: Body_generate_from_file_api_test_gen_generate_file_post
    Body_process_audio_api_voice_process_audio_post:
      properties:
        file:
          type: string
          contentMediaType: application/octet-stream
          title: File
      type: object
      required:
      - file
      title: Body_process_audio_api_voice_process_audio_post
    BrowseRequest:
      properties:
        url:
          type: string
          title: Url
        action:
          anyOf:
          - type: string
          - type: 'null'
          title: Action
          default: fetch
        selector:
          anyOf:
          - type: string
          - type: 'null'
          title: Selector
        text:
          anyOf:
          - type: string
          - type: 'null'
          title: Text
        wait_for:
          anyOf:
          - type: string
          - type: 'null'
          title: Wait For
      type: object
      required:
      - url
      title: BrowseRequest
    BulkDeleteRequest:
      properties:
        key_ids:
          items:
            type: integer
          type: array
          maxItems: 50
          minItems: 1
          title: Key Ids
      type: object
      required:
      - key_ids
      title: BulkDeleteRequest
    CIReportPayload:
      properties:
        run_id:
          type: integer
          title: Run Id
          description: GitHub Actions workflow run ID
        run_number:
          type: integer
          title: Run Number
          description: GitHub Actions workflow run number
        event_name:
          type: string
          title: Event Name
          description: Trigger event name (push, pr, schedule, etc.)
        actor:
          type: string
          title: Actor
          description: GHA runner user/actor who triggered the run
        workflow_name:
          type: string
          title: Workflow Name
          description: Name of the workflow
        status:
          type: string
          title: Status
          description: Status (success, failure, cancelled, etc.)
        runtime_seconds:
          type: integer
          title: Runtime Seconds
          description: Total execution time in seconds
        commit_sha:
          type: string
          title: Commit Sha
          description: Commit SHA of the run
        branch:
          type: string
          title: Branch
          description: Branch name of the run
        jobs_summary:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Jobs Summary
          description: Detailed status of all GHA jobs run
        error_logs:
          anyOf:
          - type: string
          - type: 'null'
          title: Error Logs
          description: Logs/error information for failed runs
      type: object
      required:
      - run_id
      - run_number
      - event_name
      - actor
      - workflow_name
      - status
      - runtime_seconds
      - commit_sha
      - branch
      title: CIReportPayload
    ChatMessage:
      properties:
        role:
          type: string
          title: Role
        content:
          type: string
          title: Content
      type: object
      required:
      - role
      - content
      title: ChatMessage
    ChatStreamRequest:
      properties:
        message:
          type: string
          title: Message
        sessionId:
          anyOf:
          - type: string
          - type: 'null'
          title: Sessionid
        messages:
          anyOf:
          - items:
              additionalProperties: true
              type: object
            type: array
          - type: 'null'
          title: Messages
        context:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Context
      type: object
      required:
      - message
      title: ChatStreamRequest
    CheckpointResponse:
      properties:
        task_id:
          type: string
          title: Task Id
        step_index:
          type: integer
          title: Step Index
        state:
          additionalProperties: true
          type: object
          title: State
        resumed:
          type: boolean
          title: Resumed
      type: object
      required:
      - task_id
      - step_index
      - state
      - resumed
      title: CheckpointResponse
    CheckpointSaveRequest:
      properties:
        task_id:
          type: string
          title: Task Id
          description: Unique task identifier
        step_index:
          type: integer
          minimum: 0.0
          title: Step Index
        state:
          additionalProperties: true
          type: object
          title: State
      type: object
      required:
      - task_id
      - step_index
      title: CheckpointSaveRequest
    ChunkRequest:
      properties:
        text:
          type: string
          title: Text
        session_id:
          type: string
          title: Session Id
          default: default
        max_tokens:
          type: integer
          title: Max Tokens
          default: 4000
        overlap_ratio:
          type: number
          title: Overlap Ratio
          default: 0.15
      type: object
      required:
      - text
      title: ChunkRequest
    ChunkResponse:
      properties:
        session_id:
          type: string
          title: Session Id
        windows:
          items:
            additionalProperties: true
            type: object
          type: array
          title: Windows
      type: object
      required:
      - session_id
      - windows
      title: ChunkResponse
    ClickAtRequest:
      properties:
        x:
          type: integer
          title: X
        y:
          type: integer
          title: Y
      type: object
      required:
      - x
      - y
      title: ClickAtRequest
    ClickRequest:
      properties:
        selector:
          type: string
          title: Selector
      type: object
      required:
      - selector
      title: ClickRequest
    CloudNodeTarget:
      properties:
        target_node:
          type: string
          title: Target Node
      type: object
      required:
      - target_node
      title: CloudNodeTarget
    CodeFlowEdge:
      properties:
        source:
          type: string
          title: Source
        target:
          type: string
          title: Target
        type:
          type: string
          title: Type
      type: object
      required:
      - source
      - target
      - type
      title: CodeFlowEdge
    CodeFlowNode:
      properties:
        id:
          type: string
          title: Id
        label:
          type: string
          title: Label
        kind:
          type: string
          title: Kind
      type: object
      required:
      - id
      - label
      - kind
      title: CodeFlowNode
    CodeFlowRequest:
      properties:
        path:
          type: string
          title: Path
        depth:
          anyOf:
          - type: integer
          - type: 'null'
          title: Depth
          default: 1
      type: object
      required:
      - path
      title: CodeFlowRequest
    CodeFlowResponse:
      properties:
        path:
          type: string
          title: Path
        nodes:
          items:
            $ref: '#/components/schemas/CodeFlowNode'
          type: array
          title: Nodes
        edges:
          items:
            $ref: '#/components/schemas/CodeFlowEdge'
          type: array
          title: Edges
      type: object
      required:
      - path
      - nodes
      - edges
      title: CodeFlowResponse
    CompareRequest:
      properties:
        clone_url:
          anyOf:
          - type: string
          - type: 'null'
          title: Clone Url
        root_dir:
          type: string
          title: Root Dir
          default: .
        range_a_since:
          anyOf:
          - type: string
          - type: 'null'
          title: Range A Since
        range_a_until:
          anyOf:
          - type: string
          - type: 'null'
          title: Range A Until
        range_b_since:
          anyOf:
          - type: string
          - type: 'null'
          title: Range B Since
        range_b_until:
          anyOf:
          - type: string
          - type: 'null'
          title: Range B Until
      type: object
      title: CompareRequest
    CompletionRequest:
      properties:
        prefix:
          type: string
          title: Prefix
        suffix:
          type: string
          title: Suffix
        filePath:
          type: string
          title: Filepath
        language:
          type: string
          title: Language
        sessionId:
          anyOf:
          - type: string
          - type: 'null'
          title: Sessionid
      type: object
      required:
      - prefix
      - suffix
      - filePath
      - language
      title: CompletionRequest
    CompletionResponse:
      properties:
        success:
          type: boolean
          title: Success
        suggestions:
          items:
            type: string
          type: array
          title: Suggestions
      type: object
      required:
      - success
      - suggestions
      title: CompletionResponse
    ConnectRequest:
      properties:
        installation_id:
          anyOf:
          - type: string
          - type: 'null'
          title: Installation Id
        repo_owner:
          type: string
          title: Repo Owner
        repo_name:
          type: string
          title: Repo Name
      type: object
      required:
      - repo_owner
      - repo_name
      title: ConnectRequest
    ContextRequest:
      properties:
        documents:
          items:
            type: string
          type: array
          title: Documents
        query:
          type: string
          title: Query
          default: ''
        session_id:
          type: string
          title: Session Id
          default: default
        budget:
          anyOf:
          - type: integer
          - type: 'null'
          title: Budget
      type: object
      title: ContextRequest
    ContextResponse:
      properties:
        session_id:
          type: string
          title: Session Id
        context:
          type: string
          title: Context
      type: object
      required:
      - session_id
      - context
      title: ContextResponse
    CreateAPIKeyRequest:
      properties:
        user_id:
          type: string
          minLength: 1
          title: User Id
          description: Owner user ID (email or uid)
        name:
          type: string
          maxLength: 255
          minLength: 1
          title: Name
        rate_limit_rps:
          type: integer
          maximum: 1000.0
          minimum: 1.0
          title: Rate Limit Rps
          default: 6
        expires_in_days:
          anyOf:
          - type: integer
            minimum: 1.0
          - type: 'null'
          title: Expires In Days
          description: Expires in N days, null = no expiry
      type: object
      required:
      - user_id
      - name
      title: CreateAPIKeyRequest
    CredentialRequest:
      properties:
        serviceName:
          type: string
          title: Servicename
        username:
          type: string
          title: Username
        password:
          type: string
          title: Password
        userId:
          anyOf:
          - type: string
          - type: 'null'
          title: Userid
          default: default
      type: object
      required:
      - serviceName
      - username
      - password
      title: CredentialRequest
    DecisionRequest:
      properties:
        approved:
          type: boolean
          title: Approved
      type: object
      required:
      - approved
      title: DecisionRequest
    DefconPayload:
      properties:
        level:
          type: integer
          title: Level
        reason:
          type: string
          title: Reason
      type: object
      required:
      - level
      - reason
      title: DefconPayload
    DeployComposeRequest:
      properties:
        overrides:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Overrides
      type: object
      title: DeployComposeRequest
    DeployHelmRequest:
      properties:
        release_name:
          type: string
          title: Release Name
          default: supremeai
        namespace:
          type: string
          title: Namespace
          default: default
        replicas:
          type: integer
          title: Replicas
          default: 3
        image_tag:
          type: string
          title: Image Tag
          default: latest
      type: object
      title: DeployHelmRequest
    DeployResponse:
      properties:
        output_path:
          type: string
          title: Output Path
        format:
          type: string
          title: Format
      type: object
      required:
      - output_path
      - format
      title: DeployResponse
    DeviceUpdateRequest:
      properties:
        type:
          type: string
          title: Type
        osVersion:
          anyOf:
          - type: string
          - type: 'null'
          title: Osversion
        screenResolution:
          anyOf:
          - type: string
          - type: 'null'
          title: Screenresolution
        densityDpi:
          anyOf:
          - type: integer
          - type: 'null'
          title: Densitydpi
      type: object
      required:
      - type
      title: DeviceUpdateRequest
    DiscoverRequest:
      properties:
        requirement:
          type: string
          title: Requirement
        tech_stack:
          items:
            type: string
          type: array
          title: Tech Stack
        criteria:
          additionalProperties: true
          type: object
          title: Criteria
      type: object
      required:
      - requirement
      - tech_stack
      - criteria
      title: DiscoverRequest
    DomainAdaptRequest:
      properties:
        domain:
          type: string
          title: Domain
        prompt:
          type: string
          title: Prompt
        context:
          anyOf:
          - type: string
          - type: 'null'
          title: Context
      type: object
      required:
      - domain
      - prompt
      title: DomainAdaptRequest
    DomainAdaptResponse:
      properties:
        domain:
          type: string
          title: Domain
        response:
          type: string
          title: Response
        disclaimer:
          type: string
          title: Disclaimer
        model:
          type: string
          title: Model
        provider:
          type: string
          title: Provider
      type: object
      required:
      - domain
      - response
      - disclaimer
      - model
      - provider
      title: DomainAdaptResponse
    DrugInteractionRequest:
      properties:
        medications:
          items:
            type: string
          type: array
          title: Medications
      type: object
      required:
      - medications
      title: DrugInteractionRequest
    EvolutionRequest:
      properties:
        skill_name:
          type: string
          title: Skill Name
        user_demand:
          type: string
          title: User Demand
      type: object
      required:
      - skill_name
      - user_demand
      title: EvolutionRequest
    FeedbackEvent:
      properties:
        event_type:
          type: string
          title: Event Type
        payload:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Payload
      type: object
      required:
      - event_type
      title: FeedbackEvent
    FeedbackResponse:
      properties:
        success:
          type: boolean
          title: Success
        event_id:
          anyOf:
          - type: integer
          - type: 'null'
          title: Event Id
      type: object
      required:
      - success
      title: FeedbackResponse
    FillRequest:
      properties:
        selector:
          type: string
          title: Selector
        value:
          type: string
          title: Value
      type: object
      required:
      - selector
      - value
      title: FillRequest
    FlowEdge:
      properties:
        id:
          type: string
          title: Id
          description: Unique ID of the edge
        source:
          type: string
          title: Source
          description: Source node ID
        target:
          type: string
          title: Target
          description: Target node ID
        animated:
          anyOf:
          - type: boolean
          - type: 'null'
          title: Animated
          description: Whether the edge is animated
          default: false
      type: object
      required:
      - id
      - source
      - target
      title: FlowEdge
    FlowNode:
      properties:
        id:
          type: string
          title: Id
          description: Unique ID of the node
        type:
          type: string
          title: Type
          description: Type of the node (e.g., agentNode, taskNode)
        position:
          $ref: '#/components/schemas/FlowPosition'
        data:
          additionalProperties: true
          type: object
          title: Data
          description: Node payload containing role, model, prompt, etc.
      type: object
      required:
      - id
      - type
      - position
      title: FlowNode
    FlowPosition:
      properties:
        x:
          type: number
          title: X
        y:
          type: number
          title: Y
      type: object
      required:
      - x
      - y
      title: FlowPosition
    ForgePayload:
      properties:
        name:
          type: string
          title: Name
          description: Name of the custom swarm flow
        description:
          anyOf:
          - type: string
          - type: 'null'
          title: Description
          description: Optional description of the swarm's purpose
          default: ''
        nodes:
          items:
            $ref: '#/components/schemas/FlowNode'
          type: array
          title: Nodes
        edges:
          items:
            $ref: '#/components/schemas/FlowEdge'
          type: array
          title: Edges
      type: object
      required:
      - name
      - nodes
      - edges
      title: ForgePayload
    GateOverridePayload:
      properties:
        target_status:
          type: string
          title: Target Status
          description: Must be 'UNLOCKED' or 'LOCKED'
        reason:
          type: string
          minLength: 10
          title: Reason
          description: Detailed justification for manual bypass
        admin_secret:
          type: string
          title: Admin Secret
          description: Master JWT/Vault secret key for authentication
      type: object
      required:
      - target_status
      - reason
      - admin_secret
      title: GateOverridePayload
    GmailAuthRequest:
      properties:
        provider:
          type: string
          title: Provider
        scopes:
          items:
            type: string
          type: array
          title: Scopes
      type: object
      required:
      - provider
      - scopes
      title: GmailAuthRequest
    GoalRequest:
      properties:
        goal:
          type: string
          title: Goal
      type: object
      required:
      - goal
      title: GoalRequest
    HTTPValidationError:
      properties:
        detail:
          items:
            $ref: '#/components/schemas/ValidationError'
          type: array
          title: Detail
      type: object
      title: HTTPValidationError
    HealthRequest:
      properties:
        agent_ids:
          items:
            type: string
          type: array
          title: Agent Ids
      type: object
      required:
      - agent_ids
      title: HealthRequest
    ImapAuthRequest:
      properties:
        host:
          type: string
          title: Host
        port:
          type: integer
          title: Port
        username:
          type: string
          title: Username
        app_password:
          type: string
          title: App Password
      type: object
      required:
      - host
      - port
      - username
      - app_password
      title: ImapAuthRequest
    ImplementRequest:
      properties:
        repo_url:
          type: string
          title: Repo Url
        integration_method:
          type: string
          title: Integration Method
        target_project:
          type: string
          title: Target Project
      type: object
      required:
      - repo_url
      - integration_method
      - target_project
      title: ImplementRequest
    ImproveRequest:
      properties:
        repo:
          anyOf:
          - type: string
          - type: 'null'
          title: Repo
        branch:
          type: string
          title: Branch
        improvement_type:
          type: string
          title: Improvement Type
      type: object
      required:
      - branch
      - improvement_type
      title: ImproveRequest
    IssueRequest:
      properties:
        issue_description:
          type: string
          title: Issue Description
        repo:
          anyOf:
          - type: string
          - type: 'null'
          title: Repo
        branch:
          anyOf:
          - type: string
          - type: 'null'
          title: Branch
          default: main
        create_pr:
          type: boolean
          title: Create Pr
          default: false
      type: object
      required:
      - issue_description
      title: IssueRequest
    KeyRequest:
      properties:
        key:
          type: string
          title: Key
      type: object
      required:
      - key
      title: KeyRequest
    KnowledgeSearchRequest:
      properties:
        query:
          type: string
          title: Query
        limit:
          type: integer
          title: Limit
          default: 5
        use_fts:
          type: boolean
          title: Use Fts
          default: true
      type: object
      required:
      - query
      title: KnowledgeSearchRequest
    KnowledgeSearchResult:
      properties:
        id:
          type: string
          title: Id
        title:
          type: string
          title: Title
        content:
          type: string
          title: Content
        score:
          anyOf:
          - type: number
          - type: 'null'
          title: Score
        source:
          anyOf:
          - type: string
          - type: 'null'
          title: Source
      type: object
      required:
      - id
      - title
      - content
      title: KnowledgeSearchResult
    LearnRequest:
      properties:
        prompt:
          type: string
          title: Prompt
        working_code:
          type: string
          title: Working Code
      type: object
      required:
      - prompt
      - working_code
      title: LearnRequest
    LegalAnalysisRequest:
      properties:
        document_text:
          type: string
          title: Document Text
        doc_type:
          type: string
          title: Doc Type
          default: contract
      type: object
      required:
      - document_text
      title: LegalAnalysisRequest
    LoginRequest:
      properties:
        username:
          type: string
          title: Username
        password:
          type: string
          title: Password
      type: object
      required:
      - username
      - password
      title: LoginRequest
    MarkdownExportRequest:
      properties:
        root_dir:
          type: string
          title: Root Dir
          default: .
        time_since:
          anyOf:
          - type: string
          - type: 'null'
          title: Time Since
        time_until:
          anyOf:
          - type: string
          - type: 'null'
          title: Time Until
        git_diff_only:
          type: boolean
          title: Git Diff Only
          default: false
        clone_url:
          anyOf:
          - type: string
          - type: 'null'
          title: Clone Url
      type: object
      title: MarkdownExportRequest
    MeResponse:
      properties:
        user_id:
          type: string
          title: User Id
        role:
          type: string
          title: Role
        scopes:
          items:
            type: string
          type: array
          title: Scopes
          default: []
      type: object
      required:
      - user_id
      - role
      title: MeResponse
    MobileChatRequest:
      properties:
        message:
          type: string
          title: Message
        history:
          items:
            additionalProperties:
              type: string
            type: object
          type: array
          title: History
          default: []
        model_preference:
          type: string
          title: Model Preference
          default: gemini-1.5-flash
      type: object
      required:
      - message
      title: MobileChatRequest
    NavigateRequest:
      properties:
        url:
          type: string
          title: Url
      type: object
      required:
      - url
      title: NavigateRequest
    OIDCCallbackRequest:
      properties:
        code:
          type: string
          title: Code
        state:
          type: string
          title: State
        provider:
          type: string
          title: Provider
          default: generic
      type: object
      required:
      - code
      - state
      title: OIDCCallbackRequest
    OIDCDiscoveryRequest:
      properties:
        issuer:
          type: string
          title: Issuer
        redirect_uri:
          type: string
          title: Redirect Uri
        client_id:
          type: string
          title: Client Id
        scope:
          type: string
          title: Scope
          default: openid profile email
      type: object
      required:
      - issuer
      - redirect_uri
      - client_id
      title: OIDCDiscoveryRequest
    OIDCLoginResponse:
      properties:
        authorization_url:
          type: string
          title: Authorization Url
        state:
          type: string
          title: State
      type: object
      required:
      - authorization_url
      - state
      title: OIDCLoginResponse
    OnboardingPayload:
      properties:
        user_id:
          type: string
          title: User Id
        provider:
          type: string
          title: Provider
          default: openrouter
        api_key:
          type: string
          title: Api Key
        default_model:
          anyOf:
          - type: string
          - type: 'null'
          title: Default Model
          default: gpt-4o-mini
        theme:
          anyOf:
          - type: string
          - type: 'null'
          title: Theme
          default: dark
        language:
          anyOf:
          - type: string
          - type: 'null'
          title: Language
          default: en
        first_chat_sent:
          type: boolean
          title: First Chat Sent
          default: false
      type: object
      required:
      - user_id
      - api_key
      title: OnboardingPayload
    OnboardingResponse:
      properties:
        status:
          type: string
          title: Status
        user_id:
          type: string
          title: User Id
        provider_valid:
          type: boolean
          title: Provider Valid
        model_ready:
          type: boolean
          title: Model Ready
        message:
          type: string
          title: Message
        setup_complete:
          type: boolean
          title: Setup Complete
      type: object
      required:
      - status
      - user_id
      - provider_valid
      - model_ready
      - message
      - setup_complete
      title: OnboardingResponse
    PRCommentPayload:
      properties:
        repo_full_name:
          type: string
          title: Repo Full Name
        pr_number:
          type: integer
          title: Pr Number
        comment_body:
          type: string
          title: Comment Body
        file_path:
          anyOf:
          - type: string
          - type: 'null'
          title: File Path
        line_number:
          anyOf:
          - type: integer
          - type: 'null'
          title: Line Number
        comment_id:
          anyOf:
          - type: integer
          - type: 'null'
          title: Comment Id
        auto_reply:
          type: boolean
          title: Auto Reply
          default: true
      type: object
      required:
      - repo_full_name
      - pr_number
      - comment_body
      title: PRCommentPayload
    PRRequest:
      properties:
        user_id:
          type: string
          title: User Id
        repo_name:
          type: string
          title: Repo Name
        file_path:
          type: string
          title: File Path
        code:
          type: string
          title: Code
        prompt:
          type: string
          title: Prompt
      type: object
      required:
      - user_id
      - repo_name
      - file_path
      - code
      - prompt
      title: PRRequest
    PatchTelemetry:
      properties:
        error_id:
          type: string
          title: Error Id
          description: Unique ID for the intercepted error
        patch_id:
          type: string
          title: Patch Id
          description: Unique ID for the generated patch
        file_path:
          type: string
          title: File Path
          description: Path of the healed file
        status:
          type: string
          title: Status
          description: '''ACCEPTED'', ''REJECTED'', or ''MODIFIED'''
        similarity_score:
          type: number
          title: Similarity Score
          description: Levenshtein similarity score (0.0 to 1.0)
      type: object
      required:
      - error_id
      - patch_id
      - file_path
      - status
      - similarity_score
      title: PatchTelemetry
    PreferenceUpdate:
      properties:
        theme:
          anyOf:
          - type: string
          - type: 'null'
          title: Theme
        default_model:
          anyOf:
          - type: string
          - type: 'null'
          title: Default Model
        max_tokens:
          anyOf:
          - type: integer
          - type: 'null'
          title: Max Tokens
        auto_save:
          anyOf:
          - type: boolean
          - type: 'null'
          title: Auto Save
        custom_shortcuts:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Custom Shortcuts
        verbosity:
          anyOf:
          - type: string
          - type: 'null'
          title: Verbosity
        preferred_frameworks:
          anyOf:
          - items:
              type: string
            type: array
          - type: 'null'
          title: Preferred Frameworks
      type: object
      title: PreferenceUpdate
    ProfileUpdateRequest:
      properties:
        installQuota:
          anyOf:
          - type: integer
          - type: 'null'
          title: Installquota
        device:
          anyOf:
          - $ref: '#/components/schemas/DeviceUpdateRequest'
          - type: 'null'
      type: object
      title: ProfileUpdateRequest
    ProviderSSORequest:
      properties:
        client_id:
          anyOf:
          - type: string
          - type: 'null'
          title: Client Id
        redirect_uri:
          anyOf:
          - type: string
          - type: 'null'
          title: Redirect Uri
        state:
          anyOf:
          - type: string
          - type: 'null'
          title: State
        scope:
          type: string
          title: Scope
          default: openid profile email
      type: object
      title: ProviderSSORequest
    PublicConfigResponse:
      properties:
        adminEmail:
          type: string
          title: Adminemail
        maxConcurrency:
          type: integer
          title: Maxconcurrency
        features:
          additionalProperties:
            type: boolean
          type: object
          title: Features
      type: object
      required:
      - adminEmail
      - maxConcurrency
      - features
      title: PublicConfigResponse
    PushRequest:
      properties:
        repo:
          anyOf:
          - type: string
          - type: 'null'
          title: Repo
        branch:
          type: string
          title: Branch
          default: main
        commit_message:
          type: string
          title: Commit Message
          default: 'AI: Automated improvements'
        files_changed:
          items:
            type: string
          type: array
          title: Files Changed
      type: object
      required:
      - files_changed
      title: PushRequest
    QuarantineRequest:
      properties:
        skill_name:
          type: string
          maxLength: 200
          minLength: 1
          title: Skill Name
      type: object
      required:
      - skill_name
      title: QuarantineRequest
    RepoCreate:
      properties:
        id:
          type: string
          title: Id
        name:
          type: string
          title: Name
        url:
          type: string
          title: Url
        description:
          type: string
          title: Description
          default: ''
        language:
          type: string
          title: Language
          default: ''
        category:
          anyOf:
          - type: string
          - type: 'null'
          title: Category
        priority:
          anyOf:
          - type: string
          - type: 'null'
          title: Priority
          default: medium
        purpose:
          anyOf:
          - type: string
          - type: 'null'
          title: Purpose
        install_command:
          anyOf:
          - type: string
          - type: 'null'
          title: Install Command
        status:
          anyOf:
          - type: string
          - type: 'null'
          title: Status
          default: active
        metadata:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Metadata
      type: object
      required:
      - id
      - name
      - url
      title: RepoCreate
    RepoUpdate:
      properties:
        name:
          anyOf:
          - type: string
          - type: 'null'
          title: Name
        url:
          anyOf:
          - type: string
          - type: 'null'
          title: Url
        description:
          anyOf:
          - type: string
          - type: 'null'
          title: Description
        language:
          anyOf:
          - type: string
          - type: 'null'
          title: Language
        category:
          anyOf:
          - type: string
          - type: 'null'
          title: Category
        priority:
          anyOf:
          - type: string
          - type: 'null'
          title: Priority
        purpose:
          anyOf:
          - type: string
          - type: 'null'
          title: Purpose
        install_command:
          anyOf:
          - type: string
          - type: 'null'
          title: Install Command
        status:
          anyOf:
          - type: string
          - type: 'null'
          title: Status
        metadata:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Metadata
      type: object
      title: RepoUpdate
    ResearchRequest:
      properties:
        query:
          type: string
          title: Query
        source:
          type: string
          title: Source
          default: arxiv
        max_results:
          type: integer
          title: Max Results
          default: 5
      type: object
      required:
      - query
      title: ResearchRequest
    RotateAPIKeyRequest:
      properties:
        old_key:
          type: string
          minLength: 1
          title: Old Key
        grace_period_hours:
          type: integer
          maximum: 168.0
          minimum: 0.0
          title: Grace Period Hours
          default: 24
      type: object
      required:
      - old_key
      title: RotateAPIKeyRequest
    RouterOverride:
      properties:
        provider:
          type: string
          title: Provider
        model:
          type: string
          title: Model
        remaining_requests:
          type: integer
          title: Remaining Requests
          default: 100
      type: object
      required:
      - provider
      - model
      title: RouterOverride
    RouterOverrideRequest:
      properties:
        provider:
          type: string
          title: Provider
        model:
          type: string
          title: Model
        remaining_requests:
          type: integer
          title: Remaining Requests
      type: object
      required:
      - provider
      - model
      - remaining_requests
      title: RouterOverrideRequest
    RuleUpdate:
      properties:
        key:
          type: string
          title: Key
        value:
          type: string
          title: Value
      type: object
      required:
      - key
      - value
      title: RuleUpdate
    RulesPayload:
      properties:
        rules:
          additionalProperties: true
          type: object
          title: Rules
      type: object
      required:
      - rules
      title: RulesPayload
    RunEvolutionRequest:
      properties:
        task_history:
          anyOf:
          - items:
              additionalProperties: true
              type: object
            type: array
          - type: 'null'
          title: Task History
        days:
          anyOf:
          - type: integer
          - type: 'null'
          title: Days
          default: 7
      type: object
      title: RunEvolutionRequest
    SAMLAssertionRequest:
      properties:
        assertion:
          type: string
          title: Assertion
      type: object
      required:
      - assertion
      title: SAMLAssertionRequest
    SSOLoginResponse:
      properties:
        access_token:
          type: string
          title: Access Token
        token_type:
          type: string
          title: Token Type
          default: bearer
        user_id:
          type: string
          title: User Id
        roles:
          items:
            type: string
          type: array
          title: Roles
        email:
          type: string
          title: Email
        method:
          type: string
          title: Method
      type: object
      required:
      - access_token
      - user_id
      - roles
      - email
      - method
      title: SSOLoginResponse
    SearchRequest:
      properties:
        query:
          type: string
          title: Query
        installed_only:
          type: boolean
          title: Installed Only
          default: false
      type: object
      required:
      - query
      title: SearchRequest
    SelfHealingRequest:
      properties:
        filePath:
          type: string
          title: Filepath
        message:
          type: string
          title: Message
        lineNumber:
          type: integer
          title: Linenumber
        codeContext:
          type: string
          title: Codecontext
        languageId:
          type: string
          title: Languageid
      type: object
      required:
      - filePath
      - message
      - lineNumber
      - codeContext
      - languageId
      title: SelfHealingRequest
    SessionIn:
      properties:
        id:
          type: string
          title: Id
        title:
          type: string
          title: Title
        status:
          type: string
          title: Status
          default: running
        created_at:
          type: string
          title: Created At
          default: ''
        updated_at:
          type: string
          title: Updated At
          default: ''
        messages:
          items:
            $ref: '#/components/schemas/SessionMessageIn'
          type: array
          title: Messages
          default: []
      type: object
      required:
      - id
      - title
      title: SessionIn
    SessionMessageIn:
      properties:
        id:
          type: integer
          title: Id
        sender:
          type: string
          title: Sender
        text:
          type: string
          title: Text
        timestamp:
          type: string
          title: Timestamp
      type: object
      required:
      - id
      - sender
      - text
      - timestamp
      title: SessionMessageIn
    ShareRequest:
      properties:
        markdown:
          type: string
          title: Markdown
        target_ai:
          type: string
          title: Target Ai
          default: claude
      type: object
      required:
      - markdown
      title: ShareRequest
    SiteActionIn:
      properties:
        site_name:
          type: string
          title: Site Name
        url_pattern:
          type: string
          title: Url Pattern
        action_name:
          type: string
          title: Action Name
        selector:
          type: string
          title: Selector
        action_type:
          type: string
          title: Action Type
          default: click
        notes:
          type: string
          title: Notes
          default: ''
        enabled:
          type: boolean
          title: Enabled
          default: true
        fallback_selectors:
          items:
            type: string
          type: array
          title: Fallback Selectors
          default: []
        selector_strategy:
          type: string
          title: Selector Strategy
          default: exact
        health_score:
          type: integer
          title: Health Score
          default: 100
      type: object
      required:
      - site_name
      - url_pattern
      - action_name
      - selector
      title: SiteActionIn
    SkillRecRequest:
      properties:
        user_id:
          type: string
          title: User Id
        task_description:
          type: string
          title: Task Description
        top_k:
          type: integer
          title: Top K
          default: 5
      type: object
      required:
      - user_id
      - task_description
      title: SkillRecRequest
    SkillRecResponse:
      properties:
        user_id:
          type: string
          title: User Id
        task:
          type: string
          title: Task
        recommendations:
          items:
            additionalProperties: true
            type: object
          type: array
          title: Recommendations
        count:
          type: integer
          title: Count
      type: object
      required:
      - user_id
      - task
      - recommendations
      - count
      title: SkillRecResponse
    SkillResponse:
      properties:
        id:
          type: string
          title: Id
        name:
          type: string
          title: Name
        version:
          type: string
          title: Version
        description:
          anyOf:
          - type: string
          - type: 'null'
          title: Description
        dependencies:
          anyOf:
          - type: string
          - type: 'null'
          title: Dependencies
        installed:
          type: boolean
          title: Installed
        source:
          type: string
          title: Source
      type: object
      required:
      - id
      - name
      - version
      - description
      - dependencies
      - installed
      - source
      title: SkillResponse
    SmellCheckRequest:
      properties:
        path:
          type: string
          title: Path
        thresholds:
          anyOf:
          - additionalProperties:
              type: integer
            type: object
          - type: 'null'
          title: Thresholds
      type: object
      required:
      - path
      title: SmellCheckRequest
    SmellCheckResponse:
      properties:
        path:
          type: string
          title: Path
        smells:
          items:
            additionalProperties: true
            type: object
          type: array
          title: Smells
        summary:
          additionalProperties:
            type: integer
          type: object
          title: Summary
      type: object
      required:
      - path
      - smells
      - summary
      title: SmellCheckResponse
    StreamRequest:
      properties:
        prompt:
          type: string
          title: Prompt
        task_type:
          type: string
          title: Task Type
          default: general
        max_cost:
          type: number
          title: Max Cost
          default: 0.01
      type: object
      required:
      - prompt
      title: StreamRequest
    StyleRequest:
      properties:
        repo_path:
          type: string
          title: Repo Path
        language:
          anyOf:
          - type: string
          - type: 'null'
          title: Language
          default: python
      type: object
      required:
      - repo_path
      title: StyleRequest
    SummarizeRequest:
      properties:
        paper:
          additionalProperties: true
          type: object
          title: Paper
        style:
          type: string
          title: Style
          default: apa
      type: object
      required:
      - paper
      title: SummarizeRequest
    SymptomRequest:
      properties:
        symptoms:
          type: string
          title: Symptoms
        age:
          anyOf:
          - type: integer
          - type: 'null'
          title: Age
        medical_history:
          anyOf:
          - type: string
          - type: 'null'
          title: Medical History
      type: object
      required:
      - symptoms
      title: SymptomRequest
    TTSRequest:
      properties:
        text:
          type: string
          title: Text
        language:
          anyOf:
          - type: string
          - type: 'null'
          title: Language
        voice_id:
          anyOf:
          - type: string
          - type: 'null'
          title: Voice Id
        provider:
          anyOf:
          - type: string
          - type: 'null'
          title: Provider
          default: auto
        stability:
          type: number
          title: Stability
          default: 0.5
        similarity_boost:
          type: number
          title: Similarity Boost
          default: 0.75
        output_format:
          anyOf:
          - type: string
          - type: 'null'
          title: Output Format
          default: mp3
      type: object
      required:
      - text
      title: TTSRequest
    TTSResponse:
      properties:
        status:
          type: string
          title: Status
        language:
          type: string
          title: Language
        provider:
          type: string
          title: Provider
        audio_url:
          type: string
          title: Audio Url
        text_length:
          type: integer
          title: Text Length
        cached:
          type: boolean
          title: Cached
          default: false
        error:
          anyOf:
          - type: string
          - type: 'null'
          title: Error
      type: object
      required:
      - status
      - language
      - provider
      - audio_url
      - text_length
      title: TTSResponse
    TaskPayload:
      properties:
        task:
          type: string
          title: Task
        task_type:
          type: string
          title: Task Type
          default: general
        messages:
          items:
            $ref: '#/components/schemas/ChatMessage'
          type: array
          title: Messages
          default: []
      type: object
      required:
      - task
      title: TaskPayload
    TaskRequest:
      properties:
        task:
          type: string
          title: Task
        task_type:
          type: string
          title: Task Type
          default: general
        max_cost:
          type: number
          title: Max Cost
          default: 0.01
        admin_token:
          anyOf:
          - type: string
          - type: 'null'
          title: Admin Token
        schema_name:
          anyOf:
          - type: string
          - type: 'null'
          title: Schema Name
        messages:
          anyOf:
          - items:
              additionalProperties: true
              type: object
            type: array
          - type: 'null'
          title: Messages
        session_id:
          anyOf:
          - type: string
          - type: 'null'
          title: Session Id
      type: object
      required:
      - task
      title: TaskRequest
    TenantLimitCreate:
      properties:
        tenant_id:
          type: string
          title: Tenant Id
        org_name:
          type: string
          title: Org Name
          default: ''
        billing_tier:
          type: string
          title: Billing Tier
          default: free
        requests_per_minute:
          anyOf:
          - type: integer
          - type: 'null'
          title: Requests Per Minute
        max_tokens_per_day:
          anyOf:
          - type: integer
          - type: 'null'
          title: Max Tokens Per Day
        max_concurrent_sessions:
          anyOf:
          - type: integer
          - type: 'null'
          title: Max Concurrent Sessions
        stripe_customer_id:
          anyOf:
          - type: string
          - type: 'null'
          title: Stripe Customer Id
        notes:
          anyOf:
          - type: string
          - type: 'null'
          title: Notes
      type: object
      required:
      - tenant_id
      title: TenantLimitCreate
    TenantLimitUpdate:
      properties:
        org_name:
          anyOf:
          - type: string
          - type: 'null'
          title: Org Name
        billing_tier:
          anyOf:
          - type: string
          - type: 'null'
          title: Billing Tier
        requests_per_minute:
          anyOf:
          - type: integer
          - type: 'null'
          title: Requests Per Minute
        max_tokens_per_day:
          anyOf:
          - type: integer
          - type: 'null'
          title: Max Tokens Per Day
        max_concurrent_sessions:
          anyOf:
          - type: integer
          - type: 'null'
          title: Max Concurrent Sessions
        stripe_customer_id:
          anyOf:
          - type: string
          - type: 'null'
          title: Stripe Customer Id
        notes:
          anyOf:
          - type: string
          - type: 'null'
          title: Notes
      type: object
      title: TenantLimitUpdate
    TestGenRequest:
      properties:
        source_code:
          type: string
          title: Source Code
        file_path:
          type: string
          title: File Path
          default: unknown.py
        stack:
          anyOf:
          - type: string
          - type: 'null'
          title: Stack
        framework:
          anyOf:
          - type: string
          - type: 'null'
          title: Framework
        coverage_target:
          type: integer
          title: Coverage Target
          default: 80
        include_mocks:
          type: boolean
          title: Include Mocks
          default: true
        include_edge_cases:
          type: boolean
          title: Include Edge Cases
          default: true
      type: object
      required:
      - source_code
      title: TestGenRequest
    TestGenResponse:
      properties:
        status:
          type: string
          title: Status
        file_path:
          type: string
          title: File Path
        stack:
          type: string
          title: Stack
        framework:
          type: string
          title: Framework
        test_code:
          type: string
          title: Test Code
        test_file_path:
          type: string
          title: Test File Path
        functions_found:
          type: integer
          title: Functions Found
        coverage_estimate:
          type: integer
          title: Coverage Estimate
        error:
          anyOf:
          - type: string
          - type: 'null'
          title: Error
      type: object
      required:
      - status
      - file_path
      - stack
      - framework
      - test_code
      - test_file_path
      - functions_found
      - coverage_estimate
      title: TestGenResponse
    TestSelectorRequest:
      properties:
        action_id:
          type: integer
          title: Action Id
      type: object
      required:
      - action_id
      title: TestSelectorRequest
    ThreadSummaryRequest:
      properties:
        repo_full_name:
          type: string
          title: Repo Full Name
        pr_number:
          anyOf:
          - type: integer
          - type: 'null'
          title: Pr Number
        issue_number:
          anyOf:
          - type: integer
          - type: 'null'
          title: Issue Number
      type: object
      required:
      - repo_full_name
      title: ThreadSummaryRequest
    TokenResponse:
      properties:
        access_token:
          type: string
          title: Access Token
        token_type:
          type: string
          title: Token Type
          default: bearer
        user_id:
          type: string
          title: User Id
        role:
          type: string
          title: Role
      type: object
      required:
      - access_token
      - user_id
      - role
      title: TokenResponse
    ToolCreate:
      properties:
        id:
          type: string
          title: Id
        name:
          type: string
          title: Name
        file_path:
          type: string
          title: File Path
        category:
          anyOf:
          - type: string
          - type: 'null'
          title: Category
        dependencies:
          anyOf:
          - items:
              type: string
            type: array
          - type: 'null'
          title: Dependencies
        cost_per_call:
          anyOf:
          - type: number
          - type: 'null'
          title: Cost Per Call
          default: 0.0
        description:
          anyOf:
          - type: string
          - type: 'null'
          title: Description
        config_schema:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Config Schema
      type: object
      required:
      - id
      - name
      - file_path
      title: ToolCreate
    ToolUpdate:
      properties:
        name:
          anyOf:
          - type: string
          - type: 'null'
          title: Name
        category:
          anyOf:
          - type: string
          - type: 'null'
          title: Category
        status:
          anyOf:
          - type: string
          - type: 'null'
          title: Status
        dependencies:
          anyOf:
          - items:
              type: string
            type: array
          - type: 'null'
          title: Dependencies
        cost_per_call:
          anyOf:
          - type: number
          - type: 'null'
          title: Cost Per Call
        description:
          anyOf:
          - type: string
          - type: 'null'
          title: Description
        config_schema:
          anyOf:
          - additionalProperties: true
            type: object
          - type: 'null'
          title: Config Schema
      type: object
      title: ToolUpdate
    TradeRequest:
      properties:
        symbol:
          type: string
          title: Symbol
        quantity:
          type: number
          title: Quantity
        price:
          anyOf:
          - type: number
          - type: 'null'
          title: Price
      type: object
      required:
      - symbol
      - quantity
      title: TradeRequest
    UploadRequest:
      properties:
        file_name:
          type: string
          title: File Name
        file_type:
          type: string
          title: File Type
        folder:
          type: string
          title: Folder
          default: skills_bundles
      type: object
      required:
      - file_name
      - file_type
      title: UploadRequest
    UrlPermissionRequest:
      properties:
        urlPattern:
          type: string
          title: Urlpattern
        userId:
          anyOf:
          - type: string
          - type: 'null'
          title: Userid
          default: default
        reason:
          anyOf:
          - type: string
          - type: 'null'
          title: Reason
          default: None
      type: object
      required:
      - urlPattern
      title: UrlPermissionRequest
    UsageMetricUpsert:
      properties:
        metric_date:
          type: string
          title: Metric Date
        total_requests:
          type: integer
          title: Total Requests
        total_tokens:
          type: integer
          title: Total Tokens
        total_cost:
          type: number
          title: Total Cost
        unique_users:
          type: integer
          title: Unique Users
        avg_latency_ms:
          type: integer
          title: Avg Latency Ms
        error_rate:
          type: number
          title: Error Rate
      type: object
      required:
      - metric_date
      - total_requests
      - total_tokens
      - total_cost
      - unique_users
      - avg_latency_ms
      - error_rate
      title: UsageMetricUpsert
    UserUpdate:
      properties:
        username:
          type: string
          title: Username
        role:
          type: string
          title: Role
        permissions:
          items:
            type: string
          type: array
          title: Permissions
      type: object
      required:
      - username
      - role
      - permissions
      title: UserUpdate
    ValidationError:
      properties:
        loc:
          items:
            anyOf:
            - type: string
            - type: integer
          type: array
          title: Location
        msg:
          type: string
          title: Message
        type:
          type: string
          title: Error Type
        input:
          title: Input
        ctx:
          type: object
          title: Context
      type: object
      required:
      - loc
      - msg
      - type
      title: ValidationError
    VulnCheckRequest:
      properties:
        file_path:
          anyOf:
          - type: string
          - type: 'null'
          title: File Path
        diff:
          anyOf:
          - type: string
          - type: 'null'
          title: Diff
      type: object
      title: VulnCheckRequest
    VulnCheckResponse:
      properties:
        file:
          type: string
          title: File
        vulnerability_score:
          type: number
          title: Vulnerability Score
        critical_count:
          type: integer
          title: Critical Count
        high_count:
          type: integer
          title: High Count
        medium_count:
          type: integer
          title: Medium Count
        low_count:
          type: integer
          title: Low Count
        findings:
          items:
            additionalProperties: true
            type: object
          type: array
          title: Findings
        recommendation:
          type: string
          title: Recommendation
      type: object
      required:
      - file
      - vulnerability_score
      - critical_count
      - high_count
      - medium_count
      - low_count
      - findings
      - recommendation
      title: VulnCheckResponse
    WorkspaceCommand:
      properties:
        prompt:
          type: string
          title: Prompt
        project_id:
          type: string
          title: Project Id
      type: object
      required:
      - prompt
      - project_id
      title: WorkspaceCommand
    api__routes__billing_api__CheckoutRequest:
      properties:
        price_id:
          type: string
          title: Price Id
        success_url:
          type: string
          title: Success Url
        cancel_url:
          type: string
          title: Cancel Url
      type: object
      required:
      - price_id
      - success_url
      - cancel_url
      title: CheckoutRequest
    api__routes__marketplace__InstallRequest:
      properties:
        tool_id:
          type: string
          title: Tool Id
        target_environment:
          anyOf:
          - type: string
          - type: 'null'
          title: Target Environment
        sandbox:
          anyOf:
          - type: boolean
          - type: 'null'
          title: Sandbox
        version:
          anyOf:
          - type: string
          - type: 'null'
          title: Version
      type: object
      required:
      - tool_id
      title: InstallRequest
    api__routes__payments__CheckoutRequest:
      properties:
        price_id:
          type: string
          title: Price Id
        success_url:
          type: string
          title: Success Url
        cancel_url:
          type: string
          title: Cancel Url
        user_id:
          type: string
          title: User Id
      type: object
      required:
      - price_id
      - success_url
      - cancel_url
      - user_id
      title: CheckoutRequest
    api__routes__simulator__InstallRequest:
      properties:
        appId:
          type: string
          title: Appid
        deviceProfile:
          anyOf:
          - type: string
          - type: 'null'
          title: Deviceprofile
          default: PIXEL_6
      type: object
      required:
      - appId
      title: InstallRequest
  securitySchemes:
    HTTPBearer:
      type: http
      scheme: bearer
    OAuth2PasswordBearer:
      type: oauth2
      flows:
        password:
          scopes: {}
          tokenUrl: /auth/login
tags:
- name: admin
  description: God-mode admin operations.
- name: agent
  description: Autonomous agents execution and planning.
- name: marketplace
  description: Discover and manage AI skills and tools.
- name: tools
  description: Registry and management of integrated tools.

```
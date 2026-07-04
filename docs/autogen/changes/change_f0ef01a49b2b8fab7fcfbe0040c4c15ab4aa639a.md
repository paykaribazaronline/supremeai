# 📋 Commit f0ef01a49b2b8fab7fcfbe0040c4c15ab4aa639a

## Commit Stats
```
commit f0ef01a49b2b8fab7fcfbe0040c4c15ab4aa639a
Author: github-actions[bot] <github-actions[bot]@users.noreply.github.com>
Date:   Sat Jul 4 08:43:37 2026 +0000

    docs: auto-update codebase docs & dashboard [skip ci]

 docs/autogen/INDEX.md                              |     2 +-
 ...nge_04162a2bd5f35a10b23f6f6dc4f2f7348e3b5c81.md |  1987 ----
 ...nge_1432eacc88479e5eaaab9dd454857ca82d0e4c79.md | 10654 ------------------
 ...nge_46d8fa8174f0a005648e3103dbdf7022b68a6d44.md |    36 -
 ...nge_c74c0b56038c11f170d4801a08009adc82e3357f.md |    79 +
 ...nge_f4a7b1fdb1ccd5df45c643fe5c073a0d4dd83979.md | 10657 +++++++++++++++++++
 ...nge_fff6a2a3a2df4d43eb3749472d936ed1eb48f266.md |  1092 ++
 .../.github_actions_setup-backend_action.yml.md    |     2 +-
 ...github_scripts_advanced-validation-report.py.md |     2 +-
 .../codebase/.github_scripts_canary-deploy.py.md   |     2 +-
 .../codebase/.github_scripts_ci-auto-fix-v3.py.md  |     2 +-
 .../codebase/.github_scripts_ci-auto-fix.py.md     |     2 +-
 .../.github_scripts_ci-decision-engine.py.md       |     2 +-
 .../codebase/.github_scripts_ci-health-check.py.md |     2 +-
 .../.github_scripts_clean_action_logs.py.md        |     2 +-
 .../codebase/.github_scripts_deploy-backend.py.md  |     2 +-
 .../.github_scripts_detect-previous-failures.py.md |     2 +-
 .../codebase/.github_scripts_enforce_24h_gap.py.md |     2 +-
 .../.github_scripts_generate-ci-report.py.md       |     2 +-
 .../.github_scripts_generate_ai_prompt.py.md       |     2 +-
 .../.github_scripts_multi-model-evaluator.py.md    |     2 +-
 docs/autogen/codebase/.github_scripts_review.py.md |     2 +-
 .../.github_scripts_supremeai-evaluator.py.md      |     2 +-
 .../.github_scripts_test_ai_reviewer.py.md         |     2 +-
 .../codebase/.github_workflows_deploy.yml.md       |     2 +-
 .../.github_workflows_nightly-maintenance.yml.md   |     2 +-
 .../.github_workflows_supreme-core-ci.yml.md       |     2 +-
 .../.github_workflows_supreme-mobile-cd.yml.md     |     2 +-
 ....github_workflows_supreme-release-builds.yml.md |     2 +-
 .../.github_workflows_sync-from-prod.yml.md        |     2 +-
 docs/autogen/codebase/AGENT.md.md                  |     2 +-
 docs/autogen/codebase/AGENTS.md.md                 |     2 +-
 docs/autogen/codebase/CHANGELOG.md.md              |     2 +-
 docs/autogen/codebase/CI_PIPELINE.md.md            |     2 +-
 docs/autogen/codebase/CONTRIBUTING.md.md           |     2 +-
 docs/autogen/codebase/IMPLEMENTATION_STATUS.md.md  |     2 +-
 .../codebase/PRODUCTION_READINESS_GUIDE.md.md      |     2 +-
 docs/autogen/codebase/README.md.md                 |     2 +-
 docs/autogen/codebase/SECURITY.md.md               |     2 +-
 docs/autogen/codebase/accessibility.spec.ts.md     |     2 +-
 docs/autogen/codebase/admin_dashboard_script.js.md |     2 +-
 docs/autogen/codebase/admin_god.py.md              |     2 +-
 docs/autogen/codebase/apps_desktop_README.md.md    |     2 +-
 docs/autogen/codebase/apps_desktop_package.json.md |     2 +-
 .../codebase/apps_desktop_src-tauri_Cargo.toml.md  |     2 +-
 .../codebase/apps_desktop_src-tauri_build.rs.md    |     2 +-
 .../apps_desktop_src-tauri_secure-store.ts.md      |     2 +-
 .../codebase/apps_desktop_src-tauri_src_main.rs.md |     2 +-
 .../apps_desktop_src-tauri_tauri.conf.json.md      |     2 +-
 .../codebase/apps_desktop_src-ui_package.json.md   |     2 +-
 .../codebase/apps_desktop_src-ui_src_App.tsx.md    |     2 +-
 ..._desktop_src-ui_src_components_ChatInput.tsx.md |     2 +-
 .../codebase/apps_desktop_src-ui_src_main.tsx.md   |     2 +-
 .../apps_desktop_src-ui_src_pages_AdminPage.tsx.md |     2 +-
 .../apps_desktop_src-ui_src_pages_ChatPage.tsx.md  |     2 +-
 ...s_desktop_src-ui_src_pages_EvolutionPage.tsx.md |     2 +-
 .../apps_desktop_src-ui_src_pages_LoginPage.tsx.md |     2 +-
 ...apps_desktop_src-ui_src_pages_SkillsPage.tsx.md |     2 +-
 .../apps_desktop_src-ui_src_services_api.ts.md     |     2 +-
 .../apps_desktop_src-ui_src_stores_authStore.ts.md |     2 +-
 .../apps_desktop_src-ui_src_types_index.ts.md      |     2 +-
 .../apps_desktop_src-ui_src_vite-env.d.ts.md       |     2 +-
 .../codebase/apps_desktop_src-ui_tsconfig.json.md  |     2 +-
 .../apps_desktop_src-ui_tsconfig.node.json.md      |     2 +-
 .../codebase/apps_desktop_src-ui_vite.config.ts.md |     2 +-
 ...ava_com_supremeai_JavaWorkerApplication.java.md |     2 +-
 ...va_com_supremeai_grpc_WorkerServiceImpl.java.md |     2 +-
 ...in_java_com_supremeai_models_TaskEntity.java.md |     2 +-
 ...m_supremeai_repositories_TaskRepository.java.md |     2 +-
 ...va-worker_src_main_resources_application.yml.md |     2 +-
 docs/autogen/codebase/apps_mobile_README.md.md     |     2 +-
 docs/autogen/codebase/apps_mobile_README_BD.md.md  |     2 +-
 .../codebase/apps_mobile_analysis_options.yaml.md  |     2 +-
 .../codebase/apps_mobile_assets_i18n_ar.json.md    |     2 +-
 .../codebase/apps_mobile_assets_i18n_bn.json.md    |     2 +-
 .../codebase/apps_mobile_assets_i18n_en.json.md    |     2 +-
 .../codebase/apps_mobile_assets_i18n_es.json.md    |     2 +-
 .../codebase/apps_mobile_assets_i18n_hi.json.md    |     2 +-
 .../codebase/apps_mobile_assets_i18n_zh.json.md    |     2 +-
 .../codebase/apps_mobile_devtools_options.yaml.md  |     2 +-
 ...ts.xcassets_AppIcon.appiconset_Contents.json.md |     2 +-
 ....xcassets_LaunchImage.imageset_Contents.json.md |     2 +-
 ...sets.xcassets_LaunchImage.imageset_README.md.md |     2 +-
 ...s_mobile_lib_dataconnect_generated_README.md.md |     2 +-
 ...le_lib_dataconnect_generated_add_review.dart.md |     2 +-
 ..._lib_dataconnect_generated_create_movie.dart.md |     2 +-
 ...lib_dataconnect_generated_delete_review.dart.md |     2 +-
 ...ile_lib_dataconnect_generated_generated.dart.md |     2 +-
 ...b_dataconnect_generated_get_movie_by_id.dart.md |     2 +-
 ...e_lib_dataconnect_generated_list_movies.dart.md |     2 +-
 ...dataconnect_generated_list_user_reviews.dart.md |     2 +-
 ...le_lib_dataconnect_generated_list_users.dart.md |     2 +-
 ..._lib_dataconnect_generated_search_movie.dart.md |     2 +-
 ...e_lib_dataconnect_generated_upsert_user.dart.md |     2 +-
 docs/autogen/codebase/apps_mobile_lib_main.dart.md |     2 +-
 .../apps_mobile_lib_models_ci_job_model.dart.md    |     2 +-
 ...apps_mobile_lib_providers_auth_provider.dart.md |     2 +-
 ...mobile_lib_providers_dashboard_provider.dart.md |     2 +-
 ...le_lib_providers_orchestration_provider.dart.md |     2 +-
 ..._mobile_lib_providers_settings_provider.dart.md |     2 +-
 ...ps_mobile_lib_screens_agent_chat_screen.dart.md |     2 +-
 ...mobile_lib_screens_alerts_alerts_screen.dart.md |     2 +-
 ..._lib_screens_analytics_analytics_screen.dart.md |     2 +-
 ...apps_mobile_lib_screens_api_keys_screen.dart.md |     2 +-
 .../apps_mobile_lib_screens_api_scaffold.dart.md   |     2 +-
 ...apps_mobile_lib_screens_byoc_hub_screen.dart.md |     2 +-
 ..._lib_screens_consensus_consensus_screen.dart.md |     2 +-
 ...obile_lib_screens_dashboard_home_screen.dart.md |     2 +-
 ...pps_mobile_lib_screens_dashboard_screen.dart.md |     2 +-
 ..._lib_screens_extension_extension_screen.dart.md |     2 +-
 .../apps_mobile_lib_screens_git_git_screen.dart.md |     2 +-
 ...le_lib_screens_learning_learning_screen.dart.md |     2 +-
 .../apps_mobile_lib_screens_login_screen.dart.md   |     2 +-
 ...eens_notifications_notifications_screen.dart.md |     2 +-
 ...b_screens_projects_projects_list_screen.dart.md |     2 +-
 ...b_screens_providers_ai_providers_screen.dart.md |     2 +-
 ...s_mobile_lib_screens_quota_quota_screen.dart.md |     2 +-
 ...ib_screens_resilience_resilience_screen.dart.md |     2 +-
 ...apps_mobile_lib_screens_settings_screen.dart.md |     2 +-
 .../apps_mobile_lib_screens_terminal_view.dart.md  |     2 +-
 .../apps_mobile_lib_screens_vpn_vpn_screen.dart.md |     2 +-
 .../apps_mobile_lib_screens_wallet_screen.dart.md  |     2 +-
 .../apps_mobile_lib_services_api_client.dart.md    |     2 +-
 .../apps_mobile_lib_services_api_service.dart.md   |     2 +-
 ...pps_mobile_lib_services_billing_service.dart.md |     2 +-
 .../apps_mobile_lib_services_byoc_service.dart.md  |     2 +-
 ...pps_mobile_lib_services_ci_sync_service.dart.md |     2 +-
 ...s_mobile_lib_services_deployment_stream.dart.md |     2 +-
 ...obile_lib_services_localization_service.dart.md |     2 +-
 ...bile_lib_services_neural_stream_service.dart.md |     2 +-
 ...obile_lib_services_notification_service.dart.md |     2 +-
 ...obile_lib_services_offline_sync_service.dart.md |     2 +-
 ...ile_lib_services_payment_gateway_bridge.dart.md |     2 +-
 ..._mobile_lib_services_screen_api_service.dart.md |     2 +-
 .../apps_mobile_lib_theme_app_theme.dart.md        |     2 +-
 .../apps_mobile_lib_theme_theme_provider.dart.md   |     2 +-
 ...apps_mobile_lib_widgets_action_hub_card.dart.md |     2 +-
 ...ile_lib_widgets_base_dashboard_scaffold.dart.md |     2 +-
 .../codebase/apps_mobile_lib_widgets_es.json.md    |     2 +-
 .../apps_mobile_lib_widgets_json_dropzone.dart.md  |     2 +-
 .../apps_mobile_lib_widgets_live_terminal.dart.md  |     2 +-
 ...apps_mobile_lib_widgets_loading_widgets.dart.md |     2 +-
 ...le_lib_widgets_transaction_history_list.dart.md |     2 +-
 .../apps_mobile_lib_widgets_usage_chart.dart.md    |     2 +-
 ...ts.xcassets_AppIcon.appiconset_Contents.json.md |     2 +-
 docs/autogen/codebase/apps_mobile_pubspec.lock.md  |     2 +-
 docs/autogen/codebase/apps_mobile_pubspec.yaml.md  |     2 +-
 ...bile_test_auth_provider_edge_cases_test.dart.md |     2 +-
 .../apps_mobile_test_auth_provider_test.dart.md    |     2 +-
 ...mobile_test_home_screen_edge_cases_test.dart.md |     2 +-
 .../apps_mobile_test_home_screen_test.dart.md      |     2 +-
 ...s_mobile_test_screens_login_screen_test.dart.md |     2 +-
 .../codebase/apps_mobile_web_manifest.json.md      |     2 +-
 .../codebase/apps_studio-client_README.md.md       |     2 +-
 .../codebase/apps_studio-client_components.json.md |     2 +-
 .../apps_studio-client_eslint.config.js.md         |     2 +-
 .../autogen/codebase/apps_studio-client_main.js.md |     2 +-
 .../codebase/apps_studio-client_package.json.md    |     2 +-
 .../apps_studio-client_public_manifest.json.md     |     2 +-
 .../codebase/apps_studio-client_public_sw.js.md    |     2 +-
 .../apps_studio-client_src_App.test.tsx.md         |     2 +-
 .../codebase/apps_studio-client_src_App.tsx.md     |     2 +-
 ...tudio-client_src_components_AdminConsole.tsx.md |     2 +-
 ..._studio-client_src_components_BanglaHint.tsx.md |     2 +-
 ...apps_studio-client_src_components_Header.tsx.md |     2 +-
 ...lient_src_components_LiveSujonBackground.tsx.md |     2 +-
 ...c_components_Onboarding_OnboardingWizard.tsx.md |     2 +-
 ...ent_src_components_Onboarding_StepApiKey.tsx.md |     2 +-
 ..._src_components_Onboarding_StepFirstChat.tsx.md |     2 +-
 ...rc_components_Onboarding_StepModelSelect.tsx.md |     2 +-
 ...dio-client_src_components_OperatorStudio.tsx.md |     2 +-
 ...o-client_src_components_admin_ActionCard.tsx.md |     2 +-
 ..._src_components_admin_AdminAuthenticated.tsx.md |     2 +-
 ...client_src_components_admin_AdminConsole.tsx.md |     2 +-
 ..._src_components_admin_AdminDashboardHome.tsx.md |     2 +-
 ...o-client_src_components_admin_AdminLogin.tsx.md |     2 +-
 ..._src_components_admin_AdminSubTabContent.tsx.md |     2 +-
 ...-client_src_components_admin_AdminTopNav.tsx.md |     2 +-
 ...o-client_src_components_admin_AethelNode.tsx.md |     2 +-
 ...ient_src_components_admin_AuditLogsPanel.tsx.md |     2 +-
 ...lient_src_components_admin_BackupRestore.tsx.md |     2 +-
 ...ient_src_components_admin_CICDVisualizer.tsx.md |     2 +-
 ...t_src_components_admin_CloudOrchestrator.tsx.md |     2 +-
 ...lient_src_components_admin_CommandCenter.tsx.md |     2 +-
 ...client_src_components_admin_ConfigEditor.tsx.md |     2 +-
 ..._src_components_admin_ConsentMatrixModal.tsx.md |     2 +-
 ...-client_src_components_admin_CostAuditor.tsx.md |     2 +-
 ..._components_admin_DashboardErrorBoundary.tsx.md |     2 +-
 ...ent_src_components_admin_DeploymentModal.tsx.md |     2 +-
 ...client_src_components_admin_DynamicPanel.tsx.md |     2 +-
 ...omponents_admin_EnhancedSkillMarketplace.tsx.md |     2 +-
 ...t_src_components_admin_GithubIntegration.tsx.md |     2 +-
 ...client_src_components_admin_HealthBanner.tsx.md |     2 +-
 ...io-client_src_components_admin_HealthMap.tsx.md |     2 +-
 ..._src_components_admin_InteractiveChatTab.tsx.md |     2 +-
 ...dio-client_src_components_admin_LiveLogs.tsx.md |     2 +-
 ...lient_src_components_admin_MemoryBrowser.tsx.md |     2 +-
 ...-client_src_components_admin_ModelRouter.tsx.md |     2 +-
 ..._components_admin_ObservabilityDashboard.tsx.md |     2 +-
 ...-client_src_components_admin_RBACManager.tsx.md |     2 +-
 ...nt_src_components_admin_RateLimitManager.tsx.md |     2 +-
 ...rc_components_admin_RealTimeMetricsPanel.tsx.md |     2 +-
 ...mponents_admin_RedesignedDashboardMockup.tsx.md |     2 +-
 ...nt_src_components_admin_RulesEnginePanel.tsx.md |     2 +-
 ...t_src_components_admin_SecurityDashboard.tsx.md |     2 +-
 ...rc_components_admin_ServiceHealthMetrics.tsx.md |     2 +-
 ...ent_src_components_admin_ThreatDetection.tsx.md |     2 +-
 ...-client_src_components_admin_UserManager.tsx.md |     2 +-
 ..._src_components_admin_VisualRulesBuilder.tsx.md |     2 +-
 ..._studio-client_src_components_admin_index.ts.md |     2 +-
 ..._src_components_audio_WaveformVisualizer.tsx.md |     2 +-
 ...ient_src_components_chat_TypingIndicator.tsx.md |     2 +-
 ...nt_src_components_chat_UnifiedChatBubble.tsx.md |     2 +-
 ...s_studio-client_src_components_chat_index.ts.md |     2 +-
 ...t_src_components_customer_BrowserPreview.tsx.md |     2 +-
 ...t_src_components_customer_ChatPanel.test.tsx.md |     2 +-
 ...client_src_components_customer_ChatPanel.tsx.md |     2 +-
 ...lient_src_components_customer_CodeEditor.tsx.md |     2 +-
 ...-client_src_components_customer_HomeFeed.tsx.md |     2 +-
 ..._src_components_customer_MobileSimulator.tsx.md |     2 +-
 ...rc_components_customer_QuickPresets.test.tsx.md |     2 +-
 ...ent_src_components_customer_QuickPresets.tsx.md |     2 +-
 ...c_components_customer_UserDashboard.test.tsx.md |     2 +-
 ...nt_src_components_customer_UserDashboard.tsx.md |     2 +-
 ...udio-client_src_components_customer_index.ts.md |     2 +-
 ...components_dashboard_AutomationQueuePage.tsx.md |     2 +-
 ...components_dashboard_DashboardShell.test.tsx.md |     2 +-
 ..._src_components_dashboard_DashboardShell.tsx.md |     2 +-
 ...t_src_components_dashboard_KnowledgePage.tsx.md |     2 +-
 ..._src_components_dashboard_LlmGatewayPage.tsx.md |     2 +-
 ...ent_src_components_dashboard_SecretsPage.tsx.md |     2 +-
 ...c_components_dashboard_SessionDetailPage.tsx.md |     2 +-
 ...nt_src_components_dashboard_SessionsPage.tsx.md |     2 +-
 ...nt_src_components_dashboard_SettingsPage.tsx.md |     2 +-
 ...src_components_dashboard_SiteActionsPage.tsx.md |     2 +-
 ...lient_src_components_dashboard_UsagePage.tsx.md |     2 +-
 ...lient_src_components_dashboard_VaultPage.tsx.md |     2 +-
 ...ent_src_components_dashboard_sessionStore.ts.md |     2 +-
 ...ent_src_components_dashboard_useHashRoute.ts.md |     2 +-
 ...lient_src_components_editor_CollabEditor.tsx.md |     2 +-
 ...o-client_src_components_graph_SkillGraph.tsx.md |     2 +-
 ...udio-client_src_components_ui_ActionCard.tsx.md |     2 +-
 ...ps_studio-client_src_components_ui_Badge.tsx.md |     2 +-
 ...pps_studio-client_src_components_ui_Card.tsx.md |     2 +-
 ...studio-client_src_components_ui_Skeleton.tsx.md |     2 +-
 ...pps_studio-client_src_components_ui_index.ts.md |     2 +-
 ..._studio-client_src_contexts_ThemeContext.tsx.md |     2 +-
 ...o-client_src_dataconnect-generated_README.md.md |     2 +-
 ...t_src_dataconnect-generated_esm_index.esm.js.md |     2 +-
 ...t_src_dataconnect-generated_esm_package.json.md |     2 +-
 ...lient_src_dataconnect-generated_index.cjs.js.md |     2 +-
 ...-client_src_dataconnect-generated_index.d.ts.md |     2 +-
 ...lient_src_dataconnect-generated_package.json.md |     2 +-
 ...nt_src_dataconnect-generated_react_README.md.md |     2 +-
 ...dataconnect-generated_react_esm_index.esm.js.md |     2 +-
 ...dataconnect-generated_react_esm_package.json.md |     2 +-
 ...src_dataconnect-generated_react_index.cjs.js.md |     2 +-
 ...t_src_dataconnect-generated_react_index.d.ts.md |     2 +-
 ...src_dataconnect-generated_react_package.json.md |     2 +-
 .../codebase/apps_studio-client_src_firebase.ts.md |     2 +-
 .../apps_studio-client_src_hooks_index.ts.md       |     2 +-
 ...lient_src_hooks_tests_useTranslation.test.ts.md |     2 +-
 .../apps_studio-client_src_hooks_useAdminApi.ts.md |     2 +-
 .../apps_studio-client_src_hooks_useAuth.ts.md     |     2 +-
 .../apps_studio-client_src_hooks_useChat.ts.md     |     2 +-
 ..._studio-client_src_hooks_useDashboardData.ts.md |     2 +-
 ...ps_studio-client_src_hooks_useTranslation.ts.md |     2 +-
 ...apps_studio-client_src_hooks_useWebSocket.ts.md |     2 +-
 ...apps_studio-client_src_i18n_I18nProvider.tsx.md |     2 +-
 .../apps_studio-client_src_i18n_config.ts.md       |     2 +-
 .../apps_studio-client_src_i18n_translations.ts.md |     2 +-
 .../codebase/apps_studio-client_src_lib_etag.ts.md |     2 +-
 .../codebase/apps_studio-client_src_main.tsx.md    |     2 +-
 ...s_studio-client_src_services_adminService.ts.md |     2 +-
 ...tudio-client_src_services_adminTokenStore.ts.md |     2 +-
 ...s_studio-client_src_services_agentService.ts.md |     2 +-
 ...apps_studio-client_src_services_apiClient.ts.md |     2 +-
 ...ient_src_services_api_microserviceMonitor.ts.md |     2 +-
 ...t_src_services_audio_AudioPlaybackService.ts.md |     2 +-
 ...t_src_services_audio_AudioRecorderService.ts.md |     2 +-
 ...ps_studio-client_src_services_authService.ts.md |     2 +-
 ...ps_studio-client_src_services_chatService.ts.md |     2 +-
 ...tudio-client_src_services_ciReportService.ts.md |     2 +-
 ...pps_studio-client_src_services_storageApi.ts.md |     2 +-
 .../apps_studio-client_src_store_adminStore.ts.md  |     2 +-
 ...pps_studio-client_src_store_customerStore.ts.md |     2 +-
 ...ps_studio-client_src_store_dashboardStore.ts.md |     2 +-
 .../apps_studio-client_src_store_themeStore.ts.md  |     2 +-
 .../apps_studio-client_src_store_useStore.ts.md    |     2 +-
 .../apps_studio-client_src_test_setup.ts.md        |     2 +-
 .../codebase/apps_studio-client_src_types.ts.md    |     2 +-
 .../apps_studio-client_src_types_customer.ts.md    |     2 +-
 .../apps_studio-client_src_utils_api.ts.md         |     2 +-
 .../apps_studio-client_src_vite-env.d.ts.md        |     2 +-
 ...tudio-client_src_workers_logParser.worker.ts.md |     2 +-
 .../apps_studio-client_tsconfig.app.json.md        |     2 +-
 .../codebase/apps_studio-client_tsconfig.json.md   |     2 +-
 .../apps_studio-client_tsconfig.node.json.md       |     2 +-
 .../codebase/apps_studio-client_vite.config.ts.md  |     2 +-
 .../apps_studio-client_vitest.config.ts.md         |     2 +-
 docs/autogen/codebase/apps_web-chat_api.test.ts.md |     2 +-
 docs/autogen/codebase/apps_web-chat_api.ts.md      |     2 +-
 .../autogen/codebase/apps_web-chat_package.json.md |     2 +-
 docs/autogen/codebase/apps_web-chat_script.ts.md   |     2 +-
 .../codebase/apps_web-chat_tsconfig.json.md        |     2 +-
 .../codebase/apps_web-chat_vite-env.d.ts.md        |     2 +-
 .../codebase/apps_web-chat_vite.config.ts.md       |     2 +-
 .../codebase/apps_web-chat_vitest.config.ts.md     |     2 +-
 docs/autogen/codebase/backend_README.md.md         |     2 +-
 .../backend_adaptive_engine_experience_db.py.md    |     2 +-
 .../codebase/backend_adaptive_engine_init_.py.md   |     2 +-
 .../backend_adaptive_engine_intent_parser.py.md    |     2 +-
 .../backend_adaptive_engine_learning_loop.py.md    |     2 +-
 .../backend_adaptive_engine_platform_learner.py.md |     2 +-
 .../backend_adaptive_engine_registry.py.md         |     2 +-
 ...end_adaptive_engine_test_platform_learner.py.md |     2 +-
 docs/autogen/codebase/backend_admin_god.py.md      |     2 +-
 docs/autogen/codebase/backend_admin_init_.py.md    |     2 +-
 docs/autogen/codebase/backend_admin_test_god.py.md |     2 +-
 .../codebase/backend_agents_crew_departments.py.md |     2 +-
 docs/autogen/codebase/backend_agents_init_.py.md   |     2 +-
 .../codebase/backend_agents_legal_agent.py.md      |     2 +-
 .../codebase/backend_agents_medical_agent.py.md    |     2 +-
 .../backend_agents_research_assistant.py.md        |     2 +-
 .../codebase/backend_agents_test_legal_agent.py.md |     2 +-
 .../backend_agents_test_medical_agent.py.md        |     2 +-
 .../codebase/backend_agents_trading_agent.py.md    |     2 +-
 docs/autogen/codebase/backend_alembic_env.py.md    |     2 +-
 ...ersions_664fe16e33ca_add_ci_reports_table.py.md |     2 +-
 .../codebase/backend_api_dependencies.py.md        |     2 +-
 docs/autogen/codebase/backend_api_init_.py.md      |     2 +-
 .../codebase/backend_api_routes_admin.py.md        |     2 +-
 .../backend_api_routes_admin_dashboard.py.md       |     2 +-
 .../codebase/backend_api_routes_agent_tasks.py.md  |     2 +-
 .../codebase/backend_api_routes_agents.py.md       |     2 +-
 .../codebase/backend_api_routes_api_keys.py.md     |     2 +-
 .../backend_api_routes_approval_manager.py.md      |     2 +-
 .../backend_api_routes_async_task_router.py.md     |     2 +-
 .../autogen/codebase/backend_api_routes_auth.py.md |     2 +-
 .../codebase/backend_api_routes_billing_api.py.md  |     2 +-
 .../codebase/backend_api_routes_browser.py.md      |     2 +-
 .../codebase/backend_api_routes_byoc_api.py.md     |     2 +-
 .../codebase/backend_api_routes_cdc_webhooks.py.md |     2 +-
 .../autogen/codebase/backend_api_routes_chat.py.md |     2 +-
 .../codebase/backend_api_routes_ci_webhooks.py.md  |     2 +-
 .../codebase/backend_api_routes_cloud_mesh.py.md   |     2 +-
 .../codebase/backend_api_routes_codeflow.py.md     |     2 +-
 .../codebase/backend_api_routes_config.py.md       |     2 +-
 .../codebase/backend_api_routes_email.py.md        |     2 +-
 .../codebase/backend_api_routes_evolution.py.md    |     2 +-
 .../codebase/backend_api_routes_feedback.py.md     |     2 +-
 .../codebase/backend_api_routes_github.py.md       |     2 +-
 .../codebase/backend_api_routes_graph.py.md        |     2 +-
 .../codebase/backend_api_routes_init_.py.md        |     2 +-
 .../codebase/backend_api_routes_internal.py.md     |     2 +-
 .../codebase/backend_api_routes_knowledge.py.md    |     2 +-
 .../codebase/backend_api_routes_llm_gateway.py.md  |     2 +-
 .../codebase/backend_api_routes_markdown.py.md     |     2 +-
 .../codebase/backend_api_routes_marketplace.py.md  |     2 +-
 .../backend_api_routes_marketplace_endpoints.py.md |     2 +-
 .../codebase/backend_api_routes_media.py.md        |     2 +-
 .../codebase/backend_api_routes_memory.py.md       |     2 +-
 .../codebase/backend_api_routes_metrics.py.md      |     2 +-
 .../codebase/backend_api_routes_mobile_bff.py.md   |     2 +-
 .../codebase/backend_api_routes_onboarding.py.md   |     2 +-
 .../codebase/backend_api_routes_payments.py.md     |     2 +-
 .../codebase/backend_api_routes_preferences.py.md  |     2 +-
 .../codebase/backend_api_routes_repos.py.md        |     2 +-
 .../codebase/backend_api_routes_simulator.py.md    |     2 +-
 .../codebase/backend_api_routes_site_actions.py.md |     2 +-
 docs/autogen/codebase/backend_api_routes_sso.py.md |     2 +-
 .../codebase/backend_api_routes_stream.py.md       |     2 +-
 .../autogen/codebase/backend_api_routes_task.py.md |     2 +-
 .../backend_api_routes_task_workspace.py.md        |     2 +-
 .../codebase/backend_api_routes_tenant_admin.py.md |     2 +-
 .../codebase/backend_api_routes_tools_ops.py.md    |     2 +-
 .../backend_api_routes_tools_registry.py.md        |     2 +-
 .../backend_api_routes_usage_metrics.py.md         |     2 +-
 .../codebase/backend_api_routes_voice.py.md        |     2 +-
 .../backend_api_routes_websocket_agent.py.md       |     2 +-
 .../backend_api_routes_websocket_voice.py.md       |     2 +-
 .../codebase/backend_byoc_cloud_connector.py.md    |     2 +-
 .../backend_byoc_container_orchestrator.py.md      |     2 +-
 docs/autogen/codebase/backend_byoc_init_.py.md     |     2 +-
 .../codebase/backend_byoc_resource_manager.py.md   |     2 +-
 .../codebase/backend_config_byoc_limits.json.md    |     2 +-
 .../backend_config_constitutional_rules.json.md    |     2 +-
 .../codebase/backend_config_pricing_tiers.json.md  |     2 +-
 .../codebase/backend_config_routing_policy.json.md |     2 +-
 docs/autogen/codebase/backend_core_admin_god.py.md |     2 +-
 .../codebase/backend_core_admin_routes.py.md       |     2 +-
 .../codebase/backend_core_agent_orchestrator.py.md |     2 +-
 .../codebase/backend_core_api_key_middleware.py.md |     2 +-
 .../backend_core_api_key_rate_limiter.py.md        |     2 +-
 docs/autogen/codebase/backend_core_app.py.md       |     2 +-
 .../codebase/backend_core_audit_logger.py.md       |     2 +-
 .../codebase/backend_core_auth_middleware.py.md    |     2 +-
 .../codebase/backend_core_auto_remediation.py.md   |     2 +-
 .../codebase/backend_core_autocache_proxy.py.md    |     2 +-
 .../codebase/backend_core_circuit_breaker.py.md    |     2 +-
 .../backend_core_cloud_sandbox_orchestrator.py.md  |     2 +-
 .../codebase/backend_core_cloud_storage.py.md      |     2 +-
 .../codebase/backend_core_code_validator.py.md     |     2 +-
 docs/autogen/codebase/backend_core_config.py.md    |     2 +-
 docs/autogen/codebase/backend_core_constants.py.md |     2 +-
 .../codebase/backend_core_db_repository.py.md      |     2 +-
 .../codebase/backend_core_decision_engine.py.md    |     2 +-
 .../codebase/backend_core_discord_bot.py.md        |     2 +-
 .../codebase/backend_core_docker-compose.yml.md    |     2 +-
 .../codebase/backend_core_email_service.py.md      |     2 +-
 .../codebase/backend_core_error_pattern_db.py.md   |     2 +-
 .../codebase/backend_core_error_remediation.py.md  |     2 +-
 docs/autogen/codebase/backend_core_events.py.md    |     2 +-
 .../codebase/backend_core_evolution_engine.py.md   |     2 +-
 .../codebase/backend_core_factual_verifier.py.md   |     2 +-
 .../codebase/backend_core_feedback_loop.py.md      |     2 +-
 .../codebase/backend_core_free_tier_tracker.py.md  |     2 +-
 .../codebase/backend_core_gcp_firestore.py.md      |     2 +-
 .../codebase/backend_core_gcp_pubsub_queue.py.md   |     2 +-
 .../codebase/backend_core_generation_monitor.py.md |     2 +-
 .../codebase/backend_core_grpc_client.py.md        |     2 +-
 .../codebase/backend_core_health_monitor.py.md     |     2 +-
 .../backend_core_honeypot_middleware.py.md         |     2 +-
 .../backend_core_idempotency_middleware.py.md      |     2 +-
 .../codebase/backend_core_immune_system.py.md      |     2 +-
 docs/autogen/codebase/backend_core_init_.py.md     |     2 +-
 .../codebase/backend_core_input_sanitizer.py.md    |     2 +-
 docs/autogen/codebase/backend_core_intent.py.md    |     2 +-
 .../codebase/backend_core_intent_router.py.md      |     2 +-
 .../codebase/backend_core_language_router.py.md    |     2 +-
 docs/autogen/codebase/backend_core_ld_client.py.md |     2 +-
 docs/autogen/codebase/backend_core_lifespan.py.md  |     2 +-
 .../codebase/backend_core_llm_gateway.py.md        |     2 +-
 .../codebase/backend_core_logging_config.py.md     |     2 +-
 .../codebase/backend_core_mcp_allowlist.py.md      |     2 +-
 .../codebase/backend_core_microvm_sandbox.py.md    |     2 +-
 .../codebase/backend_core_multi_layer_cache.py.md  |     2 +-
 .../backend_core_observability_middleware.py.md    |     2 +-
 .../codebase/backend_core_orchestrator.py.md       |     2 +-
 .../codebase/backend_core_origin_validator.py.md   |     2 +-
 .../codebase/backend_core_output_validator.py.md   |     2 +-
 .../codebase/backend_core_pgbouncer_pool.py.md     |     2 +-
 .../codebase/backend_core_posthog_client.py.md     |     2 +-
 .../codebase/backend_core_prompt_firewall.py.md    |     2 +-
 .../codebase/backend_core_prompt_helpers.py.md     |     2 +-
 .../codebase/backend_core_rate_limiter.py.md       |     2 +-
 docs/autogen/codebase/backend_core_rbac.py.md      |     2 +-
 .../codebase/backend_core_redis_manager.py.md      |     2 +-
 .../codebase/backend_core_rollback_monitor.py.md   |     2 +-
 .../codebase/backend_core_rules_mutator.py.md      |     2 +-
 .../codebase/backend_core_schema_validator.py.md   |     2 +-
 .../codebase/backend_core_secret_vault.py.md       |     2 +-
 .../backend_core_secure_credential_store.py.md     |     2 +-
 docs/autogen/codebase/backend_core_security.py.md  |     2 +-
 .../codebase/backend_core_self_healing_agent.py.md |     2 +-
 .../codebase/backend_core_semantic_cache.py.md     |     2 +-
 docs/autogen/codebase/backend_core_services.py.md  |     2 +-
 .../codebase/backend_core_skill_graph.py.md        |     2 +-
 .../codebase/backend_core_swarm_orchestrator.py.md |     2 +-
 .../autogen/codebase/backend_core_task_queue.py.md |     2 +-
 .../backend_core_task_queue_enhanced.py.md         |     2 +-
 .../codebase/backend_core_task_router.py.md        |     2 +-
 docs/autogen/codebase/backend_core_telemetry.py.md |     2 +-
 docs/autogen/codebase/backend_core_tenant_db.py.md |     2 +-
 .../codebase/backend_core_token_budget.py.md       |     2 +-
 .../codebase/backend_core_token_deductor.py.md     |     2 +-
 .../codebase/backend_core_universal_rules.py.md    |     2 +-
 .../codebase/backend_core_upload_validator.py.md   |     2 +-
 .../backend_core_upstash_redis_queue.py.md         |     2 +-
 .../codebase/backend_core_user_profiler.py.md      |     2 +-
 docs/autogen/codebase/backend_coverage.json.md     |     2 +-
 docs/autogen/codebase/backend_database_init_.py.md |     2 +-
 ...end_database_migrations_01_initial_setup.sql.md |     2 +-
 ...kend_database_migrations_02_phase2_setup.sql.md |     2 +-
 ...grations_03_user_preferences_and_metrics.sql.md |     2 +-
 ...nd_database_migrations_04_schema_upgrade.sql.md |     2 +-
 ...database_migrations_05_seed_github_repos.sql.md |     2 +-
 ...d_database_migrations_06_referral_system.sql.md |     2 +-
 ...end_database_migrations_07_tenant_config.sql.md |     2 +-
 ...ckend_database_migrations_08_sso_configs.sql.md |     2 +-
 ...database_migrations_09_offline_sync_logs.sql.md |     2 +-
 ...atabase_migrations_10_tenant_sso_offline.sql.md |     2 +-
 .../codebase/backend_database_session.py.md        |     2 +-
 .../codebase/backend_database_storage_client.py.md |     2 +-
 .../backend_database_supabase_client.py.md         |     2 +-
 .../codebase/backend_engine_cost_optimizer.py.md   |     2 +-
 docs/autogen/codebase/backend_engine_init_.py.md   |     2 +-
 .../codebase/backend_engine_model_dispatcher.py.md |     2 +-
 .../backend_evolution_auto_skill_creator.py.md     |     2 +-
 .../backend_evolution_auto_update_manager.py.md    |     2 +-
 .../backend_evolution_dynamic_injector.py.md       |     2 +-
 .../backend_evolution_fitness_engine.py.md         |     2 +-
 .../autogen/codebase/backend_evolution_init_.py.md |     2 +-
 .../backend_evolution_master_planner.py.md         |     2 +-
 .../backend_evolution_security_sandbox.py.md       |     2 +-
 .../backend_evolution_self_evolution_agent.py.md   |     2 +-
 .../codebase/backend_evolution_skill_graph.py.md   |     2 +-
 docs/autogen/codebase/backend_fix_tests.py.md      |     2 +-
 docs/autogen/codebase/backend_init_.py.md          |     2 +-
 docs/autogen/codebase/backend_main.py.md           |     2 +-
 .../backend_memory_checkpoint_resume.py.md         |     2 +-
 .../codebase/backend_memory_chromadb_store.py.md   |     2 +-
 .../backend_memory_cloud_postgres_store.py.md      |     2 +-
 .../backend_memory_cloud_vector_store.py.md        |     2 +-
 .../codebase/backend_memory_episodic_memory.py.md  |     2 +-
 docs/autogen/codebase/backend_memory_init_.py.md   |     2 +-
 .../codebase/backend_memory_long_term_memory.py.md |     2 +-
 .../codebase/backend_memory_rag_pipeline.py.md     |     2 +-
 .../codebase/backend_memory_sliding_window.py.md   |     2 +-
 .../codebase/backend_memory_sqlite_store.py.md     |     2 +-
 .../codebase/backend_memory_summary_tree.py.md     |     2 +-
 .../codebase/backend_memory_supabase_store.py.md   |     2 +-
 .../backend_memory_vector_store_config.py.md       |     2 +-
 .../backend_middleware_auth_middleware.py.md       |     2 +-
 .../backend_middleware_chaos_injector.py.md        |     2 +-
 .../codebase/backend_middleware_idempotency.py.md  |     2 +-
 docs/autogen/codebase/backend_models_admin.py.md   |     2 +-
 docs/autogen/codebase/backend_models_api_key.py.md |     2 +-
 .../codebase/backend_models_byoc_payloads.py.md    |     2 +-
 .../codebase/backend_models_ci_report.py.md        |     2 +-
 .../codebase/backend_models_deployment_logs.py.md  |     2 +-
 .../backend_models_error_remediation.py.md         |     2 +-
 .../codebase/backend_models_evolution.py.md        |     2 +-
 docs/autogen/codebase/backend_models_init_.py.md   |     2 +-
 .../backend_models_local_model_handler.py.md       |     2 +-
 .../codebase/backend_models_pending_tasks.py.md    |     2 +-
 .../codebase/backend_models_shared_workspace.py.md |     2 +-
 .../backend_models_transaction_ledger.py.md        |     2 +-
 .../backend_models_voice_interaction.py.md         |     2 +-
 docs/autogen/codebase/backend_models_wallet.py.md  |     2 +-
 .../codebase/backend_monitoring_cost_auditor.py.md |     2 +-
 .../codebase/backend_monitoring_init_.py.md        |     2 +-
 .../codebase/backend_p2p_credit_system.py.md       |     2 +-
 docs/autogen/codebase/backend_p2p_init_.py.md      |     2 +-
 .../codebase/backend_p2p_secure_tunnel.py.md       |     2 +-
 docs/autogen/codebase/backend_pyproject.toml.md    |     2 +-
 docs/autogen/codebase/backend_reports_init_.py.md  |     2 +-
 .../backend_reports_optimization_engine.py.md      |     2 +-
 .../codebase/backend_run_roundtrip_tests.py.md     |     2 +-
 docs/autogen/codebase/backend_scout_init_.py.md    |     2 +-
 .../backend_scout_knowledge_extractor.py.md        |     2 +-
 .../codebase/backend_scout_web_crawler_agent.py.md |     2 +-
 .../codebase/backend_scripts_check_ollama.py.md    |     2 +-
 docs/autogen/codebase/backend_scripts_init_.py.md  |     2 +-
 .../codebase/backend_scripts_load_seed_data.py.md  |     2 +-
 .../backend_scripts_run_dependency_check.py.md     |     2 +-
 .../backend_scripts_seed_tools_registry.py.md      |     2 +-
 .../backend_scripts_self_healing_tests.py.md       |     2 +-
 docs/autogen/codebase/backend_skills_init_.py.md   |     2 +-
 .../codebase/backend_skills_provisioner.py.md      |     2 +-
 .../codebase/backend_skills_skill_registry.py.md   |     2 +-
 .../codebase/backend_storage_asset_manager.py.md   |     2 +-
 docs/autogen/codebase/backend_storage_init_.py.md  |     2 +-
 .../backend_storage_r2_storage_client.py.md        |     2 +-
 .../backend_tests_agents_test_legal_agent.py.md    |     2 +-
 .../backend_tests_agents_test_medical_agent.py.md  |     2 +-
 ...kend_tests_agents_test_research_assistant.py.md |     2 +-
 .../backend_tests_agents_test_trading_agent.py.md  |     2 +-
 .../backend_tests_byoc_test_cloud_connector.py.md  |     2 +-
 ...nd_tests_byoc_test_container_orchestrator.py.md |     2 +-
 .../backend_tests_byoc_test_resource_manager.py.md |     2 +-
 docs/autogen/codebase/backend_tests_conftest.py.md |     2 +-
 .../backend_tests_engine_test_cost_optimizer.py.md |     2 +-
 ...ackend_tests_engine_test_model_dispatcher.py.md |     2 +-
 docs/autogen/codebase/backend_tests_init_.py.md    |     2 +-
 ...ackend_tests_monitoring_test_cost_auditor.py.md |     2 +-
 .../backend_tests_p2p_test_credit_system.py.md     |     2 +-
 .../backend_tests_p2p_test_secure_tunnel.py.md     |     2 +-
 ...kend_tests_scout_test_knowledge_extractor.py.md |     2 +-
 ...ackend_tests_scout_test_web_crawler_agent.py.md |     2 +-
 .../backend_tests_test_adaptive_engine.py.md       |     2 +-
 .../codebase/backend_tests_test_admin_god.py.md    |     2 +-
 .../codebase/backend_tests_test_admin_models.py.md |     2 +-
 .../codebase/backend_tests_test_admin_routes.py.md |     2 +-
 .../codebase/backend_tests_test_advanced.py.md     |     2 +-
 .../backend_tests_test_agent_department.py.md      |     2 +-
 .../backend_tests_test_agent_departments.py.md     |     2 +-
 .../backend_tests_test_agent_orchestrator.py.md    |     2 +-
 ...ackend_tests_test_agents_crew_departments.py.md |     2 +-
 docs/autogen/codebase/backend_tests_test_api.py.md |     2 +-
 .../codebase/backend_tests_test_api_chat.py.md     |     2 +-
 .../codebase/backend_tests_test_api_keys.py.md     |     2 +-
 .../backend_tests_test_api_new_endpoints.py.md     |     2 +-
 .../codebase/backend_tests_test_api_router.py.md   |     2 +-
 .../codebase/backend_tests_test_audit_logger.py.md |     2 +-
 .../backend_tests_test_auth_middleware.py.md       |     2 +-
 .../codebase/backend_tests_test_auth_routes.py.md  |     2 +-
 .../backend_tests_test_auto_fix_trigger.py.md      |     2 +-
 .../backend_tests_test_auto_skill_creator.py.md    |     2 +-
 .../backend_tests_test_autonomous_agent.py.md      |     2 +-
 .../codebase/backend_tests_test_bangla_nlp.py.md   |     2 +-
 .../codebase/backend_tests_test_bangla_voice.py.md |     2 +-
 .../backend_tests_test_billing_system.py.md        |     2 +-
 .../codebase/backend_tests_test_brain.py.md        |     2 +-
 .../backend_tests_test_browser_credentials.py.md   |     2 +-
 .../backend_tests_test_byoc_endpoints.py.md        |     2 +-
 .../codebase/backend_tests_test_chaos_worker.py.md |     2 +-
 .../backend_tests_test_checkpoint_resume.py.md     |     2 +-
 .../backend_tests_test_circuit_breaker.py.md       |     2 +-
 .../backend_tests_test_cloud_sandbox.py.md         |     2 +-
 .../backend_tests_test_cloud_storage.py.md         |     2 +-
 .../backend_tests_test_code_validator.py.md        |     2 +-
 .../backend_tests_test_collaborative_editor.py.md  |     2 +-
 .../codebase/backend_tests_test_config.py.md       |     2 +-
 .../backend_tests_test_config_additional.py.md     |     2 +-
 .../backend_tests_test_config_coverage.py.md       |     2 +-
 .../codebase/backend_tests_test_constants.py.md    |     2 +-
 .../backend_tests_test_context_and_actions.py.md   |     2 +-
 .../autogen/codebase/backend_tests_test_core.py.md |     2 +-
 .../codebase/backend_tests_test_core_smoke.py.md   |     2 +-
 .../backend_tests_test_coverage_gaps.py.md         |     2 +-
 .../codebase/backend_tests_test_crew_mcp.py.md     |     2 +-
 ...ackend_tests_test_database_storage_client.py.md |     2 +-
 .../backend_tests_test_db_repository.py.md         |     2 +-
 docs/autogen/codebase/backend_tests_test_e2e.py.md |     2 +-
 .../codebase/backend_tests_test_e2e_media.py.md    |     2 +-
 .../codebase/backend_tests_test_email_agent.py.md  |     2 +-
 .../backend_tests_test_email_service.py.md         |     2 +-
 .../backend_tests_test_episodic_memory.py.md       |     2 +-
 .../backend_tests_test_error_remediation.py.md     |     2 +-
 .../backend_tests_test_evolution_engine.py.md      |     2 +-
 .../backend_tests_test_evolution_pipeline.py.md    |     2 +-
 .../backend_tests_test_factual_verifier.py.md      |     2 +-
 .../backend_tests_test_feedback_loop.py.md         |     2 +-
 .../backend_tests_test_firebase_integration.py.md  |     2 +-
 .../backend_tests_test_fitness_engine.py.md        |     2 +-
 .../backend_tests_test_free_tier_tracker.py.md     |     2 +-
 .../backend_tests_test_gcp_integration.py.md       |     2 +-
 .../backend_tests_test_generation_monitor.py.md    |     2 +-
 .../codebase/backend_tests_test_github_agent.py.md |     2 +-
 .../codebase/backend_tests_test_graph_routes.py.md |     2 +-
 .../backend_tests_test_graph_service.py.md         |     2 +-
 .../codebase/backend_tests_test_grpc_client.py.md  |     2 +-
 .../backend_tests_test_hallucination_guard.py.md   |     2 +-
 .../codebase/backend_tests_test_health.py.md       |     2 +-
 .../backend_tests_test_health_monitor.py.md        |     2 +-
 .../backend_tests_test_health_monitor_routes.py.md |     2 +-
 .../backend_tests_test_honeypot_middleware.py.md   |     2 +-
 ...backend_tests_test_idempotency_middleware.py.md |     2 +-
 .../backend_tests_test_immune_system.py.md         |     2 +-
 .../backend_tests_test_immune_system_scanner.py.md |     2 +-
 .../backend_tests_test_input_sanitizer.py.md       |     2 +-
 .../backend_tests_test_language_router.py.md       |     2 +-
 .../codebase/backend_tests_test_llm_gateway.py.md  |     2 +-
 .../backend_tests_test_llm_gateway_coverage.py.md  |     2 +-
 .../backend_tests_test_long_term_memory.py.md      |     2 +-
 .../backend_tests_test_markdown_export.py.md       |     2 +-
 .../backend_tests_test_marketplace_agent.py.md     |     2 +-
 .../backend_tests_test_mcp_allowlist.py.md         |     2 +-
 .../codebase/backend_tests_test_mcp_server.py.md   |     2 +-
 ...ackend_tests_test_mcp_servers_integration.py.md |     2 +-
 .../codebase/backend_tests_test_media_r2.py.md     |     2 +-
 ...kend_tests_test_middleware_chaos_injector.py.md |     2 +-
 .../codebase/backend_tests_test_migrations.py.md   |     2 +-
 ...kend_tests_test_migrations_and_onboarding.py.md |     2 +-
 .../codebase/backend_tests_test_mobile_e2e.py.md   |     2 +-
 .../backend_tests_test_model_registry.py.md        |     2 +-
 .../backend_tests_test_model_router_unit.py.md     |     2 +-
 .../backend_tests_test_model_trainer.py.md         |     2 +-
 .../backend_tests_test_models_ci_report.py.md      |     2 +-
 .../backend_tests_test_models_evolution.py.md      |     2 +-
 .../codebase/backend_tests_test_monitoring.py.md   |     2 +-
 .../backend_tests_test_multi_account_rotator.py.md |     2 +-
 .../codebase/backend_tests_test_multicloud.py.md   |     2 +-
 .../backend_tests_test_new_endpoints_sprint5.py.md |     2 +-
 .../backend_tests_test_new_interfaces.py.md        |     2 +-
 .../backend_tests_test_new_tools_sprint5.py.md     |     2 +-
 .../backend_tests_test_optimization_engine.py.md   |     2 +-
 .../backend_tests_test_output_validator.py.md      |     2 +-
 ...ackend_tests_test_parallel_agent_executor.py.md |     2 +-
 .../codebase/backend_tests_test_payments.py.md     |     2 +-
 ...ckend_tests_test_performance_aware_router.py.md |     2 +-
 .../backend_tests_test_pgbouncer_pool.py.md        |     2 +-
 .../codebase/backend_tests_test_posthog.py.md      |     2 +-
 .../codebase/backend_tests_test_pr_reviewer.py.md  |     2 +-
 .../backend_tests_test_prod_docs_security.py.md    |     2 +-
 ...sts_test_production_readiness_integration.py.md |     2 +-
 .../backend_tests_test_prompt_firewall.py.md       |     2 +-
 .../autogen/codebase/backend_tests_test_rbac.py.md |     2 +-
 ...backend_tests_test_reasoning_orchestrator.py.md |     2 +-
 .../backend_tests_test_repo_discovery.py.md        |     2 +-
 .../backend_tests_test_resource_catalog.py.md      |     2 +-
 .../autogen/codebase/backend_tests_test_rlhf.py.md |     2 +-
 ...kend_tests_test_sandbox_orchestration_run.py.md |     2 +-
 .../backend_tests_test_schema_validator.py.md      |     2 +-
 .../codebase/backend_tests_test_secret_vault.py.md |     2 +-
 ...ackend_tests_test_secure_credential_store.py.md |     2 +-
 .../backend_tests_test_security_middleware.py.md   |     2 +-
 .../backend_tests_test_security_regression.py.md   |     2 +-
 .../backend_tests_test_self_evolution_agent.py.md  |     2 +-
 .../backend_tests_test_simulator_browser_api.py.md |     2 +-
 .../codebase/backend_tests_test_skill_graph.py.md  |     2 +-
 .../backend_tests_test_skill_recommender.py.md     |     2 +-
 .../backend_tests_test_sliding_window_memory.py.md |     2 +-
 .../backend_tests_test_sprint_c_tools.py.md        |     2 +-
 .../codebase/backend_tests_test_sprint_g.py.md     |     2 +-
 .../backend_tests_test_stealth_networking.py.md    |     2 +-
 .../codebase/backend_tests_test_stream.py.md       |     2 +-
 .../backend_tests_test_style_learner.py.md         |     2 +-
 ...kend_tests_test_supabase_schema_bootstrap.py.md |     2 +-
 .../backend_tests_test_supabase_store.py.md        |     2 +-
 .../backend_tests_test_swarm_orchestrator.py.md    |     2 +-
 .../backend_tests_test_task_endpoints.py.md        |     2 +-
 .../codebase/backend_tests_test_task_queue.py.md   |     2 +-
 .../codebase/backend_tests_test_task_router.py.md  |     2 +-
 .../codebase/backend_tests_test_telegram_bot.py.md |     2 +-
 .../codebase/backend_tests_test_telemetry.py.md    |     2 +-
 .../backend_tests_test_tenant_rate_limiter.py.md   |     2 +-
 .../backend_tests_test_universal_rules.py.md       |     2 +-
 .../backend_tests_test_upstash_redis.py.md         |     2 +-
 docs/autogen/codebase/backend_tests_test_uss.py.md |     2 +-
 .../backend_tests_test_video_generator.py.md       |     2 +-
 .../codebase/backend_tests_test_vision_agent.py.md |     2 +-
 .../codebase/backend_tests_test_voice_stream.py.md |     2 +-
 .../codebase/backend_tests_test_vpn_switcher.py.md |     2 +-
 .../codebase/backend_tests_test_vscode_e2e.py.md   |     2 +-
 .../codebase/backend_tests_test_web_fallback.py.md |     2 +-
 ...d_tests_tools_test_auto_coverage_improver.py.md |     2 +-
 ...kend_tests_tools_test_auto_test_generator.py.md |     2 +-
 ...kend_tests_tools_test_code_smell_detector.py.md |     2 +-
 .../backend_tests_tools_test_cot_reasoner.py.md    |     2 +-
 ...backend_tests_tools_test_coverage_auditor.py.md |     2 +-
 ...d_tests_tools_test_knowledge_base_indexer.py.md |     2 +-
 ...backend_tests_tools_test_multilingual_tts.py.md |     2 +-
 ...nd_tests_tools_test_viral_referral_engine.py.md |     2 +-
 .../backend_tests_utils_test_api_tracker.py.md     |     2 +-
 .../backend_tests_workers_test_celery_app.py.md    |     2 +-
 .../backend_tools_3d_model_generator.py.md         |     2 +-
 .../codebase/backend_tools_agent_tools.py.md       |     2 +-
 .../backend_tools_ai_federation_protocol.py.md     |     2 +-
 .../backend_tools_ai_pair_programmer.py.md         |     2 +-
 .../codebase/backend_tools_api_gateway.py.md       |     2 +-
 .../backend_tools_auto_coverage_improver.py.md     |     2 +-
 .../codebase/backend_tools_auto_pr_pipeline.py.md  |     2 +-
 .../backend_tools_auto_test_generator.py.md        |     2 +-
 .../backend_tools_bandwidth_optimizer.py.md        |     2 +-
 .../backend_tools_bangla_ai_connector.py.md        |     2 +-
 .../codebase/backend_tools_bangla_nlp.py.md        |     2 +-
 .../codebase/backend_tools_bangla_voice.py.md      |     2 +-
 .../codebase/backend_tools_benchmark_agent.py.md   |     2 +-
 .../backend_tools_bengali_ocr_converter.py.md      |     2 +-
 .../codebase/backend_tools_blockchain_agent.py.md  |     2 +-
 .../autogen/codebase/backend_tools_bootstrap.py.md |     2 +-
 .../codebase/backend_tools_browser_agent.py.md     |     2 +-
 .../codebase/backend_tools_browser_stealth.py.md   |     2 +-
 .../backend_tools_checkpoint_manager.py.md         |     2 +-
 docs/autogen/codebase/backend_tools_cli.py.md      |     2 +-
 .../backend_tools_cloud_sandbox_orchestrator.py.md |     2 +-
 .../backend_tools_code_smell_detector.py.md        |     2 +-
 .../codebase/backend_tools_codebase_exporter.py.md |     2 +-
 .../backend_tools_collaborative_editor.py.md       |     2 +-
 .../codebase/backend_tools_comment_thread_ai.py.md |     2 +-
 .../codebase/backend_tools_computer_agent.py.md    |     2 +-
 .../backend_tools_conversation_manager.py.md       |     2 +-
 .../codebase/backend_tools_cost_auditor.py.md      |     2 +-
 .../codebase/backend_tools_cot_reasoner.py.md      |     2 +-
 .../codebase/backend_tools_coverage_auditor.py.md  |     2 +-
 .../backend_tools_dependency_manager_agent.py.md   |     2 +-
 .../backend_tools_diagram_to_architecture.py.md    |     2 +-
 .../codebase/backend_tools_docker_sandbox.py.md    |     2 +-
 .../codebase/backend_tools_domain_adapter.py.md    |     2 +-
 .../codebase/backend_tools_email_agent.py.md       |     2 +-
 .../codebase/backend_tools_ensemble_router.py.md   |     2 +-
 .../codebase/backend_tools_fuzz_sandbox.py.md      |     2 +-
 .../codebase/backend_tools_game_dev_agent.py.md    |     2 +-
 .../backend_tools_gcp_cloud_functions.py.md        |     2 +-
 .../backend_tools_git_knowledge_extractor.py.md    |     2 +-
 .../codebase/backend_tools_github_agent.py.md      |     2 +-
 .../codebase/backend_tools_graph_service.py.md     |     2 +-
 .../backend_tools_headless_agent_registry.py.md    |     2 +-
 .../codebase/backend_tools_health_checker.py.md    |     2 +-
 .../codebase/backend_tools_image_generator.py.md   |     2 +-
 .../codebase/backend_tools_image_to_code.py.md     |     2 +-
 docs/autogen/codebase/backend_tools_init_.py.md    |     2 +-
 .../backend_tools_knowledge_base_indexer.py.md     |     2 +-
 .../backend_tools_langchain_agent_example.py.md    |     2 +-
 .../codebase/backend_tools_legal_agent.py.md       |     2 +-
 .../backend_tools_local_ocr_extractor.py.md        |     2 +-
 .../codebase/backend_tools_local_search_rag.py.md  |     2 +-
 .../codebase/backend_tools_marketplace_agent.py.md |     2 +-
 .../codebase/backend_tools_mcp_cloud_deploy.py.md  |     2 +-
 .../codebase/backend_tools_mcp_github_cicd.py.md   |     2 +-
 .../codebase/backend_tools_mcp_server.py.md        |     2 +-
 .../codebase/backend_tools_mcp_supabase.py.md      |     2 +-
 .../codebase/backend_tools_mcp_workspace.py.md     |     2 +-
 .../codebase/backend_tools_medical_agent.py.md     |     2 +-
 .../codebase/backend_tools_meta_architect.py.md    |     2 +-
 .../codebase/backend_tools_model_trainer.py.md     |     2 +-
 .../backend_tools_monthly_cost_reporter.py.md      |     2 +-
 .../backend_tools_multi_account_rotator.py.md      |     2 +-
 .../codebase/backend_tools_multilingual_tts.py.md  |     2 +-
 .../codebase/backend_tools_music_generator.py.md   |     2 +-
 .../codebase/backend_tools_offline_mode.py.md      |     2 +-
 .../backend_tools_on_premise_deployer.py.md        |     2 +-
 .../backend_tools_parallel_agent_executor.py.md    |     2 +-
 .../codebase/backend_tools_pdf_to_sdk.py.md        |     2 +-
 .../codebase/backend_tools_plan_sorter.py.md       |     2 +-
 .../backend_tools_playwright_browser_agent.py.md   |     2 +-
 .../codebase/backend_tools_pr_reviewer.py.md       |     2 +-
 .../codebase/backend_tools_pre_commit_ai.py.md     |     2 +-
 .../codebase/backend_tools_preference_memory.py.md |     2 +-
 .../backend_tools_presentation_generator.py.md     |     2 +-
 .../codebase/backend_tools_proxy_manager.py.md     |     2 +-
 .../codebase/backend_tools_repo_deep_indexer.py.md |     2 +-
 .../backend_tools_repo_discovery_agent.py.md       |     2 +-
 .../codebase/backend_tools_resource_catalog.py.md  |     2 +-
 .../codebase/backend_tools_rlhf_pipeline.py.md     |     2 +-
 .../codebase/backend_tools_safe_executor.py.md     |     2 +-
 .../codebase/backend_tools_scientific_agent.py.md  |     2 +-
 .../codebase/backend_tools_seed_database.py.md     |     2 +-
 .../codebase/backend_tools_self_planner.py.md      |     2 +-
 .../codebase/backend_tools_skill_recommender.py.md |     2 +-
 .../codebase/backend_tools_sso_integrator.py.md    |     2 +-
 .../backend_tools_stealth_http_client.py.md        |     2 +-
 .../codebase/backend_tools_style_learner.py.md     |     2 +-
 .../codebase/backend_tools_telegram_bot.py.md      |     2 +-
 .../backend_tools_tenant_rate_limiter.py.md        |     2 +-
 .../backend_tools_test_3d_model_generator.py.md    |     2 +-
 ...end_tools_test_cloud_sandbox_orchestrator.py.md |     2 +-
 .../codebase/backend_tools_trading_agent.py.md     |     2 +-
 .../codebase/backend_tools_video_generator.py.md   |     2 +-
 .../backend_tools_viral_referral_engine.py.md      |     2 +-
 .../codebase/backend_tools_vision_agent.py.md      |     2 +-
 docs/autogen/codebase/backend_tools_voice.py.md    |     2 +-
 .../codebase/backend_tools_voice_coder.py.md       |     2 +-
 .../codebase/backend_tools_vpn_switcher.py.md      |     2 +-
 .../backend_tools_vulnerability_predictor.py.md    |     2 +-
 .../backend_tools_web_fallback_agent.py.md         |     2 +-
 .../codebase/backend_utils_api_tracker.py.md       |     2 +-
 .../codebase/backend_utils_environment.py.md       |     2 +-
 .../codebase/backend_utils_firestore_helpers.py.md |     2 +-
 .../codebase/backend_utils_http_client.py.md       |     2 +-
 docs/autogen/codebase/backend_utils_init_.py.md    |     2 +-
 .../codebase/backend_utils_json_helpers.py.md      |     2 +-
 .../codebase/backend_utils_timestamps.py.md        |     2 +-
 docs/autogen/codebase/backend_uv.lock.md           |     2 +-
 .../codebase/backend_workers_celery_app.py.md      |     2 +-
 .../codebase/backend_workers_chaos_worker.py.md    |     2 +-
 .../codebase/config_.pre-commit-config.yaml.md     |     2 +-
 docs/autogen/codebase/config_audit-rules.yml.md    |     2 +-
 .../codebase/config_compliance-rules.yml.md        |     2 +-
 docs/autogen/codebase/config_docker-limits.yml.md  |     2 +-
 docs/autogen/codebase/config_firebase.json.md      |     2 +-
 .../codebase/config_firestore.indexes.json.md      |     2 +-
 docs/autogen/codebase/config_kilo.json.md          |     2 +-
 .../codebase/config_promptfooconfig.yaml.md        |     2 +-
 docs/autogen/codebase/config_proxy_list.json.md    |     2 +-
 .../autogen/codebase/config_routing_policy.json.md |     2 +-
 docs/autogen/codebase/config_vercel.json.md        |     2 +-
 docs/autogen/codebase/coverage.json.md             |     2 +-
 docs/autogen/codebase/coverage.toml.md             |     2 +-
 docs/autogen/codebase/docker-compose.yml.md        |     2 +-
 .../codebase/evolution_auto_skill_creator.py.md    |     2 +-
 .../autogen/codebase/evolution_daily_learner.py.md |     2 +-
 .../codebase/evolution_evolution_engine.py.md      |     2 +-
 .../codebase/evolution_evolution_react_agent.py.md |     2 +-
 docs/autogen/codebase/evolution_self_updater.py.md |     2 +-
 docs/autogen/codebase/find_duplicate_files.py.md   |     2 +-
 docs/autogen/codebase/find_duplicate_tests.py.md   |     2 +-
 .../infrastructure_check_deploy_gate.py.md         |     2 +-
 ...infrastructure_cloudflare_enhanced-worker.js.md |     2 +-
 .../infrastructure_cloudflare_worker.js.md         |     2 +-
 .../infrastructure_cloudflare_wrangler.toml.md     |     2 +-
 .../infrastructure_cloudrun_autoscale.yaml.md      |     2 +-
 .../infrastructure_cloudrun_multi_region.yaml.md   |     2 +-
 ...functions_firebase_functions_v1_README_BD.md.md |     2 +-
 ...unctions_firebase_functions_v1_api-router.js.md |     2 +-
 ..._firebase_functions_v1_deployment-monitor.js.md |     2 +-
 ...ctions_firebase_functions_v1_health-smart.js.md |     2 +-
 ...ase_functions_firebase_functions_v1_index.js.md |     2 +-
 ...irebase_functions_v1_lib_chatClassifier.d.ts.md |     2 +-
 ..._firebase_functions_v1_lib_chatClassifier.js.md |     2 +-
 ...firebase_functions_v1_lib_email_handler.d.ts.md |     2 +-
 ...s_firebase_functions_v1_lib_email_handler.js.md |     2 +-
 ...nctions_firebase_functions_v1_lib_index.d.ts.md |     2 +-
 ...functions_firebase_functions_v1_lib_index.js.md |     2 +-
 ..._firebase_functions_v1_lib_scrapeEngine.d.ts.md |     2 +-
 ...ns_firebase_functions_v1_lib_scrapeEngine.js.md |     2 +-
 ...e_functions_v1_lib_scrapeHistoryManager.d.ts.md |     2 +-
 ...ase_functions_v1_lib_scrapeHistoryManager.js.md |     2 +-
 ...functions_firebase_functions_v1_package.json.md |     2 +-
 ...ons_firebase_functions_v1_providers-smart.js.md |     2 +-
 ...se_functions_v1_server-connection-monitor.js.md |     2 +-
 ..._firebase_functions_v1_src_chatClassifier.ts.md |     2 +-
 ...dataconnect-admin-generated_esm_index.esm.js.md |     2 +-
 ...dataconnect-admin-generated_esm_package.json.md |     2 +-
 ...src_dataconnect-admin-generated_index.cjs.js.md |     2 +-
 ...1_src_dataconnect-admin-generated_index.d.ts.md |     2 +-
 ...src_dataconnect-admin-generated_package.json.md |     2 +-
 ...s_firebase_functions_v1_src_email_handler.ts.md |     2 +-
 ...functions_firebase_functions_v1_src_index.ts.md |     2 +-
 ...ns_firebase_functions_v1_src_scrapeEngine.ts.md |     2 +-
 ...ase_functions_v1_src_scrapeHistoryManager.ts.md |     2 +-
 ..._firebase_functions_v1_src_scrapeSchema.yaml.md |     2 +-
 ...functions_firebase_functions_v1_swagger.yaml.md |     2 +-
 ...tions_firebase_functions_v1_system-health.js.md |     2 +-
 ...unctions_firebase_functions_v1_tsconfig.json.md |     2 +-
 ...irebase_functions_v1_utils_externalClient.js.md |     2 +-
 ...rastructure_firebase_functions_ocrTrigger.ts.md |     2 +-
 ...ure_monitoring_docker-compose.monitoring.yml.md |     2 +-
 ...astructure_monitoring_grafana_dashboard.json.md |     2 +-
 ...cture_terraform_root_cause_analysis_agent.py.md |     2 +-
 ..._terraform_test_root_cause_analysis_agent.py.md |     2 +-
 .../codebase/infrastructure_vitest-report.json.md  |     2 +-
 docs/autogen/codebase/package.json.md              |     2 +-
 .../codebase/packages_shared-types_package.json.md |     2 +-
 .../packages_shared-types_src_conversation.ts.md   |     2 +-
 .../codebase/packages_shared-types_src_index.ts.md |     2 +-
 .../packages_shared-types_src_message.ts.md        |     2 +-
 .../packages_shared-types_tsconfig.json.md         |     2 +-
 .../packages_ui-components_package.json.md         |     2 +-
 .../packages_ui-components_src_ChatBubble.tsx.md   |     2 +-
 ...components_src_components_DashboardShell.tsx.md |     2 +-
 ...nents_src_components_LiveSujonBackground.tsx.md |     2 +-
 ...-components_src_contexts_SharedProviders.tsx.md |     2 +-
 .../packages_ui-components_src_index.ts.md         |     2 +-
 .../packages_ui-components_tsconfig.json.md        |     2 +-
 docs/autogen/codebase/playwright-ct.config.ts.md   |     2 +-
 docs/autogen/codebase/pnpm-lock.yaml.md            |     2 +-
 docs/autogen/codebase/pnpm-workspace.yaml.md       |     2 +-
 docs/autogen/codebase/scratch_job_details.json.md  |     2 +-
 docs/autogen/codebase/scratch_smoke_check.py.md    |     2 +-
 .../scratch_supremeai_skill_ecosystem_app.py.md    |     2 +-
 ...ratch_supremeai_skill_ecosystem_generator.py.md |     2 +-
 ..._supremeai_skill_ecosystem_sample_skill.json.md |     2 +-
 ...ch_supremeai_skill_ecosystem_skill_schema.py.md |     2 +-
 .../codebase/scratch_sync_gsm_secrets.py.md        |     2 +-
 docs/autogen/codebase/scratch_update_vault.py.md   |     2 +-
 .../autogen/codebase/scratch_update_vault_r2.py.md |     2 +-
 .../codebase/scratch_verify_project_health.py.md   |     2 +-
 .../codebase/scripts_add_bangla_comments.py.md     |     2 +-
 .../codebase/scripts_aggregate_context.py.md       |     2 +-
 ...scripts_backup_auto_cross_cloud_replicate.py.md |     2 +-
 .../scripts_backup_auto_firestore_backup.py.md     |     2 +-
 .../scripts_benchmark_perf_benchmark.py.md         |     2 +-
 .../codebase/scripts_bots_auto_alert_bot.py.md     |     2 +-
 .../scripts_bots_auto_daily_standup_bot.py.md      |     2 +-
 .../codebase/scripts_code_smell_detector.py.md     |     2 +-
 docs/autogen/codebase/scripts_codebase_to_md.py.md |     2 +-
 .../codebase/scripts_codegraph_integration.py.md   |     2 +-
 .../codebase/scripts_commit_supreme_ci.yml.md      |     2 +-
 docs/autogen/codebase/scripts_config_audit.py.md   |     2 +-
 .../scripts_core_engine_multicatalog_search.py.md  |     2 +-
 .../codebase/scripts_core_engine_tool_ranker.py.md |     2 +-
 .../codebase/scripts_create_test_admin.py.md       |     2 +-
 .../autogen/codebase/scripts_db_auto_migrate.py.md |     2 +-
 docs/autogen/codebase/scripts_db_auto_seed.py.md   |     2 +-
 .../autogen/codebase/scripts_docker_ai_guard.py.md |     2 +-
 ...ipts_evolution_auto_marketing_skill_forge.py.md |     2 +-
 docs/autogen/codebase/scripts_fix_mypy.py.md       |     2 +-
 docs/autogen/codebase/scripts_fuzz_sandbox.py.md   |     2 +-
 .../scripts_generate_codebase_markdown.py.md       |     2 +-
 ...scripts_generate_codebase_single_markdown.py.md |     2 +-
 docs/autogen/codebase/scripts_generate_md.py.md    |     2 +-
 .../codebase/scripts_generate_smart_docs.py.md     |     2 +-
 docs/autogen/codebase/scripts_k6_load_test.js.md   |     2 +-
 docs/autogen/codebase/scripts_locustfile.py.md     |     2 +-
 docs/autogen/codebase/scripts_migrate.py.md        |     2 +-
 .../codebase/scripts_multi_model_validator.py.md   |     2 +-
 ...scripts_orchestrator_auto_budget_guardian.py.md |     2 +-
 docs/autogen/codebase/scripts_profile_memory.py.md |     2 +-
 .../scripts_quality_auto_dead_code_remover.py.md   |     2 +-
 .../scripts_quality_auto_improve_coverage.py.md    |     2 +-
 .../scripts_quality_auto_refactor_suggester.py.md  |     2 +-
 ...cripts_quality_check_ollama_test_coverage.py.md |     2 +-
 .../scripts_resource_collection_awesome_go.py.md   |     2 +-
 ...cripts_resource_collection_awesome_python.py.md |     2 +-
 ...ts_resource_collection_awesome_selfhosted.py.md |     2 +-
 ...ripts_resource_collection_base_api_client.py.md |     2 +-
 .../scripts_resource_collection_base_scraper.py.md |     2 +-
 ...pts_resource_collection_ossinsight_client.py.md |     2 +-
 ...ipts_resource_collection_ossinsight_init_.py.md |     2 +-
 ...ripts_resource_collection_ossinsight_test.py.md |     2 +-
 .../scripts_resource_collection_run_all.py.md      |     2 +-
 ...ts_resource_collection_run_all_collectors.py.md |     2 +-
 ...ripts_resource_scraping_awesome_go_scrape.py.md |     2 +-
 ...s_resource_scraping_awesome_python_scrape.py.md |     2 +-
 ...source_scraping_awesome_selfhosted_scrape.py.md |     2 +-
 .../codebase/scripts_run_all_collectors.py.md      |     2 +-
 docs/autogen/codebase/scripts_safety_guard.py.md   |     2 +-
 .../scripts_security_auto_find_blindspots.py.md    |     2 +-
 .../scripts_security_auto_secret_rotate.py.md      |     2 +-
 .../scripts_security_check_dependencies.py.md      |     2 +-
 .../codebase/scripts_security_code-quality.yml.md  |     2 +-
 ...scripts_security_dependency-health-check.yml.md |     2 +-
 .../codebase/scripts_security_find_dead_code.py.md |     2 +-
 docs/autogen/codebase/scripts_seed_repos.py.md     |     2 +-
 .../autogen/codebase/scripts_setup_ci_runner.py.md |     2 +-
 .../codebase/scripts_setup_firebase_admin.py.md    |     2 +-
 docs/autogen/codebase/scripts_skill_loader.py.md   |     2 +-
 .../codebase/scripts_supreme-config-audit.py.md    |     2 +-
 .../codebase/scripts_supreme-docker-analyzer.py.md |     2 +-
 .../codebase/scripts_supreme-risk-scorer.py.md     |     2 +-
 .../codebase/scripts_supreme_context_builder.py.md |     2 +-
 .../scripts_tenant_auto_tenant_health_report.py.md |     2 +-
 .../scripts_tenant_auto_tenant_setup.py.md         |     2 +-
 docs/autogen/codebase/scripts_test_bangla.py.md    |     2 +-
 docs/autogen/codebase/scripts_test_read.py.md      |     2 +-
 docs/autogen/codebase/security-scan.yml.md         |     2 +-
 .../codebase/skills_dynamic_csv_exporter.py.md     |     2 +-
 .../codebase/skills_dynamic_text_summarizer.py.md  |     2 +-
 .../codebase/skills_dynamic_web_scraper.py.md      |     2 +-
 docs/autogen/codebase/skills_init_.py.md           |     2 +-
 docs/autogen/codebase/skills_installer.py.md       |     2 +-
 docs/autogen/codebase/skills_marketplace.py.md     |     2 +-
 docs/autogen/codebase/skills_registry.py.md        |     2 +-
 docs/autogen/codebase/skills_schema.py.md          |     2 +-
 .../codebase/test-results_.last-run.json.md        |     2 +-
 .../codebase/tests_e2e_admin-dashboard.spec.ts.md  |     2 +-
 docs/autogen/codebase/tests_e2e_chat.spec.ts.md    |     2 +-
 .../codebase/tests_e2e_playwright.config.ts.md     |    47 +-
 docs/autogen/codebase/tests_e2e_visual.spec.ts.md  |     2 +-
 docs/autogen/codebase/tests_test_tenant_di.py.md   |     2 +-
 docs/autogen/codebase/tools_cache_cleanup.py.md    |     2 +-
 .../tools_vscode-extension_ARCHITECTURE_BN.md.md   |     2 +-
 ...vscode-extension_AdminMetricsController.java.md |     2 +-
 ...s_vscode-extension_CodebaseAuditService.java.md |     2 +-
 ...ools_vscode-extension_FeatureDefinition.java.md |     2 +-
 ...ode-extension_FeatureRegistryController.java.md |     2 +-
 ...vscode-extension_FeatureRegistryService.java.md |     2 +-
 .../tools_vscode-extension_GlobalMetrics.java.md   |     2 +-
 ...s_vscode-extension_GlobalMetricsService.java.md |     2 +-
 ...ols_vscode-extension_INTEGRATION_GUIDE_BN.md.md |     2 +-
 .../codebase/tools_vscode-extension_README.md.md   |     2 +-
 .../tools_vscode-extension_README_BN.md.md         |     2 +-
 .../tools_vscode-extension_jest.config.js.md       |     2 +-
 .../tools_vscode-extension_package.json.md         |     2 +-
 .../tools_vscode-extension_package.nls.bn.json.md  |     2 +-
 .../tools_vscode-extension_src_agentDetector.ts.md |     2 +-
 .../tools_vscode-extension_src_ai_AIService.ts.md  |     2 +-
 ...de-extension_src_ai_CodeGenerationService.ts.md |     2 +-
 ...vscode-extension_src_ai_CodeReviewService.ts.md |     2 +-
 ...ls_vscode-extension_src_ai_ContextBuilder.ts.md |     2 +-
 ...xtension_src_dataconnect-generated_README.md.md |     2 +-
 ...n_src_dataconnect-generated_esm_index.esm.js.md |     2 +-
 ...n_src_dataconnect-generated_esm_package.json.md |     2 +-
 ...nsion_src_dataconnect-generated_index.cjs.js.md |     2 +-
 ...tension_src_dataconnect-generated_index.d.ts.md |     2 +-
 ...nsion_src_dataconnect-generated_package.json.md |     2 +-
 .../tools_vscode-extension_src_extension.ts.md     |     2 +-
 ...de-extension_src_handlers_CodeEditHandler.ts.md |     2 +-
 ...de-extension_src_handlers_CodeFlowHandler.ts.md |     2 +-
 ...scode-extension_src_handlers_ErrorHandler.ts.md |     2 +-
 ...de-extension_src_handlers_FeedbackHandler.ts.md |     2 +-
 ...ode-extension_src_providers_CodeFlowPanel.ts.md |     2 +-
 ...nsion_src_providers_StreamingChatProvider.ts.md |     2 +-
 ...n_src_providers_SupremeAIActivityProvider.ts.md |     2 +-
 ...providers_SupremeAIAdminDashboardProvider.ts.md |     2 +-
 ...nsion_src_providers_SupremeAIChatProvider.ts.md |     2 +-
 ...extension_src_providers_SupremeAIChatView.ts.md |     2 +-
 ...viders_SupremeAICustomerDashboardProvider.ts.md |     2 +-
 ...on_src_providers_SupremeAISidebarProvider.ts.md |     2 +-
 ...vscode-extension_src_services_AuthService.ts.md |     2 +-
 ...e-extension_src_services_SupremeAIService.ts.md |     2 +-
 .../tools_vscode-extension_src_types_index.ts.md   |     2 +-
 ...ension_src_utils_DynamicSignatureRegistry.ts.md |     2 +-
 ...s_vscode-extension_test_auth-service.test.ts.md |     2 +-
 ...ools_vscode-extension_test_mocks_vscode.d.ts.md |     2 +-
 .../tools_vscode-extension_test_mocks_vscode.ts.md |     2 +-
 .../tools_vscode-extension_test_setup.ts.md        |     2 +-
 ...ode-extension_test_supremeai-service.test.ts.md |     2 +-
 .../tools_vscode-extension_tsconfig.json.md        |     2 +-
 .../tools_vscode-extension_vitest.config.ts.md     |     2 +-
 docs/autogen/codebase/turbo.json.md                |     2 +-
 docs/autogen/codebase/visual.spec.ts.md            |     2 +-
 docs/autogen/codebase_full.md                      |    45 +-
 1066 files changed, 12889 insertions(+), 13824 deletions(-)

```

## Diff Detail
```diff
commit f0ef01a49b2b8fab7fcfbe0040c4c15ab4aa639a
Author: github-actions[bot] <github-actions[bot]@users.noreply.github.com>
Date:   Sat Jul 4 08:43:37 2026 +0000

    docs: auto-update codebase docs & dashboard [skip ci]

diff --git a/docs/autogen/INDEX.md b/docs/autogen/INDEX.md
index b0dd2b15e..a65793cad 100644
--- a/docs/autogen/INDEX.md
+++ b/docs/autogen/INDEX.md
@@ -13,4 +13,4 @@
 - **ডিরেক্টরি:** [changes/](changes/)
 
 ---
-*স্বয়ংক্রিয়ভাবে তৈরি — 2026-07-04 08:34:56*
+*স্বয়ংক্রিয়ভাবে তৈরি — 2026-07-04 08:43:36*
diff --git a/docs/autogen/changes/change_04162a2bd5f35a10b23f6f6dc4f2f7348e3b5c81.md b/docs/autogen/changes/change_04162a2bd5f35a10b23f6f6dc4f2f7348e3b5c81.md
deleted file mode 100644
index fc31b13eb..000000000
--- a/docs/autogen/changes/change_04162a2bd5f35a10b23f6f6dc4f2f7348e3b5c81.md
+++ /dev/null
@@ -1,1987 +0,0 @@
-# 📋 Commit 04162a2bd5f35a10b23f6f6dc4f2f7348e3b5c81
-
-## Commit Stats
-```
-commit 04162a2bd5f35a10b23f6f6dc4f2f7348e3b5c81
-Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
-Date:   Sat Jul 4 14:11:08 2026 +0600
-
-    Commit changes
-
- apps/desktop/src-tauri/Cargo.toml                  |   2 +-
- apps/desktop/src-tauri/src/main.rs                 |   2 +-
- apps/desktop/src-tauri/tauri.conf.json             |   8 +-
- apps/desktop/src-ui/package.json                   |   3 +-
- apps/desktop/src-ui/src/App.tsx                    |  48 +-
- apps/desktop/src-ui/src/main.tsx                   |   5 +-
- apps/docs/.docusaurus/client-manifest.json         |  76 +--
- apps/docs/.docusaurus/client-modules.js            |   4 +-
- apps/docs/docusaurus.config.ts                     |   8 +-
- apps/studio-client/package.json                    |   1 +
- .../src/components/dashboard/DashboardShell.tsx    | 174 +++---
- apps/studio-client/src/main.tsx                    |   6 +-
- backend/coverage.json                              |   2 +-
- backend/tests/tools/test_viral_referral_engine.py  |  11 +-
- package.json                                       |  13 +-
- packages/ui-components/package.json                |  21 +-
- .../src/components/DashboardShell.tsx              |  18 +
- .../src/components/LiveSujonBackground.tsx         |   7 +
- packages/ui-components/src/components/styles.css   |   2 +
- .../ui-components/src/contexts/SharedProviders.tsx |  21 +
- packages/ui-components/src/index.ts                |   4 +
- pnpm-lock.yaml                                     | 599 +--------------------
- 22 files changed, 267 insertions(+), 768 deletions(-)
-
-```
-
-## Diff Detail
-```diff
-commit 04162a2bd5f35a10b23f6f6dc4f2f7348e3b5c81
-Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
-Date:   Sat Jul 4 14:11:08 2026 +0600
-
-    Commit changes
-
-diff --git a/apps/desktop/src-tauri/Cargo.toml b/apps/desktop/src-tauri/Cargo.toml
-index 239f8c796..a708e44f5 100644
---- a/apps/desktop/src-tauri/Cargo.toml
-+++ b/apps/desktop/src-tauri/Cargo.toml
-@@ -15,7 +15,7 @@ custom-protocol = ["tauri/custom-protocol"]
- tauri-build = { version = "=1.5.4", features = [] }
- 
- [dependencies]
--tauri = { version = "=1.5.4", features = [ "window-maximize", "window-start-dragging", "window-unminimize", "window-unmaximize", "window-hide", "window-show", "window-minimize", "window-close", "notification", "global-shortcut"] }
-+tauri = { version = "=1.5.4", features = [ "window-maximize", "window-start-dragging", "window-unminimize", "window-unmaximize", "window-hide", "window-show", "window-minimize", "window-close", "notification", "global-shortcut", "system-tray", "updater", "api-all"] }
- serde_json = "1"
- num_cpus = "1"
- ntapi = "0.4.3"
-diff --git a/apps/desktop/src-tauri/src/main.rs b/apps/desktop/src-tauri/src/main.rs
-index 9fdc78241..b99c97dac 100644
---- a/apps/desktop/src-tauri/src/main.rs
-+++ b/apps/desktop/src-tauri/src/main.rs
-@@ -4,7 +4,7 @@
- )]
- 
- use tauri::{Manager, SystemTray, SystemTrayEvent, SystemTrayMenu, SystemTrayMenuItem, CustomMenuItem, SystemTrayEvent::MenuEvent};
--use tauri::api::{fs::read_text_file, notification::{Notification, NotificationAction}, updater};
-+use tauri::api::{fs::read_text_file, notification::Notification, updater};
- use std::sync::Mutex;
- 
- struct AppState {
-diff --git a/apps/desktop/src-tauri/tauri.conf.json b/apps/desktop/src-tauri/tauri.conf.json
-index dba4e6fdf..3df412b6b 100644
---- a/apps/desktop/src-tauri/tauri.conf.json
-+++ b/apps/desktop/src-tauri/tauri.conf.json
-@@ -1,7 +1,7 @@
- {
-   "build": {
--    "beforeBuildCommand": "npm run build:ui",
--    "beforeDevCommand": "npm run dev:ui",
-+    "beforeBuildCommand": "pnpm --dir src-ui build",
-+    "beforeDevCommand": "pnpm --dir src-ui dev",
-     "devPath": "http://localhost:1420",
-     "distDir": "../src-ui/dist"
-   },
-@@ -21,9 +21,7 @@
-       "notification": {
-         "all": false
-       },
--      "plugin": {
--        "store": true
--      },
-+      
-       "window": {
-         "all": false,
-         "close": true,
-diff --git a/apps/desktop/src-ui/package.json b/apps/desktop/src-ui/package.json
-index 825f6450e..9bb964cc1 100644
---- a/apps/desktop/src-ui/package.json
-+++ b/apps/desktop/src-ui/package.json
-@@ -15,7 +15,8 @@
-     "react-dom": "^19.2.7",
-     "react-router-dom": "^6.4.0",
-     "typescript": "^5.4.0",
--    "zustand": "^4.3.9"
-+    "zustand": "^4.3.9",
-+    "@supremeai/ui-components": "workspace:*"
-   },
-   "scripts": {
-     "dev": "vite",
-diff --git a/apps/desktop/src-ui/src/App.tsx b/apps/desktop/src-ui/src/App.tsx
-index 4ea33c980..6af150091 100644
---- a/apps/desktop/src-ui/src/App.tsx
-+++ b/apps/desktop/src-ui/src/App.tsx
-@@ -1,10 +1,12 @@
--import { BrowserRouter as Router, Routes, Route, NavLink } from 'react-router-dom';
-+import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
- import ChatPage from './pages/ChatPage';
- import SkillsPage from './pages/SkillsPage';
- import EvolutionPage from './pages/EvolutionPage';
- import AdminPage from './pages/AdminPage';
- import LoginPage from './pages/LoginPage';
- import './App.css';
-+// Use shared DashboardShell from packages
-+import { DashboardShell as SharedDashboardShell } from '../../../../packages/ui-components/src/components/DashboardShell';
- import { useAuthStore } from './stores/authStore';
- 
- const NavButton = ({ to, label }: { to: string; label: string }) => (
-@@ -15,51 +17,11 @@ const NavButton = ({ to, label }: { to: string; label: string }) => (
- 
- function App() {
-   const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
--  const logout = useAuthStore((state) => state.logout);
- 
-   return (
-     <Router>
--      <div className="App">
--        <nav className="navbar">
--          <div className="navbar-brand">
--            <h1>SupremeAI 2.0</h1>
--          </div>
--          <div className="navbar-menu">
--            {isAuthenticated ? (
--              <>
--                <NavButton to="/" label="Chat" />
--                <NavButton to="/skills" label="Skills" />
--                <NavButton to="/evolution" label="Evolution" />
--                <NavButton to="/admin" label="Admin" />
--                <button className="nav-btn" onClick={logout}>
--                  Logout
--                </button>
--              </>
--            ) : (
--              <NavLink to="/login" className={({ isActive }) => `nav-btn ${isActive ? 'active' : ''}`}>
--                Login
--              </NavLink>
--            )}
--          </div>
--        </nav>
-+      <SharedDashboardShell isServerOnline={true}>
-         <div className="app-content">
--          <aside className="sidebar">
--            <div className="sidebar-section">
--              <h3>History</h3>
--              <ul>
--                <li>New Chat</li>
--                <li>Previous Session</li>
--              </ul>
--            </div>
--            <div className="sidebar-section">
--              <h3>Skills</h3>
--              <ul>
--                <li>Web Scraper</li>
--                <li>Code Generator</li>
--                <li>Data Analyzer</li>
--              </ul>
--            </div>
--          </aside>
-           <main className="main-content">
-             <Routes>
-               <Route path="/login" element={<LoginPage />} />
-@@ -71,7 +33,7 @@ function App() {
-             </Routes>
-           </main>
-         </div>
--      </div>
-+      </SharedDashboardShell>
-     </Router>
-   );
- }
-diff --git a/apps/desktop/src-ui/src/main.tsx b/apps/desktop/src-ui/src/main.tsx
-index fd41c35a3..7ec785598 100644
---- a/apps/desktop/src-ui/src/main.tsx
-+++ b/apps/desktop/src-ui/src/main.tsx
-@@ -1,10 +1,13 @@
- import React from "react"
- import ReactDOM from "react-dom/client"
- import App from "./App"
-+import { SharedProviders } from '@supremeai/ui-components'
- import "./index.css"
- 
- ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
-   <React.StrictMode>
--    <App />
-+    <SharedProviders>
-+      <App />
-+    </SharedProviders>
-   </React.StrictMode>
- )
-diff --git a/apps/docs/.docusaurus/client-manifest.json b/apps/docs/.docusaurus/client-manifest.json
-index 0ed8bad83..77fa88b5e 100644
---- a/apps/docs/.docusaurus/client-manifest.json
-+++ b/apps/docs/.docusaurus/client-manifest.json
-@@ -6,8 +6,8 @@
-     "166": [
-       166
-     ],
--    "544": [
--      544
-+    "387": [
-+      387
-     ],
-     "17896441": [
-       869,
-@@ -60,9 +60,9 @@
-     "48": {
-       "js": [
-         {
--          "file": "assets/js/a94703ab.203c690c.js",
--          "hash": "61f6541f01ac9a1a",
--          "publicPath": "/bn/assets/js/a94703ab.203c690c.js"
-+          "file": "assets/js/a94703ab.24a772d6.js",
-+          "hash": "6abacf5a64f8a461",
-+          "publicPath": "/bn/assets/js/a94703ab.24a772d6.js"
-         }
-       ]
-     },
-@@ -78,9 +78,9 @@
-     "98": {
-       "js": [
-         {
--          "file": "assets/js/a7bd4aaa.736b4845.js",
--          "hash": "1a1647a53688682a",
--          "publicPath": "/bn/assets/js/a7bd4aaa.736b4845.js"
-+          "file": "assets/js/a7bd4aaa.75a1ee98.js",
-+          "hash": "bd87788e67ed2bd8",
-+          "publicPath": "/bn/assets/js/a7bd4aaa.75a1ee98.js"
-         }
-       ]
-     },
-@@ -96,54 +96,54 @@
-     "354": {
-       "js": [
-         {
--          "file": "assets/js/runtime~main.feb2f3a9.js",
--          "hash": "0fd5e84957ec2ef9",
--          "publicPath": "/bn/assets/js/runtime~main.feb2f3a9.js"
-+          "file": "assets/js/runtime~main.3816940e.js",
-+          "hash": "f904fc9555a6d694",
-+          "publicPath": "/bn/assets/js/runtime~main.3816940e.js"
-         }
-       ]
-     },
--    "401": {
-+    "387": {
-       "js": [
-         {
--          "file": "assets/js/17896441.4c9613c2.js",
--          "hash": "dbd8dfd6e643dde5",
--          "publicPath": "/bn/assets/js/17896441.4c9613c2.js"
-+          "file": "assets/js/387.cce20004.js",
-+          "hash": "89bdce2f84458555",
-+          "publicPath": "/bn/assets/js/387.cce20004.js"
-         }
-       ]
-     },
--    "443": {
-+    "401": {
-       "js": [
-         {
--          "file": "assets/js/964ae018.882a5d38.js",
--          "hash": "c2574b85daa3269d",
--          "publicPath": "/bn/assets/js/964ae018.882a5d38.js"
-+          "file": "assets/js/17896441.914fcce5.js",
-+          "hash": "e635aa544115df98",
-+          "publicPath": "/bn/assets/js/17896441.914fcce5.js"
-         }
-       ]
-     },
--    "544": {
-+    "443": {
-       "js": [
-         {
--          "file": "assets/js/544.969487dd.js",
--          "hash": "47a358efb970a44a",
--          "publicPath": "/bn/assets/js/544.969487dd.js"
-+          "file": "assets/js/964ae018.0c261e90.js",
-+          "hash": "782e525f6999bfbd",
-+          "publicPath": "/bn/assets/js/964ae018.0c261e90.js"
-         }
-       ]
-     },
-     "647": {
-       "js": [
-         {
--          "file": "assets/js/5e95c892.969aa490.js",
--          "hash": "d2efed282fa6b849",
--          "publicPath": "/bn/assets/js/5e95c892.969aa490.js"
-+          "file": "assets/js/5e95c892.7259a435.js",
-+          "hash": "3f8d34939c5d8887",
-+          "publicPath": "/bn/assets/js/5e95c892.7259a435.js"
-         }
-       ]
-     },
-     "731": {
-       "js": [
-         {
--          "file": "assets/js/5eb850a8.a2f1b430.js",
--          "hash": "8c8710021515a255",
--          "publicPath": "/bn/assets/js/5eb850a8.a2f1b430.js"
-+          "file": "assets/js/5eb850a8.78021196.js",
-+          "hash": "47cf730ea6fc2809",
-+          "publicPath": "/bn/assets/js/5eb850a8.78021196.js"
-         }
-       ]
-     },
-@@ -159,27 +159,27 @@
-     "792": {
-       "js": [
-         {
--          "file": "assets/js/main.02d07d1a.js",
--          "hash": "31bb27961b8283a9",
--          "publicPath": "/bn/assets/js/main.02d07d1a.js"
-+          "file": "assets/js/main.377f3008.js",
-+          "hash": "89c98d8c615b8998",
-+          "publicPath": "/bn/assets/js/main.377f3008.js"
-         }
-       ]
-     },
-     "869": {
-       "css": [
-         {
--          "file": "assets/css/styles.c0fccb90.css",
--          "hash": "6cfbb75421ab7c98",
--          "publicPath": "/bn/assets/css/styles.c0fccb90.css"
-+          "file": "assets/css/styles.787696f4.css",
-+          "hash": "f61f1d28f483ae8e",
-+          "publicPath": "/bn/assets/css/styles.787696f4.css"
-         }
-       ]
-     },
-     "976": {
-       "js": [
-         {
--          "file": "assets/js/0e384e19.0971fc98.js",
--          "hash": "23d5ef27c3b5c976",
--          "publicPath": "/bn/assets/js/0e384e19.0971fc98.js"
-+          "file": "assets/js/0e384e19.2897022a.js",
-+          "hash": "c9f5b2c7320acd74",
-+          "publicPath": "/bn/assets/js/0e384e19.2897022a.js"
-         }
-       ]
-     }
-diff --git a/apps/docs/.docusaurus/client-modules.js b/apps/docs/.docusaurus/client-modules.js
-index c81e8e69f..686f35bec 100644
---- a/apps/docs/.docusaurus/client-modules.js
-+++ b/apps/docs/.docusaurus/client-modules.js
-@@ -1,6 +1,6 @@
- export default [
-   require("C:\\Users\\n\\supremeai\\supremeai_2.0\\node_modules\\.pnpm\\infima@0.2.0-alpha.45\\node_modules\\infima\\dist\\css\\default\\default.css"),
--  require("C:\\Users\\n\\supremeai\\supremeai_2.0\\node_modules\\.pnpm\\@docusaurus+theme-classic@3.10.1_@types+react@19.2.17_react-dom@18.3.1_react@18.3.1__react@18.3.1_typescript@5.2.2\\node_modules\\@docusaurus\\theme-classic\\lib\\prism-include-languages"),
--  require("C:\\Users\\n\\supremeai\\supremeai_2.0\\node_modules\\.pnpm\\@docusaurus+theme-classic@3.10.1_@types+react@19.2.17_react-dom@18.3.1_react@18.3.1__react@18.3.1_typescript@5.2.2\\node_modules\\@docusaurus\\theme-classic\\lib\\nprogress"),
-+  require("C:\\Users\\n\\supremeai\\supremeai_2.0\\node_modules\\.pnpm\\@docusaurus+theme-classic@3.10.1_@types+react@19.2.17_lightningcss@1.32.0_react-dom@19.2.7_re_q2zqmppt7h2b5pw2bwblpigs3u\\node_modules\\@docusaurus\\theme-classic\\lib\\prism-include-languages"),
-+  require("C:\\Users\\n\\supremeai\\supremeai_2.0\\node_modules\\.pnpm\\@docusaurus+theme-classic@3.10.1_@types+react@19.2.17_lightningcss@1.32.0_react-dom@19.2.7_re_q2zqmppt7h2b5pw2bwblpigs3u\\node_modules\\@docusaurus\\theme-classic\\lib\\nprogress"),
-   require("C:\\Users\\n\\supremeai\\supremeai_2.0\\apps\\docs\\src\\css\\custom.css"),
- ];
-diff --git a/apps/docs/docusaurus.config.ts b/apps/docs/docusaurus.config.ts
-index b7fd9cd46..84e5a3344 100644
---- a/apps/docs/docusaurus.config.ts
-+++ b/apps/docs/docusaurus.config.ts
-@@ -13,8 +13,12 @@ const config: Config = {
-   organizationName: 'paykaribazaronline',
-   projectName: 'supremeai',
- 
--  onBrokenLinks: 'warn',
--  onBrokenMarkdownLinks: 'warn',
-+  onBrokenLinks: 'ignore',
-+  markdown: {
-+    hooks: {
-+      onBrokenMarkdownLinks: 'ignore',
-+    },
-+  },
- 
-   i18n: {
-     defaultLocale: 'en',
-diff --git a/apps/studio-client/package.json b/apps/studio-client/package.json
-index 577b1402c..7ba1e5a30 100644
---- a/apps/studio-client/package.json
-+++ b/apps/studio-client/package.json
-@@ -20,6 +20,7 @@
-   },
-   "dependencies": {
-     "@dataconnect/generated": "file:src/dataconnect-generated",
-+    "@supremeai/ui-components": "workspace:*",
-     "@monaco-editor/react": "^4.7.0",
-     "@tailwindcss/vite": "^4.2.4",
-     "@tanstack/react-query": "^5.101.0",
-diff --git a/apps/studio-client/src/components/dashboard/DashboardShell.tsx b/apps/studio-client/src/components/dashboard/DashboardShell.tsx
-index 38259d7bf..56c3f97d6 100644
---- a/apps/studio-client/src/components/dashboard/DashboardShell.tsx
-+++ b/apps/studio-client/src/components/dashboard/DashboardShell.tsx
-@@ -1,5 +1,6 @@
- // বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেল — বাম সাইডবার নেভিগেশন সহ ইউজার ও অ্যাডমিন উভয়ের জন্য মূল লেআউট
--import type { ReactNode } from 'react';
-+// হ্যাশ-ভিত্তিক রাউটিং, Sujon ব্যাকগ্রাউন্ড ইন্টিগ্রেশন ও পেজ রেন্ডারিং
-+import { type ReactNode, useMemo } from 'react';
- import {
-   LayoutList,
-   Boxes,
-@@ -7,14 +8,15 @@ import {
-   KeyRound,
-   BarChart3,
-   Settings,
--  ShieldCheck,
--  Plus,
-   Vault,
-   ListChecks,
-   Table2,
-   Cpu,
-+  Shield,
-+  Wifi,
-+  WifiOff,
- } from 'lucide-react';
--import { useHashRoute, type DashboardRoute } from './useHashRoute';
-+import { useHashRoute, type DashboardRoute, parseHash } from './useHashRoute';
- import { SessionsPage } from './SessionsPage';
- import { SessionDetailPage } from './SessionDetailPage';
- import { KnowledgePage } from './KnowledgePage';
-@@ -25,7 +27,7 @@ import { VaultPage } from './VaultPage';
- import { AutomationQueuePage } from './AutomationQueuePage';
- import { SiteActionsPage } from './SiteActionsPage';
- import { LlmGatewayPage } from './LlmGatewayPage';
--import { LiveSujonBackground } from '../LiveSujonBackground';
-+import { LiveSujonBackground, setSujonState, type SujonState } from '../LiveSujonBackground';
- 
- interface NavItem {
-   id: DashboardRoute;
-@@ -48,6 +50,7 @@ const NAV_ITEMS: NavItem[] = [
- const ADMIN_NAV_ITEMS: NavItem[] = [
-   { id: 'site-actions', label: 'Site Actions', icon: <Table2 size={15} /> },
-   { id: 'llm-gateway', label: 'LLM Gateway', icon: <Cpu size={15} /> },
-+  { id: 'admin', label: 'Admin Console', icon: <Shield size={15} /> },
- ];
- 
- interface DashboardShellProps {
-@@ -58,20 +61,39 @@ interface DashboardShellProps {
-   workspace: ReactNode;
- }
- 
--export function DashboardShell({ theme, toggleTheme, isServerOnline, workspace }: DashboardShellProps) {
-+export function DashboardShell(props: DashboardShellProps) {
-   const [route, navigate] = useHashRoute();
- 
-+  // বাংলা মন্তব্য: রাউটের ভিত্তিতে Sujon স্টেট সেট করা — টাস্ক এক্সিকিউশন আরম্ভ হলে processing, সেশন শেষে idle
-+  useMemo(() => {
-+    const sujonState: Record<DashboardRoute, SujonState> = {
-+      sessions: 'idle',
-+      session: 'processing',
-+      workspace: 'idle',
-+      vault: 'idle',
-+      automation: 'processing',
-+      'site-actions': 'idle',
-+      'llm-gateway': 'idle',
-+      knowledge: 'idle',
-+      secrets: 'idle',
-+      usage: 'idle',
-+      settings: 'idle',
-+      admin: 'idle',
-+    };
-+    setSujonState(sujonState[route.page] || 'idle');
-+  }, [route.page]);
-+
-+  const handleOpenSession = (id: string) => {
-+    navigate('session', id);
-+  };
-+
-+  // বাংলা মন্তব্য: হ্যাশ রাউটের ভিত্তিতে সংশ্লিষ্ট পেজ রেন্ডার করা হয়
-   const renderPage = () => {
-     switch (route.page) {
-       case 'session':
--        return (
--          <SessionDetailPage
--            sessionId={route.param || ''}
--            onBack={() => navigate('sessions')}
--          />
--        );
-+        return <SessionDetailPage sessionId={route.param || ''} />;
-       case 'workspace':
--        return workspace;
-+        return <>{props.workspace}</>;
-       case 'vault':
-         return <VaultPage />;
-       case 'automation':
-@@ -87,95 +109,91 @@ export function DashboardShell({ theme, toggleTheme, isServerOnline, workspace }
-       case 'usage':
-         return <UsagePage />;
-       case 'settings':
--        return <SettingsPage theme={theme} toggleTheme={toggleTheme} />;
-+        return <SettingsPage />;
-+      case 'admin':
-+        // বাংলা মন্তব্য: অ্যাডমিন কনসোলের জন্য #/admin রুট
-+        return <div className="p-6 text-slate-400 text-xs">Admin console (use /admin subdomain)</div>;
-       case 'sessions':
-       default:
--        return <SessionsPage onOpenSession={(id) => navigate('session', id)} />;
-+        return <SessionsPage onOpenSession={handleOpenSession} />;
-     }
-   };
- 
--  const activeNav = route.page === 'session' ? 'sessions' : route.page;
-+  const navItems = [...NAV_ITEMS, ...ADMIN_NAV_ITEMS];
- 
-   return (
-     <div className="relative min-h-screen flex bg-[#0b0f19] text-white">
--      {/* বাংলা মন্তব্য: Sujon লাইভ AI-কোর অ্যাম্বিয়েন্ট ব্যাকগ্রাউন্ড — Automation স্টেট অনুযায়ী বদলায় */}
-+      {/* বাংলা মন্তব্য: Sujon অ্যাম্বিয়েন্ট ব্যাকগ্রাউন্ড */}
-       <LiveSujonBackground />
-+
-+      {/* বাংলা মন্তব্য: বাম প্যানেল ব্যাকগ্রাউন্ড গ্রেডিয়েন্ট */}
-+      <div className="absolute inset-0 -z-10 bg-gradient-to-b from-[#00111a] to-[#061025]" />
-+
-+      {/* সাইডবার */}
-       <aside
-         data-testid="dashboard-sidebar"
-         className="relative z-10 w-56 shrink-0 border-r border-white/[0.06] bg-[#080b13] flex flex-col"
-       >
-+        {/* হেডার */}
-         <div className="flex items-center gap-2 px-4 py-4 border-b border-white/[0.06]">
-           <span className="text-blue-400 text-lg">▲</span>
-           <span className="text-sm font-semibold tracking-wide">SupremeAI</span>
-         </div>
- 
--        <button
--          data-testid="new-session-nav"
--          onClick={() => navigate('sessions')}
--          className="mx-3 mt-3 mb-2 flex items-center justify-center gap-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium py-2 transition-colors"
--        >
--          <Plus size={13} />
--          New Session
--        </button>
--
--        <nav className="flex-1 px-2 py-1 flex flex-col gap-0.5">
--          {NAV_ITEMS.map((item) => (
--            <button
--              key={item.id}
--              data-testid={`nav-${item.id}`}
--              onClick={() => navigate(item.id)}
--              className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs transition-colors ${
--                activeNav === item.id
--                  ? 'bg-white/[0.08] text-white'
--                  : 'text-slate-400 hover:text-white hover:bg-white/[0.04]'
--              }`}
--            >
--              {item.icon}
--              {item.label}
--            </button>
--          ))}
--
--          {/* বাংলা মন্তব্য: সুপার-অ্যাডমিন কন্ট্রোল সেকশন */}
--          <p className="px-3 pt-3 pb-1 text-[10px] uppercase tracking-wider text-slate-600">Admin</p>
--          {ADMIN_NAV_ITEMS.map((item) => (
--            <button
--              key={item.id}
--              data-testid={`nav-${item.id}`}
--              onClick={() => navigate(item.id)}
--              className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs transition-colors ${
--                activeNav === item.id
--                  ? 'bg-white/[0.08] text-white'
--                  : 'text-slate-400 hover:text-white hover:bg-white/[0.04]'
--              }`}
--            >
--              {item.icon}
--              {item.label}
--            </button>
--          ))}
--
--          {/* বাংলা মন্তব্য: অ্যাডমিন কন্সোল আলাদা রুটে (/admin) — সেখানে TOTP লগইনসহ সম্পূর্ণ অ্যাডমিন ফিচার আছে */}
--          <a
--            data-testid="nav-admin"
--            href="/admin"
--            className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs text-slate-400 hover:text-white hover:bg-white/[0.04] transition-colors"
--          >
--            <ShieldCheck size={15} />
--            Admin Console
--          </a>
-+        {/* সাইডবার নেভিগেশন লিংক */}
-+        <nav className="flex-1 overflow-y-auto px-2 py-3 space-y-0.5">
-+          {navItems.map((item) => {
-+            const isActive = route.page === item.id;
-+            return (
-+              <button
-+                key={item.id}
-+                data-testid={`nav-${item.id}`}
-+                onClick={() => navigate(item.id)}
-+                className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-medium transition-colors text-left ${
-+                  isActive
-+                    ? 'bg-blue-600/20 text-blue-300 border border-blue-500/20'
-+                    : 'text-slate-400 hover:text-slate-200 hover:bg-white/[0.04]'
-+                }`}
-+              >
-+                {item.icon}
-+                {item.label}
-+              </button>
-+            );
-+          })}
-         </nav>
- 
--        <div className="px-4 py-3 border-t border-white/[0.06] flex items-center justify-between">
--          <span
-+        {/* স্ট্যাটাস ও থিম */}
-+        <div className="px-3 py-3 border-t border-white/[0.06] space-y-2">
-+          <div
-             data-testid="sidebar-server-status"
--            className={`text-[10px] font-medium ${isServerOnline ? 'text-emerald-400' : 'text-rose-400'}`}
-+            className="flex items-center gap-2 text-[11px]"
-           >
--            ● {isServerOnline ? 'Online' : 'Offline'}
--          </span>
--          <span className="text-[10px] text-slate-500">Free plan</span>
-+            {props.isServerOnline ? (
-+              <>
-+                <Wifi size={11} className="text-emerald-400" />
-+                <span className="text-emerald-400 font-medium">Online</span>
-+              </>
-+            ) : (
-+              <>
-+                <WifiOff size={11} className="text-rose-400" />
-+                <span className="text-rose-400 font-medium">Offline</span>
-+              </>
-+            )}
-+          </div>
-+          <button
-+            onClick={props.toggleTheme}
-+            className="w-full flex items-center gap-2 px-3 py-1.5 rounded-lg text-[11px] text-slate-500 hover:text-slate-300 hover:bg-white/[0.04] transition-colors"
-+          >
-+            <Shield size={11} />
-+            {props.theme === 'dark' ? 'Dark' : 'Light'} mode
-+          </button>
-         </div>
-       </aside>
- 
--      <main className="relative z-10 flex-1 min-w-0 overflow-y-auto">{renderPage()}</main>
-+      {/* মূল কন্টেন্ট এলাকা */}
-+      <main className="relative z-10 flex-1 min-w-0 overflow-y-auto">
-+        {renderPage()}
-+      </main>
-     </div>
-   );
--}
-+}
-\ No newline at end of file
-diff --git a/apps/studio-client/src/main.tsx b/apps/studio-client/src/main.tsx
-index 10334edfe..675bc81f3 100644
---- a/apps/studio-client/src/main.tsx
-+++ b/apps/studio-client/src/main.tsx
-@@ -5,11 +5,15 @@ import './index.css'
- import { App } from './App.tsx'
- 
- import { ThemeProvider } from './contexts/ThemeContext'
-+// Shared providers (react-query, monaco defaults)
-+import { SharedProviders } from '@supremeai/ui-components'
- 
- createRoot(document.getElementById('root')!).render(
-   <StrictMode>
-     <ThemeProvider>
--      <App />
-+      <SharedProviders>
-+        <App />
-+      </SharedProviders>
-     </ThemeProvider>
-   </StrictMode>,
- )
-diff --git a/backend/coverage.json b/backend/coverage.json
-index 8526233d0..a9ace05aa 100644
---- a/backend/coverage.json
-+++ b/backend/coverage.json
-@@ -1 +1 @@
--{"meta": {"format": 3, "version": "7.14.1", "timestamp": "2026-07-04T11:51:26.553583", "branch_coverage": true, "show_contexts": false}, "files": {"core\\__init__.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "executed_branches": [], "missing_branches": [], "functions": {"": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\config.py": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 54, 56, 64, 66, 67, 68, 70, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 142, 143, 145, 147, 148, 149, 151, 156, 158, 159, 160, 162, 167, 169, 170, 171, 172, 173, 174, 178, 181, 182, 183, 184, 185, 187, 189, 190, 191, 192, 194, 202, 204, 223, 225], "summary": {"covered_lines": 115, "num_statements": 166, "percent_covered": 59.345794392523366, "percent_covered_display": "59", "missing_lines": 51, "excluded_lines": 0, "percent_statements_covered": 69.27710843373494, "percent_statements_covered_display": "69", "num_branches": 48, "num_partial_branches": 12, "covered_branches": 12, "missing_branches": 36, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [17, 18, 57, 58, 59, 60, 61, 62, 63, 65, 69, 144, 152, 153, 154, 155, 163, 164, 165, 166, 175, 179, 186, 195, 196, 197, 198, 199, 200, 201, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 226, 227, 229, 230, 231, 232, 233], "excluded_lines": [], "executed_branches": [[16, 21], [56, 64], [64, 66], [68, 70], [143, 145], [151, 156], [162, 167], [173, 174], [174, 178], [185, 187], [194, 202], [225, -1]], "missing_branches": [[16, 17], [56, 57], [58, 59], [58, 60], [64, 65], [68, 69], [143, 144], [151, 152], [153, 154], [153, 155], [162, 163], [164, 165], [164, 166], [173, 179], [174, 175], [185, 186], [194, 195], [196, 197], [196, 198], [205, -204], [205, 206], [207, 208], [207, 209], [209, 210], [209, 211], [211, 212], [211, 213], [213, 214], [213, 215], [215, 216], [215, 217], [217, -204], [217, 218], [225, 226], [229, -1], [229, 230]], "functions": {"Settings.sanitize_cors_origins": {"executed_lines": [54, 56, 64, 66, 67, 68, 70], "summary": {"covered_lines": 7, "num_statements": 16, "percent_covered": 41.666666666666664, "percent_covered_display": "42", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 43.75, "percent_statements_covered_display": "44", "num_branches": 8, "num_partial_branches": 3, "covered_branches": 3, "missing_branches": 5, "percent_branches_covered": 37.5, "percent_branches_covered_display": "38"}, "missing_lines": [57, 58, 59, 60, 61, 62, 63, 65, 69], "excluded_lines": [], "start_line": 53, "executed_branches": [[56, 64], [64, 66], [68, 70]], "missing_branches": [[56, 57], [58, 59], [58, 60], [64, 65], [68, 69]]}, "Settings.validate_env": {"executed_lines": [142, 143, 145], "summary": {"covered_lines": 3, "num_statements": 4, "percent_covered": 66.66666666666667, "percent_covered_display": "67", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 75.0, "percent_statements_covered_display": "75", "num_branches": 2, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 1, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [144], "excluded_lines": [], "start_line": 141, "executed_branches": [[143, 145]], "missing_branches": [[143, 144]]}, "Settings.parse_admin_emails": {"executed_lines": [151, 156], "summary": {"covered_lines": 2, "num_statements": 6, "percent_covered": 30.0, "percent_covered_display": "30", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 33.333333333333336, "percent_statements_covered_display": "33", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 3, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [152, 153, 154, 155], "excluded_lines": [], "start_line": 149, "executed_branches": [[151, 156]], "missing_branches": [[151, 152], [153, 154], [153, 155]]}, "Settings.parse_allowed_hosts": {"executed_lines": [162, 167], "summary": {"covered_lines": 2, "num_statements": 6, "percent_covered": 30.0, "percent_covered_display": "30", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 33.333333333333336, "percent_statements_covered_display": "33", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 3, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [163, 164, 165, 166], "excluded_lines": [], "start_line": 160, "executed_branches": [[162, 167]], "missing_branches": [[162, 163], [164, 165], [164, 166]]}, "Settings.set_test_secret": {"executed_lines": [172, 173, 174, 178], "summary": {"covered_lines": 4, "num_statements": 6, "percent_covered": 60.0, "percent_covered_display": "60", "missing_lines": 2, "excluded_lines": 0, "percent_statements_covered": 66.66666666666667, "percent_statements_covered_display": "67", "num_branches": 4, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 2, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [175, 179], "excluded_lines": [], "start_line": 171, "executed_branches": [[173, 174], [174, 178]], "missing_branches": [[173, 179], [174, 175]]}, "Settings.debug_must_be_false_in_production": {"executed_lines": [184, 185, 187], "summary": {"covered_lines": 3, "num_statements": 4, "percent_covered": 66.66666666666667, "percent_covered_display": "67", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 75.0, "percent_statements_covered_display": "75", "num_branches": 2, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 1, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [186], "excluded_lines": [], "start_line": 183, "executed_branches": [[185, 187]], "missing_branches": [[185, 186]]}, "Settings.parse_cors_origins": {"executed_lines": [192, 194, 202], "summary": {"covered_lines": 3, "num_statements": 10, "percent_covered": 28.571428571428573, "percent_covered_display": "29", "missing_lines": 7, "excluded_lines": 0, "percent_statements_covered": 30.0, "percent_statements_covered_display": "30", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 3, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [195, 196, 197, 198, 199, 200, 201], "excluded_lines": [], "start_line": 191, "executed_branches": [[194, 202]], "missing_branches": [[194, 195], [196, 197], [196, 198]]}, "Settings.validate_config": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 14, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 14, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 14, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 14, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218], "excluded_lines": [], "start_line": 204, "executed_branches": [], "missing_branches": [[205, -204], [205, 206], [207, 208], [207, 209], [209, 210], [209, 211], [211, 212], [211, 213], [213, 214], [213, 215], [215, 216], [215, 217], [217, -204], [217, 218]]}, "": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 147, 148, 149, 158, 159, 160, 169, 170, 171, 181, 182, 183, 189, 190, 191, 204, 223, 225], "summary": {"covered_lines": 91, "num_statements": 100, "percent_covered": 87.73584905660377, "percent_covered_display": "88", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 91.0, "percent_statements_covered_display": "91", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [17, 18, 226, 227, 229, 230, 231, 232, 233], "excluded_lines": [], "start_line": 1, "executed_branches": [[16, 21], [225, -1]], "missing_branches": [[16, 17], [225, 226], [229, -1], [229, 230]]}}, "classes": {"Settings": {"executed_lines": [54, 56, 64, 66, 67, 68, 70, 142, 143, 145, 151, 156, 162, 167, 172, 173, 174, 178, 184, 185, 187, 192, 194, 202], "summary": {"covered_lines": 24, "num_statements": 66, "percent_covered": 31.48148148148148, "percent_covered_display": "31", "missing_lines": 42, "excluded_lines": 0, "percent_statements_covered": 36.36363636363637, "percent_statements_covered_display": "36", "num_branches": 42, "num_partial_branches": 10, "covered_branches": 10, "missing_branches": 32, "percent_branches_covered": 23.80952380952381, "percent_branches_covered_display": "24"}, "missing_lines": [57, 58, 59, 60, 61, 62, 63, 65, 69, 144, 152, 153, 154, 155, 163, 164, 165, 166, 175, 179, 186, 195, 196, 197, 198, 199, 200, 201, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218], "excluded_lines": [], "start_line": 21, "executed_branches": [[56, 64], [64, 66], [68, 70], [143, 145], [151, 156], [162, 167], [173, 174], [174, 178], [185, 187], [194, 202]], "missing_branches": [[56, 57], [58, 59], [58, 60], [64, 65], [68, 69], [143, 144], [151, 152], [153, 154], [153, 155], [162, 163], [164, 165], [164, 166], [173, 179], [174, 175], [185, 186], [194, 195], [196, 197], [196, 198], [205, -204], [205, 206], [207, 208], [207, 209], [209, 210], [209, 211], [211, 212], [211, 213], [213, 214], [213, 215], [215, 216], [215, 217], [217, -204], [217, 218]]}, "": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 147, 148, 149, 158, 159, 160, 169, 170, 171, 181, 182, 183, 189, 190, 191, 204, 223, 225], "summary": {"covered_lines": 91, "num_statements": 100, "percent_covered": 87.73584905660377, "percent_covered_display": "88", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 91.0, "percent_statements_covered_display": "91", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [17, 18, 226, 227, 229, 230, 231, 232, 233], "excluded_lines": [], "start_line": 1, "executed_branches": [[16, 21], [225, -1]], "missing_branches": [[16, 17], [225, 226], [229, -1], [229, 230]]}}}, "core\\llm_gateway.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 107, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 107, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 38, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 38, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 20, 21, 22, 25, 26, 28, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 43, 45, 48, 56, 57, 58, 59, 61, 63, 64, 65, 66, 67, 68, 70, 72, 76, 77, 79, 80, 81, 86, 87, 89, 103, 106, 107, 110, 111, 112, 113, 114, 116, 117, 118, 119, 123, 124, 125, 126, 134, 135, 138, 139, 140, 141, 142, 143, 146, 147, 149, 151, 152, 155, 156, 157, 158, 159, 165, 171, 172, 173, 174, 176, 178, 181, 182, 183, 184, 185, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201, 204], "excluded_lines": [], "executed_branches": [], "missing_branches": [[36, 37], [36, 39], [56, -45], [56, 57], [57, 56], [57, 58], [106, 107], [106, 110], [111, 112], [111, 113], [113, 114], [113, 116], [116, 117], [116, 118], [118, 119], [118, 123], [123, 124], [123, 134], [125, 126], [125, 134], [139, 140], [139, 141], [141, 142], [141, 146], [142, 141], [142, 143], [146, 147], [146, 149], [151, 152], [151, 155], [156, 157], [156, 176], [182, 183], [182, 201], [191, 192], [191, 195], [193, 191], [193, 194]], "functions": {"LLMGateway.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [20, 21, 22, 25, 26, 28, 31, 32], "excluded_lines": [], "start_line": 19, "executed_branches": [], "missing_branches": []}, "LLMGateway._load_routing_policy": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [35, 36, 37, 38, 39, 40, 41, 43], "excluded_lines": [], "start_line": 34, "executed_branches": [], "missing_branches": [[36, 37], [36, 39]]}, "LLMGateway._inject_secrets": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 4, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 4, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [48, 56, 57, 58, 59], "excluded_lines": [], "start_line": 45, "executed_branches": [], "missing_branches": [[56, -45], [56, 57], [57, 56], [57, 58]]}, "LLMGateway._setup_callbacks": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 4, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [63, 79, 86, 87], "excluded_lines": [], "start_line": 61, "executed_branches": [], "missing_branches": []}, "LLMGateway._setup_callbacks.success_callback": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 9, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [64, 65, 66, 67, 68, 70, 72, 76, 77], "excluded_lines": [], "start_line": 63, "executed_branches": [], "missing_branches": []}, "LLMGateway._setup_callbacks.failure_callback": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 2, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 2, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [80, 81], "excluded_lines": [], "start_line": 79, "executed_branches": [], "missing_branches": []}, "LLMGateway.acompletion": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 40, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 40, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 26, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 26, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [103, 106, 107, 110, 111, 112, 113, 114, 116, 117, 118, 119, 123, 124, 125, 126, 134, 135, 138, 139, 140, 141, 142, 143, 146, 147, 149, 151, 152, 155, 156, 157, 158, 159, 165, 171, 172, 173, 174, 176], "excluded_lines": [], "start_line": 89, "executed_branches": [], "missing_branches": [[106, 107], [106, 110], [111, 112], [111, 113], [113, 114], [113, 116], [116, 117], [116, 118], [118, 119], [118, 123], [123, 124], [123, 134], [125, 126], [125, 134], [139, 140], [139, 141], [141, 142], [141, 146], [142, 141], [142, 143], [146, 147], [146, 149], [151, 152], [151, 155], [156, 157], [156, 176]]}, "LLMGateway._stream_completion": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 15, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 15, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 6, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 6, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [181, 182, 183, 184, 185, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201], "excluded_lines": [], "start_line": 178, "executed_branches": [], "missing_branches": [[182, 183], [182, 201], [191, 192], [191, 195], [193, 191], [193, 194]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 16, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 16, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 34, 45, 61, 89, 178, 204], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"LLMGateway": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 91, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 91, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 38, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 38, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [20, 21, 22, 25, 26, 28, 31, 32, 35, 36, 37, 38, 39, 40, 41, 43, 48, 56, 57, 58, 59, 63, 64, 65, 66, 67, 68, 70, 72, 76, 77, 79, 80, 81, 86, 87, 103, 106, 107, 110, 111, 112, 113, 114, 116, 117, 118, 119, 123, 124, 125, 126, 134, 135, 138, 139, 140, 141, 142, 143, 146, 147, 149, 151, 152, 155, 156, 157, 158, 159, 165, 171, 172, 173, 174, 176, 181, 182, 183, 184, 185, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201], "excluded_lines": [], "start_line": 18, "executed_branches": [], "missing_branches": [[36, 37], [36, 39], [56, -45], [56, 57], [57, 56], [57, 58], [106, 107], [106, 110], [111, 112], [111, 113], [113, 114], [113, 116], [116, 117], [116, 118], [118, 119], [118, 123], [123, 124], [123, 134], [125, 126], [125, 134], [139, 140], [139, 141], [141, 142], [141, 146], [142, 141], [142, 143], [146, 147], [146, 149], [151, 152], [151, 155], [156, 157], [156, 176], [182, 183], [182, 201], [191, 192], [191, 195], [193, 191], [193, 194]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 16, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 16, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 34, 45, 61, 89, 178, 204], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\swarm_orchestrator.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 19, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 19, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 17, 18, 19, 21, 22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "executed_branches": [], "missing_branches": [], "functions": {"SwarmOrchestrator.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 3, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 3, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19], "excluded_lines": [], "start_line": 16, "executed_branches": [], "missing_branches": []}, "SwarmOrchestrator.execute_task": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "start_line": 21, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 21], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"SwarmOrchestrator": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 11, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 11, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19, 22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "start_line": 12, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 21], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}}, "totals": {"covered_lines": 115, "num_statements": 292, "percent_covered": 33.597883597883595, "percent_covered_display": "34", "missing_lines": 177, "excluded_lines": 0, "percent_statements_covered": 39.38356164383562, "percent_statements_covered_display": "39", "num_branches": 86, "num_partial_branches": 12, "covered_branches": 12, "missing_branches": 74, "percent_branches_covered": 13.953488372093023, "percent_branches_covered_display": "14"}}
-\ No newline at end of file
-+{"meta": {"format": 3, "version": "7.14.1", "timestamp": "2026-07-04T11:52:38.069533", "branch_coverage": true, "show_contexts": false}, "files": {"core\\__init__.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "executed_branches": [], "missing_branches": [], "functions": {"": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 0, "percent_covered": 100.0, "percent_covered_display": "100", "missing_lines": 0, "excluded_lines": 0, "percent_statements_covered": 100.0, "percent_statements_covered_display": "100", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\config.py": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 54, 56, 64, 66, 67, 68, 70, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 142, 143, 145, 147, 148, 149, 151, 156, 158, 159, 160, 162, 167, 169, 170, 171, 172, 173, 174, 178, 181, 182, 183, 184, 185, 187, 189, 190, 191, 192, 194, 202, 204, 223, 225], "summary": {"covered_lines": 115, "num_statements": 166, "percent_covered": 59.345794392523366, "percent_covered_display": "59", "missing_lines": 51, "excluded_lines": 0, "percent_statements_covered": 69.27710843373494, "percent_statements_covered_display": "69", "num_branches": 48, "num_partial_branches": 12, "covered_branches": 12, "missing_branches": 36, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [17, 18, 57, 58, 59, 60, 61, 62, 63, 65, 69, 144, 152, 153, 154, 155, 163, 164, 165, 166, 175, 179, 186, 195, 196, 197, 198, 199, 200, 201, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 226, 227, 229, 230, 231, 232, 233], "excluded_lines": [], "executed_branches": [[16, 21], [56, 64], [64, 66], [68, 70], [143, 145], [151, 156], [162, 167], [173, 174], [174, 178], [185, 187], [194, 202], [225, -1]], "missing_branches": [[16, 17], [56, 57], [58, 59], [58, 60], [64, 65], [68, 69], [143, 144], [151, 152], [153, 154], [153, 155], [162, 163], [164, 165], [164, 166], [173, 179], [174, 175], [185, 186], [194, 195], [196, 197], [196, 198], [205, -204], [205, 206], [207, 208], [207, 209], [209, 210], [209, 211], [211, 212], [211, 213], [213, 214], [213, 215], [215, 216], [215, 217], [217, -204], [217, 218], [225, 226], [229, -1], [229, 230]], "functions": {"Settings.sanitize_cors_origins": {"executed_lines": [54, 56, 64, 66, 67, 68, 70], "summary": {"covered_lines": 7, "num_statements": 16, "percent_covered": 41.666666666666664, "percent_covered_display": "42", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 43.75, "percent_statements_covered_display": "44", "num_branches": 8, "num_partial_branches": 3, "covered_branches": 3, "missing_branches": 5, "percent_branches_covered": 37.5, "percent_branches_covered_display": "38"}, "missing_lines": [57, 58, 59, 60, 61, 62, 63, 65, 69], "excluded_lines": [], "start_line": 53, "executed_branches": [[56, 64], [64, 66], [68, 70]], "missing_branches": [[56, 57], [58, 59], [58, 60], [64, 65], [68, 69]]}, "Settings.validate_env": {"executed_lines": [142, 143, 145], "summary": {"covered_lines": 3, "num_statements": 4, "percent_covered": 66.66666666666667, "percent_covered_display": "67", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 75.0, "percent_statements_covered_display": "75", "num_branches": 2, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 1, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [144], "excluded_lines": [], "start_line": 141, "executed_branches": [[143, 145]], "missing_branches": [[143, 144]]}, "Settings.parse_admin_emails": {"executed_lines": [151, 156], "summary": {"covered_lines": 2, "num_statements": 6, "percent_covered": 30.0, "percent_covered_display": "30", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 33.333333333333336, "percent_statements_covered_display": "33", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 3, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [152, 153, 154, 155], "excluded_lines": [], "start_line": 149, "executed_branches": [[151, 156]], "missing_branches": [[151, 152], [153, 154], [153, 155]]}, "Settings.parse_allowed_hosts": {"executed_lines": [162, 167], "summary": {"covered_lines": 2, "num_statements": 6, "percent_covered": 30.0, "percent_covered_display": "30", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 33.333333333333336, "percent_statements_covered_display": "33", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 3, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [163, 164, 165, 166], "excluded_lines": [], "start_line": 160, "executed_branches": [[162, 167]], "missing_branches": [[162, 163], [164, 165], [164, 166]]}, "Settings.set_test_secret": {"executed_lines": [172, 173, 174, 178], "summary": {"covered_lines": 4, "num_statements": 6, "percent_covered": 60.0, "percent_covered_display": "60", "missing_lines": 2, "excluded_lines": 0, "percent_statements_covered": 66.66666666666667, "percent_statements_covered_display": "67", "num_branches": 4, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 2, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [175, 179], "excluded_lines": [], "start_line": 171, "executed_branches": [[173, 174], [174, 178]], "missing_branches": [[173, 179], [174, 175]]}, "Settings.debug_must_be_false_in_production": {"executed_lines": [184, 185, 187], "summary": {"covered_lines": 3, "num_statements": 4, "percent_covered": 66.66666666666667, "percent_covered_display": "67", "missing_lines": 1, "excluded_lines": 0, "percent_statements_covered": 75.0, "percent_statements_covered_display": "75", "num_branches": 2, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 1, "percent_branches_covered": 50.0, "percent_branches_covered_display": "50"}, "missing_lines": [186], "excluded_lines": [], "start_line": 183, "executed_branches": [[185, 187]], "missing_branches": [[185, 186]]}, "Settings.parse_cors_origins": {"executed_lines": [192, 194, 202], "summary": {"covered_lines": 3, "num_statements": 10, "percent_covered": 28.571428571428573, "percent_covered_display": "29", "missing_lines": 7, "excluded_lines": 0, "percent_statements_covered": 30.0, "percent_statements_covered_display": "30", "num_branches": 4, "num_partial_branches": 1, "covered_branches": 1, "missing_branches": 3, "percent_branches_covered": 25.0, "percent_branches_covered_display": "25"}, "missing_lines": [195, 196, 197, 198, 199, 200, 201], "excluded_lines": [], "start_line": 191, "executed_branches": [[194, 202]], "missing_branches": [[194, 195], [196, 197], [196, 198]]}, "Settings.validate_config": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 14, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 14, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 14, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 14, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218], "excluded_lines": [], "start_line": 204, "executed_branches": [], "missing_branches": [[205, -204], [205, 206], [207, 208], [207, 209], [209, 210], [209, 211], [211, 212], [211, 213], [213, 214], [213, 215], [215, 216], [215, 217], [217, -204], [217, 218]]}, "": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 147, 148, 149, 158, 159, 160, 169, 170, 171, 181, 182, 183, 189, 190, 191, 204, 223, 225], "summary": {"covered_lines": 91, "num_statements": 100, "percent_covered": 87.73584905660377, "percent_covered_display": "88", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 91.0, "percent_statements_covered_display": "91", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [17, 18, 226, 227, 229, 230, 231, 232, 233], "excluded_lines": [], "start_line": 1, "executed_branches": [[16, 21], [225, -1]], "missing_branches": [[16, 17], [225, 226], [229, -1], [229, 230]]}}, "classes": {"Settings": {"executed_lines": [54, 56, 64, 66, 67, 68, 70, 142, 143, 145, 151, 156, 162, 167, 172, 173, 174, 178, 184, 185, 187, 192, 194, 202], "summary": {"covered_lines": 24, "num_statements": 66, "percent_covered": 31.48148148148148, "percent_covered_display": "31", "missing_lines": 42, "excluded_lines": 0, "percent_statements_covered": 36.36363636363637, "percent_statements_covered_display": "36", "num_branches": 42, "num_partial_branches": 10, "covered_branches": 10, "missing_branches": 32, "percent_branches_covered": 23.80952380952381, "percent_branches_covered_display": "24"}, "missing_lines": [57, 58, 59, 60, 61, 62, 63, 65, 69, 144, 152, 153, 154, 155, 163, 164, 165, 166, 175, 179, 186, 195, 196, 197, 198, 199, 200, 201, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218], "excluded_lines": [], "start_line": 21, "executed_branches": [[56, 64], [64, 66], [68, 70], [143, 145], [151, 156], [162, 167], [173, 174], [174, 178], [185, 187], [194, 202]], "missing_branches": [[56, 57], [58, 59], [58, 60], [64, 65], [68, 69], [143, 144], [151, 152], [153, 154], [153, 155], [162, 163], [164, 165], [164, 166], [173, 179], [174, 175], [185, 186], [194, 195], [196, 197], [196, 198], [205, -204], [205, 206], [207, 208], [207, 209], [209, 210], [209, 211], [211, 212], [211, 213], [213, 214], [213, 215], [215, 216], [215, 217], [217, -204], [217, 218]]}, "": {"executed_lines": [1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 13, 16, 21, 22, 27, 28, 30, 31, 32, 33, 34, 35, 37, 38, 39, 41, 51, 52, 53, 73, 78, 83, 89, 93, 95, 96, 97, 98, 99, 100, 101, 102, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 123, 124, 125, 126, 128, 129, 131, 132, 133, 134, 135, 139, 140, 141, 147, 148, 149, 158, 159, 160, 169, 170, 171, 181, 182, 183, 189, 190, 191, 204, 223, 225], "summary": {"covered_lines": 91, "num_statements": 100, "percent_covered": 87.73584905660377, "percent_covered_display": "88", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 91.0, "percent_statements_covered_display": "91", "num_branches": 6, "num_partial_branches": 2, "covered_branches": 2, "missing_branches": 4, "percent_branches_covered": 33.333333333333336, "percent_branches_covered_display": "33"}, "missing_lines": [17, 18, 226, 227, 229, 230, 231, 232, 233], "excluded_lines": [], "start_line": 1, "executed_branches": [[16, 21], [225, -1]], "missing_branches": [[16, 17], [225, 226], [229, -1], [229, 230]]}}}, "core\\llm_gateway.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 107, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 107, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 38, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 38, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 20, 21, 22, 25, 26, 28, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 43, 45, 48, 56, 57, 58, 59, 61, 63, 64, 65, 66, 67, 68, 70, 72, 76, 77, 79, 80, 81, 86, 87, 89, 103, 106, 107, 110, 111, 112, 113, 114, 116, 117, 118, 119, 123, 124, 125, 126, 134, 135, 138, 139, 140, 141, 142, 143, 146, 147, 149, 151, 152, 155, 156, 157, 158, 159, 165, 171, 172, 173, 174, 176, 178, 181, 182, 183, 184, 185, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201, 204], "excluded_lines": [], "executed_branches": [], "missing_branches": [[36, 37], [36, 39], [56, -45], [56, 57], [57, 56], [57, 58], [106, 107], [106, 110], [111, 112], [111, 113], [113, 114], [113, 116], [116, 117], [116, 118], [118, 119], [118, 123], [123, 124], [123, 134], [125, 126], [125, 134], [139, 140], [139, 141], [141, 142], [141, 146], [142, 141], [142, 143], [146, 147], [146, 149], [151, 152], [151, 155], [156, 157], [156, 176], [182, 183], [182, 201], [191, 192], [191, 195], [193, 191], [193, 194]], "functions": {"LLMGateway.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [20, 21, 22, 25, 26, 28, 31, 32], "excluded_lines": [], "start_line": 19, "executed_branches": [], "missing_branches": []}, "LLMGateway._load_routing_policy": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 2, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 2, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [35, 36, 37, 38, 39, 40, 41, 43], "excluded_lines": [], "start_line": 34, "executed_branches": [], "missing_branches": [[36, 37], [36, 39]]}, "LLMGateway._inject_secrets": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 5, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 5, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 4, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 4, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [48, 56, 57, 58, 59], "excluded_lines": [], "start_line": 45, "executed_branches": [], "missing_branches": [[56, -45], [56, 57], [57, 56], [57, 58]]}, "LLMGateway._setup_callbacks": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 4, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 4, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [63, 79, 86, 87], "excluded_lines": [], "start_line": 61, "executed_branches": [], "missing_branches": []}, "LLMGateway._setup_callbacks.success_callback": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 9, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 9, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [64, 65, 66, 67, 68, 70, 72, 76, 77], "excluded_lines": [], "start_line": 63, "executed_branches": [], "missing_branches": []}, "LLMGateway._setup_callbacks.failure_callback": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 2, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 2, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [80, 81], "excluded_lines": [], "start_line": 79, "executed_branches": [], "missing_branches": []}, "LLMGateway.acompletion": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 40, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 40, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 26, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 26, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [103, 106, 107, 110, 111, 112, 113, 114, 116, 117, 118, 119, 123, 124, 125, 126, 134, 135, 138, 139, 140, 141, 142, 143, 146, 147, 149, 151, 152, 155, 156, 157, 158, 159, 165, 171, 172, 173, 174, 176], "excluded_lines": [], "start_line": 89, "executed_branches": [], "missing_branches": [[106, 107], [106, 110], [111, 112], [111, 113], [113, 114], [113, 116], [116, 117], [116, 118], [118, 119], [118, 123], [123, 124], [123, 134], [125, 126], [125, 134], [139, 140], [139, 141], [141, 142], [141, 146], [142, 141], [142, 143], [146, 147], [146, 149], [151, 152], [151, 155], [156, 157], [156, 176]]}, "LLMGateway._stream_completion": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 15, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 15, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 6, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 6, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [181, 182, 183, 184, 185, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201], "excluded_lines": [], "start_line": 178, "executed_branches": [], "missing_branches": [[182, 183], [182, 201], [191, 192], [191, 195], [193, 191], [193, 194]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 16, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 16, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 34, 45, 61, 89, 178, 204], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"LLMGateway": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 91, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 91, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 38, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 38, "percent_branches_covered": 0.0, "percent_branches_covered_display": "0"}, "missing_lines": [20, 21, 22, 25, 26, 28, 31, 32, 35, 36, 37, 38, 39, 40, 41, 43, 48, 56, 57, 58, 59, 63, 64, 65, 66, 67, 68, 70, 72, 76, 77, 79, 80, 81, 86, 87, 103, 106, 107, 110, 111, 112, 113, 114, 116, 117, 118, 119, 123, 124, 125, 126, 134, 135, 138, 139, 140, 141, 142, 143, 146, 147, 149, 151, 152, 155, 156, 157, 158, 159, 165, 171, 172, 173, 174, 176, 181, 182, 183, 184, 185, 191, 192, 193, 194, 195, 196, 197, 198, 199, 201], "excluded_lines": [], "start_line": 18, "executed_branches": [], "missing_branches": [[36, 37], [36, 39], [56, -45], [56, 57], [57, 56], [57, 58], [106, 107], [106, 110], [111, 112], [111, 113], [113, 114], [113, 116], [116, 117], [116, 118], [118, 119], [118, 123], [123, 124], [123, 134], [125, 126], [125, 134], [139, 140], [139, 141], [141, 142], [141, 146], [142, 141], [142, 143], [146, 147], [146, 149], [151, 152], [151, 155], [156, 157], [156, 176], [182, 183], [182, 201], [191, 192], [191, 195], [193, 191], [193, 194]]}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 16, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 16, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 5, 6, 7, 9, 10, 12, 16, 18, 19, 34, 45, 61, 89, 178, 204], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}, "core\\swarm_orchestrator.py": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 19, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 19, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 17, 18, 19, 21, 22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "executed_branches": [], "missing_branches": [], "functions": {"SwarmOrchestrator.__init__": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 3, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 3, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19], "excluded_lines": [], "start_line": 16, "executed_branches": [], "missing_branches": []}, "SwarmOrchestrator.execute_task": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "start_line": 21, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 21], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}, "classes": {"SwarmOrchestrator": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 11, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 11, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [17, 18, 19, 22, 23, 25, 28, 31, 34, 36, 37], "excluded_lines": [], "start_line": 12, "executed_branches": [], "missing_branches": []}, "": {"executed_lines": [], "summary": {"covered_lines": 0, "num_statements": 8, "percent_covered": 0.0, "percent_covered_display": "0", "missing_lines": 8, "excluded_lines": 0, "percent_statements_covered": 0.0, "percent_statements_covered_display": "0", "num_branches": 0, "num_partial_branches": 0, "covered_branches": 0, "missing_branches": 0, "percent_branches_covered": 100.0, "percent_branches_covered_display": "100"}, "missing_lines": [4, 6, 7, 8, 9, 12, 16, 21], "excluded_lines": [], "start_line": 1, "executed_branches": [], "missing_branches": []}}}}, "totals": {"covered_lines": 115, "num_statements": 292, "percent_covered": 33.597883597883595, "percent_covered_display": "34", "missing_lines": 177, "excluded_lines": 0, "percent_statements_covered": 39.38356164383562, "percent_statements_covered_display": "39", "num_branches": 86, "num_partial_branches": 12, "covered_branches": 12, "missing_branches": 74, "percent_branches_covered": 13.953488372093023, "percent_branches_covered_display": "14"}}
-\ No newline at end of file
-diff --git a/backend/tests/tools/test_viral_referral_engine.py b/backend/tests/tools/test_viral_referral_engine.py
-index 11cd60c0b..1674e212f 100644
---- a/backend/tests/tools/test_viral_referral_engine.py
-+++ b/backend/tests/tools/test_viral_referral_engine.py
-@@ -1,3 +1,4 @@
-+import asyncio
- import json
- import os
- import time
-@@ -114,9 +115,9 @@ class TestViralReferralEngine:
-             codes = engine.list_user_codes("user-456")
-         assert codes == []
- 
--    async def test_process_signup_invalid_code(self, engine, tmp_path):
-+    def test_process_signup_invalid_code(self, engine, tmp_path):
-         engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
--        result = engine.process_signup("new-user-123", "INVALID-CODE", {})
-+        result = asyncio.run(engine.process_signup("new-user-123", "INVALID-CODE", {}))
-         assert result["status"] == "skipped"
-         assert result["reason"] == "invalid_code"
- 
-@@ -124,7 +125,7 @@ class TestViralReferralEngine:
-         engine._local_store = lambda: os.path.join(str(tmp_path), "referrals.json")
-         gen = engine.generate_referral_code("referrer-1")
-         code = gen["code"]
--        result = engine.process_signup("new-user-123", code, {})
-+        result = asyncio.run(engine.process_signup("new-user-123", code, {}))
-         assert result["status"] == "success"
-         assert result["referrer_id"] == "referrer-1"
-         assert "reward_applied" in result
-@@ -134,7 +135,7 @@ class TestViralReferralEngine:
-         gen = engine.generate_referral_code("referrer-1")
-         code = gen["code"]
-         engine._load_local()["codes"][code]["expires_at"] = time.time() - 1
--        result = engine.process_signup("new-user-123", code, {})
-+        result = asyncio.run(engine.process_signup("new-user-123", code, {}))
-         assert result["status"] == "skipped"
-         assert result["reason"] == "expired_code"
- 
-@@ -144,7 +145,7 @@ class TestViralReferralEngine:
-         code = gen["code"]
-         meta = {"ip_address": "1.2.3.4", "device_fingerprint": "dev-abc"}
-         with patch.object(engine, "_is_fraudulent", return_value=True):
--            result = engine.process_signup("new-user-123", code, meta)
-+            result = asyncio.run(engine.process_signup("new-user-123", code, meta))
-         assert result["status"] == "skipped"
-         assert result["reason"] == "fraud_detected"
- 
-diff --git a/package.json b/package.json
-index b88a0aad1..dd65827b8 100644
---- a/package.json
-+++ b/package.json
-@@ -33,13 +33,12 @@
-     "miniflare": "^2.0.1"
-   },
-   "packageManager": "pnpm@9.0.0",
--  "pnpm": {
--    "overrides": {
--      "react": "^19.2.0",
--      "react-dom": "^19.2.0",
--      "typescript": "^5.4.5",
--      "vite": "7.3.5"
--    }
-+  "overrides": {
-+    "react": "^19.2.0",
-+    "react-dom": "^19.2.0",
-+    "typescript": "^5.4.5",
-+    "vite": "7.3.5",
-+    "firebase": "^12.15.0"
-   },
-   "engines": {
-     "node": ">=20.0.0",
-diff --git a/packages/ui-components/package.json b/packages/ui-components/package.json
-index 921694b9f..c9d905183 100644
---- a/packages/ui-components/package.json
-+++ b/packages/ui-components/package.json
-@@ -1,6 +1,7 @@
- {
-   "name": "@supremeai/ui-components",
--  "version": "1.0.0",
-+  "version": "0.1.0",
-+  "private": false,
-   "type": "module",
-   "main": "./src/index.ts",
-   "types": "./src/index.ts",
-@@ -12,20 +13,20 @@
-     "./package.json": "./package.json"
-   },
-   "peerDependencies": {
--    "react": "^18.0.0 || ^19.0.0",
--    "react-dom": "^18.0.0 || ^19.0.0"
-+    "react": "^18 || ^19",
-+    "react-dom": "^18 || ^19",
-+    "@tanstack/react-query": "^5.0.0",
-+    "@monaco-editor/react": "^4.0.0"
-   },
-   "peerDependenciesMeta": {
--    "react": {
--      "optional": false
--    },
--    "react-dom": {
--      "optional": false
--    }
-+    "react": { "optional": false },
-+    "react-dom": { "optional": false }
-   },
-   "devDependencies": {
-     "@types/react": "^19.0.0",
-     "@types/react-dom": "^19.0.0",
-     "typescript": "^5.4.0"
--  }
-+  },
-+  "files": ["src/**/*"],
-+  "license": "MIT"
- }
-diff --git a/packages/ui-components/src/components/DashboardShell.tsx b/packages/ui-components/src/components/DashboardShell.tsx
-new file mode 100644
-index 000000000..d1e0af186
---- /dev/null
-+++ b/packages/ui-components/src/components/DashboardShell.tsx
-@@ -0,0 +1,18 @@
-+import React from 'react';
-+import './styles.css';
-+import { LiveSujonBackground } from './LiveSujonBackground';
-+
-+export function DashboardShell({ children, isServerOnline = false }: any) {
-+  return (
-+    <div className="relative min-h-screen flex bg-[#0b0f19] text-white">
-+      <LiveSujonBackground />
-+      <aside className="relative z-10 w-56 shrink-0 border-r border-white/[0.06] bg-[#080b13] flex flex-col">
-+        <div className="flex items-center gap-2 px-4 py-4 border-b border-white/[0.06]">
-+          <span className="text-blue-400 text-lg">▲</span>
-+          <span className="text-sm font-semibold tracking-wide">SupremeAI</span>
-+        </div>
-+        <main className="relative z-10 flex-1 min-w-0 overflow-y-auto">{children}</main>
-+      </aside>
-+    </div>
-+  );
-+}
-diff --git a/packages/ui-components/src/components/LiveSujonBackground.tsx b/packages/ui-components/src/components/LiveSujonBackground.tsx
-new file mode 100644
-index 000000000..501dc840e
---- /dev/null
-+++ b/packages/ui-components/src/components/LiveSujonBackground.tsx
-@@ -0,0 +1,7 @@
-+import React from 'react';
-+
-+export function LiveSujonBackground() {
-+  return (
-+    <div aria-hidden className="absolute inset-0 -z-10 bg-gradient-to-b from-[#00111a] to-[#061025]" />
-+  );
-+}
-diff --git a/packages/ui-components/src/components/styles.css b/packages/ui-components/src/components/styles.css
-new file mode 100644
-index 000000000..8e7a2aff9
---- /dev/null
-+++ b/packages/ui-components/src/components/styles.css
-@@ -0,0 +1,2 @@
-+.dashboard-root { }
-+.live-sujon { }
-diff --git a/packages/ui-components/src/contexts/SharedProviders.tsx b/packages/ui-components/src/contexts/SharedProviders.tsx
-new file mode 100644
-index 000000000..515f8c126
---- /dev/null
-+++ b/packages/ui-components/src/contexts/SharedProviders.tsx
-@@ -0,0 +1,21 @@
-+import React from 'react';
-+import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
-+
-+const queryClient = new QueryClient({
-+  defaultOptions: {
-+    queries: {
-+      retry: 1,
-+      refetchOnWindowFocus: false,
-+    },
-+  },
-+});
-+
-+export const SharedProviders: React.FC<{children: React.ReactNode}> = ({ children }) => {
-+  return (
-+    <QueryClientProvider client={queryClient}>
-+      {children}
-+    </QueryClientProvider>
-+  );
-+};
-+
-+export default SharedProviders;
-diff --git a/packages/ui-components/src/index.ts b/packages/ui-components/src/index.ts
-index 047667120..5b74eb80f 100644
---- a/packages/ui-components/src/index.ts
-+++ b/packages/ui-components/src/index.ts
-@@ -1 +1,5 @@
-+export { DashboardShell } from './components/DashboardShell';
-+export { LiveSujonBackground } from './components/LiveSujonBackground';
-+export { SharedProviders } from './contexts/SharedProviders';
-+
- export { ChatBubble } from './ChatBubble';
-diff --git a/pnpm-lock.yaml b/pnpm-lock.yaml
-index 3228e3a3f..a791b90cf 100644
---- a/pnpm-lock.yaml
-+++ b/pnpm-lock.yaml
-@@ -9,6 +9,7 @@ overrides:
-   react-dom: ^19.2.0
-   typescript: ^5.4.5
-   vite: 7.3.5
-+  firebase: ^12.15.0
- 
- importers:
- 
-@@ -47,6 +48,9 @@ importers:
- 
-   apps/desktop/src-ui:
-     dependencies:
-+      '@supremeai/ui-components':
-+        specifier: workspace:*
-+        version: link:../../../packages/ui-components
-       '@tauri-apps/api':
-         specifier: ^1.5.0
-         version: 1.6.0
-@@ -135,10 +139,13 @@ importers:
-     dependencies:
-       '@dataconnect/generated':
-         specifier: file:src/dataconnect-generated
--        version: file:apps/studio-client/src/dataconnect-generated(@tanstack-query-firebase/react@2.1.1(@tanstack/react-query@5.101.0(react@19.2.7))(firebase@10.14.1))(firebase@10.14.1)
-+        version: file:apps/studio-client/src/dataconnect-generated(@tanstack-query-firebase/react@2.1.1(@tanstack/react-query@5.101.0(react@19.2.7))(firebase@12.15.0))(firebase@12.15.0)
-       '@monaco-editor/react':
-         specifier: ^4.7.0
-         version: 4.7.0(monaco-editor@0.55.1)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-+      '@supremeai/ui-components':
-+        specifier: workspace:*
-+        version: link:../../packages/ui-components
-       '@tailwindcss/vite':
-         specifier: ^4.2.4
-         version: 4.3.1(vite@7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3))
-@@ -146,8 +153,8 @@ importers:
-         specifier: ^5.101.0
-         version: 5.101.0(react@19.2.7)
-       firebase:
--        specifier: ^10.8.0
--        version: 10.14.1
-+        specifier: ^12.15.0
-+        version: 12.15.0
-       framer-motion:
-         specifier: ^12.42.0
-         version: 12.42.0(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-@@ -282,6 +289,12 @@ importers:
- 
-   packages/ui-components:
-     dependencies:
-+      '@monaco-editor/react':
-+        specifier: ^4.0.0
-+        version: 4.7.0(monaco-editor@0.55.1)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-+      '@tanstack/react-query':
-+        specifier: ^5.0.0
-+        version: 5.101.0(react@19.2.7)
-       react:
-         specifier: ^19.2.0
-         version: 19.2.7
-@@ -328,7 +341,7 @@ importers:
-         version: 8.57.1
-       openai:
-         specifier: ^4.0.0
--        version: 4.104.0(ws@8.21.0)(zod@4.4.3)
-+        version: 4.104.0(ws@8.21.0)(zod@3.25.76)
-       typescript:
-         specifier: ^5.4.5
-         version: 5.9.3
-@@ -1336,13 +1349,13 @@ packages:
-     engines: {node: ' >=18.0'}
-     peerDependencies:
-       '@tanstack-query-firebase/react': ^2.0.0
--      firebase: ^12.11.0
-+      firebase: ^12.15.0
- 
-   '@dataconnect/generated@file:tools/vscode-extension/src/dataconnect-generated':
-     resolution: {directory: tools/vscode-extension/src/dataconnect-generated, type: directory}
-     engines: {node: ' >=18.0'}
-     peerDependencies:
--      firebase: ^12.11.0
-+      firebase: ^12.15.0
- 
-   '@develar/schema-utils@2.6.5':
-     resolution: {integrity: sha512-0cp4PsWQ/9avqTVMCtZ+GirikIA36ikvjtHweU4/j8yLtgObI0+JUPhYFScgwlteveGB1rt3Cm8UhN04XayDig==}
-@@ -1966,19 +1979,11 @@ packages:
-       '@firebase/app': 0.x
-       '@firebase/app-types': 0.x
- 
--  '@firebase/analytics-compat@0.2.14':
--    resolution: {integrity: sha512-unRVY6SvRqfNFIAA/kwl4vK+lvQAL2HVcgu9zTrUtTyYDmtIt/lOuHJynBMYEgLnKm39YKBDhtqdapP2e++ASw==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
-   '@firebase/analytics-compat@0.2.28':
-     resolution: {integrity: sha512-lIAlqUUbBu93FJMlQfslryQtBwwzdzvp23ePC6FNgymXk6Ook5v4Uvc0vdutvoIeqmyA3LfP0ZeRFK8+11kOOQ==}
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/analytics-types@0.8.2':
--    resolution: {integrity: sha512-EnzNNLh+9/sJsimsA/FGqzakmrAUKLeJvjRHlg8df1f97NLUlFidk9600y0ZgWOp3CAxn6Hjtk+08tixlUOWyw==}
--
-   '@firebase/analytics-types@0.8.4':
-     resolution: {integrity: sha512-zQ+XTgkwH6CY/eUSHJRP7e4LxM30RCxlCmob5sy2axs25GE3Ny0XdgpDscMTHHQIGqWkxPXad4w2Mw9sCgT8zQ==}
- 
-@@ -1987,31 +1992,15 @@ packages:
-     peerDependencies:
-       '@firebase/app': 0.x
- 
--  '@firebase/analytics@0.10.8':
--    resolution: {integrity: sha512-CVnHcS4iRJPqtIDc411+UmFldk0ShSK3OB+D0bKD8Ck5Vro6dbK5+APZpkuWpbfdL359DIQUnAaMLE+zs/PVyA==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
--  '@firebase/app-check-compat@0.3.15':
--    resolution: {integrity: sha512-zFIvIFFNqDXpOT2huorz9cwf56VT3oJYRFjSFYdSbGYEJYEaXjLJbfC79lx/zjx4Fh+yuN8pry3TtvwaevrGbg==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
-   '@firebase/app-check-compat@0.4.5':
-     resolution: {integrity: sha512-JI17mVcZs34zO6ZeSCrw4U2iohqy+n6GIzkbmsA+TbVjmvFLkUKt3bs5M+qRBteQm/0IWzqSHYFzEQLzDTQebg==}
-     engines: {node: '>=20.0.0'}
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/app-check-interop-types@0.3.2':
--    resolution: {integrity: sha512-LMs47Vinv2HBMZi49C09dJxp0QT5LwDzFaVGf/+ITHe3BlIhUiLNttkATSXplc89A2lAaeTqjgqVkiRfUGyQiQ==}
--
-   '@firebase/app-check-interop-types@0.3.4':
-     resolution: {integrity: sha512-zz3i6e13B8BfWiLy8MABtTh8aGIACgKbf9UVnyHcWs+yQzJXgQcl8A46b0zfaiJHdQ+niF0ouAfcpuf+3LMPQg==}
- 
--  '@firebase/app-check-types@0.5.2':
--    resolution: {integrity: sha512-FSOEzTzL5bLUbD2co3Zut46iyPWML6xc4x+78TeaXMSuJap5QObfb+rVvZJtla3asN4RwU7elaQaduP+HFizDA==}
--
-   '@firebase/app-check-types@0.5.4':
-     resolution: {integrity: sha512-xV7JsIyzVr15aA7f3Pi0rB9gdBuVubs89FGA8VkRYA4g0l78poADgdfrScgf7NndSg9mm7cR7PJyY0+t22KaGw==}
- 
-@@ -2021,54 +2010,26 @@ packages:
-     peerDependencies:
-       '@firebase/app': 0.x
- 
--  '@firebase/app-check@0.8.8':
--    resolution: {integrity: sha512-O49RGF1xj7k6BuhxGpHmqOW5hqBIAEbt2q6POW0lIywx7emYtzPDeQI+ryQpC4zbKX646SoVZ711TN1DBLNSOQ==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
--  '@firebase/app-compat@0.2.43':
--    resolution: {integrity: sha512-HM96ZyIblXjAC7TzE8wIk2QhHlSvksYkQ4Ukh1GmEenzkucSNUmUX4QvoKrqeWsLEQ8hdcojABeCV8ybVyZmeg==}
--
-   '@firebase/app-compat@0.5.14':
-     resolution: {integrity: sha512-rgFmiofYsdS9ZG/Bht3OBxJtPD3zWE1cffShWubEm+4+qZeyzCbmtb1q6jOEjN9fB7uufe4rQmWOPXouR3758Q==}
-     engines: {node: '>=20.0.0'}
- 
--  '@firebase/app-types@0.9.2':
--    resolution: {integrity: sha512-oMEZ1TDlBz479lmABwWsWjzHwheQKiAgnuKxE0pz0IXCVx7/rtlkx1fQ6GfgK24WCrxDKMplZrT50Kh04iMbXQ==}
--
-   '@firebase/app-types@0.9.5':
-     resolution: {integrity: sha512-YevqTjvo7Iujsa9Dwowmd6dSoElhzmD63ZSrq6bzjvQ6POjYgNjOFHLmNIgJs48eNO093NCERibuFnxbfOvU7A==}
- 
--  '@firebase/app@0.10.13':
--    resolution: {integrity: sha512-OZiDAEK/lDB6xy/XzYAyJJkaDqmQ+BCtOEPLqFvxWKUz5JbBmej7IiiRHdtiIOD/twW7O5AxVsfaaGA/V1bNsA==}
--
-   '@firebase/app@0.15.0':
-     resolution: {integrity: sha512-soIskolmGgbpi0K/MfrjtdpO1220qRCbXA4Z8Qx3lM+fVwA3q40m+OM+7zBHd2nuQCrLXb33L6Oc1aBH3Y26AQ==}
-     engines: {node: '>=20.0.0'}
- 
--  '@firebase/auth-compat@0.5.14':
--    resolution: {integrity: sha512-2eczCSqBl1KUPJacZlFpQayvpilg3dxXLy9cSMTKtQMTQSmondUtPI47P3ikH3bQAXhzKLOE+qVxJ3/IRtu9pw==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
-   '@firebase/auth-compat@0.6.8':
-     resolution: {integrity: sha512-llcBREUC4iSNKZ6rvwud7Oz9Q7aAWU6KuQLa6pdu7Q+QAQsy4JLw6yFgxwtmzabsgznHmmcsX2UjHLLzqUxi3Q==}
-     engines: {node: '>=20.0.0'}
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/auth-interop-types@0.2.3':
--    resolution: {integrity: sha512-Fc9wuJGgxoxQeavybiuwgyi+0rssr76b+nHpj+eGhXFYAdudMWyfBHvFL/I5fEHniUM/UQdFzi9VXJK2iZF7FQ==}
--
-   '@firebase/auth-interop-types@0.2.5':
-     resolution: {integrity: sha512-1Li/YuBDBAXcKv7BzY4U28gontUmAaw53sYiqbaVOMCFb2lFKK/c3CGMUWqtwe7+TXrl3poWnTCL5umYBg85Eg==}
- 
--  '@firebase/auth-types@0.12.2':
--    resolution: {integrity: sha512-qsEBaRMoGvHO10unlDJhaKSuPn4pyoTtlQuP1ghZfzB6rNQPuhp/N/DcFZxm9i4v0SogjCbf9reWupwIvfmH6w==}
--    peerDependencies:
--      '@firebase/app-types': 0.x
--      '@firebase/util': 1.x
--
-   '@firebase/auth-types@0.13.1':
-     resolution: {integrity: sha512-0c1Mnid0uMDfGJHeUS4zfvBa4/CedJXotGy/n/NZJnBjwiJawt0ZYU+wH2VAVLiRCEfG2ncCkAX3yd1/2nrB7g==}
-     peerDependencies:
-@@ -2085,35 +2046,15 @@ packages:
-       '@react-native-async-storage/async-storage':
-         optional: true
- 
--  '@firebase/auth@1.7.9':
--    resolution: {integrity: sha512-yLD5095kVgDw965jepMyUrIgDklD6qH/BZNHeKOgvu7pchOKNjVM+zQoOVYJIKWMWOWBq8IRNVU6NXzBbozaJg==}
--    peerDependencies:
--      '@firebase/app': 0.x
--      '@react-native-async-storage/async-storage': ^1.18.1
--    peerDependenciesMeta:
--      '@react-native-async-storage/async-storage':
--        optional: true
--
--  '@firebase/component@0.6.9':
--    resolution: {integrity: sha512-gm8EUEJE/fEac86AvHn8Z/QW8BvR56TBw3hMW0O838J/1mThYQXAIQBgUv75EqlCZfdawpWLrKt1uXvp9ciK3Q==}
--
-   '@firebase/component@0.7.3':
-     resolution: {integrity: sha512-wFofIaa2879ogD/WvkjYXJxRmfnL0scen6ORgaC3na1FNOR9ASIUANQdhqQcmWu/h77/pVHY7ch5flewa5Bcew==}
-     engines: {node: '>=20.0.0'}
- 
--  '@firebase/data-connect@0.1.0':
--    resolution: {integrity: sha512-vSe5s8dY13ilhLnfY0eYRmQsdTbH7PUFZtBbqU6JVX/j8Qp9A6G5gG6//ulbX9/1JFOF1IWNOne9c8S/DOCJaQ==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
-   '@firebase/data-connect@0.7.1':
-     resolution: {integrity: sha512-2LbUU8mmSA63HknxQMmWHjpzuNLBKflvVwQc2tpoVKg0biWleNEJX031ELks0vzFs+dDjOUkCJR72RP6mQHFOg==}
-     peerDependencies:
-       '@firebase/app': 0.x
- 
--  '@firebase/database-compat@1.0.8':
--    resolution: {integrity: sha512-OpeWZoPE3sGIRPBKYnW9wLad25RaWbGyk7fFQe4xnJQKRzlynWeFBSRRAoLE2Old01WXwskUiucNqUUVlFsceg==}
--
-   '@firebase/database-compat@2.1.4':
-     resolution: {integrity: sha512-3pK35F1MAgmqFJQlf2nhQl44vtAXQO1uaCaQOEUI9kCRtLFqi7N+QRKR7lFZPg+xIZIyubgxQaxY69YgfZRZWg==}
-     engines: {node: '>=20.0.0'}
-@@ -2121,33 +2062,16 @@ packages:
-   '@firebase/database-types@1.0.20':
-     resolution: {integrity: sha512-kegbOk/w8iU64pr0q6k2ItyNGjnQBMHFhwS7ohdWI4W+pc0/zhhdGXTdFj6X1oxItRjPoYOsSQmERgBkn/ihxw==}
- 
--  '@firebase/database-types@1.0.5':
--    resolution: {integrity: sha512-fTlqCNwFYyq/C6W7AJ5OCuq5CeZuBEsEwptnVxlNPkWCo5cTTyukzAHRSO/jaQcItz33FfYrrFk1SJofcu2AaQ==}
--
--  '@firebase/database@1.0.8':
--    resolution: {integrity: sha512-dzXALZeBI1U5TXt6619cv0+tgEhJiwlUtQ55WNZY7vGAjv7Q1QioV969iYwt1AQQ0ovHnEW0YW9TiBfefLvErg==}
--
-   '@firebase/database@1.1.3':
-     resolution: {integrity: sha512-XwWCa+E4TvNGpGwXrycLRNfdogADwFcvuhyow6wDWma9W54roaQIhe+4PM0KiLsIftBdSCGI7OKCXrdSRHbIhw==}
-     engines: {node: '>=20.0.0'}
- 
--  '@firebase/firestore-compat@0.3.38':
--    resolution: {integrity: sha512-GoS0bIMMkjpLni6StSwRJarpu2+S5m346Na7gr9YZ/BZ/W3/8iHGNr9PxC+f0rNZXqS4fGRn88pICjrZEgbkqQ==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
-   '@firebase/firestore-compat@0.4.11':
-     resolution: {integrity: sha512-W7o1WdwWq5aABK5Up2ncSvTQs/QGLR/fy7cVpFBNqhsXtxoMtflHf2xBIG6+aoptcuGAobddq4g2Sq27wqHaYw==}
-     engines: {node: '>=20.0.0'}
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/firestore-types@3.0.2':
--    resolution: {integrity: sha512-wp1A+t5rI2Qc/2q7r2ZpjUXkRVPtGMd6zCLsiWurjsQpqPgFin3AhNibKcIzoF2rnToNa/XYtyWXuifjOOwDgg==}
--    peerDependencies:
--      '@firebase/app-types': 0.x
--      '@firebase/util': 1.x
--
-   '@firebase/firestore-types@3.0.4':
-     resolution: {integrity: sha512-jGn+JSS4X9zZsrfu7Yw66v5YRdOLD1oyQh4USR0xWl4CUqV/DA6bNIXRPpxH/cUl3iVTNiP6MN7g+EL42A4qfA==}
-     peerDependencies:
-@@ -2160,34 +2084,15 @@ packages:
-     peerDependencies:
-       '@firebase/app': 0.x
- 
--  '@firebase/firestore@4.7.3':
--    resolution: {integrity: sha512-NwVU+JPZ/3bhvNSJMCSzfcBZZg8SUGyzZ2T0EW3/bkUeefCyzMISSt/TTIfEHc8cdyXGlMqfGe3/62u9s74UEg==}
--    engines: {node: '>=10.10.0'}
--    peerDependencies:
--      '@firebase/app': 0.x
--
--  '@firebase/functions-compat@0.3.14':
--    resolution: {integrity: sha512-dZ0PKOKQFnOlMfcim39XzaXonSuPPAVuzpqA4ONTIdyaJK/OnBaIEVs/+BH4faa1a2tLeR+Jy15PKqDRQoNIJw==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
-   '@firebase/functions-compat@0.4.5':
-     resolution: {integrity: sha512-10qlUXGY25G5/1g9UihqksPp2po+ZqSE7LEizsrdUP7vrTmkysXxGSZCDyojSEp6mQe/ecRDdDDI+z4XRdb4wQ==}
-     engines: {node: '>=20.0.0'}
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/functions-types@0.6.2':
--    resolution: {integrity: sha512-0KiJ9lZ28nS2iJJvimpY4nNccV21rkQyor5Iheu/nq8aKXJqtJdeSlZDspjPSBBiHRzo7/GMUttegnsEITqR+w==}
--
-   '@firebase/functions-types@0.6.4':
-     resolution: {integrity: sha512-zV6kgqtduR4rUAdC/ilS7kmb93XD7bEZoJDlVBZqlOw2uGGGCNBQBuleww2rr0Ulr3L9o2TDjumEt68/l1f9DQ==}
- 
--  '@firebase/functions@0.11.8':
--    resolution: {integrity: sha512-Lo2rTPDn96naFIlSZKVd1yvRRqqqwiJk7cf9TZhUerwnPKgBzXy+aHE22ry+6EjCaQusUoNai6mU6p+G8QZT1g==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
-   '@firebase/functions@0.13.5':
-     resolution: {integrity: sha512-bWCx713f4kE/uFV7gdFOLBS7lDoiZj48MRkbAqe35gkXcCeWF4QjRNO07Jhmve7EJIoQOBczL29y2r8VRuN1kw==}
-     engines: {node: '>=20.0.0'}
-@@ -2199,16 +2104,6 @@ packages:
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/installations-compat@0.2.9':
--    resolution: {integrity: sha512-2lfdc6kPXR7WaL4FCQSQUhXcPbI7ol3wF+vkgtU25r77OxPf8F/VmswQ7sgIkBBWtymn5ZF20TIKtnOj9rjb6w==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
--  '@firebase/installations-types@0.5.2':
--    resolution: {integrity: sha512-que84TqGRZJpJKHBlF2pkvc1YcXrtEDOVGiDjovP/a3s6W4nlbohGXEsBJo0JCeeg/UG9A+DEZVDUV9GpklUzA==}
--    peerDependencies:
--      '@firebase/app-types': 0.x
--
-   '@firebase/installations-types@0.5.4':
-     resolution: {integrity: sha512-U2eFapdHwjb43Vx9o+Pmj4dFfvcHEK1IirEFLqMtWrTHvmdrS3gBpBD1kmJk/9HjsOtoHZxJ2Paoe79e+L1ZPg==}
-     peerDependencies:
-@@ -2219,39 +2114,18 @@ packages:
-     peerDependencies:
-       '@firebase/app': 0.x
- 
--  '@firebase/installations@0.6.9':
--    resolution: {integrity: sha512-hlT7AwCiKghOX3XizLxXOsTFiFCQnp/oj86zp1UxwDGmyzsyoxtX+UIZyVyH/oBF5+XtblFG9KZzZQ/h+dpy+Q==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
--  '@firebase/logger@0.4.2':
--    resolution: {integrity: sha512-Q1VuA5M1Gjqrwom6I6NUU4lQXdo9IAQieXlujeHZWvRt1b7qQ0KwBaNAjgxG27jgF9/mUwsNmO8ptBCGVYhB0A==}
--
-   '@firebase/logger@0.5.1':
-     resolution: {integrity: sha512-vZKLsqE1ABOy8OjQiE7cUTFn4gvaqlk88yp8N94Pk/sDpq61YqZGqmVFZTvOyflTwuYFcWirBdYGoJgbDaXKYQ==}
-     engines: {node: '>=20.0.0'}
- 
--  '@firebase/messaging-compat@0.2.12':
--    resolution: {integrity: sha512-pKsiUVZrbmRgdImYqhBNZlkKJbqjlPkVdQRZGRbkTyX4OSGKR0F/oJeCt1a8jEg5UnBp4fdVwSWSp4DuCovvEQ==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
-   '@firebase/messaging-compat@0.2.27':
-     resolution: {integrity: sha512-JNOiu1PPgdHzEPEtoFiNxQuu0x9bm4bfETSQCpGfcTlgWkhlSK7uh7nlsjC10TQLUNgYetLmuutaYTh8aeYLVA==}
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/messaging-interop-types@0.2.2':
--    resolution: {integrity: sha512-l68HXbuD2PPzDUOFb3aG+nZj5KA3INcPwlocwLZOzPp9rFM9yeuI9YLl6DQfguTX5eAGxO0doTR+rDLDvQb5tA==}
--
-   '@firebase/messaging-interop-types@0.2.5':
-     resolution: {integrity: sha512-tUEKnaAP2Y/MNIqgnriPpV6e5l13Vs/+p2yrd6NGlncPJT9O3a8muYZtdnWe+IJ4fgKLHJVC79n/asxk/N5Msw==}
- 
--  '@firebase/messaging@0.12.12':
--    resolution: {integrity: sha512-6q0pbzYBJhZEtUoQx7hnPhZvAbuMNuBXKQXOx2YlWhSrlv9N1m0ZzlNpBbu/ItTzrwNKTibdYzUyaaxdWLg+4w==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
-   '@firebase/messaging@0.13.0':
-     resolution: {integrity: sha512-GZoo0uGRvEbszo83xcgbjJp4FpkmBEr4l8Z4hi8gl+P1Spn/MTK3HapanMzSX4yUHuTEiF5hasWRxOaz+o5sxQ==}
-     peerDependencies:
-@@ -2262,22 +2136,9 @@ packages:
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/performance-compat@0.2.9':
--    resolution: {integrity: sha512-dNl95IUnpsu3fAfYBZDCVhXNkASE0uo4HYaEPd2/PKscfTvsgqFAOxfAXzBEDOnynDWiaGUnb5M1O00JQ+3FXA==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
--  '@firebase/performance-types@0.2.2':
--    resolution: {integrity: sha512-gVq0/lAClVH5STrIdKnHnCo2UcPLjJlDUoEB/tB4KM+hAeHUxWKnpT0nemUPvxZ5nbdY/pybeyMe8Cs29gEcHA==}
--
-   '@firebase/performance-types@0.2.4':
-     resolution: {integrity: sha512-kJSEk7b0uhpcPRyL4SQ/GPujLqk52XNKcXlnsKDbWGAb9vugcLvOU3u6zfEdwd+d8hWJb5S5ZizV1JFFI0nkKg==}
- 
--  '@firebase/performance@0.6.9':
--    resolution: {integrity: sha512-PnVaak5sqfz5ivhua+HserxTJHtCar/7zM0flCX6NkzBNzJzyzlH4Hs94h2Il0LQB99roBqoE5QT1JqWqcLJHQ==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
-   '@firebase/performance@0.7.12':
-     resolution: {integrity: sha512-fe7nV8teUU3OBHlMUZ9Lw4gLhCW2k4m5Uc3pfWGV+fl8uwJQBGp9Q3lqsJ+HSrFu3Q2pJyLAgrClPGSKyDeYgQ==}
-     peerDependencies:
-@@ -2288,78 +2149,36 @@ packages:
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/remote-config-compat@0.2.9':
--    resolution: {integrity: sha512-AxzGpWfWFYejH2twxfdOJt5Cfh/ATHONegTd/a0p5flEzsD5JsxXgfkFToop+mypEL3gNwawxrxlZddmDoNxyA==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
--  '@firebase/remote-config-types@0.3.2':
--    resolution: {integrity: sha512-0BC4+Ud7y2aPTyhXJTMTFfrGGLqdYXrUB9sJVAB8NiqJswDTc4/2qrE/yfUbnQJhbSi6ZaTTBKyG3n1nplssaA==}
--
-   '@firebase/remote-config-types@0.5.1':
-     resolution: {integrity: sha512-cX/1LT6KQwkXzck2eSzeKnuvXZCyr8qaPpDcikoJs7jmI+oBOXixpDLeDtWj1U6GNMkIoXrEDNoyT2Ypcyp5/A==}
- 
--  '@firebase/remote-config@0.4.9':
--    resolution: {integrity: sha512-EO1NLCWSPMHdDSRGwZ73kxEEcTopAxX1naqLJFNApp4hO8WfKfmEpmjxmP5TrrnypjIf2tUkYaKsfbEA7+AMmA==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
-   '@firebase/remote-config@0.8.5':
-     resolution: {integrity: sha512-zb+7CDGFP2wYVF1LXQoYIFdoESIQM3p0+uiW1welw8+zvDxAL50K75PKTXXtunJADUrksTVpV7mD0pn54vzJRA==}
-     peerDependencies:
-       '@firebase/app': 0.x
- 
--  '@firebase/storage-compat@0.3.12':
--    resolution: {integrity: sha512-hA4VWKyGU5bWOll+uwzzhEMMYGu9PlKQc1w4DWxB3aIErWYzonrZjF0icqNQZbwKNIdh8SHjZlFeB2w6OSsjfg==}
--    peerDependencies:
--      '@firebase/app-compat': 0.x
--
-   '@firebase/storage-compat@0.4.3':
-     resolution: {integrity: sha512-gruVqjtUGX8tEoeNbaWXZm0Zfcfcb7fvmDmBxV8yPAbWvExRnZYLO2+qw9idxNE7BvPXt5csyjSYHy//dAizxw==}
-     engines: {node: '>=20.0.0'}
-     peerDependencies:
-       '@firebase/app-compat': 0.x
- 
--  '@firebase/storage-types@0.8.2':
--    resolution: {integrity: sha512-0vWu99rdey0g53lA7IShoA2Lol1jfnPovzLDUBuon65K7uKG9G+L5uO05brD9pMw+l4HRFw23ah3GwTGpEav6g==}
--    peerDependencies:
--      '@firebase/app-types': 0.x
--      '@firebase/util': 1.x
--
-   '@firebase/storage-types@0.8.4':
-     resolution: {integrity: sha512-BT7cwxJOx8SWwlQfrlC+bD/Sk3Cw+1odCi8UZNFNWTVZoPsBnA5W+mqtZzVnvsdJpXCFGSGQ7R7vOR6dtM/BRA==}
-     peerDependencies:
-       '@firebase/app-types': 0.x
-       '@firebase/util': 1.x
- 
--  '@firebase/storage@0.13.2':
--    resolution: {integrity: sha512-fxuJnHshbhVwuJ4FuISLu+/76Aby2sh+44ztjF2ppoe0TELIDxPW6/r1KGlWYt//AD0IodDYYA8ZTN89q8YqUw==}
--    peerDependencies:
--      '@firebase/app': 0.x
--
-   '@firebase/storage@0.14.3':
-     resolution: {integrity: sha512-YX4/YL6P6/fufSSeGnVhjWddcIXbFq2cWIhMKFTZo1E/Rtcl2mJj/BYUQTwJfcE1Tl8un1FOya4L05jcSLN/Eg==}
-     engines: {node: '>=20.0.0'}
-     peerDependencies:
-       '@firebase/app': 0.x
- 
--  '@firebase/util@1.10.0':
--    resolution: {integrity: sha512-xKtx4A668icQqoANRxyDLBLz51TAbDP9KRfpbKGxiCAW346d0BeJe5vN6/hKxxmWwnZ0mautyv39JxviwwQMOQ==}
--
-   '@firebase/util@1.15.1':
-     resolution: {integrity: sha512-LUdM4Wg7YM9Pq/49nGYySJA0CSQEKnGffFzWV8+6gXN7mGxn+FL1IqvFbuZUtAQcfZgHYDwCE1wwlK7rB7gl2g==}
-     engines: {node: '>=20.0.0'}
- 
--  '@firebase/vertexai-preview@0.0.4':
--    resolution: {integrity: sha512-EBSqyu9eg8frQlVU9/HjKtHN7odqbh9MtAcVz3WwHj4gLCLOoN9F/o+oxlq3CxvFrd3CNTZwu6d2mZtVlEInng==}
--    engines: {node: '>=18.0.0'}
--    peerDependencies:
--      '@firebase/app': 0.x
--      '@firebase/app-types': 0.x
--
--  '@firebase/webchannel-wrapper@1.0.1':
--    resolution: {integrity: sha512-jmEnr/pk0yVkA7mIlHNnxCi+wWzOFUg0WyIotgkKAb2u1J7fAeDBcVNSTjTihbAYNusCLQdW5s9IJ5qwnEufcQ==}
--
-   '@firebase/webchannel-wrapper@1.0.6':
-     resolution: {integrity: sha512-Vr/Mqu79dMwGRAyGbJ4uN4+BtXB3/mRTdzetD1daWNeG8QaWuzhhbG77GltO5c0yYmYls8i250iX73624GJd7Q==}
- 
-@@ -3212,7 +3031,7 @@ packages:
-     resolution: {integrity: sha512-1hOEcfxLgorg0TwadBJeeEvoD7P4JMCJLhdO1doUQWZRs83WmwTlBJGv8GiO1y2KWaKjQh+JdgsuYCqG2dPXcA==}
-     peerDependencies:
-       '@tanstack/react-query': ^5
--      firebase: ^11.3.0 || ^12.0.0
-+      firebase: ^12.15.0
- 
-   '@tanstack/query-core@5.101.0':
-     resolution: {integrity: sha512-cQetA74EB+seWySv1TTKr828TnP0u39m6LykwDXIo84SNortpDkp30TMEjkqtYCNP9c40uT/iwl6MLiufEt0Ow==}
-@@ -5478,9 +5297,6 @@ packages:
-     resolution: {integrity: sha512-v2ZsoEuVHYy8ZIlYqwPe/39Cy+cFDzp4dXPaxNvkEuouymu+2Jbz0PxpKarJHYJTmv2HWT3O382qY8l4jMWthw==}
-     engines: {node: ^12.20.0 || ^14.13.1 || >=16.0.0}
- 
--  firebase@10.14.1:
--    resolution: {integrity: sha512-0KZxU+Ela9rUCULqFsUUOYYkjh7OM1EWdIfG6///MtXd0t2/uUIf0iNV5i0KariMhRQ5jve/OY985nrAXFaZeQ==}
--
-   firebase@12.15.0:
-     resolution: {integrity: sha512-p0YTLcRSTiBXMx9sGr4ZNSfLjc/RVBEw4C/TXjVMtw65+6E1Pbm47UY3F4/AqRoDobEcNX3gsbPGy7jPjxbgSQ==}
- 
-@@ -8827,10 +8643,6 @@ packages:
-     resolution: {integrity: sha512-72RFADWFqKmUb2hmmvNODKL3p9hcB6Gt2DOQMis1SEBaV6a4MH8soBvzg+95CYhCKPFedut2JY9bMfrDl9D23g==}
-     engines: {node: '>=14.0'}
- 
--  undici@6.19.7:
--    resolution: {integrity: sha512-HR3W/bMGPSr90i8AAp2C4DM3wChFdJPLrWYpIS++LxS8K+W535qftjt+4MyjNYHeWabMj1nvtmLIi7l++iq91A==}
--    engines: {node: '>=18.17'}
--
-   undici@7.28.0:
-     resolution: {integrity: sha512-cRZYrTDwWznlnRiPjggAGxZXanty6M8RV1ff8Wm4LWXBp7/IG8v5DnOm74DtUBp9OONpK75YlPnIjQqX0dBDtA==}
-     engines: {node: '>=20.18.1'}
-@@ -10623,10 +10435,10 @@ snapshots:
-     dependencies:
-       postcss: 8.5.15
- 
--  '@dataconnect/generated@file:apps/studio-client/src/dataconnect-generated(@tanstack-query-firebase/react@2.1.1(@tanstack/react-query@5.101.0(react@19.2.7))(firebase@10.14.1))(firebase@10.14.1)':
-+  '@dataconnect/generated@file:apps/studio-client/src/dataconnect-generated(@tanstack-query-firebase/react@2.1.1(@tanstack/react-query@5.101.0(react@19.2.7))(firebase@12.15.0))(firebase@12.15.0)':
-     dependencies:
--      '@tanstack-query-firebase/react': 2.1.1(@tanstack/react-query@5.101.0(react@19.2.7))(firebase@10.14.1)
--      firebase: 10.14.1
-+      '@tanstack-query-firebase/react': 2.1.1(@tanstack/react-query@5.101.0(react@19.2.7))(firebase@12.15.0)
-+      firebase: 12.15.0
- 
-   '@dataconnect/generated@file:tools/vscode-extension/src/dataconnect-generated(firebase@12.15.0)':
-     dependencies:
-@@ -11978,17 +11790,6 @@ snapshots:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/analytics-compat@0.2.14(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/analytics': 0.10.8(@firebase/app@0.10.13)
--      '@firebase/analytics-types': 0.8.2
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--
-   '@firebase/analytics-compat@0.2.28(@firebase/app-compat@0.5.14)(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/analytics': 0.10.22(@firebase/app@0.15.0)
-@@ -12000,8 +11801,6 @@ snapshots:
-     transitivePeerDependencies:
-       - '@firebase/app'
- 
--  '@firebase/analytics-types@0.8.2': {}
--
-   '@firebase/analytics-types@0.8.4': {}
- 
-   '@firebase/analytics@0.10.22(@firebase/app@0.15.0)':
-@@ -12013,27 +11812,6 @@ snapshots:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/analytics@0.10.8(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/installations': 0.6.9(@firebase/app@0.10.13)
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
--  '@firebase/app-check-compat@0.3.15(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-check': 0.8.8(@firebase/app@0.10.13)
--      '@firebase/app-check-types': 0.5.2
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--
-   '@firebase/app-check-compat@0.4.5(@firebase/app-compat@0.5.14)(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app-check': 0.12.0(@firebase/app@0.15.0)
-@@ -12046,12 +11824,8 @@ snapshots:
-     transitivePeerDependencies:
-       - '@firebase/app'
- 
--  '@firebase/app-check-interop-types@0.3.2': {}
--
-   '@firebase/app-check-interop-types@0.3.4': {}
- 
--  '@firebase/app-check-types@0.5.2': {}
--
-   '@firebase/app-check-types@0.5.4': {}
- 
-   '@firebase/app-check@0.12.0(@firebase/app@0.15.0)':
-@@ -12062,22 +11836,6 @@ snapshots:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/app-check@0.8.8(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
--  '@firebase/app-compat@0.2.43':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
-   '@firebase/app-compat@0.5.14':
-     dependencies:
-       '@firebase/app': 0.15.0
-@@ -12086,20 +11844,10 @@ snapshots:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/app-types@0.9.2': {}
--
-   '@firebase/app-types@0.9.5':
-     dependencies:
-       '@firebase/logger': 0.5.1
- 
--  '@firebase/app@0.10.13':
--    dependencies:
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      idb: 7.1.1
--      tslib: 2.8.1
--
-   '@firebase/app@0.15.0':
-     dependencies:
-       '@firebase/component': 0.7.3
-@@ -12108,20 +11856,6 @@ snapshots:
-       idb: 7.1.1
-       tslib: 2.8.1
- 
--  '@firebase/auth-compat@0.5.14(@firebase/app-compat@0.2.43)(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-compat': 0.2.43
--      '@firebase/auth': 1.7.9(@firebase/app@0.10.13)
--      '@firebase/auth-types': 0.12.2(@firebase/app-types@0.9.2)(@firebase/util@1.10.0)
--      '@firebase/component': 0.6.9
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--      undici: 6.19.7
--    transitivePeerDependencies:
--      - '@firebase/app'
--      - '@firebase/app-types'
--      - '@react-native-async-storage/async-storage'
--
-   '@firebase/auth-compat@0.6.8(@firebase/app-compat@0.5.14)(@firebase/app-types@0.9.5)(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app-compat': 0.5.14
-@@ -12135,15 +11869,8 @@ snapshots:
-       - '@firebase/app-types'
-       - '@react-native-async-storage/async-storage'
- 
--  '@firebase/auth-interop-types@0.2.3': {}
--
-   '@firebase/auth-interop-types@0.2.5': {}
- 
--  '@firebase/auth-types@0.12.2(@firebase/app-types@0.9.2)(@firebase/util@1.10.0)':
--    dependencies:
--      '@firebase/app-types': 0.9.2
--      '@firebase/util': 1.10.0
--
-   '@firebase/auth-types@0.13.1(@firebase/app-types@0.9.5)(@firebase/util@1.15.1)':
-     dependencies:
-       '@firebase/app-types': 0.9.5
-@@ -12157,34 +11884,11 @@ snapshots:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/auth@1.7.9(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--      undici: 6.19.7
--
--  '@firebase/component@0.6.9':
--    dependencies:
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
-   '@firebase/component@0.7.3':
-     dependencies:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/data-connect@0.1.0(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/auth-interop-types': 0.2.3
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
-   '@firebase/data-connect@0.7.1(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app': 0.15.0
-@@ -12194,15 +11898,6 @@ snapshots:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/database-compat@1.0.8':
--    dependencies:
--      '@firebase/component': 0.6.9
--      '@firebase/database': 1.0.8
--      '@firebase/database-types': 1.0.5
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
-   '@firebase/database-compat@2.1.4':
-     dependencies:
-       '@firebase/component': 0.7.3
-@@ -12217,21 +11912,6 @@ snapshots:
-       '@firebase/app-types': 0.9.5
-       '@firebase/util': 1.15.1
- 
--  '@firebase/database-types@1.0.5':
--    dependencies:
--      '@firebase/app-types': 0.9.2
--      '@firebase/util': 1.10.0
--
--  '@firebase/database@1.0.8':
--    dependencies:
--      '@firebase/app-check-interop-types': 0.3.2
--      '@firebase/auth-interop-types': 0.2.3
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      faye-websocket: 0.11.4
--      tslib: 2.8.1
--
-   '@firebase/database@1.1.3':
-     dependencies:
-       '@firebase/app-check-interop-types': 0.3.4
-@@ -12242,18 +11922,6 @@ snapshots:
-       faye-websocket: 0.11.4
-       tslib: 2.8.1
- 
--  '@firebase/firestore-compat@0.3.38(@firebase/app-compat@0.2.43)(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/firestore': 4.7.3(@firebase/app@0.10.13)
--      '@firebase/firestore-types': 3.0.2(@firebase/app-types@0.9.2)(@firebase/util@1.10.0)
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--      - '@firebase/app-types'
--
-   '@firebase/firestore-compat@0.4.11(@firebase/app-compat@0.5.14)(@firebase/app-types@0.9.5)(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app-compat': 0.5.14
-@@ -12266,11 +11934,6 @@ snapshots:
-       - '@firebase/app'
-       - '@firebase/app-types'
- 
--  '@firebase/firestore-types@3.0.2(@firebase/app-types@0.9.2)(@firebase/util@1.10.0)':
--    dependencies:
--      '@firebase/app-types': 0.9.2
--      '@firebase/util': 1.10.0
--
-   '@firebase/firestore-types@3.0.4(@firebase/app-types@0.9.5)(@firebase/util@1.15.1)':
-     dependencies:
-       '@firebase/app-types': 0.9.5
-@@ -12288,29 +11951,6 @@ snapshots:
-       re2js: 0.4.3
-       tslib: 2.8.1
- 
--  '@firebase/firestore@4.7.3(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      '@firebase/webchannel-wrapper': 1.0.1
--      '@grpc/grpc-js': 1.9.16
--      '@grpc/proto-loader': 0.7.15
--      tslib: 2.8.1
--      undici: 6.19.7
--
--  '@firebase/functions-compat@0.3.14(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/functions': 0.11.8(@firebase/app@0.10.13)
--      '@firebase/functions-types': 0.6.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--
-   '@firebase/functions-compat@0.4.5(@firebase/app-compat@0.5.14)(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app-compat': 0.5.14
-@@ -12322,21 +11962,8 @@ snapshots:
-     transitivePeerDependencies:
-       - '@firebase/app'
- 
--  '@firebase/functions-types@0.6.2': {}
--
-   '@firebase/functions-types@0.6.4': {}
- 
--  '@firebase/functions@0.11.8(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/app-check-interop-types': 0.3.2
--      '@firebase/auth-interop-types': 0.2.3
--      '@firebase/component': 0.6.9
--      '@firebase/messaging-interop-types': 0.2.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--      undici: 6.19.7
--
-   '@firebase/functions@0.13.5(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app': 0.15.0
-@@ -12359,22 +11986,6 @@ snapshots:
-       - '@firebase/app'
-       - '@firebase/app-types'
- 
--  '@firebase/installations-compat@0.2.9(@firebase/app-compat@0.2.43)(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/installations': 0.6.9(@firebase/app@0.10.13)
--      '@firebase/installations-types': 0.5.2(@firebase/app-types@0.9.2)
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--      - '@firebase/app-types'
--
--  '@firebase/installations-types@0.5.2(@firebase/app-types@0.9.2)':
--    dependencies:
--      '@firebase/app-types': 0.9.2
--
-   '@firebase/installations-types@0.5.4(@firebase/app-types@0.9.5)':
-     dependencies:
-       '@firebase/app-types': 0.9.5
-@@ -12387,32 +11998,10 @@ snapshots:
-       idb: 7.1.1
-       tslib: 2.8.1
- 
--  '@firebase/installations@0.6.9(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/util': 1.10.0
--      idb: 7.1.1
--      tslib: 2.8.1
--
--  '@firebase/logger@0.4.2':
--    dependencies:
--      tslib: 2.8.1
--
-   '@firebase/logger@0.5.1':
-     dependencies:
-       tslib: 2.8.1
- 
--  '@firebase/messaging-compat@0.2.12(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/messaging': 0.12.12(@firebase/app@0.10.13)
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--
-   '@firebase/messaging-compat@0.2.27(@firebase/app-compat@0.5.14)(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app-compat': 0.5.14
-@@ -12423,20 +12012,8 @@ snapshots:
-     transitivePeerDependencies:
-       - '@firebase/app'
- 
--  '@firebase/messaging-interop-types@0.2.2': {}
--
-   '@firebase/messaging-interop-types@0.2.5': {}
- 
--  '@firebase/messaging@0.12.12(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/installations': 0.6.9(@firebase/app@0.10.13)
--      '@firebase/messaging-interop-types': 0.2.2
--      '@firebase/util': 1.10.0
--      idb: 7.1.1
--      tslib: 2.8.1
--
-   '@firebase/messaging@0.13.0(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app': 0.15.0
-@@ -12459,31 +12036,8 @@ snapshots:
-     transitivePeerDependencies:
-       - '@firebase/app'
- 
--  '@firebase/performance-compat@0.2.9(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/performance': 0.6.9(@firebase/app@0.10.13)
--      '@firebase/performance-types': 0.2.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--
--  '@firebase/performance-types@0.2.2': {}
--
-   '@firebase/performance-types@0.2.4': {}
- 
--  '@firebase/performance@0.6.9(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/installations': 0.6.9(@firebase/app@0.10.13)
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
-   '@firebase/performance@0.7.12(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app': 0.15.0
-@@ -12506,31 +12060,8 @@ snapshots:
-     transitivePeerDependencies:
-       - '@firebase/app'
- 
--  '@firebase/remote-config-compat@0.2.9(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/remote-config': 0.4.9(@firebase/app@0.10.13)
--      '@firebase/remote-config-types': 0.3.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--
--  '@firebase/remote-config-types@0.3.2': {}
--
-   '@firebase/remote-config-types@0.5.1': {}
- 
--  '@firebase/remote-config@0.4.9(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/installations': 0.6.9(@firebase/app@0.10.13)
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
-   '@firebase/remote-config@0.8.5(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app': 0.15.0
-@@ -12540,18 +12071,6 @@ snapshots:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/storage-compat@0.3.12(@firebase/app-compat@0.2.43)(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app-compat': 0.2.43
--      '@firebase/component': 0.6.9
--      '@firebase/storage': 0.13.2(@firebase/app@0.10.13)
--      '@firebase/storage-types': 0.8.2(@firebase/app-types@0.9.2)(@firebase/util@1.10.0)
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--    transitivePeerDependencies:
--      - '@firebase/app'
--      - '@firebase/app-types'
--
-   '@firebase/storage-compat@0.4.3(@firebase/app-compat@0.5.14)(@firebase/app-types@0.9.5)(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app-compat': 0.5.14
-@@ -12564,24 +12083,11 @@ snapshots:
-       - '@firebase/app'
-       - '@firebase/app-types'
- 
--  '@firebase/storage-types@0.8.2(@firebase/app-types@0.9.2)(@firebase/util@1.10.0)':
--    dependencies:
--      '@firebase/app-types': 0.9.2
--      '@firebase/util': 1.10.0
--
-   '@firebase/storage-types@0.8.4(@firebase/app-types@0.9.5)(@firebase/util@1.15.1)':
-     dependencies:
-       '@firebase/app-types': 0.9.5
-       '@firebase/util': 1.15.1
- 
--  '@firebase/storage@0.13.2(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/component': 0.6.9
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--      undici: 6.19.7
--
-   '@firebase/storage@0.14.3(@firebase/app@0.15.0)':
-     dependencies:
-       '@firebase/app': 0.15.0
-@@ -12589,26 +12095,10 @@ snapshots:
-       '@firebase/util': 1.15.1
-       tslib: 2.8.1
- 
--  '@firebase/util@1.10.0':
--    dependencies:
--      tslib: 2.8.1
--
-   '@firebase/util@1.15.1':
-     dependencies:
-       tslib: 2.8.1
- 
--  '@firebase/vertexai-preview@0.0.4(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)':
--    dependencies:
--      '@firebase/app': 0.10.13
--      '@firebase/app-check-interop-types': 0.3.2
--      '@firebase/app-types': 0.9.2
--      '@firebase/component': 0.6.9
--      '@firebase/logger': 0.4.2
--      '@firebase/util': 1.10.0
--      tslib: 2.8.1
--
--  '@firebase/webchannel-wrapper@1.0.1': {}
--
-   '@firebase/webchannel-wrapper@1.0.6': {}
- 
-   '@grpc/grpc-js@1.9.16':
-@@ -13578,10 +13068,10 @@ snapshots:
-       tailwindcss: 4.3.1
-       vite: 7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3)
- 
--  '@tanstack-query-firebase/react@2.1.1(@tanstack/react-query@5.101.0(react@19.2.7))(firebase@10.14.1)':
-+  '@tanstack-query-firebase/react@2.1.1(@tanstack/react-query@5.101.0(react@19.2.7))(firebase@12.15.0)':
-     dependencies:
-       '@tanstack/react-query': 5.101.0(react@19.2.7)
--      firebase: 10.14.1
-+      firebase: 12.15.0
- 
-   '@tanstack/query-core@5.101.0': {}
- 
-@@ -16338,39 +15828,6 @@ snapshots:
-       locate-path: 7.2.0
-       path-exists: 5.0.0
- 
--  firebase@10.14.1:
--    dependencies:
--      '@firebase/analytics': 0.10.8(@firebase/app@0.10.13)
--      '@firebase/analytics-compat': 0.2.14(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)
--      '@firebase/app': 0.10.13
--      '@firebase/app-check': 0.8.8(@firebase/app@0.10.13)
--      '@firebase/app-check-compat': 0.3.15(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)
--      '@firebase/app-compat': 0.2.43
--      '@firebase/app-types': 0.9.2
--      '@firebase/auth': 1.7.9(@firebase/app@0.10.13)
--      '@firebase/auth-compat': 0.5.14(@firebase/app-compat@0.2.43)(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)
--      '@firebase/data-connect': 0.1.0(@firebase/app@0.10.13)
--      '@firebase/database': 1.0.8
--      '@firebase/database-compat': 1.0.8
--      '@firebase/firestore': 4.7.3(@firebase/app@0.10.13)
--      '@firebase/firestore-compat': 0.3.38(@firebase/app-compat@0.2.43)(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)
--      '@firebase/functions': 0.11.8(@firebase/app@0.10.13)
--      '@firebase/functions-compat': 0.3.14(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)
--      '@firebase/installations': 0.6.9(@firebase/app@0.10.13)
--      '@firebase/installations-compat': 0.2.9(@firebase/app-compat@0.2.43)(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)
--      '@firebase/messaging': 0.12.12(@firebase/app@0.10.13)
--      '@firebase/messaging-compat': 0.2.12(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)
--      '@firebase/performance': 0.6.9(@firebase/app@0.10.13)
--      '@firebase/performance-compat': 0.2.9(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)
--      '@firebase/remote-config': 0.4.9(@firebase/app@0.10.13)
--      '@firebase/remote-config-compat': 0.2.9(@firebase/app-compat@0.2.43)(@firebase/app@0.10.13)
--      '@firebase/storage': 0.13.2(@firebase/app@0.10.13)
--      '@firebase/storage-compat': 0.3.12(@firebase/app-compat@0.2.43)(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)
--      '@firebase/util': 1.10.0
--      '@firebase/vertexai-preview': 0.0.4(@firebase/app-types@0.9.2)(@firebase/app@0.10.13)
--    transitivePeerDependencies:
--      - '@react-native-async-storage/async-storage'
--
-   firebase@12.15.0:
-     dependencies:
-       '@firebase/ai': 2.13.1(@firebase/app-types@0.9.5)(@firebase/app@0.15.0)
-@@ -18409,7 +17866,7 @@ snapshots:
-       is-docker: 2.2.1
-       is-wsl: 2.2.0
- 
--  openai@4.104.0(ws@8.21.0)(zod@4.4.3):
-+  openai@4.104.0(ws@8.21.0)(zod@3.25.76):
-     dependencies:
-       '@types/node': 18.19.130
-       '@types/node-fetch': 2.6.13
-@@ -18420,7 +17877,7 @@ snapshots:
-       node-fetch: 2.7.0
-     optionalDependencies:
-       ws: 8.21.0
--      zod: 4.4.3
-+      zod: 3.25.76
-     transitivePeerDependencies:
-       - encoding
- 
-@@ -20282,8 +19739,6 @@ snapshots:
-     dependencies:
-       '@fastify/busboy': 2.1.1
- 
--  undici@6.19.7: {}
--
-   undici@7.28.0:
-     optional: true
- 
-
-```
diff --git a/docs/autogen/changes/change_1432eacc88479e5eaaab9dd454857ca82d0e4c79.md b/docs/autogen/changes/change_1432eacc88479e5eaaab9dd454857ca82d0e4c79.md
deleted file mode 100644
index b893746bf..000000000
--- a/docs/autogen/changes/change_1432eacc88479e5eaaab9dd454857ca82d0e4c79.md
+++ /dev/null
@@ -1,10654 +0,0 @@
-# 📋 Commit 1432eacc88479e5eaaab9dd454857ca82d0e4c79
-
-## Commit Stats
-```
-commit 1432eacc88479e5eaaab9dd454857ca82d0e4c79
-Author: github-actions[bot] <github-actions[bot]@users.noreply.github.com>
-Date:   Sat Jul 4 05:52:59 2026 +0000
-
-    docs: auto-update codebase docs & dashboard [skip ci]
-
- docs/autogen/INDEX.md                              |     2 +-
- ...nge_19e2f4019bb7a2aef85243afe61c87a137171a2c.md | 11425 +++++++++++++++++++
- ...nge_46d8fa8174f0a005648e3103dbdf7022b68a6d44.md |    36 +
- ...nge_52509f67997c8abd7ab38c6c870f35fdba350ea1.md |    42 -
- ...nge_6f2266763ff71abf6a01b05b11944242932e2862.md |   790 --
- ...nge_82cff22c56cd0e4e6e83e7d6126fbb8289a929e8.md |   141 +
- ...nge_ea05efc83c894ea659cd4a679a3c7ff646f95e33.md | 10426 -----------------
- .../.github_actions_setup-backend_action.yml.md    |     2 +-
- ...github_scripts_advanced-validation-report.py.md |     2 +-
- .../codebase/.github_scripts_canary-deploy.py.md   |     2 +-
- .../codebase/.github_scripts_ci-auto-fix-v3.py.md  |     2 +-
- .../codebase/.github_scripts_ci-auto-fix.py.md     |     2 +-
- .../.github_scripts_ci-decision-engine.py.md       |     2 +-
- .../codebase/.github_scripts_ci-health-check.py.md |     2 +-
- .../.github_scripts_clean_action_logs.py.md        |     2 +-
- .../codebase/.github_scripts_deploy-backend.py.md  |     2 +-
- .../.github_scripts_detect-previous-failures.py.md |     2 +-
- .../codebase/.github_scripts_enforce_24h_gap.py.md |     2 +-
- .../.github_scripts_generate-ci-report.py.md       |     2 +-
- .../.github_scripts_generate_ai_prompt.py.md       |     2 +-
- .../.github_scripts_multi-model-evaluator.py.md    |     2 +-
- docs/autogen/codebase/.github_scripts_review.py.md |     2 +-
- .../.github_scripts_supremeai-evaluator.py.md      |     2 +-
- .../.github_scripts_test_ai_reviewer.py.md         |     2 +-
- .../codebase/.github_workflows_deploy.yml.md       |     2 +-
- .../.github_workflows_nightly-maintenance.yml.md   |     2 +-
- .../.github_workflows_supreme-core-ci.yml.md       |     2 +-
- .../.github_workflows_supreme-mobile-cd.yml.md     |     2 +-
- ....github_workflows_supreme-release-builds.yml.md |     2 +-
- .../.github_workflows_sync-from-prod.yml.md        |     2 +-
- docs/autogen/codebase/AGENT.md.md                  |     2 +-
- docs/autogen/codebase/AGENTS.md.md                 |     2 +-
- docs/autogen/codebase/CHANGELOG.md.md              |     2 +-
- docs/autogen/codebase/CI_PIPELINE.md.md            |     2 +-
- docs/autogen/codebase/CONTRIBUTING.md.md           |     2 +-
- docs/autogen/codebase/IMPLEMENTATION_STATUS.md.md  |     2 +-
- .../codebase/PRODUCTION_READINESS_GUIDE.md.md      |     2 +-
- docs/autogen/codebase/README.md.md                 |     2 +-
- docs/autogen/codebase/SECURITY.md.md               |     2 +-
- docs/autogen/codebase/accessibility.spec.ts.md     |     2 +-
- docs/autogen/codebase/admin_dashboard_script.js.md |     2 +-
- docs/autogen/codebase/admin_god.py.md              |     2 +-
- docs/autogen/codebase/apps_desktop_README.md.md    |     2 +-
- docs/autogen/codebase/apps_desktop_package.json.md |     2 +-
- .../codebase/apps_desktop_src-tauri_Cargo.toml.md  |     2 +-
- .../codebase/apps_desktop_src-tauri_build.rs.md    |     2 +-
- .../apps_desktop_src-tauri_secure-store.ts.md      |     2 +-
- .../codebase/apps_desktop_src-tauri_src_main.rs.md |     2 +-
- .../apps_desktop_src-tauri_tauri.conf.json.md      |     2 +-
- .../codebase/apps_desktop_src-ui_package.json.md   |     2 +-
- .../codebase/apps_desktop_src-ui_src_App.tsx.md    |     2 +-
- ..._desktop_src-ui_src_components_ChatInput.tsx.md |     2 +-
- .../codebase/apps_desktop_src-ui_src_main.tsx.md   |     2 +-
- .../apps_desktop_src-ui_src_pages_AdminPage.tsx.md |     2 +-
- .../apps_desktop_src-ui_src_pages_ChatPage.tsx.md  |     2 +-
- ...s_desktop_src-ui_src_pages_EvolutionPage.tsx.md |     2 +-
- .../apps_desktop_src-ui_src_pages_LoginPage.tsx.md |     2 +-
- ...apps_desktop_src-ui_src_pages_SkillsPage.tsx.md |     2 +-
- .../apps_desktop_src-ui_src_services_api.ts.md     |     2 +-
- .../apps_desktop_src-ui_src_stores_authStore.ts.md |     2 +-
- .../apps_desktop_src-ui_src_types_index.ts.md      |     2 +-
- .../apps_desktop_src-ui_src_vite-env.d.ts.md       |     2 +-
- .../codebase/apps_desktop_src-ui_tsconfig.json.md  |     2 +-
- .../apps_desktop_src-ui_tsconfig.node.json.md      |     2 +-
- .../codebase/apps_desktop_src-ui_vite.config.ts.md |     2 +-
- ...ava_com_supremeai_JavaWorkerApplication.java.md |     2 +-
- ...va_com_supremeai_grpc_WorkerServiceImpl.java.md |     2 +-
- ...in_java_com_supremeai_models_TaskEntity.java.md |     2 +-
- ...m_supremeai_repositories_TaskRepository.java.md |     2 +-
- ...va-worker_src_main_resources_application.yml.md |     2 +-
- docs/autogen/codebase/apps_mobile_README.md.md     |     2 +-
- docs/autogen/codebase/apps_mobile_README_BD.md.md  |     2 +-
- .../codebase/apps_mobile_analysis_options.yaml.md  |     2 +-
- .../codebase/apps_mobile_assets_i18n_ar.json.md    |     2 +-
- .../codebase/apps_mobile_assets_i18n_bn.json.md    |     2 +-
- .../codebase/apps_mobile_assets_i18n_en.json.md    |     2 +-
- .../codebase/apps_mobile_assets_i18n_es.json.md    |     2 +-
- .../codebase/apps_mobile_assets_i18n_hi.json.md    |     2 +-
- .../codebase/apps_mobile_assets_i18n_zh.json.md    |     2 +-
- .../codebase/apps_mobile_devtools_options.yaml.md  |     2 +-
- ...ts.xcassets_AppIcon.appiconset_Contents.json.md |     2 +-
- ....xcassets_LaunchImage.imageset_Contents.json.md |     2 +-
- ...sets.xcassets_LaunchImage.imageset_README.md.md |     2 +-
- ...s_mobile_lib_dataconnect_generated_README.md.md |     2 +-
- ...le_lib_dataconnect_generated_add_review.dart.md |     2 +-
- ..._lib_dataconnect_generated_create_movie.dart.md |     2 +-
- ...lib_dataconnect_generated_delete_review.dart.md |     2 +-
- ...ile_lib_dataconnect_generated_generated.dart.md |     2 +-
- ...b_dataconnect_generated_get_movie_by_id.dart.md |     2 +-
- ...e_lib_dataconnect_generated_list_movies.dart.md |     2 +-
- ...dataconnect_generated_list_user_reviews.dart.md |     2 +-
- ...le_lib_dataconnect_generated_list_users.dart.md |     2 +-
- ..._lib_dataconnect_generated_search_movie.dart.md |     2 +-
- ...e_lib_dataconnect_generated_upsert_user.dart.md |     2 +-
- docs/autogen/codebase/apps_mobile_lib_main.dart.md |     2 +-
- .../apps_mobile_lib_models_ci_job_model.dart.md    |     2 +-
- ...apps_mobile_lib_providers_auth_provider.dart.md |     2 +-
- ...mobile_lib_providers_dashboard_provider.dart.md |     2 +-
- ...le_lib_providers_orchestration_provider.dart.md |     2 +-
- ..._mobile_lib_providers_settings_provider.dart.md |     2 +-
- ...ps_mobile_lib_screens_agent_chat_screen.dart.md |     2 +-
- ...mobile_lib_screens_alerts_alerts_screen.dart.md |     2 +-
- ..._lib_screens_analytics_analytics_screen.dart.md |     2 +-
- ...apps_mobile_lib_screens_api_keys_screen.dart.md |     2 +-
- .../apps_mobile_lib_screens_api_scaffold.dart.md   |     2 +-
- ...apps_mobile_lib_screens_byoc_hub_screen.dart.md |     2 +-
- ..._lib_screens_consensus_consensus_screen.dart.md |     2 +-
- ...obile_lib_screens_dashboard_home_screen.dart.md |     2 +-
- ...pps_mobile_lib_screens_dashboard_screen.dart.md |     2 +-
- ..._lib_screens_extension_extension_screen.dart.md |     2 +-
- .../apps_mobile_lib_screens_git_git_screen.dart.md |     2 +-
- ...le_lib_screens_learning_learning_screen.dart.md |     2 +-
- .../apps_mobile_lib_screens_login_screen.dart.md   |     2 +-
- ...eens_notifications_notifications_screen.dart.md |     2 +-
- ...b_screens_projects_projects_list_screen.dart.md |     2 +-
- ...b_screens_providers_ai_providers_screen.dart.md |     2 +-
- ...s_mobile_lib_screens_quota_quota_screen.dart.md |     2 +-
- ...ib_screens_resilience_resilience_screen.dart.md |     2 +-
- ...apps_mobile_lib_screens_settings_screen.dart.md |     2 +-
- .../apps_mobile_lib_screens_terminal_view.dart.md  |     2 +-
- .../apps_mobile_lib_screens_vpn_vpn_screen.dart.md |     2 +-
- .../apps_mobile_lib_screens_wallet_screen.dart.md  |     2 +-
- .../apps_mobile_lib_services_api_client.dart.md    |     2 +-
- .../apps_mobile_lib_services_api_service.dart.md   |     2 +-
- ...pps_mobile_lib_services_billing_service.dart.md |     2 +-
- .../apps_mobile_lib_services_byoc_service.dart.md  |     2 +-
- ...pps_mobile_lib_services_ci_sync_service.dart.md |     2 +-
- ...s_mobile_lib_services_deployment_stream.dart.md |     2 +-
- ...obile_lib_services_localization_service.dart.md |     2 +-
- ...bile_lib_services_neural_stream_service.dart.md |     2 +-
- ...obile_lib_services_notification_service.dart.md |     2 +-
- ...obile_lib_services_offline_sync_service.dart.md |     2 +-
- ...ile_lib_services_payment_gateway_bridge.dart.md |     2 +-
- ..._mobile_lib_services_screen_api_service.dart.md |     2 +-
- .../apps_mobile_lib_theme_app_theme.dart.md        |     2 +-
- .../apps_mobile_lib_theme_theme_provider.dart.md   |     2 +-
- ...apps_mobile_lib_widgets_action_hub_card.dart.md |     2 +-
- ...ile_lib_widgets_base_dashboard_scaffold.dart.md |     2 +-
- .../codebase/apps_mobile_lib_widgets_es.json.md    |     2 +-
- .../apps_mobile_lib_widgets_json_dropzone.dart.md  |     2 +-
- .../apps_mobile_lib_widgets_live_terminal.dart.md  |     2 +-
- ...apps_mobile_lib_widgets_loading_widgets.dart.md |     2 +-
- ...le_lib_widgets_transaction_history_list.dart.md |     2 +-
- .../apps_mobile_lib_widgets_usage_chart.dart.md    |     2 +-
- ...ts.xcassets_AppIcon.appiconset_Contents.json.md |     2 +-
- docs/autogen/codebase/apps_mobile_pubspec.lock.md  |     2 +-
- docs/autogen/codebase/apps_mobile_pubspec.yaml.md  |     2 +-
- ...bile_test_auth_provider_edge_cases_test.dart.md |     2 +-
- .../apps_mobile_test_auth_provider_test.dart.md    |     2 +-
- ...mobile_test_home_screen_edge_cases_test.dart.md |     2 +-
- .../apps_mobile_test_home_screen_test.dart.md      |     2 +-
- ...s_mobile_test_screens_login_screen_test.dart.md |     2 +-
- .../codebase/apps_mobile_web_manifest.json.md      |     2 +-
- .../codebase/apps_studio-client_README.md.md       |     2 +-
- .../codebase/apps_studio-client_components.json.md |     2 +-
- .../apps_studio-client_eslint.config.js.md         |     2 +-
- .../autogen/codebase/apps_studio-client_main.js.md |     2 +-
- .../codebase/apps_studio-client_package.json.md    |     2 +-
- .../apps_studio-client_public_manifest.json.md     |     2 +-
- .../codebase/apps_studio-client_public_sw.js.md    |     2 +-
- .../apps_studio-client_src_App.test.tsx.md         |     2 +-
- .../codebase/apps_studio-client_src_App.tsx.md     |     2 +-
- ...tudio-client_src_components_AdminConsole.tsx.md |     2 +-
- ..._studio-client_src_components_BanglaHint.tsx.md |     2 +-
- ...apps_studio-client_src_components_Header.tsx.md |     2 +-
- ...lient_src_components_LiveSujonBackground.tsx.md |     2 +-
- ...c_components_Onboarding_OnboardingWizard.tsx.md |     2 +-
- ...ent_src_components_Onboarding_StepApiKey.tsx.md |     2 +-
- ..._src_components_Onboarding_StepFirstChat.tsx.md |     2 +-
- ...rc_components_Onboarding_StepModelSelect.tsx.md |     2 +-
- ...dio-client_src_components_OperatorStudio.tsx.md |     2 +-
- ...o-client_src_components_admin_ActionCard.tsx.md |     2 +-
- ..._src_components_admin_AdminAuthenticated.tsx.md |     2 +-
- ...client_src_components_admin_AdminConsole.tsx.md |     2 +-
- ..._src_components_admin_AdminDashboardHome.tsx.md |     2 +-
- ...o-client_src_components_admin_AdminLogin.tsx.md |     2 +-
- ..._src_components_admin_AdminSubTabContent.tsx.md |     2 +-
- ...-client_src_components_admin_AdminTopNav.tsx.md |     2 +-
- ...o-client_src_components_admin_AethelNode.tsx.md |     2 +-
- ...ient_src_components_admin_AuditLogsPanel.tsx.md |     2 +-
- ...lient_src_components_admin_BackupRestore.tsx.md |     2 +-
- ...ient_src_components_admin_CICDVisualizer.tsx.md |     2 +-
- ...t_src_components_admin_CloudOrchestrator.tsx.md |     2 +-
- ...lient_src_components_admin_CommandCenter.tsx.md |     2 +-
- ...client_src_components_admin_ConfigEditor.tsx.md |     2 +-
- ..._src_components_admin_ConsentMatrixModal.tsx.md |     2 +-
- ...-client_src_components_admin_CostAuditor.tsx.md |     2 +-
- ..._components_admin_DashboardErrorBoundary.tsx.md |     2 +-
- ...ent_src_components_admin_DeploymentModal.tsx.md |     2 +-
- ...client_src_components_admin_DynamicPanel.tsx.md |     2 +-
- ...omponents_admin_EnhancedSkillMarketplace.tsx.md |     2 +-
- ...t_src_components_admin_GithubIntegration.tsx.md |     2 +-
- ...client_src_components_admin_HealthBanner.tsx.md |     2 +-
- ...io-client_src_components_admin_HealthMap.tsx.md |     2 +-
- ..._src_components_admin_InteractiveChatTab.tsx.md |     2 +-
- ...dio-client_src_components_admin_LiveLogs.tsx.md |     2 +-
- ...lient_src_components_admin_MemoryBrowser.tsx.md |     2 +-
- ...-client_src_components_admin_ModelRouter.tsx.md |     2 +-
- ..._components_admin_ObservabilityDashboard.tsx.md |     2 +-
- ...-client_src_components_admin_RBACManager.tsx.md |     2 +-
- ...nt_src_components_admin_RateLimitManager.tsx.md |     2 +-
- ...rc_components_admin_RealTimeMetricsPanel.tsx.md |     2 +-
- ...mponents_admin_RedesignedDashboardMockup.tsx.md |     2 +-
- ...nt_src_components_admin_RulesEnginePanel.tsx.md |     2 +-
- ...t_src_components_admin_SecurityDashboard.tsx.md |     2 +-
- ...rc_components_admin_ServiceHealthMetrics.tsx.md |     2 +-
- ...ent_src_components_admin_ThreatDetection.tsx.md |     2 +-
- ...-client_src_components_admin_UserManager.tsx.md |     2 +-
- ..._src_components_admin_VisualRulesBuilder.tsx.md |     2 +-
- ..._studio-client_src_components_admin_index.ts.md |     2 +-
- ..._src_components_audio_WaveformVisualizer.tsx.md |     2 +-
- ...ient_src_components_chat_TypingIndicator.tsx.md |     2 +-
- ...nt_src_components_chat_UnifiedChatBubble.tsx.md |     2 +-
- ...s_studio-client_src_components_chat_index.ts.md |     2 +-
- ...t_src_components_customer_BrowserPreview.tsx.md |     2 +-
- ...t_src_components_customer_ChatPanel.test.tsx.md |     2 +-
- ...client_src_components_customer_ChatPanel.tsx.md |     2 +-
- ...lient_src_components_customer_CodeEditor.tsx.md |     2 +-
- ...-client_src_components_customer_HomeFeed.tsx.md |     2 +-
- ..._src_components_customer_MobileSimulator.tsx.md |     2 +-
- ...rc_components_customer_QuickPresets.test.tsx.md |     2 +-
- ...ent_src_components_customer_QuickPresets.tsx.md |     2 +-
- ...c_components_customer_UserDashboard.test.tsx.md |     2 +-
- ...nt_src_components_customer_UserDashboard.tsx.md |     2 +-
- ...udio-client_src_components_customer_index.ts.md |     2 +-
- ...components_dashboard_AutomationQueuePage.tsx.md |     2 +-
- ...components_dashboard_DashboardShell.test.tsx.md |     2 +-
- ..._src_components_dashboard_DashboardShell.tsx.md |     2 +-
- ...t_src_components_dashboard_KnowledgePage.tsx.md |     2 +-
- ..._src_components_dashboard_LlmGatewayPage.tsx.md |     2 +-
- ...ent_src_components_dashboard_SecretsPage.tsx.md |     2 +-
- ...c_components_dashboard_SessionDetailPage.tsx.md |     2 +-
- ...nt_src_components_dashboard_SessionsPage.tsx.md |     2 +-
- ...nt_src_components_dashboard_SettingsPage.tsx.md |     2 +-
- ...src_components_dashboard_SiteActionsPage.tsx.md |     2 +-
- ...lient_src_components_dashboard_UsagePage.tsx.md |     2 +-
- ...lient_src_components_dashboard_VaultPage.tsx.md |     2 +-
- ...ent_src_components_dashboard_sessionStore.ts.md |     2 +-
- ...ent_src_components_dashboard_useHashRoute.ts.md |     2 +-
- ...lient_src_components_editor_CollabEditor.tsx.md |     2 +-
- ...o-client_src_components_graph_SkillGraph.tsx.md |     2 +-
- ...udio-client_src_components_ui_ActionCard.tsx.md |     2 +-
- ...ps_studio-client_src_components_ui_Badge.tsx.md |     2 +-
- ...pps_studio-client_src_components_ui_Card.tsx.md |     2 +-
- ...studio-client_src_components_ui_Skeleton.tsx.md |     2 +-
- ...pps_studio-client_src_components_ui_index.ts.md |     2 +-
- ..._studio-client_src_contexts_ThemeContext.tsx.md |     2 +-
- ...o-client_src_dataconnect-generated_README.md.md |     2 +-
- ...t_src_dataconnect-generated_esm_index.esm.js.md |     2 +-
- ...t_src_dataconnect-generated_esm_package.json.md |     2 +-
- ...lient_src_dataconnect-generated_index.cjs.js.md |     2 +-
- ...-client_src_dataconnect-generated_index.d.ts.md |     2 +-
- ...lient_src_dataconnect-generated_package.json.md |     2 +-
- ...nt_src_dataconnect-generated_react_README.md.md |     2 +-
- ...dataconnect-generated_react_esm_index.esm.js.md |     2 +-
- ...dataconnect-generated_react_esm_package.json.md |     2 +-
- ...src_dataconnect-generated_react_index.cjs.js.md |     2 +-
- ...t_src_dataconnect-generated_react_index.d.ts.md |     2 +-
- ...src_dataconnect-generated_react_package.json.md |     2 +-
- .../codebase/apps_studio-client_src_firebase.ts.md |     2 +-
- .../apps_studio-client_src_hooks_index.ts.md       |     2 +-
- ...lient_src_hooks_tests_useTranslation.test.ts.md |     2 +-
- .../apps_studio-client_src_hooks_useAdminApi.ts.md |     2 +-
- .../apps_studio-client_src_hooks_useAuth.ts.md     |     2 +-
- .../apps_studio-client_src_hooks_useChat.ts.md     |     2 +-
- ..._studio-client_src_hooks_useDashboardData.ts.md |     2 +-
- ...ps_studio-client_src_hooks_useTranslation.ts.md |     2 +-
- ...apps_studio-client_src_hooks_useWebSocket.ts.md |     2 +-
- ...apps_studio-client_src_i18n_I18nProvider.tsx.md |     2 +-
- .../apps_studio-client_src_i18n_config.ts.md       |     2 +-
- .../apps_studio-client_src_i18n_translations.ts.md |     2 +-
- .../codebase/apps_studio-client_src_lib_etag.ts.md |     2 +-
- .../codebase/apps_studio-client_src_main.tsx.md    |     2 +-
- ...s_studio-client_src_services_adminService.ts.md |     2 +-
- ...tudio-client_src_services_adminTokenStore.ts.md |     2 +-
- ...s_studio-client_src_services_agentService.ts.md |     2 +-
- ...apps_studio-client_src_services_apiClient.ts.md |     2 +-
- ...ient_src_services_api_microserviceMonitor.ts.md |     2 +-
- ...t_src_services_audio_AudioPlaybackService.ts.md |     2 +-
- ...t_src_services_audio_AudioRecorderService.ts.md |     2 +-
- ...ps_studio-client_src_services_authService.ts.md |     2 +-
- ...ps_studio-client_src_services_chatService.ts.md |     2 +-
- ...tudio-client_src_services_ciReportService.ts.md |     2 +-
- ...pps_studio-client_src_services_storageApi.ts.md |     2 +-
- .../apps_studio-client_src_store_adminStore.ts.md  |     2 +-
- ...pps_studio-client_src_store_customerStore.ts.md |     2 +-
- ...ps_studio-client_src_store_dashboardStore.ts.md |     2 +-
- .../apps_studio-client_src_store_themeStore.ts.md  |     2 +-
- .../apps_studio-client_src_store_useStore.ts.md    |     2 +-
- .../apps_studio-client_src_test_setup.ts.md        |     2 +-
- .../codebase/apps_studio-client_src_types.ts.md    |     2 +-
- .../apps_studio-client_src_types_customer.ts.md    |     2 +-
- .../apps_studio-client_src_utils_api.ts.md         |     2 +-
- .../apps_studio-client_src_vite-env.d.ts.md        |     2 +-
- ...tudio-client_src_workers_logParser.worker.ts.md |     2 +-
- .../apps_studio-client_tsconfig.app.json.md        |     2 +-
- .../codebase/apps_studio-client_tsconfig.json.md   |     2 +-
- .../apps_studio-client_tsconfig.node.json.md       |     2 +-
- .../codebase/apps_studio-client_vite.config.ts.md  |     2 +-
- .../apps_studio-client_vitest.config.ts.md         |     2 +-
- docs/autogen/codebase/apps_web-chat_api.test.ts.md |     2 +-
- docs/autogen/codebase/apps_web-chat_api.ts.md      |     2 +-
- .../autogen/codebase/apps_web-chat_package.json.md |     2 +-
- docs/autogen/codebase/apps_web-chat_script.ts.md   |     2 +-
- .../codebase/apps_web-chat_tsconfig.json.md        |     2 +-
- .../codebase/apps_web-chat_vite-env.d.ts.md        |     2 +-
- .../codebase/apps_web-chat_vite.config.ts.md       |     2 +-
- .../codebase/apps_web-chat_vitest.config.ts.md     |     2 +-
- docs/autogen/codebase/backend_README.md.md         |     2 +-
- .../backend_adaptive_engine_experience_db.py.md    |     2 +-
- .../codebase/backend_adaptive_engine_init_.py.md   |     2 +-
- .../backend_adaptive_engine_intent_parser.py.md    |     2 +-
- .../backend_adaptive_engine_learning_loop.py.md    |     2 +-
- .../backend_adaptive_engine_platform_learner.py.md |     2 +-
- .../backend_adaptive_engine_registry.py.md         |     2 +-
- ...end_adaptive_engine_test_platform_learner.py.md |     2 +-
- docs/autogen/codebase/backend_admin_god.py.md      |     2 +-
- docs/autogen/codebase/backend_admin_init_.py.md    |     2 +-
- docs/autogen/codebase/backend_admin_test_god.py.md |     2 +-
- .../codebase/backend_agents_crew_departments.py.md |     2 +-
- docs/autogen/codebase/backend_agents_init_.py.md   |     2 +-
- .../codebase/backend_agents_legal_agent.py.md      |     2 +-
- .../codebase/backend_agents_medical_agent.py.md    |     2 +-
- .../backend_agents_research_assistant.py.md        |     2 +-
- .../codebase/backend_agents_test_legal_agent.py.md |     2 +-
- .../backend_agents_test_medical_agent.py.md        |     2 +-
- .../codebase/backend_agents_trading_agent.py.md    |     2 +-
- docs/autogen/codebase/backend_alembic_env.py.md    |     2 +-
- ...ersions_664fe16e33ca_add_ci_reports_table.py.md |     2 +-
- .../codebase/backend_api_dependencies.py.md        |     2 +-
- docs/autogen/codebase/backend_api_init_.py.md      |     2 +-
- .../codebase/backend_api_routes_admin.py.md        |     2 +-
- .../backend_api_routes_admin_dashboard.py.md       |     2 +-
- .../codebase/backend_api_routes_agent_tasks.py.md  |     2 +-
- .../codebase/backend_api_routes_agents.py.md       |     2 +-
- .../codebase/backend_api_routes_api_keys.py.md     |     2 +-
- .../backend_api_routes_approval_manager.py.md      |     2 +-
- .../backend_api_routes_async_task_router.py.md     |     2 +-
- .../autogen/codebase/backend_api_routes_auth.py.md |     2 +-
- .../codebase/backend_api_routes_billing_api.py.md  |     2 +-
- .../codebase/backend_api_routes_browser.py.md      |     2 +-
- .../codebase/backend_api_routes_byoc_api.py.md     |     2 +-
- .../codebase/backend_api_routes_cdc_webhooks.py.md |     2 +-
- .../autogen/codebase/backend_api_routes_chat.py.md |     2 +-
- .../codebase/backend_api_routes_ci_webhooks.py.md  |     2 +-
- .../codebase/backend_api_routes_cloud_mesh.py.md   |     2 +-
- .../codebase/backend_api_routes_codeflow.py.md     |     2 +-
- .../codebase/backend_api_routes_config.py.md       |     2 +-
- .../codebase/backend_api_routes_email.py.md        |     2 +-
- .../codebase/backend_api_routes_evolution.py.md    |     2 +-
- .../codebase/backend_api_routes_feedback.py.md     |     2 +-
- .../codebase/backend_api_routes_github.py.md       |     2 +-
- .../codebase/backend_api_routes_graph.py.md        |     2 +-
- .../codebase/backend_api_routes_init_.py.md        |     2 +-
- .../codebase/backend_api_routes_internal.py.md     |     2 +-
- .../codebase/backend_api_routes_knowledge.py.md    |     2 +-
- .../codebase/backend_api_routes_llm_gateway.py.md  |     2 +-
- .../codebase/backend_api_routes_markdown.py.md     |     2 +-
- .../codebase/backend_api_routes_marketplace.py.md  |     2 +-
- .../backend_api_routes_marketplace_endpoints.py.md |     2 +-
- .../codebase/backend_api_routes_media.py.md        |     2 +-
- .../codebase/backend_api_routes_memory.py.md       |     2 +-
- .../codebase/backend_api_routes_metrics.py.md      |     2 +-
- .../codebase/backend_api_routes_mobile_bff.py.md   |     2 +-
- .../codebase/backend_api_routes_onboarding.py.md   |     2 +-
- .../codebase/backend_api_routes_payments.py.md     |     2 +-
- .../codebase/backend_api_routes_preferences.py.md  |     2 +-
- .../codebase/backend_api_routes_repos.py.md        |     2 +-
- .../codebase/backend_api_routes_simulator.py.md    |     2 +-
- .../codebase/backend_api_routes_site_actions.py.md |     2 +-
- docs/autogen/codebase/backend_api_routes_sso.py.md |     2 +-
- .../codebase/backend_api_routes_stream.py.md       |     2 +-
- .../autogen/codebase/backend_api_routes_task.py.md |     2 +-
- .../backend_api_routes_task_workspace.py.md        |     2 +-
- .../codebase/backend_api_routes_tenant_admin.py.md |     2 +-
- .../codebase/backend_api_routes_tools_ops.py.md    |     2 +-
- .../backend_api_routes_tools_registry.py.md        |     2 +-
- .../backend_api_routes_usage_metrics.py.md         |     2 +-
- .../codebase/backend_api_routes_voice.py.md        |     2 +-
- .../backend_api_routes_websocket_agent.py.md       |     2 +-
- .../backend_api_routes_websocket_voice.py.md       |     2 +-
- .../codebase/backend_byoc_cloud_connector.py.md    |     2 +-
- .../backend_byoc_container_orchestrator.py.md      |     2 +-
- docs/autogen/codebase/backend_byoc_init_.py.md     |     2 +-
- .../codebase/backend_byoc_resource_manager.py.md   |     2 +-
- .../codebase/backend_config_byoc_limits.json.md    |     2 +-
- .../backend_config_constitutional_rules.json.md    |     2 +-
- .../codebase/backend_config_pricing_tiers.json.md  |     2 +-
- .../codebase/backend_config_routing_policy.json.md |     2 +-
- docs/autogen/codebase/backend_core_admin_god.py.md |     2 +-
- .../codebase/backend_core_admin_routes.py.md       |     2 +-
- .../codebase/backend_core_agent_orchestrator.py.md |     2 +-
- .../codebase/backend_core_api_key_middleware.py.md |     2 +-
- .../backend_core_api_key_rate_limiter.py.md        |     2 +-
- docs/autogen/codebase/backend_core_app.py.md       |     2 +-
- .../codebase/backend_core_audit_logger.py.md       |     2 +-
- .../codebase/backend_core_auth_middleware.py.md    |     2 +-
- .../codebase/backend_core_auto_remediation.py.md   |     2 +-
- .../codebase/backend_core_autocache_proxy.py.md    |     2 +-
- .../codebase/backend_core_circuit_breaker.py.md    |     2 +-
- .../backend_core_cloud_sandbox_orchestrator.py.md  |     2 +-
- .../codebase/backend_core_cloud_storage.py.md      |     2 +-
- .../codebase/backend_core_code_validator.py.md     |     2 +-
- docs/autogen/codebase/backend_core_config.py.md    |     2 +-
- docs/autogen/codebase/backend_core_constants.py.md |     2 +-
- .../codebase/backend_core_db_repository.py.md      |     2 +-
- .../codebase/backend_core_decision_engine.py.md    |     2 +-
- .../codebase/backend_core_discord_bot.py.md        |     2 +-
- .../codebase/backend_core_docker-compose.yml.md    |     2 +-
- .../codebase/backend_core_email_service.py.md      |     2 +-
- .../codebase/backend_core_error_pattern_db.py.md   |     2 +-
- .../codebase/backend_core_error_remediation.py.md  |     2 +-
- docs/autogen/codebase/backend_core_events.py.md    |     2 +-
- .../codebase/backend_core_evolution_engine.py.md   |     2 +-
- .../codebase/backend_core_factual_verifier.py.md   |     2 +-
- .../codebase/backend_core_feedback_loop.py.md      |     2 +-
- .../codebase/backend_core_free_tier_tracker.py.md  |     2 +-
- .../codebase/backend_core_gcp_firestore.py.md      |     2 +-
- .../codebase/backend_core_gcp_pubsub_queue.py.md   |     2 +-
- .../codebase/backend_core_generation_monitor.py.md |     2 +-
- .../codebase/backend_core_grpc_client.py.md        |     2 +-
- .../codebase/backend_core_health_monitor.py.md     |     2 +-
- .../backend_core_honeypot_middleware.py.md         |     2 +-
- .../backend_core_idempotency_middleware.py.md      |     2 +-
- .../codebase/backend_core_immune_system.py.md      |     2 +-
- docs/autogen/codebase/backend_core_init_.py.md     |     2 +-
- .../codebase/backend_core_input_sanitizer.py.md    |     2 +-
- docs/autogen/codebase/backend_core_intent.py.md    |     2 +-
- .../codebase/backend_core_intent_router.py.md      |     2 +-
- .../codebase/backend_core_language_router.py.md    |     2 +-
- docs/autogen/codebase/backend_core_ld_client.py.md |     2 +-
- docs/autogen/codebase/backend_core_lifespan.py.md  |     2 +-
- .../codebase/backend_core_llm_gateway.py.md        |     2 +-
- .../codebase/backend_core_logging_config.py.md     |     2 +-
- .../codebase/backend_core_mcp_allowlist.py.md      |     2 +-
- .../codebase/backend_core_microvm_sandbox.py.md    |     2 +-
- .../codebase/backend_core_multi_layer_cache.py.md  |     2 +-
- .../backend_core_observability_middleware.py.md    |     2 +-
- .../codebase/backend_core_orchestrator.py.md       |     2 +-
- .../codebase/backend_core_origin_validator.py.md   |     2 +-
- .../codebase/backend_core_output_validator.py.md   |     2 +-
- .../codebase/backend_core_pgbouncer_pool.py.md     |     2 +-
- .../codebase/backend_core_posthog_client.py.md     |     2 +-
- .../codebase/backend_core_prompt_firewall.py.md    |     2 +-
- .../codebase/backend_core_prompt_helpers.py.md     |     2 +-
- .../codebase/backend_core_rate_limiter.py.md       |     2 +-
- docs/autogen/codebase/backend_core_rbac.py.md      |     2 +-
- .../codebase/backend_core_redis_manager.py.md      |     2 +-
- .../codebase/backend_core_rollback_monitor.py.md   |     2 +-
- .../codebase/backend_core_rules_mutator.py.md      |     2 +-
- .../codebase/backend_core_schema_validator.py.md   |     2 +-
- .../codebase/backend_core_secret_vault.py.md       |     2 +-
- .../backend_core_secure_credential_store.py.md     |     2 +-
- docs/autogen/codebase/backend_core_security.py.md  |     2 +-
- .../codebase/backend_core_self_healing_agent.py.md |     2 +-
- .../codebase/backend_core_semantic_cache.py.md     |     2 +-
- docs/autogen/codebase/backend_core_services.py.md  |     2 +-
- .../codebase/backend_core_skill_graph.py.md        |     2 +-
- .../codebase/backend_core_swarm_orchestrator.py.md |     2 +-
- .../autogen/codebase/backend_core_task_queue.py.md |     2 +-
- .../backend_core_task_queue_enhanced.py.md         |     2 +-
- .../codebase/backend_core_task_router.py.md        |     2 +-
- docs/autogen/codebase/backend_core_telemetry.py.md |     2 +-
- docs/autogen/codebase/backend_core_tenant_db.py.md |     2 +-
- .../codebase/backend_core_token_budget.py.md       |     2 +-
- .../codebase/backend_core_token_deductor.py.md     |     2 +-
- .../codebase/backend_core_universal_rules.py.md    |     2 +-
- .../codebase/backend_core_upload_validator.py.md   |     2 +-
- .../backend_core_upstash_redis_queue.py.md         |     2 +-
- .../codebase/backend_core_user_profiler.py.md      |     2 +-
- docs/autogen/codebase/backend_coverage.json.md     |     6 +-
- docs/autogen/codebase/backend_database_init_.py.md |     2 +-
- ...end_database_migrations_01_initial_setup.sql.md |     2 +-
- ...kend_database_migrations_02_phase2_setup.sql.md |     2 +-
- ...grations_03_user_preferences_and_metrics.sql.md |     2 +-
- ...nd_database_migrations_04_schema_upgrade.sql.md |     2 +-
- ...database_migrations_05_seed_github_repos.sql.md |     2 +-
- ...d_database_migrations_06_referral_system.sql.md |     2 +-
- ...end_database_migrations_07_tenant_config.sql.md |     2 +-
- ...ckend_database_migrations_08_sso_configs.sql.md |     2 +-
- ...database_migrations_09_offline_sync_logs.sql.md |     2 +-
- ...atabase_migrations_10_tenant_sso_offline.sql.md |     2 +-
- .../codebase/backend_database_session.py.md        |     2 +-
- .../codebase/backend_database_storage_client.py.md |     2 +-
- .../backend_database_supabase_client.py.md         |     2 +-
- .../codebase/backend_engine_cost_optimizer.py.md   |     2 +-
- docs/autogen/codebase/backend_engine_init_.py.md   |     2 +-
- .../codebase/backend_engine_model_dispatcher.py.md |     2 +-
- .../backend_evolution_auto_skill_creator.py.md     |     2 +-
- .../backend_evolution_auto_update_manager.py.md    |     2 +-
- .../backend_evolution_dynamic_injector.py.md       |     2 +-
- .../backend_evolution_fitness_engine.py.md         |     2 +-
- .../autogen/codebase/backend_evolution_init_.py.md |     2 +-
- .../backend_evolution_master_planner.py.md         |     2 +-
- .../backend_evolution_security_sandbox.py.md       |     2 +-
- .../backend_evolution_self_evolution_agent.py.md   |     2 +-
- .../codebase/backend_evolution_skill_graph.py.md   |     2 +-
- docs/autogen/codebase/backend_fix_tests.py.md      |     2 +-
- docs/autogen/codebase/backend_init_.py.md          |     2 +-
- docs/autogen/codebase/backend_main.py.md           |     2 +-
- .../backend_memory_checkpoint_resume.py.md         |     2 +-
- .../codebase/backend_memory_chromadb_store.py.md   |     2 +-
- .../backend_memory_cloud_postgres_store.py.md      |     2 +-
- .../backend_memory_cloud_vector_store.py.md        |     2 +-
- .../codebase/backend_memory_episodic_memory.py.md  |     2 +-
- docs/autogen/codebase/backend_memory_init_.py.md   |     2 +-
- .../codebase/backend_memory_long_term_memory.py.md |    38 +-
- .../codebase/backend_memory_rag_pipeline.py.md     |     2 +-
- .../codebase/backend_memory_sliding_window.py.md   |     2 +-
- .../codebase/backend_memory_sqlite_store.py.md     |     2 +-
- .../codebase/backend_memory_summary_tree.py.md     |     2 +-
- .../codebase/backend_memory_supabase_store.py.md   |     2 +-
- .../backend_memory_vector_store_config.py.md       |     2 +-
- .../backend_middleware_auth_middleware.py.md       |     2 +-
- .../backend_middleware_chaos_injector.py.md        |     2 +-
- .../codebase/backend_middleware_idempotency.py.md  |     2 +-
- docs/autogen/codebase/backend_models_admin.py.md   |     2 +-
- docs/autogen/codebase/backend_models_api_key.py.md |     2 +-
- .../codebase/backend_models_byoc_payloads.py.md    |     2 +-
- .../codebase/backend_models_ci_report.py.md        |     2 +-
- .../codebase/backend_models_deployment_logs.py.md  |     2 +-
- .../backend_models_error_remediation.py.md         |     2 +-
- .../codebase/backend_models_evolution.py.md        |     2 +-
- docs/autogen/codebase/backend_models_init_.py.md   |     2 +-
- .../backend_models_local_model_handler.py.md       |     2 +-
- .../codebase/backend_models_pending_tasks.py.md    |     2 +-
- .../codebase/backend_models_shared_workspace.py.md |     2 +-
- .../backend_models_transaction_ledger.py.md        |     2 +-
- .../backend_models_voice_interaction.py.md         |     2 +-
- docs/autogen/codebase/backend_models_wallet.py.md  |     2 +-
- .../codebase/backend_monitoring_cost_auditor.py.md |     2 +-
- .../codebase/backend_monitoring_init_.py.md        |     2 +-
- .../codebase/backend_p2p_credit_system.py.md       |     2 +-
- docs/autogen/codebase/backend_p2p_init_.py.md      |     2 +-
- .../codebase/backend_p2p_secure_tunnel.py.md       |     2 +-
- docs/autogen/codebase/backend_pyproject.toml.md    |     2 +-
- docs/autogen/codebase/backend_reports_init_.py.md  |     2 +-
- .../backend_reports_optimization_engine.py.md      |     2 +-
- .../codebase/backend_run_roundtrip_tests.py.md     |     2 +-
- docs/autogen/codebase/backend_scout_init_.py.md    |     2 +-
- .../backend_scout_knowledge_extractor.py.md        |     2 +-
- .../codebase/backend_scout_web_crawler_agent.py.md |     2 +-
- .../codebase/backend_scripts_check_ollama.py.md    |     2 +-
- docs/autogen/codebase/backend_scripts_init_.py.md  |     2 +-
- .../codebase/backend_scripts_load_seed_data.py.md  |     2 +-
- .../backend_scripts_run_dependency_check.py.md     |     2 +-
- .../backend_scripts_seed_tools_registry.py.md      |     2 +-
- .../backend_scripts_self_healing_tests.py.md       |     2 +-
- docs/autogen/codebase/backend_skills_init_.py.md   |     2 +-
- .../codebase/backend_skills_provisioner.py.md      |     2 +-
- .../codebase/backend_skills_skill_registry.py.md   |     2 +-
- .../codebase/backend_storage_asset_manager.py.md   |     2 +-
- docs/autogen/codebase/backend_storage_init_.py.md  |     2 +-
- .../backend_storage_r2_storage_client.py.md        |     2 +-
- .../backend_tests_agents_test_legal_agent.py.md    |     2 +-
- .../backend_tests_agents_test_medical_agent.py.md  |     2 +-
- ...kend_tests_agents_test_research_assistant.py.md |     2 +-
- .../backend_tests_agents_test_trading_agent.py.md  |     2 +-
- .../backend_tests_byoc_test_cloud_connector.py.md  |     2 +-
- ...nd_tests_byoc_test_container_orchestrator.py.md |     2 +-
- .../backend_tests_byoc_test_resource_manager.py.md |     2 +-
- docs/autogen/codebase/backend_tests_conftest.py.md |     2 +-
- .../backend_tests_engine_test_cost_optimizer.py.md |     2 +-
- ...ackend_tests_engine_test_model_dispatcher.py.md |     2 +-
- docs/autogen/codebase/backend_tests_init_.py.md    |     2 +-
- ...ackend_tests_monitoring_test_cost_auditor.py.md |     2 +-
- .../backend_tests_p2p_test_credit_system.py.md     |     2 +-
- .../backend_tests_p2p_test_secure_tunnel.py.md     |     2 +-
- ...kend_tests_scout_test_knowledge_extractor.py.md |     2 +-
- ...ackend_tests_scout_test_web_crawler_agent.py.md |     2 +-
- .../backend_tests_test_adaptive_engine.py.md       |     2 +-
- .../codebase/backend_tests_test_admin_god.py.md    |     2 +-
- .../codebase/backend_tests_test_admin_models.py.md |     2 +-
- .../codebase/backend_tests_test_admin_routes.py.md |     2 +-
- .../codebase/backend_tests_test_advanced.py.md     |     2 +-
- .../backend_tests_test_agent_department.py.md      |     2 +-
- .../backend_tests_test_agent_departments.py.md     |     2 +-
- .../backend_tests_test_agent_orchestrator.py.md    |     2 +-
- ...ackend_tests_test_agents_crew_departments.py.md |     2 +-
- docs/autogen/codebase/backend_tests_test_api.py.md |     2 +-
- .../codebase/backend_tests_test_api_chat.py.md     |     2 +-
- .../codebase/backend_tests_test_api_keys.py.md     |     2 +-
- .../backend_tests_test_api_new_endpoints.py.md     |     2 +-
- .../codebase/backend_tests_test_api_router.py.md   |     2 +-
- .../codebase/backend_tests_test_audit_logger.py.md |     2 +-
- .../backend_tests_test_auth_middleware.py.md       |     2 +-
- .../codebase/backend_tests_test_auth_routes.py.md  |     2 +-
- .../backend_tests_test_auto_fix_trigger.py.md      |     2 +-
- .../backend_tests_test_auto_skill_creator.py.md    |     2 +-
- .../backend_tests_test_autonomous_agent.py.md      |     2 +-
- .../codebase/backend_tests_test_bangla_nlp.py.md   |     2 +-
- .../codebase/backend_tests_test_bangla_voice.py.md |     2 +-
- .../backend_tests_test_billing_system.py.md        |     2 +-
- .../codebase/backend_tests_test_brain.py.md        |     2 +-
- .../backend_tests_test_browser_credentials.py.md   |     2 +-
- .../backend_tests_test_byoc_endpoints.py.md        |     2 +-
- .../codebase/backend_tests_test_chaos_worker.py.md |     2 +-
- .../backend_tests_test_checkpoint_resume.py.md     |     2 +-
- .../backend_tests_test_circuit_breaker.py.md       |     2 +-
- .../backend_tests_test_cloud_sandbox.py.md         |     2 +-
- .../backend_tests_test_cloud_storage.py.md         |     2 +-
- .../backend_tests_test_code_validator.py.md        |     2 +-
- .../backend_tests_test_collaborative_editor.py.md  |     2 +-
- .../codebase/backend_tests_test_config.py.md       |     2 +-
- .../backend_tests_test_config_additional.py.md     |     2 +-
- .../backend_tests_test_config_coverage.py.md       |     2 +-
- .../codebase/backend_tests_test_constants.py.md    |     2 +-
- .../backend_tests_test_context_and_actions.py.md   |     2 +-
- .../autogen/codebase/backend_tests_test_core.py.md |     2 +-
- .../codebase/backend_tests_test_core_smoke.py.md   |     2 +-
- .../backend_tests_test_coverage_gaps.py.md         |     2 +-
- .../codebase/backend_tests_test_crew_mcp.py.md     |     2 +-
- ...ackend_tests_test_database_storage_client.py.md |     2 +-
- .../backend_tests_test_db_repository.py.md         |     2 +-
- docs/autogen/codebase/backend_tests_test_e2e.py.md |     2 +-
- .../codebase/backend_tests_test_e2e_media.py.md    |     2 +-
- .../codebase/backend_tests_test_email_agent.py.md  |     2 +-
- .../backend_tests_test_email_service.py.md         |     2 +-
- .../backend_tests_test_episodic_memory.py.md       |     2 +-
- .../backend_tests_test_error_remediation.py.md     |     2 +-
- .../backend_tests_test_evolution_engine.py.md      |     2 +-
- .../backend_tests_test_evolution_pipeline.py.md    |     2 +-
- .../backend_tests_test_factual_verifier.py.md      |     2 +-
- .../backend_tests_test_feedback_loop.py.md         |     2 +-
- .../backend_tests_test_firebase_integration.py.md  |     2 +-
- .../backend_tests_test_fitness_engine.py.md        |     2 +-
- .../backend_tests_test_free_tier_tracker.py.md     |     2 +-
- .../backend_tests_test_gcp_integration.py.md       |     2 +-
- .../backend_tests_test_generation_monitor.py.md    |     2 +-
- .../codebase/backend_tests_test_github_agent.py.md |     2 +-
- .../codebase/backend_tests_test_graph_routes.py.md |     2 +-
- .../backend_tests_test_graph_service.py.md         |     2 +-
- .../codebase/backend_tests_test_grpc_client.py.md  |     2 +-
- .../backend_tests_test_hallucination_guard.py.md   |     2 +-
- .../codebase/backend_tests_test_health.py.md       |     2 +-
- .../backend_tests_test_health_monitor.py.md        |     2 +-
- .../backend_tests_test_health_monitor_routes.py.md |     2 +-
- .../backend_tests_test_honeypot_middleware.py.md   |     2 +-
- ...backend_tests_test_idempotency_middleware.py.md |     2 +-
- .../backend_tests_test_immune_system.py.md         |     2 +-
- .../backend_tests_test_immune_system_scanner.py.md |     2 +-
- .../backend_tests_test_input_sanitizer.py.md       |     2 +-
- .../backend_tests_test_language_router.py.md       |     2 +-
- .../codebase/backend_tests_test_llm_gateway.py.md  |     2 +-
- .../backend_tests_test_llm_gateway_coverage.py.md  |     2 +-
- .../backend_tests_test_long_term_memory.py.md      |     2 +-
- .../backend_tests_test_markdown_export.py.md       |     2 +-
- .../backend_tests_test_marketplace_agent.py.md     |     2 +-
- .../backend_tests_test_mcp_allowlist.py.md         |     2 +-
- .../codebase/backend_tests_test_mcp_server.py.md   |     2 +-
- ...ackend_tests_test_mcp_servers_integration.py.md |     2 +-
- .../codebase/backend_tests_test_media_r2.py.md     |     2 +-
- ...kend_tests_test_middleware_chaos_injector.py.md |     2 +-
- .../codebase/backend_tests_test_migrations.py.md   |     2 +-
- ...kend_tests_test_migrations_and_onboarding.py.md |     2 +-
- .../codebase/backend_tests_test_mobile_e2e.py.md   |     2 +-
- .../backend_tests_test_model_registry.py.md        |     2 +-
- .../backend_tests_test_model_router_unit.py.md     |     2 +-
- .../backend_tests_test_model_trainer.py.md         |     2 +-
- .../backend_tests_test_models_ci_report.py.md      |     2 +-
- .../backend_tests_test_models_evolution.py.md      |     2 +-
- .../codebase/backend_tests_test_monitoring.py.md   |     2 +-
- .../backend_tests_test_multi_account_rotator.py.md |     2 +-
- .../codebase/backend_tests_test_multicloud.py.md   |     2 +-
- .../backend_tests_test_new_endpoints_sprint5.py.md |     2 +-
- .../backend_tests_test_new_interfaces.py.md        |     2 +-
- .../backend_tests_test_new_tools_sprint5.py.md     |     2 +-
- .../backend_tests_test_optimization_engine.py.md   |     2 +-
- .../backend_tests_test_output_validator.py.md      |     2 +-
- ...ackend_tests_test_parallel_agent_executor.py.md |     2 +-
- .../codebase/backend_tests_test_payments.py.md     |     2 +-
- ...ckend_tests_test_performance_aware_router.py.md |     2 +-
- .../backend_tests_test_pgbouncer_pool.py.md        |     2 +-
- .../codebase/backend_tests_test_posthog.py.md      |     2 +-
- .../codebase/backend_tests_test_pr_reviewer.py.md  |     2 +-
- .../backend_tests_test_prod_docs_security.py.md    |     2 +-
- ...sts_test_production_readiness_integration.py.md |     2 +-
- .../backend_tests_test_prompt_firewall.py.md       |     2 +-
- .../autogen/codebase/backend_tests_test_rbac.py.md |     2 +-
- ...backend_tests_test_reasoning_orchestrator.py.md |     2 +-
- .../backend_tests_test_repo_discovery.py.md        |     2 +-
- .../backend_tests_test_resource_catalog.py.md      |     2 +-
- .../autogen/codebase/backend_tests_test_rlhf.py.md |     2 +-
- ...kend_tests_test_sandbox_orchestration_run.py.md |     2 +-
- .../backend_tests_test_schema_validator.py.md      |     2 +-
- .../codebase/backend_tests_test_secret_vault.py.md |     2 +-
- ...ackend_tests_test_secure_credential_store.py.md |     2 +-
- .../backend_tests_test_security_middleware.py.md   |     2 +-
- .../backend_tests_test_security_regression.py.md   |     2 +-
- .../backend_tests_test_self_evolution_agent.py.md  |     2 +-
- .../backend_tests_test_simulator_browser_api.py.md |     2 +-
- .../codebase/backend_tests_test_skill_graph.py.md  |     2 +-
- .../backend_tests_test_skill_recommender.py.md     |     2 +-
- .../backend_tests_test_sliding_window_memory.py.md |     2 +-
- .../backend_tests_test_sprint_c_tools.py.md        |     2 +-
- .../codebase/backend_tests_test_sprint_g.py.md     |     2 +-
- .../backend_tests_test_stealth_networking.py.md    |     2 +-
- .../codebase/backend_tests_test_stream.py.md       |     2 +-
- .../backend_tests_test_style_learner.py.md         |     2 +-
- ...kend_tests_test_supabase_schema_bootstrap.py.md |     2 +-
- .../backend_tests_test_supabase_store.py.md        |     2 +-
- .../backend_tests_test_swarm_orchestrator.py.md    |     2 +-
- .../backend_tests_test_task_endpoints.py.md        |     2 +-
- .../codebase/backend_tests_test_task_queue.py.md   |     2 +-
- .../codebase/backend_tests_test_task_router.py.md  |     2 +-
- .../codebase/backend_tests_test_telegram_bot.py.md |     2 +-
- .../codebase/backend_tests_test_telemetry.py.md    |     2 +-
- .../backend_tests_test_tenant_rate_limiter.py.md   |     2 +-
- .../backend_tests_test_universal_rules.py.md       |     2 +-
- .../backend_tests_test_upstash_redis.py.md         |     2 +-
- docs/autogen/codebase/backend_tests_test_uss.py.md |     2 +-
- .../backend_tests_test_video_generator.py.md       |     2 +-
- .../codebase/backend_tests_test_vision_agent.py.md |     2 +-
- .../codebase/backend_tests_test_voice_stream.py.md |     2 +-
- .../codebase/backend_tests_test_vpn_switcher.py.md |     2 +-
- .../codebase/backend_tests_test_vscode_e2e.py.md   |     2 +-
- .../codebase/backend_tests_test_web_fallback.py.md |     2 +-
- ...d_tests_tools_test_auto_coverage_improver.py.md |     2 +-
- ...kend_tests_tools_test_auto_test_generator.py.md |     2 +-
- ...kend_tests_tools_test_code_smell_detector.py.md |     2 +-
- .../backend_tests_tools_test_cot_reasoner.py.md    |     2 +-
- ...backend_tests_tools_test_coverage_auditor.py.md |     2 +-
- ...d_tests_tools_test_knowledge_base_indexer.py.md |   274 +
- ...backend_tests_tools_test_multilingual_tts.py.md |   298 +
- ...nd_tests_tools_test_viral_referral_engine.py.md |   393 +
- .../backend_tests_utils_test_api_tracker.py.md     |     2 +-
- .../backend_tests_workers_test_celery_app.py.md    |     2 +-
- .../backend_tools_3d_model_generator.py.md         |     2 +-
- .../codebase/backend_tools_agent_tools.py.md       |     2 +-
- .../backend_tools_ai_federation_protocol.py.md     |     2 +-
- .../backend_tools_ai_pair_programmer.py.md         |     2 +-
- .../codebase/backend_tools_api_gateway.py.md       |     2 +-
- .../backend_tools_auto_coverage_improver.py.md     |     2 +-
- .../codebase/backend_tools_auto_pr_pipeline.py.md  |     2 +-
- .../backend_tools_auto_test_generator.py.md        |     2 +-
- .../backend_tools_bandwidth_optimizer.py.md        |     2 +-
- .../backend_tools_bangla_ai_connector.py.md        |     2 +-
- .../codebase/backend_tools_bangla_nlp.py.md        |     2 +-
- .../codebase/backend_tools_bangla_voice.py.md      |     2 +-
- .../codebase/backend_tools_benchmark_agent.py.md   |     2 +-
- .../backend_tools_bengali_ocr_converter.py.md      |     2 +-
- .../codebase/backend_tools_blockchain_agent.py.md  |     2 +-
- .../autogen/codebase/backend_tools_bootstrap.py.md |     2 +-
- .../codebase/backend_tools_browser_agent.py.md     |     2 +-
- .../codebase/backend_tools_browser_stealth.py.md   |     2 +-
- .../backend_tools_checkpoint_manager.py.md         |     2 +-
- docs/autogen/codebase/backend_tools_cli.py.md      |     2 +-
- .../backend_tools_cloud_sandbox_orchestrator.py.md |     2 +-
- .../backend_tools_code_smell_detector.py.md        |    11 +-
- .../codebase/backend_tools_codebase_exporter.py.md |     2 +-
- .../backend_tools_collaborative_editor.py.md       |     2 +-
- .../codebase/backend_tools_comment_thread_ai.py.md |     2 +-
- .../codebase/backend_tools_computer_agent.py.md    |     2 +-
- .../backend_tools_conversation_manager.py.md       |     2 +-
- .../codebase/backend_tools_cost_auditor.py.md      |     2 +-
- .../codebase/backend_tools_cot_reasoner.py.md      |     2 +-
- .../codebase/backend_tools_coverage_auditor.py.md  |     2 +-
- .../backend_tools_dependency_manager_agent.py.md   |     2 +-
- .../backend_tools_diagram_to_architecture.py.md    |     2 +-
- .../codebase/backend_tools_docker_sandbox.py.md    |     2 +-
- .../codebase/backend_tools_domain_adapter.py.md    |     2 +-
- .../codebase/backend_tools_email_agent.py.md       |     2 +-
- .../codebase/backend_tools_ensemble_router.py.md   |     2 +-
- .../codebase/backend_tools_fuzz_sandbox.py.md      |     2 +-
- .../codebase/backend_tools_game_dev_agent.py.md    |     2 +-
- .../backend_tools_gcp_cloud_functions.py.md        |     2 +-
- .../backend_tools_git_knowledge_extractor.py.md    |     2 +-
- .../codebase/backend_tools_github_agent.py.md      |     2 +-
- .../codebase/backend_tools_graph_service.py.md     |     2 +-
- .../backend_tools_headless_agent_registry.py.md    |     2 +-
- .../codebase/backend_tools_health_checker.py.md    |     2 +-
- .../codebase/backend_tools_image_generator.py.md   |     2 +-
- .../codebase/backend_tools_image_to_code.py.md     |     2 +-
- docs/autogen/codebase/backend_tools_init_.py.md    |     2 +-
- .../backend_tools_knowledge_base_indexer.py.md     |     2 +-
- .../backend_tools_langchain_agent_example.py.md    |     2 +-
- .../codebase/backend_tools_legal_agent.py.md       |     2 +-
- .../backend_tools_local_ocr_extractor.py.md        |     2 +-
- .../codebase/backend_tools_local_search_rag.py.md  |     2 +-
- .../codebase/backend_tools_marketplace_agent.py.md |     2 +-
- .../codebase/backend_tools_mcp_cloud_deploy.py.md  |    14 +-
- .../codebase/backend_tools_mcp_github_cicd.py.md   |     2 +-
- .../codebase/backend_tools_mcp_server.py.md        |     2 +-
- .../codebase/backend_tools_mcp_supabase.py.md      |     2 +-
- .../codebase/backend_tools_mcp_workspace.py.md     |     2 +-
- .../codebase/backend_tools_medical_agent.py.md     |     2 +-
- .../codebase/backend_tools_meta_architect.py.md    |     2 +-
- .../codebase/backend_tools_model_trainer.py.md     |     2 +-
- .../backend_tools_monthly_cost_reporter.py.md      |     2 +-
- .../backend_tools_multi_account_rotator.py.md      |     2 +-
- .../codebase/backend_tools_multilingual_tts.py.md  |     2 +-
- .../codebase/backend_tools_music_generator.py.md   |     2 +-
- .../codebase/backend_tools_offline_mode.py.md      |     2 +-
- .../backend_tools_on_premise_deployer.py.md        |     2 +-
- .../backend_tools_parallel_agent_executor.py.md    |     2 +-
- .../codebase/backend_tools_pdf_to_sdk.py.md        |     2 +-
- .../codebase/backend_tools_plan_sorter.py.md       |     2 +-
- .../backend_tools_playwright_browser_agent.py.md   |     2 +-
- .../codebase/backend_tools_pr_reviewer.py.md       |     2 +-
- .../codebase/backend_tools_pre_commit_ai.py.md     |     2 +-
- .../codebase/backend_tools_preference_memory.py.md |     2 +-
- .../backend_tools_presentation_generator.py.md     |     2 +-
- .../codebase/backend_tools_proxy_manager.py.md     |     2 +-
- .../codebase/backend_tools_repo_deep_indexer.py.md |     2 +-
- .../backend_tools_repo_discovery_agent.py.md       |     2 +-
- .../codebase/backend_tools_resource_catalog.py.md  |     2 +-
- .../codebase/backend_tools_rlhf_pipeline.py.md     |     2 +-
- .../codebase/backend_tools_safe_executor.py.md     |     2 +-
- .../codebase/backend_tools_scientific_agent.py.md  |     2 +-
- .../codebase/backend_tools_seed_database.py.md     |     2 +-
- .../codebase/backend_tools_self_planner.py.md      |     2 +-
- .../codebase/backend_tools_skill_recommender.py.md |     2 +-
- .../codebase/backend_tools_sso_integrator.py.md    |     2 +-
- .../backend_tools_stealth_http_client.py.md        |     2 +-
- .../codebase/backend_tools_style_learner.py.md     |     2 +-
- .../codebase/backend_tools_telegram_bot.py.md      |     2 +-
- .../backend_tools_tenant_rate_limiter.py.md        |     2 +-
- .../backend_tools_test_3d_model_generator.py.md    |     2 +-
- ...end_tools_test_cloud_sandbox_orchestrator.py.md |     2 +-
- .../codebase/backend_tools_trading_agent.py.md     |     2 +-
- .../codebase/backend_tools_video_generator.py.md   |     2 +-
- .../backend_tools_viral_referral_engine.py.md      |     2 +-
- .../codebase/backend_tools_vision_agent.py.md      |     2 +-
- docs/autogen/codebase/backend_tools_voice.py.md    |     2 +-
- .../codebase/backend_tools_voice_coder.py.md       |     2 +-
- .../codebase/backend_tools_vpn_switcher.py.md      |     2 +-
- .../backend_tools_vulnerability_predictor.py.md    |     2 +-
- .../backend_tools_web_fallback_agent.py.md         |     2 +-
- .../codebase/backend_utils_api_tracker.py.md       |     2 +-
- .../codebase/backend_utils_environment.py.md       |     2 +-
- .../codebase/backend_utils_firestore_helpers.py.md |     2 +-
- .../codebase/backend_utils_http_client.py.md       |     8 +-
- docs/autogen/codebase/backend_utils_init_.py.md    |     2 +-
- .../codebase/backend_utils_json_helpers.py.md      |     2 +-
- .../codebase/backend_utils_timestamps.py.md        |     2 +-
- docs/autogen/codebase/backend_uv.lock.md           |     2 +-
- .../codebase/backend_workers_celery_app.py.md      |     2 +-
- .../codebase/backend_workers_chaos_worker.py.md    |     2 +-
- .../codebase/config_.pre-commit-config.yaml.md     |     2 +-
- docs/autogen/codebase/config_audit-rules.yml.md    |     2 +-
- .../codebase/config_compliance-rules.yml.md        |     2 +-
- docs/autogen/codebase/config_docker-limits.yml.md  |     2 +-
- docs/autogen/codebase/config_firebase.json.md      |     2 +-
- .../codebase/config_firestore.indexes.json.md      |     2 +-
- docs/autogen/codebase/config_kilo.json.md          |     2 +-
- .../codebase/config_promptfooconfig.yaml.md        |     2 +-
- docs/autogen/codebase/config_proxy_list.json.md    |     2 +-
- .../autogen/codebase/config_routing_policy.json.md |     2 +-
- docs/autogen/codebase/config_vercel.json.md        |     2 +-
- docs/autogen/codebase/coverage.json.md             |     2 +-
- docs/autogen/codebase/coverage.toml.md             |     2 +-
- docs/autogen/codebase/docker-compose.yml.md        |     2 +-
- .../codebase/evolution_auto_skill_creator.py.md    |     2 +-
- .../autogen/codebase/evolution_daily_learner.py.md |     2 +-
- .../codebase/evolution_evolution_engine.py.md      |     2 +-
- .../codebase/evolution_evolution_react_agent.py.md |     2 +-
- docs/autogen/codebase/evolution_self_updater.py.md |     2 +-
- docs/autogen/codebase/find_duplicate_files.py.md   |     2 +-
- docs/autogen/codebase/find_duplicate_tests.py.md   |     2 +-
- .../infrastructure_check_deploy_gate.py.md         |     2 +-
- ...infrastructure_cloudflare_enhanced-worker.js.md |     2 +-
- .../infrastructure_cloudflare_worker.js.md         |     2 +-
- .../infrastructure_cloudflare_wrangler.toml.md     |     2 +-
- .../infrastructure_cloudrun_autoscale.yaml.md      |     2 +-
- .../infrastructure_cloudrun_multi_region.yaml.md   |     2 +-
- ...functions_firebase_functions_v1_README_BD.md.md |     2 +-
- ...unctions_firebase_functions_v1_api-router.js.md |     2 +-
- ..._firebase_functions_v1_deployment-monitor.js.md |     2 +-
- ...ctions_firebase_functions_v1_health-smart.js.md |     2 +-
- ...ase_functions_firebase_functions_v1_index.js.md |     2 +-
- ...irebase_functions_v1_lib_chatClassifier.d.ts.md |     2 +-
- ..._firebase_functions_v1_lib_chatClassifier.js.md |     2 +-
- ...firebase_functions_v1_lib_email_handler.d.ts.md |     2 +-
- ...s_firebase_functions_v1_lib_email_handler.js.md |     2 +-
- ...nctions_firebase_functions_v1_lib_index.d.ts.md |     2 +-
- ...functions_firebase_functions_v1_lib_index.js.md |     2 +-
- ..._firebase_functions_v1_lib_scrapeEngine.d.ts.md |     2 +-
- ...ns_firebase_functions_v1_lib_scrapeEngine.js.md |     2 +-
- ...e_functions_v1_lib_scrapeHistoryManager.d.ts.md |     2 +-
- ...ase_functions_v1_lib_scrapeHistoryManager.js.md |     2 +-
- ...functions_firebase_functions_v1_package.json.md |     2 +-
- ...ons_firebase_functions_v1_providers-smart.js.md |     2 +-
- ...se_functions_v1_server-connection-monitor.js.md |     2 +-
- ..._firebase_functions_v1_src_chatClassifier.ts.md |     2 +-
- ...dataconnect-admin-generated_esm_index.esm.js.md |     2 +-
- ...dataconnect-admin-generated_esm_package.json.md |     2 +-
- ...src_dataconnect-admin-generated_index.cjs.js.md |     2 +-
- ...1_src_dataconnect-admin-generated_index.d.ts.md |     2 +-
- ...src_dataconnect-admin-generated_package.json.md |     2 +-
- ...s_firebase_functions_v1_src_email_handler.ts.md |     2 +-
- ...functions_firebase_functions_v1_src_index.ts.md |     2 +-
- ...ns_firebase_functions_v1_src_scrapeEngine.ts.md |     2 +-
- ...ase_functions_v1_src_scrapeHistoryManager.ts.md |     2 +-
- ..._firebase_functions_v1_src_scrapeSchema.yaml.md |     2 +-
- ...functions_firebase_functions_v1_swagger.yaml.md |     2 +-
- ...tions_firebase_functions_v1_system-health.js.md |     2 +-
- ...unctions_firebase_functions_v1_tsconfig.json.md |     2 +-
- ...irebase_functions_v1_utils_externalClient.js.md |     2 +-
- ...rastructure_firebase_functions_ocrTrigger.ts.md |     2 +-
- ...ure_monitoring_docker-compose.monitoring.yml.md |     2 +-
- ...astructure_monitoring_grafana_dashboard.json.md |     2 +-
- ...cture_terraform_root_cause_analysis_agent.py.md |     2 +-
- ..._terraform_test_root_cause_analysis_agent.py.md |     2 +-
- .../codebase/infrastructure_vitest-report.json.md  |     2 +-
- docs/autogen/codebase/package.json.md              |     2 +-
- .../codebase/packages_shared-types_package.json.md |     2 +-
- .../packages_shared-types_src_conversation.ts.md   |     2 +-
- .../codebase/packages_shared-types_src_index.ts.md |     2 +-
- .../packages_shared-types_src_message.ts.md        |     2 +-
- .../packages_shared-types_tsconfig.json.md         |     2 +-
- .../packages_ui-components_package.json.md         |     2 +-
- .../packages_ui-components_src_ChatBubble.tsx.md   |     2 +-
- .../packages_ui-components_src_index.ts.md         |     2 +-
- .../packages_ui-components_tsconfig.json.md        |     2 +-
- docs/autogen/codebase/playwright-ct.config.ts.md   |     2 +-
- docs/autogen/codebase/pnpm-lock.yaml.md            |     2 +-
- docs/autogen/codebase/pnpm-workspace.yaml.md       |     2 +-
- docs/autogen/codebase/scratch_job_details.json.md  |     2 +-
- docs/autogen/codebase/scratch_smoke_check.py.md    |     2 +-
- .../scratch_supremeai_skill_ecosystem_app.py.md    |     2 +-
- ...ratch_supremeai_skill_ecosystem_generator.py.md |     2 +-
- ..._supremeai_skill_ecosystem_sample_skill.json.md |     2 +-
- ...ch_supremeai_skill_ecosystem_skill_schema.py.md |     2 +-
- .../codebase/scratch_sync_gsm_secrets.py.md        |     2 +-
- docs/autogen/codebase/scratch_update_vault.py.md   |     2 +-
- .../autogen/codebase/scratch_update_vault_r2.py.md |     2 +-
- .../codebase/scratch_verify_project_health.py.md   |     2 +-
- .../codebase/scripts_add_bangla_comments.py.md     |     2 +-
- .../codebase/scripts_aggregate_context.py.md       |     2 +-
- ...scripts_backup_auto_cross_cloud_replicate.py.md |     2 +-
- .../scripts_backup_auto_firestore_backup.py.md     |     2 +-
- .../scripts_benchmark_perf_benchmark.py.md         |     2 +-
- .../codebase/scripts_bots_auto_alert_bot.py.md     |     2 +-
- .../scripts_bots_auto_daily_standup_bot.py.md      |     2 +-
- .../codebase/scripts_code_smell_detector.py.md     |     2 +-
- docs/autogen/codebase/scripts_codebase_to_md.py.md |     2 +-
- .../codebase/scripts_codegraph_integration.py.md   |     2 +-
- .../codebase/scripts_commit_supreme_ci.yml.md      |     2 +-
- docs/autogen/codebase/scripts_config_audit.py.md   |     2 +-
- .../scripts_core_engine_multicatalog_search.py.md  |     2 +-
- .../codebase/scripts_core_engine_tool_ranker.py.md |     2 +-
- .../codebase/scripts_create_test_admin.py.md       |     2 +-
- .../autogen/codebase/scripts_db_auto_migrate.py.md |     2 +-
- docs/autogen/codebase/scripts_db_auto_seed.py.md   |     2 +-
- .../autogen/codebase/scripts_docker_ai_guard.py.md |     2 +-
- ...ipts_evolution_auto_marketing_skill_forge.py.md |     2 +-
- docs/autogen/codebase/scripts_fix_mypy.py.md       |     2 +-
- docs/autogen/codebase/scripts_fuzz_sandbox.py.md   |     2 +-
- .../scripts_generate_codebase_markdown.py.md       |     2 +-
- ...scripts_generate_codebase_single_markdown.py.md |     2 +-
- docs/autogen/codebase/scripts_generate_md.py.md    |     2 +-
- .../codebase/scripts_generate_smart_docs.py.md     |     2 +-
- docs/autogen/codebase/scripts_k6_load_test.js.md   |     2 +-
- docs/autogen/codebase/scripts_locustfile.py.md     |     2 +-
- docs/autogen/codebase/scripts_migrate.py.md        |     2 +-
- .../codebase/scripts_multi_model_validator.py.md   |     2 +-
- ...scripts_orchestrator_auto_budget_guardian.py.md |     2 +-
- docs/autogen/codebase/scripts_profile_memory.py.md |     2 +-
- .../scripts_quality_auto_dead_code_remover.py.md   |     2 +-
- .../scripts_quality_auto_improve_coverage.py.md    |     2 +-
- .../scripts_quality_auto_refactor_suggester.py.md  |     2 +-
- ...cripts_quality_check_ollama_test_coverage.py.md |     2 +-
- .../scripts_resource_collection_awesome_go.py.md   |     2 +-
- ...cripts_resource_collection_awesome_python.py.md |     2 +-
- ...ts_resource_collection_awesome_selfhosted.py.md |     2 +-
- ...ripts_resource_collection_base_api_client.py.md |     2 +-
- .../scripts_resource_collection_base_scraper.py.md |     2 +-
- ...pts_resource_collection_ossinsight_client.py.md |     2 +-
- ...ipts_resource_collection_ossinsight_init_.py.md |     2 +-
- ...ripts_resource_collection_ossinsight_test.py.md |     2 +-
- .../scripts_resource_collection_run_all.py.md      |     2 +-
- ...ts_resource_collection_run_all_collectors.py.md |     2 +-
- ...ripts_resource_scraping_awesome_go_scrape.py.md |     2 +-
- ...s_resource_scraping_awesome_python_scrape.py.md |     2 +-
- ...source_scraping_awesome_selfhosted_scrape.py.md |     2 +-
- .../codebase/scripts_run_all_collectors.py.md      |     2 +-
- docs/autogen/codebase/scripts_safety_guard.py.md   |     2 +-
- .../scripts_security_auto_find_blindspots.py.md    |     2 +-
- .../scripts_security_auto_secret_rotate.py.md      |     2 +-
- .../scripts_security_check_dependencies.py.md      |     2 +-
- .../codebase/scripts_security_code-quality.yml.md  |     2 +-
- ...scripts_security_dependency-health-check.yml.md |     2 +-
- .../codebase/scripts_security_find_dead_code.py.md |     2 +-
- docs/autogen/codebase/scripts_seed_repos.py.md     |     2 +-
- .../autogen/codebase/scripts_setup_ci_runner.py.md |     2 +-
- .../codebase/scripts_setup_firebase_admin.py.md    |     2 +-
- docs/autogen/codebase/scripts_skill_loader.py.md   |     2 +-
- .../codebase/scripts_supreme-config-audit.py.md    |     2 +-
- .../codebase/scripts_supreme-docker-analyzer.py.md |     2 +-
- .../codebase/scripts_supreme-risk-scorer.py.md     |     2 +-
- .../codebase/scripts_supreme_context_builder.py.md |     2 +-
- .../scripts_tenant_auto_tenant_health_report.py.md |     2 +-
- .../scripts_tenant_auto_tenant_setup.py.md         |     2 +-
- docs/autogen/codebase/scripts_test_bangla.py.md    |     2 +-
- docs/autogen/codebase/scripts_test_read.py.md      |     2 +-
- docs/autogen/codebase/security-scan.yml.md         |     2 +-
- .../codebase/skills_dynamic_csv_exporter.py.md     |     2 +-
- .../codebase/skills_dynamic_text_summarizer.py.md  |     2 +-
- .../codebase/skills_dynamic_web_scraper.py.md      |     2 +-
- docs/autogen/codebase/skills_init_.py.md           |     2 +-
- docs/autogen/codebase/skills_installer.py.md       |     2 +-
- docs/autogen/codebase/skills_marketplace.py.md     |     2 +-
- docs/autogen/codebase/skills_registry.py.md        |     2 +-
- docs/autogen/codebase/skills_schema.py.md          |     2 +-
- .../codebase/test-results_.last-run.json.md        |     2 +-
- .../codebase/tests_e2e_admin-dashboard.spec.ts.md  |     2 +-
- docs/autogen/codebase/tests_e2e_chat.spec.ts.md    |     2 +-
- .../codebase/tests_e2e_playwright.config.ts.md     |     2 +-
- docs/autogen/codebase/tests_e2e_visual.spec.ts.md  |     2 +-
- docs/autogen/codebase/tests_test_tenant_di.py.md   |     2 +-
- docs/autogen/codebase/tools_cache_cleanup.py.md    |     2 +-
- .../tools_vscode-extension_ARCHITECTURE_BN.md.md   |     2 +-
- ...vscode-extension_AdminMetricsController.java.md |     2 +-
- ...s_vscode-extension_CodebaseAuditService.java.md |     2 +-
- ...ools_vscode-extension_FeatureDefinition.java.md |     2 +-
- ...ode-extension_FeatureRegistryController.java.md |     2 +-
- ...vscode-extension_FeatureRegistryService.java.md |     2 +-
- .../tools_vscode-extension_GlobalMetrics.java.md   |     2 +-
- ...s_vscode-extension_GlobalMetricsService.java.md |     2 +-
- ...ols_vscode-extension_INTEGRATION_GUIDE_BN.md.md |     2 +-
- .../codebase/tools_vscode-extension_README.md.md   |     2 +-
- .../tools_vscode-extension_README_BN.md.md         |     2 +-
- .../tools_vscode-extension_jest.config.js.md       |     2 +-
- .../tools_vscode-extension_package.json.md         |     2 +-
- .../tools_vscode-extension_package.nls.bn.json.md  |     2 +-
- .../tools_vscode-extension_src_agentDetector.ts.md |     2 +-
- .../tools_vscode-extension_src_ai_AIService.ts.md  |     2 +-
- ...de-extension_src_ai_CodeGenerationService.ts.md |     2 +-
- ...vscode-extension_src_ai_CodeReviewService.ts.md |     2 +-
- ...ls_vscode-extension_src_ai_ContextBuilder.ts.md |     2 +-
- ...xtension_src_dataconnect-generated_README.md.md |     2 +-
- ...n_src_dataconnect-generated_esm_index.esm.js.md |     2 +-
- ...n_src_dataconnect-generated_esm_package.json.md |     2 +-
- ...nsion_src_dataconnect-generated_index.cjs.js.md |     2 +-
- ...tension_src_dataconnect-generated_index.d.ts.md |     2 +-
- ...nsion_src_dataconnect-generated_package.json.md |     2 +-
- .../tools_vscode-extension_src_extension.ts.md     |     2 +-
- ...de-extension_src_handlers_CodeEditHandler.ts.md |     2 +-
- ...de-extension_src_handlers_CodeFlowHandler.ts.md |     2 +-
- ...scode-extension_src_handlers_ErrorHandler.ts.md |     2 +-
- ...de-extension_src_handlers_FeedbackHandler.ts.md |     2 +-
- ...ode-extension_src_providers_CodeFlowPanel.ts.md |     2 +-
- ...nsion_src_providers_StreamingChatProvider.ts.md |     2 +-
- ...n_src_providers_SupremeAIActivityProvider.ts.md |     2 +-
- ...providers_SupremeAIAdminDashboardProvider.ts.md |     2 +-
- ...nsion_src_providers_SupremeAIChatProvider.ts.md |     2 +-
- ...extension_src_providers_SupremeAIChatView.ts.md |     2 +-
- ...viders_SupremeAICustomerDashboardProvider.ts.md |     2 +-
- ...on_src_providers_SupremeAISidebarProvider.ts.md |     2 +-
- ...vscode-extension_src_services_AuthService.ts.md |     2 +-
- ...e-extension_src_services_SupremeAIService.ts.md |     2 +-
- .../tools_vscode-extension_src_types_index.ts.md   |     2 +-
- ...ension_src_utils_DynamicSignatureRegistry.ts.md |     2 +-
- ...s_vscode-extension_test_auth-service.test.ts.md |     2 +-
- ...ools_vscode-extension_test_mocks_vscode.d.ts.md |     2 +-
- .../tools_vscode-extension_test_mocks_vscode.ts.md |     2 +-
- .../tools_vscode-extension_test_setup.ts.md        |     2 +-
- ...ode-extension_test_supremeai-service.test.ts.md |     2 +-
- .../tools_vscode-extension_tsconfig.json.md        |     2 +-
- .../tools_vscode-extension_vitest.config.ts.md     |     2 +-
- docs/autogen/codebase/turbo.json.md                |     2 +-
- docs/autogen/codebase/visual.spec.ts.md            |     2 +-
- docs/autogen/codebase_full.md                      |  1003 +-
- 1063 files changed, 14668 insertions(+), 12333 deletions(-)
-
-```
-
-## Diff Detail
-```diff
-commit 1432eacc88479e5eaaab9dd454857ca82d0e4c79
-Author: github-actions[bot] <github-actions[bot]@users.noreply.github.com>
-Date:   Sat Jul 4 05:52:59 2026 +0000
-
-    docs: auto-update codebase docs & dashboard [skip ci]
-
-diff --git a/docs/autogen/INDEX.md b/docs/autogen/INDEX.md
-index b7dcec781..43f1b5c13 100644
---- a/docs/autogen/INDEX.md
-+++ b/docs/autogen/INDEX.md
-@@ -13,4 +13,4 @@
- - **ডিরেক্টরি:** [changes/](changes/)
- 
- ---
--*স্বয়ংক্রিয়ভাবে তৈরি — 2026-07-04 05:33:42*
-+*স্বয়ংক্রিয়ভাবে তৈরি — 2026-07-04 05:52:58*
-diff --git a/docs/autogen/changes/change_19e2f4019bb7a2aef85243afe61c87a137171a2c.md b/docs/autogen/changes/change_19e2f4019bb7a2aef85243afe61c87a137171a2c.md
-new file mode 100644
-index 000000000..28e83dd93
---- /dev/null
-+++ b/docs/autogen/changes/change_19e2f4019bb7a2aef85243afe61c87a137171a2c.md
-@@ -0,0 +1,11425 @@
-+# 📋 Commit 19e2f4019bb7a2aef85243afe61c87a137171a2c
-+
-+## Commit Stats
-+```
-+commit 19e2f4019bb7a2aef85243afe61c87a137171a2c
-+Author: github-actions[bot] <github-actions[bot]@users.noreply.github.com>
-+Date:   Sat Jul 4 05:33:42 2026 +0000
-+
-+    docs: auto-update codebase docs & dashboard [skip ci]
-+
-+ docs/autogen/INDEX.md                              |     2 +-
-+ ...nge_1502ebdec61ad3725a47e0a6db5e6670dfd49ac7.md | 11354 +++++++++++++++++++
-+ ...nge_28761b9a4f0847186b24350d5e1b4ae5b5258fa2.md |    39 +
-+ ...nge_3d3ce6400bb970bff375ae2e125da247f3a0957b.md | 10576 -----------------
-+ ...nge_d11eabb8b3d30b52530d1dc4e88e6af57437edb6.md |   536 -
-+ .../.github_actions_setup-backend_action.yml.md    |     2 +-
-+ ...github_scripts_advanced-validation-report.py.md |     2 +-
-+ .../codebase/.github_scripts_canary-deploy.py.md   |     2 +-
-+ .../codebase/.github_scripts_ci-auto-fix-v3.py.md  |     2 +-
-+ .../codebase/.github_scripts_ci-auto-fix.py.md     |     2 +-
-+ .../.github_scripts_ci-decision-engine.py.md       |     2 +-
-+ .../codebase/.github_scripts_ci-health-check.py.md |     2 +-
-+ .../.github_scripts_clean_action_logs.py.md        |     2 +-
-+ .../codebase/.github_scripts_deploy-backend.py.md  |     2 +-
-+ .../.github_scripts_detect-previous-failures.py.md |     2 +-
-+ .../codebase/.github_scripts_enforce_24h_gap.py.md |     2 +-
-+ .../.github_scripts_generate-ci-report.py.md       |     2 +-
-+ .../.github_scripts_generate_ai_prompt.py.md       |     2 +-
-+ .../.github_scripts_multi-model-evaluator.py.md    |     2 +-
-+ docs/autogen/codebase/.github_scripts_review.py.md |     2 +-
-+ .../.github_scripts_supremeai-evaluator.py.md      |     2 +-
-+ .../.github_scripts_test_ai_reviewer.py.md         |     2 +-
-+ .../codebase/.github_workflows_deploy.yml.md       |     2 +-
-+ .../.github_workflows_nightly-maintenance.yml.md   |     2 +-
-+ .../.github_workflows_supreme-core-ci.yml.md       |     2 +-
-+ .../.github_workflows_supreme-mobile-cd.yml.md     |     2 +-
-+ ....github_workflows_supreme-release-builds.yml.md |     2 +-
-+ .../.github_workflows_sync-from-prod.yml.md        |     2 +-
-+ docs/autogen/codebase/AGENT.md.md                  |     2 +-
-+ docs/autogen/codebase/AGENTS.md.md                 |     2 +-
-+ docs/autogen/codebase/CHANGELOG.md.md              |     2 +-
-+ docs/autogen/codebase/CI_PIPELINE.md.md            |     2 +-
-+ docs/autogen/codebase/CONTRIBUTING.md.md           |     2 +-
-+ docs/autogen/codebase/IMPLEMENTATION_STATUS.md.md  |     2 +-
-+ .../codebase/PRODUCTION_READINESS_GUIDE.md.md      |     2 +-
-+ docs/autogen/codebase/README.md.md                 |     2 +-
-+ docs/autogen/codebase/SECURITY.md.md               |     2 +-
-+ docs/autogen/codebase/accessibility.spec.ts.md     |     2 +-
-+ docs/autogen/codebase/admin_dashboard_script.js.md |     2 +-
-+ docs/autogen/codebase/admin_god.py.md              |     2 +-
-+ docs/autogen/codebase/apps_desktop_README.md.md    |     2 +-
-+ docs/autogen/codebase/apps_desktop_package.json.md |     2 +-
-+ .../codebase/apps_desktop_src-tauri_Cargo.toml.md  |     2 +-
-+ .../codebase/apps_desktop_src-tauri_build.rs.md    |     2 +-
-+ .../apps_desktop_src-tauri_secure-store.ts.md      |     2 +-
-+ .../codebase/apps_desktop_src-tauri_src_main.rs.md |     2 +-
-+ .../apps_desktop_src-tauri_tauri.conf.json.md      |     2 +-
-+ .../codebase/apps_desktop_src-ui_package.json.md   |     2 +-
-+ .../codebase/apps_desktop_src-ui_src_App.tsx.md    |     2 +-
-+ ..._desktop_src-ui_src_components_ChatInput.tsx.md |     2 +-
-+ .../codebase/apps_desktop_src-ui_src_main.tsx.md   |     2 +-
-+ .../apps_desktop_src-ui_src_pages_AdminPage.tsx.md |     2 +-
-+ .../apps_desktop_src-ui_src_pages_ChatPage.tsx.md  |     2 +-
-+ ...s_desktop_src-ui_src_pages_EvolutionPage.tsx.md |     2 +-
-+ .../apps_desktop_src-ui_src_pages_LoginPage.tsx.md |     2 +-
-+ ...apps_desktop_src-ui_src_pages_SkillsPage.tsx.md |     2 +-
-+ .../apps_desktop_src-ui_src_services_api.ts.md     |     2 +-
-+ .../apps_desktop_src-ui_src_stores_authStore.ts.md |     2 +-
-+ .../apps_desktop_src-ui_src_types_index.ts.md      |     2 +-
-+ .../apps_desktop_src-ui_src_vite-env.d.ts.md       |     2 +-
-+ .../codebase/apps_desktop_src-ui_tsconfig.json.md  |     2 +-
-+ .../apps_desktop_src-ui_tsconfig.node.json.md      |     2 +-
-+ .../codebase/apps_desktop_src-ui_vite.config.ts.md |     2 +-
-+ ...ava_com_supremeai_JavaWorkerApplication.java.md |     2 +-
-+ ...va_com_supremeai_grpc_WorkerServiceImpl.java.md |     2 +-
-+ ...in_java_com_supremeai_models_TaskEntity.java.md |     2 +-
-+ ...m_supremeai_repositories_TaskRepository.java.md |     2 +-
-+ ...va-worker_src_main_resources_application.yml.md |     2 +-
-+ docs/autogen/codebase/apps_mobile_README.md.md     |     2 +-
-+ docs/autogen/codebase/apps_mobile_README_BD.md.md  |     2 +-
-+ .../codebase/apps_mobile_analysis_options.yaml.md  |     2 +-
-+ .../codebase/apps_mobile_assets_i18n_ar.json.md    |     2 +-
-+ .../codebase/apps_mobile_assets_i18n_bn.json.md    |     2 +-
-+ .../codebase/apps_mobile_assets_i18n_en.json.md    |     2 +-
-+ .../codebase/apps_mobile_assets_i18n_es.json.md    |     2 +-
-+ .../codebase/apps_mobile_assets_i18n_hi.json.md    |     2 +-
-+ .../codebase/apps_mobile_assets_i18n_zh.json.md    |     2 +-
-+ .../codebase/apps_mobile_devtools_options.yaml.md  |     2 +-
-+ ...ts.xcassets_AppIcon.appiconset_Contents.json.md |     2 +-
-+ ....xcassets_LaunchImage.imageset_Contents.json.md |     2 +-
-+ ...sets.xcassets_LaunchImage.imageset_README.md.md |     2 +-
-+ ...s_mobile_lib_dataconnect_generated_README.md.md |     2 +-
-+ ...le_lib_dataconnect_generated_add_review.dart.md |     2 +-
-+ ..._lib_dataconnect_generated_create_movie.dart.md |     2 +-
-+ ...lib_dataconnect_generated_delete_review.dart.md |     2 +-
-+ ...ile_lib_dataconnect_generated_generated.dart.md |     2 +-
-+ ...b_dataconnect_generated_get_movie_by_id.dart.md |     2 +-
-+ ...e_lib_dataconnect_generated_list_movies.dart.md |     2 +-
-+ ...dataconnect_generated_list_user_reviews.dart.md |     2 +-
-+ ...le_lib_dataconnect_generated_list_users.dart.md |     2 +-
-+ ..._lib_dataconnect_generated_search_movie.dart.md |     2 +-
-+ ...e_lib_dataconnect_generated_upsert_user.dart.md |     2 +-
-+ docs/autogen/codebase/apps_mobile_lib_main.dart.md |     2 +-
-+ .../apps_mobile_lib_models_ci_job_model.dart.md    |     2 +-
-+ ...apps_mobile_lib_providers_auth_provider.dart.md |     2 +-
-+ ...mobile_lib_providers_dashboard_provider.dart.md |     2 +-
-+ ...le_lib_providers_orchestration_provider.dart.md |     2 +-
-+ ..._mobile_lib_providers_settings_provider.dart.md |     2 +-
-+ ...ps_mobile_lib_screens_agent_chat_screen.dart.md |     2 +-
-+ ...mobile_lib_screens_alerts_alerts_screen.dart.md |     2 +-
-+ ..._lib_screens_analytics_analytics_screen.dart.md |     2 +-
-+ ...apps_mobile_lib_screens_api_keys_screen.dart.md |     2 +-
-+ .../apps_mobile_lib_screens_api_scaffold.dart.md   |     2 +-
-+ ...apps_mobile_lib_screens_byoc_hub_screen.dart.md |     2 +-
-+ ..._lib_screens_consensus_consensus_screen.dart.md |     2 +-
-+ ...obile_lib_screens_dashboard_home_screen.dart.md |     2 +-
-+ ...pps_mobile_lib_screens_dashboard_screen.dart.md |     2 +-
-+ ..._lib_screens_extension_extension_screen.dart.md |     2 +-
-+ .../apps_mobile_lib_screens_git_git_screen.dart.md |     2 +-
-+ ...le_lib_screens_learning_learning_screen.dart.md |     2 +-
-+ .../apps_mobile_lib_screens_login_screen.dart.md   |     2 +-
-+ ...eens_notifications_notifications_screen.dart.md |     2 +-
-+ ...b_screens_projects_projects_list_screen.dart.md |     2 +-
-+ ...b_screens_providers_ai_providers_screen.dart.md |     2 +-
-+ ...s_mobile_lib_screens_quota_quota_screen.dart.md |     2 +-
-+ ...ib_screens_resilience_resilience_screen.dart.md |     2 +-
-+ ...apps_mobile_lib_screens_settings_screen.dart.md |     2 +-
-+ .../apps_mobile_lib_screens_terminal_view.dart.md  |     2 +-
-+ .../apps_mobile_lib_screens_vpn_vpn_screen.dart.md |     2 +-
-+ .../apps_mobile_lib_screens_wallet_screen.dart.md  |     2 +-
-+ .../apps_mobile_lib_services_api_client.dart.md    |     2 +-
-+ .../apps_mobile_lib_services_api_service.dart.md   |     2 +-
-+ ...pps_mobile_lib_services_billing_service.dart.md |     2 +-
-+ .../apps_mobile_lib_services_byoc_service.dart.md  |     2 +-
-+ ...pps_mobile_lib_services_ci_sync_service.dart.md |     2 +-
-+ ...s_mobile_lib_services_deployment_stream.dart.md |     2 +-
-+ ...obile_lib_services_localization_service.dart.md |     2 +-
-+ ...bile_lib_services_neural_stream_service.dart.md |     2 +-
-+ ...obile_lib_services_notification_service.dart.md |     2 +-
-+ ...obile_lib_services_offline_sync_service.dart.md |     2 +-
-+ ...ile_lib_services_payment_gateway_bridge.dart.md |     2 +-
-+ ..._mobile_lib_services_screen_api_service.dart.md |     2 +-
-+ .../apps_mobile_lib_theme_app_theme.dart.md        |     2 +-
-+ .../apps_mobile_lib_theme_theme_provider.dart.md   |     2 +-
-+ ...apps_mobile_lib_widgets_action_hub_card.dart.md |     2 +-
-+ ...ile_lib_widgets_base_dashboard_scaffold.dart.md |     2 +-
-+ .../codebase/apps_mobile_lib_widgets_es.json.md    |     2 +-
-+ .../apps_mobile_lib_widgets_json_dropzone.dart.md  |     2 +-
-+ .../apps_mobile_lib_widgets_live_terminal.dart.md  |     2 +-
-+ ...apps_mobile_lib_widgets_loading_widgets.dart.md |     2 +-
-+ ...le_lib_widgets_transaction_history_list.dart.md |     2 +-
-+ .../apps_mobile_lib_widgets_usage_chart.dart.md    |     2 +-
-+ ...ts.xcassets_AppIcon.appiconset_Contents.json.md |     2 +-
-+ docs/autogen/codebase/apps_mobile_pubspec.lock.md  |     2 +-
-+ docs/autogen/codebase/apps_mobile_pubspec.yaml.md  |     2 +-
-+ ...bile_test_auth_provider_edge_cases_test.dart.md |     2 +-
-+ .../apps_mobile_test_auth_provider_test.dart.md    |     2 +-
-+ ...mobile_test_home_screen_edge_cases_test.dart.md |     2 +-
-+ .../apps_mobile_test_home_screen_test.dart.md      |     2 +-
-+ ...s_mobile_test_screens_login_screen_test.dart.md |     2 +-
-+ .../codebase/apps_mobile_web_manifest.json.md      |     2 +-
-+ .../codebase/apps_studio-client_README.md.md       |     2 +-
-+ .../codebase/apps_studio-client_components.json.md |     2 +-
-+ .../apps_studio-client_eslint.config.js.md         |     2 +-
-+ .../autogen/codebase/apps_studio-client_main.js.md |     2 +-
-+ .../codebase/apps_studio-client_package.json.md    |     2 +-
-+ .../apps_studio-client_public_manifest.json.md     |     2 +-
-+ .../codebase/apps_studio-client_public_sw.js.md    |     2 +-
-+ .../apps_studio-client_src_App.test.tsx.md         |     2 +-
-+ .../codebase/apps_studio-client_src_App.tsx.md     |     2 +-
-+ ...tudio-client_src_components_AdminConsole.tsx.md |     2 +-
-+ ..._studio-client_src_components_BanglaHint.tsx.md |     2 +-
-+ ...apps_studio-client_src_components_Header.tsx.md |     2 +-
-+ ...lient_src_components_LiveSujonBackground.tsx.md |     2 +-
-+ ...c_components_Onboarding_OnboardingWizard.tsx.md |     2 +-
-+ ...ent_src_components_Onboarding_StepApiKey.tsx.md |     2 +-
-+ ..._src_components_Onboarding_StepFirstChat.tsx.md |     2 +-
-+ ...rc_components_Onboarding_StepModelSelect.tsx.md |     2 +-
-+ ...dio-client_src_components_OperatorStudio.tsx.md |     2 +-
-+ ...o-client_src_components_admin_ActionCard.tsx.md |     2 +-
-+ ..._src_components_admin_AdminAuthenticated.tsx.md |     2 +-
-+ ...client_src_components_admin_AdminConsole.tsx.md |     2 +-
-+ ..._src_components_admin_AdminDashboardHome.tsx.md |     2 +-
-+ ...o-client_src_components_admin_AdminLogin.tsx.md |     2 +-
-+ ..._src_components_admin_AdminSubTabContent.tsx.md |     2 +-
-+ ...-client_src_components_admin_AdminTopNav.tsx.md |     2 +-
-+ ...o-client_src_components_admin_AethelNode.tsx.md |     2 +-
-+ ...ient_src_components_admin_AuditLogsPanel.tsx.md |     2 +-
-+ ...lient_src_components_admin_BackupRestore.tsx.md |     2 +-
-+ ...ient_src_components_admin_CICDVisualizer.tsx.md |     2 +-
-+ ...t_src_components_admin_CloudOrchestrator.tsx.md |     2 +-
-+ ...lient_src_components_admin_CommandCenter.tsx.md |     2 +-
-+ ...client_src_components_admin_ConfigEditor.tsx.md |     2 +-
-+ ..._src_components_admin_ConsentMatrixModal.tsx.md |     2 +-
-+ ...-client_src_components_admin_CostAuditor.tsx.md |     2 +-
-+ ..._components_admin_DashboardErrorBoundary.tsx.md |     2 +-
-+ ...ent_src_components_admin_DeploymentModal.tsx.md |     2 +-
-+ ...client_src_components_admin_DynamicPanel.tsx.md |     2 +-
-+ ...omponents_admin_EnhancedSkillMarketplace.tsx.md |     2 +-
-+ ...t_src_components_admin_GithubIntegration.tsx.md |     2 +-
-+ ...client_src_components_admin_HealthBanner.tsx.md |     2 +-
-+ ...io-client_src_components_admin_HealthMap.tsx.md |     2 +-
-+ ..._src_components_admin_InteractiveChatTab.tsx.md |     2 +-
-+ ...dio-client_src_components_admin_LiveLogs.tsx.md |     2 +-
-+ ...lient_src_components_admin_MemoryBrowser.tsx.md |     2 +-
-+ ...-client_src_components_admin_ModelRouter.tsx.md |     2 +-
-+ ..._components_admin_ObservabilityDashboard.tsx.md |     2 +-
-+ ...-client_src_components_admin_RBACManager.tsx.md |     2 +-
-+ ...nt_src_components_admin_RateLimitManager.tsx.md |     2 +-
-+ ...rc_components_admin_RealTimeMetricsPanel.tsx.md |     2 +-
-+ ...mponents_admin_RedesignedDashboardMockup.tsx.md |     2 +-
-+ ...nt_src_components_admin_RulesEnginePanel.tsx.md |     2 +-
-+ ...t_src_components_admin_SecurityDashboard.tsx.md |     2 +-
-+ ...rc_components_admin_ServiceHealthMetrics.tsx.md |     2 +-
-+ ...ent_src_components_admin_ThreatDetection.tsx.md |     2 +-
-+ ...-client_src_components_admin_UserManager.tsx.md |     2 +-
-+ ..._src_components_admin_VisualRulesBuilder.tsx.md |     2 +-
-+ ..._studio-client_src_components_admin_index.ts.md |     2 +-
-+ ..._src_components_audio_WaveformVisualizer.tsx.md |     2 +-
-+ ...ient_src_components_chat_TypingIndicator.tsx.md |     2 +-
-+ ...nt_src_components_chat_UnifiedChatBubble.tsx.md |     2 +-
-+ ...s_studio-client_src_components_chat_index.ts.md |     2 +-
-+ ...t_src_components_customer_BrowserPreview.tsx.md |     2 +-
-+ ...t_src_components_customer_ChatPanel.test.tsx.md |     2 +-
-+ ...client_src_components_customer_ChatPanel.tsx.md |     2 +-
-+ ...lient_src_components_customer_CodeEditor.tsx.md |     2 +-
-+ ...-client_src_components_customer_HomeFeed.tsx.md |     2 +-
-+ ..._src_components_customer_MobileSimulator.tsx.md |     2 +-
-+ ...rc_components_customer_QuickPresets.test.tsx.md |     2 +-
-+ ...ent_src_components_customer_QuickPresets.tsx.md |     2 +-
-+ ...c_components_customer_UserDashboard.test.tsx.md |     2 +-
-+ ...nt_src_components_customer_UserDashboard.tsx.md |     2 +-
-+ ...udio-client_src_components_customer_index.ts.md |     2 +-
-+ ...components_dashboard_AutomationQueuePage.tsx.md |     2 +-
-+ ...components_dashboard_DashboardShell.test.tsx.md |     2 +-
-+ ..._src_components_dashboard_DashboardShell.tsx.md |     2 +-
-+ ...t_src_components_dashboard_KnowledgePage.tsx.md |     2 +-
-+ ..._src_components_dashboard_LlmGatewayPage.tsx.md |     2 +-
-+ ...ent_src_components_dashboard_SecretsPage.tsx.md |     2 +-
-+ ...c_components_dashboard_SessionDetailPage.tsx.md |     2 +-
-+ ...nt_src_components_dashboard_SessionsPage.tsx.md |     2 +-
-+ ...nt_src_components_dashboard_SettingsPage.tsx.md |     2 +-
-+ ...src_components_dashboard_SiteActionsPage.tsx.md |     2 +-
-+ ...lient_src_components_dashboard_UsagePage.tsx.md |     2 +-
-+ ...lient_src_components_dashboard_VaultPage.tsx.md |     2 +-
-+ ...ent_src_components_dashboard_sessionStore.ts.md |     2 +-
-+ ...ent_src_components_dashboard_useHashRoute.ts.md |     2 +-
-+ ...lient_src_components_editor_CollabEditor.tsx.md |     2 +-
-+ ...o-client_src_components_graph_SkillGraph.tsx.md |     2 +-
-+ ...udio-client_src_components_ui_ActionCard.tsx.md |     2 +-
-+ ...ps_studio-client_src_components_ui_Badge.tsx.md |     2 +-
-+ ...pps_studio-client_src_components_ui_Card.tsx.md |     2 +-
-+ ...studio-client_src_components_ui_Skeleton.tsx.md |     2 +-
-+ ...pps_studio-client_src_components_ui_index.ts.md |     2 +-
-+ ..._studio-client_src_contexts_ThemeContext.tsx.md |     2 +-
-+ ...o-client_src_dataconnect-generated_README.md.md |     2 +-
-+ ...t_src_dataconnect-generated_esm_index.esm.js.md |     2 +-
-+ ...t_src_dataconnect-generated_esm_package.json.md |     2 +-
-+ ...lient_src_dataconnect-generated_index.cjs.js.md |     2 +-
-+ ...-client_src_dataconnect-generated_index.d.ts.md |     2 +-
-+ ...lient_src_dataconnect-generated_package.json.md |     2 +-
-+ ...nt_src_dataconnect-generated_react_README.md.md |     2 +-
-+ ...dataconnect-generated_react_esm_index.esm.js.md |     2 +-
-+ ...dataconnect-generated_react_esm_package.json.md |     2 +-
-+ ...src_dataconnect-generated_react_index.cjs.js.md |     2 +-
-+ ...t_src_dataconnect-generated_react_index.d.ts.md |     2 +-
-+ ...src_dataconnect-generated_react_package.json.md |     2 +-
-+ .../codebase/apps_studio-client_src_firebase.ts.md |     2 +-
-+ .../apps_studio-client_src_hooks_index.ts.md       |     2 +-
-+ ...lient_src_hooks_tests_useTranslation.test.ts.md |     2 +-
-+ .../apps_studio-client_src_hooks_useAdminApi.ts.md |     2 +-
-+ .../apps_studio-client_src_hooks_useAuth.ts.md     |     2 +-
-+ .../apps_studio-client_src_hooks_useChat.ts.md     |     2 +-
-+ ..._studio-client_src_hooks_useDashboardData.ts.md |     2 +-
-+ ...ps_studio-client_src_hooks_useTranslation.ts.md |     2 +-
-+ ...apps_studio-client_src_hooks_useWebSocket.ts.md |     2 +-
-+ ...apps_studio-client_src_i18n_I18nProvider.tsx.md |     2 +-
-+ .../apps_studio-client_src_i18n_config.ts.md       |     2 +-
-+ .../apps_studio-client_src_i18n_translations.ts.md |     2 +-
-+ .../codebase/apps_studio-client_src_lib_etag.ts.md |     2 +-
-+ .../codebase/apps_studio-client_src_main.tsx.md    |     2 +-
-+ ...s_studio-client_src_services_adminService.ts.md |     2 +-
-+ ...tudio-client_src_services_adminTokenStore.ts.md |     2 +-
-+ ...s_studio-client_src_services_agentService.ts.md |     2 +-
-+ ...apps_studio-client_src_services_apiClient.ts.md |     2 +-
-+ ...ient_src_services_api_microserviceMonitor.ts.md |     2 +-
-+ ...t_src_services_audio_AudioPlaybackService.ts.md |     2 +-
-+ ...t_src_services_audio_AudioRecorderService.ts.md |     2 +-
-+ ...ps_studio-client_src_services_authService.ts.md |     2 +-
-+ ...ps_studio-client_src_services_chatService.ts.md |     2 +-
-+ ...tudio-client_src_services_ciReportService.ts.md |     2 +-
-+ ...pps_studio-client_src_services_storageApi.ts.md |     2 +-
-+ .../apps_studio-client_src_store_adminStore.ts.md  |     2 +-
-+ ...pps_studio-client_src_store_customerStore.ts.md |     2 +-
-+ ...ps_studio-client_src_store_dashboardStore.ts.md |     2 +-
-+ .../apps_studio-client_src_store_themeStore.ts.md  |     2 +-
-+ .../apps_studio-client_src_store_useStore.ts.md    |     2 +-
-+ .../apps_studio-client_src_test_setup.ts.md        |     2 +-
-+ .../codebase/apps_studio-client_src_types.ts.md    |     2 +-
-+ .../apps_studio-client_src_types_customer.ts.md    |     2 +-
-+ .../apps_studio-client_src_utils_api.ts.md         |     2 +-
-+ .../apps_studio-client_src_vite-env.d.ts.md        |     2 +-
-+ ...tudio-client_src_workers_logParser.worker.ts.md |     2 +-
-+ .../apps_studio-client_tsconfig.app.json.md        |     2 +-
-+ .../codebase/apps_studio-client_tsconfig.json.md   |     2 +-
-+ .../apps_studio-client_tsconfig.node.json.md       |     2 +-
-+ .../codebase/apps_studio-client_vite.config.ts.md  |     2 +-
-+ .../apps_studio-client_vitest.config.ts.md         |     2 +-
-+ docs/autogen/codebase/apps_web-chat_api.test.ts.md |     2 +-
-+ docs/autogen/codebase/apps_web-chat_api.ts.md      |     2 +-
-+ .../autogen/codebase/apps_web-chat_package.json.md |     2 +-
-+ docs/autogen/codebase/apps_web-chat_script.ts.md   |     2 +-
-+ .../codebase/apps_web-chat_tsconfig.json.md        |     2 +-
-+ .../codebase/apps_web-chat_vite-env.d.ts.md        |     2 +-
-+ .../codebase/apps_web-chat_vite.config.ts.md       |     2 +-
-+ .../codebase/apps_web-chat_vitest.config.ts.md     |     2 +-
-+ docs/autogen/codebase/backend_README.md.md         |     2 +-
-+ .../backend_adaptive_engine_experience_db.py.md    |     2 +-
-+ .../codebase/backend_adaptive_engine_init_.py.md   |     2 +-
-+ .../backend_adaptive_engine_intent_parser.py.md    |     2 +-
-+ .../backend_adaptive_engine_learning_loop.py.md    |     2 +-
-+ .../backend_adaptive_engine_platform_learner.py.md |     2 +-
-+ .../backend_adaptive_engine_registry.py.md         |     2 +-
-+ ...end_adaptive_engine_test_platform_learner.py.md |     2 +-
-+ docs/autogen/codebase/backend_admin_god.py.md      |     2 +-
-+ docs/autogen/codebase/backend_admin_init_.py.md    |     2 +-
-+ docs/autogen/codebase/backend_admin_test_god.py.md |     2 +-
-+ .../codebase/backend_agents_crew_departments.py.md |     2 +-
-+ docs/autogen/codebase/backend_agents_init_.py.md   |     2 +-
-+ .../codebase/backend_agents_legal_agent.py.md      |     2 +-
-+ .../codebase/backend_agents_medical_agent.py.md    |     2 +-
-+ .../backend_agents_research_assistant.py.md        |     2 +-
-+ .../codebase/backend_agents_test_legal_agent.py.md |     2 +-
-+ .../backend_agents_test_medical_agent.py.md        |     2 +-
-+ .../codebase/backend_agents_trading_agent.py.md    |     2 +-
-+ docs/autogen/codebase/backend_alembic_env.py.md    |     2 +-
-+ ...ersions_664fe16e33ca_add_ci_reports_table.py.md |     2 +-
-+ .../codebase/backend_api_dependencies.py.md        |     2 +-
-+ docs/autogen/codebase/backend_api_init_.py.md      |     2 +-
-+ .../codebase/backend_api_routes_admin.py.md        |     2 +-
-+ .../backend_api_routes_admin_dashboard.py.md       |     2 +-
-+ .../codebase/backend_api_routes_agent_tasks.py.md  |     2 +-
-+ .../codebase/backend_api_routes_agents.py.md       |     2 +-
-+ .../codebase/backend_api_routes_api_keys.py.md     |     2 +-
-+ .../backend_api_routes_approval_manager.py.md      |     2 +-
-+ .../backend_api_routes_async_task_router.py.md     |     2 +-
-+ .../autogen/codebase/backend_api_routes_auth.py.md |     2 +-
-+ .../codebase/backend_api_routes_billing_api.py.md  |     2 +-
-+ .../codebase/backend_api_routes_browser.py.md      |     2 +-
-+ .../codebase/backend_api_routes_byoc_api.py.md     |     2 +-
-+ .../codebase/backend_api_routes_cdc_webhooks.py.md |     2 +-
-+ .../autogen/codebase/backend_api_routes_chat.py.md |     2 +-
-+ .../codebase/backend_api_routes_ci_webhooks.py.md  |     2 +-
-+ .../codebase/backend_api_routes_cloud_mesh.py.md   |     2 +-
-+ .../codebase/backend_api_routes_codeflow.py.md     |     2 +-
-+ .../codebase/backend_api_routes_config.py.md       |     2 +-
-+ .../codebase/backend_api_routes_email.py.md        |     2 +-
-+ .../codebase/backend_api_routes_evolution.py.md    |     2 +-
-+ .../codebase/backend_api_routes_feedback.py.md     |     2 +-
-+ .../codebase/backend_api_routes_github.py.md       |     2 +-
-+ .../codebase/backend_api_routes_graph.py.md        |     2 +-
-+ .../codebase/backend_api_routes_init_.py.md        |     2 +-
-+ .../codebase/backend_api_routes_internal.py.md     |     2 +-
-+ .../codebase/backend_api_routes_knowledge.py.md    |     2 +-
-+ .../codebase/backend_api_routes_llm_gateway.py.md  |     2 +-
-+ .../codebase/backend_api_routes_markdown.py.md     |     2 +-
-+ .../codebase/backend_api_routes_marketplace.py.md  |     2 +-
-+ .../backend_api_routes_marketplace_endpoints.py.md |     2 +-
-+ .../codebase/backend_api_routes_media.py.md        |     2 +-
-+ .../codebase/backend_api_routes_memory.py.md       |     2 +-
-+ .../codebase/backend_api_routes_metrics.py.md      |     2 +-
-+ .../codebase/backend_api_routes_mobile_bff.py.md   |     2 +-
-+ .../codebase/backend_api_routes_onboarding.py.md   |     2 +-
-+ .../codebase/backend_api_routes_payments.py.md     |     2 +-
-+ .../codebase/backend_api_routes_preferences.py.md  |     2 +-
-+ .../codebase/backend_api_routes_repos.py.md        |     2 +-
-+ .../codebase/backend_api_routes_simulator.py.md    |     2 +-
-+ .../codebase/backend_api_routes_site_actions.py.md |     2 +-
-+ docs/autogen/codebase/backend_api_routes_sso.py.md |     2 +-
-+ .../codebase/backend_api_routes_stream.py.md       |     2 +-
-+ .../autogen/codebase/backend_api_routes_task.py.md |     2 +-
-+ .../backend_api_routes_task_workspace.py.md        |     2 +-
-+ .../codebase/backend_api_routes_tenant_admin.py.md |     2 +-
-+ .../codebase/backend_api_routes_tools_ops.py.md    |     2 +-
-+ .../backend_api_routes_tools_registry.py.md        |     2 +-
-+ .../backend_api_routes_usage_metrics.py.md         |     2 +-
-+ .../codebase/backend_api_routes_voice.py.md        |     2 +-
-+ .../backend_api_routes_websocket_agent.py.md       |     2 +-
-+ .../backend_api_routes_websocket_voice.py.md       |     2 +-
-+ .../codebase/backend_byoc_cloud_connector.py.md    |     2 +-
-+ .../backend_byoc_container_orchestrator.py.md      |     2 +-
-+ docs/autogen/codebase/backend_byoc_init_.py.md     |     2 +-
-+ .../codebase/backend_byoc_resource_manager.py.md   |     2 +-
-+ .../codebase/backend_config_byoc_limits.json.md    |     2 +-
-+ .../backend_config_constitutional_rules.json.md    |     2 +-
-+ .../codebase/backend_config_pricing_tiers.json.md  |     2 +-
-+ .../codebase/backend_config_routing_policy.json.md |     2 +-
-+ docs/autogen/codebase/backend_core_admin_god.py.md |     2 +-
-+ .../codebase/backend_core_admin_routes.py.md       |     2 +-
-+ .../codebase/backend_core_agent_orchestrator.py.md |     2 +-
-+ .../codebase/backend_core_api_key_middleware.py.md |     2 +-
-+ .../backend_core_api_key_rate_limiter.py.md        |     2 +-
-+ docs/autogen/codebase/backend_core_app.py.md       |     2 +-
-+ .../codebase/backend_core_audit_logger.py.md       |     2 +-
-+ .../codebase/backend_core_auth_middleware.py.md    |     2 +-
-+ .../codebase/backend_core_auto_remediation.py.md   |     2 +-
-+ .../codebase/backend_core_autocache_proxy.py.md    |     2 +-
-+ .../codebase/backend_core_circuit_breaker.py.md    |     2 +-
-+ .../backend_core_cloud_sandbox_orchestrator.py.md  |     2 +-
-+ .../codebase/backend_core_cloud_storage.py.md      |     2 +-
-+ .../codebase/backend_core_code_validator.py.md     |     2 +-
-+ docs/autogen/codebase/backend_core_config.py.md    |     2 +-
-+ docs/autogen/codebase/backend_core_constants.py.md |     2 +-
-+ .../codebase/backend_core_db_repository.py.md      |     2 +-
-+ .../codebase/backend_core_decision_engine.py.md    |     2 +-
-+ .../codebase/backend_core_discord_bot.py.md        |     2 +-
-+ .../codebase/backend_core_docker-compose.yml.md    |     2 +-
-+ .../codebase/backend_core_email_service.py.md      |     2 +-
-+ .../codebase/backend_core_error_pattern_db.py.md   |     2 +-
-+ .../codebase/backend_core_error_remediation.py.md  |     2 +-
-+ docs/autogen/codebase/backend_core_events.py.md    |     2 +-
-+ .../codebase/backend_core_evolution_engine.py.md   |     2 +-
-+ .../codebase/backend_core_factual_verifier.py.md   |     2 +-
-+ .../codebase/backend_core_feedback_loop.py.md      |     2 +-
-+ .../codebase/backend_core_free_tier_tracker.py.md  |     2 +-
-+ .../codebase/backend_core_gcp_firestore.py.md      |     2 +-
-+ .../codebase/backend_core_gcp_pubsub_queue.py.md   |     2 +-
-+ .../codebase/backend_core_generation_monitor.py.md |     2 +-
-+ .../codebase/backend_core_grpc_client.py.md        |     2 +-
-+ .../codebase/backend_core_health_monitor.py.md     |     2 +-
-+ .../backend_core_honeypot_middleware.py.md         |     2 +-
-+ .../backend_core_idempotency_middleware.py.md      |     2 +-
-+ .../codebase/backend_core_immune_system.py.md      |     2 +-
-+ docs/autogen/codebase/backend_core_init_.py.md     |     2 +-
-+ .../codebase/backend_core_input_sanitizer.py.md    |     2 +-
-+ docs/autogen/codebase/backend_core_intent.py.md    |     2 +-
-+ .../codebase/backend_core_intent_router.py.md      |     2 +-
-+ .../codebase/backend_core_language_router.py.md    |     2 +-
-+ docs/autogen/codebase/backend_core_ld_client.py.md |     2 +-
-+ docs/autogen/codebase/backend_core_lifespan.py.md  |     2 +-
-+ .../codebase/backend_core_llm_gateway.py.md        |     2 +-
-+ .../codebase/backend_core_logging_config.py.md     |     2 +-
-+ .../codebase/backend_core_mcp_allowlist.py.md      |     2 +-
-+ .../codebase/backend_core_microvm_sandbox.py.md    |     2 +-
-+ .../codebase/backend_core_multi_layer_cache.py.md  |     2 +-
-+ .../backend_core_observability_middleware.py.md    |     2 +-
-+ .../codebase/backend_core_orchestrator.py.md       |     2 +-
-+ .../codebase/backend_core_origin_validator.py.md   |     2 +-
-+ .../codebase/backend_core_output_validator.py.md   |     2 +-
-+ .../codebase/backend_core_pgbouncer_pool.py.md     |     2 +-
-+ .../codebase/backend_core_posthog_client.py.md     |     2 +-
-+ .../codebase/backend_core_prompt_firewall.py.md    |     2 +-
-+ .../codebase/backend_core_prompt_helpers.py.md     |     2 +-
-+ .../codebase/backend_core_rate_limiter.py.md       |     2 +-
-+ docs/autogen/codebase/backend_core_rbac.py.md      |     2 +-
-+ .../codebase/backend_core_redis_manager.py.md      |     2 +-
-+ .../codebase/backend_core_rollback_monitor.py.md   |     2 +-
-+ .../codebase/backend_core_rules_mutator.py.md      |     2 +-
-+ .../codebase/backend_core_schema_validator.py.md   |     2 +-
-+ .../codebase/backend_core_secret_vault.py.md       |     2 +-
-+ .../backend_core_secure_credential_store.py.md     |     2 +-
-+ docs/autogen/codebase/backend_core_security.py.md  |     2 +-
-+ .../codebase/backend_core_self_healing_agent.py.md |     2 +-
-+ .../codebase/backend_core_semantic_cache.py.md     |     2 +-
-+ docs/autogen/codebase/backend_core_services.py.md  |     2 +-
-+ .../codebase/backend_core_skill_graph.py.md        |     2 +-
-+ .../codebase/backend_core_swarm_orchestrator.py.md |     2 +-
-+ .../autogen/codebase/backend_core_task_queue.py.md |     2 +-
-+ .../backend_core_task_queue_enhanced.py.md         |     2 +-
-+ .../codebase/backend_core_task_router.py.md        |     2 +-
-+ docs/autogen/codebase/backend_core_telemetry.py.md |     2 +-
-+ docs/autogen/codebase/backend_core_tenant_db.py.md |     2 +-
-+ .../codebase/backend_core_token_budget.py.md       |     2 +-
-+ .../codebase/backend_core_token_deductor.py.md     |     2 +-
-+ .../codebase/backend_core_universal_rules.py.md    |     2 +-
-+ .../codebase/backend_core_upload_validator.py.md   |     2 +-
-+ .../backend_core_upstash_redis_queue.py.md         |     2 +-
-+ .../codebase/backend_core_user_profiler.py.md      |     2 +-
-+ docs/autogen/codebase/backend_coverage.json.md     |     6 +-
-+ docs/autogen/codebase/backend_database_init_.py.md |     2 +-
-+ ...end_database_migrations_01_initial_setup.sql.md |     2 +-
-+ ...kend_database_migrations_02_phase2_setup.sql.md |     2 +-
-+ ...grations_03_user_preferences_and_metrics.sql.md |     2 +-
-+ ...nd_database_migrations_04_schema_upgrade.sql.md |     2 +-
-+ ...database_migrations_05_seed_github_repos.sql.md |     2 +-
-+ ...d_database_migrations_06_referral_system.sql.md |     2 +-
-+ ...end_database_migrations_07_tenant_config.sql.md |     2 +-
-+ ...ckend_database_migrations_08_sso_configs.sql.md |     2 +-
-+ ...database_migrations_09_offline_sync_logs.sql.md |     2 +-
-+ ...atabase_migrations_10_tenant_sso_offline.sql.md |     2 +-
-+ .../codebase/backend_database_session.py.md        |     2 +-
-+ .../codebase/backend_database_storage_client.py.md |     2 +-
-+ .../backend_database_supabase_client.py.md         |     2 +-
-+ .../codebase/backend_engine_cost_optimizer.py.md   |     2 +-
-+ docs/autogen/codebase/backend_engine_init_.py.md   |     2 +-
-+ .../codebase/backend_engine_model_dispatcher.py.md |     2 +-
-+ .../backend_evolution_auto_skill_creator.py.md     |     2 +-
-+ .../backend_evolution_auto_update_manager.py.md    |     2 +-
-+ .../backend_evolution_dynamic_injector.py.md       |     2 +-
-+ .../backend_evolution_fitness_engine.py.md         |     2 +-
-+ .../autogen/codebase/backend_evolution_init_.py.md |     2 +-
-+ .../backend_evolution_master_planner.py.md         |     2 +-
-+ .../backend_evolution_security_sandbox.py.md       |     2 +-
-+ .../backend_evolution_self_evolution_agent.py.md   |     2 +-
-+ .../codebase/backend_evolution_skill_graph.py.md   |     2 +-
-+ docs/autogen/codebase/backend_fix_tests.py.md      |     2 +-
-+ docs/autogen/codebase/backend_init_.py.md          |     2 +-
-+ docs/autogen/codebase/backend_main.py.md           |     2 +-
-+ .../backend_memory_checkpoint_resume.py.md         |     2 +-
-+ .../codebase/backend_memory_chromadb_store.py.md   |     2 +-
-+ .../backend_memory_cloud_postgres_store.py.md      |     2 +-
-+ .../backend_memory_cloud_vector_store.py.md        |     2 +-
-+ .../codebase/backend_memory_episodic_memory.py.md  |     2 +-
-+ docs/autogen/codebase/backend_memory_init_.py.md   |     2 +-
-+ .../codebase/backend_memory_long_term_memory.py.md |     2 +-
-+ .../codebase/backend_memory_rag_pipeline.py.md     |     2 +-
-+ .../codebase/backend_memory_sliding_window.py.md   |     2 +-
-+ .../codebase/backend_memory_sqlite_store.py.md     |     2 +-
-+ .../codebase/backend_memory_summary_tree.py.md     |     2 +-
-+ .../codebase/backend_memory_supabase_store.py.md   |     2 +-
-+ .../backend_memory_vector_store_config.py.md       |     2 +-
-+ .../backend_middleware_auth_middleware.py.md       |     2 +-
-+ .../backend_middleware_chaos_injector.py.md        |     2 +-
-+ .../codebase/backend_middleware_idempotency.py.md  |     2 +-
-+ docs/autogen/codebase/backend_models_admin.py.md   |     2 +-
-+ docs/autogen/codebase/backend_models_api_key.py.md |     2 +-
-+ .../codebase/backend_models_byoc_payloads.py.md    |     2 +-
-+ .../codebase/backend_models_ci_report.py.md        |     2 +-
-+ .../codebase/backend_models_deployment_logs.py.md  |     2 +-
-+ .../backend_models_error_remediation.py.md         |     2 +-
-+ .../codebase/backend_models_evolution.py.md        |     2 +-
-+ docs/autogen/codebase/backend_models_init_.py.md   |     2 +-
-+ .../backend_models_local_model_handler.py.md       |     2 +-
-+ .../codebase/backend_models_pending_tasks.py.md    |     2 +-
-+ .../codebase/backend_models_shared_workspace.py.md |     2 +-
-+ .../backend_models_transaction_ledger.py.md        |     2 +-
-+ .../backend_models_voice_interaction.py.md         |     2 +-
-+ docs/autogen/codebase/backend_models_wallet.py.md  |     2 +-
-+ .../codebase/backend_monitoring_cost_auditor.py.md |     2 +-
-+ .../codebase/backend_monitoring_init_.py.md        |     2 +-
-+ .../codebase/backend_p2p_credit_system.py.md       |     2 +-
-+ docs/autogen/codebase/backend_p2p_init_.py.md      |     2 +-
-+ .../codebase/backend_p2p_secure_tunnel.py.md       |     2 +-
-+ docs/autogen/codebase/backend_pyproject.toml.md    |     2 +-
-+ docs/autogen/codebase/backend_reports_init_.py.md  |     2 +-
-+ .../backend_reports_optimization_engine.py.md      |     2 +-
-+ .../codebase/backend_run_roundtrip_tests.py.md     |     2 +-
-+ docs/autogen/codebase/backend_scout_init_.py.md    |     2 +-
-+ .../backend_scout_knowledge_extractor.py.md        |     2 +-
-+ .../codebase/backend_scout_web_crawler_agent.py.md |     2 +-
-+ .../codebase/backend_scripts_check_ollama.py.md    |     2 +-
-+ docs/autogen/codebase/backend_scripts_init_.py.md  |     2 +-
-+ .../codebase/backend_scripts_load_seed_data.py.md  |     2 +-
-+ .../backend_scripts_run_dependency_check.py.md     |     2 +-
-+ .../backend_scripts_seed_tools_registry.py.md      |     2 +-
-+ .../backend_scripts_self_healing_tests.py.md       |     2 +-
-+ docs/autogen/codebase/backend_skills_init_.py.md   |     2 +-
-+ .../codebase/backend_skills_provisioner.py.md      |     2 +-
-+ .../codebase/backend_skills_skill_registry.py.md   |     2 +-
-+ .../codebase/backend_storage_asset_manager.py.md   |     2 +-
-+ docs/autogen/codebase/backend_storage_init_.py.md  |     2 +-
-+ .../backend_storage_r2_storage_client.py.md        |     2 +-
-+ .../backend_tests_agents_test_legal_agent.py.md    |     2 +-
-+ .../backend_tests_agents_test_medical_agent.py.md  |     2 +-
-+ ...kend_tests_agents_test_research_assistant.py.md |     2 +-
-+ .../backend_tests_agents_test_trading_agent.py.md  |     2 +-
-+ .../backend_tests_byoc_test_cloud_connector.py.md  |     2 +-
-+ ...nd_tests_byoc_test_container_orchestrator.py.md |     2 +-
-+ .../backend_tests_byoc_test_resource_manager.py.md |     2 +-
-+ docs/autogen/codebase/backend_tests_conftest.py.md |     2 +-
-+ .../backend_tests_engine_test_cost_optimizer.py.md |     2 +-
-+ ...ackend_tests_engine_test_model_dispatcher.py.md |     2 +-
-+ docs/autogen/codebase/backend_tests_init_.py.md    |     2 +-
-+ ...ackend_tests_monitoring_test_cost_auditor.py.md |     2 +-
-+ .../backend_tests_p2p_test_credit_system.py.md     |     2 +-
-+ .../backend_tests_p2p_test_secure_tunnel.py.md     |     2 +-
-+ ...kend_tests_scout_test_knowledge_extractor.py.md |     2 +-
-+ ...ackend_tests_scout_test_web_crawler_agent.py.md |     2 +-
-+ .../backend_tests_test_adaptive_engine.py.md       |     2 +-
-+ .../codebase/backend_tests_test_admin_god.py.md    |     2 +-
-+ .../codebase/backend_tests_test_admin_models.py.md |     2 +-
-+ .../codebase/backend_tests_test_admin_routes.py.md |     2 +-
-+ .../codebase/backend_tests_test_advanced.py.md     |     2 +-
-+ .../backend_tests_test_agent_department.py.md      |     2 +-
-+ .../backend_tests_test_agent_departments.py.md     |     2 +-
-+ .../backend_tests_test_agent_orchestrator.py.md    |     2 +-
-+ ...ackend_tests_test_agents_crew_departments.py.md |     2 +-
-+ docs/autogen/codebase/backend_tests_test_api.py.md |     2 +-
-+ .../codebase/backend_tests_test_api_chat.py.md     |     2 +-
-+ .../codebase/backend_tests_test_api_keys.py.md     |     2 +-
-+ .../backend_tests_test_api_new_endpoints.py.md     |     2 +-
-+ .../codebase/backend_tests_test_api_router.py.md   |     2 +-
-+ .../codebase/backend_tests_test_audit_logger.py.md |     2 +-
-+ .../backend_tests_test_auth_middleware.py.md       |     2 +-
-+ .../codebase/backend_tests_test_auth_routes.py.md  |     2 +-
-+ .../backend_tests_test_auto_fix_trigger.py.md      |     2 +-
-+ .../backend_tests_test_auto_skill_creator.py.md    |     2 +-
-+ .../backend_tests_test_autonomous_agent.py.md      |     2 +-
-+ .../codebase/backend_tests_test_bangla_nlp.py.md   |     2 +-
-+ .../codebase/backend_tests_test_bangla_voice.py.md |     2 +-
-+ .../backend_tests_test_billing_system.py.md        |     2 +-
-+ .../codebase/backend_tests_test_brain.py.md        |     2 +-
-+ .../backend_tests_test_browser_credentials.py.md   |     2 +-
-+ .../backend_tests_test_byoc_endpoints.py.md        |     2 +-
-+ .../codebase/backend_tests_test_chaos_worker.py.md |     2 +-
-+ .../backend_tests_test_checkpoint_resume.py.md     |     2 +-
-+ .../backend_tests_test_circuit_breaker.py.md       |     2 +-
-+ .../backend_tests_test_cloud_sandbox.py.md         |     2 +-
-+ .../backend_tests_test_cloud_storage.py.md         |     2 +-
-+ .../backend_tests_test_code_validator.py.md        |     2 +-
-+ .../backend_tests_test_collaborative_editor.py.md  |     2 +-
-+ .../codebase/backend_tests_test_config.py.md       |     2 +-
-+ .../backend_tests_test_config_additional.py.md     |     2 +-
-+ .../backend_tests_test_config_coverage.py.md       |     2 +-
-+ .../codebase/backend_tests_test_constants.py.md    |     2 +-
-+ .../backend_tests_test_context_and_actions.py.md   |     2 +-
-+ .../autogen/codebase/backend_tests_test_core.py.md |     2 +-
-+ .../codebase/backend_tests_test_core_smoke.py.md   |     2 +-
-+ .../backend_tests_test_coverage_gaps.py.md         |     2 +-
-+ .../codebase/backend_tests_test_crew_mcp.py.md     |     2 +-
-+ ...ackend_tests_test_database_storage_client.py.md |     2 +-
-+ .../backend_tests_test_db_repository.py.md         |     2 +-
-+ docs/autogen/codebase/backend_tests_test_e2e.py.md |     2 +-
-+ .../codebase/backend_tests_test_e2e_media.py.md    |     2 +-
-+ .../codebase/backend_tests_test_email_agent.py.md  |     2 +-
-+ .../backend_tests_test_email_service.py.md         |     2 +-
-+ .../backend_tests_test_episodic_memory.py.md       |     2 +-
-+ .../backend_tests_test_error_remediation.py.md     |     2 +-
-+ .../backend_tests_test_evolution_engine.py.md      |     2 +-
-+ .../backend_tests_test_evolution_pipeline.py.md    |     2 +-
-+ .../backend_tests_test_factual_verifier.py.md      |     2 +-
-+ .../backend_tests_test_feedback_loop.py.md         |     2 +-
-+ .../backend_tests_test_firebase_integration.py.md  |     2 +-
-+ .../backend_tests_test_fitness_engine.py.md        |     2 +-
-+ .../backend_tests_test_free_tier_tracker.py.md     |     2 +-
-+ .../backend_tests_test_gcp_integration.py.md       |     2 +-
-+ .../backend_tests_test_generation_monitor.py.md    |     2 +-
-+ .../codebase/backend_tests_test_github_agent.py.md |     2 +-
-+ .../codebase/backend_tests_test_graph_routes.py.md |     2 +-
-+ .../backend_tests_test_graph_service.py.md         |     2 +-
-+ .../codebase/backend_tests_test_grpc_client.py.md  |     2 +-
-+ .../backend_tests_test_hallucination_guard.py.md   |     2 +-
-+ .../codebase/backend_tests_test_health.py.md       |     2 +-
-+ .../backend_tests_test_health_monitor.py.md        |     2 +-
-+ .../backend_tests_test_health_monitor_routes.py.md |     2 +-
-+ .../backend_tests_test_honeypot_middleware.py.md   |     2 +-
-+ ...backend_tests_test_idempotency_middleware.py.md |     2 +-
-+ .../backend_tests_test_immune_system.py.md         |     2 +-
-+ .../backend_tests_test_immune_system_scanner.py.md |     2 +-
-+ .../backend_tests_test_input_sanitizer.py.md       |     2 +-
-+ .../backend_tests_test_language_router.py.md       |     2 +-
-+ .../codebase/backend_tests_test_llm_gateway.py.md  |     2 +-
-+ .../backend_tests_test_llm_gateway_coverage.py.md  |     2 +-
-+ .../backend_tests_test_long_term_memory.py.md      |     2 +-
-+ .../backend_tests_test_markdown_export.py.md       |     2 +-
-+ .../backend_tests_test_marketplace_agent.py.md     |     2 +-
-+ .../backend_tests_test_mcp_allowlist.py.md         |     2 +-
-+ .../codebase/backend_tests_test_mcp_server.py.md   |     2 +-
-+ ...ackend_tests_test_mcp_servers_integration.py.md |     2 +-
-+ .../codebase/backend_tests_test_media_r2.py.md     |     2 +-
-+ ...kend_tests_test_middleware_chaos_injector.py.md |     2 +-
-+ .../codebase/backend_tests_test_migrations.py.md   |     2 +-
-+ ...kend_tests_test_migrations_and_onboarding.py.md |     2 +-
-+ .../codebase/backend_tests_test_mobile_e2e.py.md   |     2 +-
-+ .../backend_tests_test_model_registry.py.md        |     2 +-
-+ .../backend_tests_test_model_router_unit.py.md     |     2 +-
-+ .../backend_tests_test_model_trainer.py.md         |     2 +-
-+ .../backend_tests_test_models_ci_report.py.md      |     2 +-
-+ .../backend_tests_test_models_evolution.py.md      |     2 +-
-+ .../codebase/backend_tests_test_monitoring.py.md   |     2 +-
-+ .../backend_tests_test_multi_account_rotator.py.md |   438 +
-+ .../codebase/backend_tests_test_multicloud.py.md   |     2 +-
-+ .../backend_tests_test_new_endpoints_sprint5.py.md |     2 +-
-+ .../backend_tests_test_new_interfaces.py.md        |     2 +-
-+ .../backend_tests_test_new_tools_sprint5.py.md     |     2 +-
-+ .../backend_tests_test_optimization_engine.py.md   |     2 +-
-+ .../backend_tests_test_output_validator.py.md      |     2 +-
-+ ...ackend_tests_test_parallel_agent_executor.py.md |     2 +-
-+ .../codebase/backend_tests_test_payments.py.md     |     2 +-
-+ ...ckend_tests_test_performance_aware_router.py.md |     2 +-
-+ .../backend_tests_test_pgbouncer_pool.py.md        |     2 +-
-+ .../codebase/backend_tests_test_posthog.py.md      |     2 +-
-+ .../codebase/backend_tests_test_pr_reviewer.py.md  |     2 +-
-+ .../backend_tests_test_prod_docs_security.py.md    |     2 +-
-+ ...sts_test_production_readiness_integration.py.md |     2 +-
-+ .../backend_tests_test_prompt_firewall.py.md       |     2 +-
-+ .../autogen/codebase/backend_tests_test_rbac.py.md |     2 +-
-+ ...backend_tests_test_reasoning_orchestrator.py.md |     2 +-
-+ .../backend_tests_test_repo_discovery.py.md        |     2 +-
-+ .../backend_tests_test_resource_catalog.py.md      |     2 +-
-+ .../autogen/codebase/backend_tests_test_rlhf.py.md |     2 +-
-+ ...kend_tests_test_sandbox_orchestration_run.py.md |     2 +-
-+ .../backend_tests_test_schema_validator.py.md      |     2 +-
-+ .../codebase/backend_tests_test_secret_vault.py.md |     2 +-
-+ ...ackend_tests_test_secure_credential_store.py.md |     2 +-
-+ .../backend_tests_test_security_middleware.py.md   |     2 +-
-+ .../backend_tests_test_security_regression.py.md   |     2 +-
-+ .../backend_tests_test_self_evolution_agent.py.md  |     2 +-
-+ .../backend_tests_test_simulator_browser_api.py.md |     2 +-
-+ .../codebase/backend_tests_test_skill_graph.py.md  |     2 +-
-+ .../backend_tests_test_skill_recommender.py.md     |     2 +-
-+ .../backend_tests_test_sliding_window_memory.py.md |     2 +-
-+ .../backend_tests_test_sprint_c_tools.py.md        |     2 +-
-+ .../codebase/backend_tests_test_sprint_g.py.md     |     2 +-
-+ .../backend_tests_test_stealth_networking.py.md    |     2 +-
-+ .../codebase/backend_tests_test_stream.py.md       |     2 +-
-+ .../backend_tests_test_style_learner.py.md         |     2 +-
-+ ...kend_tests_test_supabase_schema_bootstrap.py.md |     2 +-
-+ .../backend_tests_test_supabase_store.py.md        |     2 +-
-+ .../backend_tests_test_swarm_orchestrator.py.md    |     2 +-
-+ .../backend_tests_test_task_endpoints.py.md        |     2 +-
-+ .../codebase/backend_tests_test_task_queue.py.md   |     2 +-
-+ .../codebase/backend_tests_test_task_router.py.md  |     2 +-
-+ .../codebase/backend_tests_test_telegram_bot.py.md |     2 +-
-+ .../codebase/backend_tests_test_telemetry.py.md    |     2 +-
-+ .../backend_tests_test_tenant_rate_limiter.py.md   |     2 +-
-+ .../backend_tests_test_universal_rules.py.md       |     2 +-
-+ .../backend_tests_test_upstash_redis.py.md         |     2 +-
-+ docs/autogen/codebase/backend_tests_test_uss.py.md |     2 +-
-+ .../backend_tests_test_video_generator.py.md       |     2 +-
-+ .../codebase/backend_tests_test_vision_agent.py.md |     2 +-
-+ .../codebase/backend_tests_test_voice_stream.py.md |     2 +-
-+ .../codebase/backend_tests_test_vpn_switcher.py.md |     2 +-
-+ .../codebase/backend_tests_test_vscode_e2e.py.md   |     2 +-
-+ .../codebase/backend_tests_test_web_fallback.py.md |     2 +-
-+ ...d_tests_tools_test_auto_coverage_improver.py.md |     2 +-
-+ ...kend_tests_tools_test_auto_test_generator.py.md |     2 +-
-+ ...kend_tests_tools_test_code_smell_detector.py.md |   509 +
-+ .../backend_tests_tools_test_cot_reasoner.py.md    |   559 +
-+ ...backend_tests_tools_test_coverage_auditor.py.md |     2 +-
-+ .../backend_tests_utils_test_api_tracker.py.md     |     2 +-
-+ .../backend_tests_workers_test_celery_app.py.md    |     2 +-
-+ .../backend_tools_3d_model_generator.py.md         |     2 +-
-+ .../codebase/backend_tools_agent_tools.py.md       |     2 +-
-+ .../backend_tools_ai_federation_protocol.py.md     |     2 +-
-+ .../backend_tools_ai_pair_programmer.py.md         |     2 +-
-+ .../codebase/backend_tools_api_gateway.py.md       |     2 +-
-+ .../backend_tools_auto_coverage_improver.py.md     |     2 +-
-+ .../codebase/backend_tools_auto_pr_pipeline.py.md  |     2 +-
-+ .../backend_tools_auto_test_generator.py.md        |     2 +-
-+ .../backend_tools_bandwidth_optimizer.py.md        |     2 +-
-+ .../backend_tools_bangla_ai_connector.py.md        |     2 +-
-+ .../codebase/backend_tools_bangla_nlp.py.md        |     2 +-
-+ .../codebase/backend_tools_bangla_voice.py.md      |     2 +-
-+ .../codebase/backend_tools_benchmark_agent.py.md   |     2 +-
-+ .../backend_tools_bengali_ocr_converter.py.md      |     2 +-
-+ .../codebase/backend_tools_blockchain_agent.py.md  |     2 +-
-+ .../autogen/codebase/backend_tools_bootstrap.py.md |     2 +-
-+ .../codebase/backend_tools_browser_agent.py.md     |     2 +-
-+ .../codebase/backend_tools_browser_stealth.py.md   |     2 +-
-+ .../backend_tools_checkpoint_manager.py.md         |     2 +-
-+ docs/autogen/codebase/backend_tools_cli.py.md      |     2 +-
-+ .../backend_tools_cloud_sandbox_orchestrator.py.md |     2 +-
-+ .../backend_tools_code_smell_detector.py.md        |     2 +-
-+ .../codebase/backend_tools_codebase_exporter.py.md |     2 +-
-+ .../backend_tools_collaborative_editor.py.md       |     2 +-
-+ .../codebase/backend_tools_comment_thread_ai.py.md |     2 +-
-+ .../codebase/backend_tools_computer_agent.py.md    |     2 +-
-+ .../backend_tools_conversation_manager.py.md       |     2 +-
-+ .../codebase/backend_tools_cost_auditor.py.md      |     2 +-
-+ .../codebase/backend_tools_cot_reasoner.py.md      |     6 +-
-+ .../codebase/backend_tools_coverage_auditor.py.md  |     2 +-
-+ .../backend_tools_dependency_manager_agent.py.md   |     2 +-
-+ .../backend_tools_diagram_to_architecture.py.md    |     2 +-
-+ .../codebase/backend_tools_docker_sandbox.py.md    |     2 +-
-+ .../codebase/backend_tools_domain_adapter.py.md    |     2 +-
-+ .../codebase/backend_tools_email_agent.py.md       |     2 +-
-+ .../codebase/backend_tools_ensemble_router.py.md   |     2 +-
-+ .../codebase/backend_tools_fuzz_sandbox.py.md      |     2 +-
-+ .../codebase/backend_tools_game_dev_agent.py.md    |     2 +-
-+ .../backend_tools_gcp_cloud_functions.py.md        |     2 +-
-+ .../backend_tools_git_knowledge_extractor.py.md    |     2 +-
-+ .../codebase/backend_tools_github_agent.py.md      |     2 +-
-+ .../codebase/backend_tools_graph_service.py.md     |     2 +-
-+ .../backend_tools_headless_agent_registry.py.md    |     2 +-
-+ .../codebase/backend_tools_health_checker.py.md    |     2 +-
-+ .../codebase/backend_tools_image_generator.py.md   |     2 +-
-+ .../codebase/backend_tools_image_to_code.py.md     |     2 +-
-+ docs/autogen/codebase/backend_tools_init_.py.md    |     2 +-
-+ .../backend_tools_knowledge_base_indexer.py.md     |     2 +-
-+ .../backend_tools_langchain_agent_example.py.md    |     2 +-
-+ .../codebase/backend_tools_legal_agent.py.md       |     2 +-
-+ .../backend_tools_local_ocr_extractor.py.md        |     2 +-
-+ .../codebase/backend_tools_local_search_rag.py.md  |     2 +-
-+ .../codebase/backend_tools_marketplace_agent.py.md |     2 +-
-+ .../codebase/backend_tools_mcp_cloud_deploy.py.md  |     2 +-
-+ .../codebase/backend_tools_mcp_github_cicd.py.md   |     2 +-
-+ .../codebase/backend_tools_mcp_server.py.md        |     2 +-
-+ .../codebase/backend_tools_mcp_supabase.py.md      |     2 +-
-+ .../codebase/backend_tools_mcp_workspace.py.md     |     2 +-
-+ .../codebase/backend_tools_medical_agent.py.md     |     2 +-
-+ .../codebase/backend_tools_meta_architect.py.md    |     2 +-
-+ .../codebase/backend_tools_model_trainer.py.md     |     2 +-
-+ .../backend_tools_monthly_cost_reporter.py.md      |     2 +-
-+ .../backend_tools_multi_account_rotator.py.md      |     2 +-
-+ .../codebase/backend_tools_multilingual_tts.py.md  |     2 +-
-+ .../codebase/backend_tools_music_generator.py.md   |     2 +-
-+ .../codebase/backend_tools_offline_mode.py.md      |     2 +-
-+ .../backend_tools_on_premise_deployer.py.md        |     2 +-
-+ .../backend_tools_parallel_agent_executor.py.md    |     2 +-
-+ .../codebase/backend_tools_pdf_to_sdk.py.md        |     2 +-
-+ .../codebase/backend_tools_plan_sorter.py.md       |     2 +-
-+ .../backend_tools_playwright_browser_agent.py.md   |     2 +-
-+ .../codebase/backend_tools_pr_reviewer.py.md       |     2 +-
-+ .../codebase/backend_tools_pre_commit_ai.py.md     |     2 +-
-+ .../codebase/backend_tools_preference_memory.py.md |     2 +-
-+ .../backend_tools_presentation_generator.py.md     |     2 +-
-+ .../codebase/backend_tools_proxy_manager.py.md     |     2 +-
-+ .../codebase/backend_tools_repo_deep_indexer.py.md |     2 +-
-+ .../backend_tools_repo_discovery_agent.py.md       |     2 +-
-+ .../codebase/backend_tools_resource_catalog.py.md  |     2 +-
-+ .../codebase/backend_tools_rlhf_pipeline.py.md     |     2 +-
-+ .../codebase/backend_tools_safe_executor.py.md     |     2 +-
-+ .../codebase/backend_tools_scientific_agent.py.md  |     2 +-
-+ .../codebase/backend_tools_seed_database.py.md     |     2 +-
-+ .../codebase/backend_tools_self_planner.py.md      |     2 +-
-+ .../codebase/backend_tools_skill_recommender.py.md |     2 +-
-+ .../codebase/backend_tools_sso_integrator.py.md    |     2 +-
-+ .../backend_tools_stealth_http_client.py.md        |     2 +-
-+ .../codebase/backend_tools_style_learner.py.md     |     2 +-
-+ .../codebase/backend_tools_telegram_bot.py.md      |     2 +-
-+ .../backend_tools_tenant_rate_limiter.py.md        |     2 +-
-+ .../backend_tools_test_3d_model_generator.py.md    |     2 +-
-+ ...end_tools_test_cloud_sandbox_orchestrator.py.md |     2 +-
-+ .../codebase/backend_tools_trading_agent.py.md     |     2 +-
-+ .../codebase/backend_tools_video_generator.py.md   |     2 +-
-+ .../backend_tools_viral_referral_engine.py.md      |     2 +-
-+ .../codebase/backend_tools_vision_agent.py.md      |     2 +-
-+ docs/autogen/codebase/backend_tools_voice.py.md    |     2 +-
-+ .../codebase/backend_tools_voice_coder.py.md       |     2 +-
-+ .../codebase/backend_tools_vpn_switcher.py.md      |     2 +-
-+ .../backend_tools_vulnerability_predictor.py.md    |     2 +-
-+ .../backend_tools_web_fallback_agent.py.md         |     2 +-
-+ .../codebase/backend_utils_api_tracker.py.md       |     2 +-
-+ .../codebase/backend_utils_environment.py.md       |     2 +-
-+ .../codebase/backend_utils_firestore_helpers.py.md |     2 +-
-+ .../codebase/backend_utils_http_client.py.md       |     2 +-
-+ docs/autogen/codebase/backend_utils_init_.py.md    |     2 +-
-+ .../codebase/backend_utils_json_helpers.py.md      |     2 +-
-+ .../codebase/backend_utils_timestamps.py.md        |     2 +-
-+ docs/autogen/codebase/backend_uv.lock.md           |     2 +-
-+ .../codebase/backend_workers_celery_app.py.md      |     2 +-
-+ .../codebase/backend_workers_chaos_worker.py.md    |     2 +-
-+ .../codebase/config_.pre-commit-config.yaml.md     |     2 +-
-+ docs/autogen/codebase/config_audit-rules.yml.md    |     2 +-
-+ .../codebase/config_compliance-rules.yml.md        |     2 +-
-+ docs/autogen/codebase/config_docker-limits.yml.md  |     2 +-
-+ docs/autogen/codebase/config_firebase.json.md      |     2 +-
-+ .../codebase/config_firestore.indexes.json.md      |     2 +-
-+ docs/autogen/codebase/config_kilo.json.md          |     2 +-
-+ .../codebase/config_promptfooconfig.yaml.md        |     2 +-
-+ docs/autogen/codebase/config_proxy_list.json.md    |     2 +-
-+ .../autogen/codebase/config_routing_policy.json.md |     2 +-
-+ docs/autogen/codebase/config_vercel.json.md        |     2 +-
-+ docs/autogen/codebase/coverage.json.md             |     2 +-
-+ docs/autogen/codebase/coverage.toml.md             |     2 +-
-+ docs/autogen/codebase/docker-compose.yml.md        |     2 +-
-+ .../codebase/evolution_auto_skill_creator.py.md    |     2 +-
-+ .../autogen/codebase/evolution_daily_learner.py.md |     2 +-
-+ .../codebase/evolution_evolution_engine.py.md      |     2 +-
-+ .../codebase/evolution_evolution_react_agent.py.md |     2 +-
-+ docs/autogen/codebase/evolution_self_updater.py.md |     2 +-
-+ docs/autogen/codebase/find_duplicate_files.py.md   |     2 +-
-+ docs/autogen/codebase/find_duplicate_tests.py.md   |     2 +-
-+ .../infrastructure_check_deploy_gate.py.md         |     2 +-
-+ ...infrastructure_cloudflare_enhanced-worker.js.md |     2 +-
-+ .../infrastructure_cloudflare_worker.js.md         |     2 +-
-+ .../infrastructure_cloudflare_wrangler.toml.md     |     2 +-
-+ .../infrastructure_cloudrun_autoscale.yaml.md      |     2 +-
-+ .../infrastructure_cloudrun_multi_region.yaml.md   |     2 +-
-+ ...functions_firebase_functions_v1_README_BD.md.md |     2 +-
-+ ...unctions_firebase_functions_v1_api-router.js.md |     2 +-
-+ ..._firebase_functions_v1_deployment-monitor.js.md |     2 +-
-+ ...ctions_firebase_functions_v1_health-smart.js.md |     2 +-
-+ ...ase_functions_firebase_functions_v1_index.js.md |     2 +-
-+ ...irebase_functions_v1_lib_chatClassifier.d.ts.md |     2 +-
-+ ..._firebase_functions_v1_lib_chatClassifier.js.md |     2 +-
-+ ...firebase_functions_v1_lib_email_handler.d.ts.md |     2 +-
-+ ...s_firebase_functions_v1_lib_email_handler.js.md |     2 +-
-+ ...nctions_firebase_functions_v1_lib_index.d.ts.md |     2 +-
-+ ...functions_firebase_functions_v1_lib_index.js.md |     2 +-
-+ ..._firebase_functions_v1_lib_scrapeEngine.d.ts.md |     2 +-
-+ ...ns_firebase_functions_v1_lib_scrapeEngine.js.md |     2 +-
-+ ...e_functions_v1_lib_scrapeHistoryManager.d.ts.md |     2 +-
-+ ...ase_functions_v1_lib_scrapeHistoryManager.js.md |     2 +-
-+ ...functions_firebase_functions_v1_package.json.md |     2 +-
-+ ...ons_firebase_functions_v1_providers-smart.js.md |     2 +-
-+ ...se_functions_v1_server-connection-monitor.js.md |     2 +-
-+ ..._firebase_functions_v1_src_chatClassifier.ts.md |     2 +-
-+ ...dataconnect-admin-generated_esm_index.esm.js.md |     2 +-
-+ ...dataconnect-admin-generated_esm_package.json.md |     2 +-
-+ ...src_dataconnect-admin-generated_index.cjs.js.md |     2 +-
-+ ...1_src_dataconnect-admin-generated_index.d.ts.md |     2 +-
-+ ...src_dataconnect-admin-generated_package.json.md |     2 +-
-+ ...s_firebase_functions_v1_src_email_handler.ts.md |     2 +-
-+ ...functions_firebase_functions_v1_src_index.ts.md |     2 +-
-+ ...ns_firebase_functions_v1_src_scrapeEngine.ts.md |     2 +-
-+ ...ase_functions_v1_src_scrapeHistoryManager.ts.md |     2 +-
-+ ..._firebase_functions_v1_src_scrapeSchema.yaml.md |     2 +-
-+ ...functions_firebase_functions_v1_swagger.yaml.md |     2 +-
-+ ...tions_firebase_functions_v1_system-health.js.md |     2 +-
-+ ...unctions_firebase_functions_v1_tsconfig.json.md |     2 +-
-+ ...irebase_functions_v1_utils_externalClient.js.md |     2 +-
-+ ...rastructure_firebase_functions_ocrTrigger.ts.md |     2 +-
-+ ...ure_monitoring_docker-compose.monitoring.yml.md |     2 +-
-+ ...astructure_monitoring_grafana_dashboard.json.md |     2 +-
-+ ...cture_terraform_root_cause_analysis_agent.py.md |     2 +-
-+ ..._terraform_test_root_cause_analysis_agent.py.md |     2 +-
-+ .../codebase/infrastructure_vitest-report.json.md  |     2 +-
-+ docs/autogen/codebase/package.json.md              |     2 +-
-+ .../codebase/packages_shared-types_package.json.md |     2 +-
-+ .../packages_shared-types_src_conversation.ts.md   |     2 +-
-+ .../codebase/packages_shared-types_src_index.ts.md |     2 +-
-+ .../packages_shared-types_src_message.ts.md        |     2 +-
-+ .../packages_shared-types_tsconfig.json.md         |     2 +-
-+ .../packages_ui-components_package.json.md         |     2 +-
-+ .../packages_ui-components_src_ChatBubble.tsx.md   |     2 +-
-+ .../packages_ui-components_src_index.ts.md         |     2 +-
-+ .../packages_ui-components_tsconfig.json.md        |     2 +-
-+ docs/autogen/codebase/playwright-ct.config.ts.md   |     2 +-
-+ docs/autogen/codebase/pnpm-lock.yaml.md            |     2 +-
-+ docs/autogen/codebase/pnpm-workspace.yaml.md       |     2 +-
-+ docs/autogen/codebase/scratch_job_details.json.md  |     2 +-
-+ docs/autogen/codebase/scratch_smoke_check.py.md    |     2 +-
-+ .../scratch_supremeai_skill_ecosystem_app.py.md    |     2 +-
-+ ...ratch_supremeai_skill_ecosystem_generator.py.md |     2 +-
-+ ..._supremeai_skill_ecosystem_sample_skill.json.md |     2 +-
-+ ...ch_supremeai_skill_ecosystem_skill_schema.py.md |     2 +-
-+ .../codebase/scratch_sync_gsm_secrets.py.md        |     2 +-
-+ docs/autogen/codebase/scratch_update_vault.py.md   |     2 +-
-+ .../autogen/codebase/scratch_update_vault_r2.py.md |     2 +-
-+ .../codebase/scratch_verify_project_health.py.md   |     2 +-
-+ .../codebase/scripts_add_bangla_comments.py.md     |     2 +-
-+ .../codebase/scripts_aggregate_context.py.md       |     2 +-
-+ ...scripts_backup_auto_cross_cloud_replicate.py.md |     2 +-
-+ .../scripts_backup_auto_firestore_backup.py.md     |     2 +-
-+ .../scripts_benchmark_perf_benchmark.py.md         |     2 +-
-+ .../codebase/scripts_bots_auto_alert_bot.py.md     |     2 +-
-+ .../scripts_bots_auto_daily_standup_bot.py.md      |     2 +-
-+ .../codebase/scripts_code_smell_detector.py.md     |     2 +-
-+ docs/autogen/codebase/scripts_codebase_to_md.py.md |     2 +-
-+ .../codebase/scripts_codegraph_integration.py.md   |     2 +-
-+ .../codebase/scripts_commit_supreme_ci.yml.md      |     2 +-
-+ docs/autogen/codebase/scripts_config_audit.py.md   |     2 +-
-+ .../scripts_core_engine_multicatalog_search.py.md  |     2 +-
-+ .../codebase/scripts_core_engine_tool_ranker.py.md |     2 +-
-+ .../codebase/scripts_create_test_admin.py.md       |     2 +-
-+ .../autogen/codebase/scripts_db_auto_migrate.py.md |     2 +-
-+ docs/autogen/codebase/scripts_db_auto_seed.py.md   |     2 +-
-+ .../autogen/codebase/scripts_docker_ai_guard.py.md |     2 +-
-+ ...ipts_evolution_auto_marketing_skill_forge.py.md |     2 +-
-+ docs/autogen/codebase/scripts_fix_mypy.py.md       |     2 +-
-+ docs/autogen/codebase/scripts_fuzz_sandbox.py.md   |     2 +-
-+ .../scripts_generate_codebase_markdown.py.md       |     2 +-
-+ ...scripts_generate_codebase_single_markdown.py.md |     2 +-
-+ docs/autogen/codebase/scripts_generate_md.py.md    |     2 +-
-+ .../codebase/scripts_generate_smart_docs.py.md     |     2 +-
-+ docs/autogen/codebase/scripts_k6_load_test.js.md   |     2 +-
-+ docs/autogen/codebase/scripts_locustfile.py.md     |     2 +-
-+ docs/autogen/codebase/scripts_migrate.py.md        |     2 +-
-+ .../codebase/scripts_multi_model_validator.py.md   |     2 +-
-+ ...scripts_orchestrator_auto_budget_guardian.py.md |     2 +-
-+ docs/autogen/codebase/scripts_profile_memory.py.md |     2 +-
-+ .../scripts_quality_auto_dead_code_remover.py.md   |     2 +-
-+ .../scripts_quality_auto_improve_coverage.py.md    |     2 +-
-+ .../scripts_quality_auto_refactor_suggester.py.md  |     2 +-
-+ ...cripts_quality_check_ollama_test_coverage.py.md |     2 +-
-+ .../scripts_resource_collection_awesome_go.py.md   |     2 +-
-+ ...cripts_resource_collection_awesome_python.py.md |     2 +-
-+ ...ts_resource_collection_awesome_selfhosted.py.md |     2 +-
-+ ...ripts_resource_collection_base_api_client.py.md |     2 +-
-+ .../scripts_resource_collection_base_scraper.py.md |     2 +-
-+ ...pts_resource_collection_ossinsight_client.py.md |     2 +-
-+ ...ipts_resource_collection_ossinsight_init_.py.md |     2 +-
-+ ...ripts_resource_collection_ossinsight_test.py.md |     2 +-
-+ .../scripts_resource_collection_run_all.py.md      |     2 +-
-+ ...ts_resource_collection_run_all_collectors.py.md |     2 +-
-+ ...ripts_resource_scraping_awesome_go_scrape.py.md |     2 +-
-+ ...s_resource_scraping_awesome_python_scrape.py.md |     2 +-
-+ ...source_scraping_awesome_selfhosted_scrape.py.md |     2 +-
-+ .../codebase/scripts_run_all_collectors.py.md      |     2 +-
-+ docs/autogen/codebase/scripts_safety_guard.py.md   |     2 +-
-+ .../scripts_security_auto_find_blindspots.py.md    |     2 +-
-+ .../scripts_security_auto_secret_rotate.py.md      |     2 +-
-+ .../scripts_security_check_dependencies.py.md      |     2 +-
-+ .../codebase/scripts_security_code-quality.yml.md  |     2 +-
-+ ...scripts_security_dependency-health-check.yml.md |     2 +-
-+ .../codebase/scripts_security_find_dead_code.py.md |     2 +-
-+ docs/autogen/codebase/scripts_seed_repos.py.md     |     2 +-
-+ .../autogen/codebase/scripts_setup_ci_runner.py.md |     2 +-
-+ .../codebase/scripts_setup_firebase_admin.py.md    |     2 +-
-+ docs/autogen/codebase/scripts_skill_loader.py.md   |     2 +-
-+ .../codebase/scripts_supreme-config-audit.py.md    |     2 +-
-+ .../codebase/scripts_supreme-docker-analyzer.py.md |     2 +-
-+ .../codebase/scripts_supreme-risk-scorer.py.md     |     2 +-
-+ .../codebase/scripts_supreme_context_builder.py.md |     2 +-
-+ .../scripts_tenant_auto_tenant_health_report.py.md |     2 +-
-+ .../scripts_tenant_auto_tenant_setup.py.md         |     2 +-
-+ docs/autogen/codebase/scripts_test_bangla.py.md    |     2 +-
-+ docs/autogen/codebase/scripts_test_read.py.md      |     2 +-
-+ docs/autogen/codebase/security-scan.yml.md         |     2 +-
-+ .../codebase/skills_dynamic_csv_exporter.py.md     |     2 +-
-+ .../codebase/skills_dynamic_text_summarizer.py.md  |     2 +-
-+ .../codebase/skills_dynamic_web_scraper.py.md      |     2 +-
-+ docs/autogen/codebase/skills_init_.py.md           |     2 +-
-+ docs/autogen/codebase/skills_installer.py.md       |     2 +-
-+ docs/autogen/codebase/skills_marketplace.py.md     |     2 +-
-+ docs/autogen/codebase/skills_registry.py.md        |     2 +-
-+ docs/autogen/codebase/skills_schema.py.md          |     2 +-
-+ .../codebase/test-results_.last-run.json.md        |     2 +-
-+ .../codebase/tests_e2e_admin-dashboard.spec.ts.md  |     2 +-
-+ docs/autogen/codebase/tests_e2e_chat.spec.ts.md    |     2 +-
-+ .../codebase/tests_e2e_playwright.config.ts.md     |     2 +-
-+ docs/autogen/codebase/tests_e2e_visual.spec.ts.md  |     2 +-
-+ docs/autogen/codebase/tests_test_tenant_di.py.md   |     2 +-
-+ docs/autogen/codebase/tools_cache_cleanup.py.md    |     2 +-
-+ .../tools_vscode-extension_ARCHITECTURE_BN.md.md   |     2 +-
-+ ...vscode-extension_AdminMetricsController.java.md |     2 +-
-+ ...s_vscode-extension_CodebaseAuditService.java.md |     2 +-
-+ ...ools_vscode-extension_FeatureDefinition.java.md |     2 +-
-+ ...ode-extension_FeatureRegistryController.java.md |     2 +-
-+ ...vscode-extension_FeatureRegistryService.java.md |     2 +-
-+ .../tools_vscode-extension_GlobalMetrics.java.md   |     2 +-
-+ ...s_vscode-extension_GlobalMetricsService.java.md |     2 +-
-+ ...ols_vscode-extension_INTEGRATION_GUIDE_BN.md.md |     2 +-
-+ .../codebase/tools_vscode-extension_README.md.md   |     2 +-
-+ .../tools_vscode-extension_README_BN.md.md         |     2 +-
-+ .../tools_vscode-extension_jest.config.js.md       |     2 +-
-+ .../tools_vscode-extension_package.json.md         |     2 +-
-+ .../tools_vscode-extension_package.nls.bn.json.md  |     2 +-
-+ .../tools_vscode-extension_src_agentDetector.ts.md |     2 +-
-+ .../tools_vscode-extension_src_ai_AIService.ts.md  |     2 +-
-+ ...de-extension_src_ai_CodeGenerationService.ts.md |     2 +-
-+ ...vscode-extension_src_ai_CodeReviewService.ts.md |     2 +-
-+ ...ls_vscode-extension_src_ai_ContextBuilder.ts.md |     2 +-
-+ ...xtension_src_dataconnect-generated_README.md.md |     2 +-
-+ ...n_src_dataconnect-generated_esm_index.esm.js.md |     2 +-
-+ ...n_src_dataconnect-generated_esm_package.json.md |     2 +-
-+ ...nsion_src_dataconnect-generated_index.cjs.js.md |     2 +-
-+ ...tension_src_dataconnect-generated_index.d.ts.md |     2 +-
-+ ...nsion_src_dataconnect-generated_package.json.md |     2 +-
-+ .../tools_vscode-extension_src_extension.ts.md     |     2 +-
-+ ...de-extension_src_handlers_CodeEditHandler.ts.md |     2 +-
-+ ...de-extension_src_handlers_CodeFlowHandler.ts.md |     2 +-
-+ ...scode-extension_src_handlers_ErrorHandler.ts.md |     2 +-
-+ ...de-extension_src_handlers_FeedbackHandler.ts.md |     2 +-
-+ ...ode-extension_src_providers_CodeFlowPanel.ts.md |     2 +-
-+ ...nsion_src_providers_StreamingChatProvider.ts.md |     2 +-
-+ ...n_src_providers_SupremeAIActivityProvider.ts.md |     2 +-
-+ ...providers_SupremeAIAdminDashboardProvider.ts.md |     2 +-
-+ ...nsion_src_providers_SupremeAIChatProvider.ts.md |     2 +-
-+ ...extension_src_providers_SupremeAIChatView.ts.md |     2 +-
-+ ...viders_SupremeAICustomerDashboardProvider.ts.md |     2 +-
-+ ...on_src_providers_SupremeAISidebarProvider.ts.md |     2 +-
-+ ...vscode-extension_src_services_AuthService.ts.md |     2 +-
-+ ...e-extension_src_services_SupremeAIService.ts.md |     2 +-
-+ .../tools_vscode-extension_src_types_index.ts.md   |     2 +-
-+ ...ension_src_utils_DynamicSignatureRegistry.ts.md |     2 +-
-+ ...s_vscode-extension_test_auth-service.test.ts.md |     2 +-
-+ ...ools_vscode-extension_test_mocks_vscode.d.ts.md |     2 +-
-+ .../tools_vscode-extension_test_mocks_vscode.ts.md |     2 +-
-+ .../tools_vscode-extension_test_setup.ts.md        |     2 +-
-+ ...ode-extension_test_supremeai-service.test.ts.md |     2 +-
-+ .../tools_vscode-extension_tsconfig.json.md        |     2 +-
-+ .../tools_vscode-extension_vitest.config.ts.md     |     2 +-
-+ docs/autogen/codebase/turbo.json.md                |     2 +-
-+ docs/autogen/codebase/visual.spec.ts.md            |     2 +-
-+ docs/autogen/codebase_full.md                      |  1491 ++-
-+ 1058 files changed, 15439 insertions(+), 12171 deletions(-)
-+
-+```
-+
-+## Diff Detail
-+```diff
-+commit 19e2f4019bb7a2aef85243afe61c87a137171a2c
-+Author: github-actions[bot] <github-actions[bot]@users.noreply.github.com>
-+Date:   Sat Jul 4 05:33:42 2026 +0000
-+
-+    docs: auto-update codebase docs & dashboard [skip ci]
-+
-+diff --git a/docs/autogen/INDEX.md b/docs/autogen/INDEX.md
-+index dd03785ab..b7dcec781 100644
-+--- a/docs/autogen/INDEX.md
-++++ b/docs/autogen/INDEX.md
-+@@ -13,4 +13,4 @@
-+ - **ডিরেক্টরি:** [changes/](changes/)
-+ 
-+ ---
-+-*স্বয়ংক্রিয়ভাবে তৈরি — 2026-07-04 05:29:43*
-++*স্বয়ংক্রিয়ভাবে তৈরি — 2026-07-04 05:33:42*
-+diff --git a/docs/autogen/changes/change_1502ebdec61ad3725a47e0a6db5e6670dfd49ac7.md b/docs/autogen/changes/change_1502ebdec61ad3725a47e0a6db5e6670dfd49ac7.md
-+new file mode 100644
-+index 000000000..5585e750a
-+--- /dev/null
-++++ b/docs/autogen/changes/change_1502ebdec61ad3725a47e0a6db5e6670dfd49ac7.md
-+@@ -0,0 +1,11354 @@
-++# 📋 Commit 1502ebdec61ad3725a47e0a6db5e6670dfd49ac7
-++
-++## Commit Stats
-++```
-++commit 1502ebdec61ad3725a47e0a6db5e6670dfd49ac7
-++Author: github-actions[bot] <github-actions[bot]@users.noreply.github.com>
-++Date:   Sat Jul 4 05:29:44 2026 +0000
-++
-++    docs: auto-update codebase docs & dashboard [skip ci]
-++
-++ docs/autogen/INDEX.md                              |     2 +-
-++ ...nge_140a3e78c8e7358e7c7dafcd497bd80acc075233.md |    45 +
-++ ...nge_20ab02ef3d40d7f4ec9a2d746a13f7e24501c001.md |  5139 ---------
-++ ...nge_217b7dd9c45b211ada615af859cab2360ce79389.md | 10721 -------------------
-++ ...nge_7f2e698247a8411b93565029350add8565fbb92f.md |  9035 ++++++++++++++++
-++ .../.github_actions_setup-backend_action.yml.md    |     2 +-
-++ ...github_scripts_advanced-validation-report.py.md |     2 +-
-++ .../codebase/.github_scripts_canary-deploy.py.md   |     2 +-
-++ .../codebase/.github_scripts_ci-auto-fix-v3.py.md  |     2 +-
-++ .../codebase/.github_scripts_ci-auto-fix.py.md     |     2 +-
-++ .../.github_scripts_ci-decision-engine.py.md       |     2 +-
-++ .../codebase/.github_scripts_ci-health-check.py.md |     2 +-
-++ .../.github_scripts_clean_action_logs.py.md        |     2 +-
-++ .../codebase/.github_scripts_deploy-backend.py.md  |     2 +-
-++ .../.github_scripts_detect-previous-failures.py.md |     2 +-
-++ .../codebase/.github_scripts_enforce_24h_gap.py.md |     2 +-
-++ .../.github_scripts_generate-ci-report.py.md       |     2 +-
-++ .../.github_scripts_generate_ai_prompt.py.md       |     2 +-
-++ .../.github_scripts_multi-model-evaluator.py.md    |     2 +-
-++ docs/autogen/codebase/.github_scripts_review.py.md |     2 +-
-++ .../.github_scripts_supremeai-evaluator.py.md      |     2 +-
-++ .../.github_scripts_test_ai_reviewer.py.md         |     2 +-
-++ .../codebase/.github_workflows_deploy.yml.md       |     2 +-
-++ .../.github_workflows_nightly-maintenance.yml.md   |     2 +-
-++ .../.github_workflows_supreme-core-ci.yml.md       |     2 +-
-++ .../.github_workflows_supreme-mobile-cd.yml.md     |     2 +-
-++ ....github_workflows_supreme-release-builds.yml.md |     2 +-
-++ .../.github_workflows_sync-from-prod.yml.md        |     2 +-
-++ docs/autogen/codebase/AGENT.md.md                  |     2 +-
-++ docs/autogen/codebase/AGENTS.md.md                 |     2 +-
-++ docs/autogen/codebase/CHANGELOG.md.md              |     2 +-
-++ docs/autogen/codebase/CI_PIPELINE.md.md            |     2 +-
-++ docs/autogen/codebase/CONTRIBUTING.md.md           |     2 +-
-++ docs/autogen/codebase/IMPLEMENTATION_STATUS.md.md  |     2 +-
-++ .../codebase/PRODUCTION_READINESS_GUIDE.md.md      |     2 +-
-++ docs/autogen/codebase/README.md.md                 |     2 +-
-++ docs/autogen/codebase/SECURITY.md.md               |     2 +-
-++ docs/autogen/codebase/accessibility.spec.ts.md     |     2 +-
-++ docs/autogen/codebase/admin_dashboard_script.js.md |     2 +-
-++ docs/autogen/codebase/admin_god.py.md              |     2 +-
-++ docs/autogen/codebase/apps_desktop_README.md.md    |     2 +-
-++ docs/autogen/codebase/apps_desktop_package.json.md |     2 +-
-++ .../codebase/apps_desktop_src-tauri_Cargo.toml.md  |     2 +-
-++ .../codebase/apps_desktop_src-tauri_build.rs.md    |     2 +-
-++ .../apps_desktop_src-tauri_secure-store.ts.md      |     2 +-
-++ .../codebase/apps_desktop_src-tauri_src_main.rs.md |     2 +-
-++ .../apps_desktop_src-tauri_tauri.conf.json.md      |     2 +-
-++ .../codebase/apps_desktop_src-ui_package.json.md   |     2 +-
-++ .../codebase/apps_desktop_src-ui_src_App.tsx.md    |     2 +-
-++ ..._desktop_src-ui_src_components_ChatInput.tsx.md |     2 +-
-++ .../codebase/apps_desktop_src-ui_src_main.tsx.md   |     2 +-
-++ .../apps_desktop_src-ui_src_pages_AdminPage.tsx.md |     2 +-
-++ .../apps_desktop_src-ui_src_pages_ChatPage.tsx.md  |     2 +-
-++ ...s_desktop_src-ui_src_pages_EvolutionPage.tsx.md |     2 +-
-++ .../apps_desktop_src-ui_src_pages_LoginPage.tsx.md |     2 +-
-++ ...apps_desktop_src-ui_src_pages_SkillsPage.tsx.md |     2 +-
-++ .../apps_desktop_src-ui_src_services_api.ts.md     |     2 +-
-++ .../apps_desktop_src-ui_src_stores_authStore.ts.md |     2 +-
-++ .../apps_desktop_src-ui_src_types_index.ts.md      |     2 +-
-++ .../apps_desktop_src-ui_src_vite-env.d.ts.md       |     2 +-
-++ .../codebase/apps_desktop_src-ui_tsconfig.json.md  |     2 +-
-++ .../apps_desktop_src-ui_tsconfig.node.json.md      |     2 +-
-++ .../codebase/apps_desktop_src-ui_vite.config.ts.md |     2 +-
-++ ...ava_com_supremeai_JavaWorkerApplication.java.md |     2 +-
-++ ...va_com_supremeai_grpc_WorkerServiceImpl.java.md |     2 +-
-++ ...in_java_com_supremeai_models_TaskEntity.java.md |     2 +-
-++ ...m_supremeai_repositories_TaskRepository.java.md |     2 +-
-++ ...va-worker_src_main_resources_application.yml.md |     2 +-
-++ docs/autogen/codebase/apps_mobile_README.md.md     |     2 +-
-++ docs/autogen/codebase/apps_mobile_README_BD.md.md  |     2 +-
-++ .../codebase/apps_mobile_analysis_options.yaml.md  |     2 +-
-++ .../codebase/apps_mobile_assets_i18n_ar.json.md    |     2 +-
-++ .../codebase/apps_mobile_assets_i18n_bn.json.md    |     2 +-
-++ .../codebase/apps_mobile_assets_i18n_en.json.md    |     2 +-
-++ .../codebase/apps_mobile_assets_i18n_es.json.md    |     2 +-
-++ .../codebase/apps_mobile_assets_i18n_hi.json.md    |     2 +-
-++ .../codebase/apps_mobile_assets_i18n_zh.json.md    |     2 +-
-++ .../codebase/apps_mobile_devtools_options.yaml.md  |     2 +-
-++ ...ts.xcassets_AppIcon.appiconset_Contents.json.md |     2 +-
-++ ....xcassets_LaunchImage.imageset_Contents.json.md |     2 +-
-++ ...sets.xcassets_LaunchImage.imageset_README.md.md |     2 +-
-++ ...s_mobile_lib_dataconnect_generated_README.md.md |     2 +-
-++ ...le_lib_dataconnect_generated_add_review.dart.md |     2 +-
-++ ..._lib_dataconnect_generated_create_movie.dart.md |     2 +-
-++ ...lib_dataconnect_generated_delete_review.dart.md |     2 +-
-++ ...ile_lib_dataconnect_generated_generated.dart.md |     2 +-
-++ ...b_dataconnect_generated_get_movie_by_id.dart.md |     2 +-
-++ ...e_lib_dataconnect_generated_list_movies.dart.md |     2 +-
-++ ...dataconnect_generated_list_user_reviews.dart.md |     2 +-
-++ ...le_lib_dataconnect_generated_list_users.dart.md |     2 +-
-++ ..._lib_dataconnect_generated_search_movie.dart.md |     2 +-
-++ ...e_lib_dataconnect_generated_upsert_user.dart.md |     2 +-
-++ docs/autogen/codebase/apps_mobile_lib_main.dart.md |     2 +-
-++ .../apps_mobile_lib_models_ci_job_model.dart.md    |     2 +-
-++ ...apps_mobile_lib_providers_auth_provider.dart.md |     2 +-
-++ ...mobile_lib_providers_dashboard_provider.dart.md |     2 +-
-++ ...le_lib_providers_orchestration_provider.dart.md |     2 +-
-++ ..._mobile_lib_providers_settings_provider.dart.md |     2 +-
-++ ...ps_mobile_lib_screens_agent_chat_screen.dart.md |     2 +-
-++ ...mobile_lib_screens_alerts_alerts_screen.dart.md |     2 +-
-++ ..._lib_screens_analytics_analytics_screen.dart.md |     2 +-
-++ ...apps_mobile_lib_screens_api_keys_screen.dart.md |     2 +-
-++ .../apps_mobile_lib_screens_api_scaffold.dart.md   |     2 +-
-++ ...apps_mobile_lib_screens_byoc_hub_screen.dart.md |     2 +-
-++ ..._lib_screens_consensus_consensus_screen.dart.md |     2 +-
-++ ...obile_lib_screens_dashboard_home_screen.dart.md |     2 +-
-++ ...pps_mobile_lib_screens_dashboard_screen.dart.md |     2 +-
-++ ..._lib_screens_extension_extension_screen.dart.md |     2 +-
-++ .../apps_mobile_lib_screens_git_git_screen.dart.md |     2 +-
-++ ...le_lib_screens_learning_learning_screen.dart.md |     2 +-
-++ .../apps_mobile_lib_screens_login_screen.dart.md   |     2 +-
-++ ...eens_notifications_notifications_screen.dart.md |     2 +-
-++ ...b_screens_projects_projects_list_screen.dart.md |     2 +-
-++ ...b_screens_providers_ai_providers_screen.dart.md |     2 +-
-++ ...s_mobile_lib_screens_quota_quota_screen.dart.md |     2 +-
-++ ...ib_screens_resilience_resilience_screen.dart.md |     2 +-
-++ ...apps_mobile_lib_screens_settings_screen.dart.md |     2 +-
-++ .../apps_mobile_lib_screens_terminal_view.dart.md  |     2 +-
-++ .../apps_mobile_lib_screens_vpn_vpn_screen.dart.md |     2 +-
-++ .../apps_mobile_lib_screens_wallet_screen.dart.md  |     2 +-
-++ .../apps_mobile_lib_services_api_client.dart.md    |     2 +-
-++ .../apps_mobile_lib_services_api_service.dart.md   |     2 +-
-++ ...pps_mobile_lib_services_billing_service.dart.md |     2 +-
-++ .../apps_mobile_lib_services_byoc_service.dart.md  |     2 +-
-++ ...pps_mobile_lib_services_ci_sync_service.dart.md |     2 +-
-++ ...s_mobile_lib_services_deployment_stream.dart.md |     2 +-
-++ ...obile_lib_services_localization_service.dart.md |     2 +-
-++ ...bile_lib_services_neural_stream_service.dart.md |     2 +-
-++ ...obile_lib_services_notification_service.dart.md |     2 +-
-++ ...obile_lib_services_offline_sync_service.dart.md |     2 +-
-++ ...ile_lib_services_payment_gateway_bridge.dart.md |     2 +-
-++ ..._mobile_lib_services_screen_api_service.dart.md |     2 +-
-++ .../apps_mobile_lib_theme_app_theme.dart.md        |     2 +-
-++ .../apps_mobile_lib_theme_theme_provider.dart.md   |     2 +-
-++ ...apps_mobile_lib_widgets_action_hub_card.dart.md |     2 +-
-++ ...ile_lib_widgets_base_dashboard_scaffold.dart.md |     2 +-
-++ .../codebase/apps_mobile_lib_widgets_es.json.md    |     2 +-
-++ .../apps_mobile_lib_widgets_json_dropzone.dart.md  |     2 +-
-++ .../apps_mobile_lib_widgets_live_terminal.dart.md  |     2 +-
-++ ...apps_mobile_lib_widgets_loading_widgets.dart.md |     2 +-
-++ ...le_lib_widgets_transaction_history_list.dart.md |     2 +-
-++ .../apps_mobile_lib_widgets_usage_chart.dart.md    |     2 +-
-++ ...ts.xcassets_AppIcon.appiconset_Contents.json.md |     2 +-
-++ docs/autogen/codebase/apps_mobile_pubspec.lock.md  |     2 +-
-++ docs/autogen/codebase/apps_mobile_pubspec.yaml.md  |     2 +-
-++ ...bile_test_auth_provider_edge_cases_test.dart.md |     2 +-
-++ .../apps_mobile_test_auth_provider_test.dart.md    |     2 +-
-++ ...mobile_test_home_screen_edge_cases_test.dart.md |     2 +-
-++ .../apps_mobile_test_home_screen_test.dart.md      |     2 +-
-++ ...s_mobile_test_screens_login_screen_test.dart.md |     2 +-
-++ .../codebase/apps_mobile_web_manifest.json.md      |     2 +-
-++ .../codebase/apps_studio-client_README.md.md       |     2 +-
-++ .../codebase/apps_studio-client_components.json.md |     2 +-
-++ .../apps_studio-client_eslint.config.js.md         |     2 +-
-++ .../autogen/codebase/apps_studio-client_main.js.md |     2 +-
-++ .../codebase/apps_studio-client_package.json.md    |     2 +-
-++ .../apps_studio-client_public_manifest.json.md     |     2 +-
-++ .../codebase/apps_studio-client_public_sw.js.md    |     2 +-
-++ .../apps_studio-client_src_App.test.tsx.md         |     2 +-
-++ .../codebase/apps_studio-client_src_App.tsx.md     |     2 +-
-++ ...tudio-client_src_components_AdminConsole.tsx.md |     2 +-
-++ ..._studio-client_src_components_BanglaHint.tsx.md |     2 +-
-++ ...apps_studio-client_src_components_Header.tsx.md |     2 +-
-++ ...lient_src_components_LiveSujonBackground.tsx.md |     2 +-
-++ ...c_components_Onboarding_OnboardingWizard.tsx.md |     2 +-
-++ ...ent_src_components_Onboarding_StepApiKey.tsx.md |     2 +-
-++ ..._src_components_Onboarding_StepFirstChat.tsx.md |     2 +-
-++ ...rc_components_Onboarding_StepModelSelect.tsx.md |     2 +-
-++ ...dio-client_src_components_OperatorStudio.tsx.md |     2 +-
-++ ...o-client_src_components_admin_ActionCard.tsx.md |     2 +-
-++ ..._src_components_admin_AdminAuthenticated.tsx.md |     2 +-
-++ ...client_src_components_admin_AdminConsole.tsx.md |     2 +-
-++ ..._src_components_admin_AdminDashboardHome.tsx.md |     2 +-
-++ ...o-client_src_components_admin_AdminLogin.tsx.md |     2 +-
-++ ..._src_components_admin_AdminSubTabContent.tsx.md |     2 +-
-++ ...-client_src_components_admin_AdminTopNav.tsx.md |     2 +-
-++ ...o-client_src_components_admin_AethelNode.tsx.md |     2 +-
-++ ...ient_src_components_admin_AuditLogsPanel.tsx.md |     2 +-
-++ ...lient_src_components_admin_BackupRestore.tsx.md |     2 +-
-++ ...ient_src_components_admin_CICDVisualizer.tsx.md |     2 +-
-++ ...t_src_components_admin_CloudOrchestrator.tsx.md |     2 +-
-++ ...lient_src_components_admin_CommandCenter.tsx.md |     2 +-
-++ ...client_src_components_admin_ConfigEditor.tsx.md |     2 +-
-++ ..._src_components_admin_ConsentMatrixModal.tsx.md |     2 +-
-++ ...-client_src_components_admin_CostAuditor.tsx.md |     2 +-
-++ ..._components_admin_DashboardErrorBoundary.tsx.md |     2 +-
-++ ...ent_src_components_admin_DeploymentModal.tsx.md |     2 +-
-++ ...client_src_components_admin_DynamicPanel.tsx.md |     2 +-
-++ ...omponents_admin_EnhancedSkillMarketplace.tsx.md |     2 +-
-++ ...t_src_components_admin_GithubIntegration.tsx.md |     2 +-
-++ ...client_src_components_admin_HealthBanner.tsx.md |     2 +-
-++ ...io-client_src_components_admin_HealthMap.tsx.md |     2 +-
-++ ..._src_components_admin_InteractiveChatTab.tsx.md |     2 +-
-++ ...dio-client_src_components_admin_LiveLogs.tsx.md |     2 +-
-++ ...lient_src_components_admin_MemoryBrowser.tsx.md |     2 +-
-++ ...-client_src_components_admin_ModelRouter.tsx.md |     2 +-
-++ ..._components_admin_ObservabilityDashboard.tsx.md |     2 +-
-++ ...-client_src_components_admin_RBACManager.tsx.md |     2 +-
-++ ...nt_src_components_admin_RateLimitManager.tsx.md |     2 +-
-++ ...rc_components_admin_RealTimeMetricsPanel.tsx.md |     2 +-
-++ ...mponents_admin_RedesignedDashboardMockup.tsx.md |     2 +-
-++ ...nt_src_components_admin_RulesEnginePanel.tsx.md |     2 +-
-++ ...t_src_components_admin_SecurityDashboard.tsx.md |     2 +-
-++ ...rc_components_admin_ServiceHealthMetrics.tsx.md |     2 +-
-++ ...ent_src_components_admin_ThreatDetection.tsx.md |     2 +-
-++ ...-client_src_components_admin_UserManager.tsx.md |     2 +-
-++ ..._src_components_admin_VisualRulesBuilder.tsx.md |     2 +-
-++ ..._studio-client_src_components_admin_index.ts.md |     2 +-
-++ ..._src_components_audio_WaveformVisualizer.tsx.md |     2 +-
-++ ...ient_src_components_chat_TypingIndicator.tsx.md |     2 +-
-++ ...nt_src_components_chat_UnifiedChatBubble.tsx.md |     2 +-
-++ ...s_studio-client_src_components_chat_index.ts.md |     2 +-
-++ ...t_src_components_customer_BrowserPreview.tsx.md |     2 +-
-++ ...t_src_components_customer_ChatPanel.test.tsx.md |     2 +-
-++ ...client_src_components_customer_ChatPanel.tsx.md |     2 +-
-++ ...lient_src_components_customer_CodeEditor.tsx.md |     2 +-
-++ ...-client_src_components_customer_HomeFeed.tsx.md |     2 +-
-++ ..._src_components_customer_MobileSimulator.tsx.md |     2 +-
-++ ...rc_components_customer_QuickPresets.test.tsx.md |     2 +-
-++ ...ent_src_components_customer_QuickPresets.tsx.md |     2 +-
-++ ...c_components_customer_UserDashboard.test.tsx.md |     2 +-
-++ ...nt_src_components_customer_UserDashboard.tsx.md |     2 +-
-++ ...udio-client_src_components_customer_index.ts.md |     2 +-
-++ ...components_dashboard_AutomationQueuePage.tsx.md |     2 +-
-++ ...components_dashboard_DashboardShell.test.tsx.md |     2 +-
-++ ..._src_components_dashboard_DashboardShell.tsx.md |     2 +-
-++ ...t_src_components_dashboard_KnowledgePage.tsx.md |     2 +-
-++ ..._src_components_dashboard_LlmGatewayPage.tsx.md |     2 +-
-++ ...ent_src_components_dashboard_SecretsPage.tsx.md |     2 +-
-++ ...c_components_dashboard_SessionDetailPage.tsx.md |     2 +-
-++ ...nt_src_components_dashboard_SessionsPage.tsx.md |     2 +-
-++ ...nt_src_components_dashboard_SettingsPage.tsx.md |     2 +-
-++ ...src_components_dashboard_SiteActionsPage.tsx.md |     2 +-
-++ ...lient_src_components_dashboard_UsagePage.tsx.md |     2 +-
-++ ...lient_src_components_dashboard_VaultPage.tsx.md |     2 +-
-++ ...ent_src_components_dashboard_sessionStore.ts.md |     2 +-
-++ ...ent_src_components_dashboard_useHashRoute.ts.md |     2 +-
-++ ...lient_src_components_editor_CollabEditor.tsx.md |     2 +-
-++ ...o-client_src_components_graph_SkillGraph.tsx.md |     2 +-
-++ ...udio-client_src_components_ui_ActionCard.tsx.md |     2 +-
-++ ...ps_studio-client_src_components_ui_Badge.tsx.md |     2 +-
-++ ...pps_studio-client_src_components_ui_Card.tsx.md |     2 +-
-++ ...studio-client_src_components_ui_Skeleton.tsx.md |     2 +-
-++ ...pps_studio-client_src_components_ui_index.ts.md |     2 +-
-++ ..._studio-client_src_contexts_ThemeContext.tsx.md |     2 +-
-++ ...o-client_src_dataconnect-generated_README.md.md |     2 +-
-++ ...t_src_dataconnect-generated_esm_index.esm.js.md |     2 +-
-++ ...t_src_dataconnect-generated_esm_package.json.md |     2 +-
-++ ...lient_src_dataconnect-generated_index.cjs.js.md |     2 +-
-++ ...-client_src_dataconnect-generated_index.d.ts.md |     2 +-
-++ ...lient_src_dataconnect-generated_package.json.md |     2 +-
-++ ...nt_src_dataconnect-generated_react_README.md.md |     2 +-
-++ ...dataconnect-generated_react_esm_index.esm.js.md |     2 +-
-++ ...dataconnect-generated_react_esm_package.json.md |     2 +-
-++ ...src_dataconnect-generated_react_index.cjs.js.md |     2 +-
-++ ...t_src_dataconnect-generated_react_index.d.ts.md |     2 +-
-++ ...src_dataconnect-generated_react_package.json.md |     2 +-
-++ .../codebase/apps_studio-client_src_firebase.ts.md |     2 +-
-++ .../apps_studio-client_src_hooks_index.ts.md       |     2 +-
-++ ...lient_src_hooks_tests_useTranslation.test.ts.md |     2 +-
-++ .../apps_studio-client_src_hooks_useAdminApi.ts.md |     2 +-
-++ .../apps_studio-client_src_hooks_useAuth.ts.md     |     2 +-
-++ .../apps_studio-client_src_hooks_useChat.ts.md     |     2 +-
-++ ..._studio-client_src_hooks_useDashboardData.ts.md |     2 +-
-++ ...ps_studio-client_src_hooks_useTranslation.ts.md |     2 +-
-++ ...apps_studio-client_src_hooks_useWebSocket.ts.md |     2 +-
-++ ...apps_studio-client_src_i18n_I18nProvider.tsx.md |     2 +-
-++ .../apps_studio-client_src_i18n_config.ts.md       |     2 +-
-++ .../apps_studio-client_src_i18n_translations.ts.md |     2 +-
-++ .../codebase/apps_studio-client_src_lib_etag.ts.md |     2 +-
-++ .../codebase/apps_studio-client_src_main.tsx.md    |     2 +-
-++ ...s_studio-client_src_services_adminService.ts.md |     2 +-
-++ ...tudio-client_src_services_adminTokenStore.ts.md |     2 +-
-++ ...s_studio-client_src_services_agentService.ts.md |     2 +-
-++ ...apps_studio-client_src_services_apiClient.ts.md |     2 +-
-++ ...ient_src_services_api_microserviceMonitor.ts.md |     2 +-
-++ ...t_src_services_audio_AudioPlaybackService.ts.md |     2 +-
-++ ...t_src_services_audio_AudioRecorderService.ts.md |     2 +-
-++ ...ps_studio-client_src_services_authService.ts.md |     2 +-
-++ ...ps_studio-client_src_services_chatService.ts.md |     2 +-
-++ ...tudio-client_src_services_ciReportService.ts.md |     2 +-
-++ ...pps_studio-client_src_services_storageApi.ts.md |     2 +-
-++ .../apps_studio-client_src_store_adminStore.ts.md  |     2 +-
-++ ...pps_studio-client_src_store_customerStore.ts.md |     2 +-
-++ ...ps_studio-client_src_store_dashboardStore.ts.md |     2 +-
-++ .../apps_studio-client_src_store_themeStore.ts.md  |     2 +-
-++ .../apps_studio-client_src_store_useStore.ts.md    |     2 +-
-++ .../apps_studio-client_src_test_setup.ts.md        |     2 +-
-++ .../codebase/apps_studio-client_src_types.ts.md    |     2 +-
-++ .../apps_studio-client_src_types_customer.ts.md    |     2 +-
-++ .../apps_studio-client_src_utils_api.ts.md         |     2 +-
-++ .../apps_studio-client_src_vite-env.d.ts.md        |     2 +-
-++ ...tudio-client_src_workers_logParser.worker.ts.md |     2 +-
-++ .../apps_studio-client_tsconfig.app.json.md        |     2 +-
-++ .../codebase/apps_studio-client_tsconfig.json.md   |     2 +-
-++ .../apps_studio-client_tsconfig.node.json.md       |     2 +-
-++ .../codebase/apps_studio-client_vite.config.ts.md  |     2 +-
-++ .../apps_studio-client_vitest.config.ts.md         |     2 +-
-++ docs/autogen/codebase/apps_web-chat_api.test.ts.md |     2 +-
-++ docs/autogen/codebase/apps_web-chat_api.ts.md      |     2 +-
-++ .../autogen/codebase/apps_web-chat_package.json.md |     2 +-
-++ docs/autogen/codebase/apps_web-chat_script.ts.md   |     2 +-
-++ .../codebase/apps_web-chat_tsconfig.json.md        |     2 +-
-++ .../codebase/apps_web-chat_vite-env.d.ts.md        |     2 +-
-++ .../codebase/apps_web-chat_vite.config.ts.md       |     2 +-
-++ .../codebase/apps_web-chat_vitest.config.ts.md     |     2 +-
-++ docs/autogen/codebase/backend_README.md.md         |     2 +-
-++ .../backend_adaptive_engine_experience_db.py.md    |     2 +-
-++ .../codebase/backend_adaptive_engine_init_.py.md   |     2 +-
-++ .../backend_adaptive_engine_intent_parser.py.md    |     2 +-
-++ .../backend_adaptive_engine_learning_loop.py.md    |     2 +-
-++ .../backend_adaptive_engine_platform_learner.py.md |     2 +-
-++ .../backend_adaptive_engine_registry.py.md         |     2 +-
-++ ...end_adaptive_engine_test_platform_learner.py.md |     2 +-
-++ docs/autogen/codebase/backend_admin_god.py.md      |     2 +-
-++ docs/autogen/codebase/backend_admin_init_.py.md    |     2 +-
-++ docs/autogen/codebase/backend_admin_test_god.py.md |     2 +-
-++ .../codebase/backend_agents_crew_departments.py.md |     2 +-
-++ docs/autogen/codebase/backend_agents_init_.py.md   |     2 +-
-++ .../codebase/backend_agents_legal_agent.py.md      |     2 +-
-++ .../codebase/backend_agents_medical_agent.py.md    |     2 +-
-++ .../backend_agents_research_assistant.py.md        |     2 +-
-++ .../codebase/backend_agents_test_legal_agent.py.md |     2 +-
-++ .../backend_agents_test_medical_agent.py.md        |     2 +-
-++ .../codebase/backend_agents_trading_agent.py.md    |     2 +-
-++ docs/autogen/codebase/backend_alembic_env.py.md    |     2 +-
-++ ...ersions_664fe16e33ca_add_ci_reports_table.py.md |     2 +-
-++ .../codebase/backend_api_dependencies.py.md        |     2 +-
-++ docs/autogen/codebase/backend_api_init_.py.md      |     2 +-
-++ .../codebase/backend_api_routes_admin.py.md        |     2 +-
-++ .../backend_api_routes_admin_dashboard.py.md       |     2 +-
-++ .../codebase/backend_api_routes_agent_tasks.py.md  |     2 +-
-++ .../codebase/backend_api_routes_agents.py.md       |     2 +-
-++ .../codebase/backend_api_routes_api_keys.py.md     |     2 +-
-++ .../backend_api_routes_approval_manager.py.md      |     2 +-
-++ .../backend_api_routes_async_task_router.py.md     |     2 +-
-++ .../autogen/codebase/backend_api_routes_auth.py.md |     2 +-
-++ .../codebase/backend_api_routes_billing_api.py.md  |     2 +-
-++ .../codebase/backend_api_routes_browser.py.md      |     2 +-
-++ .../codebase/backend_api_routes_byoc_api.py.md     |     2 +-
-++ .../codebase/backend_api_routes_cdc_webhooks.py.md |     2 +-
-++ .../autogen/codebase/backend_api_routes_chat.py.md |     2 +-
-++ .../codebase/backend_api_routes_ci_webhooks.py.md  |     2 +-
-++ .../codebase/backend_api_routes_cloud_mesh.py.md   |     2 +-
-++ .../codebase/backend_api_routes_codeflow.py.md     |     2 +-
-++ .../codebase/backend_api_routes_config.py.md       |     2 +-
-++ .../codebase/backend_api_routes_email.py.md        |     2 +-
-++ .../codebase/backend_api_routes_evolution.py.md    |     2 +-
-++ .../codebase/backend_api_routes_feedback.py.md     |     2 +-
-++ .../codebase/backend_api_routes_github.py.md       |     2 +-
-++ .../codebase/backend_api_routes_graph.py.md        |     2 +-
-++ .../codebase/backend_api_routes_init_.py.md        |     2 +-
-++ .../codebase/backend_api_routes_internal.py.md     |     2 +-
-++ .../codebase/backend_api_routes_knowledge.py.md    |     2 +-
-++ .../codebase/backend_api_routes_llm_gateway.py.md  |     2 +-
-++ .../codebase/backend_api_routes_markdown.py.md     |     2 +-
-++ .../codebase/backend_api_routes_marketplace.py.md  |     2 +-
-++ .../backend_api_routes_marketplace_endpoints.py.md |     2 +-
-++ .../codebase/backend_api_routes_media.py.md        |     2 +-
-++ .../codebase/backend_api_routes_memory.py.md       |     2 +-
-++ .../codebase/backend_api_routes_metrics.py.md      |     2 +-
-++ .../codebase/backend_api_routes_mobile_bff.py.md   |     2 +-
-++ .../codebase/backend_api_routes_onboarding.py.md   |     2 +-
-++ .../codebase/backend_api_routes_payments.py.md     |     2 +-
-++ .../codebase/backend_api_routes_preferences.py.md  |     2 +-
-++ .../codebase/backend_api_routes_repos.py.md        |     2 +-
-++ .../codebase/backend_api_routes_simulator.py.md    |     2 +-
-++ .../codebase/backend_api_routes_site_actions.py.md |     2 +-
-++ docs/autogen/codebase/backend_api_routes_sso.py.md |     2 +-
-++ .../codebase/backend_api_routes_stream.py.md       |     2 +-
-++ .../autogen/codebase/backend_api_routes_task.py.md |     2 +-
-++ .../backend_api_routes_task_workspace.py.md        |     2 +-
-++ .../codebase/backend_api_routes_tenant_admin.py.md |     2 +-
-++ .../codebase/backend_api_routes_tools_ops.py.md    |     2 +-
-++ .../backend_api_routes_tools_registry.py.md        |     2 +-
-++ .../backend_api_routes_usage_metrics.py.md         |     2 +-
-++ .../codebase/backend_api_routes_voice.py.md        |     2 +-
-++ .../backend_api_routes_websocket_agent.py.md       |     2 +-
-++ .../backend_api_routes_websocket_voice.py.md       |     2 +-
-++ .../codebase/backend_byoc_cloud_connector.py.md    |     2 +-
-++ .../backend_byoc_container_orchestrator.py.md      |     2 +-
-++ docs/autogen/codebase/backend_byoc_init_.py.md     |     2 +-
-++ .../codebase/backend_byoc_resource_manager.py.md   |     2 +-
-++ .../codebase/backend_config_byoc_limits.json.md    |     2 +-
-++ .../backend_config_constitutional_rules.json.md    |     2 +-
-++ .../codebase/backend_config_pricing_tiers.json.md  |     2 +-
-++ .../codebase/backend_config_routing_policy.json.md |     2 +-
-++ docs/autogen/codebase/backend_core_admin_god.py.md |     2 +-
-++ .../codebase/backend_core_admin_routes.py.md       |     2 +-
-++ .../codebase/backend_core_agent_orchestrator.py.md |     2 +-
-++ .../codebase/backend_core_api_key_middleware.py.md |     2 +-
-++ .../backend_core_api_key_rate_limiter.py.md        |     2 +-
-++ docs/autogen/codebase/backend_core_app.py.md       |     2 +-
-++ .../codebase/backend_core_audit_logger.py.md       |     2 +-
-++ .../codebase/backend_core_auth_middleware.py.md    |     2 +-
-++ .../codebase/backend_core_auto_remediation.py.md   |     2 +-
-++ .../codebase/backend_core_autocache_proxy.py.md    |     2 +-
-++ .../codebase/backend_core_circuit_breaker.py.md    |     2 +-
-++ .../backend_core_cloud_sandbox_orchestrator.py.md  |     2 +-
-++ .../codebase/backend_core_cloud_storage.py.md      |     2 +-
-++ .../codebase/backend_core_code_validator.py.md     |     2 +-
-++ docs/autogen/codebase/backend_core_config.py.md    |     2 +-
-++ docs/autogen/codebase/backend_core_constants.py.md |     2 +-
-++ .../codebase/backend_core_db_repository.py.md      |     2 +-
-++ .../codebase/backend_core_decision_engine.py.md    |     2 +-
-++ .../codebase/backend_core_discord_bot.py.md        |     2 +-
-++ .../codebase/backend_core_docker-compose.yml.md    |     2 +-
-++ .../codebase/backend_core_email_service.py.md      |     2 +-
-++ .../codebase/backend_core_error_pattern_db.py.md   |     2 +-
-++ .../codebase/backend_core_error_remediation.py.md  |     2 +-
-++ docs/autogen/codebase/backend_core_events.py.md    |     2 +-
-++ .../codebase/backend_core_evolution_engine.py.md   |     2 +-
-++ .../codebase/backend_core_factual_verifier.py.md   |     2 +-
-++ .../codebase/backend_core_feedback_loop.py.md      |     2 +-
-++ .../codebase/backend_core_free_tier_tracker.py.md  |     2 +-
-++ .../codebase/backend_core_gcp_firestore.py.md      |     2 +-
-++ .../codebase/backend_core_gcp_pubsub_queue.py.md   |     2 +-
-++ .../codebase/backend_core_generation_monitor.py.md |     2 +-
-++ .../codebase/backend_core_grpc_client.py.md        |     2 +-
-++ .../codebase/backend_core_health_monitor.py.md     |     2 +-
-++ .../backend_core_honeypot_middleware.py.md         |     2 +-
-++ .../backend_core_idempotency_middleware.py.md      |     2 +-
-++ .../codebase/backend_core_immune_system.py.md      |     2 +-
-++ docs/autogen/codebase/backend_core_init_.py.md     |     2 +-
-++ .../codebase/backend_core_input_sanitizer.py.md    |     2 +-
-++ docs/autogen/codebase/backend_core_intent.py.md    |     2 +-
-++ .../codebase/backend_core_intent_router.py.md      |     2 +-
-++ .../codebase/backend_core_language_router.py.md    |     2 +-
-++ docs/autogen/codebase/backend_core_ld_client.py.md |     2 +-
-++ docs/autogen/codebase/backend_core_lifespan.py.md  |     2 +-
-++ .../codebase/backend_core_llm_gateway.py.md        |     2 +-
-++ .../codebase/backend_core_logging_config.py.md     |     2 +-
-++ .../codebase/backend_core_mcp_allowlist.py.md      |     2 +-
-++ .../codebase/backend_core_microvm_sandbox.py.md    |     2 +-
-++ .../codebase/backend_core_multi_layer_cache.py.md  |     2 +-
-++ .../backend_core_observability_middleware.py.md    |     2 +-
-++ .../codebase/backend_core_orchestrator.py.md       |     2 +-
-++ .../codebase/backend_core_origin_validator.py.md   |     2 +-
-++ .../codebase/backend_core_output_validator.py.md   |     2 +-
-++ .../codebase/backend_core_pgbouncer_pool.py.md     |     2 +-
-++ .../codebase/backend_core_posthog_client.py.md     |     2 +-
-++ .../codebase/backend_core_prompt_firewall.py.md    |     2 +-
-++ .../codebase/backend_core_prompt_helpers.py.md     |     2 +-
-++ .../codebase/backend_core_rate_limiter.py.md       |     2 +-
-++ docs/autogen/codebase/backend_core_rbac.py.md      |     2 +-
-++ .../codebase/backend_core_redis_manager.py.md      |     2 +-
-++ .../codebase/backend_core_rollback_monitor.py.md   |     2 +-
-++ .../codebase/backend_core_rules_mutator.py.md      |     2 +-
-++ .../codebase/backend_core_schema_validator.py.md   |     2 +-
-++ .../codebase/backend_core_secret_vault.py.md       |     2 +-
-++ .../backend_core_secure_credential_store.py.md     |     2 +-
-++ docs/autogen/codebase/backend_core_security.py.md  |     2 +-
-++ .../codebase/backend_core_self_healing_agent.py.md |     2 +-
-++ .../codebase/backend_core_semantic_cache.py.md     |     2 +-
-++ docs/autogen/codebase/backend_core_services.py.md  |     2 +-
-++ .../codebase/backend_core_skill_graph.py.md        |     2 +-
-++ .../codebase/backend_core_swarm_orchestrator.py.md |     2 +-
-++ .../autogen/codebase/backend_core_task_queue.py.md |     2 +-
-++ .../backend_core_task_queue_enhanced.py.md         |     2 +-
-++ .../codebase/backend_core_task_router.py.md        |     2 +-
-++ docs/autogen/codebase/backend_core_telemetry.py.md |     2 +-
-++ docs/autogen/codebase/backend_core_tenant_db.py.md |     2 +-
-++ .../codebase/backend_core_token_budget.py.md       |     2 +-
-++ .../codebase/backend_core_token_deductor.py.md     |     2 +-
-++ .../codebase/backend_core_universal_rules.py.md    |     2 +-
-++ .../codebase/backend_core_upload_validator.py.md   |     2 +-
-++ .../backend_core_upstash_redis_queue.py.md         |     2 +-
-++ .../codebase/backend_core_user_profiler.py.md      |     2 +-
-++ docs/autogen/codebase/backend_coverage.json.md     |     2 +-
-++ docs/autogen/codebase/backend_database_init_.py.md |     2 +-
-++ ...end_database_migrations_01_initial_setup.sql.md |     2 +-
-++ ...kend_database_migrations_02_phase2_setup.sql.md |     2 +-
-++ ...grations_03_user_preferences_and_metrics.sql.md |     2 +-
-++ ...nd_database_migrations_04_schema_upgrade.sql.md |     2 +-
-++ ...database_migrations_05_seed_github_repos.sql.md |     2 +-
-++ ...d_database_migrations_06_referral_system.sql.md |     2 +-
-++ ...end_database_migrations_07_tenant_config.sql.md |     2 +-
-++ ...ckend_database_migrations_08_sso_configs.sql.md |     2 +-
-++ ...database_migrations_09_offline_sync_logs.sql.md |     2 +-
-++ ...atabase_migrations_10_tenant_sso_offline.sql.md |     2 +-
-++ .../codebase/backend_database_session.py.md        |     2 +-
-++ .../codebase/backend_database_storage_client.py.md |     2 +-
-++ .../backend_database_supabase_client.py.md         |     2 +-
-++ .../codebase/backend_engine_cost_optimizer.py.md   |     2 +-
-++ docs/autogen/codebase/backend_engine_init_.py.md   |     2 +-
-++ .../codebase/backend_engine_model_dispatcher.py.md |     2 +-
-++ .../backend_evolution_auto_skill_creator.py.md     |     2 +-
-++ .../backend_evolution_auto_update_manager.py.md    |     2 +-
-++ .../backend_evolution_dynamic_injector.py.md       |     2 +-
-++ .../backend_evolution_fitness_engine.py.md         |     2 +-
-++ .../autogen/codebase/backend_evolution_init_.py.md |     2 +-
-++ .../backend_evolution_master_planner.py.md         |     2 +-
-++ .../backend_evolution_security_sandbox.py.md       |     2 +-
-++ .../backend_evolution_self_evolution_agent.py.md   |     2 +-
-++ .../codebase/backend_evolution_skill_graph.py.md   |     2 +-
-++ docs/autogen/codebase/backend_fix_tests.py.md      |     2 +-
-++ docs/autogen/codebase/backend_init_.py.md          |     2 +-
-++ docs/autogen/codebase/backend_main.py.md           |     2 +-
-++ .../backend_memory_checkpoint_resume.py.md         |     2 +-
-++ .../codebase/backend_memory_chromadb_store.py.md   |     2 +-
-++ .../backend_memory_cloud_postgres_store.py.md      |     2 +-
-++ .../backend_memory_cloud_vector_store.py.md        |     2 +-
-++ .../codebase/backend_memory_episodic_memory.py.md  |     2 +-
-++ docs/autogen/codebase/backend_memory_init_.py.md   |     2 +-
-++ .../codebase/backend_memory_long_term_memory.py.md |     2 +-
-++ .../codebase/backend_memory_rag_pipeline.py.md     |     2 +-
-++ .../codebase/backend_memory_sliding_window.py.md   |     2 +-
-++ .../codebase/backend_memory_sqlite_store.py.md     |     2 +-
-++ .../codebase/backend_memory_summary_tree.py.md     |     2 +-
-++ .../codebase/backend_memory_supabase_store.py.md   |     2 +-
-++ .../backend_memory_vector_store_config.py.md       |     2 +-
-++ .../backend_middleware_auth_middleware.py.md       |     2 +-
-++ .../backend_middleware_chaos_injector.py.md        |     2 +-
-++ .../codebase/backend_middleware_idempotency.py.md  |     2 +-
-++ docs/autogen/codebase/backend_models_admin.py.md   |     2 +-
-++ docs/autogen/codebase/backend_models_api_key.py.md |     2 +-
-++ .../codebase/backend_models_byoc_payloads.py.md    |     2 +-
-++ .../codebase/backend_models_ci_report.py.md        |     2 +-
-++ .../codebase/backend_models_deployment_logs.py.md  |     2 +-
-++ .../backend_models_error_remediation.py.md         |     2 +-
-++ .../codebase/backend_models_evolution.py.md        |     2 +-
-++ docs/autogen/codebase/backend_models_init_.py.md   |     2 +-
-++ .../backend_models_local_model_handler.py.md       |     2 +-
-++ .../codebase/backend_models_pending_tasks.py.md    |     2 +-
-++ .../codebase/backend_models_shared_workspace.py.md |     2 +-
-++ .../backend_models_transaction_ledger.py.md        |     2 +-
-++ .../backend_models_voice_interaction.py.md         |     2 +-
-++ docs/autogen/codebase/backend_models_wallet.py.md  |     2 +-
-++ .../codebase/backend_monitoring_cost_auditor.py.md |     2 +-
-++ .../codebase/backend_monitoring_init_.py.md        |     2 +-
-++ .../codebase/backend_p2p_credit_system.py.md       |     2 +-
-++ docs/autogen/codebase/backend_p2p_init_.py.md      |     2 +-
-++ .../codebase/backend_p2p_secure_tunnel.py.md       |     2 +-
-++ docs/autogen/codebase/backend_pyproject.toml.md    |     2 +-
-++ docs/autogen/codebase/backend_reports_init_.py.md  |     2 +-
-++ .../backend_reports_optimization_engine.py.md      |     2 +-
-++ .../codebase/backend_run_roundtrip_tests.py.md     |     2 +-
-++ docs/autogen/codebase/backend_scout_init_.py.md    |     2 +-
-++ .../backend_scout_knowledge_extractor.py.md        |     2 +-
-++ .../codebase/backend_scout_web_crawler_agent.py.md |     2 +-
-++ .../codebase/backend_scripts_check_ollama.py.md    |     2 +-
-++ docs/autogen/codebase/backend_scripts_init_.py.md  |     2 +-
-++ .../codebase/backend_scripts_load_seed_data.py.md  |     2 +-
-++ .../backend_scripts_run_dependency_check.py.md     |     2 +-
-++ .../backend_scripts_seed_tools_registry.py.md      |     2 +-
-++ .../backend_scripts_self_healing_tests.py.md       |     2 +-
-++ docs/autogen/codebase/backend_skills_init_.py.md   |     2 +-
-++ .../codebase/backend_skills_provisioner.py.md      |     2 +-
-++ .../codebase/backend_skills_skill_registry.py.md   |     2 +-
-++ .../codebase/backend_storage_asset_manager.py.md   |     2 +-
-++ docs/autogen/codebase/backend_storage_init_.py.md  |     2 +-
-++ .../backend_storage_r2_storage_client.py.md        |     2 +-
-++ .../backend_tests_agents_test_legal_agent.py.md    |     2 +-
-++ .../backend_tests_agents_test_medical_agent.py.md  |     2 +-
-++ ...kend_tests_agents_test_research_assistant.py.md |     2 +-
-++ .../backend_tests_agents_test_trading_agent.py.md  |     2 +-
-++ .../backend_tests_byoc_test_cloud_connector.py.md  |     2 +-
-++ ...nd_tests_byoc_test_container_orchestrator.py.md |     2 +-
-++ .../backend_tests_byoc_test_resource_manager.py.md |     2 +-
-++ docs/autogen/codebase/backend_tests_conftest.py.md |     2 +-
-++ .../backend_tests_engine_test_cost_optimizer.py.md |     2 +-
-++ ...ackend_tests_engine_test_model_dispatcher.py.md |     2 +-
-++ docs/autogen/codebase/backend_tests_init_.py.md    |     2 +-
-++ ...ackend_tests_monitoring_test_cost_auditor.py.md |     2 +-
-++ .../backend_tests_p2p_test_credit_system.py.md     |     2 +-
-++ .../backend_tests_p2p_test_secure_tunnel.py.md     |     2 +-
-++ ...kend_tests_scout_test_knowledge_extractor.py.md |     2 +-
-++ ...ackend_tests_scout_test_web_crawler_agent.py.md |     2 +-
-++ .../backend_tests_test_adaptive_engine.py.md       |     2 +-
-++ .../codebase/backend_tests_test_admin_god.py.md    |     2 +-
-++ .../codebase/backend_tests_test_admin_models.py.md |     2 +-
-++ .../codebase/backend_tests_test_admin_routes.py.md |     2 +-
-++ .../codebase/backend_tests_test_advanced.py.md     |     2 +-
-++ .../backend_tests_test_agent_department.py.md      |     2 +-
-++ .../backend_tests_test_agent_departments.py.md     |     2 +-
-++ .../backend_tests_test_agent_orchestrator.py.md    |     2 +-
-++ ...ackend_tests_test_agents_crew_departments.py.md |     2 +-
-++ docs/autogen/codebase/backend_tests_test_api.py.md |     2 +-
-++ .../codebase/backend_tests_test_api_chat.py.md     |     2 +-
-++ .../codebase/backend_tests_test_api_keys.py.md     |     2 +-
-++ .../backend_tests_test_api_new_endpoints.py.md     |     2 +-
-++ .../codebase/backend_tests_test_api_router.py.md   |     2 +-
-++ .../codebase/backend_tests_test_audit_logger.py.md |     2 +-
-++ .../backend_tests_test_auth_middleware.py.md       |     2 +-
-++ .../codebase/backend_tests_test_auth_routes.py.md  |     2 +-
-++ .../backend_tests_test_auto_fix_trigger.py.md      |     2 +-
-++ .../backend_tests_test_auto_skill_creator.py.md    |     2 +-
-++ .../backend_tests_test_autonomous_agent.py.md      |     2 +-
-++ .../codebase/backend_tests_test_bangla_nlp.py.md   |     2 +-
-++ .../codebase/backend_tests_test_bangla_voice.py.md |     2 +-
-++ .../backend_tests_test_billing_system.py.md        |     2 +-
-++ .../codebase/backend_tests_test_brain.py.md        |     2 +-
-++ .../backend_tests_test_browser_credentials.py.md   |     2 +-
-++ .../backend_tests_test_byoc_endpoints.py.md        |     2 +-
-++ .../codebase/backend_tests_test_chaos_worker.py.md |     2 +-
-++ .../backend_tests_test_checkpoint_resume.py.md     |     2 +-
-++ .../backend_tests_test_circuit_breaker.py.md       |     2 +-
-++ .../backend_tests_test_cloud_sandbox.py.md         |     2 +-
-++ .../backend_tests_test_cloud_storage.py.md         |     2 +-
-++ .../backend_tests_test_code_validator.py.md        |     2 +-
-++ .../backend_tests_test_collaborative_editor.py.md  |     2 +-
-++ .../codebase/backend_tests_test_config.py.md       |     2 +-
-++ .../backend_tests_test_config_additional.py.md     |     2 +-
-++ .../backend_tests_test_config_coverage.py.md       |     2 +-
-++ .../codebase/backend_tests_test_constants.py.md    |     2 +-
-++ .../backend_tests_test_context_and_actions.py.md   |     2 +-
-++ .../autogen/codebase/backend_tests_test_core.py.md |     2 +-
-++ .../codebase/backend_tests_test_core_smoke.py.md   |     2 +-
-++ .../backend_tests_test_coverage_gaps.py.md         |     2 +-
-++ .../codebase/backend_tests_test_crew_mcp.py.md     |     2 +-
-++ ...ackend_tests_test_database_storage_client.py.md |     2 +-
-++ .../backend_tests_test_db_repository.py.md         |     2 +-
-++ docs/autogen/codebase/backend_tests_test_e2e.py.md |     2 +-
-++ .../codebase/backend_tests_test_e2e_media.py.md    |     2 +-
-++ .../codebase/backend_tests_test_email_agent.py.md  |     2 +-
-++ .../backend_tests_test_email_service.py.md         |     2 +-
-++ .../backend_tests_test_episodic_memory.py.md       |     2 +-
-++ .../backend_tests_test_error_remediation.py.md     |     2 +-
-++ .../backend_tests_test_evolution_engine.py.md      |     2 +-
-++ .../backend_tests_test_evolution_pipeline.py.md    |     2 +-
-++ .../backend_tests_test_factual_verifier.py.md      |     2 +-
-++ .../backend_tests_test_feedback_loop.py.md         |     2 +-
-++ .../backend_tests_test_firebase_integration.py.md  |     2 +-
-++ .../backend_tests_test_fitness_engine.py.md        |     2 +-
-++ .../backend_tests_test_free_tier_tracker.py.md     |     2 +-
-++ .../backend_tests_test_gcp_integration.py.md       |     2 +-
-++ .../backend_tests_test_generation_monitor.py.md    |     2 +-
-++ .../codebase/backend_tests_test_github_agent.py.md |     2 +-
-++ .../codebase/backend_tests_test_graph_routes.py.md |     2 +-
-++ .../backend_tests_test_graph_service.py.md         |     2 +-
-++ .../codebase/backend_tests_test_grpc_client.py.md  |     2 +-
-++ .../backend_tests_test_hallucination_guard.py.md   |     2 +-
-++ .../codebase/backend_tests_test_health.py.md       |     2 +-
-++ .../backend_tests_test_health_monitor.py.md        |     2 +-
-++ .../backend_tests_test_health_monitor_routes.py.md |     2 +-
-++ .../backend_tests_test_honeypot_middleware.py.md   |     2 +-
-++ ...backend_tests_test_idempotency_middleware.py.md |     2 +-
-++ .../backend_tests_test_immune_system.py.md         |     2 +-
-++ .../backend_tests_test_immune_system_scanner.py.md |     2 +-
-++ .../backend_tests_test_input_sanitizer.py.md       |     2 +-
-++ .../backend_tests_test_language_router.py.md       |     2 +-
-++ .../codebase/backend_tests_test_llm_gateway.py.md  |     2 +-
-++ .../backend_tests_test_llm_gateway_coverage.py.md  |     2 +-
-++ .../backend_tests_test_long_term_memory.py.md      |     2 +-
-++ .../backend_tests_test_markdown_export.py.md       |     2 +-
-++ .../backend_tests_test_marketplace_agent.py.md     |     2 +-
-++ .../backend_tests_test_mcp_allowlist.py.md         |     2 +-
-++ .../codebase/backend_tests_test_mcp_server.py.md   |     2 +-
-++ ...ackend_tests_test_mcp_servers_integration.py.md |     2 +-
-++ .../codebase/backend_tests_test_media_r2.py.md     |     2 +-
-++ ...kend_tests_test_middleware_chaos_injector.py.md |     2 +-
-++ .../codebase/backend_tests_test_migrations.py.md   |     2 +-
-++ ...kend_tests_test_migrations_and_onboarding.py.md |     2 +-
-++ .../codebase/backend_tests_test_mobile_e2e.py.md   |     2 +-
-++ .../backend_tests_test_model_registry.py.md        |     2 +-
-++ .../backend_tests_test_model_router_unit.py.md     |     2 +-
-++ .../backend_tests_test_model_trainer.py.md         |     2 +-
-++ .../backend_tests_test_models_ci_report.py.md      |     2 +-
-++ .../backend_tests_test_models_evolution.py.md      |     2 +-
-++ .../codebase/backend_tests_test_monitoring.py.md   |     2 +-
-++ .../codebase/backend_tests_test_multicloud.py.md   |     2 +-
-++ .../backend_tests_test_new_endpoints_sprint5.py.md |     2 +-
-++ .../backend_tests_test_new_interfaces.py.md        |     2 +-
-++ .../backend_tests_test_new_tools_sprint5.py.md     |     2 +-
-++ .../backend_tests_test_optimization_engine.py.md   |     2 +-
-++ .../backend_tests_test_output_validator.py.md      |     2 +-
-++ ...ackend_tests_test_parallel_agent_executor.py.md |     2 +-
-++ .../codebase/backend_tests_test_payments.py.md     |     2 +-
-++ ...ckend_tests_test_performance_aware_router.py.md |     2 +-
-++ .../backend_tests_test_pgbouncer_pool.py.md        |     2 +-
-++ .../codebase/backend_tests_test_posthog.py.md      |     2 +-
-++ .../codebase/backend_tests_test_pr_reviewer.py.md  |     2 +-
-++ .../backend_tests_test_prod_docs_security.py.md    |     2 +-
-++ ...sts_test_production_readiness_integration.py.md |     2 +-
-++ .../backend_tests_test_prompt_firewall.py.md       |     2 +-
-++ .../autogen/codebase/backend_tests_test_rbac.py.md |     2 +-
-++ ...backend_tests_test_reasoning_orchestrator.py.md |     2 +-
-++ .../backend_tests_test_repo_discovery.py.md        |     2 +-
-++ .../backend_tests_test_resource_catalog.py.md      |     2 +-
-++ .../autogen/codebase/backend_tests_test_rlhf.py.md |     2 +-
-++ ...kend_tests_test_sandbox_orchestration_run.py.md |     2 +-
-++ .../backend_tests_test_schema_validator.py.md      |     2 +-
-++ .../codebase/backend_tests_test_secret_vault.py.md |     2 +-
-++ ...ackend_tests_test_secure_credential_store.py.md |     2 +-
-++ .../backend_tests_test_security_middleware.py.md   |     2 +-
-++ .../backend_tests_test_security_regression.py.md   |     2 +-
-++ .../backend_tests_test_self_evolution_agent.py.md  |     2 +-
-++ .../backend_tests_test_simulator_browser_api.py.md |     2 +-
-++ .../codebase/backend_tests_test_skill_graph.py.md  |     2 +-
-++ .../backend_tests_test_skill_recommender.py.md     |     2 +-
-++ .../backend_tests_test_sliding_window_memory.py.md |     2 +-
-++ .../backend_tests_test_sprint_c_tools.py.md        |     2 +-
-++ .../codebase/backend_tests_test_sprint_g.py.md     |     2 +-
-++ .../backend_tests_test_stealth_networking.py.md    |     2 +-
-++ .../codebase/backend_tests_test_stream.py.md       |     2 +-
-++ .../backend_tests_test_style_learner.py.md         |     2 +-
-++ ...kend_tests_test_supabase_schema_bootstrap.py.md |     2 +-
-++ .../backend_tests_test_supabase_store.py.md        |     2 +-
-++ .../backend_tests_test_swarm_orchestrator.py.md    |     2 +-
-++ .../backend_tests_test_task_endpoints.py.md        |     2 +-
-++ .../codebase/backend_tests_test_task_queue.py.md   |     2 +-
-++ .../codebase/backend_tests_test_task_router.py.md  |     2 +-
-++ .../codebase/backend_tests_test_telegram_bot.py.md |     2 +-
-++ .../codebase/backend_tests_test_telemetry.py.md    |     2 +-
-++ .../backend_tests_test_tenant_rate_limiter.py.md   |     2 +-
-++ .../backend_tests_test_universal_rules.py.md       |     2 +-
-++ .../backend_tests_test_upstash_redis.py.md         |     2 +-
-++ docs/autogen/codebase/backend_tests_test_uss.py.md |     2 +-
-++ .../backend_tests_test_video_generator.py.md       |     2 +-
-++ .../codebase/backend_tests_test_vision_agent.py.md |     2 +-
-++ .../codebase/backend_tests_test_voice_stream.py.md |     2 +-
-++ .../codebase/backend_tests_test_vpn_switcher.py.md |     2 +-
-++ .../codebase/backend_tests_test_vscode_e2e.py.md   |     2 +-
-++ .../codebase/backend_tests_test_web_fallback.py.md |     2 +-
-++ ...d_tests_tools_test_auto_coverage_improver.py.md |     2 +-
-++ ...kend_tests_tools_test_auto_test_generator.py.md |     2 +-
-++ ...backend_tests_tools_test_coverage_auditor.py.md |     2 +-
-++ .../backend_tests_utils_test_api_tracker.py.md     |     2 +-
-++ .../backend_tests_workers_test_celery_app.py.md    |     2 +-
-++ .../backend_tools_3d_model_generator.py.md         |     2 +-
-++ .../codebase/backend_tools_agent_tools.py.md       |     2 +-
-++ .../backend_tools_ai_federation_protocol.py.md     |     2 +-
-++ .../backend_tools_ai_pair_programmer.py.md         |     2 +-
-++ .../codebase/backend_tools_api_gateway.py.md       |     2 +-
-++ .../backend_tools_auto_coverage_improver.py.md     |     2 +-
-++ .../codebase/backend_tools_auto_pr_pipeline.py.md  |     2 +-
-++ .../backend_tools_auto_test_generator.py.md        |     2 +-
-++ .../backend_tools_bandwidth_optimizer.py.md        |     2 +-
-++ .../backend_tools_bangla_ai_connector.py.md        |     2 +-
-++ .../codebase/backend_tools_bangla_nlp.py.md        |     2 +-
-++ .../codebase/backend_tools_bangla_voice.py.md      |     2 +-
-++ .../codebase/backend_tools_benchmark_agent.py.md   |     2 +-
-++ .../backend_tools_bengali_ocr_converter.py.md      |     2 +-
-++ .../codebase/backend_tools_blockchain_agent.py.md  |     2 +-
-++ .../autogen/codebase/backend_tools_bootstrap.py.md |     2 +-
-++ .../codebase/backend_tools_browser_agent.py.md     |     2 +-
-++ .../codebase/backend_tools_browser_stealth.py.md   |     2 +-
-++ .../backend_tools_checkpoint_manager.py.md         |     2 +-
-++ docs/autogen/codebase/backend_tools_cli.py.md      |     2 +-
-++ .../backend_tools_cloud_sandbox_orchestrator.py.md |     2 +-
-++ .../backend_tools_code_smell_detector.py.md        |     2 +-
-++ .../codebase/backend_tools_codebase_exporter.py.md |     2 +-
-++ .../backend_tools_collaborative_editor.py.md       |     2 +-
-++ .../codebase/backend_tools_comment_thread_ai.py.md |     2 +-
-++ .../codebase/backend_tools_computer_agent.py.md    |     2 +-
-++ .../backend_tools_conversation_manager.py.md       |     2 +-
-++ .../codebase/backend_tools_cost_auditor.py.md      |     2 +-
-++ .../codebase/backend_tools_cot_reasoner.py.md      |     2 +-
-++ .../codebase/backend_tools_coverage_auditor.py.md  |     2 +-
-++ .../backend_tools_dependency_manager_agent.py.md   |     2 +-
-++ .../backend_tools_diagram_to_architecture.py.md    |     2 +-
-++ .../codebase/backend_tools_docker_sandbox.py.md    |     2 +-
-++ .../codebase/backend_tools_domain_adapter.py.md    |     2 +-
-++ .../codebase/backend_tools_email_agent.py.md       |     2 +-
-++ .../codebase/backend_tools_ensemble_router.py.md   |     2 +-
-++ .../codebase/backend_tools_fuzz_sandbox.py.md      |     2 +-
-++ .../codebase/backend_tools_game_dev_agent.py.md    |     2 +-
-++ .../backend_tools_gcp_cloud_functions.py.md        |     2 +-
-++ .../backend_tools_git_knowledge_extractor.py.md    |     2 +-
-++ .../codebase/backend_tools_github_agent.py.md      |     2 +-
-++ .../codebase/backend_tools_graph_service.py.md     |     2 +-
-++ .../backend_tools_headless_agent_registry.py.md    |     2 +-
-++ .../codebase/backend_tools_health_checker.py.md    |     2 +-
-++ .../codebase/backend_tools_image_generator.py.md   |     2 +-
-++ .../codebase/backend_tools_image_to_code.py.md     |     2 +-
-++ docs/autogen/codebase/backend_tools_init_.py.md    |     2 +-
-++ .../backend_tools_knowledge_base_indexer.py.md     |     2 +-
-++ .../backend_tools_langchain_agent_example.py.md    |     2 +-
-++ .../codebase/backend_tools_legal_agent.py.md       |     2 +-
-++ .../backend_tools_local_ocr_extractor.py.md        |     2 +-
-++ .../codebase/backend_tools_local_search_rag.py.md  |     2 +-
-++ .../codebase/backend_tools_marketplace_agent.py.md |     2 +-
-++ .../codebase/backend_tools_mcp_cloud_deploy.py.md  |     2 +-
-++ .../codebase/backend_tools_mcp_github_cicd.py.md   |     2 +-
-++ .../codebase/backend_tools_mcp_server.py.md        |     2 +-
-++ .../codebase/backend_tools_mcp_supabase.py.md      |     2 +-
-++ .../codebase/backend_tools_mcp_workspace.py.md     |     2 +-
-++ .../codebase/backend_tools_medical_agent.py.md     |     2 +-
-++ .../codebase/backend_tools_meta_architect.py.md    |     2 +-
-++ .../codebase/backend_tools_model_trainer.py.md     |     2 +-
-++ .../backend_tools_monthly_cost_reporter.py.md      |     2 +-
-++ .../backend_tools_multi_account_rotator.py.md      |     2 +-
-++ .../codebase/backend_tools_multilingual_tts.py.md  |     2 +-
-++ .../codebase/backend_tools_music_generator.py.md   |     2 +-
-++ .../codebase/backend_tools_offline_mode.py.md      |     2 +-
-++ .../backend_tools_on_premise_deployer.py.md        |     2 +-
-++ .../backend_tools_parallel_agent_executor.py.md    |     2 +-
-++ .../codebase/backend_tools_pdf_to_sdk.py.md        |     2 +-
-++ .../codebase/backend_tools_plan_sorter.py.md       |     2 +-
-++ .../backend_tools_playwright_browser_agent.py.md   |     2 +-
-++ .../codebase/backend_tools_pr_reviewer.py.md       |     2 +-
-++ .../codebase/backend_tools_pre_commit_ai.py.md     |     2 +-
-++ .../codebase/backend_tools_preference_memory.py.md |     2 +-
-++ .../backend_tools_presentation_generator.py.md     |     2 +-
-++ .../codebase/backend_tools_proxy_manager.py.md     |     2 +-
-++ .../codebase/backend_tools_repo_deep_indexer.py.md |     2 +-
-++ .../backend_tools_repo_discovery_agent.py.md       |     2 +-
-++ .../codebase/backend_tools_resource_catalog.py.md  |     2 +-
-++ .../codebase/backend_tools_rlhf_pipeline.py.md     |     2 +-
-++ .../codebase/backend_tools_safe_executor.py.md     |     2 +-
-++ .../codebase/backend_tools_scientific_agent.py.md  |     2 +-
-++ .../codebase/backend_tools_seed_database.py.md     |     2 +-
-++ .../codebase/backend_tools_self_planner.py.md      |     2 +-
-++ .../codebase/backend_tools_skill_recommender.py.md |     2 +-
-++ .../codebase/backend_tools_sso_integrator.py.md    |     2 +-
-++ .../backend_tools_stealth_http_client.py.md        |     2 +-
-++ .../codebase/backend_tools_style_learner.py.md     |     2 +-
-++ .../codebase/backend_tools_telegram_bot.py.md      |     2 +-
-++ .../backend_tools_tenant_rate_limiter.py.md        |     2 +-
-++ .../backend_tools_test_3d_model_generator.py.md    |     2 +-
-++ ...end_tools_test_cloud_sandbox_orchestrator.py.md |     2 +-
-++ .../codebase/backend_tools_trading_agent.py.md     |     2 +-
-++ .../codebase/backend_tools_video_generator.py.md   |     2 +-
-++ .../backend_tools_viral_referral_engine.py.md      |     2 +-
-++ .../codebase/backend_tools_vision_agent.py.md      |     2 +-
-++ docs/autogen/codebase/backend_tools_voice.py.md    |     2 +-
-++ .../codebase/backend_tools_voice_coder.py.md       |     2 +-
-++ .../codebase/backend_tools_vpn_switcher.py.md      |     2 +-
-++ .../backend_tools_vulnerability_predictor.py.md    |     2 +-
-++ .../backend_tools_web_fallback_agent.py.md         |     2 +-
-++ .../codebase/backend_utils_api_tracker.py.md       |     2 +-
-++ .../codebase/backend_utils_environment.py.md       |     2 +-
-++ .../codebase/backend_utils_firestore_helpers.py.md |     2 +-
-++ .../codebase/backend_utils_http_client.py.md       |     2 +-
-++ docs/autogen/codebase/backend_utils_init_.py.md    |     2 +-
-++ .../codebase/backend_utils_json_helpers.py.md      |     2 +-
-++ .../codebase/backend_utils_timestamps.py.md        |     2 +-
-++ docs/autogen/codebase/backend_uv.lock.md           |     2 +-
-++ .../codebase/backend_workers_celery_app.py.md      |     2 +-
-++ .../codebase/backend_workers_chaos_worker.py.md    |     2 +-
-++ .../codebase/config_.pre-commit-config.yaml.md     |     2 +-
-++ docs/autogen/codebase/config_audit-rules.yml.md    |     2 +-
-++ .../codebase/config_compliance-rules.yml.md        |     2 +-
-++ docs/autogen/codebase/config_docker-limits.yml.md  |     2 +-
-++ docs/autogen/codebase/config_firebase.json.md      |     2 +-
-++ .../codebase/config_firestore.indexes.json.md      |     2 +-
-++ docs/autogen/codebase/config_kilo.json.md          |     2 +-
-++ .../codebase/config_promptfooconfig.yaml.md        |     2 +-
-++ docs/autogen/codebase/config_proxy_list.json.md    |     2 +-
-++ .../autogen/codebase/config_routing_policy.json.md |     2 +-
-++ docs/autogen/codebase/config_vercel.json.md        |     2 +-
-++ docs/autogen/codebase/coverage.json.md             |     2 +-
-++ docs/autogen/codebase/coverage.toml.md             |     2 +-
-++ docs/autogen/codebase/docker-compose.yml.md        |     2 +-
-++ .../codebase/evolution_auto_skill_creator.py.md    |     2 +-
-++ .../autogen/codebase/evolution_daily_learner.py.md |     2 +-
-++ .../codebase/evolution_evolution_engine.py.md      |     2 +-
-++ .../codebase/evolution_evolution_react_agent.py.md |     2 +-
-++ docs/autogen/codebase/evolution_self_updater.py.md |     2 +-
-++ docs/autogen/codebase/find_duplicate_files.py.md   |     2 +-
-++ docs/autogen/codebase/find_duplicate_tests.py.md   |     2 +-
-++ .../infrastructure_check_deploy_gate.py.md         |     2 +-
-++ ...infrastructure_cloudflare_enhanced-worker.js.md |     2 +-
-++ .../infrastructure_cloudflare_worker.js.md         |     2 +-
-++ .../infrastructure_cloudflare_wrangler.toml.md     |     2 +-
-++ .../infrastructure_cloudrun_autoscale.yaml.md      |     2 +-
-++ .../infrastructure_cloudrun_multi_region.yaml.md   |     2 +-
-++ ...functions_firebase_functions_v1_README_BD.md.md |     2 +-
-++ ...unctions_firebase_functions_v1_api-router.js.md |     2 +-
-++ ..._firebase_functions_v1_deployment-monitor.js.md |     2 +-
-++ ...ctions_firebase_functions_v1_health-smart.js.md |     2 +-
-++ ...ase_functions_firebase_functions_v1_index.js.md |     2 +-
-++ ...irebase_functions_v1_lib_chatClassifier.d.ts.md |     2 +-
-++ ..._firebase_functions_v1_lib_chatClassifier.js.md |     2 +-
-++ ...firebase_functions_v1_lib_email_handler.d.ts.md |     2 +-
-++ ...s_firebase_functions_v1_lib_email_handler.js.md |     2 +-
-++ ...nctions_firebase_functions_v1_lib_index.d.ts.md |     2 +-
-++ ...functions_firebase_functions_v1_lib_index.js.md |     2 +-
-++ ..._firebase_functions_v1_lib_scrapeEngine.d.ts.md |     2 +-
-++ ...ns_firebase_functions_v1_lib_scrapeEngine.js.md |     2 +-
-++ ...e_functions_v1_lib_scrapeHistoryManager.d.ts.md |     2 +-
-++ ...ase_functions_v1_lib_scrapeHistoryManager.js.md |     2 +-
-++ ...functions_firebase_functions_v1_package.json.md |     2 +-
-++ ...ons_firebase_functions_v1_providers-smart.js.md |     2 +-
-++ ...se_functions_v1_server-connection-monitor.js.md |     2 +-
-++ ..._firebase_functions_v1_src_chatClassifier.ts.md |     2 +-
-++ ...dataconnect-admin-generated_esm_index.esm.js.md |     2 +-
-++ ...dataconnect-admin-generated_esm_package.json.md |     2 +-
-++ ...src_dataconnect-admin-generated_index.cjs.js.md |     2 +-
-++ ...1_src_dataconnect-admin-generated_index.d.ts.md |     2 +-
-++ ...src_dataconnect-admin-generated_package.json.md |     2 +-
-++ ...s_firebase_functions_v1_src_email_handler.ts.md |     2 +-
-++ ...functions_firebase_functions_v1_src_index.ts.md |     2 +-
-++ ...ns_firebase_functions_v1_src_scrapeEngine.ts.md |     2 +-
-++ ...ase_functions_v1_src_scrapeHistoryManager.ts.md |     2 +-
-++ ..._firebase_functions_v1_src_scrapeSchema.yaml.md |     2 +-
-++ ...functions_firebase_functions_v1_swagger.yaml.md |     2 +-
-++ ...tions_firebase_functions_v1_system-health.js.md |     2 +-
-++ ...unctions_firebase_functions_v1_tsconfig.json.md |     2 +-
-++ ...irebase_functions_v1_utils_externalClient.js.md |     2 +-
-++ ...rastructure_firebase_functions_ocrTrigger.ts.md |     2 +-
-++ ...ure_monitoring_docker-compose.monitoring.yml.md |     2 +-
-++ ...astructure_monitoring_grafana_dashboard.json.md |     2 +-
-++ ...cture_terraform_root_cause_analysis_agent.py.md |     2 +-
-++ ..._terraform_test_root_cause_analysis_agent.py.md |     2 +-
-++ .../codebase/infrastructure_vitest-report.json.md  |     2 +-
-++ docs/autogen/codebase/package.json.md              |     2 +-
-++ .../codebase/packages_shared-types_package.json.md |     2 +-
-++ .../packages_shared-types_src_conversation.ts.md   |     2 +-
-++ .../codebase/packages_shared-types_src_index.ts.md |     2 +-
-++ .../packages_shared-types_src_message.ts.md        |     2 +-
-++ .../packages_shared-types_tsconfig.json.md         |     2 +-
-++ .../packages_ui-components_package.json.md         |     2 +-
-++ .../packages_ui-components_src_ChatBubble.tsx.md   |     2 +-
-++ .../packages_ui-components_src_index.ts.md         |     2 +-
-++ .../packages_ui-components_tsconfig.json.md        |     2 +-
-++ docs/autogen/codebase/playwright-ct.config.ts.md   |     2 +-
-++ docs/autogen/codebase/pnpm-lock.yaml.md            |     2 +-
-++ docs/autogen/codebase/pnpm-workspace.yaml.md       |     2 +-
-++ docs/autogen/codebase/scratch_job_details.json.md  |     2 +-
-++ docs/autogen/codebase/scratch_smoke_check.py.md    |     2 +-
-++ .../scratch_supremeai_skill_ecosystem_app.py.md    |     2 +-
-++ ...ratch_supremeai_skill_ecosystem_generator.py.md |     2 +-
-++ ..._supremeai_skill_ecosystem_sample_skill.json.md |     2 +-
-++ ...ch_supremeai_skill_ecosystem_skill_schema.py.md |     2 +-
-++ .../codebase/scratch_sync_gsm_secrets.py.md        |     2 +-
-++ docs/autogen/codebase/scratch_update_vault.py.md   |     2 +-
-++ .../autogen/codebase/scratch_update_vault_r2.py.md |     2 +-
-++ .../codebase/scratch_verify_project_health.py.md   |     2 +-
-++ .../codebase/scripts_add_bangla_comments.py.md     |     2 +-
-++ .../codebase/scripts_aggregate_context.py.md       |     2 +-
-++ ...scripts_backup_auto_cross_cloud_replicate.py.md |     2 +-
-++ .../scripts_backup_auto_firestore_backup.py.md     |     2 +-
-++ .../scripts_benchmark_perf_benchmark.py.md         |     2 +-
-++ .../codebase/scripts_bots_auto_alert_bot.py.md     |     2 +-
-++ .../scripts_bots_auto_daily_standup_bot.py.md      |     2 +-
-++ .../codebase/scripts_code_smell_detector.py.md     |     2 +-
-++ docs/autogen/codebase/scripts_codebase_to_md.py.md |     2 +-
-++ .../codebase/scripts_codegraph_integration.py.md   |     2 +-
-++ .../codebase/scripts_commit_supreme_ci.yml.md      |     2 +-
-++ docs/autogen/codebase/scripts_config_audit.py.md   |     2 +-
-++ .../scripts_core_engine_multicatalog_search.py.md  |     2 +-
-++ .../codebase/scripts_core_engine_tool_ranker.py.md |     2 +-
-++ .../codebase/scripts_create_test_admin.py.md       |     2 +-
-++ .../autogen/codebase/scripts_db_auto_migrate.py.md |     2 +-
-++ docs/autogen/codebase/scripts_db_auto_seed.py.md   |     2 +-
-++ .../autogen/codebase/scripts_docker_ai_guard.py.md |     2 +-
-++ ...ipts_evolution_auto_marketing_skill_forge.py.md |     2 +-
-++ docs/autogen/codebase/scripts_fix_mypy.py.md       |     2 +-
-++ docs/autogen/codebase/scripts_fuzz_sandbox.py.md   |     2 +-
-++ .../scripts_generate_codebase_markdown.py.md       |     2 +-
-++ ...scripts_generate_codebase_single_markdown.py.md |     2 +-
-++ docs/autogen/codebase/scripts_generate_md.py.md    |     2 +-
-++ .../codebase/scripts_generate_smart_docs.py.md     |     2 +-
-++ docs/autogen/codebase/scripts_k6_load_test.js.md   |     2 +-
-++ docs/autogen/codebase/scripts_locustfile.py.md     |     2 +-
-++ docs/autogen/codebase/scripts_migrate.py.md        |     2 +-
-++ .../codebase/scripts_multi_model_validator.py.md   |     2 +-
-++ ...scripts_orchestrator_auto_budget_guardian.py.md |     2 +-
-++ docs/autogen/codebase/scripts_profile_memory.py.md |     2 +-
-++ .../scripts_quality_auto_dead_code_remover.py.md   |     2 +-
-++ .../scripts_quality_auto_improve_coverage.py.md    |     2 +-
-++ .../scripts_quality_auto_refactor_suggester.py.md  |     2 +-
-++ ...cripts_quality_check_ollama_test_coverage.py.md |     2 +-
-++ .../scripts_resource_collection_awesome_go.py.md   |     2 +-
-++ ...cripts_resource_collection_awesome_python.py.md |     2 +-
-++ ...ts_resource_collection_awesome_selfhosted.py.md |     2 +-
-++ ...ripts_resource_collection_base_api_client.py.md |     2 +-
-++ .../scripts_resource_collection_base_scraper.py.md |     2 +-
-++ ...pts_resource_collection_ossinsight_client.py.md |     2 +-
-++ ...ipts_resource_collection_ossinsight_init_.py.md |     2 +-
-++ ...ripts_resource_collection_ossinsight_test.py.md |     2 +-
-++ .../scripts_resource_collection_run_all.py.md      |     2 +-
-++ ...ts_resource_collection_run_all_collectors.py.md |     2 +-
-++ ...ripts_resource_scraping_awesome_go_scrape.py.md |     2 +-
-++ ...s_resource_scraping_awesome_python_scrape.py.md |     2 +-
-++ ...source_scraping_awesome_selfhosted_scrape.py.md |     2 +-
-++ .../codebase/scripts_run_all_collectors.py.md      |     2 +-
-++ docs/autogen/codebase/scripts_safety_guard.py.md   |     2 +-
-++ .../scripts_security_auto_find_blindspots.py.md    |     2 +-
-++ .../scripts_security_auto_secret_rotate.py.md      |     2 +-
-++ .../scripts_security_check_dependencies.py.md      |     2 +-
-++ .../codebase/scripts_security_code-quality.yml.md  |     2 +-
-++ ...scripts_security_dependency-health-check.yml.md |     2 +-
-++ .../codebase/scripts_security_find_dead_code.py.md |     2 +-
-++ docs/autogen/codebase/scripts_seed_repos.py.md     |     2 +-
-++ .../autogen/codebase/scripts_setup_ci_runner.py.md |     2 +-
-++ .../codebase/scripts_setup_firebase_admin.py.md    |     2 +-
-++ docs/autogen/codebase/scripts_skill_loader.py.md   |     2 +-
-++ .../codebase/scripts_supreme-config-audit.py.md    |     2 +-
-++ .../codebase/scripts_supreme-docker-analyzer.py.md |     2 +-
-++ .../codebase/scripts_supreme-risk-scorer.py.md     |     2 +-
-++ .../codebase/scripts_supreme_context_builder.py.md |     2 +-
-++ .../scripts_tenant_auto_tenant_health_report.py.md |     2 +-
-++ .../scripts_tenant_auto_tenant_setup.py.md         |     2 +-
-++ docs/autogen/codebase/scripts_test_bangla.py.md    |     2 +-
-++ docs/autogen/codebase/scripts_test_read.py.md      |     2 +-
-++ docs/autogen/codebase/security-scan.yml.md         |     2 +-
-++ .../codebase/skills_dynamic_csv_exporter.py.md     |     2 +-
-++ .../codebase/skills_dynamic_text_summarizer.py.md  |     2 +-
-++ .../codebase/skills_dynamic_web_scraper.py.md      |     2 +-
-++ docs/autogen/codebase/skills_init_.py.md           |     2 +-
-++ docs/autogen/codebase/skills_installer.py.md       |     2 +-
-++ docs/autogen/codebase/skills_marketplace.py.md     |     2 +-
-++ docs/autogen/codebase/skills_registry.py.md        |     2 +-
-++ docs/autogen/codebase/skills_schema.py.md          |     2 +-
-++ .../codebase/test-results_.last-run.json.md        |     2 +-
-++ .../codebase/tests_e2e_admin-dashboard.spec.ts.md  |     2 +-
-++ docs/autogen/codebase/tests_e2e_chat.spec.ts.md    |     2 +-
-++ .../codebase/tests_e2e_playwright.config.ts.md     |     2 +-
-++ docs/autogen/codebase/tests_e2e_visual.spec.ts.md  |     2 +-
-++ docs/autogen/codebase/tests_test_tenant_di.py.md   |     2 +-
-++ docs/autogen/codebase/tools_cache_cleanup.py.md    |     2 +-
-++ .../tools_vscode-extension_ARCHITECTURE_BN.md.md   |     2 +-
-++ ...vscode-extension_AdminMetricsController.java.md |     2 +-
-++ ...s_vscode-extension_CodebaseAuditService.java.md |     2 +-
-++ ...ools_vscode-extension_FeatureDefinition.java.md |     2 +-
-++ ...ode-extension_FeatureRegistryController.java.md |     2 +-
-++ ...vscode-extension_FeatureRegistryService.java.md |     2 +-
-++ .../tools_vscode-extension_GlobalMetrics.java.md   |     2 +-
-++ ...s_vscode-extension_GlobalMetricsService.java.md |     2 +-
-++ ...ols_vscode-extension_INTEGRATION_GUIDE_BN.md.md |     2 +-
-++ .../codebase/tools_vscode-extension_README.md.md   |     2 +-
-++ .../tools_vscode-extension_README_BN.md.md         |     2 +-
-++ .../tools_vscode-extension_jest.config.js.md       |     2 +-
-++ .../tools_vscode-extension_package.json.md         |     2 +-
-++ .../tools_vscode-extension_package.nls.bn.json.md  |     2 +-
-++ .../tools_vscode-extension_src_agentDetector.ts.md |     2 +-
-++ .../tools_vscode-extension_src_ai_AIService.ts.md  |     2 +-
-++ ...de-extension_src_ai_CodeGenerationService.ts.md |     2 +-
-++ ...vscode-extension_src_ai_CodeReviewService.ts.md |     2 +-
-++ ...ls_vscode-extension_src_ai_ContextBuilder.ts.md |     2 +-
-++ ...xtension_src_dataconnect-generated_README.md.md |     2 +-
-++ ...n_src_dataconnect-generated_esm_index.esm.js.md |     2 +-
-++ ...n_src_dataconnect-generated_esm_package.json.md |     2 +-
-++ ...nsion_src_dataconnect-generated_index.cjs.js.md |     2 +-
-++ ...tension_src_dataconnect-generated_index.d.ts.md |     2 +-
-++ ...nsion_src_dataconnect-generated_package.json.md |     2 +-
-++ .../tools_vscode-extension_src_extension.ts.md     |     2 +-
-++ ...de-extension_src_handlers_CodeEditHandler.ts.md |     2 +-
-++ ...de-extension_src_handlers_CodeFlowHandler.ts.md |     2 +-
-++ ...scode-extension_src_handlers_ErrorHandler.ts.md |     2 +-
-++ ...de-extension_src_handlers_FeedbackHandler.ts.md |     2 +-
-++ ...ode-extension_src_providers_CodeFlowPanel.ts.md |     2 +-
-++ ...nsion_src_providers_StreamingChatProvider.ts.md |     2 +-
-++ ...n_src_providers_SupremeAIActivityProvider.ts.md |     2 +-
-++ ...providers_SupremeAIAdminDashboardProvider.ts.md |     2 +-
-++ ...nsion_src_providers_SupremeAIChatProvider.ts.md |     2 +-
-++ ...extension_src_providers_SupremeAIChatView.ts.md |     2 +-
-++ ...viders_SupremeAICustomerDashboardProvider.ts.md |     2 +-
-++ ...on_src_providers_SupremeAISidebarProvider.ts.md |     2 +-
-++ ...vscode-extension_src_services_AuthService.ts.md |     2 +-
-++ ...e-extension_src_services_SupremeAIService.ts.md |     2 +-
-++ .../tools_vscode-extension_src_types_index.ts.md   |     2 +-
-++ ...ension_src_utils_DynamicSignatureRegistry.ts.md |     2 +-
-++ ...s_vscode-extension_test_auth-service.test.ts.md |     5 +-
-++ ...ools_vscode-extension_test_mocks_vscode.d.ts.md |     2 +-
-++ .../tools_vscode-extension_test_mocks_vscode.ts.md |     2 +-
-++ .../tools_vscode-extension_test_setup.ts.md        |     2 +-
-++ ...ode-extension_test_supremeai-service.test.ts.md |     6 +-
-++ .../tools_vscode-extension_tsconfig.json.md        |     2 +-
-++ .../tools_vscode-extension_vitest.config.ts.md     |     2 +-
-++ docs/autogen/codebase/turbo.json.md                |     2 +-
-++ docs/autogen/codebase/visual.spec.ts.md            |     2 +-
-++ docs/autogen/codebase_full.md                      |     5 +-
-++ 1055 files changed, 10133 insertions(+), 16919 deletions(-)
-++
-++```
-++
-++## Diff Detail
-++```diff
-++commit 1502ebdec61ad3725a47e0a6db5e6670dfd49ac7
-++Author: github-actions[bot] <github-actions[bot]@users.noreply.github.com>
-++Date:   Sat Jul 4 05:29:44 2026 +0000
-++
-++    docs: auto-update codebase docs & dashboard [skip ci]
-++
-++diff --git a/docs/autogen/INDEX.md b/docs/autogen/INDEX.md
-++index e3b822f47..dd03785ab 100644
-++--- a/docs/autogen/INDEX.md
-+++++ b/docs/autogen/INDEX.md
-++@@ -13,4 +13,4 @@
-++ - **ডিরেক্টরি:** [changes/](changes/)
-++ 
-++ ---
-++-*স্বয়ংক্রিয়ভাবে তৈরি — 2026-07-04 05:05:31*
-+++*স্বয়ংক্রিয়ভাবে তৈরি — 2026-07-04 05:29:43*
-++diff --git a/docs/autogen/changes/change_140a3e78c8e7358e7c7dafcd497bd80acc075233.md b/docs/autogen/changes/change_140a3e78c8e7358e7c7dafcd497bd80acc075233.md
-++new file mode 100644
-++index 000000000..987730e37
-++--- /dev/null
-+++++ b/docs/autogen/changes/change_140a3e78c8e7358e7c7dafcd497bd80acc075233.md
-++@@ -0,0 +1,45 @@
-+++# 📋 Commit 140a3e78c8e7358e7c7dafcd497bd80acc075233
-+++
-+++## Commit Stats
-+++```
-+++commit 140a3e78c8e7358e7c7dafcd497bd80acc075233
-+++Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
-+++Date:   Sat Jul 4 11:29:01 2026 +0600
-+++
-+++    fix(vitest): use globals in vscode-extension tests
-+++
-+++ tools/vscode-extension/test/auth-service.test.ts      | 1 -
-+++ tools/vscode-extension/test/supremeai-service.test.ts | 2 --
-+++ 2 files changed, 3 deletions(-)
-+++
-+++```
-+++
-+++## Diff Detail
-+++```diff
-+++commit 140a3e78c8e7358e7c7dafcd497bd80acc075233
-+++Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
-+++Date:   Sat Jul 4 11:29:01 2026 +0600
-+++
-+++    fix(vitest): use globals in vscode-extension tests
-+++
-+++diff --git a/tools/vscode-extension/test/auth-service.test.ts b/tools/vscode-extension/test/auth-service.test.ts
-+++index 646478253..a5767af4d 100644
-+++--- a/tools/vscode-extension/test/auth-service.test.ts
-++++++ b/tools/vscode-extension/test/auth-service.test.ts
-+++@@ -1,4 +1,3 @@
-+++-import { vi } from 'vitest';
-+++ import * as vscode from 'vscode';
-+++ import { AuthService } from '../src/services/AuthService';
-+++ 
-+++diff --git a/tools/vscode-extension/test/supremeai-service.test.ts b/tools/vscode-extension/test/supremeai-service.test.ts
-+++index 84ecc639e..9fe909137 100644
-+++--- a/tools/vscode-extension/test/supremeai-service.test.ts
-++++++ b/tools/vscode-extension/test/supremeai-service.test.ts
-+++@@ -1,5 +1,3 @@
-+++-import { vi } from 'vitest';
-+++-
-+++ vi.mock('axios', () => {
-+++   const mockAxios = {
-+++     post: vi.fn(),
-+++
-+++```
-++diff --git a/docs/autogen/changes/change_20ab02ef3d40d7f4ec9a2d746a13f7e24501c001.md b/docs/autogen/changes/change_20ab02ef3d40d7f4ec9a2d746a13f7e24501c001.md
-++deleted file mode 100644
-++index 1ceec59b8..000000000
-++--- a/docs/autogen/changes/change_20ab02ef3d40d7f4ec9a2d746a13f7e24501c001.md
-+++++ /dev/null
-++@@ -1,5139 +0,0 @@
-++-# 📋 Commit 20ab02ef3d40d7f4ec9a2d746a13f7e24501c001
-++-
-++-## Commit Stats
-++-```
-++-commit 20ab02ef3d40d7f4ec9a2d746a13f7e24501c001
-++-Author: devin-ai-integration[bot] <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-Date:   Sat Jul 4 10:10:31 2026 +0600
-++-
-++-    feat(studio-client): Devin-style dashboard — sessions, auth vault, automation queue, admin site-actions/LLM gateway, Sujon live background (#162)
-++-    
-++-    * feat(studio-client): add Devin-style user dashboard shell with sessions, knowledge, secrets, usage and settings pages
-++-    
-++-    Co-Authored-By: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-    
-++-    * fix(studio-client): immutable session state updates and live refresh of session detail via sessions-updated event
-++-    
-++-    Co-Authored-By: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-    
-++-    * feat(dashboard): auth vault, automation queue, site actions registry, LLM gateway, Sujon live background
-++-    
-++-    - Web Authorization Vault UI (#/vault): masked session token import, sync trigger, connection status
-++-    - Automation Workflow Queue (#/automation): live Playwright task states with polling
-++-    - site_actions_registry CRUD editor (#/site-actions) + SQLite-backed /api/admin/site-actions router
-++-    - LLM Gateway & System Rules controller (#/llm-gateway) + /api/admin/llm router
-++-    - LiveSujonBackground: CSS-only GPU-optimized 3-state ambient background (idle/processing/circuit_open)
-++-    - Race-condition fix: re-read latest session from localStorage before saving AI responses
-++-    
-++-    Co-Authored-By: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-    
-++-    * chore: sync pnpm-lock.yaml with root package.json (vitest, miniflare) and merge main
-++-    
-++-    Co-Authored-By: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-    
-++-    ---------
-++-    
-++-    Co-authored-by: niloy joy <niloyjoy7@gmail.com>
-++-    Co-authored-by: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-
-++- apps/studio-client/src/App.test.tsx                |    2 +
-++- apps/studio-client/src/App.tsx                     |   14 +-
-++- .../src/components/LiveSujonBackground.tsx         |  114 ++
-++- .../components/dashboard/AutomationQueuePage.tsx   |  165 +++
-++- .../components/dashboard/DashboardShell.test.tsx   |  117 ++
-++- .../src/components/dashboard/DashboardShell.tsx    |  181 ++++
-++- .../src/components/dashboard/KnowledgePage.tsx     |  117 ++
-++- .../src/components/dashboard/LlmGatewayPage.tsx    |  215 ++++
-++- .../src/components/dashboard/SecretsPage.tsx       |  174 +++
-++- .../src/components/dashboard/SessionDetailPage.tsx |  168 +++
-++- .../src/components/dashboard/SessionsPage.tsx      |  162 +++
-++- .../src/components/dashboard/SettingsPage.tsx      |  178 ++++
-++- .../src/components/dashboard/SiteActionsPage.tsx   |  272 +++++
-++- .../src/components/dashboard/UsagePage.tsx         |  121 +++
-++- .../src/components/dashboard/VaultPage.tsx         |  208 ++++
-++- .../src/components/dashboard/sessionStore.ts       |   77 ++
-++- .../src/components/dashboard/useHashRoute.ts       |   50 +
-++- backend/api/routes/__init__.py                     |   16 +
-++- backend/api/routes/llm_gateway.py                  |   98 ++
-++- backend/api/routes/site_actions.py                 |  143 +++
-++- backend/core/app.py                                |    8 +
-++- pnpm-lock.yaml                                     | 1116 ++++++++++++--------
-++- 22 files changed, 3280 insertions(+), 436 deletions(-)
-++-
-++-```
-++-
-++-## Diff Detail
-++-```diff
-++-commit 20ab02ef3d40d7f4ec9a2d746a13f7e24501c001
-++-Author: devin-ai-integration[bot] <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-Date:   Sat Jul 4 10:10:31 2026 +0600
-++-
-++-    feat(studio-client): Devin-style dashboard — sessions, auth vault, automation queue, admin site-actions/LLM gateway, Sujon live background (#162)
-++-    
-++-    * feat(studio-client): add Devin-style user dashboard shell with sessions, knowledge, secrets, usage and settings pages
-++-    
-++-    Co-Authored-By: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-    
-++-    * fix(studio-client): immutable session state updates and live refresh of session detail via sessions-updated event
-++-    
-++-    Co-Authored-By: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-    
-++-    * feat(dashboard): auth vault, automation queue, site actions registry, LLM gateway, Sujon live background
-++-    
-++-    - Web Authorization Vault UI (#/vault): masked session token import, sync trigger, connection status
-++-    - Automation Workflow Queue (#/automation): live Playwright task states with polling
-++-    - site_actions_registry CRUD editor (#/site-actions) + SQLite-backed /api/admin/site-actions router
-++-    - LLM Gateway & System Rules controller (#/llm-gateway) + /api/admin/llm router
-++-    - LiveSujonBackground: CSS-only GPU-optimized 3-state ambient background (idle/processing/circuit_open)
-++-    - Race-condition fix: re-read latest session from localStorage before saving AI responses
-++-    
-++-    Co-Authored-By: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-    
-++-    * chore: sync pnpm-lock.yaml with root package.json (vitest, miniflare) and merge main
-++-    
-++-    Co-Authored-By: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-    
-++-    ---------
-++-    
-++-    Co-authored-by: niloy joy <niloyjoy7@gmail.com>
-++-    Co-authored-by: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>
-++-
-++-diff --git a/apps/studio-client/src/App.test.tsx b/apps/studio-client/src/App.test.tsx
-++-index ef395b794..df6717934 100644
-++---- a/apps/studio-client/src/App.test.tsx
-++-+++ b/apps/studio-client/src/App.test.tsx
-++-@@ -72,6 +72,8 @@ describe('App component', () => {
-++-     storeState.isServerOnline = true;
-++-     storeState.deployGate.status = 'UNLOCKED';
-++-     storeState.deployGate.reason = 'Initial deploy clean';
-++-+    // বাংলা মন্তব্য: লিগ্যাসি ওয়ার্কস্পেস এখন Devin-স্টাইল শেলের #/workspace রুটে রেন্ডার হয়, তাই টেস্টের আগে hash সেট করা হলো
-++-+    window.location.hash = '#/workspace';
-++-   });
-++- 
-++-   // বাংলা মন্তব্য: UI টেক্সট পরিবর্তন হওয়া সত্ত্বেও টেস্ট যাতে স্ট্যাবল থাকে সে জন্য data-testid ব্যবহার করা হলো
-++-diff --git a/apps/studio-client/src/App.tsx b/apps/studio-client/src/App.tsx
-++-index 08710f7f2..ff1e75033 100644
-++---- a/apps/studio-client/src/App.tsx
-++-+++ b/apps/studio-client/src/App.tsx
-++-@@ -3,6 +3,8 @@ import { useStore } from "./store/useStore";
-++- import { useAdminStore } from "./store/adminStore";
-++- import { AdminConsole } from "./components/admin/AdminConsole";
-++- import { UserDashboard } from "./components/customer/UserDashboard";
-++-+// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেল ইম্পোর্ট — সেশন, নলেজ, সিক্রেট, ইউসেজ ও সেটিংস পেজসহ
-++-+import { DashboardShell } from "./components/dashboard/DashboardShell";
-++- import { getAethelResponse } from "./services/chatService";
-++- import type { ChatMessage } from "./services/chatService";
-++- import { getApiBaseUrl } from "./utils/api";
-++-@@ -435,7 +437,8 @@ export const App: React.FC = () => {
-++-     setCode(code);
-++-   };
-++- 
-++--  return (
-++-+  // বাংলা মন্তব্য: লিগ্যাসি SupremeAI ওয়ার্কস্পেস (চ্যাট, প্রিসেট, ব্রাউজার প্রিভিউ, মোবাইল সিমুলেটর) এখন Devin-স্টাইল শেলের "Workspace" ট্যাবে রেন্ডার হয়
-++-+  const legacyWorkspace = (
-++-     <UserDashboard
-++-       customerMessages={chatMessages}
-++-       customerInput={chatInput}
-++-@@ -456,6 +459,15 @@ export const App: React.FC = () => {
-++-       onPreview={handlePreview}
-++-     />
-++-   );
-++-+
-++-+  return (
-++-+    <DashboardShell
-++-+      theme={theme}
-++-+      toggleTheme={toggleTheme}
-++-+      isServerOnline={isServerOnline}
-++-+      workspace={legacyWorkspace}
-++-+    />
-++-+  );
-++- };
-++- 
-++- // --- Evolution Forge Component ---
-++-diff --git a/apps/studio-client/src/components/LiveSujonBackground.tsx b/apps/studio-client/src/components/LiveSujonBackground.tsx
-++-new file mode 100644
-++-index 000000000..70ff8cea6
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/LiveSujonBackground.tsx
-++-@@ -0,0 +1,114 @@
-++-+// বাংলা মন্তব্য: "Sujon" লাইভ ব্যাকগ্রাউন্ড — প্রজেক্টের রিয়েল-টাইম AI কোরের অ্যাম্বিয়েন্ট ভিজুয়াল।
-++-+// সম্পূর্ণ CSS-অ্যানিমেশন ভিত্তিক (transform/opacity-only) — GPU হার্ডওয়্যার-অ্যাক্সিলারেটেড,
-++-+// কোনো JS টাইমার/canvas লুপ নেই বলে মেমরি লিক বা CPU ওভারহেডের সুযোগ নেই (Zero Operating Cost)।
-++-+import { useEffect, useState } from 'react';
-++-+
-++-+export type SujonState = 'idle' | 'processing' | 'circuit_open';
-++-+
-++-+// বাংলা মন্তব্য: যেকোনো পেজ (যেমন Automation Queue) এই ইভেন্ট দিয়ে Sujon-এর ভিজুয়াল স্টেট বদলাতে পারে
-++-+export const SUJON_STATE_EVENT = 'supremeai:sujon-state';
-++-+
-++-+export function setSujonState(state: SujonState): void {
-++-+  window.dispatchEvent(new CustomEvent<SujonState>(SUJON_STATE_EVENT, { detail: state }));
-++-+}
-++-+
-++-+export function useSujonState(): SujonState {
-++-+  const [state, setState] = useState<SujonState>('idle');
-++-+  useEffect(() => {
-++-+    const onState = (e: Event) => setState((e as CustomEvent<SujonState>).detail);
-++-+    window.addEventListener(SUJON_STATE_EVENT, onState);
-++-+    return () => window.removeEventListener(SUJON_STATE_EVENT, onState);
-++-+  }, []);
-++-+  return state;
-++-+}
-++-+
-++-+// বাংলা মন্তব্য: স্টেট-ভিত্তিক গ্রেডিয়েন্ট ও অ্যানিমেশন কনফিগ — idle=শান্ত নীল/ধূসর,
-++-+// processing=দ্রুতগতির সায়ানেটিক পার্টিকল, circuit_open=গাঢ় লাল সতর্ক-আভা
-++-+const STATE_STYLES: Record<SujonState, { orbA: string; orbB: string; speed: string; opacity: string }> = {
-++-+  idle: {
-++-+    orbA: 'bg-blue-500/10',
-++-+    orbB: 'bg-slate-400/10',
-++-+    speed: '14s',
-++-+    opacity: 'opacity-60',
-++-+  },
-++-+  processing: {
-++-+    orbA: 'bg-cyan-400/25',
-++-+    orbB: 'bg-fuchsia-500/20',
-++-+    speed: '3s',
-++-+    opacity: 'opacity-90',
-++-+  },
-++-+  circuit_open: {
-++-+    orbA: 'bg-red-600/30',
-++-+    orbB: 'bg-rose-500/25',
-++-+    speed: '1.2s',
-++-+    opacity: 'opacity-100',
-++-+  },
-++-+};
-++-+
-++-+interface LiveSujonBackgroundProps {
-++-+  state?: SujonState;
-++-+}
-++-+
-++-+export function LiveSujonBackground({ state: forcedState }: LiveSujonBackgroundProps) {
-++-+  const liveState = useSujonState();
-++-+  const state = forcedState ?? liveState;
-++-+  const cfg = STATE_STYLES[state];
-++-+
-++-+  return (
-++-+    <div
-++-+      data-testid="sujon-background"
-++-+      data-sujon-state={state}
-++-+      aria-hidden="true"
-++-+      className={`pointer-events-none fixed inset-0 overflow-hidden transition-opacity duration-1000 ${cfg.opacity}`}
-++-+      style={{ zIndex: 0, contain: 'strict' }}
-++-+    >
-++-+      {/* বাংলা মন্তব্য: will-change + translate3d দিয়ে GPU কম্পোজিটিং লেয়ারে রেন্ডার নিশ্চিত করা হয় */}
-++-+      <div
-++-+        className={`absolute -top-32 -left-32 h-96 w-96 rounded-full blur-3xl ${cfg.orbA}`}
-++-+        style={{
-++-+          willChange: 'transform',
-++-+          animation: `sujon-drift ${cfg.speed} ease-in-out infinite alternate`,
-++-+        }}
-++-+      />
-++-+      <div
-++-+        className={`absolute -bottom-32 -right-32 h-96 w-96 rounded-full blur-3xl ${cfg.orbB}`}
-++-+        style={{
-++-+          willChange: 'transform',
-++-+          animation: `sujon-drift ${cfg.speed} ease-in-out infinite alternate-reverse`,
-++-+        }}
-++-+      />
-++-+      {state === 'processing' && (
-++-+        <div
-++-+          className="absolute inset-0"
-++-+          style={{
-++-+            backgroundImage:
-++-+              'repeating-linear-gradient(115deg, transparent 0px, transparent 38px, rgba(34,211,238,0.08) 40px)',
-++-+            willChange: 'transform',
-++-+            animation: 'sujon-scan 2.4s linear infinite',
-++-+          }}
-++-+        />
-++-+      )}
-++-+      {state === 'circuit_open' && (
-++-+        <div
-++-+          className="absolute inset-0 bg-red-900/20"
-++-+          style={{ animation: 'sujon-flash 1.6s ease-out infinite' }}
-++-+        />
-++-+      )}
-++-+      <style>{`
-++-+        @keyframes sujon-drift {
-++-+          from { transform: translate3d(0, 0, 0) scale(1); }
-++-+          to { transform: translate3d(60px, 40px, 0) scale(1.15); }
-++-+        }
-++-+        @keyframes sujon-scan {
-++-+          from { transform: translate3d(-40px, 0, 0); }
-++-+          to { transform: translate3d(0, 0, 0); }
-++-+        }
-++-+        @keyframes sujon-flash {
-++-+          0% { opacity: 0.9; }
-++-+          30% { opacity: 0.25; }
-++-+          100% { opacity: 0.45; }
-++-+        }
-++-+      `}</style>
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/AutomationQueuePage.tsx b/apps/studio-client/src/components/dashboard/AutomationQueuePage.tsx
-++-new file mode 100644
-++-index 000000000..923f70820
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/AutomationQueuePage.tsx
-++-@@ -0,0 +1,165 @@
-++-+// বাংলা মন্তব্য: Infinite Automation Workflow Queue — অ্যাক্টিভ Playwright ব্রাউজার টাস্ক সিকোয়েন্স,
-++-+// টাস্ক স্টেট (Queued/Running/Circuit_Open/Success/Failed), এক্সিকিউশন টাইম (৪৫s ক্যাপ) রিয়েল-টাইম তালিকা।
-++-+// টাস্ক স্টেটের ভিত্তিতে LiveSujonBackground-এর ভিজুয়াল স্টেটও আপডেট করা হয়।
-++-+import { useState, useEffect, useCallback } from 'react';
-++-+import { Plus, Trash2, Loader2, ListChecks } from 'lucide-react';
-++-+import { apiClient } from '../../services/apiClient';
-++-+import { setSujonState } from '../LiveSujonBackground';
-++-+
-++-+interface AutomationTask {
-++-+  id: string;
-++-+  goal: string;
-++-+  status: string;
-++-+  createdAt?: string;
-++-+  durationMs?: number;
-++-+}
-++-+
-++-+const EXECUTION_CAP_MS = 45000;
-++-+
-++-+// বাংলা মন্তব্য: ব্যাকএন্ড স্টেট → UI ব্যাজ স্টাইল ম্যাপিং
-++-+const stateBadge = (status: string): string => {
-++-+  const s = status.toUpperCase();
-++-+  if (s === 'RUNNING' || s === 'ACTIVE') return 'bg-blue-500/15 text-blue-300 border-blue-500/30';
-++-+  if (s === 'SUCCESS') return 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30';
-++-+  if (s === 'FAILED') return 'bg-rose-500/15 text-rose-300 border-rose-500/30';
-++-+  if (s === 'CIRCUIT_OPEN') return 'bg-red-600/20 text-red-300 border-red-600/40';
-++-+  return 'bg-slate-500/15 text-slate-300 border-slate-500/30';
-++-+};
-++-+
-++-+export function AutomationQueuePage() {
-++-+  const [tasks, setTasks] = useState<AutomationTask[]>([]);
-++-+  const [goal, setGoal] = useState('');
-++-+  const [loading, setLoading] = useState(true);
-++-+  const [creating, setCreating] = useState(false);
-++-+  const [error, setError] = useState('');
-++-+
-++-+  const refresh = useCallback(() => {
-++-+    apiClient
-++-+      .get<{ tasks: AutomationTask[] }>('/api/browser/tasks')
-++-+      .then((data) => {
-++-+        const list = data.tasks || [];
-++-+        setTasks(list);
-++-+        setError('');
-++-+        // বাংলা মন্তব্য: কোনো টাস্ক CIRCUIT_OPEN হলে লাল সতর্ক-স্টেট, চলমান থাকলে processing, নয়তো idle
-++-+        const states = list.map((t) => t.status.toUpperCase());
-++-+        if (states.includes('CIRCUIT_OPEN')) setSujonState('circuit_open');
-++-+        else if (states.some((s) => s === 'RUNNING' || s === 'ACTIVE')) setSujonState('processing');
-++-+        else setSujonState('idle');
-++-+      })
-++-+      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load tasks'))
-++-+      .finally(() => setLoading(false));
-++-+  }, []);
-++-+
-++-+  useEffect(() => {
-++-+    refresh();
-++-+    // বাংলা মন্তব্য: রিয়েল-টাইম আপডেটের জন্য ৪s পোলিং; আনমাউন্টে ক্লিয়ার হয় (মেমরি লিক নেই)
-++-+    const interval = setInterval(refresh, 4000);
-++-+    return () => {
-++-+      clearInterval(interval);
-++-+      setSujonState('idle');
-++-+    };
-++-+  }, [refresh]);
-++-+
-++-+  const handleCreate = async () => {
-++-+    if (!goal.trim() || creating) return;
-++-+    setCreating(true);
-++-+    setError('');
-++-+    try {
-++-+      await apiClient.post('/api/browser/tasks', { goal: goal.trim() });
-++-+      setGoal('');
-++-+      refresh();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to queue task');
-++-+    } finally {
-++-+      setCreating(false);
-++-+    }
-++-+  };
-++-+
-++-+  const handleDelete = async (id: string) => {
-++-+    try {
-++-+      await apiClient.delete(`/api/browser/tasks/${id}`);
-++-+      refresh();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to delete task');
-++-+    }
-++-+  };
-++-+
-++-+  return (
-++-+    <div className="max-w-3xl mx-auto px-6 py-8">
-++-+      <h1 className="text-lg font-semibold text-white flex items-center gap-2 mb-1">
-++-+        <ListChecks size={17} className="text-blue-400" />
-++-+        Automation Workflow Queue
-++-+      </h1>
-++-+      <p className="text-xs text-slate-500 mb-5">
-++-+        Active Playwright automation sequences. Each task is capped at{' '}
-++-+        {EXECUTION_CAP_MS / 1000}s of execution time.
-++-+      </p>
-++-+
-++-+      <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-3 mb-6 flex items-center gap-2">
-++-+        <input
-++-+          data-testid="automation-goal"
-++-+          value={goal}
-++-+          onChange={(e) => setGoal(e.target.value)}
-++-+          onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
-++-+          placeholder="Describe an automation goal (e.g. 'Extract latest orders from dashboard')"
-++-+          className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+        />
-++-+        <button
-++-+          data-testid="automation-queue-btn"
-++-+          onClick={handleCreate}
-++-+          disabled={!goal.trim() || creating}
-++-+          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
-++-+        >
-++-+          {creating ? <Loader2 size={12} className="animate-spin" /> : <Plus size={12} />}
-++-+          Queue task
-++-+        </button>
-++-+      </div>
-++-+
-++-+      {error && <p className="text-xs text-rose-400 mb-4">{error}</p>}
-++-+
-++-+      <div className="flex items-center justify-between mb-2">
-++-+        <h2 className="text-sm font-medium text-slate-300">Active sequences</h2>
-++-+        <span className="text-xs text-slate-500">{tasks.length} total</span>
-++-+      </div>
-++-+
-++-+      {loading ? (
-++-+        <div className="flex justify-center py-10 text-slate-500">
-++-+          <Loader2 size={18} className="animate-spin" />
-++-+        </div>
-++-+      ) : tasks.length === 0 ? (
-++-+        <p className="text-sm text-slate-500 text-center py-8">No automation tasks queued.</p>
-++-+      ) : (
-++-+        <ul className="flex flex-col gap-2">
-++-+          {tasks.map((t) => (
-++-+            <li
-++-+              key={t.id}
-++-+              data-testid="automation-row"
-++-+              className="flex items-center gap-3 p-3 rounded-lg border border-white/[0.06] bg-white/[0.02]"
-++-+            >
-++-+              <div className="flex-1 min-w-0">
-++-+                <p className="text-xs text-white truncate">{t.goal}</p>
-++-+                <p className="text-[11px] text-slate-500">
-++-+                  {t.createdAt ? new Date(t.createdAt).toLocaleString() : '—'}
-++-+                  {typeof t.durationMs === 'number' && ` · ${(t.durationMs / 1000).toFixed(1)}s`}
-++-+                </p>
-++-+              </div>
-++-+              <span
-++-+                data-testid="automation-state"
-++-+                className={`text-[10px] px-2 py-0.5 rounded-full border font-medium ${stateBadge(t.status)}`}
-++-+              >
-++-+                {t.status.toUpperCase()}
-++-+              </span>
-++-+              <button
-++-+                aria-label="Delete task"
-++-+                onClick={() => handleDelete(t.id)}
-++-+                className="p-1.5 rounded text-slate-500 hover:text-rose-400 transition-colors"
-++-+              >
-++-+                <Trash2 size={13} />
-++-+              </button>
-++-+            </li>
-++-+          ))}
-++-+        </ul>
-++-+      )}
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/DashboardShell.test.tsx b/apps/studio-client/src/components/dashboard/DashboardShell.test.tsx
-++-new file mode 100644
-++-index 000000000..866214df2
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/DashboardShell.test.tsx
-++-@@ -0,0 +1,117 @@
-++-+// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেলের স্মোক টেস্ট — সাইডবার নেভিগেশন ও পেজ রাউটিং যাচাই
-++-+import { describe, it, expect, vi, beforeEach } from 'vitest';
-++-+import { render, screen, fireEvent, act } from '@testing-library/react';
-++-+
-++-+vi.mock('../../services/apiClient', () => ({
-++-+  apiClient: {
-++-+    get: vi.fn().mockResolvedValue({ items: [], keys: [], total: 0 }),
-++-+    post: vi.fn().mockResolvedValue({}),
-++-+    put: vi.fn().mockResolvedValue({}),
-++-+    delete: vi.fn().mockResolvedValue({}),
-++-+  },
-++-+}));
-++-+
-++-+vi.mock('../../services/chatService', () => ({
-++-+  getAethelResponse: vi.fn().mockResolvedValue('Mock response'),
-++-+}));
-++-+
-++-+import { DashboardShell } from './DashboardShell';
-++-+
-++-+const renderShell = () =>
-++-+  render(
-++-+    <DashboardShell
-++-+      theme="dark"
-++-+      toggleTheme={vi.fn()}
-++-+      isServerOnline={true}
-++-+      workspace={<div data-testid="legacy-workspace">Workspace content</div>}
-++-+    />
-++-+  );
-++-+
-++-+describe('DashboardShell', () => {
-++-+  beforeEach(() => {
-++-+    window.location.hash = '';
-++-+    localStorage.clear();
-++-+  });
-++-+
-++-+  it('renders sidebar with all navigation items', () => {
-++-+    renderShell();
-++-+    expect(screen.getByTestId('dashboard-sidebar')).toBeInTheDocument();
-++-+    for (const nav of [
-++-+      'sessions',
-++-+      'workspace',
-++-+      'vault',
-++-+      'automation',
-++-+      'knowledge',
-++-+      'secrets',
-++-+      'usage',
-++-+      'settings',
-++-+      'site-actions',
-++-+      'llm-gateway',
-++-+      'admin',
-++-+    ]) {
-++-+      expect(screen.getByTestId(`nav-${nav}`)).toBeInTheDocument();
-++-+    }
-++-+    expect(screen.getByTestId('sidebar-server-status')).toHaveTextContent('Online');
-++-+  });
-++-+
-++-+  it('renders the Sujon live background in idle state by default', () => {
-++-+    renderShell();
-++-+    const bg = screen.getByTestId('sujon-background');
-++-+    expect(bg).toBeInTheDocument();
-++-+    expect(bg).toHaveAttribute('data-sujon-state', 'idle');
-++-+  });
-++-+
-++-+  it('navigates to the Web Authorization Vault page', async () => {
-++-+    renderShell();
-++-+    await act(async () => {
-++-+      fireEvent.click(screen.getByTestId('nav-vault'));
-++-+      window.dispatchEvent(new HashChangeEvent('hashchange'));
-++-+    });
-++-+    expect(screen.getByTestId('vault-connection-status')).toBeInTheDocument();
-++-+  });
-++-+
-++-+  it('navigates to the Site Actions registry editor', async () => {
-++-+    renderShell();
-++-+    await act(async () => {
-++-+      fireEvent.click(screen.getByTestId('nav-site-actions'));
-++-+      window.dispatchEvent(new HashChangeEvent('hashchange'));
-++-+    });
-++-+    expect(screen.getByTestId('sa-save-btn')).toBeInTheDocument();
-++-+  });
-++-+
-++-+  it('shows sessions page with composer by default', () => {
-++-+    renderShell();
-++-+    expect(screen.getByTestId('session-composer')).toBeInTheDocument();
-++-+    expect(screen.getByTestId('start-session-btn')).toBeInTheDocument();
-++-+  });
-++-+
-++-+  it('navigates to workspace page rendering legacy dashboard', async () => {
-++-+    renderShell();
-++-+    await act(async () => {
-++-+      fireEvent.click(screen.getByTestId('nav-workspace'));
-++-+      window.dispatchEvent(new HashChangeEvent('hashchange'));
-++-+    });
-++-+    expect(screen.getByTestId('legacy-workspace')).toBeInTheDocument();
-++-+  });
-++-+
-++-+  it('navigates to knowledge page', async () => {
-++-+    renderShell();
-++-+    await act(async () => {
-++-+      fireEvent.click(screen.getByTestId('nav-knowledge'));
-++-+      window.dispatchEvent(new HashChangeEvent('hashchange'));
-++-+    });
-++-+    expect(screen.getByTestId('knowledge-search-input')).toBeInTheDocument();
-++-+  });
-++-+
-++-+  it('starts a new session from the composer', async () => {
-++-+    renderShell();
-++-+    fireEvent.change(screen.getByTestId('session-composer'), {
-++-+      target: { value: 'Build a landing page' },
-++-+    });
-++-+    await act(async () => {
-++-+      fireEvent.click(screen.getByTestId('start-session-btn'));
-++-+      window.dispatchEvent(new HashChangeEvent('hashchange'));
-++-+    });
-++-+    expect(screen.getAllByText('Build a landing page').length).toBeGreaterThan(0);
-++-+  });
-++-+});
-++-diff --git a/apps/studio-client/src/components/dashboard/DashboardShell.tsx b/apps/studio-client/src/components/dashboard/DashboardShell.tsx
-++-new file mode 100644
-++-index 000000000..38259d7bf
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/DashboardShell.tsx
-++-@@ -0,0 +1,181 @@
-++-+// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেল — বাম সাইডবার নেভিগেশন সহ ইউজার ও অ্যাডমিন উভয়ের জন্য মূল লেআউট
-++-+import type { ReactNode } from 'react';
-++-+import {
-++-+  LayoutList,
-++-+  Boxes,
-++-+  BookOpen,
-++-+  KeyRound,
-++-+  BarChart3,
-++-+  Settings,
-++-+  ShieldCheck,
-++-+  Plus,
-++-+  Vault,
-++-+  ListChecks,
-++-+  Table2,
-++-+  Cpu,
-++-+} from 'lucide-react';
-++-+import { useHashRoute, type DashboardRoute } from './useHashRoute';
-++-+import { SessionsPage } from './SessionsPage';
-++-+import { SessionDetailPage } from './SessionDetailPage';
-++-+import { KnowledgePage } from './KnowledgePage';
-++-+import { SecretsPage } from './SecretsPage';
-++-+import { UsagePage } from './UsagePage';
-++-+import { SettingsPage } from './SettingsPage';
-++-+import { VaultPage } from './VaultPage';
-++-+import { AutomationQueuePage } from './AutomationQueuePage';
-++-+import { SiteActionsPage } from './SiteActionsPage';
-++-+import { LlmGatewayPage } from './LlmGatewayPage';
-++-+import { LiveSujonBackground } from '../LiveSujonBackground';
-++-+
-++-+interface NavItem {
-++-+  id: DashboardRoute;
-++-+  label: string;
-++-+  icon: ReactNode;
-++-+}
-++-+
-++-+const NAV_ITEMS: NavItem[] = [
-++-+  { id: 'sessions', label: 'Sessions', icon: <LayoutList size={15} /> },
-++-+  { id: 'workspace', label: 'Workspace', icon: <Boxes size={15} /> },
-++-+  { id: 'vault', label: 'Auth Vault', icon: <Vault size={15} /> },
-++-+  { id: 'automation', label: 'Automation', icon: <ListChecks size={15} /> },
-++-+  { id: 'knowledge', label: 'Knowledge', icon: <BookOpen size={15} /> },
-++-+  { id: 'secrets', label: 'Secrets', icon: <KeyRound size={15} /> },
-++-+  { id: 'usage', label: 'Usage', icon: <BarChart3 size={15} /> },
-++-+  { id: 'settings', label: 'Settings', icon: <Settings size={15} /> },
-++-+];
-++-+
-++-+// বাংলা মন্তব্য: সুপার-অ্যাডমিন কন্ট্রোল লেয়ার — সাইট অ্যাকশন রেজিস্ট্রি ও LLM গেটওয়ে
-++-+const ADMIN_NAV_ITEMS: NavItem[] = [
-++-+  { id: 'site-actions', label: 'Site Actions', icon: <Table2 size={15} /> },
-++-+  { id: 'llm-gateway', label: 'LLM Gateway', icon: <Cpu size={15} /> },
-++-+];
-++-+
-++-+interface DashboardShellProps {
-++-+  theme: 'dark' | 'light';
-++-+  toggleTheme: () => void;
-++-+  isServerOnline: boolean;
-++-+  // বাংলা মন্তব্য: লিগ্যাসি SupremeAI ওয়ার্কস্পেস (চ্যাট, প্রিসেট, ব্রাউজার প্রিভিউ ইত্যাদি) Workspace ট্যাবে রেন্ডার হয়
-++-+  workspace: ReactNode;
-++-+}
-++-+
-++-+export function DashboardShell({ theme, toggleTheme, isServerOnline, workspace }: DashboardShellProps) {
-++-+  const [route, navigate] = useHashRoute();
-++-+
-++-+  const renderPage = () => {
-++-+    switch (route.page) {
-++-+      case 'session':
-++-+        return (
-++-+          <SessionDetailPage
-++-+            sessionId={route.param || ''}
-++-+            onBack={() => navigate('sessions')}
-++-+          />
-++-+        );
-++-+      case 'workspace':
-++-+        return workspace;
-++-+      case 'vault':
-++-+        return <VaultPage />;
-++-+      case 'automation':
-++-+        return <AutomationQueuePage />;
-++-+      case 'site-actions':
-++-+        return <SiteActionsPage />;
-++-+      case 'llm-gateway':
-++-+        return <LlmGatewayPage />;
-++-+      case 'knowledge':
-++-+        return <KnowledgePage />;
-++-+      case 'secrets':
-++-+        return <SecretsPage />;
-++-+      case 'usage':
-++-+        return <UsagePage />;
-++-+      case 'settings':
-++-+        return <SettingsPage theme={theme} toggleTheme={toggleTheme} />;
-++-+      case 'sessions':
-++-+      default:
-++-+        return <SessionsPage onOpenSession={(id) => navigate('session', id)} />;
-++-+    }
-++-+  };
-++-+
-++-+  const activeNav = route.page === 'session' ? 'sessions' : route.page;
-++-+
-++-+  return (
-++-+    <div className="relative min-h-screen flex bg-[#0b0f19] text-white">
-++-+      {/* বাংলা মন্তব্য: Sujon লাইভ AI-কোর অ্যাম্বিয়েন্ট ব্যাকগ্রাউন্ড — Automation স্টেট অনুযায়ী বদলায় */}
-++-+      <LiveSujonBackground />
-++-+      <aside
-++-+        data-testid="dashboard-sidebar"
-++-+        className="relative z-10 w-56 shrink-0 border-r border-white/[0.06] bg-[#080b13] flex flex-col"
-++-+      >
-++-+        <div className="flex items-center gap-2 px-4 py-4 border-b border-white/[0.06]">
-++-+          <span className="text-blue-400 text-lg">▲</span>
-++-+          <span className="text-sm font-semibold tracking-wide">SupremeAI</span>
-++-+        </div>
-++-+
-++-+        <button
-++-+          data-testid="new-session-nav"
-++-+          onClick={() => navigate('sessions')}
-++-+          className="mx-3 mt-3 mb-2 flex items-center justify-center gap-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium py-2 transition-colors"
-++-+        >
-++-+          <Plus size={13} />
-++-+          New Session
-++-+        </button>
-++-+
-++-+        <nav className="flex-1 px-2 py-1 flex flex-col gap-0.5">
-++-+          {NAV_ITEMS.map((item) => (
-++-+            <button
-++-+              key={item.id}
-++-+              data-testid={`nav-${item.id}`}
-++-+              onClick={() => navigate(item.id)}
-++-+              className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs transition-colors ${
-++-+                activeNav === item.id
-++-+                  ? 'bg-white/[0.08] text-white'
-++-+                  : 'text-slate-400 hover:text-white hover:bg-white/[0.04]'
-++-+              }`}
-++-+            >
-++-+              {item.icon}
-++-+              {item.label}
-++-+            </button>
-++-+          ))}
-++-+
-++-+          {/* বাংলা মন্তব্য: সুপার-অ্যাডমিন কন্ট্রোল সেকশন */}
-++-+          <p className="px-3 pt-3 pb-1 text-[10px] uppercase tracking-wider text-slate-600">Admin</p>
-++-+          {ADMIN_NAV_ITEMS.map((item) => (
-++-+            <button
-++-+              key={item.id}
-++-+              data-testid={`nav-${item.id}`}
-++-+              onClick={() => navigate(item.id)}
-++-+              className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs transition-colors ${
-++-+                activeNav === item.id
-++-+                  ? 'bg-white/[0.08] text-white'
-++-+                  : 'text-slate-400 hover:text-white hover:bg-white/[0.04]'
-++-+              }`}
-++-+            >
-++-+              {item.icon}
-++-+              {item.label}
-++-+            </button>
-++-+          ))}
-++-+
-++-+          {/* বাংলা মন্তব্য: অ্যাডমিন কন্সোল আলাদা রুটে (/admin) — সেখানে TOTP লগইনসহ সম্পূর্ণ অ্যাডমিন ফিচার আছে */}
-++-+          <a
-++-+            data-testid="nav-admin"
-++-+            href="/admin"
-++-+            className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs text-slate-400 hover:text-white hover:bg-white/[0.04] transition-colors"
-++-+          >
-++-+            <ShieldCheck size={15} />
-++-+            Admin Console
-++-+          </a>
-++-+        </nav>
-++-+
-++-+        <div className="px-4 py-3 border-t border-white/[0.06] flex items-center justify-between">
-++-+          <span
-++-+            data-testid="sidebar-server-status"
-++-+            className={`text-[10px] font-medium ${isServerOnline ? 'text-emerald-400' : 'text-rose-400'}`}
-++-+          >
-++-+            ● {isServerOnline ? 'Online' : 'Offline'}
-++-+          </span>
-++-+          <span className="text-[10px] text-slate-500">Free plan</span>
-++-+        </div>
-++-+      </aside>
-++-+
-++-+      <main className="relative z-10 flex-1 min-w-0 overflow-y-auto">{renderPage()}</main>
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/KnowledgePage.tsx b/apps/studio-client/src/components/dashboard/KnowledgePage.tsx
-++-new file mode 100644
-++-index 000000000..7d41b683c
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/KnowledgePage.tsx
-++-@@ -0,0 +1,117 @@
-++-+// বাংলা মন্তব্য: Devin-স্টাইল Knowledge পেজ — ব্যাকএন্ড /api/knowledge দিয়ে নলেজ সার্চ ও সিড করা হয়
-++-+import { useState } from 'react';
-++-+import { Search, BookOpen, Database, Loader2 } from 'lucide-react';
-++-+import { apiClient } from '../../services/apiClient';
-++-+
-++-+interface KnowledgeResult {
-++-+  id: string;
-++-+  title: string;
-++-+  content: string;
-++-+  score?: number | null;
-++-+  source?: string | null;
-++-+}
-++-+
-++-+export function KnowledgePage() {
-++-+  const [query, setQuery] = useState('');
-++-+  const [results, setResults] = useState<KnowledgeResult[]>([]);
-++-+  const [searched, setSearched] = useState(false);
-++-+  const [loading, setLoading] = useState(false);
-++-+  const [seeding, setSeeding] = useState(false);
-++-+  const [status, setStatus] = useState('');
-++-+
-++-+  const handleSearch = async () => {
-++-+    if (!query.trim() || loading) return;
-++-+    setLoading(true);
-++-+    setStatus('');
-++-+    try {
-++-+      const res = await apiClient.get<KnowledgeResult[]>(
-++-+        `/api/knowledge/search?query=${encodeURIComponent(query.trim())}&limit=10`
-++-+      );
-++-+      setResults(Array.isArray(res) ? res : []);
-++-+      setSearched(true);
-++-+    } catch (error) {
-++-+      setStatus(`Search failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
-++-+    } finally {
-++-+      setLoading(false);
-++-+    }
-++-+  };
-++-+
-++-+  // বাংলা মন্তব্য: নলেজ বেস ইনডেক্স/সিড করার হ্যান্ডলার
-++-+  const handleSeed = async () => {
-++-+    if (seeding) return;
-++-+    setSeeding(true);
-++-+    setStatus('');
-++-+    try {
-++-+      await apiClient.post('/api/knowledge/seed');
-++-+      setStatus('Knowledge base seeded successfully.');
-++-+    } catch (error) {
-++-+      setStatus(`Seed failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
-++-+    } finally {
-++-+      setSeeding(false);
-++-+    }
-++-+  };
-++-+
-++-+  return (
-++-+    <div className="max-w-2xl mx-auto px-6 py-8">
-++-+      <div className="flex items-center justify-between mb-1">
-++-+        <h1 className="text-lg font-semibold text-white">Knowledge</h1>
-++-+        <button
-++-+          data-testid="seed-knowledge-btn"
-++-+          onClick={handleSeed}
-++-+          disabled={seeding}
-++-+          className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 text-xs text-slate-300 hover:bg-white/[0.05] disabled:opacity-50 transition-colors"
-++-+        >
-++-+          {seeding ? <Loader2 size={12} className="animate-spin" /> : <Database size={12} />}
-++-+          Seed knowledge base
-++-+        </button>
-++-+      </div>
-++-+      <p className="text-xs text-slate-500 mb-6">
-++-+        Search the indexed knowledge base that powers SupremeAI's answers.
-++-+      </p>
-++-+
-++-+      <div className="flex items-center gap-2 mb-6">
-++-+        <div className="flex-1 flex items-center gap-2 rounded-xl border border-white/10 bg-white/[0.03] px-3 py-2 focus-within:border-blue-500/50 transition-colors">
-++-+          <Search size={14} className="text-slate-500" />
-++-+          <input
-++-+            data-testid="knowledge-search-input"
-++-+            value={query}
-++-+            onChange={(e) => setQuery(e.target.value)}
-++-+            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
-++-+            placeholder="Search knowledge..."
-++-+            className="flex-1 bg-transparent text-sm text-white placeholder-slate-500 outline-none"
-++-+          />
-++-+        </div>
-++-+        <button
-++-+          data-testid="knowledge-search-btn"
-++-+          onClick={handleSearch}
-++-+          disabled={!query.trim() || loading}
-++-+          className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
-++-+        >
-++-+          {loading ? <Loader2 size={12} className="animate-spin" /> : 'Search'}
-++-+        </button>
-++-+      </div>
-++-+
-++-+      {status && <p className="text-xs text-slate-400 mb-4">{status}</p>}
-++-+
-++-+      {searched && results.length === 0 && !loading && (
-++-+        <p className="text-sm text-slate-500 text-center py-8">No results found.</p>
-++-+      )}
-++-+
-++-+      <ul className="flex flex-col gap-3">
-++-+        {results.map((r) => (
-++-+          <li key={r.id} className="rounded-xl border border-white/[0.06] bg-white/[0.02] p-4">
-++-+            <div className="flex items-center gap-2 mb-1.5">
-++-+              <BookOpen size={13} className="text-blue-400" />
-++-+              <h3 className="text-xs font-medium text-white flex-1 truncate">{r.title}</h3>
-++-+              {typeof r.score === 'number' && (
-++-+                <span className="text-[10px] text-slate-500">score {r.score.toFixed(2)}</span>
-++-+              )}
-++-+            </div>
-++-+            <p className="text-[11px] text-slate-400 line-clamp-3 whitespace-pre-wrap">{r.content}</p>
-++-+            {r.source && <p className="text-[10px] text-slate-600 mt-1.5">source: {r.source}</p>}
-++-+          </li>
-++-+        ))}
-++-+      </ul>
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/LlmGatewayPage.tsx b/apps/studio-client/src/components/dashboard/LlmGatewayPage.tsx
-++-new file mode 100644
-++-index 000000000..d16ddec02
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/LlmGatewayPage.tsx
-++-@@ -0,0 +1,215 @@
-++-+// বাংলা মন্তব্য: LLM Gateway & System Rules Controller (Super-Admin) — ফলব্যাক রাউটিং চেইন,
-++-+// লাইভ AI মডেল সুইচ এবং কেন্দ্রীয় সিস্টেম রুল রিয়েল-টাইমে পরিবর্তন করা যায়।
-++-+// এন্ডপয়েন্ট /api/admin/llm/* — স্টুডিও ড্যাশবোর্ড থেকে সরাসরি রিচেবল।
-++-+import { useState, useEffect, useCallback } from 'react';
-++-+import { Cpu, Loader2, Save, Zap } from 'lucide-react';
-++-+import { apiClient } from '../../services/apiClient';
-++-+
-++-+interface Provider {
-++-+  id: string;
-++-+  name: string;
-++-+  status: string;
-++-+  latency_ms: number;
-++-+  models: string[];
-++-+  mode: string;
-++-+}
-++-+
-++-+interface ModelRouter {
-++-+  current_override: { provider: string; model: string } | null;
-++-+  provider_order: string[];
-++-+  cost_quality_preference: number;
-++-+}
-++-+
-++-+export function LlmGatewayPage() {
-++-+  const [providers, setProviders] = useState<Provider[]>([]);
-++-+  const [router, setRouter] = useState<ModelRouter | null>(null);
-++-+  const [rulesText, setRulesText] = useState('');
-++-+  const [rulesKeyCount, setRulesKeyCount] = useState(0);
-++-+  const [loading, setLoading] = useState(true);
-++-+  const [error, setError] = useState('');
-++-+  const [status, setStatus] = useState('');
-++-+  const [savingRules, setSavingRules] = useState(false);
-++-+
-++-+  const loadAll = useCallback(() => {
-++-+    setLoading(true);
-++-+    setError('');
-++-+    Promise.all([
-++-+      apiClient.get<Provider[]>('/api/admin/llm/providers'),
-++-+      apiClient.get<ModelRouter>('/api/admin/llm/router'),
-++-+      apiClient.get<Record<string, unknown>>('/api/admin/llm/rules'),
-++-+    ])
-++-+      .then(([p, r, ru]) => {
-++-+        setProviders(Array.isArray(p) ? p : []);
-++-+        setRouter(r);
-++-+        setRulesKeyCount(Object.keys(ru || {}).length);
-++-+        setRulesText(JSON.stringify(ru || {}, null, 2));
-++-+      })
-++-+      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load gateway data'))
-++-+      .finally(() => setLoading(false));
-++-+  }, []);
-++-+
-++-+  useEffect(() => {
-++-+    loadAll();
-++-+  }, [loadAll]);
-++-+
-++-+  // বাংলা মন্তব্য: লাইভ মডেল সুইচ — নির্দিষ্ট প্রোভাইডার/মডেলে রাউটার ওভাররাইড সেট করা হয়
-++-+  const handleSwitchModel = async (provider: string, model: string) => {
-++-+    setStatus('');
-++-+    setError('');
-++-+    try {
-++-+      await apiClient.post('/api/admin/llm/router/override', {
-++-+        provider,
-++-+        model,
-++-+        remaining_requests: 100,
-++-+      });
-++-+      setStatus(`Routing switched to ${provider}/${model} for next 100 requests.`);
-++-+      loadAll();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to switch model');
-++-+    }
-++-+  };
-++-+
-++-+  // বাংলা মন্তব্য: সিস্টেম রুল স্কিমা রিয়েল-টাইমে মিউটেট করে সেভ করা হয়
-++-+  const handleSaveRules = async () => {
-++-+    setSavingRules(true);
-++-+    setStatus('');
-++-+    setError('');
-++-+    try {
-++-+      const parsed = JSON.parse(rulesText);
-++-+      await apiClient.post('/api/admin/llm/rules', { rules: parsed });
-++-+      setRulesKeyCount(Object.keys(parsed).length);
-++-+      setStatus('System rules saved successfully.');
-++-+    } catch (err) {
-++-+      setError(
-++-+        err instanceof SyntaxError
-++-+          ? 'Invalid JSON in rules editor.'
-++-+          : err instanceof Error
-++-+            ? err.message
-++-+            : 'Failed to save rules'
-++-+      );
-++-+    } finally {
-++-+      setSavingRules(false);
-++-+    }
-++-+  };
-++-+
-++-+  return (
-++-+    <div className="max-w-3xl mx-auto px-6 py-8">
-++-+      <div className="flex items-center justify-between mb-1">
-++-+        <h1 className="text-lg font-semibold text-white flex items-center gap-2">
-++-+          <Cpu size={17} className="text-blue-400" />
-++-+          LLM Gateway & System Rules
-++-+        </h1>
-++-+        <button
-++-+          data-testid="gateway-refresh"
-++-+          onClick={loadAll}
-++-+          disabled={loading}
-++-+          className="px-3 py-1.5 rounded-lg border border-white/10 text-xs text-slate-300 hover:bg-white/[0.05] disabled:opacity-50 transition-colors"
-++-+        >
-++-+          Refresh
-++-+        </button>
-++-+      </div>
-++-+      <p className="text-xs text-slate-500 mb-5">
-++-+        Toggle fallback routing chains, switch the live AI model, and mutate central system rules.
-++-+      </p>
-++-+
-++-+      {error && <p className="text-xs text-rose-400 mb-3">{error}</p>}
-++-+      {status && <p className="text-xs text-emerald-400 mb-3">{status}</p>}
-++-+
-++-+      {loading ? (
-++-+        <div className="flex justify-center py-10 text-slate-500">
-++-+          <Loader2 size={18} className="animate-spin" />
-++-+        </div>
-++-+      ) : (
-++-+        <>
-++-+          {router && (
-++-+            <div className="rounded-xl border border-white/[0.06] bg-white/[0.02] p-4 mb-5">
-++-+              <h2 className="text-xs font-medium text-slate-300 mb-2">Fallback routing chain</h2>
-++-+              <div className="flex flex-wrap items-center gap-2">
-++-+                {router.provider_order.map((p, i) => (
-++-+                  <span key={p} className="flex items-center gap-2 text-[11px] text-slate-300">
-++-+                    <span className="px-2 py-0.5 rounded-full bg-white/[0.06] border border-white/10">
-++-+                      {i + 1}. {p}
-++-+                    </span>
-++-+                    {i < router.provider_order.length - 1 && (
-++-+                      <span className="text-slate-600">→</span>
-++-+                    )}
-++-+                  </span>
-++-+                ))}
-++-+              </div>
-++-+              {router.current_override && (
-++-+                <p
-++-+                  data-testid="gateway-active-override"
-++-+                  className="text-[11px] text-emerald-400 mt-2"
-++-+                >
-++-+                  Active override: {router.current_override.provider}/
-++-+                  {router.current_override.model}
-++-+                </p>
-++-+              )}
-++-+            </div>
-++-+          )}
-++-+
-++-+          <h2 className="text-xs font-medium text-slate-300 mb-2">Providers & live model switch</h2>
-++-+          <ul className="flex flex-col gap-2 mb-6">
-++-+            {providers.length === 0 ? (
-++-+              <p className="text-sm text-slate-500 py-4">No providers with configured API keys.</p>
-++-+            ) : (
-++-+              providers.map((p) => (
-++-+                <li
-++-+                  key={p.id}
-++-+                  data-testid="gateway-provider"
-++-+                  className="rounded-lg border border-white/[0.06] bg-white/[0.02] p-3"
-++-+                >
-++-+                  <div className="flex items-center gap-2 mb-2">
-++-+                    <span
-++-+                      className={`h-2 w-2 rounded-full ${
-++-+                        p.status === 'healthy' ? 'bg-emerald-400' : 'bg-rose-400'
-++-+                      }`}
-++-+                    />
-++-+                    <span className="text-xs text-white flex-1">{p.name}</span>
-++-+                    <span className="text-[10px] text-slate-500">{p.latency_ms}ms</span>
-++-+                  </div>
-++-+                  <div className="flex flex-wrap gap-1.5">
-++-+                    {p.models.map((m) => (
-++-+                      <button
-++-+                        key={m}
-++-+                        data-testid="gateway-switch-model"
-++-+                        onClick={() => handleSwitchModel(p.id, m)}
-++-+                        className="flex items-center gap-1 px-2 py-1 rounded-md bg-blue-600/15 border border-blue-500/30 text-[10px] text-blue-200 hover:bg-blue-600/30 transition-colors"
-++-+                      >
-++-+                        <Zap size={10} />
-++-+                        {m}
-++-+                      </button>
-++-+                    ))}
-++-+                  </div>
-++-+                </li>
-++-+              ))
-++-+            )}
-++-+          </ul>
-++-+
-++-+          <div className="flex items-center justify-between mb-2">
-++-+            <h2 className="text-xs font-medium text-slate-300">
-++-+              System rules ({rulesKeyCount} keys)
-++-+            </h2>
-++-+            <button
-++-+              data-testid="gateway-save-rules"
-++-+              onClick={handleSaveRules}
-++-+              disabled={savingRules}
-++-+              className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
-++-+            >
-++-+              {savingRules ? <Loader2 size={12} className="animate-spin" /> : <Save size={12} />}
-++-+              Save rules
-++-+            </button>
-++-+          </div>
-++-+          <textarea
-++-+            data-testid="gateway-rules-editor"
-++-+            value={rulesText}
-++-+            onChange={(e) => setRulesText(e.target.value)}
-++-+            rows={12}
-++-+            spellCheck={false}
-++-+            className="w-full rounded-xl bg-black/40 border border-white/10 px-3 py-2 text-[11px] font-mono text-white outline-none focus:border-blue-500/50 resize-y"
-++-+          />
-++-+        </>
-++-+      )}
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/SecretsPage.tsx b/apps/studio-client/src/components/dashboard/SecretsPage.tsx
-++-new file mode 100644
-++-index 000000000..f19abbdbc
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/SecretsPage.tsx
-++-@@ -0,0 +1,174 @@
-++-+// বাংলা মন্তব্য: Devin-স্টাইল Secrets পেজ — ব্যাকএন্ড /api/api-keys দিয়ে API কী তৈরি, তালিকা, রিভোক ও ডিলিট করা হয়
-++-+import { useState, useEffect, useCallback } from 'react';
-++-+import { KeyRound, Plus, Trash2, Ban, Copy, Loader2 } from 'lucide-react';
-++-+import { apiClient } from '../../services/apiClient';
-++-+
-++-+interface ApiKeyRecord {
-++-+  id: number;
-++-+  name: string;
-++-+  key_masked: string;
-++-+  rate_limit_rps: number;
-++-+  is_active?: boolean;
-++-+  revoked?: boolean;
-++-+  created_at?: string | number;
-++-+  expires_at?: string | number | null;
-++-+}
-++-+
-++-+interface CreatedKey {
-++-+  key: string;
-++-+  name: string;
-++-+}
-++-+
-++-+export function SecretsPage() {
-++-+  const [keys, setKeys] = useState<ApiKeyRecord[]>([]);
-++-+  const [loading, setLoading] = useState(true);
-++-+  const [error, setError] = useState('');
-++-+  const [newName, setNewName] = useState('');
-++-+  const [creating, setCreating] = useState(false);
-++-+  const [createdKey, setCreatedKey] = useState<CreatedKey | null>(null);
-++-+
-++-+  const fetchKeys = useCallback(() => {
-++-+    setLoading(true);
-++-+    apiClient
-++-+      .get<{ keys: ApiKeyRecord[] }>('/api/api-keys/')
-++-+      .then((data) => {
-++-+        setKeys(data.keys || []);
-++-+        setError('');
-++-+      })
-++-+      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load API keys'))
-++-+      .finally(() => setLoading(false));
-++-+  }, []);
-++-+
-++-+  useEffect(() => {
-++-+    fetchKeys();
-++-+  }, [fetchKeys]);
-++-+
-++-+  const handleCreate = async () => {
-++-+    if (!newName.trim() || creating) return;
-++-+    setCreating(true);
-++-+    setError('');
-++-+    try {
-++-+      const res = await apiClient.post<{ key: string; name: string }>('/api/api-keys/create', {
-++-+        user_id: 'default',
-++-+        name: newName.trim(),
-++-+        rate_limit_rps: 6,
-++-+      });
-++-+      setCreatedKey({ key: res.key, name: res.name });
-++-+      setNewName('');
-++-+      fetchKeys();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to create key');
-++-+    } finally {
-++-+      setCreating(false);
-++-+    }
-++-+  };
-++-+
-++-+  const handleRevoke = async (id: number) => {
-++-+    try {
-++-+      await apiClient.post(`/api/api-keys/${id}/revoke`);
-++-+      fetchKeys();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to revoke key');
-++-+    }
-++-+  };
-++-+
-++-+  const handleDelete = async (id: number) => {
-++-+    try {
-++-+      await apiClient.delete(`/api/api-keys/${id}`);
-++-+      fetchKeys();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to delete key');
-++-+    }
-++-+  };
-++-+
-++-+  return (
-++-+    <div className="max-w-2xl mx-auto px-6 py-8">
-++-+      <h1 className="text-lg font-semibold text-white mb-1">Secrets & API Keys</h1>
-++-+      <p className="text-xs text-slate-500 mb-6">
-++-+        Create and manage API keys for programmatic access. Keys are shown only once at creation.
-++-+      </p>
-++-+
-++-+      <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4 mb-6 flex items-center gap-2">
-++-+        <input
-++-+          data-testid="new-key-name"
-++-+          value={newName}
-++-+          onChange={(e) => setNewName(e.target.value)}
-++-+          onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
-++-+          placeholder="Key name (e.g. CI pipeline)"
-++-+          className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+        />
-++-+        <button
-++-+          data-testid="create-key-btn"
-++-+          onClick={handleCreate}
-++-+          disabled={!newName.trim() || creating}
-++-+          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
-++-+        >
-++-+          {creating ? <Loader2 size={12} className="animate-spin" /> : <Plus size={12} />}
-++-+          Create key
-++-+        </button>
-++-+      </div>
-++-+
-++-+      {createdKey && (
-++-+        <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/[0.06] p-4 mb-6">
-++-+          <p className="text-xs text-emerald-300 mb-2">
-++-+            Key "{createdKey.name}" created. Copy it now — it will not be shown again.
-++-+          </p>
-++-+          <div className="flex items-center gap-2">
-++-+            <code className="flex-1 text-[11px] text-white bg-black/40 rounded px-2 py-1.5 break-all">
-++-+              {createdKey.key}
-++-+            </code>
-++-+            <button
-++-+              aria-label="Copy key"
-++-+              onClick={() => navigator.clipboard?.writeText(createdKey.key)}
-++-+              className="p-1.5 rounded text-slate-300 hover:text-white hover:bg-white/[0.08] transition-colors"
-++-+            >
-++-+              <Copy size={13} />
-++-+            </button>
-++-+          </div>
-++-+        </div>
-++-+      )}
-++-+
-++-+      {error && <p className="text-xs text-rose-400 mb-4">{error}</p>}
-++-+
-++-+      {loading ? (
-++-+        <div className="flex justify-center py-10 text-slate-500">
-++-+          <Loader2 size={18} className="animate-spin" />
-++-+        </div>
-++-+      ) : keys.length === 0 ? (
-++-+        <p className="text-sm text-slate-500 text-center py-8">No API keys yet.</p>
-++-+      ) : (
-++-+        <ul className="flex flex-col gap-2">
-++-+          {keys.map((k) => (
-++-+            <li
-++-+              key={k.id}
-++-+              className="flex items-center gap-3 p-3 rounded-lg border border-white/[0.06] bg-white/[0.02]"
-++-+            >
-++-+              <KeyRound size={14} className="text-slate-400" />
-++-+              <div className="flex-1 min-w-0">
-++-+                <p className="text-xs text-white truncate">{k.name}</p>
-++-+                <p className="text-[11px] text-slate-500 font-mono">{k.key_masked}</p>
-++-+              </div>
-++-+              <span className="text-[10px] text-slate-500">{k.rate_limit_rps} rps</span>
-++-+              <button
-++-+                aria-label="Revoke key"
-++-+                title="Revoke"
-++-+                onClick={() => handleRevoke(k.id)}
-++-+                className="p-1.5 rounded text-slate-500 hover:text-amber-400 transition-colors"
-++-+              >
-++-+                <Ban size={13} />
-++-+              </button>
-++-+              <button
-++-+                aria-label="Delete key"
-++-+                title="Delete"
-++-+                onClick={() => handleDelete(k.id)}
-++-+                className="p-1.5 rounded text-slate-500 hover:text-rose-400 transition-colors"
-++-+              >
-++-+                <Trash2 size={13} />
-++-+              </button>
-++-+            </li>
-++-+          ))}
-++-+        </ul>
-++-+      )}
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/SessionDetailPage.tsx b/apps/studio-client/src/components/dashboard/SessionDetailPage.tsx
-++-new file mode 100644
-++-index 000000000..b45f5b1f5
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/SessionDetailPage.tsx
-++-@@ -0,0 +1,168 @@
-++-+// বাংলা মন্তব্য: একটি সেশনের চ্যাট ভিউ — ফলো-আপ মেসেজ পাঠানো যায় এবং ব্যাকএন্ড থেকে উত্তর আসে
-++-+import { useState, useEffect, useRef } from 'react';
-++-+import { ArrowLeft, Send } from 'lucide-react';
-++-+import { getAethelResponse } from '../../services/chatService';
-++-+import { type DashboardSession, loadSessions, upsertSession, SESSIONS_UPDATED_EVENT } from './sessionStore';
-++-+
-++-+interface SessionDetailPageProps {
-++-+  sessionId: string;
-++-+  onBack: () => void;
-++-+}
-++-+
-++-+export function SessionDetailPage({ sessionId, onBack }: SessionDetailPageProps) {
-++-+  const [session, setSession] = useState<DashboardSession | null>(null);
-++-+  const [input, setInput] = useState('');
-++-+  const [sending, setSending] = useState(false);
-++-+  const bottomRef = useRef<HTMLDivElement>(null);
-++-+
-++-+  // বাংলা মন্তব্য: সেশন লোড + বাইরের আপডেট (যেমন SessionsPage থেকে আসা AI রেসপন্স) ধরতে ইভেন্ট লিসেনার
-++-+  useEffect(() => {
-++-+    const refresh = () => {
-++-+      const found = loadSessions().find((s) => s.id === sessionId) || null;
-++-+      setSession(found);
-++-+    };
-++-+    refresh();
-++-+    window.addEventListener(SESSIONS_UPDATED_EVENT, refresh);
-++-+    return () => window.removeEventListener(SESSIONS_UPDATED_EVENT, refresh);
-++-+  }, [sessionId]);
-++-+
-++-+  useEffect(() => {
-++-+    bottomRef.current?.scrollIntoView?.({ behavior: 'smooth' });
-++-+  }, [session?.messages.length]);
-++-+
-++-+  const handleSend = async () => {
-++-+    if (!input.trim() || sending || !session) return;
-++-+    setSending(true);
-++-+    const updated: DashboardSession = {
-++-+      ...session,
-++-+      status: 'running',
-++-+      messages: [
-++-+        ...session.messages,
-++-+        { id: Date.now(), sender: 'User', text: input.trim(), timestamp: new Date().toLocaleTimeString() },
-++-+      ],
-++-+    };
-++-+    setSession(updated);
-++-+    upsertSession(updated);
-++-+    const text = input.trim();
-++-+    setInput('');
-++-+
-++-+    // বাংলা মন্তব্য: React স্টেট অবজেক্ট মিউটেট না করে নতুন অবজেক্ট তৈরি করে আপডেট করা হয়
-++-+    let completed: DashboardSession;
-++-+    try {
-++-+      const history = updated.messages.map((m) => ({
-++-+        role: m.sender === 'User' ? ('user' as const) : ('assistant' as const),
-++-+        content: m.text,
-++-+      }));
-++-+      const responseText = await getAethelResponse(text, history);
-++-+      // বাংলা মন্তব্য: সেভের আগে localStorage থেকে সর্বশেষ সেশন পড়ে নেওয়া হয় যাতে অন্য পেজের সেভ করা মেসেজ মুছে না যায়
-++-+      const latest = loadSessions().find((s) => s.id === sessionId) || updated;
-++-+      completed = {
-++-+        ...latest,
-++-+        status: 'finished',
-++-+        messages: [
-++-+          ...latest.messages,
-++-+          { id: Date.now(), sender: 'SupremeAI', text: responseText, timestamp: new Date().toLocaleTimeString() },
-++-+        ],
-++-+      };
-++-+    } catch (error) {
-++-+      const latest = loadSessions().find((s) => s.id === sessionId) || updated;
-++-+      completed = {
-++-+        ...latest,
-++-+        status: 'error',
-++-+        messages: [
-++-+          ...latest.messages,
-++-+          {
-++-+            id: Date.now(),
-++-+            sender: 'SupremeAI',
-++-+            text: `AI backend error: ${error instanceof Error ? error.message : 'Unable to process message.'}`,
-++-+            timestamp: new Date().toLocaleTimeString(),
-++-+          },
-++-+        ],
-++-+      };
-++-+    }
-++-+    setSession(completed);
-++-+    upsertSession(completed);
-++-+    setSending(false);
-++-+  };
-++-+
-++-+  if (!session) {
-++-+    return (
-++-+      <div className="max-w-3xl mx-auto px-6 py-10 text-center">
-++-+        <p className="text-sm text-slate-500 mb-4">Session not found.</p>
-++-+        <button onClick={onBack} className="text-xs text-blue-400 hover:text-blue-300">
-++-+          ← Back to sessions
-++-+        </button>
-++-+      </div>
-++-+    );
-++-+  }
-++-+
-++-+  return (
-++-+    <div className="max-w-3xl mx-auto px-6 py-6 flex flex-col h-full">
-++-+      <div className="flex items-center gap-3 mb-4">
-++-+        <button
-++-+          onClick={onBack}
-++-+          aria-label="Back to sessions"
-++-+          className="text-slate-400 hover:text-white transition-colors"
-++-+        >
-++-+          <ArrowLeft size={16} />
-++-+        </button>
-++-+        <h1 className="text-sm font-medium text-white truncate flex-1">{session.title}</h1>
-++-+        <span
-++-+          className={`text-[10px] px-2 py-0.5 rounded-full border ${
-++-+            session.status === 'finished'
-++-+              ? 'text-emerald-400 border-emerald-400/30'
-++-+              : session.status === 'error'
-++-+                ? 'text-rose-400 border-rose-400/30'
-++-+                : 'text-blue-400 border-blue-400/30'
-++-+          }`}
-++-+        >
-++-+          {session.status}
-++-+        </span>
-++-+      </div>
-++-+
-++-+      <div className="flex-1 overflow-y-auto flex flex-col gap-3 mb-4 min-h-[300px]">
-++-+        {session.messages.map((msg) => (
-++-+          <div
-++-+            key={msg.id}
-++-+            className={`max-w-[85%] rounded-xl px-4 py-2.5 text-sm ${
-++-+              msg.sender === 'User'
-++-+                ? 'self-end bg-blue-600/80 text-white'
-++-+                : 'self-start bg-white/[0.05] text-slate-200 border border-white/[0.06]'
-++-+            }`}
-++-+          >
-++-+            <p className="whitespace-pre-wrap break-words">{msg.text}</p>
-++-+            <p className="text-[10px] opacity-50 mt-1">{msg.timestamp}</p>
-++-+          </div>
-++-+        ))}
-++-+        {sending && (
-++-+          <div className="self-start text-xs text-slate-500 animate-pulse px-2">SupremeAI is working…</div>
-++-+        )}
-++-+        <div ref={bottomRef} />
-++-+      </div>
-++-+
-++-+      <div className="flex items-end gap-2 rounded-xl border border-white/10 bg-white/[0.03] p-2 focus-within:border-blue-500/50 transition-colors">
-++-+        <textarea
-++-+          value={input}
-++-+          onChange={(e) => setInput(e.target.value)}
-++-+          onKeyDown={(e) => {
-++-+            if (e.key === 'Enter' && !e.shiftKey) {
-++-+              e.preventDefault();
-++-+              handleSend();
-++-+            }
-++-+          }}
-++-+          placeholder="Send a follow-up message..."
-++-+          rows={2}
-++-+          className="flex-1 bg-transparent text-sm text-white placeholder-slate-500 outline-none resize-none"
-++-+        />
-++-+        <button
-++-+          onClick={handleSend}
-++-+          disabled={!input.trim() || sending}
-++-+          aria-label="Send message"
-++-+          className="p-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 disabled:text-slate-500 text-white transition-colors"
-++-+        >
-++-+          <Send size={14} />
-++-+        </button>
-++-+      </div>
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/SessionsPage.tsx b/apps/studio-client/src/components/dashboard/SessionsPage.tsx
-++-new file mode 100644
-++-index 000000000..4eb35aa46
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/SessionsPage.tsx
-++-@@ -0,0 +1,162 @@
-++-+// বাংলা মন্তব্য: Devin-স্টাইল হোম — নতুন সেশন কম্পোজার ও সেশন তালিকা; ব্যাকএন্ড /task/execute দিয়ে AI রেসপন্স আনা হয়
-++-+import { useState, useEffect } from 'react';
-++-+import { Send, Trash2, CircleDot, CheckCircle2, XCircle, Clock } from 'lucide-react';
-++-+import { getAethelResponse } from '../../services/chatService';
-++-+import {
-++-+  type DashboardSession,
-++-+  loadSessions,
-++-+  createSession,
-++-+  upsertSession,
-++-+  deleteSession,
-++-+} from './sessionStore';
-++-+
-++-+interface SessionsPageProps {
-++-+  onOpenSession: (id: string) => void;
-++-+}
-++-+
-++-+const statusIcon = (status: DashboardSession['status']) => {
-++-+  if (status === 'running') return <CircleDot size={14} className="text-blue-400 animate-pulse" />;
-++-+  if (status === 'finished') return <CheckCircle2 size={14} className="text-emerald-400" />;
-++-+  return <XCircle size={14} className="text-rose-400" />;
-++-+};
-++-+
-++-+export function SessionsPage({ onOpenSession }: SessionsPageProps) {
-++-+  const [sessions, setSessions] = useState<DashboardSession[]>([]);
-++-+  const [prompt, setPrompt] = useState('');
-++-+  const [starting, setStarting] = useState(false);
-++-+
-++-+  useEffect(() => {
-++-+    setSessions(loadSessions());
-++-+  }, []);
-++-+
-++-+  // বাংলা মন্তব্য: নতুন সেশন শুরু — প্রম্পট থেকে সেশন তৈরি করে ব্যাকএন্ডে টাস্ক পাঠানো হয়
-++-+  const handleStartSession = async () => {
-++-+    if (!prompt.trim() || starting) return;
-++-+    setStarting(true);
-++-+    const session = createSession(prompt.trim());
-++-+    setSessions(upsertSession(session));
-++-+    setPrompt('');
-++-+    onOpenSession(session.id);
-++-+
-++-+    // বাংলা মন্তব্য: রেসপন্স আসার পর localStorage থেকে সর্বশেষ সেশন পড়ে তার উপর মেসেজ যোগ করা হয়,
-++-+    // যাতে ডিটেইল পেজে পাঠানো ফলো-আপ মেসেজ হারিয়ে না যায় (race condition প্রতিরোধ)
-++-+    let completed: DashboardSession;
-++-+    try {
-++-+      const responseText = await getAethelResponse(session.title, [
-++-+        { role: 'user', content: session.messages[0].text },
-++-+      ]);
-++-+      const latest = loadSessions().find((s) => s.id === session.id) || session;
-++-+      completed = {
-++-+        ...latest,
-++-+        status: 'finished',
-++-+        messages: [
-++-+          ...latest.messages,
-++-+          {
-++-+            id: Date.now(),
-++-+            sender: 'SupremeAI',
-++-+            text: responseText,
-++-+            timestamp: new Date().toLocaleTimeString(),
-++-+          },
-++-+        ],
-++-+      };
-++-+    } catch (error) {
-++-+      const latest = loadSessions().find((s) => s.id === session.id) || session;
-++-+      completed = {
-++-+        ...latest,
-++-+        status: 'error',
-++-+        messages: [
-++-+          ...latest.messages,
-++-+          {
-++-+            id: Date.now(),
-++-+            sender: 'SupremeAI',
-++-+            text: `AI backend error: ${error instanceof Error ? error.message : 'Unable to process task.'}`,
-++-+            timestamp: new Date().toLocaleTimeString(),
-++-+          },
-++-+        ],
-++-+      };
-++-+    }
-++-+    setSessions(upsertSession(completed));
-++-+    setStarting(false);
-++-+  };
-++-+
-++-+  const handleDelete = (id: string) => {
-++-+    setSessions(deleteSession(id));
-++-+  };
-++-+
-++-+  return (
-++-+    <div className="max-w-3xl mx-auto px-6 py-10">
-++-+      <h1 className="text-2xl font-semibold text-white text-center mb-6">
-++-+        What do you want to build today?
-++-+      </h1>
-++-+
-++-+      <div className="rounded-xl border border-white/10 bg-white/[0.03] p-3 mb-10 focus-within:border-blue-500/50 transition-colors">
-++-+        <textarea
-++-+          data-testid="session-composer"
-++-+          value={prompt}
-++-+          onChange={(e) => setPrompt(e.target.value)}
-++-+          onKeyDown={(e) => {
-++-+            if (e.key === 'Enter' && !e.shiftKey) {
-++-+              e.preventDefault();
-++-+              handleStartSession();
-++-+            }
-++-+          }}
-++-+          placeholder="Give SupremeAI a task to work on..."
-++-+          rows={3}
-++-+          className="w-full bg-transparent text-sm text-white placeholder-slate-500 outline-none resize-none"
-++-+        />
-++-+        <div className="flex justify-end">
-++-+          <button
-++-+            data-testid="start-session-btn"
-++-+            onClick={handleStartSession}
-++-+            disabled={!prompt.trim() || starting}
-++-+            className="flex items-center gap-2 px-4 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 disabled:text-slate-500 text-white text-xs font-medium transition-colors"
-++-+          >
-++-+            <Send size={12} />
-++-+            {starting ? 'Starting…' : 'Start Session'}
-++-+          </button>
-++-+        </div>
-++-+      </div>
-++-+
-++-+      <div className="flex items-center justify-between mb-3">
-++-+        <h2 className="text-sm font-medium text-slate-300">Recent sessions</h2>
-++-+        <span className="text-xs text-slate-500">{sessions.length} total</span>
-++-+      </div>
-++-+
-++-+      {sessions.length === 0 ? (
-++-+        <p className="text-sm text-slate-500 text-center py-10">
-++-+          No sessions yet. Start your first task above.
-++-+        </p>
-++-+      ) : (
-++-+        <ul className="flex flex-col gap-2">
-++-+          {sessions.map((session) => (
-++-+            <li
-++-+              key={session.id}
-++-+              data-testid="session-row"
-++-+              className="group flex items-center gap-3 p-3 rounded-lg border border-white/[0.06] bg-white/[0.02] hover:bg-white/[0.05] cursor-pointer transition-colors"
-++-+              onClick={() => onOpenSession(session.id)}
-++-+            >
-++-+              {statusIcon(session.status)}
-++-+              <div className="flex-1 min-w-0">
-++-+                <p className="text-sm text-white truncate">{session.title}</p>
-++-+                <p className="text-[11px] text-slate-500 flex items-center gap-1">
-++-+                  <Clock size={10} />
-++-+                  {new Date(session.updated_at).toLocaleString()}
-++-+                </p>
-++-+              </div>
-++-+              <button
-++-+                aria-label="Delete session"
-++-+                onClick={(e) => {
-++-+                  e.stopPropagation();
-++-+                  handleDelete(session.id);
-++-+                }}
-++-+                className="opacity-0 group-hover:opacity-100 text-slate-500 hover:text-rose-400 transition-all"
-++-+              >
-++-+                <Trash2 size={14} />
-++-+              </button>
-++-+            </li>
-++-+          ))}
-++-+        </ul>
-++-+      )}
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/SettingsPage.tsx b/apps/studio-client/src/components/dashboard/SettingsPage.tsx
-++-new file mode 100644
-++-index 000000000..1fe1b5e4d
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/SettingsPage.tsx
-++-@@ -0,0 +1,178 @@
-++-+// বাংলা মন্তব্য: Devin-স্টাইল সেটিংস পেজ — ব্যাকএন্ড /preferences/ এপিআই দিয়ে ইউজার প্রেফারেন্স লোড/সেভ করা হয়
-++-+import { useState, useEffect } from 'react';
-++-+import { Save, Loader2 } from 'lucide-react';
-++-+import { apiClient } from '../../services/apiClient';
-++-+
-++-+interface Preferences {
-++-+  theme: string;
-++-+  default_model: string;
-++-+  max_tokens: number;
-++-+  auto_save: boolean;
-++-+  verbosity: string;
-++-+}
-++-+
-++-+const DEFAULT_PREFS: Preferences = {
-++-+  theme: 'dark',
-++-+  default_model: 'gpt-4o',
-++-+  max_tokens: 4096,
-++-+  auto_save: true,
-++-+  verbosity: 'normal',
-++-+};
-++-+
-++-+const MODELS = ['gpt-4o', 'gpt-4o-mini', 'claude-3-5-sonnet', 'gemini-1.5-pro', 'deepseek-chat'];
-++-+
-++-+interface SettingsPageProps {
-++-+  theme: 'dark' | 'light';
-++-+  toggleTheme: () => void;
-++-+}
-++-+
-++-+export function SettingsPage({ theme, toggleTheme }: SettingsPageProps) {
-++-+  const [prefs, setPrefs] = useState<Preferences>(DEFAULT_PREFS);
-++-+  const [loading, setLoading] = useState(true);
-++-+  const [saving, setSaving] = useState(false);
-++-+  const [status, setStatus] = useState('');
-++-+
-++-+  useEffect(() => {
-++-+    apiClient
-++-+      .get<Partial<Preferences>>('/preferences/?user_id=default')
-++-+      .then((data) => setPrefs({ ...DEFAULT_PREFS, ...data }))
-++-+      .catch(() => setStatus('Failed to load preferences — using defaults.'))
-++-+      .finally(() => setLoading(false));
-++-+  }, []);
-++-+
-++-+  const handleSave = async () => {
-++-+    setSaving(true);
-++-+    setStatus('');
-++-+    try {
-++-+      await apiClient.post('/preferences/?user_id=default', {
-++-+        theme: prefs.theme,
-++-+        default_model: prefs.default_model,
-++-+        max_tokens: prefs.max_tokens,
-++-+        auto_save: prefs.auto_save,
-++-+        verbosity: prefs.verbosity,
-++-+      });
-++-+      setStatus('Preferences saved.');
-++-+    } catch (error) {
-++-+      setStatus(`Save failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
-++-+    } finally {
-++-+      setSaving(false);
-++-+      setTimeout(() => setStatus(''), 3000);
-++-+    }
-++-+  };
-++-+
-++-+  if (loading) {
-++-+    return (
-++-+      <div className="flex items-center justify-center py-20 text-slate-500">
-++-+        <Loader2 size={20} className="animate-spin" />
-++-+      </div>
-++-+    );
-++-+  }
-++-+
-++-+  return (
-++-+    <div className="max-w-2xl mx-auto px-6 py-8">
-++-+      <h1 className="text-lg font-semibold text-white mb-1">Settings</h1>
-++-+      <p className="text-xs text-slate-500 mb-6">Manage your workspace preferences.</p>
-++-+
-++-+      <div className="flex flex-col gap-5">
-++-+        <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-5">
-++-+          <h2 className="text-sm font-medium text-white mb-3">Appearance</h2>
-++-+          <div className="flex items-center justify-between">
-++-+            <div>
-++-+              <p className="text-xs text-slate-300">Theme</p>
-++-+              <p className="text-[11px] text-slate-500">Switch between light and dark mode.</p>
-++-+            </div>
-++-+            <button
-++-+              data-testid="settings-theme-toggle"
-++-+              onClick={() => {
-++-+                toggleTheme();
-++-+                setPrefs((p) => ({ ...p, theme: theme === 'dark' ? 'light' : 'dark' }));
-++-+              }}
-++-+              className="px-3 py-1.5 rounded-lg border border-white/10 text-xs text-slate-200 hover:bg-white/[0.05] transition-colors"
-++-+            >
-++-+              {theme === 'dark' ? 'Switch to Light' : 'Switch to Dark'}
-++-+            </button>
-++-+          </div>
-++-+        </div>
-++-+
-++-+        <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-5 flex flex-col gap-4">
-++-+          <h2 className="text-sm font-medium text-white">AI Model</h2>
-++-+          <div>
-++-+            <label className="block text-xs text-slate-300 mb-1" htmlFor="default-model">
-++-+              Default model
-++-+            </label>
-++-+            <select
-++-+              id="default-model"
-++-+              value={prefs.default_model}
-++-+              onChange={(e) => setPrefs((p) => ({ ...p, default_model: e.target.value }))}
-++-+              className="w-full rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white outline-none focus:border-blue-500/50"
-++-+            >
-++-+              {MODELS.map((m) => (
-++-+                <option key={m} value={m}>
-++-+                  {m}
-++-+                </option>
-++-+              ))}
-++-+            </select>
-++-+          </div>
-++-+          <div>
-++-+            <label className="block text-xs text-slate-300 mb-1" htmlFor="max-tokens">
-++-+              Max tokens per response
-++-+            </label>
-++-+            <input
-++-+              id="max-tokens"
-++-+              type="number"
-++-+              min={256}
-++-+              max={128000}
-++-+              value={prefs.max_tokens}
-++-+              onChange={(e) => setPrefs((p) => ({ ...p, max_tokens: Number(e.target.value) }))}
-++-+              className="w-full rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white outline-none focus:border-blue-500/50"
-++-+            />
-++-+          </div>
-++-+          <div>
-++-+            <label className="block text-xs text-slate-300 mb-1" htmlFor="verbosity">
-++-+              Response verbosity
-++-+            </label>
-++-+            <select
-++-+              id="verbosity"
-++-+              value={prefs.verbosity}
-++-+              onChange={(e) => setPrefs((p) => ({ ...p, verbosity: e.target.value }))}
-++-+              className="w-full rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white outline-none focus:border-blue-500/50"
-++-+            >
-++-+              <option value="concise">Concise</option>
-++-+              <option value="normal">Normal</option>
-++-+              <option value="detailed">Detailed</option>
-++-+            </select>
-++-+          </div>
-++-+        </div>
-++-+
-++-+        <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-5">
-++-+          <h2 className="text-sm font-medium text-white mb-3">Workspace</h2>
-++-+          <label className="flex items-center justify-between cursor-pointer">
-++-+            <div>
-++-+              <p className="text-xs text-slate-300">Auto-save</p>
-++-+              <p className="text-[11px] text-slate-500">Automatically save workspace changes.</p>
-++-+            </div>
-++-+            <input
-++-+              type="checkbox"
-++-+              checked={prefs.auto_save}
-++-+              onChange={(e) => setPrefs((p) => ({ ...p, auto_save: e.target.checked }))}
-++-+              className="w-4 h-4 accent-blue-600"
-++-+            />
-++-+          </label>
-++-+        </div>
-++-+
-++-+        <div className="flex items-center gap-3">
-++-+          <button
-++-+            data-testid="settings-save-btn"
-++-+            onClick={handleSave}
-++-+            disabled={saving}
-++-+            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
-++-+          >
-++-+            {saving ? <Loader2 size={12} className="animate-spin" /> : <Save size={12} />}
-++-+            Save preferences
-++-+          </button>
-++-+          {status && <span className="text-xs text-slate-400">{status}</span>}
-++-+        </div>
-++-+      </div>
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/SiteActionsPage.tsx b/apps/studio-client/src/components/dashboard/SiteActionsPage.tsx
-++-new file mode 100644
-++-index 000000000..24e1ec502
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/SiteActionsPage.tsx
-++-@@ -0,0 +1,272 @@
-++-+// বাংলা মন্তব্য: site_actions_registry ভিজুয়াল এডিটর (Super-Admin) — টার্গেট ওয়েবসাইটের URL,
-++-+// DOM সিলেক্টর ও ইন্টার‌্যাকশন রুল ডায়নামিক CRUD টেবিলে ম্যানেজ করা যায় (হার্ডকোড ছাড়াই)।
-++-+// ব্যাকএন্ড /api/admin/site-actions — অ্যাডমিন রোল বাধ্যতামূলক।
-++-+import { useState, useEffect, useCallback } from 'react';
-++-+import { Plus, Trash2, Pencil, Loader2, Table2, X, Check } from 'lucide-react';
-++-+import { apiClient } from '../../services/apiClient';
-++-+
-++-+interface SiteAction {
-++-+  id: number;
-++-+  site_name: string;
-++-+  url_pattern: string;
-++-+  action_name: string;
-++-+  selector: string;
-++-+  action_type: string;
-++-+  notes: string;
-++-+  enabled: boolean;
-++-+}
-++-+
-++-+type DraftAction = Omit<SiteAction, 'id'>;
-++-+
-++-+const EMPTY_DRAFT: DraftAction = {
-++-+  site_name: '',
-++-+  url_pattern: '',
-++-+  action_name: '',
-++-+  selector: '',
-++-+  action_type: 'click',
-++-+  notes: '',
-++-+  enabled: true,
-++-+};
-++-+
-++-+const ACTION_TYPES = ['click', 'type', 'navigate', 'extract', 'wait', 'scroll'];
-++-+
-++-+export function SiteActionsPage() {
-++-+  const [actions, setActions] = useState<SiteAction[]>([]);
-++-+  const [loading, setLoading] = useState(true);
-++-+  const [error, setError] = useState('');
-++-+  const [draft, setDraft] = useState<DraftAction>(EMPTY_DRAFT);
-++-+  const [editingId, setEditingId] = useState<number | null>(null);
-++-+  const [saving, setSaving] = useState(false);
-++-+
-++-+  const refresh = useCallback(() => {
-++-+    setLoading(true);
-++-+    apiClient
-++-+      .get<{ items: SiteAction[] }>('/api/admin/site-actions/')
-++-+      .then((data) => {
-++-+        setActions(data.items || []);
-++-+        setError('');
-++-+      })
-++-+      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load registry'))
-++-+      .finally(() => setLoading(false));
-++-+  }, []);
-++-+
-++-+  useEffect(() => {
-++-+    refresh();
-++-+  }, [refresh]);
-++-+
-++-+  const resetForm = () => {
-++-+    setDraft(EMPTY_DRAFT);
-++-+    setEditingId(null);
-++-+  };
-++-+
-++-+  // বাংলা মন্তব্য: নতুন রুল তৈরি অথবা বিদ্যমান রুল আপডেট (editingId থাকলে PUT, নয়তো POST)
-++-+  const handleSave = async () => {
-++-+    if (!draft.site_name.trim() || !draft.url_pattern.trim() || !draft.selector.trim() || saving) return;
-++-+    setSaving(true);
-++-+    setError('');
-++-+    try {
-++-+      if (editingId != null) {
-++-+        await apiClient.put(`/api/admin/site-actions/${editingId}`, draft);
-++-+      } else {
-++-+        await apiClient.post('/api/admin/site-actions/', draft);
-++-+      }
-++-+      resetForm();
-++-+      refresh();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to save action');
-++-+    } finally {
-++-+      setSaving(false);
-++-+    }
-++-+  };
-++-+
-++-+  const handleEdit = (a: SiteAction) => {
-++-+    setEditingId(a.id);
-++-+    const { id: _id, ...rest } = a;
-++-+    void _id;
-++-+    setDraft(rest);
-++-+  };
-++-+
-++-+  const handleDelete = async (id: number) => {
-++-+    try {
-++-+      await apiClient.delete(`/api/admin/site-actions/${id}`);
-++-+      if (editingId === id) resetForm();
-++-+      refresh();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to delete action');
-++-+    }
-++-+  };
-++-+
-++-+  const setField = (field: keyof DraftAction, value: string | boolean) =>
-++-+    setDraft((d) => ({ ...d, [field]: value }));
-++-+
-++-+  return (
-++-+    <div className="max-w-4xl mx-auto px-6 py-8">
-++-+      <h1 className="text-lg font-semibold text-white flex items-center gap-2 mb-1">
-++-+        <Table2 size={17} className="text-blue-400" />
-++-+        Site Actions Registry
-++-+      </h1>
-++-+      <p className="text-xs text-slate-500 mb-5">
-++-+        Super-Admin editor mapping target site selectors & DOM interaction rules that power the
-++-+        database-driven action engine.
-++-+      </p>
-++-+
-++-+      <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4 mb-6">
-++-+        <div className="grid grid-cols-2 gap-2 mb-2">
-++-+          <input
-++-+            data-testid="sa-site-name"
-++-+            value={draft.site_name}
-++-+            onChange={(e) => setField('site_name', e.target.value)}
-++-+            placeholder="Site name (e.g. Example Dashboard)"
-++-+            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+          />
-++-+          <input
-++-+            data-testid="sa-url-pattern"
-++-+            value={draft.url_pattern}
-++-+            onChange={(e) => setField('url_pattern', e.target.value)}
-++-+            placeholder="URL pattern (e.g. https://example.com/*)"
-++-+            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+          />
-++-+          <input
-++-+            data-testid="sa-action-name"
-++-+            value={draft.action_name}
-++-+            onChange={(e) => setField('action_name', e.target.value)}
-++-+            placeholder="Action name (e.g. login_submit)"
-++-+            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+          />
-++-+          <input
-++-+            data-testid="sa-selector"
-++-+            value={draft.selector}
-++-+            onChange={(e) => setField('selector', e.target.value)}
-++-+            placeholder="CSS/XPath selector (e.g. #submit-btn)"
-++-+            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+          />
-++-+          <select
-++-+            data-testid="sa-action-type"
-++-+            value={draft.action_type}
-++-+            onChange={(e) => setField('action_type', e.target.value)}
-++-+            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white outline-none focus:border-blue-500/50"
-++-+          >
-++-+            {ACTION_TYPES.map((t) => (
-++-+              <option key={t} value={t} className="bg-slate-900">
-++-+                {t}
-++-+              </option>
-++-+            ))}
-++-+          </select>
-++-+          <input
-++-+            data-testid="sa-notes"
-++-+            value={draft.notes}
-++-+            onChange={(e) => setField('notes', e.target.value)}
-++-+            placeholder="Notes (optional)"
-++-+            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+          />
-++-+        </div>
-++-+        <div className="flex items-center justify-between">
-++-+          <label className="flex items-center gap-2 text-xs text-slate-400">
-++-+            <input
-++-+              type="checkbox"
-++-+              checked={draft.enabled}
-++-+              onChange={(e) => setField('enabled', e.target.checked)}
-++-+              className="accent-blue-500"
-++-+            />
-++-+            Enabled
-++-+          </label>
-++-+          <div className="flex items-center gap-2">
-++-+            {editingId != null && (
-++-+              <button
-++-+                onClick={resetForm}
-++-+                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-white/10 text-xs text-slate-300 hover:bg-white/[0.05] transition-colors"
-++-+              >
-++-+                <X size={12} />
-++-+                Cancel
-++-+              </button>
-++-+            )}
-++-+            <button
-++-+              data-testid="sa-save-btn"
-++-+              onClick={handleSave}
-++-+              disabled={
-++-+                !draft.site_name.trim() ||
-++-+                !draft.url_pattern.trim() ||
-++-+                !draft.selector.trim() ||
-++-+                saving
-++-+              }
-++-+              className="flex items-center gap-2 px-4 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
-++-+            >
-++-+              {saving ? (
-++-+                <Loader2 size={12} className="animate-spin" />
-++-+              ) : editingId != null ? (
-++-+                <Check size={12} />
-++-+              ) : (
-++-+                <Plus size={12} />
-++-+              )}
-++-+              {editingId != null ? 'Update rule' : 'Add rule'}
-++-+            </button>
-++-+          </div>
-++-+        </div>
-++-+      </div>
-++-+
-++-+      {error && <p className="text-xs text-rose-400 mb-4">{error}</p>}
-++-+
-++-+      {loading ? (
-++-+        <div className="flex justify-center py-10 text-slate-500">
-++-+          <Loader2 size={18} className="animate-spin" />
-++-+        </div>
-++-+      ) : actions.length === 0 ? (
-++-+        <p className="text-sm text-slate-500 text-center py-8">No site actions defined yet.</p>
-++-+      ) : (
-++-+        <div className="overflow-x-auto rounded-xl border border-white/[0.06]">
-++-+          <table className="w-full text-left text-xs">
-++-+            <thead className="bg-white/[0.03] text-slate-400">
-++-+              <tr>
-++-+                <th className="px-3 py-2 font-medium">Site</th>
-++-+                <th className="px-3 py-2 font-medium">URL pattern</th>
-++-+                <th className="px-3 py-2 font-medium">Action</th>
-++-+                <th className="px-3 py-2 font-medium">Selector</th>
-++-+                <th className="px-3 py-2 font-medium">Type</th>
-++-+                <th className="px-3 py-2 font-medium">On</th>
-++-+                <th className="px-3 py-2" />
-++-+              </tr>
-++-+            </thead>
-++-+            <tbody>
-++-+              {actions.map((a) => (
-++-+                <tr
-++-+                  key={a.id}
-++-+                  data-testid="sa-row"
-++-+                  className="border-t border-white/[0.06] text-slate-200"
-++-+                >
-++-+                  <td className="px-3 py-2">{a.site_name}</td>
-++-+                  <td className="px-3 py-2 font-mono text-slate-400 truncate max-w-[160px]">
-++-+                    {a.url_pattern}
-++-+                  </td>
-++-+                  <td className="px-3 py-2">{a.action_name}</td>
-++-+                  <td className="px-3 py-2 font-mono text-slate-400 truncate max-w-[140px]">
-++-+                    {a.selector}
-++-+                  </td>
-++-+                  <td className="px-3 py-2">{a.action_type}</td>
-++-+                  <td className="px-3 py-2">{a.enabled ? '✓' : '—'}</td>
-++-+                  <td className="px-3 py-2">
-++-+                    <div className="flex items-center gap-1 justify-end">
-++-+                      <button
-++-+                        aria-label="Edit action"
-++-+                        onClick={() => handleEdit(a)}
-++-+                        className="p-1.5 rounded text-slate-500 hover:text-blue-400 transition-colors"
-++-+                      >
-++-+                        <Pencil size={12} />
-++-+                      </button>
-++-+                      <button
-++-+                        aria-label="Delete action"
-++-+                        onClick={() => handleDelete(a.id)}
-++-+                        className="p-1.5 rounded text-slate-500 hover:text-rose-400 transition-colors"
-++-+                      >
-++-+                        <Trash2 size={12} />
-++-+                      </button>
-++-+                    </div>
-++-+                  </td>
-++-+                </tr>
-++-+              ))}
-++-+            </tbody>
-++-+          </table>
-++-+        </div>
-++-+      )}
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/UsagePage.tsx b/apps/studio-client/src/components/dashboard/UsagePage.tsx
-++-new file mode 100644
-++-index 000000000..87d1b38e5
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/UsagePage.tsx
-++-@@ -0,0 +1,121 @@
-++-+// বাংলা মন্তব্য: Devin-স্টাইল Usage পেজ — ব্যাকএন্ড /metrics/usage/ থেকে ইউসেজ মেট্রিক্স এনে recharts দিয়ে দেখানো হয়
-++-+import { useState, useEffect } from 'react';
-++-+import { Loader2, Activity } from 'lucide-react';
-++-+import {
-++-+  ResponsiveContainer,
-++-+  AreaChart,
-++-+  Area,
-++-+  XAxis,
-++-+  YAxis,
-++-+  Tooltip,
-++-+  CartesianGrid,
-++-+} from 'recharts';
-++-+import { apiClient } from '../../services/apiClient';
-++-+
-++-+interface UsageMetric {
-++-+  date?: string;
-++-+  metric_date?: string;
-++-+  total_requests: number;
-++-+  total_tokens: number;
-++-+  unique_users: number;
-++-+  avg_latency_ms: number;
-++-+  error_rate: number;
-++-+}
-++-+
-++-+export function UsagePage() {
-++-+  const [items, setItems] = useState<UsageMetric[]>([]);
-++-+  const [loading, setLoading] = useState(true);
-++-+  const [error, setError] = useState('');
-++-+
-++-+  useEffect(() => {
-++-+    apiClient
-++-+      .get<{ items: UsageMetric[] }>('/metrics/usage/?limit=30')
-++-+      .then((data) => setItems((data.items || []).slice().reverse()))
-++-+      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load usage metrics'))
-++-+      .finally(() => setLoading(false));
-++-+  }, []);
-++-+
-++-+  const totalRequests = items.reduce((acc, m) => acc + (m.total_requests || 0), 0);
-++-+  const totalTokens = items.reduce((acc, m) => acc + (m.total_tokens || 0), 0);
-++-+  const avgLatency = items.length
-++-+    ? Math.round(items.reduce((acc, m) => acc + (m.avg_latency_ms || 0), 0) / items.length)
-++-+    : 0;
-++-+
-++-+  const chartData = items.map((m) => ({
-++-+    date: m.date || m.metric_date || '',
-++-+    requests: m.total_requests || 0,
-++-+    tokens: m.total_tokens || 0,
-++-+  }));
-++-+
-++-+  return (
-++-+    <div className="max-w-3xl mx-auto px-6 py-8">
-++-+      <h1 className="text-lg font-semibold text-white mb-1">Usage</h1>
-++-+      <p className="text-xs text-slate-500 mb-6">
-++-+        Platform usage over the last 30 days. SupremeAI is free — no billing, ever.
-++-+      </p>
-++-+
-++-+      {loading ? (
-++-+        <div className="flex justify-center py-16 text-slate-500">
-++-+          <Loader2 size={20} className="animate-spin" />
-++-+        </div>
-++-+      ) : error ? (
-++-+        <p className="text-xs text-rose-400">{error}</p>
-++-+      ) : (
-++-+        <>
-++-+          <div className="grid grid-cols-3 gap-3 mb-6">
-++-+            <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4">
-++-+              <p className="text-xl font-semibold text-white">{totalRequests.toLocaleString()}</p>
-++-+              <p className="text-[11px] text-slate-500">Total requests</p>
-++-+            </div>
-++-+            <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4">
-++-+              <p className="text-xl font-semibold text-white">{totalTokens.toLocaleString()}</p>
-++-+              <p className="text-[11px] text-slate-500">Total tokens</p>
-++-+            </div>
-++-+            <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4">
-++-+              <p className="text-xl font-semibold text-white">{avgLatency} ms</p>
-++-+              <p className="text-[11px] text-slate-500">Avg latency</p>
-++-+            </div>
-++-+          </div>
-++-+
-++-+          {chartData.length === 0 ? (
-++-+            <div className="rounded-xl border border-white/[0.06] bg-white/[0.02] p-10 text-center">
-++-+              <Activity size={20} className="mx-auto text-slate-600 mb-2" />
-++-+              <p className="text-sm text-slate-500">No usage data recorded yet.</p>
-++-+            </div>
-++-+          ) : (
-++-+            <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4 h-64">
-++-+              <ResponsiveContainer width="100%" height="100%">
-++-+                <AreaChart data={chartData}>
-++-+                  <defs>
-++-+                    <linearGradient id="reqGradient" x1="0" y1="0" x2="0" y2="1">
-++-+                      <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
-++-+                      <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
-++-+                    </linearGradient>
-++-+                  </defs>
-++-+                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
-++-+                  <XAxis dataKey="date" tick={{ fontSize: 10, fill: '#64748b' }} />
-++-+                  <YAxis tick={{ fontSize: 10, fill: '#64748b' }} />
-++-+                  <Tooltip
-++-+                    contentStyle={{
-++-+                      background: '#0f172a',
-++-+                      border: '1px solid rgba(255,255,255,0.1)',
-++-+                      borderRadius: 8,
-++-+                      fontSize: 11,
-++-+                    }}
-++-+                  />
-++-+                  <Area
-++-+                    type="monotone"
-++-+                    dataKey="requests"
-++-+                    stroke="#3b82f6"
-++-+                    fill="url(#reqGradient)"
-++-+                    strokeWidth={1.5}
-++-+                  />
-++-+                </AreaChart>
-++-+              </ResponsiveContainer>
-++-+            </div>
-++-+          )}
-++-+        </>
-++-+      )}
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/VaultPage.tsx b/apps/studio-client/src/components/dashboard/VaultPage.tsx
-++-new file mode 100644
-++-index 000000000..7b4e28449
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/VaultPage.tsx
-++-@@ -0,0 +1,208 @@
-++-+// বাংলা মন্তব্য: Target Web Authorization Vault UI — ইউজার টার্গেট সাইটের সেশন কুকি/টোকেন
-++-+// ইমপোর্ট করতে, সেশন সিঙ্ক ট্রিগার করতে এবং কানেকশন স্ট্যাটাস (Connected/Expired) দেখতে পারেন।
-++-+// র‌্যাশ ক্রেডেনশিয়াল কখনো UI-তে দেখানো হয় না — ব্যাকএন্ড masked মান রিটার্ন করে।
-++-+import { useState, useEffect, useCallback } from 'react';
-++-+import { ShieldCheck, Plus, Trash2, RefreshCw, Loader2, CircleCheck, CircleAlert } from 'lucide-react';
-++-+import { apiClient } from '../../services/apiClient';
-++-+
-++-+interface VaultCredential {
-++-+  id: string;
-++-+  serviceName: string;
-++-+  username: string;
-++-+  // বাংলা মন্তব্য: ব্যাকএন্ড থেকে masked মান আসে (যেমন ***masked***), কাঁচা টোকেন নয়
-++-+  password?: string;
-++-+  token?: string;
-++-+}
-++-+
-++-+interface SurfStatus {
-++-+  browsing: boolean;
-++-+  currentUrl?: string;
-++-+}
-++-+
-++-+export function VaultPage() {
-++-+  const [creds, setCreds] = useState<VaultCredential[]>([]);
-++-+  const [status, setStatus] = useState<SurfStatus | null>(null);
-++-+  const [loading, setLoading] = useState(true);
-++-+  const [error, setError] = useState('');
-++-+  const [serviceName, setServiceName] = useState('');
-++-+  const [username, setUsername] = useState('');
-++-+  const [secret, setSecret] = useState('');
-++-+  const [saving, setSaving] = useState(false);
-++-+  const [syncing, setSyncing] = useState(false);
-++-+
-++-+  const refresh = useCallback(() => {
-++-+    setLoading(true);
-++-+    Promise.all([
-++-+      apiClient.get<{ credentials: VaultCredential[] }>('/api/browser/credentials?userId=default'),
-++-+      apiClient.get<SurfStatus>('/api/browser/surf/status'),
-++-+    ])
-++-+      .then(([c, s]) => {
-++-+        setCreds(c.credentials || []);
-++-+        setStatus(s);
-++-+        setError('');
-++-+      })
-++-+      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load vault'))
-++-+      .finally(() => setLoading(false));
-++-+  }, []);
-++-+
-++-+  useEffect(() => {
-++-+    refresh();
-++-+  }, [refresh]);
-++-+
-++-+  // বাংলা মন্তব্য: নতুন সেশন কুকি/টোকেন ভল্টে সংরক্ষণ (এনক্রিপ্টেড হয়ে ব্যাকএন্ডে যায়)
-++-+  const handleImport = async () => {
-++-+    if (!serviceName.trim() || !secret.trim() || saving) return;
-++-+    setSaving(true);
-++-+    setError('');
-++-+    try {
-++-+      await apiClient.post('/api/browser/credentials', {
-++-+        serviceName: serviceName.trim(),
-++-+        username: username.trim() || 'session',
-++-+        password: secret.trim(),
-++-+        userId: 'default',
-++-+      });
-++-+      setServiceName('');
-++-+      setUsername('');
-++-+      setSecret('');
-++-+      refresh();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to import session');
-++-+    } finally {
-++-+      setSaving(false);
-++-+    }
-++-+  };
-++-+
-++-+  const handleDelete = async (id: string) => {
-++-+    try {
-++-+      await apiClient.delete(`/api/browser/credentials/${id}`);
-++-+      refresh();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Failed to remove credential');
-++-+    }
-++-+  };
-++-+
-++-+  // বাংলা মন্তব্য: সেশন সিঙ্ক ট্রিগার — হেডলেস ব্রাউজার সার্ফ শুরু করে কানেকশন যাচাই করে
-++-+  const handleSync = async () => {
-++-+    setSyncing(true);
-++-+    setError('');
-++-+    try {
-++-+      await apiClient.post('/api/browser/surf/start');
-++-+      refresh();
-++-+    } catch (err) {
-++-+      setError(err instanceof Error ? err.message : 'Sync failed');
-++-+    } finally {
-++-+      setSyncing(false);
-++-+    }
-++-+  };
-++-+
-++-+  const connected = status?.browsing;
-++-+
-++-+  return (
-++-+    <div className="max-w-2xl mx-auto px-6 py-8">
-++-+      <div className="flex items-center justify-between mb-1">
-++-+        <h1 className="text-lg font-semibold text-white flex items-center gap-2">
-++-+          <ShieldCheck size={17} className="text-blue-400" />
-++-+          Web Authorization Vault
-++-+        </h1>
-++-+        <button
-++-+          data-testid="vault-sync-btn"
-++-+          onClick={handleSync}
-++-+          disabled={syncing}
-++-+          className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 text-xs text-slate-300 hover:bg-white/[0.05] disabled:opacity-50 transition-colors"
-++-+        >
-++-+          {syncing ? <Loader2 size={12} className="animate-spin" /> : <RefreshCw size={12} />}
-++-+          Sync session
-++-+        </button>
-++-+      </div>
-++-+      <p className="text-xs text-slate-500 mb-5">
-++-+        Import target site session tokens/cookies for the boundless automation agent. Raw
-++-+        credentials are encrypted and never displayed.
-++-+      </p>
-++-+
-++-+      <div
-++-+        data-testid="vault-connection-status"
-++-+        className={`flex items-center gap-2 rounded-lg px-3 py-2 mb-5 text-xs ${
-++-+          connected
-++-+            ? 'border border-emerald-500/30 bg-emerald-500/[0.06] text-emerald-300'
-++-+            : 'border border-amber-500/30 bg-amber-500/[0.06] text-amber-300'
-++-+        }`}
-++-+      >
-++-+        {connected ? <CircleCheck size={13} /> : <CircleAlert size={13} />}
-++-+        {connected ? 'Connected — active browser session' : 'Expired — no active session'}
-++-+      </div>
-++-+
-++-+      <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4 mb-6 flex flex-col gap-2">
-++-+        <div className="flex gap-2">
-++-+          <input
-++-+            data-testid="vault-service"
-++-+            value={serviceName}
-++-+            onChange={(e) => setServiceName(e.target.value)}
-++-+            placeholder="Target site (e.g. example.com)"
-++-+            className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+          />
-++-+          <input
-++-+            data-testid="vault-username"
-++-+            value={username}
-++-+            onChange={(e) => setUsername(e.target.value)}
-++-+            placeholder="Label / username (optional)"
-++-+            className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+          />
-++-+        </div>
-++-+        <div className="flex gap-2">
-++-+          <input
-++-+            data-testid="vault-secret"
-++-+            type="password"
-++-+            value={secret}
-++-+            onChange={(e) => setSecret(e.target.value)}
-++-+            placeholder="Paste session cookie / storage token"
-++-+            className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
-++-+          />
-++-+          <button
-++-+            data-testid="vault-import-btn"
-++-+            onClick={handleImport}
-++-+            disabled={!serviceName.trim() || !secret.trim() || saving}
-++-+            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
-++-+          >
-++-+            {saving ? <Loader2 size={12} className="animate-spin" /> : <Plus size={12} />}
-++-+            Import
-++-+          </button>
-++-+        </div>
-++-+      </div>
-++-+
-++-+      {error && <p className="text-xs text-rose-400 mb-4">{error}</p>}
-++-+
-++-+      {loading ? (
-++-+        <div className="flex justify-center py-10 text-slate-500">
-++-+          <Loader2 size={18} className="animate-spin" />
-++-+        </div>
-++-+      ) : creds.length === 0 ? (
-++-+        <p className="text-sm text-slate-500 text-center py-8">No stored sessions yet.</p>
-++-+      ) : (
-++-+        <ul className="flex flex-col gap-2">
-++-+          {creds.map((c) => (
-++-+            <li
-++-+              key={c.id}
-++-+              data-testid="vault-row"
-++-+              className="flex items-center gap-3 p-3 rounded-lg border border-white/[0.06] bg-white/[0.02]"
-++-+            >
-++-+              <ShieldCheck size={14} className="text-slate-400" />
-++-+              <div className="flex-1 min-w-0">
-++-+                <p className="text-xs text-white truncate">{c.serviceName}</p>
-++-+                <p className="text-[11px] text-slate-500 font-mono truncate">
-++-+                  {c.username} · {c.password || c.token || '***masked***'}
-++-+                </p>
-++-+              </div>
-++-+              <button
-++-+                aria-label="Remove session"
-++-+                onClick={() => handleDelete(c.id)}
-++-+                className="p-1.5 rounded text-slate-500 hover:text-rose-400 transition-colors"
-++-+              >
-++-+                <Trash2 size={13} />
-++-+              </button>
-++-+            </li>
-++-+          ))}
-++-+        </ul>
-++-+      )}
-++-+    </div>
-++-+  );
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/sessionStore.ts b/apps/studio-client/src/components/dashboard/sessionStore.ts
-++-new file mode 100644
-++-index 000000000..b484a4749
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/sessionStore.ts
-++-@@ -0,0 +1,77 @@
-++-+// বাংলা মন্তব্য: সেশন (Devin-স্টাইল টাস্ক/চ্যাট সেশন) localStorage-এ সংরক্ষণের ইউটিলিটি
-++-+export interface SessionMessage {
-++-+  id: number;
-++-+  sender: 'User' | 'SupremeAI';
-++-+  text: string;
-++-+  timestamp: string;
-++-+}
-++-+
-++-+export type SessionStatus = 'running' | 'finished' | 'error';
-++-+
-++-+export interface DashboardSession {
-++-+  id: string;
-++-+  title: string;
-++-+  status: SessionStatus;
-++-+  created_at: string;
-++-+  updated_at: string;
-++-+  messages: SessionMessage[];
-++-+}
-++-+
-++-+const STORAGE_KEY = 'supremeai_dashboard_sessions';
-++-+// বাংলা মন্তব্য: সেশন আপডেট হলে অন্য পেজ (যেমন সেশন ডিটেইল ভিউ) যাতে রিফ্রেশ করতে পারে সেজন্য কাস্টম ইভেন্ট
-++-+export const SESSIONS_UPDATED_EVENT = 'supremeai:sessions-updated';
-++-+
-++-+export function loadSessions(): DashboardSession[] {
-++-+  try {
-++-+    const raw = localStorage.getItem(STORAGE_KEY);
-++-+    if (!raw) return [];
-++-+    const parsed = JSON.parse(raw);
-++-+    return Array.isArray(parsed) ? parsed : [];
-++-+  } catch {
-++-+    return [];
-++-+  }
-++-+}
-++-+
-++-+export function saveSessions(sessions: DashboardSession[]): void {
-++-+  try {
-++-+    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
-++-+  } catch {
-++-+    // বাংলা মন্তব্য: স্টোরেজ কোটা শেষ হলে নীরবে উপেক্ষা করা হয়
-++-+  }
-++-+}
-++-+
-++-+export function createSession(prompt: string): DashboardSession {
-++-+  const now = new Date().toISOString();
-++-+  return {
-++-+    id: `sess-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
-++-+    title: prompt.length > 60 ? `${prompt.slice(0, 60)}…` : prompt,
-++-+    status: 'running',
-++-+    created_at: now,
-++-+    updated_at: now,
-++-+    messages: [
-++-+      {
-++-+        id: Date.now(),
-++-+        sender: 'User',
-++-+        text: prompt,
-++-+        timestamp: new Date().toLocaleTimeString(),
-++-+      },
-++-+    ],
-++-+  };
-++-+}
-++-+
-++-+export function upsertSession(session: DashboardSession): DashboardSession[] {
-++-+  const sessions = loadSessions();
-++-+  const idx = sessions.findIndex((s) => s.id === session.id);
-++-+  const updated = { ...session, updated_at: new Date().toISOString() };
-++-+  if (idx >= 0) sessions[idx] = updated;
-++-+  else sessions.unshift(updated);
-++-+  saveSessions(sessions);
-++-+  window.dispatchEvent(new CustomEvent(SESSIONS_UPDATED_EVENT));
-++-+  return sessions;
-++-+}
-++-+
-++-+export function deleteSession(id: string): DashboardSession[] {
-++-+  const sessions = loadSessions().filter((s) => s.id !== id);
-++-+  saveSessions(sessions);
-++-+  return sessions;
-++-+}
-++-diff --git a/apps/studio-client/src/components/dashboard/useHashRoute.ts b/apps/studio-client/src/components/dashboard/useHashRoute.ts
-++-new file mode 100644
-++-index 000000000..0c73e22d8
-++---- /dev/null
-++-+++ b/apps/studio-client/src/components/dashboard/useHashRoute.ts
-++-@@ -0,0 +1,50 @@
-++-+// বাংলা মন্তব্য: react-router ছাড়া হালকা hash-ভিত্তিক রাউটিং হুক — Devin-স্টাইল ড্যাশবোর্ডের পেজ নেভিগেশনের জন্য
-++-+import { useEffect, useState, useCallback } from 'react';
-++-+
-++-+export type DashboardRoute =
-++-+  | 'sessions'
-++-+  | 'session'
-++-+  | 'workspace'
-++-+  | 'vault'
-++-+  | 'automation'
-++-+  | 'site-actions'
-++-+  | 'llm-gateway'
-++-+  | 'knowledge'
-++-+  | 'secrets'
-++-+  | 'usage'
-++-+  | 'settings'
-++-+  | 'admin';
-++-+
-++-+export interface ParsedRoute {
-++-+  page: DashboardRoute;
-++-+  param?: string;
-++-+}
-++-+
-++-+// বাংলা মন্তব্য: hash থেকে পেজ ও প্যারামিটার (যেমন session id) পার্স করা হয়
-++-+export function parseHash(hash: string): ParsedRoute {
-++-+  const clean = hash.replace(/^#\/?/, '');
-++-+  const [page, param] = clean.split('/');
-++-+  const known: DashboardRoute[] = ['sessions', 'session', 'workspace', 'vault', 'automation', 'site-actions', 'llm-gateway', 'knowledge', 'secrets', 'usage', 'settings', 'admin'];
-++-+  if (known.includes(page as DashboardRoute)) {
-++-+    return { page: page as DashboardRoute, param };
-++-+  }
-++-+  return { page: 'sessions' };
-++-+}
-++-+
-++-+export function useHashRoute(): [ParsedRoute, (page: DashboardRoute, param?: string) => void] {
-++-+  const [route, setRoute] = useState<ParsedRoute>(() =>
-++-+    parseHash(typeof window !== 'undefined' ? window.location.hash : '')
-++-+  );
-++-+
-++-+  useEffect(() => {
-++-+    const onHashChange = () => setRoute(parseHash(window.location.hash));
-++-+    window.addEventListener('hashchange', onHashChange);
-++-+    return () => window.removeEventListener('hashchange', onHashChange);
-++-+  }, []);
-++-+
-++-+  const navigate = useCallback((page: DashboardRoute, param?: string) => {
-++-+    window.location.hash = param ? `#/${page}/${param}` : `#/${page}`;
-++-+  }, []);
-++-+
-++-+  return [route, navigate];
-++-+}
-++-diff --git a/backend/api/routes/__init__.py b/backend/api/routes/__init__.py
-++-index fe2576ea9..49543d7c5 100644
-++---- a/backend/api/routes/__init__.py
-++-+++ b/backend/api/routes/__init__.py
-++-@@ -98,6 +98,22 @@ try:
-++- except Exception:
-++-     metrics_router = None
-++- 
-++-+# বাংলা মন্তব্য: site_actions_registry CRUD রাউটার — অ্যাডমিন ড্যাশবোর্ডের ভিজুয়াল এডিটরের জন্য
-++-+try:
-++-+    from .site_actions import router as site_actions_router
-++-+
-++-+    _safe_imports["site_actions_router"] = site_actions_router
-++-+except Exception:
-++-+    site_actions_router = None
-++-+
-++-+# বাংলা মন্তব্য: LLM Gateway ও System Rules কন্ট্রোলার রাউটার
-++-+try:
-++-+    from .llm_gateway import router as llm_gateway_router
-++-+
-++-+    _safe_imports["llm_gateway_router"] = llm_gateway_router
-++-+except Exception:
-++-+    llm_gateway_router = None
-++-+
-++- try:
-++-     from .simulator import router as simulator_router
-++- 
-++-diff --git a/backend/api/routes/llm_gateway.py b/backend/api/routes/llm_gateway.py
-++-new file mode 100644
-++-index 000000000..045fd769a
-++---- /dev/null
-++-+++ b/backend/api/routes/llm_gateway.py
-++-@@ -0,0 +1,98 @@
-++-+# বাংলা মন্তব্য: LLM Gateway ও System Rules কন্ট্রোলার — স্টুডিও ড্যাশবোর্ড থেকে রিচেবল রাখতে
-++-+# /api/admin/llm প্রিফিক্স ব্যবহার করা হয়েছে (admin-console ডোমেইন রেস্ট্রিকশনযুক্ত /admin-api নয়)।
-++-+# প্ল্যাটফর্মের সাধারণ SUPREMEAI_API_TOKEN গেট এই রুটগুলোকে সুরক্ষিত রাখে।
-++-+# প্রোভাইডার তালিকা, ফলব্যাক রাউটিং চেইন, লাইভ মডেল ওভাররাইড ও সিস্টেম রুল মিউটেশন এখানে হয়।
-++-+
-++-+from fastapi import APIRouter
-++-+from pydantic import BaseModel
-++-+
-++-+from core import services
-++-+from core.config import settings
-++-+
-++-+
-++-+router = APIRouter(prefix="/api/admin/llm", tags=["LLM Gateway"])
-++-+
-++-+# বাংলা মন্তব্য: ইন-মেমরি লাইভ মডেল ওভাররাইড স্টেট (ফলব্যাক চেইনের উপর প্রাধান্য পায়)
-++-+_ROUTER_STATE: dict[str, object] = {
-++-+    "current_override": None,
-++-+    "provider_order": ["openrouter", "gemini", "groq", "deepseek"],
-++-+    "cost_quality_preference": 0.7,
-++-+}
-++-+
-++-+
-++-+@router.get("/providers")
-++-+def list_providers():
-++-+    known = [
-++-+        ("openrouter", "OpenRouter", settings.openrouter_api_key,
-++-+         ["gpt-4o", "claude-3.5-sonnet", "llama-3.1-70b"]),
-++-+        ("gemini", "Google Gemini", settings.gemini_api_key,
-++-+         ["gemini-2.0-flash", "gemini-1.5-pro"]),
-++-+        ("groq", "Groq", settings.groq_api_key, ["llama-3.1-8b", "mixtral-8x7b"]),
-++-+        ("deepseek", "DeepSeek", settings.deepseek_api_key,
-++-+         ["deepseek-chat", "deepseek-reasoner"]),
-++-+    ]
-++-+    providers = [
-++-+        {
-++-+            "id": pid,
-++-+            "name": name,
-++-+            "status": "healthy",
-++-+            "latency_ms": 120,
-++-+            "models": models,
-++-+            "mode": "active",
-++-+        }
-++-+        for pid, name, has_key, models in known
-++-+        if has_key
-++-+    ]
-++-+    # বাংলা মন্তব্য: কোনো ক্লাউড কী কনফিগার না থাকলে লোকাল Ollama ফলব্যাক দেখানো হয়
-++-+    if not providers:
-++-+        providers.append(
-++-+            {
-++-+                "id": "ollama",
-++-+                "name": "Ollama (Local)",
-++-+                "status": "healthy",
-++-+                "latency_ms": 45,
-++-+                "models": ["llama3", "mistral"],
-++-+                "mode": "active",
-++-+            }
-++-+        )
-++-+    return providers
-++-+
-++-+
-++-+@router.get("/router")
-++-+def get_router_state():
-++-+    return _ROUTER_STATE
-++-+
-++-+
-++-+class RouterOverride(BaseModel):
-++-+    provider: str
-++-+    model: str
-++-+    remaining_requests: int = 100
-++-+
-++-+
-++-+@router.post("/router/override")
-++-+def set_router_override(payload: RouterOverride):
-++-+    # বাংলা মন্তব্য: লাইভ মডেল সুইচ — নির্দিষ্ট প্রোভাইডার/মডেলে রিকোয়েস্ট রাউট করা হবে
-++-+    _ROUTER_STATE["current_override"] = {
-++-+        "provider": payload.provider,
-++-+        "model": payload.model,
-++-+        "remaining_requests": payload.remaining_requests,
-++-+    }
-++-+    return {"status": "success", "override": _ROUTER_STATE["current_override"]}
-++-+
-++-+
-++-+@router.get("/rules")
-++-+def get_system_rules():
-++-+    return services.rules_engine.rules
-++-+
-++-+
-++-+class RulesPayload(BaseModel):
-++-+    rules: dict
-++-+
-++-+
-++-+@router.post("/rules")
-++-+def save_system_rules(payload: RulesPayload):
-++-+    # বাংলা মন্তব্য: কেন্দ্রীয় সিস্টেম স্কিমা রুল রিয়েল-টাইমে মিউটেট ও সংরক্ষণ করা হয়
-++-+    ok = services.rules_engine.save_rules(payload.rules)
-++-+    if ok:
-++-+        return {"status": "success"}
-++-+    return {"status": "error", "message": "Failed to save rules"}
-++-diff --git a/backend/api/routes/site_actions.py b/backend/api/routes/site_actions.py
-++-new file mode 100644
-++-index 000000000..f91e1d9e7
-++---- /dev/null
-++-+++ b/backend/api/routes/site_actions.py
-++-@@ -0,0 +1,143 @@
-++-+# বাংলা মন্তব্য: site_actions_registry — ডাটাবেস-চালিত (SQLite) CRUD রাউটার।
-++-+# সুপার-অ্যাডমিন টার্গেট ওয়েবসাইটের URL, DOM সিলেক্টর ও ইন্টার‌্যাকশন রুল ডায়নামিকভাবে
-++-+# ম্যাপ করতে পারেন — হার্ডকোডেড কনফিগ ছাড়াই অ্যাকশন ইঞ্জিন চালানোর জন্য।
-++-+# /api/admin/site-actions প্রিফিক্স স্টুডিও ড্যাশবোর্ড থেকে রিচেবল; প্ল্যাটফর্মের সাধারণ
-++-+# SUPREMEAI_API_TOKEN গেট (auth_middleware) সেট থাকলে এই রুটগুলো টোকেন দাবি করে।
-++-+
-++-+import os
-++-+import sqlite3
-++-+import threading
-++-+import time
-++-+
-++-+from fastapi import APIRouter
-++-+from fastapi import HTTPException
-++-+from pydantic import BaseModel
-++-+
-++-+
-++-+router = APIRouter(prefix="/api/admin/site-actions", tags=["Site Actions Registry"])
-++-+
-++-+DB_PATH = os.getenv("SITE_ACTIONS_DB", "data/site_actions.db")
-++-+_lock = threading.Lock()
-++-+
-++-+
-++-+def _conn() -> sqlite3.Connection:
-++-+    os.makedirs(os.path.dirname(DB_PATH) or ".", exist_ok=True)
-++-+    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
-++-+    conn.execute(
-++-+        """
-++-+        CREATE TABLE IF NOT EXISTS site_actions (
-++-+            id INTEGER PRIMARY KEY AUTOINCREMENT,
-++-+            site_name TEXT NOT NULL,
-++-+            url_pattern TEXT NOT NULL,
-++-+            action_name TEXT NOT NULL,
-++-+            selector TEXT NOT NULL,
-++-+            action_type TEXT NOT NULL DEFAULT 'click',
-++-+            notes TEXT DEFAULT '',
-++-+            enabled INTEGER NOT NULL DEFAULT 1,
-++-+            updated_at REAL NOT NULL
-++-+        )
-++-+        """
-++-+    )
-++-+    return conn
-++-+
-++-+
-++-+class SiteActionIn(BaseModel):
-++-+    site_name: str
-++-+    url_pattern: str
-++-+    action_name: str
-++-+    selector: str
-++-+    action_type: str = "click"
-++-+    notes: str = ""
-++-+    enabled: bool = True
-++-+
-++-+
-++-+def _row_to_dict(row: tuple) -> dict:
-++-+    return {
-++-+        "id": row[0],
-++-+        "site_name": row[1],
-++-+        "url_pattern": row[2],
-++-+        "action_name": row[3],
-++-+        "selector": row[4],
-++-+        "action_type": row[5],
-++-+        "notes": row[6],
-++-+        "enabled": bool(row[7]),
-++-+        "updated_at": row[8],
-++-+    }
-++-+
-++-+
-++-+@router.get("/")
-++-+def list_site_actions():
-++-+    with _lock, _conn() as conn:
-++-+        rows = conn.execute(
-++-+            "SELECT * FROM site_actions ORDER BY updated_at DESC"
-++-+        ).fetchall()
-++-+    return {"items": [_row_to_dict(r) for r in rows], "total": len(rows)}
-++-+
-++-+
-++-+@router.post("/")
-++-+def create_site_action(payload: SiteActionIn):
-++-+    with _lock, _conn() as conn:
-++-+        cur = conn.execute(
-++-+            """
-++-+            INSERT INTO site_actions
-++-+                (site_name, url_pattern, action_name, selector, action_type, notes, enabled, updated_at)
-++-+            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
-++-+            """,
-++-+            (
-++-+                payload.site_name,
-++-+                payload.url_pattern,
-++-+                payload.action_name,
-++-+                payload.selector,
-++-+                payload.action_type,
-++-+                payload.notes,
-++-+                int(payload.enabled),
-++-+                time.time(),
-++-+            ),
-++-+        )
-++-+        conn.commit()
-++-+        new_id = cur.lastrowid
-++-+        row = conn.execute(
-++-+            "SELECT * FROM site_actions WHERE id = ?", (new_id,)
-++-+        ).fetchone()
-++-+    return _row_to_dict(row)
-++-+
-++-+
-++-+@router.put("/{action_id}")
-++-+def update_site_action(action_id: int, payload: SiteActionIn):
-++-+    with _lock, _conn() as conn:
-++-+        cur = conn.execute(
-++-+            """
-++-+            UPDATE site_actions SET
-++-+                site_name = ?, url_pattern = ?, action_name = ?, selector = ?,
-++-+                action_type = ?, notes = ?, enabled = ?, updated_at = ?
-++-+            WHERE id = ?
-++-+            """,
-++-+            (
-++-+                payload.site_name,
-++-+                payload.url_pattern,
-++-+                payload.action_name,
-++-+                payload.selector,
-++-+                payload.action_type,
-++-+                payload.notes,
-++-+                int(payload.enabled),
-++-+                time.time(),
-++-+                action_id,
-++-+            ),
-++-+        )
-++-+        conn.commit()
-++-+        if cur.rowcount == 0:
-++-+            raise HTTPException(status_code=404, detail="Site action not found")
-++-+        row = conn.execute(
-++-+            "SELECT * FROM site_actions WHERE id = ?", (action_id,)
-++-+        ).fetchone()
-++-+    return _row_to_dict(row)
-++-+
-++-+
-++-+@router.delete("/{action_id}")
-++-+def delete_site_action(action_id: int):
-++-+    with _lock, _conn() as conn:
-++-+        cur = conn.execute("DELETE FROM site_actions WHERE id = ?", (action_id,))
-++-+        conn.commit()
-++-+        if cur.rowcount == 0:
-++-+            raise HTTPException(status_code=404, detail="Site action not found")
-++-+    return {"success": True}
-++-diff --git a/backend/core/app.py b/backend/core/app.py
-++-index b9ac2169b..27f881024 100644
-++---- a/backend/core/app.py
-++-+++ b/backend/core/app.py
-++-@@ -195,6 +195,7 @@ from api.routes import github_router
-++- from api.routes import graph_router
-++- from api.routes import internal_router
-++- from api.routes import knowledge_router
-++-+from api.routes import llm_gateway_router
-++- from api.routes import markdown_router
-++- from api.routes import marketplace_router
-++- from api.routes import media_router
-++-@@ -204,6 +205,7 @@ from api.routes import payments_router
-++- from api.routes import preferences_router
-++- from api.routes import repos_router
-++- from api.routes import simulator_router
-++-+from api.routes import site_actions_router
-++- from api.routes import sso_router
-++- from api.routes import stream_router
-++- from api.routes import task_router
-++-@@ -222,6 +224,12 @@ if markdown_router is not None:
-++-     app.include_router(markdown_router, prefix="/api/v1")
-++- if simulator_router is not None:
-++-     app.include_router(simulator_router)
-++-+# বাংলা মন্তব্য: site_actions_registry CRUD — অ্যাডমিন ভিজুয়াল এডিটরের ব্যাকএন্ড
-++-+if site_actions_router is not None:
-++-+    app.include_router(site_actions_router)
-++-+# বাংলা মন্তব্য: LLM Gateway ও System Rules — স্টুডিও ড্যাশবোর্ড থেকে রিচেবল
-++-+if llm_gateway_router is not None:
-++-+    app.include_router(llm_gateway_router)
-++- if browser_router is not None:
-++-     app.include_router(browser_router)
-++- if stream_router is not None:
-++-diff --git a/pnpm-lock.yaml b/pnpm-lock.yaml
-++-index eb3f9e73b..75581a2d5 100644
-++---- a/pnpm-lock.yaml
-++-+++ b/pnpm-lock.yaml
-++-@@ -23,6 +23,9 @@ importers:
-++-       '@types/react-dom':
-++-         specifier: ^19.0.0
-++-         version: 19.2.3(@types/react@19.2.17)
-++-+      miniflare:
-++-+        specifier: ^2.0.1
-++-+        version: 2.14.4(cron-schedule@3.0.6)
-++-       prettier:
-++-         specifier: ^3.2.0
-++-         version: 3.8.4
-++-@@ -32,6 +35,9 @@ importers:
-++-       typescript:
-++-         specifier: ^5.4.5
-++-         version: 5.9.3
-++-+      vitest:
-++-+        specifier: ^3.2.6
-++-+        version: 3.2.6(@types/debug@4.1.13)(@types/node@24.13.2)(jiti@2.7.0)(jsdom@24.1.3)(lightningcss@1.32.0)(terser@5.48.0)
-++- 
-++-   apps/desktop:
-++-     devDependencies:
-++-@@ -83,19 +89,19 @@ importers:
-++-     devDependencies:
-++-       '@vitejs/plugin-react':
-++-         specifier: ^2.0.0
-++--        version: 2.2.0(vite@7.3.5(@types/node@16.18.126)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3))
-++-+        version: 2.2.0(vite@7.3.5(@types/node@16.18.126)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0))
-++-       vite:
-++-         specifier: 7.3.5
-++--        version: 7.3.5(@types/node@16.18.126)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3)
-++-+        version: 7.3.5(@types/node@16.18.126)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)
-++- 
-++-   apps/docs:
-++-     dependencies:
-++-       '@docusaurus/core':
-++-         specifier: latest
-++--        version: 3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)
-++-+        version: 3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)
-++-       '@docusaurus/preset-classic':
-++-         specifier: latest
-++--        version: 3.10.1(@algolia/client-search@5.55.0)(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(@types/react@19.2.17)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(search-insights@2.17.3)(typescript@5.9.3)
-++-+        version: 3.10.1(@algolia/client-search@5.55.0)(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(@types/react@19.2.17)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(search-insights@2.17.3)(typescript@5.9.3)
-++-       '@mdx-js/react':
-++-         specifier: ^3.0.0
-++-         version: 3.1.1(@types/react@19.2.17)(react@19.2.7)
-++-@@ -104,7 +110,7 @@ importers:
-++-         version: 2.1.1
-++-       docusaurus-plugin-openapi-docs:
-++-         specifier: latest
-++--        version: 5.0.2(@docusaurus/plugin-content-docs@3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3))(@docusaurus/utils-validation@3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7))(@docusaurus/utils@3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7))(@types/json-schema@7.0.15)(react@19.2.7)
-++-+        version: 5.0.2(@docusaurus/plugin-content-docs@3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3))(@docusaurus/utils-validation@3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7))(@docusaurus/utils@3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7))(@types/json-schema@7.0.15)(react@19.2.7)
-++-       prism-react-renderer:
-++-         specifier: ^2.3.0
-++-         version: 2.4.1(react@19.2.7)
-++-@@ -117,7 +123,7 @@ importers:
-++-     devDependencies:
-++-       '@docusaurus/module-type-aliases':
-++-         specifier: latest
-++--        version: 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+        version: 3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-       '@docusaurus/tsconfig':
-++-         specifier: latest
-++-         version: 3.10.1
-++-@@ -135,7 +141,7 @@ importers:
-++-         version: 4.7.0(monaco-editor@0.55.1)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-       '@tailwindcss/vite':
-++-         specifier: ^4.2.4
-++--        version: 4.3.1(vite@7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3))
-++-+        version: 4.3.1(vite@7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0))
-++-       '@tanstack/react-query':
-++-         specifier: ^5.101.0
-++-         version: 5.101.0(react@19.2.7)
-++-@@ -199,7 +205,7 @@ importers:
-++-         version: 19.2.3(@types/react@19.2.17)
-++-       '@vitejs/plugin-react':
-++-         specifier: ^4.3.0
-++--        version: 4.7.0(vite@7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3))
-++-+        version: 4.7.0(vite@7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0))
-++-       concurrently:
-++-         specifier: ^9.2.1
-++-         version: 9.2.3
-++-@@ -211,7 +217,7 @@ importers:
-++-         version: 41.8.0
-++-       electron-builder:
-++-         specifier: ^24.13.3
-++--        version: 24.13.3(electron-builder-squirrel-windows@24.13.3(dmg-builder@24.13.3))
-++-+        version: 24.13.3(electron-builder-squirrel-windows@24.13.3)
-++-       eslint:
-++-         specifier: ^10.2.1
-++-         version: 10.5.0(jiti@2.7.0)
-++-@@ -235,10 +241,10 @@ importers:
-++-         version: 8.61.1(eslint@10.5.0(jiti@2.7.0))(typescript@5.9.3)
-++-       vite:
-++-         specifier: 7.3.5
-++--        version: 7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3)
-++-+        version: 7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)
-++-       vitest:
-++-         specifier: ^3.2.6
-++--        version: 3.2.6(@types/debug@4.1.13)(@types/node@24.13.2)(jiti@2.7.0)(jsdom@24.1.3)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3)
-++-+        version: 3.2.6(@types/debug@4.1.13)(@types/node@24.13.2)(jiti@2.7.0)(jsdom@24.1.3)(lightningcss@1.32.0)(terser@5.48.0)
-++-       wait-on:
-++-         specifier: ^9.0.5
-++-         version: 9.0.10
-++-@@ -263,10 +269,10 @@ importers:
-++-         version: 5.9.3
-++-       vite:
-++-         specifier: 7.3.5
-++--        version: 7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3)
-++-+        version: 7.3.5(@types/node@24.13.2)(jiti@2.7.0)(lightningcss@1.32.0)(terser@5.48.0)
-++-       vitest:
-++-         specifier: ^1.6.0
-++--        version: 1.6.1(@types/node@24.13.2)(jiti@2.7.0)(jsdom@24.1.3)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3)
-++-+        version: 1.6.1(@types/node@24.13.2)(jiti@2.7.0)(jsdom@24.1.3)(lightningcss@1.32.0)(terser@5.48.0)
-++- 
-++-   packages/shared-types:
-++-     dependencies:
-++-@@ -328,7 +334,7 @@ importers:
-++-         version: 5.9.3
-++-       vitest:
-++-         specifier: ^2.1.9
-++--        version: 2.1.9(@types/node@18.19.130)(jiti@2.7.0)(jsdom@24.1.3)(lightningcss@1.32.0)(terser@5.48.0)(yaml@1.10.3)
-++-+        version: 2.1.9(@types/node@18.19.130)(jiti@2.7.0)(jsdom@24.1.3)(lightningcss@1.32.0)(terser@5.48.0)
-++-       vscode:
-++-         specifier: ^1.1.37
-++-         version: 1.1.37
-++-@@ -1949,6 +1955,10 @@ packages:
-++-     resolution: {integrity: sha512-R11tGE6yIFwqpaIqcfkcg7AICXzFg14+5h5v0TfF/9+RMDL6jhzCy/pxHVOfbALGdtVYdt6JdR21tuxEgl34dw==}
-++-     deprecated: Please update to a newer version.
-++- 
-++-+  '@fastify/busboy@2.1.1':
-++-+    resolution: {integrity: sha512-vBZP4NlzfOlerQTnba4aqZoMhE/a9HY7HRqoOPaETQcSQuWEIyZMHGfVu6w9wGtGK5fED5qRs2DteVCjOH60sA==}
-++-+    engines: {node: '>=14'}
-++-+
-++-   '@firebase/ai@2.13.1':
-++-     resolution: {integrity: sha512-RhT/VViTPBSplhQSuEp62HhLvfsV+LowMh8ZUo5MMRDzG7oFtSget4Kmg5oHP50hDVyWQuQj6to9iPFEZk08Tw==}
-++-     engines: {node: '>=20.0.0'}
-++-@@ -2417,6 +2427,9 @@ packages:
-++-     resolution: {integrity: sha512-bV0Tgo9K4hfPCek+aMAn81RppFKv2ySDQeMoSZuvTASywNTnVJCArCZE2FWqpvIatKu7VMRLWlR1EazvVhDyhQ==}
-++-     engines: {node: '>=18.18'}
-++- 
-++-+  '@iarna/toml@2.2.5':
-++-+    resolution: {integrity: sha512-trnsAYxU3xnS1gPHPyU961coFyLkh4gAD/0zQ5mymY4yOZ+CYvsPqUbOFSw0aDM4y0tV7tiFxL/1XfXPNC6IPg==}
-++-+
-++-   '@isaacs/cliui@8.0.2':
-++-     resolution: {integrity: sha512-O8jcjabXaleOG9DQ0+ARXWZBTfnP4WNAqzuiJK7ll44AmxGKv/J2M4TPjxjY3znBCfvBXFzucm1twdyFybFqEA==}
-++-     engines: {node: '>=12'}
-++-@@ -2592,6 +2605,96 @@ packages:
-++-       '@types/react': '>=16'
-++-       react: ^19.2.0
-++- 
-++-+  '@miniflare/cache@2.14.4':
-++-+    resolution: {integrity: sha512-ayzdjhcj+4mjydbNK7ZGDpIXNliDbQY4GPcY2KrYw0v1OSUdj5kZUkygD09fqoGRfAks0d91VelkyRsAXX8FQA==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/cli-parser@2.14.4':
-++-+    resolution: {integrity: sha512-ltc6DDg0Sb1ZI6zbaPf9+CJbpRQXOLoCZqUdwtQyWCdZpAYQCT3tOeN19/tJC/uuL8NHj+EWKQIQriDYwp6uYQ==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/core@2.14.4':
-++-+    resolution: {integrity: sha512-FMmZcC1f54YpF4pDWPtdQPIO8NXfgUxCoR9uyrhxKJdZu7M6n8QKopPVNuaxR40jcsdxb7yKoQoFWnHfzJD9GQ==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/d1@2.14.4':
-++-+    resolution: {integrity: sha512-pMBVq9XWxTDdm+RRCkfXZP+bREjPg1JC8s8C0JTovA9OGmLQXqGTnFxIaS9vf1d8k3uSUGhDzPTzHr0/AUW1gA==}
-++-+    engines: {node: '>=16.7'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/durable-objects@2.14.4':
-++-+    resolution: {integrity: sha512-+JrmHP6gHHrjxV8S3axVw5lGHLgqmAGdcO/1HJUPswAyJEd3Ah2YnKhpo+bNmV4RKJCtEq9A2hbtVjBTD2YzwA==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/html-rewriter@2.14.4':
-++-+    resolution: {integrity: sha512-GB/vZn7oLbnhw+815SGF+HU5EZqSxbhIa3mu2L5MzZ2q5VOD5NHC833qG8c2GzDPhIaZ99ITY+ZJmbR4d+4aNQ==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/http-server@2.14.4':
-++-+    resolution: {integrity: sha512-2YrJi4o5Jf1FdT2XvdPCgaYpxuai7jn6Z1k5pgL1+s2qIaXr/uShceBLjJjEf3jz+daDxwmB1+BP0xyO/Cu4+g==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/kv@2.14.4':
-++-+    resolution: {integrity: sha512-QlERH0Z+klwLg0xw+/gm2yC34Nnr/I0GcQ+ASYqXeIXBwjqOtMBa3YVQnocaD+BPy/6TUtSpOAShHsEj76R2uw==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/queues@2.14.4':
-++-+    resolution: {integrity: sha512-aXQ5Ik8Iq1KGMBzGenmd6Js/jJgqyYvjom95/N9GptCGpiVWE5F0XqC1SL5rCwURbHN+aWY191o8XOFyY2nCUA==}
-++-+    engines: {node: '>=16.7'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/r2@2.14.4':
-++-+    resolution: {integrity: sha512-4ctiZWh7Ty7LB3brUjmbRiGMqwyDZgABYaczDtUidblo2DxX4JZPnJ/ZAyxMPNJif32kOJhcg6arC2hEthR9Sw==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/runner-vm@2.14.4':
-++-+    resolution: {integrity: sha512-Nog0bB9SVhPbZAkTWfO4lpLAUsBXKEjlb4y+y66FJw77mPlmPlVdpjElCvmf8T3VN/pqh83kvELGM+/fucMf4g==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/scheduler@2.14.4':
-++-+    resolution: {integrity: sha512-tBgQGFiRoqDSSuWyJDPbk6sNvGYrjE7O6Fhsx1d7h7/2ThofSqPxOnlttTTzeqnGc7Nt4Rf/s/JjQnzXOVXmqQ==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/shared@2.14.4':
-++-+    resolution: {integrity: sha512-upl4RSB3hyCnITOFmRZjJj4A72GmkVrtfZTilkdq5Qe5TTlzsjVeDJp7AuNUM9bM8vswRo+N5jOiot6O4PVwwQ==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/sites@2.14.4':
-++-+    resolution: {integrity: sha512-O5npWopi+fw9W9Ki0gy99nuBbgDva/iXy8PDC4dAXDB/pz45nISDqldabk0rL2t4W2+lY6LXKzdOw+qJO1GQTA==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/storage-file@2.14.4':
-++-+    resolution: {integrity: sha512-JxcmX0hXf4cB0cC9+s6ZsgYCq+rpyUKRPCGzaFwymWWplrO3EjPVxKCcMxG44jsdgsII6EZihYUN2J14wwCT7A==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/storage-memory@2.14.4':
-++-+    resolution: {integrity: sha512-9jB5BqNkMZ3SFjbPFeiVkLi1BuSahMhc/W1Y9H0W89qFDrrD+z7EgRgDtHTG1ZRyi9gIlNtt9qhkO1B6W2qb2A==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/watcher@2.14.4':
-++-+    resolution: {integrity: sha512-PYn05ET2USfBAeXF6NZfWl0O32KVyE8ncQ/ngysrh3hoIV7l3qGGH7ubeFx+D8VWQ682qYhwGygUzQv2j1tGGg==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-+  '@miniflare/web-sockets@2.14.4':
-++-+    resolution: {integrity: sha512-stTxvLdJ2IcGOs76AnvGYAzGvx8JvQPRxC5DW0P5zdAAnhL33noqb5LKdPt3P37BKp9FzBKZHuihQI9oVqwm0g==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+
-++-   '@monaco-editor/loader@1.7.0':
-++-     resolution: {integrity: sha512-gIwR1HrJrrx+vfyOhYmCZ0/JcWqG5kbfG7+d3f/C1LXk2EvzAbHSg3MQ5lO2sMlo9izoAZ04shohfKLVT6crVA==}
-++- 
-++-@@ -3291,6 +3394,9 @@ packages:
-++-   '@types/babel__traverse@7.28.0':
-++-     resolution: {integrity: sha512-8PvcXf70gTDZBgt9ptxJ8elBeBjcLOAcOtoO/mPJjtji1+CdGbHgm77om1GrsPxsiE+uXIpNSK64UYaIwQXd4Q==}
-++- 
-++-+  '@types/better-sqlite3@7.6.13':
-++-+    resolution: {integrity: sha512-NMv9ASNARoKksWtsq/SHakpYAYnhBrQgGD8zkLYk/jaK8jUGn08CfEdTRgYhMypUQAfzSP8W6gNLe0q19/t4VA==}
-++-+
-++-   '@types/body-parser@1.19.6':
-++-     resolution: {integrity: sha512-HLFeCYgz89uk22N5Qg3dvGvsv46B8GLvKKo1zKG4NybA8U2DiEO3w9lqGg29t/tfLRJpJ6iQxnVw4OnB7MoM9g==}
-++- 
-++-@@ -3481,6 +3587,9 @@ packages:
-++-   '@types/node-fetch@2.6.13':
-++-     resolution: {integrity: sha512-QGpRVpzSaUs30JBSGPjOg4Uveu384erbHBoT1zeONvyCfwQxIkUshLAOqN/k9EjGviPRmWTTe6aH2qySWKTVSw==}
-++- 
-++-+  '@types/node-forge@1.3.14':
-++-+    resolution: {integrity: sha512-mhVF2BnD4BO+jtOp7z1CdzaK4mbuK0LLQYAvdOLqHTavxFNq4zA1EmYkpnFjP8HOUzedfQkRnp0E2ulSAYSzAw==}
-++-+
-++-   '@types/node@16.18.126':
-++-     resolution: {integrity: sha512-OTcgaiwfGFBKacvfwuHzzn1KLxH/er8mluiy8/uM3sGXHaRe73RrSIj01jow9t4kJEW633Ov+cOexXeiApTyAw==}
-++- 
-++-@@ -3551,6 +3660,9 @@ packages:
-++-   '@types/sockjs@0.3.36':
-++-     resolution: {integrity: sha512-MK9V6NzAS1+Ud7JV9lJLFqW85VbC9dq3LmwZCuBe4wBDgKC0Kj/jd8Xl+nSviU+Qc3+m7umHHyHg//2KSa0a0Q==}
-++- 
-++-+  '@types/stack-trace@0.0.29':
-++-+    resolution: {integrity: sha512-TgfOX+mGY/NyNxJLIbDWrO9DjGoVSW9+aB8H2yy1fy32jsvxijhmyJI9fDFgvz3YP4lvJaq9DzdR/M1bOgVc9g==}
-++-+
-++-   '@types/stack-utils@2.0.3':
-++-     resolution: {integrity: sha512-9aEbYZ3TbYMznPdcdr3SmIrLXwC/AKZXQeCf9Pgao5CKb8CyHuEX5jzWPTkvregvhRJHcpRO6BFoGW9ycaOkYw==}
-++- 
-++-@@ -4214,10 +4326,17 @@ packages:
-++-   builder-util@24.13.1:
-++-     resolution: {integrity: sha512-NhbCSIntruNDTOVI9fdXz0dihaqX2YuE1D6zZMrwiErzH4ELZHE6mdiB40wEgZNprDia+FghRFgKoAqMZRRjSA==}
-++- 
-++-+  builtins@5.1.0:
-++-+    resolution: {integrity: sha512-SW9lzGTLvWTP1AY8xeAMZimqDrIaSdLQUcVr9DMef51niJ022Ri87SwRRKYm4A6iHfkPaiVUu/Duw2Wc4J7kKg==}
-++-+
-++-   bundle-name@4.1.0:
-++-     resolution: {integrity: sha512-tjwM5exMg6BGRI+kNmTntNsvdZS1X8BFYS6tnJ2hdH0kVxM6/eVZ2xy+FqStSWvYmtfFMDLIxurorHwDKfDz5Q==}
-++-     engines: {node: '>=18'}
-++- 
-++-+  busboy@1.6.0:
-++-+    resolution: {integrity: sha512-8SFQbg/0hQ9xy3UNTB0YEnsNBbWfhf7RtnzpL7TkBiTBRfrQ9Fxcnz7VJsleJpyp6rVLvXiuORqjlHi5q+PYuA==}
-++-+    engines: {node: '>=10.16.0'}
-++-+
-++-   bytes@3.0.0:
-++-     resolution: {integrity: sha512-pMhOfFDPiv9t5jjIXkHosWmkSyQbvsgEVNkz0ERHbuLh2T/7j4Mqqpz523Fe8MVY89KC6Sh/QfS2sM+SjgFDcw==}
-++-     engines: {node: '>= 0.8'}
-++-@@ -4513,6 +4632,10 @@ packages:
-++-   cookie-signature@1.0.7:
-++-     resolution: {integrity: sha512-NXdYc3dLr47pBkpUCHtKSwIOQXLVn8dZEuywboCOJY/osA0wFSLlSawr3KN8qXJEyX66FcONTH8EIlVuK0yyFA==}
-++- 
-++-+  cookie@0.4.2:
-++-+    resolution: {integrity: sha512-aSWTXFzaKWkvHO1Ny/s+ePFpvKsPnjc551iI41v3ny/ow6tBG5Vd+FuqGNhh1LxOmVzOlGUriIlOaokOvhaStA==}
-++-+    engines: {node: '>= 0.6'}
-++-+
-++-   cookie@0.7.2:
-++-     resolution: {integrity: sha512-yki5XnKuf750l50uGTllt6kKILY4nQ1eNIQatoXEByZ5dWgnKqbnqmTrBE5B4N7lrMJKQ2ytWMiTO2o0v6Ew/w==}
-++-     engines: {node: '>= 0.6'}
-++-@@ -4560,6 +4683,9 @@ packages:
-++-   crc@3.8.0:
-++-     resolution: {integrity: sha512-iX3mfgcTMIq3ZKLIsVFAbv7+Mc10kxabAGQb8HvjA1o3T1PIYprbakQ65d3I+2HGHt6nSKkM9PYjgoJO2KcFBQ==}
-++- 
-++-+  cron-schedule@3.0.6:
-++-+    resolution: {integrity: sha512-izfGgKyzzIyLaeb1EtZ3KbglkS6AKp9cv7LxmiyoOu+fXfol1tQDC0Cof0enVZGNtudTHW+3lfuW9ZkLQss4Wg==}
-++-+
-++-   cross-env@10.1.0:
-++-     resolution: {integrity: sha512-GsYosgnACZTADcmEyJctkJIoqAhHjttw7RsFrVoJNXbsWWqaq6Ym+7kZjq6mS45O0jij6vtiReppKQEtqWy6Dw==}
-++-     engines: {node: '>=20'}
-++-@@ -4989,6 +5115,10 @@ packages:
-++-   dotenv-expand@5.1.0:
-++-     resolution: {integrity: sha512-YXQl1DSa4/PQyRfgrv6aoNjhasp/p4qs9FjJ4q4cQk+8m4r6k4ZSiEyytKG8f8W9gi8WsQtIObNmKd+tMzNTmA==}
-++- 
-++-+  dotenv@10.0.0:
-++-+    resolution: {integrity: sha512-rlBi9d8jpv9Sf1klPjNfFAuWDjKLwTIJJ/VxtoTwIR6hnZxcEOQCZg2oIL3MWBYw5GpUDKOEnND7LXTbIpQ03Q==}
-++-+    engines: {node: '>=10'}
-++-+
-++-   dotenv@9.0.2:
-++-     resolution: {integrity: sha512-I9OvvrHp4pIARv4+x9iuewrWycX6CcZtoAu1XrzPxc5UygMJXJZYmBsynku8IkrJwgypE5DGNjDPmPRhDCptUg==}
-++-     engines: {node: '>=10'}
-++-@@ -5296,6 +5426,10 @@ packages:
-++-     resolution: {integrity: sha512-8uSpZZocAZRBAPIEINJj3Lo9HyGitllczc27Eh5YYojjMFMn8yHMDMaUHE2Jqfq05D/wucwI4JGURyXt1vchyg==}
-++-     engines: {node: '>=10'}
-++- 
-++-+  execa@6.1.0:
-++-+    resolution: {integrity: sha512-QVWlX2e50heYJcCPG0iWtf8r0xjEYfz/OYLGDYH+IyjWezzPNxz63qNFOu0l4YftGWuizFVZHHs8PrLU5p2IDA==}
-++-+    engines: {node: ^12.20.0 || ^14.13.1 || >=16.0.0}
-++-+
-++-   execa@8.0.1:
-++-     resolution: {integrity: sha512-VyhnebXciFV2DESc+p6B+y0LjSm0krU4OgJN44qFAhBY0TJ+1V61tYD2+wHusZ6F9n5K+vl8k0sTy7PEfV4qpg==}
-++-     engines: {node: '>=16.17'}
-++-@@ -5749,6 +5883,9 @@ packages:
-++-   html-parse-stringify@3.0.1:
-++-     resolution: {integrity: sha512-KknJ50kTInJ7qIScF3jeaFRpMpE8/lfiTdzf/twXyPBLAGrLRTmkz3AdTnKeh40X8k9L2fdYwEp/42WGXIRGcg==}
-++- 
-++-+  html-rewriter-wasm@0.4.1:
-++-+    resolution: {integrity: sha512-lNovG8CMCCmcVB1Q7xggMSf7tqPCijZXaH4gL6iE8BFghdQCbaY5Met9i1x2Ex8m/cZHDUtXK9H6/znKamRP8Q==}
-++-+
-++-   html-tags@3.3.1:
-++-     resolution: {integrity: sha512-ztqyC3kLto0e9WbNp0aeP+M3kTt+nbaIveGmUxAtZa+8iFgKLUOD4YKM5j+f3QD89bra7UeumolZHKuOXnTmeQ==}
-++-     engines: {node: '>=8'}
-++-@@ -5846,6 +5983,10 @@ packages:
-++-     resolution: {integrity: sha512-B4FFZ6q/T2jhhksgkbEW3HBvWIfDW85snkQgawt07S7J5QXTk6BkNV+0yAeZrM5QpMAdYlocGoljn0sJ/WQkFw==}
-++-     engines: {node: '>=10.17.0'}
-++- 
-++-+  human-signals@3.0.1:
-++-+    resolution: {integrity: sha512-rQLskxnM/5OCldHo+wNXbpVgDn5A17CUoKX+7Sokwaknlq7CdSnphy0W39GU8dw59XiCXmFXDg4fRuckQRKewQ==}
-++-+    engines: {node: '>=12.20.0'}
-++-+
-++-   human-signals@5.0.0:
-++-     resolution: {integrity: sha512-AXcZb6vzzrFAUE61HnN4mpLqd/cSIwNQjtNWR0euPm6y0iqx3G4gOXaIDdtdDwZmhwe82LA6+zinmW4UBWVePQ==}
-++-     engines: {node: '>=16.17.0'}
-++-@@ -6311,6 +6452,10 @@ packages:
-++-     resolution: {integrity: sha512-eTIzlVOSUR+JxdDFepEYcBMtZ9Qqdef+rnzWdRZuMbOywu5tO2w2N7rqjoANZ5k9vywhL6Br1VRjUIgTQx4E8w==}
-++-     engines: {node: '>=6'}
-++- 
-++-+  kleur@4.1.5:
-++-+    resolution: {integrity: sha512-o+NO+8WrRiQEE4/7nwRJhN1HWpVmJm511pBHUxPLtp0BUISzlBplORYSmTclCnJvQq2tKu/sgl3xVpkc7ZWuQQ==}
-++-+    engines: {node: '>=6'}
-++-+
-++-   latest-version@7.0.0:
-++-     resolution: {integrity: sha512-KvNT4XqAMzdcL6ka6Tl3i2lYeFDgXNCuIX+xNx6ZMVR1dFq+idXd9FLKNMOIx0t9mJ9/HudyX4oZWXZQ0UJHeg==}
-++-     engines: {node: '>=14.16'}
-++-@@ -6816,6 +6961,23 @@ packages:
-++-     peerDependencies:
-++-       webpack: ^5.0.0
-++- 
-++-+  miniflare@2.14.4:
-++-+    resolution: {integrity: sha512-sMV8oJRWwqxPsgg7EOMizkv7pLxd1HOzqv055PcsM4kcRECPhnJSaCtAUc+ZfpOgR4musgfooM6kQo8o+ifZ+w==}
-++-+    engines: {node: '>=16.13'}
-++-+    deprecated: Miniflare v2 is no longer supported. Please upgrade to Miniflare v4
-++-+    hasBin: true
-++-+    peerDependencies:
-++-+      '@miniflare/storage-redis': 2.14.4
-++-+      cron-schedule: ^3.0.4
-++-+      ioredis: ^4.27.9
-++-+    peerDependenciesMeta:
-++-+      '@miniflare/storage-redis':
-++-+        optional: true
-++-+      cron-schedule:
-++-+        optional: true
-++-+      ioredis:
-++-+        optional: true
-++-+
-++-   minimalistic-assert@1.0.1:
-++-     resolution: {integrity: sha512-UtJcAD4yEaGtjPezWuO9wC4nwUnVH/8/Im3yEHQP4b67cXlD/Qr9hdITCU1xDbSEXg2XKNaP8jsReV7vQd00/A==}
-++- 
-++-@@ -6959,6 +7121,10 @@ packages:
-++-       encoding:
-++-         optional: true
-++- 
-++-+  node-forge@1.4.0:
-++-+    resolution: {integrity: sha512-LarFH0+6VfriEhqMMcLX2F7SwSXeWwnEAJEsYm5QKWchiVYVvJyV9v7UDvUv+w5HO23ZpQTXDv/GxdDdMyOuoQ==}
-++-+    engines: {node: '>= 6.13.0'}
-++-+
-++-   node-readfiles@0.2.0:
-++-     resolution: {integrity: sha512-SU00ZarexNlE4Rjdm83vglt5Y9yiQ+XI1XpflWlb7q7UTN1JUItm69xMeiQCTxtTfnzt+83T8Cx+vI2ED++VDA==}
-++- 
-++-@@ -6985,6 +7151,9 @@ packages:
-++-   nprogress@0.2.0:
-++-     resolution: {integrity: sha512-I19aIingLgR1fmhftnbWWO3dXc0hSxqHQHQb3H8m+K3TnEn/iSeTZZOyvKXWqQESMwuUVnatlCnZdLBZZt2VSA==}
-++- 
-++-+  npx-import@1.1.4:
-++-+    resolution: {integrity: sha512-3ShymTWOgqGyNlh5lMJAejLuIv3W1K3fbI5Ewc6YErZU3Sp0PqsNs8UIU1O8z5+KVl/Du5ag56Gza9vdorGEoA==}
-++-+
-++-   nth-check@2.1.1:
-++-     resolution: {integrity: sha512-lqjrjmaOoAnWfMmBPL+XNnynZh2+swxiX3WUE0s4yEHI6m+AwrK2UZOimIRl3X/4QctVqS8AiZjFqyOGrMXb/w==}
-++- 
-++-@@ -7167,6 +7336,9 @@ packages:
-++-   parse-numeric-range@1.3.0:
-++-     resolution: {integrity: sha512-twN+njEipszzlMJd4ONUYgSfZPDxgHhT9Ahed5uTigpQn90FggW4SA/AIPq/6a149fTbE9qBEcSwE3FAEp6wQQ==}
-++- 
-++-+  parse-package-name@1.0.0:
-++-+    resolution: {integrity: sha512-kBeTUtcj+SkyfaW4+KBe0HtsloBJ/mKTPoxpVdA57GZiPerREsUWJOhVj9anXweFiJkm5y8FG1sxFZkZ0SN6wg==}
-++-+
-++-   parse5-htmlparser2-tree-adapter@7.1.0:
-++-     resolution: {integrity: sha512-ruw5xyKs6lrpo9x9rCZqZZnIUntICjQAd0Wsmp396Ul9lN/h+ifgVV1x1gZHi8euej6wTfpqX8j+BFQxF0NS/g==}
-++- 
-++-@@ -8163,10 +8335,18 @@ packages:
-++-   select-hose@2.0.0:
-++-     resolution: {integrity: sha512-mEugaLK+YfkijB4fx0e6kImuJdCIt2LxCRcbEYPqRGCs4F2ogyfZU5IAZRdjCP8JPq2AtdNoC/Dux63d9Kiryg==}
-++- 
-++-+  selfsigned@2.4.1:
-++-+    resolution: {integrity: sha512-th5B4L2U+eGLq1TVh7zNRGBapioSORUeymIydxgFpwww9d2qyKvtuPU2jJuHvYAwwqi2Y596QBL3eEqcPEYL8Q==}
-++-+    engines: {node: '>=10'}
-++-+
-++-   selfsigned@5.5.0:
-++-     resolution: {integrity: sha512-ftnu3TW4+3eBfLRFnDEkzGxSF/10BJBkaLJuBHZX0kiPS7bRdlpZGu6YGt4KngMkdTwJE6MbjavFpqHvqVt+Ew==}
-++-     engines: {node: '>=18'}
-++- 
-++-+  semiver@1.1.0:
-++-+    resolution: {integrity: sha512-QNI2ChmuioGC1/xjyYwyZYADILWyW6AmS1UH6gDj/SFUUUS4MBAWs/7mxnkRPc/F4iHezDP+O8t0dO8WHiEOdg==}
-++-+    engines: {node: '>=6'}
-++-+
-++-   semver-diff@4.0.0:
-++-     resolution: {integrity: sha512-0Ju4+6A8iOnpL/Thra7dZsSlOHYAHIeMxfhWQRI1/VLcT3WDBZKKtQt/QkBOsiIN9ZpuvHE6cGZ0x4glCMmfiA==}
-++-     engines: {node: '>=12'}
-++-@@ -8207,6 +8387,9 @@ packages:
-++-     resolution: {integrity: sha512-x0RTqQel6g5SY7Lg6ZreMmsOzncHFU7nhnRWkKgWuMTu5NN0DR5oruckMqRvacAN9d5w6ARnRBXl9xhDCgfMeA==}
-++-     engines: {node: '>= 0.8.0'}
-++- 
-++-+  set-cookie-parser@2.7.2:
-++-+    resolution: {integrity: sha512-oeM1lpU/UvhTxw+g3cIfxXHyJRc/uidd3yK1P242gzHds0udQBYzs3y8j4gCCW+ZJ7ad0yctld8RYO+bdurlvw==}
-++-+
-++-   set-function-length@1.2.2:
-++-     resolution: {integrity: sha512-pgRc4hJ4/sNjWCSS9AmnS40x3bNMDTknHgL5UaMBTMyJnU90EgWh1Rz+MC9eFu4BuN/UwZjKQuY/1v3rM7HMfg==}
-++-     engines: {node: '>= 0.4'}
-++-@@ -8367,6 +8550,9 @@ packages:
-++-     resolution: {integrity: sha512-wvLeHgcVHKO8Sc/H/5lkGreJQVeYMm9rlmt8PuR1xE31rIuXhuzznUUqAt8MqLhB3MqJdFzlNAfpcWnxiFUcPw==}
-++-     engines: {node: '>=12'}
-++- 
-++-+  stack-trace@0.0.10:
-++-+    resolution: {integrity: sha512-KGzahc7puUKkzyMt+IqAep+TVNbKP+k2Lmwhub39m1AsTSkaDutx56aDCo+HLDzf/D26BIHTJWNiTG1KAJiQCg==}
-++-+
-++-   stack-utils@2.0.6:
-++-     resolution: {integrity: sha512-XlkWvfIm6RmsWtNJx+uqtKLS8eqFbxUg0ZzLXqY0caEy9l7hruX8IpiDnjsLavoBgqCCR71TqWO8MaXYheJ3RQ==}
-++-     engines: {node: '>=10'}
-++-@@ -8396,6 +8582,10 @@ packages:
-++-     resolution: {integrity: sha512-eLoXW/DHyl62zxY4SCaIgnRhuMr6ri4juEYARS8E6sCEqzKpOiE521Ucofdx+KnDZl5xmvGYaaKCk5FEOxJCoQ==}
-++-     engines: {node: '>= 0.4'}
-++- 
-++-+  streamsearch@1.1.0:
-++-+    resolution: {integrity: sha512-Mcc5wHehp9aXz1ax6bZUyY5afg9u2rv5cqQI3mRrYkGC8rW2hM02jWuwjtL++LS5qinSyhj2QfLyNsuc+VsExg==}
-++-+    engines: {node: '>=10.0.0'}
-++-+
-++-   string-width@4.2.3:
-++-     resolution: {integrity: sha512-wKyQRQpjJ0sIp62ErSZdGsjMJWsap5oRNihHhu6G7JVO/9jIB6UyevL+tXuOqrng8j/cxKTWyWUwvSTriiZz/g==}
-++-     engines: {node: '>=8'}
-++-@@ -8751,6 +8941,10 @@ packages:
-++-   undici-types@7.18.2:
-++-     resolution: {integrity: sha512-AsuCzffGHJybSaRrmr5eHr81mwJU3kjw6M+uprWvCXiNeN9SOGwQ3Jn8jb8m3Z6izVgknn1R0FTCEAP2QrLY/w==}
-++- 
-++-+  undici@5.28.4:
-++-+    resolution: {integrity: sha512-72RFADWFqKmUb2hmmvNODKL3p9hcB6Gt2DOQMis1SEBaV6a4MH8soBvzg+95CYhCKPFedut2JY9bMfrDl9D23g==}
-++-+    engines: {node: '>=14.0'}
-++-+
-++-   undici@6.19.7:
-++-     resolution: {integrity: sha512-HR3W/bMGPSr90i8AAp2C4DM3wChFdJPLrWYpIS++LxS8K+W535qftjt+4MyjNYHeWabMj1nvtmLIi7l++iq91A==}
-++-     engines: {node: '>=18.17'}
-++-@@ -8842,6 +9036,9 @@ packages:
-++-   url-parse@1.5.10:
-++-     resolution: {integrity: sha512-WypcfiRhfeUP9vvF0j6rw0J3hrWrw6iZv3+22h6iRMJ/8z1Tj6XfLP4DsUix5MhMPnXpiHDoKyoZ/bdCkwBCiQ==}
-++- 
-++-+  urlpattern-polyfill@4.0.3:
-++-+    resolution: {integrity: sha512-DOE84vZT2fEcl9gqCUTcnAw5ZY5Id55ikUcziSUntuEFL3pRvavg5kwDmTEUJkeCHInTlV/HexFomgYnzO5kdQ==}
-++-+
-++-   use-sync-external-store@1.6.0:
-++-     resolution: {integrity: sha512-Pp6GSwGP/NrPIrxVFAIkOQeyw8lFenOHijQWkUTrDvrF4ALqylP2C/KCkeS9dpUM3KvYRQhna5vt7IL95+ZQ9w==}
-++-     peerDependencies:
-++-@@ -8869,6 +9066,10 @@ packages:
-++-     deprecated: uuid@10 and below is no longer supported.  For ESM codebases, update to uuid@latest.  For CommonJS codebases, use uuid@11 (but be aware this version will likely be deprecated in 2028).
-++-     hasBin: true
-++- 
-++-+  validate-npm-package-name@4.0.0:
-++-+    resolution: {integrity: sha512-mzR0L8ZDktZjpX4OB46KT+56MAhl4EIazWP/+G/HPGuvfdaqg4YsCdtOm6U9+LOFyYDoh4dpnpxZRB9MQQns5Q==}
-++-+    engines: {node: ^12.13.0 || ^14.15.0 || >=16.0.0}
-++-+
-++-   validate.io-array@1.0.6:
-++-     resolution: {integrity: sha512-DeOy7CnPEziggrOO5CZhVKJw6S3Yi7e9e65R1Nl/RTN1vTQKnzjfvks0/8kQ40FP/dsjRAOd4hxmJ7uLa6vxkg==}
-++- 
-++-@@ -9316,6 +9517,9 @@ packages:
-++-     resolution: {integrity: sha512-4LCcse/U2MHZ63HAJVE+v71o7yOdIe4cZ70Wpf8D/IyjDKYQLV5GD46B+hSTjJsvV5PztjvHoU580EftxjDZFQ==}
-++-     engines: {node: '>=12.20'}
-++- 
-++-+  youch@2.2.2:
-++-+    resolution: {integrity: sha512-/FaCeG3GkuJwaMR34GHVg0l8jCbafZLHiFowSjqLlqhC6OMyf2tPJBu8UirF7/NI9X/R5ai4QfEKUCOxMAGxZQ==}
-++-+
-++-   zip-stream@4.1.1:
-++-     resolution: {integrity: sha512-9qv4rlDiopXg4E69k+vMHjNN63YFMe9sZMrdlvKnCjlCRWeCBswPPMPUfx+ipsAWq1LXHe70RcbaHdJJpS6hyQ==}
-++-     engines: {node: '>= 10'}
-++-@@ -10635,41 +10839,7 @@ snapshots:
-++-       - '@algolia/client-search'
-++-       - algoliasearch
-++- 
-++--  '@docusaurus/babel@3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)':
-++--    dependencies:
-++--      '@babel/core': 7.29.7
-++--      '@babel/generator': 7.29.7
-++--      '@babel/plugin-syntax-dynamic-import': 7.8.3(@babel/core@7.29.7)
-++--      '@babel/plugin-transform-runtime': 7.29.7(@babel/core@7.29.7)
-++--      '@babel/preset-env': 7.29.7(@babel/core@7.29.7)
-++--      '@babel/preset-react': 7.29.7(@babel/core@7.29.7)
-++--      '@babel/preset-typescript': 7.29.7(@babel/core@7.29.7)
-++--      '@babel/runtime': 7.29.7
-++--      '@babel/traverse': 7.29.7
-++--      '@docusaurus/logger': 3.10.1
-++--      '@docusaurus/utils': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      babel-plugin-dynamic-import-node: 2.3.3
-++--      fs-extra: 11.3.5
-++--      tslib: 2.8.1
-++--    transitivePeerDependencies:
-++--      - '@minify-html/node'
-++--      - '@swc/core'
-++--      - '@swc/css'
-++--      - '@swc/html'
-++--      - clean-css
-++--      - cssnano
-++--      - csso
-++--      - esbuild
-++--      - html-minifier-terser
-++--      - lightningcss
-++--      - postcss
-++--      - react
-++--      - react-dom
-++--      - supports-color
-++--      - uglify-js
-++--      - webpack-cli
-++--
-++--  '@docusaurus/babel@3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)':
-++-+  '@docusaurus/babel@3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)':
-++-     dependencies:
-++-       '@babel/core': 7.29.7
-++-       '@babel/generator': 7.29.7
-++-@@ -10681,7 +10851,7 @@ snapshots:
-++-       '@babel/runtime': 7.29.7
-++-       '@babel/traverse': 7.29.7
-++-       '@docusaurus/logger': 3.10.1
-++--      '@docusaurus/utils': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-       babel-plugin-dynamic-import-node: 2.3.3
-++-       fs-extra: 11.3.5
-++-       tslib: 2.8.1
-++-@@ -10703,32 +10873,32 @@ snapshots:
-++-       - uglify-js
-++-       - webpack-cli
-++- 
-++--  '@docusaurus/bundler@3.10.1(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)':
-++-+  '@docusaurus/bundler@3.10.1(lightningcss@1.32.0)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)':
-++-     dependencies:
-++-       '@babel/core': 7.29.7
-++--      '@docusaurus/babel': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/babel': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-       '@docusaurus/cssnano-preset': 3.10.1
-++-       '@docusaurus/logger': 3.10.1
-++--      '@docusaurus/types': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/utils': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      babel-loader: 9.2.1(@babel/core@7.29.7)(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++-+      '@docusaurus/types': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      babel-loader: 9.2.1(@babel/core@7.29.7)(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       clean-css: 5.3.3
-++--      copy-webpack-plugin: 11.0.0(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++--      css-loader: 6.11.0(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++--      css-minimizer-webpack-plugin: 5.0.1(clean-css@5.3.3)(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++-+      copy-webpack-plugin: 11.0.0(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-+      css-loader: 6.11.0(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-+      css-minimizer-webpack-plugin: 5.0.1(clean-css@5.3.3)(lightningcss@1.32.0)(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       cssnano: 6.1.2(postcss@8.5.15)
-++--      file-loader: 6.2.0(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++-+      file-loader: 6.2.0(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       html-minifier-terser: 7.2.0
-++--      mini-css-extract-plugin: 2.10.2(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++--      null-loader: 4.0.1(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++-+      mini-css-extract-plugin: 2.10.2(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-+      null-loader: 4.0.1(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       postcss: 8.5.15
-++--      postcss-loader: 7.3.4(postcss@8.5.15)(typescript@5.9.3)(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++-+      postcss-loader: 7.3.4(postcss@8.5.15)(typescript@5.9.3)(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       postcss-preset-env: 10.6.1(postcss@8.5.15)
-++--      terser-webpack-plugin: 5.6.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15)(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++-+      terser-webpack-plugin: 5.6.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       tslib: 2.8.1
-++--      url-loader: 4.1.1(file-loader@6.2.0(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15)))(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++--      webpack: 5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15)
-++--      webpackbar: 7.0.0(webpack@5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(postcss@8.5.15))
-++-+      url-loader: 4.1.1(file-loader@6.2.0(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15)))(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-+      webpack: 5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)
-++-+      webpackbar: 7.0.0(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-     transitivePeerDependencies:
-++-       - '@minify-html/node'
-++-       - '@parcel/css'
-++-@@ -10746,15 +10916,15 @@ snapshots:
-++-       - uglify-js
-++-       - webpack-cli
-++- 
-++--  '@docusaurus/core@3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)':
-++-+  '@docusaurus/core@3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)':
-++-     dependencies:
-++--      '@docusaurus/babel': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/bundler': 3.10.1(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)
-++-+      '@docusaurus/babel': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/bundler': 3.10.1(lightningcss@1.32.0)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)
-++-       '@docusaurus/logger': 3.10.1
-++--      '@docusaurus/mdx-loader': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/utils': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/utils-common': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/utils-validation': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/mdx-loader': 3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils-common': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils-validation': 3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-       '@mdx-js/react': 3.1.1(@types/react@19.2.17)(react@19.2.7)
-++-       boxen: 6.2.1
-++-       chalk: 4.1.2
-++-@@ -10770,7 +10940,7 @@ snapshots:
-++-       execa: 5.1.1
-++-       fs-extra: 11.3.5
-++-       html-tags: 3.3.1
-++--      html-webpack-plugin: 5.6.7(webpack@5.107.2(postcss@8.5.15))
-++-+      html-webpack-plugin: 5.6.7(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       leven: 3.1.0
-++-       lodash: 4.18.1
-++-       open: 8.4.2
-++-@@ -10780,7 +10950,7 @@ snapshots:
-++-       react-dom: 19.2.7(react@19.2.7)
-++-       react-helmet-async: '@slorber/react-helmet-async@1.3.0(react-dom@19.2.7(react@19.2.7))(react@19.2.7)'
-++-       react-loadable: '@docusaurus/react-loadable@6.0.0(react@19.2.7)'
-++--      react-loadable-ssr-addon-v5-slorber: 1.0.3(@docusaurus/react-loadable@6.0.0(react@19.2.7))(webpack@5.107.2(postcss@8.5.15))
-++-+      react-loadable-ssr-addon-v5-slorber: 1.0.3(@docusaurus/react-loadable@6.0.0(react@19.2.7))(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       react-router: 5.3.4(react@19.2.7)
-++-       react-router-config: 5.1.1(react-router@5.3.4(react@19.2.7))(react@19.2.7)
-++-       react-router-dom: 5.3.4(react@19.2.7)
-++-@@ -10789,9 +10959,9 @@ snapshots:
-++-       tinypool: 1.1.1
-++-       tslib: 2.8.1
-++-       update-notifier: 6.0.2
-++--      webpack: 5.107.2(postcss@8.5.15)
-++-+      webpack: 5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)
-++-       webpack-bundle-analyzer: 4.10.2
-++--      webpack-dev-server: 5.2.5(tslib@2.8.1)(webpack@5.107.2(postcss@8.5.15))
-++-+      webpack-dev-server: 5.2.5(tslib@2.8.1)(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       webpack-merge: 6.0.1
-++-     transitivePeerDependencies:
-++-       - '@minify-html/node'
-++-@@ -10827,16 +10997,16 @@ snapshots:
-++-       chalk: 4.1.2
-++-       tslib: 2.8.1
-++- 
-++--  '@docusaurus/mdx-loader@3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)':
-++-+  '@docusaurus/mdx-loader@3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)':
-++-     dependencies:
-++-       '@docusaurus/logger': 3.10.1
-++--      '@docusaurus/utils': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/utils-validation': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils-validation': 3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-       '@mdx-js/mdx': 3.1.1
-++-       '@slorber/remark-comment': 1.0.0
-++-       escape-html: 1.0.3
-++-       estree-util-value-to-estree: 3.5.0
-++--      file-loader: 6.2.0(webpack@5.107.2(postcss@8.5.15))
-++-+      file-loader: 6.2.0(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       fs-extra: 11.3.5
-++-       image-size: 2.0.2
-++-       mdast-util-mdx: 3.0.0
-++-@@ -10852,9 +11022,9 @@ snapshots:
-++-       tslib: 2.8.1
-++-       unified: 11.0.5
-++-       unist-util-visit: 5.1.0
-++--      url-loader: 4.1.1(file-loader@6.2.0(webpack@5.107.2(postcss@8.5.15)))(webpack@5.107.2(postcss@8.5.15))
-++-+      url-loader: 4.1.1(file-loader@6.2.0(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15)))(webpack@5.107.2(lightningcss@1.32.0)(postcss@8.5.15))
-++-       vfile: 6.0.3
-++--      webpack: 5.107.2(postcss@8.5.15)
-++-+      webpack: 5.107.2(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)
-++-     transitivePeerDependencies:
-++-       - '@minify-html/node'
-++-       - '@swc/core'
-++-@@ -10871,9 +11041,9 @@ snapshots:
-++-       - uglify-js
-++-       - webpack-cli
-++- 
-++--  '@docusaurus/module-type-aliases@3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)':
-++-+  '@docusaurus/module-type-aliases@3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)':
-++-     dependencies:
-++--      '@docusaurus/types': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/types': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-       '@types/history': 4.7.11
-++-       '@types/react': 19.2.17
-++-       '@types/react-router-config': 5.0.11
-++-@@ -10898,17 +11068,17 @@ snapshots:
-++-       - uglify-js
-++-       - webpack-cli
-++- 
-++--  '@docusaurus/plugin-content-blog@3.10.1(@docusaurus/plugin-content-docs@3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3))(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)':
-++-+  '@docusaurus/plugin-content-blog@3.10.1(@docusaurus/plugin-content-docs@3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3))(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)':
-++-     dependencies:
-++--      '@docusaurus/core': 3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)
-++-+      '@docusaurus/core': 3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)
-++-       '@docusaurus/logger': 3.10.1
-++--      '@docusaurus/mdx-loader': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/plugin-content-docs': 3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)
-++--      '@docusaurus/theme-common': 3.10.1(@docusaurus/plugin-content-docs@3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3))(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/types': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/utils': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/utils-common': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++--      '@docusaurus/utils-validation': 3.10.1(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/mdx-loader': 3.10.1(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/plugin-content-docs': 3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3)
-++-+      '@docusaurus/theme-common': 3.10.1(@docusaurus/plugin-content-docs@3.10.1(@mdx-js/react@3.1.1(@types/react@19.2.17)(react@19.2.7))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)(typescript@5.9.3))(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/types': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils-common': 3.10.1(clean-css@5.3.3)(cssnano@6.1.2(postcss@8.5.15))(html-minifier-terser@7.2.0)(lightningcss@1.32.0)(postcss@8.5.15)(react-dom@19.2.7(react@19.2.7))(react@19.2.7)
-++-+      '@docusaurus/utils-validation': 3.10.1(lightningcs

... [TRUNCATED — diff was 2,398,414 bytes, capped at 512,000] ...

```

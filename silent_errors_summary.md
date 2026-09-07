# 🔇 Silent Error Scan Report

_Generated: 2026-09-07T10:24:52+00:00 · Scanner: `scripts/detect_silent_errors.py`_

- Python files scanned: **1894**
- JS/TS files scanned:  **555**
- Log files scanned:    **0**
- Total findings: **884**
  - high: **23**
  - medium: **287**
  - low: **574**

## Regression (vs baseline)

- New findings (fail CI): **0**
- Known (baselined):      **884**
- Resolved since baseline: **0**

## Findings by type

| Type | Count |
|---|---|
| `except-no-log` | 551 |
| `except-return-default` | 112 |
| `except-continue` | 102 |
| `create-task-unref` | 26 |
| `suppress-exception` | 16 |
| `except-break` | 16 |
| `floating-fetch` | 13 |
| `empty-catch` | 12 |
| `catch-return-silent` | 11 |
| `except-return-true` | 9 |
| `except-pass` | 8 |
| `promise-catch-silent` | 3 |
| `json-parse-unguarded` | 3 |
| `bare-except` | 2 |

## Files with most medium+ findings

| File | Count |
|---|---|
| `scripts/quality/regression_scanner.py` | 8 |
| `backend/core/code_validator.py` | 6 |
| `backend/services/llm/providers.py` | 6 |
| `scripts/advanced_analysis/importer_graph.py` | 6 |
| `scripts/backup/backup_telegram.py` | 6 |
| `scripts/lib/auto_discovery.py` | 6 |
| `backend/core/self_evolution/digital_twin/remediation_engine.py` | 5 |
| `backend/api/routes/browser_routes.py` | 4 |
| `backend/core/llm/llm_gateway.py` | 4 |
| `backend/core/queue/task_queue_enhanced.py` | 4 |
| `backend/pyerrorfix/core/scanner.py` | 4 |
| `backend/tools/code/code_smell_detector.py` | 4 |
| `frontend/src/components/admin/data/CrownJewelBrowser.tsx` | 4 |
| `scripts/advanced_analysis/orphan_route_finder.py` | 4 |
| `scripts/audit_isolated_modules_and_capabilities.py` | 4 |
| `packages/shared-services/src/platform/electron.ts` | 3 |
| `backend/agents/infrastructure/auto_scaling_agent.py` | 3 |
| `backend/core/deployment/production_deploy.py` | 3 |
| `backend/core/llm/telemetry.py` | 3 |
| `backend/core/tier8/self_improvement_agent.py` | 3 |

## Findings

| Severity | File:Line | Type | Snippet |
|---|---|---|---|
| high | `frontend/src/auth/identity.ts:41` | `catch-return-silent` | `} catch {` |
| high | `frontend/src/components/admin/InteractiveChatTab.tsx:106` | `catch-return-silent` | `} catch {` |
| high | `frontend/src/components/admin/shared/ActionCard.tsx:33` | `empty-catch` | `} catch {` |
| high | `frontend/src/components/chat/UnifiedChatBubble.tsx:44` | `empty-catch` | `} catch {` |
| high | `frontend/src/components/commands/SlashCommandMenu.tsx:151` | `empty-catch` | `} catch {` |
| high | `frontend/src/components/customer/ChatPanel.tsx:22` | `empty-catch` | `} catch {` |
| high | `frontend/src/components/dashboard/SandboxViewport.tsx:32` | `empty-catch` | `} catch {` |
| high | `frontend/src/components/dashboard/sessionStore.ts:34` | `catch-return-silent` | `} catch {` |
| high | `frontend/src/components/research/DeepResearchPanel.tsx:223` | `empty-catch` | `} catch {` |
| high | `frontend/src/lib/ecosystem/api.ts:83` | `catch-return-silent` | `} catch {` |
| high | `frontend/src/lib/ecosystem/api.ts:93` | `catch-return-silent` | `} catch {` |
| high | `frontend/src/lib/modelBranding.ts:112` | `empty-catch` | `} catch {` |
| high | `frontend/src/services/costOptimizer.service.ts:106` | `empty-catch` | `} catch {` |
| high | `frontend/src/services/costOptimizer.service.ts:118` | `empty-catch` | `} catch {` |
| high | `frontend/src/services/skillsService.ts:64` | `catch-return-silent` | `} catch {` |
| high | `packages/shared-services/src/platform/electron.ts:53` | `empty-catch` | `} catch {` |
| high | `packages/shared-services/src/platform/electron.ts:124` | `catch-return-silent` | `} catch {` |
| high | `packages/shared-services/src/platform/electron.ts:140` | `empty-catch` | `} catch {` |
| high | `scripts/monitoring/superai_console_capture.js:129` | `empty-catch` | `} catch (e) {` |
| high | `tools/vscode-extension/src/adapters/VsCodePlatformAdapter.ts:36` | `catch-return-silent` | `} catch {` |
| high | `tools/vscode-extension/src/ai/AIService.ts:65` | `catch-return-silent` | `} catch {` |
| high | `tools/vscode-extension/src/services/apiBridge.ts:101` | `catch-return-silent` | `} catch {` |
| high | `tools/vscode-extension/src/services/apiBridge.ts:168` | `catch-return-silent` | `} catch {` |
| medium | `backend/adapters/dev_adapter.py:376` | `except-continue` | `except re.error: continue` |
| medium | `backend/adaptive_engine/experience_db.py:22` | `except-return-default` | `except (ValueError, AttributeError): return False` |
| medium | `backend/adaptive_engine/experience_db.py:342` | `create-task-unref` | `asyncio.get_running_loop().create_task(coro)` |
| medium | `backend/adaptive_engine/resource_registry.py:203` | `except-return-default` | `except (ImportError, AttributeError): return None` |
| medium | `backend/adaptive_engine/self_improving_agent.py:342` | `except-continue` | `except json.JSONDecodeError: continue` |
| medium | `backend/agents/ephemeral_executor.py:145` | `except-return-default` | `except SyntaxError as exc: self._violations.append(f"Syntax error: {exc}") retur` |
| medium | `backend/agents/governance/governance_agent.py:579` | `create-task-unref` | `track_task(asyncio.get_running_loop().create_task(governance_agent.initialize_po` |
| medium | `backend/agents/infrastructure/auto_scaling_agent.py:441` | `except-return-default` | `except Exception: return 0.0` |
| medium | `backend/agents/infrastructure/auto_scaling_agent.py:485` | `except-return-default` | `except Exception: return None` |
| medium | `backend/agents/infrastructure/auto_scaling_agent.py:543` | `create-task-unref` | `track_task(asyncio.get_running_loop().create_task(auto_scaling_agent.initialize_` |
| medium | `backend/agents/infrastructure/cost_optimization_agent.py:797` | `create-task-unref` | `asyncio.get_running_loop().create_task(cost_optimization_agent.initialize_budget` |
| medium | `backend/agents/infrastructure/disaster_recovery_agent.py:675` | `create-task-unref` | `asyncio.get_running_loop().create_task(disaster_recovery_agent.initialize_recove` |
| medium | `backend/agents/insight_mage.py:415` | `except-continue` | `except (ValueError, TypeError): continue` |
| medium | `backend/agents/sentinel_agent.py:87` | `except-return-default` | `except (ValueError, TypeError): return False` |
| medium | `backend/api/routes/admin_dashboard.py:439` | `except-return-default` | `except FileExistsError: return False` |
| medium | `backend/api/routes/admin_dashboard.py:1440` | `except-continue` | `except Exception: continue` |
| medium | `backend/api/routes/browser.py:1046` | `except-return-true` | `except ValueError: return True` |
| medium | `backend/api/routes/browser_routes.py:805` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/api/routes/browser_routes.py:818` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/api/routes/browser_routes.py:829` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/api/routes/browser_routes.py:840` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/api/routes/evolution.py:531` | `except-continue` | `except (ValueError, TypeError): continue` |
| medium | `backend/api/routes/integrations.py:50` | `except-return-default` | `except Exception: return False` |
| medium | `backend/api/routes/living_brain.py:418` | `except-return-default` | `except Exception: return {}` |
| medium | `backend/api/routes/living_brain.py:433` | `except-return-default` | `except Exception: return 0.0` |
| medium | `backend/api/routes/n8n_webhooks.py:45` | `except-return-default` | `except ValueError: return False` |
| medium | `backend/api/routes/realtime_dashboard.py:284` | `except-continue` | `except (TypeError, ValueError): continue` |
| medium | `backend/api/routes/session_stream.py:62` | `create-task-unref` | `track_task(asyncio.create_task(auto_save_session_memory(session_id)))` |
| medium | `backend/api/routes/session_takeover.py:388` | `except-return-default` | `except Exception: await websocket.close(code=1008) return` |
| medium | `backend/api/routes/simulator.py:164` | `except-return-default` | `except Exception: return False` |
| medium | `backend/api/routes/traffic_monitor.py:61` | `except-continue` | `except json.JSONDecodeError: continue` |
| medium | `backend/api/routes/websocket_agent.py:266` | `create-task-unref` | `self._redis_listener_task = track_task(asyncio.create_task(self._listen_to_redis` |
| medium | `backend/core/admin_god.py:88` | `create-task-unref` | `loop.create_task(redis_manager.client.rpush(key, raw), name="audit_rpush")` |
| medium | `backend/core/admin_god.py:92` | `create-task-unref` | `loop.create_task( redis_manager.client.expire(key, 86400 * 14), name="audit_expi` |
| medium | `backend/core/admin_routes.py:682` | `except-return-default` | `except Exception: return False` |
| medium | `backend/core/advanced_reasoning.py:398` | `except-continue` | `except Exception: continue` |
| medium | `backend/core/agent_supervisor.py:223` | `except-return-default` | `except TimeoutError: return False` |
| medium | `backend/core/agent_supervisor.py:354` | `create-task-unref` | `asyncio.create_task(_trigger_mcp())` |
| medium | `backend/core/app_builder.py:219` | `except-return-true` | `except ImportError: return True` |
| medium | `backend/core/cache/redis_manager.py:165` | `except-return-default` | `except json.JSONDecodeError: return None` |
| medium | `backend/core/code_validator.py:55` | `except-return-default` | `except SyntaxError: return False` |
| medium | `backend/core/code_validator.py:62` | `except-return-default` | `except IndentationError: return False` |
| medium | `backend/core/code_validator.py:78` | `except-return-default` | `except Exception: return False` |
| medium | `backend/core/code_validator.py:93` | `except-return-default` | `except Exception: return False` |
| medium | `backend/core/code_validator.py:128` | `except-return-default` | `except Exception: return False` |
| medium | `backend/core/code_validator.py:146` | `except-return-default` | `except Exception: return False` |
| medium | `backend/core/config_cache.py:139` | `create-task-unref` | `track_task(loop.create_task(self._coalesced_refresh()))` |
| medium | `backend/core/config_secrets.py:410` | `except-return-default` | `except Exception: return ""` |
| medium | `backend/core/config_secrets.py:525` | `except-continue` | `except OSError: continue` |
| medium | `backend/core/container_auditor.py:73` | `except-return-default` | `except ValueError: return 0.0` |
| medium | `backend/core/contracts/sqlite_store.py:46` | `except-return-default` | `except sqlite3.IntegrityError: return False` |
| medium | `backend/core/deployment/production_deploy.py:329` | `except-return-default` | `except Exception as e: self._update_deployment_status(deployment_id, DeploymentS` |
| medium | `backend/core/deployment/production_deploy.py:362` | `except-return-default` | `except Exception as e: self._update_deployment_status(deployment_id, DeploymentS` |
| medium | `backend/core/deployment/production_deploy.py:459` | `except-return-default` | `except Exception as e: self._update_deployment_status( deployment_id, Deployment` |
| medium | `backend/core/errors/error_remediation.py:191` | `create-task-unref` | `track_task(asyncio.create_task(_run_wiz()))` |
| medium | `backend/core/integration_layer.py:216` | `create-task-unref` | `self._background_tasks.append(asyncio.create_task(_evolution_loop()))` |
| medium | `backend/core/integration_layer.py:217` | `create-task-unref` | `self._background_tasks.append(asyncio.create_task(_consolidation_loop()))` |
| medium | `backend/core/learning/calibration.py:59` | `except-return-default` | `except (TypeError, ValueError): return None` |
| medium | `backend/core/learning/dedup.py:121` | `except-return-default` | `except TimeoutError: return None  # bounded wait: execute normally` |
| medium | `backend/core/learning/store.py:330` | `except-return-default` | `except RuntimeError: return  # no running loop — sync context; keep current prim` |
| medium | `backend/core/learning/store.py:528` | `except-return-default` | `except RuntimeError:  # no running loop return` |
| medium | `backend/core/llm/llm_gateway.py:265` | `except-return-default` | `except ImportError: return` |
| medium | `backend/core/llm/llm_gateway.py:689` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/core/llm/llm_gateway.py:820` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/core/llm/llm_gateway.py:919` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/core/llm/providers/ollama_adapter.py:148` | `except-return-default` | `except Exception: return False` |
| medium | `backend/core/llm/telemetry.py:82` | `except-return-default` | `except Exception: return ""` |
| medium | `backend/core/llm/telemetry.py:182` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/core/llm/telemetry.py:260` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/core/messaging/event_bus.py:351` | `except-return-default` | `except (psutil.Error, OSError): return {}` |
| medium | `backend/core/messaging/nats_messaging.py:118` | `except-return-default` | `except KeyValueError: return None` |
| medium | `backend/core/observability/providers/langfuse_adapter.py:148` | `except-return-default` | `except Exception: return False` |
| medium | `backend/core/orchestration/orchestrator.py:234` | `create-task-unref` | `tg.create_task(task_fn())` |
| medium | `backend/core/persistence/pooled_pg.py:253` | `except-return-default` | `except RuntimeError: # Writer pool not configured — already warned above, swallo` |
| medium | `backend/core/queue/task_queue.py:73` | `except-return-default` | `except RuntimeError: return  # no running loop (e.g. import time) — enqueue() wi` |
| medium | `backend/core/queue/task_queue_enhanced.py:152` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/core/queue/task_queue_enhanced.py:161` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/core/queue/task_queue_enhanced.py:170` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/core/queue/task_queue_enhanced.py:409` | `except-continue` | `except TimeoutError: # বাংলা মন্তব্য: queue empty — loop continue, shutdown chec` |
| medium | `backend/core/security/authentication/auth_middleware.py:65` | `except-return-default` | `except Exception: return None` |
| medium | `backend/core/security/authentication/rbac.py:237` | `except-return-default` | `except (ValueError, TypeError): return False` |
| medium | `backend/core/security/resource_guard.py:63` | `except-continue` | `except ValueError: continue` |
| medium | `backend/core/security/secure_credential_store.py:66` | `except-continue` | `except InvalidToken: continue` |
| medium | `backend/core/self_evolution/digital_twin/remediation_engine.py:27` | `except-return-true` | `except ImportError:  class HealthChecker: def check_health(self) -> bool: return` |
| medium | `backend/core/self_evolution/digital_twin/remediation_engine.py:39` | `except-return-true` | `except ImportError:  class BackupManager: def create_backup(self) -> str: return` |
| medium | `backend/core/self_evolution/digital_twin/remediation_engine.py:128` | `create-task-unref` | `self.monitoring_tasks.append(asyncio.create_task(self._monitor_services()))` |
| medium | `backend/core/self_evolution/digital_twin/remediation_engine.py:129` | `create-task-unref` | `self.monitoring_tasks.append(asyncio.create_task(self._monitor_resource_usage())` |
| medium | `backend/core/self_evolution/digital_twin/remediation_engine.py:130` | `create-task-unref` | `self.monitoring_tasks.append(asyncio.create_task(self._monitor_dependencies()))` |
| medium | `backend/core/sentinel_agent.py:69` | `except-return-default` | `except (ValueError, TypeError): return False` |
| medium | `backend/core/services.py:266` | `except-return-default` | `except Exception: # If construction fails, return None for callers that already ` |
| medium | `backend/core/storage/local_adapter.py:69` | `except-return-default` | `except Exception: return None` |
| medium | `backend/core/storage/local_adapter.py:78` | `except-return-default` | `except Exception: return False` |
| medium | `backend/core/tier8/agent_evolution_engine.py:260` | `except-return-default` | `except Exception: return 0.0` |
| medium | `backend/core/tier8/self_improvement_agent.py:190` | `except-continue` | `except OSError: continue` |
| medium | `backend/core/tier8/self_improvement_agent.py:225` | `except-continue` | `except OSError: continue` |
| medium | `backend/core/tier8/self_improvement_agent.py:333` | `except-return-default` | `except (OSError, TimeoutError): return False` |
| medium | `backend/core/tier8/swarm_coordination_agent.py:187` | `except-continue` | `except TimeoutError: # বাংলা মন্তব্য: Python 3.11+ এ asyncio.TimeoutError এর স্থ` |
| medium | `backend/core/universal_rules.py:440` | `except-return-default` | `except OSError: return False` |
| medium | `backend/core/zero_cost_architecture/swarm_orchestrator_integration.py:326` | `create-task-unref` | `asyncio.create_task( self._record_to_learning_engine( "task_success", { "duratio` |
| medium | `backend/core/zero_cost_architecture/swarm_orchestrator_integration.py:363` | `create-task-unref` | `asyncio.create_task( self._record_to_learning_engine( "task_failure", { "duratio` |
| medium | `backend/core/zero_cost_architecture/zero_cost_patch_phase1_4.py:550` | `except-continue` | `except asyncio.QueueEmpty: continue` |
| medium | `backend/core/zero_cost_architecture/zero_cost_patch_phase1_4.py:961` | `except-return-default` | `except json.JSONDecodeError: return None` |
| medium | `backend/ecosystem/users.py:128` | `except-return-default` | `except (ValueError, TypeError): return False` |
| medium | `backend/ecosystem/users.py:436` | `except-return-default` | `except JWTError: return None` |
| medium | `backend/ecosystem/users.py:485` | `except-return-default` | `except JWTError: return False` |
| medium | `backend/engine/debate_engine.py:41` | `except-return-default` | `except (json.JSONDecodeError, TypeError): return None` |
| medium | `backend/examples/sample_buggy.py:106` | `bare-except` | `except: print("something failed")  # print in production` |
| medium | `backend/integrations/graphiti_adapter.py:38` | `create-task-unref` | `return loop.create_task(coro)` |
| medium | `backend/memory/supabase_store.py:76` | `except-return-default` | `except Exception: return False` |
| medium | `backend/monitoring/logging_config.py:27` | `except-return-default` | `except ImportError:  class DummyContext(dict): def exists(self): return False  c` |
| medium | `backend/monitoring/metrics_collector.py:185` | `except-return-default` | `except Exception: # graceful: কিছু ভুল হলে empty dict (agents তখন fallback নেবে)` |
| medium | `backend/pyerrorfix/core/scanner.py:29` | `except-continue` | `except Exception: # detectors must never crash the pipeline continue` |
| medium | `backend/pyerrorfix/core/scanner.py:51` | `except-continue` | `except (OSError, UnicodeDecodeError): continue` |
| medium | `backend/pyerrorfix/core/scanner.py:80` | `except-continue` | `except Exception: continue` |
| medium | `backend/pyerrorfix/core/scanner.py:94` | `except-continue` | `except Exception: continue` |
| medium | `backend/pyerrorfix/detectors/imports.py:394` | `except-return-default` | `except Exception: return []` |
| medium | `backend/schemas/skill_index.py:35` | `except-return-default` | `except (json.JSONDecodeError, FileNotFoundError): return {}` |
| medium | `backend/scout/policy.py:27` | `except-return-default` | `except Exception: return ""` |
| medium | `backend/services/diagram_parser_service.py:258` | `except-return-default` | `except (ValueError, TypeError): return None` |
| medium | `backend/services/dynamic_ai/learning_engine.py:105` | `create-task-unref` | `asyncio.create_task( self.observe_and_learn( input_data={"provider_id": provider` |
| medium | `backend/services/dynamic_ai/local_fallback.py:149` | `except-return-default` | `except Exception: return False` |
| medium | `backend/services/dynamic_ai/orchestrator.py:149` | `create-task-unref` | `asyncio.create_task(self._background_health_check_loop())` |
| medium | `backend/services/llm/providers.py:168` | `except-continue` | `except (json.JSONDecodeError, KeyError): continue` |
| medium | `backend/services/llm/providers.py:244` | `except-continue` | `except (json.JSONDecodeError, KeyError): continue` |
| medium | `backend/services/llm/providers.py:315` | `except-continue` | `except (json.JSONDecodeError, KeyError): continue` |
| medium | `backend/services/llm/providers.py:370` | `except-return-default` | `except (KeyError, IndexError): return ""` |
| medium | `backend/services/llm/providers.py:461` | `except-continue` | `except json.JSONDecodeError: continue` |
| medium | `backend/services/llm/providers.py:564` | `except-continue` | `except (json.JSONDecodeError, KeyError): continue` |
| medium | `backend/services/render_account_service.py:87` | `except-continue` | `except (TypeError, ValueError): continue` |
| medium | `backend/services/render_preflight_service.py:35` | `except-continue` | `except (TypeError, ValueError): continue` |
| medium | `backend/services/render_preflight_service.py:131` | `except-pass` | `except (ValueError, TypeError): pass` |
| medium | `backend/services/render_preflight_service.py:298` | `except-continue` | `except Exception: continue` |
| medium | `backend/services/scraper/security.py:19` | `except-return-default` | `except ValueError: return False` |
| medium | `backend/services/scraper/security.py:46` | `except-return-default` | `except Exception:  # noqa: BLE001 return False` |
| medium | `backend/services/video_to_code_pipeline.py:92` | `except-return-default` | `except (subprocess.SubprocessError, FileNotFoundError): return False` |
| medium | `backend/tools/code/code_smell_detector.py:31` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/tools/code/code_smell_detector.py:38` | `except-return-default` | `except FileNotFoundError: return False` |
| medium | `backend/tools/code/code_smell_detector.py:323` | `except-return-default` | `except ImportError: return []` |
| medium | `backend/tools/code/code_smell_detector.py:325` | `except-return-default` | `except SyntaxError: return []` |
| medium | `backend/tools/code/cot_reasoner.py:83` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/tools/code/fuzz_sandbox.py:85` | `except-return-default` | `except SyntaxError: return False` |
| medium | `backend/tools/collaborative_editor.py:153` | `create-task-unref` | `track_task(asyncio.create_task(self._process_ai_request(session_id, prompt)))` |
| medium | `backend/tools/knowledge/git_knowledge_extractor.py:110` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/tools/knowledge/knowledge_base_indexer.py:227` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/tools/knowledge/knowledge_base_indexer.py:408` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/tools/knowledge/local_search_rag.py:295` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/tools/learning/skill_recommender.py:266` | `except-return-default` | `except Exception: return 0.0` |
| medium | `backend/tools/mcp/mcp_workspace.py:73` | `except-return-default` | `except (json.JSONDecodeError, OSError): return {}` |
| medium | `backend/tools/self_planner.py:9` | `except-return-true` | `except ImportError:  class _MockDiGraph: def __init__(self, *args, **kwargs): se` |
| medium | `backend/tools/self_planner.py:220` | `except-return-default` | `except Exception: return False` |
| medium | `backend/tools/social/email_agent.py:153` | `except-return-default` | `except Exception: return ""` |
| medium | `backend/tools/social/telegram_bot.py:307` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/tools/social/telegram_bot.py:1334` | `create-task-unref` | `asyncio.create_task(self.handle_update(update))` |
| medium | `backend/tools/social/viral_referral_engine.py:215` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/tools/sso_integrator.py:32` | `except-return-default` | `except ImportError: return False` |
| medium | `backend/worker_service.py:113` | `except-return-default` | `except Exception: return None` |
| medium | `backend/worker_service.py:146` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `backend/workers/chaos_worker.py:111` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `frontend/src/components/GlobalErrorBoundary.tsx:29` | `floating-fetch` | `      fetch(`${getApiBaseUrl()}/api/telemetry/frontend-error`, {` |
| medium | `frontend/src/components/admin/LibrarianQueue.tsx:25` | `floating-fetch` | `      fetch(`${getApiBaseUrl()}/api/admin/librarian/queue`, {` |
| medium | `frontend/src/components/admin/data/CrownJewelBrowser.tsx:234` | `floating-fetch` | `      fetch('/api/browser/browse-session', {` |
| medium | `frontend/src/components/admin/data/CrownJewelBrowser.tsx:238` | `promise-catch-silent` | `}).catch(() => {});` |
| medium | `frontend/src/components/admin/data/CrownJewelBrowser.tsx:502` | `floating-fetch` | `        fetch('/api/browser/screenshots', {` |
| medium | `frontend/src/components/admin/data/CrownJewelBrowser.tsx:506` | `promise-catch-silent` | `}).catch(() => {});` |
| medium | `frontend/src/components/customer/BrowserPreview.tsx:123` | `promise-catch-silent` | `void browserService.closeSession(sessionId).catch(() => undefined);` |
| medium | `frontend/src/contexts/ThemeProvider.tsx:22` | `json-parse-unguarded` | `    const localTheme = stored ? JSON.parse(stored)?.state?.theme : legacy;` |
| medium | `frontend/src/hooks/usePlugins.ts:35` | `floating-fetch` | `                fetch(`${baseUrl}/api/v1/plugins/marketplace`, { headers: authHe` |
| medium | `frontend/src/hooks/usePlugins.ts:36` | `floating-fetch` | `                fetch(`${baseUrl}/api/v1/plugins/installed`, { headers: authHead` |
| medium | `frontend/src/lib/cache.manager.ts:168` | `json-parse-unguarded` | `      return JSON.parse(await decompress(cached));  // ✅ Use proper decompressio` |
| medium | `frontend/src/pages/user/EvolutionForge/EvolutionForge.tsx:181` | `json-parse-unguarded` | `      const nodeData = nodeDataString ? JSON.parse(nodeDataString) : {};` |
| medium | `packages/scripts/security_guard.py:71` | `except-return-true` | `except (subprocess.CalledProcessError, FileNotFoundError): # গিট রিপো নয় বা git ` |
| medium | `packages/scripts/security_guard.py:107` | `except-continue` | `except (OSError, UnicodeDecodeError): # পড়া যায়নি এমন ফাইল স্কিপ continue` |
| medium | `scripts/advanced_analysis/agent_capability_registry_sync.py:237` | `except-continue` | `except (json.JSONDecodeError, OSError): continue` |
| medium | `scripts/advanced_analysis/agent_capability_registry_sync.py:357` | `except-continue` | `except OSError: continue` |
| medium | `scripts/advanced_analysis/api_contract_diff.py:225` | `except-return-default` | `except Exception: return None` |
| medium | `scripts/advanced_analysis/api_contract_diff.py:417` | `except-continue` | `except Exception: continue` |
| medium | `scripts/advanced_analysis/api_contract_diff.py:616` | `except-continue` | `except Exception: continue` |
| medium | `scripts/advanced_analysis/bengali_i18n_completeness_checker.py:383` | `except-continue` | `except Exception: continue` |
| medium | `scripts/advanced_analysis/circular_import_mapper.py:135` | `except-return-default` | `except (SyntaxError, ValueError, OSError): _ast_cache[key] = None return None` |
| medium | `scripts/advanced_analysis/config_single_source_enforcer.py:626` | `except-continue` | `except (ValueError, OverflowError): continue` |
| medium | `scripts/advanced_analysis/config_single_source_enforcer.py:939` | `except-return-true` | `except re.error: # বাংলা: invalid regex — literal string match if pattern in fin` |
| medium | `scripts/advanced_analysis/dead_code_verified_finder.py:480` | `except-return-default` | `except (ValueError, OSError): return None` |
| medium | `scripts/advanced_analysis/dead_code_verified_finder.py:524` | `except-continue` | `except OSError: continue` |
| medium | `scripts/advanced_analysis/dead_code_verified_finder.py:629` | `except-continue` | `except OSError: continue` |
| medium | `scripts/advanced_analysis/dependency_freshness_radar.py:173` | `except-return-default` | `except (subprocess.TimeoutExpired, FileNotFoundError, OSError): return ""` |
| medium | `scripts/advanced_analysis/dependency_freshness_radar.py:226` | `except-return-default` | `except OSError: return None` |
| medium | `scripts/advanced_analysis/duplicate_detector.py:272` | `except-continue` | `except Exception: continue` |
| medium | `scripts/advanced_analysis/duplicate_detector.py:488` | `except-continue` | `except Exception: continue` |
| medium | `scripts/advanced_analysis/duplicate_detector.py:543` | `except-continue` | `except Exception: continue` |
| medium | `scripts/advanced_analysis/env_var_reconciler.py:468` | `except-continue` | `except Exception: continue` |
| medium | `scripts/advanced_analysis/error_handling_consistency_checker.py:899` | `except-return-default` | `except Exception: return ""` |
| medium | `scripts/advanced_analysis/hardcode_config_scanner.py:176` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/advanced_analysis/importer_graph.py:199` | `except-return-default` | `except ValueError: return None` |
| medium | `scripts/advanced_analysis/importer_graph.py:293` | `except-return-default` | `except (OSError, IOError): return None` |
| medium | `scripts/advanced_analysis/importer_graph.py:534` | `except-continue` | `except SyntaxError: continue` |
| medium | `scripts/advanced_analysis/importer_graph.py:606` | `except-continue` | `except SyntaxError: continue` |
| medium | `scripts/advanced_analysis/importer_graph.py:736` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/advanced_analysis/importer_graph.py:848` | `except-return-default` | `except (OSError, IOError): return False` |
| medium | `scripts/advanced_analysis/llm_cost_projector.py:456` | `except-return-default` | `except OSError: return` |
| medium | `scripts/advanced_analysis/orphan_route_finder.py:144` | `except-return-default` | `except (OSError, UnicodeDecodeError): return ''` |
| medium | `scripts/advanced_analysis/orphan_route_finder.py:148` | `except-return-default` | `except SyntaxError: return ''` |
| medium | `scripts/advanced_analysis/orphan_route_finder.py:363` | `except-continue` | `except OSError: continue` |
| medium | `scripts/advanced_analysis/orphan_route_finder.py:389` | `except-continue` | `except OSError: continue` |
| medium | `scripts/advanced_analysis/pydantic_schema_consistency_checker.py:326` | `except-continue` | `except (SyntaxError, UnicodeDecodeError) as exc: # বাংলা: syntax error বা encodi` |
| medium | `scripts/advanced_analysis/pydantic_schema_consistency_checker.py:614` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/advanced_analysis/secret_rotation_reminder.py:282` | `except-pass` | `except (subprocess.TimeoutExpired, FileNotFoundError, OSError) as exc: # git পাও` |
| medium | `scripts/ai/repository_metadata_index.py:29` | `except-return-default` | `except (OSError, subprocess.CalledProcessError): return None` |
| medium | `scripts/audit_env_usage.py:66` | `except-continue` | `except Exception: continue` |
| medium | `scripts/audit_isolated_modules_and_capabilities.py:101` | `except-continue` | `except OSError: continue` |
| medium | `scripts/audit_isolated_modules_and_capabilities.py:133` | `except-pass` | `except OSError: pass` |
| medium | `scripts/audit_isolated_modules_and_capabilities.py:232` | `except-continue` | `except SyntaxError: continue` |
| medium | `scripts/audit_isolated_modules_and_capabilities.py:260` | `except-pass` | `except OSError: pass` |
| medium | `scripts/backup/backup_telegram.py:29` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `scripts/backup/backup_telegram.py:120` | `except-continue` | `except OSError: continue` |
| medium | `scripts/backup/backup_telegram.py:337` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/backup/backup_telegram.py:376` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/backup/backup_telegram.py:406` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/backup/backup_telegram.py:858` | `suppress-exception` | `with contextlib.suppress(Exception):` |
| medium | `scripts/backup/create_desktop_backup.py:148` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/backup/create_desktop_backup.py:219` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/backup/create_desktop_backup.py:250` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/billing/fraud_detector.py:195` | `except-continue` | `except Exception: continue` |
| medium | `scripts/check_actions.py:48` | `bare-except` | `except: print('Silenced error in except block')` |
| medium | `scripts/checkpoint_update.py:45` | `except-return-default` | `except Exception: return []` |
| medium | `scripts/ci/check_config_contract.py:39` | `except-continue` | `except (OSError, SyntaxError): continue` |
| medium | `scripts/ci/check_config_control_plane.py:34` | `except-continue` | `except OSError: continue` |
| medium | `scripts/ci/check_hardcoded_deployment_config.py:166` | `except-return-true` | `except ValueError: return True` |
| medium | `scripts/ci/check_single_frontend.py:93` | `except-continue` | `except Exception: continue` |
| medium | `scripts/ci/check_single_frontend.py:107` | `except-continue` | `except Exception: continue` |
| medium | `scripts/ci/check_truthy_env_var.py:77` | `except-pass` | `except SyntaxError: pass # Ignore syntax errors, handled by ruff` |
| medium | `scripts/ci/config_registry_evidence.py:39` | `except-continue` | `except (OSError, SyntaxError): continue` |
| medium | `scripts/ci/generate_module_capability_matrix.py:27` | `except-return-default` | `except (OSError, SyntaxError): return []` |
| medium | `scripts/ci/project_health_check.py:78` | `except-return-default` | `except Exception: return ""` |
| medium | `scripts/ci/render_build_budget_guard.py:97` | `except-continue` | `except Exception: continue` |
| medium | `scripts/ci/render_build_budget_guard.py:130` | `except-return-default` | `except Exception: return None` |
| medium | `scripts/ci/render_deploy_preflight.py:42` | `except-continue` | `except (TypeError, ValueError): continue` |
| medium | `scripts/ci/validate_config_registry.py:77` | `except-continue` | `except OSError: continue` |
| medium | `scripts/core_engine/tool_ranker.py:325` | `except-continue` | `except (ValueError, TypeError): continue` |
| medium | `scripts/devops/bug_prophet.py:681` | `create-task-unref` | `asyncio.create_task(error_event_bus.async_emit(outage_event))` |
| medium | `scripts/devops/devops_ai_scribe.py:228` | `except-return-default` | `except SyntaxError: return None` |
| medium | `scripts/devops/devops_security_scan.py:53` | `except-continue` | `except Exception: # Skip binary files or unreadable files continue` |
| medium | `scripts/devops/generate_modular_audits.py:431` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/devops/todo_manager.py:150` | `except-continue` | `except (OSError, PermissionError): continue` |
| medium | `scripts/diagnostics/superai_console_detective.py:632` | `except-continue` | `except re.error: continue` |
| medium | `scripts/docs/auto_adr_generator.py:81` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/fix_time_sleep.py:44` | `except-return-default` | `except Exception: return False` |
| medium | `scripts/generate_script_index.py:228` | `except-continue` | `except OSError: continue` |
| medium | `scripts/generate_script_index.py:315` | `except-return-default` | `except OSError: return []` |
| medium | `scripts/generate_types.py:161` | `except-return-default` | `except ImportError: return False` |
| medium | `scripts/health/superai_health_check.py:251` | `except-pass` | `except DiscoveryError: pass` |
| medium | `scripts/lib/auto_discovery.py:161` | `except-return-default` | `except (OSError, subprocess.TimeoutExpired): return None` |
| medium | `scripts/lib/auto_discovery.py:280` | `except-continue` | `except SyntaxError: continue` |
| medium | `scripts/lib/auto_discovery.py:298` | `except-continue` | `except (ValueError, SyntaxError): continue` |
| medium | `scripts/lib/auto_discovery.py:313` | `except-continue` | `except OSError: continue` |
| medium | `scripts/lib/auto_discovery.py:441` | `except-pass` | `except OSError: pass` |
| medium | `scripts/lib/auto_discovery.py:473` | `except-return-default` | `except OSError: return []` |
| medium | `scripts/monitoring/capacity_planner.py:142` | `except-continue` | `except json.JSONDecodeError: continue` |
| medium | `scripts/monitoring/superai_log_analyzer.py:402` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/monitoring/superai_log_analyzer.py:712` | `except-continue` | `except ValueError: continue` |
| medium | `scripts/pre_merge_guard.py:290` | `except-continue` | `except OSError: continue` |
| medium | `scripts/pre_merge_guard.py:568` | `except-continue` | `except Exception:  # noqa: BLE001 continue` |
| medium | `scripts/pre_merge_guard.py:707` | `except-pass` | `except json.JSONDecodeError: pass` |
| medium | `scripts/quality/docs_drift_check.py:57` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/quality/regression_scanner.py:110` | `except-continue` | `except (SyntaxError, UnicodeDecodeError): continue` |
| medium | `scripts/quality/regression_scanner.py:190` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/quality/regression_scanner.py:228` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/quality/regression_scanner.py:320` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/quality/regression_scanner.py:342` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/quality/regression_scanner.py:373` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/quality/regression_scanner.py:395` | `except-continue` | `except UnicodeDecodeError: continue` |
| medium | `scripts/quality/regression_scanner.py:418` | `except-continue` | `except (SyntaxError, UnicodeDecodeError): continue` |
| medium | `scripts/security/auto_vulnerability_scanner.py:430` | `except-continue` | `except Exception: continue` |
| medium | `scripts/security/auto_vulnerability_scanner.py:522` | `except-continue` | `except Exception: continue` |
| medium | `tools/autonomy/tools/common.py:26` | `except-return-default` | `except OSError: return ''` |
| medium | `tools/autonomy/tools/deploy_guard.py:14` | `except-return-default` | `except Exception: return []` |
| medium | `tools/autonomy/tools/maintenance_watchdog.py:15` | `except-continue` | `except OSError: continue` |
| medium | `tools/gap_finder/helpers.py:94` | `except-return-default` | `except Exception: return None` |
| medium | `tools/gap_finder/scanner.py:253` | `except-return-default` | `except SyntaxError as exc: add_finding( self.findings, rule_id="PY-001", categor` |
| medium | `tools/gap_miner/tools/architecture_miner.py:20` | `except-continue` | `except OSError:continue` |
| medium | `tools/gap_miner/tools/context_packager.py:33` | `except-continue` | `except OSError: continue` |
| medium | `tools/gap_miner/tools/gap_miner.py:137` | `except-continue` | `except OSError: continue` |
| medium | `tools/gap_miner/tools/project_fingerprint.py:26` | `except-continue` | `except OSError:continue` |
| medium | `tools/gap_miner/tools/provider_capacity_miner.py:20` | `except-continue` | `except OSError: continue` |
| medium | `tools/gap_miner/tools/provider_capacity_miner.py:34` | `except-continue` | `except OSError:continue` |
| medium | `tools/gap_miner/tools/security_config_miner.py:19` | `except-continue` | `except OSError:continue` |
| medium | `tools/intelligence_extensions/supremeai_intelligence/knowledge_revalidator.py:12` | `except-return-true` | `except ValueError:return True` |
| medium | `tools/intelligence_extensions/supremeai_intelligence/memory_curator.py:21` | `except-return-default` | `except ValueError:return False` |
| low | `backend/adapters/business_adapter.py:138` | `except-no-log` | `except Exception as e: self._update_stats(False, 0.0) return AdaptationResult( s` |
| low | `backend/adapters/dev_adapter.py:132` | `except-no-log` | `except Exception as e: self._update_stats(False, 0.0) return AdaptationResult( s` |
| low | `backend/adapters/red_team_adapter.py:111` | `except-no-log` | `except FileNotFoundError: return SecurityReport( findings=[ SecurityFinding( cha` |
| low | `backend/adapters/ux_adapter.py:176` | `except-no-log` | `except Exception as e: self._update_stats(False, 0.0) return AdaptationResult( s` |
| low | `backend/adaptive_engine/_store.py:79` | `except-no-log` | `except (TypeError, ValueError): return default if default is not None else {}` |
| low | `backend/adaptive_engine/capability_node.py:72` | `except-no-log` | `except Exception as exc: self.registry.record_usage(capability.capability_id, su` |
| low | `backend/adaptive_engine/experience_db.py:99` | `except-no-log` | `except (PermissionError, OSError) as _perm_err: import tempfile  tmp_dir = Path(` |
| low | `backend/adaptive_engine/experience_db.py:343` | `except-no-log` | `except RuntimeError: asyncio.run(coro)` |
| low | `backend/adaptive_engine/mcp_skeleton.py:136` | `except-no-log` | `except Exception as exc:  # noqa: BLE001 — MCP must never crash a caller return ` |
| low | `backend/adaptive_engine/resource_registry.py:306` | `except-no-log` | `except NotImplementedError as exc: return {"ok": False, "error": "not_implemente` |
| low | `backend/adaptive_engine/resource_registry.py:308` | `except-no-log` | `except Exception as exc:  # noqa: BLE001 — adapters must never crash the registr` |
| low | `backend/adaptive_engine/source_governance.py:410` | `except-no-log` | `except Exception: return url` |
| low | `backend/adaptive_engine/supabase_vector_backend.py:148` | `except-no-log` | `except Exception: metadata = {}` |
| low | `backend/agents/domain/bangla_nlp_agent.py:168` | `except-no-log` | `except Exception: sentiment = BanglaSentiment( text=text, sentiment="neutral", c` |
| low | `backend/agents/domain/bangla_nlp_agent.py:217` | `except-no-log` | `except Exception: transliteration = TransliterationResult( bangla_text=romanized` |
| low | `backend/agents/ephemeral_executor.py:441` | `except-no-log` | `except (json.JSONDecodeError, TypeError): payload_json = json.dumps({"input": st` |
| low | `backend/agents/governance/ethics_monitor_agent.py:96` | `except-no-log` | `except Exception: violations = [] score = 1.0` |
| low | `backend/agents/governance/ethics_monitor_agent.py:171` | `except-no-log` | `except Exception: return EthicsVerdict( verdict="flagged", confidence=0.5, expla` |
| low | `backend/agents/governance/governance_agent.py:380` | `except-no-log` | `except Exception: return "user"` |
| low | `backend/agents/headless_terminal_agent.py:296` | `except-no-log` | `except TimeoutError: process.kill() return CommandResult( command=command, exit_` |
| low | `backend/agents/infrastructure/auto_scaling_agent.py:137` | `except-no-log` | `except Exception: # If metrics collector is not available, use simulated values ` |
| low | `backend/agents/infrastructure/auto_scaling_agent.py:474` | `except-no-log` | `except Exception: return 0.7  # Default confidence` |
| low | `backend/agents/infrastructure/disaster_recovery_agent.py:640` | `except-no-log` | `except zipfile.BadZipFile: is_valid_structure = False` |
| low | `backend/agents/infrastructure/performance_tuning_agent.py:186` | `except-no-log` | `except Exception: # Fallback values if metrics collector unavailable active_conn` |
| low | `backend/agents/monitoring/technology_radar_agent.py:144` | `except-no-log` | `except Exception as e: return {"technology": tech_name, "error": str(e)}` |
| low | `backend/agents/morphic_adapter.py:105` | `except-no-log` | `except Exception as e: return { "success": False, "code": "", "detail": f"LLM Mo` |
| low | `backend/agents/performance_guardian.py:263` | `except-no-log` | `except Exception as e: return { "operation": operation_name, "error": str(e), }` |
| low | `backend/agents/skill_gc.py:60` | `except-no-log` | `except ValueError: # Parse করতে না পারলে খুব পুরনো ধরে নাও last_used = datetime.` |
| low | `backend/agents/skill_ingestor.py:67` | `except-no-log` | `except SyntaxError: return False, "Invalid Python syntax."` |
| low | `backend/agents/skill_ingestor.py:175` | `except-no-log` | `except Exception as e: manifest.status = SkillStatus.REJECTED self.index_manager` |
| low | `backend/api/routes/admin.py:212` | `except-no-log` | `except TypeError: # Fallback for sync mock results = query.get()` |
| low | `backend/api/routes/admin.py:303` | `except-no-log` | `except TypeError: doc_ref.update(update_data)` |
| low | `backend/api/routes/admin_dashboard.py:92` | `except-no-log` | `except Exception as e: yield f"data: Error reading logs: {e}\n\n"` |
| low | `backend/api/routes/admin_dashboard.py:1593` | `except-no-log` | `except Exception: return { "current_429_events": 0, "per_ip": {}, "per_tenant": ` |
| low | `backend/api/routes/admin_dashboard.py:1620` | `except-no-log` | `except Exception: return { "banks": [{"name": "System Memory", "entry_count": 1,` |
| low | `backend/api/routes/agent_workspace.py:112` | `except-no-log` | `except Exception as e: return {"status": "error", "message": str(e)}` |
| low | `backend/api/routes/approval_manager.py:243` | `except-no-log` | `except WebSocketDisconnect: _connections.remove(ws)` |
| low | `backend/api/routes/browser.py:241` | `except-no-log` | `except Exception: decrypted_dict = {"secret": decrypted}` |
| low | `backend/api/routes/browser.py:272` | `except-no-log` | `except Exception: decrypted_dict = {}` |
| low | `backend/api/routes/browser.py:419` | `except-no-log` | `except Exception: decrypted_payload = {"secret": decrypted}` |
| low | `backend/api/routes/browser_routes.py:449` | `except-no-log` | `except ssl.SSLCertVerificationError as e: issues.append( SecurityIssue( severity` |
| low | `backend/api/routes/browser_routes.py:788` | `except-no-log` | `except Exception as e: health_status["capabilities"].append( {"name": capability` |
| low | `backend/api/routes/byoc_api.py:142` | `except-no-log` | `except Exception as ex: job.status = "failed" job.finished_at = datetime.now(UTC` |
| low | `backend/api/routes/chat_export.py:108` | `except-no-log` | `except (ValueError, TypeError): return ts` |
| low | `backend/api/routes/commandcenter/__init__.py:65` | `except-no-log` | `except Exception as exc: metrics["system"] = {"error": str(exc)[:100]}` |
| low | `backend/api/routes/commandcenter/__init__.py:89` | `except-no-log` | `except Exception as exc: metrics["websocket"] = {"error": f"manager unavailable:` |
| low | `backend/api/routes/commandcenter/__init__.py:100` | `except-no-log` | `except Exception as exc: metrics["maintenance"] = {"error": f"pipeline unavailab` |
| low | `backend/api/routes/commandcenter/__init__.py:111` | `except-no-log` | `except Exception as exc: metrics["errors"] = {"error": f"event_bus unavailable: ` |
| low | `backend/api/routes/commandcenter/__init__.py:123` | `except-no-log` | `except Exception: # App not yet created (e.g., during testing) metrics["app"] = ` |
| low | `backend/api/routes/control_plane.py:55` | `except-no-log` | `except httpx.TimeoutException: return { **service.public_dict(), "status": "time` |
| low | `backend/api/routes/control_plane.py:62` | `except-no-log` | `except Exception as exc: return { **service.public_dict(), "status": "unreachabl` |
| low | `backend/api/routes/deep_research.py:280` | `except-no-log` | `except Exception: refined = query` |
| low | `backend/api/routes/feedback.py:25` | `except-no-log` | `except PermissionError: fallback = Path(tempfile.gettempdir()) / "data" fallback` |
| low | `backend/api/routes/global_memory.py:107` | `except-no-log` | `except (json.JSONDecodeError, TypeError): metadata = {}` |
| low | `backend/api/routes/global_memory.py:398` | `except-no-log` | `except (json.JSONDecodeError, TypeError): metadata = {}` |
| low | `backend/api/routes/health_aggregation.py:134` | `except-no-log` | `except Exception: status = "healthy"` |
| low | `backend/api/routes/health_aggregation.py:152` | `except-no-log` | `except httpx.TimeoutException: return ServiceHealth( name=config["name"], displa` |
| low | `backend/api/routes/health_aggregation.py:163` | `except-no-log` | `except Exception as e: return ServiceHealth( name=config["name"], display_name=c` |
| low | `backend/api/routes/health_aggregation.py:365` | `except-no-log` | `except Exception as e: return { "success": False, "url": service_url, "error": s` |
| low | `backend/api/routes/keys.py:22` | `except-no-log` | `except Exception: # Fallback to random if invalid key was provided in env cipher` |
| low | `backend/api/routes/living_brain.py:204` | `except-no-log` | `except Exception as e: components["economic_optimizer"] = {"status": "error", "e` |
| low | `backend/api/routes/living_brain.py:259` | `except-no-log` | `except Exception as e: metrics["learning"] = {"error": str(e)}` |
| low | `backend/api/routes/living_brain.py:276` | `except-no-log` | `except Exception as e: metrics["memory"] = {"error": str(e)}` |
| low | `backend/api/routes/localization.py:74` | `except-no-log` | `except Exception as e: return {"translation": payload.key, "error": str(e), "fal` |
| low | `backend/api/routes/preferences.py:148` | `except-no-log` | `except TimeoutError: # Heartbeat ping yield { "event": "ping", "data": json.dump` |
| low | `backend/api/routes/prompt_templates.py:349` | `except-no-log` | `except (json.JSONDecodeError, TypeError): variables = []` |
| low | `backend/api/routes/prompt_templates.py:627` | `except-no-log` | `except (json.JSONDecodeError, TypeError): variables = []` |
| low | `backend/api/routes/realtime_dashboard.py:116` | `except-no-log` | `except Exception: # Client disconnected, remove from active connections self.dis` |
| low | `backend/api/routes/realtime_dashboard.py:257` | `except-no-log` | `except WebSocketDisconnect: dashboard_manager.disconnect(websocket)` |
| low | `backend/api/routes/reasoning.py:116` | `except-no-log` | `except (ValueError, IndexError): confidence = 0.5` |
| low | `backend/api/routes/service_topology.py:308` | `except-no-log` | `except httpx.TimeoutException: return ServiceHealthResult( name=service.name, di` |
| low | `backend/api/routes/service_topology.py:320` | `except-no-log` | `except httpx.ConnectError as e: return ServiceHealthResult( name=service.name, d` |
| low | `backend/api/routes/service_topology.py:332` | `except-no-log` | `except Exception as e: return ServiceHealthResult( name=service.name, display_na` |
| low | `backend/api/routes/service_topology.py:591` | `except-no-log` | `except Exception: await self.disconnect(connection)` |
| low | `backend/api/routes/service_topology.py:650` | `except-no-log` | `except WebSocketDisconnect: manager.disconnect(websocket)` |
| low | `backend/api/routes/session_stream.py:51` | `except-no-log` | `except TimeoutError: # Heartbeat yield { "event": "ping", "data": json.dumps({"c` |
| low | `backend/api/routes/session_takeover.py:258` | `except-no-log` | `except Exception: data = None` |
| low | `backend/api/routes/stream_chat_sse.py:106` | `except-no-log` | `except Exception: chunk = "[binary decode error]"` |
| low | `backend/api/routes/stream_chat_sse.py:112` | `except-no-log` | `except Exception: chunk = str(chunk)` |
| low | `backend/api/routes/stream_hitl_sse.py:66` | `except-no-log` | `except TimeoutError: yield ": ping\n\n"` |
| low | `backend/api/routes/websocket_agent.py:234` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/api/routes/websocket_agent.py:515` | `except-no-log` | `except json.JSONDecodeError: content_to_send = user_message` |
| low | `backend/api/routes/websocket_hitl.py:58` | `except-no-log` | `except WebSocketDisconnect: disconnected.add(connection)` |
| low | `backend/api/routes/websocket_voice.py:201` | `except-no-log` | `except WebSocketDisconnect: manager.disconnect(websocket)` |
| low | `backend/brain/api_router.py:17` | `except-no-log` | `except (ValueError, TypeError): self._signatures[capability] = None` |
| low | `backend/brain/gcp_router.py:127` | `except-no-log` | `except Exception: return {"text": response.text}` |
| low | `backend/brain/mcp_client.py:31` | `except-no-log` | `except subprocess.TimeoutExpired: self.process.kill() self.process.wait()` |
| low | `backend/brain/model_router.py:54` | `except-no-log` | `except RuntimeError: loop = None` |
| low | `backend/brain/model_router.py:201` | `except-no-log` | `except Exception as e: return { "success": False, "text": f"Error: {e} (Services` |
| low | `backend/brain/task_execution_engine.py:148` | `except-no-log` | `except Exception as e: failed.add(task_id) errors[task_id] = str(e) self.executi` |
| low | `backend/core/agents/framework/agent_department.py:31` | `except-no-log` | `except Exception as exc:  # pylint: disable=broad-except return {"role": self.ro` |
| low | `backend/core/agents/framework/agent_department.py:75` | `except-no-log` | `except Exception as exc:  # pylint: disable=broad-except return {"role": self.ro` |
| low | `backend/core/agents/framework/agent_department.py:111` | `except-no-log` | `except Exception as exc:  # pylint: disable=broad-except return {"role": self.ro` |
| low | `backend/core/agents/framework/langgraph_agent.py:64` | `except-no-log` | `except PermissionError as exc: return {"success": False, "result": f"Blocked: {e` |
| low | `backend/core/agents/framework/task_runner_agent.py:30` | `except-no-log` | `except Exception: self.skill_creator = None  # type: ignore` |
| low | `backend/core/agents/framework/task_runner_agent.py:138` | `except-no-log` | `except RuntimeError: # If no event loop is running, run it directly asyncio.run(` |
| low | `backend/core/agents/live/computer_agent.py:42` | `except-no-log` | `except subprocess.TimeoutExpired: return {"success": False, "error": "Command ti` |
| low | `backend/core/agents/live/computer_agent.py:44` | `except-no-log` | `except Exception as e: return {"success": False, "error": str(e)}` |
| low | `backend/core/agents/live/computer_agent.py:53` | `except-no-log` | `except Exception as e: return {"success": False, "error": str(e)}` |
| low | `backend/core/autonoguard_engine.py:231` | `except-no-log` | `except (ValueError, TypeError): failures = 0` |
| low | `backend/core/cache/__init__.py:24` | `except-no-log` | `except json.JSONDecodeError: return val` |
| low | `backend/core/cache/multi_layer_cache.py:370` | `except-no-log` | `except Exception: exact_ttl = 3600` |
| low | `backend/core/cache/predictive_cache_engine.py:118` | `except-no-log` | `except Exception as e: decisions.append({"key": pred.key, "status": f"error: {e}` |
| low | `backend/core/cache_manager.py:107` | `except-no-log` | `except zlib.error: return value  # Not compressed` |
| low | `backend/core/cache_manager.py:217` | `except-no-log` | `except (json.JSONDecodeError, TypeError): result[key] = value` |
| low | `backend/core/circles/registry.py:72` | `except-no-log` | `except Exception as exc: result = ExecutionResult( execution_id=request.context.` |
| low | `backend/core/code_validator.py:64` | `except-no-log` | `except SyntaxError as e: return not ("unexpected indent" in str(e) or "unindent ` |
| low | `backend/core/config_cache.py:140` | `except-no-log` | `except RuntimeError: self._refresh_pending = False` |
| low | `backend/core/config_secrets.py:260` | `except-no-log` | `except Exception: url = ""` |
| low | `backend/core/config_secrets.py:559` | `except-no-log` | `except json.JSONDecodeError: origins = [o.strip() for o in env_origins.split(","` |
| low | `backend/core/config_validation.py:351` | `except-no-log` | `except json.JSONDecodeError: return [o.strip() for o in v.split(",") if o.strip(` |
| low | `backend/core/config_validator.py:314` | `except-no-log` | `except (ValueError, TypeError): return ValidationError( var_name=var_def.name, s` |
| low | `backend/core/contracts/render_preflight_store.py:116` | `except-no-log` | `except Exception: data["last_render_payload"] = None` |
| low | `backend/core/contracts/render_preflight_store.py:131` | `except-no-log` | `except Exception: data["last_render_payload"] = None` |
| low | `backend/core/contracts/render_preflight_store.py:321` | `except-no-log` | `except Exception: data["details"] = None` |
| low | `backend/core/db.py:59` | `except-no-log` | `except AttributeError: url = ""` |
| low | `backend/core/degraded_mode.py:86` | `except-no-log` | `except Exception:  # pragma: no cover - defensive: config unavailable env = ""` |
| low | `backend/core/deployment/production_deploy.py:539` | `except-no-log` | `except Exception as e: self._update_deployment_status(deployment_id, DeploymentS` |
| low | `backend/core/evolution_module.py:194` | `except-no-log` | `except Exception as e: return EvolutionResult( evolved_solution=current_solution` |
| low | `backend/core/evolution_module.py:347` | `except-no-log` | `except Exception: return 0.85` |
| low | `backend/core/factual_verifier.py:155` | `except-no-log` | `except StopIteration: return { "claim": claim, "is_verified": False, "confidence` |
| low | `backend/core/factual_verifier.py:259` | `except-no-log` | `except Exception as e: try: clean_expr = re.sub(r"[^0-9\+\-\*\/\(\)\.]", "", exp` |
| low | `backend/core/factual_verifier.py:272` | `except-no-log` | `except Exception as inner_e: return { "is_correct": False, "is_verified": False,` |
| low | `backend/core/health/health_monitor.py:103` | `except-no-log` | `except Exception as e: probe_results[name] = {"status": "error", "message": str(` |
| low | `backend/core/health/health_probes.py:29` | `except-no-log` | `except Exception as e: return {"status": "down", "latency": None, "reason": str(` |
| low | `backend/core/health/health_probes.py:45` | `except-no-log` | `except Exception as e: return {"status": "down", "latency": None, "reason": str(` |
| low | `backend/core/health/health_probes.py:62` | `except-no-log` | `except Exception as e: return {"status": "down", "latency": None, "reason": str(` |
| low | `backend/core/health_routes.py:114` | `except-no-log` | `except Exception as e: latency = (time.monotonic() - start) * 1000 return Health` |
| low | `backend/core/integration_layer.py:200` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/core/integration_layer.py:211` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/core/intelligent_cache.py:498` | `except-no-log` | `except Exception: health["status"] = "degraded" health["error"] = "Redis unreach` |
| low | `backend/core/intelligent_silent_catcher.py:76` | `except-no-log` | `except Exception as e: handle_unhandled_exception(type(e), e, e.__traceback__)` |
| low | `backend/core/intent_router_v2.py:360` | `except-no-log` | `except RuntimeError: # No event loop at all — sync mode return self._route_regex` |
| low | `backend/core/learning/dedup.py:51` | `except-no-log` | `except Exception: blob = str(messages_payload)` |
| low | `backend/core/learning/dedup.py:135` | `except-no-log` | `except Exception: return dict(entry.response)` |
| low | `backend/core/learning/loop.py:32` | `except-no-log` | `except Exception:  # pragma: no cover import logging  logger = logging.getLogger` |
| low | `backend/core/learning/policies.py:47` | `except-no-log` | `except (TypeError, ValueError): return int(base_ttl_seconds)` |
| low | `backend/core/learning/policies.py:80` | `except-no-log` | `except (TypeError, ValueError): return baseline` |
| low | `backend/core/learning/provider_scorer.py:72` | `except-no-log` | `except (TypeError, ValueError): return _DEFAULT_WEIGHTS[name]` |
| low | `backend/core/learning/store.py:42` | `except-no-log` | `except Exception:  # pragma: no cover - only when logging_config is broken logge` |
| low | `backend/core/learning/store.py:483` | `except-no-log` | `except (TypeError, ValueError): inserted = 0` |
| low | `backend/core/learning/store.py:552` | `except-break` | `except Exception: break` |
| low | `backend/core/llm/advanced_model_router.py:271` | `except-no-log` | `except json.JSONDecodeError: result = {"content": raw_text}` |
| low | `backend/core/llm/advanced_model_router.py:278` | `except-no-log` | `except Exception as exc: return {"error": str(exc)}` |
| low | `backend/core/llm/llm_gateway.py:300` | `except-no-log` | `except Exception: duration = 0.0` |
| low | `backend/core/llm/llm_gateway.py:387` | `except-no-log` | `except ValueError: # If Retry-After is in date format, calculate difference try:` |
| low | `backend/core/llm/llm_gateway.py:396` | `except-no-log` | `except (ValueError, TypeError): # Default fallback if parsing fails pause_second` |
| low | `backend/core/llm/llm_gateway.py:534` | `except-no-log` | `except Exception:  # Safe fallback cost on token estimate failure estimated_cost` |
| low | `backend/core/llm/llm_gateway.py:714` | `except-no-log` | `except Exception: _estimated_tokens = None` |
| low | `backend/core/localization/voice_didi.py:198` | `except-no-log` | `except json.JSONDecodeError: # Fallback: treat raw text as corrected transcripti` |
| low | `backend/core/memory_manager.py:64` | `except-no-log` | `except (psutil.NoSuchProcess, psutil.ZombieProcess): self._process = psutil.Proc` |
| low | `backend/core/messaging/event_bus.py:268` | `except-no-log` | `except Exception as exc: return exc  # exception return করা হচ্ছে, suppress নয়` |
| low | `backend/core/messaging/event_bus.py:325` | `except-break` | `except asyncio.QueueEmpty: break` |
| low | `backend/core/messaging/gcp_pubsub_queue.py:20` | `except-no-log` | `except Exception: PUBSUB_AVAILABLE = False` |
| low | `backend/core/microvm_sandbox.py:327` | `except-no-log` | `except subprocess.TimeoutExpired: return { "success": False, "error": "Execution` |
| low | `backend/core/microvm_sandbox.py:380` | `except-no-log` | `except subprocess.TimeoutExpired: return { "success": False, "error": "Execution` |
| low | `backend/core/microvm_sandbox.py:457` | `except-no-log` | `except subprocess.TimeoutExpired: return { "success": False, "error": "Execution` |
| low | `backend/core/observability/log_batcher.py:81` | `except-break` | `except asyncio.QueueEmpty: break` |
| low | `backend/core/observability/log_batcher.py:86` | `except-no-log` | `except TimeoutError: if self.buffer: await self._flush()` |
| low | `backend/core/observability/providers/langfuse_adapter.py:77` | `except-no-log` | `except Exception: safe_prompt = str(prompt)` |
| low | `backend/core/optimization/performance_optimizer.py:280` | `except-no-log` | `except RuntimeError: loop = None` |
| low | `backend/core/optimization/performance_optimizer.py:296` | `except-no-log` | `except RuntimeError: loop = None` |
| low | `backend/core/optimization/performance_optimizer.py:506` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/core/orchestration/conversation_orchestrator.py:257` | `except-no-log` | `except TimeoutError: event.update(type="orchestration.timed_out", status="failed` |
| low | `backend/core/orchestration/conversation_orchestrator.py:269` | `except-no-log` | `except Exception: event.update(type="orchestration.failed", status="failed", ret` |
| low | `backend/core/orchestration/crew_departments.py:336` | `except-no-log` | `except Exception: parsed = { "what_worked": [analysis], "what_failed": [], "sugg` |
| low | `backend/core/performance_enhancer.py:504` | `except-no-log` | `except Exception as e: execution_time = time.time() - start_time await self.trac` |
| low | `backend/core/persistence/write_behind.py:84` | `except-break` | `except queue.Empty: break` |
| low | `backend/core/provider_rate_limiter.py:222` | `except-no-log` | `except RateLimitException as e: self._handle_rate_limit(provider_name, e) last_e` |
| low | `backend/core/provider_rate_limiter.py:226` | `except-no-log` | `except Exception as e: self._handle_provider_down(provider_name) last_error = e ` |
| low | `backend/core/queue/task_queue.py:22` | `except-no-log` | `except Exception:  # noqa: BLE001 — settings may be unavailable very early url =` |
| low | `backend/core/queue/task_queue.py:124` | `except-no-log` | `except TimeoutError: result = None` |
| low | `backend/core/queue/task_queue_enhanced.py:37` | `except-no-log` | `except (TypeError, ValueError): return 100` |
| low | `backend/core/queue/task_queue_enhanced.py:238` | `except-no-log` | `except Exception: priorities = ["asyncio"]` |
| low | `backend/core/retry_handler.py:120` | `except-no-log` | `except RuntimeError: time.sleep(current_delay)` |
| low | `backend/core/retry_handler.py:246` | `except-no-log` | `except RuntimeError: time.sleep(current_delay)` |
| low | `backend/core/schema_validator.py:80` | `except-no-log` | `except (SchemaValidationError, KeyError) as exc: return {"status": "error", "sch` |
| low | `backend/core/security/__init__.py:187` | `except-no-log` | `except Exception as exc: sys.stderr.write(f"❌ Security scan failed: {exc}\n") re` |
| low | `backend/core/security/__init__.py:404` | `except-no-log` | `except RuntimeError: caller_loop = None` |
| low | `backend/core/security/authentication/rbac.py:114` | `except-no-log` | `except ValueError: return frozenset()` |
| low | `backend/core/security/injections/sql_prevention.py:239` | `except-no-log` | `except (ValueError, TypeError): return default` |
| low | `backend/core/security/injections/sql_prevention.py:576` | `except-no-log` | `except SyntaxError: return findings` |
| low | `backend/core/security/origin_validator.py:27` | `except-no-log` | `except json.JSONDecodeError: return frozenset([x.strip() for x in val.split(",")` |
| low | `backend/core/security/protection/honeypot.py:199` | `except-no-log` | `except RuntimeError: # বাংলা মন্তব্য: event loop না থাকলে synchronously execute ` |
| low | `backend/core/security/protection/ssrf_protection.py:209` | `except-no-log` | `except Exception as e: result.is_safe = False result.reason = f"URL parsing fail` |
| low | `backend/core/security/protection/ssrf_protection.py:253` | `except-no-log` | `except (socket.gaierror, OSError) as e: result.is_safe = False result.reason = f` |
| low | `backend/core/security/protection/ssrf_protection.py:357` | `except-no-log` | `except ValueError as e: result.is_safe = False result.reason = f"Invalid IP addr` |
| low | `backend/core/security/scanning/ast_scanner.py:234` | `except-no-log` | `except SyntaxError as e: result.is_safe = False result.severity = "HIGH" result.` |
| low | `backend/core/security/scanning/secret_scanner.py:256` | `except-no-log` | `except ValueError: rel_parts = file_path.parts` |
| low | `backend/core/security/scanning/secret_scanner.py:391` | `except-no-log` | `except OSError: context = finding.matched_text` |
| low | `backend/core/security/secure_credential_store.py:75` | `except-no-log` | `except InvalidToken as e: last_exc = e continue` |
| low | `backend/core/security/tool_gateway.py:107` | `except-no-log` | `except RuntimeError: loop = None` |
| low | `backend/core/self_benchmark.py:239` | `except-no-log` | `except Exception: times.append(float(self.test_duration_per_query_ms))` |
| low | `backend/core/self_evolution/evolution_engine.py:340` | `except-no-log` | `except Exception as e: return {"status": "error", "error": str(e)}` |
| low | `backend/core/self_evolution/evolution_react_agent.py:147` | `except-no-log` | `except SyntaxError as e: return { "passed": False, "reason": f"SyntaxError on li` |
| low | `backend/core/self_evolution/evolution_react_agent.py:152` | `except-no-log` | `except (ValueError, OverflowError) as e: return {"passed": False, "reason": str(` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:245` | `except-no-log` | `except Exception: derivatives[var] = "undefined"` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:257` | `except-no-log` | `except Exception: integrals[var] = "cannot integrate"` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:404` | `except-no-log` | `except Exception as e: results["symbolic"] = {"error": str(e)}` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:419` | `except-no-log` | `except Exception as e: results["neural"] = {"error": str(e)}` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:514` | `except-no-log` | `except Exception: results["solution"] = {"parsed": str(expr.parsed_expr)}` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:521` | `except-no-log` | `except Exception as e: results["error"] = str(e) results["confidence"] = 0.0` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:592` | `except-no-log` | `except Exception: results["verification"].append( {"solution": str(sol), "error"` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:605` | `except-no-log` | `except Exception as e: return {"equation": equation, "variable": variable, "erro` |
| low | `backend/core/self_evolution/neural_symbolic/integration.py:650` | `except-no-log` | `except Exception as e: return { "original_expression": expression, "operation": ` |
| low | `backend/core/self_evolution/self_evolution_agent.py:120` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/core/storage/local_adapter.py:44` | `except-no-log` | `except Exception as e: return StorageResult(success=False, bucket=bucket, key=ke` |
| low | `backend/core/swarm_pubsub.py:224` | `except-no-log` | `except TimeoutError: if buffer: yield json.dumps({"type": "batched_delta", "even` |
| low | `backend/core/swarm_pubsub.py:229` | `except-break` | `except StopAsyncIteration: break` |
| low | `backend/core/swarm_pubsub.py:235` | `except-no-log` | `except json.JSONDecodeError: buffer.append({"type": "raw", "data": raw_msg})` |
| low | `backend/core/tier8/agent_evolution_engine.py:183` | `except-no-log` | `except json.JSONDecodeError: seeds = []` |
| low | `backend/core/tier8/agent_evolution_engine.py:226` | `except-no-log` | `except Exception as exc: await self._log_error("evolution_loop", str(exc))` |
| low | `backend/core/tier8/self_improvement_agent.py:141` | `except-no-log` | `except Exception as exc: await self._feedback.record_error_report(  # type: igno` |
| low | `backend/core/tier8/self_improvement_agent.py:259` | `except-no-log` | `except json.JSONDecodeError: parsed = {"patch": raw, "confidence": 0.5, "rationa` |
| low | `backend/core/tier8/skill_marketplace_curator.py:226` | `except-no-log` | `except Exception as exc: await self._log_error("curation_loop", str(exc))` |
| low | `backend/core/tier8/swarm_coordination_agent.py:190` | `except-no-log` | `except Exception as exc: await self._log_error("coordination_loop", str(exc))` |
| low | `backend/core/tier8/swarm_coordination_agent.py:209` | `except-no-log` | `except Exception as exc: await self._log_error("heartbeat_loop", str(exc))` |
| low | `backend/core/tier8/swarm_coordination_agent.py:295` | `except-no-log` | `except Exception as exc: return {"agent_id": agent.agent_id, "error": str(exc)}` |
| low | `backend/core/type_sync_bus.py:189` | `except-no-log` | `except TimeoutError: return { "success": False, "error": f"Generation timed out ` |
| low | `backend/core/type_sync_bus.py:195` | `except-no-log` | `except Exception as e: return { "success": False, "error": str(e), "timestamp": ` |
| low | `backend/core/type_sync_bus.py:223` | `except-no-log` | `except Exception as e: return { "drift_detected": True, "error": str(e), "timest` |
| low | `backend/core/type_sync_bus.py:271` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/core/unified_learning.py:254` | `except-no-log` | `except json.JSONDecodeError: # If merge fails, just update existing_knowledge.ou` |
| low | `backend/core/universal_rules.py:148` | `except-no-log` | `except (json.JSONDecodeError, TypeError): parsed_val = value` |
| low | `backend/core/zero_cost_architecture/swarm_orchestrator_integration.py:209` | `except-no-log` | `except Exception as e: # Record failure duration = time.monotonic() - task_start` |
| low | `backend/core/zero_cost_architecture/zero_cost_patch_phase1_4.py:1032` | `except-no-log` | `except Exception as e: status["status"] = f"error: {str(e)[:50]}"` |
| low | `backend/database/supabase_client.py:109` | `except-no-log` | `except RuntimeError: # No running event loop — sync context, time.sleep is fine ` |
| low | `backend/ecosystem/seed_ecosystem.py:138` | `except-no-log` | `except Exception: skipped += 1` |
| low | `backend/ecosystem/seed_ecosystem.py:209` | `except-no-log` | `except Exception: skipped += 1` |
| low | `backend/ecosystem/seed_ecosystem.py:256` | `except-no-log` | `except Exception: skipped += 1` |
| low | `backend/ecosystem/seed_ecosystem.py:278` | `except-no-log` | `except Exception: return 0, 0` |
| low | `backend/ecosystem/seed_ecosystem.py:301` | `except-no-log` | `except Exception: return 0, 0` |
| low | `backend/ecosystem/users.py:452` | `except-no-log` | `except (ValueError, AttributeError): exp_dt = datetime.now(UTC)` |
| low | `backend/engine/compression/token_juice.py:228` | `except-no-log` | `except Exception: return self.compress_generic_text(raw_json)` |
| low | `backend/engine/debate_engine.py:136` | `except-no-log` | `except (TypeError, ValueError): winner.score = 0.0` |
| low | `backend/evolution/auto_evolution_controller.py:218` | `except-no-log` | `except Exception as e: cycle.errors_encountered.append(str(e)) cycle.end_time = ` |
| low | `backend/evolution/memory_consolidator.py:211` | `except-no-log` | `except Exception: return str(data).encode("utf-8")` |
| low | `backend/evolution/performance_monitor.py:198` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/get_tb.py:9` | `except-no-log` | `except Exception: with open("traceback.txt", "w") as f: traceback.print_exc(file` |
| low | `backend/integrations/e2b_adapter.py:94` | `except-no-log` | `except subprocess.TimeoutExpired as exc: return { "status": "error", "engine": "` |
| low | `backend/integrations/e2b_adapter.py:101` | `except-no-log` | `except Exception as exc:  # pragma: no cover - defensive return {"status": "erro` |
| low | `backend/integrations/graphiti_adapter.py:39` | `except-no-log` | `except RuntimeError: return asyncio.run(coro)` |
| low | `backend/memory/chromadb_store.py:19` | `except-no-log` | `except Exception: _CHROMA_AVAILABLE = False` |
| low | `backend/memory/supabase_store.py:390` | `except-no-log` | `except Exception as e: failed_count += 1 errors.append(str(e))` |
| low | `backend/middleware/cors_policy.py:28` | `except-no-log` | `except json.JSONDecodeError: return tuple([x.strip() for x in val.split(",") if ` |
| low | `backend/middleware/idempotency_middleware.py:178` | `except-no-log` | `except json.JSONDecodeError: # যদি JSON না হয় (যেমন ছবি বা ফাইল), তাহলে Base64 এ` |
| low | `backend/monitoring/logging_config.py:64` | `except-no-log` | `except ValueError: level = record.levelno` |
| low | `backend/monitoring/logging_config.py:114` | `except-no-log` | `except Exception as _ctx_err: # বাংলা মন্তব্য: starlette_context request scope-এ` |
| low | `backend/p2p/resource_broker.py:93` | `except-no-log` | `except InsufficientCreditsError as e: node["status"] = "idle" return {"status": ` |
| low | `backend/pyerrorfix/detectors/asyncio_err.py:58` | `except-no-log` | `except SyntaxError: return self.issues` |
| low | `backend/pyerrorfix/detectors/asyncio_err.py:194` | `except-no-log` | `except SyntaxError: self._cached_tree = ast.Module(body=[], type_ignores=[])` |
| low | `backend/pyerrorfix/detectors/base.py:40` | `except-no-log` | `except SyntaxError: # SyntaxError is reported by the syntax detector, not here. ` |
| low | `backend/pyerrorfix/detectors/imports.py:251` | `except-no-log` | `except SyntaxError: return self.issues` |
| low | `backend/pyerrorfix/detectors/logging_err.py:158` | `except-no-log` | `except Exception: args.append("?")` |
| low | `backend/pyerrorfix/detectors/logging_err.py:166` | `except-no-log` | `except Exception: base = "logger"` |
| low | `backend/pyerrorfix/detectors/network_io.py:135` | `except-no-log` | `except SyntaxError: self._cached_tree = ast.Module(body=[], type_ignores=[])` |
| low | `backend/pyerrorfix/detectors/syntax.py:34` | `except-no-log` | `except SyntaxError as exc: # Classify: IndentationError vs TabError vs generic S` |
| low | `backend/pyerrorfix/fixers/await_fixer.py:20` | `except-no-log` | `except SyntaxError: return self.source` |
| low | `backend/pyerrorfix/fixers/fstring_log_fixer.py:21` | `except-no-log` | `except SyntaxError: return self.source` |
| low | `backend/pyerrorfix/fixers/import_fixer.py:41` | `except-no-log` | `except SyntaxError: return self.source` |
| low | `backend/pyerrorfix/pyerrorfix_config.py:166` | `except-no-log` | `except json.JSONDecodeError: return base` |
| low | `backend/runtime/task_runtime.py:107` | `except-no-log` | `except BudgetExceededError as budget_err: task.fail(f"Budget exceeded: {budget_e` |
| low | `backend/runtime/task_runtime.py:117` | `except-no-log` | `except TimeoutError: task.fail(f"Task exceeded budget timeout ({task.budget.max_` |
| low | `backend/sandbox/docker_sandbox.py:94` | `except-no-log` | `except subprocess.TimeoutExpired: return { "exit_code": 124,  # Standard timeout` |
| low | `backend/sandbox/docker_sandbox.py:100` | `except-no-log` | `except Exception as e: return { "exit_code": -1, "stdout": "", "stderr": f"Docke` |
| low | `backend/scaling/distributed_manager.py:187` | `except-break` | `except asyncio.QueueEmpty: break` |
| low | `backend/scaling/distributed_manager.py:213` | `except-no-log` | `except Exception as e: task.error = str(e) node.tasks_failed += 1 self.stats["ta` |
| low | `backend/scripts/auto_test_gen.py:131` | `except-no-log` | `except ValueError: rel = Path(module.source_path.name)` |
| low | `backend/scripts/benchmark/load_test_phase3.py:23` | `except-no-log` | `except Exception as e: if "402 Payment Required" in str(e): return "402" return ` |
| low | `backend/scripts/benchmark/load_test_phase3.py:85` | `except-no-log` | `except Exception as e: if str(e) == "Exit Loop": pass` |
| low | `backend/scripts/run_chaos_experiment.py:53` | `except-no-log` | `except Exception as e: results.append({"scenario": "Network Latency Spike", "sta` |
| low | `backend/scripts/run_chaos_experiment.py:63` | `except-no-log` | `except ConnectionError: failures += 1 cb.mark_failure()` |
| low | `backend/scripts/seed_ecosystem.py:171` | `except-no-log` | `except Exception: skipped += 1` |
| low | `backend/scripts/self_healing_tests.py:109` | `except-no-log` | `except TimeoutError: _quarantine_and_diagnose(state, "Healing Loop Timeout (5s e` |
| low | `backend/services/auto_healer.py:421` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/services/auto_healer.py:668` | `except-no-log` | `except Exception as e: return FixResult( success=False, issue_id=issue.id, fix_a` |
| low | `backend/services/browser/main.py:44` | `except-no-log` | `except Exception as e: return web.json_response({"ok": False, "error": str(e)}, ` |
| low | `backend/services/browser/main.py:66` | `except-no-log` | `except Exception as e: return web.json_response({"ok": False, "error": str(e)}, ` |
| low | `backend/services/config_service.py:54` | `except-no-log` | `except json.JSONDecodeError: return cached_val` |
| low | `backend/services/diagram_parser_service.py:486` | `except-no-log` | `except Exception as e: return {"status": "error", "error": str(e)}` |
| low | `backend/services/dynamic_ai/local_fallback.py:271` | `except-no-log` | `except Exception as e: return { "success": False, "error": f"Ollama generation e` |
| low | `backend/services/dynamic_ai/orchestrator.py:304` | `except-no-log` | `except Exception as e: last_error = f"Local fallback error: {e}"` |
| low | `backend/services/dynamic_ai/provider_registry.py:373` | `except-no-log` | `except Exception as e: config.last_error = str(e) config.last_error_time = time.` |
| low | `backend/services/ide_trio/cline_checker.py:78` | `except-no-log` | `except SyntaxError as e: return { "passed": False, "severity": "blocking", "mess` |
| low | `backend/services/llm/llm_router.py:374` | `except-no-log` | `except ValueError: prov_enum = Provider.OLLAMA  # Fallback mapping` |
| low | `backend/services/llm/llm_router.py:571` | `except-no-log` | `except (ValueError, TypeError): retry_after = 60.0` |
| low | `backend/services/render_preflight_service.py:142` | `except-no-log` | `except urllib.error.HTTPError as err: err_msg = f"HTTP {err.code}: {err.reason}"` |
| low | `backend/services/render_preflight_service.py:171` | `except-no-log` | `except Exception as exc: return self.store.upsert_account_and_record_event( acco` |
| low | `backend/services/self_correction.py:62` | `except-no-log` | `except Exception as exc: violations.append(f"DAG Topological Sort Failed: {exc}"` |
| low | `backend/services/storage/gcp_firestore.py:33` | `except-no-log` | `except Exception: FIRESTORE_AVAILABLE = False` |
| low | `backend/services/worker/main.py:68` | `except-break` | `except asyncio.CancelledError: break` |
| low | `backend/skills/installer.py:36` | `except-no-log` | `except Exception: return (os.getenv("ENV", "") or "").lower() in {"production", ` |
| low | `backend/tools/agent_tools.py:109` | `except-no-log` | `except Exception as exc: health["system_error"] = str(exc)` |
| low | `backend/tools/agent_tools.py:123` | `except-no-log` | `except Exception as exc: health["redis"] = f"OFFLINE ({exc})"` |
| low | `backend/tools/agent_tools.py:141` | `except-no-log` | `except Exception as exc: health["database"] = f"OFFLINE ({exc})"` |
| low | `backend/tools/api_gateway.py:44` | `except-no-log` | `except Exception as exc: return {"success": False, "error": str(exc)}` |
| low | `backend/tools/browser/ai_web_extractor.py:36` | `except-no-log` | `except Exception as e: # মডেল রাউটার বা ডেটা এক্সট্র্যাকশন সম্পর্কিত যেকোনো ত্রু` |
| low | `backend/tools/browser/playwright_browser_agent.py:153` | `except-no-log` | `except RuntimeError: loop = asyncio.new_event_loop() asyncio.set_event_loop(loop` |
| low | `backend/tools/browser/playwright_browser_agent.py:345` | `except-no-log` | `except Exception as e: return {"success": False, "error": str(e)}` |
| low | `backend/tools/code/auto_test_generator.py:116` | `except-no-log` | `except SyntaxError: return {"functions": [], "classes": [], "async_functions": [` |
| low | `backend/tools/code/auto_test_generator.py:386` | `except-no-log` | `except Exception as exc: return {"returncode": -1, "passed": False, "error": str` |
| low | `backend/tools/code/code_smell_detector.py:173` | `except-no-log` | `except SyntaxError as e: smells.append( { "type": "Syntax Error", "line": e.line` |
| low | `backend/tools/code/cot_reasoner.py:69` | `except-no-log` | `except Exception as exc: return {"success": False, "error": str(exc)}` |
| low | `backend/tools/code/cot_reasoner.py:91` | `except-no-log` | `except Exception as e: try: clean_expr = re.sub(r"[^0-9\+\-\*\/\(\)\.\s]", "", e` |
| low | `backend/tools/code/cot_reasoner.py:103` | `except-no-log` | `except Exception as inner_e: return { "is_verified": False, "error": f"Sympy err` |
| low | `backend/tools/code/diagram_to_architecture.py:214` | `except-no-log` | `except Exception as e: return {"status": "error", "error": str(e)}` |
| low | `backend/tools/code/pre_commit_ai.py:12` | `except-no-log` | `except Exception: _PR_REVIEWER_AVAILABLE = False PRReviewer = None  # type: igno` |
| low | `backend/tools/code/voice_coder.py:102` | `except-no-log` | `except Exception as e: return f"Could not explain: {e}"` |
| low | `backend/tools/collaborative_editor.py:282` | `except-no-log` | `except WebSocketDisconnect: await editor_manager.disconnect_client(session_id, c` |
| low | `backend/tools/comment_thread_ai.py:222` | `except-no-log` | `except Exception as exc: return {"status": "error", "error": f"GitHub API failed` |
| low | `backend/tools/comment_thread_ai.py:265` | `except-no-log` | `except Exception as exc: return {"status": "error", "error": str(exc)}` |
| low | `backend/tools/creative/creative_agents_registry.py:50` | `except-no-log` | `except Exception as exc: results[name] = f"failed: {exc}"` |
| low | `backend/tools/devops/docker_sandbox.py:128` | `except-no-log` | `except ( FileNotFoundError, subprocess.TimeoutExpired, OSError, subprocess.Calle` |
| low | `backend/tools/devops/docker_sandbox.py:179` | `except-no-log` | `except ( FileNotFoundError, subprocess.TimeoutExpired, OSError, subprocess.Calle` |
| low | `backend/tools/devops/docker_sandbox.py:242` | `except-no-log` | `except subprocess.TimeoutExpired: return { "success": False, "error": f"Executio` |
| low | `backend/tools/knowledge/codebase_exporter.py:129` | `except-no-log` | `except Exception as exc: return f"### File: `{rel}` (read error: {exc})\n\n"` |
| low | `backend/tools/knowledge/codebase_exporter.py:194` | `except-no-log` | `except Exception as e: return f"Failed to retrieve git changes: {e}"` |
| low | `backend/tools/knowledge/knowledge_base_indexer.py:74` | `except-no-log` | `except SyntaxError: return docs` |
| low | `backend/tools/knowledge/knowledge_base_indexer.py:272` | `except-no-log` | `except Exception as exc: errors.append(f"{name}: {exc}")` |
| low | `backend/tools/knowledge/knowledge_base_indexer.py:318` | `except-no-log` | `except Exception as exc: errors.append(str(exc))` |
| low | `backend/tools/learning/style_learner.py:86` | `except-no-log` | `except Exception as parse_err: skipped_ast_files.append(f"{path} ({parse_err})")` |
| low | `backend/tools/learning/style_learner.py:158` | `except-no-log` | `except Exception as read_err: skipped_sample_files.append(f"{path} ({read_err})"` |
| low | `backend/tools/mcp/mcp_cloud_deploy.py:197` | `except-no-log` | `except httpx.HTTPStatusError as e: return handle_api_error(e, e.response.status_` |
| low | `backend/tools/mcp/mcp_cloud_deploy.py:199` | `except-no-log` | `except Exception as e: return handle_api_error(e)` |
| low | `backend/tools/mcp/mcp_cloud_deploy.py:268` | `except-no-log` | `except httpx.HTTPStatusError as e: return handle_api_error(e, e.response.status_` |
| low | `backend/tools/mcp/mcp_cloud_deploy.py:270` | `except-no-log` | `except Exception as e: return handle_api_error(e)` |
| low | `backend/tools/mcp/mcp_github_cicd.py:132` | `except-no-log` | `except httpx.HTTPStatusError as e: return handle_api_error(e, e.response.status_` |
| low | `backend/tools/mcp/mcp_github_cicd.py:134` | `except-no-log` | `except Exception as e: return handle_api_error(e)` |
| low | `backend/tools/mcp/mcp_github_cicd.py:202` | `except-no-log` | `except httpx.HTTPStatusError as e: return handle_api_error(e, e.response.status_` |
| low | `backend/tools/mcp/mcp_github_cicd.py:204` | `except-no-log` | `except Exception as e: return handle_api_error(e)` |
| low | `backend/tools/mcp/mcp_github_cicd.py:271` | `except-no-log` | `except httpx.HTTPStatusError as e: return handle_api_error(e, e.response.status_` |
| low | `backend/tools/mcp/mcp_github_cicd.py:273` | `except-no-log` | `except Exception as e: return handle_api_error(e)` |
| low | `backend/tools/mcp/mcp_github_cicd.py:323` | `except-no-log` | `except httpx.HTTPStatusError as e: return handle_api_error(e, e.response.status_` |
| low | `backend/tools/mcp/mcp_github_cicd.py:325` | `except-no-log` | `except Exception as e: return handle_api_error(e)` |
| low | `backend/tools/mcp/mcp_github_cicd.py:409` | `except-no-log` | `except httpx.HTTPStatusError as e: return handle_api_error(e, e.response.status_` |
| low | `backend/tools/mcp/mcp_github_cicd.py:411` | `except-no-log` | `except Exception as e: return handle_api_error(e)` |
| low | `backend/tools/mcp/mcp_github_cicd.py:479` | `except-no-log` | `except httpx.HTTPStatusError as e: return handle_api_error(e, e.response.status_` |
| low | `backend/tools/mcp/mcp_github_cicd.py:481` | `except-no-log` | `except Exception as e: return handle_api_error(e)` |
| low | `backend/tools/mcp/mcp_observability.py:102` | `except-no-log` | `except Exception as e: return json.dumps({"error": f"Failed to fetch Sentry issu` |
| low | `backend/tools/mcp/mcp_observability.py:141` | `except-no-log` | `except Exception as e: return json.dumps({"error": f"Failed to read log file: {e` |
| low | `backend/tools/mcp/mcp_supabase.py:201` | `except-no-log` | `except Exception as e: return _handle_db_error(e)` |
| low | `backend/tools/mcp/mcp_supabase.py:277` | `except-no-log` | `except Exception as e: return _handle_db_error(e)` |
| low | `backend/tools/mcp/mcp_supabase.py:377` | `except-no-log` | `except Exception as e: return _handle_db_error(e)` |
| low | `backend/tools/mcp/mcp_supabase.py:439` | `except-no-log` | `except Exception as e: return _handle_db_error(e)` |
| low | `backend/tools/mcp/mcp_supabase.py:511` | `except-no-log` | `except Exception as e: return _handle_db_error(e)` |
| low | `backend/tools/mcp/mcp_supabase.py:589` | `except-no-log` | `except Exception as e: return _handle_db_error(e)` |
| low | `backend/tools/mcp/mcp_telegram.py:173` | `except-no-log` | `except Exception as exc: return json.dumps({"success": False, "error": str(exc)}` |
| low | `backend/tools/mcp/mcp_workspace.py:125` | `except-no-log` | `except FileExistsError: # Reduced sleep to minimize event-loop blocking in async` |
| low | `backend/tools/mcp/mcp_workspace.py:245` | `except-no-log` | `except (json.JSONDecodeError, OSError): workspace_path = Path("backend")` |
| low | `backend/tools/mcp/mcp_workspace.py:283` | `except-no-log` | `except ValueError: return json.dumps( { "error": "Invalid path", "message": "Pat` |
| low | `backend/tools/mcp/mcp_workspace.py:314` | `except-no-log` | `except (json.JSONDecodeError, OSError): workspace_path = _workspace_root / "back` |
| low | `backend/tools/mcp/mcp_workspace.py:356` | `except-no-log` | `except (json.JSONDecodeError, OSError): current_session = None` |
| low | `backend/tools/mcp/mcp_workspace.py:434` | `except-no-log` | `except Exception as e: return json.dumps({"error": f"Failed to read file: {e}"},` |
| low | `backend/tools/mcp/mcp_workspace.py:467` | `except-no-log` | `except Exception as e: return json.dumps({"error": f"Failed to write file: {e}"}` |
| low | `backend/tools/mcp/mcp_workspace.py:514` | `except-no-log` | `except Exception as e: return json.dumps({"error": f"Failed to search files: {e}` |
| low | `backend/tools/media/multilingual_tts.py:413` | `except-no-log` | `except Exception as exc: return {"status": "error", "error": str(exc)}` |
| low | `backend/tools/security_tools/vulnerability_predictor.py:13` | `except-no-log` | `except Exception: _PR_REVIEWER_AVAILABLE = False` |
| low | `backend/tools/security_tools/vulnerability_predictor.py:135` | `except-no-log` | `except SyntaxError: return findings` |
| low | `backend/tools/self_planner.py:59` | `except-no-log` | `except Exception:  class ModelRouter: pass` |
| low | `backend/tools/social/marketplace_agent.py:167` | `except-no-log` | `except subprocess.CalledProcessError as exc: return { "success": False, "tool_id` |
| low | `backend/tools/social/teldrive_storage.py:150` | `except-no-log` | `except Exception: get_db_session = None` |
| low | `backend/tools/social/telegram_bot.py:911` | `except-no-log` | `except Exception as exc: await self.send_message(chat_id, f"❌ <b>Execution Faile` |
| low | `backend/tools/social/telegram_bot.py:938` | `except-no-log` | `except Exception: status_lines.append("🔴 <b>Render Backend:</b> <code>Degraded/U` |
| low | `backend/tools/social/telegram_bot.py:951` | `except-no-log` | `except Exception as e: status_lines.append(f"⚪ <b>Database:</b> <code>{e}</code>` |
| low | `backend/tools/sso_integrator.py:163` | `except-no-log` | `except Exception: saml_xml = saml_response_raw` |
| low | `backend/tools/sso_integrator.py:366` | `except-no-log` | `except Exception as exc: return {"error": str(exc)}` |
| low | `backend/tools/sso_integrator.py:387` | `except-no-log` | `except Exception as exc: return {"status": "error", "message": str(exc)}` |
| low | `backend/verification/verifier.py:93` | `except-no-log` | `except SyntaxError as syn_err: err_msg = f"Python Syntax Error: {syn_err.msg} at` |
| low | `backend/worker_service.py:138` | `except-no-log` | `except Exception as exc:  # HTTP service must survive celery/redis failures _sta` |
| low | `backend/worker_service.py:236` | `except-no-log` | `except Exception as exc: _state.update(degraded=True, detail=f"task submit faile` |
| low | `backend/worker_service.py:276` | `except-no-log` | `except Exception as exc: _state.update(degraded=True, detail=f"queue stats faile` |
| low | `backend/worker_service.py:286` | `except-no-log` | `except Exception as exc: _state.update(degraded=True, detail=f"drain failed: {ex` |
| low | `packages/scripts/master_validator.py:125` | `except-no-log` | `except httpx.RequestError as e: self.errors.append(f"LLM Gateway unreachable: {e` |
| low | `packages/scripts/master_validator.py:145` | `except-no-log` | `except Exception as e:  # noqa: BLE001 - রেডিনেস চেক কখনোই ক্র্যাশ করা যাবে না s` |
| low | `scripts/advanced_analysis/agent_capability_registry_sync.py:188` | `except-no-log` | `except (SyntaxError, UnicodeDecodeError, OSError) as e: # বাংলা: পার্স ত্রুটি হল` |
| low | `scripts/advanced_analysis/agent_capability_registry_sync.py:198` | `except-no-log` | `except OSError: return agents` |
| low | `scripts/advanced_analysis/agent_loop_limiter_check.py:55` | `except-no-log` | `except Exception as e: issues.append(f"Error parsing {py_file}: {e}")` |
| low | `scripts/advanced_analysis/ai_memory_integrity_audit.py:36` | `except-no-log` | `except Exception as e: issues.append(f"Failed to parse memory_service.py: {e}") ` |
| low | `scripts/advanced_analysis/api_contract_diff.py:157` | `except-no-log` | `except SyntaxError: # বাংলা: AST parse ব্যর্থ হলে regex fallback ব্যবহার করি pat` |
| low | `scripts/advanced_analysis/api_contract_diff.py:424` | `except-no-log` | `except SyntaxError: # বাংলা: syntax error থাকলে regex fallback দিয়ে prefix বের ` |
| low | `scripts/advanced_analysis/bengali_i18n_completeness_checker.py:489` | `except-no-log` | `except Exception: return text.encode("ascii", errors="replace").decode("ascii")` |
| low | `scripts/advanced_analysis/circular_import_mapper.py:68` | `except-no-log` | `except ValueError: # ব্যাকএন্ডের বাইরে হলে রিপো রুট থেকে রিলেটিভ নেবো try: rel =` |
| low | `scripts/advanced_analysis/circular_import_mapper.py:72` | `except-no-log` | `except ValueError: return filepath.stem` |
| low | `scripts/advanced_analysis/circular_import_mapper.py:171` | `except-no-log` | `except Exception: import traceback traceback.print_exc()` |
| low | `scripts/advanced_analysis/circular_import_mapper.py:677` | `except-no-log` | `except ValueError: lines.append(f"- `{mod}` → `{fp}`")` |
| low | `scripts/advanced_analysis/config_single_source_enforcer.py:503` | `except-no-log` | `except ValueError: rel_path = str(file_path)` |
| low | `scripts/advanced_analysis/dead_code_verified_finder.py:69` | `except-no-log` | `except DiscoveryError: # বাংলা: রিপো পাওয়া যায়নি — run() এ fail-loud হবে, impo` |
| low | `scripts/advanced_analysis/dead_code_verified_finder.py:261` | `except-no-log` | `except ValueError: return filepath.stem` |
| low | `scripts/advanced_analysis/dead_code_verified_finder.py:495` | `except-no-log` | `except Exception: route_files = set()` |
| low | `scripts/advanced_analysis/dead_code_verified_finder.py:501` | `except-no-log` | `except Exception: core_roles = {}` |
| low | `scripts/advanced_analysis/dead_code_verified_finder.py:546` | `except-no-log` | `except Exception: core_roles = {}` |
| low | `scripts/advanced_analysis/dead_code_verified_finder.py:907` | `except-no-log` | `except DiscoveryError: origin = "repo: (auto-discovery failed — SCAN_ROOT fallba` |
| low | `scripts/advanced_analysis/dead_code_verified_finder.py:912` | `except-no-log` | `except Exception: role_names = "none"` |
| low | `scripts/advanced_analysis/duplicate_detector.py:340` | `except-no-log` | `except Exception: return funcs` |
| low | `scripts/advanced_analysis/duplicate_detector.py:358` | `except-no-log` | `except Exception: body_segments.append(ast.dump(stmt))` |
| low | `scripts/advanced_analysis/duplicate_logic_detector.py:280` | `except-no-log` | `except Exception: source = ast.dump(normalized)` |
| low | `scripts/advanced_analysis/duplicate_logic_detector.py:373` | `except-no-log` | `except SyntaxError as e: # সিনট্যাক্স ত্রুটি থাকলে এড়িয়ে যাওয়া return [], []` |
| low | `scripts/advanced_analysis/duplicate_logic_detector.py:964` | `except-no-log` | `except ValueError: return filepath` |
| low | `scripts/advanced_analysis/duplicate_logic_detector.py:1001` | `except-no-log` | `except Exception as _ig_err:  # pragma: no cover _ig_err_msg = str(_ig_err)` |
| low | `scripts/advanced_analysis/duplicate_logic_detector.py:1121` | `except-no-log` | `except Exception: parse_errors += 1` |
| low | `scripts/advanced_analysis/env_var_reconciler.py:287` | `except-no-log` | `except _pyyaml.composer.ComposerError: # বাংলা: multi-document YAML (যেমন k8s ma` |
| low | `scripts/advanced_analysis/env_var_reconciler.py:309` | `except-no-log` | `except Exception: return results` |
| low | `scripts/advanced_analysis/env_var_reconciler.py:372` | `except-no-log` | `except Exception: return vars_found` |
| low | `scripts/advanced_analysis/importer_graph.py:359` | `except-no-log` | `except SyntaxError as e: graph.parse_errors.append((filepath, f"SyntaxError: {e.` |
| low | `scripts/advanced_analysis/metrics_cardinality_auditor.py:68` | `except-no-log` | `except Exception as e: issues.append(f"Error checking {py_file}: {e}")` |
| low | `scripts/advanced_analysis/migration_safety_diff.py:220` | `except-no-log` | `except OSError as exc: result.errors.append(f"ফাইল পড়তে সমস্যা: {exc}") return ` |
| low | `scripts/advanced_analysis/migration_safety_diff.py:256` | `except-no-log` | `except SyntaxError as exc: result.errors.append(f"Python সিনট্যাক্স ত্রুটি: {exc` |
| low | `scripts/advanced_analysis/migration_safety_diff.py:697` | `except-no-log` | `except OSError as exc: result.errors.append(f"ফাইল পড়তে সমস্যা: {exc}") return ` |
| low | `scripts/advanced_analysis/migration_safety_diff.py:839` | `except-no-log` | `except OSError: return new_findings` |
| low | `scripts/advanced_analysis/orphan_route_finder.py:169` | `except-no-log` | `except (OSError, UnicodeDecodeError): return routes` |
| low | `scripts/advanced_analysis/orphan_route_finder.py:174` | `except-no-log` | `except SyntaxError: return _extract_routes_regex(source, rel_path, extra_prefix)` |
| low | `scripts/advanced_analysis/orphan_route_finder.py:264` | `except-no-log` | `except OSError: return registry` |
| low | `scripts/advanced_analysis/orphan_route_finder.py:268` | `except-no-log` | `except SyntaxError: return registry` |
| low | `scripts/advanced_analysis/orphan_route_finder.py:298` | `except-no-log` | `except OSError: return registry` |
| low | `scripts/advanced_analysis/orphan_route_finder.py:423` | `except-no-log` | `except ValueError: rel = file_path` |
| low | `scripts/advanced_analysis/orphan_route_finder.py:467` | `except-no-log` | `except (OSError, UnicodeDecodeError): return calls` |
| low | `scripts/advanced_analysis/pydantic_schema_consistency_checker.py:249` | `except-no-log` | `except (ValueError, TypeError): constraints[kw.arg] = ast.unparse(kw.value)` |
| low | `scripts/advanced_analysis/pydantic_schema_consistency_checker.py:429` | `except-no-log` | `except (ValueError, TypeError): status_code = None` |
| low | `scripts/advanced_analysis/pydantic_schema_consistency_checker.py:448` | `except-no-log` | `except (SyntaxError, UnicodeDecodeError): return routes` |
| low | `scripts/advanced_analysis/queue_health_checker.py:39` | `except-no-log` | `except Exception as e: issues.append(f"Error checking {py_file}: {e}")` |
| low | `scripts/advanced_analysis/secret_rotation_reminder.py:414` | `except-no-log` | `except OSError as exc: report.errors.append(f"secrets_registry.yaml পড়তে ত্রুটি` |
| low | `scripts/advanced_analysis/secret_rotation_reminder.py:429` | `except-no-log` | `except OSError as exc: report.errors.append(f"render.yaml পড়তে ত্রুটি: {exc}")` |
| low | `scripts/ai/feature_store_sync.py:208` | `except-no-log` | `except Exception as e: results.append(SyncResult( source=feature.source, destina` |
| low | `scripts/ai/feature_store_sync.py:248` | `except-no-log` | `except Exception as e: return SyncResult( source=feature.source, destination='re` |
| low | `scripts/ai/memory_write.py:97` | `except-no-log` | `except Exception: return f"Memory save at {datetime.now().isoformat()}", "genera` |
| low | `scripts/ai/prompt_injection_tester.py:173` | `except-no-log` | `except Exception as e: response = f"[Error calling LLM: {e!s}]"` |
| low | `scripts/audit_isolated_modules_and_capabilities.py:125` | `except-no-log` | `except OSError: texts[d] = ""` |
| low | `scripts/audit_isolated_modules_and_capabilities.py:149` | `except-no-log` | `except SyntaxError: tree = None` |
| low | `scripts/audit_isolated_modules_and_capabilities.py:293` | `except-no-log` | `except subprocess.TimeoutExpired: return False, "TimeoutExpired"` |
| low | `scripts/audit_observability.py:181` | `except-no-log` | `except Exception as e: safe_print(f"⚠️ Could not parse {py_file}: {e}")` |
| low | `scripts/backup/backup_telegram.py:445` | `except-no-log` | `except Exception: content = file_path.read_text(encoding="latin-1", errors="repl` |
| low | `scripts/backup/backup_telegram.py:453` | `except-no-log` | `except Exception as e: md_lines.append(f"### 📄 `{rel_path}`\n*(Error reading fil` |
| low | `scripts/backup/backup_telegram.py:482` | `except-no-log` | `except Exception: diff_target = None` |
| low | `scripts/backup/create_desktop_backup.py:181` | `except-no-log` | `except Exception: diff_target = None` |
| low | `scripts/backup/create_desktop_backup.py:287` | `except-no-log` | `except Exception: content = file_path.read_text(encoding="latin-1", errors="repl` |
| low | `scripts/backup/create_desktop_backup.py:294` | `except-no-log` | `except Exception as e: md_lines.append(f"### 📄 `{rel_path}`\n*(Error reading fil` |
| low | `scripts/backup/superai_backup_manager.py:643` | `except-no-log` | `except: all_data[key_str] = '[unable_to_retrieve]'` |
| low | `scripts/backup/superai_backup_manager.py:1036` | `except-no-log` | `except Exception as e: return {'valid': False, 'error': str(e)}` |
| low | `scripts/benchmark/superai_load_tester.py:344` | `except-no-log` | `except urllib.error.HTTPError as e: status_code = e.code response_size = 0 respo` |
| low | `scripts/benchmark/superai_load_tester.py:360` | `except-no-log` | `except Exception as e: end_perf = time.perf_counter() latency_ms = (end_perf - s` |
| low | `scripts/bots/auto_alert_bot.py:133` | `except-no-log` | `except requests.exceptions.RequestException as e: alert_msg = ( f"🏥 **System Hea` |
| low | `scripts/checkpoint_update.py:64` | `except-no-log` | `except Exception: return "  - (LESSONS_LEARNED.md not found)"` |
| low | `scripts/ci/check_hardcoded_deployment_config.py:146` | `except-no-log` | `except ValueError: warnings.append( f"[discovery] exception anchor {hits[0]} out` |
| low | `scripts/ci/check_hardcoded_deployment_config.py:208` | `except-no-log` | `except Exception as e: # Ignore binary files or unreadable files _ = e` |
| low | `scripts/ci/project_health_check.py:507` | `except-no-log` | `except SyntaxError as e: errors += 1 if errors <= 20:  # cap to avoid flood repo` |
| low | `scripts/ci/project_health_check.py:539` | `except-no-log` | `except Exception: is_tracked = False` |
| low | `scripts/ci/rate_limit_endpoint_checker.py:143` | `except-no-log` | `except Exception as e: issues.append(f"Error checking {py_file}: {e}")` |
| low | `scripts/ci/render_deploy_preflight.py:113` | `except-no-log` | `except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, json.JSONDe` |
| low | `scripts/ci/render_deploy_preflight.py:125` | `except-no-log` | `except json.JSONDecodeError: return {"status": "invalid", "path": str(path), "sh` |
| low | `scripts/ci/render_deploy_preflight.py:156` | `except-no-log` | `except (RuntimeError, urllib.error.URLError, urllib.error.HTTPError, TimeoutErro` |
| low | `scripts/ci/render_recheck_scheduler.py:38` | `except-no-log` | `except Exception: should_recheck = True` |
| low | `scripts/ci/rls_rbac_auditor.py:88` | `except-no-log` | `except Exception as e: issues.append(f"Error parsing {admin_routes_file}: {e}")` |
| low | `scripts/ci/security_headers_checker.py:45` | `except-no-log` | `except Exception as e: issues.append(f"Failed to parse {middleware_file}: {e}") ` |
| low | `scripts/ci/validate_frontend_build.py:59` | `except-no-log` | `except Exception as e: # Ignore binary files (like images, fonts) _ = e` |
| low | `scripts/ci/validate_router_imports.py:166` | `except-no-log` | `except Exception: exc_type, exc_val, _tb = sys.exc_info() last_line = traceback.` |
| low | `scripts/ci/verify_preflight_evidence.py:19` | `except-no-log` | `except (OSError, json.JSONDecodeError) as exc: return [f"invalid JSON: {exc}"]` |
| low | `scripts/ci/webhook_signature_checker.py:105` | `except-no-log` | `except Exception as e: all_issues.append(f"Error analyzing {py_file.relative_to(` |
| low | `scripts/db/ingest_knowledge.py:383` | `except-no-log` | `except Exception as e: errors.append(f"{src}: cannot parse JSON ({e})") per_file` |
| low | `scripts/db/verify_pgvector.py:115` | `except-no-log` | `except Exception: dsn = os.getenv("SUPABASE_DATABASE_URL_WRITER")` |
| low | `scripts/deploy/canary_deploy.py:283` | `except-no-log` | `except Exception: result.promoted = False` |
| low | `scripts/detect_silent_errors.py:70` | `except-no-log` | `except Exception:  # pragma: no cover - not all consoles support reconfigure _ =` |
| low | `scripts/detect_silent_errors.py:190` | `except-no-log` | `except Exception: return False, ""` |
| low | `scripts/detect_silent_errors.py:216` | `except-no-log` | `except SyntaxError as e: findings.append({ "file": rel(path), "line": getattr(e,` |
| low | `scripts/detect_silent_errors.py:223` | `except-no-log` | `except Exception as e: findings.append({ "file": rel(path), "line": 1, "type": "` |
| low | `scripts/detect_silent_errors.py:350` | `except-no-log` | `except Exception as e: return [{ "file": rel(path), "line": 1, "type": "unreadab` |
| low | `scripts/detect_silent_errors.py:453` | `except-no-log` | `except Exception as e: return [{ "file": rel(path), "line": 1, "type": "log-unre` |
| low | `scripts/devops/_audit.py:193` | `except-no-log` | `except Exception as e: verdict, note = ("UNVERIFIABLE", str(e))` |
| low | `scripts/devops/_audit.py:217` | `except-no-log` | `except Exception as e: inf_err = str(e)` |
| low | `scripts/devops/_audit.py:221` | `except-no-log` | `except Exception as e: gh_err = str(e)[:120]` |
| low | `scripts/devops/_audit.py:227` | `except-no-log` | `except Exception as e: render_err = str(e)[:120]` |
| low | `scripts/devops/bug_prophet.py:303` | `except-no-log` | `except SyntaxError as e: return [Issue("BP-SYNTAX", "ParseError", SEVERITY_CRITI` |
| low | `scripts/devops/cloud_watchman.py:177` | `except-no-log` | `except Exception: severity = "HIGH"` |
| low | `scripts/devops/cloud_watchman.py:317` | `except-no-log` | `except Exception as e: report.healthy = False report.anomalies.append(Anomaly("r` |
| low | `scripts/devops/cloud_watchman.py:349` | `except-no-log` | `except Exception as e: report.healthy = False report.anomalies.append(Anomaly("v` |
| low | `scripts/devops/cloud_watchman.py:399` | `except-no-log` | `except Exception as e: report.healthy = False report.anomalies.append(Anomaly("g` |
| low | `scripts/devops/config/validators.py:137` | `except-no-log` | `except Exception as e: self.add_result(ValidationResult( category=category_name,` |
| low | `scripts/devops/config/validators.py:597` | `except-no-log` | `except Exception as e: results.append(ValidationResult( category="nextjs", check` |
| low | `scripts/devops/devops_ai_scribe.py:569` | `except-no-log` | `except Exception as e: return f"Error: ChromaDB কালেকশন বা ক্লায়েন্ট লোড করতে ব` |
| low | `scripts/devops/devops_ai_scribe.py:578` | `except-no-log` | `except Exception as e: return f"Error: ChromaDB কোয়েরি করার সময় এরর ঘটেছে — {e}` |
| low | `scripts/devops/devops_ai_scribe.py:591` | `except-no-log` | `except Exception as e: return f"Error: LLM উত্তর তৈরি করতে ব্যর্থ হয়েছে — {e}"` |
| low | `scripts/devops/generate_modular_audits.py:495` | `except-no-log` | `except Exception:  # noqa: BLE001 return "  - *(git log unavailable)*"` |
| low | `scripts/devops/generate_modular_audits.py:515` | `except-no-log` | `except Exception:  # noqa: BLE001 mtime = "unknown"` |
| low | `scripts/devops/generate_modular_audits.py:520` | `except-no-log` | `except Exception as exc:  # noqa: BLE001 content = f"# Error reading file: {exc}` |
| low | `scripts/devops/refactor_wiz.py:250` | `except-no-log` | `except SyntaxError: return MetricsVisitor(), lines` |
| low | `scripts/devops/run_local_audit.py:47` | `except-no-log` | `except Exception as exc: return f"Ollama execution error: {exc}. Ensure Ollama i` |
| low | `scripts/devops/run_local_audit.py:75` | `except-no-log` | `except Exception as exc: return f"Gemini API execution error: {exc}"` |
| low | `scripts/devops/todo_manager.py:124` | `except-no-log` | `except ValueError: rel_path = str(filepath)` |
| low | `scripts/diagnostics/superai_console_detective.py:498` | `except-no-log` | `except json.JSONDecodeError: # Not valid JSON, treat as plain text return json_c` |
| low | `scripts/diagnostics/superai_console_detective.py:538` | `except-no-log` | `except Exception as e: return f"[ERROR] Failed to fetch URL: {e!s}"` |
| low | `scripts/evolution/auto_marketing_skill_forge.py:75` | `except-no-log` | `except ValueError: # Initialize with application default credentials cred = cred` |
| low | `scripts/evolution/auto_marketing_skill_forge.py:97` | `except-no-log` | `except ValueError: # Initialize with application default credentials cred = cred` |
| low | `scripts/find_stub_data.py:105` | `except-no-log` | `except Exception: return findings` |
| low | `scripts/generate_api_health_report.py:46` | `except-no-log` | `except UnicodeEncodeError: sys.stdout.buffer.write(report.encode("utf-8"))` |
| low | `scripts/generate_script_index.py:130` | `except-no-log` | `except SyntaxError as exc: return f"(unparseable — syntax error: {exc.msg})", "e` |
| low | `scripts/generate_script_index.py:132` | `except-no-log` | `except OSError as exc: return f"(unreadable: {exc})", "error"` |
| low | `scripts/generate_script_index.py:154` | `except-no-log` | `except OSError as exc: return f"(unreadable: {exc})", "error"` |
| low | `scripts/health/check_system_health.py:118` | `except-no-log` | `except Exception as exc:  # noqa: BLE001 last_error = str(exc)` |
| low | `scripts/health/check_system_health.py:140` | `except-no-log` | `except Exception as exc:  # noqa: BLE001 return False, f"Database query failed: ` |
| low | `scripts/health/check_system_health.py:159` | `except-no-log` | `except Exception as exc:  # noqa: BLE001 return False, f"Redis check failed: {ex` |
| low | `scripts/health/check_system_health.py:174` | `except-no-log` | `except Exception as exc:  # noqa: BLE001 return False, f"Dependency/env check er` |
| low | `scripts/health/superai_health_check.py:345` | `except-no-log` | `except Exception as e: self.add_result(HealthCheckResult( component=comp, check_` |
| low | `scripts/health/superai_health_check.py:362` | `except-no-log` | `except Exception as e: self.add_result(HealthCheckResult( component=comp, check_` |
| low | `scripts/health/superai_health_check.py:488` | `except-no-log` | `except Exception: results.append(HealthCheckResult( component="python", check_na` |
| low | `scripts/health/superai_health_check.py:678` | `except-no-log` | `except Exception as e: return HealthCheckResult( component="database", check_nam` |
| low | `scripts/health/superai_health_check.py:742` | `except-no-log` | `except Exception as e: return HealthCheckResult( component="redis", check_name="` |
| low | `scripts/health/superai_health_check.py:799` | `except-no-log` | `except Exception as e: results.append(HealthCheckResult( component="llm_provider` |
| low | `scripts/health/superai_health_check.py:861` | `except-no-log` | `except requests.exceptions.ConnectionError: results.append(HealthCheckResult( co` |
| low | `scripts/health/superai_health_check.py:868` | `except-no-log` | `except Exception as e: results.append(HealthCheckResult( component="backend_api"` |
| low | `scripts/health/superai_health_check.py:922` | `except-no-log` | `except: results.append(HealthCheckResult( component="frontend", check_name="Dev ` |
| low | `scripts/health/superai_health_check.py:1103` | `except-no-log` | `except DiscoveryError as exc: results.append(HealthCheckResult( component="patch` |
| low | `scripts/health/superai_health_check.py:1117` | `except-no-log` | `except OSError: ok = False` |
| low | `scripts/i18n/bangla_translator.py:250` | `except-break` | `except EOFError: break` |
| low | `scripts/lib/auto_discovery.py:89` | `except-no-log` | `except Exception: return str(p)` |
| low | `scripts/lib/auto_discovery.py:424` | `except-no-log` | `except (json.JSONDecodeError, KeyError, TypeError) as exc: disc.notes.append(f"R` |
| low | `scripts/monitoring/capacity_planner.py:248` | `except-no-log` | `except httpx.ConnectError: return -1.0, "unreachable"` |
| low | `scripts/monitoring/capacity_planner.py:250` | `except-no-log` | `except httpx.TimeoutException: return -1.0, "timeout"` |
| low | `scripts/monitoring/superai_log_analyzer.py:585` | `except-no-log` | `except Exception: file_positions[filepath] = 0` |
| low | `scripts/multi_model_validator.py:160` | `except-no-log` | `except json.JSONDecodeError: parsed = {"raw_response": response_text, "risk_leve` |
| low | `scripts/multi_model_validator.py:164` | `except-no-log` | `except Exception as e: return {"model": model, "validator_type": validator_type,` |
| low | `scripts/pre_merge_guard.py:248` | `except-no-log` | `except FileNotFoundError: return 127, f"command not found: {cmd[0]}"` |
| low | `scripts/pre_merge_guard.py:250` | `except-no-log` | `except subprocess.TimeoutExpired: return 124, f"timeout after {timeout}s: {' '.j` |
| low | `scripts/pre_merge_guard.py:263` | `except-no-log` | `except urllib.error.HTTPError as e: return e.code, {k.lower(): v for k, v in e.h` |
| low | `scripts/pre_merge_guard.py:265` | `except-no-log` | `except Exception as e:  # noqa: BLE001 - probe must never crash the guard return` |
| low | `scripts/pre_merge_guard.py:273` | `except-no-log` | `except Exception as e:  # noqa: BLE001 return CheckResult(name, group, "ERROR", ` |
| low | `scripts/pre_merge_guard.py:298` | `except-no-log` | `except ValueError: return str(p)` |
| low | `scripts/pre_merge_guard.py:310` | `except-no-log` | `except json.JSONDecodeError: return [x.strip().strip("'\"").rstrip("/") for x in` |
| low | `scripts/pre_merge_guard.py:559` | `except-no-log` | `except Exception as e:  # noqa: BLE001 findings.append(Finding("live-bundles", "` |
| low | `scripts/pre_merge_guard.py:605` | `except-no-log` | `except Exception:  # noqa: BLE001 return ("FAIL" if rc else "PASS"), [], 0 if rc` |
| low | `scripts/pre_merge_guard.py:629` | `except-no-log` | `except Exception:  # noqa: BLE001 return ("FAIL" if rc else "PASS"), [], 0, out[` |
| low | `scripts/quality/check_ollama_test_coverage.py:125` | `except-no-log` | `except Exception as exc: return {"returncode": -1, "passed": False, "error": str` |
| low | `scripts/quality/check_ollama_test_coverage.py:138` | `except-no-log` | `except Exception as exc: # বাংলা: coverage XML পার্স ব্যর্থ হলে চুপচাপ 0.0% না দ` |
| low | `scripts/quality/check_ollama_test_coverage.py:187` | `except-no-log` | `except Exception as exc: bprint(f"  ❌ সার্ভারে কানেক্ট hologram না: {exc}", RED)` |
| low | `scripts/quality/check_ollama_test_coverage.py:201` | `except-no-log` | `except httpx.HTTPStatusError as exc: bprint(f"  ❌ HTTP এরর: {exc.response.status` |
| low | `scripts/quality/check_ollama_test_coverage.py:204` | `except-no-log` | `except httpx.TimeoutException: bprint("  ❌ request টাইমআউট (120s)", RED) return ` |
| low | `scripts/quality/check_ollama_test_coverage.py:207` | `except-no-log` | `except Exception as exc: bprint(f"  ❌ এরর: {exc}", RED) return 1` |
| low | `scripts/quality/check_ollama_test_coverage.py:270` | `except-no-log` | `except KeyboardInterrupt: bprint("\n⏹ interrupted.", YELLOW) return 130` |
| low | `scripts/quality/regression_scanner.py:49` | `except-no-log` | `except Exception as e: _ = e` |
| low | `scripts/quality/self_audit_scan.py:88` | `except-no-log` | `except SyntaxError as e: syntax_errors.append(f"{rel}: {e}") continue` |
| low | `scripts/refactor/superai_transform.py:256` | `except-no-log` | `except subprocess.TimeoutExpired: return False, "", "Command timed out"` |
| low | `scripts/refactor/superai_transform.py:258` | `except-no-log` | `except Exception as e: return False, "", str(e)` |
| low | `scripts/safety_guard.py:308` | `except-no-log` | `except ValueError: file_ref = str(self.approval_requests)` |
| low | `scripts/security/audit_log_analyzer.py:161` | `except-no-log` | `except Exception: return datetime.now(timezone.utc)` |
| low | `scripts/security/secrets_rotation_manager.py:520` | `except-no-log` | `except Exception: return datetime.now(timezone.utc)` |
| low | `tools/discovery_fabric/supremeai_discovery/source_scout.py:143` | `except-no-log` | `except Exception: return 0.35` |
| low | `tools/discovery_fabric/supremeai_discovery/source_scout.py:171` | `except-no-log` | `except Exception as exc: providers.append(Candidate(source=fn.__name__, title="S` |
| low | `tools/discovery_fabric/supremeai_discovery/trust_engine.py:32` | `except-no-log` | `except Exception: return 0.4` |
| low | `tools/gap_finder/helpers.py:15` | `except-no-log` | `except ValueError: return path.as_posix()` |
| low | `tools/gap_finder/scanner.py:1105` | `except-no-log` | `except Exception: return set()` |
| low | `tools/gap_miner/tools/context_packager.py:24` | `except-no-log` | `except OSError: _ = None` |
| low | `tools/gap_miner/tools/drift_detector.py:20` | `except-no-log` | `except OSError: _ = None` |
| low | `tools/gap_miner/tools/gap_miner.py:57` | `except-no-log` | `except OSError: _ = None` |
| low | `tools/gap_miner/tools/gap_miner.py:65` | `except-no-log` | `except OSError: s = ""` |
| low | `tools/gap_miner/tools/gap_miner.py:87` | `except-no-log` | `except OSError: _ = None` |
| low | `tools/gap_miner/tools/project_fingerprint.py:33` | `except-no-log` | `except OSError:t=''` |
| low | `tools/intelligence_extensions/supremeai_intelligence/autonomous_red_team.py:16` | `except-no-log` | `except Exception as e:return {'campaign':c,'passed':False,'severity':'unknown','` |
| low | `tools/intelligence_extensions/supremeai_intelligence/execution_verifier.py:15` | `except-no-log` | `except SyntaxError as e:return ExecutionReport(False,[{'name':'ast','passed':Fal` |
| low | `tools/intelligence_extensions/supremeai_intelligence/execution_verifier.py:26` | `except-no-log` | `except Exception as e: out.append({'passed':False,'error':str(e)})` |
| low | `tools/intelligence_extensions/supremeai_intelligence/knowledge_revalidator.py:19` | `except-no-log` | `except Exception as e:return {'memory_id':m.get('memory_id'),'status':'error','e` |
| low | `tools/intelligence_extensions/supremeai_intelligence/pipeline.py:128` | `except-no-log` | `except Exception as e: reasons.append(f'Cache optimization monitoring: {e}')` |
| low | `tools/knowledge/injector.py:77` | `except-no-log` | `except Exception: stored_meta = {}` |
| low | `tools/knowledge/injector.py:121` | `except-no-log` | `except Exception as exc: status = f"FAILED: {exc}" results["failed"] += 1` |
| low | `tools/knowledge/injector.py:153` | `except-no-log` | `except Exception as exc: recall_results.append({"query": query, "result": str(ex` |
| low | `tools/knowledge_squeezer/knowledge_squeezer/engine.py:54` | `except-no-log` | `except Exception as exc: last_error = exc if attempt < self.config.max_retries: ` |
| low | `tools/knowledge_squeezer/knowledge_squeezer/engine.py:199` | `except-no-log` | `except json.JSONDecodeError: data = { "title": topic, "claim": "Synthesis could ` |
| low | `tools/pipeline_recipe_compiler.py:407` | `except-no-log` | `except Exception as exc: status = f"FAILED: {exc}" results["failed"] += 1` |
| low | `tools/solution_synthesizer/tools/solution_synthesizer.py:144` | `except-no-log` | `except subprocess.TimeoutExpired as exc: return 124, f"TIMEOUT: {exc}"` |
| low | `tools/solution_synthesizer/tools/solution_synthesizer.py:335` | `except-no-log` | `except Exception as exc: report["attempts"].append({"attempt": attempt, "error":` |
| low | `tools/vscode-extension/test/supremeai-service.test.ts:57` | `floating-fetch` | `    axios.mockReset();` |
| low | `tools/vscode-extension/test/supremeai-service.test.ts:94` | `floating-fetch` | `      axios.post.mockResolvedValueOnce({` |
| low | `tools/vscode-extension/test/supremeai-service.test.ts:122` | `floating-fetch` | `      axios.post.mockRejectedValueOnce(new Error('Network error'));` |
| low | `tools/vscode-extension/test/supremeai-service.test.ts:161` | `floating-fetch` | `      axios.post.mockResolvedValueOnce({` |
| low | `tools/vscode-extension/test/supremeai-service.test.ts:185` | `floating-fetch` | `      axios.post.mockResolvedValueOnce({` |
| low | `tools/vscode-extension/test/supremeai-service.test.ts:209` | `floating-fetch` | `      axios.post.mockResolvedValueOnce({` |
| low | `tools/vscode-extension/test/supremeai-service.test.ts:243` | `floating-fetch` | `      axios.post.mockRejectedValueOnce(new Error('Server error'));` |

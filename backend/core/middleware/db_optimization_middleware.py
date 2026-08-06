"""Database Optimization Middleware integrating all Phase 3 improvements."""

import asyncio
import time
from collections.abc import Callable
from typing import Any

from core.database.query_optimizer import (DatabaseOptimizationMiddleware,
                                           query_optimizer,
                                           setup_query_profiling)
from core.logging_config import logger
from core.memory.memory_manager import memory_manager, track_memory_usage
from core.security.secret_scanner import secret_scanner
from core.security.sql_injection_guard import sql_injection_middleware
from fastapi import Request, Response
from sqlalchemy.ext.asyncio import AsyncSession


class ComprehensiveDBOptimizationMiddleware:
    """Comprehensive middleware that integrates all database optimization features."""

    def __init__(self):
        self.query_optimizer_middleware = DatabaseOptimizationMiddleware(
            query_optimizer
        )
        self.sql_guard_middleware = sql_injection_middleware
        self.request_start_time = None
        self.query_count_before = 0

    async def __call__(self, request: Request, call_next: Callable) -> Response:
        """Execute the middleware logic."""
        self.request_start_time = time.time()

        # Track initial state
        self.query_count_before = len(query_optimizer.analyzer.queries)

        # Perform pre-request optimizations
        await self._pre_request_optimizations(request)

        try:
            # Process the request
            response = await call_next(request)

            # Perform post-request optimizations and analysis
            await self._post_request_analysis(request, response)

            return response
        except Exception as e:
            # Handle errors appropriately
            await self._handle_error(request, e)
            raise

    async def _pre_request_optimizations(self, request: Request):
        """Perform optimizations before request processing."""
        # Validate request parameters for potential SQL injection
        try:
            # Check query parameters
            query_params = dict(request.query_params)
            await self.sql_guard_middleware.validate_request_params(query_params)

            # Check form data if present
            if request.method in ["POST", "PUT", "PATCH"]:
                try:
                    form_data = await request.form()
                    form_dict = dict(form_data)
                    await self.sql_guard_middleware.validate_request_params(form_dict)
                except Exception as _form_err:
                    logger.debug(
                        f"Form parsing skipped for non-form request: {_form_err}"
                    )

        except ValueError as e:
            logger.warning(f"SQL injection attempt detected: {e!s}")
            # In a real implementation, we'd return an error response here
            raise

    async def _post_request_analysis(self, request: Request, response: Response):
        """Analyze and optimize after request processing."""
        # Calculate request duration
        request_duration = time.time() - self.request_start_time

        # Check for N+1 queries
        queries_during_request = (
            len(query_optimizer.analyzer.queries) - self.query_count_before
        )

        if queries_during_request > 10:  # Threshold for concern
            n_plus_one_warnings = query_optimizer.analyzer.get_n_plus_one_warnings()
            if n_plus_one_warnings:
                logger.warning(
                    f"Potential N+1 issue in request {request.url}: {n_plus_one_warnings}"
                )

                # Add performance header to response
                response.headers["X-Performance-Warning"] = (
                    "Potential N+1 query detected"
                )

        # Log performance metrics
        logger.debug(
            f"Request {request.url} completed in {request_duration:.3f}s "
            f"with {queries_during_request} queries"
        )

        # Take memory snapshot if significant activity occurred
        if queries_during_request > 5 or request_duration > 1.0:
            memory_manager.take_memory_snapshot(
                f"post_request_{request.url.path.split('/')[-1]}"
            )

        # Update middleware statistics
        self.sql_guard_middleware.request_counter += 1

    async def _handle_error(self, request: Request, error: Exception):
        """Handle errors during request processing."""
        request_duration = time.time() - self.request_start_time

        logger.error(
            f"Request {request.url} failed after {request_duration:.3f}s: {error!s}"
        )

        # Update error statistics
        self.sql_guard_middleware.blocked_requests += 1


# Integration with application startup
async def initialize_db_optimizations(engine):
    """Initialize all database optimizations when the application starts."""
    logger.info("Initializing database optimizations...")

    # Setup query profiling on the database engine
    setup_query_profiling(engine)

    # Register common eager loading strategies to prevent N+1
    _register_common_eager_load_strategies()

    # Start memory management background tasks
    memory_manager.start_background_cleanup()

    # Start secret scanning background tasks if needed
    # (In a real app, this would scan files periodically)

    logger.info("Database optimizations initialized successfully")


def _register_common_eager_load_strategies():
    """Register common eager loading strategies to prevent N+1 queries."""
    from models.agent_session import AgentSession
    from models.execution_policy import ExecutionPolicy
    from models.patch_telemetry import PatchTelemetry

    # Register relationships that are commonly accessed together
    query_optimizer.register_eager_load_strategy(
        AgentSession, ["handoff_events"], strategy="selectinload"
    )

    query_optimizer.register_eager_load_strategy(
        PatchTelemetry, [], strategy="selectinload"
    )

    query_optimizer.register_eager_load_strategy(
        ExecutionPolicy, [], strategy="selectinload"
    )


def get_optimization_stats() -> dict[str, Any]:
    """Get comprehensive optimization statistics."""
    return {
        "query_optimizer": {
            "total_queries": len(query_optimizer.analyzer.queries),
            "n_plus_one_warnings": len(
                query_optimizer.analyzer.get_n_plus_one_warnings()
            ),
            "cache_stats": query_optimizer.optimization_cache.stats(),
        },
        "memory_manager": memory_manager.get_memory_stats(),
        "sql_injection_middleware": sql_injection_middleware.get_stats(),
        "timestamp": time.time(),
    }


def run_comprehensive_security_scan():
    """Run a comprehensive security scan of the codebase."""

    async def scan():
        # Run secret scanning
        scan_results = await secret_scanner.scan_directory(".", check_git_history=False)
        logger.info(f"Security scan completed. Status: {scan_results['status']}")

        # Return the results
        return scan_results

    # Note: In a real application, you might want to run this in a separate thread
    # or as part of a scheduled task
    try:
        from core.utils.background_tasks import track_task

        loop = asyncio.get_running_loop()
        # If we're already in an event loop, schedule the task
        track_task(loop.create_task(scan()))
        # We can't await here since we don't know if called from sync context
    except RuntimeError:
        # No event loop running, run it normally
        results = asyncio.run(scan())
        return results


# Performance monitoring decorator that combines multiple optimization techniques
def monitor_performance(func):
    """Decorator that applies multiple performance optimizations."""

    async def wrapper(*args, **kwargs):
        # Apply memory tracking
        with track_memory_usage(func):
            start_time = time.time()

            try:
                result = await func(*args, **kwargs)

                execution_time = time.time() - start_time

                # Log performance metrics
                logger.debug(
                    f"Function {func.__name__} executed in {execution_time:.3f}s"
                )

                # Take a memory snapshot if the function took a long time
                if execution_time > 1.0:
                    memory_manager.take_memory_snapshot(
                        f"slow_function_{func.__name__}"
                    )

                return result
            except Exception as e:
                execution_time = time.time() - start_time
                logger.error(
                    f"Function {func.__name__} failed after {execution_time:.3f}s: {e!s}"
                )
                raise

    return wrapper


# Utility function to optimize a database session
async def optimize_session_queries(session: AsyncSession):
    """Apply optimization strategies to a database session."""
    # This would apply various optimization techniques to the session
    # In practice, this would customize the session with appropriate loading strategies

    # For now, we'll just return the session as-is, but in a real implementation
    # this would set up eager loading and other optimizations
    return session


# Example of how to integrate the middleware in a FastAPI app
def integrate_with_fastapi_app(app, engine):
    """Integrate all optimizations with a FastAPI application."""

    # Add the comprehensive middleware
    app.add_middleware(ComprehensiveDBOptimizationMiddleware)

    # Initialize all optimizations

    try:
        # Try to run in current event loop if available
        from core.utils.background_tasks import track_task

        loop = asyncio.get_running_loop()
        track_task(loop.create_task(initialize_db_optimizations(engine)))
    except RuntimeError:
        # No event loop, run synchronously
        asyncio.run(initialize_db_optimizations(engine))

    # Add a route to get optimization stats
    @app.get("/admin/performance-stats")
    async def get_performance_stats():
        return get_optimization_stats()


# Global instance
comprehensive_db_middleware = ComprehensiveDBOptimizationMiddleware()

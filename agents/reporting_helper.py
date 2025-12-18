"""
Reporting Helper - Utility functions for agents to easily report activities

This module provides convenient decorators and context managers for agents
to automatically report their activities.
"""

from typing import Callable, Any, Optional
from functools import wraps
import time
from contextlib import contextmanager

try:
    from .reporting_service import get_reporting_service
    REPORTING_AVAILABLE = True
except ImportError:
    REPORTING_AVAILABLE = False


def report_task(agent_name: str, task_type: str = "general"):
    """
    Decorator to automatically report task execution.
    
    Usage:
        @report_task("MyAgent", "testing")
        def run_test():
            # test code
            return result
    """
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            if not REPORTING_AVAILABLE:
                return func(*args, **kwargs)
            
            start_time = time.time()
            success = False
            result = None
            error = None
            
            try:
                result = func(*args, **kwargs)
                success = True
                return result
            except Exception as e:
                error = str(e)
                raise
            finally:
                duration_ms = (time.time() - start_time) * 1000
                try:
                    reporting_service = get_reporting_service()
                    reporting_service.log_task_execution(
                        agent_name=agent_name,
                        task=f"{func.__name__}({', '.join(map(str, args))})",
                        task_type=task_type,
                        success=success,
                        duration_ms=duration_ms,
                        result=result,
                        error=error
                    )
                except Exception:
                    pass  # Don't fail if reporting fails
        
        return wrapper
    return decorator


@contextmanager
def report_activity(agent_name: str, activity_type: str, description: str, **metadata):
    """
    Context manager to report an activity.
    
    Usage:
        with report_activity("MyAgent", "file_read", "Reading config.json"):
            data = read_file("config.json")
    """
    if REPORTING_AVAILABLE:
        try:
            reporting_service = get_reporting_service()
            reporting_service.log_activity(
                agent_name=agent_name,
                activity_type=activity_type,
                description=f"Started: {description}",
                **metadata
            )
        except Exception:
            pass
    
    start_time = time.time()
    success = False
    error = None
    
    try:
        yield
        success = True
    except Exception as e:
        error = str(e)
        raise
    finally:
        if REPORTING_AVAILABLE:
            duration_ms = (time.time() - start_time) * 1000
            try:
                reporting_service = get_reporting_service()
                reporting_service.log_activity(
                    agent_name=agent_name,
                    activity_type=activity_type,
                    description=f"Completed: {description} ({'success' if success else 'failed'})",
                    success=success,
                    duration_ms=duration_ms,
                    error=error,
                    **metadata
                )
            except Exception:
                pass


def report_information_source(
    agent_name: str,
    source_type: str,
    source_description: str,
    information: Optional[str] = None
):
    """
    Helper function to report an information source.
    
    Usage:
        report_information_source(
            "MyAgent",
            "file",
            "config.json",
            "Configuration loaded successfully"
        )
    """
    if REPORTING_AVAILABLE:
        try:
            reporting_service = get_reporting_service()
            reporting_service.log_information_source(
                agent_name=agent_name,
                source_type=source_type,
                source_description=source_description,
                information=information
            )
        except Exception:
            pass


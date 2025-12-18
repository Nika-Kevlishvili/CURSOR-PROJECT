"""
AI Response Logger - Logs AI assistant responses and creates reports

This module provides functionality to log when the AI assistant responds to user queries,
including which expert/agent was used to provide the answer.
"""

from typing import Optional, List, Dict, Any
from datetime import datetime
import time

try:
    from .reporting_service import get_reporting_service
    REPORTING_AVAILABLE = True
except ImportError:
    REPORTING_AVAILABLE = False


def log_ai_response(
    user_query: str,
    expert_name: Optional[str] = None,
    agents_used: Optional[List[str]] = None,
    response_summary: Optional[str] = None,
    success: bool = True,
    duration_ms: Optional[float] = None
) -> bool:
    """
    Log an AI assistant response to a user query.
    
    This function logs which expert/agent was used to answer the user's question
    and creates a report entry.
    
    Args:
        user_query: The user's question/query
        expert_name: Name of the expert/agent that provided the answer (if single)
        agents_used: List of agent names used (if multiple)
        response_summary: Brief summary of the response
        success: Whether the response was successful
        duration_ms: Duration in milliseconds (optional)
        
    Returns:
        True if logging was successful, False otherwise
    """
    # #region agent log
    import json
    try:
        with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
            f.write(json.dumps({'id': 'log_ai_response_called', 'timestamp': __import__('time').time() * 1000, 'location': 'ai_response_logger.py:19', 'message': 'log_ai_response called', 'data': {'expert_name': expert_name, 'agents_used': agents_used, 'has_summary': bool(response_summary)}, 'sessionId': 'debug-session', 'runId': 'run1', 'hypothesisId': 'C'}) + '\n')
    except: pass
    # #endregion
    if not REPORTING_AVAILABLE:
        return False
    
    try:
        reporting_service = get_reporting_service()
        
        # Determine which agents were involved
        if agents_used:
            agent_list = agents_used
        elif expert_name:
            agent_list = [expert_name]
        else:
            agent_list = ["AI Assistant (direct)"]
        
        # Log activity for each agent used
        for agent_name in agent_list:
            activity_description = f"Answered user query: {user_query[:100]}..."
            if response_summary:
                activity_description += f" Response: {response_summary[:100]}..."
            
            reporting_service.log_activity(
                agent_name=agent_name,
                activity_type="user_query_response",
                description=activity_description,
                user_query=user_query[:500],  # Truncate long queries
                response_summary=response_summary[:500] if response_summary else None,
                success=success,
                duration_ms=duration_ms,
                agents_involved=agent_list
            )
        
        # Rule 0.6: MANDATORY Report Generation After Task Completion
        # Automatically save reports after logging AI response
        # #region agent log
        import json
        try:
            with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                f.write(json.dumps({'id': 'log_ai_response_auto_report', 'timestamp': __import__('time').time() * 1000, 'location': 'ai_response_logger.py:75', 'message': 'auto saving reports after log_ai_response', 'data': {'agent_list': agent_list}, 'sessionId': 'debug-session', 'runId': 'run1', 'hypothesisId': 'C'}) + '\n')
        except: pass
        # #endregion
        try:
            for agent_name in agent_list:
                try:
                    reporting_service.save_agent_report(agent_name)
                except Exception as e:
                    print(f"AIResponseLogger: ⚠ Failed to save report for {agent_name}: {str(e)}")
            reporting_service.save_summary_report()
        except Exception as e:
            print(f"AIResponseLogger: ⚠ Failed to auto-save reports: {str(e)}")
        
        return True
    except Exception as e:
        print(f"AIResponseLogger: Failed to log response: {str(e)}")
        return False


def log_expert_consultation(
    user_query: str,
    expert_name: str,
    consultation_result: Dict[str, Any],
    duration_ms: Optional[float] = None
) -> bool:
    """
    Log a consultation with an expert agent.
    
    Args:
        user_query: The user's question/query
        expert_name: Name of the expert consulted
        consultation_result: Result from the expert consultation
        duration_ms: Duration in milliseconds
        
    Returns:
        True if logging was successful, False otherwise
    """
    if not REPORTING_AVAILABLE:
        return False
    
    try:
        reporting_service = get_reporting_service()
        
        success = consultation_result.get('success', False)
        response = consultation_result.get('response', {})
        
        reporting_service.log_consultation(
            from_agent="AI Assistant",
            to_agent=expert_name,
            query=user_query,
            success=success,
            duration_ms=duration_ms or 0,
            response=response
        )
        
        # Also log as activity
        reporting_service.log_activity(
            agent_name="AI Assistant",
            activity_type="expert_consultation",
            description=f"Consulted {expert_name} for user query: {user_query[:100]}...",
            expert_name=expert_name,
            success=success,
            duration_ms=duration_ms
        )
        
        return True
    except Exception as e:
        print(f"AIResponseLogger: Failed to log consultation: {str(e)}")
        return False


def ensure_reports_saved(agents_used: Optional[List[str]] = None) -> bool:
    """
    Ensure reports are saved for all agents involved (Rule 0.6 compliance).
    
    This function should be called by Cursor AI after any task/answer/interaction
    to ensure reports are automatically generated.
    
    Args:
        agents_used: List of agent names that were used (if None, saves reports for all agents with activities)
        
    Returns:
        True if reports were saved successfully, False otherwise
    """
    if not REPORTING_AVAILABLE:
        return False
    
    try:
        reporting_service = get_reporting_service()
        
        # #region agent log
        import json
        try:
            with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                f.write(json.dumps({'id': 'log_ensure_reports', 'timestamp': __import__('time').time() * 1000, 'location': 'ai_response_logger.py:133', 'message': 'ensure_reports_saved called', 'data': {'agents_used': agents_used}, 'sessionId': 'debug-session', 'runId': 'run2', 'hypothesisId': 'F'}) + '\n')
        except: pass
        # #endregion
        
        if agents_used:
            # Save reports for specified agents
            for agent_name in agents_used:
                try:
                    reporting_service.save_agent_report(agent_name)
                except Exception as e:
                    print(f"AIResponseLogger: ⚠ Failed to save report for {agent_name}: {str(e)}")
        else:
            # Save reports for all agents with activities
            all_agents = set()
            all_agents.update(reporting_service.agent_communications.keys())
            all_agents.update(reporting_service.information_sources.keys())
            all_agents.update(reporting_service.task_executions.keys())
            all_agents.update([a.agent_name for a in reporting_service.activities])
            
            for agent_name in all_agents:
                try:
                    reporting_service.save_agent_report(agent_name)
                except Exception as e:
                    print(f"AIResponseLogger: ⚠ Failed to save report for {agent_name}: {str(e)}")
        
        # Always save summary report
        try:
            reporting_service.save_summary_report()
            print("AIResponseLogger: ✓ Reports automatically saved (Rule 0.6 compliance)")
            return True
        except Exception as e:
            print(f"AIResponseLogger: ⚠ Failed to save summary report: {str(e)}")
            return False
            
    except Exception as e:
        print(f"AIResponseLogger: Failed to ensure reports saved: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


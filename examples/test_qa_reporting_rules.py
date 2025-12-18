#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Test script to check Q&A mode, reporting, and rules compliance
"""
import sys
import time
from pathlib import Path

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents.phoenix_expert import get_phoenix_expert
from agents.ai_response_logger import log_ai_response
from agents.reporting_service import get_reporting_service
from agents.agent_router import get_agent_router

def main():
    question = "როგორ მუშაობს customer creation?"
    
    print("=== Testing Q&A Mode, Reporting, and Rules ===")
    print(f"Question: {question}\n")
    
    # Test 1: Direct PhoenixExpert call
    print("--- Test 1: Direct PhoenixExpert.answer_question() ---")
    start_time = time.time()
    expert = get_phoenix_expert()
    result = expert.answer_question(question)
    duration_ms = (time.time() - start_time) * 1000
    
    print(f"Answer: {result.get('answer', 'No answer')[:200]}...")
    print(f"Duration: {duration_ms:.2f}ms")
    
    # Test 2: Check if log_ai_response is called
    print("\n--- Test 2: Calling log_ai_response() ---")
    log_ai_response(
        user_query=question,
        expert_name="PhoenixExpert",
        response_summary=result.get('answer', '')[:200],
        success=True,
        duration_ms=duration_ms
    )
    print("log_ai_response() called")
    
    # Test 3: Check if reports are saved
    print("\n--- Test 3: Checking report generation ---")
    reporting_service = get_reporting_service()
    print(f"Reporting service available: {reporting_service is not None}")
    
    # Check if reports directory exists
    reports_dir = reporting_service.reports_dir
    print(f"Reports directory: {reports_dir}")
    
    # Test 4: AgentRouter routing
    print("\n--- Test 4: AgentRouter.route_query() ---")
    router = get_agent_router()
    routing_result = router.route_query(question)
    print(f"Routing success: {routing_result.get('success', False)}")
    print(f"Agents used: {routing_result.get('agents_used', [])}")
    
    print("\n=== Test Complete ===")
    print("Check debug.log for instrumentation logs")
    
    return {
        'qa_result': result,
        'routing_result': routing_result,
        'reports_dir': str(reports_dir)
    }

if __name__ == "__main__":
    main()


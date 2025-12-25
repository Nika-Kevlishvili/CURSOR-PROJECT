#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Temporary script to query PhoenixExpert and create report
"""
import sys
import time
from pathlib import Path

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent))

from agents.phoenix_expert import get_phoenix_expert
from agents.ai_response_logger import log_ai_response
from agents.reporting_service import get_reporting_service

def main():
    question = "Brief business idea of Phoenix project"
    
    print("=== Querying PhoenixExpert ===")
    print(f"Question: {question}\n")
    
    # Get PhoenixExpert and answer question
    start_time = time.time()
    expert = get_phoenix_expert()
    result = expert.answer_question(question)
    duration_ms = (time.time() - start_time) * 1000
    
    print("=== PhoenixExpert Response ===")
    print(result.get('answer', 'No answer'))
    print(f"\n=== Sources Found ===")
    sources = result.get('sources', {})
    print(f"Code files: {len(sources.get('code', []))}")
    print(f"Classes: {len(sources.get('classes', []))}")
    print(f"Controllers: {len(sources.get('controllers', []))}")
    print(f"Services: {len(sources.get('services', []))}")
    print(f"Confluence pages: {len(sources.get('confluence', []))}")
    print(f"\nDuration: {duration_ms:.2f}ms")
    
    # Log AI response
    print("\n=== Logging AI Response ===")
    log_ai_response(
        user_query=question,
        expert_name="PhoenixExpert",
        response_summary="Phoenix project business idea explained",
        success=True,
        duration_ms=duration_ms
    )
    
    # Save report
    print("\n=== Saving Report ===")
    reporting_service = get_reporting_service()
    reporting_service.save_agent_report("PhoenixExpert")
    reporting_service.save_summary_report()
    print("Reports saved successfully!")
    
    return result

if __name__ == "__main__":
    main()


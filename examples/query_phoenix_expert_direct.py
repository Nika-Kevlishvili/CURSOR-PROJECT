#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Direct script to query PhoenixExpert avoiding import issues
"""
import sys
import time
import json
from pathlib import Path

# Add agents directory to path
agents_path = Path(__file__).parent / "agents"
sys.path.insert(0, str(agents_path.parent))

# Import PhoenixExpert directly
try:
    from agents.phoenix_expert import PhoenixExpert, get_phoenix_expert
except ImportError as e:
    print(f"Import error: {e}")
    # Try direct import
    import importlib.util
    spec = importlib.util.spec_from_file_location("phoenix_expert", agents_path / "phoenix_expert.py")
    phoenix_expert_module = importlib.util.module_from_spec(spec)
    sys.modules["phoenix_expert"] = phoenix_expert_module
    spec.loader.exec_module(phoenix_expert_module)
    PhoenixExpert = phoenix_expert_module.PhoenixExpert
    get_phoenix_expert = phoenix_expert_module.get_phoenix_expert

def main():
    question = "ფენიქსის პროექტის მოკლე ბიზნეს იდეა"
    
    print("=== Querying PhoenixExpert ===")
    print(f"Question: {question}\n")
    
    try:
        # Get PhoenixExpert and answer question
        start_time = time.time()
        expert = get_phoenix_expert()
        result = expert.answer_question(question)
        duration_ms = (time.time() - start_time) * 1000
        
        print("=== PhoenixExpert Response ===")
        answer = result.get('answer', 'No answer')
        print(answer)
        
        print(f"\n=== Sources Found ===")
        sources = result.get('sources', {})
        print(f"Code files: {len(sources.get('code', []))}")
        print(f"Classes: {len(sources.get('classes', []))}")
        print(f"Controllers: {len(sources.get('controllers', []))}")
        print(f"Services: {len(sources.get('services', []))}")
        print(f"Confluence pages: {len(sources.get('confluence', []))}")
        print(f"\nDuration: {duration_ms:.2f}ms")
        
        # Try to log and save report
        try:
            from agents.ai_response_logger import log_ai_response
            from agents.reporting_service import get_reporting_service
            
            print("\n=== Logging AI Response ===")
            log_ai_response(
                user_query=question,
                expert_name="PhoenixExpert",
                response_summary="Phoenix project business idea explained",
                success=True,
                duration_ms=duration_ms
            )
            
            print("\n=== Saving Report ===")
            reporting_service = get_reporting_service()
            reporting_service.save_agent_report("PhoenixExpert")
            reporting_service.save_summary_report()
            print("Reports saved successfully!")
        except Exception as e:
            print(f"Warning: Could not save report: {e}")
        
        return result
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    main()


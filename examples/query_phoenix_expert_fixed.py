#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Fixed script to query PhoenixExpert avoiding import issues
"""
import sys
import time
import json
import importlib.util
from pathlib import Path

# Get the agents directory (go up one level from examples/)
project_root = Path(__file__).parent.parent
agents_dir = project_root / "agents"
sys.path.insert(0, str(project_root))

# Import PhoenixExpert directly without going through __init__.py
spec = importlib.util.spec_from_file_location("phoenix_expert", agents_dir / "phoenix_expert.py")
phoenix_expert_module = importlib.util.module_from_spec(spec)
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
        
        # Try to log and save report (import separately to avoid issues)
        try:
            # Import reporting service directly
            reporting_spec = importlib.util.spec_from_file_location("reporting_service", project_root / "agents" / "reporting_service.py")
            reporting_module = importlib.util.module_from_spec(reporting_spec)
            reporting_spec.loader.exec_module(reporting_module)
            get_reporting_service = reporting_module.get_reporting_service
            
            # Import AI response logger directly
            logger_spec = importlib.util.spec_from_file_location("ai_response_logger", project_root / "agents" / "ai_response_logger.py")
            logger_module = importlib.util.module_from_spec(logger_spec)
            logger_spec.loader.exec_module(logger_module)
            log_ai_response = logger_module.log_ai_response
            
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
            import traceback
            traceback.print_exc()
        
        return result
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    main()


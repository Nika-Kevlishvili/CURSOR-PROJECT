#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Test script to verify automatic report generation fixes
"""
import sys
import time
from pathlib import Path
from datetime import datetime

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

# Direct imports avoiding __init__.py
from agents.reporting_service import ReportingService, get_reporting_service
from agents.phoenix_expert import PhoenixExpert

def test_reporting_service():
    """Test that reporting service can save reports"""
    print("=== Test 1: Reporting Service ===\n")
    
    reporting_service = get_reporting_service()
    
    # Log some test activity
    reporting_service.log_activity(
        agent_name="TestAgent",
        activity_type="test",
        description="Testing report generation"
    )
    
    # Try to save reports
    try:
        agent_path = reporting_service.save_agent_report("TestAgent")
        summary_path = reporting_service.save_summary_report()
        print(f"✓ Agent report saved: {agent_path}")
        print(f"✓ Summary report saved: {summary_path}")
        return True
    except Exception as e:
        print(f"✗ Failed to save reports: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_phoenix_expert_reporting():
    """Test that PhoenixExpert automatically saves reports"""
    print("\n=== Test 2: PhoenixExpert Auto-Reporting ===\n")
    
    try:
        # Create PhoenixExpert instance directly
        phoenix_dir = Path(__file__).parent.parent / "Phoenix"
        expert = PhoenixExpert(phoenix_dir=phoenix_dir)
        
        # Answer a simple question
        result = expert.answer_question("test question")
        
        # Check if reports directory has new files
        reports_dir = expert.reporting_service.reports_dir if expert.reporting_service else None
        if reports_dir:
            today = datetime.now().strftime('%Y-%m-%d')
            date_dir = reports_dir / today
            if date_dir.exists():
                files = list(date_dir.glob("PhoenixExpert_*.md"))
                if files:
                    print(f"✓ PhoenixExpert reports found: {len(files)} files")
                    for f in files[-3:]:  # Show last 3
                        print(f"  - {f.name}")
                    return True
                else:
                    print("⚠ No PhoenixExpert report files found")
                    return False
            else:
                print(f"⚠ Reports directory for today ({today}) doesn't exist")
                return False
        else:
            print("⚠ Reporting service not available")
            return False
            
    except Exception as e:
        print(f"✗ Failed to test PhoenixExpert: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    print("Testing automatic report generation fixes...\n")
    
    test1 = test_reporting_service()
    test2 = test_phoenix_expert_reporting()
    
    print("\n" + "="*70)
    if test1 and test2:
        print("✓ All tests passed!")
    else:
        print("✗ Some tests failed")
    print("="*70)

if __name__ == "__main__":
    main()


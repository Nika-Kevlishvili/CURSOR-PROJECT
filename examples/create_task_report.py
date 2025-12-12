#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Create report for rule compliance analysis task
"""

import sys
from pathlib import Path

# Add agents directory to path
sys.path.insert(0, str(Path(__file__).parent))

from agents.reporting_service import get_reporting_service
from datetime import datetime

def main():
    """Create report for rule compliance analysis task."""
    try:
        # Get reporting service
        service = get_reporting_service()
        
        # Log task execution
        service.log_task_execution(
            agent_name="RuleComplianceAnalysis",
            task="რულების დაცვის შემოწმება და აგენტების შეფასება",
            task_type="analysis",
            success=True,
            duration_ms=0,
            result={
                'analysis_type': 'rule_compliance',
                'compliance_rate': '85%',
                'report_file': 'reports/2025-12-10/RuleComplianceAnalysis_1706.md',
                'agents_modified': [
                    'TestAgent',
                    'PhoenixExpert',
                    'PostmanCollectionGenerator',
                    'GitLabUpdateAgent'
                ]
            }
        )
        
        # Log activities
        service.log_activity(
            agent_name="RuleComplianceAnalysis",
            activity_type="code_analysis",
            description="შემოწმებულია რულების დაცვა და აგენტების კოდი"
        )
        
        service.log_activity(
            agent_name="RuleComplianceAnalysis",
            activity_type="code_modification",
            description="შეცვლილია ყველა აგენტი რომ იყენებდეს reporting_service-ს"
        )
        
        # Log information sources
        service.log_information_source(
            agent_name="RuleComplianceAnalysis",
            source_type="file",
            source_description="agents/test_agent.py",
            information="TestAgent კოდის ანალიზი და reporting_service-ის დამატება"
        )
        
        service.log_information_source(
            agent_name="RuleComplianceAnalysis",
            source_type="file",
            source_description="agents/phoenix_expert.py",
            information="PhoenixExpert კოდის ანალიზი და reporting_service-ის დამატება"
        )
        
        service.log_information_source(
            agent_name="RuleComplianceAnalysis",
            source_type="file",
            source_description="agents/postman_collection_generator.py",
            information="PostmanCollectionGenerator კოდის ანალიზი და reporting_service-ის დამატება"
        )
        
        service.log_information_source(
            agent_name="RuleComplianceAnalysis",
            source_type="file",
            source_description="agents/gitlab_update_agent.py",
            information="GitLabUpdateAgent კოდის ანალიზი და reporting_service-ის დამატება"
        )
        
        service.log_information_source(
            agent_name="RuleComplianceAnalysis",
            source_type="file",
            source_description=".cursor/rules/phoenix.mdc",
            information="რულების ანალიზი და დაცვის შემოწმება"
        )
        
        # Save report
        report_path = service.save_agent_report("RuleComplianceAnalysis")
        print(f"✓ Report saved to: {report_path}")
        
        return 0
        
    except Exception as e:
        print(f"✗ Error creating report: {str(e)}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    sys.exit(main())


"""
Example script for generating agent reports

This script demonstrates how to generate reports for all agents.
"""

from agents.reporting_service import get_reporting_service
from pathlib import Path


def main():
    """Generate reports for all agents."""
    print("="*70)
    print("Generating Reports")
    print("="*70)
    print()
    
    # Get reporting service
    reporting_service = get_reporting_service()
    
    # Generate and save all reports
    print("Creating reports...")
    saved_paths = reporting_service.save_all_reports()
    
    print(f"\nCreated {len(saved_paths)} reports:")
    for path in saved_paths:
        print(f"  - {path}")
    
    # Display summary report content
    print("\n" + "="*70)
    print("Summary Report:")
    print("="*70)
    summary = reporting_service.generate_summary_report()
    print(summary)
    
    # Display statistics for each agent
    print("\n" + "="*70)
    print("Statistics:")
    print("="*70)
    
    all_agents = set()
    all_agents.update(reporting_service.agent_communications.keys())
    all_agents.update(reporting_service.information_sources.keys())
    all_agents.update(reporting_service.task_executions.keys())
    
    for agent_name in sorted(all_agents):
        stats = reporting_service.get_agent_statistics(agent_name)
        print(f"\n{agent_name}:")
        print(f"  Tasks: {stats['total_tasks']} (Successful: {stats['successful_tasks']})")
        print(f"  Communications: {stats['total_communications']} (Successful: {stats['successful_communications']})")
        print(f"  Information Sources: {stats['total_information_sources']}")
        if stats['consulted_agents']:
            print(f"  Consultations with agents: {', '.join(stats['consulted_agents'])}")


if __name__ == "__main__":
    main()


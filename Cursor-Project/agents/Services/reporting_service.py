"""
Reporting Service - Tracks and generates reports for all agent activities

This service collects information about:
- What each agent did
- Which agents they communicated with
- Where they got information from
- Task execution details
- Consultation history
"""

from typing import Dict, Any, List, Optional
from datetime import datetime
from pathlib import Path
import json
from collections import defaultdict


class AgentActivity:
    """Represents a single activity performed by an agent."""
    
    def __init__(
        self,
        agent_name: str,
        activity_type: str,
        description: str,
        timestamp: Optional[str] = None,
        **kwargs
    ):
        """
        Initialize agent activity.
        
        Args:
            agent_name: Name of the agent performing the activity
            activity_type: Type of activity (consultation, task_execution, information_source, etc.)
            description: Description of what was done
            timestamp: Timestamp of the activity (defaults to now)
            **kwargs: Additional metadata
        """
        self.agent_name = agent_name
        self.activity_type = activity_type
        self.description = description
        self.timestamp = timestamp or datetime.now().isoformat()
        self.metadata = kwargs
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert activity to dictionary."""
        return {
            'agent_name': self.agent_name,
            'activity_type': self.activity_type,
            'description': self.description,
            'timestamp': self.timestamp,
            'metadata': self.metadata
        }


class ReportingService:
    """
    Service for tracking and generating reports for agent activities.
    
    Tracks:
    - Agent tasks and executions
    - Inter-agent communications
    - Information sources
    - Consultation history
    """
    
    def __init__(self, reports_dir: Optional[Path] = None):
        """
        Initialize reporting service.
        
        Args:
            reports_dir: Directory for storing reports (defaults to reports/ in project root)
        """
        if reports_dir is None:
            # Default to reports/ folder in project root
            project_root = Path(__file__).parent.parent
            reports_dir = project_root / "reports"
        
        self.reports_dir = Path(reports_dir)
        self.reports_dir.mkdir(exist_ok=True)
        
        # Activity tracking
        self.activities: List[AgentActivity] = []
        
        # Agent communication tracking (agent_name -> list of consulted agents)
        self.agent_communications: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
        
        # Information sources tracking (agent_name -> list of sources)
        self.information_sources: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
        
        # Task execution tracking (agent_name -> list of tasks)
        self.task_executions: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
    
    def log_activity(
        self,
        agent_name: str,
        activity_type: str,
        description: str,
        **metadata
    ):
        """
        Log an agent activity.
        
        Args:
            agent_name: Name of the agent
            activity_type: Type of activity
            description: Description of the activity
            **metadata: Additional metadata
        """
        activity = AgentActivity(
            agent_name=agent_name,
            activity_type=activity_type,
            description=description,
            **metadata
        )
        self.activities.append(activity)
    
    def log_consultation(
        self,
        from_agent: str,
        to_agent: str,
        query: str,
        success: bool,
        duration_ms: float,
        response: Optional[Dict[str, Any]] = None,
        **kwargs
    ):
        """
        Log an inter-agent consultation.
        
        Args:
            from_agent: Agent initiating the consultation
            to_agent: Agent being consulted
            query: Query/question asked
            success: Whether consultation was successful
            duration_ms: Duration in milliseconds
            response: Response from consulted agent
            **kwargs: Additional metadata
        """
        consultation_record = {
            'timestamp': datetime.now().isoformat(),
            'to_agent': to_agent,
            'query': query[:200],  # Truncate long queries
            'success': success,
            'duration_ms': duration_ms,
            'response_summary': self._summarize_response(response) if response else None,
            **kwargs
        }
        
        self.agent_communications[from_agent].append(consultation_record)
        
        # Also log as activity
        status = "successful" if success else "failed"
        self.log_activity(
            agent_name=from_agent,
            activity_type="consultation",
            description=f"Consulted {to_agent}: {query[:100]}... ({status})",
            to_agent=to_agent,
            success=success,
            duration_ms=duration_ms,
            **kwargs
        )
    
    def log_information_source(
        self,
        agent_name: str,
        source_type: str,
        source_description: str,
        information: Optional[str] = None,
        **kwargs
    ):
        """
        Log an information source used by an agent.
        
        Args:
            agent_name: Agent using the source
            source_type: Type of source (file, api, database, agent, etc.)
            source_description: Description of the source
            information: Information retrieved (optional)
            **kwargs: Additional metadata
        """
        source_record = {
            'timestamp': datetime.now().isoformat(),
            'source_type': source_type,
            'source_description': source_description,
            'information_summary': information[:200] if information else None,
            **kwargs
        }
        
        self.information_sources[agent_name].append(source_record)
        
        # Also log as activity
        self.log_activity(
            agent_name=agent_name,
            activity_type="information_source",
            description=f"Retrieved information from {source_type}: {source_description}",
            source_type=source_type,
            source_description=source_description,
            **kwargs
        )
    
    def log_task_execution(
        self,
        agent_name: str,
        task: str,
        task_type: str,
        success: bool,
        duration_ms: float,
        result: Optional[Any] = None,
        **kwargs
    ):
        """
        Log a task execution by an agent.
        
        Args:
            agent_name: Agent executing the task
            task: Task description
            task_type: Type of task
            success: Whether task was successful
            duration_ms: Duration in milliseconds
            result: Task result (optional)
            **kwargs: Additional metadata
        """
        task_record = {
            'timestamp': datetime.now().isoformat(),
            'task': task,
            'task_type': task_type,
            'success': success,
            'duration_ms': duration_ms,
            'result_summary': self._summarize_result(result) if result else None,
            **kwargs
        }
        
        self.task_executions[agent_name].append(task_record)
        
        # Also log as activity
        status = "successful" if success else "failed"
        self.log_activity(
            agent_name=agent_name,
            activity_type="task_execution",
            description=f"Executed {task_type}: {task[:100]}... ({status})",
            task_type=task_type,
            success=success,
            duration_ms=duration_ms,
            **kwargs
        )
    
    def generate_agent_report(self, agent_name: str) -> str:
        """
        Generate a detailed report for a specific agent.
        
        Args:
            agent_name: Name of the agent
            
        Returns:
            Markdown formatted report
        """
        report_lines = []
        report_lines.append(f"# Report: {agent_name}")
        report_lines.append("")
        report_lines.append(f"**Time:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report_lines.append("")
        
        # Agent activities summary
        agent_activities = [a for a in self.activities if a.agent_name == agent_name]
        report_lines.append(f"## Total Activities: {len(agent_activities)}")
        report_lines.append("")
        
        # Task executions
        if agent_name in self.task_executions:
            tasks = self.task_executions[agent_name]
            report_lines.append(f"### Completed Tasks ({len(tasks)})")
            report_lines.append("")
            for i, task in enumerate(tasks, 1):
                status_icon = "✓" if task['success'] else "✗"
                report_lines.append(f"{i}. **{status_icon} {task['task_type']}**")
                report_lines.append(f"   - Task: {task['task']}")
                report_lines.append(f"   - Time: {task['timestamp']}")
                report_lines.append(f"   - Duration: {task['duration_ms']:.2f}ms")
                if task.get('result_summary'):
                    report_lines.append(f"   - Result: {task['result_summary']}")
                report_lines.append("")
        
        # Inter-agent communications
        if agent_name in self.agent_communications:
            communications = self.agent_communications[agent_name]
            report_lines.append(f"### Communication with Other Agents ({len(communications)})")
            report_lines.append("")
            
            # Group by agent
            by_agent = defaultdict(list)
            for comm in communications:
                by_agent[comm['to_agent']].append(comm)
            
            for consulted_agent, comms in by_agent.items():
                report_lines.append(f"#### {consulted_agent} ({len(comms)} consultations)")
                report_lines.append("")
                for i, comm in enumerate(comms, 1):
                    status_icon = "✓" if comm['success'] else "✗"
                    report_lines.append(f"{i}. **{status_icon} {comm['timestamp']}**")
                    report_lines.append(f"   - Query: {comm['query']}")
                    report_lines.append(f"   - Duration: {comm['duration_ms']:.2f}ms")
                    if comm.get('response_summary'):
                        report_lines.append(f"   - Response: {comm['response_summary']}")
                    report_lines.append("")
        
        # Information sources
        if agent_name in self.information_sources:
            sources = self.information_sources[agent_name]
            report_lines.append(f"### Information Sources ({len(sources)})")
            report_lines.append("")
            
            # Group by source type
            by_type = defaultdict(list)
            for source in sources:
                by_type[source['source_type']].append(source)
            
            for source_type, type_sources in by_type.items():
                report_lines.append(f"#### {source_type} ({len(type_sources)})")
                report_lines.append("")
                for i, source in enumerate(type_sources, 1):
                    report_lines.append(f"{i}. **{source['timestamp']}**")
                    report_lines.append(f"   - Description: {source['source_description']}")
                    if source.get('information_summary'):
                        report_lines.append(f"   - Information: {source['information_summary']}")
                    report_lines.append("")
        
        # Recent activities
        recent_activities = [a for a in agent_activities[-10:]]  # Last 10 activities
        if recent_activities:
            report_lines.append("### Recent Activities")
            report_lines.append("")
            for activity in recent_activities:
                report_lines.append(f"- **{activity.timestamp}** [{activity.activity_type}] {activity.description}")
            report_lines.append("")
        
        return "\n".join(report_lines)
    
    def generate_summary_report(self) -> str:
        """
        Generate a summary report for all agents.
        
        Returns:
            Markdown formatted summary report
        """
        report_lines = []
        report_lines.append("# Report: All Agents Overview")
        report_lines.append("")
        report_lines.append(f"**Time:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report_lines.append("")
        
        # Get all unique agent names
        all_agents = set()
        all_agents.update(self.agent_communications.keys())
        all_agents.update(self.information_sources.keys())
        all_agents.update(self.task_executions.keys())
        all_agents.update([a.agent_name for a in self.activities])
        
        if not all_agents:
            report_lines.append("No activities yet.")
            return "\n".join(report_lines)
        
        report_lines.append(f"## Number of Agents: {len(all_agents)}")
        report_lines.append("")
        
        # Summary for each agent
        for agent_name in sorted(all_agents):
            report_lines.append(f"### {agent_name}")
            report_lines.append("")
            
            # Counts
            tasks_count = len(self.task_executions.get(agent_name, []))
            comms_count = len(self.agent_communications.get(agent_name, []))
            sources_count = len(self.information_sources.get(agent_name, []))
            activities_count = len([a for a in self.activities if a.agent_name == agent_name])
            
            report_lines.append(f"- **Tasks:** {tasks_count}")
            report_lines.append(f"- **Communications:** {comms_count}")
            report_lines.append(f"- **Information Sources:** {sources_count}")
            report_lines.append(f"- **Total Activities:** {activities_count}")
            report_lines.append("")
            
            # List consulted agents
            if agent_name in self.agent_communications:
                consulted_agents = set(comm['to_agent'] for comm in self.agent_communications[agent_name])
                if consulted_agents:
                    report_lines.append(f"  Communication with agents: {', '.join(sorted(consulted_agents))}")
                    report_lines.append("")
        
        return "\n".join(report_lines)
    
    def save_agent_report(self, agent_name: str, filename: Optional[str] = None) -> Path:
        """
        Save agent report to file.
        
        Reports are saved in date-based folders (YYYY-MM-DD) with filenames
        containing the agent name, hour, and minutes.
        
        Args:
            agent_name: Name of the agent
            filename: Optional filename (defaults to {agent_name}_{HHMM}.md)
            
        Returns:
            Path to saved report file
        """
        now = datetime.now()
        
        # Create date-based folder (YYYY-MM-DD)
        date_folder = now.strftime('%Y-%m-%d')
        date_dir = self.reports_dir / date_folder
        date_dir.mkdir(exist_ok=True)
        
        if filename is None:
            # Format: {agent_name}_{HHMM}.md (hour and minutes only)
            time_str = now.strftime('%H%M')
            filename = f"{agent_name}_{time_str}.md"
        
        report_path = date_dir / filename
        report_content = self.generate_agent_report(agent_name)
        
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        return report_path
    
    def save_summary_report(self, filename: Optional[str] = None) -> Path:
        """
        Save summary report to file.
        
        Reports are saved in date-based folders (YYYY-MM-DD) with filenames
        containing "Summary", hour, and minutes.
        
        Args:
            filename: Optional filename (defaults to Summary_{HHMM}.md)
            
        Returns:
            Path to saved report file
        """
        # #region agent log
        import json
        try:
            with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                f.write(json.dumps({'id': 'log_report_save_summary', 'timestamp': __import__('time').time() * 1000, 'location': 'reporting_service.py:427', 'message': 'save_summary_report called', 'data': {}, 'sessionId': 'debug-session', 'runId': 'run1', 'hypothesisId': 'B'}) + '\n')
        except: pass
        # #endregion
        now = datetime.now()
        
        # Create date-based folder (YYYY-MM-DD)
        date_folder = now.strftime('%Y-%m-%d')
        date_dir = self.reports_dir / date_folder
        date_dir.mkdir(exist_ok=True)
        
        if filename is None:
            # Format: Summary_{HHMM}.md (hour and minutes only)
            time_str = now.strftime('%H%M')
            filename = f"Summary_{time_str}.md"
        
        report_path = date_dir / filename
        report_content = self.generate_summary_report()
        
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        return report_path
    
    def save_all_reports(self) -> List[Path]:
        """
        Save reports for all agents.
        
        Returns:
            List of paths to saved report files
        """
        saved_paths = []
        
        # Save summary report
        summary_path = self.save_summary_report()
        saved_paths.append(summary_path)
        
        # Save individual agent reports
        all_agents = set()
        all_agents.update(self.agent_communications.keys())
        all_agents.update(self.information_sources.keys())
        all_agents.update(self.task_executions.keys())
        all_agents.update([a.agent_name for a in self.activities])
        
        for agent_name in all_agents:
            agent_path = self.save_agent_report(agent_name)
            saved_paths.append(agent_path)
        
        return saved_paths
    
    def _summarize_response(self, response: Dict[str, Any]) -> str:
        """Summarize an agent response for display."""
        if isinstance(response, dict):
            if 'success' in response:
                return f"Success: {response.get('success')}"
            if 'answer' in response:
                return str(response['answer'])[:100]
            if 'result' in response:
                return str(response['result'])[:100]
            return str(response)[:100]
        return str(response)[:100] if response else "No response"
    
    def _summarize_result(self, result: Any) -> str:
        """Summarize a task result for display."""
        if isinstance(result, dict):
            if 'success' in result:
                return f"Success: {result.get('success')}"
            return str(result)[:100]
        return str(result)[:100] if result else "No result"
    
    def get_agent_statistics(self, agent_name: str) -> Dict[str, Any]:
        """
        Get statistics for a specific agent.
        
        Args:
            agent_name: Name of the agent
            
        Returns:
            Dictionary with statistics
        """
        tasks = self.task_executions.get(agent_name, [])
        communications = self.agent_communications.get(agent_name, [])
        sources = self.information_sources.get(agent_name, [])
        activities = [a for a in self.activities if a.agent_name == agent_name]
        
        successful_tasks = sum(1 for t in tasks if t['success'])
        successful_comms = sum(1 for c in communications if c['success'])
        
        avg_task_duration = sum(t['duration_ms'] for t in tasks) / len(tasks) if tasks else 0
        avg_comm_duration = sum(c['duration_ms'] for c in communications) / len(communications) if communications else 0
        
        consulted_agents = set(c['to_agent'] for c in communications)
        
        return {
            'agent_name': agent_name,
            'total_tasks': len(tasks),
            'successful_tasks': successful_tasks,
            'failed_tasks': len(tasks) - successful_tasks,
            'task_success_rate': successful_tasks / len(tasks) if tasks else 0,
            'avg_task_duration_ms': avg_task_duration,
            'total_communications': len(communications),
            'successful_communications': successful_comms,
            'failed_communications': len(communications) - successful_comms,
            'comm_success_rate': successful_comms / len(communications) if communications else 0,
            'avg_comm_duration_ms': avg_comm_duration,
            'consulted_agents': list(consulted_agents),
            'total_information_sources': len(sources),
            'total_activities': len(activities)
        }


# Global reporting service instance
_reporting_service = None


def get_reporting_service(reports_dir: Optional[Path] = None) -> ReportingService:
    """
    Get or create global reporting service instance.
    
    Args:
        reports_dir: Optional directory for reports
        
    Returns:
        ReportingService instance
    """
    global _reporting_service
    if _reporting_service is None:
        _reporting_service = ReportingService(reports_dir)
    return _reporting_service


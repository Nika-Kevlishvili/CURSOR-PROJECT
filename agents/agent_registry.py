"""
Agent Registry - Manages and coordinates multiple agents

This module provides a registry for managing different agents and allows
agents to consult with each other.
"""

from typing import Dict, Any, Optional, List
from abc import ABC, abstractmethod


class Agent(ABC):
    """Base class for all agents."""
    
    @abstractmethod
    def get_name(self) -> str:
        """Get agent name."""
        pass
    
    @abstractmethod
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        pass
    
    @abstractmethod
    def can_help_with(self, query: str) -> bool:
        """Check if agent can help with a query."""
        pass
    
    @abstractmethod
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """Consult with agent about a query."""
        pass


class AgentRegistry:
    """
    Registry for managing multiple agents.
    Allows agents to consult with each other.
    """
    
    def __init__(self):
        """Initialize agent registry."""
        self.agents: Dict[str, Agent] = {}
        self.consultation_history: List[Dict[str, Any]] = []
    
    def register_agent(self, agent: Agent):
        """Register an agent."""
        agent_name = agent.get_name()
        self.agents[agent_name] = agent
        print(f"AgentRegistry: Registered agent '{agent_name}'")
    
    def get_agent(self, agent_name: str) -> Optional[Agent]:
        """Get agent by name."""
        return self.agents.get(agent_name)
    
    def list_agents(self) -> List[str]:
        """List all registered agent names."""
        return list(self.agents.keys())
    
    def find_helpful_agents(self, query: str) -> List[Agent]:
        """Find agents that can help with a query."""
        helpful_agents = []
        for agent in self.agents.values():
            if agent.can_help_with(query):
                helpful_agents.append(agent)
        return helpful_agents
    
    def consult_agent(self, agent_name: str, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with a specific agent.
        
        Args:
            agent_name: Name of the agent to consult
            query: Query/question to ask
            context: Additional context information
        
        Returns:
            Response from the agent
        """
        agent = self.get_agent(agent_name)
        if not agent:
            return {
                'success': False,
                'error': f"Agent '{agent_name}' not found",
                'available_agents': self.list_agents()
            }
        
        try:
            print(f"AgentRegistry: Consulting '{agent_name}' with query: {query[:100]}...")
            response = agent.consult(query, context or {})
            
            # Record consultation
            consultation_record = {
                'timestamp': self._get_timestamp(),
                'from_agent': 'TestAgent',
                'to_agent': agent_name,
                'query': query,
                'response': response
            }
            self.consultation_history.append(consultation_record)
            
            return {
                'success': True,
                'agent': agent_name,
                'response': response
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'agent': agent_name
            }
    
    def consult_best_agent(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with the best matching agent for a query.
        
        Args:
            query: Query/question to ask
            context: Additional context information
        
        Returns:
            Response from the best matching agent
        """
        helpful_agents = self.find_helpful_agents(query)
        
        if not helpful_agents:
            return {
                'success': False,
                'error': 'No agents found that can help with this query',
                'available_agents': self.list_agents()
            }
        
        # Use the first helpful agent (could be improved with ranking)
        best_agent = helpful_agents[0]
        return self.consult_agent(best_agent.get_name(), query, context)
    
    def get_consultation_history(self) -> List[Dict[str, Any]]:
        """Get consultation history."""
        return self.consultation_history
    
    def _get_timestamp(self) -> str:
        """Get current timestamp."""
        from datetime import datetime
        return datetime.now().isoformat()


# Global agent registry instance
_agent_registry = None

def get_agent_registry() -> AgentRegistry:
    """Get or create global agent registry."""
    global _agent_registry
    if _agent_registry is None:
        _agent_registry = AgentRegistry()
    return _agent_registry


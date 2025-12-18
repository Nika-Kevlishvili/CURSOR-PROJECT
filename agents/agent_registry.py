"""
Agent Registry - Manages and coordinates multiple agents

This module provides a registry for managing different agents and allows
agents to consult with each other.

Improvements:
- Intelligent agent ranking based on query matching
- Caching of consultation results for performance
- Retry logic with exponential backoff
- Timeout handling for agent consultations
- Better error handling and logging
"""

from typing import Dict, Any, Optional, List, Tuple
from abc import ABC, abstractmethod
import time
import hashlib
import json
from functools import lru_cache

# Import reporting service (optional - handle import errors gracefully)
try:
    from .reporting_service import get_reporting_service
    REPORTING_AVAILABLE = True
except ImportError:
    REPORTING_AVAILABLE = False


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
    
    Features:
    - Intelligent agent ranking and selection
    - Result caching for performance
    - Retry logic with exponential backoff
    - Timeout handling
    - Comprehensive error handling
    """
    
    def __init__(self, enable_caching: bool = True, default_timeout: int = 30, max_retries: int = 2):
        """
        Initialize agent registry.
        
        Args:
            enable_caching: Enable caching of consultation results (default: True)
            default_timeout: Default timeout for consultations in seconds (default: 30)
            max_retries: Maximum number of retries for failed consultations (default: 2)
        """
        self.agents: Dict[str, Agent] = {}
        self.consultation_history: List[Dict[str, Any]] = []
        self.enable_caching = enable_caching
        self.default_timeout = default_timeout
        self.max_retries = max_retries
        
        # Cache for consultation results (query_hash -> result)
        self._consultation_cache: Dict[str, Dict[str, Any]] = {}
        self._cache_max_size = 100  # Maximum number of cached results
        
        # Agent performance tracking (for ranking)
        self._agent_performance: Dict[str, Dict[str, Any]] = {}
    
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
    
    def find_helpful_agents(self, query: str) -> List[Tuple[Agent, float]]:
        """
        Find agents that can help with a query, ranked by relevance.
        
        Args:
            query: Query string
            
        Returns:
            List of tuples (agent, relevance_score) sorted by score (highest first)
        """
        helpful_agents = []
        query_lower = query.lower()
        
        for agent in self.agents.values():
            if agent.can_help_with(query):
                # Calculate relevance score
                score = self._calculate_relevance_score(agent, query_lower)
                helpful_agents.append((agent, score))
        
        # Sort by score (highest first)
        helpful_agents.sort(key=lambda x: x[1], reverse=True)
        return helpful_agents
    
    def _calculate_relevance_score(self, agent: Agent, query_lower: str) -> float:
        """
        Calculate relevance score for an agent based on query.
        
        Args:
            agent: Agent to score
            query_lower: Lowercase query string
            
        Returns:
            Relevance score (0.0 to 1.0)
        """
        score = 0.0
        
        # Base score from can_help_with (0.3)
        score += 0.3
        
        # Score based on agent name matching query keywords (0.2)
        agent_name_lower = agent.get_name().lower()
        query_words = query_lower.split()
        name_matches = sum(1 for word in query_words if word in agent_name_lower)
        if name_matches > 0:
            score += min(0.2, name_matches * 0.1)
        
        # Score based on capabilities matching query (0.3)
        capabilities = agent.get_capabilities()
        capability_matches = sum(1 for cap in capabilities if any(word in cap.lower() for word in query_words))
        if capability_matches > 0:
            score += min(0.3, capability_matches * 0.1)
        
        # Score based on historical performance (0.2)
        agent_name = agent.get_name()
        if agent_name in self._agent_performance:
            perf = self._agent_performance[agent_name]
            success_rate = perf.get('success_rate', 0.5)
            score += 0.2 * success_rate
        
        return min(score, 1.0)
    
    def consult_agent(
        self, 
        agent_name: str, 
        query: str, 
        context: Dict[str, Any] = None,
        timeout: Optional[int] = None,
        use_cache: bool = True,
        from_agent: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Consult with a specific agent with improved error handling and caching.
        
        Args:
            agent_name: Name of the agent to consult
            query: Query/question to ask
            context: Additional context information
            timeout: Timeout in seconds (uses default_timeout if not provided)
            use_cache: Whether to use cached results if available
            from_agent: Name of the agent making the consultation (for reporting)
            
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
        
        # Check cache first
        if self.enable_caching and use_cache:
            cache_key = self._get_cache_key(agent_name, query, context)
            if cache_key in self._consultation_cache:
                cached_result = self._consultation_cache[cache_key]
                print(f"AgentRegistry: Using cached result for '{agent_name}' query")
                return cached_result
        
        timeout = timeout or self.default_timeout
        start_time = time.time()
        
        # Retry logic with exponential backoff
        last_exception = None
        for attempt in range(self.max_retries + 1):
            try:
                print(f"AgentRegistry: Consulting '{agent_name}' with query: {query[:100]}... (attempt {attempt + 1}/{self.max_retries + 1})")
                
                # Execute consultation with timeout
                response = self._consult_with_timeout(agent, query, context or {}, timeout)
                
                # Record consultation
                duration_ms = (time.time() - start_time) * 1000
                consultation_record = {
                    'timestamp': self._get_timestamp(),
                    'to_agent': agent_name,
                    'query': query,
                    'response': response,
                    'duration_ms': duration_ms,
                    'attempt': attempt + 1
                }
                self.consultation_history.append(consultation_record)
                
                # Log to reporting service if available and from_agent is provided
                if REPORTING_AVAILABLE and from_agent:
                    try:
                        reporting_service = get_reporting_service()
                        reporting_service.log_consultation(
                            from_agent=from_agent,
                            to_agent=agent_name,
                            query=query,
                            success=True,
                            duration_ms=duration_ms,
                            response=response
                        )
                    except Exception:
                        pass  # Don't fail if reporting fails
                
                # Update performance tracking
                self._update_agent_performance(agent_name, success=True)
                
                result = {
                    'success': True,
                    'agent': agent_name,
                    'response': response,
                    'duration_ms': duration_ms,
                    'cached': False
                }
                
                # Cache result
                if self.enable_caching:
                    self._cache_result(cache_key, result)
                
                return result
                
            except TimeoutError as e:
                last_exception = e
                print(f"AgentRegistry: Timeout consulting '{agent_name}' (attempt {attempt + 1}): {str(e)}")
                if attempt < self.max_retries:
                    wait_time = 2 ** attempt  # Exponential backoff
                    print(f"AgentRegistry: Retrying in {wait_time} seconds...")
                    time.sleep(wait_time)
                else:
                    break
                    
            except Exception as e:
                last_exception = e
                print(f"AgentRegistry: Error consulting '{agent_name}' (attempt {attempt + 1}): {str(e)}")
                if attempt < self.max_retries:
                    wait_time = 2 ** attempt
                    print(f"AgentRegistry: Retrying in {wait_time} seconds...")
                    time.sleep(wait_time)
                else:
                    break
        
        # All retries failed
        self._update_agent_performance(agent_name, success=False)
        
        duration_ms = (time.time() - start_time) * 1000
        error_result = {
            'success': False,
            'error': str(last_exception) if last_exception else 'Unknown error',
            'agent': agent_name,
            'duration_ms': duration_ms
        }
        
        # Record failed consultation
        consultation_record = {
            'timestamp': self._get_timestamp(),
            'to_agent': agent_name,
            'query': query,
            'response': error_result,
            'duration_ms': duration_ms,
            'error': str(last_exception) if last_exception else 'Unknown error'
        }
        self.consultation_history.append(consultation_record)
        
        # Log to reporting service if available and from_agent is provided
        if REPORTING_AVAILABLE and from_agent:
            try:
                reporting_service = get_reporting_service()
                reporting_service.log_consultation(
                    from_agent=from_agent,
                    to_agent=agent_name,
                    query=query,
                    success=False,
                    duration_ms=duration_ms,
                    response=error_result
                )
            except Exception:
                pass  # Don't fail if reporting fails
        
        return error_result
    
    def _consult_with_timeout(
        self, 
        agent: Agent, 
        query: str, 
        context: Dict[str, Any], 
        timeout: int
    ) -> Dict[str, Any]:
        """
        Consult with agent with timeout handling.
        
        Args:
            agent: Agent to consult
            query: Query string
            context: Context dictionary
            timeout: Timeout in seconds
            
        Returns:
            Response from agent
            
        Raises:
            TimeoutError: If consultation exceeds timeout
        """
        import signal
        
        # Simple timeout implementation (for Unix-like systems)
        # For Windows, we'll use a simpler approach
        try:
            # Try to use signal-based timeout (Unix)
            def timeout_handler(signum, frame):
                raise TimeoutError(f"Agent consultation timed out after {timeout} seconds")
            
            old_handler = signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(timeout)
            
            try:
                response = agent.consult(query, context)
            finally:
                signal.alarm(0)
                signal.signal(signal.SIGALRM, old_handler)
            
            return response
            
        except (AttributeError, OSError):
            # Windows or signal not available - use simple timeout check
            start_time = time.time()
            response = agent.consult(query, context)
            elapsed = time.time() - start_time
            
            if elapsed > timeout:
                raise TimeoutError(f"Agent consultation took {elapsed:.2f}s, exceeded timeout of {timeout}s")
            
            return response
    
    def consult_best_agent(
        self, 
        query: str, 
        context: Dict[str, Any] = None,
        timeout: Optional[int] = None,
        from_agent: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Consult with the best matching agent for a query using intelligent ranking.
        
        Args:
            query: Query/question to ask
            context: Additional context information
            timeout: Timeout in seconds (uses default_timeout if not provided)
            from_agent: Name of the agent making the consultation (for reporting)
        
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
        
        # Use the highest-ranked agent
        best_agent, best_score = helpful_agents[0]
        
        print(f"AgentRegistry: Selected '{best_agent.get_name()}' (relevance score: {best_score:.2f})")
        if len(helpful_agents) > 1:
            print(f"AgentRegistry: Alternative agents: {[a.get_name() for a, _ in helpful_agents[1:3]]}")
        
        return self.consult_agent(best_agent.get_name(), query, context, timeout, from_agent=from_agent)
    
    def get_consultation_history(self, limit: Optional[int] = None) -> List[Dict[str, Any]]:
        """
        Get consultation history.
        
        Args:
            limit: Maximum number of records to return (None for all)
        
        Returns:
            List of consultation records
        """
        if limit:
            return self.consultation_history[-limit:]
        return self.consultation_history.copy()
    
    def get_agent_performance(self, agent_name: Optional[str] = None) -> Dict[str, Any]:
        """
        Get performance statistics for agent(s).
        
        Args:
            agent_name: Specific agent name (None for all agents)
        
        Returns:
            Performance statistics dictionary
        """
        if agent_name:
            return self._agent_performance.get(agent_name, {})
        return self._agent_performance.copy()
    
    def clear_cache(self):
        """Clear consultation result cache."""
        self._consultation_cache.clear()
        print("AgentRegistry: Cache cleared")
    
    def _get_cache_key(self, agent_name: str, query: str, context: Optional[Dict[str, Any]]) -> str:
        """Generate cache key for consultation."""
        cache_data = {
            'agent': agent_name,
            'query': query,
            'context': context or {}
        }
        cache_str = json.dumps(cache_data, sort_keys=True)
        return hashlib.md5(cache_str.encode()).hexdigest()
    
    def _cache_result(self, cache_key: str, result: Dict[str, Any]):
        """Cache consultation result."""
        # Limit cache size
        if len(self._consultation_cache) >= self._cache_max_size:
            # Remove oldest entry (simple FIFO)
            oldest_key = next(iter(self._consultation_cache))
            del self._consultation_cache[oldest_key]
        
        self._consultation_cache[cache_key] = result
    
    def _update_agent_performance(self, agent_name: str, success: bool):
        """Update agent performance tracking."""
        if agent_name not in self._agent_performance:
            self._agent_performance[agent_name] = {
                'total_consultations': 0,
                'successful_consultations': 0,
                'failed_consultations': 0,
                'success_rate': 0.5
            }
        
        perf = self._agent_performance[agent_name]
        perf['total_consultations'] += 1
        
        if success:
            perf['successful_consultations'] += 1
        else:
            perf['failed_consultations'] += 1
        
        # Calculate success rate
        if perf['total_consultations'] > 0:
            perf['success_rate'] = perf['successful_consultations'] / perf['total_consultations']
    
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


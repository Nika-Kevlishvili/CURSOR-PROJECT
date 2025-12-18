"""
Agent Router - Intelligent routing and orchestration of agents

This module provides automatic agent selection and orchestration based on query analysis.
It determines which agent(s) are most competent for a given query and combines their responses.
"""

from typing import Dict, Any, Optional, List, Tuple
from agents.agent_registry import AgentRegistry, Agent, get_agent_registry
from agents.global_rules import get_global_rules, GlobalRules
import re

# Import reporting service (optional - handle import errors gracefully)
try:
    from .reporting_service import get_reporting_service
    REPORTING_AVAILABLE = True
except ImportError:
    REPORTING_AVAILABLE = False


class AgentRouter:
    """
    Intelligent router that automatically selects and orchestrates agents.
    
    This router:
    1. Analyzes user queries to understand intent
    2. Determines which agent(s) are most competent
    3. Routes queries to appropriate agents
    4. Combines responses from multiple agents when needed
    """
    
    def __init__(self, agent_registry: AgentRegistry = None):
        """
        Initialize agent router.
        
        Args:
            agent_registry: Agent registry instance (uses global if not provided)
        """
        self.agent_registry = agent_registry or get_agent_registry()
        self.global_rules = get_global_rules()
        self.routing_history: List[Dict[str, Any]] = []
        
        # Agent capability keywords for intelligent routing
        self.agent_keywords = {
            'test': ['test', 'testing', 'api test', 'ui test', 'integration test', 
                    'e2e test', 'automation', 'test case', 'test suite', 'run test',
                    'execute test', 'test endpoint', 'test api', 'validate'],
            'phoenix_expert': ['phoenix', 'question', 'how', 'what', 'why', 'explain',
                              'documentation', 'code', 'endpoint', 'api', 'controller',
                              'model', 'validation', 'permission', 'business logic',
                              'architecture', 'confluence', 'knowledge'],
            'postman': ['postman', 'collection', 'export', 'import', 'generate collection',
                       'postman collection', 'api collection'],
            'environment_access': ['dev', 'dev-2', 'dev2', 'environment', 'access environment',
                                  'login', 'portal', 'navigate', 'open dev', 'open dev-2',
                                  'go to dev', 'go to dev-2', 'switch to dev', 'switch to dev-2',
                                  'enter dev', 'enter dev-2', 'connect to dev', 'connect to dev-2'],
        }
    
    def route_query(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Route a query to the most appropriate agent(s) and return combined result.
        
        This is the main entry point for automatic agent routing.
        
        Args:
            query: User query or request
            context: Additional context information
            
        Returns:
            Dictionary with routing result and combined agent responses
        """
        print("\n" + "="*70)
        print("AgentRouter: [AUTOMATIC AGENT ROUTING]")
        print("="*70)
        print(f"AgentRouter: Query: {query[:100]}...")
        print("-"*70)
        
        # Step 1: Analyze query to determine intent
        intent = self._analyze_intent(query)
        print(f"AgentRouter: Detected intent: {intent.get('primary_intent')}")
        print(f"AgentRouter: Confidence: {intent.get('confidence', 0):.2f}")
        
        # Step 2: Find competent agents
        competent_agents = self._find_competent_agents(query, intent)
        print(f"AgentRouter: Found {len(competent_agents)} competent agent(s)")
        for agent_info in competent_agents:
            print(f"AgentRouter:   - {agent_info['agent'].get_name()} (score: {agent_info['score']:.2f})")
        
        if not competent_agents:
            return {
                'success': False,
                'error': 'No agents found that can handle this query',
                'available_agents': self.agent_registry.list_agents(),
                'query': query
            }
        
        # Step 3: Route to agent(s) and combine responses
        if len(competent_agents) == 1:
            # Single agent - direct routing
            result = self._route_to_single_agent(competent_agents[0], query, context)
        else:
            # Multiple agents - orchestrate and combine
            result = self._orchestrate_multiple_agents(competent_agents, query, context)
        
        # Step 4: Record routing history
        routing_record = {
            'timestamp': self._get_timestamp(),
            'query': query,
            'intent': intent,
            'agents_used': [a['agent'].get_name() for a in competent_agents],
            'result': result
        }
        self.routing_history.append(routing_record)
        
        # #region agent log
        import json
        try:
            with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                f.write(json.dumps({'id': 'log_router_complete', 'timestamp': __import__('time').time() * 1000, 'location': 'agent_router.py:107', 'message': 'route_query completed', 'data': {'agents_used': [a['agent'].get_name() for a in competent_agents], 'success': result.get('success', False)}, 'sessionId': 'debug-session', 'runId': 'run1', 'hypothesisId': 'D'}) + '\n')
        except: pass
        # #endregion
        
        # Log routing to reporting service
        if REPORTING_AVAILABLE:
            try:
                reporting_service = get_reporting_service()
                agents_used = [a['agent'].get_name() for a in competent_agents]
                for agent_name in agents_used:
                    reporting_service.log_activity(
                        agent_name=agent_name,
                        activity_type="routing",
                        description=f"Routed query: {query[:100]}...",
                        intent=intent.get('primary_intent'),
                        success=result.get('success', False)
                    )
                
                # Rule 0.6: MANDATORY Report Generation After Task Completion
                # Automatically save reports for all agents involved after routing
                # #region agent log
                import json
                try:
                    with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                        f.write(json.dumps({'id': 'log_router_auto_report', 'timestamp': __import__('time').time() * 1000, 'location': 'agent_router.py:137', 'message': 'auto saving reports after routing', 'data': {'agents_used': agents_used}, 'sessionId': 'debug-session', 'runId': 'run1', 'hypothesisId': 'D'}) + '\n')
                except: pass
                # #endregion
                for agent_name in agents_used:
                    try:
                        reporting_service.save_agent_report(agent_name)
                    except Exception as e:
                        print(f"AgentRouter: ⚠ Failed to save report for {agent_name}: {str(e)}")
                try:
                    reporting_service.save_summary_report()
                    print("AgentRouter: ✓ Reports automatically saved (Rule 0.6 compliance)")
                except Exception as e:
                    print(f"AgentRouter: ⚠ Failed to save summary report: {str(e)}")
            except Exception as e:
                print(f"AgentRouter: ⚠ Failed to log/save reports: {str(e)}")
                import traceback
                traceback.print_exc()
        
        print("="*70)
        print("AgentRouter: [ROUTING COMPLETED]")
        print("="*70 + "\n")
        
        return result
    
    def _check_restricted_operations(self, query: str) -> Dict[str, Any]:
        """
        Check if query involves restricted operations.
        
        Args:
            query: User query
            
        Returns:
            Dictionary with permission check result
        """
        # No restricted operations currently
        return {'permitted': True}
    
    def _analyze_intent(self, query: str) -> Dict[str, Any]:
        """
        Analyze query to determine intent and requirements.
        
        Args:
            query: User query
            
        Returns:
            Dictionary with intent analysis
        """
        query_lower = query.lower()
        
        # Detect primary intent
        intent_scores = {}
        
        # Test-related intent
        test_score = sum(1 for keyword in self.agent_keywords['test'] if keyword in query_lower)
        intent_scores['test'] = test_score
        
        # Phoenix Expert intent (knowledge/Q&A)
        phoenix_score = sum(1 for keyword in self.agent_keywords['phoenix_expert'] if keyword in query_lower)
        intent_scores['phoenix_expert'] = phoenix_score
        
        # Postman intent
        postman_score = sum(1 for keyword in self.agent_keywords['postman'] if keyword in query_lower)
        intent_scores['postman'] = postman_score
        
        # GitLab Update intent
        gitlab_update_score = sum(1 for keyword in self.agent_keywords['gitlab_update'] if keyword in query_lower)
        intent_scores['gitlab_update'] = gitlab_update_score
        
        # Environment Access intent
        environment_access_score = sum(1 for keyword in self.agent_keywords['environment_access'] if keyword in query_lower)
        intent_scores['environment_access'] = environment_access_score
        
        
        # Determine primary intent
        primary_intent = max(intent_scores.items(), key=lambda x: x[1])[0] if intent_scores else 'general'
        max_score = max(intent_scores.values()) if intent_scores else 0
        total_keywords = sum(intent_scores.values())
        confidence = max_score / total_keywords if total_keywords > 0 else 0.0
        
        return {
            'primary_intent': primary_intent,
            'intent_scores': intent_scores,
            'confidence': confidence,
            'requires_multiple_agents': max_score > 0 and len([s for s in intent_scores.values() if s > 0]) > 1
        }
    
    def _find_competent_agents(self, query: str, intent: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        Find agents that are competent to handle the query.
        
        Args:
            query: User query
            intent: Intent analysis result
            
        Returns:
            List of agent info dictionaries with scores, sorted by competence
        """
        all_agents = list(self.agent_registry.agents.values())
        agent_scores = []
        
        for agent in all_agents:
            # Check if agent can help with query
            if not agent.can_help_with(query):
                continue
            
            # Calculate competence score
            score = self._calculate_competence_score(agent, query, intent)
            
            if score > 0:
                agent_scores.append({
                    'agent': agent,
                    'score': score,
                    'name': agent.get_name()
                })
        
        # Sort by score (highest first)
        agent_scores.sort(key=lambda x: x['score'], reverse=True)
        
        # Return top agents (at least one, up to 3 for orchestration)
        return agent_scores[:3] if len(agent_scores) > 1 else agent_scores
    
    def _calculate_competence_score(self, agent: Agent, query: str, intent: Dict[str, Any]) -> float:
        """
        Calculate how competent an agent is for a given query.
        
        Args:
            agent: Agent to evaluate
            query: User query
            intent: Intent analysis
            
        Returns:
            Competence score (0.0 to 1.0)
        """
        agent_name = agent.get_name().lower()
        query_lower = query.lower()
        
        score = 0.0
        
        # Base score from can_help_with
        if agent.can_help_with(query):
            score += 0.3
        
        # Score based on agent name matching intent
        primary_intent = intent.get('primary_intent', '')
        if 'test' in agent_name and primary_intent == 'test':
            score += 0.4
        elif 'phoenix' in agent_name and primary_intent == 'phoenix_expert':
            score += 0.4
        elif 'postman' in agent_name and primary_intent == 'postman':
            score += 0.4
        elif 'gitlab' in agent_name and primary_intent == 'gitlab_update':
            score += 0.4
        elif 'environment' in agent_name and primary_intent == 'environment_access':
            score += 0.4
        
        # Score based on capabilities
        capabilities = agent.get_capabilities()
        for capability in capabilities:
            capability_lower = capability.lower()
            if any(keyword in capability_lower for keyword in query_lower.split()):
                score += 0.1
        
        # Cap at 1.0
        return min(score, 1.0)
    
    def _route_to_single_agent(self, agent_info: Dict[str, Any], query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Route query to a single agent.
        
        Args:
            agent_info: Agent information dictionary
            query: User query
            context: Additional context
            
        Returns:
            Agent response
        """
        agent = agent_info['agent']
        agent_name = agent.get_name()
        
        print(f"AgentRouter: Routing to single agent: {agent_name}")
        
        # Pass agent_name as from_agent for tracking (routing is system-level)
        result = self.agent_registry.consult_agent(agent_name, query, context, from_agent="AgentRouter")
        
        return {
            'success': result.get('success', False),
            'routing_type': 'single',
            'agents_used': [agent_name],
            'primary_agent': agent_name,
            'response': result.get('response', {}),
            'error': result.get('error'),
            'query': query
        }
    
    def _orchestrate_multiple_agents(self, agent_infos: List[Dict[str, Any]], query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Orchestrate multiple agents and combine their responses.
        
        Args:
            agent_infos: List of agent information dictionaries
            query: User query
            context: Additional context
            
        Returns:
            Combined response from multiple agents
        """
        print(f"AgentRouter: Orchestrating {len(agent_infos)} agents")
        
        agent_responses = []
        agents_used = []
        
        # Consult each agent
        for agent_info in agent_infos:
            agent = agent_info['agent']
            agent_name = agent.get_name()
            agents_used.append(agent_name)
            
            print(f"AgentRouter: Consulting {agent_name}...")
            # Pass agent_name as from_agent for tracking (routing is system-level)
            result = self.agent_registry.consult_agent(agent_name, query, context, from_agent="AgentRouter")
            
            if result.get('success'):
                agent_responses.append({
                    'agent': agent_name,
                    'score': agent_info['score'],
                    'response': result.get('response', {}),
                    'success': True
                })
            else:
                agent_responses.append({
                    'agent': agent_name,
                    'score': agent_info['score'],
                    'response': None,
                    'success': False,
                    'error': result.get('error', 'Unknown error')
                })
        
        # Combine responses
        combined_response = self._combine_responses(agent_responses, query)
        
        return {
            'success': any(r.get('success') for r in agent_responses),
            'routing_type': 'orchestrated',
            'agents_used': agents_used,
            'primary_agent': agents_used[0] if agents_used else None,
            'agent_responses': agent_responses,
            'combined_response': combined_response,
            'query': query
        }
    
    def _combine_responses(self, agent_responses: List[Dict[str, Any]], query: str) -> Dict[str, Any]:
        """
        Combine responses from multiple agents into a unified response.
        
        Args:
            agent_responses: List of agent response dictionaries
            query: Original query
            
        Returns:
            Combined response dictionary
        """
        successful_responses = [r for r in agent_responses if r.get('success')]
        
        if not successful_responses:
            return {
                'combined': False,
                'message': 'No successful agent responses to combine',
                'errors': [r.get('error') for r in agent_responses if not r.get('success')]
            }
        
        if len(successful_responses) == 1:
            # Single successful response - return as-is
            return {
                'combined': False,
                'primary_response': successful_responses[0].get('response', {}),
                'source_agent': successful_responses[0].get('agent')
            }
        
        # Multiple successful responses - combine intelligently
        # Prioritize by agent score
        sorted_responses = sorted(successful_responses, key=lambda x: x.get('score', 0), reverse=True)
        
        primary_response = sorted_responses[0].get('response', {})
        supplementary_responses = sorted_responses[1:]
        
        combined = {
            'combined': True,
            'primary_response': primary_response,
            'primary_agent': sorted_responses[0].get('agent'),
            'supplementary_responses': [
                {
                    'agent': r.get('agent'),
                    'response': r.get('response', {}),
                    'score': r.get('score', 0)
                }
                for r in supplementary_responses
            ],
            'summary': f"Combined responses from {len(successful_responses)} agents",
            'query': query
        }
        
        return combined
    
    def get_routing_history(self) -> List[Dict[str, Any]]:
        """Get routing history."""
        return self.routing_history.copy()
    
    def _get_timestamp(self) -> str:
        """Get current timestamp."""
        from datetime import datetime
        return datetime.now().isoformat()


# Global router instance
_agent_router = None

def get_agent_router(agent_registry: AgentRegistry = None) -> AgentRouter:
    """Get or create global agent router instance."""
    global _agent_router
    if _agent_router is None:
        _agent_router = AgentRouter(agent_registry)
    return _agent_router


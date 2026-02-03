"""
ProductionDataReaderAgent Adapter - Adapts ProductionDataReaderAgent to Agent interface

This adapter allows ProductionDataReaderAgent to be used as an agent in the AgentRegistry.
"""

from typing import Dict, Any, List
from agents.Core import Agent
from agents.Main import get_production_data_reader_agent


class ProductionDataReaderAdapter(Agent):
    """
    Adapter that makes ProductionDataReaderAgent compatible with Agent interface.
    """
    
    def __init__(self):
        """Initialize ProductionDataReaderAgent adapter."""
        self.production_data_reader = get_production_data_reader_agent()
    
    def get_name(self) -> str:
        """Get agent name."""
        return "ProductionDataReaderAgent"
    
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        return [
            "Production database reading",
            "Any entity type analysis (liability, receivable, payment, deposit, invoice, contract, etc.)",
            "Liability offset analysis",
            "Receivable history analysis",
            "Payment offset analysis",
            "Deposit usage analysis",
            "Invoice analysis",
            "Contract analysis",
            "Relationship analysis",
            "Step-by-step data creation explanation",
            "Data traceability",
            "Production data investigation",
            "Generic table queries",
            "Data flow analysis"
        ]
    
    def can_help_with(self, query: str) -> bool:
        """Check if ProductionDataReaderAgent can help with a query."""
        query_lower = query.lower()
        
        # Keywords that indicate production data reading queries
        keywords = [
            'production data', 'prod data', 'production database',
            'liability offset', 'receivable history', 'payment offset',
            'deposit', 'invoice', 'contract', 'customer',
            'how was created', 'step by step', 'data traceability',
            'reversed receivable', 'offset sequence', 'creation process',
            'analyze data', 'read data', 'query data', 'data analysis',
            'prod გარემო', 'ლაიაბილითი', 'ოფსეტი', 'როგორ შეიქმნა',
            'დატის ანალიზი', 'დატის წაკითხვა', 'რესივებლი', 'გადახდა',
            'დეპოზიტი', 'ინვოისი', 'კონტრაქტი'
        ]
        
        # Check if query contains relevant keywords
        return any(keyword in query_lower for keyword in keywords)
    
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with ProductionDataReaderAgent about a query.
        
        Args:
            query: User query about production data
            context: Optional context dictionary
            
        Returns:
            Dictionary with consultation result
        """
        try:
            # Parse query to extract entity ID and type
            entity_id, entity_type = self._parse_query(query)
            
            if not entity_id:
                return {
                    "success": False,
                    "error": "Could not extract entity ID from query",
                    "suggestion": "Please provide entity ID (e.g., 'liability 45319' or 'receivable 11925')"
                }
            
            # Analyze based on entity type using universal analyze_entity method
            analysis = self.production_data_reader.analyze_entity(entity_type, entity_id)
            
            # Format report
            report = self.production_data_reader.format_analysis_report(analysis)
            
            return {
                "success": True,
                "agent": "ProductionDataReaderAgent",
                "entity_id": entity_id,
                "entity_type": entity_type,
                "analysis": analysis,
                "report": report,
                "explanation": analysis.get("explanation", [])
            }
            
        except Exception as e:
            return {
                "success": False,
                "error": str(e),
                "agent": "ProductionDataReaderAgent"
            }
    
    def _parse_query(self, query: str) -> Tuple[Optional[int], str]:
        """
        Parse query to extract entity ID and type.
        
        Args:
            query: User query string
            
        Returns:
            Tuple of (entity_id, entity_type)
        """
        import re
        
        query_lower = query.lower()
        
        # Try to find liability ID
        liability_match = re.search(r'liability[:\s]+(\d+)|ლაიაბილითი[:\s]+(\d+)', query_lower)
        if liability_match:
            entity_id = int(liability_match.group(1) or liability_match.group(2))
            return entity_id, "liability"
        
        # Try to find receivable ID
        receivable_match = re.search(r'receivable[:\s]+(\d+)|რესივებლი[:\s]+(\d+)', query_lower)
        if receivable_match:
            entity_id = int(receivable_match.group(1) or receivable_match.group(2))
            return entity_id, "receivable"
        
        # Try to find payment ID
        payment_match = re.search(r'payment[:\s]+(\d+)|გადახდა[:\s]+(\d+)', query_lower)
        if payment_match:
            entity_id = int(payment_match.group(1) or payment_match.group(2))
            return entity_id, "payment"
        
        # Try to find deposit ID
        deposit_match = re.search(r'deposit[:\s]+(\d+)|დეპოზიტი[:\s]+(\d+)', query_lower)
        if deposit_match:
            entity_id = int(deposit_match.group(1) or deposit_match.group(2))
            return entity_id, "deposit"
        
        # Try to find invoice ID
        invoice_match = re.search(r'invoice[:\s]+(\d+)|ინვოისი[:\s]+(\d+)', query_lower)
        if invoice_match:
            entity_id = int(invoice_match.group(1) or invoice_match.group(2))
            return entity_id, "invoice"
        
        # Try to find contract ID
        contract_match = re.search(r'contract[:\s]+(\d+)|კონტრაქტი[:\s]+(\d+)', query_lower)
        if contract_match:
            entity_id = int(contract_match.group(1) or contract_match.group(2))
            return entity_id, "contract"
        
        # Try to find any number (fallback)
        number_match = re.search(r'(\d{4,})', query)
        if number_match:
            entity_id = int(number_match.group(1))
            # Try to infer type from context
            if 'receivable' in query_lower or 'რესივებლი' in query_lower:
                return entity_id, "receivable"
            elif 'payment' in query_lower or 'გადახდა' in query_lower:
                return entity_id, "payment"
            elif 'deposit' in query_lower or 'დეპოზიტი' in query_lower:
                return entity_id, "deposit"
            elif 'invoice' in query_lower or 'ინვოისი' in query_lower:
                return entity_id, "invoice"
            elif 'contract' in query_lower or 'კონტრაქტი' in query_lower:
                return entity_id, "contract"
            else:
                return entity_id, "liability"  # Default to liability
        
        return None, "unknown"

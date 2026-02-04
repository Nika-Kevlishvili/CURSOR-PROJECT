"""
ProductionDataReaderAgent - Universal Agent for Reading and Analyzing Production Database Data

ROLE:
- Reads and analyzes ANY production database data
- Explains step-by-step how entities were created
- Analyzes relationships, dependencies, and data flow
- Provides detailed traceability of data creation history
- Supports any entity type: liabilities, receivables, payments, deposits, invoices, contracts, etc.
- Analyzes any database table and its relationships

WORKFLOW:
1. Receives entity ID and type (or table name and criteria)
2. Connects to Production database (PostgreSQLProd)
3. Queries all related data (relationships, history, dependencies)
4. Analyzes data structure and relationships
5. Provides step-by-step explanation of how data was created and related

ACCESS:
- READ-ONLY access to Production database (via MCP PostgreSQLProd)
- Does NOT modify any data
- Uses readonly_user credentials for safety
- Can query any table in production database
"""

import json
from typing import Dict, List, Any, Optional, Tuple
from pathlib import Path
from datetime import datetime
from decimal import Decimal

# Import services
try:
    from agents.Core.integration_service import get_integration_service
    INTEGRATION_SERVICE_AVAILABLE = True
except ImportError:
    INTEGRATION_SERVICE_AVAILABLE = False

try:
    from agents.Services.reporting_service import get_reporting_service
    REPORTING_SERVICE_AVAILABLE = True
except ImportError:
    REPORTING_SERVICE_AVAILABLE = False

try:
    from agents.Main.phoenix_expert import get_phoenix_expert
    PHOENIX_EXPERT_AVAILABLE = True
except ImportError:
    PHOENIX_EXPERT_AVAILABLE = False


class ProductionDataReaderAgent:
    """
    Universal agent for reading and analyzing production database data.
    
    Analyzes ANY production data:
    - Any entity type (liabilities, receivables, payments, deposits, invoices, contracts, etc.)
    - Relationships and dependencies between entities
    - Data creation history and traceability
    - Offset sequences and reversals
    - Any database table and its relationships
    - Step-by-step data creation and modification process
    """
    
    def __init__(self):
        """Initialize ProductionDataReaderAgent."""
        self.agent_name = "ProductionDataReaderAgent"
        self.db_connected = False
        
        # Initialize services
        if INTEGRATION_SERVICE_AVAILABLE:
            self.integration_service = get_integration_service()
        else:
            self.integration_service = None
            
        if REPORTING_SERVICE_AVAILABLE:
            self.reporting_service = get_reporting_service()
        else:
            self.reporting_service = None
    
    def analyze_liability_offsets(self, liability_id: int) -> Dict[str, Any]:
        """
        Analyze liability offsets and explain step-by-step how they were created.
        
        Args:
            liability_id: Liability ID to analyze
            
        Returns:
            Dictionary with detailed analysis including:
            - Liability details
            - All offsets (deposits, payments, receivables, rescheduling)
            - Offset sequence and status
            - Step-by-step creation explanation
        """
        result = {
            "liability_id": liability_id,
            "liability_details": None,
            "offsets": [],
            "sequence": [],
            "explanation": [],
            "errors": []
        }
        
        try:
            # Step 1: Get liability details
            liability_details = self._get_liability_details(liability_id)
            if not liability_details:
                result["errors"].append(f"Liability {liability_id} not found")
                return result
            
            result["liability_details"] = liability_details
            
            # Step 2: Get all offsets
            offsets = self._get_all_offsets(liability_id)
            result["offsets"] = offsets
            
            # Step 3: Build sequence
            sequence = self._build_offset_sequence(offsets)
            result["sequence"] = sequence
            
            # Step 4: Generate explanation
            explanation = self._explain_creation_process(liability_details, offsets, sequence)
            result["explanation"] = explanation
            
        except Exception as e:
            result["errors"].append(f"Error analyzing liability offsets: {str(e)}")
        
        return result
    
    def analyze_entity(self, entity_type: str, entity_id: int) -> Dict[str, Any]:
        """
        Universal method to analyze any entity type.
        
        Args:
            entity_type: Type of entity (liability, receivable, payment, deposit, invoice, contract, etc.)
            entity_id: Entity ID to analyze
            
        Returns:
            Dictionary with detailed analysis
        """
        entity_type_lower = entity_type.lower()
        
        if entity_type_lower == 'liability':
            return self.analyze_liability_offsets(entity_id)
        elif entity_type_lower == 'receivable':
            return self.analyze_receivable_history(entity_id)
        elif entity_type_lower == 'payment':
            return self.analyze_payment_history(entity_id)
        elif entity_type_lower == 'deposit':
            return self.analyze_deposit_history(entity_id)
        elif entity_type_lower == 'invoice':
            return self.analyze_invoice_history(entity_id)
        elif entity_type_lower == 'contract':
            return self.analyze_contract_history(entity_id)
        else:
            # Generic analysis for unknown entity types
            return self.analyze_generic_entity(entity_type, entity_id)
    
    def query_table(self, table_name: str, schema: str = "receivable", filters: Dict[str, Any] = None) -> List[Dict[str, Any]]:
        """
        Query any table in production database.
        
        Args:
            table_name: Name of the table to query
            schema: Schema name (default: receivable)
            filters: Dictionary of filters (column: value)
            
        Returns:
            List of rows as dictionaries
        """
        # This will be implemented using MCP PostgreSQLProd tools
        # For now, return structure
        return []
    
    def analyze_relationships(self, entity_type: str, entity_id: int) -> Dict[str, Any]:
        """
        Analyze all relationships for an entity.
        
        Args:
            entity_type: Type of entity
            entity_id: Entity ID
            
        Returns:
            Dictionary with all relationships
        """
        result = {
            "entity_type": entity_type,
            "entity_id": entity_id,
            "relationships": [],
            "dependencies": [],
            "dependents": [],
            "errors": []
        }
        
        try:
            # Query foreign key relationships
            # Query reverse relationships
            # Build relationship graph
            pass
        except Exception as e:
            result["errors"].append(f"Error analyzing relationships: {str(e)}")
        
        return result
    
    def analyze_payment_history(self, payment_id: int) -> Dict[str, Any]:
        """Analyze payment creation and offset history."""
        result = {
            "payment_id": payment_id,
            "payment_details": None,
            "offset_history": [],
            "explanation": [],
            "errors": []
        }
        
        try:
            payment_details = self._get_payment_details(payment_id)
            if not payment_details:
                result["errors"].append(f"Payment {payment_id} not found")
                return result
            
            result["payment_details"] = payment_details
            
            # Get offset history
            offset_history = self._get_payment_offset_history(payment_id)
            result["offset_history"] = offset_history
            
            # Generate explanation
            explanation = self._explain_payment_history(payment_details, offset_history)
            result["explanation"] = explanation
            
        except Exception as e:
            result["errors"].append(f"Error analyzing payment history: {str(e)}")
        
        return result
    
    def analyze_deposit_history(self, deposit_id: int) -> Dict[str, Any]:
        """Analyze deposit creation and usage history."""
        result = {
            "deposit_id": deposit_id,
            "deposit_details": None,
            "usage_history": [],
            "explanation": [],
            "errors": []
        }
        
        try:
            deposit_details = self._get_deposit_details(deposit_id)
            if not deposit_details:
                result["errors"].append(f"Deposit {deposit_id} not found")
                return result
            
            result["deposit_details"] = deposit_details
            
            # Get usage history
            usage_history = self._get_deposit_usage_history(deposit_id)
            result["usage_history"] = usage_history
            
            # Generate explanation
            explanation = self._explain_deposit_history(deposit_details, usage_history)
            result["explanation"] = explanation
            
        except Exception as e:
            result["errors"].append(f"Error analyzing deposit history: {str(e)}")
        
        return result
    
    def analyze_invoice_history(self, invoice_id: int) -> Dict[str, Any]:
        """Analyze invoice creation and related entities."""
        result = {
            "invoice_id": invoice_id,
            "invoice_details": None,
            "related_entities": [],
            "explanation": [],
            "errors": []
        }
        
        try:
            invoice_details = self._get_invoice_details(invoice_id)
            if not invoice_details:
                result["errors"].append(f"Invoice {invoice_id} not found")
                return result
            
            result["invoice_details"] = invoice_details
            
            # Get related entities (liabilities, receivables, etc.)
            related_entities = self._get_invoice_related_entities(invoice_id)
            result["related_entities"] = related_entities
            
            # Generate explanation
            explanation = self._explain_invoice_history(invoice_details, related_entities)
            result["explanation"] = explanation
            
        except Exception as e:
            result["errors"].append(f"Error analyzing invoice history: {str(e)}")
        
        return result
    
    def analyze_contract_history(self, contract_id: int) -> Dict[str, Any]:
        """Analyze contract creation and related entities."""
        result = {
            "contract_id": contract_id,
            "contract_details": None,
            "related_entities": [],
            "explanation": [],
            "errors": []
        }
        
        try:
            contract_details = self._get_contract_details(contract_id)
            if not contract_details:
                result["errors"].append(f"Contract {contract_id} not found")
                return result
            
            result["contract_details"] = contract_details
            
            # Get related entities
            related_entities = self._get_contract_related_entities(contract_id)
            result["related_entities"] = related_entities
            
            # Generate explanation
            explanation = self._explain_contract_history(contract_details, related_entities)
            result["explanation"] = explanation
            
        except Exception as e:
            result["errors"].append(f"Error analyzing contract history: {str(e)}")
        
        return result
    
    def analyze_generic_entity(self, entity_type: str, entity_id: int) -> Dict[str, Any]:
        """Generic analysis for any entity type."""
        result = {
            "entity_type": entity_type,
            "entity_id": entity_id,
            "entity_details": None,
            "relationships": [],
            "explanation": [],
            "errors": []
        }
        
        try:
            # Try to find entity in common tables
            # Query relationships
            # Generate explanation
            pass
        except Exception as e:
            result["errors"].append(f"Error analyzing {entity_type} {entity_id}: {str(e)}")
        
        return result
    
    def analyze_receivable_history(self, receivable_id: int) -> Dict[str, Any]:
        """
        Analyze receivable creation and offset history.
        
        Args:
            receivable_id: Receivable ID to analyze
            
        Returns:
            Dictionary with receivable history and offsets
        """
        result = {
            "receivable_id": receivable_id,
            "receivable_details": None,
            "offset_history": [],
            "reversal_history": [],
            "explanation": [],
            "errors": []
        }
        
        try:
            # Get receivable details
            receivable_details = self._get_receivable_details(receivable_id)
            if not receivable_details:
                result["errors"].append(f"Receivable {receivable_id} not found")
                return result
            
            result["receivable_details"] = receivable_details
            
            # Get offset history
            offset_history = self._get_receivable_offset_history(receivable_id)
            result["offset_history"] = offset_history
            
            # Get reversal history
            reversal_history = self._get_receivable_reversal_history(receivable_id)
            result["reversal_history"] = reversal_history
            
            # Generate explanation
            explanation = self._explain_receivable_history(receivable_details, offset_history, reversal_history)
            result["explanation"] = explanation
            
        except Exception as e:
            result["errors"].append(f"Error analyzing receivable history: {str(e)}")
        
        return result
    
    def _get_liability_details(self, liability_id: int) -> Optional[Dict[str, Any]]:
        """Get liability details from database."""
        # This will be implemented using MCP PostgreSQLProd tools
        # For now, return structure
        return {
            "id": liability_id,
            "liability_number": None,
            "initial_amount": None,
            "current_amount": None,
            "create_date": None,
            "due_date": None,
            "currency": None
        }
    
    def _get_all_offsets(self, liability_id: int) -> List[Dict[str, Any]]:
        """Get all offsets for a liability."""
        # This will query:
        # - customer_liabilitie_paid_by_deposits
        # - customer_liabilitie_paid_by_payments
        # - customer_liabilitie_paid_by_receivables
        # - customer_liabilitie_paid_by_rescheduling
        return []
    
    def _build_offset_sequence(self, offsets: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Build chronological sequence of offsets."""
        # Sort by create_date
        sorted_offsets = sorted(offsets, key=lambda x: x.get("create_date", ""))
        return sorted_offsets
    
    def _explain_creation_process(
        self, 
        liability_details: Dict[str, Any],
        offsets: List[Dict[str, Any]],
        sequence: List[Dict[str, Any]]
    ) -> List[str]:
        """Generate step-by-step explanation of creation process."""
        explanation = []
        
        explanation.append(f"Liability {liability_details.get('id')} was created on {liability_details.get('create_date')}")
        explanation.append(f"Initial amount: {liability_details.get('initial_amount')}")
        explanation.append(f"Current amount: {liability_details.get('current_amount')}")
        
        for i, offset in enumerate(sequence, 1):
            offset_type = offset.get("type", "UNKNOWN")
            offset_id = offset.get("id")
            amount = offset.get("amount")
            status = offset.get("status", "ACTIVE")
            create_date = offset.get("create_date")
            
            explanation.append(f"\nStep {i}: {offset_type} offset")
            explanation.append(f"  - Offset ID: {offset_id}")
            explanation.append(f"  - Amount: {amount}")
            explanation.append(f"  - Date: {create_date}")
            explanation.append(f"  - Status: {status}")
            
            if status == "REVERSED":
                explanation.append(f"  - This offset was later REVERSED")
        
        return explanation
    
    def _get_receivable_details(self, receivable_id: int) -> Optional[Dict[str, Any]]:
        """Get receivable details from database."""
        return {
            "id": receivable_id,
            "receivable_number": None,
            "initial_amount": None,
            "current_amount": None,
            "create_date": None,
            "due_date": None,
            "currency": None
        }
    
    def _get_receivable_offset_history(self, receivable_id: int) -> List[Dict[str, Any]]:
        """Get receivable offset history."""
        return []
    
    def _get_receivable_reversal_history(self, receivable_id: int) -> List[Dict[str, Any]]:
        """Get receivable reversal history."""
        return []
    
    def _get_payment_details(self, payment_id: int) -> Optional[Dict[str, Any]]:
        """Get payment details from database."""
        return {
            "id": payment_id,
            "payment_number": None,
            "amount": None,
            "payment_date": None,
            "create_date": None,
            "currency": None
        }
    
    def _get_payment_offset_history(self, payment_id: int) -> List[Dict[str, Any]]:
        """Get payment offset history."""
        return []
    
    def _get_deposit_details(self, deposit_id: int) -> Optional[Dict[str, Any]]:
        """Get deposit details from database."""
        return {
            "id": deposit_id,
            "deposit_number": None,
            "amount": None,
            "create_date": None,
            "currency": None
        }
    
    def _get_deposit_usage_history(self, deposit_id: int) -> List[Dict[str, Any]]:
        """Get deposit usage history."""
        return []
    
    def _get_invoice_details(self, invoice_id: int) -> Optional[Dict[str, Any]]:
        """Get invoice details from database."""
        return {
            "id": invoice_id,
            "invoice_number": None,
            "total_amount": None,
            "create_date": None,
            "currency": None
        }
    
    def _get_invoice_related_entities(self, invoice_id: int) -> List[Dict[str, Any]]:
        """Get entities related to invoice."""
        return []
    
    def _get_contract_details(self, contract_id: int) -> Optional[Dict[str, Any]]:
        """Get contract details from database."""
        return {
            "id": contract_id,
            "contract_number": None,
            "create_date": None,
            "status": None
        }
    
    def _get_contract_related_entities(self, contract_id: int) -> List[Dict[str, Any]]:
        """Get entities related to contract."""
        return []
    
    def _explain_receivable_history(
        self,
        receivable_details: Dict[str, Any],
        offset_history: List[Dict[str, Any]],
        reversal_history: List[Dict[str, Any]]
    ) -> List[str]:
        """Generate explanation of receivable history."""
        explanation = []
        
        explanation.append(f"Receivable {receivable_details.get('id')} was created on {receivable_details.get('create_date')}")
        
        if reversal_history:
            explanation.append(f"\nReversal History:")
            for reversal in reversal_history:
                explanation.append(f"  - Reversed on {reversal.get('reversal_date')}")
                explanation.append(f"  - Reason: {reversal.get('reason', 'N/A')}")
        
        return explanation
    
    def _explain_payment_history(
        self,
        payment_details: Dict[str, Any],
        offset_history: List[Dict[str, Any]]
    ) -> List[str]:
        """Generate explanation of payment history."""
        explanation = []
        
        explanation.append(f"Payment {payment_details.get('id')} was created on {payment_details.get('create_date')}")
        explanation.append(f"Amount: {payment_details.get('amount')}")
        
        if offset_history:
            explanation.append(f"\nOffset History:")
            for offset in offset_history:
                explanation.append(f"  - Offset with {offset.get('type')} {offset.get('id')}")
                explanation.append(f"  - Amount: {offset.get('amount')}")
                explanation.append(f"  - Date: {offset.get('date')}")
        
        return explanation
    
    def _explain_deposit_history(
        self,
        deposit_details: Dict[str, Any],
        usage_history: List[Dict[str, Any]]
    ) -> List[str]:
        """Generate explanation of deposit history."""
        explanation = []
        
        explanation.append(f"Deposit {deposit_details.get('id')} was created on {deposit_details.get('create_date')}")
        explanation.append(f"Amount: {deposit_details.get('amount')}")
        
        if usage_history:
            explanation.append(f"\nUsage History:")
            for usage in usage_history:
                explanation.append(f"  - Used for {usage.get('type')} {usage.get('id')}")
                explanation.append(f"  - Amount: {usage.get('amount')}")
                explanation.append(f"  - Date: {usage.get('date')}")
        
        return explanation
    
    def _explain_invoice_history(
        self,
        invoice_details: Dict[str, Any],
        related_entities: List[Dict[str, Any]]
    ) -> List[str]:
        """Generate explanation of invoice history."""
        explanation = []
        
        explanation.append(f"Invoice {invoice_details.get('id')} was created on {invoice_details.get('create_date')}")
        explanation.append(f"Total Amount: {invoice_details.get('total_amount')}")
        
        if related_entities:
            explanation.append(f"\nRelated Entities:")
            for entity in related_entities:
                explanation.append(f"  - {entity.get('type')} {entity.get('id')}")
                explanation.append(f"  - Relationship: {entity.get('relationship', 'N/A')}")
        
        return explanation
    
    def _explain_contract_history(
        self,
        contract_details: Dict[str, Any],
        related_entities: List[Dict[str, Any]]
    ) -> List[str]:
        """Generate explanation of contract history."""
        explanation = []
        
        explanation.append(f"Contract {contract_details.get('id')} was created on {contract_details.get('create_date')}")
        explanation.append(f"Status: {contract_details.get('status')}")
        
        if related_entities:
            explanation.append(f"\nRelated Entities:")
            for entity in related_entities:
                explanation.append(f"  - {entity.get('type')} {entity.get('id')}")
                explanation.append(f"  - Relationship: {entity.get('relationship', 'N/A')}")
        
        return explanation
    
    def format_analysis_report(self, analysis: Dict[str, Any]) -> str:
        """
        Format analysis result as a readable report.
        
        Args:
            analysis: Analysis result dictionary
            
        Returns:
            Formatted markdown report
        """
        report = []
        
        # Determine entity type and ID
        entity_id = None
        entity_type = None
        
        if "liability_id" in analysis:
            entity_id = analysis["liability_id"]
            entity_type = "Liability"
        elif "receivable_id" in analysis:
            entity_id = analysis["receivable_id"]
            entity_type = "Receivable"
        elif "payment_id" in analysis:
            entity_id = analysis["payment_id"]
            entity_type = "Payment"
        elif "deposit_id" in analysis:
            entity_id = analysis["deposit_id"]
            entity_type = "Deposit"
        elif "invoice_id" in analysis:
            entity_id = analysis["invoice_id"]
            entity_type = "Invoice"
        elif "contract_id" in analysis:
            entity_id = analysis["contract_id"]
            entity_type = "Contract"
        elif "entity_id" in analysis:
            entity_id = analysis["entity_id"]
            entity_type = analysis.get("entity_type", "Entity").title()
        
        if entity_id:
            report.append(f"# {entity_type} {entity_id} Analysis\n")
            
            # Get details based on entity type
            details_key = f"{entity_type.lower()}_details" if entity_type else "entity_details"
            details = analysis.get(details_key) or analysis.get("entity_details")
            
            if details:
                report.append(f"## {entity_type} Details")
                for key, value in details.items():
                    if value is not None:
                        report.append(f"- **{key.replace('_', ' ').title()}:** {value}")
                report.append("")
            
            # Add relationships if available
            if analysis.get("relationships"):
                report.append(f"## Relationships\n")
                for rel in analysis["relationships"]:
                    report.append(f"- {rel.get('type')} {rel.get('id')}: {rel.get('relationship', 'N/A')}")
                report.append("")
            
            # Add explanation
            if analysis.get("explanation"):
                report.append(f"## Step-by-Step Creation Process\n")
                for step in analysis["explanation"]:
                    report.append(step)
                report.append("")
            
            # Add offset history if available
            if analysis.get("offsets"):
                report.append(f"## Offsets\n")
                for i, offset in enumerate(analysis["offsets"], 1):
                    report.append(f"{i}. {offset.get('type')} {offset.get('id')}: {offset.get('amount')} on {offset.get('date')}")
                report.append("")
            
            # Add offset history if available (for receivables)
            if analysis.get("offset_history"):
                report.append(f"## Offset History\n")
                for offset in analysis["offset_history"]:
                    report.append(f"- {offset.get('type')} {offset.get('id')}: {offset.get('amount')} on {offset.get('date')}")
                report.append("")
        
        if analysis.get("errors"):
            report.append(f"\n## Errors\n")
            for error in analysis["errors"]:
                report.append(f"- {error}")
        
        return "\n".join(report)


def get_production_data_reader_agent() -> ProductionDataReaderAgent:
    """Get ProductionDataReaderAgent instance."""
    return ProductionDataReaderAgent()

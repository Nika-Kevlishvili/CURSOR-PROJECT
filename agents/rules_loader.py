"""
Rules Loader Service - CRITICAL SYSTEM COMPONENT

This service ensures that ALL rules from .cursor/rules/ directory are ALWAYS
read and applied BEFORE any response or action is taken.

ABSOLUTE REQUIREMENT: This service MUST be called at the START of EVERY
conversation, BEFORE any tool calls or responses.
"""

import os
import re
from pathlib import Path
from typing import Dict, List, Optional

# Try to import frontmatter, but work without it if not available
try:
    import frontmatter
    HAS_FRONTMATTER = True
except ImportError:
    HAS_FRONTMATTER = False


class RulesLoader:
    """Loads and manages rules from .cursor/rules/ directory."""
    
    def __init__(self, rules_dir: Optional[str] = None):
        """
        Initialize RulesLoader.
        
        Args:
            rules_dir: Path to rules directory. Defaults to .cursor/rules/
        """
        if rules_dir is None:
            # Find project root (look for .cursor directory)
            current = Path.cwd()
            while current != current.parent:
                cursor_dir = current / ".cursor" / "rules"
                if cursor_dir.exists():
                    rules_dir = str(cursor_dir)
                    break
                current = current.parent
            
            if rules_dir is None:
                # Fallback: try relative to current directory
                rules_dir = ".cursor/rules"
        
        self.rules_dir = Path(rules_dir)
        self._rules_cache: Dict[str, str] = {}
        self._always_apply_rules: List[str] = []
    
    def load_all_rules(self) -> Dict[str, str]:
        """
        Load ALL rules from .cursor/rules/ directory.
        
        Returns:
            Dictionary mapping rule file names to their content.
        """
        rules = {}
        
        if not self.rules_dir.exists():
            return rules
        
        for rule_file in self.rules_dir.glob("*.mdc"):
            try:
                content = rule_file.read_text(encoding='utf-8')
                rules[rule_file.name] = content
                
                # Check if alwaysApply is True
                if self._has_always_apply(content):
                    self._always_apply_rules.append(rule_file.name)
                    
            except Exception as e:
                print(f"Error loading rule file {rule_file}: {e}")
        
        self._rules_cache = rules
        return rules
    
    def _has_always_apply(self, content: str) -> bool:
        """Check if rule file has alwaysApply: true in frontmatter."""
        if HAS_FRONTMATTER:
            try:
                post = frontmatter.loads(content)
                return post.metadata.get('alwaysApply', False)
            except Exception:
                pass
        
        # Fallback: check for alwaysApply: true pattern in frontmatter
        # Pattern: ---\nalwaysApply: true\n--- or alwaysApply: true
        frontmatter_pattern = r'---\s*\nalwaysApply:\s*true\s*\n---'
        simple_pattern = r'alwaysApply:\s*true'
        
        return bool(re.search(frontmatter_pattern, content, re.IGNORECASE | re.MULTILINE) or 
                   re.search(simple_pattern, content, re.IGNORECASE))
    
    def get_always_apply_rules(self) -> List[str]:
        """Get list of rule files that have alwaysApply: true."""
        if not self._always_apply_rules:
            self.load_all_rules()
        return self._always_apply_rules.copy()
    
    def get_rules_content(self) -> str:
        """
        Get combined content of ALL rules, with alwaysApply rules first.
        
        Returns:
            Combined rules content as string.
        """
        if not self._rules_cache:
            self.load_all_rules()
        
        if not self._rules_cache:
            return ""
        
        # Sort: alwaysApply rules first, then others
        always_apply = [name for name in self._rules_cache.keys() 
                       if name in self._always_apply_rules]
        other_rules = [name for name in self._rules_cache.keys() 
                      if name not in self._always_apply_rules]
        
        sorted_rules = always_apply + other_rules
        
        combined = []
        for rule_name in sorted_rules:
            content = self._rules_cache[rule_name]
            combined.append(f"# Rules from {rule_name}\n\n{content}\n\n")
        
        return "\n".join(combined)
    
    def get_rules_summary(self) -> str:
        """
        Get a summary of loaded rules for quick reference.
        
        Returns:
            Summary string with rule file names and alwaysApply status.
        """
        if not self._rules_cache:
            self.load_all_rules()
        
        summary = ["=== LOADED RULES ==="]
        for rule_name in sorted(self._rules_cache.keys()):
            status = "ALWAYS APPLY" if rule_name in self._always_apply_rules else "On demand"
            summary.append(f"- {rule_name}: {status}")
        
        return "\n".join(summary)


# Global instance
_rules_loader: Optional[RulesLoader] = None


def get_rules_loader() -> RulesLoader:
    """Get or create global RulesLoader instance."""
    global _rules_loader
    if _rules_loader is None:
        _rules_loader = RulesLoader()
    return _rules_loader


def load_rules_at_start() -> str:
    """
    CRITICAL FUNCTION: Load all rules at the start of conversation.
    
    This MUST be called BEFORE any response or action.
    
    Returns:
        Combined rules content.
    """
    loader = get_rules_loader()
    rules_content = loader.get_rules_content()
    
    # Log that rules were loaded
    summary = loader.get_rules_summary()
    print(f"[RULES LOADED]\n{summary}\n")
    
    return rules_content


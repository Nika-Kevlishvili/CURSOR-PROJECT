"""
Logging Utilities for Agents

Provides structured logging with different log levels, formatting, and optional file output.
"""

import logging
import sys
from pathlib import Path
from typing import Optional
from datetime import datetime
import json


class AgentLogger:
    """
    Enhanced logger for agents with structured logging and file output.
    """
    
    def __init__(
        self,
        name: str,
        log_level: int = logging.INFO,
        enable_file_logging: bool = True,
        log_dir: Optional[Path] = None
    ):
        """
        Initialize agent logger.
        
        Args:
            name: Logger name (usually agent name)
            log_level: Logging level (default: INFO)
            enable_file_logging: Enable file logging (default: True)
            log_dir: Directory for log files (default: agents/logs)
        """
        self.name = name
        self.logger = logging.getLogger(name)
        self.logger.setLevel(log_level)
        
        # Prevent duplicate handlers
        if self.logger.handlers:
            return
        
        # Console handler with colored output
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(log_level)
        
        # Create formatter
        console_formatter = logging.Formatter(
            '%(asctime)s | %(name)s | %(levelname)s | %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        console_handler.setFormatter(console_formatter)
        self.logger.addHandler(console_handler)
        
        # File handler
        if enable_file_logging:
            if log_dir is None:
                log_dir = Path(__file__).parent / "logs"
            log_dir.mkdir(exist_ok=True)
            
            # Create log file with timestamp
            log_file = log_dir / f"{name}_{datetime.now().strftime('%Y%m%d')}.log"
            
            file_handler = logging.FileHandler(log_file, encoding='utf-8')
            file_handler.setLevel(log_level)
            
            # More detailed formatter for file
            file_formatter = logging.Formatter(
                '%(asctime)s | %(name)s | %(levelname)s | %(funcName)s:%(lineno)d | %(message)s',
                datefmt='%Y-%m-%d %H:%M:%S'
            )
            file_handler.setFormatter(file_formatter)
            self.logger.addHandler(file_handler)
    
    def info(self, message: str, **kwargs):
        """Log info message."""
        self.logger.info(self._format_message(message, **kwargs))
    
    def debug(self, message: str, **kwargs):
        """Log debug message."""
        self.logger.debug(self._format_message(message, **kwargs))
    
    def warning(self, message: str, **kwargs):
        """Log warning message."""
        self.logger.warning(self._format_message(message, **kwargs))
    
    def error(self, message: str, error: Optional[Exception] = None, **kwargs):
        """Log error message with optional exception."""
        if error:
            self.logger.error(
                self._format_message(message, **kwargs),
                exc_info=True
            )
        else:
            self.logger.error(self._format_message(message, **kwargs))
    
    def critical(self, message: str, error: Optional[Exception] = None, **kwargs):
        """Log critical message with optional exception."""
        if error:
            self.logger.critical(
                self._format_message(message, **kwargs),
                exc_info=True
            )
        else:
            self.logger.critical(self._format_message(message, **kwargs))
    
    def log_consultation(
        self,
        to_agent: str,
        query: str,
        success: bool,
        duration_ms: float,
        **kwargs
    ):
        """Log agent consultation."""
        status = "✓" if success else "✗"
        self.info(
            f"{status} Consultation: {to_agent} | Query: {query[:100]} | "
            f"Duration: {duration_ms:.2f}ms",
            consultation=True,
            to_agent=to_agent,
            success=success,
            duration_ms=duration_ms,
            **kwargs
        )
    
    def log_task_execution(
        self,
        task: str,
        task_type: str,
        success: bool,
        duration_ms: float,
        **kwargs
    ):
        """Log task execution."""
        status = "✓" if success else "✗"
        self.info(
            f"{status} Task Execution: {task_type} | Task: {task[:100]} | "
            f"Duration: {duration_ms:.2f}ms",
            task_execution=True,
            task_type=task_type,
            success=success,
            duration_ms=duration_ms,
            **kwargs
        )
    
    def _format_message(self, message: str, **kwargs) -> str:
        """Format message with additional context."""
        if kwargs:
            context = " | ".join([f"{k}={v}" for k, v in kwargs.items() if k not in ['consultation', 'task_execution']])
            if context:
                return f"{message} | {context}"
        return message


# Global loggers cache
_loggers: dict = {}


def get_agent_logger(
    name: str,
    log_level: int = logging.INFO,
    enable_file_logging: bool = True
) -> AgentLogger:
    """
    Get or create agent logger.
    
    Args:
        name: Logger name
        log_level: Logging level
        enable_file_logging: Enable file logging
    
    Returns:
        AgentLogger instance
    """
    if name not in _loggers:
        _loggers[name] = AgentLogger(name, log_level, enable_file_logging)
    return _loggers[name]

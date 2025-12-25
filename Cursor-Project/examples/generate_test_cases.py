"""
Example: Generate Test Cases using TestCaseGeneratorAgent

This script demonstrates how to use TestCaseGeneratorAgent to generate
test cases from bug descriptions or task descriptions.
"""

from agents.Main import get_test_case_generator_agent


def main():
    """Main function to demonstrate test case generation."""
    
    # Initialize the agent
    print("Initializing TestCaseGeneratorAgent...")
    agent = get_test_case_generator_agent()
    
    # Example 1: Generate test cases from a bug description
    print("\n" + "="*70)
    print("Example 1: Generating test cases from bug description")
    print("="*70)
    
    bug_description = """
    Bug: User cannot save customer data when customer identifier is longer than 17 characters.
    The system accepts the input but fails silently without showing any error message.
    Expected behavior: System should validate and show an error message if identifier exceeds 17 characters.
    """
    
    result = agent.generate_test_cases(bug_description, prompt_type='bug')
    
    print(f"\n✓ Generated {result['test_case_count']} test cases")
    print(f"✓ Prompt type: {result['prompt_type']}")
    print(f"✓ Saved to: {result['file_path']}")
    print(f"\nFirst 500 characters of generated test cases:")
    print("-"*70)
    print(result['test_cases'][:500])
    print("...")
    
    # Example 2: Generate test cases from a task description
    print("\n" + "="*70)
    print("Example 2: Generating test cases from task description")
    print("="*70)
    
    task_description = """
    Task: Implement customer search functionality with filters by name, identifier, and status.
    The search should support partial matches and return results sorted by relevance.
    Users should be able to combine multiple filters.
    """
    
    result = agent.generate_test_cases(task_description, prompt_type='task')
    
    print(f"\n✓ Generated {result['test_case_count']} test cases")
    print(f"✓ Prompt type: {result['prompt_type']}")
    print(f"✓ Saved to: {result['file_path']}")
    print(f"\nFirst 500 characters of generated test cases:")
    print("-"*70)
    print(result['test_cases'][:500])
    print("...")
    
    # Example 3: Auto-detect prompt type
    print("\n" + "="*70)
    print("Example 3: Auto-detecting prompt type")
    print("="*70)
    
    auto_prompt = """
    Feature: Add email validation to customer registration form.
    The system should validate email format and check for duplicate emails.
    """
    
    result = agent.generate_test_cases(auto_prompt)  # No prompt_type specified
    
    print(f"\n✓ Auto-detected prompt type: {result['prompt_type']}")
    print(f"✓ Generated {result['test_case_count']} test cases")
    print(f"✓ Saved to: {result['file_path']}")
    
    # Show generation history
    print("\n" + "="*70)
    print("Generation History")
    print("="*70)
    history = agent.get_generation_history()
    print(f"Total generations: {len(history)}")
    for i, gen in enumerate(history, 1):
        print(f"{i}. {gen['prompt_type']} - {gen['test_case_count']} test cases - {gen['timestamp']}")


if __name__ == "__main__":
    main()


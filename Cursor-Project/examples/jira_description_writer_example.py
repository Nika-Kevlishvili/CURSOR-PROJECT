"""
Example: Using JiraDescriptionWriterAgent to write test descriptions to Jira

This example shows how to use JiraDescriptionWriterAgent to analyze test code
and write detailed descriptions to Jira tickets.
"""

from agents.Main import get_jira_description_writer_agent


def example_write_description_to_jira():
    """Example: Write description to Jira from test file."""
    
    # Initialize agent
    agent = get_jira_description_writer_agent()
    
    # Write description from test file
    result = agent.write_description_from_test_file(
        jira_id="REG-1037",
        test_file_path="Cursor-Project/EnergoTS/tests/billing/forVolumes/forVolumes.spec.ts",
        cloud_id="ad451d5c-7331-46f8-9a47-f51dc8e6bbde"  # Optional
    )
    
    if result.get('success') and result.get('ready_to_write'):
        # Get description and cloud_id
        description = result.get('description')
        cloud_id = result.get('cloud_id')
        jira_id = result.get('jira_id')
        
        # Write to Jira using MCP tools
        # Note: In actual usage, this would be done via tool calls:
        # mcp_Jira_editJiraIssue(
        #     cloudId=cloud_id,
        #     issueIdOrKey=jira_id,
        #     fields={'description': description}
        # )
        
        print(f"Description generated for {jira_id}")
        print(f"Objects extracted: {result.get('objects_count')}")
        print(f"\nDescription preview (first 500 chars):")
        print(description[:500])
        print("\n...")
        print("\nUse mcp_Jira_editJiraIssue() to write to Jira")
    else:
        print(f"Error: {result.get('error')}")


def example_analyze_test_code():
    """Example: Analyze test code and generate description."""
    
    # Initialize agent
    agent = get_jira_description_writer_agent()
    
    # Read test code
    test_code = """
    test('[REG-1037]: splitting when total amount of scale codes is zero and "Number or days" checkbox does not selected', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints}) => {
        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        })
        
        await test.step('create POD', async () => {
            const grid = await Nomenclatures.grid_operator('splitting');
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.gridOperatorId = await grid;
            const PodSettlement = await Request.post(Endpoints.pod, {data:payload});
            await expect(PodSettlement).CheckResponse();
            Responses.pod.push(await PodSettlement.json());
        })
    })
    """
    
    # Analyze test code
    analysis = agent.analyze_test_code(test_code, "REG-1037")
    
    # Generate description
    description = agent.generate_jira_description(analysis)
    
    print("Analysis Results:")
    print(f"Test Title: {analysis.get('test_title')}")
    print(f"Objects Found: {len(analysis.get('objects', []))}")
    print(f"Test Conditions: {len(analysis.get('test_conditions', []))}")
    print(f"\nGenerated Description:\n{description}")


if __name__ == "__main__":
    print("=" * 70)
    print("JiraDescriptionWriterAgent Examples")
    print("=" * 70)
    
    print("\n1. Example: Analyze test code")
    print("-" * 70)
    example_analyze_test_code()
    
    print("\n\n2. Example: Write description to Jira")
    print("-" * 70)
    example_write_description_to_jira()

"""
Example: Generate POD Create Postman Collection

This example demonstrates how to generate a Postman collection
with POD create test cases and upload it to Postman workspace.
"""

from agents import get_test_agent
import os

def main():
    """Generate POD create Postman collection."""
    
    # Configuration
    config = {
        # Postman API credentials (optional - can use environment variables)
        'postman_api_key': os.getenv('POSTMAN_API_KEY', ''),
        'postman_workspace_id': os.getenv('POSTMAN_WORKSPACE_ID', ''),
    }
    
    # Initialize TestAgent
    agent = get_test_agent(
        base_url="http://localhost:8080",
        config=config
    )
    
    print("="*70)
    print("POD Create Postman Collection Generator")
    print("="*70)
    print()
    
    # Generate and upload POD create collection
    result = agent.generate_postman_collection(
        collection_type="pod_create",
        collection_name="POD Create - Automation Test Cases",
        upload=True  # Set to False if you don't want to upload
    )
    
    # Display results
    print("\n" + "="*70)
    print("Generation Results")
    print("="*70)
    
    if result['success']:
        print(f"✓ Collection generated successfully")
        print(f"  Local file: {result['file_path']}")
        
        if result.get('upload_result'):
            upload_result = result['upload_result']
            if upload_result.get('success'):
                print(f"✓ Collection uploaded to Postman")
                print(f"  Collection ID: {upload_result.get('collection_id', 'N/A')}")
                if upload_result.get('url'):
                    print(f"  Collection URL: {upload_result['url']}")
            else:
                print(f"✗ Upload failed: {upload_result.get('error', 'Unknown error')}")
                print(f"  Message: {upload_result.get('message', 'N/A')}")
    else:
        print(f"✗ Generation failed: {result.get('error', 'Unknown error')}")
        print(f"  Message: {result.get('message', 'N/A')}")
    
    print("="*70)


if __name__ == "__main__":
    main()


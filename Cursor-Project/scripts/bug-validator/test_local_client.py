#!/usr/bin/env python3
"""
Test script for local Phoenix client to verify it's working correctly.
"""

import sys
from local_phoenix_client import create_client

def test_local_phoenix_client():
    """Test the local Phoenix client functionality."""
    print("=== Testing Local Phoenix Client ===")
    
    # Create client
    try:
        client = create_client()
        print(f"✅ Client created successfully")
        print(f"📂 Phoenix root: {client.phoenix_root}")
        
        # Check if Phoenix directory exists
        availability_error = client._check_phoenix_availability()
        if availability_error:
            print(f"❌ Phoenix unavailable: {availability_error}")
            return False
        else:
            print(f"✅ Phoenix directory accessible")
        
        # Test search functionality with a simple bug
        print("\n=== Testing Search Functionality ===")
        test_summary = "Invoice payment API returning 500 error"
        test_description = "When calling /api/payment/process endpoint, server returns 500 internal server error"
        
        print(f"🔍 Testing search with:")
        print(f"   Summary: {test_summary}")
        print(f"   Description: {test_description}")
        
        # Perform search
        results = client.search_for_bug(test_summary, test_description)
        
        # Display results
        print(f"\n📊 Search Results:")
        print(f"   Status: {results.get('status', 'unknown')}")
        print(f"   Keywords used: {results.get('keywords_used', [])}")
        print(f"   Files found: {len(results.get('files', []))}")
        print(f"   Code snippets: {len(results.get('snippets', []))}")
        
        if results.get('files'):
            print(f"\n📁 Sample files found:")
            for i, file_info in enumerate(results['files'][:3]):  # Show first 3
                rel_path = file_info.get('relative_path', file_info.get('file', 'unknown'))
                project = file_info.get('project', 'unknown')
                search_term = file_info.get('search_term', 'unknown')
                print(f"   {i+1}. {rel_path} (project: {project}, term: {search_term})")
        
        if results.get('snippets'):
            print(f"\n📄 Sample code snippet:")
            snippet = results['snippets'][0]
            print(f"   File: {snippet.get('file', 'unknown')}")
            print(f"   Content: {snippet.get('content', '')[:200]}...")
        
        print(f"\n✅ Local Phoenix Client test completed successfully!")
        return True
        
    except Exception as e:
        print(f"❌ Error during testing: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = test_local_phoenix_client()
    sys.exit(0 if success else 1)
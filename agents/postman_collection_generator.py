"""
Postman Collection Generator

This service generates Postman collections with test cases and uploads them to Postman workspace.
"""

import json
import os
import requests
from typing import Dict, List, Any, Optional
from datetime import datetime
from pathlib import Path


class PostmanCollectionGenerator:
    """
    Service for generating Postman collections and uploading them to Postman workspace.
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize Postman Collection Generator.
        
        Args:
            config: Configuration dictionary with Postman API credentials
        """
        self.config = config or {}
        
        # Postman API configuration
        self.postman_api_key = (
            self.config.get('postman_api_key') or 
            os.getenv('POSTMAN_API_KEY', '')
        )
        self.postman_api_url = "https://api.getpostman.com"
        self.workspace_id = (
            self.config.get('postman_workspace_id') or 
            os.getenv('POSTMAN_WORKSPACE_ID', '')
        )
        
        # Base directory for saving collections locally
        base_dir = Path(__file__).parent.parent
        self.collections_dir = base_dir / "postman" / "postman_collections"
        self.collections_dir.mkdir(parents=True, exist_ok=True)
        
        # Validate configuration
        if not self.postman_api_key:
            print("PostmanCollectionGenerator: ⚠ Postman API key not configured (set POSTMAN_API_KEY)")
        if not self.workspace_id:
            print("PostmanCollectionGenerator: ⚠ Postman workspace ID not configured (set POSTMAN_WORKSPACE_ID)")
    
    def generate_pod_create_collection(
        self, 
        base_url: str = "http://localhost:8080",
        collection_name: str = None
    ) -> Dict[str, Any]:
        """
        Generate Postman collection for POD create with various test cases.
        
        Args:
            base_url: Base URL for API requests
            collection_name: Name for the collection (auto-generated if not provided)
        
        Returns:
            Generated collection dictionary
        """
        if not collection_name:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            collection_name = f"POD Create - Test Cases - {timestamp}"
        
        print(f"\n{'='*70}")
        print(f"PostmanCollectionGenerator: Generating POD Create Collection")
        print(f"{'='*70}")
        print(f"Collection Name: {collection_name}")
        print(f"Base URL: {base_url}")
        print("-"*70)
        
        # Generate test cases
        test_cases = self._generate_pod_create_test_cases(base_url)
        
        # Create collection structure
        collection = {
            "info": {
                "name": collection_name,
                "description": "Automated test cases for POD (Point of Delivery) Create endpoint with various scenarios",
                "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
                "_exporter_id": "automation-agent"
            },
            "item": test_cases,
            "variable": [
                {
                    "key": "base_url",
                    "value": base_url,
                    "type": "string"
                }
            ]
        }
        
        print(f"PostmanCollectionGenerator: ✓ Generated {len(test_cases)} test cases")
        print(f"{'='*70}\n")
        
        return collection
    
    def _generate_pod_create_test_cases(self, base_url: str) -> List[Dict[str, Any]]:
        """
        Generate POD create test cases with different scenarios.
        
        Args:
            base_url: Base URL for API requests
        
        Returns:
            List of test case items
        """
        test_cases = []
        
        # Test Case 1: Successful POD Create - Basic Consumer
        test_cases.append(self._create_pod_test_case(
            name="1. POD Create - Success - Basic Consumer",
            description="Successful POD creation with minimal required fields for CONSUMER type",
            identifier="32XAUTOMATION" + datetime.now().strftime("%Y%m%d%H%M%S"),
            pod_type="CONSUMER",
            consumption_purpose="HOUSEHOLD",
            expected_status=200,
            is_success=True
        ))
        
        # Test Case 2: Successful POD Create - Generator
        test_cases.append(self._create_pod_test_case(
            name="2. POD Create - Success - Generator",
            description="Successful POD creation for GENERATOR type",
            identifier="32XAUTOMATION" + datetime.now().strftime("%Y%m%d%H%M%S") + "GEN",
            pod_type="GENERATOR",
            consumption_purpose="NON_HOUSEHOLD",
            expected_status=200,
            is_success=True
        ))
        
        # Test Case 3: Validation Error - Missing Required Fields
        test_cases.append(self._create_pod_test_case(
            name="3. POD Create - Validation Error - Missing Required Fields",
            description="Should fail with 400 when required fields are missing",
            identifier="",
            pod_type=None,
            consumption_purpose=None,
            expected_status=400,
            is_success=False,
            remove_required_fields=True
        ))
        
        # Test Case 4: Validation Error - Invalid Identifier Format
        test_cases.append(self._create_pod_test_case(
            name="4. POD Create - Validation Error - Invalid Identifier Format",
            description="Should fail with 400 when identifier contains invalid characters",
            identifier="INVALID-IDENTIFIER-123!@#",
            pod_type="CONSUMER",
            consumption_purpose="HOUSEHOLD",
            expected_status=400,
            is_success=False
        ))
        
        # Test Case 5: Validation Error - Identifier Too Long
        test_cases.append(self._create_pod_test_case(
            name="5. POD Create - Validation Error - Identifier Too Long",
            description="Should fail with 400 when identifier exceeds max length (33)",
            identifier="A" * 34,  # 34 characters (max is 33)
            pod_type="CONSUMER",
            consumption_purpose="HOUSEHOLD",
            expected_status=400,
            is_success=False
        ))
        
        # Test Case 6: Validation Error - Invalid Consumption Value
        test_cases.append(self._create_pod_test_case(
            name="6. POD Create - Validation Error - Invalid Consumption Value",
            description="Should fail with 400 when estimatedMonthlyAvgConsumption is out of range",
            identifier="32XAUTOMATION" + datetime.now().strftime("%Y%m%d%H%M%S") + "INV",
            pod_type="CONSUMER",
            consumption_purpose="HOUSEHOLD",
            estimated_monthly_avg_consumption=999999999,  # Too large
            expected_status=400,
            is_success=False
        ))
        
        # Test Case 7: Success - With SLP (Smart Load Profile)
        test_cases.append(self._create_pod_test_case(
            name="7. POD Create - Success - With SLP",
            description="Successful POD creation with SLP enabled",
            identifier="32XAUTOMATION" + datetime.now().strftime("%Y%m%d%H%M%S") + "SLP",
            pod_type="CONSUMER",
            consumption_purpose="HOUSEHOLD",
            slp=True,
            expected_status=200,
            is_success=True
        ))
        
        # Test Case 8: Success - With Settlement Period
        test_cases.append(self._create_pod_test_case(
            name="8. POD Create - Success - With Settlement Period",
            description="Successful POD creation with settlement period enabled",
            identifier="32XAUTOMATION" + datetime.now().strftime("%Y%m%d%H%M%S") + "SET",
            pod_type="CONSUMER",
            consumption_purpose="HOUSEHOLD",
            settlement_period=True,
            expected_status=200,
            is_success=True
        ))
        
        # Test Case 9: Success - Non-Household Consumer
        test_cases.append(self._create_pod_test_case(
            name="9. POD Create - Success - Non-Household Consumer",
            description="Successful POD creation for NON_HOUSEHOLD consumption purpose",
            identifier="32XAUTOMATION" + datetime.now().strftime("%Y%m%d%H%M%S") + "NH",
            pod_type="CONSUMER",
            consumption_purpose="NON_HOUSEHOLD",
            expected_status=200,
            is_success=True
        ))
        
        # Test Case 10: Validation Error - Duplicate Identifier
        duplicate_id = "32XAUTOMATION" + datetime.now().strftime("%Y%m%d%H%M%S") + "DUP"
        # First create a successful one
        test_cases.append(self._create_pod_test_case(
            name="10a. POD Create - Success - For Duplicate Test",
            description="Create POD for duplicate identifier test",
            identifier=duplicate_id,
            pod_type="CONSUMER",
            consumption_purpose="HOUSEHOLD",
            expected_status=200,
            is_success=True
        ))
        # Then try to create duplicate
        test_cases.append(self._create_pod_test_case(
            name="10b. POD Create - Validation Error - Duplicate Identifier",
            description="Should fail with 400 when identifier already exists",
            identifier=duplicate_id,
            pod_type="CONSUMER",
            consumption_purpose="HOUSEHOLD",
            expected_status=400,
            is_success=False
        ))
        
        return test_cases
    
    def _create_pod_test_case(
        self,
        name: str,
        description: str,
        identifier: str,
        pod_type: Optional[str],
        consumption_purpose: Optional[str],
        expected_status: int = 200,
        is_success: bool = True,
        estimated_monthly_avg_consumption: int = 1000,
        slp: bool = False,
        settlement_period: bool = False,
        remove_required_fields: bool = False
    ) -> Dict[str, Any]:
        """
        Create a POD test case item.
        
        Args:
            name: Test case name
            description: Test case description
            identifier: POD identifier
            pod_type: POD type (CONSUMER or GENERATOR)
            consumption_purpose: Consumption purpose (HOUSEHOLD or NON_HOUSEHOLD)
            expected_status: Expected HTTP status code
            is_success: Whether this is a success case
            estimated_monthly_avg_consumption: Estimated monthly average consumption
            slp: Enable SLP (Smart Load Profile)
            settlement_period: Enable settlement period
            remove_required_fields: Remove required fields for validation error test
        
        Returns:
            Postman item dictionary
        """
        # Generate unique name using timestamp pattern
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        unique_name = f"AUTOMATION{timestamp}"
        
        # Build request body
        if remove_required_fields:
            # Minimal invalid request
            body = {
                "name": ""
            }
        else:
            body = {
                "identifier": identifier,
                "name": f"POD {unique_name}",
                "additionalIdentifier": f"ADD{timestamp[:10]}",
                "type": pod_type,
                "estimatedMonthlyAvgConsumption": estimated_monthly_avg_consumption,
                "consumptionPurpose": consumption_purpose,
                "gridOperatorId": 1,  # Default, should be configured
                "balancingGroupCoordinatorId": 1,  # Default, should be configured
                "userTypeId": 1,  # Default, should be configured
                "voltageLevel": "LOW",  # Default voltage level
                "addressRequest": {
                    "localAddressData": {
                        "countryId": 1,
                        "regionId": 1,
                        "municipalityId": 1,
                        "settlementId": 1,
                        "street": "Test Street",
                        "streetNumber": "1"
                    },
                    "foreign": False
                }
            }
            
            # Add optional fields
            if slp:
                body["slp"] = True
                body["measurementTypeId"] = 1  # Should be configured
            
            if settlement_period:
                body["settlementPeriod"] = True
        
        # Create test script
        test_script = self._create_test_script(expected_status, is_success, identifier)
        
        # Create Postman item
        item = {
            "name": name,
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": json.dumps(body, indent=2)
                },
                "url": {
                    "raw": "{{base_url}}/api/pod",
                    "host": ["{{base_url}}"],
                    "path": ["api", "pod"]
                },
                "description": description
            },
            "response": [],
            "event": [
                {
                    "listen": "test",
                    "script": {
                        "exec": test_script,
                        "type": "text/javascript"
                    }
                }
            ]
        }
        
        return item
    
    def _create_test_script(self, expected_status: int, is_success: bool, identifier: str) -> List[str]:
        """
        Create Postman test script for validation.
        
        Args:
            expected_status: Expected HTTP status code
            is_success: Whether this is a success case
            identifier: POD identifier for assertions
        
        Returns:
            List of test script lines
        """
        script = [
            "pm.test('Status code is " + str(expected_status) + "', function () {",
            "    pm.response.to.have.status(" + str(expected_status) + ");",
            "});"
        ]
        
        if is_success and expected_status == 200:
            script.extend([
                "",
                "pm.test('Response has POD data', function () {",
                "    const jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property('id');",
                "    pm.expect(jsonData).to.have.property('identifier');",
                "    pm.expect(jsonData.identifier).to.eql('" + identifier + "');",
                "});",
                "",
                "pm.test('Response time is less than 5000ms', function () {",
                "    pm.expect(pm.response.responseTime).to.be.below(5000);",
                "});"
            ])
        else:
            script.extend([
                "",
                "pm.test('Response contains error message', function () {",
                "    const jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property('message');",
                "    pm.expect(jsonData.message).to.be.a('string');",
                "});"
            ])
        
        return script
    
    def save_collection_locally(self, collection: Dict[str, Any], filename: str = None) -> Path:
        """
        Save collection to local file.
        
        Args:
            collection: Collection dictionary
            filename: Filename (auto-generated if not provided)
        
        Returns:
            Path to saved file
        """
        if not filename:
            collection_name = collection['info']['name']
            # Sanitize filename
            filename = collection_name.replace(" ", "_").replace("/", "-") + ".postman_collection.json"
        
        file_path = self.collections_dir / filename
        
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(collection, f, indent=2, ensure_ascii=False)
        
        print(f"PostmanCollectionGenerator: ✓ Collection saved locally: {file_path}")
        return file_path
    
    def upload_to_postman(self, collection: Dict[str, Any]) -> Dict[str, Any]:
        """
        Upload collection to Postman workspace.
        
        Args:
            collection: Collection dictionary
        
        Returns:
            Upload result dictionary
        """
        if not self.postman_api_key:
            return {
                'success': False,
                'error': 'Postman API key not configured',
                'message': 'Set POSTMAN_API_KEY environment variable'
            }
        
        if not self.workspace_id:
            return {
                'success': False,
                'error': 'Postman workspace ID not configured',
                'message': 'Set POSTMAN_WORKSPACE_ID environment variable'
            }
        
        print(f"\n{'='*70}")
        print(f"PostmanCollectionGenerator: Uploading Collection to Postman")
        print(f"{'='*70}")
        print(f"Collection: {collection['info']['name']}")
        print(f"Workspace ID: {self.workspace_id}")
        print("-"*70)
        
        headers = {
            'X-Api-Key': self.postman_api_key,
            'Content-Type': 'application/json'
        }
        
        # Prepare collection for upload (Postman API format)
        collection_data = {
            'collection': collection
        }
        
        if self.workspace_id:
            collection_data['workspace'] = self.workspace_id
        
        url = f"{self.postman_api_url}/collections"
        
        try:
            response = requests.post(url, headers=headers, json=collection_data, timeout=30)
            
            if response.status_code in [200, 201]:
                result = response.json()
                collection_info = result.get('collection', {})
                
                print(f"PostmanCollectionGenerator: ✓ Collection uploaded successfully")
                print(f"Collection ID: {collection_info.get('uid', 'N/A')}")
                print(f"Collection URL: https://app.getpostman.com/collection/{collection_info.get('uid', '')}")
                print(f"{'='*70}\n")
                
                return {
                    'success': True,
                    'collection_id': collection_info.get('uid'),
                    'collection_name': collection_info.get('name'),
                    'message': 'Collection uploaded successfully',
                    'url': f"https://app.getpostman.com/collection/{collection_info.get('uid', '')}"
                }
            else:
                error_msg = response.text[:500]
                print(f"PostmanCollectionGenerator: ✗ Upload failed: {response.status_code}")
                print(f"Error: {error_msg}")
                print(f"{'='*70}\n")
                
                return {
                    'success': False,
                    'error': f'HTTP {response.status_code}',
                    'message': error_msg,
                    'status_code': response.status_code
                }
        
        except requests.exceptions.RequestException as e:
            print(f"PostmanCollectionGenerator: ✗ Upload error: {str(e)}")
            print(f"{'='*70}\n")
            
            return {
                'success': False,
                'error': str(e),
                'message': f'Error uploading collection: {str(e)}'
            }
    
    def generate_and_upload_pod_collection(
        self,
        base_url: str = "http://localhost:8080",
        collection_name: str = None,
        upload: bool = True
    ) -> Dict[str, Any]:
        """
        Generate POD create collection and optionally upload to Postman.
        
        Args:
            base_url: Base URL for API requests
            collection_name: Name for the collection
            upload: Whether to upload to Postman workspace
        
        Returns:
            Result dictionary with collection and upload status
        """
        # Generate collection
        collection = self.generate_pod_create_collection(base_url, collection_name)
        
        # Save locally
        file_path = self.save_collection_locally(collection)
        
        # Upload to Postman if requested
        upload_result = None
        if upload:
            upload_result = self.upload_to_postman(collection)
        
        return {
            'collection': collection,
            'file_path': str(file_path),
            'upload_result': upload_result,
            'success': upload_result['success'] if upload_result else True
        }


# Global generator instance
_postman_generator = None

def get_postman_collection_generator(config: Dict[str, Any] = None) -> PostmanCollectionGenerator:
    """Get or create global Postman collection generator instance."""
    global _postman_generator
    if _postman_generator is None:
        _postman_generator = PostmanCollectionGenerator(config)
    return _postman_generator


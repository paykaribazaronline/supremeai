"""
SupremeAI Python SDK
Auto-generated API client for SupremeAI Platform

Installation:
    pip install supremeai-sdk

Usage:
    from supremeai import Client
    
    client = Client(token='your-jwt-token')
    webhooks = client.list_webhooks()
"""

import requests
import json
from typing import Optional, Dict, Any, List


class SupremeAIClient:
    """Client for SupremeAI API"""
    
    def __init__(self, token: str, base_url: str = 'https://api.supremeai.example.com/api', timeout: int = 30):
        """
        Initialize SupremeAI client
        
        Args:
            token: JWT authentication token
            base_url: Base URL for API endpoints
            timeout: Request timeout in seconds
        """
        if not token:
            raise ValueError('JWT token is required')
            
        self.base_url = base_url
        self.token = token
        self.timeout = timeout
        self.headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }

    def _request(self, method: str, path: str, data: Optional[Dict] = None) -> Dict[str, Any]:
        """Make HTTP request to API"""
        url = f'{self.base_url}{path}'
        
        try:
            response = requests.request(
                method=method,
                url=url,
                json=data,
                headers=self.headers,
                timeout=self.timeout
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            raise Exception(f'API request failed: {str(e)}')

    # API Information
    def get_api_info(self) -> Dict[str, Any]:
        """Get API root information"""
        return self._request('GET', '/')

    def get_api_info_v1(self) -> Dict[str, Any]:
        """Get API v1 information (deprecated)"""
        return self._request('GET', '/v1/info')

    def get_api_info_v2(self) -> Dict[str, Any]:
        """Get API v2 information"""
        return self._request('GET', '/v2/info')

    # Webhooks
    def register_webhook(self, project_id: str, url: str, events: List[str], 
                        secret_key: str) -> Dict[str, Any]:
        """Register a new webhook"""
        return self._request('POST', '/v2/webhooks', {
            'projectId': project_id,
            'url': url,
            'events': events,
            'secretKey': secret_key
        })

    def get_webhook(self, webhook_id: str) -> Dict[str, Any]:
        """Get webhook details"""
        return self._request('GET', f'/v2/webhooks/{webhook_id}')

    def list_webhooks(self) -> List[Dict[str, Any]]:
        """List all webhooks"""
        return self._request('GET', '/v2/webhooks')

    def test_webhook(self, webhook_id: str, payload: Optional[Dict] = None) -> Dict[str, Any]:
        """Send test payload to webhook"""
        test_payload = {'test': True}
        if payload:
            test_payload.update(payload)
        return self._request('POST', f'/v2/webhooks/{webhook_id}/test', test_payload)

    def delete_webhook(self, webhook_id: str) -> None:
        """Delete a webhook"""
        self._request('DELETE', f'/v2/webhooks/{webhook_id}')

    # Batch Operations
    def create_batch(self, name: str) -> Dict[str, Any]:
        """Create a new batch"""
        return self._request('POST', '/v2/batch', {'name': name})

    def get_batch(self, batch_id: str) -> Dict[str, Any]:
        """Get batch details"""
        return self._request('GET', f'/v2/batch/{batch_id}')

    def list_batches(self) -> List[Dict[str, Any]]:
        """List all batches"""
        return self._request('GET', '/v2/batch')

    def add_request_to_batch(self, batch_id: str, request: Dict[str, Any]) -> Dict[str, Any]:
        """Add request to batch"""
        return self._request('POST', f'/v2/batch/{batch_id}/requests', request)

    def execute_batch(self, batch_id: str) -> Dict[str, Any]:
        """Execute batch"""
        return self._request('POST', f'/v2/batch/{batch_id}/execute')

    def cancel_batch(self, batch_id: str) -> Dict[str, Any]:
        """Cancel batch"""
        return self._request('POST', f'/v2/batch/{batch_id}/cancel')

    def clear_completed_batches(self) -> Dict[str, Any]:
        """Clear completed batches"""
        return self._request('DELETE', '/v2/batch/completed')

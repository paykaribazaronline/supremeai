/**
 * SupremeAI JavaScript SDK
 * Auto-generated API client for SupremeAI Platform
 * 
 * Installation:
 *   npm install supremeai-sdk
 * 
 * Usage:
 *   const SupremeAI = require('supremeai-sdk');
 *   const client = new SupremeAI.Client({ token: 'your-jwt-token' });
 */

class SupremeAIClient {
  constructor(options = {}) {
    this.baseURL = options.baseURL || 'https://api.supremeai.example.com/api';
    this.token = options.token;
    this.timeout = options.timeout || 30000;
    
    if (!this.token) {
      throw new Error('JWT token is required');
    }
  }

  async request(method, path, data = null) {
    const url = `${this.baseURL}${path}`;
    const headers = {
      'Authorization': `Bearer ${this.token}`,
      'Content-Type': 'application/json'
    };

    const config = {
      method,
      headers,
      timeout: this.timeout
    };

    if (data) {
      config.body = JSON.stringify(data);
    }

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      throw new Error(`API request failed: ${error.message}`);
    }
  }

  // API Information
  async getAPIInfo() {
    return this.request('GET', '/');
  }

  async getAPIInfoV1() {
    return this.request('GET', '/v1/info');
  }

  async getAPIInfoV2() {
    return this.request('GET', '/v2/info');
  }

  // Webhooks
  async registerWebhook(projectId, url, events, secretKey) {
    return this.request('POST', '/v2/webhooks', {
      projectId,
      url,
      events,
      secretKey
    });
  }

  async getWebhook(webhookId) {
    return this.request('GET', `/v2/webhooks/${webhookId}`);
  }

  async listWebhooks() {
    return this.request('GET', '/v2/webhooks');
  }

  async testWebhook(webhookId, payload = {}) {
    return this.request('POST', `/v2/webhooks/${webhookId}/test`, {
      test: true,
      ...payload
    });
  }

  async deleteWebhook(webhookId) {
    return this.request('DELETE', `/v2/webhooks/${webhookId}`);
  }

  // Batch Operations
  async createBatch(name) {
    return this.request('POST', '/v2/batch', { name });
  }

  async getBatch(batchId) {
    return this.request('GET', `/v2/batch/${batchId}`);
  }

  async listBatches() {
    return this.request('GET', '/v2/batch');
  }

  async addRequestToBatch(batchId, request) {
    return this.request('POST', `/v2/batch/${batchId}/requests`, request);
  }

  async executeBatch(batchId) {
    return this.request('POST', `/v2/batch/${batchId}/execute`);
  }

  async cancelBatch(batchId) {
    return this.request('POST', `/v2/batch/${batchId}/cancel`);
  }

  async clearCompletedBatches() {
    return this.request('DELETE', '/v2/batch/completed');
  }
}

module.exports = { Client: SupremeAIClient };

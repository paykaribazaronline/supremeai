package supremeai

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

/*
SupremeAI Go SDK
Auto-generated API client for SupremeAI Platform

Installation:
    go get github.com/supremeai/go-sdk

Usage:
    import "github.com/supremeai/go-sdk"
    
    client := supremeai.NewClient("your-jwt-token")
    webhooks, err := client.ListWebhooks()
*/

// Client represents SupremeAI API client
type Client struct {
	BaseURL    string
	Token      string
	HTTPClient *http.Client
}

// NewClient creates a new SupremeAI client
func NewClient(token string) (*Client, error) {
	return NewClientWithBaseURL(token, "https://api.supremeai.example.com/api")
}

// NewClientWithBaseURL creates a new client with custom base URL
func NewClientWithBaseURL(token, baseURL string) (*Client, error) {
	if token == "" {
		return nil, fmt.Errorf("JWT token is required")
	}

	return &Client{
		BaseURL: baseURL,
		Token:   token,
		HTTPClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}, nil
}

// request makes HTTP request to API
func (c *Client) request(method, path string, body interface{}) (map[string]interface{}, error) {
	url := c.BaseURL + path

	var reqBody io.Reader
	if body != nil {
		jsonBody, err := json.Marshal(body)
		if err != nil {
			return nil, fmt.Errorf("failed to marshal request body: %w", err)
		}
		reqBody = bytes.NewBuffer(jsonBody)
	} else {
		reqBody = nil
	}

	req, err := http.NewRequest(method, url, reqBody)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Authorization", fmt.Sprintf("Bearer %s", c.Token))
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to send request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		bodyBytes, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("HTTP %d: %s", resp.StatusCode, string(bodyBytes))
	}

	var result map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return result, nil
}

// GetAPIInfo retrieves API root information
func (c *Client) GetAPIInfo() (map[string]interface{}, error) {
	return c.request("GET", "/", nil)
}

// GetAPIInfoV1 retrieves API v1 information (deprecated)
func (c *Client) GetAPIInfoV1() (map[string]interface{}, error) {
	return c.request("GET", "/v1/info", nil)
}

// GetAPIInfoV2 retrieves API v2 information
func (c *Client) GetAPIInfoV2() (map[string]interface{}, error) {
	return c.request("GET", "/v2/info", nil)
}

// WebhookRequest represents a webhook registration request
type WebhookRequest struct {
	ProjectID string   `json:"projectId"`
	URL       string   `json:"url"`
	Events    []string `json:"events"`
	SecretKey string   `json:"secretKey"`
}

// RegisterWebhook registers a new webhook
func (c *Client) RegisterWebhook(projectID, url string, events []string, secretKey string) (map[string]interface{}, error) {
	body := WebhookRequest{
		ProjectID: projectID,
		URL:       url,
		Events:    events,
		SecretKey: secretKey,
	}
	return c.request("POST", "/v2/webhooks", body)
}

// GetWebhook retrieves webhook details
func (c *Client) GetWebhook(webhookID string) (map[string]interface{}, error) {
	return c.request("GET", fmt.Sprintf("/v2/webhooks/%s", webhookID), nil)
}

// ListWebhooks lists all webhooks
func (c *Client) ListWebhooks() ([]map[string]interface{}, error) {
	result, err := c.request("GET", "/v2/webhooks", nil)
	if err != nil {
		return nil, err
	}

	// Convert result to slice of maps
	var webhooks []map[string]interface{}
	webhooksJSON, _ := json.Marshal(result)
	json.Unmarshal(webhooksJSON, &webhooks)
	return webhooks, nil
}

// TestWebhook sends test payload to webhook
func (c *Client) TestWebhook(webhookID string, payload map[string]interface{}) (map[string]interface{}, error) {
	testPayload := map[string]interface{}{"test": true}
	for k, v := range payload {
		testPayload[k] = v
	}
	return c.request("POST", fmt.Sprintf("/v2/webhooks/%s/test", webhookID), testPayload)
}

// DeleteWebhook deletes a webhook
func (c *Client) DeleteWebhook(webhookID string) error {
	_, err := c.request("DELETE", fmt.Sprintf("/v2/webhooks/%s", webhookID), nil)
	return err
}

// CreateBatch creates a new batch
func (c *Client) CreateBatch(name string) (map[string]interface{}, error) {
	body := map[string]string{"name": name}
	return c.request("POST", "/v2/batch", body)
}

// GetBatch retrieves batch details
func (c *Client) GetBatch(batchID string) (map[string]interface{}, error) {
	return c.request("GET", fmt.Sprintf("/v2/batch/%s", batchID), nil)
}

// ListBatches lists all batches
func (c *Client) ListBatches() ([]map[string]interface{}, error) {
	result, err := c.request("GET", "/v2/batch", nil)
	if err != nil {
		return nil, err
	}

	var batches []map[string]interface{}
	batchesJSON, _ := json.Marshal(result)
	json.Unmarshal(batchesJSON, &batches)
	return batches, nil
}

// AddRequestToBatch adds request to batch
func (c *Client) AddRequestToBatch(batchID string, request map[string]interface{}) (map[string]interface{}, error) {
	return c.request("POST", fmt.Sprintf("/v2/batch/%s/requests", batchID), request)
}

// ExecuteBatch executes batch
func (c *Client) ExecuteBatch(batchID string) (map[string]interface{}, error) {
	return c.request("POST", fmt.Sprintf("/v2/batch/%s/execute", batchID), nil)
}

// CancelBatch cancels batch
func (c *Client) CancelBatch(batchID string) (map[string]interface{}, error) {
	return c.request("POST", fmt.Sprintf("/v2/batch/%s/cancel", batchID), nil)
}

// ClearCompletedBatches clears completed batches
func (c *Client) ClearCompletedBatches() (map[string]interface{}, error) {
	return c.request("DELETE", "/v2/batch/completed", nil)
}

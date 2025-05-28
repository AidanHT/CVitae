package com.cvitae.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple admin controller that doesn't depend on other services
 * This is a backup controller in case the main AdminController has dependency issues
 */
@RestController
@RequestMapping("/api/simple-admin")
@Slf4j
public class SimpleAdminController {

    @GetMapping("/ui")
    public ResponseEntity<String> getSimpleAdminUI() {
        log.info("🔧 Loading simple admin UI");
        
        String simpleUI = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>CVitae Simple Admin</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
                    .container { max-width: 1000px; margin: 0 auto; }
                    .card { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-align: center; padding: 30px; border-radius: 8px; }
                    .btn { background: #667eea; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; margin: 5px; }
                    .btn:hover { background: #5a67d8; }
                    .response { background: #f8f9fa; border: 1px solid #dee2e6; padding: 15px; border-radius: 4px; margin: 10px 0; }
                    .status-good { color: #28a745; }
                    .status-bad { color: #dc3545; }
                    .endpoint-list { background: #f8f9fa; padding: 15px; border-radius: 4px; }
                    .endpoint { font-family: monospace; margin: 5px 0; padding: 5px; background: white; border-radius: 3px; }
                    pre { overflow-x: auto; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔧 CVitae Simple Admin</h1>
                        <p>Basic API testing and monitoring interface</p>
                        <div id="status">
                            <span id="connection" class="status-good">● Connected</span>
                        </div>
                    </div>

                    <div class="card">
                        <h2>🏓 Quick Tests</h2>
                        <button class="btn" onclick="testPing()">Test Ping</button>
                        <button class="btn" onclick="testHealth()">Test Health</button>
                        <button class="btn" onclick="testStatus()">Test Status</button>
                        <button class="btn" onclick="getAllEndpoints()">List Endpoints</button>
                        
                        <div id="quick-result" class="response" style="display: none;">
                            <h3>Result:</h3>
                            <pre id="quick-content"></pre>
                        </div>
                    </div>

                    <div class="card">
                        <h2>🔬 Custom API Test</h2>
                        <div style="margin: 10px 0;">
                            <label>Method:</label>
                            <select id="method">
                                <option value="GET">GET</option>
                                <option value="POST">POST</option>
                            </select>
                        </div>
                        <div style="margin: 10px 0;">
                            <label>Endpoint:</label>
                            <input type="text" id="endpoint" style="width: 300px; padding: 5px;" placeholder="/api/health" value="/api/health">
                        </div>
                        <div style="margin: 10px 0;" id="body-section" style="display: none;">
                            <label>Request Body (JSON):</label><br>
                            <textarea id="request-body" rows="4" style="width: 100%; padding: 5px;" placeholder='{"key": "value"}'></textarea>
                        </div>
                        <button class="btn" onclick="testCustomEndpoint()">Test Endpoint</button>
                        
                        <div id="custom-result" class="response" style="display: none;">
                            <h3>Response:</h3>
                            <pre id="custom-content"></pre>
                        </div>
                    </div>

                    <div class="card">
                        <h2>📊 Available Endpoints</h2>
                        <div class="endpoint-list">
                            <div class="endpoint">GET /api/health - Health check</div>
                            <div class="endpoint">GET /api/admin/status - System status</div>
                            <div class="endpoint">GET /api/admin/logs - Application logs</div>
                            <div class="endpoint">GET /api/test/ping - Simple ping</div>
                            <div class="endpoint">POST /api/resumes/generate - Generate resume</div>
                            <div class="endpoint">POST /api/export/pdf - Export PDF</div>
                            <div class="endpoint">POST /api/export/latex - Export LaTeX</div>
                            <div class="endpoint">GET /api/export/debug/{sessionId} - Debug info</div>
                        </div>
                    </div>

                    <div class="card">
                        <h2>💡 Instructions</h2>
                        <p>This is a simplified admin interface that works independently of other services.</p>
                        <ul>
                            <li>Use the Quick Tests to verify basic functionality</li>
                            <li>Test custom endpoints using the API test section</li>
                            <li>Check the browser console (F12) for detailed error information</li>
                            <li>For the full admin interface, try <a href="/api/admin/ui">/api/admin/ui</a> once dependencies are resolved</li>
                        </ul>
                    </div>
                </div>

                <script>
                    // Show/hide request body based on method
                    document.getElementById('method').addEventListener('change', function() {
                        const bodySection = document.getElementById('body-section');
                        bodySection.style.display = this.value === 'POST' ? 'block' : 'none';
                    });

                    async function makeRequest(url, options = {}) {
                        try {
                            console.log('Making request to:', url, options);
                            const response = await fetch(url, options);
                            const data = await response.json();
                            updateConnectionStatus(true);
                            return { success: response.ok, data, status: response.status };
                        } catch (error) {
                            console.error('Request failed:', error);
                            updateConnectionStatus(false);
                            return { success: false, error: error.message };
                        }
                    }

                    function updateConnectionStatus(connected) {
                        const status = document.getElementById('connection');
                        if (connected) {
                            status.className = 'status-good';
                            status.textContent = '● Connected';
                        } else {
                            status.className = 'status-bad';
                            status.textContent = '● Disconnected';
                        }
                    }

                    function showResult(elementId, result) {
                        const resultDiv = document.getElementById(elementId);
                        const contentPre = document.getElementById(elementId.replace('result', 'content'));
                        
                        resultDiv.style.display = 'block';
                        contentPre.textContent = JSON.stringify(result, null, 2);
                    }

                    async function testPing() {
                        const result = await makeRequest('/api/test/ping');
                        showResult('quick-result', result);
                    }

                    async function testHealth() {
                        const result = await makeRequest('/api/health');
                        showResult('quick-result', result);
                    }

                    async function testStatus() {
                        const result = await makeRequest('/api/simple-admin/status');
                        showResult('quick-result', result);
                    }

                    async function getAllEndpoints() {
                        const result = await makeRequest('/api/admin/endpoints');
                        showResult('quick-result', result);
                    }

                    async function testCustomEndpoint() {
                        const method = document.getElementById('method').value;
                        const endpoint = document.getElementById('endpoint').value;
                        const requestBody = document.getElementById('request-body').value;

                        let options = { method };
                        
                        if (method === 'POST' && requestBody.trim()) {
                            options.headers = { 'Content-Type': 'application/json' };
                            options.body = requestBody;
                        }

                        const result = await makeRequest(endpoint, options);
                        showResult('custom-result', result);
                    }

                    // Initialize
                    testPing();
                </script>
            </body>
            </html>
            """;
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(simpleUI);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSimpleStatus() {
        log.info("🔍 Getting simple status");
        
        Map<String, Object> status = new HashMap<>();
        status.put("service", "CVitae Backend");
        status.put("timestamp", LocalDateTime.now());
        status.put("status", "running");
        
        // Basic system info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        system.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        system.put("processors", runtime.availableProcessors());
        status.put("system", system);
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint(@RequestBody(required = false) Map<String, Object> request) {
        log.info("🧪 Test endpoint called with: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test endpoint is working");
        response.put("received", request);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}

# BOSH MCP Server

A comprehensive Model Context Protocol (MCP) server that provides AI-powered access to BOSH Director operations through specialized tools. Built with [Spring AI 1.1.0 GA](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) and deployed on Cloud Foundry.

![BOSH MCP Server](https://img.shields.io/badge/BOSH-MCP%20Server-blue?style=for-the-badge)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.0-green?style=for-the-badge&logo=spring)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?style=for-the-badge&logo=springboot)
![MCP](https://img.shields.io/badge/MCP-Protocol-purple?style=for-the-badge)
![Streamable](https://img.shields.io/badge/Transport-HTTP%20Streamable-blue?style=for-the-badge)

![Version](https://img.shields.io/badge/Version-0.1.0-red?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-3.9+-red?style=for-the-badge&logo=apache-maven)
![Cloud Foundry](https://img.shields.io/badge/Deployed%20on-Cloud%20Foundry-0066CC?style=for-the-badge&logo=cloudfoundry)

## 🚀 Quick Start

### Prerequisites

- **BOSH Director Access**: Valid BOSH Director URL, client credentials, and CA certificate
- **Java 21**: For building and running the server
- **Maven 3.9+**: For building the project
- **Cloud Foundry CLI**: For deploying to Cloud Foundry (optional, for deployment only)

**Note**: The BOSH CLI is automatically installed in the container at runtime - no manual installation required!

### Step 1: Configure BOSH Credentials

Choose one of the following configuration methods:

#### Option A: Local Development (`.env` folder)

1. Create `.env` folder in the project root:
   ```bash
   mkdir -p .env
   ```

2. Create `.env/bosh-env.ini`:
   ```bash
   export BOSH_ENVIRONMENT=your-bosh-director-url
   export BOSH_CLIENT=your-bosh-client
   export BOSH_CLIENT_SECRET=your-bosh-client-secret
   export BOSH_CA_CERT=bosh.pem
   ```

3. Create `.env/bosh.pem` with your BOSH Director CA certificate:
   ```bash
   -----BEGIN CERTIFICATE-----
   YOUR_CA_CERTIFICATE_CONTENT
   -----END CERTIFICATE-----
   ```

#### Option B: Cloud Foundry Deployment (manifest.yml)

1. Copy the template:
   ```bash
   cp manifest-template.yml manifest.yml
   ```

2. Edit `manifest.yml` and replace the placeholders:
   - `YOUR_BOSH_DIRECTOR_URL` - Your BOSH Director IP or hostname
   - `YOUR_BOSH_CLIENT` - Your BOSH client username
   - `YOUR_BOSH_CLIENT_SECRET` - Your BOSH client secret
   - `YOUR_CA_CERTIFICATE_CONTENT_HERE` - Your BOSH Director CA certificate content

### Step 2: Build and Deploy

```bash
# Build the application
./mvnw clean package -DskipTests

# Deploy to Cloud Foundry (if using manifest.yml)
cf push bosh-mcp-server

# Get the deployed URL
cf apps
# Note the URL (e.g., https://bosh-mcp-server.apps.your-domain.com)
```

### Step 3: Configure MCP Client

Add the BOSH MCP server to your MCP client configuration:

```json
{
  "mcpServers": {
    "bosh-mcp": {
      "disabled": false,
      "timeout": 300,
      "type": "streamable",
      "url": "https://bosh-mcp-server.apps.your-cf-domain.com/mcp",
      "autoApprove": []
    }
  }
}
```

**Replace** `bosh-mcp-server.apps.your-cf-domain.com` with your actual deployed server URL from Step 2.

### Step 4: Verify Connection

Once configured, you can test the connection by asking your AI assistant to:
- "List BOSH deployments"
- "Show me the VMs in deployment X"
- "Get the status of deployment Y"

The MCP server will handle all BOSH operations automatically!

## 🛠 Building & Deployment

### Build the Server

```bash
./mvnw clean package -DskipTests
```

### Deploy to Cloud Foundry

```bash
# 1. Copy the template
cp manifest-template.yml manifest.yml

# 2. Edit manifest.yml with your BOSH credentials
#    Replace all placeholders (YOUR_BOSH_DIRECTOR_URL, etc.)

# 3. Build the application
./mvnw clean package -DskipTests

# 4. Deploy to Cloud Foundry
cf push bosh-mcp-server

# 5. Get the deployed URL
cf apps
# Look for your app and copy the URL (e.g., https://bosh-mcp-server.apps.your-domain.com)
```

**Note**: The BOSH CLI will be automatically downloaded and installed in the container at startup if not already available. This eliminates the need for a custom buildpack or Docker image.

### Local Development

```bash
# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## ⚙️ Configuration Guide

### How to Configure the MCP Server

The BOSH MCP Server supports two configuration methods, allowing flexibility for different deployment scenarios:

#### Configuration Priority

1. **Environment Variables** (highest priority) - Used in Cloud Foundry deployments
2. **`.env` folder** (fallback) - Used for local development

Environment variables always take precedence when both are present.

### Method 1: Local Development with `.env` Folder

This method is ideal for local development and testing. The `.env` folder is automatically ignored by git for security.

#### Step-by-Step Setup

1. **Create the `.env` directory**:
   ```bash
   mkdir -p .env
   ```

2. **Create `.env/bosh-env.ini`** with your BOSH credentials:
   ```bash
   cat > .env/bosh-env.ini << EOF
   export BOSH_ENVIRONMENT=10.0.10.51
   export BOSH_CLIENT=ops_manager
   export BOSH_CLIENT_SECRET=your-client-secret-here
   export BOSH_CA_CERT=bosh.pem
   EOF
   ```

3. **Create `.env/bosh.pem`** with your BOSH Director CA certificate:
   ```bash
   cat > .env/bosh.pem << EOF
   -----BEGIN CERTIFICATE-----
   YOUR_CA_CERTIFICATE_CONTENT_HERE
   -----END CERTIFICATE-----
   EOF
   ```

4. **Run the server locally**:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

The server will automatically read configuration from the `.env` folder at startup.

#### Getting Your BOSH Credentials

If you have access to your BOSH Director, you can get the credentials:

```bash
# Source your BOSH environment file (if you have one)
source bosh-env.ini

# Or get credentials from BOSH Director
bosh alias-env my-env -e <director-ip> --ca-cert <cert-path>
```

### Method 2: Cloud Foundry Deployment with manifest.yml

This method is used when deploying to Cloud Foundry. Credentials are provided via environment variables in the manifest.

#### Step-by-Step Setup

1. **Copy the template**:
   ```bash
   cp manifest-template.yml manifest.yml
   ```

2. **Edit `manifest.yml`** and replace all placeholders:

   ```yaml
   env:
     # Required: BOSH Director URL (IP address or hostname)
     BOSH_DIRECTOR: "10.0.10.51"  # Replace with your Director URL
     
     # Required: BOSH client credentials
     BOSH_CLIENT: "ops_manager"  # Replace with your client name
     BOSH_CLIENT_SECRET: "your-secret-here"  # Replace with your secret
     
     # Required: BOSH Director CA Certificate
     BOSH_CA_CERT: |
       -----BEGIN CERTIFICATE-----
       YOUR_CERTIFICATE_CONTENT_HERE
       -----END CERTIFICATE-----
   ```

3. **Build and deploy**:
   ```bash
   ./mvnw clean package -DskipTests
   cf push bosh-mcp-server
   ```

#### Getting Your BOSH CA Certificate

You can obtain the CA certificate from your BOSH Director:

```bash
# Option 1: From BOSH Director info endpoint
curl -k https://<director-ip>:25555/info | jq -r '.cert'

# Option 2: From your BOSH environment file
# Look for BOSH_CA_CERT in your bosh-env.ini or similar file

# Option 3: From Ops Manager (if using Pivotal Ops Manager)
# Navigate to Ops Manager > BOSH Director > Credentials > Bosh Commandline Credentials
```

### Configuration Validation

The server automatically validates configuration on startup:

- ✅ **Required fields**: BOSH Director URL, client, and client secret
- ✅ **BOSH CLI**: Automatically installed if not found
- ✅ **Certificate**: Validated and loaded
- ✅ **Connectivity**: Tests connection to BOSH Director
- ⚠️ **Warnings**: Non-critical issues are logged but don't prevent startup

### Troubleshooting Configuration

**Problem**: "BOSH Director connection test failed"
- **Solution**: Verify your BOSH Director URL, credentials, and CA certificate are correct

**Problem**: "BOSH CLI not found"
- **Solution**: The CLI should auto-install. Check logs for installation errors

**Problem**: "Certificate verification failed"
- **Solution**: Ensure your CA certificate matches the BOSH Director's certificate

**Problem**: "Configuration not found"
- **Solution**: Check that either `.env` folder exists (local) or `manifest.yml` has all required environment variables (CF)

## ⚙️ Configuration Reference

### Configuration Variables

#### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `BOSH_DIRECTOR` or `BOSH_ENVIRONMENT` | BOSH Director URL (IP or hostname) | `10.0.10.51` or `bosh.example.com` |
| `BOSH_CLIENT` | BOSH client username | `ops_manager` |
| `BOSH_CLIENT_SECRET` | BOSH client password/secret | `your-secret-here` |
| `BOSH_CA_CERT` | BOSH Director CA certificate content | Full certificate with BEGIN/END lines |

#### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `BOSH_CLI_PATH` | Path to BOSH CLI binary | `bosh` (or auto-installed path) |
| `BOSH_CLI_INSTALL_ENABLED` | Enable automatic CLI installation | `true` |
| `BOSH_CLI_INSTALL_PATH` | Directory for CLI installation | `${java.io.tmpdir}/bosh-cli` |
| `BOSH_CA_CERT_PATH` | Path to certificate file (alternative to BOSH_CA_CERT) | Not set |
| `BOSH_CONNECTION_TIMEOUT` | Connection timeout in seconds | `60` |
| `BOSH_RETRY_MAX_ATTEMPTS` | Maximum retry attempts for failed operations | `3` |
| `BOSH_RETRY_DELAY` | Delay between retries in seconds | `2` |

#### BOSH CLI Auto-Installation

The server automatically downloads and installs the BOSH CLI binary (v7.9.5) in the container at startup if:
- The BOSH CLI is not found in PATH
- No custom `BOSH_CLI_PATH` is configured
- Auto-installation is enabled (default: `true`)

The CLI is installed to `${java.io.tmpdir}/bosh-cli/bosh` by default, or to the path specified by `BOSH_CLI_INSTALL_PATH`.

This feature allows the MCP server to run in Cloud Foundry containers without requiring:
- Custom buildpacks
- Docker images with pre-installed BOSH CLI
- Manual BOSH CLI installation

The CLI binary is downloaded from GitHub releases and made executable automatically.

### Configuration Validation

The server includes automatic configuration validation on startup:
- **Required**: BOSH Director URL, client, and client secret must be configured
- **Auto-Installation**: BOSH CLI is automatically downloaded and installed if not found in PATH
- **Optional**: CA certificate (warning if not set)
- **Startup Check**: Validates BOSH CLI availability and Director connectivity
- **Error Handling**: Fails fast with clear error messages if configuration is invalid

### Application Properties

```properties
spring.ai.mcp.server.name=bosh-mcp
spring.ai.mcp.server.version=0.1.0
spring.ai.mcp.server.prompt-change-notification=false
spring.ai.mcp.server.resource-change-notification=false
spring.ai.mcp.server.protocol=streamable

management.endpoints.web.exposure.include=health,info,mappings
management.endpoint.health.show-details=always

logging.level.io.modelcontextprotocol=DEBUG
logging.level.org.springframework.ai.mcp=DEBUG

# BOSH Connection Settings
bosh.connection.timeout=60

# BOSH Retry Settings
bosh.retry.maxAttempts=3
bosh.retry.delay=2
```

## 🛠 Capabilities & Tools

This MCP server exposes **31 comprehensive BOSH operations** as AI-powered tools, organized into 7 categories:

### 📦 Deployment Management (6 tools)

Manage BOSH deployments with full lifecycle operations:

- **`listDeployments`** - List all BOSH deployments in the Director
  - Returns: Array of deployment names
  - Example: `["cf-deployment", "pivotal-mysql", ...]`

- **`getDeployment`** - Get detailed information about a specific deployment
  - Parameters: `deploymentName` (required)
  - Returns: Deployment details including VMs, releases, stemcells, and configuration

- **`deployDeployment`** - Deploy or update a BOSH deployment from a manifest file
  - Parameters: `deploymentName`, `manifestPath` (path to manifest YAML file)
  - Returns: Deployment task information

- **`deleteDeployment`** - Delete a BOSH deployment
  - Parameters: `deploymentName` (required)
  - Returns: Deletion task status

- **`recreateDeployment`** - Recreate all VMs in a deployment
  - Parameters: `deploymentName` (required)
  - Use case: Force VM recreation for troubleshooting or updates

- **`updateDeployment`** - Update an existing deployment configuration
  - Parameters: `deploymentName`, `manifestPath`
  - Returns: Update task information

### 🖥️ VM Management (6 tools)

Control and monitor individual VMs within deployments:

- **`listVms`** - List all VMs in a deployment with status and details
  - Parameters: `deploymentName` (required)
  - Returns: VM list with IPs, states, instance IDs, and VM types
  - Example output: Table with columns (Instance, IPs, Process State, VM Type, AZ, Stemcell)

- **`getVmStatus`** - Get detailed status and information for VMs
  - Parameters: `deploymentName` (required)
  - Returns: Comprehensive VM status including health, resources, and metadata

- **`startVm`** - Start a stopped VM
  - Parameters: `deploymentName`, `instanceGroup`, `instanceId` (optional)
  - Returns: Start task status

- **`stopVm`** - Stop a running VM
  - Parameters: `deploymentName`, `instanceGroup`, `instanceId` (optional)
  - Returns: Stop task status

- **`restartVm`** - Restart a VM
  - Parameters: `deploymentName`, `instanceGroup`, `instanceId` (optional)
  - Returns: Restart task status

- **`recreateVm`** - Recreate a specific VM
  - Parameters: `deploymentName`, `instanceGroup`, `instanceId` (optional)
  - Use case: Troubleshooting or applying configuration changes

### 📋 Log Management (4 tools)

Access and stream logs from deployments, VMs, and tasks:

- **`getDeploymentLogs`** - Retrieve logs from a deployment
  - Parameters: `deploymentName`, `instanceGroup` (optional), `instanceId` (optional)
  - Returns: Log content for analysis

- **`getVmLogs`** - Get logs from a specific VM
  - Parameters: `deploymentName`, `instanceGroup`, `instanceId` (optional)
  - Returns: VM-specific log output

- **`getTaskLogs`** - Retrieve logs from a BOSH task
  - Parameters: `taskId` (required)
  - Returns: Task execution logs

- **`streamLogs`** - Stream logs from a deployment in real-time
  - Parameters: `deploymentName`, `instanceGroup` (optional)
  - Returns: Real-time log stream
  - Use case: Live monitoring and debugging

### 🎯 Stemcell Management (3 tools)

Manage BOSH stemcells:

- **`listStemcells`** - List all available stemcells
  - Returns: Stemcell list with versions, names, and OS information

- **`uploadStemcell`** - Upload a new stemcell to the Director
  - Parameters: `stemcellPath` (file path or URL)
  - Returns: Upload task status

- **`deleteStemcell`** - Delete a stemcell
  - Parameters: `stemcellName`, `version` (required)
  - Returns: Deletion confirmation

### 📦 Release Management (4 tools)

Manage BOSH releases:

- **`listReleases`** - List all available releases
  - Returns: Release list with versions and names

- **`uploadRelease`** - Upload a new release
  - Parameters: `releasePath` (file path or URL)
  - Returns: Upload task information

- **`deleteRelease`** - Delete a release
  - Parameters: `releaseName`, `version` (optional)
  - Returns: Deletion status

- **`getReleaseVersions`** - Get all versions of a specific release
  - Parameters: `releaseName` (required)
  - Returns: List of available versions

### 🔧 Errand Management (3 tools)

Execute and monitor errands:

- **`listErrands`** - List all errands for a deployment
  - Parameters: `deploymentName` (required)
  - Returns: Available errands list

- **`runErrand`** - Execute an errand
  - Parameters: `deploymentName`, `errandName` (required)
  - Returns: Errand execution task information

- **`getErrandStatus`** - Get the status of an errand execution
  - Parameters: `taskId` (required)
  - Returns: Errand execution status and results

### ☁️ Cloud Config Management (3 tools)

Manage BOSH cloud configuration:

- **`getCloudConfig`** - Get the current cloud config
  - Returns: Complete cloud config YAML

- **`updateCloudConfig`** - Update the cloud config
  - Parameters: `configPath` (path to cloud config file)
  - Returns: Update task status

- **`getCloudConfigDiff`** - Get diff of cloud config changes
  - Parameters: `configPath` (optional)
  - Returns: Diff showing proposed changes

### 🔐 SSH Operations (2 tools)

Access VMs via SSH:

- **`sshToVm`** - Get SSH connection information for a VM
  - Parameters: `deploymentName`, `instanceGroup`, `instanceId` (optional)
  - Returns: SSH connection details and credentials

- **`executeCommandOnVm`** - Execute a command on a VM via SSH
  - Parameters: `deploymentName`, `instanceGroup`, `command`, `instanceId` (optional)
  - Returns: Command output
  - Use case: Remote troubleshooting and administration

### 💡 Usage Examples

**Example 1: List all deployments and their VMs**
```
"List all BOSH deployments and show me the VMs in the Cloud Foundry deployment"
```

**Example 2: Monitor deployment health**
```
"Get the status of all VMs in the cf-deployment and show me any that are not running"
```

**Example 3: Troubleshooting**
```
"Get the logs from the database VM in the cf-deployment"
```

**Example 4: Deployment operations**
```
"Recreate the router VM in the cf-deployment"
```

All tools are automatically available to your AI assistant once the MCP server is configured and connected!

## 🔧 Technical Details

- **Spring AI Version**: 1.1.0 (GA)
- **Spring Boot Version**: 3.4.2
- **Java Version**: 21
- **Transport**: HTTP Streamable
- **Health Endpoint**: `/actuator/health`
- **Configuration**: Environment variable-based BOSH credentials
- **MCP Java SDK**: v0.15.0 (included with Spring AI 1.1.0)
- **BOSH CLI**: Uses BOSH CLI for all operations (must be installed)

## 📊 Health Status

The server provides comprehensive health monitoring:
- **Application Health**: Memory, disk, CPU usage
- **BOSH CLI Availability**: Checks if BOSH CLI is installed and accessible
- **BOSH Director Connectivity**: Tests connection to BOSH Director
- **MCP Server Status**: Tool registration and transport health
- **Retry Logic**: Automatic retry for transient network failures

## 🔒 Security

- **Credential Management**: Environment variable-based configuration
- **SSL/TLS**: HTTPS endpoints for secure communication
- **Authentication**: BOSH Director certificate-based authentication
- **Authorization**: BOSH Director role-based access control

### 🔐 Credential Security

**Important**: Sensitive credentials should never be committed to git.

- **`.env` folder**: Automatically ignored by `.gitignore` - safe for local development
  - Contains `.env/bosh-env.ini` (BOSH credentials)
  - Contains `.env/bosh.pem` (CA certificate)
  - Never commit these files to git
- **`manifest.yml`**: Excluded from git via `.gitignore`
  - Use `manifest-template.yml` as a starting point
  - Copy template and add your credentials for deployment
  - Environment variables are passed via CF environment variables
  - Never commit actual manifest files with credentials to git

## 📚 Documentation

- **API Documentation**: Comprehensive tool descriptions
- **Configuration Guide**: Setup and deployment instructions
- **BOSH CLI Requirements**: Installation and setup guide

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

---

**Built with ❤️ using Spring AI and BOSH**

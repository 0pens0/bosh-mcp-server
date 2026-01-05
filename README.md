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

- **BOSH CLI**: The BOSH CLI must be installed and available in the PATH
- **BOSH Director Access**: Valid BOSH Director URL, client credentials, and CA certificate
- **Java 21**: For building and running the server
- **Maven 3.9+**: For building the project

### Deploy the Server

1. **Build and deploy** the MCP server to your Cloud Foundry foundation
2. **Configure** your BOSH credentials and certificate in the manifest
3. **Get the deployed URL** from `cf apps` command

### MCP Client Configuration

Once deployed, configure your MCP client with the server URL:

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

**Replace** `bosh-mcp-server.apps.your-cf-domain.com` with your actual deployed server URL.

## 🛠 Building & Deployment

### Build the Server

```bash
./mvnw clean package -DskipTests
```

### Deploy to Cloud Foundry

```bash
# Copy the template and configure with your credentials
cp manifest-template.yml manifest.yml
# Edit manifest.yml with your BOSH credentials
cf push bosh-mcp-server

# Get the deployed URL
cf apps
# Look for your app and copy the URL (e.g., https://bosh-mcp-server.apps.your-domain.com)
```

**Note**: The BOSH CLI will be automatically downloaded and installed in the container at startup if not already available. This eliminates the need for a custom buildpack or Docker image.

### Local Development

```bash
# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## ⚙️ Configuration

### Configuration Methods

The server supports two methods for providing BOSH configuration:

1. **Environment Variables** (for Cloud Foundry deployment)
2. **`.env` folder** (for local development)

**Priority**: Environment variables take precedence over `.env` folder values.

### Local Development with `.env` Folder

For local development, you can use the `.env` folder to store your BOSH credentials:

1. **Create the `.env` folder** in the project root:
   ```bash
   mkdir -p .env
   ```

2. **Create `.env/bosh-env.ini`** with your BOSH credentials:
   ```bash
   export BOSH_ENVIRONMENT=10.0.10.51
   export BOSH_CLIENT=ops_manager
   export BOSH_CLIENT_SECRET=your-client-secret
   export BOSH_CA_CERT=bosh.pem
   ```

3. **Create `.env/bosh.pem`** with your BOSH Director CA certificate:
   ```bash
   -----BEGIN CERTIFICATE-----
   YOUR_CA_CERTIFICATE_CONTENT
   -----END CERTIFICATE-----
   ```

**Note**: The `.env` folder is automatically ignored by git (see `.gitignore`). Never commit sensitive credentials to the repository.

### Environment Variables (Cloud Foundry)

For Cloud Foundry deployment, use environment variables in your `manifest.yml`:

```bash
BOSH_DIRECTOR=https://bosh-director.example.com
BOSH_CLIENT=your-bosh-client
BOSH_CLIENT_SECRET=your-bosh-client-secret
BOSH_CA_CERT="-----BEGIN CERTIFICATE-----
YOUR_CA_CERTIFICATE_CONTENT
-----END CERTIFICATE-----"
BOSH_CLI_PATH=bosh  # Optional, defaults to 'bosh' or auto-installed path
BOSH_CLI_INSTALL_ENABLED=true  # Optional, enable auto-installation (default: true)
BOSH_CLI_INSTALL_PATH=/tmp/bosh-cli  # Optional, installation directory
```

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

## 🛠 Capabilities

This MCP server exposes comprehensive BOSH operations as AI-powered tools:

### Deployment Management (6 tools)
- **listDeployments** - List all BOSH deployments
- **getDeployment** - Get detailed deployment information
- **deployDeployment** - Deploy a BOSH deployment from a manifest
- **deleteDeployment** - Delete a BOSH deployment
- **recreateDeployment** - Recreate all VMs in a deployment
- **updateDeployment** - Update deployment configuration

### VM Management (6 tools)
- **listVms** - List all VMs in a deployment
- **getVmStatus** - Get VM status and details
- **startVm** - Start a VM
- **stopVm** - Stop a VM
- **restartVm** - Restart a VM
- **recreateVm** - Recreate a VM

### Log Management (4 tools)
- **getDeploymentLogs** - Get logs from a deployment
- **getVmLogs** - Get logs from a specific VM
- **getTaskLogs** - Get logs from a BOSH task
- **streamLogs** - Stream logs from a deployment

### Stemcell Management (3 tools)
- **listStemcells** - List available stemcells
- **uploadStemcell** - Upload a new stemcell
- **deleteStemcell** - Delete a stemcell

### Release Management (4 tools)
- **listReleases** - List available releases
- **uploadRelease** - Upload a new release
- **deleteRelease** - Delete a release
- **getReleaseVersions** - Get versions of a release

### Errand Management (3 tools)
- **listErrands** - List errands for a deployment
- **runErrand** - Run an errand
- **getErrandStatus** - Get errand execution status

### Cloud Config Management (3 tools)
- **getCloudConfig** - Get current cloud config
- **updateCloudConfig** - Update cloud config
- **getCloudConfigDiff** - Get diff of cloud config changes

### SSH Operations (2 tools)
- **sshToVm** - Get SSH connection information for a VM
- **executeCommandOnVm** - Execute a command on a VM via SSH

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

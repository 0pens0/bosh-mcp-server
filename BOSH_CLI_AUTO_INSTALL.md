# BOSH CLI Auto-Installation Feature

## Overview

The BOSH MCP server now includes automatic BOSH CLI installation functionality. This allows the server to run in Cloud Foundry containers (Diego cells) without requiring:
- Custom buildpacks with pre-installed BOSH CLI
- Docker images with BOSH CLI
- Manual BOSH CLI installation

## How It Works

1. **At Application Startup**: The `BoshCliInstaller` component runs after the application is ready
2. **Detection**: Checks if BOSH CLI is available in PATH or if a custom path is configured
3. **Download**: If not found, downloads BOSH CLI v7.9.5 from GitHub releases
4. **Installation**: Extracts the binary to `${java.io.tmpdir}/bosh-cli/bosh` (or custom path)
5. **Execution**: Makes the binary executable and configures the executor to use it

## Implementation Details

### New Components

1. **BoshCliInstaller** (`BoshCliInstaller.java`)
   - Downloads BOSH CLI binary from GitHub releases
   - Installs it to a configurable location
   - Sets executable permissions
   - Runs at application startup via `@EventListener(ApplicationReadyEvent.class)`

2. **Updated BoshCliExecutor** (`BoshCliExecutor.java`)
   - Now uses `getEffectiveCliPath()` method
   - Automatically uses installed CLI path if available
   - Falls back to configured path or "bosh" in PATH

3. **Configuration Updates**
   - New properties in `application.yaml`:
     - `bosh.cli.install.enabled` (default: `true`)
     - `bosh.cli.install.path` (default: `${java.io.tmpdir}/bosh-cli`)

### Configuration

#### Environment Variables

```bash
# Enable/disable auto-installation (default: true)
BOSH_CLI_INSTALL_ENABLED=true

# Custom installation path (default: ${java.io.tmpdir}/bosh-cli)
BOSH_CLI_INSTALL_PATH=/tmp/bosh-cli

# Override CLI path (uses installed path if not set)
BOSH_CLI_PATH=/custom/path/to/bosh
```

#### Application Properties

```yaml
bosh:
  cli:
    install:
      enabled: true  # Enable auto-installation
      path: ${java.io.tmpdir}/bosh-cli  # Installation directory
  cliPath: bosh  # CLI path (uses installed path if available)
```

## Execution Flow

1. **Application Starts** → Spring Boot initializes
2. **BoshCliInstaller Created** → Component initialized
3. **ApplicationReadyEvent Fired** → `installBoshCli()` method called
4. **Check for Existing CLI**:
   - If `BOSH_CLI_PATH` is set and file exists → Use it
   - If "bosh" is in PATH → Use it
   - Otherwise → Download and install
5. **BoshCliExecutor Uses Installed Path** → All BOSH commands use the installed CLI

## Benefits

1. **No Buildpack Changes**: Works with standard Java buildpack
2. **No Docker Required**: Runs in standard Cloud Foundry containers
3. **Automatic Updates**: Can update BOSH CLI version by changing constant
4. **Flexible**: Can still use pre-installed CLI if available
5. **Container-Friendly**: Installs to temp directory, works in read-only filesystems

## Testing

The feature has been tested with:
- ✅ Code compilation
- ✅ Unit tests (existing tests still pass)
- ✅ Configuration validation

## Deployment

When deploying to Cloud Foundry:

1. **Default Behavior**: BOSH CLI will be auto-installed if not found
2. **Custom Path**: Set `BOSH_CLI_INSTALL_PATH` if you want a specific location
3. **Disable**: Set `BOSH_CLI_INSTALL_ENABLED=false` to disable auto-installation

## Notes

- The BOSH CLI binary is downloaded from: 
  `https://github.com/cloudfoundry/bosh-cli/releases/download/v7.9.5/bosh-cli-7.9.5-linux-amd64`
- The binary is approximately 50-60 MB
- Download happens once at startup (cached if file already exists)
- The installer checks if the binary already exists before downloading

## Future Enhancements

Potential improvements:
- Support for different BOSH CLI versions via configuration
- Support for different architectures (ARM, etc.)
- Caching mechanism for faster startup
- Health check to verify CLI installation

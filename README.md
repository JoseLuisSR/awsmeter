# AWS Meter 🚀

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-kinesis-context-view.png)

**AWS Meter** is a comprehensive JMeter plugin designed to execute performance tests against AWS services including Kinesis, SQS, SNS, Cognito, and EventBridge. This plugin provides specialized Java samplers for each AWS service, leveraging the AWS SDK for seamless integration and communication.

## 🎯 Why Use AWS Meter?

When deploying AWS services in production, you need to configure them to handle both normal and peak loads effectively. AWS Meter helps you:

- **Validate service configurations** under various load conditions
- **Execute proof-of-concepts (POCs)** to understand AWS service capabilities and boundaries
- **Perform load testing** to ensure your AWS infrastructure can handle expected traffic
- **Identify bottlenecks** before they impact your production environment
- **Test locally** using LocalStack for development and CI/CD pipelines

## 🌟 Supported AWS Services

AWS Meter currently supports testing for the following services:

- 🌊 **[Kinesis Data Stream](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/kinesis)** - Real-time data streaming
- 📬 **[SQS](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sqs)** - Message queuing (Standard & FIFO)
- 📢 **[SNS](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sns)** - Push notifications (Standard & FIFO)
- 🔐 **[Cognito](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/cognito)** - User authentication and authorization
- 🎫 **[EventBridge](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/eventbus)** - Event-driven architecture

**All services are fully compatible with LocalStack for local development! 🐳**

## 📋 Prerequisites

Before installing AWS Meter, ensure you have:

- ☕ **Java 11 or higher** (recommended for full compatibility)
- 🔧 **Apache JMeter** (latest version recommended)
- 🏗️ **Gradle** (optional - wrapper included)
- ☁️ **Active AWS Account** OR 🐳 **LocalStack** for local development
- 🐳 **Docker** (required for LocalStack)

### LocalStack Prerequisites 🐳

For local development with LocalStack:

- **Docker**: Install [Docker Desktop](https://www.docker.com/products/docker-desktop) or Docker Engine
- **LocalStack**: Install via pip: `pip install localstack`
- **awslocal CLI** (optional): `pip install awscli-local`

## 🔧 Installation

### Option 1: JMeter Plugins Manager (Recommended) ⭐

![JMeter Plugins Logo](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/jmeter-plugins-logo.png)

The easiest way to install AWS Meter is through the JMeter Plugins Manager:

1. **Install JMeter Plugins Manager**
   - Download `plugins-manager.jar` from [jmeter-plugins.org](https://jmeter-plugins.org/install/Install/)
   - Place it in `$JMETER_HOME/lib/ext/` directory

2. **Restart JMeter**

3. **Install AWS Meter**
   - Navigate to **Options → Plugins Manager**
   - Go to **Available Plugins** tab
   - Search for "aws"
   - Select AWS Meter and click **Apply Changes and Restart JMeter**

![JMeter Plugins AWS Meter](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/jmeter-plugins-awsmeter.png)

### Option 2: Manual Installation 🛠️

If you prefer to build from source:

1. **Clone the repository**
   ```bash
   git clone https://github.com/JoseLuisSR/awsmeter.git
   cd awsmeter
   ```

2. **Build the JAR file**
   ```bash
   # Using Gradle wrapper (recommended)
   ./gradlew uberJar
   
   # Or using system Gradle
   gradle uberJar
   ```

3. **Install the plugin**
   - Copy `awsmeter-x.y.z.jar` from `build/libs/` to `$JMETER_HOME/lib/ext/`
   - Restart JMeter

### LocalStack Installation 🐳

Set up LocalStack for local AWS service emulation:

```bash
# Install LocalStack
pip install localstack
localstack start

# Install LocalStack CLI tools (optional but recommended)
pip install awscli-local

# Verify installation
localstack --version
```

## ⚙️ Configuration

### 🔑 AWS Account Setup

#### Step 1: Create AWS Account
- Create a free [AWS account](https://aws.amazon.com/premiumsupport/knowledge-center/create-and-activate-aws-account/)
- Take advantage of the [12-month free tier](https://aws.amazon.com/free)
- Choose your preferred [AWS region](https://infrastructure.aws/)

#### Step 2: Create IAM User
- Create an [IAM user](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_users_create.html) with **programmatic access**
- Generate access key ID and secret access key
- **Important**: Follow the principle of least privilege

#### Step 3: Configure IAM Permissions
- Create an IAM Group with appropriate policies
- Attach the user to the group
- Grant only the minimum permissions needed for your tests

#### Step 4: Set Up Credentials (Choose One Method)

**Method A: Credentials File (Recommended for local development)**

Create `~/.aws/credentials`:
```ini
[default]
aws_access_key_id = YOUR_ACCESS_KEY_ID
aws_secret_access_key = YOUR_SECRET_ACCESS_KEY

[localstack]
aws_access_key_id = test
aws_secret_access_key = test

[profile-name]
aws_access_key_id = ANOTHER_ACCESS_KEY_ID
aws_secret_access_key = ANOTHER_SECRET_ACCESS_KEY
```

Create `~/.aws/config`:
```ini
[default]
region = us-east-1
output = json

[profile localstack]
region = us-east-1
output = json
endpoint_url = http://localhost:4566

[profile profile-name]
region = eu-west-1
output = json
```

**Method B: AWS CLI Setup**
```bash
# For AWS
aws configure

# For LocalStack
awslocal configure
```

**Method C: Environment Variables**
```bash
# For AWS
export AWS_ACCESS_KEY_ID=your-access-key-id
export AWS_SECRET_ACCESS_KEY=your-secret-access-key
export AWS_DEFAULT_REGION=us-east-1

# For LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566
```

![Credentials File Example](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/credentials-file.png)
![Config File Example](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/config-file.png)

### 🌐 Cloud Environment Support

AWS Meter automatically detects and uses the most appropriate credential source:

#### Local Development 💻
- **Explicit credentials**: Provide access key and secret in JMeter parameters
- **Profile-based**: Use credential files with specific profiles
- **LocalStack**: Use test credentials with custom endpoint

#### Cloud Environments (ECS, EC2, Lambda) ☁️
For applications running in AWS with IAM roles, leave credential parameters empty:

```
aws_access_key_id: (empty)
aws_secret_access_key: (empty)  
aws_session_token: (empty)
aws_configure_profile: default
```

AWS Meter will automatically use:
- **ECS Task IAM Role** (Amazon ECS)
- **EC2 Instance Profile** (Amazon EC2)
- **Environment variables** (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)

## 🚀 Getting Started

### First Steps with JMeter

If you're new to JMeter, watch this helpful [JMeter Beginners Tutorial](https://youtube.com/playlist?list=PLhW3qG5bs-L-zox1h3eIL7CZh5zJmci4c).

### Using the Example Test Plan

1. **Open the example**: Load `awsmeter.jmx` in JMeter
2. **Configure AWS parameters**: Fill in the following Java request parameters

![AWS Meter Parameters](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-parameters.png)

### 📝 Parameter Configuration

#### For AWS Cloud ☁️

| Parameter | Description | Required | Example |
|-----------|-------------|----------|---------|
| `aws_access_key_id` | Your AWS access key ID | ⚠️ | `AKIAIOSFODNN7EXAMPLE` |
| `aws_secret_access_key` | Your AWS secret access key | ⚠️ | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `aws_session_token` | Session token for temporary credentials | ❌ | Leave empty unless using temporary credentials |
| `aws_region` | AWS region for your services | ✅ | `us-east-1` |
| `aws_configure_profile` | Profile name from credentials file | ❌ | `default` |
| `aws_endpoint_custom` | Custom endpoint URL | ❌ | Leave empty for AWS |

#### For LocalStack 🐳

| Parameter | Description | Required | Example |
|-----------|-------------|----------|---------|
| `aws_access_key_id` | LocalStack access key | ✅ | `test` |
| `aws_secret_access_key` | LocalStack secret key | ✅ | `test` |
| `aws_session_token` | Session token | ❌ | Leave empty |
| `aws_region` | AWS region | ✅ | `us-east-1` |
| `aws_configure_profile` | Profile name | ❌ | `localstack` |
| `aws_endpoint_custom` | LocalStack endpoint | ✅ | `http://localhost:4566` |

**💡 Tip**: Leave credential parameters empty to use credential files or IAM roles automatically.

### Service-Specific Configuration

For detailed configuration instructions for each AWS service:

- 🌊 **[Kinesis Data Stream](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/kinesis)**
- 📬 **[SQS](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sqs)**
- 📢 **[SNS](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sns)**
- 🔐 **[Cognito](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/cognito)**
- 🎫 **[EventBridge](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/eventbus)**


## 🏗️ Architecture & Design

AWS Meter follows a modular, extensible architecture that makes it easy to add support for new AWS services.

![AWS Meter Class Diagram](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-class-diagram.png)

### Core Components

- **`AWSSampler.java`**: Base class for all AWS service samplers
- **`AWSClientSDK1.java`**: Interface for AWS SDK v1 implementations
- **`AWSClientSDK2.java`**: Interface for AWS SDK v2 implementations

### Package Structure

Following JMeter's protocol convention:

- `org.apache.jmeter.protocol.aws.kinesis` - Kinesis Data Stream samplers
- `org.apache.jmeter.protocol.aws.sqs` - SQS Standard and FIFO queue samplers  
- `org.apache.jmeter.protocol.aws.sns` - SNS Standard and FIFO topic samplers
- `org.apache.jmeter.protocol.aws.cognito` - Cognito authentication samplers
- `org.apache.jmeter.protocol.aws.eventbus` - EventBridge samplers

### Extending AWS Meter

To add support for a new AWS service:

1. Create a new package following the naming convention
2. Extend `AWSSampler.java`
3. Implement either `AWSClientSDK1.java` or `AWSClientSDK2.java`
4. Override required methods for client creation, parameters, execution, and cleanup

## 🛠️ Troubleshooting

### Common Issues

#### Java Version Compatibility ☕

**Problem**: Exception when using Java 8 or lower with JMeter Plugins installation

![Compilation Issue](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-compiled-issue.png)

**Solutions**:
1. **Upgrade Java**: Install Java 11+ and run JMeter with this version
2. **Manual Installation**: Build from source and install manually
3. **Check JMeter Version**: Ensure you're using a compatible JMeter version

#### Authentication Issues 🔐

**Problem**: "Unable to load AWS credentials" error

**Solutions for AWS**:
1. Verify credential file format and location (`~/.aws/credentials`)
2. Check IAM user permissions
3. Ensure correct region configuration
4. Validate access key and secret key values

**Solutions for LocalStack**:
1. Use `test`/`test` credentials
2. Ensure LocalStack is running: `curl http://localhost:4566/_localstack/health`
3. Verify endpoint configuration: `http://localhost:4566`
4. Check LocalStack logs for errors

#### Connection Issues 🌐

**Problem**: Timeout or connection refused errors

**Solutions**:
1. Verify AWS region matches your service location
2. Check network connectivity and firewall settings
3. Validate service endpoints
4. Ensure IAM policies allow service access

#### Performance Issues ⚡

**Problem**: Slow test execution or timeouts

**Solutions**:
1. Increase JMeter heap size: `-Xms1g -Xmx4g`
2. Adjust thread pool settings
3. Optimize AWS service configurations
4. Monitor AWS service limits and quotas

### Getting Help 💬

If you encounter issues not covered here:

1. Check the [GitHub Issues](https://github.com/JoseLuisSR/awsmeter/issues)
2. Review AWS service documentation
3. Consult JMeter performance testing best practices
4. Consider AWS support if using paid services

## 📚 Additional Resources

- 📖 **[AWS Documentation](https://docs.aws.amazon.com/)**
- 🐳 **[LocalStack Documentation](https://docs.localstack.cloud/)**
- 🎯 **[JMeter User Manual](https://jmeter.apache.org/usermanual/)**
- 🏗️ **[AWS Architecture Center](https://aws.amazon.com/architecture/)**
- 💡 **[Performance Testing Best Practices](https://jmeter.apache.org/usermanual/best-practices.html)**
- 🔧 **[LocalStack Samples](https://github.com/localstack/localstack/tree/master/examples)**

---

**Happy Testing!** 🎉 If you find AWS Meter helpful, please consider giving it a ⭐ on GitHub.
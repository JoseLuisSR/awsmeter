# 🔐 AWS Cognito

Amazon Cognito is AWS's fully managed identity service that provides secure user registration, authentication, and access control for web and mobile applications. Its User Pools feature acts as a scalable user directory that can handle millions of users, offering comprehensive identity management capabilities including:

This guide provides specific instructions for testing AWS Cognito using the `awsmeter` plugin with Apache JMeter. For general installation and setup instructions, please refer to the [instructions](../../../../../../../../../README.md).

## Overview 📋

Amazon Cognito is a robust identity management service that enables secure user registration, authentication, and access control for web and mobile applications. This integration provides JMeter samplers to test Cognito user management operations, including user creation and authentication workflows.

**Key Benefits:**
- 🚀 Scalable identity store supporting millions of users
- 🛡️ Advanced security features and compliance with industry standards
- 🔗 Seamless integration with social and enterprise identity providers
- ⚡ Built on open identity standards (OAuth 2.0, SAML, OpenID Connect)

For comprehensive Cognito documentation, visit: https://aws.amazon.com/cognito/

## Prerequisites ✅

Before using the Cognito samplers, ensure you have:

1. **AWS Account** with appropriate permissions
2. **IAM User** with administrative access to Cognito services
3. **JMeter Installation** with `awsmeter` plugin installed (see main [installation guide](../../../../../../../../../README.md) for installation instructions)
4. **AWS Credentials** configured for programmatic access

### Required AWS Permissions 🔑

Your IAM user needs the following permissions:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "cognito-idp:AdminCreateUser",
                "cognito-idp:AdminInitiateAuth",
                "cognito-idp:AdminSetUserPassword"
            ],
            "Resource": "*"
        }
    ]
}
```

## Setup 🛠️

### AWS Cognito Configuration

1. **Sign in to AWS Console** 🌐
   - Navigate to the AWS Management Console
   - Access the Cognito service

2. **Create User Pool** 👥
   - Go to **Cognito → User pools → Create user pool**
   - Configure authentication settings according to your requirements
   - Note the **User Pool ID** for later configuration

3. **Configure App Client** 📱
   - Create an app client within your user pool
   - **Important:** Enable `ALLOW_ADMIN_USER_PASSWORD_AUTH` in the app client's authentication flows
   - Record the **Client ID** and **Client Secret** (if applicable)

### LocalStack Configuration (Development) 🐳

For local development and testing:

1. **Start LocalStack** with Cognito service enabled:
   ```bash
   localstack start --services cognito-idp
   ```

2. **Configure AWS CLI** for LocalStack:
   ```bash
   aws configure set aws_access_key_id test
   aws configure set aws_secret_access_key test
   aws configure set region us-east-1
   aws configure set endpoint_url http://localhost:4566
   ```

3. **Create User Pool in LocalStack** 🏗️:
   ```bash
   # Create the user pool
   aws cognito-idp create-user-pool \
     --pool-name "TestUserPool" \
     --policies '{"PasswordPolicy":{"MinimumLength":8,"RequireUppercase":true,"RequireLowercase":true,"RequireNumbers":true,"RequireSymbols":false}}' \
     --endpoint-url http://localhost:4566 \
     --region us-east-1
   
   # Note the UserPoolId from the response, e.g., us-east-1_123456789
   ```

4. **Create App Client in LocalStack** 📱:
   ```bash
   # Replace USER_POOL_ID with the ID from step 3
   aws cognito-idp create-user-pool-client \
     --user-pool-id us-east-1_123456789 \
     --client-name "TestAppClient" \
     --explicit-auth-flows ALLOW_ADMIN_USER_PASSWORD_AUTH ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH \
     --endpoint-url http://localhost:4566 \
     --region us-east-1
   
   # Note the ClientId from the response for JMeter configuration
   ```

5. **Verify LocalStack Setup** ✅:
   ```bash
   # List user pools to confirm creation
   aws cognito-idp list-user-pools \
     --max-results 10 \
     --endpoint-url http://localhost:4566 \
     --region us-east-1
   ```

## Configuration ⚙️

> 💡 **AWS Credentials:** Configure your AWS credentials as described in the [Authentication section](../../../../../../../../../README.md#aws-authentication).

### Common Parameters

Both samplers share these AWS connection parameters (refer to main [README.md](../../../../../../../README.md) for AWS credentials configuration):

- **AWS Region:** Target AWS region for Cognito operations
- **AWS Access Key ID:** IAM user access key
- **AWS Secret Access Key:** IAM user secret key

### CognitoProducerAdminCreateUser 👤

Creates new users in the Cognito user pool with administrative privileges.

**Required Parameters:**
- **`cognito_user_pool_id`** 🆔: Your Cognito User Pool identifier
- **`cognito_user_username`** 👤: Unique username for the new user
- **`cognito_user_email`** 📧: Valid email address for the user
- **`cognito_user_password`** 🔐: Secure password meeting pool requirements

**Example Configuration:**
```
User Pool ID: us-east-1_XXXXXXXXX
Username: testuser001
Email: testuser001@example.com
Password: TempPass123!
```

### CognitoProducerAdminLoginUser 🔑

Authenticates existing users and retrieves JWT tokens for further API calls.

**Additional Parameters (beyond common ones):**
- **`cognito_client_id`** 📱: App client identifier from your user pool
- **`cognito_client_secret_key`** 🔒: App client secret (if configured)
- **`cognito_user_access_token_var_name`** 🎫: JMeter variable name to store the access token
- **`cognito_user_id_token_var_name`** 🆔: JMeter variable name to store the ID token  
- **`cognito_user_refresh_token_var_name`** 🔄: JMeter variable name to store the refresh token

**Token Usage Example:**
```
Access Token Variable: cognito_access_token
ID Token Variable: cognito_id_token
Refresh Token Variable: cognito_refresh_token
```

These tokens can be used in subsequent HTTP requests for authenticated API calls.

## Monitoring 📊

### Success Indicators ✅
- **Response Code:** 200 for successful operations
- **Token Presence:** Valid JWT tokens in response (for login operations)
- **No Error Messages:** Clean response without AWS error codes

### Common Error Scenarios 🚨
- **Invalid Credentials:** Check AWS access keys and permissions
- **User Already Exists:** Verify username uniqueness for creation operations
- **Authentication Flow Disabled:** Ensure `ALLOW_ADMIN_USER_PASSWORD_AUTH` is enabled
- **Invalid Password:** Verify password meets user pool policy requirements

### Troubleshooting Tips 🔧
1. **Enable JMeter Logging:** Set log level to DEBUG for detailed error information
2. **Validate AWS Credentials:** Test credentials using AWS CLI
3. **Check Cognito Logs:** Monitor CloudWatch logs for detailed error messages
4. **Verify Network Connectivity:** Ensure JMeter can reach AWS endpoints

For additional configuration options and advanced usage, refer to
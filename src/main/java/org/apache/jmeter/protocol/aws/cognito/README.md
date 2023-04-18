# Cognito 



Amazon Cognito enables the addition of user registration and login capabilities while managing access to your web and mobile apps. It offers a scalable identity store for millions of users, accommodating social and enterprise identity federation, and advanced security features to safeguard your customers and business. Utilizing open identity standards, Amazon Cognito complies with various regulations and seamlessly integrates with frontend and backend development tools.

See https://aws.amazon.com/cognito/ for details.


# Setting up

To start using `awsmeter` to create and login user in Cognito you need first create Cognito user pool, follow the below steps:

1. Sig-on AWS account.

   
2. Go to Cognito > Create user pool.

   
3. Follow the steps

* Note in order to use the CognitoProducerAdminLoginUser, you need to enable ALLOW_ADMIN_USER_PASSWORD_AUTH in App client's Authentication flows.

# Getting Started

After you installed `awsmeter` in JMeter you can start using it to create and login user via Cognito. There are two Java Request Samplers, one for creating user, one for login user. Each one has fields to set up the username, password and others parameters, also you need to fill the fields to connect AWS account in a region and IAM user with admin access to Cognito.

The fields of each Java Request Sampler are:

## CognitoProducerAdminCreateUser

* **cognito_user_pool_id:** Id of the Cognito user pool.


* **cognito_user_username:** The username of the user you want to create.


* **cognito_user_email:** The email of the user you want to create. 


* **cognito_user_password:** The password of the user you want to create.


## CognitoProducerAdminLoginUser

There are some fields share between CognitoProducerAdminLoginUser and CognitoProducerAdminCreateUser like user pool id, user's username and password, but there are parameters that only apply for CognitoProducerAdminLoginUser, those are:

* **cognito_client_id:** The Cognito app client's id.


* **cognito_client_secret_key:** Secret key for the Cognito app client.


* **cognito_user_access_token_var_name:** The access token returned from Cognito after a successful login will be stored in a JMeter variable with this name.


* **cognito_user_id_token_var_name:** The id token returned from Cognito after a successful login will be stored in a JMeter variable with this name.


* **cognito_user_refresh_token_var_name:** The refresh token returned from Cognito after a successful login will be stored in a JMeter variable with this name.
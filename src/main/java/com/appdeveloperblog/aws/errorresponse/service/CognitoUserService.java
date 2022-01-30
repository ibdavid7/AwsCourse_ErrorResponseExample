package com.appdeveloperblog.aws.errorresponse.service;

import com.appdeveloperblog.aws.errorresponse.shared.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CognitoUserService {

    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    public CognitoUserService(String region) {
        this.cognitoIdentityProviderClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }

    public CognitoUserService(CognitoIdentityProviderClient cognitoIdentityProviderClient) {
        this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
    }

    public JsonObject createUser(JsonObject storedUserDetails, String appClientId, String appClientSecret) {

        String email = storedUserDetails.get("email").getAsString();
        String password = storedUserDetails.get("password").getAsString();
        String username = storedUserDetails.get("username").getAsString();
        String userId = UUID.randomUUID().toString();
        String firstName = storedUserDetails.get("firstName").getAsString();
        String lastname = storedUserDetails.get("lastName").getAsString();


        AttributeType attributeUserId = AttributeType.builder()
                .name("custom:userid")
                .value(userId)
                .build();

        AttributeType attributeName = AttributeType.builder()
                .name("name")
                .value(firstName + " " + lastname)
                .build();

        AttributeType attributeEmail = AttributeType.builder()
                .name("email")
                .value(email)
                .build();

        List<AttributeType> attributeTypeList = List.of(attributeName, attributeEmail, attributeUserId);

        // Calculated Secret hash value

        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, username);

        // Sign up request
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username(username)
                .password(password)
                .userAttributes(attributeTypeList)
                .clientId(appClientId)
                .secretHash(generatedSecretHash)
                .build();

        // Call signup API and return Sign up response; can generate RunTime exception, handled in CreateUserHandler
        SignUpResponse signUpResponse = cognitoIdentityProviderClient.signUp(signUpRequest);

        JsonObject createUserResult = new JsonObject();
        createUserResult.addProperty(Constants.IS_SUCCESSFUL, signUpResponse.sdkHttpResponse().isSuccessful());
        createUserResult.addProperty(Constants.STATUS_CODE, signUpResponse.sdkHttpResponse().statusCode());
        createUserResult.addProperty(Constants.COGNITO_USER_ID, signUpResponse.userSub());
        createUserResult.addProperty(Constants.IS_CONFIRMED, signUpResponse.userConfirmed());

        return createUserResult;
    }

    public JsonObject confirmUserSignUp(String userPoolClientId,
                                        String userPoolClientSecret,
                                        String userName,
                                        String confirmationCode) {


        String generatedSecretHash = calculateSecretHash(userPoolClientId, userPoolClientSecret, userName);

        ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                .username(userName)
                .secretHash(generatedSecretHash)
                .confirmationCode(confirmationCode)
                .clientId(userPoolClientId)
                .build();

        ConfirmSignUpResponse confirmSignUpResponse = cognitoIdentityProviderClient.confirmSignUp(confirmSignUpRequest);

        JsonObject confirmUserResponse = new JsonObject();
        confirmUserResponse.addProperty("isSuccessful", confirmSignUpResponse.sdkHttpResponse().isSuccessful());
        confirmUserResponse.addProperty("statusCode", confirmSignUpResponse.sdkHttpResponse().statusCode());
        return confirmUserResponse;

    }

    public JsonObject loginUser(String username, String password, String userPoolClientId,
                                String userPoolClientSecret) {

        String generatedSecretHash = calculateSecretHash(userPoolClientId, userPoolClientSecret, username);


        Map<String, String> params = new HashMap<>();
        params.put("USERNAME", username);
        params.put("PASSWORD", password);
        params.put("SECRET_HASH", generatedSecretHash);

        InitiateAuthRequest initialRequest = InitiateAuthRequest.builder()
                .clientId(userPoolClientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(params)
                .build();

        InitiateAuthResponse authResponse = cognitoIdentityProviderClient.initiateAuth(initialRequest);
        AuthenticationResultType authenticationResultType = authResponse.authenticationResult();


        JsonObject loginUserResult = new JsonObject();
        loginUserResult.addProperty("isSuccessful", authResponse.sdkHttpResponse().isSuccessful());
        loginUserResult.addProperty("statusCode", authResponse.sdkHttpResponse().statusCode());
        loginUserResult.addProperty("idToken", authenticationResultType.idToken());
        loginUserResult.addProperty("accessToken", authenticationResultType.accessToken());
        loginUserResult.addProperty("refreshToken", authenticationResultType.refreshToken());

        return loginUserResult;


    }

    public JsonObject addUserToGroup(String groupName, String username, String userPoolId) {

        AdminAddUserToGroupRequest addUserToGroupRequest = AdminAddUserToGroupRequest.builder()
                .groupName(groupName)
                .username(username)
                .userPoolId(userPoolId)
                .build();

        AdminAddUserToGroupResponse addUserToGroupResponse = cognitoIdentityProviderClient
                .adminAddUserToGroup(addUserToGroupRequest);


        JsonObject addUserToGroupResult = new JsonObject();

        addUserToGroupResult.addProperty("isSuccessful", addUserToGroupResponse.sdkHttpResponse().isSuccessful());
        addUserToGroupResult.addProperty("statusCode", addUserToGroupResponse.sdkHttpResponse().statusCode());

        return addUserToGroupResult;

    }

    public JsonObject getUser(String accessToken) {
        GetUserRequest userRequest = GetUserRequest.builder()
                .accessToken(accessToken)
                .build();

        GetUserResponse getUserResponse = cognitoIdentityProviderClient.getUser(userRequest);

        JsonObject getUserResult = new JsonObject();

        getUserResult.addProperty("isSuccessful", getUserResponse.sdkHttpResponse().isSuccessful());
        getUserResult.addProperty("statusCode", getUserResponse.sdkHttpResponse().statusCode());

        JsonObject userAttributes = new JsonObject();
        getUserResponse.userAttributes().forEach(userAttribute -> userAttributes.addProperty(userAttribute.name(),
                userAttribute.value()));

        getUserResult.add("user", userAttributes);
        return getUserResult;
    }

    public String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

        SecretKeySpec signingKey = new SecretKeySpec(
                userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating ");
        }
    }

    public JsonObject getUserByUsername(String username, String poolId) {


        AdminGetUserRequest adminGetUserRequest = AdminGetUserRequest.builder()
                .username(username)
                .userPoolId(poolId)
                .build();

        AdminGetUserResponse adminGetUserResponse = cognitoIdentityProviderClient.adminGetUser(adminGetUserRequest);

        JsonObject userDetails = new JsonObject();

        if (!adminGetUserResponse.sdkHttpResponse().isSuccessful()) {
            throw new IllegalArgumentException("Unsuccessful result. Status Code: " +
                    adminGetUserResponse.sdkHttpResponse().statusCode());
        }

        List<AttributeType> userAttributes = adminGetUserResponse.userAttributes();

        userAttributes.stream().forEach(userAttribute -> {
            userDetails.addProperty(userAttribute.name(), userAttribute.value());
        });

        return userDetails;

    }
}

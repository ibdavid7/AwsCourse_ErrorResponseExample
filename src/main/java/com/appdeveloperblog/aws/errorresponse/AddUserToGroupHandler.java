package com.appdeveloperblog.aws.errorresponse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appdeveloperblog.aws.errorresponse.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class AddUserToGroupHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String userPoolId;

    public AddUserToGroupHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.userPoolId = Utils.decryptKey("MY_COGNITO_USER_POOL_ID");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {

        String username = apiGatewayProxyRequestEvent.getPathParameters().get("username");

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        LambdaLogger logger = context.getLogger();


        try {

            String requestEventBody = apiGatewayProxyRequestEvent.getBody();
            JsonObject requestEventJson = JsonParser.parseString(requestEventBody).getAsJsonObject();

            String groupName = requestEventJson.get("groupName").getAsString();

            JsonObject addUserToGroupResponse = cognitoUserService.addUserToGroup(
                    groupName,
                    username,
                    userPoolId);

            return responseEvent
                    .withStatusCode(200)
                    .withBody(new Gson().fromJson(addUserToGroupResponse, JsonObject.class).toString());

        } catch (AwsServiceException awsServiceException) {
            logger.log(awsServiceException.awsErrorDetails().errorMessage());
            return responseEvent
                    .withStatusCode(awsServiceException.awsErrorDetails().sdkHttpResponse().statusCode())
                    .withBody(awsServiceException.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            return responseEvent
                    .withStatusCode(500)
                    .withBody(e.getMessage());
        }

    }
}

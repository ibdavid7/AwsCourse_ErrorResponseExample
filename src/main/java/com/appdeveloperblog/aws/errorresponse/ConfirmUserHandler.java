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

public class ConfirmUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public ConfirmUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        LambdaLogger logger = context.getLogger();


        try {

            String requestEventBody = apiGatewayProxyRequestEvent.getBody();
            JsonObject requestEventJson = JsonParser.parseString(requestEventBody).getAsJsonObject();

            String username = requestEventJson.get("username").getAsString();
            String confirmationCode = requestEventJson.get("code").getAsString();

            JsonObject confirmUserSignUpResponse = cognitoUserService.confirmUserSignUp(
                    appClientId,
                    appClientSecret,
                    username,
                    confirmationCode);

            return responseEvent
                    .withStatusCode(200)
                    .withBody(new Gson().fromJson(confirmUserSignUpResponse, JsonObject.class).toString());

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

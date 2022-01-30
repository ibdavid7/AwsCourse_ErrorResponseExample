package com.appdeveloperblog.aws.errorresponse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appdeveloperblog.aws.errorresponse.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.Map;

public class GetUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;

    public GetUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        LambdaLogger logger = context.getLogger();

        try {

            Map<String, String> requestHeaders = apiGatewayProxyRequestEvent.getHeaders();
            String accessToken = requestHeaders.get("AccessToken");

            JsonObject getUserResult = cognitoUserService.getUser(accessToken);

            return responseEvent
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(new Gson().toJson(getUserResult, JsonObject.class).toString());

        } catch (AwsServiceException awsServiceException) {
            logger.log(awsServiceException.awsErrorDetails().errorMessage());

            ErrorResponse errorResponse = new ErrorResponse(awsServiceException.awsErrorDetails().errorMessage());

            return responseEvent
                    .withStatusCode(awsServiceException.awsErrorDetails().sdkHttpResponse().statusCode())
                    .withBody(
                            new GsonBuilder()
                                    .serializeNulls()
                                    .create()
                                    .toJson(errorResponse, ErrorResponse.class)
                    );

        } catch (Exception e) {
            logger.log(e.getMessage());

            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());

            return responseEvent
                    .withStatusCode(500)
                    .withBody(
                            new GsonBuilder()
                                    .serializeNulls()
                                    .create()
                                    .toJson(errorResponse, ErrorResponse.class)
                    );
        }
    }
}

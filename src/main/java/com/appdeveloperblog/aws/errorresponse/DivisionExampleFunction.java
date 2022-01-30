package com.appdeveloperblog.aws.errorresponse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class DivisionExampleFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        try {

            Map<String, String> inputMap = input.getQueryStringParameters();

            int dividend = Integer.parseInt(inputMap.get("dividend"));
            int divisor = Integer.parseInt(inputMap.get("divisor"));
            int result = dividend / divisor;

            Map<String, Integer> responseBody = new HashMap<>();
            responseBody.put("dividend", dividend);
            responseBody.put("divisor", divisor);
            responseBody.put("result", result);

            LambdaLogger logger = context.getLogger();
            logger.log("Handling HTTP request for the /users API endpoint\n");

            return response
                    .withStatusCode(200)
                    .withBody(new Gson().toJson(responseBody, Map.class));

        } catch (NumberFormatException | ArithmeticException numberFormatException) {

            MyException myException = new MyException(numberFormatException.getMessage(),
                    numberFormatException.getCause());

            return response
                    .withBody("{\"Parameter Error\":\"" +
                            myException
                                    .toString()
                                    .replaceAll("\"", "\\\\\"") + "\"}")
                    .withStatusCode(500);
        } catch (Exception e) {
            return response
                    .withBody("{\"Internal Server Error:\":\"" + e + "\"}")
                    .withStatusCode(500);
        }
    }

}

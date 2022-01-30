package com.appdeveloperblog.aws.errorresponse;

public class MyException extends RuntimeException{

    MyException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

}

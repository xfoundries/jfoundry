package org.jfoundry.webmvc.spring;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Resolves HTTP and Spring MVC request-processing exceptions to HTTP ProblemDetail codes.
 */
public class HttpProblemCodeResolver {

    public HttpProblemCode resolve(Exception exception, HttpStatusCode statusCode) {
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return HttpProblemCode.METHOD_NOT_ALLOWED;
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return HttpProblemCode.UNSUPPORTED_MEDIA_TYPE;
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return HttpProblemCode.NOT_ACCEPTABLE;
        }
        if (exception instanceof NoHandlerFoundException || exception instanceof NoResourceFoundException) {
            return HttpProblemCode.NOT_FOUND;
        }
        if (exception instanceof MaxUploadSizeExceededException) {
            return HttpProblemCode.PAYLOAD_TOO_LARGE;
        }
        if (exception instanceof AsyncRequestTimeoutException) {
            return HttpProblemCode.REQUEST_TIMEOUT;
        }
        if (isBadRequest(exception)) {
            return HttpProblemCode.BAD_REQUEST;
        }
        if (isInternalError(exception)) {
            return HttpProblemCode.INTERNAL_ERROR;
        }
        return fromStatus(statusCode);
    }

    private static boolean isBadRequest(Exception exception) {
        return exception instanceof MissingServletRequestPartException
                || exception instanceof ServletRequestBindingException
                || exception instanceof MethodArgumentNotValidException
                || exception instanceof HandlerMethodValidationException
                || exception instanceof TypeMismatchException
                || exception instanceof HttpMessageNotReadableException;
    }

    private static boolean isInternalError(Exception exception) {
        return exception instanceof ConversionNotSupportedException
                || exception instanceof HttpMessageNotWritableException
                || exception instanceof MethodValidationException;
    }

    private static HttpProblemCode fromStatus(HttpStatusCode statusCode) {
        if (statusCode.isSameCodeAs(HttpStatus.BAD_REQUEST)) {
            return HttpProblemCode.BAD_REQUEST;
        }
        if (statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return HttpProblemCode.NOT_FOUND;
        }
        if (statusCode.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
            return HttpProblemCode.METHOD_NOT_ALLOWED;
        }
        if (statusCode.isSameCodeAs(HttpStatus.NOT_ACCEPTABLE)) {
            return HttpProblemCode.NOT_ACCEPTABLE;
        }
        if (statusCode.isSameCodeAs(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
            return HttpProblemCode.UNSUPPORTED_MEDIA_TYPE;
        }
        if (statusCode.isSameCodeAs(HttpStatus.PAYLOAD_TOO_LARGE)) {
            return HttpProblemCode.PAYLOAD_TOO_LARGE;
        }
        if (statusCode.is5xxServerError()) {
            return HttpProblemCode.INTERNAL_ERROR;
        }
        return HttpProblemCode.BAD_REQUEST;
    }
}

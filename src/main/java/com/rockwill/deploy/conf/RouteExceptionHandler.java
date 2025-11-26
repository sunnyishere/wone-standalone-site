package com.rockwill.deploy.conf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;

@ControllerAdvice
@Slf4j
public class RouteExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleRouteException(Exception ex, HttpServletRequest request) {
        log.error("global exception:url:{},{},",request.getRequestURI(), ex.getMessage(), ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/"))
                .headers(headers)
                .build();
    }
}
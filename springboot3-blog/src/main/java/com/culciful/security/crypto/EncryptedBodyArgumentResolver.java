package com.culciful.security.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EncryptedBodyArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(EncryptedBody.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String cipherBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        String plainBody = RSAUtil.decryptWithPrivate(cipherBody);
        Object argument = objectMapper.readValue(plainBody, parameter.getParameterType());
        validateIfNeeded(parameter, webRequest, binderFactory, argument);
        return argument;
    }

    private void validateIfNeeded(
            MethodParameter parameter,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory,
            Object argument
    ) throws Exception {
        if (binderFactory == null) {
            return;
        }
        var binder = binderFactory.createBinder(webRequest, argument, parameter.getParameterName());
        for (Annotation annotation : parameter.getParameterAnnotations()) {
            Object[] hints = ValidationAnnotationUtils.determineValidationHints(annotation);
            if (hints != null) {
                binder.validate(hints);
                BindingResult bindingResult = binder.getBindingResult();
                if (bindingResult.hasErrors()) {
                    throw new MethodArgumentNotValidException(parameter, bindingResult);
                }
                return;
            }
        }
    }
}

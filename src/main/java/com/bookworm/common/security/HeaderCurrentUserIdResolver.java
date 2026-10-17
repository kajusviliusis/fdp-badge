package com.bookworm.common.security;

import com.bookworm.common.error.UnauthorizedException;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Temporary identity source: reads the user id from the {@value #HEADER} request header.
 * <p>
 * The header is not authenticated and can be forged, so this must not be exposed beyond local development.
 * It is replaced by JWT-based resolution in the authentication increment.
 */
public class HeaderCurrentUserIdResolver implements HandlerMethodArgumentResolver {

    static final String HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(HEADER);
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Authentication is required.");
        }
        try {
            return Long.valueOf(header.strip());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Authentication is required.");
        }
    }
}

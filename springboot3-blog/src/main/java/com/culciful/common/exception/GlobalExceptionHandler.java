package com.culciful.common.exception;

import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            MaxUploadSizeExceededException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingServletRequestParameterException.class
    })
    public R<Void> handleBadRequest(Exception ignored) {
        return R.fail(ResultCodeEnum.PARAM_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public R<Void> handleDataIntegrity(Exception ignored) {
        return R.fail(ResultCodeEnum.BUSINESS_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleSystem(Exception ignored) {
        return R.fail(ResultCodeEnum.SYSTEM_ERROR);
    }
}

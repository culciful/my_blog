package com.culciful.common.exception;

import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
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
    public R<Void> handleBadRequest(Exception e, HttpServletRequest request) {
        // 请求本身有问题（参数/格式/大小），预期内会发生，DEBUG 级别够用，不刷 ERROR 噪音
        log.debug("bad request: {} {} - {}", request.getMethod(), request.getRequestURI(), e.toString());
        return R.fail(ResultCodeEnum.PARAM_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public R<Void> handleDataIntegrity(Exception e, HttpServletRequest request) {
        log.warn("data integrity violation: {} {} - {}", request.getMethod(), request.getRequestURI(), e.toString());
        return R.fail(ResultCodeEnum.BUSINESS_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleSystem(Exception e, HttpServletRequest request) {
        // 兜底分支：走到这里说明是没预料到的异常，之前这里直接吞掉（参数叫 ignored），
        // 出了问题在服务端一点痕迹都留不下。带上完整堆栈打 ERROR。
        log.error("unhandled exception: {} {}", request.getMethod(), request.getRequestURI(), e);
        return R.fail(ResultCodeEnum.SYSTEM_ERROR);
    }
}

package cn.xy.fire.exception;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public abstract class I18nException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    Object[] args;

    public I18nException() {
        super();
    }

    public I18nException(String message) {
        super(message);
    }

    public I18nException(String message, Exception ex) {
        super(message, ex);
    }

    public abstract String i18n(MessageSource messageSource);

    protected String i18n(MessageSource messageSource, String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }

    public Object[] getData() {
        return args;
    }

    public I18nException setData(Object... args) {
        this.args = args;
        return this;
    }

    public int httpStatus() {
        return 400;
    }
}

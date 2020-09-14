package cn.xy.fire.exception;

public abstract class ServiceException extends I18nException {

    private static final long serialVersionUID = 1L;

    public ServiceException(Exception ex, Object data, String format, Object... args) {
        super(String.format(format, args), ex);
        this.setData(data);
    }

    public ServiceException(Object data, String format, Object... args) {
        super(String.format(format, args));
        this.setData(data);
    }

    public ServiceException(String format, Object... args) {
        super(String.format(format, args));
    }

}

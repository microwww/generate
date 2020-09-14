package cn.xy.fire.exception;

import org.springframework.context.MessageSource;

public abstract class ExistException {
    public static class Exist extends ServiceException {

        private static final long serialVersionUID = 1L;

        public Exist(Object data) {
            this(data, "-");
        }

        public Exist(Object data, String message) {
            super(data, "Resources exist ( %s ) %s", data.getClass(), message);
        }

        @Override
        public String i18n(MessageSource messageSource) {
            String name = this.getData()[0].getClass().getSimpleName();
            return this.i18n(messageSource, "exist.resource.class." + name, this.getData());
        }

    }

    public static class NotExist extends ServiceException {

        private static final long serialVersionUID = 1L;

        public NotExist(Class resource) {
            this(resource, "-");
        }

        public NotExist(Class resource, String message) {
            super(resource, "RESOURCES not exist ( %s ) %s", resource.getName(), message == null ? "-" : message);
        }

        @Override
        public String i18n(MessageSource messageSource) {
            String name = ((Class) getData()[0]).getSimpleName();
            return this.i18n(messageSource, "not.find.resource.class." + name, this.getData());
        }

    }
}

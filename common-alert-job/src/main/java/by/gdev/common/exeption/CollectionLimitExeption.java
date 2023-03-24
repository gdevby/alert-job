package by.gdev.common.exeption;

public class CollectionLimitExeption extends RuntimeException {

    private static final long serialVersionUID = -3639118869348656522L;

    public CollectionLimitExeption() {
    }

    public CollectionLimitExeption(String message) {
	this(message, null);
    }

    public CollectionLimitExeption(String message, Throwable cause) {
	super(message, cause);
    }
}
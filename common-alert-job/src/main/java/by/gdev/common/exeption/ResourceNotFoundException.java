package by.gdev.common.exeption;


public class ResourceNotFoundException extends RuntimeException{

	private static final long serialVersionUID = 4778523522069939786L;

	public ResourceNotFoundException() {
		this("Entity not found!");
	}
	
	public ResourceNotFoundException(String message) {
		this(message, null);
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}

package by.gdev.common.exeption;

public class ConflictExeption extends RuntimeException{
	
	private static final long serialVersionUID = -3639118869348656522L;

	public ConflictExeption() {
	}
	
	public ConflictExeption(String message) {
		this(message, null);
	}

	public ConflictExeption(String message, Throwable cause) {
		super(message, cause);
	}

}

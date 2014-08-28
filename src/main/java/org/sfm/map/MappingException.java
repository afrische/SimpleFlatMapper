package org.sfm.map;

public class MappingException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6011846874461134804L;

	public MappingException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public  MappingException(String message) {
		super(message);
	}

}

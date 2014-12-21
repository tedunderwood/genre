package pages;

/**
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-17
 * 
 *
 */
@SuppressWarnings("serial")
public class InputFileException extends Throwable {

	/**
	 * 
	 */
	public InputFileException() {
		super();
	}

	/**
	 * @param arg0 String message assigned when thrown.
	 */
	public InputFileException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public InputFileException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public InputFileException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}

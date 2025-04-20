public class InvalidProgramException extends Exception {
    public InvalidProgramException() {
        super("Invalid program format or syntax.");
    }

    public InvalidProgramException(String message) {
        super(message);
    }
}


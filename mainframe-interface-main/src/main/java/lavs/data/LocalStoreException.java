package lavs.data;


// Local SQLite file couldn't be read or written (eg disk full or file's locked).
public class LocalStoreException extends RuntimeException {

    public LocalStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}

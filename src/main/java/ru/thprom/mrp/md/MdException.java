package ru.thprom.mrp.md;


public class MdException extends RuntimeException {

    /**
     * Construct a <code>MdException</code> with the specified detail message.
     * @param message the detail message
     */
    public MdException(String message) {
        super(message);
    }

    /**
     * Construct a <code>MdException</code> with the specified detail message
     * and nested exception.
     * @param message the detail message
     * @param ex the nested exception
     */
    public MdException(String message, Throwable ex) {
        super(message, ex);
    }

}

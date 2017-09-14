package org.gcsl.sdif;

public class SdifException extends Exception
{
    public SdifException(String message)    { super(message); }
    public SdifException(Throwable cause)   { super(cause); }
    public SdifException(String message,
                         Throwable cause)   { super(message, cause); }
}

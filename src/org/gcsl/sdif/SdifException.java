package org.gcsl.sdif;

public class SdifException extends Exception
{
    SdifException(String message)    { super(message); }
    SdifException(Throwable cause)   { super(cause); }
    SdifException(String message,
                  Throwable cause)   { super(message, cause); }
}

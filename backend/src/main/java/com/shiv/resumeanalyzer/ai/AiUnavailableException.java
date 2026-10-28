package com.shiv.resumeanalyzer.ai;

/** The LLM couldn't produce a usable answer (no key, network error, timeout, bad response). */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

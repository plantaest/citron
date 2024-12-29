package io.github.plantaest.citron.enumeration;

public enum FeedbackStatus {
    GOOD(0),
    BAD(1);

    private final int code;

    FeedbackStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

package com.supremeai.exception;

public class SimulatorQuotaExceededException extends SimulatorException {
    private final int used;
    private final int limit;

    public SimulatorQuotaExceededException(int used, int limit) {
        super(String.format("Quota exceeded: %d/%d apps installed", used, limit));
        this.used = used;
        this.limit = limit;
    }

    public SimulatorQuotaExceededException(String message) {
        super(message);
        this.used = -1;
        this.limit = -1;
    }

    public int getUsed() { return used; }
    public int getLimit() { return limit; }
}

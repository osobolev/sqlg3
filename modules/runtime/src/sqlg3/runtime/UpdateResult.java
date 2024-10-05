package sqlg3.runtime;

public final class UpdateResult {

    private final int rows;
    private final Object[] generatedKeys;

    public UpdateResult(int rows, Object[] generatedKeys) {
        this.rows = rows;
        this.generatedKeys = generatedKeys;
    }

    public int getRows() {
        return rows;
    }

    public Object[] getGeneratedKeys() {
        return generatedKeys;
    }

    public int getGeneratedInt() {
        return ((Number) generatedKeys[0]).intValue();
    }

    public long getGeneratedLong() {
        return ((Number) generatedKeys[0]).longValue();
    }
}

package sqlg3.preprocess;

final class ClassName {

    private final String name;

    private ClassName(String name) {
        this.name = name;
    }

    static ClassName nested(String outer, String inner) {
        return new ClassName(outer + "." + inner);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClassName) {
            ClassName that = (ClassName) obj;
            return this.name.equals(that.name);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}

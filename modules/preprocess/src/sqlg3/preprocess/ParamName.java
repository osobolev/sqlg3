package sqlg3.preprocess;

final class ParamName {

    private final String name;

    private ParamName(String name) {
        this.name = name;
    }

    static ParamName fromId(String id) {
        return new ParamName(id);
    }

    static ParamName fromLocation(String fullClassName, String methodName, String paramName) {
        String location = fullClassName + (methodName == null ? "" : "." + methodName);
        return new ParamName(location + "." + paramName);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ParamName) {
            ParamName that = (ParamName) obj;
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

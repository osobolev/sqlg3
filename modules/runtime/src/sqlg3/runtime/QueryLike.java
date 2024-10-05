package sqlg3.runtime;

public interface QueryLike {

    String getSql();

    Parameter[] getParameters();
}

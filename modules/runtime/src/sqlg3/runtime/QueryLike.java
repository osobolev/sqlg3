package sqlg3.runtime;

import java.util.List;

public interface QueryLike {

    String getSql();

    List<Parameter> getParameters();
}

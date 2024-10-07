package sqlg3.runtime;

import java.util.List;

public interface QueryLike {

    CharSequence getSqlChars();

    String getSql();

    List<Parameter> getParameters();
}

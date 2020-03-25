package sqlg3.preprocess;

import java.lang.reflect.Method;

final class RunMethod {

    final MethodEntry entry;
    final Method method;

    RunMethod(MethodEntry entry, Method method) {
        this.entry = entry;
        this.method = method;
    }
}

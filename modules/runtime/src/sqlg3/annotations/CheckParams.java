package sqlg3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods marking that given method contains parameters and their type cannot be determined
 * from calls to business methods. Annotate methods which cause preprocessing errors of unknown parameter types.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface CheckParams {
}

package sqlg3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for upper level class marking that this class should be processed
 * by preprocessor. Such class should inherit from {@link sqlg3.runtime.GBase}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface SQLG {
}

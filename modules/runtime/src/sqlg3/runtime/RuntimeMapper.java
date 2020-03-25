package sqlg3.runtime;

/**
 * This class defines custom Java/SQL mapping.
 */
public interface RuntimeMapper {

    <T> TypeMapper<T> getMapper(Class<T> cls);
}

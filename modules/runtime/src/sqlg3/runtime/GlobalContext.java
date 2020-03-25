package sqlg3.runtime;

import sqlg3.core.SQLGLogger;

public final class GlobalContext {

    public final SQLGLogger logger;
    final DBSpecific db;
    final TypeMappers mappers;
    final SqlTrace trace; // todo: make mutable???
    final Caches caches = new Caches();

    public GlobalContext(SQLGLogger logger, DBSpecific db, TypeMappers mappers, SqlTrace trace) {
        this.logger = logger;
        this.db = db;
        this.mappers = mappers;
        this.trace = trace;
    }

    public GlobalContext(SQLGLogger logger, DBSpecific db, TypeMappers mappers) {
        this(logger, db, mappers, SqlTrace.createDefault(logger));
    }
}

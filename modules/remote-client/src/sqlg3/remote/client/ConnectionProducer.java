package sqlg3.remote.client;

import sqlg3.remote.common.IRemoteDBInterface;

/**
 * Used for client reconnects.
 */
public interface ConnectionProducer {

    IRemoteDBInterface open() throws Exception;
}

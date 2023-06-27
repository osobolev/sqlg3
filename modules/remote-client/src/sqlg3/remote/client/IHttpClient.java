package sqlg3.remote.client;

import sqlg3.remote.common.HttpRequest;
import sqlg3.remote.common.HttpResult;

import java.io.IOException;
import java.lang.reflect.Type;

public interface IHttpClient {

    Object newContext();

    HttpResult call(Type retType, Object clientContext, HttpRequest request) throws IOException;
}

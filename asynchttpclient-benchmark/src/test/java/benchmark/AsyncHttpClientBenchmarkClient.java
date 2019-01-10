package benchmark;

import org.asynchttpclient.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class AsyncHttpClientBenchmarkClient implements com.ss.benchmark.httpclient.HttpClient {

    private AsyncHttpClient client;
    private String baseUrl;

    @Override
    public void createClient(String host, int port) {
        AsyncHttpClientConfig cf = new DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setReadTimeout(READ_TIMEOUT)
                //.setRequestTimeout(REQUEST_TIMEOUT)
                .setMaxConnections(MAX_CONNECTION_POOL_SIZE)
                .setKeepAlive(true)
                //.setConnectionTtl(CONNECTION_TTL)
                .build();

        client = new DefaultAsyncHttpClient(cf);
        baseUrl = url(host, port);
    }

    @Override
    public String blockingGET(String path) {
        return suppressChecked(() -> nonblockingGET(path).get());
    }

    @Override
    public String blockingPOST(String path, String body) {
        return suppressChecked(() -> nonblockingPOST(path, body).get());
    }

    @Override
    public CompletableFuture<String> nonblockingGET(String path) {
        Request req = client.prepareGet(mkUrl(path)).build();
        return execute(req);
    }

    @Override
    public CompletableFuture<String> nonblockingPOST(String path, String body) {
        Request req = client.preparePost(mkUrl(path)).setBody(body).build();
        return execute(req);
    }

    private CompletableFuture<String> execute(Request request) {
        return client.executeRequest(request)
                .toCompletableFuture()
                .thenApply(resp ->
                        suppressChecked(() -> new String(resp.getResponseBodyAsBytes(), "UTF-8"))
                );
    }

    private String mkUrl(String path) {
        if (path.startsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    private <T> T suppressChecked(Callable<T> c) {
        try {
            return c.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException("Suppressed checked exception.", e);
        }
    }
}

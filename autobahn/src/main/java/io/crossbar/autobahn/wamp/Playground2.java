// This is a WAMP test client using Netty for WAMP-over-WebSocket
// to be run on server side
public class WAMPClientNetty {

    // the Crossbar.io router listening URL, eg
    // ws(s)://<hostname>[:<port>][/<path>]
    private final String url;

    // the WAMP session to communicate with Crossbar.io
    private final Session session;

    // the WAMP transport we use
    private final NettyTransport transport;

    public WAMPClientNetty(String url) {
        this.url = url;
        this.session = new Session();
    }


    public static void main(String[] args) throws Exception {
        String url;
        if (args.length < 1) {
            url = "ws://localhost:8080/ws";
        } else {
            url = args[0];
        }
        new EchoClient(url).start();
    }

    private void start() {

        // TODO: actually connect transport ...

        // attach the (already connected) transport to the session
        this.session.attach(this.transport);

        // list of WAMP auth methods we will offer
        List<Auth> auths = new List<Auth>();
        auths.add(new CraAuth("john", "secret123"));
        auths.add(new AnonymousAuth());

        // now trigger the WAMP opening handshake
        CompletableFuture<SessionDetails> future = this.session.join("realm1", auths);

        // we wait until opening handshake has finished: the session is now "joined"
        SessionDetails details = future.get();
        System.out.println("session joined under session ID " + details.id);

        // register a procedure.
        this.session.register("com.example.add2", this.add2, null).thenApply(
            registration -> System.out.println("add2 registered under " + registration.id)
        );

        // TODO: now sit and wait "forever" .. as we want to serve calls
        // under our registered procedure "add2" forever
    }

    private CompletableFuture<InvocationResult> add2(List<Object> args, Map<String, Object> kwargs) {
        System.out.println("add2 called");
        int result = (int) args.get(0) + (int) args.get(1);
        return new InvocationResult(result)
    }
}

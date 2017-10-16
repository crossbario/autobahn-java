# Release v17.10.5

  * Enable exceptions in user code to include error details (#327)
  * Extend POJO APIs for call/subscribe to accept class references (#326)
  * Simplify examples and remove redundant code (#325)
  * Cleanup Session class (#324)
  * WAMP: Allow Session.register() endpoints to return POJOs (#323)
  * WAMP: Session.subscribe() POJOs support (#320)
  * Update README with simple examples (#319)
  * Extend legacy android support (incubating) (#315)
  * Correctly use gradle deps API so that clients download deps automatically (#314)
  * make: dynamically disable debug logging before releasing (#313)


# Release v17.10.4

  * Session API convenience and cleanup (#312)
  * Rename NettyTransport to NettyWebSocket (#310)
  * Revamp logging (#309)
  * Android: Execute callbacks on main(UI) thread (#307)
  * Simplify the Client API (#306)
  * Fix WebSocket auto-reconnect (#305)


# Release v17.10.3

  * Fix android package to not conflict with main app's label (#299)


# Release v17.10.1

  * Add change log generator (#294)
  * Maven and JCenter config (#293)
  * add script to support older versions of android (#291)
  * Expose API for WS pings/pongs and implement auto keepalive (#287)
  * Reduce java8 usage internally (#285)
  * Make example client universal (#283)
  * Refactor WebSocket impl (#281)
  * Autobahn WAMP transport (#277)
  * Complete event retention support and add Event.topic property (#275)



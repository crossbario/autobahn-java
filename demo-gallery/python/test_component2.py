import os
import argparse
import asyncio
import txaio

from autobahn.wamp.types import RegisterOptions, PublishOptions
from autobahn.asyncio.wamp import ApplicationSession, ApplicationRunner
from autobahn.wamp.exception import ApplicationError


class ClientSession(ApplicationSession):
    def onConnect(self):
        self.log.info("Client connected: {klass}", klass=ApplicationSession)
        self.join(self.config.realm, [u'anonymous'])

    def onChallenge(self, challenge):
        self.log.info("Challenge for method {authmethod} received", authmethod=challenge.method)
        raise Exception("We haven't asked for authentication!")

    async def onJoin(self, details):
        await self.producer()

    async def producer(self):
        self.produce = True

        def stop_producing():
            self.produce = False

        await self.register(stop_producing, u'io.crossbar.example.client1.stop_producing',
                            options=RegisterOptions(invoke=u'roundrobin'))

        # REGISTER
        def add2(a, b):
            print('----------------------------')
            print("add2 called with values {} and {}".format(a, b))
            return a + b

        await self.register(add2, u'io.crossbar.example.client1.add2', options=RegisterOptions(invoke=u'roundrobin'))
        print('----------------------------')
        print('procedure registered: io.crossbar.example.client1.add2')

        counter = 0
        while self.produce:
            # PUBLISH
            await self.publish(u'io.crossbar.example.client1.oncounter', counter,
                               options=PublishOptions(acknowledge=True, exclude_me=True))
            print('----------------------------')
            self.log.info("published to 'oncounter' with counter {counter}", counter=counter)
            counter += 1
            print('----------------------------')

            await asyncio.sleep(1)

        await self.consumer()

    async def consumer(self):
        self.incoming_counter = 0

        # SUBSCRIBE
        def oncounter(counter):
            print('----------------------------')
            self.log.info("'oncounter' event, counter value: {counter}", counter=counter)
            self.incoming_counter += 1

        await self.subscribe(oncounter, u'io.crossbar.example.client2.oncounter')
        print('----------------------------')
        self.log.info("subscribed to topic 'io.crossbar.example.client2.oncounter'")

        x = 0
        while self.incoming_counter < 5 and x < 5:

            # CALL
            try:
                res = await self.call(u'io.crossbar.example.client2.add2', x, 3)
                print('----------------------------')
                self.log.info("add2 result: {result}", result=res)
                x += 1
            except ApplicationError as e:
                # ignore errors due to the frontend not yet having
                # registered the procedure we would like to call
                if e.error != 'wamp.error.no_such_procedure':
                    raise e

            await asyncio.sleep(2)

        res = await self.call("io.crossbar.example.client2.stop_producing")
        print(res)

        self.leave()

    def onLeave(self, details):
        self.log.info("Router session closed ({details})", details=details)
        self.disconnect()

    def onDisconnect(self):
        self.log.info("Router connection closed")
        asyncio.get_event_loop().stop()


if __name__ == '__main__':

    # Crossbar.io connection configuration
    url = os.environ.get('CBURL', u'ws://crossbar:8080/ws')
    realm = os.environ.get('CBREALM', u'realm1')

    # parse command line parameters
    parser = argparse.ArgumentParser()
    parser.add_argument('-d', '--debug', action='store_true', help='Enable debug output.')
    parser.add_argument('--url', dest='url', type=str, default=url,
                        help='The router URL (default: "ws://localhost:8080/ws").')
    parser.add_argument('--realm', dest='realm', type=str, default=realm,
                        help='The realm to join (default: "realm1").')

    args = parser.parse_args()

    # start logging
    if args.debug:
        txaio.start_logging(level='debug')
    else:
        txaio.start_logging(level='info')

    # any extra info we want to forward to our ClientSession (in self.config.extra)
    extra = {
        u'foobar': u'A custom value'
    }

    # now actually run a WAMP client using our session class ClientSession
    runner = ApplicationRunner(url=args.url, realm=args.realm, extra=extra)
    runner.run(ClientSession)

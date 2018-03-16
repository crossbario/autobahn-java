import os
import argparse
import six
import txaio

from twisted.internet import reactor
from twisted.internet.error import ReactorNotRunning
from twisted.internet.defer import inlineCallbacks

from autobahn.twisted.util import sleep
from autobahn.wamp.types import RegisterOptions, PublishOptions
from autobahn.twisted.wamp import ApplicationSession, ApplicationRunner
from autobahn.wamp.exception import ApplicationError

DEPARTMENTS = [u'hr', u'sales', u'development']

PERSONS = [
    {
        u'firstname': u'homer',
        u'lastname': u'simpson',
        u'department': u'hr',
    },
    {
        u'firstname': u'joe',
        u'lastname': u'doe',
        u'department': u'development',
    },
    {
        u'firstname': u'sammy',
        u'lastname': u'davis',
        u'department': u'development',
    },
    {
        u'firstname': u'freddy',
        u'lastname': u'krueger',
        u'department': u'hr',
    },
    {
        u'firstname': u'walter',
        u'lastname': u'white',
        u'department': u'sales',
    },
    {
        u'firstname': u'jesse',
        u'lastname': u'pinkman',
        u'department': u'sales',
    },
    {
        u'firstname': u'pablo',
        u'lastname': u'escobar',
        u'department': u'sales',
    },
]

PERSONS_BY_DEPARTMENT = {}

for person in PERSONS:
    if person[u'department'] not in PERSONS_BY_DEPARTMENT:
        PERSONS_BY_DEPARTMENT[person[u'department']] = []
    PERSONS_BY_DEPARTMENT[person[u'department']].append(person)


class ClientSession(ApplicationSession):
    """
    Our WAMP session class .. place your app code here!
    """

    def onConnect(self):
        self.log.info("Client connected: {klass}", klass=ApplicationSession)
        self.join(self.config.realm, [u'anonymous'])

    def onChallenge(self, challenge):
        self.log.info("Challenge for method {authmethod} received", authmethod=challenge.method)
        raise Exception("We haven't asked for authentication!")

    @inlineCallbacks
    def _init_person_api(self):

        def get_person(emp_no=None):
            self.log.info('PERSON API: get_person(emp_no={emp_no}) called', emp_no=emp_no)
            if emp_no:
                return PERSONS[emp_no]
            else:
                return PERSONS[0]

        yield self.register(get_person, u'com.example.get_person')

        @inlineCallbacks
        def get_person_delayed(emp_no=None, delay=3):
            self.log.info('PERSON API: get_person_delayed(emp_no={emp_no}, delay={delay}) called', emp_no=emp_no,
                          delay=delay)
            if delay:
                yield sleep(delay)
            if emp_no:
                return PERSONS[emp_no]
            else:
                return PERSONS[0]

        yield self.register(get_person_delayed, u'com.example.get_person_delayed')

        def get_all_persons():
            print('PERSON API: get_all_persons() called')
            return PERSONS

        yield self.register(get_all_persons, u'com.example.get_all_persons')

        def get_persons_by_department(department=None):
            self.log.info('PERSON API: get_persons_by_department({department}) called', department=department)
            if department:
                return PERSONS_BY_DEPARTMENT[department]
            else:
                return PERSONS_BY_DEPARTMENT

        yield self.register(get_persons_by_department, u'com.example.get_persons_by_department')

        def add_person(person):
            self.log.info('PERSON API: add_person({person}) called', person=person)
            department = person.get(u'department', None)
            if department not in DEPARTMENTS:
                raise Exception('no such department: {}'.format(department))

            PERSONS.append(person)
            PERSONS_BY_DEPARTMENT[department].append(person)

        yield self.register(add_person, u'com.example.add_person')

        self.log.info('PERSON API registered!')

    @inlineCallbacks
    def onJoin(self, details):

        self.log.info("Connected:  {details}", details=details)

        self._ident = details.authid
        self._type = u'Python'

        self.log.info("Component ID is  {ident}", ident=self._ident)
        self.log.info("Component type is  {type}", type=self._type)

        yield self._init_person_api()

    @inlineCallbacks
    def test(self):
        # REGISTER
        def add2(a, b):
            print('----------------------------')
            print("add2 called on {}".format(self._ident))
            return [ a + b, self._ident, self._type]

        yield self.register(add2, u'com.example.add2', options=RegisterOptions(invoke=u'roundrobin'))
        print('----------------------------')
        print('procedure registered: com.myexample.add2')

        # SUBSCRIBE
        def oncounter(counter, id, type):
            print('----------------------------')
            self.log.info("'oncounter' event, counter value: {counter}", counter=counter)
            self.log.info("from component {id} ({type})", id=id, type=type)

        yield self.subscribe(oncounter, u'com.example.oncounter')
        print('----------------------------')
        self.log.info("subscribed to topic 'oncounter'")

        x = 0
        counter = 0
        while True:

            # CALL
            try:
                res = yield self.call(u'com.example.add2', x, 3)
                print('----------------------------')
                self.log.info("add2 result: {result}", result=res)
                #self.log.info("add2 result: {result}", result=res.results[0])
                # self.log.info("from component {id} ({type})", id=res[1], type=res[2])
                x += 1
            except ApplicationError as e:
                ## ignore errors due to the frontend not yet having
                ## registered the procedure we would like to call
                if e.error != 'wamp.error.no_such_procedure':
                    raise e

            # PUBLISH
            yield self.publish(u'com.example.oncounter', counter, self._ident, self._type,
                               options=PublishOptions(acknowledge=True, exclude_me=False))
            print('----------------------------')
            self.log.info("published to 'oncounter' with counter {counter}",
                          counter=counter)
            counter += 1

            yield sleep(2)

    def onLeave(self, details):
        self.log.info("Router session closed ({details})", details=details)
        self.disconnect()

    def onDisconnect(self):
        self.log.info("Router connection closed")
        try:
            reactor.stop()
        except ReactorNotRunning:
            pass


if __name__ == '__main__':

    # Crossbar.io connection configuration
    url = os.environ.get('CBURL', u'ws://crossbar:8080/ws')
    realm = os.environ.get('CBREALM', u'realm1')

    # parse command line parameters
    parser = argparse.ArgumentParser()
    parser.add_argument('-d', '--debug', action='store_true', help='Enable debug output.')
    parser.add_argument('--url', dest='url', type=six.text_type, default=url,
                        help='The router URL (default: "ws://localhost:8080/ws").')
    parser.add_argument('--realm', dest='realm', type=six.text_type, default=realm,
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
    runner.run(ClientSession, auto_reconnect=True)

package io.crossbar.autobahn.wamp.reflectionRoles;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.types.Registration;
import io.crossbar.autobahn.wamp.types.Subscription;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReflectionServices {
    private final Session mSession;
    private final ISerializer mSerializer;

    public ReflectionServices(Session session, ISerializer serializer){
        mSession = session;
        mSerializer = serializer;
    }

    public List<CompletableFuture<Registration>> registerCallee(Object instance) {
        List<CompletableFuture<Registration>> registrations = new ArrayList<CompletableFuture<Registration>>();
        Class<?> classType = instance.getClass();

        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(WampProcedure.class)) {
                WampProcedure anotation = method.getAnnotation(WampProcedure.class);

                MethodInvocationHandler currentMethodHandler =
                        new MethodInvocationHandler(instance, method, this.mSerializer);

                CompletableFuture<Registration> currentRegistration =
                        mSession.register(anotation.value(), currentMethodHandler);

                registrations.add(currentRegistration);
            }
        }

        return registrations;
    }

    public List<CompletableFuture<Subscription>> registerSubscriber(Object instance) {
        List<CompletableFuture<Subscription>> subscriptions = new ArrayList<CompletableFuture<Subscription>>();
        Class<?> classType = instance.getClass();

        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(WampTopic.class)) {
                WampTopic anotation = method.getAnnotation(WampTopic.class);

                MethodEventHandler currentMethodHandler =
                        new MethodEventHandler(instance, method, this.mSerializer);

                CompletableFuture<Subscription> currentRegistration =
                        mSession.subscribe(anotation.value(), currentMethodHandler);

                subscriptions.add(currentRegistration);
            }
        }

        return subscriptions;
    }

    public <TProxy> TProxy getCalleeProxy(Class<TProxy> proxyClass) {
        // TODO: check the current type is valid for proxy.
        TProxy result =
                (TProxy)
                        Proxy.newProxyInstance(proxyClass.getClassLoader(),
                                new Class[]{proxyClass},
                                new CalleeProxyInvocationHandler(this.mSession));

        return result;
    }
}
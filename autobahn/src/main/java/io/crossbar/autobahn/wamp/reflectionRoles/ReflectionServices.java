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
                CompletableFuture<Registration> currentRegistration = RegisterSingleMethod(instance, method);
                registrations.add(currentRegistration);
            }
        }

        for (Class<?> interfaceType : classType.getInterfaces()) {
            for (Method method : interfaceType.getMethods()) {
                if (method.isAnnotationPresent(WampProcedure.class)) {
                    CompletableFuture<Registration> currentRegistration = RegisterSingleMethod(instance, method);
                    registrations.add(currentRegistration);
                }
            }
        }

        return registrations;
    }

    private CompletableFuture<Registration> RegisterSingleMethod(Object instance, Method method) {
        WampProcedure anotation = method.getAnnotation(WampProcedure.class);

        MethodInvocationHandler currentMethodHandler =
                new MethodInvocationHandler(instance, method, this.mSerializer);

        return mSession.register(anotation.value(), currentMethodHandler);
    }

    public List<CompletableFuture<Subscription>> registerSubscriber(Object instance) {
        List<CompletableFuture<Subscription>> subscriptions = new ArrayList<CompletableFuture<Subscription>>();
        Class<?> classType = instance.getClass();

        for (Method method : classType.getMethods()) {
            if (method.isAnnotationPresent(WampTopic.class)) {
                CompletableFuture<Subscription> currentSubscription = singleSubscribe(instance, method);

                subscriptions.add(currentSubscription);
            }
        }

        for (Class<?> interfaceType : classType.getInterfaces()) {
            for (Method method : interfaceType.getMethods()) {
                if (method.isAnnotationPresent(WampTopic.class)) {
                    CompletableFuture<Subscription> currentSubscription = singleSubscribe(instance, method);

                    subscriptions.add(currentSubscription);
                }
            }
        }


        return subscriptions;
    }

    private CompletableFuture<Subscription> singleSubscribe(Object instance, Method method) {
        WampTopic anotation = method.getAnnotation(WampTopic.class);

        MethodEventHandler currentMethodHandler =
                new MethodEventHandler(instance, method, this.mSerializer);

        return mSession.subscribe(anotation.value(), currentMethodHandler);
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
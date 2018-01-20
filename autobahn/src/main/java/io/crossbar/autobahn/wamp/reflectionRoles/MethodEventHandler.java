package io.crossbar.autobahn.wamp.reflectionRoles;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.TriConsumer;
import io.crossbar.autobahn.wamp.types.EventDetails;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class MethodEventHandler implements TriConsumer<List<Object>, Map<String, Object>, EventDetails> {

    private final Object mInstance;
    private final ArgumentUnpacker mUnpacker;
    private final ISerializer mSerializer;
    private final Method mMethod;

    public MethodEventHandler(Object instance, Method method, ISerializer serializer) {
        this.mInstance = instance;
        this.mMethod = method;
        this.mUnpacker = new ArgumentUnpacker(method);
        this.mSerializer = serializer;
    }

    @Override
    public void accept(List<Object> args, Map<String, Object> kwargs, EventDetails details) {
        try {
            Object[] parameters = this.mUnpacker.unpackParameters(this.mSerializer, args, kwargs);
            this.mMethod.invoke(this.mInstance, parameters);
        }
        catch (Throwable ex){
            // TODO: deal with exception
        }
    }
}

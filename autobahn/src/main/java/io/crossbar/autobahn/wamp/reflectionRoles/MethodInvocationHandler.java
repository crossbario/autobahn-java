package io.crossbar.autobahn.wamp.reflectionRoles;

import io.crossbar.autobahn.wamp.interfaces.IInvocationHandler;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.types.InvocationDetails;
import io.crossbar.autobahn.wamp.types.InvocationResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class MethodInvocationHandler implements IInvocationHandler {

    private final Object mInstance;
    private final ArgumentUnpacker mUnpacker;
    private final ISerializer mSerializer;
    private final Method mMethod;

    public MethodInvocationHandler(Object instance, Method method, ISerializer serializer) {
        this.mInstance = instance;
        this.mMethod = method;
        this.mUnpacker = new ArgumentUnpacker(method);
        this.mSerializer = serializer;
    }

    @Override
    public InvocationResult apply(List<Object> list, Map<String, Object> map, InvocationDetails invocationDetails) {
        Object[] parameters = this.mUnpacker.unpackParameters(this.mSerializer, list, map);

        try
        {
            Object result = this.mMethod.invoke(mInstance, parameters);
            return new InvocationResult(result);
        }
        catch (Exception e){
            if (e instanceof InvocationTargetException){
                InvocationTargetException casted = (InvocationTargetException) e;
                Throwable targetException = casted.getTargetException();
                Throwable convertedException = convertRuntimeException(targetException);
                // TODO: throw convertedException
                // TODO: It is currently not possible to throw an exception from this interface.
            }
        }

        return new InvocationResult((Object)null);
    }

    private Throwable convertRuntimeException(Throwable targetException) {
        if (targetException instanceof WampException){
            return targetException;
        }

        return new WampException("wamp.error.runtime_error", targetException.getMessage());
    }
}

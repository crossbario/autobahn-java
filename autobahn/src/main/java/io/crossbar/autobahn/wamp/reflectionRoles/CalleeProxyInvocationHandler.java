package io.crossbar.autobahn.wamp.reflectionRoles;

import io.crossbar.autobahn.wamp.Session;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CalleeProxyInvocationHandler implements InvocationHandler {

    private final Session mSession;

    public CalleeProxyInvocationHandler(Session session) {
        this.mSession = session;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getReturnType() == CompletableFuture.class) {
            return handleAsync(method, args);
        } else {
            return handleSync(method, args);
        }
    }

    public Object handleSync(Method method, Object[] args) {
        try {
            CompletableFuture<?> task =
                    innerHandleAsync(method,
                            args,
                            method.getReturnType());

            return task.get();
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();

            if (cause instanceof RuntimeException) {
                throw ((RuntimeException) cause);
            } else {
                throw new RuntimeException(cause.getMessage(), cause);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public Object handleAsync(Method method, Object[] args) {
        Type taskType = method.getGenericReturnType();
        Type returnType = getTaskGenericParameterType(taskType);

        return innerHandleAsync(method, args, returnType);
    }

    private Type getTaskGenericParameterType(Type taskType) {
        Type result = ((ParameterizedType) taskType).getActualTypeArguments()[0];
        return result;
    }

    private CompletableFuture<?> innerHandleAsync(Method method, Object[] args, Type returnType) {
        WampProcedure annotation = method.getAnnotation(WampProcedure.class);
        String procedureUri = annotation.value();

        List<Object> callArguments = null;

        if (args != null){
            callArguments = Arrays.asList(args);
        }

        CompletableFuture<?> result = this.mSession.call(procedureUri, callArguments, new ReflectionTypeReference(returnType));

        return result;
    }
}
package io.crossbar.autobahn.wamp.reflectionRoles;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.utils.Platform;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

public class ArgumentUnpacker {
    private final ParameterInfo[] mParameters;

    public ArgumentUnpacker(Method method) {

        if (!Platform.isAndroid() || Platform.getAndroidAPIVersion() >= 26) {
            Parameter[] parameters = method.getParameters();
            mParameters = new ParameterInfo[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                String parameterName = parameter.getName();
                Class<?> parameterType = parameter.getType();
                mParameters[i] = new ParameterInfo(i, parameterName, parameterType);
            }
        } else {
            Class<?>[] parameterTypes = method.getParameterTypes();
            mParameters = new ParameterInfo[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                String parameterName = "arg" + i;
                Class<?> parameterType = parameterTypes[i];
                mParameters[i] = new ParameterInfo(i, parameterName, parameterType);
            }
        }
    }

    public Object[] unpackParameters(ISerializer serializer, List<Object> list, Map<String, Object> map) {
        Object[] result = new Object[mParameters.length];

        int namedParametersStartPosition = 0;

        if (list != null) {
            namedParametersStartPosition = list.size();

            // Positional arguments have higher precedence.
            for (int i = 0; i < list.size(); i++) {
                // TODO: surround with a try-catch block and throw a WampException indicating that the
                // TODO: parameter at position i wasn't of the expected type.
                result[i] = serializer.convertValue(list.get(i), mParameters[i].getType());
            }
        }

        if (map != null) {
            for (int i = namedParametersStartPosition; i < mParameters.length; i++) {
                ParameterInfo currentParameter = mParameters[i];

                String parameterName = currentParameter.getName();

                if (!map.containsKey(parameterName)) {
                    // TODO: throw a WampException indicating that a
                    // TODO: parameter with name parameterName or position i was not present.
                } else {
                    // TODO: surround with a try-catch block and throw a WampException indicating that the
                    // TODO: parameter with name parameterName wasn't of the expected type.
                    result[i] = serializer.convertValue(map.get(parameterName), currentParameter.getType());
                }
            }
        }

        return result;
    }
}


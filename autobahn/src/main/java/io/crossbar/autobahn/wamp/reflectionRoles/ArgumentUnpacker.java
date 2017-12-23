package io.crossbar.autobahn.wamp.reflectionRoles;

import io.crossbar.autobahn.wamp.interfaces.ISerializer;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

public class ArgumentUnpacker {
    private final ParameterInfo[] mParameters;

    public ArgumentUnpacker(Parameter[] parameters) {
        mParameters = new ParameterInfo[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String parameterName = parameter.getName();
            Class<?> parameterType = parameter.getType();
            mParameters[i] = new ParameterInfo(i, parameterName, parameterType);
        }
    }

    public Object[] unpackParameters(ISerializer serializer, List<Object> list, Map<String, Object> map){
        Object[] result = new Object[mParameters.length];

        // Positional arguments have higher precedence.
        for (int i = 0; i < list.size(); i++) {
            // TODO: surround with a try-catch block and throw a WampException indicating that the
            // TODO: parameter at position i wasn't of the expected type.
            result[i] = serializer.convertValue(list.get(i), mParameters[i].getType());
        }

        for (int i = list.size(); i < mParameters.length; i++){
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

        return result;
    }
}


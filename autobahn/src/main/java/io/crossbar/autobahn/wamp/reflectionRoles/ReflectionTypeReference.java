package io.crossbar.autobahn.wamp.reflectionRoles;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Type;

class ReflectionTypeReference extends TypeReference<Object> {
    private final Type _newType;

    public ReflectionTypeReference(Type type) {
        _newType = type;
    }

    @Override
    public Type getType() {

        return this._newType;
    }
}

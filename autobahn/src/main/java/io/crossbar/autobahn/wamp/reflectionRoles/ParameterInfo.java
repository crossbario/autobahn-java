package io.crossbar.autobahn.wamp.reflectionRoles;

public class ParameterInfo{
    private final int mPosition;
    private final String mName;
    private final Class<?> mType;

    public ParameterInfo(int position, String name, Class<?> type) {
        this.mPosition = position;
        this.mName = name;
        this.mType = type;
    }

    public int getPosition() {
        return mPosition;
    }

    public String getName() {
        return mName;
    }

    public Class<?> getType() {
        return mType;
    }
}

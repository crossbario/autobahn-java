package io.crossbar.autobahn.wamp.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Formatter;

public class AuthUtil {

    private static Class getBase64ClassAndroid() {
        try {
            return Class.forName("android.util.Base64");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class getBase64ClassJava8() {
        try {
            return Class.forName("java.util.Base64");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toBinary(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                    Character.digit(s.charAt(i + 1), 16));
        return data;
    }

    public static String toHexString(byte[] buf) {
        Formatter formatter = new Formatter();
        for (byte b : buf) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static byte[] decodeString(String privateKey) throws Exception {
        if (Platform.isAndroid()) {
            Class base64Class = getBase64ClassAndroid();
            Method method = base64Class.getMethod("decode", String.class, int.class);
            Field field = base64Class.getField("DEFAULT");
            return (byte[]) method.invoke(null, privateKey, field.getInt(base64Class));
        } else {
            Class base64Class = getBase64ClassJava8();
            Object encoderObject = base64Class.getMethod("getDecoder").invoke(null);
            return (byte[]) encoderObject.getClass().getMethod(
                    "decode", String.class).invoke(encoderObject, privateKey);
        }
    }

    public static String encodeToString(byte[] challenge) throws Exception {
        if (Platform.isAndroid()) {
            // Base64.encodeToString(sha256_HMAC.doFinal(challenge), Base64.DEFAULT).trim()
            // Equivalent of the above commented line of code but using reflections
            // so that this class works on both android and non-android systems.
            Class base64Class = getBase64ClassAndroid();
            Method method = base64Class.getMethod("encodeToString", byte[].class, int.class);
            Field field = base64Class.getField("DEFAULT");
            String result = (String) method.invoke(null, challenge, field.getInt(base64Class));
            return result.trim();
        } else {
            // Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(challenge)).trim();
            Class base64Class = getBase64ClassJava8();
            Object encoderObject = base64Class.getMethod("getEncoder").invoke(null);
            String result = (String) encoderObject.getClass().getMethod(
                    "encodeToString", byte[].class).invoke(encoderObject, challenge);
            return result.trim();
        }
    }
}

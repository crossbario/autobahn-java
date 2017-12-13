package io.crossbar.autobahn.wamp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthUtil {

    private static final String SSH_BEGIN = "-----BEGIN OPENSSH PRIVATE KEY-----";
    private static final String SSH_END = "-----END OPENSSH PRIVATE KEY-----";
    private static final String OPENSSH_KEY_V1 = "openssh-key-v1";

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

    public static Map<String, byte[]> parseOpenSSHFile(File file) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String sCurrentLine;
        List<String> lines = new ArrayList<>();
        while ((sCurrentLine = br.readLine()) != null) {
            lines.add(sCurrentLine);
        }
        return parseOpenSSHFile(lines);
    }

    private static Map<String, byte[]> parseOpenSSHFile(List<String> lines) throws Exception {
        if (!lines.get(0).equals(SSH_BEGIN) || !lines.get(lines.size() - 1).equals(SSH_END)) {
            throw new RuntimeException("Invalid OPENSSH file");
        }
        lines.remove(0);
        lines.remove(lines.size() - 1);
        StringBuilder base64StringBuilder = new StringBuilder();
        for (String line: lines) {
            base64StringBuilder.append(line);
        }
        String base64String = base64StringBuilder.toString();
        byte[] rawKey = decodeString(base64String);

        byte[] verify = Arrays.copyOfRange(rawKey, 0, OPENSSH_KEY_V1.length());
        if (!new String(verify).equals(OPENSSH_KEY_V1)) {
            throw new RuntimeException("Invalid OPENSSH file");
        }

        boolean occurred = false;
        int index = 0;
        for (int i = 0; i < rawKey.length; i++) {
            if (rawKey[i] == 's'
                    && rawKey[i + 1] == 's'
                    && rawKey[i + 2] == 'h'
                    && rawKey[i + 3] == '-'
                    && rawKey[i + 4] == 'e'
                    && rawKey[i + 5] == 'd'
                    && rawKey[i + 6] == '2'
                    && rawKey[i + 7] == '5'
                    && rawKey[i + 8] == '5'
                    && rawKey[i + 9] == '1'
                    && rawKey[i + 10] == '9'
                    && rawKey[i + 11] == 0x00
                    && rawKey[i + 12] == 0x00
                    && rawKey[i + 13] == 0x00
                    && rawKey[i + 14] == ' ') {
                index = i + 15;
                if (occurred) {
                    break;
                }
                occurred = true;
            }
        }

        byte[] publicKey = Arrays.copyOfRange(rawKey, index, index + 32);

        index += 32;
        for (int i = index; i < rawKey.length; i++) {
            if (rawKey[i] == 0x00
                    && rawKey[i + 1] == 0x00
                    && rawKey[i + 2] == 0x00
                    && rawKey[i + 3] == '@') {
                index = i + 4;
                break;
            }
        }

        byte[] privateKey = Arrays.copyOfRange(rawKey, index, index + 32);

        Map<String, byte[]> result = new HashMap<>();
        result.put("pubkey", publicKey);
        result.put("privkey", privateKey);
        return result;
    }
}

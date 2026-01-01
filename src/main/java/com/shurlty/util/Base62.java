package com.shurlty.util;

public final class Base62 {
    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int BASE = 62;

    private Base62() {}

    public static String encode(long num) {
        if (num < 0) throw new IllegalArgumentException("num must be >= 0");
        if (num == 0) return "0";

        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            int r = (int) (num % BASE);
            sb.append(ALPHABET[r]);
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    public static String padLeft(String s, int len) {
        if (s.length() >= len) return s;
        StringBuilder out = new StringBuilder(len);
        for (int i = s.length(); i < len; i++) out.append('0');
        out.append(s);
        return out.toString();
    }
}

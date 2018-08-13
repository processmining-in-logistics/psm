package org.processmining.scala.log.common.utils.common;

import java.util.Arrays;

public final class Radix26 {

    private final static int RADIX = 26;

    private Radix26() {

    }

    private static String repeat(final char l, final int n) {
        if(n > 0) {
            char[] chars = new char[n];
            Arrays.fill(chars, l);
            return new String(chars);
        }else{
            return new String();
        }
    }

    public static String toString(final long number, final int width) {
        final String javaNumber = Long.toString(number, RADIX);
        final StringBuilder sb = new StringBuilder();
        for (final char l : javaNumber.toCharArray()) {
            if (l >= '0' && l <= '9') {
                sb.append((char) ((l - '0') + 'a'));
            } else {
                sb.append((char) (l + 10));
            }
        }
        final String numberString = sb.toString();
        return repeat('a', width - numberString.length()) + numberString;
    }

    public static Long parseString(final String numberString) {
        final StringBuilder sb = new StringBuilder();
        for (final char l : numberString.toCharArray()) {
            if (l >= 'a' && l <= 'a' + 10) {
                sb.append((char) (l + '0' - 'a'));
            } else {
                sb.append((char) (l - 10));
            }
        }
        return Long.parseLong(sb.toString(), RADIX);
    }



}

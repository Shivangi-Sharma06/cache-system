package com.cse309.cache.persistence;

/**
 * Default pass-through codec for String values.
 */
public final class StringValueCodec implements ValueCodec<String> {

    public static final StringValueCodec INSTANCE = new StringValueCodec();

    @Override
    public String encode(String value) {
        return value != null ? value : "";
    }

    @Override
    public String decode(String encoded) {
        return encoded != null ? encoded : "";
    }
}

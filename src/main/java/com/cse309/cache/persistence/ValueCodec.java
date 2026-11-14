package com.cse309.cache.persistence;

/**
 * Strategy codec for encoding and decoding generic cache values to and from text.
 * Demonstrates generic interface (Unit 5).
 *
 * @param <V> value type
 */
public interface ValueCodec<V> {

    String encode(V value);

    V decode(String encoded);
}

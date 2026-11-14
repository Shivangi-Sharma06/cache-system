package com.cse309.cache.lab;

import com.cse309.cache.core.Cache;
import com.cse309.cache.core.CacheLookupResult;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericsTest {

    @Test
    void testIntegerStringCache() throws CacheException {
        MutableClock clock = new MutableClock();
        Cache<Integer, String> cache = GenericCacheLab.createIntegerStringCache(5, clock);

        cache.put(101, "One Zero One");
        cache.put(102, "One Zero Two");

        CacheLookupResult<String> res = cache.get(101);
        assertTrue(res.isHit());
        assertEquals("One Zero One", res.getValueOrNull());
    }

    @Test
    void testStringStudentCache() throws CacheException {
        MutableClock clock = new MutableClock();
        Cache<String, Student> cache = GenericCacheLab.createStringStudentCache(5, clock);

        Student s1 = new Student(1, "Alice", 3.9);
        Student s2 = new Student(2, "Bob", 3.7);

        cache.put("std:1", s1);
        cache.put("std:2", s2);

        CacheLookupResult<Student> res = cache.get("std:1");
        assertTrue(res.isHit());
        assertEquals(s1, res.getValueOrNull());
        assertEquals(3.9, res.getValueOrNull().getGpa());
    }

    @Test
    void testLongDoubleCache() throws CacheException {
        MutableClock clock = new MutableClock();
        Cache<Long, Double> cache = GenericCacheLab.createLongDoubleCache(5, clock);

        cache.put(9999999999L, 3.14159);
        CacheLookupResult<Double> res = cache.get(9999999999L);
        assertTrue(res.isHit());
        assertEquals(3.14159, res.getValueOrNull());
    }

    @Test
    void testWildcardAndBoundedGenerics() throws CacheException {
        MutableClock clock = new MutableClock();
        Cache<Integer, String> cache = GenericCacheLab.createIntegerStringCache(5, clock);

        cache.put(50, "Fifty");
        cache.put(20, "Twenty");
        cache.put(70, "Seventy");

        // Test wildcard method: inspectAllEntries(Cache<?, ?>)
        List<String> summaries = GenericCacheLab.inspectAllEntries(cache);
        assertEquals(3, summaries.size());

        // Test bounded generic method: <K extends Comparable<K>, V> K smallestKey(Cache<K,V>)
        Integer minKey = GenericCacheLab.smallestKey(cache);
        assertNotNull(minKey);
        assertEquals(20, minKey);
    }

    @Test
    void testStudentComparableAndEquals() {
        Student a = new Student(1, "Alice", 3.9);
        Student b = new Student(1, "Alice", 3.9);
        Student c = new Student(2, "Charlie", 3.5);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.compareTo(c) > 0, "Higher GPA should come first");
    }
}

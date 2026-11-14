package com.cse309.cache.lab;

import java.util.Objects;

/**
 * Domain model class demonstrating equals(), hashCode(), toString(),
 * and Comparable implementation (Unit 2, Unit 4).
 */
public final class Student implements Comparable<Student> {

    private final int id;
    private final String name;
    private final double gpa;

    public Student(int id, String name, double gpa) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.gpa = gpa;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getGpa() {
        return gpa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student student)) return false;
        return id == student.id &&
                Double.compare(student.gpa, gpa) == 0 &&
                name.equals(student.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, gpa);
    }

    @Override
    public String toString() {
        return String.format("Student{id=%d, name='%s', gpa=%.2f}", id, name, gpa);
    }

    @Override
    public int compareTo(Student other) {
        if (other == null) return 1;
        int gpaComp = Double.compare(this.gpa, other.gpa);
        if (gpaComp != 0) return gpaComp;
        return Integer.compare(this.id, other.id);
    }
}

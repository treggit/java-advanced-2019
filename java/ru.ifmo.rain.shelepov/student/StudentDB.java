package ru.ifmo.rain.shelepov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    final private String DEFAULT_STRING = "";

    private <R, V, C extends Collection<R>> C mapAndCollect(Collection<V> collection, Function<V, R> mapper, Supplier<C> collectionFactory) {
        return collection.stream()
                .map(mapper)
                .collect(Collectors.toCollection(collectionFactory));
    }

    private <R, V> List<R> mapAndCollectToList(Collection<V> collection, Function<V, R> mapper) {
        return mapAndCollect(collection, mapper, ArrayList::new);
    }

    private <R, V> Set<R> mapAndCollectToSet(Collection<V> collection, Function<V, R> mapper) {
        return mapAndCollect(collection, mapper, TreeSet::new);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapAndCollectToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapAndCollectToList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapAndCollectToList(students, Student::getGroup);
    }

    private String getStudentFullName(Student student){
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapAndCollectToList(students, this::getStudentFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapAndCollectToSet(students, Student::getFirstName);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.comparing(Student::getId))
                .map(Student::getFirstName)
                .orElse(DEFAULT_STRING);
    }

    private List<Student> sortStudents(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(students, Comparator.comparing(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(students, Comparator.comparing(Student::getLastName)
                .thenComparing(Student::getFirstName)
                .thenComparing(Student::getId));
    }

    private List<Student> filterStudents(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private List<Student> filterAndSortStudents(Collection<Student> students, Predicate<Student> predicate) {
        return sortStudentsByName(filterStudents(students, predicate));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterAndSortStudents(students, s -> s.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterAndSortStudents(students, s -> s.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filterAndSortStudents(students, s -> s.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }

    private Stream<Map.Entry<String, List<Student>>> getGroupsMapStream(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, Collectors.toList()))
                .entrySet().stream();
    }

    private List<Group> getGroups(Collection<Student> students, UnaryOperator<List<Student>> studentsSorter) {
        return getGroupsMapStream(students)
                .map(e -> new Group(e.getKey(), studentsSorter.apply(e.getValue())))
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroups(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroups(students, this::sortStudentsById);
    }

    private String getLargestGroup(Stream<Map.Entry<String, List<Student>>> students, Comparator<Map.Entry<String, List<Student>>> comparator) {
        return students
                .max(comparator.thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroup(getGroupsMapStream(students),
                Comparator.comparingInt((Map.Entry<String, List<Student>> entry) -> entry.getValue().size()));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroup(getGroupsMapStream(students),
                Comparator.comparing((Map.Entry<String, List<Student>> entry) -> entry.getValue().stream()
                                                                                    .map(Student::getFirstName)
                                                                                    .collect(Collectors.toSet()).size()));
    }
}
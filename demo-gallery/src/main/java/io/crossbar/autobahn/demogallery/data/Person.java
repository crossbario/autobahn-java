package io.crossbar.autobahn.demogallery.data;

public class Person {
    public String firstname;
    public String lastname;
    public String department;

    public Person() {
        this.firstname = "unknown";
        this.lastname = "unknown";
        this.department = "unknown";
    }

    public Person(String firstname, String lastname, String department) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.department = department;
    }
}

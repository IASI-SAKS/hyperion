package it.cnr.saks.hyperion.grouping;

import java.util.ArrayList;
import java.util.List;

public class TestGroup {
    List<String> tests = new ArrayList<>();

    public TestGroup() {}

    public List<String> getTests() {
        return tests;
    }

    public void setTests(List<String> tests) {
        this.tests = tests;
    }

    public void addTest(String test) {
        this.tests.add(test);
    }
}

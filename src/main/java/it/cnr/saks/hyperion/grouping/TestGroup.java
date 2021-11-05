package it.cnr.saks.hyperion.grouping;

import java.util.ArrayList;
import java.util.List;

public class TestGroup {
    private List<String> include = new ArrayList<>();
    private List<String> exclude = new ArrayList<>();

    public TestGroup() {}

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }

    public void addIncludedTest(String test) {
        this.include.add(test);
    }

    public void addExcludedTest(String test) {
        this.exclude.add(test);
    }
}

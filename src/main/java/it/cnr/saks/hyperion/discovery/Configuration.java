package it.cnr.saks.hyperion.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.saks.hyperion.Main;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
    private static final Logger log = LoggerFactory.getLogger(Configuration.class);
    private List<String> sut;
    private List<String> testPrograms;
    private List<String> includeTest;
    private List<String> excludeTest;
    private List<String> additionalClasspath;
    private List<String> excludeTracedPackages;
    private Integer depth;
    private Integer timeout;
    private Integer skip;
    private URL[] classPath;

    private Configuration() {}

    public static Configuration loadConfiguration(String jsonPath) throws AnalyzerException, MalformedURLException {
        File jsonFile = new File(jsonPath);
        Configuration configuration;

        log.info("Loading configuration...");
        ObjectMapper om = new ObjectMapper();
        try {
            configuration = om.readValue(jsonFile, Configuration.class);
        } catch (IOException e) {
            throw new AnalyzerException("Error parsing JSON configuration file " + jsonPath + ": " + e.getMessage());
        }

        configuration.initializeClasspath();;

        return configuration;
    }

    private void initializeClasspath() throws MalformedURLException {
        List<URL> ret = new ArrayList<>();

        for (String p: this.getTestPrograms()) {
            ret.add(new File(p).toURI().toURL());
        }
        for (String p: this.getSut()) {
            ret.add(new File(p).toURI().toURL());
        }
        for (String p: this.getAdditionalClasspath()) {
            ret.add(new File(p).toURI().toURL());
        }
        ret.add(new File("data/jre/rt.jar").toURI().toURL());

        String runtimeClasspath = ManagementFactory.getRuntimeMXBean().getClassPath();
        String separator = System.getProperty("path.separator");
        String[] additionalClasspath = runtimeClasspath.split(separator);

        for (String p: additionalClasspath) {
            ret.add(new File(p).toURI().toURL());
        }

        URL[] arr = new URL[ret.size()];
        this.classPath = ret.toArray(arr);
    }

    public URL[] getClassPath() {
        return this.classPath;
    }

    public List<String> getSut() {
        return sut;
    }

    public void setSut(List<String> sut) {
        this.sut = sut;
    }

    public List<String> getTestPrograms() {
        return testPrograms;
    }

    public void setTestPrograms(List<String> testPrograms) {
        this.testPrograms = testPrograms;
    }

    public List<String> getIncludeTest() {
        return includeTest;
    }

    public void setIncludeTest(List<String> includeTest) {
        this.includeTest = includeTest;
    }

    public List<String> getExcludeTest() {
        return excludeTest;
    }

    public void setExcludeTest(List<String> excludeTest) {
        this.excludeTest = excludeTest;
    }

    public List<String> getAdditionalClasspath() {
        return additionalClasspath;
    }

    public void setAdditionalClasspath(List<String> additionalClasspath) {
        this.additionalClasspath = additionalClasspath;
    }

    public List<String> getExcludeTracedPackages() {
        return excludeTracedPackages;
    }

    public void setExcludeTracedPackages(List<String> excludeTracedPackages) {
        this.excludeTracedPackages = excludeTracedPackages;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getSkip() {
        return skip;
    }

    public void setSkip(Integer skip) {
        this.skip = skip;
    }
}


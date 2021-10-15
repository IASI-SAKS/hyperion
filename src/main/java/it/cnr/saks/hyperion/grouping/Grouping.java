package it.cnr.saks.hyperion.grouping;

import it.cnr.saks.hyperion.GroupingRunnerHelper;
import it.cnr.saks.hyperion.similarity.SimilarTests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Grouping {
    private static final Logger log = LoggerFactory.getLogger(Grouping.class);
    String policy = null;

    // Don't use, we need a policy
    private Grouping() {}

    public Grouping(String policy) {
        this.policy = policy;
    }

    private TestGroup policy1(List<String> allTests, SimilarTests[] similarTests, double threshold) {
        TestGroup result = new TestGroup();

        // Build the random group according to this policy
        while(allTests.size() > 0) {
            String test = allTests.get(ThreadLocalRandom.current().nextInt(allTests.size()));

            log.info("Picking {} as a candidate test.", test);

            boolean include = true;

            /* Find a test program in `result` such that `similarTests` has at least one pair
               involving `test` as either TP1 or TP2, such that the similarity score is
               greater than threshold specified by the user
            */
            outerLoop:
            for (String otherTest : result.getTests()) {
                for (SimilarTests pair : similarTests) {
                    if (((pair.getTP1().equals(test) && pair.getTP1().equals(otherTest))
                            || (pair.getTP1().equals(otherTest) && pair.getTP1().equals(test)))
                            && pair.getScore() > threshold) {
                        include = false;
                        break outerLoop;
                    }
                }
            }

            if(include) {
                result.addTest(test);
                log.info("Included {} in the test group.", test);
            }

            allTests.remove(test);
        }

        return result;
    }

    public TestGroup groupTests(SimilarTests[] similarTests, double threshold) throws GroupingException {
        List<String> allTests = new ArrayList<>();

        // Build the set of all tests that have a similarity
        for(SimilarTests tests: similarTests) {
            if(!allTests.contains(tests.getTP1()))
                allTests.add(tests.getTP1());
            if(!allTests.contains(tests.getTP2()))
                allTests.add(tests.getTP2());
        }

        log.info("Determining test group according to policy \"{}\"...", policy);

        // Pick the corresponding policy
        if (this.policy.equals("policy 1")) {
            return policy1(allTests, similarTests, threshold);
        }
        throw new GroupingException("Unsupported Grouping Policy");
    }
}

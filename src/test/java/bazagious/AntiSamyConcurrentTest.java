package bazagious;

import org.apache.commons.lang.mutable.MutableInt;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/**
 * Tests the thread safety and performance of the filter
 *
 * @author barry
 * @since 2011/04/13 10:58 AM
 */
public class AntiSamyConcurrentTest {

    @Test
    public void testThreadsafe() throws Exception {
        //basic test that our filter is thread-safe
        Filter filter = new AntiSamyFilter();
        int nThreads = 1000;
        filter.init(null);

        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        List<Future<String[]>> results = new ArrayList<Future<String[]>>();
        for (int i = 0; i < nThreads; i++) {
            //add the requests to the executor for parallel execution
            results.add(executor.submit(new TestRunnable(filter)));
        }
        //don't accept new tasks
        executor.shutdown();

        for (Future<String[]> result : results) {
            String[] values = result.get();
            String msg = "parameter " + values[0] + " did not include any markup, original value " +
                    "should be the same as the filtered value.";
            assertEquals(msg, values[0], values[1]);
        }

        // Wait until all threads are finish
        while (!executor.isTerminated()) {}
    }

    @Test
    public void testPerformanceComparedToDummyFilter() throws Exception {
        int numberOfParams = 100;
        int requestCount = 10;

        Map<String, String[]> randomParameters = generatedRandomParameters(numberOfParams);

        DummyFilter dummyFilter = new DummyFilter();
        long timeTakenDummyFilter = executeDummyRequestsAgainstFilter(dummyFilter, requestCount, randomParameters);
        System.out.println("timeTakenDummyFilter = " + timeTakenDummyFilter + "ms");

        Filter antiSamyFilter = new AntiSamyFilter();
        long timeTakenAntsamyFilter = executeDummyRequestsAgainstFilter(antiSamyFilter, requestCount, randomParameters);
        System.out.println("timeTakenAntsamyFilter = " + timeTakenAntsamyFilter + "ms");

        double timePenaltyPerRequestParam = ((double) timeTakenAntsamyFilter - timeTakenDummyFilter) / ((double) numberOfParams * requestCount);
        System.out.println("overhead per request parameter = " + timePenaltyPerRequestParam + "ms");
    }

    private long executeDummyRequestsAgainstFilter(Filter dummyFilter, int requestCount, Map<String, String[]> randomParameters) throws Exception {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameters(randomParameters);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            dummyFilter.doFilter(request, response, chain);

            @SuppressWarnings("unchecked")
            Map<String, String[]> filteredParams = chain.getRequest().getParameterMap();
            assertMapEquals(randomParameters, filteredParams);
        }
        return System.currentTimeMillis() - startTime;
    }

    private void assertMapEquals(Map<String, String[]> expected, Map<String, String[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (String key : expected.keySet()) {
            assertTrue(actual.containsKey(key));
            assertArrayEquals(expected.get(key), actual.get(key));
        }
    }

    private Map<String, String[]> generatedRandomParameters(int numberOfParams) {
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        for (int i = 0; i < numberOfParams; i++) {
            parameters.put("param" + i, new String[]{UUID.randomUUID().toString()});
        }
        return parameters;
    }

    private static class TestRunnable implements Callable<String[]> {

        private final static MutableInt concurrentThreads = new MutableInt(0);
        private final Filter filter;

        private TestRunnable(Filter filter) {
            this.filter = filter;
        }

        public String[] call() throws Exception {
            synchronized (concurrentThreads) {
                concurrentThreads.increment();
                //System.out.println("concurrentThreads = " + concurrentThreads);
            }
            try {
                MockHttpServletRequest request = new MockHttpServletRequest();
                String randomValue = UUID.randomUUID().toString();
                String param_name = "param_name";
                request.setParameter(param_name, randomValue);
                MockHttpServletResponse response = new MockHttpServletResponse();
                MockFilterChain filterChain = new MockFilterChain();
                try {
                    filter.doFilter(request, response, filterChain);
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                String filteredValue = filterChain.getRequest().getParameter(param_name);
                //return the original parameter and filtered parameter
                return new String[]{filteredValue, randomValue};
            } finally {
                synchronized (concurrentThreads) {
                    concurrentThreads.decrement();
                }
            }
        }
    }

    private static class DummyFilter implements Filter {
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            chain.doFilter(request, response);
        }

        public void destroy() {
        }
    }
}

package bazagious;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

/**
 * Unit test for AntiSamyFilterTest
 */
public class AntiSamyFilterTest {

    private AntiSamyFilter filter;

    //mocks
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @Before
    public void setUp() throws Exception {
        filter = new AntiSamyFilter();
        filter.init(null);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    public void testValidParamPassesThrough() throws Exception {
        String param = "valid_param";
        String value = "valid_value";
        request.setParameter(param, value);
        filter.doFilter(request, response, filterChain);

        ServletRequest wrappedRequest = filterChain.getRequest();
        assertEquals(wrappedRequest.getParameter(param), value);
    }

    @Test
    public void testInvalidParamIsFiltered() throws Exception {
        String param = "valid_param";
        request.setParameter(param, "<SCRIPT SRC=//ha.ckers.org/.j>");
        filter.doFilter(request, response, filterChain);

        ServletRequest wrappedRequest = filterChain.getRequest();
        assertThat(wrappedRequest.getParameter(param), not(containsString("SCRIPT")));
    }

    @Test
    public void testValidContentNearInvalidContentSurvives() throws Exception {
        String param = "valid_param";
        request.setParameter(param, "<SCRIPT></SCRIPT> should pass through");
        filter.doFilter(request, response, filterChain);

        ServletRequest wrappedRequest = filterChain.getRequest();
        assertThat(wrappedRequest.getParameter(param), containsString("pass through"));
        assertThat(wrappedRequest.getParameter(param), not(containsString("SCRIPT")));
    }

    @Test
    public void testNonHttpRequestPassesThrough() throws Exception {
        String param = "valid_param";
        String value = "should pass through";
        ServletRequest servletRequest = new ServletRequestWrapper(request);
        request.setParameter(param, value);
        filter.doFilter(servletRequest, response, filterChain);

        ServletRequest originalRequest = filterChain.getRequest();
        assertThat(originalRequest.getParameter(param), equalTo(value));
        assertThat(originalRequest, is(originalRequest));
    }
}

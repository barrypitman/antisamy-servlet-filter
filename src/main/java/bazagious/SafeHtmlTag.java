package bazagious;

import org.apache.log4j.Logger;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSP tag that renders AntiSamy-filtered HTML, useful for displaying HTML markup in a safe way.
 *
 * @author barry pitman
 * @since 2011/04/14 1:26 PM
 */
public class SafeHtmlTag extends TagSupport {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SafeHtmlTag.class);
    private static final AntiSamy DEFAULT_ANTI_SAMY = new AntiSamy(loadPolicy("antisamy-default.xml"));
    private static final Map<String, Policy> POLICY_CACHE = new ConcurrentHashMap<String, Policy>();

    private String text;
    private String policyFile;

    public void setText(String text) {
        this.text = text;
    }

    public void setPolicyFile(String policyFile) {
        this.policyFile = policyFile;
    }

    public int doStartTag() throws JspException {
        try {
            CleanResults scan = getAntiSamy().scan(text);
            if (scan.getNumberOfErrors() > 0) {
                LOG.warn("antisamy encountered problem with input: " + scan.getErrorMessages());
            }
            pageContext.getOut().print(scan.getCleanHTML());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    protected AntiSamy getAntiSamy() {
        if (policyFile == null || "".equals(policyFile)) {
            return DEFAULT_ANTI_SAMY;
        }
        return new AntiSamy(loadPolicy(policyFile));
    }

    private static Policy loadPolicy(String name) {
        if (POLICY_CACHE.containsKey(name)) {
            return POLICY_CACHE.get(name);
        }
        try {
            LOG.debug("Loading policy file '" + name + "' from classpath");
            URL url = SafeHtmlTag.class.getClassLoader().getResource(name);
            if (url == null || url.getFile() == null) {
                throw new FileNotFoundException("classpath file '" + name + "' was not found!");
            }
            Policy loadedPolicy = Policy.getInstance(url.getFile());
            POLICY_CACHE.put(name, loadedPolicy);
            return loadedPolicy;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}

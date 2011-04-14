<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="safeHtml" uri="http://www.bazageous.com/safeHtml" %>
<html>
<head>
    <title>OWASP antisamy demo</title>
    <style type="text/css">
        .output {
            color: blue;
        }
    </style>
</head>
<body>

<h3>Methods of preventing XSS attacks</h3>

<ol>
    <li>
        <h5>Filtering out dangerous content from request parameters on user submit using anti-samy:</h5>
        This happens before the content is processed/saved, using a servlet filter to clean
        the request parameters. An XML policy file defines what content is allowed and what isn't.
        See AntiSamyFilter and web.xml.
    </li>
    <li>
        <h5>Escaping user-supplied content with &lt;c:out&gt;:</h5>
        All user-supplied content is escaped. This method is safer, but will not work if you want to allow
        html formatting, for instance using a rich text editor like
        <a href="http://tinymce.moxiecode.com/index.php">tinymce</a>
    </li>
    <li>
        <h5>Using anti-samy during rendering using a custom JSP tag:</h5>
        Escape only content that is deemed dangerous by the anti-samy policy. This approach
        makes it easy to use different policy files for different use-cases. See the JSP source code for an example.
        We can configure the policy to allow certain types of HTML formatting.
    </li>
</ol>

<form method="post" action="./index.jsp">
    <label for="sampleTextArea">
        Try some XSS <a href="http://ha.ckers.org/xss.html" target="_blank">attacks</a> or HTML formatting, e.g.
        <c:out value="<strong>bold</strong><em>italic</em>"/>:
    </label><br/>
    <textarea rows="5" cols="50" name="sampleTextInput" id="sampleTextArea"></textarea><br/>

    <%--todo--%>
    <%--<input type="file" name="fileUpload">--%>

    <input type="submit" name="submit" value="Submit">
</form>

<c:if test="${not empty param.submit}">
    <c:set var="originalInput" value="${pageContext.request.originalRequest.parameterMap['sampleTextInput'][0]}"/>
    <h3>Redisplaying submitted contents to user</h3>
    <ol>
        <li>
            <h5>Output using method 1: '<span class="output">${param.sampleTextInput}</span>'</h5>
            The dangerous content in the request parameter is filtered out by the AntiSamyFilter,
            and is safe to re-display as is using <c:out value="\${}"/>
        </li>
        <li>
            <h5>Output using method 2:
                '<span class="output">
                        <c:out value="${originalInput}"/>
                </span>'
            </h5>
            The dangerous content is escaped using &lt;c:out&gt, rendering it without modification. Note that this
            won't allow HTML formatting, using e.g. a rich text editor, as the formatting will be escaped too.
        </li>
        <li>
            <h5>
                Using <c:out value="<safeHtml:out/>"/> tag:
                '<span class="output"><safeHtml:out text="${originalInput}"/></span>'
            </h5>
        </li>
        <li>
            <h5>
                Using <c:out value="<safeHtml:out/>"/> tag with antisamy-anythinggoes.xml:
                <span class="output">
                    '<safeHtml:out text="${originalInput}" policyFile="antisamy-anythinggoes.xml"/>'
                </span>
            </h5>
        </li>
    </ol>

</c:if>

</body>
</html>
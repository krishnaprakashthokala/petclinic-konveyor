<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<br/>
<br/>
<div class="container">  
    <div class="row">
        <div class="col-12 text-center"><label>Session ID: " </label><%=request.getSession().getId()%><label>"</label></div>
    </div>
    <div class="row">
        <div class="col-12 text-center"><label></label></div>
    </div>
    <div class="row">
        <div class="col-12 text-center"><img src="<spring:url value="/resources/images/spring-pivotal-logo.png" htmlEscape="true" />"
                                             alt="Sponsored by Pivotal"/></div>
    </div>  
</div>

<%@ page import="org.jahia.data.templates.JahiaTemplatesPackage" %>
<%@ page import="org.jahia.services.templates.JahiaTemplateManagerService" %>
<%@ page import="org.jahia.services.usermanager.mongo.JahiaMongoConfig" %>
<%@ page import="org.jahia.services.usermanager.mongo.JahiaMongoConfigFactory" %>
<%@ page import="org.jahia.registries.ServicesRegistry" %>
<%@ page import="org.jahia.services.render.Resource" %>
<%@ page import="org.jahia.utils.i18n.ResourceBundles" %>
<%@ page import="org.osgi.service.cm.Configuration" %>
<%@ page import="org.osgi.service.cm.ConfigurationAdmin" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocalizationContext" %>
<%@ page import="java.util.Dictionary" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.util.TreeMap" %>
<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="providerKey" type="java.lang.String"--%>

<%
    final Resource currentResource = (Resource) pageContext.findAttribute("currentResource");
    final JahiaTemplateManagerService jahiaTemplateManagerService = ServicesRegistry.getInstance()
            .getJahiaTemplateManagerService();
    final JahiaTemplatesPackage mongo = jahiaTemplateManagerService.getTemplatePackageById("mongo");
    final ResourceBundle rb = ResourceBundles.get(mongo, currentResource.getLocale());
    final LocalizationContext ctx = new LocalizationContext(rb);
    pageContext.setAttribute("bundle", ctx);
    final String providerKey = (String) pageContext.findAttribute("providerKey");
    final Map<String, String> previousProperties = (Map<String, String>) pageContext.findAttribute("mongoProperties");
    final List<String> defaultProperties = (List<String>) mongo.getContext().getBean("defaultProperties");
    pageContext.setAttribute("defaultProperties", defaultProperties);

    final JahiaMongoConfigFactory jahiaMongoConfigFactory = (JahiaMongoConfigFactory) mongo.getContext()
            .getBean("JahiaMongoConfigFactory");
    final ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) mongo.getContext()
            .getBean("configurationAdmin");

    Dictionary<String, Object> properties = null;
    if (providerKey != null) {
        String pid = jahiaMongoConfigFactory.getConfigPID(providerKey);
        Configuration configuration = configurationAdmin.getConfiguration(pid);
        properties = configuration.getProperties();
    }

    final Map<String, String> res = new LinkedHashMap<String, String>();
    for (final String key : defaultProperties) {
        if (previousProperties != null && previousProperties.containsKey(key)) {
            res.put(key, previousProperties.get(key));
        } else if (properties != null && properties.get(key) != null) {
            res.put(key, (String) properties.get(key));
        } else {
            res.put(key, "");
        }
    }

    final Map<String, String> sorted = new TreeMap<String, String>();
    if (previousProperties != null) {
        for (final String key : previousProperties.keySet()) {
            if (!defaultProperties.contains(key)
                    && !JahiaMongoConfig.MONGO_PROVIDER_KEY.equals(key)) {
                sorted.put(key, previousProperties.get(key));
            }
        }
    }

    if (properties != null) {
        final Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            if (!sorted.containsKey(key) && !defaultProperties.contains(key)
                    && !key.startsWith("service.")
                    && !key.startsWith("felix.") && !JahiaMongoConfig.MONGO_PROVIDER_KEY.equals(key)) {
                sorted.put(key, (String) properties.get(key));
            }
        }
    }
    res.putAll(sorted);
    pageContext.setAttribute("mongoProperties", res);

%>
<jcr:jqom
        statement="select * from [jnt:virtualsite] as site where ischildnode(site,'/sites') and localname(site) <> 'systemsite'"
        var="sites"/>
<datalist id="sites">
    <c:forEach items="${sites.nodes}" var="site">
        <option value="${site.name}"/>
    </c:forEach>
</datalist>
<template:addResources type="javascript" resources="jquery.min.js,jquery.form.min.js"/>
<template:addResources>
    <script type="text/javascript">
        function addField() {
            $("<div class=\"row-fluid\">" +
                    "<div class=\"span4\"><input type=\"text\" name=\"propKey\" value=\"\" required class=\"span12\"/></div>" +
                    "<div class=\"span7\"><input type=\"text\" name=\"propValue\" value=\"\" class=\"span12\"/></div>" +
                    "<div class=\"span1\"><a class=\"btn\" onclick=\"$(this).parent().parent().remove()\"><i class=\"icon icon-minus\"></i></a></div>" +
                    "</div>").insertBefore($("#addField${currentNode.identifier}"));
        }
    </script>
</template:addResources>

<fieldset class="box-1">
    <c:if test="${empty providerKey}">
        <label>
            <div class="row-fluid">
                <div class="span4">
                    <fmt:message bundle="${bundle}" key="mongo.provider.name"/>
                </div>
                <div class="span8">
                    <input type="text" name="configName" value="${configName}"/>
                </div>
            </div>
        </label>
    </c:if>
    <c:forEach var="property" items="${mongoProperties}">
        <label>
            <div class="row-fluid">
                <div class="span4">
                    <fmt:message bundle="${bundle}" key="mongo.provider.${property.key}" var="label"/>
                    <c:if test="${fn:startsWith(label,'???')}">
                        ${property.key}
                    </c:if>
                    <c:if test="${not fn:startsWith(label,'???')}">
                        ${label} ( ${property.key} )
                    </c:if>
                </div>
                <div class="span7">
                    <input type="text" name="propValue.${property.key}"
                           value="${property.value}" ${property.key eq 'target.site'? 'list="sites"' : 'class="span12"'}/>
                </div>
                <c:if test="${not functions:contains(defaultProperties, property.key)}">
                    <div class="span1">
                        <a class="btn" onclick="$(this).parent().parent().remove()"><i class="icon icon-minus"></i></a>
                    </div>
                </c:if>
            </div>
        </label>
    </c:forEach>
    <a id="addField${currentNode.identifier}" class="btn" onclick="addField()"><i class="icon icon-plus"></i></a>
</fieldset>

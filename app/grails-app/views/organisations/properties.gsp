<%@ page import="com.k_int.kbplus.Org; com.k_int.properties.PropertyDefinition" %>

<!doctype html>
<html>
  <head>
    <meta name="layout" content="semanticUI">
    <g:set var="entityName" value="${message(code: 'org.label', default: 'Org')}" />
    <title><g:message code="default.show.label" args="[entityName]" /></title>
    <r:require module="annotations" />
    <g:javascript src="properties.js"/>
  </head>
  <body>

    <div>
      <h1>${orgInstance.name}</h1>
      <g:render template="nav" contextPath="." />
    </div>

    <div>
		<g:if test="${flash.message}">
			<bootstrap:alert class="alert-info">${flash.message}</bootstrap:alert>
		</g:if>

        <g:each in="${authorizedOrgs}" var="authOrg">
            <h6>@ ${authOrg.name}</h6>

            <div id="custom_props_div_${authOrg.shortcode}" class="span12">
                <g:render template="/templates/properties/private" model="${[
                        prop_desc: PropertyDefinition.ORG_PROP,
                        ownobj: orgInstance,
                        custom_props_div: "custom_props_div_${authOrg.shortcode}",
                        tenant: authOrg]}"/>

                <r:script language="JavaScript">
                    $(document).ready(function(){
                        initPropertiesScript("<g:createLink controller='ajax' action='lookup'/>", "#custom_props_div_${authOrg.shortcode}", ${authOrg.id});
                    });
                </r:script>
            </div>
		</g:each>
    </div>

  </body>
</html>

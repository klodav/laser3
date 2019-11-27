<%@ page import="com.k_int.kbplus.Package" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <title>${message(code:'laser', default:'LAS:eR')} : ${message(code:'package.show.nav.notes')}</title>
</head>
<body>

    <semui:breadcrumbs>
        <semui:crumb controller="package" action="index" text="${message(code:'package.show.all')}" />
        <semui:crumb text="${packageInstance.name}" id="${packageInstance.id}" class="active"/>
    </semui:breadcrumbs>
    <semui:controlButtons>
        <semui:exportDropdown>
            <semui:exportDropdownItem>
                <g:link class="item" action="show" params="${params+[format:'json']}">JSON</g:link>
            </semui:exportDropdownItem>
            <semui:exportDropdownItem>
                <g:link class="item" action="show" params="${params+[format:'xml']}">XML</g:link>
            </semui:exportDropdownItem>

            <g:each in="${transforms}" var="transkey,transval">
                <semui:exportDropdownItem>
                    <g:link class="item" action="show" id="${params.id}" params="${[format:'xml', transformId:transkey, mode:params.mode]}"> ${transval.name}</g:link>
                </semui:exportDropdownItem>
            </g:each>
        </semui:exportDropdown>
        <g:render template="actions" />
    </semui:controlButtons>

    <semui:modeSwitch controller="package" action="show" params="${params}"/>

<semui:messages data="${flash}" />


    <h1 class="ui left floated aligned icon header la-clear-before"><semui:headerIcon />

        ${packageInstance?.name}
    </h1>

    <g:render template="nav"/>

    <g:render template="/templates/notes/table" model="${[instance: packageInstance, redirect: 'notes']}"/>

</body>
</html>

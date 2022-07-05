<%@ page import="de.laser.storage.RDStore;de.laser.storage.RDConstants;de.laser.OrgRole;de.laser.RefdataCategory;de.laser.RefdataValue;de.laser.properties.PropertyDefinition;de.laser.Subscription;de.laser.finance.CostItem" %>

<!doctype html>
<html>
<head>
    <meta name="layout" content="laser">
    <g:set var="entityName" value="${message(code: 'org.label')}"/>
    <title>${message(code: 'laser')} : ${message(code: 'menu.my.consortiaSubscriptions')}</title>
</head>

<body>

<semui:debugInfo>
    <laser:render template="/templates/debug/benchMark" model="[debug: benchMark]" />
</semui:debugInfo>

<semui:breadcrumbs>
    <semui:crumb message="menu.my.consortiaSubscriptions" class="active"/>
</semui:breadcrumbs>

<semui:controlButtons>
    <semui:exportDropdown>
        <semui:exportDropdownItem>
            <g:if test="${filterSet || defaultSet}">
                <g:link class="item js-open-confirm-modal"
                        data-confirm-tokenMsg = "${message(code: 'confirmation.content.exportPartial')}"
                        data-confirm-term-how="ok" controller="myInstitution" action="manageConsortiaSubscriptions"
                        params="${params+[exportXLS:true]}">
                    ${message(code:'default.button.exports.xls')}
                </g:link>
            </g:if>
            <g:else>
                <g:link class="item" controller="myInstitution" action="manageConsortiaSubscriptions" params="${params+[exportXLS:true]}">${message(code:'default.button.exports.xls')}</g:link>
            </g:else>
        </semui:exportDropdownItem>
        <semui:exportDropdownItem>
            <g:if test="${filterSet || defaultSet}">
                <g:link class="item js-open-confirm-modal"
                        data-confirm-tokenMsg = "${message(code: 'confirmation.content.exportPartial')}"
                        data-confirm-term-how="ok" controller="myInstitution" action="manageConsortiaSubscriptions"
                        params="${params+[format:'csv']}">
                    ${message(code:'default.button.exports.csv')}
                </g:link>
            </g:if>
            <g:else>
                <g:link class="item" controller="myInstitution" action="manageConsortiaSubscriptions" params="${params+[format:'csv']}">${message(code:'default.button.exports.csv')}</g:link>
            </g:else>
        </semui:exportDropdownItem>

    </semui:exportDropdown>
    <semui:actionsDropdown>
        <semui:actionsDropdownItem data-semui="modal" href="#copyEmailaddresses_ajaxModal" message="menu.institutions.copy_emailaddresses.button"/>
    </semui:actionsDropdown>
</semui:controlButtons>

<semui:headerWithIcon message="menu.my.consortiaSubscriptions" floated="true">
    <semui:totalNumber total="${totalCount}"/>
</semui:headerWithIcon>

<semui:messages data="${flash}"/>

<laser:render template="/templates/subscription/consortiaSubscriptionFilter"/>

<laser:render template="/templates/subscription/consortiaSubscriptionTable"/>

<laser:render template="/templates/copyEmailaddresses" model="[orgList: totalMembers]"/>

</body>
</html>

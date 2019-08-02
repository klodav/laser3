<%@ page import="com.k_int.kbplus.Person; de.laser.helper.RDStore; com.k_int.properties.PropertyDefinition; com.k_int.kbplus.RefdataValue; de.laser.AuditConfig" %>
<laser:serviceInjection/>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'subscription.details.subscriberManagement.label')}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="myInstitution" action="dashboard" text="${contextService.getOrg()?.getDesignation()}"/>
    <semui:crumb controller="myInstitution" action="currentSubscriptions"
                 text="${message(code: 'myinst.currentSubscriptions.label')}"/>
    <semui:crumb controller="subscription" action="show" id="${subscriptionInstance.id}"
                 text="${subscriptionInstance.name}"/>

    <semui:crumb class="active" text="${message(code: 'subscription.details.subscriberManagement.label')}"/>

</semui:breadcrumbs>

<h1 class="ui left aligned icon header">
    ${message(code: 'subscription.details.subscriberManagement.label')}
</h1>

<g:render template="navSubscriberManagement"/>

<h3 class="ui left aligned icon header"><semui:headerIcon/>
${message(code: 'subscription.propertiesMembers.header')}
</h3>

<semui:messages data="${flash}"/>

<h4>
    <g:message code="subscription.linkPackagesMembers.subscription" args="${args.superOrgType}"/>:
    <g:link controller="subscription" action="show" id="${parentSub.id}">${parentSub.name}</g:link><br><br>

</h4>


<semui:filter>
    <h4>${message(code: 'subscription.propertiesMembers.onlyPropOfParentSubscription', args: [parentSub.name])}</h4>
    <g:form action="propertiesMembers" method="post" class="ui form" id="${params.id}">
        <g:render template="../templates/properties/genericFilter" model="[propList: propList, hideFilterProp: true]"/>

        <div class="field la-field-right-aligned">
            <a href="${request.forwardURI}"
               class="ui reset primary button">${message(code: 'default.button.reset.label')}</a>
            <input type="submit" value="${message(code: 'default.button.filter.label')}" class="ui secondary button"/>
        </div>
    </g:form>
</semui:filter>




<g:if test="${filteredSubChilds && filterPropDef}">

%{--<div class="ui segment">

    <b>${message(code: 'subscription.propertiesMembers.propertySelected')}: ${filterPropDef?.getI10n('name')}</b>
    <br>${message(code: 'propertyDefinition.type.label')}: ${PropertyDefinition.getLocalizedValue(filterPropDef?.type)}
    <g:if test="${filterPropDef?.type == 'class com.k_int.kbplus.RefdataValue'}">
        <g:set var="refdataValues" value="${[]}"/>
        <g:each in="${com.k_int.kbplus.RefdataCategory.getAllRefdataValues(filterPropDef.refdataCategory)}"
                var="refdataValue">
            <g:set var="refdataValues"
                   value="${refdataValues + refdataValue?.getI10n('value')}"/>
        </g:each>
        <br>
        (${refdataValues.join('/')})
    </g:if>

</div>--}%

    <div class="ui icon info message">
        <i class="info icon"></i>

        <div class="content">
            <div class="header">Info</div>

            <div class="ui bulleted list">
                <div class="item">
                    <g:message code="subscription.propertiesMembers.info2"/>
                </div>

                <div class="item">
                    <g:message code="subscription.propertiesMembers.info3" args="${args.superOrgType}"/>
                </div>

                <div class="item">
                    <g:message code="subscription.propertiesMembers.info4"/>
                </div>
            </div>
        </div>
    </div>

    <div class="ui segment">
        <g:form action="processPropertiesMembers" method="post" class="ui form">
            <g:hiddenField name="id" value="${params.id}"/>

            <div class="field required">
                <h4>${message(code: 'subscription.propertiesMembers.info')}</h4>

                <div class="inline field">
                    <label>${message(code: 'subscription.propertiesMembers.propertySelected')}:</label>

                    <b>${filterPropDef?.getI10n('name')}
                        <g:if test="${filterPropDef?.tenant != null}">
                            <i class="shield alternate icon"></i>
                        </g:if>
                    </b>

                </div>
                <g:hiddenField name="filterPropDef" value="${filterPropDef}"/>


                ${message(code: 'propertyDefinition.type.label')}: ${PropertyDefinition.getLocalizedValue(filterPropDef?.type)}
                <g:if test="${filterPropDef?.type == 'class com.k_int.kbplus.RefdataValue'}">
                    <g:set var="refdataValues" value="${[]}"/>
                    <g:each in="${com.k_int.kbplus.RefdataCategory.getAllRefdataValues(filterPropDef.refdataCategory)}"
                            var="refdataValue">
                        <g:set var="refdataValues"
                               value="${refdataValues + refdataValue?.getI10n('value')}"/>
                    </g:each>

                    (${refdataValues.join('/')})
                </g:if>

            </div>

            <div class="field required">
                <label for="filterPropValue">${message(code: 'subscription.property.value')}</label>
                <g:if test="${filterPropDef?.type == 'class com.k_int.kbplus.RefdataValue'}">
                    <g:select class="ui search dropdown"
                              optionKey="id" optionValue="${{ it.getI10n('value') }}"
                              from="${com.k_int.kbplus.RefdataCategory.getAllRefdataValues(filterPropDef.refdataCategory)}"
                              name="filterPropValue" value=""
                              required=""
                              noSelection='["": "${message(code: 'default.select.choose.label', default: 'Please Choose...')}"]'/>
                </g:if>
                <g:else>
                    <input id="filterPropValue" type="text" name="filterPropValue"
                           placeholder="${message(code: 'license.search.property.ph', default: 'property value')}"/>
                </g:else>
            </div>

            <button class="ui button" type="submit">${message(code: 'default.button.save_changes')}</button>
        </g:form>
    </div>

    <g:set var="editableOld" value="${editable}"/>


    <div class="ui segment">
        <h4>${message(code: 'subscription.propertiesMembers.deletePropertyInfo')}</h4>

            <g:link class="ui button js-open-confirm-modal"
                    data-confirm-term-content = "${message(code: 'subscription.propertiesMembers.deleteProperty.button.confirm')}"
                    data-confirm-term-how="ok" action="processDeletePropertiesMembers" id="${params.id}" params="[filterPropDef: filterPropDef]">${message(code: 'subscription.propertiesMembers.deleteProperty.button', args: [filterPropDef?.getI10n('name')])}</g:link>

    </div>

    <div class="divider"></div>

    <div class="ui segment">
        <h3><g:message code="subscription.propertiesMembers.subscription" args="${args.superOrgType}"/></h3>
        <table class="ui celled la-table table">
            <thead>
            <tr>
                <th>${message(code: 'subscription')}</th>
                <th>${message(code: 'default.startDate.label')}</th>
                <th>${message(code: 'default.endDate.label')}</th>
                <th>${message(code: 'subscription.details.status')}</th>
                <th>${message(code: 'subscription.propertiesMembers.propertySelected')}: ${filterPropDef?.getI10n('name')}</th>
                <th></th>
            </tr>
            </thead>
            <tbody>

            <td>${parentSub.name}</td>

            <td>
                <g:formatDate formatName="default.date.format.notime" date="${parentSub?.startDate}"/>
                <semui:auditButton auditable="[parentSub, 'startDate']"/>
            </td>
            <td>
                <g:formatDate formatName="default.date.format.notime" date="${parentSub?.endDate}"/>
                <semui:auditButton auditable="[parentSub, 'endDate']"/>
            </td>
            <td>
                ${parentSub.status.getI10n('value')}
                <semui:auditButton auditable="[parentSub, 'status']"/>
            </td>
            <td>

                <div class="ui middle aligned selection list">

                    <g:if test="${filterPropDef?.tenant == null}">
                        <div class="item">

                            <div class="right floated content">
                                <semui:totalNumber total="${parentSub.customProperties?.size()}"/>
                            </div>

                            <g:set var="customProperty"
                                   value="${parentSub.customProperties.find { it.type == filterPropDef }}"/>
                            <g:if test="${customProperty}">
                                <div class="header">${message(code: 'subscription.propertiesMembers.CustomProperty')}: ${filterPropDef?.getI10n('name')}</div>

                                <div class="content">

                                    %{-- <g:set var="editable" value="${!(AuditConfig.getConfig(customProperty))}"
                                            scope="request"/>--}%

                                    <g:if test="${customProperty.type.type == Integer.toString()}">
                                        <semui:xEditable owner="${customProperty}" type="number"
                                                         field="intValue"/>
                                    </g:if>
                                    <g:elseif test="${customProperty.type.type == String.toString()}">
                                        <semui:xEditable owner="${customProperty}" type="text"
                                                         field="stringValue"/>
                                    </g:elseif>
                                    <g:elseif test="${customProperty.type.type == BigDecimal.toString()}">
                                        <semui:xEditable owner="${customProperty}" type="text"
                                                         field="decValue"/>
                                    </g:elseif>
                                    <g:elseif test="${customProperty.type.type == Date.toString()}">
                                        <semui:xEditable owner="${customProperty}" type="date"
                                                         field="dateValue"/>
                                    </g:elseif>
                                    <g:elseif test="${customProperty.type.type == URL.toString()}">
                                        <semui:xEditable owner="${customProperty}" type="url" field="urlValue"

                                                         class="la-overflow la-ellipsis"/>
                                        <g:if test="${customProperty.value}">
                                            <semui:linkIcon href="${customProperty.value}"/>
                                        </g:if>
                                    </g:elseif>
                                    <g:elseif test="${customProperty.type.type == RefdataValue.toString()}">
                                        <semui:xEditableRefData owner="${customProperty}" type="text"
                                                                field="refValue"
                                                                config="${customProperty.type.refdataCategory}"/>
                                    </g:elseif>

                                    <%
                                        if (AuditConfig.getConfig(customProperty)) {
                                            if (parentSub.isSlaved) {
                                                println '&nbsp; <span class="la-popup-tooltip la-delay" data-content="Wert wird automatisch geerbt." data-position="top right"><i class="icon thumbtack blue"></i></span>'
                                            } else {
                                                println '&nbsp; <span class="la-popup-tooltip la-delay" data-content="Wert wird geerbt." data-position="top right"><i class="icon thumbtack grey"></i></span>'
                                            }
                                        }
                                    %>

                                </div>
                            </g:if><g:else>
                            <div class="content">
                                ${message(code: 'subscription.propertiesMembers.noCustomProperty')}
                            </div>
                        </g:else>

                        </div>
                    </g:if>

                    <g:if test="${filterPropDef?.tenant != null}">

                        <div class="item">

                            <div class="right floated content">
                                <semui:totalNumber total="${parentSub.privateProperties?.size()}"/>
                            </div>

                            <g:set var="privateProperty"
                                   value="${parentSub.privateProperties.find { it.type == filterPropDef }}"/>
                            <g:if test="${privateProperty}">
                                <div class="header">${message(code: 'subscription.propertiesMembers.PrivateProperty')} ${contextService.org}: ${filterPropDef?.getI10n('name')}</div>

                                <div class="content">

                                    <g:set var="editable" value="${!(AuditConfig.getConfig(privateProperty))}"
                                           scope="request"/>

                                    <g:if test="${privateProperty.type.type == Integer.toString()}">
                                        <semui:xEditable owner="${privateProperty}" type="number"
                                                         field="intValue"/>
                                    </g:if>
                                    <g:elseif test="${privateProperty.type.type == String.toString()}">
                                        <semui:xEditable owner="${privateProperty}" type="text"
                                                         field="stringValue"/>
                                    </g:elseif>
                                    <g:elseif test="${privateProperty.type.type == BigDecimal.toString()}">
                                        <semui:xEditable owner="${privateProperty}" type="text"
                                                         field="decValue"/>
                                    </g:elseif>
                                    <g:elseif test="${privateProperty.type.type == Date.toString()}">
                                        <semui:xEditable owner="${privateProperty}" type="date"
                                                         field="dateValue"/>
                                    </g:elseif>
                                    <g:elseif test="${privateProperty.type.type == URL.toString()}">
                                        <semui:xEditable owner="${privateProperty}" type="url" field="urlValue"

                                                         class="la-overflow la-ellipsis"/>
                                        <g:if test="${privateProperty.value}">
                                            <semui:linkIcon href="${privateProperty.value}"/>
                                        </g:if>
                                    </g:elseif>
                                    <g:elseif test="${privateProperty.type.type == RefdataValue.toString()}">
                                        <semui:xEditableRefData owner="${privateProperty}" type="text"
                                                                field="refValue"
                                                                config="${privateProperty.type.refdataCategory}"/>
                                    </g:elseif>

                                    <%
                                        if (AuditConfig.getConfig(privateProperty)) {
                                            if (parentSub.isSlaved) {
                                                println '&nbsp; <span class="la-popup-tooltip la-delay" data-content="Wert wird automatisch geerbt." data-position="top right"><i class="icon thumbtack blue"></i></span>'
                                            } else {
                                                println '&nbsp; <span class="la-popup-tooltip la-delay" data-content="Wert wird geerbt." data-position="top right"><i class="icon thumbtack grey"></i></span>'
                                            }
                                        }
                                    %>

                                </div>
                            </g:if><g:else>
                            <div class="content">
                                ${message(code: 'subscription.propertiesMembers.noPrivateProperty')}
                            </div>
                        </g:else>

                        </div>
                    </g:if>
                </div>

            </td>

            <td class="x">
                <g:link controller="subscription" action="show" id="${parentSub.id}"
                        class="ui icon button"><i
                        class="write icon"></i></g:link>
            </td>
            </tr>
            </tbody>
        </table>
    </div>

    <div class="ui segment">
        <h3>${message(code: 'subscription.propertiesMembers.subscriber')} <semui:totalNumber
                total="${filteredSubChilds?.size()}"/></h3>
        <table class="ui celled la-table table">
            <thead>
            <tr>
                <th>${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'default.sortname.label')}</th>
                <th>${message(code: 'subscriptionDetails.members.members')}</th>
                <th>${message(code: 'default.startDate.label')}</th>
                <th>${message(code: 'default.endDate.label')}</th>
                <th>${message(code: 'subscription.details.status')}</th>
                <th>${message(code: 'subscription.propertiesMembers.propertySelected')}: ${filterPropDef?.getI10n('name')}</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${filteredSubChilds}" status="i" var="zeile">
                <g:set var="sub" value="${zeile.sub}"/>
                <tr>
                    <td>${i + 1}</td>
                    <g:set var="filteredSubscribers" value="${zeile.orgs}"/>
                    <g:each in="${filteredSubscribers}" var="subscr">
                        <td>${subscr.sortname}</td>
                        <td>
                            <g:link controller="organisation" action="show"
                                    id="${subscr.id}">${subscr}</g:link>

                            <g:if test="${sub.isSlaved}">
                                <span data-position="top right"
                                      data-tooltip="${message(code: 'license.details.isSlaved.tooltip')}">
                                    <i class="thumbtack blue icon"></i>
                                </span>
                            </g:if>

                        </td>
                    </g:each>
                    <g:if test="${!sub.getAllSubscribers()}">
                        <td></td>
                        <td></td>
                    </g:if>

                    <td>
                        <semui:xEditable owner="${sub}" field="startDate" type="date"
                                         overwriteEditable="${editableOld}"/>
                        <semui:auditButton auditable="[sub, 'startDate']"/>
                    </td>
                    <td><semui:xEditable owner="${sub}" field="endDate" type="date"
                                         overwriteEditable="${editableOld}"/>
                        <semui:auditButton auditable="[sub, 'endDate']"/>
                    </td>
                    <td>
                        ${sub.status.getI10n('value')}
                        <semui:auditButton auditable="[sub, 'status']"/>
                    </td>
                    <td>

                        <div class="ui middle aligned selection list">

                            <g:if test="${filterPropDef?.tenant == null}">
                                <div class="item">

                                    <div class="right floated content">
                                        <semui:totalNumber total="${sub.customProperties?.size()}"/>
                                    </div>

                                    <g:set var="customProperty"
                                           value="${sub.customProperties.find { it.type == filterPropDef }}"/>
                                    <g:if test="${customProperty}">
                                        <div class="header">${message(code: 'subscription.propertiesMembers.CustomProperty')}: ${filterPropDef?.getI10n('name')}</div>

                                        <div class="content">
                                            <g:if test="${customProperty.type.type == Integer.toString()}">
                                                <semui:xEditable owner="${customProperty}" type="number"
                                                                 field="intValue"/>
                                            </g:if>
                                            <g:elseif test="${customProperty.type.type == String.toString()}">
                                                <semui:xEditable owner="${customProperty}" type="text"
                                                                 field="stringValue"/>
                                            </g:elseif>
                                            <g:elseif
                                                    test="${customProperty.type.type == BigDecimal.toString()}">
                                                <semui:xEditable owner="${customProperty}" type="text"
                                                                 field="decValue"/>
                                            </g:elseif>
                                            <g:elseif test="${customProperty.type.type == Date.toString()}">
                                                <semui:xEditable owner="${customProperty}" type="date"
                                                                 field="dateValue"/>
                                            </g:elseif>
                                            <g:elseif test="${customProperty.type.type == URL.toString()}">
                                                <semui:xEditable owner="${customProperty}" type="url"
                                                                 field="urlValue"

                                                                 class="la-overflow la-ellipsis"/>
                                                <g:if test="${customProperty.value}">
                                                    <semui:linkIcon href="${customProperty.value}"/>
                                                </g:if>
                                            </g:elseif>
                                            <g:elseif
                                                    test="${customProperty.type.type == RefdataValue.toString()}">
                                                <semui:xEditableRefData owner="${customProperty}" type="text"
                                                                        field="refValue"
                                                                        config="${customProperty.type.refdataCategory}"/>
                                            </g:elseif>

                                            <%
                                                if (customProperty.hasProperty('instanceOf') && customProperty.instanceOf && AuditConfig.getConfig(customProperty.instanceOf)) {
                                                    if (sub.isSlaved) {
                                                        println '&nbsp; <span class="la-popup-tooltip la-delay" data-content="Wert wird automatisch geerbt." data-position="top right"><i class="icon thumbtack blue"></i></span>'
                                                    } else {
                                                        println '&nbsp; <span class="la-popup-tooltip la-delay" data-content="Wert wird geerbt." data-position="top right"><i class="icon thumbtack grey"></i></span>'
                                                    }
                                                }
                                            %>

                                        </div>
                                    </g:if><g:else>
                                    <div class="content">
                                        ${message(code: 'subscription.propertiesMembers.noCustomProperty')}
                                    </div>
                                </g:else>
                                </div>
                            </g:if>
                            <g:if test="${filterPropDef?.tenant != null}">

                                <div class="item">

                                    <g:set var="privateProperty"
                                           value="${sub.privateProperties.find { it.type == filterPropDef }}"/>

                                    <div class="right floated content">
                                        <semui:totalNumber total="${sub.privateProperties?.size()}"/>
                                    </div>

                                    <g:if test="${privateProperty}">
                                        <div class="header">${message(code: 'subscription.propertiesMembers.PrivateProperty')} ${contextService.org}: ${filterPropDef?.getI10n('name')}</div>

                                        <div class="content">
                                            <g:if test="${privateProperty.type.type == Integer.toString()}">
                                                <semui:xEditable owner="${privateProperty}" type="number"
                                                                 field="intValue"/>
                                            </g:if>
                                            <g:elseif test="${privateProperty.type.type == String.toString()}">
                                                <semui:xEditable owner="${privateProperty}" type="text"
                                                                 field="stringValue"/>
                                            </g:elseif>
                                            <g:elseif
                                                    test="${privateProperty.type.type == BigDecimal.toString()}">
                                                <semui:xEditable owner="${privateProperty}" type="text"
                                                                 field="decValue"/>
                                            </g:elseif>
                                            <g:elseif test="${privateProperty.type.type == Date.toString()}">
                                                <semui:xEditable owner="${privateProperty}" type="date"
                                                                 field="dateValue"/>
                                            </g:elseif>
                                            <g:elseif test="${privateProperty.type.type == URL.toString()}">
                                                <semui:xEditable owner="${privateProperty}" type="url"
                                                                 field="urlValue"
                                                                 class="la-overflow la-ellipsis"/>
                                                <g:if test="${privateProperty.value}">
                                                    <semui:linkIcon href="${privateProperty.value}"/>
                                                </g:if>
                                            </g:elseif>
                                            <g:elseif
                                                    test="${privateProperty.type.type == RefdataValue.toString()}">
                                                <semui:xEditableRefData owner="${privateProperty}" type="text"
                                                                        field="refValue"
                                                                        config="${privateProperty.type.refdataCategory}"/>
                                            </g:elseif>

                                            <%
                                                if (privateProperty.hasProperty('instanceOf') && privateProperty.instanceOf && AuditConfig.getConfig(privateProperty.instanceOf)) {
                                                    if (sub.isSlaved) {
                                                        println '&nbsp; <span class="la-popup-tooltip la-delay" data-content="Wert wird automatisch geerbt." data-position="top right"><i class="icon thumbtack blue"></i></span>'
                                                    } else {
                                                        println '&nbsp; <span class="la-popup-tooltip la-delay" data-content="Wert wird geerbt." data-position="top right"><i class="icon thumbtack grey"></i></span>'
                                                    }
                                                }
                                            %>

                                        </div>
                                    </g:if><g:else>
                                    <div class="content">
                                        ${message(code: 'subscription.propertiesMembers.noPrivateProperty')}
                                    </div>
                                </g:else>

                                </div>
                            </g:if>
                        </div>

                    </td>

                    <td class="x">
                        <g:link controller="subscription" action="show" id="${sub.id}"
                                class="ui icon button"><i
                                class="write icon"></i></g:link>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>

    </div>
</g:if>
<g:else>

    <br><strong>

    <g:if test="${!filteredSubChilds}"><g:message code="subscription.details.nomembers.label"
                                                  default="No members have been added to this license. You must first add members."/></strong>
    </g:if>

    <g:if test="${!filterPropDef}"><g:message
            code="subscription.propertiesMembers.noPropertySeleced"/></strong>
    </g:if>
</g:else>

<div id="magicArea"></div>

</body>
</html>


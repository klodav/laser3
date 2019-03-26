<%@ page import="com.k_int.kbplus.Person" %>
<%@ page import="com.k_int.kbplus.RefdataValue" %>
<% def contextService = grailsApplication.mainContext.getBean("contextService") %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'subscription.details.copyElementsIntoSubscription.label')}</title>
</head>
<body>
    <g:render template="breadcrumb" model="${[params: params]}"/>

    <h1 class="ui left aligned icon header"><semui:headerIcon />
    ${message(code: 'subscription.details.copyElementsIntoSubscription.label')}
    </h1>

    <semui:messages data="${flash}"/>

    <% Map params = [id: params.id];
        if (sourceSubscriptionId) params << [sourceSubscriptionId: sourceSubscriptionId];
        if (targetSubscriptionId) params << [targetSubscriptionId: targetSubscriptionId];
    %>
    <semui:subNav>
        <semui:complexSubNavItem controller="subscriptionDetails" action="copyElementsIntoSubscription" params="${params << [workFlowPart: 1]}" workFlowPart="1" >
            <div class="content" >
                <div class="description">
                    <i class="calendar alternate outline icon"></i>Datum
                    <i class="balance scale icon"></i>Vertrag
                    <i class="university icon"></i>Organisationen
                </div>
            </div>
        </semui:complexSubNavItem>

        <semui:complexSubNavItem controller="subscriptionDetails" action="copyElementsIntoSubscription" params="${params << [workFlowPart: 5]}" workFlowPart="5" >
            <div class="content" >
                <div class="description">
                    <i class="gift icon"></i>Pakete
                    <i class="newspaper icon"></i>Titel
                </div>
            </div>
        </semui:complexSubNavItem>

        <semui:complexSubNavItem controller="subscriptionDetails" action="copyElementsIntoSubscription" params="${params << [workFlowPart: 2]}"  workFlowPart="2">
            <div class="content">
                <div class="description">
                    <i class="file outline icon"></i>Dokumente
                    <i class="sticky note outline icon"></i>Anmerkungen
                    <i class="checked calendar icon"></i>Aufgaben
                </div>
            </div>
        </semui:complexSubNavItem>

        %{--TODO: Teilnehmer ist noch nicht fertig implementiert, wird später als wieder eingeblendet--}%
        %{--<semui:complexSubNavItem controller="subscriptionDetails" action="copyElementsIntoSubscription" params="${params << [workFlowPart: 3]}"  workFlowPart="3">--}%
            %{--<i class="university icon"></i>--}%
            %{--<div class="content">--}%
                %{--<div class="title">--}%
                    %{--Teilnehmer--}%
                %{--</div>--}%
            %{--</div>--}%
        %{--</semui:complexSubNavItem>--}%

        <semui:complexSubNavItem controller="subscriptionDetails" action="copyElementsIntoSubscription" params="${params << [workFlowPart: 4]}"  workFlowPart="4">
            <i class="tags icon"></i>
            <div class="content">
                <div class="title">
                   Merkmale
                </div>
            </div>
        </semui:complexSubNavItem>
    </semui:subNav>

    <br>

    <g:if test="${workFlowPart == '2'}">
        <g:render template="copyDocsAndTasks" />
    </g:if>
    <g:elseif test="${workFlowPart == '3'}">
        <g:render template="copySubscriber" />
    </g:elseif>
    <g:elseif test="${workFlowPart == '4'}">
        <g:render template="copyProperties" />
    </g:elseif>
    <g:elseif test="${workFlowPart == '5'}">
        %{--<g:render template="copyPackagesAndIEs" model="${[source_validSubChilds: source_validSubChilds, target_validSubChilds: target_validSubChilds]}"/>--}%
        <g:render template="copyPackagesAndIEs" />
    </g:elseif>
    %{--workFlowPart == '1'--}%
    <g:else>
        %{--<g:render template="copyElements" model="${[source_validSubChilds: source_validSubChilds, target_validSubChilds: target_validSubChilds]}"/>--}%
        <g:render template="copyElements" />
    </g:else>
</body>
</html>

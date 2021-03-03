<%@ page import="de.laser.IdentifierNamespace; de.laser.Identifier; de.laser.helper.RDStore; de.laser.Subscription; de.laser.properties.PropertyDefinition; de.laser.properties.SubscriptionProperty; de.laser.reporting.OrganisationConfig;de.laser.reporting.SubscriptionConfig;" %>

<h3 class="ui header">3. Details</h3>

<div class="ui message success">
    <p>${label}</p>
</div>

<div class="ui segment">
    <table class="ui table la-table compact">
        <thead>
        <tr>
            <th></th>
            <th>Lizenz</th>
            <g:if test="${query == 'subscription-property-assignment'}">
                <th>Merkmalswert</th>
            </g:if>
            <g:elseif test="${query == 'subscription-identifier-assignment'}">
                <th>Identifikator</th>
            </g:elseif>
            <g:else>
                <th>Teilnehmer</th>
            </g:else>
            <th>Startdatum</th>
            <th>Enddatum</th>
        </tr>
        </thead>
        <tbody>
            <g:each in="${list}" var="sub" status="i">
                <tr>
                    <td>${i + 1}.</td>
                    <td>
                        <g:link controller="subscription" action="show" id="${sub.id}" target="_blank">${sub.name}</g:link>
                    </td>

                    <g:if test="${query == 'subscription-property-assignment'}">
                        <td>
                            <%
                                SubscriptionProperty sp = SubscriptionProperty.findByOwnerAndType(sub, PropertyDefinition.get(id))
                                if (sp) {
                                    if (sp.getType().isRefdataValueType()) {
                                        println sp.getRefValue()?.getI10n('value')
                                    } else {
                                        println sp.getValue()
                                    }
                                }
                            %>
                        </td>
                    </g:if>
                    <g:elseif test="${query == 'subscription-identifier-assignment'}">
                        <td>
                            <%
                                List<Identifier> identList = Identifier.findAllBySubAndNs(sub, IdentifierNamespace.get(id))
                                println identList.collect{ it.value ?: null }.findAll().join(' ,<br/>') // removing empty and null values
                            %>
                        </td>
                    </g:elseif>
                    <g:else>
                        <td>
                            <%
                                int members = Subscription.executeQuery('select count(s) from Subscription s join s.orgRelations oo where s.instanceOf = :parent and oo.roleType in :subscriberRoleTypes',
                                        [parent: sub, subscriberRoleTypes: [RDStore.OR_SUBSCRIBER, RDStore.OR_SUBSCRIBER_CONS, RDStore.OR_SUBSCRIBER_CONS_HIDDEN]]
                                )[0]
                                println members
                            %>
                        </td>
                    </g:else>

                    <td>
                        <g:formatDate format="${message(code:'default.date.format.notime')}" date="${sub.startDate}" />
                    </td>
                    <td>
                        <g:formatDate format="${message(code:'default.date.format.notime')}" date="${sub.endDate}" />
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>


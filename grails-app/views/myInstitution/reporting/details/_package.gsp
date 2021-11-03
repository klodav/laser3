<%@ page import="de.laser.reporting.report.myInstitution.base.BaseDetails;" %>
<laser:serviceInjection />

<g:render template="/myInstitution/reporting/details/top" />

<div class="ui segment">
    <table class="ui table la-table compact">
        <thead>
        <tr>
            <th></th>
            <th>${message(code:'package.label')}</th>
            <th>${message(code:'package.nominalPlatform')}</th>
            <th>${message(code:'package.lastUpdated.label')}</th>
        </tr>
        </thead>
        <tbody>
            <g:each in="${list}" var="pkg" status="i">
                <tr>
                    <td>${i + 1}.</td>
                    <td>
                        <g:link controller="package" action="show" id="${pkg.id}" target="_blank">${pkg.name}</g:link>
                    </td>
                    <td>
                        <g:if test="${pkg.nominalPlatform}">
                            <g:link controller="platform" action="show" id="${pkg.nominalPlatform.id}" target="_blank">${pkg.nominalPlatform.name}</g:link>
                        </g:if>
                    </td>
                    <td>
                        <g:formatDate format="${message(code:'default.date.format.notime')}" date="${pkg._getCalculatedLastUpdated()}" />
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>

<g:render template="/myInstitution/reporting/export/detailsModal" model="[modalID: 'detailsExportModal', token: token]" />

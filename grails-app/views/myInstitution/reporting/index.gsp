<%@page import="de.laser.ReportingService;de.laser.Org;de.laser.Subscription" %>
<laser:serviceInjection/>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="laser">
        <title><g:message code="laser"/> : <g:message code="myinst.reporting"/></title>
        <laser:javascript src="echarts.js"/>%{-- dont move --}%
    </head>

    <body>
        <semui:breadcrumbs>
            <semui:crumb controller="myInstitution" action="dashboard" text="${institution.getDesignation()}"/>
            <semui:crumb text="${message(code:'myinst.reporting')}" class="active" />
        </semui:breadcrumbs>

        %{-- <semui:controlButtons>
            <semui:exportDropdown>
                <semui:exportDropdownItem>
                    <g:link class="item" action="reporting_old" params="${exportParams}">${message(code: 'default.button.export.xls')}</g:link>
                </semui:exportDropdownItem>
            </semui:exportDropdown>
        </semui:controlButtons> --}%

        <h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon />
            <g:message code="myinst.reporting"/> <span class="ui label red">DEMO</span>
        </h1>

        <div class="ui message info">
            <p>
                <strong>1. Suchanfrage definieren ..</strong>
                <br />
                Erzeugen Sie eine Ergebnismenge anhand kombinierbarer Filter.
                Verschiedene Einstiegspunkte ermöglichen eine individuelle Zusammenstellung Ihrer Daten.
            </p>
            <p>
                <strong>2. Ergebnis untersuchen ..</strong>
                <br />
                Die in Schritt 1 erzeugte Ergebnismenge kann durch vordefinierte Abfragen
                weiter analyisert und visualisiert werden.
            </p>
        </div>
        <h2 class="ui header la-noMargin-top">1. Suchanfrage definieren ..</h2>

        <g:set var="hidden" value="hidden" />

        <g:if test="${!filter}">
            <div class="ui segment">
                Suche über folgenden Einstiegspunkt beginnen: &nbsp;
                <g:select name="filter-chooser"
                          from="${filterChooserList}"
                          optionKey="key"
                          optionValue="value"
                          class="ui selection dropdown"
                          noSelection="${['': message(code: 'default.select.choose.label')]}" />
            </div>
        </g:if>
        <g:else>
            <g:set var="hidden" value="" />
        </g:else>

        <g:if test="${!filter || filter == 'subscription'}">
            <div id="filter-subscription" class="filter-form-wrapper ${hidden}">
                <g:render template="/myInstitution/reporting/filter/subscription" />
            </div>
        </g:if>

        <g:if test="${!filter || filter == 'organisation'}">
            <div id="filter-organisation" class="filter-form-wrapper ${hidden}">
                <g:render template="/myInstitution/reporting/filter/organisation" />
            </div>
        </g:if>

        <g:if test="${result}">
            <h2 class="ui header la-noMargin-top">2. Ergebnis untersuchen ..</h2>

            <g:if test="${filter == 'subscription'}">
                <g:render template="/myInstitution/reporting/query/subscription" />

                <g:render template="/myInstitution/reporting/result/subscription" />
            </g:if>

            <g:if test="${filter == 'organisation'}">
                <g:render template="/myInstitution/reporting/query/organisation" />

                <g:render template="/myInstitution/reporting/result/organisation" />
            </g:if>

        </g:if>

        <laser:script file="${this.getGroovyPageFileName()}">
            $('#filter-chooser').on('change', function(e){
                $('.filter-form-wrapper').addClass('hidden')
                $('#filter-' + $('#filter-chooser').dropdown('get value')).removeClass('hidden')
            })
/*
            $('#result-chooser').on('change', function(e){
                alert( $('#result-chooser').dropdown('get value') )
            })

            $('#echart-chooser').on('change', function(e){
                alert( $('#echart-chooser').dropdown('get value') )
            })
            */
        </laser:script>

    </body>
</html>

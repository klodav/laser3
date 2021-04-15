<%@page import="de.laser.reporting.myInstitution.base.BaseConfig;de.laser.ReportingService;de.laser.Org;de.laser.Subscription" %>
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

        <h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon />
            <g:message code="myinst.reporting"/> <span class="ui label red">DEMO : in Entwicklung</span>
        </h1>

        <h2 class="ui header hidden">Diese Funktionalität befindet sich in Entwicklung</h2>

        <div class="ui message info">
            <p>
                <strong>${message(code:'reporting.macro.step1')}</strong> <br />
                ${message(code:'reporting.macro.info1')}
            </p>
            <p>
                <strong>${message(code:'reporting.macro.step2')}</strong> <br />
                ${message(code:'reporting.macro.info2')}
            </p>
            <p>
                <strong>${message(code:'reporting.macro.step3')}</strong> <br />
                ${message(code:'reporting.macro.info3')}
            </p>
        </div>

       %{-- <g:if test="${filterHistory}">
            ${filterHistory}
        </g:if> --}%

        <h3 class="ui header">${message(code:'reporting.macro.step1')}</h3>

        <g:set var="hidden" value="hidden" />

        <g:if test="${!filter}">
            <div class="ui segment form">
                <div class="fields two">
                    <div class="field">
                        <label for="filter-chooser">${message(code:'reporting.filter.base')}</label>
                        <g:select name="filter-chooser"
                                  from="${cfgFilterList}"
                                  optionKey="key"
                                  optionValue="value"
                                  class="ui selection dropdown la-not-clearable"
                                  noSelection="${['': message(code: 'default.select.choose.label')]}" />
                    </div>
                </div>
            </div>
        </g:if>
        <g:else>
            <g:set var="hidden" value="" />
        </g:else>

        <g:each in="${BaseConfig.FILTER}" var="filterItem">

            <g:if test="${!filter || filter == filterItem.key}">
                <div id="filter-${filterItem.key}" class="filter-form-wrapper ${hidden}">
                    <g:render template="/myInstitution/reporting/filter/${filterItem.key}" />
                </div>
            </g:if>
        </g:each>

        <g:render template="/templates/reporting/helper" />

        <g:if test="${filterResult}">

            <h3 class="ui header">${message(code:'reporting.macro.step2')}</h3>

            <g:render template="/myInstitution/reporting/query/${filter}" />

            <div id="chart-wrapper"></div>
            <div id="chart-details"></div>
        </g:if>

        <style>
            #chart-wrapper { height: 400px; width: 98%; margin: 2em auto 1em; }

            h3.ui.header { margin-top: 3em !important; }

            .ui.form .fields .field { margin-bottom: 0 !important; }
        </style>

        <laser:script file="${this.getGroovyPageFileName()}">
            $('#filter-chooser').on( 'change', function(e) {
                $('.filter-form-wrapper').addClass('hidden')
                $('#filter-' + $(e.target).dropdown('get value')).removeClass('hidden');
            })

            $('*[id^=query-chooser').on( 'change', function(e) {
                var value = $(e.target).dropdown('get value');
                if (value) {
                    $('*[id^=query-chooser').not($('#' + e.target.id)).dropdown('clear');
                    JSPC.app.reporting.current.request.query = value;
                    JSPC.app.reporting.requestChartJsonData();
                }
            })

            $('#chart-chooser').on( 'change', function(e) {
                JSPC.app.reporting.current.request.chart = $(e.target).dropdown('get value');
                JSPC.app.reporting.requestChartJsonData();
            })

            $('#chart-export').on( 'click', function(e) {
                if ( JSPC.app.reporting.current.request.query ) {
                    alert('[msg:1] - Nicht implementiert');
                }
            })

            JSPC.app.reporting.requestChartJsonData = function() {
                if ( JSPC.app.reporting.current.request.query && JSPC.app.reporting.current.request.chart ) {
                    JSPC.app.reporting.current.chart = {};

                    $.ajax({
                        url: "<g:createLink controller="ajaxJson" action="chart" />",
                        dataType: 'script',
                        method: 'post',
                        data: JSPC.app.reporting.current.request,
                        beforeSend: function (xhr) {
                            //$('#chart-export').attr('disabled', 'disabled');
                        }
                    })
                    .done( function (data) {
                        $('#chart-wrapper').replaceWith( '<div id="chart-wrapper"></div>' );
                        $('#chart-details').replaceWith( '<div id="chart-details"></div>' );

                        if (JSPC.app.reporting.current.request.chart == 'bar') {
                            $('#chart-wrapper').css('height', 150 + (18 * JSPC.app.reporting.current.chart.option.dataset.source.length) + 'px');
                        }
                        else if (JSPC.app.reporting.current.request.chart == 'pie') {
                            $('#chart-wrapper').css('height', 350 + (12 * JSPC.app.reporting.current.chart.option.dataset.source.length) + 'px');
                        }
                        else if (JSPC.app.reporting.current.request.chart == 'radar') {
                            $('#chart-wrapper').css('height', 400 + (8 * JSPC.app.reporting.current.chart.option.dataset.source.length) + 'px');
                        }

                        var echart = echarts.init($('#chart-wrapper')[0]);
                        echart.setOption( JSPC.app.reporting.current.chart.option );
                        echart.on( 'click', function (params) {
                            var clone = {}
                            Object.assign(clone, JSPC.app.reporting.current.request);
                            clone.id = params.data[0];
                            clone.label = params.data[1];
                            clone.context = '${BaseConfig.KEY}';
                            JSPC.app.reporting.requestChartHtmlDetails(clone);
                        });
                        echart.on( 'legendselectchanged', function (params) { /* console.log(params); */ });

                        JSPC.app.reporting.current.chart.echart = echart;
                        //$('#chart-export').removeAttr('disabled');
                    })
                    .fail( function (data) {
                        $('#chart-wrapper').replaceWith( '<div id="chart-wrapper"></div>' );
                        $('#chart-details').replaceWith( '<div id="chart-details"></div>' );
                    })
                }
            }
        </laser:script>

        <semui:modal id="reporting-modal-error" text="REPORTING" hideSubmitButton="true">
            <p>${message(code:'reporting.modal.error')}</p>
        </semui:modal>
        <semui:modal id="reporting-modal-nodata" text="REPORTING" hideSubmitButton="true">
            <p>${message(code:'reporting.modal.nodata')}</p>
        </semui:modal>

    </body>
</html>

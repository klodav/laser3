<%@ page import="de.laser.reporting.export.base.BaseExport; de.laser.reporting.export.myInstitution.ExportGlobalHelper; de.laser.reporting.export.DetailsExportManager; de.laser.reporting.myInstitution.base.BaseConfig; de.laser.reporting.myInstitution.base.BaseDetails;" %>
<laser:serviceInjection />
<!-- _detailsModal.gsp -->
<g:set var="export" value="${DetailsExportManager.createExport( token, BaseConfig.KEY_MYINST )}" />

<g:if test="${export}">
    <g:set var="formFields" value="${export.getAllFields()}" />
    <g:set var="filterLabels" value="${ExportGlobalHelper.getCachedFilterLabels( token )}" />
    <g:set var="queryLabels" value="${ExportGlobalHelper.getCachedQueryLabels( token )}" />

    <semui:modal id="${modalID}" text="${message(code: 'reporting.modal.export.key.' + export.KEY)}" msgSave="${message(code: 'default.button.export.label')}">

        <div class="ui form">
            <div class="field">
                <label>${message(code: 'reporting.modal.export.todoData')}</label>
            </div>
            <div class="ui segments">
                <g:render template="/myInstitution/reporting/query/generic_filterLabels" model="${[filterLabels: filterLabels, stacked: true]}" />
                <g:render template="/myInstitution/reporting/details/generic_queryLabels" model="${[queryLabels: queryLabels, stacked: true]}" />
            </div>
        </div>

        <g:set var="dcSize" value="${ExportGlobalHelper.getDetailsCache(token).idList.size()}" />
        <g:if test="${dcSize > 50}">
            <div class="ui info message">
                <i class="info circle icon"></i> ${message(code: 'reporting.modal.export.todoTime')}
            </div>
        </g:if>

        <g:form controller="ajaxHtml" action="chartDetailsExport" method="POST" target="_blank">

            <div class="ui form">

            <div class="ui vertical segment">
                <div class="field">
                    <label>${message(code: 'reporting.modal.export.todoFields')}</label>
                </div>
                <div class="fields">

                    <g:each in="${ExportGlobalHelper.reorderFieldsForUI( formFields.findAll { !ExportGlobalHelper.isFieldMultiple( it.key ) } )}" var="field" status="fc">
                        <div class="wide eight field">

                            <g:if test="${field.key == 'globalUID'}">
                                <div class="ui checkbox">
                                    <input type="checkbox" name="cde:${field.key}" id="cde:${field.key}">
                                    <label for="cde:${field.key}">${export.getFieldLabel(field.key as String)}</label>
                                </div>
                            </g:if>
                            <g:else>
                                <div class="ui checkbox">
                                    <input type="checkbox" name="cde:${field.key}" id="cde:${field.key}" checked="checked">
                                    <label for="cde:${field.key}">${export.getFieldLabel(field.key as String)}</label>
                                </div>
                            </g:else>

                        </div><!-- .field -->

                        <g:if test="${fc%2 == 1}">
                            </div>
                            <div class="fields">
                        </g:if>
                    </g:each>

                </div><!-- .fields -->

                <div class="fields">

                    <g:each in="${formFields.findAll { ['x-identifier','@ae-org-accessPoint','@ae-org-contact','@ae-org-readerNumber'].contains( it.key ) }}" var="field" status="fc"> %{-- TODO --}%
                        <div class="wide eight field">

                            <g:set var="multiList" value="${ExportGlobalHelper.getMultipleFieldListForDropdown(field.key, export.getCurrentConfig( export.KEY ))}" />

                            <g:select name="cde:${field.key}" class="ui selection dropdown"
                                      from="${multiList}" multiple="true"
                                      optionKey="${{it[0]}}" optionValue="${{it[1]}}"
                                      noSelection="${['': export.getFieldLabel(field.key as String)]}"
                            />

                        </div><!-- .field -->

                        <g:if test="${fc%2 == 1}">
                            </div>
                            <div class="fields">
                        </g:if>
                    </g:each>

                </div><!-- .fields -->
            </div><!-- .segment -->

            <div class="ui vertical segment">
                <div class="fields">

                    <div id="fileformat-csv" class="wide eight field">
                        <label>${message(code: 'reporting.modal.export.cfg.csv')}</label>
                        <p>
                            ${message(code: 'reporting.modal.export.cfg.csv.fieldSeparator')}: <span class="ui circular label">${BaseExport.CSV_FIELD_SEPARATOR}</span> <br />
                            ${message(code: 'reporting.modal.export.cfg.csv.fieldQuotation')}: <span class="ui circular label">${BaseExport.CSV_FIELD_QUOTATION}</span> <br />
                            ${message(code: 'reporting.modal.export.cfg.csv.valueSeparator')}: <span class="ui circular label">${BaseExport.CSV_VALUE_SEPARATOR}</span>
                        </p>
                    </div>

                    <div id="fileformat-pdf" class="wide eight field">
                        <label>${message(code: 'reporting.modal.export.cfg.pdf')}</label>
                        <p>
                            ${message(code: 'reporting.modal.export.cfg.pdf.pageFormat')}: <span class="ui circular label">${message(code: 'reporting.modal.export.cfg.pdf.pageFormat.default')}</span> <br />
                            ${message(code: 'reporting.modal.export.cfg.pdf.queryInfo')}: <span class="ui circular label">${message(code: 'reporting.modal.export.cfg.pdf.queryInfo.default')}</span> <br />
                        </p>
                    </div>

                    <div class="wide eight field">
                        <div class="field" style="margin-bottom: 1em !important;">
                            <label for="fileformat">${message(code: 'default.fileFormat.label')}</label>
                            <g:select name="fileformat" class="ui selection dropdown la-not-clearable"
                                      optionKey="key" optionValue="value"
                                      from="${[csv:'CSV', pdf:'PDF']}"
                            />
                        </div>
                        <div class="field">
                            <label for="filename">${message(code: 'default.fileName.label')}</label>
                            <input name="filename" id="filename" value="${ExportGlobalHelper.getFileName(queryLabels)}" />
                        </div>
                    </div>

                </div><!-- .fields -->
            </div><!-- .segment -->

            </div><!-- .form -->

            <input type="hidden" name="token" value="${token}" />
            <input type="hidden" name="context" value="${BaseConfig.KEY_MYINST}" />
        </g:form>

    </semui:modal>

    <laser:script file="${this.getGroovyPageFileName()}">

        $('#${modalID} select[name=fileformat]').on( 'change', function() {
            $('#${modalID} *[id^=fileformat-').addClass('hidden')
            $('#${modalID} *[id^=fileformat-' + $('#${modalID} select[name=fileformat]').val()).removeClass('hidden')
        }).trigger('change');
    </laser:script>
</g:if>
<!-- _detailsModal.gsp -->


<laser:htmlStart message="menu.public.all_insts" serviceInjection="true"/>
        <g:set var="entityName" value="${message(code: 'org.label')}" />

    <semui:breadcrumbs>
        <semui:crumb message="menu.public.all_insts" class="active" />
    </semui:breadcrumbs>

    <semui:controlButtons>
        <semui:exportDropdown>
            <semui:exportDropdownItem>
                <a class="item" data-semui="modal" href="#individuallyExportModal">Click Me Excel Export</a>
            </semui:exportDropdownItem>
        </semui:exportDropdown>

        <%
            editable = (editable && accessService.checkPerm('ORG_INST,ORG_CONSORTIUM')) || contextService.getUser()?.hasRole('ROLE_ADMIN,ROLE_ORG_EDITOR') || accessService.checkConstraint_ORG_COM_EDITOR()
        %>
        <g:if test="${editable}">
            <laser:render template="actions" />
        </g:if>
    </semui:controlButtons>

    <semui:h1HeaderWithIcon message="menu.public.all_insts" total="${consortiaMemberTotal}" floated="true" />

    <semui:messages data="${flash}" />

    <laser:render template="/templates/filter/javascript" />
    <semui:filter showFilterButton="true">
        <g:form action="listInstitution" method="get" class="ui form">
            <laser:render template="/templates/filter/orgFilter"
                      model="[
                              tmplConfigShow: [['name', 'identifier'], ['country&region', 'libraryNetwork', 'libraryType', 'subjectGroup']],
                              tmplConfigFormFilter: true
                      ]"/>
        </g:form>
    </semui:filter>

    <laser:render template="/templates/filter/orgFilterTable"
              model="[orgList: availableOrgs,
                      consortiaMemberIds: consortiaMemberIds,
                      tmplShowCheckbox: false,
                      tmplConfigShow: [
                              'sortname', 'name', 'wibid', 'isil', 'region', 'libraryNetwork', 'libraryType', 'status', 'legalInformation'
                      ]
              ]"/>

    <semui:paginate action="listInstitution" params="${params}" max="${max}" total="${consortiaMemberTotal}" />

    <laser:render template="/myInstitution/export/individuallyExportModalOrgs" model="[modalID: 'individuallyExportModal', orgType: 'institution']" />

<laser:htmlEnd />

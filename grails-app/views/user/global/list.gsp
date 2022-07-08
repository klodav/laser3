<%@ page import="de.laser.*; de.laser.auth.*" %>
<laser:htmlStart message="menu.institutions.users" serviceInjection="true"/>

        <g:if test="${controllerName == 'myInstitution'}">
        %{-- myInstitution has no breadcrumb yet --}%
            <laser:render template="/organisation/breadcrumb" model="${[ inContextOrg: inContextOrg, orgInstance: orgInstance, institutionalView: institutionalView, params:params ]}"/>
        </g:if>
        <g:if test="${controllerName == 'organisation'}">
            <laser:render template="/organisation/breadcrumb" model="${[ inContextOrg: inContextOrg, orgInstance: orgInstance, institutionalView: institutionalView, params:params ]}"/>
        </g:if>
        <g:if test="${controllerName == 'user'}">
            <laser:render template="/user/breadcrumb" model="${[ inContextOrg: inContextOrg, orgInstance: orgInstance, institutionalView: institutionalView, params:params ]}"/>
        </g:if>

        <semui:h1HeaderWithIcon text="${titleMessage}" total="${total}" />

        <semui:controlButtons>
            <laser:render template="/user/global/actions" />
        </semui:controlButtons>

        <g:if test="${controllerName == 'myInstitution'}">
            <laser:render template="/organisation/nav" model="${navConfig}"/>
        </g:if>
        <g:if test="${controllerName == 'organisation'}">
            <laser:render template="/organisation/nav" model="${navConfig}"/>
        </g:if>

        <laser:render template="/templates/user/filter" model="${filterConfig}"/>

        <g:if test="${multipleAffiliationsWarning}">
            <div class="ui info message la-clear-before">${message(code:'user.edit.info')}</div>
        </g:if>

        <semui:messages data="${flash}" />

        <laser:render template="/templates/user/list" model="${tmplConfig}" />

<laser:htmlEnd />

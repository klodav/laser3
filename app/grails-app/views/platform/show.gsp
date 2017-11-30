<%@ page import="com.k_int.kbplus.Platform" %>
<r:require module="annotations" />
<!doctype html>
<html>
  <head>
    <meta name="layout" content="semanticUI">
    <g:set var="entityName" value="${message(code: 'platform.label', default: 'Platform')}" />
    <title><g:message code="default.show.label" args="[entityName]" /></title>
  </head>
  <body>

<semui:breadcrumbs>
    <semui:crumb controller="platform" action="index" message="platform.show.all" />
    <semui:crumb class="active" id="${platformInstance.id}" text="${platformInstance.name}" />

        <li class="pull-right">
            <g:if test="${editable}">
                <semui:crumbAsBadge message="default.editable" class="orange" />
            </g:if>
          View:
          <div class="btn-group" data-toggle="buttons-radio">
            <g:link controller="platform" action="show" params="${params+['mode':'basic']}" class="btn btn-primary btn-mini ${((params.mode=='basic')||(params.mode==null))?'active':''}">${message(code:'default.basic', default:'Basic')}</g:link>
            <g:link controller="platform" action="show" params="${params+['mode':'advanced']}" class="btn btn-primary btn-mini ${params.mode=='advanced'?'active':''}">${message(code:'default.advanced', default:'Advanced')}</g:link>
          </div>
          &nbsp;
         </li>
</semui:breadcrumbs>

          <h1 class="ui header">Platform : <g:if test="${editable}"><span id="platformNameEdit"
                                                        class="xEditableValue"
                                                        data-type="textarea"
                                                        data-pk="${platformInstance.class.name}:${platformInstance.id}"
                                                        data-name="name"
                                                        data-url='<g:createLink controller="ajax" action="editableSetValue"/>'>${platformInstance.name}</span></g:if><g:else>${platformInstance.name}</g:else>
          </h1>


        <semui:messages data="${flash}" />

        <fieldset class="inline-lists">
            <dl>
              <dt>${message(code:'platform.name', default:'Platform Name')}</dt>
              <dd> <g:xEditable owner="${platformInstance}" field="name"/></dd>
            </dl>

            <dl>
              <dt>${message(code:'platform.primaryUrl', default:'Primary URL')}</dt>
              <dd> <g:xEditable owner="${platformInstance}" field="primaryUrl"/></dd>
            </dl>

            <dl>
              <dt>${message(code:'platform.serviceProvider', default:'Service Provider')}</dt>
              <dd>
                <g:xEditableRefData owner="${platformInstance}" field="serviceProvider" config="YN"/>
              </dd>
            </dl>

            <dl>
              <dt>${message(code:'platform.softwareProvider', default:'Software Provider')}</dt>
              <dd>
                <g:xEditableRefData owner="${platformInstance}" field="softwareProvider" config="YN"/>
              </dd>
            </dl>

            <g:if test="${params.mode=='advanced'}">

              <dl>
                <dt>${message(code:'platform.type', default:'Type')}</dt>
                <dd> <g:xEditableRefData owner="${platformInstance}" field="type" config="YNO"/></dd>
              </dl>

              <dl>
                <dt>${message(code:'platform.status', default:'Status')}</dt>
                <dd> <g:xEditableRefData owner="${platformInstance}" field="status" config="UsageStatus"/></dd>
              </dl>

            </g:if>

        </fieldset>

        ${message(code:'platform.show.availability', default:'Availability of titles in this platform by package')}

            <table class="ui celled table">
                <thead>
                  <tr>
                    <th rowspan="2" style="width: 25%;">${message(code:'title.label', default:'Title')}</th>
                    <th rowspan="2" style="width: 20%;">${message(code:'identifier.plural', default:'Identifiers')}</th>
                    <th colspan="${packages.size()}">${message(code:'platform.show.provided_by', default:'Provided by package')}</th>
                  </tr>
                  <tr>
                    <g:each in="${packages}" var="p">
                      <td><g:link controller="package" action="show" id="${p.id}">${p.name} (${p.contentProvider?.name})</g:link></td>
                    </g:each>
                  </tr>
                </thead>
              <g:each in="${titles}" var="t">
                <tr>
                  <td style="text-align:left;"><g:link controller="titleInstance" action="show" id="${t.title.id}">${t.title.title}</g:link>&nbsp;</td>
                  <td>
                    <g:each in="${t.title.ids}" var="tid">
                      <g:if test="${tid.identifier.ns.ns != 'originediturl'}">
                        <div><span>${tid.identifier.ns.ns}</span>: <span>${tid.identifier.value}</span></div>
                      </g:if>
                      <g:else>
                        <div><span>GOKb</span>: <a href="${tid.identifier.value}">${message(code:'component.originediturl.label')}</a></div>
                      </g:else>
                    </g:each>
                  </td>
                  <g:each in="${crosstab[t.position]}" var="tipp">
                    <g:if test="${tipp}">
                      <td>${message(code:'platform.show.from', default:'from')}: <g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${tipp.startDate}"/>
                            <g:if test="${tipp.startVolume}"> / ${message(code:'tipp.volume', default:'volume')}: ${tipp.startVolume} </g:if>
                            <g:if test="${tipp.startIssue}"> / ${message(code:'tipp.issue', default:'issue')}: ${tipp.startIssue} </g:if> <br/>
                          ${message(code:'platform.show.to', default:'to')}:  <g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${tipp.endDate}"/>
                            <g:if test="${tipp.endVolume}"> / ${message(code:'tipp.volume', default:'volume')}: ${tipp.endVolume}</g:if>
                            <g:if test="${tipp.endIssue}"> / ${message(code:'tipp.issue', default:'issue')}: ${tipp.endIssue}</g:if> <br/>
                          ${message(code:'tipp.coverageDepth', default:'coverage Depth')}: ${tipp.coverageDepth}</br>
                        <g:link controller="titleInstancePackagePlatform" action="show" id="${tipp.id}">${message(code:'platform.show.full_tipp', default:'Full TIPP Details')}</g:link>
                      </g:if>
                      <g:else>
                        <td></td>
                      </g:else>
                    </td>
                  </g:each>
                </tr>
              </g:each>
            </table>

  </body>
</html>

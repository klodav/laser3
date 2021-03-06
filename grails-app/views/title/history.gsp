<%@ page import="de.laser.titles.TitleInstance" %>
<laser:htmlStart message="title.nav.history" />

    <ui:breadcrumbs>
        <ui:crumb controller="title" action="list" message="menu.public.all_titles" />
        <ui:crumb class="active" text="${message(code:'default.title.label')}: ${titleInstance.title}" />
    </ui:breadcrumbs>

    <g:if test="${editable}">
        <ui:crumbAsBadge message="default.editable" class="orange" />
    </g:if>

    <ui:h1HeaderWithIcon text="${titleInstance.title}" />

    <laser:render template="nav" />

    <ui:messages data="${flash}" />

    <g:if test="${formattedHistoryLines?.size() > 0}">

      <div class="container alert-warn">
        <table class="ui celled la-js-responsive-table la-table table">
          <thead>
            <tr>
              <th>${message(code:'default.name.label')}</th>
              <th>${message(code:'event.actor')}</th>
              <th>${message(code:'event.name')}</th>
              <th>${message(code:'default.property.label')}</th>
              <th>${message(code:'event.old')}</th>
              <th>${message(code:'event.new')}</th>
              <th>${message(code:'default.date.label')}</th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${formattedHistoryLines}" var="hl">
              <tr>
                <td><a href="${hl.link}">${hl.name}</a></td>
                <td>
                  <g:link controller="user" action="edit" id="${hl.actor?.id}">${hl.actor?.displayName}</g:link>
                </td>
                <td>${hl.eventName}</td>
                <td>${hl.propertyName}</td>
                <td>${hl.oldValue}</td>
                <td>${hl.newValue}</td>
                <td><g:formatDate format="${message(code:'default.date.format.notime')}" date="${hl.lastUpdated}"/></td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>

        <g:if test="${historyLines != null}" >
          <ui:paginate action="history" controller="title" params="${params}" maxsteps="${max}" total="${num_hl}" />
        </g:if>

    </g:if>
    <g:else>
      <div class="container alert-warn"> </div>
    </g:else>


<laser:htmlEnd />

<laser:htmlStart text="${message(code:'menu.datamanager')} ${message(code:'default.dashboard')}" />

    <ui:breadcrumbs>
      <ui:crumb message="menu.datamanager" class="active"/>
    </ui:breadcrumbs>

  <ui:h1HeaderWithIcon message="menu.datamanager" type="datamanager" />

  <br />
  <br />

    <ui:messages data="${flash}" />

    <g:if test="${pendingChanges?.size() > 0}">
        <h2 class="ui icon header la-clear-before la-noMargin-top">${message(code:'datamanager.pending.label')}</h2>
        <table class="ui celled la-js-responsive-table la-table table">
          <thead>
            <tr>
              <th>${message(code:'default.info.label')}</th>
              <th class="la-action-info">${message(code:'default.actions.label')}</th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${pendingChanges}" var="pc">
              <tr>
                <td>
                  <g:link controller="package" action="show" id="${pc.pkg.id}">${pc.pkg.name}</g:link> <br />${pc.desc}
                </td>
                <td class="x">
                  <g:link controller="pendingChange" action="accept" id="${pc.id}" class="ui positive button">${message(code:'datamanager.accept.label')}</g:link>
                  <g:link controller="pendingChange" action="reject" id="${pc.id}" class="ui negative button">${message(code:'datamanager.reject.label')}</g:link>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>

    </g:if>
    <g:else>
      <div class="container alert-warn">
        <h2 class="ui header">${message(code:'datamanager.none_pending.label')}</h2>
      </div>
    </g:else>

<laser:htmlEnd />

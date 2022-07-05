<%@ page import="de.laser.Org" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="laser">
    <title>${message(code:'laser')} : ${user.display}</title>
</head>
<body>
      <laser:render template="breadcrumb" model="${[ params:params ]}"/>

      <semui:h1HeaderWithIcon text="${user.username} : ${user.displayName ?: 'No username'}" />

      <semui:messages data="${flash}" />

      <h2 class="ui header">${message(code:'user.affiliation.plural')}</h2>

      <table class="ui celled la-js-responsive-table la-table table">
        <thead>
          <tr>
            <th>${message(code:'user.id')}</th>
            <th>${message(code:'user.org')}</th>
            <th>${message(code:'default.role.label')}</th>
          </tr>
        </thead>
        <tbody>
          <g:each in="${user.affiliations}" var="af">
            <tr>
              <td>${af.id}</td>
              <td>${af.org.name}</td>
              <td>${message(code:"cv.roles.${af.formalRole.authority}")}</td>
            </tr>
          </g:each>
        </tbody>
      </table>

      <h2 class="ui header">${message(code:'user.role.plural')}</h2>

      <table class="ui celled la-js-responsive-table la-table table">
        <thead>
          <tr>
            <th>${message(code:'default.role.label')}</th>
          </tr>
        </thead>
        <tbody>
          <g:each in="${user.roles}" var="rl">
            <tr>
              <td>${rl.role.authority}</td>
            </tr>
          </g:each>
        </tbody>
      </table>
</body>
</html>

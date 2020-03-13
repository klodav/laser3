<%@ page import="com.k_int.kbplus.RefdataValue; de.laser.helper.RDStore; de.laser.helper.RDConstants" %>
<table id="subPkgPlatformTable" class="ui celled la-table table compact">
  <thead>
  <tr>
    <th>${message(code: "accessPoint.subscription.label")}</th>
    <th>${message(code: "accessPoint.package.label")}</th>
    <g:sortableColumn property="platform" title="${message(code: "platform.label")}"/>
  </tr>
  </thead>
  <tbody>
  <g:each in="${linkedPlatformSubscriptionPackages}" var="linkedPlatformSubscriptionPackage">
    <g:set var="subscription" value="${linkedPlatformSubscriptionPackage[2]}"/>
    <tr>
      <td><g:link controller="subscription" action="show"
                  id="${subscription.id}">${subscription.name} ${(subscription.status != RDStore.SUBSCRIPTION_CURRENT) ? '('+ com.k_int.kbplus.RefdataValue.getByValueAndCategory(subscription.status.value, RDConstants.SUBSCRIPTION_STATUS).getI10n('value') +')': ''}</g:link></td>
      <td><g:link controller="package" action="show"
                  id="${linkedPlatformSubscriptionPackage[1].pkg.id}">${linkedPlatformSubscriptionPackage[1].pkg.name}</g:link></td>
      <td><g:link controller="platform" action="show"
                  id="${linkedPlatformSubscriptionPackage[0].id}">${linkedPlatformSubscriptionPackage[0].name}</g:link></td>
    </tr>
  </g:each>
  </tbody>
</table>
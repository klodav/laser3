<%@ page import="de.laser.TitleInstancePackagePlatform; de.laser.storage.RDStore;" %>
<ui:subNav actionName="${actionName}">

    <ui:subNavItem controller="package" action="show" params="${[id:params.id]}" message="package.show.nav.details" />

    <ui:subNavItem controller="package" action="current" counts="${currentTippsCounts}" params="${[id:params.id]}" message="package.show.nav.current" />

    <ui:subNavItem controller="package" action="planned" counts="${plannedTippsCounts}" params="${[id:params.id]}" message="package.show.nav.planned" />

    <ui:subNavItem controller="package" action="expired" counts="${expiredTippsCounts}" params="${[id:params.id]}" message="package.show.nav.expired" />

    <ui:subNavItem controller="package" action="deleted" counts="${deletedTippsCounts}" params="${[id:params.id]}" message="package.show.nav.deleted" />

    <ui:subNavItem controller="package" action="tippChanges" params="${[id:params.id]}" message="package.show.nav.tippChanges" />
%{--    <ui:securedSubNavItem orgPerm="ORG_INST,ORG_CONSORTIUM" controller="package" action="tasks" params="${[id:params.id]}" message="task.plural" />

    <ui:securedSubNavItem orgPerm="ORG_INST,ORG_CONSORTIUM" controller="package" action="documents" params="${[id:params.id]}" message="package.show.nav.docs" />

    <ui:subNavItem controller="package" action="notes" params="${[id:params.id]}" message="package.show.nav.notes" />--}%

    <sec:ifAnyGranted roles="ROLE_ADMIN">
        <ui:subNavItem controller="package" action="history" params="${[id:params.id]}" class="la-role-admin" message="package.show.nav.history" />
    </sec:ifAnyGranted>

</ui:subNav>

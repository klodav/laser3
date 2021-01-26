<%@ page import="com.k_int.kbplus.GenericOIDService; grails.plugin.springsecurity.SpringSecurityUtils; de.laser.auth.Role;de.laser.auth.UserRole;de.laser.UserSetting" %>
<laser:serviceInjection/>

<table class="ui sortable celled la-table compact table">
    <thead>
    <tr>
        <th>${message(code:'user.username.label')}</th>
        <th>${message(code:'user.displayName.label')}</th>
        <th>${message(code:'user.email')}</th>
        <th>
            <g:if test="${showAllAffiliations}">
                <g:message code="user.org"/>
            </g:if>
            <g:else>
                <g:message code="profile.membership.role"/>
            </g:else>
        </th>
        <th>${message(code:'user.enabled.label')}</th>
        <th class="la-action-info">${message(code:'default.actions.label')}</th>
    </tr>
    </thead>
    <tbody>
        <g:each in="${users}" var="us">
            <tr>
                <td>
                    ${fieldValue(bean: us, field: "username")}

                    <g:if test="${! UserRole.findByUserAndRole(us, Role.findByAuthority('ROLE_USER'))}">
                        <span  class="la-popup-tooltip la-delay" data-content="Dieser Account besitzt keine ROLE_USER-Rechte." data-position="top right">
                            <i class="icon minus circle red"></i>
                        </span>
                    </g:if>
                </td>
                <td>${us.getDisplayName()}</td>
                <td>${us.email}</td>
                <td>
                    <g:each in="${us.getAuthorizedAffiliations()}" var="affi">
                        <g:set var="uoId" value="${affi.id}"/><%-- ERMS-2370 fix this for count>1 --%>
                        <g:if test="${showAllAffiliations}">
                            ${affi.org?.getDesignation()} <span>(${affi.formalRole.authority})</span> <br />
                        </g:if>
                        <g:else>
                            <g:if test="${affi.org.id == orgInstance.id}">
                                <g:message code="cv.roles.${affi.formalRole.authority}"/>
                            </g:if>
                        </g:else>
                    </g:each>
                </td>
                <td>
                    <g:if test="${modifyAccountEnability}">
                        <semui:xEditableBoolean owner="${us}" field="enabled"/>
                    </g:if>
                    <g:else>
                        <g:if test="${! us.enabled}">
                            <span data-position="top left" class="la-popup-tooltip la-delay" data-content="${message(code:'user.disabled.text')}">
                                <i class="icon minus circle red"></i>
                            </span>
                        </g:if>
                    </g:else>
                </td>
                <td class="x">
                    <g:if test="${editable && (instAdmService.isUserEditableForInstAdm(us, editor) || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN'))}">

                        <g:if test="${controllerName == 'user'}">
                            <g:link controller="${controllerName}" action="${editLink}" params="${[id: us.id]}" class="ui icon button"><i class="write icon"></i></g:link>
                        </g:if>
                        <g:if test="${controllerName == 'myInstitution'}">
                            <g:link controller="${controllerName}" action="${editLink}" params="${[uoid: genericOIDService.getOID(us)]}" class="ui icon button"><i class="write icon"></i></g:link>
                        </g:if>
                        <g:if test="${controllerName == 'organisation'}">
                            <g:link controller="${controllerName}" action="${editLink}" id="${orgInstance.id}" params="${[uoid: genericOIDService.getOID(us)]}" class="ui icon button"><i class="write icon"></i></g:link>
                        </g:if>

                        <g:if test="${! instAdmService.isUserLastInstAdminForOrg(us, orgInstance)}">
                            <g:if test="${controllerName == 'user'}">
                                <g:link controller="${controllerName}" action="${deleteLink}" params="${[id: us.id]}" class="ui icon negative button"><i class="trash alternate icon"></i></g:link>
                            </g:if>
                            <g:if test="${controllerName == 'myInstitution'}">
                                <g:link controller="${controllerName}" action="${deleteLink}" params="${[uoid: genericOIDService.getOID(us)]}" class="ui icon negative button"><i class="trash alternate icon"></i></g:link>
                                %{--
                                <g:link class="ui icon negative button js-open-confirm-modal la-popup-tooltip la-delay"
                                        data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.user.organisation", args: [us.displayName, orgInstance.name ])}"
                                        data-confirm-term-how="delete"
                                        controller="organisation"
                                        action="processAffiliation"
                                        params="${[assoc:uoId, id:orgInstance?.id, cmd:'delete']}"
                                        data-content="${message(code:'profile.membership.delete.button')}" data-position="top left" >
                                    <i class="trash alternate icon"></i>
                                </g:link>
                                --}%
                            </g:if>
                            <g:if test="${controllerName == 'organisation'}">
                                <g:link controller="${controllerName}" action="${deleteLink}" id="${orgInstance.id}" params="${[uoid: genericOIDService.getOID(us)]}" class="ui icon negative button"><i class="trash alternate icon"></i></g:link>
                            </g:if>
                        </g:if>
                        <g:else>
                            <span  class="la-popup-tooltip la-delay" data-content="${message(code:'user.affiliation.lastAdminForOrg1', args: [us.getDisplayName()])}">
                                <button class="ui icon negative button" disabled="disabled">
                                    <i class="trash alternate icon"></i>
                                </button>
                            </span>
                        </g:else>

                    </g:if>
                </td>
            </tr>
        </g:each>
    </tbody>
</table>

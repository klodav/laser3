<%@ page import="com.k_int.kbplus.Doc" %>
<semui:card message="license.notes" class="card-grey notes">

        <g:each in="${ownobj.documents}" var="docctx">
            <g:if test="${((docctx.owner?.contentType == Doc.CONTENT_TYPE_STRING) && !(docctx.domain) && (docctx.status?.value != 'Deleted') )}">
                <div class="ui small feed">
                    <!--<div class="event">-->
                        <div class="content">
                            <div class="summary">
                                <g:if test="${docctx.owner.title}">
                                    <g:link controller="doc" action="show" id="${docctx.owner.id}">${docctx.owner.title}</g:link>
                                </g:if>
                                <g:else>
                                    <g:link controller="doc" action="show" id="${docctx.owner.id}">Ohne Titel</g:link>
                                </g:else>
                                <br/>

                                ${message(code:'template.notes.created')}
                                <g:formatDate format="${message(code:'default.date.format.notime', default:'yyyy-MM-dd')}" date="${docctx.owner.dateCreated}"/>

                                <g:if test="${docctx.alert}">
                                    ${message(code:'template.notes.shared')} ${docctx.alert.createdBy.displayName}
                                    <g:if test="${docctx.alert.sharingLevel == 1}">
                                        ${message(code:'template.notes.shared_jc')}
                                    </g:if>
                                    <g:if test="${docctx.alert.sharingLevel == 2}">
                                        ${message(code:'template.notes.shared_community')}
                                    </g:if>
                                    <div class="comments">
                                        <a href="#modalComments" class="announce" data-id="${docctx.alert.id}">
                                            ${docctx.alert?.comments != null ? docctx.alert?.comments?.size() : 0} Comment(s)
                                        </a>
                                    </div>
                                </g:if>
                                <g:else>
                                    ${message(code:'template.notes.not_shared')}
                                </g:else>
                            </div>
                        </div>
                    <!--</div>-->
                </div>
            </g:if>
        </g:each>

    <g:if test="${editable}">
        </div>
        <div class="extra content">
            <input type="submit" class="ui button" value="${message(code:'default.button.create_new.label')}" data-semui="modal" href="#modalCreateNote" />
    </g:if>
</semui:card>

<g:render template="/templates/notes/modal" />

<div class="modal hide fade" id="modalComments"></div>
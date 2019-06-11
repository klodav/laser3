<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils; com.k_int.kbplus.*;de.laser.helper.RDStore;" %>
<laser:serviceInjection/>
<%
    List<DocContext> baseItems = []
    List<DocContext> sharedItems = []

    ownobj.documents.sort{it.owner?.title}.each{ it ->
        if (it.sharedFrom) {
            sharedItems << it
        }
        else {
            baseItems << it
        }
    }

    String documentMessage
    switch(ownobj.class.name) {
        case Org.class.name: documentMessage = "menu.my.documents"
            editable = accessService.checkMinUserOrgRole(user, contextService.org, 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_ORG_EDITOR')
            break
        default: documentMessage = "license.documents"
            break
    }

    boolean editable2 = accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR")
    //println "EDITABLE: ${editable}"
    //println "EDITABLE2: ${editable2}"
%>
<semui:card message="${documentMessage}" class="documents la-js-hideable ${css_class}" href="#modalCreateDocument" editable="${editable || editable2}">
    <g:each in="${baseItems}" var="docctx">
        <%
            boolean visible = false
            if(docctx.org) {
                boolean inOwnerOrg = false
                boolean isCreator = false

                if(docctx.owner.owner?.id == contextService.org.id)
                    inOwnerOrg = true
                if(docctx.owner.creator?.id == user.id)
                    isCreator = true

                switch(docctx.shareConf) {
                    case RDStore.SHARE_CONF_CREATOR: if(isCreator) visible = true
                        break
                    case RDStore.SHARE_CONF_UPLOADER_ORG: if(inOwnerOrg) visible = true
                        break
                /*case RDStore.SHARE_CONF_UPLOADER_AND_TARGET: if(inOwnerOrg || contextService.org.id == docctx.org?.id) visible = true
                    break*/
                    case RDStore.SHARE_CONF_CONSORTIUM:
                    case RDStore.SHARE_CONF_ALL: visible = true //definition says that everyone with "access" to target org. How are such access roles defined and where?
                        break
                    default:
                        if(docctx.shareConf) log.debug(docctx.shareConf)
                        else visible = true
                        break
                }
            }
            else visible = true
        %>
        <g:if test="${(( (docctx.owner?.contentType==1) || ( docctx.owner?.contentType==3) ) && ( docctx.status?.value!='Deleted') && visible)}">
            <div class="ui small feed content la-js-dont-hide-this-card">
                <div class="ui grid summary">
                    <div class="twelve wide column">
                        <g:link controller="docstore" id="${docctx.owner.uuid}" class="js-no-wait-wheel">
                            <g:if test="${docctx.owner?.title}">
                                ${docctx.owner.title}
                            </g:if>
                            <g:elseif test="${docctx.owner?.filename}">
                                ${docctx.owner.filename}
                            </g:elseif>
                            <g:else>
                                ${message(code:'template.documents.missing', default: 'Missing title and filename')}
                            </g:else>

                        </g:link>(${docctx.owner?.type?.getI10n("value")})
                    </div>
                    <div class="center aligned four wide column la-js-editmode-container">
                        <g:if test="${!(ownobj instanceof Org) && ownobj.showUIShareButton()}">
                            <g:if test="${docctx.isShared}">
                                <laser:remoteLink class="ui mini icon green button js-no-wait-wheel la-popup-tooltip la-delay"
                                                  controller="ajax"
                                                  action="toggleShare"
                                                  params='[owner:"${ownobj.class.name}:${ownobj.id}", sharedObject:"${docctx.class.name}:${docctx.id}", tmpl:"documents"]'
                                                  data-content="${message(code:'property.share.tooltip.on')}"
                                                  data-done=""
                                                  data-always="bb8.init('#container-documents')"
                                                  data-update="container-documents"
                                >
                                    <i class="icon la-share la-js-editmode-icon"></i>
                                </laser:remoteLink>

                            </g:if>
                            <g:else>
                                <laser:remoteLink class="ui mini icon button js-no-wait-wheel la-popup-tooltip la-delay js-open-confirm-modal"
                                                  controller="ajax"
                                                  action="toggleShare"
                                                  params='[owner:"${ownobj.class.name}:${ownobj.id}", sharedObject:"${docctx.class.name}:${docctx.id}", tmpl:"documents"]'
                                                  data-content="${message(code:'property.share.tooltip.off')}"
                                                  data-confirm-term-what="element"
                                                  data-confirm-term-what-detail="${docctx.owner.title}"
                                                  data-confirm-term-where="member"
                                                  data-confirm-term-how="share"
                                                  data-done=""
                                                  data-always="bb8.init('#container-documents')"
                                                  data-update="container-documents"
                                >
                                    <i class="la-share slash icon la-js-editmode-icon"></i>
                                </laser:remoteLink>
                            </g:else>
                        </g:if>
                    </div>
                </div>
            </div>
        </g:if>
    </g:each>
</semui:card>

<g:if test="${sharedItems}">
    <semui:card message="license.documents.shared" class="documents la-js-hideable ${css_class}" editable="${editable}">
        <g:each in="${sharedItems}" var="docctx">
            <g:if test="${(( (docctx.owner?.contentType==1) || ( docctx.owner?.contentType==3) ) && ( docctx.status?.value!='Deleted'))}">
                <div class="ui small feed content la-js-dont-hide-this-card">

                    <div class="summary">
                        <g:link controller="docstore" id="${docctx.owner.uuid}" class="js-no-wait-wheel">
                            <g:if test="${docctx.owner?.title}">
                                ${docctx.owner.title}
                            </g:if>
                            <g:elseif test="${docctx.owner?.filename}">
                                ${docctx.owner.filename}
                            </g:elseif>
                            <g:else>
                                ${message(code:'template.documents.missing', default: 'Missing title and filename')}
                            </g:else>

                        </g:link>(${docctx.owner?.type?.getI10n("value")})
                    </div>
                </div>
            </g:if>

        </g:each>
    </semui:card>
</g:if>

<script>
    $( document ).ready(function() {
        if (r2d2) {
            r2d2.initDynamicSemuiStuff('#container-documents');
        }


    });

</script>

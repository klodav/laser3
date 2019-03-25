<%@ page import="com.k_int.kbplus.IssueEntitlement; com.k_int.kbplus.SubscriptionDetailsController; de.laser.helper.RDStore; com.k_int.kbplus.Person; com.k_int.kbplus.Subscription; com.k_int.kbplus.GenericOIDService "%>
<%@ page import="com.k_int.kbplus.SubscriptionDetailsController" %>
<%@ page import="static com.k_int.kbplus.SubscriptionDetailsController.COPY" %>
<%@ page import="static com.k_int.kbplus.SubscriptionDetailsController.REPLACE" %>
<%@ page import="static com.k_int.kbplus.SubscriptionDetailsController.DO_NOTHING" %>
<laser:serviceInjection />

<semui:form>
    <g:render template="selectSourceAndTargetSubscription" model="[
            sourceSubscription: sourceSubscription,
            targetSubscription: targetSubscription,
            allSubscriptions_readRights: allSubscriptions_readRights,
            allSubscriptions_writeRights: allSubscriptions_writeRights]"/>

    <g:form action="copyElementsIntoSubscription" controller="subscriptionDetails" id="${params.id}"
            params="[workFlowPart: workFlowPart, sourceSubscriptionId: sourceSubscriptionId, targetSubscriptionId: targetSubscription?.id]" method="post" class="ui form newLicence">
        <table class="ui celled table table-tworow la-table">
            <thead>
                <tr>
                    <th class="center aligned">${message(code: 'default.copy.label')}</th>
                    <th class="center aligned">${message(code: 'default.replace.label')}</th>
                    <th class="center aligned">${message(code: 'default.doNothing.label')}</th>
                    <th class="six wide">
                        <g:if test="${sourceSubscription}"><g:link controller="subscriptionDetails" action="show" id="${sourceSubscription?.id}">${sourceSubscription?.name}</g:link></g:if>
                    </th>
                    <th class="six wide">
                        <g:if test="${targetSubscription}"><g:link controller="subscriptionDetails" action="show" id="${targetSubscription?.id}">${targetSubscription?.name}</g:link></g:if>
                    </th>
                </tr>
            </thead>
            <tbody>
            <tr>
                <td class="center aligned" style="vertical-align: top"><div class="ui radio checkbox la-toggle-radio la-append"><input type="radio" name="subscription.takePackages" value="${COPY}" /></div></td>
                <td class="center aligned" style="vertical-align: top"><div class="ui radio checkbox la-toggle-radio la-replace">
                    <input type="radio" name="subscription.takePackages" value="${REPLACE}" data-pkgIds="${targetSubscription?.packages?.collect {it.pkgId}?.join(',')}"/></div></td>
                <td class="center aligned" style="vertical-align: top"><div class="ui radio checkbox la-toggle-radio la-noChange"><input type="radio" name="subscription.takePackages" value="${DO_NOTHING}" checked /></div></td>
                <td style="vertical-align: top" name="subscription.takePackages.source">
                    <b><i class="gift icon"></i>&nbsp${message(code: 'subscription.packages.label')}: ${sourceSubscription?.packages?.size()}</b>
                    <g:each in="${sourceSubscription?.packages?.sort { it.pkg?.name }}" var="sp">
                        <div data-pckOid="${genericOIDService.getOID(sp.pkg)}">
                            <div class="ui checkbox">
                                <g:checkBox name="subscription.takePackageIds" value="${genericOIDService.getOID(sp.pkg)}" data-pkgId="${sp.pkg?.id}" checked="false"/>
                                <label>
                                    <g:link controller="package" action="show" target="_blank" id="${sp.pkg?.id}">${sp?.pkg?.name}</g:link>
                                    <semui:debugInfo>PkgId: ${sp.pkg?.id}</semui:debugInfo>
                                    <g:if test="${sp.pkg?.contentProvider}">(${sp.pkg?.contentProvider?.name})</g:if>
                                </label>
                            </div>

                        </div>
                    </g:each>
                </td>
                <td style="vertical-align: top" name="subscription.takePackages.target">
                    <b><i class="gift icon"></i>&nbsp${message(code: 'subscription.packages.label')}: ${targetSubscription?.packages?.size()}</b>
                    <div>
                        <g:each in="${targetSubscription?.packages?.sort { it.pkg?.name }}" var="sp">
                            <g:link controller="packageDetails" action="show" target="_blank" id="${sp.pkg?.id}">${sp?.pkg?.name}</g:link>
                            <semui:debugInfo>PkgId: ${sp.pkg?.id}</semui:debugInfo>
                            <g:if test="${sp.pkg?.contentProvider}">(${sp.pkg?.contentProvider?.name})</g:if>
                            <br>
                        </g:each>
                    </div>
                </td>
            </tr>
            <tr>
                <td class="center aligned" style="vertical-align: top"><div class="ui radio checkbox la-toggle-radio la-append"><input type="radio" name="subscription.takeEntitlements" value="${COPY}" /></div></td>
                <td class="center aligned" style="vertical-align: top"><div class="ui radio checkbox la-toggle-radio la-replace"><input type="radio" name="subscription.takeEntitlements" value="${REPLACE}" /></div></td>
                <td class="center aligned" style="vertical-align: top"><div class="ui radio checkbox la-toggle-radio la-noChange"><input type="radio" name="subscription.takeEntitlements" value="${DO_NOTHING}" checked /></div></td>
                <td style="vertical-align: top" name="subscription.takeEntitlements.source">
                    <b><i class="newspaper icon"></i>&nbsp${message(code: 'issueEntitlement.countSubscription')} </b>${sourceSubscription? sourceIEs?.size() : ""}<br>
                    <g:each in="${sourceIEs}" var="ie">
                        <div data-ieOid="${genericOIDService.getOID(ie)}">
                            <div class="ui checkbox">
                                <g:checkBox name="subscription.takeEntitlementIds" value="${genericOIDService.getOID(ie)}" checked="false"/>&nbsp
                                <label>
                                    <semui:listIcon type="${ie.tipp.title.type.getI10n('value')}"/>&nbsp
                                    <strong><g:link controller="title" action="show" id="${ie?.tipp.title.id}">${ie.tipp.title.title}</g:link></strong>
                                    <semui:debugInfo>Tipp PkgId: ${ie.tipp.pkg.id}, Tipp ID: ${ie.tipp.id}</semui:debugInfo>
                                </label>
                            </div>
                        </div>
                    </g:each>
                </td>
                <td style="vertical-align: top" name="subscription.takeEntitlements.target">
                    <b><i class="newspaper icon"></i>&nbsp${message(code: 'issueEntitlement.countSubscription')} </b>${targetSubscription? targetIEs?.size(): ""} <br />
                    <g:each in="${targetIEs}" var="ie">
                        <div data-pkgId="${ie?.tipp?.pkg?.id}">
                            <semui:listIcon type="${ie.tipp.title.type.getI10n('value')}"/>
                            <strong><g:link controller="title" action="show" id="${ie?.tipp.title.id}">${ie.tipp.title.title}</g:link></strong>
                            <semui:debugInfo>Tipp PkgId: ${ie.tipp.pkg.id}, Tipp ID: ${ie.tipp.id}</semui:debugInfo>
                        </div>
                    </g:each>
                </td>
            </tr>
            </tbody>
        </table>
        <input type="submit" class="ui button js-click-control" value="Ausgewählte Elemente kopieren/überschreiben" />
    </g:form>
</semui:form>

<r:script>
    // $('input:checkbox').change( function(event) {
    //     // TODO Logik überprüfen, wann gewarnt werden soll!
    //     if (this.checked) {
    //         var pkgId = $(this).attr('data-pkgId');
    //         $('.table tr div[data-pkgId="' + pkgId + '"]').addClass('willBeReplaced')
    //     } else {
    //         var pkgId = $(this).attr('data-pkgId');
    //         $('.table tr div[data-pkgId="' + pkgId + '"]').removeClass('willBeReplaced')
    //     }
    // })

    %{--$('input:checkbox').change( function(event) {--}%
    // FUNKTIONIERT
    $('input:radio[name="subscription.takePackages"]').change( function(event) {
        if (this.checked && this.value=='COPY') {
            $('.table tr td[name="subscription.takePackages.target"] div').addClass('willStay')
            $('.table tr td[name="subscription.takePackages.target"] div').removeClass('willBeReplaced')
        }
        if (this.checked && this.value=='REPLACE') {
            $('.table tr td[name="subscription.takePackages.target"] div').addClass('willBeReplaced')
            // TODO: Was ist wenn Lizenz-Hinzufügen/-Ersetzen gleichzeitig ausgewählt ist?
            var pkgIds = $(this).attr('data-pkgIds')
            if (pkgIds != null) {
                pkgIds = pkgIds.split(",");
                for (var i = 0; i<pkgIds.length; i++){
                    $('.table tr div[data-pkgId="' + pkgIds[i] + '"]').addClass('willBeReplaced')
                }
            }
        }
        if (this.checked && this.value=='DO_NOTHING') {
            $('.table tr td[name="subscription.takePackages.source"] div').removeClass('willStay')
            $('.table tr td[name="subscription.takePackages.target"] div').removeClass('willStay')
            $('.table tr td[name="subscription.takePackages.target"] div').removeClass('willBeReplaced')
        }
    })
    $('input[name="subscription.takePackageIds"]').change( function(event) {
        var pckOId = this.value
        if (this.checked) {
            $('.table tr td[name="subscription.takePackages.source"] div[data-pckOid="' + pckOId + '"]').addClass('willStay')
        } else {
            $('.table tr td[name="subscription.takePackages.source"] div[data-pckOid="' + pckOId + '"]').removeClass('willStay')
        }
        // $('.table tr div[data-pkgId="' + pkgId + '"]').addClass('willBeReplaced')
    })
    $('input:radio[name="subscription.takeEntitlements"]').change( function(event) {
        if (this.checked && this.value=='COPY') {
            $('.table tr td[name="subscription.takeEntitlements.target"] div').addClass('willStay')
            $('.table tr td[name="subscription.takeEntitlements.target"] div').removeClass('willBeReplaced')
        }
        if (this.checked && this.value=='REPLACE') {
            $('.table tr td[name="subscription.takeEntitlements.target"] div').addClass('willBeReplaced')
        }
        if (this.checked && this.value=='DO_NOTHING') {
            $('.table tr td[name="subscription.takeEntitlements.source"] div').removeClass('willStay')
            $('.table tr td[name="subscription.takeEntitlements.target"] div').removeClass('willStay')
            $('.table tr td[name="subscription.takeEntitlements.target"] div').removeClass('willBeReplaced')
        }
    })
    $('input[name="subscription.takeEntitlementIds"]').change( function(event) {
        var pckOId = this.value
        if (this.checked) {
            $('.table tr td[name="subscription.takeEntitlements.source"] div[data-ieOid="' + pckOId + '"]').addClass('willStay')
        } else {
            $('.table tr td[name="subscription.takeEntitlements.source"] div[data-ieOid="' + pckOId + '"]').removeClass('willStay')
        }
    })
</r:script>



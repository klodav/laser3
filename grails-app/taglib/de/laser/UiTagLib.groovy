package de.laser


import de.laser.auth.User
import de.laser.cache.SessionCacheWrapper
import de.laser.remote.ApiSource
import de.laser.storage.BeanStore
import de.laser.storage.RDConstants
import de.laser.storage.RDStore
import de.laser.utils.DateUtils
import de.laser.utils.LocaleUtils
import de.laser.utils.SwissKnife
import org.grails.encoder.CodecLookup
import org.grails.encoder.Encoder
import org.grails.taglib.TagLibraryLookup
import org.grails.taglib.TagOutput
import org.grails.taglib.encoder.OutputContextLookupHelper
import org.springframework.context.MessageSource
import org.springframework.web.servlet.support.RequestContextUtils

import java.text.SimpleDateFormat

// Semantic UI

class UiTagLib {

    AuditService auditService
    CodecLookup codecLookup
    ContextService contextService
    GenericOIDService genericOIDService
    GokbService gokbService
    SystemService systemService
    TagLibraryLookup gspTagLibraryLookup
    YodaService yodaService

    static namespace = 'ui'

    // <ui:h1HeaderWithIcon text="${text}" message="18n.token" args="[key:value]" type="${headerTitleIconType}" total="${totalNumber}" floated="true">
    //    content
    // </ui:headerWithIcon>

    def h1HeaderWithIcon = { attrs, body ->
        if (attrs.floated && attrs.floated != 'false') {
            out << '<h1 class="ui icon header la-clear-before left floated aligned">'
        } else {
            out << '<h1 class="ui icon header la-clear-before la-noMargin-top">'
        }

        if (attrs.type) {
            out << ui.headerTitleIcon([type: attrs.type])
        } else {
            out << ui.headerIcon()
        }

        if (attrs.text) {
            out << attrs.text
        }
        if (attrs.message) {
            SwissKnife.checkMessageKey(attrs.message as String)
            out << "${message(code: attrs.message, args: attrs.args)}"
        }

        if (attrs.total) {
            out << ui.totalNumber([total: attrs.total])
        }
        if ( body ) {
            out << body()
        }
        out << '</h1>'
    }

    // <ui:messages data="${flash}" />

    def messages = { attrs, body ->

        def flash = attrs.data

        if (flash && flash.message) {
            out << '<div class="ui success message la-clear-before">'
            out << '<i aria-hidden="true" class="close icon"></i>'
            out << '<p>' + flash.message + '</p>'
            out << '</div>'
        }

        if (flash && flash.error) {
            out << '<div class="ui negative message la-clear-before">'
            out << '<i aria-hidden="true" class="close icon"></i>'
            out << '<p>' + flash.error + '</p>'
            out << '</div>'
        }
    }

    // <ui:msg class="negative|positive|warning|.." icon="${icon}" header="${text}" text="${text}" message="18n.token" noClose="true" />

    def msg = { attrs, body ->

        out << '<div class="ui ' + attrs.class + ' message ' + (attrs.icon ? 'icon ' : '') + 'la-clear-before">'

        if (! attrs.noClose) {
            out << '<i aria-hidden="true" class="close icon"></i>'
        }
        if (attrs.icon) {
            out << '<i class="icon ' + attrs.icon + '"></i>'
        }
        if (attrs.header) {
            out << '<div class="header">'
            out << attrs.header
            out << '</div>'
        }

        out << '<p>'

        if (attrs.text) {
            out << attrs.text
        }
        if (attrs.message) {
            SwissKnife.checkMessageKey(attrs.message as String)

            out << "${message(code: attrs.message, args: attrs.args)}"
        }
        if ( body ) {
            out << body()
        }

        out << '</p>'
        out << '</div>'
    }

    // <ui:errors bean="${instanceOfObject}" />

    def errors = { attrs, body ->

        if (attrs.bean?.errors?.allErrors) {
            out << '<div class="ui negative message">'
            out << '<i aria-hidden="true" class="close icon"></i>'
            out << '<ul class="list">'
            attrs.bean.errors.allErrors.each { e ->
                if (e in org.springframework.validation.FieldError) {
                    out << '<li data-field-id="${error.field}">'
                } else {
                    out << '<li>'
                }
                out << g.message(error: "${e}") + '</li>'
            }
            out << '</ul>'
            out << '</div>'
        }
    }

    // <ui:objectStatus object="${obj}" status="${status}"/>

    def objectStatus = { attrs, body ->

        if ('deleted'.equalsIgnoreCase(attrs.status?.value)) {

            out << '<div class="ui segment inverted red">'
            out << '<p><strong>' + message(code: 'default.object.isDeleted') + '</strong></p>'
            out << '</div>'
        }
    }

    // <ui:card text="${text}" message="local.string" class="some_css_class">
    // </ui:card>

    def card = { attrs, body ->
        def (text, message) = SwissKnife.getTextAndMessage(attrs)
        String title = (text && message) ? text + " - " + message : text + message

        out << '<div class="ui card ' + attrs.class + '">'
        out << '    <div class="content">'

        if (title) {
            out << '    <div class="header">'
            out << '        <div class="ui grid">'
            out << '            <div class="twelve wide column">'
            out <<                title
            out << '            </div>'
            if (attrs.editable && attrs.href) {
                out << '        <div class="right aligned four wide column">'
                out << '            <button type="button" class="ui icon button blue la-modern-button editable-cancel" data-ui="modal" data-href="' + attrs.href + '" ><i aria-hidden="true" class="plus icon"></i></button>'
                out << '        </div>'
            }
            out << '        </div>'
            out << '   </div>'

        }
        out << body()

        out << '    </div>'
        out << '</div>'
    }

    def debugInfo = { attrs, body ->

        if (yodaService.showDebugInfo()) {

            out << '<a href="#debugInfo" id="showDebugInfo" role="dialog" aria-label="Debug Info" class="ui button icon" data-ui="modal">'
            out << '<i aria-hidden="true" class="red bug icon"></i>'
            out << '</a>'

            out << '<div id="debugInfo" class="ui modal">'
            out << '<h4 class="ui red header"> <i aria-hidden="true" class="bug icon"></i> DEBUG-INFORMATION</h4>'
            out << '<div class="scrolling content">'
            out << body()
            out << '<br />'
            out << '</div>'
            out << '<div class="actions">'
            out << '<a href="#" class="ui button" onclick="$(\'#debugInfo\').modal(\'hide\')">Schlie??en</a>'
            out << '</div>'
            out << '</div>'
        }
    }

    def systemInfo = { attrs, body ->

        Map<String, Object> systemChecks = systemService.serviceCheck()

        if (systemChecks) {

            out << '<a href="#systemInfo" id="showSystemInfo" role="dialog" aria-label="System Info" class="ui button icon" data-ui="modal">'
            out << '<i aria-hidden="true" class="red exclamation triangle icon"></i>'
            out << '</a>'

            out << '<div id="systemInfo" class="ui modal">'
            out << '<h4 class="ui red header"> <i aria-hidden="true" class="red exclamation triangle icon"></i> SYSTEM-INFORMATION</h4>'
            out << '<div class="scrolling content">'
            out << '<div class="ui list">'
            systemChecks.each {systemCheck ->
                out << '<div class="item">'
                out << "<strong>${systemCheck.key}</strong>: ${systemCheck.value}"
                out << '</div>'
            }
            out << '</div>'
            out << '<br />'
            out << '</div>'
            out << '<div class="actions">'
            out << '<a href="#" class="ui button" onclick="$(\'#systemInfo\').modal(\'hide\')">Schlie??en</a>'
            out << '</div>'
            out << '</div>'
        }
    }

    def auditButton = { attrs, body ->

        if (attrs.auditable) {
            try {
                def obj = attrs.auditable[0]
                def objAttr = attrs.auditable[1]
                boolean hasAuditConfig

                if (obj?.getLogIncluded()?.contains(objAttr)) {

                    // inherited (to)
                    if (obj.instanceOf) {
                        if(attrs.auditConfigs)
                            hasAuditConfig = attrs.auditConfigs[objAttr]
                        else hasAuditConfig = auditService.getAuditConfig(obj.instanceOf, objAttr)
                        if (hasAuditConfig) {
                            if (obj.isSlaved) {
                                out << '<span class="la-popup-tooltip la-delay" data-content="Wert wird automatisch geerbt" data-position="top right">'
                                out << '<i aria-hidden="true" class="icon grey la-thumbtack-regular"></i>'
                                out << '</span>'
                            }
                            else {
                                out << '<span class="la-popup-tooltip la-delay" data-content="Wert wird geerbt" data-position="top right">'
                                out << '<i aria-hidden="true" class="icon thumbtack grey"></i>'
                                out << '</span>'
                            }
                        }
                    }
                    // inherit (from)
                    else if (obj?.showUIShareButton()) {
                        String oid = genericOIDService.getOID(obj)
                        if(attrs.auditConfigs)
                            hasAuditConfig = attrs.auditConfigs[objAttr]
                        else hasAuditConfig = auditService.getAuditConfig(obj, objAttr)

                        if (hasAuditConfig) {
                            if(attrs.withoutOptions) {
                                out << '<a role="button" data-content="Wert wird vererbt" class="ui icon green button la-modern-button ' + attrs.class + ' la-audit-button la-popup-tooltip la-delay" href="'
                                out << g.createLink(
                                        controller: 'ajax',
                                        action: 'toggleAudit',
                                        params: ['owner': oid, 'property': [objAttr], keep: true],
                                )
                                out << '">'
                                out << '<i aria-hidden="true" class="icon la-js-editmode-icon thumbtack"></i>'
                                out << '</a>'
                            }
                            else {
                                out << '<div class="ui simple dropdown icon green button la-modern-button ' + attrs.class + ' la-audit-button" data-content="Wert wird vererbt">'
                                out   << '<i aria-hidden="true" class="icon la-js-editmode-icon thumbtack"></i>'
                                out   << '<div class="menu">'
                                out << g.link( 'Vererbung deaktivieren. Wert f??r Teilnehmer <strong>l??schen</strong>',
                                        controller: 'ajax',
                                        action: 'toggleAudit',
                                        params: ['owner': oid, 'property': [objAttr]],
                                        class: 'item'
                                )
                                out << g.link( 'Vererbung deaktivieren. Wert f??r Teilnehmer <strong>erhalten</strong>',
                                        controller: 'ajax',
                                        action: 'toggleAudit',
                                        params: ['owner': oid, 'property': [objAttr], keep: true],
                                        class: 'item'
                                )
                                out   << '</div>'
                                out << '</div>'
                            }
                        }
                        else {
                            out << '<a role="button" data-content="Wert wird nicht vererbt" class="ui icon blue button la-modern-button ' + attrs.class + ' la-audit-button la-popup-tooltip la-delay" href="'
                            out << g.createLink(
                                    controller: 'ajax',
                                    action: 'toggleAudit',
                                    params: ['owner': oid, 'property': [objAttr]],
                            )
                            out << '">'
                            out << '<i aria-hidden="true" class="icon la-js-editmode-icon la-thumbtack slash"></i>'
                            out << '</a>'
                        }
                    }
                }

            } catch (Exception e) {
            }
        }
    }

    def auditInfo = { attrs, body ->

        if (attrs.auditable) {
            try {
                def obj = attrs.auditable[0]
                def objAttr = attrs.auditable[1]

                if (obj?.getLogIncluded()?.contains(objAttr)) {

                    // inherited (to)
                    if (obj.instanceOf) {

                        if (auditService.getAuditConfig(obj.instanceOf, objAttr)) {
                            if (obj.isSlaved) {
                                out << '<span class="la-popup-tooltip la-delay" data-content="Wert wird automatisch geerbt" data-position="top right">'
                                out << '<i aria-hidden="true" class="icon grey la-thumbtack-regular"></i>'
                                out << '</span>'
                            }
                            else {
                                out << '<span class="la-popup-tooltip la-delay" data-content="Wert wird geerbt" data-position="top right">'
                                out << '<i aria-hidden="true" class="icon thumbtack grey"></i>'
                                out << '</span>'
                            }
                        }
                    }
                    // inherit (from)
                    else if (obj?.showUIShareButton()) {
                        String oid = "${obj.getClass().getName()}:${obj.getId()}"

                        if (auditService.getAuditConfig(obj, objAttr)) {

                            if (obj.isSlaved) {
                                out << '<span class="la-popup-tooltip la-delay" data-content="Wert wird automatisch geerbt" data-position="top right">'
                                out << '<i aria-hidden="true" class="icon grey la-thumbtack-regular"></i>'
                                out << '</span>'
                            }
                            else {
                                out << '<span class="la-popup-tooltip la-delay" data-content="Wert wird geerbt" data-position="top right">'
                                out << '<i aria-hidden="true" class="icon thumbtack grey"></i>'
                                out << '</span>'
                            }
                        }
                        else {
                            out << '<span class="la-popup-tooltip la-delay" data-content="Wert wird nicht vererbt" data-position="top right">'
                            out << '<i aria-hidden="true" class="icon la-thumbtack slash"></i>'
                            out << '</span>'
                        }
                    }
                }

            } catch (Exception e) {
            }
        }
    }

    // <ui:modeSwitch controller="controller" action="action" params="params" />

    def modeSwitch = { attrs, body ->

        //return;
        String mode = (attrs.params.mode == 'basic') ? 'basic' : ((attrs.params.mode == 'advanced') ? 'advanced' : null)

        if (!mode) {
            User user = contextService.getUser()
            mode = (user.getSettingsValue(UserSetting.KEYS.SHOW_SIMPLE_VIEWS)?.value == 'No') ? 'advanced' : 'basic'

            // CAUTION: inject default mode
            attrs.params.mode = mode
        }
    }

    //<ui:filter showFilterButton="true|false" addFilterJs="true" extended="true|false"> CONTENT <ui:filter>

    def filter = { attrs, body ->

        boolean extended = true
        boolean showFilterButton = false

        if (attrs.showFilterButton) {
            if (attrs.showFilterButton.toLowerCase() == 'true') {
                showFilterButton = true
            }
            else if (attrs.showFilterButton.toLowerCase() == 'false') {
                showFilterButton = false
            }
        }

        if (showFilterButton) {

			// overwrite due attribute
            if (attrs.extended) {
                if (attrs.extended.toLowerCase() == 'true') {
                    extended = true
                } else if (attrs.extended.toLowerCase() == 'false') {
                    extended = false
                }
            }
            else {
				// overwrite due session
                SessionCacheWrapper sessionCache = contextService.getSessionCache()
                def cacheEntry = sessionCache.get("${UserSetting.KEYS.SHOW_EXTENDED_FILTER.toString()}/${controllerName}/${actionName}")

                if (cacheEntry) {
                    if (cacheEntry.toLowerCase() == 'true') {
                        extended = true
                    } else if (cacheEntry.toLowerCase() == 'false') {
                        extended = false
                    }
                }
				// default profile setting
                else {
                    User currentUser = contextService.getUser()
                    String settingValue = currentUser.getSettingsValue(UserSetting.KEYS.SHOW_EXTENDED_FILTER, RefdataValue.getByValueAndCategory('Yes', RDConstants.Y_N)).value

                    if (settingValue.toLowerCase() == 'yes') {
                        extended = true
                    } else if (settingValue.toLowerCase() == 'no') {
                        extended = false
                    }
                }
            }
        }
        // for WCAG
        out << '<section class="la-clearfix" aria-label="filter">'
            if (showFilterButton) {
                out << '<button aria-expanded="' + (extended ?'true':'false')  + '"  class="ui right floated button la-inline-labeled la-js-filterButton la-clearfix ' + (extended ?'':'blue') + '">'
                out << '    Filter'
                out << '    <i aria-hidden="true" class="filter icon"></i>'
                out << '   <span class="ui circular label la-js-filter-total hidden">0</span>'
                out << '</button>'
            }

            out << '<div class="ui la-filter segment la-clear-before"' + (extended ?'':' style="display: none;"') + '>'
            out << body()
            out << '</div>'
        out << '</section>'

        if (attrs.addFilterJs) {
            out << render(template: '/templates/filter/js', model: [filterAjaxUri: "${controllerName}/${actionName}"])
        }
    }

    def searchSegment = { attrs, body ->

        String method = attrs.method ?: 'GET'
        String controller = attrs.controller ?: ''
        String action = attrs.action ?: ''

        out << '<div class="ui la-search segment">'
        out << '<form class="ui form" controller="' + controller + '" action="' + action + '" method="' + method + '">'
        out << body()
        out << '</form>'
        out << '</div>'
    }

    //<ui:form> CONTENT <ui:form>

    def form = { attrs, body ->

        out << '<div class="ui grey segment la-clear-before">'
        out << body()
        out << '</div>'
    }

    //<ui:modal id="myModalDialog" text="${text}" message="local.string" hideSubmitButton="true" modalSize="large/small/tiny/mini" >
    // CONTENT
    // </ui:modal>

    def modal = { attrs, body ->

        def (text, message) = SwissKnife.getTextAndMessage(attrs)
        String id           = attrs.id ? ' id="' + attrs.id + '" ' : ''
        String modalSize    = attrs.modalSize ? attrs.modalSize  : ''
        String title        = (text && message) ? text + " - " + message : text + message
        String isEditModal  = attrs.isEditModal

        String msgClose    = attrs.msgClose  ?: "${g.message(code:'default.button.close.label')}"
        String msgSave     = attrs.msgSave   ?: (isEditModal ? "${g.message(code:'default.button.save_changes')}" : "${g.message(code:'default.button.create.label')}")
        String msgDelete   = attrs.msgDelete ?: "${g.message(code:'default.button.delete.label')}"

        out << '<div role="dialog" class="ui large modal ' + modalSize + '"' + id + ' aria-label="Modal">'
        out << '<div class="header">' + title + '</div>'

        if (attrs.contentClass) {
            out << '<div class="content ' + attrs.contentClass + '">'
        } else {
            out << '<div class="content">'
        }

        out << body()
        out << '</div>'
        out << '<div class="actions">'
        out << '<button class="ui button ' + attrs.id + '" onclick="$(\'#' + attrs.id + '\').modal(\'hide\')">' + msgClose + '</button>'

        if (attrs.showDeleteButton) {

            out << '<input type="submit" class="ui negative button" name="delete" value="' + msgDelete + '" onclick="'
            out << "return confirm('${g.message(code:'default.button.delete.confirmDeletion.message')}')?"
            out << '$(\'#' + attrs.id + '\').find(\'#' + attrs.deleteFormID + '\').submit():null'
            out << '"/>'
        }

        if (attrs.hideSubmitButton == null) {
            if (attrs.formID) {
                out << '<input type="submit" class="ui button green" name="save" value="' + msgSave + '" onclick="event.preventDefault(); $(\'#' + attrs.id + '\').find(\'#' + attrs.formID + '\').submit()"/>'
            } else {
                out << '<input type="submit" class="ui button green" name="save" value="' + msgSave + '" onclick="event.preventDefault(); $(\'#' + attrs.id + '\').find(\'form\').submit()"/>'
            }
        }

        out << '</div>'
        out << '</div>'
    }

    //  <ui:infoModal> ${content} <ui:infoModal />

    def infoModal = { attrs, body ->

        String id        = attrs.id ? ' id="' + attrs.id + '" ' : ''
        String modalSize = attrs.modalSize ? attrs.modalSize  : ''
        String msgClose  = attrs.msgClose  ?: "${g.message(code:'default.button.merci.label')}"

        out << '<div role="dialog" class="ui modal ' + modalSize + '"' + id + ' aria-label="Modal">'
        out <<    '<div class="content ui items">'
        out <<       '<div class="item">'
        out <<          '<div class="image"><i class="ui icon huge circular question"></i></div>'
        out <<          '<div class="content">'
        out << body()
        out <<          '</div>'
        out <<       '</div>'
        out <<    '</div>'
        out <<    '<div class="actions">'
        out <<       '<button class="ui button ' + attrs.id + '" onclick="$(\'#' + attrs.id + '\').modal(\'hide\')">' + msgClose + '</button>'
        out <<    '</div>'
        out << '</div>'
    }

    //  <ui:confirmationModal  />
    // global included at semanticUI.gsp
    // called by the specific delete button
    //  - to send a form oridden
    //        <g:form data-confirm-id="${person?.id.toString()+ '_form'}">
    //        <div class="....... js-open-confirm-modal" data-confirm-term-what="diese Person" data-confirm-id="${person?.id}" >
    //  - to call a link
    //        <g:link class="..... js-open-confirm-modal" data-confirm-term-what="diese Kontaktdresse" ...... >
    def confirmationModal = { attrs, body ->
        String msgDelete = "Endg??ltig l??schen"
        String msgCancel = "Abbrechen"

        out << '<div id="js-modal" class="ui tiny modal" role="dialog" aria-modal="true" tabindex="-1" aria-label="'+ "${message(code: 'wcag.label.confirmationModal')}" +'" >'
        out << '<div class="header">'
        out << '<span class="confirmation-term" id="js-confirmation-term"></span>'
        out << '</div>'

        out << '<div class="content confirmation-content" id="js-confirmation-content-term">'
        out << '</div>'

        out << '<div class="actions">'
        out << '<button class="ui deny button">' + msgCancel + '</button>'
        out << '<button id="js-confirmation-button" class="ui positive right labeled icon button">' + msgDelete
        out << '    <i aria-hidden="true" class="trash alternate outline icon"></i>'
        out << '</button>'
        out << '</div>'
        out << '</div>'
    }

    //<ui:datepicker class="grid stuff here" label="" bean="${objInstance}" name="fieldname" value="" required="" modifiers="" />

    def datepicker = { attrs, body ->

        String inputCssClass = attrs.inputCssClass ?: ''
        String label = attrs.label ? "${message(code: attrs.label)}" : '&nbsp'
        String name = attrs.name ? "${message(code: attrs.name)}" : ''
        String id = attrs.id ? "${message(code: attrs.id)}" : ''
        String placeholder = attrs.placeholder ? "${message(code: attrs.placeholder)}" : "${message(code: 'default.date.label')}"

        SimpleDateFormat sdf = DateUtils.getLocalizedSDF_noTime()
        String value = ''
        try {
            value = attrs.value ? sdf.format(attrs.value) : value
        }
        catch (Exception e) {
            value = attrs.value
        }

        String classes    = attrs.containsKey('required') ? 'field required' : 'field'
        String required   = attrs.containsKey('required') ? 'required=""' : ''
        String mandatoryField   = attrs.containsKey('required') ? "${message(code: 'messageRequiredField')}" :""

        boolean hideLabel = attrs.hideLabel ? false : true

        if (attrs.class) {
            classes += ' ' + attrs.class
        }
        // check for field errors
        if (attrs.bean && g.fieldError([bean: attrs.bean, field: "${name}"])) {
            classes += ' error'
        }

        // reporting -->
        if (attrs.modifiers) {
            String modName = name + '_modifier'
            String modValue = params.get(modName) ?: attrs.defaultModifier
            String modIconClass = 'small icon la-equals'

            switch (modValue) {
                case 'less':
                    modIconClass = 'small icon la-less-than'
                    break
                case 'greater':
                    modIconClass = 'small icon la-greater-than'
                    break
                case 'equals':
                    modIconClass = 'small icon la-equals'
                    break
                case 'less-equal':
                    modIconClass = 'small icon la-less-than-equal'
                    break
                case 'greater-equal':
                    modIconClass = 'small icon la-greater-than-equal'
                    break
            }
            out << '<div class="field la-combi-input-left">'
            out <<   '<label for="dateBeforeVal">&nbsp;</label>'
            out <<   '<div class="ui compact selection dropdown la-not-clearable">'
            out <<     '<input type="hidden" name="' + modName + '" value="' + modValue + '">'
            out <<     '<i class="dropdown icon"></i>'
            out <<      '<div class="text"><i class="' + modIconClass + '"></i></div>'
            out <<      '<div class="menu">'
            out <<        '<div class="item' + ( modValue == 'less' ? ' active' : '' ) + '" data-value="less"><i class="la-less-than small icon"></i></div>'
            out <<        '<div class="item' + ( modValue == 'greater' ? ' active' : '' ) + '" data-value="greater"><i class="la-greater-than small icon"></i></div>'
            out <<        '<div class="item' + ( modValue == 'equals' ? ' active' : '' ) + '" data-value="equals"><i class="la-equals small icon"></i></div>'
            out <<        '<div class="item' + ( modValue == 'less-equal' ? ' active' : '' ) + '" data-value="less-equal"><i class="la-less-than-equal small icon"></i></div>'
            out <<        '<div class="item' + ( modValue == 'greater-equal' ? ' active' : '' ) + '" data-value="greater-equal"><i class="la-greater-than-equal small icon"></i></div>'
            out <<     '</div>'
            out <<   '</div>'
            out << '</div>'
        }

        String modClass = attrs.modifiers ? ' la-combi-input-right' : ''

        out << '<div class="' + classes + modClass +'">'
        if (hideLabel) {
            out << '<label for="' + id + '">' + label + ' ' + mandatoryField + '</label>'
        }
        out <<   '<div class="ui calendar datepicker">'
        out <<     '<div class="ui input left icon">'
        out <<       '<i aria-hidden="true" class="calendar icon"></i>'
        out <<       '<input class="' + inputCssClass + '" name="' + name +  '" id="' + id +'" type="text" placeholder="' + placeholder + '" value="' + value + '" ' + required + '>'
        out <<     '</div>'
        out <<   '</div>'

        out << '</div>'
    }

    def anualRings = { attrs, body ->
        def object = attrs.object

        def prev = attrs.navPrev
        def next = attrs.navNext
        def status = object.status?.value
        String color
        String tooltip
        String startDate
        String endDate
        String dash

        String prevStartDate
        String prevEndDate

        String nextStartDate
        String nextEndDate

        if (object.status) {
            tooltip = object.status.getI10n('value')
            switch (object.status) {
                case 'Current': color = 'la-status-active'
                    break
                case 'Expired': color = 'la-status-inactive'
                    break
                default: color = 'la-status-else'
                    break
            }
        } else {
            tooltip = message(code: 'subscription.details.statusNotSet')
        }
        out << "<div class='ui large label la-annual-rings'>"
        if (object.startDate) {
            startDate = g.formatDate(date: object.startDate, format: message(code: 'default.date.format.notime'))
        }
        if (object.endDate) {
            dash = '???'
            endDate = g.formatDate(date: object.endDate, format: message(code: 'default.date.format.notime'))
        }
        if (prev) {
            if (prev?.size() == 1) {
                prev?.each { p ->
                    if (attrs.mapping) {
                        out << g.link("<i class='arrow left icon'></i>", controller: attrs.controller, action: attrs.action, class: "item", params: [sub: p.id], mapping: attrs.mapping)

                    } else {
                        out << g.link("<i class='arrow left icon'></i>", controller: attrs.controller, action: attrs.action, class: "item", id: p.id)
                    }
                }
            } else {

                out << "<div class='ui right pointing dropdown'>" +
                        "<i class='arrow left icon'></i>" +
                        "<div class='menu'>"
                prev?.each { p ->


                    if (p.startDate) {
                        prevStartDate = g.formatDate(date: p.startDate, format: message(code: 'default.date.format.notime'))
                    }
                    if (p.endDate) {
                        prevEndDate = g.formatDate(date: p.endDate, format: message(code: 'default.date.format.notime'))
                    }
                    if (attrs.mapping) {
                        out << g.link("<strong>${p instanceof License ? p.reference : p.name}:</strong> " + "${prevStartDate}" + "${dash}" + "${prevEndDate}", controller: attrs.controller, action: attrs.action, class: "item", params: [sub: p.id], mapping: attrs.mapping)
                    } else {
                        out << g.link("<strong>${p instanceof License ? p.reference : p.name}:</strong> " + "${prevStartDate}" + "${dash}" + "${prevEndDate}", controller: attrs.controller, action: attrs.action, class: "item", id: p.id)
                    }
                }
                out << "</div>" +
                        "</div>"
            }
        } else {
            out << '<i aria-hidden="true" class="arrow left icon disabled"></i>'
        }
        out << "<span class='la-annual-rings-text'>"
        out << startDate
        out << dash
        out << endDate
        out << "</span>"

        out << "<a class='ui ${color} circular tiny label la-popup-tooltip la-delay'  data-variation='tiny' data-content='Status: ${tooltip}'>"
        out << '       &nbsp;'
        out << '</a>'

        if (next) {

            if (next?.size() == 1) {
                next?.each { n ->
                    if (attrs.mapping) {
                        out << g.link("<i class='arrow right icon'></i>", controller: attrs.controller, action: attrs.action, class: "item", params: [sub: n.id], mapping: attrs.mapping)

                    } else {
                        out << g.link("<i class='arrow right icon'></i>", controller: attrs.controller, action: attrs.action, class: "item", id: n.id)
                    }
                }
            } else {
                out << "<div class='ui left pointing dropdown'>" +
                        "<i class='arrow right icon'></i>" +
                        "<div class='menu'>"
                next?.each { n ->

                    if (n.startDate) {
                        nextStartDate = g.formatDate(date: n.startDate, format: message(code: 'default.date.format.notime'))
                    }
                    if (n.endDate) {
                        nextEndDate = g.formatDate(date: n.endDate, format: message(code: 'default.date.format.notime'))
                    }
                    if (attrs.mapping) {
                        out << g.link("<strong>${n instanceof License ? n.reference : n.name}:</strong> " + "${nextStartDate}" + "${dash}" + "${nextEndDate}", controller: attrs.controller, action: attrs.action, class: "item", params: [sub: n.id], mapping: attrs.mapping)
                    } else {
                        out << g.link("<strong>${n instanceof License ? n.reference : n.name}:</strong> " + "${nextStartDate}" + "${dash}" + "${nextEndDate}", controller: attrs.controller, action: attrs.action, class: "item", id: n.id)
                    }
                }
                out << "</div>" +
                        "</div>"
            }
        } else {
            out << '<i aria-hidden="true" class="arrow right icon disabled"></i>'
        }
        out << '</div>'
    }

    def anualRingsModern = { attrs, body ->
        def object = attrs.object

        def prev = attrs.navPrev
        def next = attrs.navNext
        def status = object.status?.value
        def color
        def tooltip
        def startDate
        def endDate
        def dash

        def prevStartDate
        def prevEndDate

        def nextStartDate
        def nextEndDate

        if (object.status) {
            tooltip = object.status.getI10n('value')
            switch (object.status) {
                case 'Current': color = 'la-status-active'
                    break
                case 'Expired': color = 'la-status-inactive'
                    break
                default: color = 'la-status-else'
                    break
            }
        } else {
            tooltip = message(code: 'subscription.details.statusNotSet')
        }
        out << "<div class='ui large label la-annual-rings-modern'>"
        if (object.startDate) {
            startDate = g.formatDate(date: object.startDate, format: message(code: 'default.date.format.notime'))
        }
        if (object.endDate) {
            dash = '???'
            endDate = g.formatDate(date: object.endDate, format: message(code: 'default.date.format.notime'))
        }
        if (prev) {
            if (prev?.size() == 1) {
                prev?.each { p ->
                    if (attrs.mapping) {
                        out << g.link("<i class='arrow left icon'></i>", controller: attrs.controller, action: attrs.action, class: "item", params: [sub: p.id], mapping: attrs.mapping)

                    } else {
                        out << g.link("<i class='arrow left icon'></i>", controller: attrs.controller, action: attrs.action, class: "item", id: p.id)
                    }
                }
            } else {

                out << "<div class='ui right pointing dropdown'>" +
                        "<i class='arrow left icon'></i>" +
                        "<div class='menu'>"
                prev?.each { p ->


                    if (p.startDate) {
                        prevStartDate = g.formatDate(date: p.startDate, format: message(code: 'default.date.format.notime'))
                    }
                    if (p.endDate) {
                        prevEndDate = g.formatDate(date: p.endDate, format: message(code: 'default.date.format.notime'))
                    }
                    if (attrs.mapping) {
                        out << g.link("<strong>${p instanceof License ? p.reference : p.name}:</strong> " + "${prevStartDate}" + "${dash}" + "${prevEndDate}", controller: attrs.controller, action: attrs.action, class: "item", params: [sub: p.id], mapping: attrs.mapping)
                    } else {
                        out << g.link("<strong>${p instanceof License ? p.reference : p.name}:</strong> " + "${prevStartDate}" + "${dash}" + "${prevEndDate}", controller: attrs.controller, action: attrs.action, class: "item", id: p.id)
                    }
                }
                out << "</div>" +
                        "</div>"
            }
        } else {
            out << '<i aria-hidden="true" class="arrow left icon disabled"></i>'
        }
        out << "<span class='la-annual-rings-text'>"
        out << startDate
        out << dash
        out << endDate
        out << "</span>"

        out << "<a class='ui ${color} circular tiny label la-popup-tooltip la-delay'  data-variation='tiny' data-content='Status: ${tooltip}'>"
        out << '       &nbsp;'
        out << '</a>'

        if (next) {

            if (next?.size() == 1) {
                next?.each { n ->
                    if (attrs.mapping) {
                        out << g.link("<i class='arrow right icon'></i>", controller: attrs.controller, action: attrs.action, class: "item", params: [sub: n.id], mapping: attrs.mapping)

                    } else {
                        out << g.link("<i class='arrow right icon'></i>", controller: attrs.controller, action: attrs.action, class: "item", id: n.id)
                    }
                }
            } else {
                out << "<div class='ui left pointing dropdown'>" +
                        "<i class='arrow right icon'></i>" +
                        "<div class='menu'>"
                next?.each { n ->

                    if (n.startDate) {
                        nextStartDate = g.formatDate(date: n.startDate, format: message(code: 'default.date.format.notime'))
                    }
                    if (n.endDate) {
                        nextEndDate = g.formatDate(date: n.endDate, format: message(code: 'default.date.format.notime'))
                    }
                    if (attrs.mapping) {
                        out << g.link("<strong>${n instanceof License ? n.reference : n.name}:</strong> " + "${nextStartDate}" + "${dash}" + "${nextEndDate}", controller: attrs.controller, action: attrs.action, class: "item", params: [sub: n.id], mapping: attrs.mapping)
                    } else {
                        out << g.link("<strong>${n instanceof License ? n.reference : n.name}:</strong> " + "${nextStartDate}" + "${dash}" + "${nextEndDate}", controller: attrs.controller, action: attrs.action, class: "item", id: n.id)
                    }
                }
                out << "</div>" +
                        "</div>"
            }
        } else {
            out << '<i aria-hidden="true" class="arrow right icon disabled"></i>'
        }
        out << '</div>'
    }

    def totalNumber = { attrs, body ->
        def total = attrs.total ?: 0
        def newClass = attrs.class ?: ''

        out << '<span class="ui circular ' + newClass + ' label">'
        out << total
        out << '</span>'
    }

    def dateDevider = { attrs, body ->
        out << "<span class='ui grey horizontal divider la-date-devider'>"
        out << "        ${message(code:'default.to')}"
        out << "</span>"
    }

    def tabs = { attrs, body ->
        def newClass = attrs.class ?: ''
        out << '<div class="ui top attached tabular ' + newClass + ' stackable menu">'
        out << body()
        out << '</div>'
    }

    def tabsItem = { attrs, body ->

        def (text, message) = SwissKnife.getTextAndMessage(attrs)
        String linkBody = (text && message) ? text + " - " + message : text + message
        String aClass = ((this.pageScope.variables?.actionName == attrs.action && (attrs.tab == params.tab || attrs.tab == params[attrs.subTab])) ? 'item active' : 'item') + (attrs.class ? ' ' + attrs.class : '')

        String counts = (attrs.counts >= 0) ? '<div class="ui '  + ' circular label">' + attrs.counts + '</div>' : null

        linkBody = counts ? linkBody + counts : linkBody

        if (attrs.controller) {
            out << g.link(linkBody,
                    class: aClass,
                    controller: attrs.controller,
                    action: attrs.action,
                    params: attrs.params
            )
        } else {
            out << linkBody
        }
    }

    def gokbValue = { attrs, body ->

        if(attrs.gokbId && attrs.field) {

            ApiSource api = ApiSource.findByTypAndActive(ApiSource.ApiTyp.GOKBAPI, true)
            String gokbId = "${attrs.gokbId}"
            Map record = gokbService.getPackageMapWithUUID(api, gokbId)

            if(record && record[attrs.field]){
                out << ((record[attrs.field] instanceof List) ? record[attrs.field].join(', ') : record[attrs.field])
            }
        }
    }

    @Deprecated
    Closure sortableColumn = { attrs, body ->
        def writer = out
        if (!attrs.property) {
            throwTagError("Tag [sortableColumn] is missing required attribute [property]")
        }

        if (!attrs.title && !attrs.titleKey) {
            throwTagError("Tag [sortableColumn] is missing required attribute [title] or [titleKey]")
        }

        def property = attrs.remove("property")
        def action = attrs.action ? attrs.remove("action") : (actionName ?: "list")
        def namespace = attrs.namespace ? attrs.remove("namespace") : ""

        def defaultOrder = attrs.remove("defaultOrder")
        if (defaultOrder != "desc") defaultOrder = "asc"

        // current sorting property and order
        def sort = params.sort
        def order = params.order

        // add sorting property and params to link params
        Map linkParams = [:]
        if (params.id) linkParams.put("id", params.id)
        def paramsAttr = attrs.remove("params")
        if (paramsAttr instanceof Map) linkParams.putAll(paramsAttr)
        linkParams.sort = property

        // propagate "max" and "offset" standard params
        if (params.max) linkParams.max = params.max
        if (params.offset) linkParams.offset = params.offset

        // determine and add sorting order for this column to link params
        attrs['class'] = (attrs['class'] ? "${attrs['class']} " : "")
        if (property == sort) {
            if (order == "asc") {
                linkParams.order = "desc"
                attrs['class'] = (attrs['class'] as String) + " sorted ascending "
            }
            else {
                linkParams.order = "asc"
                attrs['class'] = (attrs['class'] as String) + " sorted descending "
            }
        }
        else {
            linkParams.order = defaultOrder
            attrs['class'] = (attrs['class'] as String) + " sortable "
        }

        // determine column title
        String title = attrs.remove("title") as String
        String titleKey = attrs.remove("titleKey") as String
        Object mapping = attrs.remove('mapping')
        if (titleKey) {
            if (!title) title = titleKey
            MessageSource messageSource = BeanStore.getMessageSource()
            Locale locale = RequestContextUtils.getLocale(request)
            title = messageSource.getMessage(titleKey, null, title, locale)
        }

        writer << "<th "
        // process remaining attributes
        Encoder htmlEncoder = codecLookup.lookupEncoder('HTML')
        attrs.each { k, v ->
            writer << k
            writer << "=\""
            writer << htmlEncoder.encode(v)
            writer << "\" "
        }
        writer << '>'
        Map linkAttrs = [:]
        linkAttrs.params = linkParams
        if (mapping) {
            linkAttrs.mapping = mapping
        }

        linkAttrs.action = action
        linkAttrs.namespace = namespace

        writer << _callLink((Map)linkAttrs) {
            title
        }

        if(body)
        {
            writer << body()
        }

        writer << '</th>'
    }

    private Object _callLink(Map attrs, Object body) {
        TagOutput.captureTagOutput(gspTagLibraryLookup, 'g', 'link', attrs, body, OutputContextLookupHelper.lookupOutputContext())
    }

    def showPropertyValue = { attrs, body ->
        def property = attrs.property

        if(property instanceof Date) {
            out << g.formatDate(date: property, format: message(code: 'default.date.format.notime'))
        }
        else if(property instanceof RefdataValue) {
            out << property?.getI10n('value')
        }
        else if(property instanceof Boolean) {
            out << (property ? RDStore.YN_YES.getI10n("value") : RDStore.YN_NO.getI10n("value"))
        }else {
            out << (property ?: '')
        }
    }

    // <ui:select optionValue="field" />  ==> <ui:select optionValue="field_(de|en|fr)" />

    def select = { attrs, body ->
        attrs.optionValue = attrs.optionValue + "_" + LocaleUtils.getCurrentLang()
        out << g.select(attrs)
    }

    def statsLink = {attrs, body ->
        if (attrs.module) {
            attrs.base = attrs.base ? attrs.base+"/${attrs.module}" : "/${attrs.module}"
            attrs.remove('module')
        }
        if (!attrs.params.packages){
            attrs.params.remove('packages')
        }
        String cleanLink = g.link(attrs, body)
        out << cleanLink.replaceAll("(?<!(http:|https:))[//]+", "/")
    }
}

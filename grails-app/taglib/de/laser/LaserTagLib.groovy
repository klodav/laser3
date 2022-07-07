package de.laser

import de.laser.utils.LocaleUtils
import de.laser.utils.SwissKnife

class LaserTagLib {

    static namespace = "laser"

    // <laser:serviceInjection/>

    def serviceInjection = { attrs, body ->

        g.set( var:'accessService',             bean:'accessService' )
        g.set( var:'auditService',              bean:'auditService' )
        g.set( var:'cacheService',              bean:'cacheService' )
        g.set( var:'compareService',            bean:'compareService' )
        g.set( var:'contextService',            bean:'contextService' )
        g.set( var:'controlledListService',     bean:'controlledListService' )
        g.set( var:'deletionService',           bean:'deletionService')
        g.set( var:'docstoreService',           bean:'docstoreService' )
        g.set( var:'escapeService',             bean:'escapeService')
        g.set( var:'exportClickMeService',      bean:'exportClickMeService')
        g.set( var:'filterService',             bean:'filterService' )
        g.set( var:'formService',               bean:'formService' )
        g.set( var:'genericOIDService',         bean:'genericOIDService' )
        g.set( var:'gokbService',               bean:'gokbService' )
        g.set( var:'instAdmService',            bean:'instAdmService' )
        g.set( var:'identifierService',         bean:'identifierService' )
        g.set( var:'linksGenerationService',    bean:'linksGenerationService' )
        g.set( var:'packageService',            bean:'packageService')
        g.set( var:'pendingChangeService',      bean:'pendingChangeService')
        g.set( var:'propertyService',           bean:'propertyService')
        g.set( var:'subscriptionsQueryService', bean:'subscriptionsQueryService' )
        g.set( var:'subscriptionService',       bean:'subscriptionService' )
        g.set( var:'surveyService',             bean:'surveyService' )
        g.set( var:'systemService',             bean:'systemService' )
        g.set( var:'taskService',               bean:'taskService' )
        g.set( var:'yodaService',               bean:'yodaService' )
    }

    def htmlStart = {attrs, body ->

        String title = message(code: 'laser')

        if (attrs.text) {
            title = title + ' : ' + attrs.text
        }
        if (attrs.message) {
            SwissKnife.checkMessageKey(attrs.message as String)
            title = title + ' : ' + message(code: attrs.message, args: attrs.args)
        }

        if (attrs.serviceInjection) {
            laser.serviceInjection()
        }
        out << '<!doctype html><html><head>'
        out << '<meta name="layout" content="laser">'
        out << body()
        out << '<title>' + title + '</title>'
        out << '</head>'
        out << '<body>'
    }

    def htmlEnd = { attrs, body ->
        out << '</body></head>'
    }

    // <laser:select optionValue="field" />  ==> <laser:select optionValue="field_(de|en|fr)" />

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

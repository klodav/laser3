package de.laser

import de.laser.annotations.CheckFor404
import de.laser.utils.AppUtils
import de.laser.utils.CodeUtils
import de.laser.config.ConfigMapper
import de.laser.utils.DateUtils
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.plugin.springsecurity.annotation.Secured
import grails.web.Action
import grails.web.mapping.UrlMappingData
import org.grails.exceptions.ExceptionUtils
import org.grails.web.mapping.DefaultUrlMappingParser

import java.lang.annotation.Annotation
import java.lang.reflect.Method

/**
 * This controller handles the server code mapping output
 */
@Secured(['permitAll'])
class ServerCodesController {

    GrailsApplication grailsApplication

    /**
     * Shows the error page with stack trace extracts; acts on codes 403, 405 and 500
     */
    def error() {
        Map<String, Object> result = [
                exception: request.getAttribute('exception') ?: request.getAttribute('javax.servlet.error.exception'),
                status: request.getAttribute('javax.servlet.error.status_code'),
                mailString: ''
        ]

        if (result.exception) {

            Throwable exception = (Throwable) result.exception
            Throwable root = ExceptionUtils.getRootCause(exception)

            String nl = " %0D%0A"

            result.mailString =
                    "mailto:laser@hbz-nrw.de?subject=Fehlerbericht - " + ConfigMapper.getLaserSystemId() +
                    "&body=Ihre Fehlerbeschreibung (bitte angeben): " + nl + nl +
                    "URI: "     + request.forwardURI + nl +
                    "Zeitpunkt: " + DateUtils.getLocalizedSDF_noZ().format( new Date() ) + nl +
                    "System: "  + ConfigMapper.getLaserSystemId() + nl +
                    "Branch: "  + AppUtils.getMeta('git.branch') + nl +
                    "Commit: "  + AppUtils.getMeta('git.commit.id') + nl +
                    "Class: "   + (root?.class?.name ?: exception.class.name) + nl

            if (exception.message) {
                result.mailString += "Message: " + exception.message + nl
            }
            if (root?.message != exception.message) {
                result.mailString += "Cause: " + root.message + nl
            }
        }
        render view:'error', model: result
    }

    /**
     * Shows the unauthorised access page, mapping for code 401
     */
    def forbidden() {
        Map<String, Object> result = [status: request.getAttribute('javax.servlet.error.status_code')]
        render view:'forbidden', model: result
    }

    /**
     * Shows the resource not found page, mapping for code 404
     */
    def notFound() {
        Map<String, Object> result = [
                status: request.getAttribute('javax.servlet.error.status_code'),
                alternatives: []
        ]
        GrailsClass controller = getControllerClass()

        if (controller) {
            if (request.getAttribute('javax.servlet.error.message') == CheckFor404.KEY) {
                Method ccm = controller.clazz.declaredMethods.find { it.getAnnotation(Action) && it.name == getActionName() }
                Annotation cfa = ccm.getAnnotation(CheckFor404)

//            if (aa.label()) {
//                result.customMessage = message(code: 'default.not.found.message2', args: [message(code: aa.label())]) as String
//            }
                cfa.alternatives().each{ fb ->
                    if (fb.contains('/')) {
                        result.alternatives << "${g.createLink(uri: (fb.startsWith('/') ? fb : '/' + fb), absolute: true)}"
                    } else {
                        result.alternatives << "${g.createLink(controller: controller.logicalPropertyName, action: fb, absolute: true)}"
                    }
                }
            }
            else {
                UrlMappingData umd = (new DefaultUrlMappingParser()).parse( request.forwardURI )

                result.alternatives = controller.clazz.declaredMethods.findAll {
                    it.getAnnotation(Action) && it.name in ['show', 'list']
                }.collect {
                    if (it.name == 'show' && umd.tokens.size() == 3 && umd.tokens[2].isNumber()) {
                        "${g.createLink(controller: controller.logicalPropertyName, action: 'show', params: [id: umd.tokens[2]], absolute: true)}"
                    }
                    else {
                        "${g.createLink(controller: controller.logicalPropertyName, action: it.name, absolute: true)}"
                    }
                }
            }
        }

        render view:'notFound', model: result
    }

    /**
     * Shows the service unavailable page
     */
    def unavailable() {
        Map<String, Object> result = [status: request.getAttribute('javax.servlet.error.status_code')]
        render view:'unavailable', model: result
    }
}

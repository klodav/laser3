package de.laser.system

import de.laser.http.AjaxHelper
import de.laser.utils.AppUtils
import grails.util.Environment
import org.grails.io.support.GrailsResourceUtils
import org.grails.web.servlet.mvc.GrailsWebRequest

class LaserAssetsTagLib {

    static namespace = 'laser'

    static final String NL = "\n"

    def javascript = {final attrs ->
        out << asset.javascript(attrs).toString().replace(' type="text/javascript" ', ' data-type="external" ')
    }

    def script = { attrs, body ->

        if (AjaxHelper.isXHR(request)) {
            out << NL + '<script data-type="xhr">'
            out << NL + '$(function() {'
            out << NL + ' ' + body() + ' '
            out << NL + '});</script>'
        }
        else {
            Map<String, Object> map = [:]

            if (AppUtils.getCurrentServer() != AppUtils.PROD) {
                if (attrs.file) {
                    map = [file: GrailsResourceUtils.getPathFromBaseDir(attrs.file)]
                }
                else {
                    map = [uri: request.getRequestURI()]
                }
            }
            asset.script(map, body())
        }
    }

    // adaption of AssetsTagLib.deferredScripts ..

    def scriptBlock = {attrs ->
        def assetBlocks = request.getAttribute('assetScriptBlocks')
        if (!assetBlocks) {
            return
        }

        out << NL + '<script data-type="scriptBlock">'
        out << NL + '$(function() {'

        assetBlocks.each { assetBlock ->
            out << NL + '//-> asset: ' + assetBlock.attrs ?: ''
            out << NL + ' ' + assetBlock.body + ' '
        }
        out << '});</script>'
    }

    // render override for dev environment

    def render = { attrs ->

        if (Environment.isDevelopmentMode()) {
            GrailsWebRequest webRequest = getWebRequest()
            String uri = webRequest.getAttributes().getTemplateUri(attrs.template as String, webRequest.getRequest())

            if (attrs.get('model')) {
                out << '<!-- [template: ' + uri + '], [model: ' + (attrs.get('model') as Map).keySet().join(',') + '] -- START -->'

            } else {
                out << '<!-- [template: ' + uri + '] -- START -->'
            }

            if (AppUtils.isDebugMode()) {
                out << '<div style="border:2px dotted orangered" title="' + uri + '">'
                out << g.render(attrs)
                out << '</div>'
            } else {
                out << g.render(attrs)
            }

            out << '<!-- [template: ' + uri + '] -- END -->'
        } else {
            out << g.render(attrs)
        }
    }
}
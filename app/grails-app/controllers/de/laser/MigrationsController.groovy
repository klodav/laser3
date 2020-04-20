package de.laser

import com.k_int.kbplus.Identifier
import com.k_int.kbplus.IdentifierNamespace
import com.k_int.kbplus.License
import com.k_int.kbplus.Org
import com.k_int.kbplus.Package
import com.k_int.kbplus.Subscription
import com.k_int.kbplus.TitleInstance
import grails.plugin.springsecurity.annotation.Secured

@Secured(['IS_AUTHENTICATED_FULLY'])
class MigrationsController {

    def springSecurityService
    def contextService

    static boolean ftupdate_running = false

    @Secured(['ROLE_YODA'])
    def erms2362() {
        // List<String> nsList = ['acs', 'ebookcentral', 'emerald', 'herdt', 'mitpress', 'oup']

        Closure pre = { tmp ->
            "<pre>" + tmp + "</pre>"
        }
        Closure code = { tmp ->
            '<code>' + tmp.join('<br>') + '</code>'
        }
        Closure href = { reference ->
            String link = reference instanceof TitleInstance ? '/title/show/' : (
                          reference instanceof License ? '/lic/show/' : (
                          reference instanceof Subscription ? '/subscription/show/' : (
                          reference instanceof Package ? '/package/show/' : (
                          reference instanceof Org ? '/org/show/' : (
                          null
            )))))

            return link ? (grailsApplication.config.grails.serverURL + link + reference.id) : null
        }

        String result       = ''
        String ns_str       = params.ns ?: null
        String ns_old_str   = ns_str ? 'inid_' + ns_str : null

        long ts = System.currentTimeMillis()

        log.debug('erms2362() processing: ' + ns_old_str + ' <-- ' + ns_str)

        IdentifierNamespace oldNs = IdentifierNamespace.findByNs(ns_old_str)
        IdentifierNamespace newNs = IdentifierNamespace.findByNs(ns_str)

        if (oldNs && newNs) {
            List<Identifier> oldIds = Identifier.findAllByNs(oldNs)
            List matches  = []
            List orphaned = []

            result += "<h2>NS:${oldNs.id} <strong>${oldNs.ns}</strong> <-- NS:${newNs.id} <strong>${newNs.ns}</strong></h2>"
            result += pre("${oldIds.size()} identifier/s in old namespace found")

            oldIds.eachWithIndex { old, i ->
                log.debug('processing (' + (i+1) + ' from ' + oldIds.size() + '): ' + old)

                List<Identifier> newIds = Identifier.findAllWhere([
                        value: old.value,
                        ns:   newNs,
                        lic:  old.lic,
                        org:  old.org,
                        pkg:  old.pkg,
                        sub:  old.sub,
                        ti:   old.ti,
                        tipp: old.tipp,
                        cre:  old.cre
                ])
                def reference = old.getReference()

                if (! newIds.isEmpty()) {
                    matches.add([
                            oldNs: oldNs,
                            oldId: old,
                            newNs: newNs,
                            newIds: newIds,
                            link: href(reference)
                    ])
                }
                else {
                    orphaned.add([
                            oldNs: oldNs,
                            oldId: old,
                            reference: [
                                    id: reference.id,
                                    gokbId: reference.hasProperty('gokbId') ? reference.gokbId : null,
                                    type: reference.getClass()?.canonicalName,
                                    link: href(reference)
                            ]
                    ])
                }
            }
            result += pre("${matches.size()} matched, ${orphaned.size()} orphaned")

            if (! matches.isEmpty()) {
                String[] m = []
                matches.each { c ->
                    String tmp = "[ NS:${c.oldNs.id} <strong>${c.oldNs.ns}</strong> / ID:${c.oldId.id} <strong>${c.oldId.value}</strong> ]"
                    tmp += " <strong><--</strong> " + (c.newIds.collect{ it ->
                        "[ NS:${c.newNs.id} <strong>${c.newNs.ns}</strong> / ID:${it.id} <strong>${it.value}</strong> ]"
                    }).join(", ")

                    if( c.link) {
                        tmp += " -----> [ <a href='${c.link}' target='_blank'>${c.link.replace(grailsApplication.config.grails.serverURL,'')}</a> ]"
                    }
                    m += tmp
                }
                result += pre('Matches:')
                result += code(m)
            }

            if (! orphaned.isEmpty()) {
                String[] o = []
                orphaned.each { c ->
                    String tmp = "[ NS:${c.oldNs.id} <strong>${c.oldNs.ns}</strong> / ID:${c.oldId.id} <strong>${c.oldId.value}</strong> ]"
                    tmp += " -----> [ ${c.reference.type}:${c.reference.id} - "

                    if (c.reference.gokbId) {
                        tmp += "GOKBID:${c.reference.gokbId} - "
                    }
                    if (c.reference.link) {
                        tmp += "<a href='${c.reference.link}' target='_blank'>${c.reference.link.replace(grailsApplication.config.grails.serverURL,'')}</a> ]"
                    }
                    o += tmp
                }
                result += pre('Orphans:')
                result += code(o)
            }
            result += '<br/><br/>'
        }
        else {
            result += oldNs ? '' : pre('<strong>ERROR</strong>: old namespace <strong>' + ns_old_str + '</strong> not found')
            result += newNs ? '' : pre('<strong>ERROR</strong>: new namespace <strong>' + ns_str + '</strong> not found')
        }

        result += code([
                "# ${grailsApplication.config.grails.serverURL}",
                "# ${grailsApplication.config.laserSystemId}",
                "# Processing time: ${System.currentTimeMillis() - ts} ms"
        ])

        render text: '<!DOCTYPE html><html lang="en"><head><title>' + grailsApplication.config.grails.serverURL + '</title></head><body>' + result + '</body></html>'
    }

}

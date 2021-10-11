package de.laser.reporting.export.myInstitution

import de.laser.ContextService
import de.laser.Identifier
import de.laser.License
import de.laser.helper.RDStore
import de.laser.reporting.export.base.BaseDetailsExport
import de.laser.reporting.report.myInstitution.base.BaseDetails
import grails.util.Holders
import org.grails.plugins.web.taglib.ApplicationTagLib


class LicenseExport extends BaseDetailsExport {

    static String KEY = 'license'

    static Map<String, Object> CONFIG_ORG_CONSORTIUM = [

            base : [
                    meta : [
                            class: License
                    ],
                    fields : [
                            default: [
                                    'globalUID'         : FIELD_TYPE_PROPERTY,
                                    'reference'         : FIELD_TYPE_PROPERTY,
                                    'startDate'         : FIELD_TYPE_PROPERTY,
                                    'endDate'           : FIELD_TYPE_PROPERTY,
                                    'status'            : FIELD_TYPE_REFDATA,
                                    'licenseCategory'   : FIELD_TYPE_REFDATA,
                                    '@-license-subscriptionCount' : FIELD_TYPE_CUSTOM_IMPL,       // virtual
                                    '@-license-memberCount'       : FIELD_TYPE_CUSTOM_IMPL,       // virtual
                                    'x-identifier'          : FIELD_TYPE_CUSTOM_IMPL,
                                    'x-property'            : FIELD_TYPE_CUSTOM_IMPL_QDP,   // qdp
                            ]
                    ]
            ]
    ]

    static Map<String, Object> CONFIG_ORG_INST = [

            base : [
                    meta : [
                            class: License
                    ],
                    fields : [
                            default: [
                                    'globalUID'         : FIELD_TYPE_PROPERTY,
                                    'reference'         : FIELD_TYPE_PROPERTY,
                                    'startDate'         : FIELD_TYPE_PROPERTY,
                                    'endDate'           : FIELD_TYPE_PROPERTY,
                                    'status'            : FIELD_TYPE_REFDATA,
                                    'licenseCategory'   : FIELD_TYPE_REFDATA,
                                    'x-identifier'      : FIELD_TYPE_CUSTOM_IMPL,
                                    'x-property'        : FIELD_TYPE_CUSTOM_IMPL_QDP,   // qdp
                            ]
                    ]
            ]
    ]

    LicenseExport (String token, Map<String, Object> fields) {
        this.token = token

        // keeping order ..
        getAllFields().keySet().each { k ->
            if (k in fields.keySet() ) {
                selectedExportFields.put(k, fields.get(k))
            }
        }
        normalizeSelectedMultipleFields( this )
    }

    @Override
    Map<String, Object> getSelectedFields() {
        selectedExportFields
    }

    @Override
    String getFieldLabel(String fieldName) {
        GlobalExportHelper.getFieldLabel( this, fieldName )
    }

    @Override
    List<Object> getDetailedObject(Object obj, Map<String, Object> fields) {

        ApplicationTagLib g = Holders.grailsApplication.mainContext.getBean(ApplicationTagLib)
        ContextService contextService = (ContextService) Holders.grailsApplication.mainContext.getBean('contextService')

        License lic = obj as License
        List content = []

        fields.each{ f ->
            String key = f.key
            String type = getAllFields().get(f.key)

            // --> generic properties
            if (type == FIELD_TYPE_PROPERTY) {

                if (key == 'globalUID') {
                    content.add( g.createLink( controller: 'license', action: 'show', absolute: true ) + '/' + lic.getProperty(key) as String )
                }
                else {
                    content.add( getPropertyContent(lic, key, License.getDeclaredField(key).getType()) )
                }
            }
            // --> generic refdata
            else if (type == FIELD_TYPE_REFDATA) {
                content.add( getRefdataContent(lic, key) )
            }
            // --> refdata join tables
            else if (type == FIELD_TYPE_REFDATA_JOINTABLE) {
                content.add( getJointableRefdataContent(lic, key) )
            }
            // --> custom filter implementation
            else if (type == FIELD_TYPE_CUSTOM_IMPL) {

                if (key == 'x-identifier') {
                    List<Identifier> ids = []

                    if (f.value) {
                        ids = Identifier.executeQuery( "select i from Identifier i where i.value != null and i.value != '' and i.lic = :lic and i.ns.id in (:idnsList)",
                                [lic: lic, idnsList: f.value] )
                    }
                    content.add( ids.collect{ (it.ns.getI10n('name') ?: it.ns.ns + ' *') + ':' + it.value }.join( CSV_VALUE_SEPARATOR ))
                }
                else if (key == '@-license-subscriptionCount') { // TODO: query
                    int count = License.executeQuery(
                            'select count(distinct li.destinationSubscription) from Links li where li.sourceLicense = :lic and li.linkType = :linkType',
                            [lic: lic, linkType: RDStore.LINKTYPE_LICENSE]
                    )[0]
                    content.add( count )
                }
                else if (key == '@-license-memberCount') {
                    int count = License.executeQuery('select count(l) from License l where l.instanceOf = :parent', [parent: lic])[0]
                    content.add( count )
                }
            }
            // --> custom query depending filter implementation
            else if (type == FIELD_TYPE_CUSTOM_IMPL_QDP) {

                if (key == 'x-property') {
                    Long pdId = GlobalExportHelper.getDetailsCache(token).id as Long

                    List<String> properties = BaseDetails.resolvePropertiesGeneric(lic, pdId, contextService.getOrg())
                    content.add( properties.findAll().join( CSV_VALUE_SEPARATOR ) ) // removing empty and null values
                }
            }
            else {
                content.add( '- not implemented -' )
            }
        }

        content
    }
}

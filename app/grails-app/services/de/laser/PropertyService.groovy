package de.laser

import com.k_int.kbplus.RefdataValue
import com.k_int.kbplus.SystemAdmin
import com.k_int.kbplus.abstract_domain.AbstractProperty
import com.k_int.properties.PropertyDefinition

class PropertyService {

    def grailsApplication
    def genericOIDService

    private List<String> splitQueryFromOrderBy(String sql) {
        String order_by = null
        int pos = sql.toLowerCase().indexOf("order by")
        if (pos >= 0) {
            order_by = sql.substring(pos-1)
            sql = sql.substring(0, pos-1)
        }
        [sql, order_by]
    }

    Map<String, Object> evalFilterQuery(Map params, String base_qry, String hqlVar, Map base_qry_params) {
        def order_by
        (base_qry, order_by) = splitQueryFromOrderBy(base_qry)

        if (params.filterPropDef) {
            def pd = genericOIDService.resolveOID(params.filterPropDef)
            
            if (pd.tenant) {
                base_qry += " and exists ( select gProp from ${hqlVar}.privateProperties as gProp where gProp.type = :propDef "
            } else {
                base_qry += " and exists ( select gProp from ${hqlVar}.customProperties as gProp where gProp.type = :propDef "
            }
            base_qry_params.put('propDef', pd)
            
            if (params.filterProp) {

                switch (pd.type) {
                    case RefdataValue.toString():
                        base_qry += " and gProp.refValue= :prop "
                        def pdValue = genericOIDService.resolveOID(params.filterProp)
                        base_qry_params.put('prop', pdValue)

                        break
                    case Integer.toString():
                        base_qry += " and gProp.intValue = :prop "
                        base_qry_params.put('prop', AbstractProperty.parseValue(params.filterProp, pd.type))
                        break
                    case String.toString():
                        base_qry += " and gProp.stringValue = :prop "
                        base_qry_params.put('prop', AbstractProperty.parseValue(params.filterProp, pd.type))
                        break
                    case BigDecimal.toString():
                        base_qry += " and gProp.decValue = :prop "
                        base_qry_params.put('prop', AbstractProperty.parseValue(params.filterProp, pd.type))
                        break
                    case Date.toString():
                        base_qry += " and gProp.dateValue = :prop "
                        base_qry_params.put('prop', AbstractProperty.parseValue(params.filterProp, pd.type))
                        break
                }
            }
            base_qry += " ) "
        }
        if (order_by) {
            base_qry += order_by
        }
        [query: base_qry, queryParams: base_qry_params]
    }

    def getUsageDetails() {
        def usedPdList  = []
        def detailsMap = [:]

        grailsApplication.getArtefacts("Domain").toList().each { dc ->

            if (dc.shortName.endsWith('CustomProperty') || dc.shortName.endsWith('PrivateProperty')) {

                //log.debug( dc.shortName )
                def query = "SELECT DISTINCT type FROM ${dc.name}"
                //log.debug(query)

                def pds = SystemAdmin.executeQuery(query)
                log.debug(pds)
                detailsMap << ["${dc.shortName}": pds.collect{ it -> "${it.id}:${it.type}:${it.descr}"}.sort()]

                // ids of used property definitions
                pds.each{ it ->
                    usedPdList << it.id
                }
            }
        }

        [usedPdList.unique().sort(), detailsMap.sort()]
    }

    def replacePropertyDefinitions(PropertyDefinition pdFrom, PropertyDefinition pdTo) {

        log.debug("replacing: ${pdFrom} with: ${pdTo}")
        def count = 0

        def implClass = pdFrom.getImplClass('custom')
        def customProps = Class.forName(implClass)?.findAllWhere(
                type: pdFrom
        )
        customProps.each{ cp ->
            log.debug("exchange type at: ${implClass}(${cp.id}) from: ${pdFrom.id} to: ${pdTo.id}")
            cp.type = pdTo
            cp.save(flush:true)
            count++
        }

        count
    }
}


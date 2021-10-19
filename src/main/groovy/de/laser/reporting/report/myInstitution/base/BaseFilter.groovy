package de.laser.reporting.report.myInstitution.base

import de.laser.ContextService
import de.laser.base.AbstractPropertyWithCalculatedLastUpdated
import de.laser.properties.PropertyDefinition
import grails.util.Holders
import grails.web.servlet.mvc.GrailsParameterMap

class BaseFilter {

    static Set<String> getCurrentFilterKeys(GrailsParameterMap params, String cmbKey) {

        params.keySet().findAll{ it.toString().startsWith(cmbKey) && ! it.toString().endsWith(BaseConfig.FILTER_SOURCE_POSTFIX) }
    }

    static String getDateModifier(String modifier) {

        if (modifier == 'less') {
            return '<'
        }
        else if (modifier == 'greater') {
            return '>'
        }
        else if (modifier == 'less-equal') {
            return '<='
        }
        else if (modifier == 'greater-equal') {
            return '>='
        }
        else {
            return '='
        }
    }

    static String getLegalInfoQueryWhereParts(Long key) {

        if (key == 0){
            return 'org.createdBy is null and org.legallyObligedBy is null'
        }
        else if (key == 1){
            return 'org.createdBy is not null and org.legallyObligedBy is not null'
        }
        else if (key == 2){
            return 'org.createdBy is not null and org.legallyObligedBy is null'
        }
        else if (key == 3){
            return 'org.createdBy is null and org.legallyObligedBy is not null'
        }
    }

    static String getPropertyFilterSubQuery(String hqlDc, String hqlVar, Long pdId, String pValue, Map<String, Object> queryParams) {

        ContextService contextService = (ContextService) Holders.grailsApplication.mainContext.getBean('contextService')

        String pvQuery = ''
        if (pValue) {
            PropertyDefinition pd = PropertyDefinition.get(pdId)
            pvQuery = ' and prop.' + pd.getImplClassValueProperty() + ' = :pfsq3'
            queryParams.put('pfsq3', AbstractPropertyWithCalculatedLastUpdated.parseValue(pValue, pd.type))
        }

        String query =  'select prop from ' + hqlDc + ' prop join prop.owner owner join prop.type pd' +
                        ' where owner = ' + hqlVar +
                        ' and pd.id = :pfsq1 ' +
                        ' and (prop.tenant = :pfsq2 or prop.isPublic = true)' + pvQuery

        queryParams.put('pfsq1', pdId)
        queryParams.put('pfsq2', contextService.getOrg())

        query
    }

    static List<Long> getCachedFilterIdList(prefix, params) {

        List<Long> idList = params?.filterCache?.data?.get(prefix + 'IdList')?.collect { it as Long }
        return idList ?: []
    }
}

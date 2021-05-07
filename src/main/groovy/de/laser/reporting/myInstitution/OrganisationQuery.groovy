package de.laser.reporting.myInstitution

import de.laser.ContextService
import de.laser.Org
import de.laser.Subscription
import de.laser.auth.Role
import de.laser.helper.RDStore
import de.laser.reporting.myInstitution.base.BaseFilter
import de.laser.reporting.myInstitution.base.BaseQuery
import grails.util.Holders
import grails.web.servlet.mvc.GrailsParameterMap

class OrganisationQuery extends BaseQuery {

    static List<String> PROPERTY_QUERY = [ 'select p.id, p.value_de, count(*) ', ' group by p.id, p.value_de order by p.value_de' ]

    static Map<String, Object> query(GrailsParameterMap params) {

        ContextService contextService = (ContextService) Holders.grailsApplication.mainContext.getBean('contextService')

        //def messageSource = Holders.grailsApplication.mainContext.getBean('messageSource')
        //Locale locale = LocaleContextHolder.getLocale()

        //println 'OrganisationQuery.query()'
        //println params

        Map<String, Object> result = getEmptyResult( params.query, params.chart )

        String prefix = params.query.split('-')[0]
        String suffix = params.query.split('-')[1] // only simply cfg.query
        List idList   = BaseFilter.getCachedFilterIdList(prefix, params)

        if (! idList) {
        }
        else if ( suffix in ['libraryType']) {

            processSimpleRefdataQuery(params.query, 'libraryType', idList, result)
        }
        else if ( suffix in ['region']) {

            processSimpleRefdataQuery(params.query,'region', idList, result)
        }
        else if ( suffix in ['country']) {

            processSimpleRefdataQuery(params.query,'country', idList, result)
        }
        else if ( suffix in ['libraryNetwork']) {

            processSimpleRefdataQuery(params.query, 'libraryNetwork', idList, result)
        }
        else if ( suffix in ['funderType']) {

            processSimpleRefdataQuery(params.query, 'funderType', idList, result)
        }
        else if ( suffix in ['funderHskType']) {

            processSimpleRefdataQuery(params.query, 'funderHskType', idList, result)
        }
        else if ( suffix in ['orgType']) {

            handleGenericRefdataQuery(
                    params.query,
                    PROPERTY_QUERY[0] + 'from Org o join o.orgType p where o.id in (:idList)' + PROPERTY_QUERY[1],
                    'select o.id from Org o join o.orgType p where o.id in (:idList) and p.id = :d order by o.name',
                    'select distinct o.id from Org o where o.id in (:idList) and not exists (select ot from o.orgType ot)',
                    idList,
                    result
            )
        }
        else if ( suffix in ['customerType']) {

            result.data = Org.executeQuery(
                    'select r.id, r.authority, count(*) from Org o, OrgSetting oss, Role r where oss.org = o and oss.key = \'CUSTOMER_TYPE\' and o.id in (:idList) and oss.roleValue = r group by r.id',
                    [idList: idList]
            )

            result.data.each { d ->
                d[1] = Role.get(d[0]).getI10n('authority')

                result.dataDetails.add([
                        query : params.query,
                        id    : d[0],
                        label : d[1],
                        idList: Org.executeQuery(
                                'select o.id from Org o, OrgSetting oss where oss.org = o and oss.key = \'CUSTOMER_TYPE\' and o.id in (:idList) and oss.roleValue.id = :d order by o.name',
                                [idList: idList, d: d[0]]
                        )
                ])
            }
            handleGenericNonMatchingData(
                    params.query,
                    'select distinct o.id from Org o where o.id in (:idList) and not exists (select oss from OrgSetting oss where oss.org = o and oss.key = \'CUSTOMER_TYPE\')',
                    idList,
                    result
            )
        }
        else if ( suffix in ['subjectGroup']) {

            handleGenericRefdataQuery(
                    params.query,
                    PROPERTY_QUERY[0] + 'from Org o join o.subjectGroup rt join rt.subjectGroup p where o.id in (:idList)' + PROPERTY_QUERY[1],
                    'select o.id from Org o join o.subjectGroup rt join rt.subjectGroup p where o.id in (:idList) and p.id = :d order by o.name',
                    'select distinct o.id from Org o where o.id in (:idList) and not exists (select osg from OrgSubjectGroup osg where osg.org = o)',
                    idList,
                    result
            )
        }
        else if ( params.query in ['org-identifier-assignment']) {

            handleGenericIdentifierAssignmentQuery(
                    params.query,
                    'select ns.id, ns.ns, count(*) from Org o join o.ids ident join ident.ns ns where o.id in (:idList)',
                    'select o.id from Org o join o.ids ident join ident.ns ns where o.id in (:idList)',
                    'select o.id from Org o where o.id in (:idList)', // modified idList
                    idList,
                    result
            )
        }
        else if ( params.query in ['org-property-assignment']) {

            handleGenericPropertyAssignmentQuery(
                    params.query,
                    'select pd.id, pd.name, count(*) from Org o join o.propertySet prop join prop.type pd where o.id in (:idList)',
                    'select o.id from Org o join o.propertySet prop join prop.type pd where o.id in (:idList)',
                    idList,
                    contextService.getOrg(),
                    result
            )
        }

        result
    }

    static void processSimpleRefdataQuery(String query, String refdata, List idList, Map<String, Object> result) {

        handleGenericRefdataQuery(
                query,
                PROPERTY_QUERY[0] + 'from Org o join o.' + refdata + ' p where o.id in (:idList)' + PROPERTY_QUERY[1],
                'select o.id from Org o join o.' + refdata + ' p where o.id in (:idList) and p.id = :d order by o.name',
                'select distinct o.id from Org o where o.id in (:idList) and o.' + refdata + ' is null',
                idList,
                result
        )
    }
}

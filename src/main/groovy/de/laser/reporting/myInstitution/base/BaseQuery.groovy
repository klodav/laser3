package de.laser.reporting.myInstitution.base

import de.laser.ContextService
import de.laser.I10nTranslation
import de.laser.Org
import de.laser.RefdataValue
import de.laser.Subscription
import de.laser.helper.DateUtils
import de.laser.helper.SessionCacheWrapper
import de.laser.properties.PropertyDefinition
import grails.util.Holders
import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.context.i18n.LocaleContextHolder

import java.sql.Timestamp

class BaseQuery {

    static String NO_DATA_LABEL         = '* keine Angabe'
    static String NO_IDENTIFIER_LABEL   = '* ohne Identifikator'
    static String NO_PLATFORM_LABEL     = '* ohne Plattform'
    static String NO_PROVIDER_LABEL     = '* ohne Anbieter'

    static Map<String, Object> getQueryCache(String token) {
        ContextService contextService = (ContextService) Holders.grailsApplication.mainContext.getBean('contextService')

        SessionCacheWrapper sessionCache = contextService.getSessionCache()
        Map<String, Object> cacheMap = sessionCache.get("MyInstitutionController/reporting/" + token)

        cacheMap.queryCache as Map<String, Object>
    }

    // ----- ----- -----

    static Map<String, Object> getEmptyResult(String query, String chart) {
        return [
                chart       : chart,
                query       : query,
                labels      : [:],
                data        : [],
                dataDetails : []
        ]
    }

    static List<String> getQueryLabels(Map<String, Object> config, GrailsParameterMap params) {

        List<String> meta = []

        //println 'BaseQuery.getQueryLabels()'
        //println params

        config.each {it ->
            it.value.get('query')?.default.each { it2 ->  // TODO ???
                if (it2.value.containsKey(params.query)) {
                    meta = [ it2.key, it2.value.get(params.query), params.label ]
                }
            }
            it.value.get('query2')?.each { it2 ->
                if (it2.value.containsKey(params.query)) {
                    meta = [ it2.key, it2.value.get(params.query).label, params.label ]
                }
            }
        }
        meta
    }

    static Object getDataDetailsByIdAndKey(Long id, String key, List<Map<String, Object>> idList) {
        def result

        idList.each{ it ->
            if (it.id == id) {
                result = it.get(key)
                return
            }
        }
        result
    }

    static void handleGenericQuery(String query, String dataHql, String dataDetailsHql, String nonMatchingHql, List idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery( dataHql, [idList: idList] ) : []

        result.data.each { d ->
            d[1] = d[0]

            result.dataDetails.add( [
                    query:  query,
                    id:     d[0],
                    label:  d[1],
                    idList: Org.executeQuery( dataDetailsHql, [idList: idList, d: d[0]] )
            ])
        }
        handleGenericNonMatchingData( query, nonMatchingHql, idList, result )
    }

    static void handleGenericRefdataQuery(String query, String dataHql, String dataDetailsHql, String nonMatchingHql, List idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery( dataHql, [idList: idList] ) : []

        result.data.each { d ->
            d[1] = RefdataValue.get(d[0]).getI10n('value').replaceAll("'", '"')

            result.dataDetails.add( [
                    query:  query,
                    id:     d[0],
                    label:  d[1],
                    idList: Org.executeQuery( dataDetailsHql, [idList: idList, d: d[0]] )
            ])
        }
        handleGenericNonMatchingData( query, nonMatchingHql, idList, result )
    }

    static void handleGenericNonMatchingData(String query, String hql, List idList, Map<String, Object> result) {

        List noDataList = idList ? Org.executeQuery( hql, [idList: idList] ) : []

        if (noDataList) {
            result.data.add( [null, NO_DATA_LABEL, noDataList.size()] )

            result.dataDetails.add( [
                    query:  query,
                    id:     null,
                    label:  NO_DATA_LABEL,
                    idList: noDataList
            ])
        }
    }

    static void handleGenericBooleanQuery(String query, String dataHql, String dataDetailsHql, List idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery( dataHql, [idList: idList] ) : []

        result.data.each { d ->
            d[0] = (d[0] == true ? 1 : 0)
            d[1] = (d[1] == true ? 'Ja' : 'Nein')

            result.dataDetails.add([
                    query : query,
                    id    : d[0],
                    label : d[1],
                    idList: Org.executeQuery( dataDetailsHql, [idList: idList, d: (d[0] == 1)] )
            ])
        }
    }

    static void handleGenericDateQuery(String query, String dataHql, String dataDetailsHql, String nonMatchingHql, List idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery( dataHql, [idList: idList] ) : []

        result.data.each { d ->
            Timestamp ts = d[0]
            Long d0Id = ts.toInstant().getEpochSecond()
            d[1] = DateUtils.getSDF_NoTime().format(d[1])

            result.dataDetails.add( [
                    query:  query,
                    id:     d0Id,
                    label:  d[1],
                    idList: Org.executeQuery( dataDetailsHql, [idList: idList, d: d[0]] )
            ])

            d[0] = d0Id
        }
        handleGenericNonMatchingData( query, nonMatchingHql, idList, result )
    }

    static void handleGenericIdentifierAssignmentQuery(String query, String dataHqlPart, String dataDetailsHqlPart, String nonMatchingHql, List idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery(
                dataHqlPart + " and ident.value is not null and trim(ident.value) != '' group by ns.id order by ns.ns",
                [idList: idList]
        ) : []

        result.data.each { d ->
            List<Long> objIdList = Org.executeQuery(
                    dataDetailsHqlPart + " and ns.id = :d and ident.value is not null and trim(ident.value) != ''",
                    [idList: idList, d: d[0]]
            )
            result.dataDetails.add([
                    query : query,
                    id    : d[0],
                    label : d[1],
                    idList: objIdList,
                    value1: objIdList.size(),
                    value2: objIdList.unique().size()
            ])
        }

        List<Long> nonMatchingIdList = idList.minus( result.dataDetails.collect { it.idList }.flatten() )
        List noDataList = nonMatchingIdList ? Org.executeQuery( nonMatchingHql, [idList: nonMatchingIdList] ) : []

        if (noDataList) {
            result.data.add( [null, NO_IDENTIFIER_LABEL, noDataList.size()] )

            result.dataDetails.add( [
                    query:  query,
                    id:     null,
                    label:  NO_IDENTIFIER_LABEL,
                    idList: noDataList,
                    value1: 0,
                    value2: noDataList.size(),
            ])
        }
    }

    static void handleGenericPropertyAssignmentQuery(String query, String dataHqlPart, String dataDetailsHqlPart, List idList, Org ctxOrg, Map<String, Object> result) {

        String locale = I10nTranslation.decodeLocale(LocaleContextHolder.getLocale())

        result.data = idList ? Org.executeQuery(
                dataHqlPart + " and (prop.tenant = :ctxOrg or prop.isPublic = true) and pd.descr like '%Property' group by pd.id order by pd.name_" + locale,
                [idList: idList, ctxOrg: ctxOrg]
        ) : []

        result.data.each { d ->
            d[1] = PropertyDefinition.get(d[0]).getI10n('name').replaceAll("'", '"')

            List<Long> objIdList =  Org.executeQuery(
                    dataDetailsHqlPart + ' and (prop.tenant = :ctxOrg or prop.isPublic = true) and pd.id = :d order by pd.name_' + locale,
                    [idList: idList, d: d[0], ctxOrg: ctxOrg]
            )
            result.dataDetails.add([
                    query : query,
                    id    : d[0],
                    label : d[1],
                    idList: objIdList,
                    value1: objIdList.size(),
                    value2: objIdList.unique().size()
            ])
        }
    }

    static void handleGenericAnnualAssignmentQuery(String query, String domainClass, List idList, Map<String, Object> result) {

        List years = Org.executeQuery( 'select distinct YEAR(dc.startDate) from ' + domainClass + ' dc' )
        years.addAll( Org.executeQuery( 'select distinct YEAR(dc.endDate) from ' + domainClass + ' dc' ) )

        years = years.findAll().unique().sort() // TODO hardcoded

        years.each { y ->
            String hql = 'select dc.id from ' + domainClass + ' dc where dc.id in (:idList) and ' +
                    '( (YEAR(dc.startDate) <= ' + y + ' or dc.startDate is null) and (YEAR(dc.endDate) >= ' + y + ' or dc.endDate is null) ) and ' +
                    'not (dc.startDate is null and dc.endDate is null)'

            List<Long> annualList = Org.executeQuery( hql, [idList: idList] )

            if (annualList) {
                result.data.add( [y, y, annualList.size()] )
                result.dataDetails.add( [
                        query: query,
                        id: y,
                        label: y,
                        idList: annualList
                ] )
            }
        }

        List<Long> noDataList = Org.executeQuery(
                'select dc.id from ' + domainClass + ' dc where dc.id in (:idList) and dc.startDate is null and dc.endDate is null',
                [idList: idList]
        )
        result.data.add( [null, BaseQuery.NO_DATA_LABEL, noDataList.size()] )

        result.dataDetails.add( [
                query:  query,
                id:     null,
                label:  BaseQuery.NO_DATA_LABEL,
                idList: noDataList
        ])
    }
}

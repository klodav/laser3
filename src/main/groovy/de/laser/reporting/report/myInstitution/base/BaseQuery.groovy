package de.laser.reporting.report.myInstitution.base

import de.laser.I10nTranslation
import de.laser.IdentifierNamespace
import de.laser.Org
import de.laser.RefdataValue
import de.laser.auth.Role
import de.laser.helper.DateUtils
import de.laser.properties.PropertyDefinition
import de.laser.reporting.report.GenericHelper
import de.laser.reporting.report.local.SubscriptionReport
import grails.util.Holders
import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

import java.sql.Timestamp
import java.time.Year

class BaseQuery {

    static String NO_DATA_LABEL         = 'noData.label'
    static String NO_MATCH_LABEL        = 'noMatch.label'
    static String NO_COUNTERPART_LABEL  = 'noCounterpart.label'
    static String NO_IDENTIFIER_LABEL   = 'noIdentifier.label'
    static String NO_PLATFORM_LABEL     = 'noPlatform.label'
    static String NO_PROVIDER_LABEL     = 'noProvider.label'
    static String NO_STARTDATE_LABEL    = 'noStartDate.label'
    static String NO_ENDDATE_LABEL      = 'noEndDate.label'

    static def NO_DATA_ID           = null
    static int NO_COUNTERPART_ID    = 0 // dyn.neg.values for unmapped es refdata
    static int FAKE_DATA_ID_1       = -1
    static int FAKE_DATA_ID_2       = -2
    static int FAKE_DATA_ID_3       = -3

    static String SQM_MASK      = "\\\\\'"

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

        List<String> labels = getQueryLabels(config, params.query)

        if (labels) {
            labels.add( params.label ) // not set in step 2
        }
        labels
    }

    static List<String> getQueryLabels(Map<String, Object> config, String query) {
        List<String> meta = []

        config.each {it ->
            String cfgKey = it.value.get('meta').cfgKey

            it.value.get('query')?.default?.each { it2 ->
                if (it2.value.contains(query)) {
                    if (cfgKey == 'SubscriptionReport') {
                        meta = [SubscriptionReport.getMessage(it2.key), SubscriptionReport.getMessage('query.' + query) ]
                    } else {
                        meta = [ BaseConfig.getMessage(it2.key), BaseConfig.getMessage(cfgKey + '.query.' + query) ]
                    }
                }
            }
            it.value.get('distribution')?.each { it2 ->
                if (it2.value.containsKey(query)) {
                    meta = [ BaseConfig.getMessage('distribution'), BaseConfig.getMessage(cfgKey + '.dist.' + query) ]
                }
            }
            it.value.get('timeline')?.each { it2 ->
                if (it2.value.containsKey(query)) {
                    meta = [SubscriptionReport.getMessage(it2.key), SubscriptionReport.getMessage('timeline.' + query) ]
                }
            }
        }
        meta
    }

    static Object getDataDetailsByIdAndKey(Long id, String key, List<Map<String, Object>> ddList) {
        def result

        ddList.each{ it ->
            if (it.id == id) {
                result = it.get(key)
                return
            }
        }
        result
    }

    static void handleGenericQuery(String query, String dataHql, String dataDetailsHql, String nonMatchingHql, List<Long> idList, Map<String, Object> result) {

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

    static void handleGenericAllQuery(String query, String dataHql, String dataDetailsHql, List<Long> idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery( dataHql, [idList: idList] ) : []

        result.data.each { d ->
            d[0] = Math.abs(d[0].hashCode())

            result.dataDetails.add([
                    query : query,
                    id    : d[0],
                    label : d[1],
                    idList: Org.executeQuery( dataDetailsHql, [idList: idList, d: d[1]] )
            ])
        }
    }

    static void handleGenericRefdataQuery(String query, String dataHql, String dataDetailsHql, String nonMatchingHql, List<Long> idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery( dataHql, [idList: idList] ) : []

        result.data.each { d ->
            d[1] = RefdataValue.get(d[0]).getI10n('value')

            result.dataDetails.add( [
                    query:  query,
                    id:     d[0],
                    label:  d[1],
                    idList: Org.executeQuery( dataDetailsHql, [idList: idList, d: d[0]] )
            ])
        }
        handleGenericNonMatchingData( query, nonMatchingHql, idList, result )
    }

    static void handleGenericRoleQuery(String query, String dataHql, String dataDetailsHql, String nonMatchingHql, List<Long> idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery( dataHql, [idList: idList] ) : []

        result.data.each { d ->
            d[1] = Role.get(d[0]).getI10n('authority')

            result.dataDetails.add( [
                    query:  query,
                    id:     d[0],
                    label:  d[1],
                    idList: Org.executeQuery( dataDetailsHql, [idList: idList, d: d[0]] )
            ])
        }
        handleGenericNonMatchingData( query, nonMatchingHql, idList, result )
    }

    static void handleGenericNonMatchingData(String query, String hql, List<Long> idList, Map<String, Object> result) {

        List<Long> noDataList = idList ? Org.executeQuery( hql, [idList: idList] ) : []

        handleGenericNonMatchingData1Value_TMP(query, NO_DATA_LABEL, noDataList, result)
    }

    static void handleGenericNonMatchingData1Value_TMP(String query, String label, List<Long> noDataList, Map<String, Object> result) {

        if (noDataList) {
            result.data.add([NO_DATA_ID, getMessage(label), noDataList.size()])

            result.dataDetails.add([
                    query : query,
                    id    : NO_DATA_ID,
                    label : getMessage(label),
                    idList: noDataList,
            ])
        }
    }

    static void handleGenericNonMatchingData2Values_TMP(String query, String label, List<Long> noDataIdList, Map<String, Object> result) {

        if (noDataIdList) {
            result.data.add([NO_DATA_ID, getMessage(label), noDataIdList.size()])

            result.dataDetails.add([
                    query : query,
                    id    : NO_DATA_ID,
                    label : getMessage(label),
                    idList: noDataIdList,
                    value1: 0,
                    value2: noDataIdList.size()
            ])
        }
    }

    static void handleGenericBooleanQuery(String query, String dataHql, String dataDetailsHql, List<Long> idList, Map<String, Object> result) {

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

    static void handleGenericDateQuery(String query, String dataHql, String dataDetailsHql, String nonMatchingHql, List<Long> idList, Map<String, Object> result) {

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

    static void handleGenericIdentifierXQuery(String query, String dataHqlPart, String dataDetailsHqlPart, String nonMatchingHql, List<Long> idList, Map<String, Object> result) {

        result.data = idList ? Org.executeQuery(
                dataHqlPart + " and ident.value is not null and trim(ident.value) != '' group by ns.id order by ns.ns",
                [idList: idList]
        ) : []

        result.data.each { d ->
            List<Long> objIdList = Org.executeQuery(
                    dataDetailsHqlPart + " and ns.id = :d and ident.value is not null and trim(ident.value) != ''",
                    [idList: idList, d: d[0]]
            )
            d[1] = IdentifierNamespace.get(d[0]).getI10n('name') ?: GenericHelper.flagUnmatched(d[1])

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
        List<Long> noDataList = nonMatchingIdList ? Org.executeQuery( nonMatchingHql, [idList: nonMatchingIdList] ) : []

        handleGenericNonMatchingData2Values_TMP(query, NO_IDENTIFIER_LABEL, noDataList, result)
    }

    static void handleGenericPropertyXQuery(String query, String dataHqlPart, String dataDetailsHqlPart, List<Long> idList, Org ctxOrg, Map<String, Object> result) {

        String locale = I10nTranslation.decodeLocale(LocaleContextHolder.getLocale())

        result.data = idList ? Org.executeQuery(
                dataHqlPart + " and (prop.tenant = :ctxOrg or prop.isPublic = true) and pd.descr like '%Property' group by pd.id order by pd.name_" + locale,
                [idList: idList, ctxOrg: ctxOrg]
        ) : []

        result.data.each { d ->
            d[1] = PropertyDefinition.get(d[0]).getI10n('name')

            List<Long> obj2IdList =  Org.executeQuery(
                    dataDetailsHqlPart + ' and (prop.isPublic = true) and pd.id = :d order by pd.name_' + locale,
                    [idList: idList, d: d[0]]
            )
            List<Long> obj3IdList =  Org.executeQuery(
                    dataDetailsHqlPart + ' and (prop.tenant = :ctxOrg and prop.isPublic != true) and pd.id = :d order by pd.name_' + locale,
                    [idList: idList, d: d[0], ctxOrg: ctxOrg]
            )
            int obj2IdListSize = obj2IdList.size()
            int obj3IdListSize = obj3IdList.size()
            result.dataDetails.add([
                    query : query,
                    id    : d[0],
                    label : d[1],
                    idList: obj3IdList + obj2IdList,
                    value1: obj3IdList.unique().size() + obj2IdList.unique().size(),
                    value2: obj2IdListSize,
                    value3: obj3IdListSize
            ])
        }
    }

    static void handleGenericAnnualXQuery(String query, String domainClass, List<Long> idList, Map<String, Object> result) {

        List dd = Org.executeQuery( 'select min(YEAR(dc.startDate)), max(YEAR(dc.endDate)) from ' + domainClass + ' dc where dc.id in (:idList)', [idList: idList] )
        dd[0][1] = dd[0][1] ? Math.min( dd[0][1] as int, Year.now().value + 5 ) : Year.now().value
        List years = ( (dd[0][0] ?: Year.now().value)..(dd[0][1]) ).toList()

        years.sort().each { y ->
            String hql = 'select dc.id from ' + domainClass + ' dc where dc.id in (:idList) and ' +
                    '( (YEAR(dc.startDate) <= ' + y + ') and (YEAR(dc.endDate) >= ' + y + ' or dc.endDate is null) ) and ' +
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

        List<Long> sp1DataList = Org.executeQuery( 'select dc.id from ' + domainClass + ' dc where dc.id in (:idList) and dc.startDate != null and dc.endDate is null', [idList: idList] )
        if (sp1DataList) {
            result.data.add([FAKE_DATA_ID_1, getMessage(NO_ENDDATE_LABEL), sp1DataList.size()])

            result.dataDetails.add([
                    query : query,
                    id    : FAKE_DATA_ID_1,
                    label : getMessage(NO_ENDDATE_LABEL),
                    idList: sp1DataList
            ])
        }

        List<Long> sp2DataList = Org.executeQuery( 'select dc.id from ' + domainClass + ' dc where dc.id in (:idList) and dc.startDate is null and dc.endDate != null', [idList: idList] )
        if (sp2DataList) {
            result.data.add([FAKE_DATA_ID_2, getMessage(NO_STARTDATE_LABEL), sp2DataList.size()])

            result.dataDetails.add([
                    query : query,
                    id    : FAKE_DATA_ID_2,
                    label : getMessage(NO_STARTDATE_LABEL),
                    idList: sp2DataList
            ])
        }

        List<Long> noDataList = Org.executeQuery( 'select dc.id from ' + domainClass + ' dc where dc.id in (:idList) and dc.startDate is null and dc.endDate is null', [idList: idList] )

        handleGenericNonMatchingData1Value_TMP(query, NO_DATA_LABEL, noDataList, result)
    }

    static String getMessage(String token) {
        MessageSource messageSource = Holders.grailsApplication.mainContext.getBean('messageSource')
        Locale locale = LocaleContextHolder.getLocale()

        // println ' ---> ' + 'reporting.query.base.' + token
        messageSource.getMessage('reporting.query.base.' + token, null, locale)
    }
}

package de.laser.reporting

import de.laser.Org
import de.laser.RefdataValue
import grails.web.servlet.mvc.GrailsParameterMap

class SubscriptionQuery extends GenericQuery {

    static List<String> PROPERTY_QUERY = [ 'select p.id, p.value_de, count(*) ', ' group by p.id, p.value_de order by p.value_de' ]

    static Map<String, Object> query(GrailsParameterMap params) {

        Map<String, Object> result = [
                chart    : params.chart,
                query    : params.query,
                data     : [],
                dataDetails : []
        ]

        String prefix = params.query.split('-')[0]
        List idList = params.list(prefix + 'IdList[]').collect { it as Long }

        if (! idList) {
        }
        else if ( params.query in ['subscription-form']) {

            processSimpleRefdataQuery(params.query,'form', idList, result)
        }
        else if ( params.query in ['subscription-kind']) {

            processSimpleRefdataQuery(params.query,'kind', idList, result)
        }
        else if ( params.query in ['subscription-resource']) {

            processSimpleRefdataQuery(params.query,'resource', idList, result)
        }
        else if ( params.query in ['subscription-status']) {

            processSimpleRefdataQuery(params.query,'status', idList, result)
        }

        result
    }

    static void processSimpleRefdataQuery(String query, String refdata, List idList, Map<String, Object> result) {

        result.data = Org.executeQuery(
                PROPERTY_QUERY[0] + 'from Subscription s join s.' + refdata + ' p where s.id in (:idList)' + PROPERTY_QUERY[1], [idList: idList]
        )
        result.data.each { d ->
            result.dataDetails.add( [
                    query:  query,
                    id:     d[0],
                    label:  RefdataValue.get(d[0]).getI10n('value'),
                    idList: Org.executeQuery(
                        'select s.id from Subscription s join s.' + refdata + ' p where s.id in (:idList) and p.id = :d order by s.name',
                        [idList: idList, d: d[0]]
                    )
            ])
        }

        handleNonMatchingData(
                query,
                'select distinct s.id from Subscription s where s.id in (:idList) and s.'+ refdata + ' is null',
                idList,
                result
        )
    }
}

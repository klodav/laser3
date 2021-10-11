package de.laser.reporting.export.local

import de.laser.*
import de.laser.reporting.report.ReportingCache
import de.laser.reporting.export.base.BaseDetailsExport
import de.laser.reporting.export.base.BaseExportHelper
import de.laser.reporting.report.myInstitution.GenericHelper

class ExportLocalHelper extends BaseExportHelper {

    static BaseDetailsExport createExport(String token, Map<String, Object> selectedFields) {

        String tmpl = getCachedExportStrategy( token )

        if (tmpl == OrgExport.KEY) {
            return new OrgExport( token, selectedFields )
        }
        else if (tmpl == IssueEntitlementExport.KEY) {
            return new IssueEntitlementExport( token, selectedFields )
        }
        else if (tmpl == SubscriptionExport.KEY) {
            return new SubscriptionExport( token, selectedFields )
        }
        else if (tmpl == CostItemExport.KEY) {
            return new CostItemExport( token, selectedFields )
        }
    }

    // ----- Cache -----

    static Map<String, Object> getFilterCache(String token) {
        ReportingCache rCache = new ReportingCache( ReportingCache.CTX_SUBSCRIPTION )
        Map<String, Object> cacheMap = rCache.get()

        cacheMap.filterCache as Map<String, Object>
    }

    static Map<String, Object> getQueryCache(String token) {
        ReportingCache rCache = new ReportingCache( ReportingCache.CTX_SUBSCRIPTION )
        Map<String, Object> cacheMap = rCache.get()

        cacheMap.queryCache as Map<String, Object>
    }

    static Map<String, Object> getDetailsCache(String token) {
        ReportingCache rCache = new ReportingCache( ReportingCache.CTX_SUBSCRIPTION )
        Map<String, Object> cacheMap = rCache.get()

        cacheMap.detailsCache as Map<String, Object>
    }

    static String getCachedExportStrategy(String token) {

        Map<String, Object> detailsCache = getDetailsCache(token)
        List parts = detailsCache.tmpl.split('/')
        parts[parts.size()-1]
    }

    static String getCachedConfigStrategy(String token) {

        Map<String, Object> detailsCache = getDetailsCache(token)
        List<String> queryParts = detailsCache.query.split('-')
        queryParts.size() == 3 ? queryParts[2] : queryParts[0]
    }

    static String getCachedFieldStrategy(String token) {

        Map<String, Object> detailsCache = getDetailsCache(token)
        detailsCache.query.substring( detailsCache.query.indexOf('-') + 1 )
    }

    // -----

//    static Map<String, Object> getCachedFilterLabels(String token) {
//
//        Map<String, Object> filterCache = getFilterCache(token)
//        filterCache.labels as Map<String, Object>
//    }

    static String getCachedFilterResult(String token) {

        Map<String, Object> filterCache = getFilterCache(token)
        filterCache.result
    }

    static List<String> getCachedQueryLabels(String token) {

        Map<String, Object> queryCache = getQueryCache(token)
        queryCache.labels.labels as List<String>
    }

    // -----

    static String getFieldLabel(BaseDetailsExport export, String fieldName) {

        if ( isFieldMultiple(fieldName) ) {
            // String label = BaseDetailsExport.CUSTOM_LABEL.get(fieldName)
            String label = BaseDetailsExport.getMessage(fieldName)

            if (fieldName == 'x-identifier' || fieldName == '@ae-entitlement-tippIdentifier') {
                List<Long> selList = export.getSelectedFields().get(fieldName) as List<Long>
                label += (selList ? ': ' + selList.collect{it ->
                    IdentifierNamespace idns = IdentifierNamespace.get(it)
                    idns.getI10n('name') ?: idns.ns + ' *'
                }.join(', ') : '')
            }
            else if (fieldName == '@ae-org-accessPoint') {
                List<Long> selList = export.getSelectedFields().get(fieldName) as List<Long>
                label += (selList ? ': ' + selList.collect{it -> RefdataValue.get(it).getI10n('value') }.join(', ') : '') // TODO - export
            }
            else if (fieldName == '@ae-org-contact') {
                List<Long> selList = export.getSelectedFields().get(fieldName) as List<Long>
                label += (selList ? ': ' + selList.collect{it -> RefdataValue.get(it).getI10n('value') }.join(', ') : '') // TODO - export
            }
            else if (fieldName == '@ae-org-readerNumber') {
                List selList = export.getSelectedFields().get(fieldName)
                List semList = selList.findAll{ it.startsWith('sem-') }.collect{ RefdataValue.get( it.replace('sem-', '') ).getI10n('value') }
                List ddList  = selList.findAll{ it.startsWith('dd-') }.collect{ it.replace('dd-', 'Stichtage ') }
                label += (selList ? ': ' + (semList + ddList).join(', ') : '') // TODO - export
            }

            return label
        }
        else if (fieldName == 'x-property') {
            return 'Merkmal: ' + getQueryCache( export.token ).labels.labels[2] // TODO - modal
        }
        else if (BaseDetailsExport.CUSTOM_FIELD_KEY.contains(fieldName)) {
            return BaseDetailsExport.getMessage(fieldName)
        }

        // --- adapter ---

        String cfg = getCachedConfigStrategy( export.token )
        Map<String, Object> objConfig = export.getCurrentConfig( export.KEY ).base

        if (! objConfig.fields.keySet().contains(cfg)) {
            cfg = 'default'
        }
        Map<String, Object> objConfig2 = [
                meta   : objConfig.meta,
                fields : objConfig.fields.get(cfg)
        ]

        GenericHelper.getFieldLabel( objConfig2, fieldName )
    }
}

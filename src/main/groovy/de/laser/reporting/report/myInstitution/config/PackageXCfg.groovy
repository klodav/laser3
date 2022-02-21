package de.laser.reporting.report.myInstitution.config

import de.laser.Org
import de.laser.Platform
import de.laser.helper.RDConstants
import de.laser.reporting.report.myInstitution.base.BaseConfig

class PackageXCfg extends BaseConfig {

    static Map<String, Object> CONFIG = [

            base : [
                    meta : [
                            class:  de.laser.Package,
                            cfgKey: KEY_PACKAGE
                    ],
                    source : [
                            'all-pkg',
                            'my-pkg'
                    ],
                    fields: [
                            'breakable'         : [ FIELD_TYPE_ELASTICSEARCH ],
                            //'consistent'        : [ FIELD_TYPE_ELASTICSEARCH ],
                            'contentType'       : [ FIELD_TYPE_REFDATA ],
                            'file'              : [ FIELD_TYPE_REFDATA ],
                            'openAccess'        : [ FIELD_TYPE_ELASTICSEARCH ],
                            'packageStatus'             : [ FIELD_TYPE_REFDATA ],
                            'subscription$status'       : [ FIELD_TYPE_CUSTOM_IMPL ],
                            'paymentType'               : [ FIELD_TYPE_ELASTICSEARCH ],
                            'package$platform'          : [ FIELD_TYPE_CUSTOM_IMPL, FIELD_IS_MULTIPLE ],
                            'package$orgRole$provider'  : [ FIELD_TYPE_CUSTOM_IMPL, FIELD_IS_MULTIPLE ],
                            'scope'                     : [ FIELD_TYPE_ELASTICSEARCH ]
                    ],
                    filter : [
                            default: [
                                    [ 'contentType', 'packageStatus' ],
                                    [ 'file', 'package$orgRole$provider', 'package$platform' ],
                                    [ 'breakable', 'scope' ],
                                    [ 'paymentType', 'openAccess']
                            ],
                            my: [
                                    [ 'contentType',  'subscription$status', 'packageStatus' ],
                                    [ 'file', 'package$orgRole$provider', 'package$platform' ],
                                    [ 'breakable', 'scope' ],
                                    [ 'paymentType', 'openAccess']
                            ]
                    ],
                    query : [
                            default: [
                                    package : [
                                            'package-contentType' :     [ '@' ],
                                            'package-packageStatus' :   [ '@' ],
                                            'package-file' :            [ '@' ],
                                            'package-breakable' :       [ '@' ],    // ES
                                            'package-paymentType' :     [ '@' ],    // ES
                                            'package-openAccess' :      [ '@' ],    // ES
                                            'package-consistent' :      [ '@' ],    // ES
                                            'package-scope' :           [ '@' ],    // ES
                                            'package-*' :               [ 'generic.all' ]
                                    ]
                            ]
                    ],
                    distribution : [
                            default : [
                                    /* 'package-x-identifier' : [
                                            detailsTemplate     : 'package',
                                            chartTemplate       : '2axis2values_nonMatches',
                                            chartLabels         : [ 'base', 'x.identifiers' ]
                                    ], */
                                    'package-x-id' : [ // ES
                                            detailsTemplate     : 'package',
                                            chartTemplate       : 'generic',
                                            chartLabels         : []
                                    ],
                                    'package-x-provider' : [
                                            detailsTemplate     : 'package',
                                            chartTemplate       : '2axis2values_nonMatches',
                                            chartLabels         : [ 'x.providers.1', 'x.providers.2' ]
                                    ],
                                    'package-x-platform' : [
                                            detailsTemplate     : 'package',
                                            chartTemplate       : 'generic',
                                            chartLabels         : []
                                    ],
                                    'package-x-platformProvider' : [
                                            detailsTemplate     : 'package',
                                            chartTemplate       : 'generic',
                                            chartLabels         : []
                                    ],
                                    'package-x-curatoryGroup' : [ // ES
                                            detailsTemplate     : 'package',
                                            chartTemplate       : 'generic',
                                            chartLabels         : []
                                    ],
                                    'package-x-ddc' : [  // ES
                                             detailsTemplate     : 'package',
                                             chartTemplate       : 'generic',
                                             chartLabels         : []
                                    ],
                                    'package-x-language' : [
                                            detailsTemplate     : 'package',
                                            chartTemplate       : 'generic',
                                            chartLabels         : []
                                    ],
                                    'package-x-nationalRange' : [   // ES
                                            detailsTemplate     : 'package',
                                            chartTemplate       : 'generic',
                                            chartLabels         : []
                                    ],
                                    'package-x-regionalRange' : [   // ES
                                            detailsTemplate     : 'package',
                                            chartTemplate       : 'generic',
                                            chartLabels         : []
                                    ]
                            ]
                    ]
            ],

            provider : [
                    meta : [
                            class:  Org,
                            cfgKey: KEY_PACKAGE
                    ],
                    source : [
                            'filter-restricting-provider'
                    ],
                    fields : [ ],
                    filter : [
                            default : []
                    ],
                    query : [
                            default : [
                                    provider : [
                                            'provider-orgType' : [ 'generic.org.orgType' ],
                                            'provider-*' :       [ 'generic.all' ],
                                    ]
                            ]
                    ]
            ],

            platform : [
                    meta : [
                            class:  Platform,
                            cfgKey: KEY_PACKAGE
                    ],
                    source : [
                            'filter-restricting-platform'
                    ],
                    fields : [ ],
                    filter : [
                            default : []
                    ],
                    query : [
                            default : [
                                    platform : [
                                            'platform-x-org' :              [ '@' ],    // KEY_PLATFORM -> distribution
                                            'platform-serviceProvider' :    [ '@' ],
                                            'platform-softwareProvider' :   [ '@' ],
                                            'platform-*' :                  [ 'generic.all' ]
                                    ]
                            ]
                    ]
            ]
    ]

    static Map<String, Map> ES_DATA = [

            'package-altname'           : [                 export: true,   label: 'package.show.altname'],

            'package-breakable'         : [filter: true,    export: true,   label: 'package.breakable',          rdc: RDConstants.PACKAGE_BREAKABLE],
            'package-consistent'        : [filter: true,    export: true,   label: 'package.consistent',         rdc: RDConstants.PACKAGE_CONSISTENT],
            'package-description'       : [                 export: true,   label: 'package.description.label'],
            'package-descriptionURL'    : [                 export: true,   label: 'package.descriptionURL.label'],
            'package-openAccess'        : [filter: true,    export: true,   label: 'package.openAccess.label',   rdc: RDConstants.LICENSE_OA_TYPE],
            'package-paymentType'       : [filter: true,    export: true,   label: 'package.paymentType.label',  rdc: RDConstants.PAYMENT_TYPE],
            'package-scope'             : [filter: true,    export: true,   label: 'package.scope.label',        rdc: RDConstants.PACKAGE_SCOPE],

            'package-x-curatoryGroup'   : [                 export: true,   mapping: 'curatoryGroups',  label: 'package.curatoryGroup.label'],
            'package-x-id'              : [                 export: true,   mapping: 'identifiers',     label: 'identifier.label'],
            'package-x-nationalRange'   : [                                 mapping: 'nationalRanges',  label: 'package.nationalRange.label'],
            'package-x-regionalRange'   : [                                 mapping: 'regionalRanges',  label: 'package.regionalRange.label'],
            'package-x-ddc'             : [                 export: true,   mapping: 'ddcs',            label: 'package.ddc.label', rdc: RDConstants.DDC]
    ]

    static Map<String, Boolean> DETAILS_TABLE_CONFIG = [

            'name' : true,
            'sortname' : false,
            'package-altname' : false, // ES
            'package-x-id' : false, // ES
            'contentType' : false,
            'packageStatus' : false,
            'package$orgRole$provider' : true,
            'package$platform' : true,
            'file' : false,
            '_+_currentTitles' : true, // virtual
            'package-breakable' : false, // ES
            'package-paymentType' : false, // ES
            'package-openAccess' : false, // ES
            'package-consistent' : false, // ES
            'package-scope' : false, // ES
            'package-x-ddc' : false, // ES
            'package-x-nationalRange' : false, // ES
            'package-x-regionalRange' : false, // ES
            'language' : false,
            'package-description' : false, // ES
            'package-descriptionURL' : false, // ES
            'package-x-curatoryGroup' : false, // ES
            '_+_lastUpdated' : true, // virtual
            '_+_wekb' : true // virtual
    ]
}

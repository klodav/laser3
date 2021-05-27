package com.k_int.kbplus

import de.laser.ApiSource
import de.laser.Contact
import de.laser.DeweyDecimalClassification
import de.laser.EscapeService
import de.laser.GlobalRecordSource
import de.laser.Identifier
import de.laser.IssueEntitlement
import de.laser.Language
import de.laser.Org
import de.laser.OrgRole
import de.laser.Package
import de.laser.PendingChange
import de.laser.Person
import de.laser.PersonRole
import de.laser.Platform
import de.laser.RefdataCategory
import de.laser.RefdataValue
import de.laser.Subscription
import de.laser.SubscriptionPackage
import de.laser.TitleInstancePackagePlatform
import de.laser.finance.PriceItem
import de.laser.system.SystemEvent
import de.laser.base.AbstractCoverage
import de.laser.IssueEntitlementCoverage
import de.laser.PendingChangeConfiguration
import de.laser.TIPPCoverage
import de.laser.exceptions.SyncException
import de.laser.helper.DateUtils
import de.laser.helper.RDConstants
import de.laser.helper.RDStore
import de.laser.interfaces.AbstractLockableService
import de.laser.titles.TitleHistoryEvent
import grails.converters.JSON
import grails.gorm.transactions.Transactional
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService

/**
 * Implements the synchronisation workflow according to https://dienst-wiki.hbz-nrw.de/display/GDI/GOKB+Sync+mit+LASER
 */
@Transactional
class GlobalSourceSyncService extends AbstractLockableService {

    ExecutorService executorService
    PendingChangeService pendingChangeService
    def genericOIDService
    EscapeService escapeService
    GlobalRecordSource source
    ApiSource apiSource

    final static long RECTYPE_PACKAGE = 0
    final static long RECTYPE_TITLE = 1
    final static long RECTYPE_ORG = 2
    final static long RECTYPE_TIPP = 3

    Map<String, RefdataValue> titleMedium = [:],
            tippStatus = [:],
            packageStatus = [:],
            orgStatus = [:],
            orgTypes = [:],
            currency = [:],
            ddc = [:],
            contactTypes = [:]
    Long maxTimestamp
    Map<String,Integer> initialPackagesCounter = [:]
    Map<String,Set<Map<String,Object>>> pkgPropDiffsContainer = [:]
    Map<String,Set<Map<String,Object>>> packagesToNotify = [:]

    boolean running = false

    /**
     * This is the entry point for triggering the sync workflow. To ensure locking, a flag will be set when a process is already running
     * @return a flag whether a process is already running
     */
    boolean startSync() {
        if (!running) {
            executorService.execute({ doSync() })
            //doSync()
            return true
        }
        else {
            log.warn("Sync already running, not starting again")
            return false
        }
    }

    /**
     * The sync process wrapper. It takes every {@link GlobalRecordSource}, fetches the information since a given timestamp
     * and updates the local records
     */
    void doSync() {
        running = true
        defineMapFields()
        //we need to consider that there may be several sources per instance
        List<GlobalRecordSource> jobs = GlobalRecordSource.findAllByActive(true)
        jobs.each { GlobalRecordSource source ->
            this.source = source
            maxTimestamp = 0
            try {
                SystemEvent.createEvent('GSSS_JSON_START',['jobId':source.id])
                Thread.currentThread().setName("GlobalDataSync_Json")
                this.apiSource = ApiSource.findByTypAndActive(ApiSource.ApiTyp.GOKBAPI,true)
                Date oldDate = source.haveUpTo
                log.info("getting records from job #${source.id} with uri ${source.uri} since ${oldDate}")
                SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
                String componentType
                /*
                    structure:
                    { packageUUID: [
                        diffs of tipp 1 concerned,
                        diffs of tipp 2 concerned,
                        ...
                        diffs of tipp n concerned
                        ]
                    }
                */
                switch(source.rectype) {
                    case RECTYPE_ORG: componentType = 'Org'
                        break
                    case RECTYPE_TIPP: componentType = 'TitleInstancePackagePlatform'
                        break
                }
                //do prequest: are we needing the scroll api?
                //5000 records because of local testing ability
                Map<String,Object> result = fetchRecordJSON(false,[componentType:componentType,changedSince:sdf.format(oldDate),max:5000])
                if(result.error == 404) {
                    log.error("we:kb server is down")
                    SystemEvent.createEvent('GSSS_JSON_ERROR',['jobId':source.id])
                }
                else {
                    if(result) {
                        processScrollPage(result, componentType)
                    }
                    else {
                        log.info("no records updated - leaving everything as is ...")
                    }
                    if(maxTimestamp+1000 > source.haveUpTo.getTime()) {
                        log.debug("old ${sdf.format(source.haveUpTo)}")
                        source.haveUpTo = new Date(maxTimestamp + 1000)
                        log.debug("new ${sdf.format(source.haveUpTo)}")
                        if (!source.save())
                            log.error(source.getErrors().getAllErrors().toListString())
                    }
                    if(source.rectype == RECTYPE_TIPP) {
                        if(packagesToNotify.keySet().size() > 0) {
                            log.info("notifying subscriptions ...")
                            trackPackageHistory()
                            //get subscription packages and their respective holders, parent level only!
                            String query = 'select oo.org,sp from SubscriptionPackage sp join sp.pkg pkg ' +
                                    'join sp.subscription s ' +
                                    'join s.orgRelations oo ' +
                                    'where s.instanceOf = null and pkg.gokbId in (:packages) ' +
                                    'and oo.roleType in (:roleTypes)'
                            List subPkgHolders = SubscriptionPackage.executeQuery(query,[packages:packagesToNotify.keySet(),roleTypes:[RDStore.OR_SUBSCRIPTION_CONSORTIA,RDStore.OR_SUBSCRIBER]])
                            subPkgHolders.each { row ->
                                Org org = (Org) row[0]
                                SubscriptionPackage sp = (SubscriptionPackage) row[1]
                                autoAcceptPendingChanges(org,sp)
                                //nonAutoAcceptPendingChanges(org, sp)
                            }
                        }
                        else {
                            log.info("no diffs recorded ...")
                        }
                    }
                    log.info("sync job finished")
                    SystemEvent.createEvent('GSSS_JSON_COMPLETE',['jobId':source.id])
                }
            }
            catch (Exception e) {
                SystemEvent.createEvent('GSSS_JSON_ERROR',['jobId':source.id])
                log.error("sync job has failed, please consult stacktrace as follows: ",e)
            }
        }
        running = false
    }

    void reloadData(String componentType) {
        running = true
        defineMapFields()
        executorService.execute({
            Thread.currentThread().setName("GlobalDataUpdate_${componentType}")
            this.source = GlobalRecordSource.findByActiveAndRectype(true,RECTYPE_TIPP)
            this.apiSource = ApiSource.findByTypAndActive(ApiSource.ApiTyp.GOKBAPI,true)
            log.info("getting all records from job #${source.id} with uri ${source.uri}")
            try {
                //5000 records because of local testing ability
                Map<String,Object> result = fetchRecordJSON(false,[componentType: componentType, max: 5000])
                if(result) {
                    if(result.error == 404) {
                        log.error("we:kb server currently down")
                    }
                    else
                        processScrollPage(result, componentType)
                }
            }
            catch (Exception e) {
                log.error("package reloading has failed, please consult stacktrace as follows: ",e)
            }
            running = false
        })
    }

    void processScrollPage(Map<String, Object> result, String componentType) throws SyncException {
        if(result.count >= 5000) {
            String scrollId
            boolean more = true
            while(more) {
                //actually, scrollId alone should do the trick but tests revealed that other parameters are necessary, too, because of current workaround solution
                log.debug("using scrollId ${scrollId}")
                if(scrollId) {
                    result = fetchRecordJSON(true, [component_type: componentType, scrollId: scrollId, max: 5000])
                }
                else {
                    result = fetchRecordJSON(true, [component_type: componentType, max: 5000])
                }
                if(result.error && result.error == 404) {
                    more = false
                }
                else if(result.count > 0) {
                    switch (source.rectype) {
                        case RECTYPE_ORG: result.records.each { record ->
                            createOrUpdateOrgJSON(record)
                        }
                            break
                        case RECTYPE_TIPP: updateRecords(result.records)
                            break
                    }
                    if(result.hasMoreRecords) {
                        scrollId = result.scrollId
                    }
                    else {
                        more = false
                    }
                }
                else {
                    //workaround until GOKb-ES migration is done and hopefully works ...
                    if(result.hasMoreRecords) {
                        scrollId = result.scrollId
                        log.info("page is empty, turning to next page ...")
                    }
                    else {
                        more = false
                        log.info("no records updated - leaving everything as is ...")
                    }
                }
            }
        }
        else if(result.count > 0 && result.count < 5000) {
            switch (source.rectype) {
                case RECTYPE_ORG: result.records.each { record ->
                    createOrUpdateOrgJSON(record)
                }
                    break
                case RECTYPE_TIPP: updateRecords(result.records)
                    break
            }
        }
        else if(result.error && result.error == 404) {
            log.error("we:kb server is down")
            throw new SyncException("we:kb server is unavailable!")
        }
    }

    void updateRecords(List<Map> rawRecords) {
        //necessary filter for DEV database
        List<Map> records = rawRecords.findAll { Map tipp -> tipp.containsKey("hostPlatformUuid") && tipp.containsKey("tippPackageUuid") }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        Set<String> platformUUIDs = records.collect { Map tipp -> tipp.hostPlatformUuid } as Set<String>
        log.debug("found platform UUIDs: ${platformUUIDs.toListString()}")
        Set<String> packageUUIDs = records.collect { Map tipp -> tipp.tippPackageUuid } as Set<String>
        log.debug("found package UUIDs: ${packageUUIDs.toListString()}")
        Set<String> tippUUIDs = records.collect { Map tipp -> tipp.uuid } as Set<String>
        Map<String,Package> packagesOnPage = [:]
        Map<String,Platform> platformsOnPage = [:]

        //packageUUIDs is null if package has no tipps
        Set<String> existingPackageUUIDs = packageUUIDs ? Platform.executeQuery('select pkg.gokbId from Package pkg where pkg.gokbId in (:pkgUUIDs)',[pkgUUIDs:packageUUIDs]) : []
        Map<String,TitleInstancePackagePlatform> tippsInLaser = [:]
        //collect existing TIPPs
        if(tippUUIDs) {
            TitleInstancePackagePlatform.findAllByGokbIdInList(tippUUIDs.toList()).each { TitleInstancePackagePlatform tipp ->
                tippsInLaser.put(tipp.gokbId, tipp)
            }
        }
        //create or update platforms
        platformUUIDs.each { String platformUUID ->
            try {
                platformsOnPage.put(platformUUID,createOrUpdatePlatformJSON(platformUUID))
            }
            catch (SyncException e) {
                log.error("Error on updating platform ${platformUUID}: ",e)
                SystemEvent.createEvent("GSSS_JSON_WARNING",[platformRecordKey:platformUUID])
            }
        }
        //create or update packages
        packageUUIDs.each { String packageUUID ->
            try {
                Package pkg = createOrUpdatePackage(packageUUID)
                if(pkg)
                    packagesOnPage.put(packageUUID,pkg)
            }
            catch (SyncException e) {
                log.error("Error on updating package ${packageUUID}: ",e)
                SystemEvent.createEvent("GSSS_JSON_WARNING",[packageRecordKey:packageUUID])
            }
        }

        records.eachWithIndex { Map tipp, int idx ->
            log.debug("now processing entry #${idx} with uuid ${tipp.uuid}")
            try {
                Map<String,Object> updatedTIPP = [
                    titleType: tipp.titleType,
                    name: tipp.name,
                    packageUUID: tipp.tippPackageUuid ?: null,
                    platformUUID: tipp.hostPlatformUuid ?: null,
                    titlePublishers: [],
                    publisherName: tipp.publisherName,
                    firstAuthor: tipp.firstAuthor ?: null,
                    firstEditor: tipp.firstEditor ?: null,
                    dateFirstInPrint: tipp.dateFirstInPrint ? DateUtils.parseDateGeneric(tipp.dateFirstInPrint) : null,
                    dateFirstOnline: tipp.dateFirstOnline ? DateUtils.parseDateGeneric(tipp.dateFirstOnline) : null,
                    imprint: tipp.titleImprint ?: null,
                    status: tipp.status,
                    seriesName: tipp.series ?: null,
                    subjectReference: tipp.subjectArea ?: null,
                    volume: tipp.volumeNumber ?: null,
                    coverages: [],
                    priceItems: [],
                    hostPlatformURL: tipp.url ?: null,
                    identifiers: [],
                    ddcs: [],
                    languages: [],
                    history: [],
                    uuid: tipp.uuid,
                    accessStartDate : tipp.accessStartDate ? DateUtils.parseDateGeneric(tipp.accessStartDate) : null,
                    accessEndDate   : tipp.accessEndDate ? DateUtils.parseDateGeneric(tipp.accessEndDate) : null,
                    medium: tipp.medium
                ]
                if(tipp.titleType == 'Journal') {
                    tipp.coverage.each { cov ->
                        updatedTIPP.coverages << [
                                startDate: cov.startDate ? DateUtils.parseDateGeneric(cov.startDate) : null,
                                endDate: cov.endDate ? DateUtils.parseDateGeneric(cov.endDate) : null,
                                startVolume: cov.startVolume ?: null,
                                endVolume: cov.endVolume ?: null,
                                startIssue: cov.startIssue ?: null,
                                endIssue: cov.endIssue ?: null,
                                coverageDepth: cov.coverageDepth ?: null,
                                coverageNote: cov.coverageNote ?: null,
                                embargo: cov.embargo ?: null
                        ]
                    }
                    updatedTIPP.coverages = updatedTIPP.coverages.toSorted { a, b -> a.startDate <=> b.startDate }
                }
                tipp.prices.each { price ->
                    updatedTIPP.priceItems << [
                            listPrice: escapeService.parseFinancialValue(price.amount),
                            startDate: DateUtils.parseDateGeneric(price.startDate),
                            endDate: DateUtils.parseDateGeneric(price.endDate),
                            listCurrency: currency.get(price.currency)
                    ]
                }
                tipp.titlePublishers.each { publisher ->
                    updatedTIPP.titlePublishers << [uuid:publisher.uuid,name:publisher.name]
                }
                tipp.identifiers.each { identifier ->
                    updatedTIPP.identifiers << [namespace:identifier.namespace, value: identifier.value, name_de: identifier.namespaceName]
                }
                tipp.ddcs.each { ddcData ->
                    updatedTIPP.ddcs << ddc.get(ddcData.value)
                }
                tipp.languages.each { langData ->
                    updatedTIPP.languages << RefdataValue.getByValueAndCategory(langData.value, RDConstants.LANGUAGE_ISO)
                }
                tipp.titleHistory.each { historyEvent ->
                    updatedTIPP.history << [date:DateUtils.parseDateGeneric(historyEvent.date),from:historyEvent.from,to:historyEvent.to]
                }
                if(updatedTIPP.packageUUID in existingPackageUUIDs) {
                    Map<String,Object> diffs = createOrUpdateTIPP(tippsInLaser.get(updatedTIPP.uuid),updatedTIPP,packagesOnPage,platformsOnPage)
                    Set<Map<String,Object>> diffsOfPackage = packagesToNotify.get(updatedTIPP.packageUUID)
                    if(!diffsOfPackage) {
                        diffsOfPackage = []
                    }
                    diffsOfPackage << diffs
                    if(pkgPropDiffsContainer.get(updatedTIPP.packageUUID)) {
                        diffsOfPackage.addAll(pkgPropDiffsContainer.get(updatedTIPP.packageUUID))
                    }//test with set, otherwise make check
                    packagesToNotify.put(updatedTIPP.packageUUID,diffsOfPackage)
                }
                else if(updatedTIPP.status != RDStore.TIPP_STATUS_DELETED.value) {
                    Package pkg = packagesOnPage.get(updatedTIPP.packageUUID)
                    if(pkg)
                        addNewTIPP(pkg, updatedTIPP, platformsOnPage)
                }
                Date lastUpdatedTime = DateUtils.parseDateGeneric(tipp.lastUpdatedDisplay)
                if(lastUpdatedTime.getTime() > maxTimestamp) {
                    maxTimestamp = lastUpdatedTime.getTime()
                }
            }
            catch (SyncException e) {
                log.error("Error on updating tipp ${tipp.uuid}: ",e)
                SystemEvent.createEvent("GSSS_JSON_WARNING",[tippRecordKey:tipp.uuid])
            }
        }
    }

    /**
     * This records the package changes so that subscription holders may decide whether they apply them or not
     * @param packagesToTrack
     */
    void trackPackageHistory() {
        //Package.withSession { Session sess ->
            //loop through all packages
            packagesToNotify.each { String packageUUID, Set<Map<String,Object>> diffsOfPackage ->
                if(diffsOfPackage.find { Map<String,Object> diff -> diff.event in [PendingChangeConfiguration.NEW_TITLE,PendingChangeConfiguration.TITLE_DELETED] }) {
                    int newCount = TitleInstancePackagePlatform.executeQuery('select count(tipp.id) from TitleInstancePackagePlatform tipp where tipp.pkg.gokbId = :gokbId',[gokbId:packageUUID])[0]
                    String newValue = newCount.toString()
                    String oldValue = initialPackagesCounter.get(packageUUID).toString()
                    PendingChange.construct([msgToken:PendingChangeConfiguration.PACKAGE_TIPP_COUNT_CHANGED,target:Package.findByGokbId(packageUUID),status:RDStore.PENDING_CHANGE_HISTORY,prop:"tippCount",newValue:newValue,oldValue:oldValue])
                }
                //println("diffsOfPackage:"+diffsOfPackage)
                diffsOfPackage.each { Map<String,Object> diff ->
                    log.debug(diff.toMapString())
                    //[event:update, target:de.laser.TitleInstancePackagePlatform : 196477, diffs:[[prop:price, priceDiffs:[[event:add, target:de.laser.finance.PriceItem : 10791]]]]]
                    switch(diff.event) {
                        case 'add': PendingChange.construct([msgToken:PendingChangeConfiguration.NEW_TITLE,target:diff.target,status:RDStore.PENDING_CHANGE_HISTORY])
                            break
                        case 'update':
                            diff.diffs.each { tippDiff ->
                                switch(tippDiff.prop) {
                                    case 'coverage': tippDiff.covDiffs.each { covEntry ->
                                        switch(covEntry.event) {
                                            case 'add': PendingChange.construct([msgToken:PendingChangeConfiguration.NEW_COVERAGE,target:covEntry.target,status:RDStore.PENDING_CHANGE_HISTORY])
                                                break
                                            case 'update': covEntry.diffs.each { covDiff ->
                                                    PendingChange.construct([msgToken: PendingChangeConfiguration.COVERAGE_UPDATED, target: covEntry.target, status: RDStore.PENDING_CHANGE_HISTORY, prop: covDiff.prop, newValue: covDiff.newValue, oldValue: covDiff.oldValue])
                                                    //log.debug("tippDiff.covDiffs.covDiff: " + covDiff)
                                                }
                                                break
                                            case 'delete': JSON oldMap = covEntry.target.properties as JSON
                                                PendingChange.construct([msgToken:PendingChangeConfiguration.COVERAGE_DELETED, target:covEntry.targetParent, oldValue: oldMap.toString() , status:RDStore.PENDING_CHANGE_HISTORY])
                                                break
                                        }
                                    }
                                        break
                                    case 'price': tippDiff.priceDiffs.each { priceEntry ->
                                        switch(priceEntry.event) {
                                            case 'add': PendingChange.construct([msgToken:PendingChangeConfiguration.NEW_PRICE,target:priceEntry.target,status:RDStore.PENDING_CHANGE_HISTORY])
                                                break
                                            case 'update':
                                                priceEntry.diffs.each { priceDiff ->
                                                    PendingChange.construct([msgToken: PendingChangeConfiguration.PRICE_UPDATED, target: priceEntry.target, status: RDStore.PENDING_CHANGE_HISTORY, prop: priceDiff.prop, newValue: priceDiff.newValue, oldValue: priceDiff.oldValue])
                                                }
                                                //log.debug("tippDiff.priceDiffs: "+ priceEntry)
                                                break
                                            case 'delete': JSON oldMap = priceEntry.target.properties as JSON
                                                PendingChange.construct([msgToken:PendingChangeConfiguration.PRICE_DELETED,target:priceEntry.targetParent,oldValue:oldMap.toString(),status:RDStore.PENDING_CHANGE_HISTORY])
                                                break
                                        }
                                    }
                                        break
                                    default:
                                        PendingChange.construct([msgToken:PendingChangeConfiguration.TITLE_UPDATED,target:diff.target,status:RDStore.PENDING_CHANGE_HISTORY,prop:tippDiff.prop,newValue:tippDiff.newValue,oldValue:tippDiff.oldValue])
                                        break
                                }
                            }
                            break
                        case 'delete': PendingChange.construct([msgToken:PendingChangeConfiguration.TITLE_DELETED,target:diff.target,status:RDStore.PENDING_CHANGE_HISTORY])
                            break
                    }
                    //PendingChange.construct([msgToken,target,status,prop,newValue,oldValue])
                }
                //sess.flush()
            }
        //}
    }

    void autoAcceptPendingChanges(Org contextOrg, SubscriptionPackage subPkg) {
        //get for each subscription package the tokens which should be accepted
        String query = 'select pcc.settingKey from PendingChangeConfiguration pcc join pcc.subscriptionPackage sp where pcc.settingValue = :accept and sp = :sp and pcc.settingKey not in (:excludes)'
        List<String> pendingChangeConfigurations = PendingChangeConfiguration.executeQuery(query,[accept:RDStore.PENDING_CHANGE_CONFIG_ACCEPT,sp:subPkg,excludes:[PendingChangeConfiguration.NEW_PRICE,PendingChangeConfiguration.PRICE_DELETED,PendingChangeConfiguration.PRICE_UPDATED]])
        if(pendingChangeConfigurations) {
            Map<String,Object> changeParams = [pkg:subPkg.pkg,history:RDStore.PENDING_CHANGE_HISTORY,subscriptionJoin:subPkg.dateCreated,msgTokens:pendingChangeConfigurations]
            Set<PendingChange> newChanges = [],
                               acceptedChanges = PendingChange.findAllByOidAndStatusAndMsgTokenIsNotNull(genericOIDService.getOID(subPkg.subscription),RDStore.PENDING_CHANGE_ACCEPTED)
            newChanges.addAll(PendingChange.executeQuery('select pc from PendingChange pc join pc.tipp tipp join tipp.pkg pkg where pkg = :pkg and pc.status = :history and pc.ts > :subscriptionJoin and pc.msgToken in (:msgTokens)',changeParams))
            newChanges.addAll(PendingChange.executeQuery('select pc from PendingChange pc join pc.tippCoverage tc join tc.tipp tipp join tipp.pkg pkg where pkg = :pkg and pc.status = :history and pc.ts > :subscriptionJoin and pc.msgToken in (:msgTokens)',changeParams))
            //newChanges.addAll(PendingChange.executeQuery('select pc from PendingChange pc join pc.priceItem pi join pi.tipp tipp join tipp.pkg pkg where pkg = :pkg and pc.status = :history and pc.ts > :subscriptionJoin and pc.msgToken in (:msgTokens)',changeParams))
            newChanges.each { PendingChange newChange ->
                boolean processed = false
                if(newChange.tipp) {
                    if(newChange.targetProperty)
                        processed = acceptedChanges.find { PendingChange accepted -> accepted.tipp == newChange.tipp && accepted.msgToken == newChange.msgToken && accepted.targetProperty == newChange.targetProperty } != null
                    else
                        processed = acceptedChanges.find { PendingChange accepted -> accepted.tipp == newChange.tipp && accepted.msgToken == newChange.msgToken } != null
                }
                else if(newChange.tippCoverage) {
                    if(newChange.targetProperty)
                        processed = acceptedChanges.find { PendingChange accepted -> accepted.tippCoverage == newChange.tippCoverage && accepted.msgToken == newChange.msgToken && accepted.targetProperty == newChange.targetProperty } != null
                    else
                        processed = acceptedChanges.find { PendingChange accepted -> accepted.tippCoverage == newChange.tippCoverage && accepted.msgToken == newChange.msgToken } != null
                }
                /*else if(newChange.priceItem && newChange.priceItem.tipp) {
                    processed = acceptedChanges.find { PendingChange accepted -> accepted.priceItem == newChange.priceItem && accepted.msgToken == newChange.msgToken } != null
                }*/

                if(!processed) {
                    /*
                    get each change for each subscribed package and token, fetch issue entitlement equivalent and process the change
                    if a change is being accepted, create a copy with target = subscription of subscription package and oid = the target of the processed change
                     */
                    pendingChangeService.applyPendingChange(newChange,subPkg,contextOrg)
                }
            }
        }
    }

    Map<String,Object> createOrUpdateTIPP(TitleInstancePackagePlatform tippA,Map tippB, Map<String,Package> newPackages,Map<String,Platform> newPlatforms) {
        Map<String,Object> result = [:]
        //TitleInstancePackagePlatform.withSession { Session sess ->
            if(tippA) {
                //update or delete TIPP
                result.putAll(processTippDiffs(tippA,tippB)) //maybe I have to make some adaptations on tippB!
            }
            else {
                Package pkg = newPackages.get(tippB.packageUUID)
                //Unbelievable! But package may miss at this point!
                if(pkg && pkg?.packageStatus != RDStore.PACKAGE_STATUS_DELETED) {
                    //new TIPP
                    TitleInstancePackagePlatform target = addNewTIPP(pkg, tippB, newPlatforms)
                    result.event = 'add'
                    result.target = target
                }
            }
        //}
        result
    }

    /**
     * Looks up for a given UUID if a local record exists or not. If no {@link Package} record exists, it will be
     * created with the given remote record data, otherwise, the local record is going to be updated. The {@link TitleInstancePackagePlatform records}
     * in the {@link Package} will be checked for differences and if there are such, the according fields updated. Same counts for the {@link TIPPCoverage} records
     * in the {@link TitleInstancePackagePlatform}s. If {@link Subscription}s are linked to the {@link Package}, the {@link IssueEntitlement}s (just as their
     * {@link IssueEntitlementCoverage}s) are going to be notified; it is up to the respective subscription tenants to accept the changes or not.
     * Replaces the method GokbDiffEngine.diff and the onNewTipp, onUpdatedTipp and onUnchangedTipp closures
     *
     * @param packageData - A UUID pointing to record extract for a given package
     * @return
     */
    Package createOrUpdatePackage(String packageUUID) throws SyncException {
        Map<String,Object> packageJSON = fetchRecordJSON(false,[uuid: packageUUID])
        if(packageJSON.records) {
            Map packageRecord = (Map) packageJSON.records[0]
            Package result = Package.findByGokbId(packageUUID)
            Date lastUpdatedDisplay = DateUtils.parseDateGeneric(packageRecord.lastUpdatedDisplay)
            if(!result || result?.lastUpdated < lastUpdatedDisplay) {
                log.info("package record loaded, reconciling package record for UUID ${packageUUID}")
                RefdataValue packageStatus = packageRecord.status ? RefdataValue.getByValueAndCategory(packageRecord.status, RDConstants.PACKAGE_STATUS) : null
                RefdataValue contentType = packageRecord.contentType ? RefdataValue.getByValueAndCategory(packageRecord.contentType,RDConstants.PACKAGE_CONTENT_TYPE) : null
                RefdataValue file = packageRecord.file ? RefdataValue.getByValueAndCategory(packageRecord.file,RDConstants.PACKAGE_FILE) : null
                RefdataValue scope = packageRecord.scope ? RefdataValue.getByValueAndCategory(packageRecord.scope,RDConstants.PACKAGE_SCOPE) : null
                Map<String,Object> newPackageProps = [
                    uuid: packageUUID,
                    name: packageRecord.name,
                    packageStatus: packageStatus,
                    contentType: packageRecord.contentType,
                    file: file,
                    scope: scope
                ]
                if(result) {
                    if(packageStatus == RDStore.PACKAGE_STATUS_DELETED && result.packageStatus != RDStore.PACKAGE_STATUS_DELETED) {
                        log.info("package #${result.id}, with GOKb id ${result.gokbId} got deleted, mark as deleted and rapport!")
                        result.packageStatus = packageStatus
                    }
                    else {
                        if(packageRecord.nominalPlatformUuid) {
                            newPackageProps.nominalPlatform = Platform.findByGokbId(packageRecord.nominalPlatformUuid)
                        }
                        if(packageRecord.providerUuid) {
                            newPackageProps.contentProvider = Org.findByGokbId(packageRecord.providerUuid)
                        }
                        Set<Map<String,Object>> pkgPropDiffs = getPkgPropDiff(result, newPackageProps)
                        if(pkgPropDiffs) {
                            pkgPropDiffsContainer.put(packageUUID, [event: "pkgPropUpdate", diffs: pkgPropDiffs, target: result])
                        }

                        if(!initialPackagesCounter.get(packageUUID))
                            initialPackagesCounter.put(packageUUID,TitleInstancePackagePlatform.executeQuery('select count(tipp.id) from TitleInstancePackagePlatform tipp where tipp.pkg = :pkg',[pkg:result])[0] as Integer)
                    }
                }
                else {
                    result = new Package(gokbId: packageRecord.uuid)
                }
                result.name = packageRecord.name
                result.packageStatus = packageStatus
                result.contentType = contentType
                result.scope = scope
                result.file = file
                if(result.save()) {
                    if(packageRecord.nominalPlatformUuid) {
                        result.nominalPlatform = Platform.findByGokbId(packageRecord.nominalPlatformUuid)
                    }
                    if(packageRecord.providerUuid) {
                        try {
                            Map<String, Object> providerRecord = fetchRecordJSON(false,[uuid:packageRecord.providerUuid])
                            if(providerRecord && !providerRecord.error) {
                                Org provider = createOrUpdateOrgJSON(providerRecord)
                                createOrUpdatePackageProvider(provider,result)
                            }
                            else if(providerRecord && providerRecord.error == 404) {
                                log.error("we:kb server is down")
                                throw new SyncException("we:kb server is unvailable")
                            }
                            else
                                throw new SyncException("Provider loading failed for UUID ${packageRecord.providerUuid}!")
                        }
                        catch (SyncException e) {
                            throw e
                        }
                    }
                    if(packageRecord.identifiers) {
                        if(result.ids) {
                            Identifier.executeUpdate('delete from Identifier i where i.pkg = :pkg',[pkg:result]) //damn those wrestlers ...
                        }
                        packageRecord.identifiers.each { id ->
                            if(!(id.namespace.toLowerCase() in ['originediturl','uri'])) {
                                Identifier.construct([namespace: id.namespace, value: id.value, name_de: id.namespaceName, reference: result, isUnique: false, nsType: Package.class.name])
                            }
                        }
                    }
                    if(packageRecord.ddcs) {
                        if(result.ddcs) {
                            DeweyDecimalClassification.executeUpdate('delete from DeweyDecimalClassification ddc where ddc.pkg = :pkg',[pkg:result])
                        }
                        packageRecord.ddcs.each { ddcData ->
                            if(!DeweyDecimalClassification.construct(ddc: ddc.get(ddcData.value), pkg: result))
                                throw new SyncException("Error on saving Dewey decimal classification! See stack trace as follows:")
                        }
                    }
                    if(packageRecord.languages) {
                        if(result.languages) {
                            Language.executeUpdate('delete from Language lang where lang.pkg = :pkg',[pkg:result])
                        }
                        packageRecord.languages.each { langData ->
                            if(!Language.construct(language: RefdataValue.getByValueAndCategory(langData.value,RDConstants.LANGUAGE_ISO), pkg: result))
                                throw new SyncException("Error on saving language! See stack trace as follows:")
                        }
                    }
                }
                else {
                    throw new SyncException(result.errors)
                }
            }
            result
        }
        else if(packageJSON.error == 404) {
            log.error("we:kb server is unavailable!")
            throw new SyncException("we:kb server was down!")
        }
        else {
            log.warn("Package ${packageUUID} seems to be unexistent!")
            //test if local record trace exists ...
            Package result = Package.findByGokbId(packageUUID)
            if(result) {
                log.warn("Package found, set cascading delete ...")
                result.packageStatus = RDStore.PACKAGE_STATUS_DELETED
                result.save()
                result
            }
            else null
        }
    }

    /**
     * Was formerly in the {@link Org} domain class; deployed for better maintainability
     * Checks for a given UUID if the provider exists, otherwise, it will be created.
     *
     * @param providerUUID - the GOKb UUID of the given provider {@link Org}
     * @throws SyncException
     */
    Org createOrUpdateOrgJSON(Map<String,Object> providerJSON) throws SyncException {
        Map providerRecord
        if(providerJSON.records)
            providerRecord = providerJSON.records[0]
        else providerRecord = providerJSON
        log.info("provider record loaded, reconciling provider record for UUID ${providerRecord.uuid}")
        Org provider = Org.findByGokbId(providerRecord.uuid)
        if(provider) {
            provider.name = providerRecord.name
            provider.status = orgStatus.get(providerRecord.status)
            if(!provider.sector)
                provider.sector = RDStore.O_SECTOR_PUBLISHER
        }
        else {
            provider = new Org(
                    name: providerRecord.name,
                    sector: RDStore.O_SECTOR_PUBLISHER,
                    status: orgStatus.get(providerRecord.status),
                    gokbId: providerRecord.uuid
            )
        }
        if(provider.orgType)
            provider.orgType.clear()
        if(provider.save()) {
            provider.addToOrgType(orgTypes.get("Provider"))
            provider.save()
            //providedPlatforms are missing in ES output -> see GOKb-ticket #378! But maybe, it is wiser to not implement it at all
            if(providerRecord.contacts) {
                List<Person> oldPersons = Person.executeQuery('select p from Person p where p.tenant = :provider and p.isPublic = true and p.last_name in (:contactTypes)',[provider: provider, contactTypes: contactTypes.values().collect { RefdataValue cct -> cct.getI10n("value") }])
                if(oldPersons)
                    Contact.executeUpdate('delete from Contact c where c.prs in (:oldPersons) and c.type = :type',[oldPersons: oldPersons, type: RDStore.CONTACT_TYPE_JOBRELATED])
                providerRecord.contacts.findAll{ Map<String, String> cParams -> cParams.content != null }.each { contact ->
                    switch(contact.type) {
                        case "Technical Support":
                            contact.rdType = RDStore.PRS_FUNC_TECHNICAL_SUPPORT
                            break
                        case "Service Support":
                            contact.rdType = RDStore.PRS_FUNC_SERVICE_SUPPORT
                            break
                        default: log.warn("unhandled additional property type for ${provider.gokbId}: ${contact.name}")
                            break
                    }
                    if(contact.rdType)
                        createOrUpdateSupport(provider, contact)
                }
            }
            Date lastUpdatedTime = DateUtils.parseDateGeneric(providerRecord.lastUpdatedDisplay)
            if(lastUpdatedTime.getTime() > maxTimestamp) {
                maxTimestamp = lastUpdatedTime.getTime()
            }
            provider
        }
        else throw new SyncException(provider.errors)
    }

    /**
     * Was complicatedly included in the Org domain class, has been deployed externally for better maintainability
     * Retrieves an {@link Org} instance as title publisher, if the given {@link Org} instance does not exist, it will be created.
     * The TIPP given with it will be linked with the provider data retrieved.
     *
     * @param publisherParams - a {@link Map} containing the OAI PMH extract of the title publisher
     * @param tipp - the title to check against
     * @throws SyncException
     */
    void lookupOrCreateTitlePublisher(Map<String,Object> publisherParams, TitleInstancePackagePlatform tipp) throws SyncException {
        if(publisherParams.gokbId && publisherParams.gokbId instanceof String) {
            Map<String, Object> publisherData = fetchRecordJSON(false, [uuid: publisherParams.gokbId])
            if(publisherData && !publisherData.error) {
                Org publisher = createOrUpdateOrgJSON(publisherData)
                setupOrgRole([org: publisher, tipp: tipp, roleTypeCheckup: [RDStore.OR_PUBLISHER,RDStore.OR_CONTENT_PROVIDER], definiteRoleType: RDStore.OR_PUBLISHER])
            }
            else if(publisherData && publisherData.error) throw new SyncException("we:kb server is down")
            else throw new SyncException("Provider record loading failed for ${publisherParams.gokbId}")
        }
        else {
            throw new SyncException("Org submitted without UUID! No checking possible!")
        }
    }

    /**
     * Checks for a given provider uuid if there is a link with the package for the given uuid
     * @param providerUUID - the provider UUID
     * @param pkg - the package to check against
     */
    void createOrUpdatePackageProvider(Org provider, Package pkg) {
        setupOrgRole([org: provider, pkg: pkg, roleTypeCheckup: [RDStore.OR_PROVIDER,RDStore.OR_CONTENT_PROVIDER], definiteRoleType: RDStore.OR_PROVIDER])
    }

    void setupOrgRole(Map configMap) throws SyncException {
        OrgRole role
        def reference
        if(configMap.tipp) {
            reference = configMap.tipp
            role = OrgRole.findByTippAndRoleTypeInList(reference, configMap.roleTypeCheckup)
        }
        else if(configMap.pkg) {
            reference = configMap.pkg
            role = OrgRole.findByPkgAndRoleTypeInList(reference, configMap.roleTypeCheckup)
        }
        if(reference) {
            if(!role) {
                role = new OrgRole(roleType: configMap.definiteRoleType, isShared: false)
                role.setReference(reference)
            }
            role.org = configMap.org
            if(!role.save()) {
                throw new SyncException("Error on saving org role: ${role.getErrors().getAllErrors().toListString()}")
            }
        }
        else {
            throw new SyncException("reference missing! Config map is: ${configMap.toMapString()}")
        }
    }

    void createOrUpdateSupport(Org provider, Map<String, String> supportProps) throws SyncException {
        Person personInstance = Person.findByTenantAndIsPublicAndLast_name(provider, true, supportProps.rdType.getI10n("value"))
        if(!personInstance) {
            personInstance = new Person(tenant: provider, isPublic: true, last_name: supportProps.rdType.getI10n("value"))
            if(!personInstance.save()) {
                throw new SyncException("Error on setting up technical support for ${provider}, concerning person instance: ${personInstance.getErrors().getAllErrors().toListString()}")
            }
        }
        PersonRole personRole = PersonRole.findByPrsAndOrgAndFunctionType(personInstance, provider, supportProps.rdType)
        if(!personRole) {
            personRole = new PersonRole(prs: personInstance, org: provider, functionType: supportProps.rdType)
            if(!personRole.save()) {
                throw new SyncException("Error on setting technical support for ${provider}, concerning person role: ${personRole.getErrors().getAllErrors().toListString()}")
            }
        }
        RefdataValue contentType = RefdataValue.getByValueAndCategory(supportProps.contentType, RDConstants.CONTACT_CONTENT_TYPE)
        Contact contact = new Contact(prs: personInstance, type: RDStore.CONTACT_TYPE_JOBRELATED)
        if(supportProps.language)
            contact.language = RefdataValue.getByValueAndCategory(supportProps.language, RDConstants.LANGUAGE_ISO) ?: null
        if(!contentType) {
            throw new SyncException("Invalid contact type submitted: ${supportProps.contentType}")
        }
        contact.contentType = contentType
        contact.content = supportProps.content
        if(!contact.save()) {
            throw new SyncException("Error on setting technical support for ${provider}, concerning contact: ${contact.getErrors().getAllErrors().toListString()}")
        }
    }

    /**
     * Updates a {@link Platform} with the given parameters. If it does not exist, it will be created.
     *
     * @param platformUUID - the platform UUID
     * @throws SyncException
     */
    Platform createOrUpdatePlatformJSON(String platformUUID) throws SyncException {
        Map<String,Object> platformJSON = fetchRecordJSON(false,[uuid: platformUUID])
        if(platformJSON.records) {
            Map platformRecord = platformJSON.records[0]
            //Platform.withTransaction { TransactionStatus ts ->
                Platform platform = Platform.findByGokbId(platformUUID)
                if(platform) {
                    platform.name = platformRecord.name
                }
                else {
                    platform = new Platform(name: platformRecord.name, gokbId: platformRecord.uuid)
                }
                platform.normname = platformRecord.name.toLowerCase()
                if(platformRecord.primaryUrl)
                    platform.primaryUrl = new URL(platformRecord.primaryUrl)
                if(platformRecord.providerUuid) {
                    Map<String, Object> providerData = fetchRecordJSON(false,[uuid: platformRecord.providerUuid])
                    if(providerData && !providerData.error)
                        platform.org = createOrUpdateOrgJSON(providerData)
                    else if(providerData && providerData.error == 404) {
                        throw new SyncException("we:kb server is currently down")
                    }
                    else {
                        throw new SyncException("Provider loading failed for ${platformRecord.providerUuid}")
                    }
                }
                if(platform.save()) {
                   platform
                }
                else throw new SyncException("Error on saving platform: ${platform.errors}")
            //}
        }
        else if(platformJSON && platformJSON.error == 404) {
            throw new SyncException("we:kb server is down")
        }
        else {
            throw new SyncException("Platform data called without data for UUID ${platformUUID}! PANIC!")
        }
    }

    /**
     * Compares two packages on domain property level against each other, retrieving the differences between both.
     * @param pkgA - the old package (as {@link Package} which is already persisted)
     * @param pkgB - the new package (as unprocessed {@link Map}
     * @return a {@link Set} of {@link Map}s with the differences
     */
    Set<Map<String,Object>> getPkgPropDiff(Package pkgA, Map<String,Object> pkgB) {
        log.info("processing package prop diffs; the respective GOKb UUIDs are: ${pkgA.gokbId} (LAS:eR) vs. ${pkgB.uuid} (remote)")
        Set<Map<String,Object>> result = []
        Set<String> controlledProperties = ['name','packageStatus']

        controlledProperties.each { String prop ->
            if(pkgA[prop] != pkgB[prop]) {
                if(prop in PendingChange.REFDATA_FIELDS)
                    result.add([prop: prop, newValue: pkgB[prop]?.id, oldValue: pkgA[prop]?.id])
                else result.add([prop: prop, newValue: pkgB[prop], oldValue: pkgA[prop]])
            }
        }

        if(pkgA.nominalPlatform != pkgB.nominalPlatform) {
            result.add([prop: 'nominalPlatform', newValue: pkgB.nominalPlatform?.name, oldValue: pkgA.nominalPlatform?.name])
        }

        if(pkgA.contentProvider != pkgB.contentProvider) {
            result.add([prop: 'nominalProvider', newValue: pkgB.contentProvider?.name, oldValue: pkgA.contentProvider?.name])
        }

        //the tipp diff count cannot be executed at this place because it depends on TIPP processing result

        result
    }

    Map<String,Object> processTippDiffs(TitleInstancePackagePlatform tippA, Map tippB) {
        //ex updatedTippClosure / tippUnchangedClosure
        RefdataValue status = tippStatus.get(tippB.status)
        if ((status == RDStore.TIPP_STATUS_DELETED || tippA.pkg.packageStatus == RDStore.PACKAGE_STATUS_DELETED) && tippA.status != status) {
            log.info("TIPP with UUID ${tippA.gokbId} has been deleted from package ${tippA.pkg.gokbId}")
            tippA.status = RDStore.TIPP_STATUS_DELETED
            tippA.save()
            [event: "delete", target: tippA]
        }
        else if(tippA.status != RDStore.TIPP_STATUS_DELETED && status != RDStore.TIPP_STATUS_DELETED) {
            //process central differences which are without effect to issue entitlements
            tippA.titleType = tippB.titleType
            tippA.name = tippB.name //TODO include name, sortName in IssueEntitlements, then, this property may move to the controlled ones
            tippA.firstAuthor = tippB.firstAuthor
            tippA.firstEditor = tippB.firstEditor
            tippA.publisherName = tippB.publisherName
            tippA.hostPlatformURL = tippB.hostPlatformURL
            tippA.dateFirstInPrint = (Date) tippB.dateFirstInPrint
            tippA.dateFirstOnline = (Date) tippB.dateFirstOnline
            tippA.imprint = tippB.imprint
            tippA.seriesName = tippB.seriesName
            tippA.subjectReference = tippB.subjectReference
            tippA.volume = tippB.volume
            tippA.medium = titleMedium.get(tippB.medium)
            if(!tippA.save())
                throw new SyncException("Error on updating base title data: ${tippA.errors}")
            if(tippB.titlePublishers) {
                if(tippA.publishers) {
                    OrgRole.executeUpdate('delete from OrgRole oo where oo.tipp = :tippA',[tippA:tippA])
                }
                tippB.titlePublishers.each { publisher ->
                    lookupOrCreateTitlePublisher([name: publisher.name, gokbId: publisher.uuid], tippA)
                }
            }
            if(tippB.identifiers) {
                //I hate this solution ... wrestlers of GOKb stating that Identifiers do not need UUIDs were stronger.
                //Identifier.withTransaction { TransactionStatus ts ->
                    if(tippA.ids) {
                        Identifier.executeUpdate('delete from Identifier i where i.tipp = :tipp',[tipp:tippA]) //damn those wrestlers ...
                    }
                    tippB.identifiers.each { idData ->
                        if(!(idData.namespace.toLowerCase() in ['originediturl','uri'])) {
                            Identifier.construct([namespace: idData.namespace, value: idData.value, name_de: idData.namespaceName, reference: tippA, isUnique: false, nsType: TitleInstancePackagePlatform.class.name])
                        }
                    }
                //}
            }
            if(tippB.ddcs) {
                if(tippA.ddcs) {
                    DeweyDecimalClassification.executeUpdate('delete from DeweyDecimalClassification ddc where ddc.tipp = :tipp',[tipp:tippA])
                }
                tippB.ddcs.each { ddcData ->
                    if(!DeweyDecimalClassification.construct(ddc: ddcData, tipp: tippA))
                        throw new SyncException("Error on saving Dewey decimal classification! See stack trace as follows:")
                }
            }
            if(tippB.languages) {
                if(tippA.languages) {
                    Language.executeUpdate('delete from Language lang where lang.tipp = :tipp',[tipp:tippA])
                }
                tippB.languages.each { langData ->
                    if(!Language.construct(language: langData, tipp: tippA))
                        throw new SyncException("Error on saving language! See stack trace as follows:")
                }
            }
            if(tippB.history) {
                if(tippA.historyEvents) {
                    TitleHistoryEvent.executeUpdate('delete from TitleHistoryEvent the where the.tipp = :tipp',[tipp:tippA])
                }
                tippB.history.each { historyEvent ->
                    historyEvent.from.each { from ->
                        TitleHistoryEvent the = new TitleHistoryEvent(tipp:tippA,from:from.name,eventDate:historyEvent.date)
                        if(!the.save())
                            throw new SyncException("Error on saving title history data: ${the.errors}")
                    }
                    historyEvent.to.each { to ->
                        TitleHistoryEvent the = new TitleHistoryEvent(tipp:tippA,from:to.name,eventDate:historyEvent.date)
                        if(!the.save())
                            throw new SyncException("Error on saving title history data: ${the.errors}")
                    }
                }
            }
            //get to diffs that need to be notified
            //println("tippA:"+tippA)
            //println("tippB:"+tippB)
            Set<Map<String, Object>> diffs = getTippDiff(tippA, tippB)
            //includes also changes in coverage statement set
            if (diffs) {
                //process actual diffs
                diffs.each { Map<String,Object> diff ->
                    log.info("Got tipp diff: ${diff}")
                    switch(diff.prop) {
                        case 'coverage':
                            diff.covDiffs.each { entry ->
                                switch (entry.event) {
                                    case 'add':
                                        if (!entry.target.save())
                                            throw new SyncException("Error on adding coverage statement for TIPP ${tippA.gokbId}: ${entry.target.errors}")
                                        break
                                    case 'delete': PendingChange.executeUpdate('delete from PendingChange pc where pc.tippCoverage = :toDelete',[toDelete:entry.target])
                                        TIPPCoverage.executeUpdate('delete from TIPPCoverage tc where tc.id = :id',[id:entry.target.id])
                                        break
                                    case 'update': entry.diffs.each { covDiff ->
                                        entry.target[covDiff.prop] = covDiff.newValue
                                    }
                                        if (!entry.target.save())
                                            throw new SyncException("Error on updating coverage statement for TIPP ${tippA.gokbId}: ${entry.target.errors}")
                                        break
                                }
                            }
                            break
                        case 'price':
                            diff.priceDiffs.each { entry ->
                                switch (entry.event) {
                                    case 'add':
                                        if (!entry.target.save())
                                            throw new SyncException("Error on adding price item for TIPP ${tippA.gokbId}: ${entry.target.errors}")
                                        break
                                    case 'delete': PendingChange.executeUpdate('delete from PendingChange pc where pc.priceItem = :toDelete',[toDelete:entry.target])
                                        PriceItem.executeUpdate('delete from PriceItem pi where pi.id = :id',[id:entry.target.id])
                                        break
                                    case 'update': entry.diffs.each { priceDiff ->
                                        entry.target[priceDiff.prop] = priceDiff.newValue
                                    }
                                        if (!entry.target.save())
                                            throw new SyncException("Error on updating coverage statement for TIPP ${tippA.gokbId}: ${entry.target.errors}")
                                        break
                                }
                            }
                            break
                        default:
                            if (diff.prop in PendingChange.REFDATA_FIELDS) {
                                tippA[diff.prop] = tippStatus.values().find { RefdataValue rdv -> rdv.id == diff.newValue }
                            } else {
                                tippA[diff.prop] = diff.newValue
                            }
                            break
                    }
                }
                if (tippA.save())
                    [event: 'update', target: tippA, diffs: diffs]
                else throw new SyncException("Error on updating TIPP with UUID ${tippA.gokbId}: ${tippA.errors}")
            }
            else [:]
        }
        else [:]
    }

    /**
     * Replaces the onNewTipp closure.
     * Creates a new {@link TitleInstancePackagePlatform} with its respective {@link TIPPCoverage} statements
     * @param pkg
     * @param tippData
     * @return the new {@link TitleInstancePackagePlatform} object
     */
    TitleInstancePackagePlatform addNewTIPP(Package pkg, Map<String,Object> tippData, Map<String,Platform> platformsInPackage) throws SyncException {
        TitleInstancePackagePlatform newTIPP = new TitleInstancePackagePlatform(
                titleType: tippData.titleType,
                name: tippData.name,
                firstAuthor: tippData.firstAuthor,
                firstEditor: tippData.firstEditor,
                dateFirstInPrint: (Date) tippData.dateFirstInPrint,
                dateFirstOnline: (Date) tippData.dateFirstOnline,
                imprint: tippData.imprint,
                publisherName: tippData.publisherName,
                seriesName: tippData.seriesName,
                subjectReference: tippData.subjectReference,
                volume: tippData.volume,
                medium: titleMedium.get(tippData.medium),
                gokbId: tippData.uuid,
                status: tippStatus.get(tippData.status),
                hostPlatformURL: tippData.hostPlatformURL,
                accessStartDate: (Date) tippData.accessStartDate,
                accessEndDate: (Date) tippData.accessEndDate,
                pkg: pkg
        )
        //ex updatedTitleAfterPackageReconcile
        //long start = System.currentTimeMillis()
        Platform platform = platformsInPackage.get(tippData.platformUUID)
        //log.debug("time needed for queries: ${System.currentTimeMillis()-start}")
        if(!platform) {
            platform = Platform.findByGokbId(tippData.platformUUID)
        }
        if(!platform) {
            platform = createOrUpdatePlatformJSON(tippData.platformUUID)
        }
        newTIPP.platform = platform
        if(newTIPP.save()) {
            tippData.coverages.each { covB ->
                TIPPCoverage covStmt = new TIPPCoverage(
                        startDate: (Date) covB.startDate ?: null,
                        startVolume: covB.startVolume,
                        startIssue: covB.startIssue,
                        endDate: (Date) covB.endDate ?: null,
                        endVolume: covB.endVolume,
                        endIssue: covB.endIssue,
                        embargo: covB.embargo,
                        coverageDepth: covB.coverageDepth,
                        coverageNote: covB.coverageNote,
                        tipp: newTIPP
                )
                if (!covStmt.save())
                    throw new SyncException("Error on saving coverage data: ${covStmt.errors}")
            }
            tippData.priceItems.each { piB ->
                PriceItem priceItem = new PriceItem(startDate: (Date) piB.startDate ?: null,
                        endDate: (Date) piB.endDate ?: null,
                        listPrice: piB.listPrice,
                        listCurrency: piB.listCurrency,
                        tipp: newTIPP
                )
                priceItem.setGlobalUID()
                if(!priceItem.save())
                    throw new SyncException("Error on saving price data: ${priceItem.errors}")
            }
            tippData.titlePublishers.each { publisher ->
                lookupOrCreateTitlePublisher([name: publisher.name, gokbId: publisher.uuid], newTIPP)
            }
            tippData.identifiers.each { idB ->
                if(idB.namespace.toLowerCase() != 'originediturl') {
                    Identifier.construct([namespace: idB.namespace, value: idB.value, name_de: idB.namespaceName, reference: newTIPP, isUnique: false, nsType: TitleInstancePackagePlatform.class.name])
                }
            }
            tippData.ddcs.each { ddcB ->
                if(!DeweyDecimalClassification.construct(ddc: ddcB, tipp: newTIPP))
                    throw new SyncException("Error on saving Dewey decimal classification! See stack trace as follows:")
            }
            tippData.languages.each { langB ->
                if(!Language.construct(language: langB, tipp: newTIPP))
                    throw new SyncException("Error on saving language! See stack trace as follows:")
            }
            tippData.history.each { historyEvent ->
                historyEvent.from.each { from ->
                    TitleHistoryEvent the = new TitleHistoryEvent(tipp:newTIPP,from:from.name,eventDate:historyEvent.date)
                    if(!the.save())
                        throw new SyncException("Error on saving title history data: ${the.errors}")
                }
                historyEvent.to.each { to ->
                    TitleHistoryEvent the = new TitleHistoryEvent(tipp:newTIPP,from:to.name,eventDate:historyEvent.date)
                    if(!the.save())
                        throw new SyncException("Error on saving title history data: ${the.errors}")
                }
            }
            newTIPP
        }
        else throw new SyncException("Error on saving TIPP data: ${newTIPP.errors}")
    }

    /**
     * Compares two package entries against each other, retrieving the differences between both.
     * @param tippa - the old TIPP
     * @param tippb - the new TIPP
     * @return a {@link Set} of {@link Map}s with the differences
     */
    Set<Map<String,Object>> getTippDiff(tippa, tippb) {
        if(tippa instanceof TitleInstancePackagePlatform && tippb instanceof Map)
            log.info("processing diffs; the respective GOKb UUIDs are: ${tippa.gokbId} (LAS:eR) vs. ${tippb.uuid} (remote)")
        else if(tippa instanceof TitleInstancePackagePlatform && tippb instanceof TitleInstancePackagePlatform)
            log.info("processing diffs; the respective objects are: ${tippa.id} (TitleInstancePackagePlatform) pointing to ${tippb.id} (TIPP)")
        Set<Map<String, Object>> result = []

        /*
        IssueEntitlements do not have hostPlatformURLs
        if (tippa.hasProperty("hostPlatformURL") && tippa.hostPlatformURL != tippb.hostPlatformURL) {
            if(!((tippa.hostPlatformURL == null && tippb.hostPlatformURL == "") || (tippa.hostPlatformURL == "" && tippb.hostPlatformURL == null)))
                result.add([prop: 'hostPlatformURL', newValue: tippb.hostPlatformURL, oldValue: tippa.hostPlatformURL])
        }
        */

        // This is the boss enemy when refactoring coverage statements ... works so far, is going to be kept
        // the question marks are necessary because only JournalInstance's TIPPs are supposed to have coverage statements
        Set<Map<String, Object>> coverageDiffs = getSubListDiffs(tippa,tippb.coverages,'coverage')
        if(!coverageDiffs.isEmpty())
            result.add([prop: 'coverage', covDiffs: coverageDiffs])

        Set<Map<String, Object>> priceDiffs = getSubListDiffs(tippa,tippb.priceItems,'price')
        if(!priceDiffs.isEmpty())
            result.add([prop: 'price', priceDiffs: priceDiffs])

        /* is perspectively ordered; needs more refactoring
        if (tippb.containsKey("name") && tippa.name != tippb.name) {
            result.add([prop: 'name', newValue: tippb.name, oldValue: tippa.name])
        }
        */

        if (tippb.containsKey("accessStartDate") && tippa.accessStartDate != tippb.accessStartDate) {
            result.add([prop: 'accessStartDate', newValue: tippb.accessStartDate, oldValue: tippa.accessStartDate])
        }

        if (tippb.containsKey("accessEndDate") && tippa.accessEndDate != tippb.accessEndDate) {
            result.add([prop: 'accessEndDate', newValue: tippb.accessEndDate, oldValue: tippa.accessEndDate])
        }

        if(tippa instanceof TitleInstancePackagePlatform && tippb instanceof Map) {
            if(tippa.status != tippStatus.get(tippb.status)) {
                result.add([prop: 'status', newValue: tippStatus.get(tippb.status).id, oldValue: tippa.status.id])
            }
        }
        else if(tippa instanceof IssueEntitlement && tippb instanceof TitleInstancePackagePlatform) {
            if(tippa.status != tippb.status) {
                result.add([prop: 'status', newValue: tippb.status.id, oldValue: tippa.status.id])
            }
        }

        //println("getTippDiff:"+result)
        result
    }

    /**
     * Compares two sub list entries against each other, retrieving the differences between both.
     * @param tippA - the old {@link TitleInstancePackagePlatform} object, containing the current {@link Set} of  or price items
     * @param listB - the new statements (a {@link List} of remote records, kept in {@link Map}s)
     * @return a {@link Set} of {@link Map}s reflecting the differences between the statements
     */
    Set<Map<String,Object>> getSubListDiffs(TitleInstancePackagePlatform tippA, listB, String instanceType) {
        Set subDiffs = []
        Set listA
        if(instanceType == "coverage")
            listA = tippA.coverages
        else if(instanceType == "price")
            listA = tippA.priceItems
        if(listA != null) {
            if(listA.size() == listB.size()) {
                //statements may have changed or not, no deletions or insertions
                //sorting has been done by mapping (listA) resp. when converting data (listB)
                listB.eachWithIndex { itemB, int i ->
                    def itemA = locateEquivalent(itemB,listA)
                    if(!itemA)
                        itemA = listA[i]
                    Set<Map<String,Object>> currDiffs = compareSubListItem(itemA,itemB)
                    if(currDiffs)
                        subDiffs << [event: 'update', target: itemA, diffs: currDiffs]
                }
            }
            else if(listA.size() > listB.size()) {
                //statements have been deleted
                Set toKeep = []
                listB.each { itemB ->
                    def itemA = locateEquivalent(itemB,listA)
                    if(itemA) {
                        toKeep << itemA
                        Set<Map<String,Object>> currDiffs = compareSubListItem(itemA,itemB)
                        if(currDiffs)
                            subDiffs << [event: 'update', target: itemA, diffs: currDiffs]
                    }
                    else {
                        //a new statement may have been added for which I cannot determine an equivalent
                        def newItem
                        if(instanceType == 'coverage')
                            newItem = addNewStatement(tippA,itemB)
                        else if(instanceType == 'price')
                            newItem = addNewPriceItem(tippA,itemB)
                        if(newItem)
                            subDiffs << [event: 'add', target: newItem]
                    }
                }
                listA.each { itemA ->
                    if(!toKeep.contains(itemA)) {
                        subDiffs << [event: 'delete', target: itemA, targetParent: tippA]
                    }
                }
            }
            else if(listA.size() < listB.size()) {
                //coverage statements have been added
                listB.each { itemB ->
                    def itemA = locateEquivalent(itemB,listA)
                    if(itemA) {
                        Set<Map<String,Object>> currDiffs = compareSubListItem(itemA,itemB)
                        if(currDiffs)
                            subDiffs << [event: 'update', target: itemA, diffs: currDiffs]
                    }
                    else {
                        def newItem
                        if(instanceType == 'coverage')
                            newItem = addNewStatement(tippA,itemB)
                        else if(instanceType == 'price')
                            newItem = addNewPriceItem(tippA,itemB)
                        if(newItem)
                            subDiffs << [event: 'add', target: newItem]
                    }
                }
            }
        }

        subDiffs
    }

    Set<Map<String,Object>> compareSubListItem(itemA,itemB) {
        Set<String> controlledProperties = []
        if(itemA instanceof AbstractCoverage) {
            controlledProperties.addAll([
                    'startDate',
                    'startVolume',
                    'startIssue',
                    'endDate',
                    'endVolume',
                    'endIssue',
                    'embargo',
                    'coverageDepth',
                    'coverageNote',
            ])
        }
        else if(itemA instanceof PriceItem) {
            controlledProperties.addAll([
                    'startDate',
                    'endDate',
                    'listPrice',
                    'listCurrency'
            ])
        }
        Set<Map<String,Object>> diffs = []
        controlledProperties.each { String cp ->
            if(cp in ['startDate','endDate']) {
                Calendar calA = Calendar.getInstance(), calB = Calendar.getInstance()
                if(itemA[cp] != null && itemB[cp] != null) {
                    calA.setTime((Date) itemA[cp])
                    calB.setTime((Date) itemB[cp])
                    if(!(calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) && calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR))) {
                        diffs << [prop: cp, oldValue: itemA[cp], newValue: itemB[cp]]
                    }
                }
                else {
                    /*
                    Means that one of the coverage dates is null or became null.
                    Cases to cover: null -> date (covA == null, covB instanceof Date)
                    date -> null (covA instanceof Date, covB == null)
                     */
                    if(itemA[cp] != null && itemB[cp] == null) {
                        calA.setTime((Date) itemA[cp])
                        diffs << [prop:cp, oldValue:itemA[cp],newValue:null]
                    }
                    else if(itemA[cp] == null && itemB[cp] != null) {
                        calB.setTime((Date) itemB[cp])
                        diffs << [prop:cp, oldValue:null, newValue: itemB[cp]]
                    }
                }
            }
            else {
                if(itemA[cp] != itemB[cp] && !((itemA[cp] == '' && itemB[cp] == null) || (itemA[cp] == null && itemB[cp] == ''))) {
                    diffs << [prop:cp, oldValue: itemA[cp], newValue: itemB[cp]]
                }
            }
        }
        diffs
    }

    /**
     * Contrary to {@link AbstractCoverage}.findEquivalent() resp {@link PriceItem}.findEquivalent(), this method locates a non-persisted coverage statement an equivalent from the given {@link Collection}
     * @param itemB - a {@link Map}, reflecting the non-persisited item
     * @param listA - a {@link Collection} on {@link TIPPCoverage} or {@link PriceItem} statements, the list to be updated
     * @return the equivalent LAS:eR {@link TIPPCoverage} or {@link PriceItem} from the collection
     */
    def locateEquivalent(itemB, listA) {
        def equivalent = null
        Set<String> equivalencyProperties = []
        if(listA[0] instanceof AbstractCoverage)
            equivalencyProperties.addAll(AbstractCoverage.equivalencyProperties)
        else if(listA[0] instanceof PriceItem)
            equivalencyProperties.addAll(PriceItem.equivalencyProperties)
        for (String k : equivalencyProperties) {
            if(k in ['startDate','endDate']) {
                Calendar calA = Calendar.getInstance(), calB = Calendar.getInstance()
                listA.each { itemA ->
                    if(itemA[k] != null && itemB[k] != null) {
                        calA.setTime(itemA[k])
                        calB.setTime(itemB[k])
                        if (calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) && calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR))
                            equivalent = itemA
                    }
                    else if(itemA[k] == null && itemB[k] == null)
                        equivalent = itemA
                }
            }
            else
                equivalent = listA.find { it[k] == itemB[k] }
            if (equivalent != null) {
                println "Statement ${equivalent.id} located as equivalent to ${itemB}"
                break
            }
        }
        equivalent
    }

    AbstractCoverage addNewStatement(tippA, covB) {
        Map<String,Object> params = [startDate: (Date) covB.startDate,
                                     startVolume: covB.startVolume,
                                     startIssue: covB.startIssue,
                                     endDate: (Date) covB.endDate,
                                     endVolume: covB.endVolume,
                                     endIssue: covB.endIssue,
                                     embargo: covB.embargo,
                                     coverageDepth: covB.coverageDepth,
                                     coverageNote: covB.coverageNote]
        AbstractCoverage newStatement
        if(tippA instanceof TitleInstancePackagePlatform)
            newStatement = new TIPPCoverage(params+[tipp: tippA])
        if(tippA instanceof IssueEntitlement)
            newStatement = new IssueEntitlementCoverage(params+[issueEntitlement: tippA])
        if(newStatement)
            newStatement
        else null
    }

    PriceItem addNewPriceItem(tippA, piB) {
        Map<String,Object> params = [startDate: (Date) piB.startDate,
                                     endDate: (Date) piB.endDate,
                                     listPrice: piB.listPrice,
                                     listCurrency: piB.listCurrency,
                                     tipp: tippA]
        PriceItem pi = new PriceItem(params)
        pi.setGlobalUID()
        pi
    }

    Map<String,Object> fetchRecordJSON(boolean useScroll, Map<String,Object> queryParams) throws SyncException {
        //I need to address a bulk output endpoint like https://github.com/hbz/lobid-resources/blob/f93201bec043cc732b27814a6ab4aea390d1aa9e/web/app/controllers/resources/Application.java, method bulkResult().
        //By then, I should query the "normal" endpoint /wekb/api/find?
        HTTPBuilder http
        if(useScroll) {
            http = new HTTPBuilder(source.uri + '/scroll')
        }
        else http = new HTTPBuilder(source.uri+'/find')
        Map<String,Object> result = [:]
        //setting default status
        queryParams.status = ["Current","Expected","Retired","Deleted"]
        log.debug("mem check: ${Runtime.getRuntime().freeMemory()} bytes")
        http.request(Method.POST, ContentType.JSON) { req ->
            body = queryParams
            requestContentType = ContentType.URLENC
            response.success = { resp, json ->
                if(resp.status == 200) {
                    result.count = json.size ?: json.count
                    result.records = json.records
                    result.scrollId = json.scrollId
                    result.hasMoreRecords = Boolean.valueOf(json.hasMoreRecords)
                }
                else {
                    throw new SyncException("erroneous response")
                }
            }
            response.failure = { resp, reader ->
                log.error("server response: ${resp.statusLine}")
                if(resp.status == 404) {
                    result.error = resp.status
                }
                else
                    throw new SyncException("error on request: ${resp.statusLine} : ${reader}")
            }
        }
        http.shutdown()
        result
    }

    void defineMapFields() {
        //define map fields
        tippStatus.put(RDStore.TIPP_STATUS_CURRENT.value,RDStore.TIPP_STATUS_CURRENT)
        tippStatus.put(RDStore.TIPP_STATUS_DELETED.value,RDStore.TIPP_STATUS_DELETED)
        tippStatus.put(RDStore.TIPP_STATUS_RETIRED.value,RDStore.TIPP_STATUS_RETIRED)
        tippStatus.put(RDStore.TIPP_STATUS_EXPECTED.value,RDStore.TIPP_STATUS_EXPECTED)
        tippStatus.put(RDStore.TIPP_STATUS_TRANSFERRED.value,RDStore.TIPP_STATUS_TRANSFERRED)
        tippStatus.put(RDStore.TIPP_STATUS_UNKNOWN.value,RDStore.TIPP_STATUS_UNKNOWN)
        contactTypes.put(RDStore.PRS_FUNC_TECHNICAL_SUPPORT.value,RDStore.PRS_FUNC_TECHNICAL_SUPPORT)
        contactTypes.put(RDStore.PRS_FUNC_SERVICE_SUPPORT.value,RDStore.PRS_FUNC_SERVICE_SUPPORT)
        //this complicated way is necessary because of static in order to avoid a NonUniqueObjectException
        List<RefdataValue> staticMediumTypes = [RDStore.TITLE_TYPE_DATABASE,RDStore.TITLE_TYPE_EBOOK,RDStore.TITLE_TYPE_JOURNAL]
        RefdataValue.findAllByIdNotInListAndOwner(staticMediumTypes.collect { RefdataValue rdv -> rdv.id },RefdataCategory.findByDesc(RDConstants.TITLE_MEDIUM)).each { RefdataValue rdv ->
            titleMedium.put(rdv.value,rdv)
        }
        staticMediumTypes.each { RefdataValue rdv ->
            titleMedium.put(rdv.value,rdv)
        }
        RefdataCategory.getAllRefdataValues(RDConstants.PACKAGE_STATUS).each { RefdataValue rdv ->
            packageStatus.put(rdv.value,rdv)
        }
        RefdataCategory.getAllRefdataValues(RDConstants.ORG_STATUS).each { RefdataValue rdv ->
            orgStatus.put(rdv.value,rdv)
        }
        RefdataCategory.getAllRefdataValues(RDConstants.CURRENCY).each { RefdataValue rdv ->
            currency.put(rdv.value,rdv)
        }
        RefdataCategory.getAllRefdataValues(RDConstants.ORG_TYPE).each { RefdataValue rdv ->
            orgTypes.put(rdv.value,rdv)
        }
        RefdataCategory.getAllRefdataValues(RDConstants.DDC).each { RefdataValue rdv ->
            ddc.put(rdv.value,rdv)
        }
    }
}

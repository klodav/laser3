package com.k_int.kbplus

import de.laser.domain.AbstractBaseDomain

import com.k_int.kbplus.auth.User
import de.laser.helper.RDStore

import javax.persistence.Transient

class Links extends AbstractBaseDomain {


    @Transient
    def contextService
    @Transient
    def springSecurityService
    @Transient
    def genericOIDService
    @Transient
    static def grailsApplication

    Long id
    Long source
    Long destination
    String objectType
    RefdataValue linkType
    Org owner
    Date lastUpdated
    User createdBy
    User lastUpdatedBy

    static mapping = {
        id          column: 'l_id'
        globalUID   column: 'l_guid'
        source      column: 'l_source_fk'
        destination column: 'l_destination_fk'
        objectType  column: 'l_object'
        linkType    column: 'l_link_type_rv_fk'
        owner       column: 'l_owner_fk'
        autoTimestamp true
    }

    static constraints = {
        globalUID     (nullable: true, blank: false, unique: true, maxSize: 255)
        source        (nullable: false, blank: false)
        destination   (nullable: false, blank: false)
        objectType    (nullable: false, blank: false)
        linkType      (nullable: false, blank: false)
        owner         (nullable: false, blank: false)
        lastUpdatedBy (nullable: true)
        createdBy     (nullable: true)
    }

    def beforeInsert() {
        super.beforeInsert()

        def user = springSecurityService.getCurrentUser()
        if (user) {
            createdBy     = user
            lastUpdatedBy = user
        } else
            return false
    }

    def beforeUpdate() {
        super.beforeUpdate()

        def user = springSecurityService.getCurrentUser()
        if (user)
            lastUpdatedBy = user
        else
            return false
    }

    Subscription getOther(key) {
        Subscription context
        if(key instanceof Subscription)
            context = key
        else if(key instanceof GString || key instanceof String)
            context = genericOIDService.resolveOID(key)
        else {
            log.error("No context key!")
            return null
        }
        if(context.id == source)
            return Subscription.get(destination)
        else if(context.id == destination)
            return Subscription.get(source)
        else return null
    }

    static LinkedHashMap<String,List> generateNavigation(String contextClassName, Long contextID) {
        List prevLink = []
        List nextLink = []
        List previous = findAllBySourceAndLinkTypeAndObjectType(contextID,RDStore.LINKTYPE_FOLLOWS,contextClassName)
        List next = findAllByDestinationAndLinkTypeAndObjectType(contextID,RDStore.LINKTYPE_FOLLOWS,contextClassName)
        def domainClass = grailsApplication.getArtefact('Domain', contextClassName)
        prevLink = previous ? previous.collect { it -> domainClass.getClazz().get(it.destination) } : null
        nextLink = next ? next.collect { it -> domainClass.getClazz().get(it.source)} : null
        return [prevLink:prevLink,nextLink:nextLink]
    }

    static LinkedHashMap<String, List> generateNavigation(String contextClassName, String contextID) {
        return generateNavigation(contextClassName,Long.parseLong(contextID))
    }

}

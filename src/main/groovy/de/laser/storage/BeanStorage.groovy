package de.laser.storage

import com.k_int.kbplus.ChangeNotificationService
import com.k_int.kbplus.ESWrapperService
import com.k_int.kbplus.GenericOIDService
import com.k_int.kbplus.PendingChangeService
import de.laser.AccessService
import de.laser.AuditService
import de.laser.CacheService
import de.laser.CascadingUpdateService
import de.laser.ContextService
import de.laser.DeletionService
import de.laser.EscapeService
import de.laser.FinanceService
import de.laser.InstAdmService
import de.laser.LicenseService
import de.laser.LinksGenerationService
import de.laser.OrgTypeService
import de.laser.OrganisationService
import de.laser.PropertyService
import de.laser.SubscriptionsQueryService
import de.laser.SystemService
import de.laser.UserService
import de.laser.YodaService
import de.laser.ctrl.FinanceControllerService
import grails.gsp.PageRenderer
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugins.mail.MailService
import grails.util.Holders
import org.grails.plugins.web.taglib.ApplicationTagLib
import org.springframework.context.MessageSource

import javax.sql.DataSource

class BeanStorage {

    static def get(String bean) {
        Holders.grailsApplication.mainContext.getBean(bean)
    }

    // -- Grails --

    static ApplicationTagLib getApplicationTagLib() {
        Holders.grailsApplication.mainContext.getBean(ApplicationTagLib)
    }
    static DataSource getDataSource() {
        Holders.grailsApplication.mainContext.getBean('dataSource') as DataSource
    }
    static PageRenderer getGroovyPageRenderer() {
        Holders.grailsApplication.mainContext.getBean('groovyPageRenderer') as PageRenderer
    }
    static MessageSource getMessageSource() {
        // PluginAwareResourceBundleMessageSource
        Holders.grailsApplication.mainContext.getBean('messageSource') as MessageSource
    }
    static SpringSecurityService getSpringSecurityService() {
        Holders.grailsApplication.mainContext.getBean('springSecurityService') as SpringSecurityService
    }

    // -- Laser --

    static AccessService getAccessService() {
        Holders.grailsApplication.mainContext.getBean('accessService') as AccessService
    }
    static AuditService getAuditService() {
        Holders.grailsApplication.mainContext.getBean('auditService') as AuditService
    }
    static CacheService getCacheService() {
        Holders.grailsApplication.mainContext.getBean('cacheService') as CacheService
    }
    static CascadingUpdateService getCascadingUpdateService() {
        Holders.grailsApplication.mainContext.getBean('cascadingUpdateService') as CascadingUpdateService
    }
    static ChangeNotificationService getChangeNotificationService() {
        Holders.grailsApplication.mainContext.getBean('changeNotificationService') as ChangeNotificationService
    }
    static ContextService getContextService() {
        Holders.grailsApplication.mainContext.getBean('contextService') as ContextService
    }
    static DeletionService getDeletionService() {
        Holders.grailsApplication.mainContext.getBean('deletionService') as DeletionService
    }
    static EscapeService getEscapeService() {
        Holders.grailsApplication.mainContext.getBean('escapeService') as EscapeService
    }
    static ESWrapperService getESWrapperService() {
        Holders.grailsApplication.mainContext.getBean('ESWrapperService') as ESWrapperService
    }
    static FinanceControllerService getFinanceControllerService() {
        Holders.grailsApplication.mainContext.getBean('financeControllerService') as FinanceControllerService
    }
    static FinanceService getFinanceService() {
        Holders.grailsApplication.mainContext.getBean('financeService') as FinanceService
    }
    static GenericOIDService getGenericOIDService() {
        Holders.grailsApplication.mainContext.getBean('genericOIDService') as GenericOIDService
    }
    static InstAdmService getInstAdmService() {
        Holders.grailsApplication.mainContext.getBean('instAdmService') as InstAdmService
    }
    static LicenseService getLicenseService() {
        Holders.grailsApplication.mainContext.getBean('licenseService') as LicenseService
    }
    static LinksGenerationService getLinksGenerationService() {
        Holders.grailsApplication.mainContext.getBean('linksGenerationService') as LinksGenerationService
    }
    static MailService getMailService() {
        Holders.grailsApplication.mainContext.getBean('mailService') as MailService
    }
    static OrganisationService getOrganisationService() {
        Holders.grailsApplication.mainContext.getBean('organisationService') as OrganisationService
    }
    static OrgTypeService getOrgTypeService() {
        Holders.grailsApplication.mainContext.getBean('orgTypeService') as OrgTypeService
    }
    static PendingChangeService getPendingChangeService() {
        Holders.grailsApplication.mainContext.getBean('pendingChangeService') as PendingChangeService
    }
    static PropertyService getPropertyService() {
        Holders.grailsApplication.mainContext.getBean('propertyService') as PropertyService
    }
    static SubscriptionsQueryService getSubscriptionsQueryService() {
        Holders.grailsApplication.mainContext.getBean('subscriptionsQueryService') as SubscriptionsQueryService
    }
    static SystemService getSystemService() {
        Holders.grailsApplication.mainContext.getBean('systemService') as SystemService
    }
    static UserService getUserService() {
        Holders.grailsApplication.mainContext.getBean('userService') as UserService
    }
    static YodaService getYodaService() {
        Holders.grailsApplication.mainContext.getBean('yodaService') as YodaService
    }
}
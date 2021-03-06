package de.laser.system

import de.laser.RefdataValue
import de.laser.UserSetting
import de.laser.auth.User
import de.laser.utils.AppUtils
import de.laser.config.ConfigMapper
import de.laser.utils.DateUtils
import de.laser.storage.BeanStore
import de.laser.storage.RDConstants
import de.laser.storage.RDStore
import net.sf.json.JSON
import org.springframework.context.MessageSource

import java.time.LocalDate

/**
 * This class reflects system-wide messages which can be shown on the dashboard (for announcements).
 * The system announcement may be sent moreover to users who subscribed to these reminders. This reminder setting may be done in the user profile (and is stored as a {@link UserSetting}).
 */
class SystemAnnouncement {

    User    user
    String  title
    String  content
    String  status

    boolean isPublished = false

    Date    lastPublishingDate
    Date    dateCreated
    Date    lastUpdated

    static transients = ['cleanTitle', 'cleanContent'] // mark read-only accessor methods

    static mapping = {
        id              column: 'sa_id'
        version         column: 'sa_version'
        user            column: 'sa_user_fk'
        title           column: 'sa_title'
        content         column: 'sa_content', type: 'text'
        status          column: 'sa_status', type: 'text'
        isPublished     column: 'sa_is_published'
        lastPublishingDate column: 'sa_last_publishing_date'
        dateCreated     column: 'sa_date_created'
        lastUpdated     column: 'sa_last_updated'
    }

    static constraints = {
        title       (blank:false)
        content     (blank:false)
        status      (nullable:true, blank:false)
        lastPublishingDate (nullable:true)
        dateCreated (nullable:true)
        lastUpdated (nullable:true)
    }

    /**
     * Retrieves a {@link List} of system announces which have been published in the given period of days
     * @param periodInDays the amount of days to look back for recently published messages
     * @return a {@link List} of system announces
     */
    static List<SystemAnnouncement> getPublished(int periodInDays) {
        Date dcCheck = DateUtils.localDateToSqlDate( LocalDate.now().minusDays(periodInDays) )

        SystemAnnouncement.executeQuery(
                'select sa from SystemAnnouncement sa ' +
                'where sa.isPublished = true and sa.lastPublishingDate >= :dcCheck order by sa.lastPublishingDate desc',
                [dcCheck: dcCheck]
        )
    }

    /**
     * Gets all users who should be notified about system messages. The criteria to be checked is the {@link UserSetting.KEYS#IS_NOTIFICATION_FOR_SYSTEM_MESSAGES} setting
     * @return a {@link List} of {@link User}s to be notified
     */
    static List<User> getRecipients() {
        User.executeQuery(
                'select u from UserSetting uss join uss.user u where uss.key = :ussKey and uss.rdValue = :ussValue order by u.id',
                [ussKey: UserSetting.KEYS.IS_NOTIFICATION_FOR_SYSTEM_MESSAGES, ussValue: RDStore.YN_YES]
        )
    }

    /**
     * Strips the chars '<' and '>' from a given string
     * @param s the string to be sanitized
     * @return the sanitized string
     */
    static String cleanUp(String s) {
        s.replaceAll("\\<.*?>","")
    }

    /**
     * Sanitizes the title of the system message
     * @return the sanitized title string
     */
    String getCleanTitle() {
        SystemAnnouncement.cleanUp(BeanStore.getEscapeService().replaceUmlaute(title))
    }

    /**
     * Sanitizes the content of the system message
     * @return the sanitized content string
     */
    String getCleanContent() {
        SystemAnnouncement.cleanUp(BeanStore.getEscapeService().replaceUmlaute(content))
    }

    /**
     * Publishes the system message via the given channels (display on pages, sending of reminder mails)
     * @return true if the publishing was successful, false otherwise
     */
    boolean publish() {
        if (ConfigMapper.getConfig('grails.mail.disabled', Boolean) == true) {
            println 'SystemAnnouncement.publish() failed due grails.mail.disabled = true'
            return false
        }

        withTransaction {

            List<User> reps = SystemAnnouncement.getRecipients()
            List validUserIds = []
            List failedUserIds = []

            lastPublishingDate = new Date()
            isPublished = true
            save()

            reps.each { u ->
                try {
                    _sendMail(u)
                    validUserIds << u.id
                }
                catch (Exception e) {
                    log.error(e.getMessage())
                    e.printStackTrace()
                    failedUserIds << u.id
                }
            }

            status = ([
                    validUserIds : validUserIds,
                    failedUserIds: failedUserIds
            ] as JSON).toString()

            save()

            if (validUserIds.size() > 0) {
                SystemEvent.createEvent('SYSANN_SENDING_OK', ['count': validUserIds.size()])
            }

            if (failedUserIds.size() > 0) {
                SystemEvent.createEvent('SYSANN_SENDING_ERROR', ['users': failedUserIds, 'count': failedUserIds.size()])
            }

            return failedUserIds.isEmpty()
        }
    }

    /**
     * Sends a mail to a given user. The system announcement is being included in a mail template
     * @param user the {@link User} to be notified
     * @throws Exception
     */
    private void _sendMail(User user) throws Exception {

        MessageSource messageSource = BeanStore.getMessageSource()
        Locale language = new Locale(user.getSetting(UserSetting.KEYS.LANGUAGE_OF_EMAILS, RefdataValue.getByValueAndCategory('de', RDConstants.LANGUAGE)).value.toString())

        String currentServer = AppUtils.getCurrentServer()
        String subjectSystemPraefix = (currentServer == AppUtils.PROD) ? "LAS:eR - " : (ConfigMapper.getLaserSystemId() + " - ")
        String mailSubject = subjectSystemPraefix + messageSource.getMessage('email.subject.sysAnnouncement', null, language)

        boolean isRemindCCbyEmail = user.getSetting(UserSetting.KEYS.IS_REMIND_CC_BY_EMAIL, RDStore.YN_NO)?.rdValue == RDStore.YN_YES
        String ccAddress

        if (isRemindCCbyEmail){
            ccAddress = user.getSetting(UserSetting.KEYS.REMIND_CC_EMAILADDRESS, null)?.getValue()
            // println user.toString() + " : " + isRemindCCbyEmail + " : " + ccAddress
        }

        if (isRemindCCbyEmail && ccAddress) {
            BeanStore.getMailService().sendMail {
                to      user.getEmail()
                from    ConfigMapper.getNotificationsEmailFrom()
                cc      ccAddress
                replyTo ConfigMapper.getNotificationsEmailReplyTo()
                subject mailSubject
                body    (view: "/mailTemplates/text/systemAnnouncement", model: [user: user, announcement: this])
            }
        }
        else {
            BeanStore.getMailService().sendMail {
                to      user.getEmail()
                from    ConfigMapper.getNotificationsEmailFrom()
                replyTo ConfigMapper.getNotificationsEmailReplyTo()
                subject mailSubject
                body    (view: "/mailTemplates/text/systemAnnouncement", model: [user: user, announcement: this])
            }
        }
    }
}

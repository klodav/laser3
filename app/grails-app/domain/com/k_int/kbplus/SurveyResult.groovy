package com.k_int.kbplus

import com.k_int.kbplus.abstract_domain.AbstractProperty
import com.k_int.kbplus.auth.User



class SurveyResult extends AbstractProperty {

    Date dateCreated
    Date lastUpdated

    Org owner
    Org participant
    User user

    Date finishDate
    Date startDate
    Date endDate

    String comment
    String participantComment
    String ownerComment

    SurveyProperty type
    SurveyConfig surveyConfig
    ArrayList resultValues

    static constraints = {
        importFrom AbstractProperty

        finishDate (nullable:true, blank:false)
        user (nullable:true, blank:false)
        comment (nullable:true, blank:false)
        resultValues (nullable:true, blank:false)
        startDate  (nullable:true, blank:false)
        endDate (nullable:true, blank:false)
        participantComment (nullable:true, blank:false)
        ownerComment (nullable:true, blank:false)
    }

    static mapping = {
        includes AbstractProperty.mapping

        id column: 'surre_id'
        version column: 'surre_version'

        dateCreated column: 'surre_date_created'
        lastUpdated column: 'surre_last_updated'

        resultValues column: 'surre_result_values'

        startDate column: 'surre_start_date'
        endDate column: 'surre_end_date'
        finishDate column: 'surre_finish_date'


        owner column: 'surre_owner_fk'
        participant column: 'surre_participant_fk'
        user column: 'surre_user_fk'

        type column: 'surre_type_fk'
        surveyConfig column: 'surre_survey_config_fk'


        comment column: 'surre_comment'
        participantComment column: 'surre_participant_comment'
        ownerComment column: 'surre_owner_comment'


    }

    boolean getFinish()
    {
        if(type?.type == Integer.toString())
        {
            return intValue ? true : false
        }
        else if (type?.type == String.toString())
        {
            return stringValue ? true : false
        }
        else if (type?.type ==  BigDecimal.toString())
        {
            return decValue ? true : false
        }
        else if (type?.type == Date.toString())
        {
            return dateValue ? true : false
        }
        else if (type?.type == URL.toString())
        {
            return urlValue ? true : false
        }
        else if (type?.type == RefdataValue.toString())
        {
            return refValue ? true : false
        }

    }

    CostItem getCostItem(){
        return CostItem.findBySurveyOrg(SurveyOrg.findBySurveyConfigAndOrg(surveyConfig, participant))
    }

    def getResult()
    {
        if(type?.type == Integer.toString())
        {
            return intValue.toString()
        }
        else if (type?.type == String.toString())
        {
            return stringValue
        }
        else if (type?.type ==  BigDecimal.toString())
        {
            return decValue.toString()
        }
        else if (type?.type == Date.toString())
        {
            return dateValue.getDateString()
        }
        else if (type?.type == URL.toString())
        {
            return urlValue.toString()
        }
        else if (type?.type == RefdataValue.toString())
        {
            return refValue ? refValue?.getI10n('value') : ""
        }

    }

}

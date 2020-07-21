package com.k_int.kbplus

import de.laser.helper.RDConstants
import de.laser.helper.RefdataAnnotation

class Combo {

    @RefdataAnnotation(cat = RDConstants.COMBO_STATUS)
    RefdataValue status

    @RefdataAnnotation(cat = RDConstants.COMBO_TYPE)
    RefdataValue type

    Org fromOrg
    Org toOrg

    Date dateCreated
    Date lastUpdated

    static mapping = {
                cache true
                id column:'combo_id'
           version column:'combo_version'
            status column:'combo_status_rv_fk'
              type column:'combo_type_rv_fk'
           fromOrg column:'combo_from_org_fk'
             toOrg column:'combo_to_org_fk'

        dateCreated column: 'combo_date_created'
        lastUpdated column: 'combo_last_updated'
    }

    static constraints = {
        status  (nullable:true, blank:false)
        type    (blank:false)
        fromOrg (blank:false)
        toOrg   (blank:false)

        // Nullable is true, because values are already in the database
        lastUpdated (nullable: true)
        dateCreated (nullable: true)
    }
}

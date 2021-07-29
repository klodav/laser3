package de.laser.workflow

import de.laser.RefdataValue
import de.laser.annotations.RefdataAnnotation
import de.laser.helper.RDConstants

class WfTask extends WfTaskBase {

    static final String KEY = 'WF_TASK'

    @RefdataAnnotation(cat = RDConstants.WF_TASK_STATUS)
    RefdataValue status

    WfTaskPrototype prototype

    WfCondition condition

    WfTask child
    WfTask next

    String comment

    // static belongsTo = [ workflow: WfWorkflow ]

    static mapping = {
                 id column: 'wft_id'
            version column: 'wft_version'
           priority column: 'wft_priority_rv_fk'
             status column: 'wft_status_rv_fk'
               //type column: 'wft_type_rv_fk'
          prototype column: 'wft_prototype_fk'
              title column: 'wft_title'
        description column: 'wft_description', type: 'text'
            comment column: 'wft_comment', type: 'text'
          condition column: 'wft_condition_fk'
              child column: 'wft_child_fk'
               next column: 'wft_next_fk'

        dateCreated column: 'wft_date_created'
        lastUpdated column: 'wft_last_updated'
    }

    static constraints = {
        title       (blank: false)
        description (nullable: true)
        condition   (nullable: true)
        child       (nullable: true)
        next        (nullable: true)
        comment     (nullable: true)
    }

    List<WfTask> getSequence() {
        List<WfTask> sequence = []

        WfTask t = this
        while (t) {
            sequence.add( t ); t = t.next
        }
        sequence
    }

    void remove() throws Exception {
        if (this.child) {
            this.child.remove()
        }
        if (this.next) {
            this.next.remove()
        }
        if (this.condition) {
            this.condition.remove()
        }
        this.delete()
    }

    WfWorkflow getWorkflow() {
        WfWorkflow.findByChild( this )
    }

    WfTask getParent() {
        WfTask.findByChild( this )
    }

    WfTask getPrevious() {
        WfTask.findByNext( this )
    }
}

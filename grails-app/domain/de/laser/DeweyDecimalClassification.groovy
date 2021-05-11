package de.laser

import de.laser.annotations.RefdataAnnotation
import de.laser.helper.RDConstants
import de.laser.interfaces.CalculatedLastUpdated
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class DeweyDecimalClassification implements CalculatedLastUpdated, Comparable{

    def cascadingUpdateService

    Long id
    Long version
    @RefdataAnnotation(cat = RDConstants.DDC)
    RefdataValue ddc
    Date dateCreated
    Date lastUpdated
    Date lastUpdatedCascading

    static Log static_logger = LogFactory.getLog(DeweyDecimalClassification)

    static belongsTo = [
        tipp: TitleInstancePackagePlatform,
        pkg: Package
    ]

    static constraints = {
        tipp (nullable: true)
        pkg  (nullable: true)
        dateCreated (nullable: true)
        lastUpdated (nullable: true)
        lastUpdatedCascading (nullable: true)
    }

    static mapping = {
        id                    column: 'ddc_id'
        version               column: 'ddc_version'
        ddc                   column: 'ddc_rv_fk'
        tipp                  column: 'ddc_tipp_fk'
        pkg                   column: 'ddc_pkg_fk'
        dateCreated           column: 'ddc_date_created'
        lastUpdated           column: 'ddc_last_updated'
        lastUpdatedCascading  column: 'ddc_last_updated_cascading'
    }

    @Override
    int compareTo(Object o) {
        DeweyDecimalClassification ddc2 = (DeweyDecimalClassification) o
        ddc <=> ddc2.ddc
    }

    @Override
    def afterInsert() {
        static_logger.debug("afterInsert")
        cascadingUpdateService.update(this, dateCreated)
    }

    @Override
    def afterUpdate() {
        static_logger.debug("afterUpdate")
        cascadingUpdateService.update(this, lastUpdated)
    }

    @Override
    def afterDelete() {
        static_logger.debug("afterDelete")
        cascadingUpdateService.update(this, new Date())
    }

    @Override
    Date _getCalculatedLastUpdated() {
        (lastUpdatedCascading > lastUpdated) ? lastUpdatedCascading : lastUpdated
    }

    static DeweyDecimalClassification construct(Map<String, Object> configMap) {
        if(configMap.tipp || configMap.pkg) {
            DeweyDecimalClassification ddc = new DeweyDecimalClassification(ddc: configMap.ddc)
            if(configMap.tipp)
                ddc.tipp = configMap.tipp
            else if(configMap.pkg)
                ddc.pkg = configMap.pkg
            if(!ddc.save()) {
                static_logger.error("error on creating ddc: ${ddc.getErrors().getAllErrors().toListString()}")
            }
            ddc
        }
        else {
            static_logger.error("No reference object specified for DDC!")
            null
        }
    }
}

package de.laser.reporting.report

import de.laser.annotations.RefdataAnnotation
import de.laser.base.AbstractBase
import de.laser.reporting.export.base.BaseDetailsExport
import de.laser.reporting.report.myInstitution.base.BaseConfig
import grails.util.Holders
import org.springframework.context.i18n.LocaleContextHolder

import java.lang.reflect.Field

class GenericHelper {

    static boolean isFieldMultiple(String fieldName) {
        if (fieldName in [ 'annual' ]) {
            return true
        }
        return false
    }

    static boolean isFieldVirtual(String fieldName) {
        if (fieldName in [ 'region' ]) {
            return true
        }
        return false
    }

    static String getFieldType(Map<String, Object> objConfig, String fieldName) {
        objConfig.fields.get(fieldName)
    }

    static String getFieldLabel(Map<String, Object> objConfig, String fieldName) {

        String label = '?'
        String type = getFieldType(objConfig, fieldName)

        // println 'objConfig - ' + objConfig
        // println 'fieldName - ' + fieldName
        // println 'type - ' + type

        Object messageSource = Holders.grailsApplication.mainContext.getBean('messageSource')
        Locale locale = LocaleContextHolder.getLocale()

        if (type in [BaseConfig.FIELD_TYPE_PROPERTY, BaseDetailsExport.FIELD_TYPE_PROPERTY] ) {
            // LaserReportingTagLib:reportFilterProperty

            Field prop = (fieldName == 'globalUID') ? AbstractBase.getDeclaredField(fieldName) : objConfig.meta.class.getDeclaredField(fieldName)
            String csn = objConfig.meta.class.simpleName.uncapitalize() // TODO -> check

//            try {
            label = messageSource.getMessage(csn + '.' + prop.getName() + '.label', null, locale)
//            } catch(Exception e) {
//                println " -----------> No message found under code '${csn}.${prop.getName()}.label'"
//                label = messageSource.getMessage(csn + '.' + prop.getName(), null, locale)
//            }
        }

        if (type in [BaseConfig.FIELD_TYPE_REFDATA, BaseDetailsExport.FIELD_TYPE_REFDATA] ) {
            // LaserReportingTagLib:reportFilterRefdata

            Field refdata   = objConfig.meta.class.getDeclaredField(fieldName)
            def anno        = refdata.getAnnotationsByType(RefdataAnnotation).head()
            String rdCat    = anno.cat()
            String rdI18n   = anno.i18n()

            label = rdI18n != 'n/a' ? messageSource.getMessage(rdI18n, null, locale) : messageSource.getMessage(rdCat + '.label', null, locale) // TODO -> @RefdataAnnotation
        }

        if (type in [BaseConfig.FIELD_TYPE_REFDATA_JOINTABLE, BaseDetailsExport.FIELD_TYPE_REFDATA_JOINTABLE] ) {
            // LaserReportingTagLib:reportFilterRefdata

            Map<String, Object> customRdv = BaseConfig.getCustomImplRefdata(fieldName)
            label = customRdv.get('label')
        }
        if (type in [BaseConfig.FIELD_TYPE_CUSTOM_IMPL, BaseDetailsExport.FIELD_TYPE_CUSTOM_IMPL] ) {
            // LaserReportingTagLib:reportFilterRefdata

            Map<String, Object> customRdv = BaseConfig.getCustomImplRefdata(fieldName)
            if (!customRdv) {
                println '>> ' + fieldName + ' : ' + type + ' not found!'
            }
            label = customRdv.get('label')
        }

        label
    }
}

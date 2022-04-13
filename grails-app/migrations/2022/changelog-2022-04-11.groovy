import de.laser.helper.MigrationHelper

databaseChangeLog = {

    changeSet(author: "klober (modified)", id: "1649658426259-1") {
        grailsChange {
            change {
                String collate = MigrationHelper.DE_U_CO_PHONEBK_X_ICU
                sql.execute('create collation if not exists "' + collate + '" (provider = icu, locale = "' + collate.replace('-x-icu', '') + '")')
            }
            rollback {}
        }
    }
    changeSet(author: "klober (modified)", id: "1649658426259-2") {
        grailsChange {
            change {
                String collate = MigrationHelper.EN_US_U_VA_POSIX_X_ICU
                sql.execute('create collation if not exists "' + collate + '" (provider = icu, locale = "' + collate.replace('-x-icu', '') + '")')
            }
            rollback {}
        }
    }

    changeSet(author: "klober (modified)", id: "1649658426259-3") {
        grailsChange {
            change {
                List<List> todo = [
                        ['due_date_object', 'ddo_attribute_value', 'varchar(255)', false],
                        ['i10n_translation', 'i10n_value', 'text', false],
                        ['identifier_namespace', 'idns_description', 'text', false],
                        ['identifier_namespace', 'idns_name', 'varchar(255)', false],
                        ['property_definition', 'pd_name', 'varchar(255)', false],
                        ['property_definition', 'pd_explanation', 'text', false],
                        ['refdata_category', 'rdc_description', 'varchar(255)', true],
                        ['refdata_value', 'rdv_value', 'varchar(255)', true],
                        ['refdata_value', 'rdv_explanation', 'text', false],
                        ['system_message', 'sm_content', 'text', false]
                ]
                todo.each { nfo ->
                    String table = nfo[0]
                    String column = nfo[1]
                    String type = nfo[2]
                    boolean index = nfo[3]

                    sql.execute('alter table ' + table + ' alter column ' + column + '_de type ' + type + ' collate "' + MigrationHelper.DE_U_CO_PHONEBK_X_ICU + '"')
                    if (index) {
                        sql.execute('drop index ' + column + '_de_idx')
                        sql.execute('create index ' + column + '_de_idx on ' + table + '(' + column + ')')
                    }

                    sql.execute('alter table ' + table + ' alter column ' + column + '_en type ' + type + ' collate "' + MigrationHelper.EN_US_U_VA_POSIX_X_ICU + '"')
                    if (index) {
                        sql.execute('drop index ' + column + '_en_idx')
                        sql.execute('create index ' + column + '_en_idx on ' + table + '(' + column + ')')
                    }
                }
            }
            rollback {}
        }
    }
}
databaseChangeLog = {

    changeSet(author: "galffy (generated)", id: "1622209756718-1") {
        addColumn(tableName: "org") {
            column(name: "org_link_resolver_base_url", type: "text")
        }
    }

    changeSet(author: "galffy (hand-coded)", id: "1622209756718-2") {
        grailsChange {
            change {
                sql.execute("delete from reader_number where num_reference_group not in (select rdv_value_de from refdata_value join refdata_category on rdv_owner = rdc_id where rdc_description = 'number.type')")
            }
            rollback {}
        }
    }

    changeSet(author: "djebeniani (generated)", id: "1622209756718-3") {
        dropForeignKeyConstraint(baseTableName: "survey_property", constraintName: "fk594d0d1aab666169")
    }

    changeSet(author: "djebeniani (generated)", id: "1622209756718-4") {
        dropTable(tableName: "survey_property")
    }

}

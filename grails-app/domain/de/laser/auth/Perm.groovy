package de.laser.auth

/**
 * Class to retain permissions to role types; this ensures the permission cascade for users and orgs
 */
class Perm {

    /**
     * the perm being granted
     */
    String code

    static mapping = {
        cache   true
        version false
        id      column:'pm_id'
        code    column:'pm_code'
    }

    static constraints = {
        code    blank: false, unique: true
    }

    static hasMany = [
            grantedTo: PermGrant
    ]

    static mappedBy = [
            grantedTo: 'perm'
    ]
}

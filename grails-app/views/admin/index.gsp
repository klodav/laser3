<%@ page import="de.laser.config.ConfigMapper; de.laser.utils.DateUtils; grails.util.Metadata; de.laser.utils.AppUtils" %>

<laser:htmlStart text="${message(code:'menu.admin')} ${message(code:'default.dashboard')}" />

    <ui:breadcrumbs>
        <ui:crumb message="menu.admin" controller="admin" action="index" class="active"/>
    </ui:breadcrumbs>

    <ui:h1HeaderWithIcon message="menu.admin" type="admin" />

    <table class="ui celled la-js-responsive-table la-table la-hover-table table compact">
        <thead>
            <tr><th class="seven wide">${AppUtils.getMeta('info.app.name')}</th><th class="nine wide"></th></tr>
        </thead>
        <tbody>
            <tr><td>App version</td><td> ${AppUtils.getMeta('info.app.version')}</td></tr>
            <tr><td>Configuration file</td><td> ${ConfigMapper.getCurrentConfigFile(this.applicationContext.getEnvironment()).name}</td></tr>
            <tr><td>Environment</td><td> ${Metadata.getCurrent().getEnvironment()}</td></tr>
            <tr><td>Database</td><td> ${ConfigMapper.getConfig('dataSource.url', String).split('/').last()}</td></tr>
            <tr><td>DBM version</td><td> ${dbmVersion[0]} -> ${dbmVersion[1]} <br/> ${DateUtils.getLocalizedSDF_noZ().format(dbmVersion[2])}</td></tr>
            <tr><td>Document storage</td><td> ${docStore.filesCount} Files -> ${docStore.folderSize} MB</td></tr>
        </tbody>
    </table>

<br />
<br />

    <table class="ui sortable celled la-js-responsive-table la-table la-hover-table compact table">
        <thead>
        <tr>
            <th>${message(code:'default.category.label')}</th>
            <th>${message(code:'default.relevance.label')}</th>
            <th>${message(code:'default.source.label')}</th>
            <th>${message(code:'default.event.label')}</th>
            <th>Payload</th>
            <th>${message(code:'default.date.label')}</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${events}" var="el" status="i">
            <tr>
                <td> ${el.category} </td>
                <td> ${el.relevance} </td>
                <td> ${el.source} </td>
                <td> ${el.event} </td>
                <td> ${el.payload?.replaceAll(',', ', ')} </td>
                <td> <g:formatDate date="${el.created}" format="${message(code:'default.date.format.noZ')}" /> </td>
            </tr>
        </g:each>
        </tbody>
    </table>

<laser:htmlEnd />

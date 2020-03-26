<%@ page import="de.laser.helper.RDStore; com.k_int.properties.PropertyDefinition;com.k_int.kbplus.RefdataCategory;com.k_int.kbplus.RefdataValue;" %>
<laser:serviceInjection/>
<!doctype html>



<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser')} : ${message(code: 'myinst.currentSubscriptions.label')}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="survey" action="currentSurveysConsortia" text="${message(code: 'menu.my.surveys')}"/>

    <g:if test="${surveyInfo}">
        <semui:crumb controller="survey" action="show" id="${surveyInfo.id}" text="${surveyInfo.name}"/>
    </g:if>
    <semui:crumb message="myinst.currentSubscriptions.label" class="active"/>
</semui:breadcrumbs>

<br>

<h1 class="ui icon header"><semui:headerTitleIcon type="Survey"/>
<semui:xEditable owner="${surveyInfo}" field="name"/>
<semui:surveyStatus object="${surveyInfo}"/>
</h1>

<g:render template="nav"/>

<semui:messages data="${flash}"/>


<g:if test="${editable}">
    <input class="ui button" value="${message(code:'surveyProperty.create_new')}"
           data-semui="modal" data-href="#createSurveyPropertyModal" type="submit">
</g:if>

<semui:form>

    <h4 class="ui icon header la-clear-before la-noMargin-top">${message(code: 'surveyProperty.all.label')}

        <i class="question circle icon la-popup"></i>

        <div class="ui popup">
            <i class="shield alternate icon"></i> = ${message(code: 'subscription.properties.my')}
        </div>
        <semui:totalNumber total="${properties.size()}"/>

    </h4>

    <table class="ui celled sortable table la-table">
        <thead>
        <tr>
            <g:if test="${addSurveyConfigs}">
                <th class="left aligned"></th>
            </g:if>
            <th class="center aligned">${message(code: 'sidewide.number')}</th>
            <g:if test="${language?.toLowerCase() in ['de_de', 'de']}">
                <g:set var="SUBSTITUTE" value="de"/>
                <th>${message(code: 'default.name.label')}</th>
                <th>${message(code: 'propertyDefinition.expl.label')}</th>
            </g:if>
            <g:else>
                <g:set var="SUBSTITUTE" value="en"/>
                <th>${message(code: 'default.name.label')}</th>
                <th>${message(code: 'propertyDefinition.expl.label')}</th>
            </g:else>
            <th>${message(code: 'default.type.label')}</th>
            <th class="la-action-info">${message(code: 'default.actions.label')}</th>
        </tr>
        </thead>

        <g:each in="${properties.sort { it."name_${SUBSTITUTE}"?.toLowerCase() }}" var="property" status="i">
            <tr>
                <g:if test="${addSurveyConfigs}">
                    <td>
                        <g:if test="${com.k_int.kbplus.SurveyConfig.findBySurveyInfoAndSurveyProperty(surveyInfo, property)}">
                            <i class="check circle icon green"></i>
                        </g:if>
                        <g:else>
                            <g:checkBox name="selectedProperty" value="${property.id}" checked="false"/>
                        </g:else>
                    </td>
                </g:if>
                <td class="center aligned">
                    ${i + 1}
                </td>
                <td>
                    <g:if test="${property?.tenant && property?.tenant.id == institution.id}">
                        <semui:xEditable owner="${property}" field="name_${SUBSTITUTE}" type="textarea"/>
                        <i class='shield alternate icon'></i>
                    </g:if>
                    <g:else>
                        ${property?.getI10n('name')}
                    </g:else>
                </td>

                <td>

                    <g:if test="${property?.tenant && property?.tenant.id == institution.id}">
                        <semui:xEditable owner="${property}" field="expl_${SUBSTITUTE}" type="textarea"/>
                    </g:if>
                    <g:else>
                        ${property?.getI10n('expl')}
                    </g:else>

                </td>
                <td>
                    ${com.k_int.properties.PropertyDefinition.getLocalizedValue(property.type)}
                    <g:if test="${property.type == 'class com.k_int.kbplus.RefdataValue'}">
                        <g:set var="refdataValues" value="${[]}"/>
                        <g:each in="${com.k_int.kbplus.RefdataCategory.getAllRefdataValues(property.refdataCategory)}"
                                var="refdataValue">
                            <g:set var="refdataValues"
                                   value="${refdataValues + refdataValue?.getI10n('value')}"/>
                        </g:each>
                        <br>
                        (${refdataValues.join('/')})
                    </g:if>
                </td>
                <td class="x">
                    <g:if test="${property.countUsages() == 0 && property?.tenant?.id == institution?.id}">
                        <g:link action="deleteSurveyProperty" id="${params.id}" params="[deleteId: property?.id]"
                                class="ui icon negative button">
                            <i class="trash alternate icon"></i>
                        </g:link>
                    </g:if>
                </td>
            </tr>
        </g:each>
    </table>

</semui:form>

<semui:modal id="createSurveyPropertyModal" message="surveyProperty.create_new.label">
    <div class=" content">

    <g:form class="ui form" action="createSurveyProperty" method="post" params="[surveyInfo: surveyInfo?.id]">

        <div class="field">
            <label class="property-label">Name</label>
            <input type="text" name="pd_name"/>
        </div>

        <div class="field">
            <label class="property-label">${message(code: 'propertyDefinition.expl.label')}</label>
            <textarea name="pd_expl" id="pd_expl" class="ui textarea" rows="2"></textarea>
        </div>

        <div class="fields">

            <div class="field six wide">
                <label class="property-label">${message(code: 'propertyDefinition.descr.label')}</label>
                <%--<g:select name="pd_descr" from="${PropertyDefinition.AVAILABLE_PRIVATE_DESCR}"/>--%>
                <select name="pd_descr" id="pd_descr" class="ui dropdown">
                    <g:each in="${[PropertyDefinition.SUR_PROP]}" var="pd">
                        <option value="${pd}"><g:message code="propertyDefinition.${pd}.label"
                                                         default="${pd}"/></option>
                    </g:each>
                </select>
            </div>

            <div class="field five wide">
                <label class="property-label"><g:message code="default.type.label"/></label>
                <g:select class="ui dropdown"
                          from="${PropertyDefinition.validTypes2.entrySet()}"
                          optionKey="key" optionValue="${{ PropertyDefinition.getLocalizedValue(it.key) }}"
                          name="pd_type"
                          id="cust_prop_modal_select"/>
            </div>

            %{--<div class="field four wide">
                <label class="property-label">Optionen</label>

                <g:checkBox type="text" name="pd_mandatory"/> ${message(code: 'default.mandatory.tooltip')}
                <br/>
                <g:checkBox type="text"
                            name="pd_multiple_occurrence"/> ${message(code: 'default.multipleOccurrence.tooltip')}
            </div>--}%

        </div>

        <div class="fields">
            <div class="field hide" id="cust_prop_ref_data_name" style="width: 100%">
                <label class="property-label"><g:message code="refdataCategory.label"/></label>

                <input type="hidden" name="refdatacategory" id="cust_prop_refdatacatsearch"/>
                <g:set var="propertyService" bean="propertyService"/>

                <div class="ui grid" style="margin-top:1em">
                    <div class="ten wide column">
                        <g:each in="${propertyService.getRefdataCategoryUsage()}" var="cat">

                            <p class="hidden" data-prop-def-desc="${cat.key}">
                                Häufig verwendete Kategorien: <br/>

                                <%
                                    List catList = cat.value?.take(3)
                                    catList = catList.collect { entry ->
                                        '&nbsp; - ' + (RefdataCategory.getByDesc(entry[0]))?.getI10n('desc')
                                    }
                                    println catList.join('<br />')
                                %>

                            </p>
                        </g:each>
                    </div>

                    <div class="six wide column">
                        <br/>
                        <a href="<g:createLink controller="profile" action="properties"/>" target="_blank">
                            <i class="icon external alternate"></i>
                            Alle Kategorien und Referenzwerte<br/>als Übersicht öffnen
                        </a>
                    </div>
                </div><!-- .grid -->
            </div>
        </div>

    </g:form>
</semui:modal>

<g:javascript>

$('#pd_descr').change(function() {
$('#cust_prop_modal_select').trigger('change');
});

$('#cust_prop_modal_select').change(function() {
var selectedText = $( "#cust_prop_modal_select option:selected" ).val();
if( selectedText == "class com.k_int.kbplus.RefdataValue") {
$("#cust_prop_ref_data_name").show();

var $pMatch = $( "p[data-prop-def-desc='" + $( "#pd_descr option:selected" ).val() + "']" )
if ($pMatch) {
$( "p[data-prop-def-desc]" ).addClass('hidden')
$pMatch.removeClass('hidden')
}
}
else {
$("#cust_prop_ref_data_name").hide();
}
});

$('#cust_prop_modal_select').trigger('change');

$("#cust_prop_refdatacatsearch").select2({
placeholder: "Kategorie eintippen...",
minimumInputLength: 1,

formatInputTooShort: function () {
return "${message(code:'select2.minChars.note', default:'Please enter 1 or more character')}";
},
formatNoMatches: function() {
return "${message(code:'select2.noMatchesFound')}";
},
formatSearching:  function() {
return "${message(code:'select2.formatSearching')}";
},
ajax: { // instead of writing the function to execute the request we use Select2's convenient helper
url: '${createLink(controller:'ajax', action:'lookup')}',
dataType: 'json',
data: function (term, page) {
return {
q: term, // search term
page_limit: 10,
baseClass:'com.k_int.kbplus.RefdataCategory'
};
},
results: function (data, page) {
return {results: data.values};
}
}
});
$(".la-popup").popup({
});
</g:javascript>

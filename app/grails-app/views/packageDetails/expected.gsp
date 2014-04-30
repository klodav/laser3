<%@ page import="com.k_int.kbplus.Package" %>
<!doctype html>
<html>
  <head>
    <meta name="layout" content="mmbootstrap">
    <g:set var="entityName" value="${message(code: 'package.label', default: 'Package')}" />
    <title><g:message code="default.edit.label" args="[entityName]" /></title>
  </head>
  <body>


    <div class="container">
      <ul class="breadcrumb">
        <li><g:link controller="home" action="index">Home</g:link> <span class="divider">/</span></li>
        <li><g:link controller="packageDetails" action="index">All Packages</g:link><span class="divider">/</span></li>
        <li><g:link controller="packageDetails" action="show" id="${packageInstance.id}">${packageInstance.name}</g:link></li>

        <li class="dropdown pull-right">
          <a class="dropdown-toggle" id="export-menu" role="button" data-toggle="dropdown" data-target="#" href="">Exports<b class="caret"></b></a>

          <ul class="dropdown-menu filtering-dropdown-menu" role="menu" aria-labelledby="export-menu">
            <li><g:link action="show" params="${params+[format:'json']}">Json Export</g:link></li>
            <li><g:link action="show" params="${params+[format:'xml']}">XML Export</g:link></li>
            <g:each in="${transforms}" var="transkey,transval">
              <li><g:link action="show" id="${params.id}" params="${[format:'xml',transformId:transkey]}"> ${transval.name}</g:link></li>
            </g:each>
          </ul>
        </li>

        <li class="pull-right">
          View:
          <div class="btn-group" data-toggle="buttons-radio">
            <g:link controller="packageDetails" action="expected" params="${params+['mode':'basic']}" class="btn btn-primary btn-mini ${((params.mode=='basic')||(params.mode==null))?'active':''}">Basic</g:link>
            <g:link controller="packageDetails" action="expected" params="${params+['mode':'advanced']}" button type="button" class="btn btn-primary btn-mini ${params.mode=='advanced'?'active':''}">Advanced</g:link>
          </div>
          &nbsp;
        </li>
        
      </ul>
    </div>

    <g:if test="${pendingChanges?.size() > 0}">
      <div class="container alert-warn">
        <h6>This Package has pending change notifications</h6>
        <g:if test="${editable}">
          <g:link controller="pendingChange" action="acceptAll" id="com.k_int.kbplus.Package:${packageInstance.id}" class="btn btn-success"><i class="icon-white icon-ok"></i>Accept All</g:link>
          <g:link controller="pendingChange" action="rejectAll" id="com.k_int.kbplus.Package:${packageInstance.id}" class="btn btn-danger"><i class="icon-white icon-remove"></i>Reject All</g:link>
        </g:if>
        <br/>&nbsp;<br/>
        <table class="table table-bordered">
          <thead>
            <tr>
              <td>Info</td>
              <td>Action</td>
            </tr>
          </thead>
          <tbody>
            <g:each in="${pendingChanges}" var="pc">
              <tr>
                <td>${pc.desc}</td>
                <td>
                  <g:if test="${editable}">
                    <g:link controller="pendingChange" action="accept" id="${pc.id}" class="btn btn-success"><i class="icon-white icon-ok"></i>Accept</g:link>
                    <g:link controller="pendingChange" action="reject" id="${pc.id}" class="btn btn-danger"><i class="icon-white icon-remove"></i>Reject</g:link>
                  </g:if>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>
    </g:if>


      <div class="container">

        <div class="page-header">
          <div>
          <h1><g:if test="${editable}"><span id="packageNameEdit"
                        class="xEditableValue"
                        data-type="textarea"
                        data-pk="${packageInstance.class.name}:${packageInstance.id}"
                        data-name="name"
                        data-url='<g:createLink controller="ajax" action="editableSetValue"/>'>${packageInstance.name}</span></g:if><g:else>${packageInstance.name}</g:else></h1>
            <g:render template="nav" contextPath="." />
            <sec:ifAnyGranted roles="ROLE_ADMIN,KBPLUS_EDITOR">
            <g:link controller="announcement" action="index" params='[at:"Package Link: ${pkg_link_str}",as:"RE: Package ${packageInstance.name}"]'>Mention this package in an announcement</g:link> |
            </sec:ifAnyGranted>
            <g:if test="${forum_url != null}">
              <a href="${forum_url}">Discuss this package in forums</a> <a href="${forum_url}" title="Discuss this package in forums (new Window)" target="_blank"><i class="icon-share-alt"></i></a>
            </g:if>

          </div>

        </div>
    </div>

        <g:if test="${flash.message}">
        <bootstrap:alert class="alert-info">${flash.message}</bootstrap:alert>
        </g:if>

        <g:hasErrors bean="${packageInstance}">
        <bootstrap:alert class="alert-error">
        <ul>
          <g:eachError bean="${packageInstance}" var="error">
          <li <g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message error="${error}"/></li>
          </g:eachError>
        </ul>
        </bootstrap:alert>
        </g:hasErrors>

    <div class="container">

        <dl>
          <dt>Titles (${offset+1} to ${lasttipp}  of ${num_tipp_rows})
            <g:if test="${params.mode=='advanced'}">Includes Expected or Expired titles, switch to the <g:link controller="packageDetails" action="show" params="${params+['mode':'basic']}">Basic</g:link> view to hide them</g:if>
                <g:else>Expected or Expired titles are not shown, use the <g:link controller="packageDetails" action="show" params="${params+['mode':'advanced']}" button type="button" >Advanced</g:link> view to see them</g:else>
              )

          </dt>
          <dd>

          <table class="table table-bordered">
            <g:form action="packageBatchUpdate" params="${[id:packageInstance?.id]}">
            <thead>
            <tr>
              <th>&nbsp;</th>
              <th>&nbsp;</th>
              <g:sortableColumn params="${params}" property="tipp.title.title" title="Title" />
              <th style="">Platform</th>
              <th style="">Identifiers</th>
              <th style="">Coverage Start</th>
              <th style="">Coverage End</th>
              <th style="">Coverage Depth</th>
            </tr>
            </thead>
            <tbody>
            <g:set var="counter" value="${offset+1}" />
            <g:each in="${titlesList}" var="t">
              <g:set var="hasCoverageNote" value="${t.coverageNote?.length() > 0}" />
              <tr>
                <td ${hasCoverageNote==true?'rowspan="2"':''}><g:if test="${editable}"><input type="checkbox" name="_bulkflag.${t.id}" class="bulkcheck"/></g:if></td>
                <td ${hasCoverageNote==true?'rowspan="2"':''}>${counter++}</td>
                <td style="vertical-align:top;">
                   ${t.title.title}
                   <g:link controller="titleDetails" action="show" id="${t.title.id}">(Title)</g:link>
                   <g:link controller="tipp" action="show" id="${t.id}">(TIPP)</g:link><br/>
                   <span title="${t.availabilityStatusExplanation}">Access: ${t.availabilityStatus?.value}</span>
                   <g:if test="${params.mode=='advanced'}">
                     <br/> Record Status: <g:xEditableRefData owner="${t}" field="status" config='TIPPStatus'/>
                     <br/> Access Start: <g:xEditable owner="${t}" type="date" field="accessStartDate" />
                     <br/> Access End: <g:xEditable owner="${t}" type="date" field="accessEndDate" />
                   </g:if>
                </td>
                <td style="white-space: nowrap;vertical-align:top;">
                   <g:if test="${t.hostPlatformURL != null}">
                     <a href="${t.hostPlatformURL}">${t.platform?.name}</a>
                   </g:if>
                   <g:else>
                     ${t.platform?.name}
                   </g:else>
                </td>
                <td style="white-space: nowrap;vertical-align:top;">
                  <g:each in="${t.title.ids}" var="id">
                    ${id.identifier.ns.ns}:${id.identifier.value}<br/>
                  </g:each>
                </td>

                <td style="white-space: nowrap">
                  Date: <g:xEditable owner="${t}" type="date" field="startDate" /><br/>
                  Volume: <g:xEditable owner="${t}" field="startVolume" /><br/>
                  Issue: <g:xEditable owner="${t}" field="startIssue" />     
                </td>

                <td style="white-space: nowrap"> 
                   Date: <g:xEditable owner="${t}" type="date" field="endDate" /><br/>
                   Volume: <g:xEditable owner="${t}" field="endVolume" /><br/>
                   Issue: <g:xEditable owner="${t}" field="endIssue" />
                </td>
                <td>
                  <g:xEditable owner="${t}" field="coverageDepth" />
                </td>
              </tr>

              <g:if test="${hasCoverageNote==true}">
                <tr>
                  <td colspan="6">coverageNote: ${t.coverageNote}</td>
                </tr>
              </g:if>

            </g:each>
            </tbody>
            </g:form>
          </table>
          </dd>
        </dl>

        <div class="pagination" style="text-align:center">
          <g:if test="${titlesList}" >
            <bootstrap:paginate  action="show" controller="packageDetails" params="${params}" next="Next" prev="Prev" maxsteps="${max}" total="${num_tipp_rows}" />
          </g:if>
        </div>



        <g:if test="${editable}">
        
        <g:form controller="ajax" action="addToCollection">
          <fieldset>
            <legend><h3>Add A Title To This Package</h3></legend>
            <input type="hidden" name="__context" value="${packageInstance.class.name}:${packageInstance.id}"/>
            <input type="hidden" name="__newObjectClass" value="com.k_int.kbplus.TitleInstancePackagePlatform"/>
            <input type="hidden" name="__recip" value="pkg"/>

            <!-- N.B. this should really be looked up in the controller and set, not hard coded here -->
            <input type="hidden" name="status" value="com.k_int.kbplus.RefdataValue:29"/>

            <label>Title To Add</label>
            <g:simpleReferenceTypedown class="input-xxlarge" style="width:350px;" name="title" baseClass="com.k_int.kbplus.TitleInstance"/><br/>
            <span class="help-block"></span>
            <label>Platform For Added Title</label>
            <g:simpleReferenceTypedown class="input-large" style="width:350px;" name="platform" baseClass="com.k_int.kbplus.Platform"/><br/>
            <span class="help-block"></span>
            <button type="submit" class="btn">Add Title...</button>
          </fieldset>
        </g:form>


        </g:if>

      </div>


    <g:render template="enhanced_select" contextPath="../templates" />
    <g:render template="orgLinksModal" 
              contextPath="../templates" 
              model="${[roleLinks:packageInstance?.orgs,parent:packageInstance.class.name+':'+packageInstance.id,property:'orgs',recip_prop:'pkg']}" />

    <r:script language="JavaScript">
      $(function(){
        $.fn.editable.defaults.mode = 'inline';
        $('.xEditableValue').editable();
      });
      function selectAll() {
        $('.bulkcheck').attr('checked', true);
      }

      function confirmSubmit() {
        if ( $('#bulkOperationSelect').val() === 'remove' ) {
          var agree=confirm("Are you sure you wish to continue?");
          if (agree)
            return true ;
          else
            return false ;
        }
      }

    </r:script>

  </body>
</html>

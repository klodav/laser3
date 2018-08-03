<g:if test="${contact}">
	<div class="ui item contact-details">
		<semui:contactIcon type="${contact?.contentType.('value')}"/>
        <div class="content">
            <g:link controller="contact" action="show" id="${contact?.id}">
                <%-- ${contact.contentType?.getI10n('value')}: --%>
                ${contact?.content?.encodeAsHTML()}
            </g:link>
        </div>

		<g:if test="${editable && tmplShowDeleteButton}">
			<div class="content">
                <g:if test="${['Mail', 'E-Mail'].contains(contact?.contentType?.value)}">
                    <span data-position="top right" data-tooltip="Mail senden an ..">
                        <a href="mailto:${contact?.content?.encodeAsHTML()}" class="ui mini icon button">
                            <i class="share square icon"></i>
                        </a>
                    </span>
                </g:if>

                <g:set var="oid" value="${contact.class.name}:${contact.id}" />

				<g:link class="ui mini icon negative button" controller="${controller}" action="${action}" id="${id}" params="[cmd: 'deleteContact', oid: oid]">
					<i class="trash alternate icon"></i>
				</g:link>
			</div>
		</g:if>
	</div>			
</g:if>
${contactInstance?.contentType}
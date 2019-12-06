<g:set var="uri" value="${controllerName}/${actionName}" />

<r:script>
    $('.la-js-filterButton').on('click', function(){
        $( ".la-filter").toggle( "fast" );
        $(this).toggleClass("blue");
        $.ajax({
            url: '<g:createLink controller="ajax" action="updateSessionCache"/>',
            data: {
                key:    "${com.k_int.kbplus.UserSettings.KEYS.SHOW_EXTENDED_FILTER}",
                value: !($(this).hasClass('blue')),
                uri:    "${uri}"
            }
        });
    });
</r:script>
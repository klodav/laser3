
<form class="form-search">
    <input type="text" id="spotlight_text" class="input-medium search-query">
 	<i onclick="showHelp()"class="icon-question-sign"></i>
</form>
<div id="spotlight-search-results">
</div>


%{-- Problem with layers. Probably caused because of spotlight popup 
	<div class="modal hide" id="spotlight_help">
	<div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">×</button>
        <h3 class="ui header">Spotlight help</h3>
    </div>

    <div class="modal-body">
    	instructions here
	</div>
</div> --}%

<script language="JavaScript">
/*    function showHelp() {
        var helpStr = "${message(code: "spotlight.search.help")}"
    	alert(helpStr)
    }
	function reloadSpotlightSearchResults() {
	  
	  var q =  $("#spotlight_text").val();
	  if(q.length > 0 ){
		  q= encodeURIComponent(q);
		  $('#spotlight-search-results').load("<g:createLink controller='spotlight' action='search'/>"+"?query="+q);
	  }
	}
	var timeoutReference;

	//make sure we dont send too many requests. Limit to 1 per 500ms
	$('#spotlight_text').keyup(function(event) {
        var _this = $(this); 
    	if (timeoutReference) clearTimeout(timeoutReference);

        if(event.keyCode == 27){ //esc
        	$('.dlpopover').popover('toggle') // TODO: removed
        }else{
	        timeoutReference = setTimeout(function() {
	            reloadSpotlightSearchResults();
	        }, 500);       	
        }
    });
	
	$("#spotlight_text").focus()*/

</script>

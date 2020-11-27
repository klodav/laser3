<%@ page import="org.grails.web.util.GrailsApplicationAttributes" %>
<!doctype html>

<!--[if lt IE 7]> <html class="no-js lt-ie9 lt-ie8 lt-ie7" lang="de"> <![endif]-->
<!--[if IE 7]>    <html class="no-js lt-ie9 lt-ie8" lang="de"> <![endif]-->
<!--[if IE 8]>    <html class="no-js lt-ie9" lang="de"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js" lang="de"> <!--<![endif]-->

<head>
    <meta charset="utf-8">
    <title><g:layoutTitle default="${meta(name: 'app.name')}"/></title>
    <meta name="description" content="">
    <meta name="author" content="">

    <meta name="viewport" content="initial-scale = 1.0">

    <asset:stylesheet src="laser.css"/>%{-- dont move --}%

    <script>
        <g:render template="/templates/javascript/laser.js" />%{-- dont move --}%
        <g:render template="/templates/javascript/dict.js" />%{-- dont move --}%
    </script>
    <asset:javascript src="base.js"/>%{-- dont move --}%


    <!-- Le HTML5 shim, for IE6-8 support of HTML elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <g:layoutHead/>

    <tmpl:/layouts/favicon />

</head>
    <body class="public">

        <g:layoutBody/><!-- body here -->
    
        <div id="Footer">
            <div class="clearfix"></div>
        </div>

        <asset:javascript src="laser.js"/>%{-- dont move --}%

        <asset:deferredScripts/>

    </body>
</html>

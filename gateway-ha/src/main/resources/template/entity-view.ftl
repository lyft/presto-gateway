<html>
<head>
    <meta charset="UTF-8"/>
    <link rel="stylesheet" type="text/css" href="assets/css/common.css"/>
    <link rel="stylesheet" type="text/css" href="assets/css/jquery.dataTables.min.css"/>
    <script src="assets/js/jquery-3.3.1.js"></script>

    <!-- JSON Editor comes here-->
    <link rel="stylesheet" href="assets/js/jsonedit/jsoneditor.min.css"/>
    <script src="assets/js/jsonedit/jsoneditor.min.js" defer></script>
    <script src="assets/js/entity-editor.js" defer></script>

    <script type="text/javascript">
        $(document).ready(function () {
            renderConfigSelector();
            document.getElementById("entity_editor_tab").style.display = "inline";
            document.getElementById("entity_editor_tab").style.backgroundColor = "grey";
        })
    </script>
</head>
<body>
<#include "header.ftl">
<h2>!!Admin only!!</h2>
<div>
  <span>Override the Database (Optional): <span>
  <input id="databaseOverride" type="text"/>
  <input id="refreshEntity" type="button" onclick="renderConfigSelector()" value="Refresh Entities" />
</div>
<div id="entity-editor-place-holder"></div>

<#include "footer.ftl">

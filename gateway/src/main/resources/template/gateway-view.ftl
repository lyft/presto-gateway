<#-- @ftlvariable name="" type="com.lyft.data.gateway.resource.GatewayViewResource$GatewayView" -->

<html>
<head>
    <link rel="stylesheet" type="text/css" href="assets/css/jquery.dataTables.min.css"/>

    <script src="assets/js/jquery-3.3.1.js"></script>
    <script src="assets/js/jquery.dataTables.min.js"></script>
    <script type="application/javascript">
        $(document).ready(function () {
            $('#queryHistory').DataTable();
        });
    </script>
</head>
<body>
<div>Gateway Server started at : ${gatewayStartTime?number_to_datetime}</div>
<div>
    All active backends:
</div>
<div>
    <table>
        <thead>
        <tr>
            <th>ClusterName</th>
            <th>URL</th>
            <th>LocalPort</th>
            <th>ScheduledCluster</th>
        </tr>
        </thead>
        <tbody>
        <#list backendConfigurations as bc>
        <tr>
            <td>  ${bc.name}</td>
            <td><a href="${bc.proxyTo}" target="_blank">${bc.proxyTo}</a></td>
            <td> ${bc.localPort}</td>
            <td> ${bc.scheduledCluster?c}</td>
        </tr>
        </#list>
        </tbody>
    </table>
</div>
<br/><br/>
<a onclick="location.reload()" href="">Refresh</a>
<#if queryHistory?size != 0>

<div>Query details [history size = ${queryHistory?size} ]</div>
<div>
    <table id="queryHistory" class="display" style="width:100%">
        <thead>
        <tr>
            <th>queryId</th>
            <th>user</th>
            <th>source</th>
            <th>queryText</th>
            <th>captureTime</th>
        </tr>
        </thead>
        <tbody>
            <#list queryHistory as q>
            <tr>
                <td><a href="${q.backendUrl}/ui/query.html?${q.queryId}"
                       target="_blank">${q.queryId}</a></td>
                <td data-search="${q.user}">  ${q.user}</td>
                <td>
                    <#if q.source??>
                        ${q.source}
                    </#if>
                </td>
                <td>${q.queryText}</td>
                <td data-order="${q.captureTime}">${q.captureTime?number_to_datetime}</td>
            </tr>
            </#list>
        </tbody>
    </table>
</div>
</#if>
</body>
</html>
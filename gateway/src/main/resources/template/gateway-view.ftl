<#-- @ftlvariable name="" type="com.lyft.data.gateway.resource.GatewayViewResource$GatewayView" -->

<html>
<head>
    <meta http-equiv="refresh" content="20"/>
</head>
<body>
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
<#if queryHistory?size != 0>
<div>Query details</div>
<div>
    <table>
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
                <td>  ${q.user}</td>
                <td>  ${q.source}</td>
                <td>${q.queryText}</td>
                <td>${q.captureTime?number_to_datetime}</td>
            </tr>
            </#list>
        </tbody>
    </table>
</div>
</#if>
</body>
</html>
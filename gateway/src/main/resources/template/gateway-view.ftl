<#-- @ftlvariable name="" type="com.lyft.data.gateway.resource.GatewayViewResource$GatewayView" -->
<#setting datetime_format = "MM/dd/yyyy hh:mm:ss a '('zzz')'">
<html>
<head>
    <style>
        .pull-left {
            float: left !important
        }

        .pull-right {
            float: right !important
        }

        .dataTables_filter input {
            width: 500px
        }

        /* cellspacing */
        table {
            border-collapse: separate;
            border-spacing: 0px;
        }

        /* valign */
        th, td {
            vertical-align: top;
            border: 0.5px solid;
        }
    </style>

    <link rel="stylesheet" type="text/css" href="assets/css/jquery.dataTables.min.css"/>

    <script src="assets/js/jquery-3.3.1.js"></script>
    <script src="assets/js/jquery.dataTables.min.js"></script>
    <script src="assets/js/hbar-chart.js"></script>
    <script type="application/javascript">
        $(document).ready(function () {
            $('#queryHistory').DataTable(
                    {
                        "ordering": false,
                        "dom": '<"pull-left"f><"pull-right"l>tip'
                    }
            );
            $("ul.chart").hBarChart();
        });
    </script>
</head>
<body>
<div><h3>Gateway Server started at
    : ${gatewayStartTime?number_to_datetime}</h3></div>

<div>
    <table style="border: 0px;">
        <tr>
            <td>
                <h3>All active backends:</h3>
                <table id="availableClusters" class="display">
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
            </td>
        <#if queryHistory?size != 0>
            <td width="400px">
                <div><h3> Query history distribution</h3>
                    <ul class="chart">
                        <#list queryDistribution?keys as cluster>
                            <li data-data="${queryDistribution[cluster]?number}">
                            ${cluster} => ${queryDistribution[cluster]?number}
                            </li>
                        </#list>
                    </ul>
                </div>
            </td>
        </#if>
        </tr>
    </table>

</div>


<a onclick="location.reload()" href="">Refresh</a>

<#if queryHistory?size != 0>
<div><b>Query details [history size = ${queryHistory?size}]</b></div>
<div>
    <table id="queryHistory" class="display" style="width:100%">
        <thead>
        <tr>
            <th>queryId</th>
            <th>user</th>
            <th>source</th>
            <th>queryText</th>
            <th>submissionTime</th>
        </tr>
        </thead>
        <tbody>
            <#list queryHistory as q>
            <tr>
                <td><a href="${q.backendUrl}/ui/query.html?${q.queryId}"
                       target="_blank">${q.queryId}</a></td>
                <td>  ${q.user}</td>
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
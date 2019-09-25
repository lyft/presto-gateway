<#setting datetime_format = "MM/dd/yyyy hh:mm:ss a '('zzz')'">
<html>
<head>
    <meta charset="UTF-8"/>
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
    </style>
    <link rel="stylesheet" type="text/css" href="assets/css/common.css"/>
    <link rel="stylesheet" type="text/css" href="assets/css/jquery.dataTables.min.css"/>

    <script src="assets/js/jquery-3.3.1.js"></script>
    <script src="assets/js/jquery.dataTables.min.js"></script>
    <script src="assets/js/hbar-chart.js"></script>

    <script type="application/javascript">
        $(document).ready(function () {
            $('#queryHistory').DataTable(
                {
                    "ordering": false,
                    "dom": '<"pull-left"f><"pull-right"l>tip',
                    "width": '100%'
                }
            );
            $("ul.chart").hBarChart();
            document.getElementById("query_history_tab").style.backgroundColor = "grey";
        });

    </script>
</head>
<body>
<#include "header.ftl">
<div>
    Started at :
    <script>document.write(new Date(${gatewayStartTime?long?c}).toLocaleString());</script>
</div>

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
                    <td><a href="/ui/query.html?${q.queryId}"
                           target="_blank">${q.queryId}</a></td>
                    <td>  ${q.user}</td>
                    <td>
                        <#if q.source??>
                            ${q.source}
                        </#if>
                    </td>
                    <td>${q.queryText}</td>
                    <td data-order="${q.captureTime}">
                        <script>document.write(new Date(${q.captureTime?long?c}).toLocaleString());</script>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>

    <div><h3> Query history distribution</h3>
        <ul class="chart">
            <#list queryDistribution?keys as cluster>
                <li data-data="${queryDistribution[cluster]?string}">
                    ${cluster?string} => ${queryDistribution[cluster]?string}
                </li>
            </#list>
        </ul>
    </div>

</#if>
<#include "footer.ftl">


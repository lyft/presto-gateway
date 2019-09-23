var entityData;
var editor;

function submitData(url, data, type) {
    if (data === undefined) {
        data = ""
    }
    return $.ajax({
        url: url,
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        type: type, // get,post,put,delete
        dataType: "json", // NOTE: expecting 'json' fails on empty 200 response
        data: data,
        statusCode: {
            404: function () {
                console.log("404 Error in response")
            },
            500: function () {
                console.log("500 Error in response")
            }
        }
    }).always(function () {
    })
}

function renderConfigSelector() {
    var html = "<table><tr><td>Select config type</td><td><select name='entityTypeSelector' id='entityTypeSelector' onchange='renderEntitySelector()'></select></td></tr>"
        + "<tr><td>Select entity to edit</td><td><select name='entitySelector' id='entitySelector' onchange='renderEntityEditor()'></select></td></tr>"
        + "</table>" + "<div id='entityEditorDiv'>"
        + "<table><tr><td><div id='entityDetails' style='width: 700px; height: 500px;'></div></td>"
        + "<td><div id='entityDetailsTextArea' style='width: 700px; height: 500px;'></div></td></tr></table>"
        + "</div> <div id='entityEditorBottom'></div>";

    $("#entity-editor-place-holder").html(html);
    submitData("/entity", "", "get").done(function (data) {
        var select = "<option value='select'>Select</option>";
        for (var i in data) {
            select += "<option value='" + data[i] + "'>" + data[i] + "</option>";
        }
        $("#entityTypeSelector").html(select);
    });
}

function renderEntitySelector() {
    var entityType = $("#entityTypeSelector").find(':selected').val();

    clear();
    $("#entitySelector").empty();
    entityData = "";

    if (entityType !== 'select') {
        submitData("/entity/" + entityType, "", "get").done(function (data) {
            entityData = data;
            var select = "<option value='select'>Select</option>";
            for (var i in data) {
                select += "<option value='" + buildNameForEntity(data[i], entityType) + "'>" + buildNameForEntity(data[i], entityType)
                    + "</option>";
            }
            $("#entitySelector").html(select);
        });
    }
    renderEntityEditor()
}

function buildNameForEntity(entity, entityType) {
    switch (entityType) {
        case "GATEWAY_BACKEND":
            return entity.name;
        default:
            console.log("entity type not found : " + entityType);
            return entity[0];
    }
}

function renderEntityEditor() {
    clear();
    var entityType = $("#entityTypeSelector").find(':selected').val();
    var element = document.getElementById("entityDetails");
    editor = new JSONEditor(element, {schema: {type: "object"}});
    var entity = "";
    var entityId = $("#entitySelector").find(':selected').val();
    for (var i in entityData) {
        if (buildNameForEntity(entityData[i], entityType) == entityId) {
            entity = entityData[i];
        }
    }

    editor.set(entity);

    var submitHtml = "<p/><input type='submit' name='submit' onclick='updateObject()' />";
    $("#entityEditorBottom").html(submitHtml);

    var entityDetailsTextArea = $("#entityDetailsTextArea");
    entityDetailsTextArea.html("<textarea rows='20' cols='50' id='jsonTextArea'>" + JSON.stringify(editor.get(), null, 4) + "</textarea>");
    entityDetailsTextArea.append("<p/><input type='button' value='load to editor' onclick='loadToEditor()' />");
}

function updateObject() {
    var entityType = $("#entityTypeSelector").find(':selected').val();
    var jsonVal = JSON.stringify(editor.get());
    submitData("/entity?entityType=" + entityType, jsonVal, "post").done(function (data) {
        console.log(data);
        renderEntitySelector();
    })
}

function loadToEditor() {
    editor.set(JSON.parse($("#jsonTextArea").val()));
}

function clear() {
    $("#entityDetails").empty();
    $("#entityDetailsTextArea").empty();
    $("#entityEditorBottom").empty();
}

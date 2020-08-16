var CLOUD_LABELS = ["0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100"];
var CLOUD_MIN_MAX_LABELS = ["Min", "", "", "", "", "", "", "", "", "Max"];
var CLOUD_MIN_MAX_LABEL_CLASSES = ["cloud-report-min-label", "", "", "", "", "", "", "", "", "cloud-report-max-label"];

function cloudDescriptionTable(labels, labelClasses) {
    var table = '<div>' +
        '<table class="cloud-report-table">' +
        '<tr>' +
        '<td style="background: #d04437"></td>' +
        '<td style="background: #d5584c"></td>' +
        '<td style="background: #d9695f"></td>' +
        '<td style="background: #d9695f"></td>' +
        '<td style="background: #de7b72"></td>' +
        '<td style="background: #e28e87"></td>' +
        '<td style="background: #f6c342"></td>' +
        '<td style="background: #fae1a0"></td>' +
        '<td style="background: #14892c"></td>' +
        '<td style="background: #89c495"></td>' +
        '</tr><tr>';


    if (labelClasses) {
        labels.forEach(function (element, index) {
            table += "<td class=\"" + labelClasses[index] + "\">" + element + "</td>";
        });
    } else {
        labels.forEach(function (element, index) {
            table += "<td class=\"cloud-report-label\">" + element + "</td>";
        });
    }
    table += '</tr></table></div>';
    return table;
}


function topRisksDescription() {
    var description = "<p>The Top Risks tag cloud highlights those classes that are the most complex, yet are" +
        "the least covered by your tests. The larger and redder the class, the greater" +
        "the risk that class poses for your project or package. </p>" +
        "<p>Font size represents" +
        "the Average Method Complexity metric, while the colour represents the Total" +
        "Coverage metric as follows:</p>";
    return description + cloudDescriptionTable(CLOUD_LABELS);
}

function quickWinsDescription() {
    var description = "<p>The Quick Wins tag cloud highlights the \"low hanging coverage fruit\" of your project" +
        "or package. You will achieve the greatest increase in overall code coverage by" +
        "covering the largest, reddest classes first. Big red classes contain the highest" +
        "number of untested elements. </p>" +
        "<p>Font size represents the Number of Elements metric," +
        "while the colour represents the Number of Tested Elements as follows:" +
        "</p>";

    return description + cloudDescriptionTable(CLOUD_MIN_MAX_LABELS, CLOUD_MIN_MAX_LABEL_CLASSES);
}

function treeMapDescription() {
    var description = "<p>The coverage treemap report allows simultaneous comparison of classes and package by" +
        "complexity and by code coverage. This is useful for spotting untested clusters of code." +
        "The treemap is divided by package (labelled) and" +
        "then further divided by class (unlabelled). The size of the package or class" +
        "indicates its complexity (larger squares indicate great complexity, while smaller" +
        "squares indicate less complexity).</p>" +
        "<p>Colours indicate the level of coverage, as follows:</p>";

    return description + cloudDescriptionTable(CLOUD_LABELS);
}
AJS.$(function($) {
    // initialize tooltips on all elements with title attribute
    $("[title]").tooltip({
        aria:true,
        html: true,
        hideOnClick: true,
        hoverable: false,
        gravity: $.fn.tipsy.autoWE
    });


    // shows the dialog when the "Show dialog" button is clicked
    $(".dialog-show-button").click(function(e) {
        AJS.dialog2("#" + $(this).data('dialog-id')).show();
    });

    // hides the dialog
    $(".dialog-close-button").click(function(e) {
        e.preventDefault();
        AJS.dialog2("#" + $(this).data('dialog-id')).hide();
    });

    // set checkbox state, firing an event only if state has changed
    // element - jquery object
    // newValue - true/false
    function toggleWithEvent(element, newValue) {
        if (element.prop('checked') != newValue) {
            element.prop('checked', newValue);
            element.change(); // fire event
        }
    }

    var $testMethodCheckboxesToggle = $("#selectalltests");
    var $testMethodCheckboxes = $('input[name="testMethod"]');
    // select all tests for hi-lighting
    $testMethodCheckboxesToggle.click(function toggleSelectAllTests() {
        if (!$testMethodCheckboxesToggle.hasClass("active")) {
            $testMethodCheckboxes.each(function () { toggleWithEvent($(this), true) } );
            $testMethodCheckboxesToggle.addClass("active");
        } else {
            $testMethodCheckboxes.each(function () { toggleWithEvent($(this), false) } );
            $testMethodCheckboxesToggle.removeClass("active");
        }
    });

    $testMethodCheckboxes.change(function(element) {
        // change state of the main button
        if ($testMethodCheckboxes.length != $('input[name="testMethod"]:checked').length) {
            $testMethodCheckboxesToggle.removeClass("active");
        } else {
            $testMethodCheckboxesToggle.addClass("active");
        }

        // highlight source code and methods
        hiLightByTest($(this).prop('value'), $(this).prop('checked'));
    });

    // ======================================================================

    var $packagesTree = $(".packages-tree-container");
    var $packagesWrapper = $(".packages-tree-wrapper");

    $packagesTree.cloverPackages({
        currentPackage: $packagesTree.parent().data("package-name"),
        lozengesPlaceholder: ".clover-packages-lozenges",
        noResultsMessage: ".package-filter-no-results-message",
        packages: Packages.nodes,
        urlPrefix: $packagesTree.parent().data("root-relative"),
        wrapper: ".packages-tree-wrapper"
    });
    $packagesTree.data("current-search", "");

    var $filterForm = $('.package-filter-container');
    var $filterInput = $('#package-filter');

    $filterForm.submit(function(e) {
        e.preventDefault();
    });

    $filterInput.on("input keyup", function(e){
        var val = $filterInput.val();

        if ($packagesTree.data("current-search") !== val){
            $packagesTree.data("current-search", val);

            $packagesTree.cloverPackages({
                "search": val
            });
        }
    });

    $filterInput.on("keydown", function(e){
        var special = [13, 27, 37, 38, 39, 40];

        if (special.indexOf(e.which) !== -1){
            e.preventDefault();

            if (e.which === 27){
                $filterInput.val("");

                $packagesTree.data("current-search", "");
                $packagesTree.cloverPackages({
                    "search": ""
                });
            } else {
                $packagesTree.cloverPackages({
                    "keyDown": e.which
                });
            }
        }
    });

    $filterInput.on("blur", function(){
        $packagesTree.cloverPackages({
            "resetKeyboardSelection": true
        });
    });

    /* === sidebar === */

    var updateSidebarHeight = function(){
        var $footer = $("#footer");
        var $sidebar = $(".aui-page-panel-nav-clover");
        var clientHeight = document.documentElement.clientHeight;
        var height = clientHeight - parseInt($sidebar.css("top"));

        $sidebar.css({
            height: height+"px",
            visibility: "visible"
        });

        $packagesWrapper.css({
            height: height-217+"px"
        });

        $footer.css("margin-top", "0px");

        var footerOffset = $footer.offset();
        var footerHeight = $footer.outerHeight();
        var marginTop = clientHeight - footerOffset.top - footerHeight - 1;

        if (marginTop > 0){
            $footer.css("margin-top", marginTop + "px");
        }

        $filterInput.focus();
    };

    updateSidebarHeight();

    $(window).on("resize", updateSidebarHeight);
});


var SRC_FILE_LEGEND_TEXT =
        '<h2>Legend</h2>' +
        '<p><b>Left margin ruler</b><br/>' +
        '<small>Left margin ruler shows information about global and per-test coverage</small></p>' +
        '<table class="legend" id="legend" style="padding: 0; border-spacing: 0;">' +
        '<tbody>' +
        '<tr>' +
        '<td class="methodToggle" align="right">line#</td>' +
        '<td class="methodToggle" align="right">hit count</td>' +
        '<td class="methodToggle"></td>' +
        '</tr><tr>' +
        '<td class="lineCount Good" align="right">1</td>' +
        '<td class="coverageCount Good missedByTest" align="right">17</td>' +
        '<td class="srcCellLegend">line was covered, but not by a test (e.g. by main(), setUp() or tearDown() method)</td>' +
        '</tr><tr>' +
        '<td class="lineCount Good" align="right">2</td>' +
        '<td class="coverageCount Good hitByTest" align="right">86</td>' +
        '<td class="srcCellLegend">line was covered by test(s) which passed</td>' +
        '</tr><tr>' +
        '<td class="lineCount Good" align="right">3</td>' +
        '<td class="coverageCount Good hitByFailedTest" align="right">7</td>' +
        '<td class="srcCellLegend">line was covered by test(s) which did not pass (includeFailedTestCoverage=true)</td>' +
        '</tr><tr>' +
        '<td class="lineCount Bad" align="right">4</td>' +
        '<td class="coverageCount Bad hitByFailedTest" align="right">7</td>' +
        '<td class="srcCellLegend">line was covered by test(s) which did not pass (includeFailedTestCoverage=false)</td>' +
        '</tr><tr>' +
        '<td class="lineCount Good" align="right">5</td>' +
        '<td class="coverageCount Bad hitByTest" align="right">1</td>' +
        '<td class="srcCellLegend">line is covered partially (i.e some branch or a statement was not covered) and was hit by a test</td>' +
        '</tr><tr>' +
        '<td class="lineCount Good" align="right">6</td>' +
        '<td class="coverageCount Bad missedByTest" align="right">1</td>' +
        '<td class="srcCellLegend">line is covered partially, but not by a test</td>' +
        '</tr><tr>' +
        '<td class="lineCount Bad" align="right">7</td>' +
        '<td class="coverageCount Bad missedByTest" align="right">0</td>' +
        '<td class="srcCellLegend">line was not covered at all</td>' +
        '</tr><tr>' +
        '<td class="lineCount Filtered " align="right">8</td>' +
        '<td class="coverageCount Filtered" align="right"></td>' +
        '<td class="srcCellLegend">line was filtered</td>' +
        '</tr>' +
        '</tbody></table>' +
        '<p><b>Source code highlighting</b><br/>' +
        '<small>Highlighing shows information about contributing tests as well as about code which was not covered or filtered.</small></p>' +
        '<table class="legend" id="legend" style="padding: 0; border-spacing: 0">' +
        '<tbody>' +
        '<tr>' +
        '<td class="methodToggle"></td>' +
        '</tr><tr>' +
        '<td class="srcCellWithSpacer"><span class="srcLine">line not covered by any of the selected tests</span></td>' +
        '</tr><tr>' +
        '<td class="srcCellWithSpacer"><span class="srcLine coveredByTest">line hit by more than one of the selected tests</span></td>' +
        '</tr><tr>' +
        '<td class="srcCellWithSpacer"><span class="srcLine coveredByTestUniq">line hit by one test only (unique per-test coverage)</span></td>' +
        '</tr><tr>' +
        '<td class="srcCellWithSpacer"><span class="srcLine coveredByFailedTest">line hit by one or more of the selected tests that all failed</span></td>' +
        '</tr><tr>' +
        '<td class="srcCellWithSpacer"><span class="srcLine srcLineHilight">line was not covered (partially or at all)</span></td>' +
        '</tr><tr>' +
        '<td class="srcCellWithSpacer"><span class="srcLine srcLineFiltered">line was filtered</span></td>' +
        '</tr>' +
        '</tbody></table>';

// close a modal dialog with the given id and scroll view
function closeDialogAndScrollTo(modalDialogId, anchor) {
    AJS.dialog2("#" + modalDialogId).hide();
    location.hash = "#" + anchor;
}

// Filter rows in 'Contributing tests' table by a test name (it's in a 2nd or a 3rd column)
// @param tableBodyId - id of <tbody> containing rows to be filtered
// @param inputFilterId - id of the input field with a substring to match
// @param testColumnNo - number of a column containing test name
function filterTests(tableBodyId, inputFilterId, testColumnNo) {
    var searchedText = $("#" + inputFilterId).attr('value');
    if (searchedText == undefined || $.trim(searchedText).length == 0) {
        // show all rows
        $("#" + tableBodyId + " > tr").show();
    } else {
        // walk through all rows and hide those where 3rd td has a href's text matching text
        $("#" + tableBodyId + " > tr")
            .has("td:nth-child(" + testColumnNo + ") > span:not( :contains('" +  $.trim(searchedText) + "') )")
            .hide();
        $("#" + tableBodyId + " > tr")
            .has("td:nth-child(" + testColumnNo + ") > span:contains('" +  $.trim(searchedText) + "')")
            .show();
    }
}

// Filter rows in 'Class methods' table by a method signature (it's in a 1st column)
// @param tableBodyId - id of <tbody> containing rows to be filtered
// @param inputFilterId - id of the input field with a substring to match
function filterMethods(tableBodyId, inputFilterId) {
    var searchedText = $("#" + inputFilterId).attr('value');
    if (searchedText == undefined || $.trim(searchedText).length == 0) {
        // show all rows
        $("#" + tableBodyId + " > tr").show();
    } else {
        // walk through all rows and hide those where 3rd td has a href's text matching text
        $("#" + tableBodyId + " > tr")
            .has("td:nth-child(1):not( :contains('" +  $.trim(searchedText) + "') )")
            .hide();
        $("#" + tableBodyId + " > tr")
            .has("td:nth-child(1):contains('" +  $.trim(searchedText) + "')")
            .show();
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////// OLD JS

function toggleInlineStats(ele, hiddenEleId) {
    var statsEle = document.getElementById(hiddenEleId);
    var showStats = ele.className.match(/\baui-iconfont-arrows-right\b/);
    statsEle.style.display = showStats ? '' : 'none';
    var regex = showStats ? /\baui-iconfont-arrows-right\b/ : /\baui-iconfont-arrows-left\b/ ;
    var replacement = showStats ? 'aui-iconfont-arrows-left' : 'aui-iconfont-arrows-right';
    replaceClass(ele, regex, replacement);
}
function toggleNodeExpansion(ele, collapsed, expanded) {
    toggleNodeEx(document.getElementById(ele), document.getElementById(collapsed), document.getElementById(expanded));
}

function toggleNodeEx(ele, collapsed, expanded) {
    var expand = expanded.style.display == 'none';
    collapsed.style.display = expand ? 'none' : '';
    expanded.style.display = expand ? '' : 'none';

    var regex = expand ? /\bexpand\b/ : /\bcollapse\b/ ;
    var replacement = expand ? 'collapse' : 'expand';
    replaceClass(ele, regex, replacement);
}

function toggleAllInlineMethods(element) {
    var inlineExpandAll = (element.innerHTML == 'Expand all methods');
    setToggleLabel(element, inlineExpandAll);
    var visitor = function (method) {
        var startEle = document.getElementById('img-' + method.sl);
        forceToggleSrcRowVis(startEle, method.sl, method.el, inlineExpandAll);
    };
    visitAllMethods(visitor);
}

// traveses the data model executes the visitor() function on each method.
function visitAllMethods(visitor) {

    var classes = clover.pageData.classes;
    for (var i = 0; i < classes.length; i++) {
        var classData = classes[i];
        var methods = classData.methods;
        for (var j = 0; j < methods.length; j++) {
            var method = methods[j];
            visitor(method);
        }
    }
}

// traveses the DOM, executes visitor for each src-LineNumber element
function visitAllSrcLines(visitor) {

    var ele;
    var i = 1;
    while ((ele = document.getElementById("src-" + i)) != undefined){
        visitor(ele, i);
        i++;
    }
}

function toggleSrcRowVis(toggle, start, end) {
    var first = start + 1;
    var display = 'none';
    var expand = document.getElementById("l" + first).style.display == 'none';
    forceToggleSrcRowVis(toggle, start, end, expand);
}

// expands or collapses a method
function forceToggleSrcRowVis(toggle, start, end, expand) {
    var display = expand ? '' : 'none';
    if (expand) {
        swapExpandImg(toggle);
        document.getElementById("e"+start).style.display='none';
    } else {
        swapCollapseImg(toggle);
        document.getElementById("e"+start).style.display='';
    }

    for (var i = start + 1; i <= end; i++) {
        document.getElementById("l" + i).style.display = display;
    }
}

// Acts as a map: methodStartLine --> hit count
var methodsToHiLight = new Object();
// srcLine --> hit count
var linesToHiLight = new Object();
// linuNum --> LineTestInfo
var selectedLinesTestResult = new Object();

function loadPkgPane(title, fileName) {
    if (parent.packagePane != undefined && fileName != undefined) {
        parent.packagePane.location.href = fileName;
        setBrowserTitle(title);
    }
}

function setBrowserTitle(title) {
    if (title != undefined && parent) {
        parent.document.title = title;
    }
}

function parseQueryArgs() {
    var params = [];
    var paramsString = window.location.search.split('?')[1];
    if (paramsString != undefined) {
        paramsString.split('&').forEach(function (item) {
            var splitted = item.split("=");
            params[splitted[0]] = splitted[1];
        });
    }
    return params;
}


//used by src-file.vm
function onLoad(title) {
    var queryArgs = parseQueryArgs();

    var testId = queryArgs["id"];
    var lineNo = queryArgs["line"];
    
    if (testId != undefined) {
        hilightTestOnLoad(testId);
    } else if (lineNo != undefined) {
        hilightLineOnLoad(lineNo);
    }

    setBrowserTitle(title);

}

function hilightLineOnLoad(lineNo) {
    var srcLineSpan = document.getElementById('src-' + lineNo);
    if (srcLineSpan == undefined) return;
    window.setTimeout("window.scrollBy(0, -50);", 10);
    flashLine(srcLineSpan);
}

function flashLine(obj) {
    var originalColor = obj.style.backgroundColor;
    var fade = {
        full : function() {
            obj.style.backgroundColor = '#ffe7c6';
        },
        clear : function() {
            obj.style.backgroundColor = originalColor;
        }
    };
    for (var i = 0; i <= 12; i++) {
        var fadeMethod = (i % 2 == 0) ? fade.clear : fade.full;
        setTimeout(fadeMethod, 150 * i);
    }
}

function hilightTestOnLoad(testId) {
    var cbox = document.getElementById('cb-' + testId);
    if (cbox == undefined) return;
    cbox.checked = true;
    hiLightByTest(testId, true);
}

// called in the onlick on the srcLine margin
function showTestsForLine(ele, startLine, overTitle) {
    var parentTestDiv = createTableForPopup(startLine);
    var inlineDialog = AJS.InlineDialog(ele, "testsForLineDialog",
        function(content, trigger, showPopup) {
            content.html(
                '<h2 style="margin-bottom:20px;">' + overTitle +
                '<input id="test-filter-inline-' + startLine + '" class="test-filter text" type="text" name="test-filter" ' +
                ' placeholder="Type to filter tests..." autocomplete="off" style="float:right"' +
                ' onkeyup="filterTests(\'tests-body-inline-' + startLine + '\', \'test-filter-inline-' + startLine + '\', 2);"/>' +
                '</h2>'
            ).append(parentTestDiv);
            showPopup();
            return false;
        },
        {
            width: 700,
            cacheContent: false,
            hideDelay: 60000,
            hideCallback: function() {
                $("#inline-dialog-testsForLineDialog").remove();
            }
        }
    );
    inlineDialog.show();
}


function showFailingTestsPopup(element, line, traces) {
    var holderDiv = document.createElement('div');

    for (var i = 0; i < traces.length; i++) {
        var tid = traces[i][0];
        var fid = traces[i][1];

        var traceDiv = document.getElementById('trace-'+tid);
        if (traceDiv == undefined) {
            continue;
        }
        var traceControl = document.getElementById('traceControl').cloneNode(true);
        traceControl.className = 'expand';
        traceControl.id = "traceControl"+tid+'-'+i;

        var traceCol = document.getElementById('traceCol'+tid).cloneNode(true);
        traceCol.id = 'traceCol'+tid+'-'+i;
        var traceEx = document.getElementById('traceEx'+tid).cloneNode(true);
        traceEx.id = 'traceEx'+tid+'-'+i;

        var traceLines = traceEx.getElementsByTagName('div');

        traceLines[fid].className='errorTraceStrong';

        holderDiv.appendChild(traceControl);
        holderDiv.appendChild(traceCol);
        holderDiv.appendChild(traceEx);
        holderDiv.appendChild(document.createElement('br'));

        traceControl.setAttribute(
                "onclick",
                "toggleNodeExpansion('"+traceControl.id+"', '"+traceCol.id+"', '"+traceEx.id+"');");
    }

    var inlineDialog = AJS.InlineDialog(element, "testsForLineDialog",
        function(content, trigger, showPopup) {
            content.html(
                    '<h2>Test failures at line ' + line + '</h2>')
                    .append(holderDiv);
            showPopup();
            return false;
        },
        {
            width: 600,
            cacheContent: false
        }
    );
    inlineDialog.show();
}

// gernerate the contents for the per-srcline popup on the fly.
function createTableForPopup(startLine) {
    // a top-level div with a scroll view and a message (optional)
    var holderDiv = $(document.createElement('div'));

    // create scroll view (scroll bars visible when needed) with an aui table
    var scrollViewDiv = $(document.createElement('div'));
    scrollViewDiv.attr('style', 'overflow-x: hidden; overflow-y: auto; max-height:500px; width:100%; padding-right: 20px; border-top: 1px solid #cccccc; border-bottom: 1px solid #cccccc');
    var table = $(document.createElement('table'));
    table.addClass('aui'); // TODO aui-table-sortable
    var thead = $(document.createElement('thead'));
    var tbody = $(document.createElement('tbody'));
    tbody.attr('id', 'tests-body-inline-' + startLine);
    scrollViewDiv.append(table);
    table.append(thead);
    table.append(tbody);

    // table header
    var header = $("#testHeaderRow").clone();
    header.find("th:first").remove();
    thead.append(header);

    // table rows
    var testIdsForLine = clover.srcFileLines[startLine];
    var missingTestCount = 0;
    for (var i = 0; i < testIdsForLine.length; i++) {
        var testId = testIdsForLine[i];
        var linksRow = $('#test-' + testId);
        if (linksRow == undefined) {
            missingTestCount = missingTestCount + 1;
            continue;
        }

        // copy row and remove first cell (with a checkbox)
        var clonedLinksRow = $(linksRow).clone();
        clonedLinksRow.find("td:first").remove();

        // initialize tooltips on all elements with 'title' attribute in the cloned row; we're searching for the
        // 'original-title' (instead of 'title'), because the tooltip was already initialized on the original row
        clonedLinksRow.find("[original-title]").tooltip({
            aria:true,
            html: true,
            hideOnClick: true,
            hoverable: false,
            gravity: $.fn.tipsy.autoWE
        });

        // ensure that the row is visible (the 'display:none' could have been set in the original table)
        clonedLinksRow.css('display', 'table-row');

        tbody.append(clonedLinksRow[0]);
    }

    // add an empty line after the table so that it won't touch with scroll bar's bottom border when scrolled down
    scrollViewDiv.append($(document.createElement('div')).append('<div>&#160;</div>'));

    // add scroll view to a parent
    holderDiv.append(scrollViewDiv);

    // add message below a scroll view
    if (missingTestCount > 0) {
        var textDiv = $(document.createElement('div'));
        textDiv.html('<small>' + missingTestCount + ' ' +
                pluralise(missingTestCount, 'test is', 'tests are') +
                ' not displayed. This report was configured to display the top ' +
                testsPerFile + ' contributing tests for this file.</small>');
        holderDiv.append(textDiv);
    }

//    AJS.tablessortable.setTableSortable($(table)); TODO does not work - double triangle appears

    return holderDiv[0];
}

function pluralise(count, singular, plural) {
    return count != 1 ? plural : singular;
}


// highlights source and summary depending on which tests are checked.
function hiLightByTest(testId, checked) {

    // get all methods hit by the test
    var methods = getMethodsForTest(testId);
    if (methods == undefined) return;
    var testData = getDataForTest(testId);
    if (testData == undefined) return;
    var passed = testData.pass;

    addHitsToMap(methods, methodsToHiLight, checked, passed);
    // now visit all methods on the page, and highlight or unhighlight as needed
    var visitor = function(method) {
        var summTd = document.getElementById('summary-' + method.sl + '-' + method.sc);

        if (methodsToHiLight[method.sl] > 0) {
            addCoverageClass(summTd, selectedLinesTestResult[method.sl]);
        } else {
            removeCoverageClass(summTd, selectedLinesTestResult[method.sl]);            
        }
    };
    visitAllMethods(visitor);

    // hi-light individual src lines.
    var statements = getStatementsForTest(testId);
    if (statements == undefined) return;
    addHitsToMap(statements, linesToHiLight, checked, passed);


    var srcLineVisitor = function(srcEle, lineNumber) {

        if (linesToHiLight[lineNumber] > 0 || methodsToHiLight[lineNumber] > 0) {
            addCoverageClass(srcEle, selectedLinesTestResult[lineNumber]);
        } else {
            removeCoverageClass(srcEle, selectedLinesTestResult[lineNumber]);
        }
        
    };
    visitAllSrcLines(srcLineVisitor);
}

// collects test info for selected tests per line
var LineTestInfo = function(lineNum) {
    this.lineNum = lineNum;
    this.passes = 0;
    this.fails = 0;
};

LineTestInfo.prototype.addResult = function(passed) {
    if (passed) {
        this.passes++;
    } else {
        this.fails++;
    }
};

LineTestInfo.prototype.removeResult = function(passed) {
    if (passed) {
        this.passes--;
    } else {
        this.fails--;
    }
};

LineTestInfo.prototype.isUniqueHit = function() {
    return clover.srcFileLines[this.lineNum].length == 1;
};

// Returns true if all selected tests for the line failed
LineTestInfo.prototype.showFailed = function() {
    return this.passes <= 0 && this.fails > 0;
};

LineTestInfo.prototype.calcCoverageClass = function() {
    if (this.showFailed()) {
        return "coveredByFailedTest";
    }

    if (this.isUniqueHit()) {
        return "coveredByTestUniq";
    }

    return "coveredByTest"

};


function addHitsToMap(hitElements, elementsToHiLight, checked, passed) {

    for(var i = 0; i < hitElements.length; i++) {
        var ele = hitElements[i];
        var currCount = elementsToHiLight[ele.sl];
        currCount = currCount == undefined ? 0 : currCount;
        var increment = checked ? 1 : -1;
        elementsToHiLight[ele.sl] = currCount + increment;

        // get the test info object for the current line and add result
        var info = selectedLinesTestResult[ele.sl] ? selectedLinesTestResult[ele.sl] : new LineTestInfo(ele.sl);
        if (checked) {
            info.addResult(passed);
        } else {
            info.removeResult(passed);
        }
        selectedLinesTestResult[ele.sl] = info;
    }
}

// gets all the methods hit by a given test.
function getMethodsForTest(testId) {
    var testData = getDataForTest(testId);
    if (testData == undefined) return;

    var methods = testData.methods;
    if (methods == undefined) return;
    return methods;
}

// gets all the statements hit by a given test.
function getStatementsForTest(testId) {
    var testData = getDataForTest(testId);
    if (testData == undefined) return;

    var statements = testData.statements;
    if (statements == undefined) return;
    return statements;
}

function getDataForTest(testId) {
    if (testId == undefined) return;

    var testData = clover.testTargets['test_' + testId];
    if (testData == undefined) return;

    return testData;
}

function toggleStats(eleToHide, eleToDisplay) {
    displayEle(document.getElementById(eleToDisplay));
    hideEle(document.getElementById(eleToHide));
}

function displayEle(ele) {
    if (ele == undefined) return;
    ele.style.display = '';
}

function hideEle(ele) {
    if (ele == undefined) return;
    ele.style.display = 'none';
}

function setToggleLabel(ele, expandAll) {
    ele.innerHTML = (expandAll) ? 'Collapse all methods' : 'Expand all methods';
}

function swapExpand(ele) {
    replaceClass(ele, /aui-iconfont-collapsed/, 'aui-iconfont-expanded');
}

function swapCollapse(ele) {
    replaceClass(ele, /aui-iconfont-expanded/, 'aui-iconfont-collapsed');
}

function swapExpandImg(ele) {
    replaceImg(ele, /aui-iconfont-arrows-left/, 'aui-iconfont-arrows-right');
}

function swapCollapseImg(ele) {
    replaceImg(ele, /aui-iconfont-arrows-right/, 'aui-iconfont-arrows-left');
}

var coveredByRegExp = /\b(coveredBy.*)\b/; // matches one of the *coveredBy* classes: ~Test, ~TestUniq, ~FailedTest
var srcLineHilight = /\bsrcLineHilight\b/; // matches already hilighted src lines

// adds the appropriate coverage class to the given element
// testInfo must be a LineTestInfo, and is used to calculate the coverage class to use
function addCoverageClass(ele, testInfo) {
    if (testInfo != null && !ele.className.match(srcLineHilight)) { // if line already hilighted, nothing to do
        var coverageClass = testInfo.calcCoverageClass();
        var matchArray = coveredByRegExp.exec(ele.className); 
        if(matchArray && matchArray.length > 0) { // replace the existing coveredBy class
            replaceClass(ele, coveredByRegExp, coverageClass);
        } else { // add a coveredBy class to the existing className
            ele.className = ele.className + ' ' + testInfo.calcCoverageClass();            
        }        
    }
}
// removes the coverageBy class from the existing element
function removeCoverageClass(ele, testInfo) {
    if (testInfo != null && ele.className.match(coveredByRegExp)) { // do nothing if no coveredBy class
        replaceClass(ele, coveredByRegExp, '');
    }
}

function replaceClass(ele, regex, newClass) {
    ele.className = ele.className.replace(regex, newClass);
}
function replaceImg(ele, regex, newClass) {
    ele.src = ele.src.replace(regex, newClass);
}


function createTreeMap(json) {

    // our own colour interpolation - see also ReportColors.ADG_HEAT_COLORS, as colours should match (moreless)
    TM.Squarified.implement({
        'setColor': function (json) {
            var x = (json.data.$color - 0);
            if (x > 80) {
                var alpha = 0.75 - 0.50 * (x - 80.0)/20.0;
                return "rgba(20,137,44," + alpha + ")"; // ADG green 75%-25%
            } else if (x > 60) {
                var alpha = 0.75 - 0.50 * (x - 60.0)/20.0;
                return "rgba(246,195,66," + alpha + ")"; // ADG yellow 75%-25%
            } else {
                var alpha = 0.75 - 0.50 * x/60.0;
                return "rgba(208,68,55," + alpha + ")"; // ADG red 75%-25%
            }
        }
    });

    return new TM.Squarified({
        //Where to inject the Treemap
        rootId: 'infovis',
        offset: 1,
        titleHeight: 14,

        //Add click handlers for
        //zooming the Treemap in and out
        addLeftClickHandler: true,
        addRightClickHandler: true,

        //When hovering a node highlight the nodes
        //between the root node and the hovered node. This
        //is done by adding the 'in-path' CSS class to each node.
        selectPathOnHover: true,

        Color: {
            //Allow coloring
            allow: true,
            //Select a value range for the $color
            //property. Default's to -100 and 100.
            minValue: 0,
            maxValue: 100,
            //Set color range. Default's to reddish and
            //greenish. It takes an array of three
            //integers as R, G and B values.
            maxColorValue: [241, 227, 226],   // adg red 10%, light gray 90%
            minColorValue: [208, 68, 55]      // adg red 100%
        },

        //Allow tips
        Tips: {
            allow: true,
            //add positioning offsets
            offsetX: 20,
            offsetY: 20,
            //implement the onShow method to
            //add content to the tooltip when a node
            //is hovered
            onShow: function(tip, node, isLeaf, domElement) {
                tip.innerHTML = node.data.title;

            }}
        ,
        // This method is called on each newly created node.
        onCreateElement: function(content, node, isLeaf, elem1, elem2) {
            if (isLeaf) {
                elem1.innerHTML = "";
            }
        },

        // Called for each click to a node.
        request: function(nodeId, level, onComplete) {
            var tree = eval(json);
            var subtree = TreeUtil.getSubtree(tree, nodeId);
            TreeUtil.prune(subtree, 1);
            if (level < 3) { // Leaves are level 3
                onComplete.onComplete(nodeId, subtree);
            } else {
                window.location = subtree.data.path;
            }
        }

    });

}

function processTreeMapJson(json) {
    var tm = createTreeMap(json);
    //load JSON data
    tm.loadJSON(json);

}

function processTreeMapDashJson(json) {
    var tm = createTreeMap(json);

    // customize treemap for the dashboard.
    tm.config.titleHeight = 0;
    tm.config.onCreateElement = function(content, node, isLeaf, elem1, elem2) { };
    // Called for each click to a node.
    tm.config.request = function(nodeId, level, onComplete) {
        window.location = "treemap.html";
    };

    //load JSON data
    tm.loadJSON(json);

}
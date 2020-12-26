(function($){

    var nodes = {},
        nodesByIds = {},
        dashedNodesIds = {},
        uiBackup = {};

    var keyboardSelectedId;
    var keyboardSelectedIndex;

    /**
     * Classes uses by UI elements
     */
    var classes = {
        "arrow": {
            "collapsed": "aui-icon aui-icon-small clover-tree-package-node-arrow aui-iconfont-collapsed",
            "expanded": "aui-icon aui-icon-small clover-tree-package-node-arrow aui-iconfont-expanded"
        },
        "icon": {
            "open": "aui-icon aui-icon-small clover-tree-package-node-icon aui-iconfont-devtools-folder-open",
            "closed": "aui-icon aui-icon-small clover-tree-package-node-icon aui-iconfont-devtools-folder-closed"
        },
        "list": {
            "hidden": "clover-tree-packages-list hidden",
            "visible": "clover-tree-packages-list"
        }
    };

    /**
     * Explicitly defined UI elements
     */
    var ui = {
        packageLink: '.clover-tree-package-link',
        node: '.clover-tree-package-node',
        list: '.clover-tree-packages-list',
        rootList: ".clover-tree-root-list"
    };

    /**
     * Definition of event handlers to be attached
     */
    var events = {
        "click packageLink": "onLinkClick",
        "mouseover node": "onNodeMouseOver",
        "mouseout node": "onNodeMouseOut",
        "click node": "onNodeClick"
    };

    /**
     * The event handlers themselves
     */
    var eventHandlers = {
        "onLinkClick": function(e){
            e.stopPropagation();
        },
        "onNodeMouseOver": function(e){
            e.stopPropagation();
            $(e.currentTarget).addClass("hovered");
        },
        "onNodeMouseOut": function(e){
            e.stopPropagation();
            $(e.currentTarget).removeClass("hovered");
        },
        "onNodeClick": function(e, options){
            e.stopPropagation();
            toggleOpen($(e.currentTarget).data("node-id"), options);
        }
    };

    /**
     * Replaces values in UI hash with corresponding jQuery collections
     */
    var bindUIElements = function(options){
        uiBackup = $.extend({}, ui);

        $.each(ui, function(key, selector){
            ui[key] = options.placeholder.find(selector);
        });
    };

    /**
     * Delegate event handling for defined events for UI elements, as well
     * as event handlers for placeholder
     */
    var bindEvents = function(options){
        $.each(events, function(descriptor, handlerName){
            var tmp = descriptor.split(" ");

            options.placeholder.on(tmp[0], uiBackup[tmp[1]], function(e){
                eventHandlers[handlerName](e, options);
            });
        });

        _updateNodeRowsWidth(options);

        $(options.wrapper).on("scroll", function(){
            var left = $(this).scrollLeft();

            _updateNodeRowsWidth(options);

            $(".clover-tree-package-node-row").each(function(index, elem){
                var newLeft = $(elem).data("default-left") + left;

                $(elem).css("left", newLeft+"px");
            });

            $(options.lozengesPlaceholder).css({
                right: -left+"px"
            });
        });
    };

    /**
     * Execute given function for all the nodes in the tree
     */
    var iterateOverTree = function(func, _nodes, parentData){
        var _nodes = _nodes || nodes;
        var parentData = parentData || null;

        $.each(_nodes, function(index, nodeData){
            var shouldContinue = func(nodeData, parentData);

            if (shouldContinue !== false && nodeData.children.length){
                iterateOverTree(func, nodeData.children, nodeData);
            }
        });
    };

    /**
     * Helper function intended to be used only internally, update of rows' width is necessary
     * since otherwise the area of tree can be scrolled indefinitely
     */
    var _updateNodeRowsWidth = function(options){
        $(".clover-tree-package-node-row").css({
            width: options.placeholder.width()+"px"
        });
    };

    /**
     * Helper function intended to be used only internally
     */
    var _isNodeVisible = function(nodeData, parentData){
        var visible = ((parentData && parentData.state.open && !parentData.hasHiddenClass) || parentData === null) &&
            !nodeData.hasHiddenClass;

        return visible;
    };

    /**
     * Update the list of currently visible nodes (linearly, as they are shown) and return it
     */
    var getVisibleNodesList = function(){
        var visibleNodes = [];

        iterateOverTree(function(nodeData, parentData){
            var visible = _isNodeVisible(nodeData, parentData);

            if (visible){
                visibleNodes.push(nodeData.id);
            }

            return visible;
        });

        return visibleNodes;
    };

    /**
     * Get the width of the tree by computing offset and length of the deepest items
     */
    var getTreeWidth = function(){
        var maxDepth = 0;
        var maxWidth = 0;
        var nodes = [];

        $.each(getVisibleNodesList(), function(index, id){
            var depth = getNodeData(id).depth;

            if (depth > maxDepth){
                nodes = [id];
                maxDepth = depth;
            } else if (depth === maxDepth){
                nodes.push(id);
            }
        });

        $.each(nodes, function(index, id){
            var width = $("#" + dashedNodesIds[id] + "-name").width();

            if (width > maxWidth){
                maxWidth = width;
            }
        });

        return maxWidth + maxDepth * 120;
    };

    /**
     * Transform packages input to the format digestable by the plugin
     */
    var prepareNodes = function(packages){
        nodes = packages;

        iterateOverTree(function(nodeData, parentData){
            dashedNodesIds[nodeData.id] = "clover-package-node-" + nodeData.id.replace(/\./g, "-");
            nodeData.parent_id = parentData ? parentData.id : null;
            nodesByIds[nodeData.id] = nodeData;

            nodeData.state = {
                open: true
            };

            nodeData.depth = parentData ? parentData.depth + 1 : 0;
        });
    };

    /**
     * Create a DOM representation of a single node and return it
     */
    var renderNode = function(nodeData, options){
        var $node = $('<li class="clover-tree-package-node"></li>');

        // the magical values below should be keep in sync with css rules
        var left = (nodeData.depth * -28) - 16;
        var $row = $("<div></div>")
            .addClass("clover-tree-package-node-row")
            .data("default-left", left)
            .css("left", left+"px");

        $node.append($row);
        $node.attr("id", dashedNodesIds[nodeData.id]);
        $node.data("node-id", nodeData.id);

        if (nodeData.children && nodeData.children.length){
            $node.append(
                $("<span></span>")
                    .addClass(classes.arrow.collapsed)
                    .attr("id", dashedNodesIds[nodeData.id]+"-arrow")
            );
        }

        $node.append(
            $("<span></span>")
                .addClass(classes.icon.closed)
                .attr("id", dashedNodesIds[nodeData.id]+"-icon")
        );

        if (nodeData.a_attr.href){
            $node.append(
                $("<a></a>")
                    .addClass("clover-tree-package-link")
                    .addClass(nodeData.id === options.currentPackage ? "clover-tree-current-package" : "")
                    .attr("href", options.urlPrefix + nodeData.a_attr.href)
                    .attr("id", dashedNodesIds[nodeData.id]+"-name")
                    .html(nodeData.text)
            );
        } else {
            $node.append(
                $("<span></span>")
                    .addClass("clover-tree-package-name")
                    .attr("id", dashedNodesIds[nodeData.id]+"-name")
                    .html(nodeData.text)
            );
        }

        if (nodeData.children.length){
            var $ul = $('<ul></ul>');
            $ul.addClass(classes.list.hidden);
            $ul.attr("id", dashedNodesIds[nodeData.id]+"-list")

            $.each(nodeData.children, function(index, nodeData2){
                $ul.append(renderNode(nodeData2, options));
            });

            $node.append($ul);
        }

        nodeData.state.open = false;

        return $node;
    };

    /**
     * Render the entire tree into the given placeholder
     */
    var render = function(options){
        var $tree = $('<ul class="clover-tree-packages-list clover-tree-root-list"></ul>');

        $.each(nodes, function(index, nodeData){
            $tree.append(renderNode(nodeData, options));
        });

        options.placeholder.html($tree);

        renderLozenges(options);
    };

    /**
     * Render the vertical strip containing lozenges
     */
    var renderLozenges = function(options){
        var visible = getVisibleNodesList();

        $(options.lozengesPlaceholder).html("");

        $.each(visible, function(index, nodeId){
            var nodeData = getNodeData(nodeId);
            var $span = $("<span></span>")
                .addClass("clover-package-node-lozenge-placeholder");

            if (nodeData.coverage){
                $span.append(
                    $("<span></span>")
                        .addClass("aui-lozenge aui-lozenge-subtle clover-package-node-lozenge")
                        .html(nodeData.coverage)
                );
            }

            $(options.lozengesPlaceholder).append($span);
        });

        // adjust right padding of list item so that visually there's always the same margin
        // between list items and lozenges
        var width = $(options.lozengesPlaceholder).outerWidth();
        $(".clover-tree-package-link").css("margin-right", width+8+"px");
    };

    /**
     * Toggle state of the node with given id
     */
    var toggleOpen = function(nodeId, options){
        var nodeData = getNodeData(nodeId);

        if (nodeData.state.open){
            closeNode(nodeId);
        } else {
            openNode(nodeId);
        }

        renderLozenges(options);
        _updateNodeRowsWidth(options);
    };

    /**
     * Return the data of node with given id
     */
    var getNodeData = function(nodeId){
        return nodesByIds[nodeId];
    };

    /**
     * Return the DOM representation of the node with given id
     */
    var getNodeDOMElement = function(nodeId){
        return $("#" + dashedNodesIds[nodeId]);
    };

    /**
     * Open node with given id
     */
    var openNode = function(nodeId){
        var nodeData = getNodeData(nodeId);

        if (!nodeData || nodeData.state.open) return;

        if (!nodeData.children || !nodeData.children.length){
            return;
        }

        var selector = "#" + dashedNodesIds[nodeId] + "-";
        var arrow = $(selector + "arrow");
        var list = $(selector + "list");

        if (arrow.length){
            arrow[0].className = classes.arrow.expanded;
        }

        if (list.length){
            list[0].className = classes.list.visible;
        }

        $(selector + "icon")[0].className = classes.icon.open;

        nodeData.state.open = true;
    };

    /**
     * Close node with given id
     */
    var closeNode = function(nodeId){
        var nodeData = getNodeData(nodeId);

        if (!nodeData || !nodeData.state.open) return;

        if (!nodeData.children || !nodeData.children.length){
            return;
        }

        var selector = "#" + dashedNodesIds[nodeId] + "-";
        var arrow = $(selector + "arrow");
        var list = $(selector + "list");

        if (arrow.length){
            arrow[0].className = classes.arrow.collapsed;
        }

        if (list.length){
            list[0].className = classes.list.hidden;
        }

        $(selector + "icon")[0].className = classes.icon.closed;

        nodeData.state.open = false;
    };

    /**
     * Close all the nodes by iteratively closing each single one
     */
    var closeAllNodes = function(){
        iterateOverTree(function(nodeData){
            closeNode(nodeData.id);
        });
    };

    /**
     * Open all the nodes by iteratively opening each single one
     */
    var openAllNodes = function(){
        iterateOverTree(function(nodeData){
            openNode(nodeData.id);
        });
    };

    /**
     * Open all the parent nodes of the one with given id; if the second
     * parameter is set to true, open the innermost node too
     */
    var openNodesTo = function(nodeId, openInnermost){
        var nodeData = getNodeData(nodeId);

        if (!nodeData) return;

        var parentId = nodeData.parent_id;
        var openInnermost = !!openInnermost;

        if (openInnermost){
            openNode(nodeId);
        }

        while (parentId !== null){
            openNode(parentId);
            parentId = getNodeData(parentId).parent_id;
        }
    };

    /**
     * Save the tree instance options by attaching it to the placeholder
     */
    var saveTreeOptions = function(options){
        options.placeholder.data("clover-packages-options", options);
    };

    /**
     * Get the tree instance options for given placeholder
     */
    var getTreeOptions = function(placeholder){
        return placeholder.data("clover-packages-options");
    };

    /**
     * Unhide nodes that got hidden for the purpose of clean search results
     */
    var unhideNodes = function(){
        $(".clover-package-node-hidden").each(function(index, elem){
            var id = $(elem).data("node-id");
            getNodeData(id).hasHiddenClass = false;

            $(elem).removeClass("clover-package-node-hidden");
        });
    };

    /**
     * Compute search result
     */
    var computeSearchResult = function(query){
        var hasDot = query.indexOf(".") >= 0;
        var hidden = [], results = [], total = 0;

        // check if id1 is (direct or indirect) parent of id2
        var isParentOf = function(id1, id2){
            return id2.indexOf(id1 + ".") === 0;
        };

        var isSiblingOf = function(id1, id2){
            var pos1 = id1.lastIndexOf(".");
            var pos2 = id2.lastIndexOf(".");

            return id1.substr(0, pos1) === id2.substr(0, pos2);
        };

        var collectResult = function(id){
            var childrenAlready = false;
            var siblingAlready = false;

            Object.keys(results).forEach(function(elem){
                if (!childrenAlready && !siblingAlready && isParentOf(id, elem)){
                    childrenAlready = true;
                    total++;
                }

                if (!childrenAlready && !siblingAlready && isSiblingOf(id, elem)){
                    siblingAlready = true;
                    total++;
                }
            });

            if (!childrenAlready && !siblingAlready){
                results.push(id);
                total++;
            }
        };

        var func = function(nodes){
            var foundInChildren = false;
            var foundInNodes = false;

            $.each(nodes, function(index, nodeData){
                var foundInSingleNode = false;

                if (!hasDot && nodeData.text.indexOf(query) !== -1){
                    foundInSingleNode = true;
                } else if (hasDot && nodeData.id.indexOf(query) !== -1){
                    foundInSingleNode = true;
                }

                if (nodeData.children){
                    foundInChildren = func(nodeData.children)
                }

                foundInNodes = foundInNodes || foundInSingleNode || foundInChildren;

                if (!foundInSingleNode && !foundInChildren){
                    hidden.push(nodeData.id);
                } else if (foundInSingleNode){
                    collectResult(nodeData.id);
                }
            });

            return foundInNodes;
        };
        func(nodes);

        return {
            hidden: hidden,
            results: results,
            total: total
        };
    };

    /**
     * Perform a search on the tree, including synchronisation with previous searches
     * and UI changes
     */
    var search = function(options){
        closeAllNodes();

        if (!options.search.length){
            $(options.noResultsMessage).hide();
            ui.rootList.show();
            unhideNodes();
            openNodesTo(options.currentPackage);
            renderLozenges(options);

            return;
        }

        var results = computeSearchResult(options.search);

        if (results.total){
            $(options.noResultsMessage).hide();
            ui.rootList.show();

            $.each(results.results, function(index, id){
                openNodesTo(id);
            });

            unhideNodes();
            var visible = getVisibleNodesList();

            $.each(results.hidden, function(index, id){
                if (visible.indexOf(id) !== -1){
                    getNodeData(id).hasHiddenClass = true;
                    getNodeDOMElement(id).addClass("clover-package-node-hidden");
                }
            });
        } else {
            $(options.noResultsMessage).show();
            ui.rootList.hide();
        }

        renderLozenges(options);
    };

    /**
     * Helper function used to remove selection made by keyboard, intended to be used only internally.
     */
    var _unhoverAllKeyboardSelections = function(){
        $(".keyboard-hovered").removeClass("keyboard-hovered");
    };

    /**
     * Handle keyboard to operate on tree.
     */
    var handleKeyDown = function(options){
        var key = options.keyDown;
        var special = [13, 37, 38, 39, 40];
        var visible = getVisibleNodesList();

        if (special.indexOf(key) === -1){
            return;
        }

        var selectNode = function(nodeId, select){
            var select = select === undefined ? true : select;

            if (nodeId){
                getNodeDOMElement(nodeId)[select ? "addClass" : "removeClass"]("keyboard-hovered");
            }
        };

        var previouslySelected = keyboardSelectedId;
        if (visible.indexOf(keyboardSelectedId) === -1){
            _unhoverAllKeyboardSelections();
            keyboardSelectedId = undefined;
            keyboardSelectedIndex = undefined;
        }

        if (key === 38){ // up
            if (keyboardSelectedIndex === undefined){
                keyboardSelectedIndex = visible.length - 1;
            } else {
                keyboardSelectedIndex--;
            }

            if (keyboardSelectedIndex < 0){
                keyboardSelectedIndex = visible.length - 1;
            }

            keyboardSelectedId = visible[keyboardSelectedIndex];
            selectNode(previouslySelected, false);
            selectNode(keyboardSelectedId);
        } else if (key === 40){ // down
            if (keyboardSelectedIndex === undefined){
                keyboardSelectedIndex = 0;
            } else {
                keyboardSelectedIndex++;
            }

            if (keyboardSelectedIndex >= visible.length){
                keyboardSelectedIndex = 0;
            }

            keyboardSelectedId = visible[keyboardSelectedIndex];
            selectNode(previouslySelected, false);
            selectNode(keyboardSelectedId);
        } else if (key === 39){ // right
            openNode(keyboardSelectedId);
            renderLozenges(options);
        } else if (key === 37){ // left
            closeNode(keyboardSelectedId);
            renderLozenges(options);
        } else if (key === 13){ // enter
            var $link = getNodeDOMElement(keyboardSelectedId).children(".clover-tree-package-link");

            if ($link.length){
                window.location = $link.attr("href");
            }
        }
    };

    /**
     * Reset selection made using keyboard
     */
    var resetKeyboardSelection = function(options){
        _unhoverAllKeyboardSelections();

        keyboardSelectedId = undefined;
        keyboardSelectedIndex = undefined;
    };

    /**
     * The jQuery plugin itself
     */
    $.fn.cloverPackages = function(options){
        var options = $.extend({
            placeholder: this
        }, getTreeOptions(this), options);

        if (typeof options.search !== "undefined"){
            search(options);

            return this;
        }

        if (typeof options.keyDown !== "undefined"){
            handleKeyDown(options);

            return this;
        }

        if (options.resetKeyboardSelection === true){
            resetKeyboardSelection(options);

            return this;
        }

        if (!options.packages){
            throw new Error("CloverPackages requires packages option!");
        }

        saveTreeOptions(options);
        prepareNodes(options.packages);
        render(options);
        bindUIElements(options);
        bindEvents(options);

        openNodesTo(options.currentPackage, true);

        renderLozenges(options);

        return this;
    };

})(jQuery);
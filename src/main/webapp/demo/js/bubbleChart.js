function createBubbleChart(divId){
jq.getScript('js/lib/d3.v3.min.js', function() {
var json = {
		    "name": "flare",
		    "children": [
		        {
		            "name": "analytics",
		            "children": [
		                {
		                    "name": "cluster",
		                    "children": [
		                        {
		                            "name": "AgglomerativeCluster",
		                            "size": 3938
		                        },
		                        {
		                            "name": "CommunityStructure",
		                            "size": 3812
		                        },
		                        {
		                            "name": "HierarchicalCluster",
		                            "size": 6714
		                        },
		                        {
		                            "name": "MergeEdge",
		                            "size": 743
		                        }
		                    ]
		                },
		                {
		                    "name": "optimization",
		                    "children": [
		                        {
		                            "name": "AspectRatioBanker",
		                            "size": 7074
		                        }
		                    ]
		                }
		            ]
		        },
		        {
		            "name": "vis",
		            "children": [
		                {
		                    "name": "axis",
		                    "children": [
		                        {
		                            "name": "Axes",
		                            "size": 1302
		                        },
		                        {
		                            "name": "Axis",
		                            "size": 24593
		                        },
		                        {
		                            "name": "AxisGridLine",
		                            "size": 652
		                        },
		                        {
		                            "name": "AxisLabel",
		                            "size": 636
		                        },
		                        {
		                            "name": "CartesianAxes",
		                            "size": 6703
		                        }
		                    ]
		                },
		                {
		                    "name": "controls",
		                    "children": [
		                        {
		                            "name": "AnchorControl",
		                            "size": 2138
		                        },
		                        {
		                            "name": "ClickControl",
		                            "size": 3824
		                        },
		                        {
		                            "name": "Control",
		                            "size": 1353
		                        },
		                        {
		                            "name": "ControlList",
		                            "size": 4665
		                        },
		                        {
		                            "name": "DragControl",
		                            "size": 2649
		                        },
		                        {
		                            "name": "ExpandControl",
		                            "size": 2832
		                        },
		                        {
		                            "name": "HoverControl",
		                            "size": 4896
		                        },
		                        {
		                            "name": "IControl",
		                            "size": 763
		                        },
		                        {
		                            "name": "PanZoomControl",
		                            "size": 5222
		                        },
		                        {
		                            "name": "SelectionControl",
		                            "size": 7862
		                        },
		                        {
		                            "name": "TooltipControl",
		                            "size": 8435
		                        }
		                    ]
		                },
		                {
		                    "name": "data",
		                    "children": [
		                        {
		                            "name": "Data",
		                            "size": 20544
		                        },
		                        {
		                            "name": "DataList",
		                            "size": 19788
		                        },
		                        {
		                            "name": "DataSprite",
		                            "size": 10349
		                        },
		                        {
		                            "name": "EdgeSprite",
		                            "size": 3301
		                        },
		                        {
		                            "name": "NodeSprite",
		                            "size": 19382
		                        },
		                        {
		                            "name": "render",
		                            "children": [
		                                {
		                                    "name": "ArrowType",
		                                    "size": 698
		                                },
		                                {
		                                    "name": "EdgeRenderer",
		                                    "size": 5569
		                                },
		                                {
		                                    "name": "IRenderer",
		                                    "size": 353
		                                },
		                                {
		                                    "name": "ShapeRenderer",
		                                    "size": 2247
		                                }
		                            ]
		                        },
		                        {
		                            "name": "ScaleBinding",
		                            "size": 11275
		                        },
		                        {
		                            "name": "Tree",
		                            "size": 7147
		                        },
		                        {
		                            "name": "TreeBuilder",
		                            "size": 9930
		                        }
		                    ]
		                },
		                {
		                    "name": "events",
		                    "children": [
		                        {
		                            "name": "DataEvent",
		                            "size": 2313
		                        },
		                        {
		                            "name": "SelectionEvent",
		                            "size": 1880
		                        },
		                        {
		                            "name": "TooltipEvent",
		                            "size": 1701
		                        },
		                        {
		                            "name": "VisualizationEvent",
		                            "size": 1117
		                        }
		                    ]
		                },
		                {
		                    "name": "legend",
		                    "children": [
		                        {
		                            "name": "Legend",
		                            "size": 20859
		                        },
		                        {
		                            "name": "LegendItem",
		                            "size": 4614
		                        },
		                        {
		                            "name": "LegendRange",
		                            "size": 10530
		                        }
		                    ]
		                },
		                {
		                    "name": "operator",
		                    "children": [
		                        {
		                            "name": "distortion",
		                            "children": [
		                                {
		                                    "name": "BifocalDistortion",
		                                    "size": 4461
		                                },
		                                {
		                                    "name": "Distortion",
		                                    "size": 6314
		                                },
		                                {
		                                    "name": "FisheyeDistortion",
		                                    "size": 3444
		                                }
		                            ]
		                        },
		                        {
		                            "name": "encoder",
		                            "children": [
		                                {
		                                    "name": "ColorEncoder",
		                                    "size": 3179
		                                },
		                                {
		                                    "name": "Encoder",
		                                    "size": 4060
		                                },
		                                {
		                                    "name": "PropertyEncoder",
		                                    "size": 4138
		                                },
		                                {
		                                    "name": "ShapeEncoder",
		                                    "size": 1690
		                                },
		                                {
		                                    "name": "SizeEncoder",
		                                    "size": 1830
		                                }
		                            ]
		                        },
		                        {
		                            "name": "filter",
		                            "children": [
		                                {
		                                    "name": "FisheyeTreeFilter",
		                                    "size": 5219
		                                },
		                                {
		                                    "name": "GraphDistanceFilter",
		                                    "size": 3165
		                                },
		                                {
		                                    "name": "VisibilityFilter",
		                                    "size": 3509
		                                }
		                            ]
		                        },
		                        {
		                            "name": "IOperator",
		                            "size": 1286
		                        },
		                        {
		                            "name": "label",
		                            "children": [
		                                {
		                                    "name": "Labeler",
		                                    "size": 9956
		                                },
		                                {
		                                    "name": "RadialLabeler",
		                                    "size": 3899
		                                },
		                                {
		                                    "name": "StackedAreaLabeler",
		                                    "size": 3202
		                                }
		                            ]
		                        },
		                        {
		                            "name": "layout",
		                            "children": [
		                                {
		                                    "name": "AxisLayout",
		                                    "size": 6725
		                                },
		                                {
		                                    "name": "BundledEdgeRouter",
		                                    "size": 3727
		                                },
		                                {
		                                    "name": "CircleLayout",
		                                    "size": 9317
		                                },
		                                {
		                                    "name": "CirclePackingLayout",
		                                    "size": 12003
		                                },
		                                {
		                                    "name": "DendrogramLayout",
		                                    "size": 4853
		                                },
		                                {
		                                    "name": "ForceDirectedLayout",
		                                    "size": 8411
		                                },
		                                {
		                                    "name": "IcicleTreeLayout",
		                                    "size": 4864
		                                },
		                                {
		                                    "name": "IndentedTreeLayout",
		                                    "size": 3174
		                                },
		                                {
		                                    "name": "Layout",
		                                    "size": 7881
		                                },
		                                {
		                                    "name": "NodeLinkTreeLayout",
		                                    "size": 12870
		                                },
		                                {
		                                    "name": "PieLayout",
		                                    "size": 2728
		                                },
		                                {
		                                    "name": "RadialTreeLayout",
		                                    "size": 12348
		                                },
		                                {
		                                    "name": "RandomLayout",
		                                    "size": 870
		                                },
		                                {
		                                    "name": "StackedAreaLayout",
		                                    "size": 9121
		                                },
		                                {
		                                    "name": "TreeMapLayout",
		                                    "size": 9191
		                                }
		                            ]
		                        },
		                        {
		                            "name": "Operator",
		                            "size": 2490
		                        },
		                        {
		                            "name": "OperatorList",
		                            "size": 5248
		                        },
		                        {
		                            "name": "OperatorSequence",
		                            "size": 4190
		                        },
		                        {
		                            "name": "OperatorSwitch",
		                            "size": 2581
		                        },
		                        {
		                            "name": "SortOperator",
		                            "size": 2023
		                        }
		                    ]
		                },
		                {
		                    "name": "Visualization",
		                    "size": 16540
		                }
		            ]
		        }
		    ]
		};


		var divElement = jq('$'+divId).empty();
		divElement.append(jq("<div id='chartHolder'/>" ));		
		var divToDraw = d3.select(divElement.get(0)).select("div");	
		
		var height = divElement.height();
		var width = divElement.width();
		
		if(width < 50 ){ width = 400; }
		if(height < 50 ){ height = 385; }

		 var fill = d3.scale.ordinal()
         .domain(d3.range(4))
         .range(["#000000", "#FFDD89", "#957244", "#F26223"]);
		 
		var r = 400,
		    format = d3.format(",d"),
		    fill = d3.scale.category20c();

		var bubble = d3.layout.pack()
		    .sort(null)
		    .size([r, r])
		    .padding(1.5);
		var vis = divToDraw.append("svg")
		    .attr("width", width)
		    .attr("height", height)
		    .attr("class", "bubble");


		  var node = vis.selectAll("g.node")
		      .data(bubble.nodes(classes(json))
		      .filter(function(d) { return !d.children; }))
		    .enter().append("g")
		      .attr("class", "node")
		      .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });

		  node.append("title")
		      .text(function(d) { return d.className + ": " + format(d.value); });

		  node.append("circle")
		      .attr("r", function(d) { return d.r; })
		      .style("fill", function(d) { return fill(d.packageName); });

		  node.append("text")
		      .attr("text-anchor", "middle")
		      .attr("dy", ".3em")
		      .text(function(d) { return d.className.substring(0, d.r / 3); });

		// Returns a flattened hierarchy containing all leaf nodes under the root.
		function classes(root) {
		  var classes = [];

		  function recurse(name, node) {
		    if (node.children) node.children.forEach(function(child) { recurse(node.name, child); });
		    else classes.push({packageName: name, className: node.name, value: node.size});
		  }

		  recurse(null, root);
		  return {children: classes};
		}
	});
}	

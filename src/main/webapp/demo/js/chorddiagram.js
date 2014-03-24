function drawChordDiagram(divId, chartData) {
	jq.getScript('js/lib/d3.v3.min.js', function() {
		var chordData = jq.parseJSON(chartData);
		console.log(chordData);		
		
		var divElement = jq('$'+divId).empty();
		divElement.append(jq("<div id='chartHolder'/>" ));		
		var divToDraw = d3.select(divElement.get(0)).select("div");	
		
		var matrix = chordData.matrix;
		console.log(matrix);	
		            var chord = d3.layout.chord()
		                .padding(.05)
		                .sortSubgroups(d3.descending)
		                .matrix(matrix);

		var width = divElement.width();
		var height = divElement.height();
		
		if(width < 50 ){ width = 400; } 
		if(height < 50 ){ height = 385; }
		                innerRadius = Math.min(width, height) * .41,
		                outerRadius = innerRadius * 1.1;

		 var fill = d3.scale.ordinal()
		                .domain(d3.range(4))
		                .range(["#000000", "#FFDD89", "#957244", "#F26223"]);

		 var svg = divToDraw.append("svg")
		                .attr("width", width)
		                .attr("height", height)
		              .append("g")
		                .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

		            svg.append("g").selectAll("path")
		                .data(chord.groups)
		              .enter().append("path")
		                .style("fill", function(d) { return fill(d.index); })
		                .style("stroke", function(d) { return fill(d.index); })
		                .attr("d", d3.svg.arc().innerRadius(innerRadius).outerRadius(outerRadius))
		                .on("mouseover", fade(.1))
		                .on("mouseout", fade(1));

		 var ticks = svg.append("g").selectAll("g")
		                .data(chord.groups)
		              .enter().append("g").selectAll("g")
		                .data(groupTicks)
		              .enter().append("g")
		                .attr("transform", function(d) {
		                  return "rotate(" + (d.angle * 180 / Math.PI - 90) + ")"
		                      + "translate(" + outerRadius + ",0)";
		                });

		            ticks.append("line")
		                .attr("x1", 1)
		                .attr("y1", 0)
		                .attr("x2", 5)
		                .attr("y2", 0)
		                .style("stroke", "#000");

		            ticks.append("text")
		                .attr("x", 8)
		                .attr("dy", ".35em")
		                .attr("transform", function(d) { return d.angle > Math.PI ? "rotate(180)translate(-16)" : null; })
		                .style("text-anchor", function(d) { return d.angle > Math.PI ? "end" : null; })
		                .text(function(d) { return d.label; });

		            svg.append("g")
		                .attr("class", "chord")
		              .selectAll("path")
		                .data(chord.chords)
		              .enter().append("path")
		                .attr("d", d3.svg.chord().radius(innerRadius))
		                .style("fill", function(d) { return fill(d.target.index); })
		                .style("opacity", 1);

		            // Returns an array of tick angles and labels, given a group.
		            function groupTicks(d) {
		              var k = (d.endAngle - d.startAngle) / d.value;
		              return d3.range(0, d.value, 1000).map(function(v, i) {
		                return {
		                  angle: v * k + d.startAngle,
		                  label: i % 5 ? null : v / 1000 + "k"
		                };
		              });
		            }

		            // Returns an event handler for fading a given chord group.
		            function fade(opacity) {
		              return function(g, i) {
		                svg.selectAll(".chord path")
		                    .filter(function(d) { return d.source.index != i && d.target.index != i; })
		                  .transition()
		                    .style("opacity", opacity);
		              };
		            }
	});
	
}
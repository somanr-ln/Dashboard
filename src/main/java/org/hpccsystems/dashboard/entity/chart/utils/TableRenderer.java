package org.hpccsystems.dashboard.entity.chart.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.Attribute;
import org.hpccsystems.dashboard.entity.chart.Filter;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.services.HPCCService;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Span;
import org.zkoss.zul.Vbox;

/**
 * TableRenderer class is used to construct Table Widget.
 * 
 */
public class TableRenderer {

	private static final Log LOG = LogFactory.getLog(TableRenderer.class);

	FileConverter fileConverter = new FileConverter();

	/**
	 * Constructs a table widget
	 * 
	 * @param tableDataMap
	 *            Data to draw table
	 * @param isEditing
	 *            Specify weather table is to be rendered in the edit widget
	 *            screen. Used to compute Height of the rendered table
	 * @return Table as Listbox
	 */
	public Vbox constructTableWidget(final Portlet portlet,
			XYChartData chartData, Boolean isEditing) {

		HPCCService hpccService = (HPCCService) SpringUtil.getBean("hpccService");

		LinkedHashMap<String, List<Attribute>> tableDataMap;
		try {
			tableDataMap = hpccService.fetchTableData(chartData);
		} catch (Exception e) {
			Clients.showNotification("Unexpected error. Could not construct table.", true);
			LOG.error("Table data error", e);
			return null;
		}

		final Listbox listBox = new Listbox();
		listBox.setMold("paging");
		listBox.setSizedByContent(true);
		listBox.setHflex("1");

		if (isEditing) {
			listBox.setHeight("512px"); // .. 542 - 30
		} else {
			listBox.setHeight("355px"); // .. 385 - 30
		}
		listBox.setAutopaging(true);

		Listhead listhead = new Listhead();

		Listheader listheader = null;
		Listcell listcell;
		Listitem listitem;
		List<List<Attribute>> columnList = new ArrayList<List<Attribute>>();
		List<Attribute> displayColumnList = new ArrayList<Attribute>();
		for (Attribute attribute : chartData.getTableColumns()) {
			displayColumnList.add(new Attribute(attribute
					.getDisplayName()));
		}

		for (Map.Entry<String, List<Attribute>> entry : tableDataMap.entrySet()) {
			String columnStr = entry.getKey();
			for (Attribute attribute : chartData.getTableColumns()) {
				if (attribute.getColumnName().equals(columnStr)) {
					if (attribute.getDisplayName() == null) {
						listheader = new Listheader(columnStr);
					} else {
						listheader = new Listheader(attribute.getDisplayName());
					}
					break;
				}
			}
			listheader.setSort("auto");
			listheader.setParent(listhead);
			listhead.setParent(listBox);
			columnList.add(entry.getValue());
		}
		for (int index = 0; index < columnList.get(0).size(); index++) {
			listitem = new Listitem();
			for (List<Attribute> list : columnList) {
				listcell = new Listcell();
				Attribute listCellValue = list.get(index);
				listcell.setLabel(listCellValue.getColumnName());
				listcell.setParent(listitem);
			}
			listitem.setParent(listBox);
		}
		listBox.appendChild(listhead);
		Hbox hbox = new Hbox();
		hbox.setStyle("margin-left: 3px");
		Button button = new Button();
		button.setIconSclass("glyphicon glyphicon-save");
		button.setZclass("btn btn-xs btn-info");
		button.setLabel("Excel");
		button.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event event) throws Exception {
				fileConverter.exportListboxToExcel(listBox, portlet.getName());
			}
		});
		hbox.appendChild(button);
		button = new Button();
		button.setIconSclass("glyphicon glyphicon-save");
		button.setZclass("btn btn-xs btn-info");
		button.setLabel("CSV");
		button.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			public void onEvent(Event event) throws Exception {
				TableRenderer.this.fileConverter.exportListboxToCsv(listBox, portlet.getName());
			}
		});
		hbox.appendChild(button);
		Vbox vbox = new Vbox();
		//Appending Chart Title as Data file name
		final Div div = new Div();			
		div.setStyle("margin-top: 3px; margin-left: 5px; height: 15px;");
		final Label title = new Label();
		title.setWidth("100%");
		title.setValue(chartData.getFileName());
		div.appendChild(title);	
		
		if(chartData.getIsFiltered()){				
			constructFilterTitle(div,chartData);
		}
		vbox.appendChild(div);
		vbox.appendChild(listBox);
		vbox.appendChild(hbox);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Created table widget..");
		}
		return vbox;
	}
	
	/**
	 * Method to construct Filter title for Table widget
	 * @param div
	 * @param chartData
	 * @return Div
	 */		
	private Div constructFilterTitle(Div div, XYChartData chartData){
		
		Span filterSpan = new Span();
		filterSpan.setClass("btn-link btn-sm");
		filterSpan.setStyle("float: right; padding: 0px 10px;");
		filterSpan.appendChild(new Label("Filters"));			
		div.appendChild(filterSpan);		
		
		final Div filterContentDiv = new Div();
		StringBuffer styleBuffer = new StringBuffer();
		styleBuffer
			.append("line-height: initial; position: absolute; padding: 2px;")
			.append(" border: 1px solid rgb(124, 124, 124); margin: 5px; ")
			.append("background-color: rgb(177, 177, 177);")
			.append("font-size: small; color: white; z-index: 2; display: none;");
		filterContentDiv.setStyle(styleBuffer.toString());
		
		StringBuffer filterDescription = new StringBuffer();
		
		filterDescription.append(" WHERE ");
		
		Iterator<Filter> filterIterator = chartData.getFilterSet().iterator(); 
		while (filterIterator.hasNext()) {
			Filter filter = (Filter) filterIterator.next();
			if(LOG.isDebugEnabled()) {
				LOG.debug("Filter -> " + filter);
			}
			
			if(chartData.getIsFiltered() &&
					Constants.STRING_DATA.equals(filter.getType())) {
				filterDescription.append(filter.getColumn());
				filterDescription.append(" IS ");
				
				Iterator<String> iterator = filter.getValues().iterator();
				while(iterator.hasNext()){
					filterDescription.append(iterator.next());
					if(iterator.hasNext()){
						filterDescription.append(", ");
					}
				}

			} else if (chartData.getIsFiltered() && 
					Constants.NUMERIC_DATA.equals(filter.getType())) {
				filterDescription.append(filter.getColumn());
				filterDescription.append(" BETWEEN " + filter.getStartValue());
				filterDescription.append(" & " + filter.getEndValue());
			}
			
			if(filterIterator.hasNext()){
				filterDescription.append(" AND "); 
			}
		}
		filterContentDiv.appendChild(new Label(filterDescription.toString()));
		filterContentDiv.setVisible(false);
		div.appendChild(filterContentDiv);
		//Shows the filter content
		EventListener<Event> showFilterContent = new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				filterContentDiv.setVisible(true);	
			}
		};
		//Hides the filter content
		EventListener<Event> hideFilterContent = new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {
				filterContentDiv.setVisible(false);	
			}
		};
		filterSpan.addEventListener(Events.ON_MOUSE_OVER, showFilterContent);
		filterSpan.addEventListener(Events.ON_MOUSE_OUT, hideFilterContent);
		return div;
	}
}

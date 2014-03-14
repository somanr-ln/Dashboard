package org.hpccsystems.dashboard.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.Filter;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.entity.chart.utils.ChartRenderer;
import org.hpccsystems.dashboard.services.AuthenticationService;
import org.hpccsystems.dashboard.services.HPCCService;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class NumericFilterController extends SelectorComposer<Component>{

	private static final long serialVersionUID = 1L;
	private static final  Log LOG = LogFactory.getLog(NumericFilterController.class);
	
	private Portlet portlet;
	private Filter filter;
	private XYChartData chartData;
	private Button doneButton;
	
	@WireVariable
	ChartRenderer chartRenderer;
	
	@WireVariable
	HPCCService hpccService;
	
	@WireVariable
	AuthenticationService  authenticationService;
	
	@Wire
	Label minimumLabel;
	
	@Wire
	Label maximumLabel;
	
	@Wire
	Div sliderDiv;
	
	@Wire
	Button filtersSelectedBtn;
	
	BigDecimal min;
	BigDecimal max;

	BigDecimal rangeFactor;
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		portlet = (Portlet) Executions.getCurrent().getAttribute(Constants.PORTLET);
		filter = (Filter) Executions.getCurrent().getAttribute(Constants.FILTER);
		chartData = (XYChartData) Executions.getCurrent().getAttribute(Constants.CHART_DATA);
		doneButton = (Button) Executions.getCurrent().getAttribute(Constants.EDIT_WINDOW_DONE_BUTTON);
	
		Map<Integer, BigDecimal> map = null;
		try	{
			if(chartData.getxColumnNames().contains(filter.getColumn()) ||
					chartData.getYColumns().contains(filter.getColumn())){
				map = hpccService.getMinMax(filter.getColumn(), chartData, true);
			} else {
				map = hpccService.getMinMax(filter.getColumn(), chartData, false);
			}
		} catch(Exception e) {
			if(!authenticationService.getUserCredential().getApplicationId().equals(Constants.CIRCUIT_APPLICATION_ID) || 
					authenticationService.getUserCredential().hasRole(Constants.CIRCUIT_ROLE_VIEW_DASHBOARD)){
				Clients.showNotification(Labels.getLabel("unableToFetchDataForFilterColumn"), 
						"error", doneButton.getParent().getParent().getParent(), "middle_center", 3000, true);
			}else{
				Clients.showNotification(Labels.getLabel("unableToFetchColumnData"), true);
			}
			LOG.error("Exception while fetching data from Hpcc for selected Numeric filter", e);
			return;
		}
		
		min = map.get(Constants.FILTER_MINIMUM);
		max = map.get(Constants.FILTER_MAXIMUM);
			
		minimumLabel.setValue(min.toString());
		maximumLabel.setValue(max.toString());
		
		//Intitialising Slider positions
		Integer sliderStart = 0;
		Integer sliderEnd = 100;
		
		//Translating min & max to a scale of 0 to 100 using Linear equation 
		//((actualVal - actualMin)/(actualMax- actualMin)) = ((sliderVal - sliderMin)/(sliderMax- sliderMin))
		// Range Factor = (actualMax- actualMin)/(sliderMax- sliderMin)
		rangeFactor = max.subtract(min).divide(new BigDecimal(100));
		
		if(filter.getStartValue() != null && filter.getEndValue() != null) {
			//Updating slider positions for already applied filters
			sliderStart = filter.getStartValue().subtract(min).divide(rangeFactor).intValue();
			sliderEnd = filter.getEndValue().subtract(min).divide(rangeFactor).intValue();
		} else {
			filter.setStartValue(min);
			filter.setEndValue(max);
		}
		
		StringBuilder html = new StringBuilder();
		html.append("<div id=\"");
			html.append(filter.getColumn());
			html.append("_sdiv\" style=\"margin: 8px;\" class=\"slider-grey\">");
		
		html.append("<script type=\"text/javascript\">");
			html.append("$('#").append(filter.getColumn()).append("_sdiv').slider({")
				.append("range: true,")
				.append("values: [").append(sliderStart).append(", ").append(sliderEnd).append("]")
				.append("});");
	
			html.append("$('#").append(filter.getColumn()).append("_sdiv').on( \"slide\", function( event, ui ) {")
				.append("payload = \"").append(filter.getColumn()).append("_hbox,\" + ui.values;")
				.append("zAu.send(new zk.Event(zk.Widget.$('$")
					.append("numericFilterPopup").append("'), 'onSlide', payload, {toServer:true}));")
				.append("});");
			
			html.append("$('#").append(filter.getColumn()).append("_sdiv').on( \"slidestop\", function( event, ui ) {")
				.append("payload = \"").append(filter.getColumn()).append("_hbox,\" + ui.values;")
				.append("zAu.send(new zk.Event(zk.Widget.$('$")
					.append("numericFilterPopup").append("'), 'onSlideStop', payload, {toServer:true}));")
				.append("});");
		html.append("</script>");
		
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("Generated HTML " + html.toString());
		}
		
		sliderDiv.appendChild(new Html(html.toString()));
	}
	
	@Listen("onSlide = #numericFilterPopup")
	public void onSlide(Event event) {
		String[] data = ((String) event.getData()).split(",");
		
		Integer startPosition = Integer.valueOf(data[1]);
		Integer endPosition = Integer.valueOf(data[2]);
		
		//Converting position into value
		// value = pos . rangeFactor + min  
		minimumLabel.setValue(String.valueOf(rangeFactor.multiply(new BigDecimal(startPosition)).add(min).intValue()));
		maximumLabel.setValue(String.valueOf(rangeFactor.multiply(new BigDecimal(endPosition)).add(min).intValue()));
	}

	@Listen("onSlideStop = #numericFilterPopup")
	public void onSlideStop(Event event) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("On Slide Stop Event - Data -- " + event.getData());
		}
		
		String[] data = ((String) event.getData()).split(",");
		
		Integer startPosition = Integer.valueOf(data[1]);
		Integer endPosition = Integer.valueOf(data[2]);
		
		//Updating Change to filter object
		// value = pos . rangeFactor + min  
		filter.setStartValue(rangeFactor.multiply(new BigDecimal(startPosition)).add(min));
		filter.setEndValue(rangeFactor.multiply(new BigDecimal(endPosition)).add(min));
		
	}
	
	@Listen("onClick = button#filtersSelectedBtn")
	public void onfiltersSelected() {
				
		chartData.setIsFiltered(true);
		if(!chartData.getFilterSet().contains(filter)){
			chartData.getFilterSet().add(filter);
		}
		
		try	{
			chartRenderer.constructChartJSON(chartData, portlet, true);
			chartRenderer.drawChart(chartData, Constants.EDIT_WINDOW_CHART_DIV, portlet);
		} catch(Exception ex) {
			if(!authenticationService.getUserCredential().getApplicationId().equals(Constants.CIRCUIT_APPLICATION_ID) || 
					authenticationService.getUserCredential().hasRole(Constants.CIRCUIT_ROLE_VIEW_DASHBOARD)){
				Clients.showNotification(Labels.getLabel("unableToFetchColumnData"), "error", 
						doneButton.getParent().getParent().getParent(), "middle_center", 3000, true);			
			}else{
				Clients.showNotification(Labels.getLabel("unableToFetchColumnData"), true);
			}
			LOG.error("Exception while fetching column data from Hpcc", ex);
			return;
		}		
		
		if(!authenticationService.getUserCredential().getApplicationId().equals(Constants.CIRCUIT_APPLICATION_ID) || 
				authenticationService.getUserCredential().hasRole(Constants.CIRCUIT_ROLE_VIEW_DASHBOARD)){
			doneButton.setDisabled(false);	
		}

		if(LOG.isDebugEnabled()){
			LOG.debug("Drawn filtered chart with Numeric filter");
		}
	}
}

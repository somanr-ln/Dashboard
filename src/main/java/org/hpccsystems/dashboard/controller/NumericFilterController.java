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
import org.zkoss.zk.ui.event.ScrollEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Slider;

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
	Slider minimumSlider;
	@Wire
	Label minimumLabel;
	
	@Wire
	Slider maximumSlider;
	@Wire
	Label maximumLabel;
	@Wire
	Button filtersSelectedBtn;
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		portlet = (Portlet) Executions.getCurrent().getAttribute(Constants.PORTLET);
		filter = (Filter) Executions.getCurrent().getAttribute(Constants.FILTER);
		chartData = (XYChartData) Executions.getCurrent().getAttribute(Constants.CHART_DATA);
		doneButton = (Button) Executions.getCurrent().getAttribute(Constants.EDIT_WINDOW_DONE_BUTTON);
		
		BigDecimal min;
		BigDecimal max;
		if(filter.getStartValue() != null && filter.getEndValue() != null ) {
			min = new BigDecimal(filter.getStartValue().toString());
			max = new BigDecimal(filter.getEndValue().toString());
		} else {
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
		}
			
		minimumLabel.setValue(min.toString());
		maximumLabel.setValue(max.toString());
		
		//TODO Add minimum positions after upgrading to zk 7.0.1 
		minimumSlider.setMaxpos(max.intValue());
		minimumSlider.setCurpos(min.intValue());
		
		maximumSlider.setMaxpos(max.intValue());
		maximumSlider.setCurpos(max.intValue());
		
		chartData.getFilterSet().add(filter);
	}
	
	@Listen("onScroll = #minimumSlider")
	public void onMinSliderScroll(ScrollEvent event) {
		minimumLabel.setValue(String.valueOf(minimumSlider.getCurpos()));
	}
	
	@Listen("onScroll = #maximumSlider")
	public void onMaxSliderScroll(ScrollEvent event) {
		int currentMinimumPos = minimumSlider.getCurpos();
		
		maximumLabel.setValue(String.valueOf(maximumSlider.getCurpos()));
		
		minimumSlider.setMaxpos(maximumSlider.getCurpos());
		minimumSlider.setCurpos(currentMinimumPos);
		
		if(currentMinimumPos > maximumSlider.getCurpos()){
			minimumSlider.setCurpos(maximumSlider.getCurpos());
			minimumLabel.setValue(String.valueOf(maximumSlider.getCurpos()));
		}
	}
	
	@Listen("onClick = button#filtersSelectedBtn")
	public void onfiltersSelected() {
				
		filter.setStartValue(new BigDecimal(minimumSlider.getCurpos()));
		filter.setEndValue(new BigDecimal(maximumSlider.getCurpos()));
		
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

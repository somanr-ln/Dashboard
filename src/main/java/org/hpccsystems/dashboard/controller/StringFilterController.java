package org.hpccsystems.dashboard.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;


@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class StringFilterController extends SelectorComposer<Component>{

	private static final long serialVersionUID = 1L;
	private static final  Log LOG = LogFactory.getLog(StringFilterController.class);
	
	private XYChartData chartData;
	private Portlet portlet;
	private Filter filter;
	private Button doneButton;
	
	@WireVariable
	ChartRenderer chartRenderer;
	
	@WireVariable
	HPCCService hpccService;
	
	@WireVariable
	AuthenticationService  authenticationService;
	
	@Wire
	Listbox filterListBox;
	@Wire
	Button filtersSelectedBtn;
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		portlet = (Portlet) Executions.getCurrent().getAttribute(Constants.PORTLET);
		chartData = (XYChartData) Executions.getCurrent().getAttribute(Constants.CHART_DATA);
		doneButton =  (Button) Executions.getCurrent().getAttribute(Constants.EDIT_WINDOW_DONE_BUTTON);
		filter = (Filter) Executions.getCurrent().getAttribute(Constants.FILTER);
		
		Listitem listitem;
		Listcell listcell;
		
		List<String> valueList = null;
		try	{
			// If filter column is already used to plot graph, then distinct values are filtered as well
			// String Filters are checked against X Columns only
			if(chartData.getxColumnNames().contains(filter.getColumn())) {
				valueList = hpccService.getDistinctValues(filter.getColumn(), chartData, true);
			} else {
				valueList = hpccService.getDistinctValues(filter.getColumn(), chartData, false);
			}
		} catch(Exception e) {
			Clients.showNotification(Labels.getLabel("unableToFetchFilterColumn"), "error", 
					doneButton.getParent().getParent().getParent(), "top_left", 3000, true);
			LOG.error("Exception while fetching data from Hpcc for selected String filter", e);
		}
		
		List<String> filteredList = null;
		
		if(filter.getValues() != null && valueList != null){
			filteredList = filter.getValues();
			for (String value : valueList) {
				listitem = new Listitem();
				listcell = new Listcell(value);
				listitem.appendChild(listcell);
				
				if(filteredList.contains(value)) {
					listitem.setSelected(true);
				}
				
				filterListBox.appendChild(listitem);
			}
		} else if(valueList != null) {
			// Initializing empty list to avoid null pointers
			filter.setValues(new ArrayList<String>());
			
			for (String value : valueList) {
				listitem = new Listitem();
				listcell = new Listcell(value);
				listitem.appendChild(listcell);
				filterListBox.appendChild(listitem);
			}
		}
		
	}
	
	@Listen("onClick = button#filtersSelectedBtn")
	public void onfiltersSelected() {
		List<String> selectedValues = new ArrayList<String>();
		
		Set<Listitem> selectedSet =  filterListBox.getSelectedItems();
		
		for (Listitem listitem : selectedSet) {
			selectedValues.add(listitem.getLabel());
		}
		
		// Check for no values selected
		if(selectedValues.size() < 1) {
			Clients.showNotification(Labels.getLabel("noFilterareSelected"), "error", 
					doneButton.getParent().getParent().getParent(), "middle_center", 3000, true);
			return;
		}
		
		filter.setValues(selectedValues);

		chartData.setIsFiltered(true);
		if(!chartData.getFilterSet().contains(filter)){
			chartData.getFilterSet().add(filter);
		
		}
		try {
			chartRenderer.constructChartJSON(chartData, portlet, true);
			chartRenderer.drawChart(chartData, Constants.EDIT_WINDOW_CHART_DIV, portlet);
		} catch(Exception ex) {
			Clients.showNotification(Labels.getLabel("unableToFetchColumnData"), "error", 
					doneButton.getParent().getParent().getParent(), "middle_center", 3000, true);
			LOG.error("Exception while fetching column data from Hpcc", ex);
			return;
		}
		if(!authenticationService.getUserCredential().getApplicationId().equals(Constants.CIRCUIT_APPLICATION_ID) || 
				authenticationService.getUserCredential().hasRole(Constants.CIRCUIT_ROLE_VIEW_DASHBOARD)){
			doneButton.setDisabled(false);
		}
		
		if(LOG.isDebugEnabled()){
			LOG.debug("Drawn filtered chart");
		}
		
	}

}

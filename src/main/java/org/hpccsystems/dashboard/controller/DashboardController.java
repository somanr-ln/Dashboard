package org.hpccsystems.dashboard.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.api.entity.Field;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.controller.component.ChartPanel;
import org.hpccsystems.dashboard.entity.Dashboard;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.Filter;
import org.hpccsystems.dashboard.entity.chart.TreeData;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.entity.chart.utils.ChartRenderer;
import org.hpccsystems.dashboard.services.AuthenticationService;
import org.hpccsystems.dashboard.services.DashboardService;
import org.hpccsystems.dashboard.services.HPCCService;
import org.hpccsystems.dashboard.services.WidgetService;
import org.hpccsystems.dashboard.util.DashboardUtil;
import org.springframework.dao.DataAccessException;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkmax.ui.event.PortalMoveEvent;
import org.zkoss.zkmax.zul.Navbar;
import org.zkoss.zkmax.zul.Navitem;
import org.zkoss.zkmax.zul.Portalchildren;
import org.zkoss.zkmax.zul.Portallayout;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Anchorchildren;
import org.zkoss.zul.Anchorlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

/**
 * DashboardController class is used to add new dashboard into sidebar and 
 *  controller class for dashboard.zul.
 *
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class DashboardController extends SelectorComposer<Window>{

	private static final long serialVersionUID = 1L;
	private static final  Log LOG = LogFactory.getLog(DashboardController.class); 
	
	private static String MINIMUM_VALUE = "minVal";
	private static String RANGE_FACTOR = "rangeFactor";
	
	private Dashboard dashboard; 
	private Integer oldColumnCount = null;
	
	Integer dashboardId = null;

	@Wire
	Label nameLabel;
	
    @Wire
    Window dashboardWin;
    
    @Wire
    Toolbar dashboardToolbar;
        
    @Wire("portallayout")
	Portallayout portalLayout;
    
	@Wire("portalchildren")
    List<Portalchildren> portalChildren;
	
	@Wire
	Panel commonFiltersPanel;
	@Wire
	Rows filterRows;
	
	@Wire
	Listbox commonFilterList;
	
    Integer panelCount = 0;
    Set<Filter> appliedCommonFilterSet;
    Set<Field> commonFilterFieldSet;
    
    private static final String PERCENTAGE_SIGN = "%";
    
    @WireVariable
    private AuthenticationService authenticationService;
    
    @WireVariable
	private DashboardService dashboardService;
    
    @WireVariable
   	private WidgetService widgetService;
    
    @WireVariable
	private ChartRenderer chartRenderer;
    
    @WireVariable
	HPCCService hpccService;
    
	@Override
	public void doAfterCompose(Window comp) throws Exception {
		super.doAfterCompose(comp);
		
		dashboardId =(Integer) Executions.getCurrent().getAttribute(Constants.ACTIVE_DASHBOARD_ID);
		
		//For the first Dashboard, getting Id from Session
		if(dashboardId == null ){
			dashboardId = (Integer) Sessions.getCurrent().getAttribute(Constants.ACTIVE_DASHBOARD_ID);
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("Dashboard ID - " + dashboardId);
		}
		//removing the previous/existing commonfilterset from session 
		Sessions.getCurrent().removeAttribute(Constants.COMMON_FILTERS);
		
		if(dashboardId != null ){
			List<String> dashboardIdList = new ArrayList<String>(); 
			dashboardIdList.add(String.valueOf(dashboardId));
			List<Dashboard> dashboardList =null;
			try {
				dashboardList = dashboardService.retrieveDashboardMenuPages(
						authenticationService.getUserCredential().getApplicationId(),
						authenticationService.getUserCredential().getUserId(),
						dashboardIdList,null);				
			} catch(Exception ex) {
				Clients.showNotification(
						Labels.getLabel("unabletoRetrieveDB"),
						"error", comp, "middle_center", 3000, true);
				LOG.error("Exception while fetching widget details from DB", ex);
			}			
			
			if(dashboardList != null && dashboardList.size() > 0){
				dashboard = dashboardList.get(0);
				dashboard.setPersisted(true);
			}
			if(LOG.isDebugEnabled()){
				LOG.debug("dashboardList in DashboardController.doAfterCompose()-->"+dashboardList);
				LOG.debug("Creating dashboard - Dashboard Id " + dashboardId);
			}
			nameLabel.setValue(dashboard.getName());
			
			//Preparing the layout
			Integer count = 0;
			for (Portalchildren portalchildren : portalChildren) {
				if( count < dashboard.getColumnCount()) {
					portalchildren.setVisible(true);
					portalchildren.setWidth(100/dashboard.getColumnCount() + PERCENTAGE_SIGN);
				}
				count ++;
			}		

			try	{
				dashboard.setPortletList((ArrayList<Portlet>) widgetService.retriveWidgetDetails(dashboardId));
			} catch(DataAccessException ex) {
				Clients.showNotification(
						Labels.getLabel("unableToRetrieveWidget"),
						"error", comp, "middle_center", 3000, true);
				LOG.error("Exception while fetching widget details from DB", ex);
			}
			
			if(LOG.isDebugEnabled()){
				LOG.debug("PortletList of selected Dashboard -->"+dashboard.getPortletList());
			}
			
			XYChartData chartData = null;
			TreeData treeData = null;
			ChartPanel panel = null;
			for (Portlet portlet : dashboard.getPortletList()) {
				//Constructing chart data only when live chart is drawn
				if(Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())){
					//For Pie/Line/Bar charts
					if(!Constants.TREE_LAYOUT.equals(portlet.getChartType())){
						chartData = chartRenderer.parseXML(portlet.getChartDataXML());
						
						//Getting fields
						List<Field> fields = new ArrayList<Field>();
						fields.addAll(hpccService.getColumnSchema(chartData.getFileName(), chartData.getHpccConnection()));
						chartData.setFields(fields);
						
						portlet.setChartData(chartData);
						if(! portlet.getChartType().equals(Constants.TABLE_WIDGET)){
							//For chart widgets
							try	{
								chartRenderer.constructChartJSON(chartData, portlet, false);
							}catch(Exception ex) {
								Clients.showNotification(Labels.getLabel("unableToFetchColumnData"), 
										"error", comp, "middle_center", 3000, true);
								LOG.error("Exception while fetching column data from Hpcc", ex);
							}					
							
						}
						//Checking for Common filters
						if(chartData.getIsFiltered()){
							for (Filter filter : chartData.getFilterSet()) {
								if(filter.getIsCommonFilter())
									dashboard.setShowFiltersPanel(true);
							}
						}
					}else{
						//For Tree Layout
						treeData = chartRenderer.parseXMLToTreeData(portlet.getChartDataXML());
						portlet.setTreeData(treeData);
						String[] rootArray = treeData.getRootKey().split("\\s+");
						portlet.setChartDataJSON(chartRenderer.constructTreeJSON(
								rootArray[0], rootArray[1], treeData.getHpccConnection()));
					}
				}
				
				panel = new ChartPanel(portlet);
				portalChildren.get(portlet.getColumn()).appendChild(panel);
				//creating search textbox for tree layout
				if(Constants.TREE_LAYOUT.equals(portlet.getChartType())){
					panel.constructTreeSearchDiv();
				}
									
				if(panel.drawD3Graph() != null){
					Clients.evalJavaScript(panel.drawD3Graph());
				}
			}
			
			if(! authenticationService.getUserCredential().getApplicationId().equals(Constants.CIRCUIT_APPLICATION_ID)) {
				dashboardToolbar.setVisible(true);
			}
			
		} else {
			dashboardWin.setBorder("none");			
			return;
		}
		
		dashboardWin.addEventListener("onPortalClose", onPanelClose);
		dashboardWin.addEventListener("onLayoutChange", onLayoutChange);
		
		if(LOG.isDebugEnabled()){
			LOG.debug("Created Dashboard");
			LOG.debug("Panel Count - " + dashboard.getColumnCount());
			LOG.debug("dashboard.isShowFiltersPanel() --> " + dashboard.isShowFiltersPanel());
		}
		
		if(dashboard.isShowFiltersPanel()) {
			//Unifying Filter Objects - Making Duplicates filters a single instance
			List<Filter> persistedGlobalFilters = new ArrayList<Filter>();
			Set<Filter> filters;
			for (Portlet portlet : dashboard.getPortletList()) {
				if(Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState()) 
						&& !Constants.TREE_LAYOUT.equals(portlet.getChartType())
						&&	portlet.getChartData().getIsFiltered()){
					filters = new LinkedHashSet<Filter>();
					for (Filter filter : portlet.getChartData().getFilterSet()) {
						if(filter.getIsCommonFilter() && persistedGlobalFilters.contains(filter)){
							filters.add(persistedGlobalFilters.get(persistedGlobalFilters.indexOf(filter)));
						} else {
							filters.add(filter);
							if(filter.getIsCommonFilter())
								persistedGlobalFilters.add(filter);
						}
					}
					portlet.getChartData().setFilterSet(filters);
				}
			}
 
			if(LOG.isDebugEnabled()) {
				LOG.debug("Persisted Common filters -> " + persistedGlobalFilters);
			}	
				
			//Generating applied filter rows, with values
			Set<Field> commonFilters = new HashSet<Field>();
			for (Portlet portlet : dashboard.getPortletList()) {
				if(Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState()) 
						&& !Constants.TREE_LAYOUT.equals(portlet.getChartType())
						&& portlet.getChartData().getIsFiltered()) {
					for (Filter filter : portlet.getChartData().getFilterSet()) {
						if(filter.getIsCommonFilter()) {
							Field field = null;
							field = new Field();
							field.setColumnName(filter.getColumn());
							if(filter.getType().equals(Constants.STRING_DATA)) {
								// String filters now
								if(commonFilters.add(field))
									filterRows.appendChild(createStringFilterRow(field, filter));
								
							} else if( filter.getType().equals(Constants.NUMERIC_DATA)) {
								//Numeric filters
								if(commonFilters.add(field))
									filterRows.appendChild(createNumericFilterRow(field, filter));
							}
						}
					}
				}
			}

			// Getting All filter columns
			commonFilterFieldSet = new LinkedHashSet<Field>();
			for (Portlet portlet : dashboard.getPortletList()) {
				if (Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState()) 
						&& !Constants.TREE_LAYOUT.equals(portlet.getChartType())) {
					for (Field field : portlet.getChartData().getFields()) {
						// Excluding persisted
						if (!commonFilters.contains(field)) {
							commonFilterFieldSet.add(field);
						}
					}
				}
			}

			if(LOG.isDebugEnabled()) {
				LOG.debug("Column set -> " + commonFilterFieldSet);
			}
			
			constructFilterItem(commonFilterFieldSet);

			commonFiltersPanel.setVisible(true);
		}
		
		this.getSelf().addEventListener("onDrawingLiveChart", onDrawingLiveChart);
		this.getSelf().addEventListener("onPanelReset", onPanelReset);
	}
		

	private void constructFilterItem(Set<Field> fields) {
		Listitem listitem;
		for (Field field : fields) {
			listitem = new Listitem(field.getColumnName());
			listitem.setAttribute(Constants.FIELD, field);
			listitem.setParent(commonFilterList);
		}
	}	
	
	/**
	 * onSelect listener when a coloumn is selected in Popup
	 * @param event
	 * @throws Exception
	 */	
	@Listen("onSelect = #commonFilterList")
	public void onFilterColumnSelect(SelectEvent<Component, Object> event) throws Exception {
		Listitem selectedItem = (Listitem) event.getSelectedItems().iterator().next();
		
		Field field = (Field) selectedItem.getAttribute(Constants.FIELD);
		
		Popup popup = (Popup) selectedItem.getParent().getParent();
		popup.close();
		
		Filter newFilter = new Filter();
		newFilter.setIsCommonFilter(true);
		newFilter.setColumn(field.getColumnName());
		
		if(DashboardUtil.checkNumeric(field.getDataType())){
			newFilter.setType(Constants.NUMERIC_DATA);
			filterRows.appendChild(createNumericFilterRow(field, newFilter));
		} else {
			newFilter.setType(Constants.STRING_DATA);
			filterRows.appendChild(createStringFilterRow(field, newFilter));
		}
		
		commonFilterFieldSet.remove(field);
		selectedItem.detach();
		
		List<Filter> filtersToReomove = new ArrayList<Filter>();
		for (Portlet portlet : dashboard.getPortletList()) {
			if(Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState()) 
					&& !Constants.TREE_LAYOUT.equals(portlet.getChartType())
					&& portlet.getChartData().getIsFiltered()) {
				
				filtersToReomove = new ArrayList<Filter>();
				for (Filter filter : portlet.getChartData().getFilterSet()) {
					// overriding the portlet specific filter by selected global/dashboard filter
					if (!filter.getIsCommonFilter()
							&& filter.getColumn().equals(field.getColumnName())) {
						filtersToReomove.add(filter);						
					}
				}
				
				//Removing portlet specific filters, when selecting the same global filter
				if ( portlet.getChartData().getFilterSet().removeAll(filtersToReomove) ) {
					if(portlet.getChartData().getFilterSet().size() < 1){
						portlet.getChartData().setIsFiltered(false);
					}			
					updateWidgets(portlet);
				}
			}
			
		}
	}
	
	/**
	 * Updates charts in the portlet passed
	 * 
	 * @param portlet
	 * @param portalChildren
	 * @throws Exception
	 */
	public void updateWidgets(Portlet portlet) throws Exception{

		if(LOG.isDebugEnabled()){
			LOG.debug("Updating charts in portlet - " + portlet);
		}
		Portalchildren children = portalChildren.get(portlet.getColumn());
		LOG.debug("portalchildren in updateWidgets()-->"+children);
		ChartPanel panel =null;
		for (Component comp : children.getChildren()) {
			panel = (ChartPanel) comp;
			if (panel.getPortlet().getId() == portlet.getId()) {
				break;
			}
		}
		//Updating widget with latest filter details into DB
		WidgetService widgetService = (WidgetService)SpringUtil.getBean("widgetService");
		portlet.setChartDataXML(chartRenderer.convertToXML(portlet.getChartData()));
		widgetService.updateWidget(portlet);
		
		if(Constants.TABLE_WIDGET.equals(portlet.getChartType())){	
			//Refreshing table with updated filter values
			panel.drawTableWidget();
		}else{
			ChartRenderer chartRenderer = (ChartRenderer) SpringUtil.getBean("chartRenderer");
			//Refreshing chart with updated filter values
			chartRenderer.constructChartJSON(portlet.getChartData(), portlet, false);	
			if (panel.drawD3Graph() != null) {
					Clients.evalJavaScript(panel.drawD3Graph());
			}
		}
	}
	
	/**
	 * Event listener to be invoked when a new live chart is drawn.
	 * Adds new columns to the displayed Filter Column list 
	 */
	EventListener<Event> onDrawingLiveChart = new EventListener<Event>() {
		
		@Override
		public void onEvent(Event event) throws Exception {
			Portlet portlet = (Portlet) event.getData();
			
			if(dashboard.isShowFiltersPanel() && 
					Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState()) ) {
				Listitem filterItem = null;
				for (Field field : portlet.getChartData().getFields()) {
					if(commonFilterFieldSet.add(field)){
						filterItem = new Listitem();
						filterItem.setLabel(field.getColumnName());
						filterItem.setAttribute(Constants.FIELD, field);
						filterItem.setParent(commonFilterList);
					}
				}
			}
			
		}
	};
	
	/**
	 * Creates a row of Common filters with a list of Distinct Values 
	 * from the specified filter column from all datasets present in the Dashboard
	 * 
	 * @param field
	 * @param filter
	 * @return
	 * 	Constructed row
	 * @throws Exception
	 */
	private Row createStringFilterRow(Field field, Filter filter) throws Exception {
		Row row = new Row();
		row.setAttribute(Constants.ROW_CHECKED, false);
		if(appliedCommonFilterSet == null ){
			appliedCommonFilterSet = new HashSet<Filter>();
		}
		Sessions.getCurrent().setAttribute(Constants.COMMON_FILTERS, appliedCommonFilterSet);
		appliedCommonFilterSet.add(filter);
		
		row.setAttribute(Constants.FILTER, filter);
		row.setAttribute(Constants.FIELD, field);
		
		Div div = new Div();
		Label label = new Label(field.getColumnName());
		label.setSclass("h5");
		div.appendChild(label);
		Button button = new Button();
		button.setSclass("glyphicon glyphicon-remove-circle btn btn-link img-btn");
		button.setStyle("float: right;");
		button.addEventListener(Events.ON_CLICK, removeGlobalFilter);
		div.appendChild(button);
		
		Anchorlayout anchorlayout = new Anchorlayout();
		anchorlayout.setHflex("1");
		
		Set<String> values = new LinkedHashSet<String>();
		//A set of Datasets used in dashboard, for avoiding multiple fetches to the same dataset
		Set<String> dataFiles = new HashSet<String>();
		// Getting distinct values from all live Portlets
		for (Portlet portlet : dashboard.getPortletList()) {
			if(portlet.getWidgetState().equals(Constants.STATE_LIVE_CHART) 
					&& !Constants.TREE_LAYOUT.equals(portlet.getChartType())) {
				if(!dataFiles.contains(portlet.getChartData().getFileName()) && 
						portlet.getChartData().getFields().contains(field)) {
					dataFiles.add(portlet.getChartData().getFileName());
					Iterator<String> iterator = hpccService.getDistinctValues(field.getColumnName(), portlet.getChartData(), false).iterator();
					while (iterator.hasNext()) {
						values.add(iterator.next());
					}
				}
			}
		}
		
		Boolean showApplyButton = false;
		if(values.size() > 5){
			showApplyButton = true;
		}
		
		//Generating Checkboxes
		Anchorchildren anchorchildren;
		Checkbox checkbox;
		for (String value : values) {
			anchorchildren = new Anchorchildren();
			checkbox = new Checkbox(value);
			checkbox.setZclass("checkbox");
			checkbox.setStyle("margin: 0px; padding-right: 5px;");
			if(showApplyButton){
				checkbox.addEventListener(Events.ON_CHECK, stringFilterMultiCheckListener);
			} else {
				checkbox.addEventListener(Events.ON_CHECK, stringFilterCheckListener);
			}
			anchorchildren.appendChild(checkbox);
			anchorlayout.appendChild(anchorchildren);
			//To display previously selected filter values
			if(filter != null && filter.getValues() != null && filter.getValues().contains(value)){
				checkbox.setChecked(true);
				row.setAttribute(Constants.ROW_CHECKED, true);
			}
		}
		
		row.appendChild(div);
		
		Hbox hbox = new Hbox();
		hbox.appendChild(anchorlayout);
		
		if(showApplyButton) {
			Button applyButton = new Button("Apply");
			applyButton.setZclass("btn btn-xs");
			applyButton.setSclass("btn-default");
			applyButton.addEventListener(Events.ON_CLICK, new EventListener<Event>() {

				@Override
				public void onEvent(Event event) throws Exception {
					Button button = (Button) event.getTarget();
					Row row = (Row) button.getParent().getParent();

					row.setAttribute(Constants.ROW_CHECKED, true);
					
					Filter filter = (Filter) row.getAttribute(Constants.FILTER);
					Field field = (Field) row.getAttribute(Constants.FIELD);
					if(filter.getValues() != null) {
						updateStringFilterToPortlets(filter, field);
						button.setSclass("btn-default");
					} else {
						Clients.showNotification(Labels.getLabel("selectValueToApply"), "warning", row, "after_center", 2000);
					}
				}
				
			});
			hbox.appendChild(applyButton);
		}
				
		row.appendChild(hbox);
		return row;
	}	
	
	/**
	 * Event listener for string filters when there's a separate apply button.
	 */
	EventListener<Event> stringFilterMultiCheckListener = new EventListener<Event>() {
		
		@Override
		public void onEvent(Event event) throws Exception {
			Anchorlayout anchorlayout = (Anchorlayout) event.getTarget().getParent().getParent();
			Hbox hbox = (Hbox) anchorlayout.getParent();
			Row row = (Row) hbox.getParent();
			Filter filter = (Filter) row.getAttribute(Constants.FILTER);
			
			//Instantiating Value list if empty
			if(filter.getValues() == null){
				List<String> values = new ArrayList<String>();
				filter.setValues(values);
			}
			
			//Updating change to filter object
			for (Component component : anchorlayout.getChildren()) {
				Checkbox checkbox = (Checkbox) component.getFirstChild();
				String value = checkbox.getLabel();
				if(checkbox.isChecked()){
					if(!filter.getValues().contains(value))
						filter.getValues().add(value);
				} else {
					filter.getValues().remove(value);
				}
			}
			
			Button button = (Button) hbox.getLastChild();
			button.setSclass("btn-warning");
		}
	};
	
	/**
	 * Creates a row with Slider for Numeric Filters
	 * 
	 * @param field
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	private Row createNumericFilterRow(Field field, Filter filter) throws Exception {
		Row row = new Row();
		row.setAttribute(Constants.ROW_CHECKED, false);
		if(appliedCommonFilterSet == null ){
			appliedCommonFilterSet = new HashSet<Filter>();
		}
		Sessions.getCurrent().setAttribute(Constants.COMMON_FILTERS, appliedCommonFilterSet);
		appliedCommonFilterSet.add(filter);
		
		row.setAttribute(Constants.FILTER, filter);
		row.setAttribute(Constants.FIELD, field);
		
		Div div = new Div();
		Label label = new Label(field.getColumnName());
		label.setSclass("h5");
		div.appendChild(label);
		Button button = new Button();
		button.setSclass("glyphicon glyphicon-remove-circle btn btn-link img-btn");
		button.setStyle("float: right;");
		button.addEventListener(Events.ON_CLICK, removeGlobalFilter);
		div.appendChild(button);
		
		//A set of Datasets used in dashboard, for avoiding multiple fetches to the same dataset
		Set<String> dataFiles = new HashSet<String>();
		// Getting Minimum and Maximum across all portlets
		BigDecimal min = null;
		BigDecimal max = null;
		for (Portlet portlet : dashboard.getPortletList()) {
			if(portlet.getWidgetState().equals(Constants.STATE_LIVE_CHART) && 
					!Constants.TABLE_WIDGET.equals(portlet.getChartType())
					&& !Constants.TREE_LAYOUT.equals(portlet.getChartType())) {
				if(!dataFiles.contains(portlet.getChartData().getFileName()) && 
						portlet.getChartData().getFields().contains(field)) {
					dataFiles.add(portlet.getChartData().getFileName());
					Map<Integer, BigDecimal> map = hpccService.getMinMax(filter.getColumn(), portlet.getChartData(), false);
					
					if(min == null || min.compareTo(map.get(Constants.FILTER_MINIMUM)) > 0) {
						min = map.get(Constants.FILTER_MINIMUM);
					}
					if(max==null || max.compareTo(map.get(Constants.FILTER_MAXIMUM)) <0 ) {
						max = map.get(Constants.FILTER_MAXIMUM);
					}
				}
			}
		}
		
		//Intitialising Slider positions
		Integer sliderStart = 0;
		Integer sliderEnd = 100;
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("Min & Max - " + min + max);
		}
		
		//Translating min & max to a scale of 0 to 100 using Linear equation 
		//((actualVal - actualMin)/(actualMax- actualMin)) = ((sliderVal - sliderMin)/(sliderMax- sliderMin))
		// Range Factor = (actualMax- actualMin)/(sliderMax- sliderMin)
		BigDecimal rangeFactor = max.subtract(min).divide(new BigDecimal(100));
		
		if(filter.getStartValue() != null && filter.getEndValue() != null) {
			//Updating slider positions for already applied filters
			sliderStart = filter.getStartValue().subtract(min).divide(rangeFactor, RoundingMode.DOWN).intValue();
			sliderEnd = filter.getEndValue().subtract(min).divide(rangeFactor, RoundingMode.CEILING).intValue();
		} else {
			filter.setStartValue(min);
			filter.setEndValue(max);
		}
		
		//Adding minimum and Range factor to Row to resume calculations on listener
		row.setAttribute(MINIMUM_VALUE, min);
		row.setAttribute(RANGE_FACTOR, rangeFactor);
		
		Hbox hbox = new Hbox();
		//Setting Id to be passed from Java script
		hbox.setId(field.getColumnName() + "_hbox");
		
		Label minLabel = new Label(String.valueOf(filter.getStartValue().intValue()));
		minLabel.setWidth("75px");
		Label maxLabel = new Label(String.valueOf(filter.getEndValue().intValue()));
		maxLabel.setWidth("75px");
		
		Div sliderDiv = new Div();
		sliderDiv.setWidth("300px");
		
		StringBuilder html = new StringBuilder();
		html.append("<div id=\"");
			html.append(field.getColumnName());
			html.append("_sdiv\" style=\"margin: 8px;\" class=\"slider-grey\">");
			html.append("</div>");
		
		html.append("<script type=\"text/javascript\">");
			html.append("$('#").append(field.getColumnName()).append("_sdiv').slider({")
				.append("range: true,")
				.append("values: [").append(sliderStart).append(", ").append(sliderEnd).append("]")
				.append("});");
	
			html.append("$('#").append(field.getColumnName()).append("_sdiv').on( \"slide\", function( event, ui ) {")
				.append("payload = \"").append(field.getColumnName()).append("_hbox,\" + ui.values;")
				.append("zAu.send(new zk.Event(zk.Widget.$('$")
					.append("dashboardWin").append("'), 'onSlide', payload, {toServer:true}));")
				.append("});");
			
			html.append("$('#").append(field.getColumnName()).append("_sdiv').on( \"slidestop\", function( event, ui ) {")
				.append("payload = \"").append(field.getColumnName()).append("_hbox,\" + ui.values;")
				.append("zAu.send(new zk.Event(zk.Widget.$('$")
					.append("dashboardWin").append("'), 'onSlideStop', payload, {toServer:true}));")
				.append("});");
		html.append("</script>");
		
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("Generated HTML " + html.toString());
		}
		
		sliderDiv.appendChild(new Html(html.toString()));
		
		Div holder = new Div();
		holder.setStyle("min-width: 70px; text-align: end;");
		holder.appendChild(minLabel);
		hbox.appendChild(holder);
		
		hbox.appendChild(sliderDiv);
		
		holder = new Div();
		holder.setStyle("min-width: 70px");
		holder.appendChild(maxLabel);
		hbox.appendChild(holder);
		
		row.appendChild(div);
		row.appendChild(hbox);
		return row;
	}
	
	/**
	 * Listener event, to be triggered by Slider from Client
	 * Event is triggered onSlide
	 * 
	 * @param event
	 */
	@Listen("onSlide = #dashboardWin")
	public void onSlide(Event event) {
		String[] data = ((String) event.getData()).split(",");
		
		Hbox hbox = (Hbox) this.getSelf().getFellow(data[0]);
		Row row = (Row) hbox.getParent();
		
		BigDecimal min = (BigDecimal) row.getAttribute(MINIMUM_VALUE);
		BigDecimal rangeFactor = (BigDecimal) row.getAttribute(RANGE_FACTOR);
		
		Integer startPosition = Integer.valueOf(data[1]);
		Integer endPosition = Integer.valueOf(data[2]);
		
		Label minLabel = (Label) hbox.getFirstChild().getFirstChild();
		Label maxLabel = (Label) hbox.getLastChild().getFirstChild();
		
		//Converting position into value
		// value = pos . rangeFactor + min  
		minLabel.setValue(String.valueOf(rangeFactor.multiply(new BigDecimal(startPosition)).add(min).intValue()));
		maxLabel.setValue(String.valueOf(rangeFactor.multiply(new BigDecimal(endPosition)).add(min).intValue()));
	}

	/**
	 * Listener event, to be triggered by Slider from Client
	 * Event is triggered when the slider id stopped after sliding
	 * @param event
	 */
	@Listen("onSlideStop = #dashboardWin")
	public void onSlideStop(Event event) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("On Slide Stop Event - Data -- " + event.getData());
		}
		
		String[] data = ((String) event.getData()).split(",");
		
		Hbox hbox = (Hbox) this.getSelf().getFellow(data[0]);
		Row row = (Row) hbox.getParent();
		row.setAttribute(Constants.ROW_CHECKED, true);
		Filter filter = (Filter) row.getAttribute(Constants.FILTER);
		Field field = (Field) row.getAttribute(Constants.FIELD);
		
		BigDecimal min = (BigDecimal) row.getAttribute(MINIMUM_VALUE);
		BigDecimal rangeFactor = (BigDecimal) row.getAttribute(RANGE_FACTOR);
		
		Integer startPosition = Integer.valueOf(data[1]);
		Integer endPosition = Integer.valueOf(data[2]);
		
		//Updating Change to filter object
		// value = pos . rangeFactor + min  
		filter.setStartValue(rangeFactor.multiply(new BigDecimal(startPosition)).add(min));
		filter.setEndValue(rangeFactor.multiply(new BigDecimal(endPosition)).add(min));
		
		for (Portlet portlet : dashboard.getPortletList()) {
			if(!Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())
					|| Constants.TREE_LAYOUT.equals(portlet.getChartType()))
				continue;
			
			XYChartData chartData = portlet.getChartData();
			
			if(chartData.getFields().contains(field)) {
				//Overriding filter if applied already
				if(chartData.getIsFiltered() && chartData.getFilterSet().contains(filter)){
					chartData.getFilterSet().remove(filter);
					chartData.getFilterSet().add(filter);
				} else {
					chartData.setIsFiltered(true);
					chartData.getFilterSet().add(filter);
				}
				
				try {
					updateWidgets(portlet);
				} catch (Exception e) {
					LOG.error("Error Updating Charts", e);
					//TODO: Show Notification
				}
			}
		}
		
	}

	
	/**
	 * Listener to remove global filters
	 */
	EventListener<MouseEvent> removeGlobalFilter = new EventListener<MouseEvent>() {

		@Override
		public void onEvent(MouseEvent event) throws Exception {
			Row removedRow = (Row) event.getTarget().getParent().getParent();
			Boolean rowChecked = (Boolean)removedRow.getAttribute(Constants.ROW_CHECKED);
			//refresh the portlets, if the removed row/filter has any checked values
			if(rowChecked){
			Iterator<Portlet> iterator = removeFilter(removedRow).iterator();
			
			// refreshing the chart && updating DB
			while (iterator.hasNext()) {
				Portlet portlet = iterator.next();
				updateWidgets(portlet);
				}
			}
			//Removing the Filter row in UI
			removeFilterRow(removedRow);
		}
	};

	/**
	 * Event to be triggered when any filter value is checked or Unchecked
	 * @param event
	 */
	EventListener<CheckEvent> stringFilterCheckListener = new EventListener<CheckEvent>() {
		
		@Override
		public void onEvent(CheckEvent event) throws Exception {
			Anchorlayout anchorlayout = (Anchorlayout) event.getTarget().getParent().getParent();
			Row row = (Row) anchorlayout.getParent().getParent();
			Filter filter = (Filter) row.getAttribute(Constants.FILTER);
			Field field = (Field) row.getAttribute(Constants.FIELD);
			row.setAttribute(Constants.ROW_CHECKED, true);
			//Instantiating Value list if empty
			if(filter.getValues() == null){
				List<String> values = new ArrayList<String>();
				filter.setValues(values);
			}
			
			//Updating change to filter object
			for (Component component : anchorlayout.getChildren()) {
				Checkbox checkbox = (Checkbox) component.getFirstChild();
				String value = checkbox.getLabel();
				if(checkbox.isChecked()){
					if(!filter.getValues().contains(value))
						filter.getValues().add(value);
				} else {
					filter.getValues().remove(value);
				}
			}
			
			if(LOG.isDebugEnabled()){
				LOG.debug("Selected Filter Column -> " + field.getColumnName());
				LOG.debug("Selected Filter Values -> " + filter.getValues());
			}
			
			updateStringFilterToPortlets(filter, field);
		}
	};
	
	/**
	 * Updates portlet objects on the dashboard according to the filter object passed.
	 * 
	 * @param filter
	 * @param field
	 */
	private void updateStringFilterToPortlets(Filter filter, Field field) {
		for (Portlet portlet : dashboard.getPortletList()) {
			if(!Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())
					 || Constants.TREE_LAYOUT.equals(portlet.getChartType())){
				continue;
			}
			
			XYChartData chartData = portlet.getChartData();
			if(filter.getValues().size() < 1) {
				//Removing Filter if no values selected
				if(chartData.getIsFiltered()){
					chartData.getFilterSet().remove(filter);
					if(chartData.getFilterSet().size() < 1)
						chartData.setIsFiltered(false);
				}
			} else {
				// Adding Filter to Portlets
				if(chartData.getFields().contains(field)) {
					//Overriding filter if applied already
					if(chartData.getIsFiltered() && chartData.getFilterSet().contains(filter)){
						chartData.getFilterSet().remove(filter);
						chartData.getFilterSet().add(filter);
					} else {
						chartData.setIsFiltered(true);
						chartData.getFilterSet().add(filter);
					}
					
				}
			}
			
			if(chartData.getFields().contains(field)){
				try {
					updateWidgets(portlet);
				} catch (Exception e) {
					LOG.error("Error Updating Charts", e);
					//TODO: Show Notification
				}
			}
		}
	}
	
	/**
	 * Event to be triggered onClick of 'Add Widget' Button
	 */
	@Listen("onClick = #addWidget")
	public void addWidget() {
		ChartPanel chartPanel=null;
		try{
			final Portlet portlet = new Portlet();
			
			portlet.setWidgetState(Constants.STATE_EMPTY);
			dashboard.getPortletList().add(portlet);
			
			// Adding new Widget to the column with lowest number of widgets
			Integer count = 0, childCount = 0, column = 0;
			for (Portalchildren portalchildren : portalChildren) {
				if(! (count < dashboard.getColumnCount())) {
					break;
				}
				if(portalchildren.getChildren().size() < childCount) {
					column = count;
				}
				childCount = portalchildren.getChildren().size();
				count ++;
			}
			portlet.setColumn(column);
			chartPanel = new ChartPanel(portlet);
			portalChildren.get(portlet.getColumn()).appendChild(chartPanel);
			chartPanel.focus();
			
			manipulatePortletObjects(Constants.ReorderPotletPanels);
			
			portlet.setId(widgetService.addWidget(dashboardId, portlet, dashboard.getPortletList().indexOf(portlet)));
			
			//Updating new widget sequence to DB
			widgetService.updateWidgetSequence(dashboard);
		}catch (DataAccessException e) {
			LOG.error("Error while adding new Widget", e);
			Clients.showNotification(Labels.getLabel("widgetHaventSaved"), "error", chartPanel, "middle_center", 5000, true);
		}
		catch (Exception e) {
			LOG.error("Error while adding new Widget", e);
			Clients.showNotification(Labels.getLabel("widgetHaventSaved"), "error", chartPanel, "middle_center", 5000, true);
		}
		
	}
	
	/**
	 * Event to be triggered onClick of 'Configure Dashboard' Button
	 * @param event
	 */
	@Listen("onClick = #configureDashboard")
	public void configureDashboard(Event event) {
		oldColumnCount = dashboard.getColumnCount();
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(Constants.PARENT, dashboardWin);
		parameters.put(Constants.DASHBOARD, dashboard);
		
		Window window  = (Window) Executions.createComponents("/demo/layout/dashboard_config.zul", dashboardWin, parameters);
		window.doModal();
	}
	
	public void manipulatePortletObjects(short option) {
		
		switch(option)
		{
			case Constants.ReorderPotletPanels:
				if(LOG.isDebugEnabled()) {
					LOG.debug("Reordering portlets.");
					LOG.debug("Now the portlet size is -> " + DashboardController.this.dashboard.getPortletList().size());
				}
				ArrayList<Portlet> newPortletList = new ArrayList<Portlet>();
				short portletChild=0;int colCount=0;Iterator<Component> iterator=null;
				
				Component component=null;
				Portlet portlet=null;
				do
				{
					if(portalChildren.get(portletChild).getChildren().size()>0)
					{
						iterator = (Iterator<Component>) portalChildren.get(portletChild).getChildren().iterator();
						while(iterator.hasNext()){
							 component = iterator.next();
							 portlet = ((ChartPanel)component).getPortlet();
							 portlet.setColumn(colCount);
							 newPortletList.add(portlet);
						 }
						colCount++;
					}
					portletChild++;
					
				}while(portletChild<3);
				
				dashboard.setPortletList(newPortletList);
			break;
			
			case Constants.ResizePotletPanels:
				if(LOG.isDebugEnabled()) {
					LOG.debug("Resizing portlet children");
					LOG.debug("Now the portlet size is -> " + DashboardController.this.dashboard.getPortletList().size());
				}
				Integer counter = 0;
				for(final Portalchildren portalChildren : this.portalChildren) {
					if(counter < dashboard.getColumnCount()){
						portalChildren.setVisible(true);
						portalChildren.setWidth((100/dashboard.getColumnCount()) + PERCENTAGE_SIGN);
						final List<Component> list = portalChildren.getChildren();
						for (final Component component1 : list) {
								final ChartPanel panel = (ChartPanel) component1;
								if(panel.drawD3Graph() != null) {
									Clients.evalJavaScript(panel.drawD3Graph());
								}
						}
					} else {
						portalChildren.setVisible(false);
					}
					counter ++;
				}
			break;
		}
	}
	
	/**
	 * Event listener to listen to 'Dashboard Configuration'
	 */
	final EventListener<Event> onLayoutChange = new EventListener<Event>() {

		@Override
		public void onEvent(Event event) throws Exception {
			// Check if any visible panels are hidden when layout is changed
			if(dashboard.getColumnCount() < oldColumnCount) {
				//List to capture hidden panels
				List<Component> hiddenPanels = new ArrayList<Component>();
				
				Integer counter = 0;
				for (Portalchildren component : portalChildren) {
					if( !(counter < dashboard.getColumnCount()) ) {
						hiddenPanels.addAll(component.getChildren());
						component.getChildren().clear();
					}
					counter ++;
				}
				
				//Adding hidden panels to last visible column 
				for (Component component : hiddenPanels) {
					if(component instanceof ChartPanel) {
						portalChildren.get(dashboard.getColumnCount() -1).appendChild(component);
					}
				}
			}
			
			//To update Dashboard Name
			onNameChange();
			if(commonFilterFieldSet == null ) {
				commonFilterFieldSet = new LinkedHashSet<Field>();
			}
			//Showing Common filters panel
			if(dashboard.isShowFiltersPanel()){
				// Getting All filter columns
				for (Portlet portlet : dashboard.getPortletList()) {
					if(portlet.getChartData() != null){
						commonFilterFieldSet.addAll(portlet.getChartData().getFields());
					}				
				}
				
				// Generating popup
				Listitem filterItem = null;
				for(Field field : commonFilterFieldSet){
					filterItem = new Listitem();
					filterItem.setLabel(field.getColumnName());
					filterItem.setAttribute(Constants.FIELD, field);
					filterItem.setParent(commonFilterList);
				}
				
				commonFiltersPanel.setVisible(true);
			} else {				
				if (filterRows.getChildren() != null&& filterRows.getChildren().size() > 0) {
					removeGlobalFilters();
				}
			}
			
			manipulatePortletObjects(Constants.ReorderPotletPanels);
			manipulatePortletObjects(Constants.ResizePotletPanels);
			try{
				//updating Dashboard details
				dashboard.setLastupdatedDate(new Timestamp(Calendar.getInstance().getTime().getTime()));
				dashboardService.updateDashboard(dashboard);
				
				//updating Widget sequence
				widgetService.updateWidgetSequence(dashboard);
			}catch(DataAccessException ex){
				LOG.error("Exception while configuring Dashboard in onLayoutChange()", ex);
			}
		}		
	};

	/**
	 *Method to remove all global filters, while unchecking common filter
	 * in dashboard configuration page
	 */
	private void removeGlobalFilters() {
		try {
			EventListener<ClickEvent> removeAllGlobalFilters = new EventListener<ClickEvent>() {
				@Override
				public void onEvent(ClickEvent event) throws Exception {
					if (Messagebox.Button.YES.equals(event.getButton()) &&appliedCommonFilterSet != null) {							
						for (Portlet portlet : dashboard.getPortletList()) {
							if (!Constants.TREE_LAYOUT.equals(portlet.getChartType()) &&
									Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())) {
								for (Filter filter : appliedCommonFilterSet) {
									// removing global filter object from filterlist
									if (portlet.getChartData().getIsFiltered()
											&& portlet.getChartData().getFilterSet().contains(filter)) {
										portlet.getChartData().getFilterSet().remove(filter);
										if (portlet.getChartData().getFilterSet().size() < 1) {
											portlet.getChartData().setIsFiltered(false);
										}
									}
								}
								// refreshing the chart && updating DB
								updateWidgets(portlet);
							}
						}
						Sessions.getCurrent().removeAttribute(Constants.COMMON_FILTERS);
						// Removing common filters Row from UI
						filterRows.getChildren().clear();
						// making common filters panel unvisible
						commonFiltersPanel.setVisible(false);
						dashboard.setShowFiltersPanel(false);
					}else if(Messagebox.Button.NO.equals(event.getButton())){
						dashboard.setShowFiltersPanel(true);
					}

				}
			};

			Map<String, String> params = new HashMap<String, String>();
			params.put("sclass", "panel");

			Messagebox.show(
					Constants.REMOVE_GLOBAL_FILTERS, 
					Constants.REMOVE_GLOBAL_FILTERS_TITLE, 
					new Messagebox.Button[]{
							Messagebox.Button.YES, 
							Messagebox.Button.NO },
					new String[] {
						"Yes", "No"
					},
					Messagebox.QUESTION,
					Messagebox.Button.YES,
					removeAllGlobalFilters, 
					params
					);
		} catch (Exception e) {
			LOG.debug(" Exception while removing global filters", e);
		}
	}
	
	/**
	 *  When a widget is deleted
	 */
	final EventListener<Event> onPanelClose = new EventListener<Event>() {

		public void onEvent(final Event event) throws Exception {
			
			Portlet deletedPortlet = (Portlet) event.getData();
			dashboard.getPortletList().remove(deletedPortlet);
			
			if(LOG.isDebugEnabled()) {
				LOG.debug("Deleted portlet -> " + deletedPortlet);
			}
			
			Events.sendEvent("onPanelReset", DashboardController.this.getSelf(), deletedPortlet);			
			
			if(LOG.isDebugEnabled()) {
				LOG.debug("hide portlet event");
			}
			manipulatePortletObjects(Constants.ReorderPotletPanels);
			manipulatePortletObjects(Constants.ResizePotletPanels);
			
			if(LOG.isDebugEnabled()) {
				LOG.debug("Now the portlet size is -> " + DashboardController.this.dashboard.getPortletList().size());
			}
			try{
				if(dashboard.getPortletList().size() > 0){
					//Updating new widget sequence to DB
					widgetService.updateWidgetSequence(dashboard);
				}
			}catch(DataAccessException e){
				LOG.error("Exception in onPanelClose()", e);
			}
		}
	};	
	
	/**
	 * Event listener to be invoked when a Panel is reset
	 */
	final EventListener<Event> onPanelReset = new EventListener<Event>() {
		
		@Override
		public void onEvent(Event event) throws Exception {
			Portlet deletedPortlet = (Portlet) event.getData();
			dashboard.getPortletList().remove(deletedPortlet);
			//Remove applied filters
			Set<Filter> filtersToRemove = new HashSet<Filter>();
			Set<Filter> filtersToRefresh = new HashSet<Filter>();
			if(Constants.STATE_LIVE_CHART.equals(deletedPortlet.getWidgetState()) 
					&& deletedPortlet.getChartData() != null
					&& deletedPortlet.getChartData().getIsFiltered())  {
				
				for (Filter filter : deletedPortlet.getChartData().getFilterSet()) {
					if(filter.getIsCommonFilter()){
						if(LOG.isDebugEnabled()) {
							LOG.debug("Adding to remove list -> " + filter);
						}
						filtersToRemove.add(filter);
						for (Portlet portlet : dashboard.getPortletList()) {
							if(!Constants.TREE_LAYOUT.equals(portlet.getChartType())
									&& Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState()) && 
									! deletedPortlet.getChartData().getFileName().equals(portlet.getChartData().getFileName()) &&
									portlet.getChartData().getIsFiltered()) {
								for (Filter portletFilter : portlet.getChartData().getFilterSet()) {
									if(portletFilter.equals(filter)) {
										filtersToRefresh.add(filter);
									}
								}
							} else if (!Constants.TREE_LAYOUT.equals(portlet.getChartType()) &&
									Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState()) &&
									deletedPortlet.getChartData().getFileName().equals(portlet.getChartData().getFileName())) {
								filtersToRemove.remove(filter);
							}
						}
					}
				}
			}
			
			deletedPortlet.setChartData(null);
			deletedPortlet.setChartDataJSON(null);
			deletedPortlet.setChartDataXML(null);
			deletedPortlet.setChartType(null);
			deletedPortlet.setName(null);
			deletedPortlet.setWidgetState(Constants.STATE_EMPTY);
			
			//Clears all chart data from DB
    		WidgetService widgetService =(WidgetService) SpringUtil.getBean("widgetService");
    		widgetService.updateWidget(deletedPortlet);
    		
			filtersToRemove.removeAll(filtersToRefresh);
			
			if(LOG.isDebugEnabled()){
				LOG.debug(" Filters to remove -> " + filtersToRemove);
				LOG.debug(" Filters to refresh -> " + filtersToRefresh);
			}
			
			//Refreshing filters
			Row row;
			{
				Filter filter;
				Field field;
				List<Row> rowsToDelete = new ArrayList<Row>();
				List<Row> rowsToReplace = new ArrayList<Row>();
				for (Component component : filterRows.getChildren()) {
					row = (Row) component;
					filter = (Filter) row.getAttribute(Constants.FILTER);
					field = (Field) row.getAttribute(Constants.FIELD);
					if(filtersToRefresh.contains(filter)) {
						rowsToDelete.add(row);
						rowsToReplace.add(createStringFilterRow(field, filter));
					}
				}
				
				for (Row row2 : rowsToDelete) {
					row2.detach();
				}
				for (Row row2 : rowsToReplace) {
					filterRows.appendChild(row2);
				}
			}
			
			//Removing filters
			Set<Portlet> portlets = new HashSet<Portlet>();
			List<Row> rowsToReplace = new ArrayList<Row>();
			for (Filter filter : filtersToRemove) {
				for (Component component : filterRows.getChildren()) {
					row = (Row) component;
					Filter rowFilter = (Filter) row.getAttribute(Constants.FILTER);
					if(filter.equals(rowFilter)) {
						rowsToReplace.add(row);
					}
				}
			}
			for (Row row2 : rowsToReplace) {
				//refreshing portlets
				portlets.addAll(removeFilter(row2));
				//removing filter row in UI
				removeFilterRow(row2);
			}
			for (Portlet portlet : portlets) {
				updateWidgets(portlet);
			}
			
			//Refreshing Column List for Common filters
			Filter fieldFilter;
			if(dashboard.isShowFiltersPanel()) {
				// Remove existing List
				commonFilterList.getChildren().clear();
				commonFilterFieldSet = new LinkedHashSet<Field>(); 
				//Generating New List
				for (Portlet portlet : dashboard.getPortletList()) {
					if(!Constants.TREE_LAYOUT.equals(portlet.getChartType())
							&& Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())) {
						for (Field field : portlet.getChartData().getFields()) {
							fieldFilter = new Filter();
							fieldFilter.setColumn(field.getColumnName());
							if(!appliedCommonFilterSet.contains(fieldFilter)) {
								commonFilterFieldSet.add(field);
							}
						}
					}
				}
				constructFilterItem(commonFilterFieldSet);
			}
			if(LOG.isDebugEnabled()) {
				LOG.debug("Common Filters applied -> " + appliedCommonFilterSet);
				LOG.debug("Set of Columns -> " + commonFilterFieldSet);
			}
		}
	};
	
	/**
	 * Removes the filter from specified row. Updates portlet objects
	 *  
	 * @param rowToRemove
	 * 	Row to be detached
	 * @return
	 * 	A set of portlets for those, charts has to be refreshed and changes to be saved in DB
	 */
	Set<Portlet> removeFilter(Row rowToRemove) {
		Set<Portlet> portletsToRefresh = new HashSet<Portlet>();
		Filter filter = (Filter) rowToRemove.getAttribute(Constants.FILTER);
		Field field = (Field) rowToRemove.getAttribute(Constants.FIELD);
		for (Portlet portlet : dashboard.getPortletList()) {
			if (!Constants.TREE_LAYOUT.equals(portlet.getChartType()) &&
					Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())
					&& portlet.getChartData().getIsFiltered()) {
				// removing global filter object from filter list
				if (portlet.getChartData().getFilterSet().contains(filter)) {
					portlet.getChartData().getFilterSet().remove(filter);
					if (portlet.getChartData().getFilterSet().size() < 1) {
						portlet.getChartData().setIsFiltered(false);
					}
				}
				
				portletsToRefresh.add(portlet);
			}
		}	
		return portletsToRefresh;
	}
	
	/**
	 * Method to remove global filter row in UI
	 * @param rowToRemove
	 */
	private void removeFilterRow(Row rowToRemove){
		
		Filter filter = (Filter) rowToRemove.getAttribute(Constants.FILTER);
		Field field = (Field) rowToRemove.getAttribute(Constants.FIELD);
		// removing global filter object from session filter list
				if(appliedCommonFilterSet.remove(filter)) {
					//Adding to the list of columns
					commonFilterFieldSet.add(field);
					Listitem listitem = new Listitem(field.getColumnName());
					listitem.setAttribute(Constants.FIELD, field);
					listitem.setParent(commonFilterList);			
				}				
				rowToRemove.detach();
		}
	
	
	@Listen("onPortalMove = portallayout")
	public void onPanelMove(final PortalMoveEvent event) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("onPanelMove");
		}
		final ChartPanel panel = (ChartPanel) event.getDragged();
		if(panel.drawD3Graph() != null)
			Clients.evalJavaScript(panel.drawD3Graph());
		
		manipulatePortletObjects(Constants.ReorderPotletPanels);
		manipulatePortletObjects(Constants.ResizePotletPanels);
		
		//Updating new widget sequence to DB
		try {
			widgetService.updateWidgetSequence(dashboard);
		} catch (Exception e) {
			Clients.showNotification(Labels.getLabel("errorOnUpdatingWidgetDetails"), "error", this.getSelf(), "middle_center", 3000, true);
			LOG.error("Exception in onPanelMove()", e);
		}
	}
	
	
	public void onNameChange() {
		nameLabel.setValue(dashboard.getName());
		
		final Navbar navBar=(Navbar)Sessions.getCurrent().getAttribute(Constants.NAVBAR);
		final List<Component> childNavBars = navBar.getChildren(); 
		Integer navDashId=0;Navitem dashBoardObj=null;
        for (final Component childNavBar : childNavBars) {
        	if(childNavBar instanceof Navitem){
        		dashBoardObj = (Navitem) childNavBar;
        		navDashId =  (Integer) dashBoardObj.getAttribute(Constants.DASHBOARD_ID);
        		if(dashboard.getDashboardId().equals(navDashId))
        		{
        			dashBoardObj.setLabel(dashboard.getName());
        			break;
        		}
        	}
        }
	}
	
	/**
	 * deleteDashboard() is used to delete the selected Dashboard in the sidebar page.
	 */
	@Listen("onClick = #deleteDashboard")
	public void deleteDashboard() {
		try{
		 // ask confirmation before deleting dashboard
		 EventListener<ClickEvent> clickListener = new EventListener<Messagebox.ClickEvent>() {
			 public void onEvent(ClickEvent event) {
	             
				 if(Messagebox.Button.YES.equals(event.getButton())) {
	            	final Navbar navBar  = (Navbar) Selectors.iterable(DashboardController.this.getSelf().getPage(), "navbar").iterator().next();
	           		
	            	navBar.getSelectedItem().setVisible(false);
	           		
	           		final Include include = (Include) Selectors.iterable(DashboardController.this.getSelf().getPage(), "#mainInclude")
	           				.iterator().next();
	           		List<Integer> dashboardIdList = new ArrayList<Integer>();
	           		
	           		if(LOG.isDebugEnabled()){
	           			LOG.debug("Setting first visible Nav item as active");
	           		}
	           		
	           		Navitem navitem;
	           		Boolean isSelected = false;
	           		for (Component component : navBar.getChildren()) {
	           			navitem = (Navitem) component;
	           			if(navitem.isVisible()){
	           				//Adding visible items to list
	           				dashboardIdList.add((Integer) navitem.getAttribute(Constants.DASHBOARD_ID));
	           				
	           				//Selecting first visible Item
	           				if(!isSelected){
	           					navitem.setSelected(true);
	           					Events.sendEvent(Events.ON_CLICK, navitem, null);
	           					isSelected = !isSelected;
	           				}
	           			}
	           		}
	           		
	           		if( !isSelected ) {
	           			Sessions.getCurrent().setAttribute(Constants.ACTIVE_DASHBOARD_ID, null);
	           			//Detaching the include and Including the page again to trigger reload
	           			final Component component2 = include.getParent();
	           			include.detach();
	           			final Include newInclude = new Include("/demo/layout/dashboard.zul");
	           			newInclude.setId("mainInclude");
	           			component2.appendChild(newInclude);
	           			Clients.evalJavaScript("showPopUp()");
	           		}	           		
	           		dashboardService.deleteDashboard(dashboard.getDashboardId(),authenticationService.getUserCredential().getUserId());
	           		dashboardService.updateSidebarDetails(dashboardIdList);
	             }

	           } 
	       };
	       
       Messagebox.show(Constants.DELETE_DASHBOARD, Constants.DELETE_DASHBOARD_TITLE, new Messagebox.Button[]{
               Messagebox.Button.YES, Messagebox.Button.NO }, Messagebox.QUESTION, clickListener);
		}catch(DataAccessException ex){
			Clients.showNotification(Labels.getLabel("unableToDeleteDashboard"), "error", this.getSelf(), "middle_center", 3000, true);
			LOG.error("Exception while deleting Dashboard in DashboardController", ex);
			return;
		}catch(Exception ex){
			Clients.showNotification(Labels.getLabel("unableToDeleteDashboard"), "error", this.getSelf(), "middle_center", 3000, true);
			LOG.error("Exception while deleting Dashboard in DashboardController", ex);
			return;			
		}
  }
	
	
}

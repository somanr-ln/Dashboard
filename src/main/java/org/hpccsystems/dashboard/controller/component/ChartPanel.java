package org.hpccsystems.dashboard.controller.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.HpccConnection;
import org.hpccsystems.dashboard.entity.chart.TreeData;
import org.hpccsystems.dashboard.entity.chart.utils.ChartRenderer;
import org.hpccsystems.dashboard.entity.chart.utils.TableRenderer;
import org.hpccsystems.dashboard.services.AuthenticationService;
import org.hpccsystems.dashboard.services.HPCCService;
import org.hpccsystems.dashboard.services.WidgetService;
import org.springframework.dao.DataAccessException;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;


/**
 * ChartPanel class is used to create,edit and delete the Dashboard portlet's.
 *
 */
public class ChartPanel extends Panel {

	private static final  Log LOG = LogFactory.getLog(ChartPanel.class);
	private static final long serialVersionUID = 1L;	
	
	private static final String ADD_STYLE = "glyphicon glyphicon-plus btn btn-link img-btn";
	private static final String EDIT_STYLE = "glyphicon glyphicon-cog btn btn-link img-btn";
	private static final String RESET_STYLE = "glyphicon glyphicon-repeat btn btn-link img-btn";
	private static final String DELETE_STYLE = "glyphicon glyphicon-trash btn btn-link img-btn";
	
	final Button addBtn = new Button();
	final Button resetBtn = new Button();
	final Button deleteBtn = new Button();
	final Div holderDiv = new Div();
	final Div chartDiv = new Div();
	final Textbox textbox = new Textbox();	
	final Box imageContainer = new Box();	
	final Combobox treetextBox = new Combobox();
	Portlet portlet;

	public ChartPanel(final Portlet argPortlet) {
		this.setZclass("panel");
		this.imageContainer.setVflex("1");
		this.imageContainer.setHflex("1");
		this.imageContainer.setAlign("center");
		this.imageContainer.setPack("center");
		
		this.portlet = argPortlet;
		
		this.setBorder("normal");
		this.setWidth("99%");
		this.setStyle("margin-bottom:5px");

		// Creating title bar for the panel
		final Caption caption = new Caption();
		caption.setWidth("100%");

		final Div div = new Div();
		div.setStyle("padding:0");

		final Hbox hbox = new Hbox();
		hbox.setPack("stretch");
		hbox.setWidth("100%");

		textbox.setInplace(true);
		textbox.setVflex("1");
		textbox.setStyle("border: none;	color: black;");
		if(portlet.getName() != null){
			textbox.setValue(portlet.getName());			
		} else {
			Session session = Sessions.getCurrent();
			String lang = (String)session.getAttribute("lang");
			if(lang!=null && lang.equalsIgnoreCase("Chinese")){
			textbox.setValue(Labels.getLabel("chartTitle"));
			}else{
				textbox.setValue("Chart Title");
			}
		}
		textbox.setWidth("250px");
		textbox.setMaxlength(30);
		textbox.addEventListener(Events.ON_CHANGE, titleChangeLisnr);

		final Toolbar toolbar = new Toolbar();
		toolbar.setAlign("end");
		toolbar.setStyle("float:right; border-style: none;");

		//resetBtn.setLabel("Delete");
		resetBtn.setSclass(RESET_STYLE);

		//deleteBtn.setLabel("Delete");
		deleteBtn.setSclass(DELETE_STYLE);
		deleteBtn.addEventListener(Events.ON_CLICK, deleteListener);

		toolbar.appendChild(addBtn);
		AuthenticationService authenticationService = (AuthenticationService)SpringUtil.getBean("authenticationService");
		if(!Constants.CIRCUIT_APPLICATION_ID.equals(authenticationService.getUserCredential().getApplicationId())){
			toolbar.appendChild(resetBtn);
			toolbar.appendChild(deleteBtn);
		}

		hbox.appendChild(textbox);
		hbox.appendChild(toolbar);

		div.appendChild(hbox);
		caption.appendChild(div);
		this.appendChild(caption);

		// Creating panel contents
		final Panelchildren panelchildren = new Panelchildren();
		holderDiv.setHeight("385px");
		panelchildren.appendChild(holderDiv);
		this.appendChild(panelchildren);
		
		resetBtn.addEventListener(Events.ON_CLICK, resetListener);
		
		if(portlet.getWidgetState().equals(Constants.STATE_EMPTY)){
			addBtn.setSclass(ADD_STYLE);
			addBtn.addEventListener(Events.ON_CLICK, addListener);
			resetBtn.setDisabled(true);
		}else if(portlet.getWidgetState().equals(Constants.STATE_LIVE_CHART)){
			addBtn.setSclass(EDIT_STYLE);
			addBtn.addEventListener(Events.ON_CLICK, editListener);
			resetBtn.setDisabled(false);
			createChartHolder();
			//To construct Table Widget
			if(portlet.getChartType().equals(Constants.TABLE_WIDGET)){
				drawTableWidget();
			}
			else{
				drawD3Graph();
			}	
		} else if(portlet.getWidgetState().equals(Constants.STATE_GRAYED_CHART)){
			//Only Static image is added
			setStaticImage();
			addBtn.setSclass(EDIT_STYLE);
			addBtn.addEventListener(Events.ON_CLICK, editListener);
			resetBtn.setDisabled(false);
		}
		
		chartDiv.setVflex("1");
	}
	
	/**
	 * Provides the java script to draw the graph
	 * @return
	 * Returns null if Chart is not drawn in the panel yet
	 */
	public final String drawD3Graph() {
		if(!portlet.getWidgetState().equals(Constants.STATE_LIVE_CHART)){
			return null;
		}	
		if(portlet.getChartType().equals(Constants.BAR_CHART) || 
				portlet.getChartType().equals(Constants.LINE_CHART)){
			return "createChart('" + chartDiv.getId() +  "','"+ portlet.getChartDataJSON() +"')" ;
		}else if(Constants.TREE_LAYOUT.equals(portlet.getChartType())){
			return "drawTreeLayout('" +chartDiv.getId()+  "','"+ portlet.getChartDataJSON() +"')" ;
		}else {
			return "createPieChart('" + chartDiv.getId() +  "','"+ portlet.getChartDataJSON() +"')" ;
		}
	}
	
	//To construct Table Widget
	public void drawTableWidget(){
		TableRenderer tableRenderer = (TableRenderer) SpringUtil.getBean("tableRenderer");
		Vbox vbox = tableRenderer.constructTableWidget(portlet, portlet.getChartData(), false);
		chartDiv.getChildren().clear();
		chartDiv.appendChild(vbox);
	}

	/**
	 * Adds static image
	 */
	private void setStaticImage() {
		createChartHolder();
		Image image = new Image();
		image.setSrc(Constants.CHART_MAP.get(portlet.getChartType()).getStaticImageURL());
		image.setSclass("img-responsive");
		imageContainer.appendChild(image);
		chartDiv.appendChild(imageContainer);
		portlet.setWidgetState(Constants.STATE_GRAYED_CHART);
	}
	
	/**
	 * Creates div for chart
	 */
	private void createChartHolder() {
		String divId;
		Integer seq = 0;
		if(Sessions.getCurrent().getAttribute("divSeq") != null){
			seq = (Integer) Sessions.getCurrent().getAttribute("divSeq");
			if(LOG.isDebugEnabled()){
				LOG.debug("Seq present in Session --> " + seq);
			}
		}
		seq += 1;
		Sessions.getCurrent().setAttribute("divSeq", seq);
		
		if(portlet.getWidgetState().equals(Constants.STATE_EMPTY)) {
			divId = "chartDiv";
		}	
		else{
			divId = "chartDivOld";
		}	
		
		divId = divId + seq;
		
		chartDiv.setId(divId);
		holderDiv.appendChild(chartDiv);
	}

	// Defining and adding event listener to 'Add' button
	EventListener<Event> addListener = new EventListener<Event>() {

		public void onEvent(final Event event) throws Exception {
			// Defining parameters to send to Modal Dialog
			final Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put(Constants.PARENT, ChartPanel.this);
			parameters.put(Constants.PORTLET, portlet);

			final Window window = (Window) Executions.createComponents(
					"/demo/add_widget.zul", holderDiv, parameters);
			window.doModal();
		}

	};
	
	//Defining event listener to 'Edit' button in the portlet
	EventListener<Event> editListener = new EventListener<Event>() {
		public void onEvent(final Event event) throws Exception {
			// Defining parameters to send to Modal Dialog
			final Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put(Constants.PARENT, ChartPanel.this);
			parameters.put(Constants.PORTLET, portlet);
			if(Constants.TREE_LAYOUT == portlet.getChartType() 
					&& Constants.STATE_GRAYED_CHART.equals(portlet.getWidgetState())){
				onTreeInclude();
			}else{
			final Window window = (Window) Executions.createComponents(
					"/demo/layout/edit_portlet.zul", holderDiv, parameters);
			window.doModal();
			}
		}

	};

	//Reset button listener
	EventListener<Event> resetListener = new EventListener<Event>() { 
        public void onEvent(final Event event)throws Exception {
        	portlet.setWidgetState(Constants.STATE_EMPTY);
        	portlet.setChartDataJSON(null);
        	portlet.setChartDataXML(null);
        	portlet.setChartType(null);
        	portlet.setName(null);
        	
        	Components.removeAllChildren(chartDiv);
        	Components.removeAllChildren(imageContainer);
        	chartDiv.detach();
        	
        	addBtn.setSclass(ADD_STYLE);
        	resetBtn.setDisabled(true);
        	addBtn.removeEventListener(Events.ON_CLICK, editListener);
    		addBtn.addEventListener(Events.ON_CLICK, addListener);
    		
    		//Clears all chart data from DB
    		WidgetService widgetService =(WidgetService) SpringUtil.getBean("widgetService");
    		widgetService.updateWidget(portlet);
    		
    		//Calling listener in Dashboard - This listener resets portlet object
    		Window window =  null;
    		Session session = Sessions.getCurrent();
			final ArrayList<Component> list = (ArrayList<Component>) Selectors.find(((Component)session.getAttribute(Constants.NAVBAR)).getPage(), "window");
			for (final Component component : list) {
				if(component instanceof Window){
					window = (Window) component;
					Events.sendEvent(new Event("onPanelReset", window, portlet));
				}
			}
			
        } 
	};
	
	//Delete panel listener
	EventListener<Event> deleteListener = new EventListener<Event>() {

		public void onEvent(final Event event)throws Exception  {
			try{
			WidgetService widgetService = (WidgetService) SpringUtil.getBean("widgetService");
			widgetService.deleteWidget(portlet.getId());
			ChartPanel.this.detach();
			
			Window window =  null;
			Session session = Sessions.getCurrent();
			final ArrayList<Component> list = (ArrayList<Component>) Selectors.find(((Component)session.getAttribute(Constants.NAVBAR)).getPage(), "window");
			for (final Component component : list) {
				if(component instanceof Window){
					window = (Window) component;
					Events.sendEvent(new Event("onPortalClose", window, portlet));
				}
			}
			}catch(DataAccessException ex){
				LOG.error("Exception while deleting widget", ex);
			}
		} 
	};
	
	//Event to create Static chart
	public void onCloseDialog(final Event event){
		final Map<String,Integer> paramMap = (Map<String, Integer>) event.getData();
		if(paramMap!=null){
			portlet.setChartType(paramMap.get(Constants.CHART_TYPE));
			setStaticImage();	
			addBtn.removeEventListener(Events.ON_CLICK, addListener);
			addBtn.setSclass(EDIT_STYLE);
			resetBtn.setDisabled(false);
			addBtn.addEventListener(Events.ON_CLICK, editListener);
		}
	}
	
	//Event Listener for Change of title text
	EventListener<Event> titleChangeLisnr = new EventListener<Event>() {
		public void onEvent(final Event event) throws Exception {
			if(LOG.isDebugEnabled()){
				LOG.debug("Title is being changed");
			}
			portlet.setName(textbox.getValue());
			//Update Chart Title in DB
			try{
			WidgetService widgetService =(WidgetService) SpringUtil.getBean("widgetService");
    		widgetService.updateWidgetTitle(portlet);
			}catch(DataAccessException ex){
				LOG.error("Exception while updating chart title", ex);
			}
		}
	};
	
	/**
	 * Method to enable Tree layout window
	 */
	public void onTreeInclude() {
		TreeData treeData = new TreeData();
		treeData.setHpccConnection(constructHpccObj());		
		portlet.setTreeData(treeData);
		constructTreeSearchDiv();		
	}
	
	private HpccConnection constructHpccObj(){
		HpccConnection hpccConnection = new HpccConnection(
				"216.19.105.2", 18010, "", "generic_dashboard",
				"Lexis123!", true, false);	
		return hpccConnection;
			
	}
	/**
	 * Method to create root key search div for Tree layout
	 */
	public void constructTreeSearchDiv(){
		Hbox hbox = new Hbox();
		Label searchLabel = new Label();
		searchLabel.setValue("Root Key :");
		Button searchButton = new Button();
		searchButton.setLabel("DrawTree");
		searchButton.addEventListener(Events.ON_CLICK, drawTreeListener);
		hbox.appendChild(searchLabel);
		getRootKeyList();		
		if(Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())){
			treetextBox.setValue(portlet.getTreeData().getRootKey());
		}
		hbox.appendChild(treetextBox);
		hbox.appendChild(searchButton);
		final Div treeDiv = new Div();
		treeDiv.appendChild(hbox);		
		holderDiv.getChildren().clear();
		holderDiv.appendChild(treeDiv);
		holderDiv.appendChild(chartDiv);
		holderDiv.setHeight("620px");
		removeStaticImage();
	}
			
	/**
	 * Method to generate Root Key List for TreeLayout Autocomplete function
	 */
	private void getRootKeyList() {
		try{
		HPCCService hpccService = (HPCCService)SpringUtil.getBean("hpccService");
		treetextBox.setAutodrop(true);
		treetextBox.setButtonVisible(false);
		treetextBox.setModel(new SimpleListModel(hpccService.getRootKeyList(portlet.getTreeData().getHpccConnection())));
		}catch(Exception ex){
			LOG.error("Exception while getting root key list", ex);
		}
		
	}

	/**
	 * Listener to draw tree layout
	 */
	EventListener<Event> drawTreeListener = new EventListener<Event>() {

		@Override
		public void onEvent(Event event) throws Exception {
			ChartRenderer chartRenderer = (ChartRenderer)SpringUtil.getBean("chartRenderer");
			String[] rootArray = treetextBox.getValue().split("\\s+");
			String treeJSON = chartRenderer.constructTreeJSON(rootArray[0], rootArray[1], portlet.getTreeData().getHpccConnection());
			portlet.setChartDataJSON(treeJSON);
			Clients.evalJavaScript("drawTreeLayout('" + chartDiv.getId()+  "','"+ portlet.getChartDataJSON() +"')" ); 
			portlet.setWidgetState(Constants.STATE_LIVE_CHART);
			WidgetService widgetService = (WidgetService)SpringUtil.getBean("widgetService");
			portlet.getTreeData().setRootKey(treetextBox.getValue());
			portlet.setChartDataXML(chartRenderer.convertTreeDataToXML(portlet.getTreeData()));
			widgetService.updateWidget(portlet);
		}
	};
	
	/**
	 * @return
	 * Portlet associated with the ChartPanel Object
	 */
	public Portlet getPortlet() {
		return portlet;
	}
	
	/**
	 * Detaches the static image attached to the Chartpanel and returns the chartDiv ID
	 * @return
	 * 	The div id to draw chaert on
	 */
	public Div removeStaticImage() {
		imageContainer.detach();
		return chartDiv;
	}
}

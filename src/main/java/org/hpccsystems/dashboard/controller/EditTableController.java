package org.hpccsystems.dashboard.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.api.entity.ChartConfiguration;
import org.hpccsystems.dashboard.api.entity.Field;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.Attribute;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.entity.chart.utils.TableRenderer;
import org.hpccsystems.dashboard.services.AuthenticationService;
import org.hpccsystems.dashboard.services.DashboardService;
import org.hpccsystems.dashboard.services.HPCCService;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;


@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class EditTableController extends SelectorComposer<Component> {
	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(EditTableController.class);

	@Wire
	Listbox sourceList, targetList;
	
	@Wire
	Div tableHolder;
	
	@WireVariable
	AuthenticationService  authenticationService;
	
	@WireVariable
	private DashboardService dashboardService;
	
	@WireVariable
	TableRenderer tableRenderer;
	
	@WireVariable
	HPCCService hpccService;

	private XYChartData tableData;
	private Portlet portlet;
	private Button doneButton;
	
	@Override
	public void doAfterCompose(final Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		portlet = (Portlet) Executions.getCurrent().getAttribute(Constants.PORTLET);
		tableData = (XYChartData) Executions.getCurrent().getAttribute(Constants.CHART_DATA);
		doneButton = (Button) Executions.getCurrent().getAttribute(Constants.EDIT_WINDOW_DONE_BUTTON);
		
		sourceList.addEventListener(Events.ON_DROP, dropListener);
		targetList.addEventListener(Events.ON_DROP, dropListener);
		
		Set<Field> columnSet ;
		if(authenticationService.getUserCredential().hasRole(Constants.CIRCUIT_ROLE_CONFIG_CHART)) {
			ChartConfiguration configuration = (ChartConfiguration) Executions.getCurrent().getAttribute(Constants.CIRCUIT_CONFIG);
			columnSet = new HashSet<Field>();
			for (Field field : configuration.getFields()) {
				columnSet.add(field);
			}
		} else {
			columnSet = hpccService.getColumnSchema(tableData.getFileName(), tableData.getHpccConnection());
		}
		
		if(Constants.CIRCUIT_APPLICATION_ID.equals(authenticationService.getUserCredential().getApplicationId())) {
			try {
				Set<Field> schemaSet = hpccService.getColumnSchema(tableData.getFileName(), tableData.getHpccConnection());
				for (Attribute column : tableData.getTableColumns()) {
					boolean columnExist = false;
					for(Field field : schemaSet){
						if(column.getColumnName().equals(field.getColumnName().trim())){
							columnExist =true;
							break;
						}
					}
					if(!columnExist){
						throw new Exception("Column doesn't exist");
					}
				}
				tableHolder.appendChild(tableRenderer.constructTableWidget(portlet, tableData, true));
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
			
		}
		
		//Setting fields to ChartData
		List<Field> fields = new ArrayList<Field>();
		fields.addAll(columnSet);
		tableData.setFields(fields);
		
		
		Listitem listItem;
		if(Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())) {
			for (Field field : columnSet) {
				listItem = new Listitem(field.getColumnName());
				listItem.setDraggable("true");
				listItem.setDroppable("true");
				System.out.println("field.getColumnName()========" +field.getColumnName());
				System.out.println("tableData.getTableColumns()============" +tableData.getTableColumns());
				if(tableData.getTableColumns().contains(field.getColumnName())) {
					System.out.println("targetList========" +targetList);
					listItem.setParent(targetList);
				} else {
					System.out.println("sourceList=========" +sourceList);
					listItem.setParent(sourceList);
				}
			}
			
			//TODO: Add else part
			tableHolder.appendChild(
						tableRenderer.constructTableWidget(portlet, tableData, true)
					);
		} else { 
			for (Field field : columnSet) {
				System.out.println("Inside for loop");
				listItem = new Listitem(field.getColumnName());
				listItem.setDraggable("true");
				listItem.setDroppable("true");
				listItem.setParent(sourceList);
			}
		}
	}
		
	/**
	 * Common Drop event listener for both listbox and listitem
	 */
	private EventListener<DropEvent> dropListener = new EventListener<DropEvent>() {

		public void onEvent(DropEvent event) throws Exception {
			Component dragged = event.getDragged();
			Component dropped = event.getTarget();
			Listitem draggedItem = (Listitem) dragged;
			Listitem newListItem = new Listitem();
			newListItem.setDraggable("true");
			if (draggedItem.getAttribute(Constants.ATTRIBUTE) != null && sourceList.equals(dropped)) {
				Attribute attribute = (Attribute) draggedItem.getAttribute(Constants.ATTRIBUTE);
				newListItem.setLabel(attribute.getColumnName());
				newListItem.setParent(sourceList);
			} else if (targetList.equals(dropped)) {
				Listcell listCell = new Listcell();
				Attribute attribute = new Attribute();
				attribute.setColumnName(draggedItem.getLabel());
				newListItem.setAttribute(Constants.ATTRIBUTE, attribute);
				Textbox textBox = new Textbox();
				textBox.setInplace(true);
				textBox.setValue(draggedItem.getLabel());
				textBox.addEventListener(Events.ON_CHANGE, titleChangeLisnr);
				textBox.setParent(listCell);
				listCell.setParent(newListItem);
				newListItem.setParent(targetList);
			}
			draggedItem.detach();
			if (Constants.CIRCUIT_APPLICATION_ID.equals(authenticationService.getUserCredential().getApplicationId())
					&& targetList.getChildren().size() > 1)
				doneButton.setDisabled(false);
			else
				doneButton.setDisabled(true);
			// Code to update the selected columns since the draw table is not  applicaple for circuit config flow
			List<Attribute> selectedTableColumns = tableData.getTableColumns();
			Listitem listitem;
			selectedTableColumns.clear();
			for (Component component : targetList.getChildren()) {
				if (component instanceof Listitem) {
					listitem = (Listitem) component;
					selectedTableColumns.add(new Attribute(listitem.getLabel()));
				}
			}
		}
	};
	
	// Event Listener for Change of tableColumn title text
		private EventListener<Event> titleChangeLisnr = new EventListener<Event>() {
			public void onEvent(final Event event) throws Exception {
				Listitem listItem = (Listitem) event.getTarget().getParent().getParent();
				Attribute attribute = (Attribute) listItem.getAttribute(Constants.ATTRIBUTE);
				Textbox textBox = (Textbox) event.getTarget();
				if (LOG.isDebugEnabled()) {
					LOG.debug("TableColumn Title is being changed");
				}
				attribute.setDisplayXColumnName(textBox.getValue());
				tableData.getTableColumns().add(attribute);
			}
		};
	
	
	@Listen("onClick = #drawTable")
	public void drawTable() {
		tableData.getTableColumns().clear();
		if (targetList.getChildren().size() > 1) {
			tableData.getTableColumns().clear();
			Listitem listitem;
			for (Component component : targetList.getChildren()) {
				if (component instanceof Listitem) {
					listitem = (Listitem) component;
					Attribute attribute = (Attribute) listitem.getAttribute(Constants.ATTRIBUTE);
					tableData.getTableColumns().add(attribute);
				}
			}
			try {

				tableHolder.getChildren().clear();
				tableHolder.appendChild(tableRenderer.constructTableWidget(portlet, tableData, true));
			} catch (Exception e) {
				Clients.showNotification(Labels.getLabel("tableCreationFailed"), "error", tableHolder, "middle_center", 3000, true);
				LOG.error("Table creation failed", e);
				return;
			}
			doneButton.setDisabled(false);
		} else {
			Clients.showNotification(Labels.getLabel("moveSomeColumn"),
					"error", targetList, "middle_center", 3000, true);
		}
	}
}

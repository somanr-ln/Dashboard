package org.hpccsystems.dashboard.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.api.entity.ChartConfiguration;
import org.hpccsystems.dashboard.api.entity.Field;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.entity.chart.utils.TableRenderer;
import org.hpccsystems.dashboard.services.AuthenticationService;
import org.hpccsystems.dashboard.services.DashboardService;
import org.hpccsystems.dashboard.services.HPCCService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
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
import org.zkoss.zul.Listitem;

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
		
		Map<String,String> columnSchemaMap;
		if(authenticationService.getUserCredential().hasRole(Constants.CIRCUIT_ROLE_CONFIG_CHART)) {
			ChartConfiguration configuration = (ChartConfiguration) Executions.getCurrent().getAttribute(Constants.CIRCUIT_CONFIG);
			columnSchemaMap = new HashMap<String, String>();
			for (Field field : configuration.getFields()) {
				columnSchemaMap.put(field.getColumnName(), field.getDataType());
			}
		} else {
			columnSchemaMap = hpccService.getColumnSchema(tableData.getFileName(), tableData.getHpccConnection());
		}
		
		if(Constants.CIRCUIT_APPLICATION_ID.equals(authenticationService.getUserCredential().getApplicationId())) {
			try {
				Map<String,String> schemaMap = hpccService.getColumnSchema(tableData.getFileName(), tableData.getHpccConnection());
				for (String column : tableData.getTableColumns()) {
					if(!schemaMap.containsKey(column)){
						throw new Exception("Column doesn't exist");
					}
				}
				tableHolder.appendChild(tableRenderer.constructTableWidget(portlet, true));
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
			
		}
		
		Listitem listItem;
		if(Constants.STATE_LIVE_CHART.equals(portlet.getWidgetState())) {
			for (Map.Entry<String, String> entry : columnSchemaMap.entrySet()) {
				listItem = new Listitem(entry.getKey());
				listItem.setDraggable("true");
				listItem.setDroppable("true");
				listItem.addEventListener(Events.ON_DROP, dropListener);
				
				if(tableData.getTableColumns().contains(entry.getKey())) {
					listItem.setParent(targetList);
				} else {
					listItem.setParent(sourceList);
				}
			}
			
			//TODO: Add else part
			tableHolder.appendChild(
						tableRenderer.constructTableWidget(portlet, true)
					);
		} else {
			for (Map.Entry<String, String> entry : columnSchemaMap.entrySet()) {
				listItem = new Listitem(entry.getKey());
				listItem.setDraggable("true");
				listItem.setDroppable("true");
				listItem.addEventListener(Events.ON_DROP, dropListener);
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
			
			if(dropped instanceof Listitem) {
				dropped.getParent().insertBefore(dragged, dropped);
			} else {
				//When dropped in list box
				dropped.appendChild(dragged);
			}
			if(Constants.CIRCUIT_APPLICATION_ID.equals(authenticationService.getUserCredential().getApplicationId()) && targetList.getChildren().size() > 1)
				doneButton.setDisabled(false);
			else
				doneButton.setDisabled(true);
			//Code to update the selected columns since the draw table is not applicaple for circuit config flow
			List<String> selectedTableColumns=tableData.getTableColumns();
			Listitem listitem;
			selectedTableColumns.clear();
			for (Component component : targetList.getChildren()) {
				if(component instanceof Listitem){
					listitem = (Listitem) component;
					selectedTableColumns.add(
								listitem.getLabel()
							);
				}
			}
		}
		
	};
	
	@Listen("onClick = #drawTable")
	public void drawTable() {
		tableData.getTableColumns().clear();
		if(targetList.getChildren().size() > 1) {
			tableData.getTableColumns().clear();
			Listitem listitem;
			for (Component component : targetList.getChildren()) {
				if(component instanceof Listitem){
					listitem = (Listitem) component;
					tableData.getTableColumns().add(
								listitem.getLabel()
							);
				}
			}
			
			try {
				
				tableHolder.getChildren().clear();
				tableHolder.appendChild(
						tableRenderer.constructTableWidget(portlet, true)
						);
			} catch (Exception e) {
				Clients.showNotification("Table Creation failed. Please try again.", "error", tableHolder, "middle_center", 3000, true);
				LOG.error("Table creation failed", e);
				return;
			}
			
			doneButton.setDisabled(false);
		} else {
			Clients.showNotification("Move some columns over here to draw a Table", "error", targetList, "middle_center", 3000, true);
		}
	}

}

package org.hpccsystems.dashboard.util;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.controller.component.ChartPanel;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.utils.ChartRenderer;
import org.hpccsystems.dashboard.services.WidgetService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkmax.zul.Portalchildren;
import org.zkoss.zkplus.spring.SpringUtil;

public class DashboardHelper {
	
private static final  Log LOG = LogFactory.getLog(DashboardHelper.class); 
	
public void updateWidgets(Portlet portlet,List<Portalchildren> portalChildren) throws Exception{
		
		if(LOG.isDebugEnabled()){
			LOG.debug("Updating charts in portlet - " + portlet);
		}
		
		//Updating widget with latest filter details into DB
		ChartRenderer chartRenderer = (ChartRenderer) SpringUtil.getBean("chartRenderer");
		WidgetService widgetService = (WidgetService)SpringUtil.getBean("widgetService");
		portlet.setChartDataXML(chartRenderer.convertToXML(portlet.getChartData()));
		widgetService.updateWidget(portlet);
		
		//Refreshing chart with updated filter values
		chartRenderer.constructChartJSON(portlet.getChartData(), portlet, false);
		
		Portalchildren children = portalChildren.get(portlet.getColumn());
		LOG.debug("portalchildren in updateWidgets()-->"+children);
		ChartPanel panel =null;
		for (Component comp : children.getChildren()) {
			panel = (ChartPanel) comp;
			if (panel.getPortlet().getId() == portlet.getId()) {
				if (panel.drawD3Graph() != null) {
					Clients.evalJavaScript(panel.drawD3Graph());
				}
			}
		}
	}

}

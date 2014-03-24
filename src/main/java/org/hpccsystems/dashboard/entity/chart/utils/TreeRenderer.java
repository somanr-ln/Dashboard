package org.hpccsystems.dashboard.entity.chart.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.HpccConnection;
import org.hpccsystems.dashboard.entity.chart.TreeData;
import org.hpccsystems.dashboard.services.HPCCService;
import org.hpccsystems.dashboard.services.WidgetService;
import org.hpccsystems.dashboard.util.DashboardUtil;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.SimpleListModel;

public class TreeRenderer {
	private static final  Log LOG = LogFactory.getLog(TreeRenderer.class);
	

	Portlet portlet;
	
	public Portlet getPortlet() {
		return portlet;
	}

	/**
	 * Method to enable Tree layout window
	 */
	public Portlet drawLiveTree(Portlet portletpassed) {
		this.portlet = portletpassed;
		TreeData treeData = new TreeData();
		treeData.setHpccConnection(new DashboardUtil().constructHpccObj());		
		portlet.setTreeData(treeData);
		return portlet;
	}	
			
	/**
	 * Method to generate Root Key List for TreeLayout Autocomplete function
	 */
	public Combobox getRootKeyList(Combobox treetextBox) {
		try{
			HPCCService hpccService = (HPCCService)SpringUtil.getBean("hpccService");
			treetextBox.setAutodrop(true);
			treetextBox.setButtonVisible(false);
			
			treetextBox.setModel(new SimpleListModel<Object>(hpccService.getRootKeyList(portlet.getTreeData().getHpccConnection())){
				private static final long serialVersionUID = 1L;

				@Override
				public ListModel<Object> getSubModel(Object arg0, int arg1) {
					String str = (String) arg0;
					str = str.toUpperCase();
					return super.getSubModel(str, arg1);
				}
			});
		}catch(Exception ex){
			LOG.error("Exception while getting root key list", ex);
		}
		return treetextBox;
		
	}

}

/* 
	Description:
		ZK Essentials
	History:
		Created by dennis

Copyright (C) 2012 Potix Corporation. All Rights Reserved.
*/
package org.hpccsystems.dashboard.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.entity.Application;
import org.hpccsystems.dashboard.services.ApplicationService;
import org.hpccsystems.dashboard.services.AuthenticationService;
import org.hpccsystems.dashboard.services.DashboardService;
import org.zkoss.util.Locales;
import org.zkoss.util.resource.Labels;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;


/**
 * LoginController class is used to handle the login activities for Dashboard project
 *  and controller class for login.zul.
 *
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class LoginController extends SelectorComposer<Component> {
	
	private static final  Log LOG = LogFactory.getLog(LoginController.class);
	
	private static final long serialVersionUID = 1L;
	
	@WireVariable
	private DashboardService dashboardService;
	
	//wire components
	
	@Wire
	Listbox language;
	@Wire Listitem listItemEnglish,listItemChinese;
	@Wire
	Textbox account;
	@Wire
	Textbox password;
	@Wire
	Label message;
	@Wire
	Listbox apps;
	
	@Wire
	Button login;
	@WireVariable
	AuthenticationService  authenticationService;
	
	@WireVariable
	ApplicationService applicationService;
	Session session = Sessions.getCurrent();
	
	@Override
	public void doAfterCompose(final Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		//setting default language as English
		String lang = (String)session.getAttribute("lang");
		if(lang!=null && lang.equalsIgnoreCase("Chinese")){
			listItemChinese.setSelected(true);
			listItemChinese.setValue("Chinese");
		}
		else{
			listItemEnglish.setSelected(true);
			listItemEnglish.setValue("English");
		}
		//Redirecting if the user is already logged in.
		if(!authenticationService.getUserCredential().isAnonymous()) {
			Executions.sendRedirect("/demo/");
			return;
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Handling 'doAfterCompose' in LoginController");
			LOG.debug("dashboardService:loginctrler -->"+dashboardService);
		}
		try	{			
			final List<Application> applicationList = new ArrayList<Application>(applicationService.retrieveApplicationIds());
			final ListModelList<Application> appModel = new ListModelList<Application>(applicationList);
			apps.setModel(appModel);
		} catch(Exception ex) {
			Clients.showNotification(Labels.getLabel("unableToRetrieveApplications"), false);
			LOG.error("Exception while fetching applications from DB", ex);
		}
	}
	
	
	// For Internalization

	@Listen("onSelect=#language")
	public void doSelect() {
		String lang = language.getSelectedItem().getValue();
		if (lang.equalsIgnoreCase("English")) {
			session.setAttribute("lang", "English");
			changeLocale("en");
		} else {
			session.setAttribute("lang", "Chinese");
			changeLocale("zh");
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Current Language" + lang);
		}
	}

	private void changeLocale(String locale) {
		Session session = Sessions.getCurrent();
		Locale preferredLocale = Locales.getLocale(locale);
		session.setAttribute(Attributes.PREFERRED_LOCALE, preferredLocale);
		Executions.sendRedirect(null);
	}

	@Listen("onClick=#login; onOK=#loginWin")
	public void doLogin() {
		Boolean isLoginSuccessful = false;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Handling 'doLogin' in LoginController");
		}

		final String name = account.getValue();
		final String passWord = password.getValue();
		try {
			isLoginSuccessful = authenticationService.login(name, passWord,
					apps.getSelectedItem().getValue().toString());
			LOG.debug("User authenticated sucessfully.." + isLoginSuccessful);
		} catch (Exception ex) {
			Clients.showNotification(Labels.getLabel("loginFailed"), false);
			LOG.error("Exception while authendicating user in doLogin()", ex);
		}

		if (!isLoginSuccessful) {
			message.setValue("Username or Password are not correct.");
			return;
		} else {
			message.setValue("Welcome " + name);
		}

		LOG.debug("Loged in. sending redirect...");
		Executions.sendRedirect("/demo/");
	}
	
}

package com.fletch22.app.designer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fletch22.app.designer.app.AppService;
import com.fletch22.app.designer.appContainer.AppContainerService;
import com.fletch22.app.designer.layout.LayoutService;
import com.fletch22.app.designer.layoutMinion.LayoutMinionService;
import com.fletch22.app.designer.page.PageService;
import com.fletch22.app.designer.webFolder.WebFolderService;
import com.fletch22.app.designer.website.WebsiteService;

@Component
public class ServiceJunction {

	@Autowired
	public AppContainerService appContainerService;
	
	@Autowired
	public AppService appService;
	
	@Autowired
	public WebsiteService websiteService;
	
	@Autowired
	public WebFolderService webFolderService;
	
	@Autowired
	public PageService pageService;
	
	@Autowired
	public LayoutService layoutService;
	
	@Autowired
	public LayoutMinionService layoutMinionService;
	
//	@Autowired
//	HeadService headService;
	
//	@Autowired
//	BodyService bodyService;
}

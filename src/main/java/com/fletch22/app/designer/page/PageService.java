package com.fletch22.app.designer.page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fletch22.app.designer.DomainService;

@Component
public class PageService extends DomainService {
	
	@Autowired
	PageDao pageDao;

	public void addToParent(Page page, PageChild pageChild) {
		connectParentAndChild(page, pageChild);
		save(page);
	}

	public Page createInstance(String pageName) {
		Page page = new Page();
		page.pageName = pageName;
		return save(page);
	}
	
	public Page save(Page page) {
		return pageDao.save(page);
	}

	public Page get(long orbInternalId) {
		return pageDao.read(orbInternalId);
	}
}

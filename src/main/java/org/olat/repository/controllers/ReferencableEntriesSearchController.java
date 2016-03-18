/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.repository.controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.dropdown.Dropdown;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.Roles;
import org.olat.fileresource.types.BlogFileResource;
import org.olat.fileresource.types.ImsCPFileResource;
import org.olat.fileresource.types.PodcastFileResource;
import org.olat.fileresource.types.ScormCPFileResource;
import org.olat.fileresource.types.VideoFileResource;
import org.olat.fileresource.types.WikiResource;
import org.olat.group.BusinessGroupModule;
import org.olat.ims.qti.fileresource.SurveyFileResource;
import org.olat.ims.qti.fileresource.TestFileResource;
import org.olat.portfolio.EPTemplateMapResource;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;
import org.olat.repository.controllers.RepositorySearchController.Can;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.repository.ui.RepositoryTableModel;
import org.olat.repository.ui.author.CreateRepositoryEntryController;
import org.olat.repository.ui.author.ImportRepositoryEntryController;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Description: Implements a repository entry search workflow
 *         used by OLAT authors. Entries can be found that are either owned by
 *         the user or visible to authors in conjunction with the canReference
 *         flag.
 * @author gnaegi        
 * Initial Date: Aug 17, 2006
 * 
 */
public class ReferencableEntriesSearchController extends BasicController {
	
	public static final Event EVENT_REPOSITORY_ENTRY_SELECTED = new Event("event.repository.entry.selected");
	public static final Event EVENT_REPOSITORY_ENTRIES_SELECTED = new Event("event.repository.entries.selected");

	private static final String CMD_SEARCH_ENTRIES = "cmd.searchEntries";
	private static final String CMD_ADMIN_SEARCH_ENTRIES = "cmd.adminSearchEntries";
	private static final String CMD_ALL_ENTRIES = "cmd.allEntries";
	private static final String CMD_MY_ENTRIES = "cmd.myEntries";

	private VelocityContainer mainVC;
	private RepositorySearchController searchCtr;
	private String[] limitTypes;

	private SegmentViewComponent segmentView;
	private Link myEntriesLink, allEntriesLink, searchEntriesLink, adminEntriesLink;
	private Link importRessourceButton;
	private Component createRessourceCmp;
	private List<Link> createRessourceButtons;
	
	private CreateRepositoryEntryController createController;
	private ImportRepositoryEntryController importController;
	private CloseableModalController cmc;
	
	private RepositoryEntry selectedRepositoryEntry;
	private List<RepositoryEntry> selectedRepositoryEntries;
	
	private final boolean canImport;
	private final boolean canCreate;
	private final Can canBe;
	
	private Object userObject;
	
	@Autowired
	private RepositoryHandlerFactory repositoryHandlerFactory;

	public ReferencableEntriesSearchController(WindowControl wControl, UserRequest ureq, String limitType, String commandLabel) {
		this(wControl, ureq, new String[]{ limitType }, null, commandLabel, true, true, false, false, Can.referenceable);
	}
	
	public ReferencableEntriesSearchController(WindowControl wControl, UserRequest ureq, String[] limitTypes, String commandLabel) {
		this(wControl, ureq, limitTypes, null, commandLabel, true, true, false, false, Can.referenceable);
	}
	
	public ReferencableEntriesSearchController(WindowControl wControl, UserRequest ureq, String[] limitTypes, String commandLabel,
			boolean canImport, boolean canCreate, boolean multiSelect, boolean adminSearch) {
		this(wControl, ureq, limitTypes, null, commandLabel, canImport, canCreate, multiSelect, adminSearch, Can.referenceable);
	}

	public ReferencableEntriesSearchController(WindowControl wControl, UserRequest ureq,
			String[] limitTypes, RepositoryEntryFilter filter, String commandLabel,
			boolean canImport, boolean canCreate, boolean multiSelect, boolean adminSearch,
			Can canBe) {

		super(ureq, wControl);
		this.canBe = canBe;
		this.canImport = canImport;
		this.canCreate = canCreate;
		this.limitTypes = limitTypes;
		setBasePackage(RepositoryService.class);
		mainVC = createVelocityContainer("referencableSearch");
		
		if(limitTypes != null && limitTypes.length == 1 && limitTypes[0] != null) {
			mainVC.contextPut("titleCss", "o_icon o_" + limitTypes[0] + "_icon");
		}

		// add repo search controller
		searchCtr = new RepositorySearchController(commandLabel, ureq, getWindowControl(), false, multiSelect, limitTypes, filter);
		listenTo(searchCtr);
		
		// do instantiate buttons
		if (canCreate && isCreateButtonVisible()) {
			if(limitTypes != null && limitTypes.length == 1) {
				Link createButton = LinkFactory.createButtonSmall("cmd.create.ressource", mainVC, this);
				createButton.setElementCssClass("o_sel_repo_popup_create_resource");
				RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(limitTypes[0]);
				createButton.setUserObject(handler);
				createRessourceCmp = createButton;
			} else if(limitTypes != null && limitTypes.length > 1) {
				Dropdown dropdown = new Dropdown("cmd.create.ressource", "cmd.create.ressource", false, getTranslator());
				for(String limitType:limitTypes) {
					RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(limitType);
					Link createLink = LinkFactory.createLink(handler.getSupportedType(), getTranslator(), this);
					dropdown.addComponent(createLink);
				}
				createRessourceCmp = dropdown;
			}
		}
		if (canImport && isImportButtonVisible()) {
			importRessourceButton = LinkFactory.createButtonSmall("cmd.import.ressource", mainVC, this);
			importRessourceButton.setElementCssClass("o_sel_repo_popup_import_resource");
		}

		segmentView = SegmentViewFactory.createSegmentView("segments", mainVC, this);
		allEntriesLink = LinkFactory.createCustomLink(CMD_ALL_ENTRIES, CMD_ALL_ENTRIES, "referencableSearch." + CMD_ALL_ENTRIES, Link.LINK, mainVC, this);
		allEntriesLink.setElementCssClass("o_sel_repo_popup_all_resources");
		segmentView.addSegment(allEntriesLink, false);
		myEntriesLink = LinkFactory.createCustomLink(CMD_MY_ENTRIES, CMD_MY_ENTRIES, "referencableSearch." + CMD_MY_ENTRIES, Link.LINK, mainVC, this);
		myEntriesLink.setElementCssClass("o_sel_repo_popup_my_resources");
		segmentView.addSegment(myEntriesLink, true);
		searchEntriesLink = LinkFactory.createCustomLink(CMD_SEARCH_ENTRIES, CMD_SEARCH_ENTRIES, "referencableSearch." + CMD_SEARCH_ENTRIES, Link.LINK, mainVC, this);
		searchEntriesLink.setElementCssClass("o_sel_repo_popup_search_resources");
		segmentView.addSegment(searchEntriesLink, false);
		
		if(adminSearch && isAdminSearchVisible(limitTypes, ureq)) {
			adminEntriesLink = LinkFactory.createCustomLink(CMD_ADMIN_SEARCH_ENTRIES, CMD_ADMIN_SEARCH_ENTRIES, "referencableSearch." + CMD_ADMIN_SEARCH_ENTRIES, Link.LINK, mainVC, this);
			adminEntriesLink.setElementCssClass("o_sel_repo_popup_admin_search_resources");	
			segmentView.addSegment(adminEntriesLink, false);
		}

		searchCtr.doSearchByOwnerLimitType(ureq.getIdentity(), limitTypes);
		searchCtr.enableSearchforAllXXAbleInSearchForm(canBe);
		mainVC.put("searchCtr", searchCtr.getInitialComponent());
		
		putInitialPanel(mainVC);
	}
	
	public Object getUserObject() {
		return userObject;
	}

	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	/**
	 * Admin. search allow group managers to search all courses
	 * @param limitingTypes
	 * @param ureq
	 * @return
	 */
	private boolean isAdminSearchVisible(String[] limitingTypes, UserRequest ureq) {
		Roles roles = ureq.getUserSession().getRoles();
		return limitingTypes != null && limitingTypes.length == 1 && "CourseModule".equals(limitingTypes[0])
				&& (roles.isOLATAdmin() ||
						(roles.isInstitutionalResourceManager() && roles.isGroupManager()) ||
						(roles.isGroupManager()
								&& CoreSpringFactory.getImpl(BusinessGroupModule.class).isGroupManagersAllowedToLinkCourses()));
	}
	
	/**
	 * if building block can be imported, return true
	 * 
	 * @return
	 */
	private boolean isImportButtonVisible() {
		if(!canImport) return false;
		
		List<String> limitTypeList = Arrays.asList(limitTypes);
		
		String[] importAllowed = new String[] {
				TestFileResource.TYPE_NAME,
				WikiResource.TYPE_NAME,
				ImsCPFileResource.TYPE_NAME,
				ScormCPFileResource.TYPE_NAME,
				SurveyFileResource.TYPE_NAME,
				BlogFileResource.TYPE_NAME,
				PodcastFileResource.TYPE_NAME,
				VideoFileResource.TYPE_NAME
		};
		
		if (Collections.indexOfSubList(Arrays.asList(importAllowed), limitTypeList) != -1) { return true; }
		
		return false;
	}

	/**
	 * if building block can be created during choose-process, return true
	 * 
	 * @return
	 */
	private boolean isCreateButtonVisible() {
		if(!canCreate) return false;
		
		List<String> limitTypeList = Arrays.asList(limitTypes);
		
		String[] createAllowed = new String[] {
				TestFileResource.TYPE_NAME,
				WikiResource.TYPE_NAME,
				ImsCPFileResource.TYPE_NAME,
				SurveyFileResource.TYPE_NAME,
				BlogFileResource.TYPE_NAME,
				PodcastFileResource.TYPE_NAME,
				EPTemplateMapResource.TYPE_NAME
		};
		
		if (Collections.indexOfSubList(Arrays.asList(createAllowed), limitTypeList) != -1) { return true; }
		return false;
	}

	/**
	 * @return Returns the selectedEntry.
	 */
	public RepositoryEntry getSelectedEntry() {
		return selectedRepositoryEntry;
	}
	
	/**
	 * @return Returns the selected entries
	 */
	public List<RepositoryEntry> getSelectedEntries() {
		if(selectedRepositoryEntries == null && selectedRepositoryEntry != null) {
			return Collections.singletonList(selectedRepositoryEntry);
		}
		return selectedRepositoryEntries;
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(source == segmentView) {
			if(event instanceof SegmentViewEvent) {
				SegmentViewEvent sve = (SegmentViewEvent)event;
				String segmentCName = sve.getComponentName();
				Component clickedLink = mainVC.getComponent(segmentCName);
				if (clickedLink == myEntriesLink) {
					searchCtr.doSearchByOwnerLimitType(ureq.getIdentity(), limitTypes);
				} else if (clickedLink == allEntriesLink){
					switch(canBe) {
						case referenceable:
							searchCtr.doSearchForReferencableResourcesLimitType(ureq.getIdentity(), limitTypes, ureq.getUserSession().getRoles());
							break;
						case copyable:
							searchCtr.doSearchForCopyableResourcesLimitType(ureq.getIdentity(), limitTypes, ureq.getUserSession().getRoles());
							break;
						case all:
							searchCtr.doSearchByTypeLimitAccess(limitTypes, ureq);							
							break;
					}
				} else if (clickedLink == searchEntriesLink){
					searchCtr.displaySearchForm();
				} else if (clickedLink == adminEntriesLink) {
					searchCtr.displayAdminSearchForm();
				}
				mainVC.setDirty(true);
			}
		} else if(source == createRessourceCmp ||
				(createRessourceButtons != null && createRessourceButtons.contains(source))) {
			removeAsListenerAndDispose(cmc);
			removeAsListenerAndDispose(createController);
			RepositoryHandler handler = (RepositoryHandler)((Link)source).getUserObject();
			createController = new CreateRepositoryEntryController(ureq, getWindowControl(), handler);
			listenTo(createController);
			
			String title = translate(handler.getCreateLabelI18nKey());
			cmc = new CloseableModalController(getWindowControl(), translate("close"),
					createController.getInitialComponent(), true, title);
			cmc.setCustomWindowCSS("o_sel_author_create_popup");
			listenTo(cmc);
			
			cmc.activate();
		} else if (source == importRessourceButton) {
			
			removeAsListenerAndDispose(importController);
			importController = new ImportRepositoryEntryController(ureq, getWindowControl(), limitTypes);
			listenTo(importController);
			
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), importController.getInitialComponent(),
					true, "");
			listenTo(cmc);
			
			cmc.activate();
		}
	}

	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		String cmd = event.getCommand();
		if (source == searchCtr) {
			if (cmd.equals(RepositoryTableModel.TABLE_ACTION_SELECT_LINK)) {
				// done, user selected a repo entry
				selectedRepositoryEntry = searchCtr.getSelectedEntry();
				selectedRepositoryEntries = null;
				fireEvent(ureq, EVENT_REPOSITORY_ENTRY_SELECTED);
			} else if (cmd.equals(RepositoryTableModel.TABLE_ACTION_SELECT_ENTRIES)) {
				selectedRepositoryEntry = null;
				selectedRepositoryEntries = searchCtr.getSelectedEntries();
				fireEvent(ureq, EVENT_REPOSITORY_ENTRIES_SELECTED);
			}
			//initLinks();
		} else if (source == createController) { 
			if (event.equals(Event.DONE_EVENT)) {
				cmc.deactivate();
				
				selectedRepositoryEntry = createController.getAddedEntry();
				fireEvent(ureq, EVENT_REPOSITORY_ENTRY_SELECTED);
				// info message
				String message = translate("message.entry.selected", new String[] { selectedRepositoryEntry.getDisplayname(), selectedRepositoryEntry.getResourcename()});
				getWindowControl().setInfo(message);
			} else if (event.equals(Event.CANCELLED_EVENT)) {
				cmc.deactivate();
			} else if (event.equals(Event.FAILED_EVENT)) {
				showError("add.failed");
			}
		} else if (source == importController) { 
			if (event.equals(Event.DONE_EVENT)) {
				cmc.deactivate();
				
				selectedRepositoryEntry = importController.getImportedEntry();
				fireEvent(ureq, EVENT_REPOSITORY_ENTRY_SELECTED);
				// info message
				String message = translate("message.entry.selected", new String[] { selectedRepositoryEntry.getDisplayname(), selectedRepositoryEntry.getResourcename()});
				getWindowControl().setInfo(message);
			} else if (event.equals(Event.CANCELLED_EVENT)) {
				cmc.deactivate();
			} else if (event.equals(Event.FAILED_EVENT)) {
				showError("add.failed");
			}
		}
	}
	

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		//
	}
}

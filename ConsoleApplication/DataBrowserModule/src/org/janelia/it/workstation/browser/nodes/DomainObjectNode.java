package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.actions.ServiceAcceptorActionHelper;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.components.DomainListViewManager;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.components.ViewerUtils;
import org.janelia.it.workstation.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.browser.flavors.DomainObjectNodeFlavor;
import org.janelia.it.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.browser.nb_action.RemoveAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A domain object node in the graph, usually a Filter or Object Set. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DomainObjectNode<T extends DomainObject> extends AbstractNode implements Has2dRepresentation, HasIdentifier {

    private final static Logger log = LoggerFactory.getLogger(DomainObjectNode.class);

    private final ChildFactory<?> parentChildFactory;
    private final InstanceContent lookupContents;

    public DomainObjectNode(ChildFactory<?> parentChildFactory, Children children, T domainObject) {
        this(new InstanceContent(), parentChildFactory, children, domainObject);
    }

    public DomainObjectNode(InstanceContent lookupContents, ChildFactory<?> parentChildFactory, Children children, T domainObject) {
        super(children, new AbstractLookup(lookupContents));
        this.parentChildFactory = parentChildFactory;
        this.lookupContents = lookupContents;
        lookupContents.add(domainObject);
        DomainObjectNodeTracker.getInstance().registerNode(this);
    }
    
    public void update(T domainObject) {
        String oldName = getName();
        String oldDisplayName = getDisplayName();
        log.debug("Updating node with: {}",domainObject.getName());
        lookupContents.remove(getDomainObject());
        lookupContents.add(domainObject);
        fireCookieChange();
        fireNameChange(oldName, getName());
        log.debug("Display name changed {} -> {}",oldDisplayName, getDisplayName());
        fireDisplayNameChange(oldDisplayName, getDisplayName());
    }

    public ChildFactory<?> getParentChildFactory() {
        return parentChildFactory;
    }
    
    protected InstanceContent getLookupContents() {
        return lookupContents;
    }

    @SuppressWarnings("unchecked")
    public T getDomainObject() {
        return (T) getLookup().lookup(DomainObject.class);
    }
    
    @Override
    public String getName() {
        return getDomainObject().getName();
    }
    
    @Override
    public Long getId() {
        return getDomainObject().getId();
    }

    public String getPrimaryLabel() {
        return getDomainObject().getId().toString();
    }

    public String getSecondaryLabel() {
    	return DomainUtils.getNameFromSubjectKey(getDomainObject().getOwnerKey());
    }

    public String getExtraLabel() {
        return null;
    }

    @Override
    public String getDisplayName() {
        String displayName = getPrimaryLabel()+" "+getSecondaryLabel();
        String extra = getExtraLabel();
        if (extra!=null) {
            displayName += " "+extra;    
        }
        return displayName;
    }

    @Override
    public String getHtmlDisplayName() {
        String primary = getPrimaryLabel();
        String secondary = getSecondaryLabel();
        String extra = getExtraLabel();
        StringBuilder sb = new StringBuilder();
        if (primary!=null) {
            sb.append("<font color='!Label.foreground'>");
            sb.append(primary);
            sb.append("</font>");
        }
        if (secondary!=null) {
            sb.append(" <font color='#957D47'>");
            sb.append(secondary);
            sb.append("</font>");
        }
        if (extra!=null) {
            sb.append(" <font color='#959595'>");
            sb.append(extra);
            sb.append("</font>");
        }
        return sb.toString();
    }

    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("brick_grey.png").getImage();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public String get2dImageFilepath(String role) {
        DomainObject domainObject = getDomainObject();
        if (domainObject instanceof HasFiles) {
            return DomainUtils.getFilepath((HasFiles) domainObject, role);
        }
        return null;
    }
    
    @Override
    public boolean canCut() {
        return ClientDomainUtils.hasWriteAccess(getDomainObject());
    }

    @Override
    public boolean canCopy() {
        return true;
    }

    @Override
    public boolean canRename() {
        return false;
    }

    @Override
    public boolean canDestroy() {
        return ClientDomainUtils.hasWriteAccess(getDomainObject());
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new CopyToClipboardAction("Name", getName()));
        actions.add(new CopyToClipboardAction("GUID", getId()+""));
        actions.add(null);
        actions.add(new OpenInViewerAction());
        actions.add(new OpenInNewViewerAction());
        actions.add(null);
        actions.add(new ViewDetailsAction());
        actions.add(new ChangePermissionsAction());
        actions.add(AddToFolderAction.get());
        actions.add(new RenameAction());
        actions.add(RemoveAction.get());
        actions.add(null);
        for (AbstractAction action : ServiceAcceptorActionHelper.getOpenForContextActions(getDomainObject())) {
            if (action==null) {
                actions.add(null);
            }
            else {
                String name = (String)action.getValue(Action.NAME);
                if (name!=null) {
                    action.putValue(Action.NAME, name.trim());
                }
                actions.add(action);
            }
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public SystemAction[] getActions() {
        return null;
    }
    
    @Override
    protected Sheet createSheet() {

        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        DomainObject obj = getDomainObject();

        try {
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                Method getter = propertyDescriptor.getReadMethod();
                if (getter==null) continue;
                Method setter = propertyDescriptor.getWriteMethod();
                PropertySupport.Reflection<?> prop = new PropertySupport.Reflection<>(obj, getter.getReturnType(), getter, setter);
                prop.setName(DomainUtils.unCamelCase(getter.getName().replaceFirst("get", "")));
                set.put(prop);
            }
        }
        catch (IntrospectionException ex) {
            ConsoleApp.handleException(ex);
        }

        sheet.put(set);
        return sheet;
    }

    protected final class RenameAction extends AbstractAction {

        public RenameAction() {
            putValue(NAME, "Rename");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DomainObject domainObject = getLookup().lookup(DomainObject.class);
            if (domainObject==null) {
                return;
            }
            String newName = (String) JOptionPane.showInputDialog(ConsoleApp.getMainFrame(), "Name:\n", "Rename "
                    + domainObject.getName(), JOptionPane.PLAIN_MESSAGE, null, null, domainObject.getName());
            if ((newName == null) || (newName.length() <= 0)) {
                return;
            }
            try {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                final String oldName = domainObject.getName();
                model.updateProperty(domainObject, "name", newName);
                fireDisplayNameChange(oldName, newName); 
            } 
            catch (Exception ex) {
                ConsoleApp.handleException(ex);
            }
        }

        @Override
        public boolean isEnabled() {
            DomainObject domainObject = getLookup().lookup(DomainObject.class);
            return ClientDomainUtils.hasWriteAccess(domainObject);
        }
    }

    protected final class OpenInViewerAction extends AbstractAction {

        public OpenInViewerAction() {
            putValue(NAME, "Open In Viewer");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DomainListViewTopComponent viewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor");
            viewer.loadDomainObjectNode(DomainObjectNode.this, true);
        }

        @Override
        public boolean isEnabled() {
            return DomainListViewTopComponent.isSupported(getDomainObject());
        }
    }
    
    protected final class OpenInNewViewerAction extends AbstractAction {

        public OpenInNewViewerAction() {
            putValue(NAME, "Open In New Viewer");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DomainListViewTopComponent viewer = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
            viewer.requestActive();
            viewer.loadDomainObjectNode(DomainObjectNode.this, true);
        }

        @Override
        public boolean isEnabled() {
            return DomainListViewTopComponent.isSupported(getDomainObject());
        }
    }

    protected final class ViewDetailsAction extends AbstractAction {

        public ViewDetailsAction() {
            putValue(NAME, "View Details");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new DomainDetailsDialog().showForDomainObject(getDomainObject());
        }
    }

    protected final class ChangePermissionsAction extends AbstractAction {

        public ChangePermissionsAction() {
            putValue(NAME, "Change Permissions");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new DomainDetailsDialog().showForDomainObject(getDomainObject(), DomainInspectorPanel.TAB_NAME_PERMISSIONS);
        }

        @Override
        public boolean isEnabled() {
            return ClientDomainUtils.isOwner(getDomainObject());
        }
    }

    @Override
    public void destroy() throws IOException {
        DomainObjectNodeTracker.getInstance().deregisterNode(DomainObjectNode.this);
    }
    
    @Override
    public Transferable clipboardCopy() throws IOException {
        log.debug("Copy to clipboard: {}",getDomainObject());
        Transferable deflt = super.clipboardCopy();
        return addFlavors(ExTransferable.create(deflt));
    }

    @Override
    public Transferable clipboardCut() throws IOException {
        log.debug("Cut to clipboard: {}",getDomainObject());
        Transferable deflt = super.clipboardCut();
        return addFlavors(ExTransferable.create(deflt));
    }
    
    private Transferable addFlavors(ExTransferable added) {
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        added.put(new ExTransferable.Single(DomainObjectFlavor.SINGLE_FLAVOR) {
            @Override
            protected DomainObject getData() {
                return getDomainObject();
            }
        });
        added.put(new ExTransferable.Single(DomainObjectNodeFlavor.SINGLE_FLAVOR) {
            @Override
            protected DomainObjectNode<?> getData() {
                return DomainObjectNode.this;
            }
        });
        return added;
    }
    
    /**
     * Subclasses should override this method to add their their paste type to the set. 
     */
    @Override
    public PasteType getDropType(final Transferable t, int action, int index) {
        // Let subclasses define their paste types by overriding this method
        return null;
    }

}
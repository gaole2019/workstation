package org.janelia.workstation.common.gui.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.core.model.ImageModel;
import org.janelia.workstation.core.actions.ViewerContext;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainUIUtils {

    public static DomainObjectImageModel getDomainObjectImageModel(ViewerContext viewerContext) {
        ImageModel imageModel = viewerContext.getImageModel();
        if (!viewerContext.isMultiple() && imageModel instanceof DomainObjectImageModel) {
            return (DomainObjectImageModel) imageModel;
        }
        return null;
    }

    public static DomainObject getLastSelectedDomainObject(ViewerContext viewerContext) {
        if (viewerContext.getLastSelectedObject() instanceof DomainObject) {
            return ((DomainObject) viewerContext.getLastSelectedObject());
        }
        return null;
    }

    public static Collection<DomainObject> getSelectedDomainObjects(ViewerContext viewerContext) {
        List<DomainObject> domainObjects = new ArrayList<>();
        for(Object obj : viewerContext.getSelectedObjects()) {
            if (obj instanceof DomainObject) {
                DomainObject domainObject = (DomainObject) obj;
                domainObjects.add(domainObject);
            }
        }
        return domainObjects;
    }

}

package org.janelia.workstation.integration.spi.domain;

import org.janelia.model.domain.DomainObject;

/**
 * Implement this to make a means of creating a domain object for viewing another
 * such object.  This generally involves wrapping the original entity.
 * @author fosterl
 */
public interface DomainObjectCreator extends Compatible<DomainObject> {

    void useDomainObject( DomainObject e );
    @Override
    boolean isCompatible( DomainObject e );
    String getActionLabel();
}

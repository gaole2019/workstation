package org.janelia.jacs2.sampleprocessing;

import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

@Named("lsmMetadataFiles")
public class CreateLsmMetadataFilesServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "lsmMetadataFiles";

    @Named("lsmMetadataFilesService")
    @Inject
    private Instance<ServiceComputation> lsmMetadataFilesComputationSource;

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        return smd;
    }

    @Override
    public ServiceComputation createComputationInstance() {
        return lsmMetadataFilesComputationSource.get();
    }

}
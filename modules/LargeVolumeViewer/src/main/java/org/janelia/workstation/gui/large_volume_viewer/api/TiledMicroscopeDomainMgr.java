package org.janelia.workstation.gui.large_volume_viewer.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.model.access.domain.DomainObjectComparator;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.BulkNeuronStyleUpdate;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.janelia.model.domain.tiledMicroscope.TmReviewTask;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.domain.workspace.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing the Tiled Microscope Domain Model and related data access.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TiledMicroscopeDomainMgr {

    private static final Logger LOG = LoggerFactory.getLogger(TiledMicroscopeDomainMgr.class);

    // Singleton
    private static TiledMicroscopeDomainMgr instance;
    
    public static TiledMicroscopeDomainMgr getDomainMgr() {
        if (instance==null) {
            instance = new TiledMicroscopeDomainMgr();
        }
        return instance;
    }

    private final TiledMicroscopeRestClient client;
    
    private TiledMicroscopeDomainMgr() {
        client = new TiledMicroscopeRestClient();
    }
    
    private final DomainModel model = DomainMgr.getDomainMgr().getModel();

    public List<String> getSamplePaths() throws Exception {
        return client.getTmSamplePaths();
    }

    public void setSamplePaths(List<String> paths) throws Exception {
        client.updateSamplePaths(paths);
    }
    
    public TmSample getSample(Long sampleId) throws Exception {
        LOG.debug("getSample(sampleId={})",sampleId);
        return model.getDomainObject(TmSample.class, sampleId);
    }

    public TmSample getSample(TmWorkspace workspace) throws Exception {
        LOG.debug("getSample({})",workspace);
        return getSample(workspace.getSampleRef().getTargetId());
    }

    public TmSample createSample(String name, String filepath) throws Exception {
        LOG.debug("createTiledMicroscopeSample(name={}, filepath={})", name, filepath);
        Map<String,Object> constants = client.getTmSampleConstants(filepath);
        if (constants != null) {
            TreeNode tmSampleFolder = model.getDefaultWorkspaceFolder(DomainConstants.NAME_TM_SAMPLE_FOLDER, true);

            TmSample sample = new TmSample();
            sample.setOwnerKey(AccessManager.getSubjectKey());
            sample.setName(name);
            sample.setFilepath(filepath);
            Map originMap = (Map)constants.get("origin");
            List<Integer> origin = new ArrayList<>();
            origin.add ((Integer)originMap.get("x"));
            origin.add ((Integer)originMap.get("y"));
            origin.add ((Integer)originMap.get("z"));            
            Map scalingMap = (Map)constants.get("scaling");
            List<Double> scaling = new ArrayList<>();
            scaling.add ((Double)scalingMap.get("x"));
            scaling.add ((Double)scalingMap.get("y"));
            scaling.add ((Double)scalingMap.get("z"));
            
            sample.setOrigin(origin);
            sample.setScaling(scaling);
            if (constants.get("numberLevels") instanceof Integer) {
                sample.setNumImageryLevels(((Integer)constants.get("numberLevels")).longValue());
            } else {
                sample.setNumImageryLevels(Long.parseLong((String)constants.get("numberLevels")));
            }

            // call out to server to get origin/scaling information
            TmSample persistedSample = save(sample);

            // Server should have put the sample in the Samples root folder. Refresh the Samples folder to show it in the explorer.
            model.invalidate(tmSampleFolder);

            return persistedSample;
        } else {
            LOG.error("Problem creating sample; no sample constants available for sample {} at {}.", name, filepath);
            return null;
        }
    }

    public TmSample save(TmSample sample) throws Exception {
        LOG.debug("save({})",sample);
        TmSample canonicalObject;
        synchronized (this) {
            canonicalObject = model.putOrUpdate(sample.getId()==null ? client.create(sample) : client.update(sample));
        }
        if (sample.getId()==null) {
            model.notifyDomainObjectCreated(canonicalObject);
        }
        else {
            model.notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public void remove(TmSample sample) throws Exception {
        LOG.debug("remove({})",sample);
        client.remove(sample);
        model.notifyDomainObjectRemoved(sample);
    }

    public List<TmWorkspace> getWorkspaces(Long sampleId) throws Exception {
        Collection<TmWorkspace> workspaces = client.getTmWorkspacesForSample(sampleId);
        List<TmWorkspace> canonicalObjects = DomainMgr.getDomainMgr().getModel().putOrUpdate(workspaces, false);
        // TODO: sort these on the server side
        Collections.sort(canonicalObjects, new DomainObjectComparator(AccessManager.getSubjectKey()));
        return canonicalObjects;
    }

    public TmWorkspace getWorkspace(Long workspaceId) throws Exception {
        LOG.debug("getWorkspace(workspaceId={})",workspaceId);
        TmWorkspace workspace = model.getDomainObject(TmWorkspace.class, workspaceId);
        if (workspace==null) {
            throw new Exception("Workspace with id="+workspaceId+" does not exist");
        }
        return workspace;
    }
    
    public TmWorkspace createWorkspace(Long sampleId, String name) throws Exception {
        LOG.debug("createWorkspace(sampleId={}, name={})", sampleId, name);
        TmSample sample = getSample(sampleId);
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+sampleId);
        }

        TreeNode defaultWorkspaceFolder = model.getDefaultWorkspaceFolder(DomainConstants.NAME_TM_WORKSPACE_FOLDER, true);

        TmWorkspace workspace = new TmWorkspace();
        workspace.setOwnerKey(AccessManager.getSubjectKey());
        workspace.setName(name);
        workspace.setSampleRef(Reference.createFor(TmSample.class, sampleId));
        workspace = save(workspace);
        
        // Server should have put the workspace in the Workspaces root folder. Refresh the Workspaces folder to show it in the explorer.
        model.invalidate(defaultWorkspaceFolder);
        
        // Also invalidate the sample, so that the Explorer tree can be updated 
        model.invalidate(sample);
        
        return workspace;
    }

    public TmWorkspace copyWorkspace(TmWorkspace workspace, String name, String assignOwner) throws Exception {
        LOG.debug("copyWorkspace(workspace={}, name={}, neuronOwner={})", workspace, name, assignOwner);
        TmSample sample = getSample(workspace.getSampleId());
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+workspace.getSampleId());
        }

        TreeNode defaultWorkspaceFolder = model.getDefaultWorkspaceFolder(DomainConstants.NAME_TM_WORKSPACE_FOLDER, true);

        TmWorkspace workspaceCopy = client.copy(workspace, name, assignOwner);

        // Server should have put the new workspace in the Workspaces root folder. Refresh the Workspaces folder to show it in the explorer.
        model.invalidate(defaultWorkspaceFolder);
        
        // Also invalidate the sample, so that the Explorer tree can be updated 
        model.invalidate(sample);
        
        return workspaceCopy;
    }

    public TmWorkspace save(TmWorkspace workspace) throws Exception {
        LOG.debug("save({})", workspace);
        TmWorkspace canonicalObject;
        synchronized (this) {
            canonicalObject = model.putOrUpdate(workspace.getId()==null ? client.create(workspace) : client.update(workspace));
        }
        if (workspace.getId()==null) {
            model.notifyDomainObjectCreated(canonicalObject);
        }
        else {
            model.notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public void remove(TmWorkspace workspace) throws Exception {
        LOG.debug("remove({})", workspace);
        client.remove(workspace);
        model.notifyDomainObjectRemoved(workspace);
    }
    
    public List<TmNeuronMetadata> getWorkspaceNeurons(Long workspaceId) throws Exception {
        LOG.debug("getWorkspaceNeurons(workspaceId={})",workspaceId);
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        List<TmNeuronMetadata> neurons = new ArrayList<>();
        for(Pair<TmNeuronMetadata, InputStream> pair : client.getWorkspaceNeuronPairs(workspaceId)) {
            TmNeuronMetadata neuronMetadata = pair.getLeft();
            exchanger.deserializeNeuron(pair.getRight(), neuronMetadata);
            LOG.trace("Got neuron {} with payload '{}'", neuronMetadata.getId(), neuronMetadata);
            neurons.add(neuronMetadata);
        }
        LOG.trace("Loaded {} neurons for workspace {}", neurons.size(), workspaceId);
        return neurons;
    }

    public TmNeuronMetadata saveMetadata(TmNeuronMetadata neuronMetadata) throws Exception {
        LOG.debug("save({})", neuronMetadata);
        TmNeuronMetadata savedMetadata;
        if (neuronMetadata.getId()==null) {
            savedMetadata = client.createMetadata(neuronMetadata);
            model.notifyDomainObjectCreated(savedMetadata);
        }
        else {
            savedMetadata = client.updateMetadata(neuronMetadata);
            model.notifyDomainObjectChanged(savedMetadata);
        }
        return savedMetadata;
    }

    public List<TmNeuronMetadata> saveMetadata(List<TmNeuronMetadata> neuronList) throws Exception {
        LOG.debug("save({})", neuronList);
        for(TmNeuronMetadata tmNeuronMetadata : neuronList) {
            if (tmNeuronMetadata.getId()==null) {
                throw new IllegalArgumentException("Bulk neuron creation is currently unsupported");
            }
        }
        List<TmNeuronMetadata> updatedMetadata = client.updateMetadata(neuronList);
        for(TmNeuronMetadata tmNeuronMetadata : updatedMetadata) {
            model.notifyDomainObjectChanged(tmNeuronMetadata);
        }
        return updatedMetadata;
    }
    
    public TmNeuronMetadata save(TmNeuronMetadata neuronMetadata) throws Exception {
        LOG.debug("save({})", neuronMetadata);
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        InputStream protobufStream = new ByteArrayInputStream(exchanger.serializeNeuron(neuronMetadata));
        TmNeuronMetadata savedMetadata;
        if (neuronMetadata.getId()==null) {
            savedMetadata = client.create(neuronMetadata, protobufStream);
            model.notifyDomainObjectCreated(savedMetadata);
        }
        else {
            savedMetadata = client.update(neuronMetadata, protobufStream);
            model.notifyDomainObjectChanged(savedMetadata);
        }
        // We assume that the neuron data was saved on the server, but it only returns metadata for efficiency. We
        // already have the data, so let's copy it over into the new object.
        exchanger.copyNeuronData(neuronMetadata, savedMetadata);
        return savedMetadata;
    }
    
    public TmReviewTask save(TmReviewTask reviewTask) throws Exception {
        LOG.debug("save({})", reviewTask);
        if (reviewTask.getId()==null) {
            reviewTask = client.create(reviewTask);
        } else {
            reviewTask = client.update(reviewTask);
        }
        model.notifyDomainObjectChanged(reviewTask);
        return reviewTask;
    }
    
    public void remove(TmReviewTask reviewTask) throws Exception {
        LOG.debug("remove({})", reviewTask);
        client.remove(reviewTask);
        model.notifyDomainObjectRemoved(reviewTask);
    }
    
    public void updateNeuronStyles(BulkNeuronStyleUpdate bulkNeuronStyleUpdate) throws Exception {
        client.updateNeuronStyles(bulkNeuronStyleUpdate);
    }
    
    public void remove(TmNeuronMetadata tmNeuron) throws Exception {
        LOG.debug("remove({})", tmNeuron);
        TmNeuronMetadata neuronMetadata = new TmNeuronMetadata();
        neuronMetadata.setId(tmNeuron.getId());
        client.remove(neuronMetadata);
        model.notifyDomainObjectRemoved(neuronMetadata);
    }
    
    public List<TmReviewTask> getReviewTasks() throws Exception {
        LOG.debug("getReviewTasks()");
        List<TmReviewTask> reviewTasks = model.getAllDomainObjectsByClass(TmReviewTask.class);
        return reviewTasks;
    }

    public void bulkEditNeuronTags(List<TmNeuronMetadata> neurons, List<String> tags, boolean tagState) throws Exception {
        LOG.debug("bulkEditTags({})", neurons);
        client.changeTags(neurons, tags, tagState);
    }

    public CoordinateToRawTransform getCoordToRawTransform(String basePath) throws Exception {
        LOG.debug("getCoordToRawTransform({})", basePath);
        return client.getLvvCoordToRawTransform(basePath);
    }

    public byte[] getRawTextureBytes(String basePath, int[] viewerCoord, int[] dimensions, int channel) throws Exception {
        LOG.debug("getTextureBytes({}, viewerCoord={}, dimensions={})", basePath, viewerCoord, dimensions);
        return client.getRawTextureBytes(basePath, viewerCoord, dimensions, channel);
    }

    public String getNearestChannelFilesURL(String basePath, int[] viewerCoord) {
        LOG.debug("getNearestChannelFilesURL({}, viewerCoord={})", basePath, viewerCoord);
        return client.getNearestChannelFilesURL(basePath, viewerCoord);
    }
}

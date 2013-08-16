package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;



// workstation imports


import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Anchor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;


import javax.swing.*;


public class AnnotationManager
{

    ModelMgr modelMgr;

    // annotation model object
    private AnnotationModel annotationModel;
    
    private Entity initialEntity;

    // signals & slots
    public Slot1<Skeleton.AnchorSeed> addAnchorRequestedSlot = new Slot1<Skeleton.AnchorSeed>() {
        @Override
        public void execute(Skeleton.AnchorSeed seed) {
            addAnnotation(seed.getLocation(), seed.getParentGuid());
        }
    };

    public Slot1<Anchor> deleteAnchorRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            // currently delete subtree, probably change later
            deleteSubTree(anchor.getGuid());
        }
    };

    public Slot1<Anchor> moveAnchorRequestedSlot = new Slot1<Anchor>() {
        @Override
        public void execute(Anchor anchor) {
            moveAnnotation(anchor.getGuid(), anchor.getLocation());
        }
    };

    public Slot1<Long> selectAnnotationSlot = new Slot1<Long>() {
        @Override
        public void execute(Long annotationID) {
            selectNeuronFromAnnotation(annotationID);
        }
    };

    // constants
    public static final String WORKSPACES_FOLDER_NAME = "Workspaces";



    public AnnotationManager(AnnotationModel annotationModel) {


        this.annotationModel = annotationModel;

        modelMgr = ModelMgr.getModelMgr();



    }


    public Entity getInitialEntity() {
        return initialEntity;
    }

    public void setInitialEntity(final Entity initialEntity) {

        this.initialEntity = initialEntity;

        if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            // currently nothing to do for this case
        }

        else if (initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {

            SimpleWorker loader = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    // make sure the entity's fully loaded or the workspace creation will fail
                    TmWorkspace workspace = new TmWorkspace(modelMgr.loadLazyEntity(initialEntity, false));
                    annotationModel.loadWorkspace(workspace);
                }

                @Override
                protected  void hadSuccess() {
                    // no hadSuccess(); signals will be emitted in the loadWorkspace() call
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            loader.execute();

        }


        // (eventually) update state to saved state (selection, visibility, etc)



    }
    

    // methods that are called by actions from the 2d view; should be not
    //  much more than what tool is active and where the click was;
    //  we are responsible for 

    public void addAnnotation(Vec3 xyz, Long parentID) {

        // get current workspace, etc.; if they don't exist, error
        if (annotationModel.getCurrentWorkspace() == null) {
            JOptionPane.showMessageDialog(null,
                "You must load a workspace before beginning annotation!",
                "No workspace!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        TmNeuron currentNeuron = annotationModel.getCurrentNeuron();
        if (currentNeuron == null) {
            JOptionPane.showMessageDialog(null,
                "You must select a neuron before beginning annotation!",
                "No neuron!",
                JOptionPane.ERROR_MESSAGE);
            return;
        }


        // if parentID is null, it's a new root in current neuron
        if (parentID == null) {
            // new root in current neuron:

            // this should probably not take the ws and neuron (assume current), but
            //  we're testing:
            annotationModel.addRootAnnotation(annotationModel.getCurrentWorkspace(),
                currentNeuron, xyz);


        } else {
            // new node with existing parent

            // verify the supposed parent annotation is in our neuron
            //  (probably temporary; at some point, we'll have to handle display of,
            //  and smooth switching of operations between, different neurons, triggered
            //  from the 2D view)
            if (!currentNeuron.getGeoAnnotationMap().containsKey(parentID)) {
                JOptionPane.showMessageDialog(null,
                        "Current neuron does not contain selected root annotation!",
                        "Wrong neuron!",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            annotationModel.addChildAnnotation(currentNeuron,
                currentNeuron.getGeoAnnotationMap().get(parentID), xyz);

        }

        // select new annotation (?) (currently marked graphically?)



    }

    public void deleteSubTree(final Long annotationID) {
        if (!annotationModel.hasCurrentWorkspace()) {
            // dialog?

            return;
        } else {
            SimpleWorker deleter = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.deleteSubTree(annotationModel.getGeoAnnotationFromID(annotationID));
                }

                @Override
                protected void hadSuccess() {
                    // nothing here; annotationModel emits signals
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            deleter.execute();
        }
    }

    public void moveAnnotation(final Long annotationID, final Vec3 location) {
        if (!annotationModel.hasCurrentWorkspace()) {
            // dialog?

            return;
        } else {

            SimpleWorker mover = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    annotationModel.moveAnnotation(annotationID, location);
                }

                @Override
                protected void hadSuccess() {
                    // nothing here; annotationModel will emit signals
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            mover.execute();

        }

    }

    public void createNeuron() {
        // is there a workspace?  if not, fail; actually, doesn't the model know?
        //  yes; who should test?  who should pop up UI feedback to user?
        //  model shouldn't, but should annMgr? 

        if (!annotationModel.hasCurrentWorkspace()) {
            // dialog?

            return;
        }

        // ask user for name; you *can* rename on the sidebar, but that will 
        //  trigger a need to reload the slice viewer, so don't make the user go 
        //  through that
        final String neuronName = (String)JOptionPane.showInputDialog(
            null,
            "Neuron name:",
            "Create neuron",
            JOptionPane.PLAIN_MESSAGE,
            null,                           // icon
            null,                           // choice list; absent = freeform
            "new neuron");
        if (neuronName == null || neuronName.length() == 0) {
            return;
        }

        // validate neuron name;  are there any rules for entity names?


        // create it:
        SimpleWorker creator = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                annotationModel.createNeuron(neuronName);
            }

            @Override
            protected void hadSuccess() {
                // nothing here, annModel emits its own signals
            }

            @Override
            protected void hadError(Throwable error) {
                JOptionPane.showMessageDialog(null,
                        "Could not create neuron!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        creator.execute();

    }

    public void selectNeuronFromAnnotation(Long annotationID) {
        if (annotationID == null) {
            return;
        }

        TmNeuron neuron = annotationModel.getNeuronFromAnnotation(annotationID);
        annotationModel.setCurrentNeuron(neuron);
    }

    public void createWorkspace() {

        // first we need to figure out the brain sample; the user may
        //  open the slice viewer from either a brain sample or a workspace; if
        //  it's the latter, grab its brain sample
        // (currently can't open Slice Viewer without an initial entity)


        // need to reorder stuff so I can isolate db stuff

        // if no sample loaded, error
        //  how do we even do this?

        // ask re: new workspace if one is active

        // ask for name (there's no cancel at this point; blank name = default name)

        // db stuff:

        // create new workspaces folder if needed (push down to annModel?  make it
        //  a "checkcreateworkspacesfolder()" kind of thing?  currently provide 
        //  the entity, but that should also be done in annModel, right?)
        // not at all clear how to report errors from this part

        // obtain sample to create it with (can we do w/o db call?  can annModel
        //  figure out currently loaded sample (by now we know there is one that's
        //  loaded)?)  (annMgr knows initialentity; annModel knows sample if it's a 
        //  workspace, but not otherwise);  so just push initial entity down
        //  and let it happen deeper in code?


        // create workspace: need to pass:
        //  - parent entity (workspaces, or create it) (so don't need to pass right now,
        //      although later, we'll want to enable people to choose where to create)
        //  - sample ID
        //  - name



        // NOTE: ask the user if you're creating a new workspace when one is
        //  already active
        Entity sampleEntity;
        if (annotationModel.hasCurrentWorkspace()) {
            // dialog
            int ans = JOptionPane.showConfirmDialog(null,
                "You already have an active workspace!  Close and create another?",
                "Workspace exists",
                JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                Long sampleID = annotationModel.getCurrentWorkspace().getSampleID();
                try {
                    sampleEntity = modelMgr.getEntityById(sampleID);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                // users says no
                return;
            }
        } else {
            // no workspace, look at initial entity; it must be a brain sample!
            if (!initialEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                JOptionPane.showMessageDialog(null,
                        "You must load a brain sample before creating a workspace!",
                        "No brain sample!",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            sampleEntity= initialEntity;
        }

        // for now, we'll put the new workspace into a default, top-level folder
        //  named "Workspaces", which we will create if it does not exit; later,
        //  we'll create a dialog to let the user choose the location of the
        //  new workspace, and perhaps the brain sample, too
        Entity workspaceRootEntity;
        try {
            workspaceRootEntity = modelMgr.getCommonRootEntityByName(WORKSPACES_FOLDER_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (workspaceRootEntity == null) {
            try {
                workspaceRootEntity = modelMgr.createCommonRoot(WORKSPACES_FOLDER_NAME);
            } catch (Exception e) {
                e.printStackTrace();
                // fail: dialog
                JOptionPane.showMessageDialog(null,
                    "Could not create Workspaces top-level folder!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // get a name for the new workspace and validate (are there any rules for entity names?)
        String workspaceName = (String)JOptionPane.showInputDialog(
            null,
            "Workspace name:",
            "Create workspace",
            JOptionPane.PLAIN_MESSAGE,
            null,                           // icon
            null,                           // choice list; absent = freeform
            "new workspace");
        if ((workspaceName == null) || (workspaceName.length() == 0)) {
            workspaceName = "new workspace";
        }

        // create it
        if (!annotationModel.createWorkspace(workspaceRootEntity, sampleEntity, workspaceName)) {
            JOptionPane.showMessageDialog(null, 
                "Could not create workspace!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }

    }

}
















package org.janelia.it.workstation.ab2.controller;

import java.awt.event.MouseEvent;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.actor.Image2DActor;
import org.janelia.it.workstation.ab2.actor.PickSquareActor;
import org.janelia.it.workstation.ab2.event.AB2DomainObjectUpdateEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2Image2DClickEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.event.AB2PickSquareColorChangeEvent;
import org.janelia.it.workstation.ab2.model.AB2SkeletonDomainObject;
import org.janelia.it.workstation.ab2.renderer.AB2SkeletonRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleMode extends AB2View3DMode {

    Logger logger= LoggerFactory.getLogger(AB2SampleMode.class);


    public AB2SampleMode(AB2Controller controller, AB2SkeletonRenderer renderer) {
        super(controller, renderer);
        logger.info("AB2SampleMode() constructor finished");
    }

    @Override
    public void processEvent(AB2Event event) {
        //logger.info("processEvent()");
        super.processEvent(event);
        if  (event instanceof AB2DomainObjectUpdateEvent) {
            ((AB2SkeletonRenderer)renderer).setSkeletonsAndVolume(((AB2SkeletonDomainObject)controller.getDomainObject()).getSkeletons(),
                    ((AB2SkeletonDomainObject)controller.getDomainObject()).getVolumeGenerator());
            controller.repaint();
        } else if (event instanceof AB2MouseClickedEvent) {
            MouseEvent mouseEvent=((AB2MouseClickedEvent) event).getMouseEvent();
            int x = mouseEvent.getX();
            int y = mouseEvent.getY(); // y is inverted - 0 is at the top
            //logger.info("renderer.addMouseClickEvent() x="+x+" y="+y);
            renderer.addMouseClickEvent(x, y);
            //logger.info("processEvent() calling renderer.addMouseClick() x="+x+" y="+y);
            controller.repaint();
        } else if (event instanceof AB2PickSquareColorChangeEvent) {
            PickSquareActor pickSquareActor=((AB2PickSquareColorChangeEvent)event).getPickSquareActor();
            int actorId=pickSquareActor.getActorId();
            logger.info("Handling AB2PickSquareColorChangeEvent actorId="+actorId+" pickIndex="+pickSquareActor.getPickIndex());
            AB2SkeletonRenderer skeletonRenderer=(AB2SkeletonRenderer)renderer;
            Vector4 currentColor=skeletonRenderer.getColorId(actorId);
            Vector4 color0=pickSquareActor.getColor0();
            if (currentColor.equals(color0)) {
                skeletonRenderer.setColorId(actorId, pickSquareActor.getColor1());
            } else {
                skeletonRenderer.setColorId(actorId, pickSquareActor.getColor0());
            }
            controller.repaint();
        } else if (event instanceof AB2Image2DClickEvent) {
            Image2DActor image2DActor=((AB2Image2DClickEvent)event).getImage2DActor();
            logger.info("Handling AB2Image2DClickEvent - actorId="+image2DActor.getActorId()+" pickIndex="+image2DActor.getPickIndex());
        }
    }


}

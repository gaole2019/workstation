/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.horta.actors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Resource;
import org.janelia.horta.blocks.BlockChooser;
import org.janelia.horta.blocks.BlockTileKey;
import org.janelia.horta.blocks.BlockTileSource;

/**
 *
 * @author brunsc
 */
public class DynamicRegionBlockActorSet<K extends BlockTileKey>
        implements SortableBlockActorSource, // designed to be contained within a TetVolumeActor
                   GL3Resource {

    private final BlockTileSource<K> tileSource;
    private final BlockChooser<K, BlockTileSource<K>> tileChooser;
    private final Map<K, SortableBlockActor> blockActors = new HashMap<>();
    private Collection<SortableBlockActor> obsoleteActors = new ArrayList<>();

    public DynamicRegionBlockActorSet(BlockTileSource<K> tileSource, BlockChooser<K, BlockTileSource<K>> tileChooser) {
        this.tileSource = tileSource;
        this.tileChooser = tileChooser;
    }

    public synchronized void updateActors(Vector3 focus, Vector3 previousFocus) {
        List<K> desiredBlocks = tileChooser.chooseBlocks(tileSource, focus, previousFocus);
        List<K> newBlocks = new ArrayList<>();
        Set<K> desiredSet = new HashSet<>();
        boolean bChanged = false;
        for (K key : desiredBlocks) {
            desiredSet.add(key);
            if (!blockActors.containsKey(key)) {
                newBlocks.add(key);
                bChanged = true;
            }
        }
        for (BlockTileKey key : blockActors.keySet()) {
            if (!desiredSet.contains(key)) {
                SortableBlockActor actor = blockActors.remove(key);
                obsoleteActors.add(actor);
                bChanged = true;
            }
        }
        // TODO: - load more blocks from source
    }

    @Override
    public Collection<SortableBlockActor> getSortableBlockActors() {
        return blockActors.values();
    }

    @Override
    public void dispose(GL3 gl) {
        for (SortableBlockActor actor : blockActors.values()) {
            actor.dispose(gl);
        }
        blockActors.clear();
        for (SortableBlockActor actor : obsoleteActors) {
            actor.dispose(gl);
        }
        obsoleteActors.clear();
    }

    @Override
    public void init(GL3 gl) {
    }
}
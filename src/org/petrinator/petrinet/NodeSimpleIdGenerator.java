/*
 * Copyright (C) 2008-2010 Martin Riesz <riesz.martin at gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.petrinator.petrinet;

import java.util.Set;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */

/*
 * Modificado: hice que extienda de NodeIdGenerator para usar su método P1 P2 T3 -> P1 P2 T1
 */
public class NodeSimpleIdGenerator extends NodeIdGenerator{

    private int nextUniqueId = 1; //rootSubnet has already id == 0
    private PetriNet petriNet;

    public NodeSimpleIdGenerator(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    public void setUniqueId(Node node) 
    {
    	
    	/*
    	 * Original:
    	 */
    	
        /*
           	String id;
        	id = Integer.toString(nextUniqueId++);
       		node.setId(id);
       	*/
    	
    	/*
    	 * Modificado:  para usar método ya creado en NodeIdGenerator.
    	 */
    		
    	super.setUniqueId(node);
        
        /*
         * Modificado, mi versión:
         */
    /*    
        Set<Node> allNodes = petriNet.getRootSubnet().getNodesRecursively();
        allNodes.add(petriNet.getRootSubnet());
        int aux = 0;
        for (Node n : allNodes)
        {
        	if(n.getClass().equals(node.getClass()))
        	{
        		if(Integer.parseInt(n.getId()) > aux)
        			aux = Integer.parseInt(n.getId());
        	}
        } 
        String id = Integer.toString(aux+1);
        node.setId(id);*/
        
    }

    public void fixFutureUniqueIds() {
        int maxId = 0;
        Set<Node> allNodes = petriNet.getRootSubnet().getNodesRecursively();
        allNodes.add(petriNet.getRootSubnet());
        for (Node node : allNodes) {
            try {
                int id = Integer.parseInt(node.getId());
                if (id > maxId) {
                    maxId = id;
                }
            } catch (NumberFormatException ex) {
                //do nothing
            }
        }
        nextUniqueId = maxId + 1;
    }

    public void ensureNumberIds() {
        for (Node node : petriNet.getRootSubnet().getNodesRecursively()) {
            try {
                Integer.parseInt(node.getId());
            } catch (NumberFormatException ex) {
                setUniqueId(node);
            }
        }
    }

    public void resetUniqueIds() 
    {
    	
    }
}

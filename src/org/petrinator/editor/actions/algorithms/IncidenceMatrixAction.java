/*
 * Copyright (C) 2008-2010 Martin Riesz <riesz.martin at gmail.com>
 * Copyright (C) 2016-2017 Joaquin Rodriguez Felici <joaquinfelici at gmail.com>
 * Copyright (C) 2016-2017 Leandro Asson <leoasson at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.petrinator.editor.actions.algorithms;

import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.petrinator.editor.commands.*;
import org.petrinator.petrinet.*;
import org.petrinator.editor.Root;
/**
 *
 * @author Joaquin Rodriguez Felici <joaquinfelici at gmail.com>
 */
public class IncidenceMatrixAction extends AbstractAction {

    private Root root;


    public IncidenceMatrixAction(Root root) {
        this.root = root;
        String name = "Incidence Matrix";
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) 
    {
    	 PetriNet petriNet = root.getDocument().petriNet;
    	 
    	 toString("Incidence", petriNet.incidenceMatrix());
    	 toString("Inhibition", petriNet.inhibitionMatrix());
    	 toString("Reset", petriNet.resetMatrix());
    	 toString("Marking", petriNet.getInitialMarking().getMarkingAsArray());
    	 
    }
    
    /*
     * Imprimir matriz:
     */
    public void toString(String type, int [][] matrix)
    {
    	System.out.println(type + " matrix: ");
    	for(int i = 0; i<matrix.length; i++)
    	{
    		System.out.print("[  ");
    		for(int j = 0; j<matrix[0].length; j++)
    		{
    			System.out.print(matrix[i][j]+ "  ");
    		}
    		System.out.println("]");
    	}
    	System.out.print("\n");
    }
    
    /*
     * Imprimir arreglo:
     */
    public void toString(String type, int [] array)
    {
    	System.out.println(type + " array: ");
    	System.out.print("[  ");
    	
    	for(int j = 0; j<array.length; j++)
    	{
    		System.out.print(array[j]+ "  ");
    	}
    		System.out.println("]");
    	
    	System.out.print("\n");
    }
    
    
    /*
     * Intento, para crear grafo a partir de I+ e I-. Funca.
     
    public void actionPerformed(ActionEvent e)
    {
    	PetriNet p = root.getDocument().petriNet;
    	int [][] matrixIPlus = {{0,0,0},{0,0,0},{1,0,0},{0,1,0}};
    	int [][] matrixIMinus = {{1,0,0},{0,1,7},{0,0,1},{0,0,1}};
    	p.reconstructFromMatrix(matrixIPlus, matrixIMinus);
    	p.resetView();
    }
    */
    
   /*
    * Intento simple para crear una plaza, una transici�n y un arco y que sea mostrado en el editor. Funca.
    
    public void actionPerformed(ActionEvent e) 
    {
    	 PetriNet petriNet = root.getDocument().petriNet;
    	 Subnet s = petriNet.getCurrentSubnet();
    	 
    	 AddPlaceCommand p1 = new AddPlaceCommand(s,0,0, petriNet);
    	 p1.execute();
    	 
    	 AddTransitionCommand t1 = new AddTransitionCommand(s,80,80,petriNet);
    	 t1.execute();
    	 
    	 AddArcCommand a1 = new AddArcCommand(p1.getCreatedPlace(),t1.getCreatedTransition(), true);
    	 a1.execute();
    	 
    	 petriNet.resetView();
    }
    */
}

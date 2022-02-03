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

import org.petrinator.editor.Root;
import org.petrinator.editor.filechooser.*;

import org.petrinator.util.GraphicsTools;
import pipe.gui.ApplicationSettings;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.PetriNetChooserPanel;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.views.MarkingView;
import pipe.views.PetriNetView;
import pipe.views.PlaceView;
import pipe.views.TransitionView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author Joaquin Felici <joaquinfelici at gmail.com>
 * @brief
 */
public class MatricesAction extends AbstractAction
{
    Root root;
    private ResultsHTMLPane results;

    public MatricesAction(Root root)
    {
        this.root = root;
        String name = "Matrices";
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/matrices16.png"));
    }

    public void actionPerformed(ActionEvent e)
    {
         /*
         * Create tmp.pnml file
         */
        FileChooserDialog chooser = new FileChooserDialog();

        if (root.getCurrentFile() != null)
        {
            chooser.setSelectedFile(root.getCurrentFile());
        }

        chooser.addChoosableFileFilter(new PipePnmlFileType());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(root.getCurrentDirectory());
        chooser.setDialogTitle("Save as...");

        File file = new File("tmp/" + "tmp" + "." + "pnml");
        FileType chosenFileType = (FileType) chooser.getFileFilter();
        try
        {
            chosenFileType.save(root.getDocument(), file);
        }
        catch (FileTypeException e1)
        {
            e1.printStackTrace();
        }

        /*
         * Show initial pane
         */
        EscapableDialog guiDialog = new EscapableDialog(root.getParentFrame(), "Petri net matrices and marking", true);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        //sourceFilePanel = new PetriNetChooserPanel("Source net", null);
        results = new ResultsHTMLPane("");
        contentPane.add(results);
        contentPane.add(new ButtonBar("Calculate", calculateButtonClick, guiDialog.getRootPane()));
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
    }

    private final ActionListener calculateButtonClick = new ActionListener()
    {
        public void actionPerformed(ActionEvent arg0)
        {
            /*
             * Read tmp file
             */
            PetriNetView data = new PetriNetView("tmp/tmp.pnml");

            /*
             *  Create HTML file with data
             */
            String s = "<h2>Petri Net Matrices</h2>";

            //String s = "<h3>Petri net matrices and marking</h3>"; // Do we REALLY need a third title? Let's think about that...
            if(data == null)
            {
                return;
            }
            if(!root.getDocument().getPetriNet().getRootSubnet().hasPlaces() || !root.getDocument().getPetriNet().getRootSubnet().hasTransitions())
            {
                s += "Invalid net!";
            }
            else
            {
                try
                {
                    //PNMLWriter.saveTemporaryFile(data, this.getClass().getName());

                    s += ResultsHTMLPane.makeTable(new String[]{
                            "Forwards incidence matrix <i>I<sup>+</sup></i>",
                            renderMatrix(data, data.getActiveTokenView().getForwardsIncidenceMatrix(
                                    data.getArcsArrayList(), data.getTransitionsArrayList(),
                                    data.getPlacesArrayList()))
                    }, 1, false, false, true, false);
                    s += ResultsHTMLPane.makeTable(new String[]{
                            "Backwards incidence matrix <i>I<sup>-</sup></i>",
                            renderMatrix(data, data.getActiveTokenView().getBackwardsIncidenceMatrix(
                                    data.getArcsArrayList(), data.getTransitionsArrayList(),
                                    data.getPlacesArrayList()))
                    }, 1, false, false, true, false);
                    s += ResultsHTMLPane.makeTable(new String[]{
                            "Combined incidence matrix <i>I</i>",
                            renderMatrix(data, data.getActiveTokenView().getIncidenceMatrix(
                                    data.getArcsArrayList(), data.getTransitionsArrayList(),
                                    data.getPlacesArrayList()))
                    }, 1, false, false, true, false);
                    s += ResultsHTMLPane.makeTable(new String[]{
                            "Inhibition matrix <i>H</i>",
                            renderMatrix(data, data.getActiveTokenView().getInhibitionMatrix(
                                    data.getInhibitorsArrayList(), data.getTransitionsArrayList(),
                                    data.getPlacesArrayList()))
                    }, 1, false, false, true, false);
                    s += ResultsHTMLPane.makeTable(new String[]{
                            "Marking",
                            renderMarkingMatrices(data)
                    }, 1, false, false, true, false);
                    s += ResultsHTMLPane.makeTable(new String[]{
                            "Enabled transitions",
                            renderTransitionStates(data)
                    }, 1, false, false, true, false);
                }
                catch(OutOfMemoryError oome)
                {
                    System.gc();
                    results.setText("");
                    s = "Memory error: " + oome.getMessage();

                    s += "<br>Not enough memory. Please use a larger heap size." + "<br>" + "<br>Note:" + "<br>The Java heap size can be specified with the -Xmx option." + "<br>E.g., to use 512MB as heap size, the command line looks like this:" + "<br>java -Xmx512m -classpath ...\n";
                    results.setText(s);
                    return;
                }
                catch(Exception e)
                {
                    //e.printStackTrace();
                    s = "<br>Invalid net";
                    results.setText(s);
                    return;
                }
            }
            results.setEnabled(true);
            results.setText(s);
        }
    };

    /*
     * @brief Format matrix as HTML
     * @param data petri net as read from the .pnml file, used to get transitions and places names
     * @param matrix the matrix that wants to be formatted into HTML
     */
    public String renderMatrix(PetriNetView data, int[][] matrix)
    {
        if((matrix.length == 0) || (matrix[0].length == 0))
        {
            return "n/a";
        }

        ArrayList result = new ArrayList();
        result.add("");
        for(int i = 0; i < matrix[0].length; i++)
        {
            result.add(data.getTransition(i).getName());
        }

        for(int i = 0; i < matrix.length; i++)
        {
            result.add(data.getPlace(i).getName());
            for(int j = 0; j < matrix[i].length; j++)
            {
                result.add(Integer.toString(matrix[i][j]));
            }
        }

        return ResultsHTMLPane.makeTable(
                result.toArray(), matrix[0].length + 1, false, true, true, true);
    }

    /*
   * @brief Format array as HTML
   * @param data petri net as read from the .pnml file, used to get places names
   */
    private String renderMarkingMatrices(PetriNetView data)
    {
        PlaceView[] placeViews = data.places();
        if(placeViews.length == 0)
        {
            return "n/a";
        }

        LinkedList<MarkingView>[] markings = data.getInitialMarkingVector();
        int[] initial = new int[markings.length];
        for(int i = 0; i < markings.length; i++)
        {
            if(markings[i].size()==0){
                initial[i] = 0;
            }else{
                initial[i] = markings[i].getFirst().getCurrentMarking();
            }
        }

        markings = data.getCurrentMarkingVector();
        int[] current = new int[markings.length];
        for(int i = 0; i < markings.length; i++)
        {
            current[i] = markings[i].getFirst().getCurrentMarking();
        }

        ArrayList result = new ArrayList();
        // add headers t o table
        result.add("");
        for(PlaceView placeView : placeViews)
        {
            result.add(placeView.getName());
        }

        result.add("Initial");
        for(int anInitial : initial)
        {
            result.add(Integer.toString(anInitial));
        }
        result.add("Current");
        for(int aCurrent : current)
        {
            result.add(Integer.toString(aCurrent));
        }

        return ResultsHTMLPane.makeTable(
                result.toArray(), placeViews.length + 1, false, true, true, true);
    }

    /*
   * @brief Format transitions states as HTML
   * @param data petri net as read from the .pnml file, used to get transitions names and properties
   */
    private String renderTransitionStates(PetriNetView data)
    {
        TransitionView[] transitionViews = data.getTransitionViews();
        if(transitionViews.length == 0)
        {
            return "n/a";
        }

        ArrayList result = new ArrayList();
        data.setEnabledTransitions();
        result.add("");
        for(TransitionView transitionView1 : transitionViews)
        {
            result.add(transitionView1.getName());
        }
        result.add("Enabled");
        for(TransitionView transitionView : transitionViews)
        {
            result.add((transitionView.isEnabled() ? "yes" : "no"));
        }
        data.resetEnabledTransitions();

        return ResultsHTMLPane.makeTable(
                result.toArray(), transitionViews.length + 1, false, false, true, true);
    }
}
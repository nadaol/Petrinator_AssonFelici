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

import org.petrinator.auxiliar.GraphFrame;
import org.petrinator.editor.Root;
import org.petrinator.editor.filechooser.*;
import org.petrinator.util.GraphicsTools;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.ResultsHTMLPane;
import java.util.Date;

import pipe.modules.reachability.ReachabilityGraphGenerator;
import pipe.views.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import pipe.calculations.StateSpaceGenerator;
import pipe.calculations.myTree;
import pipe.gui.ApplicationSettings;
import pipe.controllers.PipeApplicationController;
import pipe.models.PipeApplicationModel;
import net.sourceforge.jpowergraph.defaults.DefaultGraph;
import net.sourceforge.jpowergraph.defaults.DefaultNode;
import pipe.views.PetriNetView;

/**
 * Created module to produce the reachability graph representation of a Petri
 * net. If the petri net is bounded, then the reachability and coverability
 * graphs are the same. If it's not bounded, it's reachability graph is not
 * finit, so we generate the coverability one instead.
 *
 * @author Matthew Worthington
 * @author Edwin Chung
 * @author Will Master
 * @author Joaquin Rodriguez Felici
 */
public class ReachabilityAction extends AbstractAction
{
    Root root;
    String graphName = "";
    private ResultsHTMLPane results;

    public ReachabilityAction(Root root)
    {
        this.root = root;
        String name = "Reachabilty/Coverability graph";
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/graph16.png"));
    }

    public void actionPerformed(ActionEvent e)
    {
        /*
         * Create tmp.pnml file
         */
        FileChooserDialog chooser = new FileChooserDialog();

        if (root.getCurrentFile() != null) {
            chooser.setSelectedFile(root.getCurrentFile());
        }

        chooser.addChoosableFileFilter(new PipePnmlFileType());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(root.getCurrentDirectory());
        chooser.setDialogTitle("Save as...");

        File file = new File("tmp/" + "tmp" + "." + "pnml");
        FileType chosenFileType = (FileType) chooser.getFileFilter();
        try {
            chosenFileType.save(root.getDocument(), file);
        } catch (FileTypeException e1) {
            e1.printStackTrace();
        }

        /*
         * Show initial pane
         */
        EscapableDialog guiDialog = new EscapableDialog(root.getParentFrame(), "Reachabilty/Coverability graph", false);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        //sourceFilePanel = new PetriNetChooserPanel("Source net", null);
        results = new ResultsHTMLPane("");
        contentPane.add(results);
        contentPane.add(new ButtonBar("Generate graph", generateButtonClick, guiDialog.getRootPane()));
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
    }

    private final ActionListener generateButtonClick = new ActionListener() {
        public void actionPerformed(ActionEvent arg0) {
            /*
             * Read tmp file
             */
            PetriNetView sourcePetriNetView = new PetriNetView("tmp/tmp.pnml");
            String s = "<h2>Reachability/Coverability Graph Information</h2>";

            long start = new Date().getTime();
            long gfinished;
            long allfinished;
            double graphtime;
            double constructiontime;
            double totaltime;

            /*
             * Let's try to create the reachability graph
             */
            File reachabilityGraph = new File("results.rg");
            if(!root.getDocument().getPetriNet().getRootSubnet().hasPlaces() || !root.getDocument().getPetriNet().getRootSubnet().hasTransitions())
            {
                s += "Invalid net!";
            }
            else
            {
                try
                {
                    /*
                     * Check if petri net is bounded
                     */
                    LinkedList<MarkingView>[] markings = sourcePetriNetView.getCurrentMarkingVector();
                    int[] markup = new int[markings.length];
                    for(int k = 0; k < markings.length; k++)
                    {
                        markup[k] = markings[k].getFirst().getCurrentMarking();
                    }
                    myTree tree = new myTree(sourcePetriNetView, markup);
                    boolean bounded = !tree.foundAnOmega;

                    if(bounded)
                    {
                        StateSpaceGenerator.generate(sourcePetriNetView, reachabilityGraph);
                        graphName = "Reachability graph";
                        System.out.println("Reachability graph successfully created");
                    }
                    else
                    {
                         /*
                          * If we found the net to be unbounded, then we need to create the coverability graph
                          */
                        LinkedList<MarkingView>[] graphMarkings = sourcePetriNetView.getCurrentMarkingVector();
                        int[] currentMarking = new int[markings.length];
                        for(int i = 0; i < markings.length; i++)
                        {
                            currentMarking[i] = markings[i].getFirst().getCurrentMarking();
                        }
                        myTree graphTree = new myTree(sourcePetriNetView, currentMarking, reachabilityGraph);
                        graphName = "Coverability graph";
                        System.out.println("Coverability graph successfully created");
                    }

                    /*
                     * Let's show the results
                     */
                    gfinished = new Date().getTime();
                    System.gc();
                    generateGraph(reachabilityGraph, sourcePetriNetView, !bounded);
                    allfinished = new Date().getTime();
                    graphtime = (gfinished - start) / 1000.0;
                    constructiontime = (allfinished - gfinished) / 1000.0;
                    totaltime = (allfinished - start) / 1000.0;
                    DecimalFormat f = new DecimalFormat();
                    f.setMaximumFractionDigits(5);
                    s += "<br>Generating " + graphName + " took " +
                            f.format(graphtime) + "s";
                    s += "<br>Constructing it took " +
                            f.format(constructiontime) + "s";
                    s += "<br>Total time was " + f.format(totaltime) + "s";
                    results.setEnabled(true);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                results.setText(s);
            }
        }
    };

    public void generateGraph(File rgFile, PetriNetView dataLayer, boolean coverabilityGraph) throws Exception
    {
        ReachabilityGraphGenerator graphGenerator = new ReachabilityGraphGenerator();

        DefaultGraph graph = graphGenerator.createGraph(rgFile, dataLayer, coverabilityGraph);

        GraphFrame frame = new GraphFrame();
        PlaceView[] placeView = dataLayer.places();
        String legend = "";
        if (placeView.length > 0) {
            legend = "{" + placeView[0].getName();
        }
        for (int i = 1; i < placeView.length; i++) {
            legend += ", " + placeView[i].getName();
        }
        legend += "}";
        frame.setTitle(graphName);
        frame.constructGraphFrame(graph, legend, root);
        frame.toFront();
    }
}
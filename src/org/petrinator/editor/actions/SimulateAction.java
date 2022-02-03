package org.petrinator.editor.actions;

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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.petrinator.editor.Root;
import org.petrinator.editor.filechooser.FileChooserDialog;
import org.petrinator.editor.filechooser.FileType;
import org.petrinator.editor.filechooser.FileTypeException;
import org.petrinator.monitor.ConcreteObserver;
import org.petrinator.petrinet.*;
import org.petrinator.util.GraphicsTools;
import org.petrinator.editor.commands.FireTransitionCommand;
import org.petrinator.auxiliar.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;

import org.unc.lac.javapetriconcurrencymonitor.errors.DuplicatedNameError;
import org.unc.lac.javapetriconcurrencymonitor.errors.IllegalTransitionFiringError;
import org.unc.lac.javapetriconcurrencymonitor.exceptions.PetriNetException;
import org.unc.lac.javapetriconcurrencymonitor.monitor.PetriMonitor;
import org.unc.lac.javapetriconcurrencymonitor.monitor.policies.FirstInLinePolicy;
import org.unc.lac.javapetriconcurrencymonitor.monitor.policies.TransitionsPolicy;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.RootPetriNet;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MTransition;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.factory.PetriNetFactory;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.factory.PetriNetFactory.petriNetType;
import rx.Observer;
import rx.Subscription;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

/**
 * @author Joaquin Felici <joaquinfelici at gmail.com>
 * @brief Does N firings, one every Y seconds.
 * @detail Creates a monitor, subscribes to all transitions and creates
 * a thread for each one of them. Every thread will try to persistently fire
 * it's associated transition, until N firings have been executed.
 * Once's it's finished, a new thread is created, in charge of graphically
 * executing all these firings, one every Y seconds.
 */
public class SimulateAction extends AbstractAction
{
    private Root root;
    private List<FileType> fileTypes;
    protected static boolean stop = false;
    ActionEvent e;
    public static List<Double> instants = new ArrayList<Double>();
    private boolean running = false;

    public SimulateAction(Root root, List<FileType> fileTypes) {
        this.root = root;
        this.fileTypes = fileTypes;
        String name = "Simulate";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/play16.png"));
        putValue(SHORT_DESCRIPTION, name);
    }

    public void actionPerformed(ActionEvent e)
    {
        stop = false;

        /*
         * Create tmp.pnml file
         */
        FileChooserDialog chooser = new FileChooserDialog();

        if (root.getCurrentFile() != null)
        {
            chooser.setSelectedFile(root.getCurrentFile());
        }

        for (FileType fileType : fileTypes)
        {
            chooser.addChoosableFileFilter(fileType);
        }
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
         * Ask user to insert times
         */
        int numberOfTransitions = 1, timeBetweenTransitions = 1000;

        JTextField number = new JTextField(8);
        JTextField time = new JTextField(8);
        JPanel myPanel = new JPanel();
        myPanel.setLayout(new MigLayout());
        myPanel.add(new JLabel("Number of transitions:  "));
        myPanel.add(new JLabel ("    "));
        myPanel.add(number,"wrap");

        if(!root.getDocument().petriNet.getRootSubnet().anyStochastic())
        {
            myPanel.add(new JLabel("Time between transition:  "));
            myPanel.add(new JLabel ("    "));
            myPanel.add(time,"    ");
            myPanel.add(new JLabel("ms"));
        }

        time.setText("1000");
        number.setText("10");

        int result = JOptionPane.showConfirmDialog(root.getParentFrame(), myPanel, "Simulation time", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, GraphicsTools.getIcon("pneditor/play32.png"));
        if (result == JOptionPane.OK_OPTION)
        {
            try
            {
                numberOfTransitions = Integer.valueOf(number.getText());
                timeBetweenTransitions = Integer.valueOf(time.getText());
            }
            catch(NumberFormatException e1)
            {
                JOptionPane.showMessageDialog(null, "Invalid number");
                return; // Don't execute further code
            }
        }
        else {
            return; // Don't execute further code
        }

        setEnabled(false);

        /*
         * Run a single thread to fire the transitions graphically
         */
        final int a = numberOfTransitions; final int b= timeBetweenTransitions;
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                runInMonitor(a, b);
            }
        });
        t.start();

    }

    /*
     * @brief Creates monitor, threads for transitions, observer, and runs all threads.
     * @detail After getting all the firings the user set, it creates a thread that
     * will "fire" the transitions within our editor every x millis.
     */
    public void runInMonitor(int numberOfTransitions, int timeBetweenTransitions)
    {
        /*
         * Create monitor, petri net, and all related variables.
         */
        PetriNetFactory factory = new PetriNetFactory("tmp/tmp.pnml");
        RootPetriNet petri;

        try
        {  // The exception tell us if there's two places or transitions with the same name
            petri = factory.makePetriNet(petriNetType.PLACE_TRANSITION);
        } catch (DuplicatedNameError e)
        {
            JOptionPane.showMessageDialog(null, "Two places or transitions cannot have the same label");
            stop = false;
            setEnabled(true);
            return; // Don't execute further code
        }

        TransitionsPolicy policy = new FirstInLinePolicy();
        PetriMonitor monitor = new PetriMonitor(petri, policy);
        monitor.simulationRunning = true;

        petri.initializePetriNet();

		 /*
		  * Subscribe to all transitions
		  */
        Observer<String> observer = new ConcreteObserver(root);
        for(int i = 0; i < petri.getTransitions().length; i++)
        {
            MTransition t = petri.getTransitions()[i];
            Subscription subscription = monitor.subscribeToTransition(t, observer);
        }

		 /*
		  * Create one thread per transition, start them all to try and fire them.
		  */
        List<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < petri.getTransitions().length; i++)
        {
            if(!(root.getDocument().petriNet.getRootSubnet().getTransition(petri.getTransitions()[i].getId()).isAutomatic()))
            {
                Thread t = createThread(monitor, petri.getTransitions()[i].getName());
                threads.add(t);
                t.start();
            }
        }

        System.out.println("Simulation");
        System.out.println(" > Started firing");

        ProgressBarDialog dialog = new ProgressBarDialog(root, "Simulating...");
        dialog.show(true);

		 /*
		  * Wait for the number of events to occur
		  */
        while(true)
        {
            //System.out.println(((ConcreteObserver) observer).getEvents().size() + " | Tread " + threads.get(0).getId() + " " +  threads.get(0).getState() + " | Tread " + threads.get(1).getId() + " " + threads.get(1).getState() + "\n");

            //for(int i= 0; i<petri.getEnabledTransitions().length; i++)
            //    System.out.print(petri.getEnabledTransitions()[i]);

            if(((ConcreteObserver) observer).getEvents().size() >= numberOfTransitions)  // If there have been N events already
                break;
            else
            {
                try
                {
                    Thread.currentThread().sleep(10);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                // System.out.println(""); // Need at least one instruction in while, otherwise it will explode
                if(checkAllAre(petri.getEnabledTransitions(),false))   // We need to check if the net is blocked and no more transitions can be fored
                {
                    JOptionPane.showMessageDialog(root.getParentFrame(), "The net is blocked, " + ((ConcreteObserver) observer).getEvents().size() + " transitions were fired.");
                    break;
                }
                else if(blockedMonitor(threads, petri))
                {
                    JOptionPane.showMessageDialog(root.getParentFrame(), " \n The net is blocked. Make sure that at least one \n fired transition comes before the automatic ones.      \n ");
                    System.out.println(" > Monitor blocked");
                    break;
                }
            }
        }

        monitor.simulationRunning = false;
        System.out.println(" > Simulation started");
        dialog.show(false);

         /*
          * Stop all threads from firing
          */
        for(Thread t: threads)
        {
            t.stop();
        }

        /*
         * We simulate to press the EditTokens/EditTransition button so the enabled transitions
         * will be shown in green.
         */
        new TokenSelectToolAction(root).actionPerformed(e);

        /*
         * We fire the net graphically
         */
        running = true;
        instants.clear();
        for(Place place : root.getDocument().petriNet.getRootSubnet().getPlaces())
        {
            place.clearValues();
        }
        analyzePlaces(timeBetweenTransitions);
        fireGraphically(((ConcreteObserver) observer).getEvents(), timeBetweenTransitions, numberOfTransitions);
        new SelectionSelectToolAction(root).actionPerformed(e);

        running = false;
        System.out.println(" > Simulation ended");
        setEnabled(true);
    }

    /*
     * @brief Creates a thread that tries to fire one given transition
     * @param m the monitor that holds our petri net
     * @param id the id of the transition this thread will try to fire
     * @return t the created tread
     */
    Thread createThread(PetriMonitor m, String id)
    {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run()
            {
                while(true)
                {
                    try
                    {
                        Thread.sleep(new Random().nextInt(50)); // Random value between 0 and 50 ms
                        m.fireTransition(id);
                    } catch (IllegalTransitionFiringError | IllegalArgumentException | PetriNetException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        return t;
    }

    /*
     * @brief Takes the list of events, and performs one per one, every x millis (set by user)
     * @param list list of events
     * @param timeBetweenTransitions milliseconds to wait between events performed
     * @return
     */
    void fireGraphically(List<String> list, int timeBetweenTransitions, int numberOfTransitions)
    {
        /*
         * If we wanna keep track of the current iteration, we need to do it with a separate variable,
         * because the list usually has EQUAL objects (such as two equal strings that indicate that the
         * same transition was fired twice), and the method indexOf(element) returns the index of the
         * first occurrence in the list (and as the strings are equal, the  objects are equal), so
         * we might get the index of the first event that fired this transition, not the current one.
         */
        int i = 0;
        for(String event : list)
        {
            /*
             * Check if stop button has been pressed
             */
            if(stop)
            {
                stop = false;
                setEnabled(true);
                list.clear();
                System.out.println(" > Simulation stopped by user");
                return;
            }

            System.out.println(event);
            List<String> transitionInfo = Arrays.asList(event.split(","));
            String transitionId = transitionInfo.get(2);
            transitionId = transitionId.replace("\"", "");
            transitionId = transitionId.replace("id:", "");
            transitionId = transitionId.replace("}", "");

            double time = 0;
            try
            {
                String _time  = transitionInfo.get(3);
                _time = _time.replace("\"", "");
                _time = _time.replace("time:", "");
                _time = _time.replace("}", "");
                time = Double.parseDouble(_time) * 1000;
            }
            catch (ArrayIndexOutOfBoundsException e) {} // The transition is not timed, so no time to retrieve. No biggy.


            Transition transition = root.getDocument().petriNet.getRootSubnet().getTransition(transitionId);
            Marking marking = root.getDocument().petriNet.getInitialMarking();

            //System.out.println(transition.getLabel() + " was fired!");
            root.getEventList().addEvent((transition.getLabel() + " was fired!"));

            if(transition.isTimed())
            {
                transition.setTime((int) time);
                transition.setWaiting(true);
                countDown(transition);

                try
                {
                    System.out.println("Sleeping " + (int) time);
                    Thread.currentThread().sleep((int) time);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                transition.setWaiting(false);
            }

            FireTransitionCommand fire = new FireTransitionCommand(transition, marking);
            fire.execute();
            root.refreshAll();

            /*
             * Maybe, if several threads executed multiple transitions concurrently,
             * there are more events than "numberOfTransitions" specified.
             * Let's make sure we won't fire more than "numberOfTransitions"
             */
            if(++i >= numberOfTransitions)
            {
                setEnabled(true);
                return;
            }

            if(!root.getDocument().petriNet.getRootSubnet().anyStochastic())
            {
                try
                {
                    Thread.currentThread().sleep(timeBetweenTransitions);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            else
            {
                try
                {
                    Thread.currentThread().sleep(50);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /*
    * @brief Checks if all booleans in array are either true or false
    * @param array that contains all booleans
    * @param value the value that we want all the booleans to have
    * @returns true if all match, false otherwise
    */
    static boolean checkAllAre(boolean[] array, boolean value)
    {
        for(int i = 0; i < array.length; i++)
        {
            if(array[i] != value)
                return false;
        }
        return true;
    }

    static boolean blockedMonitor(List<Thread> threads, RootPetriNet p)
    {
        for(Thread t: threads)
        {
            if((t.getState() != Thread.State.WAITING) || p.anyWaiting())
                return false;
        }
        return true;
    }

    public void countDown(Transition t)
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                long begin = System.currentTimeMillis();

                while(t.getTime()>1)
                {
                    try
                    {
                        root.repaintCanvas();
                        Thread.currentThread().sleep(5);
                        t.setTime((int) (t.getTime()-(System.currentTimeMillis()-begin)));
                        begin = System.currentTimeMillis();
                    } catch (IllegalTransitionFiringError | IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void analyzePlaces(int timeBetweenTransitions)
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                double time = 0;
                int timeToSleep = 0;

                if(root.getDocument().petriNet.getRootSubnet().anyStochastic())
                {
                    timeToSleep = 500;
                }
                else
                {
                    timeToSleep = timeBetweenTransitions;
                }

                while(running)
                {
                    Marking marking = root.getDocument().petriNet.getInitialMarking();
                    Set<Place> places = root.getDocument().petriNet.getRootSubnet().getPlaces();

                    for(Place place : places)
                    {
                        place.addValue(marking.getTokens(place));
                    }

                    instants.add(time);

                    time += (double) timeToSleep / 1000;
                    time = roundDouble(time);

                    try
                    {
                        Thread.sleep(timeToSleep);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public static double roundDouble(double value)
    {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
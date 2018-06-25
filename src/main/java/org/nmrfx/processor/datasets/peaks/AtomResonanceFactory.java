/*
 * NMRFx Structure : A Program for Calculating Structures 
 * Copyright (C) 2004-2017 One Moon Scientific, Inc., Westfield, N.J., USA
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
package org.nmrfx.processor.datasets.peaks;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import static org.nmrfx.processor.datasets.peaks.AtomResonance.resonanceLoopStrings;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Molecule;

/**
 *
 * @author Bruce Johnson
 */
public class AtomResonanceFactory extends ResonanceFactory implements FreezeListener {

    Map<Long, AtomResonance> map = new HashMap<>();
    private long lastID = -1;
    private Long[] arrayView = null;

    public AtomResonanceFactory() {
    }

    @Override
    public void init() {
        System.out.println("register freeze listener");
        PeakList.registerFreezeListener(this);
    }

    public Resonance build() {
        lastID++;
        AtomResonance resonance = new AtomResonance(lastID);
        map.put(lastID, resonance);
        return resonance;
    }

    public Resonance build(long id) {
        AtomResonance resonance = (AtomResonance) get(id);
        if (resonance == null) {
            resonance = new AtomResonance(id);
            map.put(id, resonance);
        }
        return resonance;
    }

    public Resonance get(long id) {
        return map.get(id);
    }

    public void clean() {
        Map<Long, AtomResonance> resonancesNew = new TreeMap<Long, AtomResonance>();
        for (Map.Entry<Long, AtomResonance> entry : map.entrySet()) {
            AtomResonance resonance = entry.getValue();
            if (((resonance.getPeakDims() != null) && (resonance.getPeakDims().size() != 0))) {
                resonancesNew.put(entry.getKey(), resonance);
            }
        }
        map.clear();
        map = resonancesNew;
        arrayView = null;
    }

    public synchronized HashMap<String, ArrayList<AtomResonance>> getLabelMap() {
        clean();
        HashMap<String, ArrayList<AtomResonance>> labelMap = new HashMap<>();
        map.values().forEach((resonance) -> {
            String label = resonance.getName();
            if ((label != null) && (label.length() != 0)) {
                label = label.trim().toUpperCase();
                if ((label.length() > 1) && Character.isLetter(label.charAt(0)) && Character.isDigit(label.charAt(1))) {
                    label = label.substring(1);
                }
                ArrayList resList = labelMap.get(label);
                if (resList == null) {
                    resList = new ArrayList<>();
                    labelMap.put(label, resList);
                }

                resList.add(resonance);
            }
        });
        return labelMap;
    }

    public AtomResonance merge(AtomResonance resonanceA, AtomResonance resonanceB) {
        if (resonanceA == resonanceB) {
            return null;
        }
        // FIXME  should we also test if they have names assigned and the names are different
        if ((resonanceA.getAtom() != null) && (resonanceB.getAtom() != null)) {
            if (resonanceA.getAtom() != resonanceB.getAtom()) {
                System.out.println("resonance merge:  both resonances have atoms");
                return null;
            }
        }
        if ((resonanceA.getAtom() == null) && (resonanceB.getAtom() != null)) {
            AtomResonance hold = resonanceA;
            resonanceA = resonanceB;
            resonanceB = hold;
        } else if (resonanceA.getName().equals("") && !resonanceB.getName().equals("")) {
            AtomResonance hold = resonanceA;
            resonanceA = resonanceB;
            resonanceB = hold;
        }
        for (PeakDim peakDim : resonanceB.peakDims) {
            resonanceA.add(peakDim);

        }
        resonanceB.peakDims = null;
        map.remove(resonanceB.id);
        arrayView = null;
        return resonanceA;
    }

    public synchronized void merge(String condition, double tol) {
        HashMap<String, ArrayList<AtomResonance>> labelMap = getLabelMap();
        for (String label : labelMap.keySet()) {
            ArrayList<AtomResonance> resList = labelMap.get(label);
            // find res with atom
            // if none find res closest to mean
            // or resonance with most peaks    
            // merge remaining
            AtomResonance refRes = null;
            for (AtomResonance res : resList) {
                if (res.getPeakCount(condition) > 0) {
                    if (res.getAtom() != null) {
                        refRes = res;
                        break;
                    }
                }
            }
            if (refRes == null) {
                int maxCount = 0;
                for (AtomResonance res : resList) {
                    int nPeakDims = res.getPeakCount(condition);
                    if (nPeakDims > maxCount) {
                        maxCount = nPeakDims;
                        refRes = res;
                    }
                }
            }
            if (refRes != null) {
                Double ppmAvg = refRes.getPPMAvg(null);
                Double widthAvg = refRes.getWidthAvg(null);
                if (ppmAvg == null) {
                    continue;
                }
                if (widthAvg < 0.05) {
                    widthAvg = 0.05;
                }
                for (AtomResonance res : resList) {
                    if (res == refRes) {
                        continue;
                    }
                    if (res.getPeakCount(condition) == 0) {
                        continue;
                    }
                    double ppmAvg2 = res.getPPMAvg(null);
                    double delta = Math.abs(ppmAvg - ppmAvg2);
                    if (delta < (tol * widthAvg)) {
                        refRes = merge(refRes, res);
                    }
                }
            }
        }
        arrayView = null;
    }

    public void assignFrozenAtoms(String condition) {
        for (AtomResonance res : map.values()) {
            for (PeakDim peakDim : res.peakDims) {
                if (peakDim.getPeak().getPeakList().getSampleConditionLabel().equals(condition)) {
                    if (peakDim.isFrozen()) {
                        Double ppmAvg = res.getPPMAvg(condition);
                        Atom atom = Molecule.getAtomByName(peakDim.getLabel());
                        if (atom != null) {
                            atom.setPPM(ppmAvg);
                            res.atomName = atom.getFullName();
                            break;
                        }
                    }
                }

            }

        }
    }

    @Override
    public void freezeHappened(Peak peak, boolean state) {
        System.out.println("freeze " + peak.getName() + " " + state);
        for (PeakDim peakDim : peak.peakDim) {
            String condition = peak.getPeakList().getSampleConditionLabel();
            AtomResonance res = (AtomResonance) peakDim.getResonance();
            Double ppmAvg = res.getPPMAvg(condition);
            Atom atom = Molecule.getAtomByName(peakDim.getLabel());
            System.out.println(ppmAvg);
            if (peakDim.isFrozen()) {
                if (atom != null) {
                    if (ppmAvg != null) {
                        atom.setPPM(ppmAvg);
                    }
                    res.atomName = atom.getFullName();
                }
            } else {
                atom.setPPMValidity(0, false);
                res.atomName = "";
            }
        }
    }

    public void writeResonancesSTAR3(FileWriter chan)
            throws IOException {

        chan.write("save_resonance_linker_list\n");

        chan.write("_Resonance_linker_list.Sf_category    ");
        chan.write("resonance_linker\n");

        chan.write("_Resonance_linker_list.Sf_framecode   ");
        chan.write("resonance_linker_list\n");

        chan.write("_Resonance_linker_list.Details        ");
        chan.write(".\n");

        chan.write("\n");
        String[] loopStrings = resonanceLoopStrings;
        chan.write("loop_\n");
        for (int j = 0; j < loopStrings.length; j++) {
            chan.write(loopStrings[j] + "\n");
        }
        chan.write("\n");
        for (Map.Entry<Long, AtomResonance> entry : map.entrySet()) {
            AtomResonance resonance = entry.getValue();
            if (resonance == null) {
                throw new IOException("Resonance.writeResonances: resonance null at ");
            }
            chan.write(resonance.toSTARResonanceString() + "\n");
        }
        chan.write("stop_\n");
        chan.write("save_\n");

    }

}

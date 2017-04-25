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

import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Residue;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ResonanceSet implements Comparable {

    static long lastID = -1;
    private Long ID = Long.valueOf(-1);
    static Map resonanceSets = new TreeMap();
    private ArrayList resonances = new ArrayList();
    private Atom atom = null;

    private ResonanceSet(long id) {
        ID = Long.valueOf(id);
        // FIXME  is this really right?
        if (lastID <= id) {
            lastID = id + 1;
        }
    }

    public ResonanceSet(AtomResonance resonance) {
        ID = Long.valueOf(++lastID);
        resonances.add(resonance);
        resonanceSets.put(ID, this);
        resonance.setResonanceSet(this);

    }

    public static ResonanceSet newInstance(long id) {
        ResonanceSet resonanceSet = new ResonanceSet(id);
        resonanceSets.put(resonanceSet.getIDAsLong(), resonanceSet);
        return resonanceSet;
    }

    public static void clear() {
        resonanceSets.clear();
        resonanceSets = new TreeMap();
    }

    public static boolean exists(long id) {
        return resonanceSets.containsKey(Long.valueOf(id));
    }

    public static ResonanceSet get(long id) {
        ResonanceSet resonanceSet = (ResonanceSet) resonanceSets.get(Long.valueOf(id));
        return resonanceSet;
    }

    public long getID() {
        return ID.longValue();
    }

    public Long getIDAsLong() {
        return ID;
    }

    public String getIDString() {
        return ID.toString();
    }

    public int compareTo(Object o) {
        int result = 1;
        if (o instanceof ResonanceSet) {
            ResonanceSet oResonanceSet = (ResonanceSet) o;
            result = ID.compareTo(oResonanceSet.ID);
        }
        return result;
    }

    public void setAtom(Atom atom) {
        this.atom = atom;
        if (atom != null) {
            Iterator iter = resonances.iterator();
            while (iter.hasNext()) {
                Resonance resonance = (Resonance) iter.next();
                resonance.setName(atom.getShortName());
            }
        }
    }

    public Atom getAtom() {
        return atom;
    }

    public void addResonance(AtomResonance resonance) {
        if (!resonances.contains(resonance)) {
            resonances.add(resonance);
            resonance.setResonanceSet(this);
        }
    }

    public ArrayList getResonances() {
        return resonances;
    }

    public void merge(ResonanceSet resonanceSet) {
        if (this == resonanceSet) {
            return;
        }
        ArrayList resonances2 = resonanceSet.getResonances();
        if (resonances2 != null) {
            Iterator iter = resonances2.iterator();
            while (iter.hasNext()) {
                AtomResonance resonance = (AtomResonance) iter.next();
                addResonance(resonance);
            }
            resonanceSet.resonances = null;
            resonanceSet.remove();
        } else {
            System.out.println("resonances2 is null  ***************************");
        }
    }

    public void removeResonance(AtomResonance resonance) {
        resonances.remove(resonance);
        if (resonances.size() == 0) {
            remove();
        }
    }

    public void remove() {
        resonanceSets.remove(getIDAsLong());
        ID = Long.valueOf(-1);
    }
    static String[] resonanceAssignmentStrings = {
        "_Resonance_assignment.Resonance_set_ID",
        "_Resonance_assignment.Assembly_atom_ID",
        "_Resonance_assignment.Entity_assembly_ID",
        "_Resonance_assignment.Entity_ID",
        "_Resonance_assignment.Comp_index_ID",
        "_Resonance_assignment.Comp_ID",
        "_Resonance_assignment.Atom_ID",
        "_Resonance_assignment.Atom_isotope_number",
        "_Resonance_assignment.Atom_set_ID",
        "_Resonance_assignment.Resonance_linker_list_ID"
    };

    String toSTARResonanceSetAssignmentString(Atom atom) {
        Entity entity = atom.getEntity();
        int entityAssemblyID = entity.assemblyID;
        if (entity instanceof Residue) {
            entityAssemblyID = ((Residue) entity).polymer.assemblyID;
        }

        StringBuffer result = new StringBuffer();
        String sep = " ";
        result.append(getID());                       //  Resonance_set_ID
        result.append(sep);
        result.append(entityAssemblyID);                           //  Entity_assembly_ID
        result.append(sep);
        result.append(".");                           //  Entity_assembly_ID
        result.append(sep);
        int entityID = entity.getIDNum();
        if (entity instanceof Residue) {
            entityID = ((Residue) entity).polymer.getIDNum();
        }

        result.append(entityID);                           //  Entity__ID
        result.append(sep);
        int number = atom.getEntity().getIDNum();
        result.append(number);    //  Comp_index_ID
        result.append(sep);
        result.append(atom.getEntity().getName());    //  Comp_ID
        result.append(sep);
        result.append(atom.getName());                //  Atom_ID
        result.append(sep);
        //FIXME result.append(atom.getIsotopeNumber());                //  Atom_isotope_number
        result.append(".");
        result.append(sep);
        result.append(".");                           //  Atom_set_ID
        result.append(sep);
        result.append(1);                             // Resonance_linker_list_ID
        return result.toString();
    }

    public static void writeResonanceSetsSTAR3(FileWriter chan)
            throws IOException, InvalidPeakException {

        String[] loopStrings = resonanceAssignmentStrings;
        chan.write("loop_\n");
        for (int j = 0; j < loopStrings.length; j++) {
            chan.write(loopStrings[j] + "\n");
        }
        chan.write("\n");
        boolean wroteAtLeastOne = false;
        Iterator iter = resonanceSets.values().iterator();
        while (iter.hasNext()) {
            ResonanceSet resonanceSet = (ResonanceSet) iter.next();
            if (resonanceSet == null) {
                throw new InvalidPeakException("ResonanceSet.writeResonanceSets: resonanceSet null at ");
            }
            Atom atom = resonanceSet.atom;
            if (atom != null) {
                String string = resonanceSet.toSTARResonanceSetAssignmentString(atom);
                if (string != null) {
                    chan.write(string + "\n");
                    wroteAtLeastOne = true;
                }
            }
        }
        if (!wroteAtLeastOne) {
            chan.write("? ? ? ? ? ? ? ? ? ?\n");
        }
        chan.write("stop_\n");
        chan.write("\n");
    }
}

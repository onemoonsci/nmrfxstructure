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

package org.nmrfx.structure.chemistry.constraints;

import org.nmrfx.project.StructureProject;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.Point3;
import java.util.*;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author brucejohnson
 */
public class AngleConstraintSet implements ConstraintSet, Iterable {

    private static HashMap<String, AngleConstraintSet> angleSets () {
        return StructureProject.getActive().angleSets;
    }
    private ArrayList<AngleConstraint> constraints = new ArrayList<AngleConstraint>(64);
    int nStructures = 0;
    private final String name;
    boolean dirty = true;
    public static char[] violCharArray = new char[0];
    public static int ID = 1;

    public AngleConstraintSet(String name) {
        this.name = name;
    }

    public static AngleConstraintSet addSet(String name) {
        AngleConstraintSet angleSet = new AngleConstraintSet(name);
        angleSets().put(name, angleSet);
        AngleConstraint.setActive(angleSet);
        return angleSet;

    }

    public String getName() {
        return name;
    }

    public static void reset() {
        for (Map.Entry<String, AngleConstraintSet> cSet : angleSets().entrySet()) {
            cSet.getValue().clear();
        }
        angleSets().clear();
        addSet("default");
    }

    public String getCategory() {
        return "torsion_angle_constraints";
    }

    public String getListType() {
        return "_Torsion_angle_constraint_list";
    }

    public String getType() {
        return "dihedral angle";
    }

    public static AngleConstraintSet getSet(String name) {
        AngleConstraintSet noeSet = angleSets().get(name);
        return noeSet;
    }

    public static ArrayList<String> getNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (String name : angleSets().keySet()) {
            names.add(name);
        }
        return names;
    }

    public int getSize() {
        return constraints.size();
    }

    public void clear() {
        constraints.clear();
    }

    public void add(Constraint constraint) {
        constraints.add((AngleConstraint) constraint);
        dirty = true;
    }

    public ArrayList<AngleConstraint> get() {
        if (dirty) {
            updateAngleData();
        }
        return constraints;
    }

    public AngleConstraint get(int i) {
        return constraints.get(i);
    }

    public Iterator iterator() {
        return constraints.iterator();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        dirty = true;
    }

    public void updateAngleData() {
        Molecule mol = Molecule.getActive();
        if (mol == null) {
            return;
        }
        int[] structures = mol.getActiveStructures();
        if (structures.length == 0) {
            structures = new int[1];
        }
        int lastStruct = 0;
        for (int iStruct : structures) {
            lastStruct = iStruct > lastStruct ? iStruct : lastStruct;
        }
        nStructures = structures.length;
        violCharArray = new char[lastStruct + 1];
        SummaryStatistics sumStat = new SummaryStatistics();
        for (AngleConstraint aConstraint : constraints) {
            sumStat.clear();
            int nInBounds = 0;
            BitSet violStructures = new BitSet(nStructures);
            Point3[] pts = new Point3[4];
            boolean okPoint = true;
            for (int iStruct : structures) {
                for (int i = 0; i < 4; i++) {
                    pts[i] = aConstraint.getSpSets()[i].getPoint(iStruct);
                    if (pts[i] == null) {
                        System.out.println(i + " " + aConstraint.getSpSets()[i].getFullName() + " has null point");
                        okPoint = false;
                    }
                }
                if (!okPoint) {
                    break;
                }
                double angle = Atom.calcDihedral(pts[0], pts[1], pts[2], pts[3]) * 180.0 / Math.PI;
                double lower = aConstraint.getLower();
                double upper = aConstraint.getUpper();
                boolean ok = false;
                if (upper > lower) {
                    if ((angle >= lower) && (angle <= upper)) {
                        ok = true;
                    }
                } else if ((angle >= lower) || (angle <= upper)) {
                    ok = true;
                }
                if (ok) {
                    nInBounds++;
                } else {
                    violStructures.set(iStruct);
                }
                sumStat.addValue(angle);
            }
            if (okPoint) {

                double stdDevDis = Math.sqrt(sumStat.getVariance());
                DistanceStat dStat = new DistanceStat(sumStat.getMin(), sumStat.getMax(), sumStat.getMean(), stdDevDis, (double) nInBounds / nStructures, violStructures);
                aConstraint.setDisStat(dStat);
            }

        }
        dirty = false;
    }
    private static String[] angleConstraintLoopStrings = {
        "_Torsion_angle_constraint.ID",
        "_Torsion_angle_constraint.Torsion_angle_name",
        "_Torsion_angle_constraint.Assembly_atom_ID_1",
        "_Torsion_angle_constraint.Entity_assembly_ID_1",
        "_Torsion_angle_constraint.Entity_ID_1",
        "_Torsion_angle_constraint.Comp_index_ID_1",
        "_Torsion_angle_constraint.Seq_ID_1",
        "_Torsion_angle_constraint.Comp_ID_1",
        "_Torsion_angle_constraint.Atom_ID_1",
        "_Torsion_angle_constraint.Atom_type_1",
        "_Torsion_angle_constraint.Resonance_ID_1",
        "_Torsion_angle_constraint.Assembly_atom_ID_2",
        "_Torsion_angle_constraint.Entity_assembly_ID_2",
        "_Torsion_angle_constraint.Entity_ID_2",
        "_Torsion_angle_constraint.Comp_index_ID_2",
        "_Torsion_angle_constraint.Seq_ID_2",
        "_Torsion_angle_constraint.Comp_ID_2",
        "_Torsion_angle_constraint.Atom_ID_2",
        "_Torsion_angle_constraint.Atom_type_2",
        "_Torsion_angle_constraint.Resonance_ID_2",
        "_Torsion_angle_constraint.Assembly_atom_ID_3",
        "_Torsion_angle_constraint.Entity_assembly_ID_3",
        "_Torsion_angle_constraint.Entity_ID_3",
        "_Torsion_angle_constraint.Comp_index_ID_3",
        "_Torsion_angle_constraint.Seq_ID_3",
        "_Torsion_angle_constraint.Comp_ID_3",
        "_Torsion_angle_constraint.Atom_ID_3      _Torsion_angle_constraint.Atom_type_3",
        "_Torsion_angle_constraint.Resonance_ID_3",
        "_Torsion_angle_constraint.Assembly_atom_ID_4",
        "_Torsion_angle_constraint.Entity_assembly_ID_4",
        "_Torsion_angle_constraint.Entity_ID_4",
        "_Torsion_angle_constraint.Comp_index_ID_4",
        "_Torsion_angle_constraint.Seq_ID_4",
        "_Torsion_angle_constraint.Comp_ID_4",
        "_Torsion_angle_constraint.Atom_ID_4",
        "_Torsion_angle_constraint.Atom_type_4",
        "_Torsion_angle_constraint.Resonance_ID_4",
        "_Torsion_angle_constraint.Angle_lower_bound_val",
        "_Torsion_angle_constraint.Angle_upper_bound_val",
        "_Torsion_angle_constraint.Entry_ID",
        "_Torsion_angle_constraint.Gen_dist_constraint_list_ID",};

    public String[] getLoopStrings() {
        return angleConstraintLoopStrings;
    }

    public void resetWriting() {
        ID = 1;
    }
}

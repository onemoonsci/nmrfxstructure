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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Bruce Johnson
 */
public class AtomResonanceFactory extends ResonanceFactory {

    Map<Long, AtomResonance> map = new HashMap<>();
    private static long lastID = -1;
    private static Long[] arrayView = null;

    public Resonance build() {
        lastID++;
        AtomResonance resonance = new AtomResonance(lastID);
        map.put(lastID, resonance);
        return resonance;
    }

    public Resonance build(long id) {
        Resonance resonance = new AtomResonance(id);
        return resonance;
    }

    public Resonance build(PeakDimContrib pdC) {
        Resonance resonance = build();
        resonance.addPeakDimContrib(pdC);
        return resonance;
    }

    public Resonance get(long id) {
        return map.get(id);
    }

    public void clean() {
        Map<Long, AtomResonance> resonancesNew = new TreeMap<Long, AtomResonance>();
        for (Map.Entry<Long, AtomResonance> entry : map.entrySet()) {
            AtomResonance resonance = entry.getValue();
            if (((resonance.pdCs != null) && (resonance.pdCs.size() != 0))) {
                resonancesNew.put(entry.getKey(), resonance);
            } else if (resonance.getResonanceSet() != null) {
                resonance.getResonanceSet().removeResonance(resonance);
            }
        }
        map.clear();
        map = resonancesNew;
        arrayView = null;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.datasets.peaks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.Residue;

/**
 *
 * @author brucejohnson
 */
public class PeakLabeller {

    static Pattern rPat = Pattern.compile("^(.+:)?(([a-zA-Z]+)([0-9\\-]+))\\.(['a-zA-Z0-9u]+)$");
    static Pattern rPat2 = Pattern.compile("^(.+:)?([0-9\\-]+)\\.(['a-zA-Z0-9u]+)$");
    static Pattern rPat3 = Pattern.compile("^(.+:)?([a-zA-Z]+)([0-9\\-]+)\\.(['a-zA-Z0-9u]+)$");

    public static void labelWithSingleResidueChar(PeakList peakList) {
        peakList.peaks().stream().forEach(pk -> {
            for (PeakDim peakDim : pk.getPeakDims()) {
                String label = peakDim.getLabel();
                Matcher matcher1 = rPat.matcher(label);
                if (!matcher1.matches()) {
                    Matcher matcher2 = rPat2.matcher(label);
                    if (matcher2.matches()) {
                        String chain = matcher2.group(1);
                        String resNum = matcher2.group(2);
                        String aName = matcher2.group(3);
                        String atomSpec = chain + resNum + "." + aName;
                        Atom atom = Molecule.getAtomByName(atomSpec);
                        if (atom != null) {
                            if (atom.getEntity() instanceof Residue) {
                                char oneChar = ((Residue) atom.getEntity()).getOneLetter();
                                StringBuilder sBuilder = new StringBuilder();
                                if (chain != null) {
                                    sBuilder.append(chain);
                                }
                                sBuilder.append(oneChar).append(resNum);
                                sBuilder.append(".").append(aName);
                                peakDim.setLabel(sBuilder.toString());
                            }
                        }
                    }
                }
            }
        });
    }

    public static void removeSingleResidueChar(PeakList peakList) {
        peakList.peaks().stream().forEach(pk -> {
            for (PeakDim peakDim : pk.getPeakDims()) {
                String label = peakDim.getLabel();
                Matcher matcher1 = rPat3.matcher(label);
                if (matcher1.matches()) {
                    String chain = matcher1.group(1);
                    String resNum = matcher1.group(3);
                    String aName = matcher1.group(4);
                    StringBuilder sBuilder = new StringBuilder();
                    if (chain != null) {
                        sBuilder.append(chain);
                    }
                    sBuilder.append(resNum);
                    sBuilder.append(".").append(aName);
                    peakDim.setLabel(sBuilder.toString());
                }
            }
        }
        );
    }
}

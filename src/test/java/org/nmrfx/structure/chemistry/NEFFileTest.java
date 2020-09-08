/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.structure.chemistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Assert;
import org.junit.Test;
import org.nmrfx.structure.chemistry.io.NMRNEFReader;
import org.nmrfx.structure.chemistry.io.NMRNEFWriter;

/**
 *
 * @author Martha
 */
public class NEFFileTest {

    // old files:
    // ok 1pqx 2k2e 2kpu 2kw5 2loy 2luz
    // HH% twice 2jr2 2juw 2kko
    // HB% twice etc 2ko1
    // Asp HD2 2kzn   should have residue modifier +HD2
    // ILE CGx CGy  (these are not stereo equiv  2png
    // has ligand 6nbn
    
    // new files:
    // OK 1pqx 2jr2 2juw 2k2e 2kcu 2kpu 2kw5 2ko1

    // MISTAKES IN ORIGINAL FILES
    // SEQUENCE 6nbn has unreadable residue ACD
    // CHEM SHIFT 2k07 only one each of 90 HD% and HE% in written when 2 each in orig (HD/E% repeats mistake in original file, should be HD1/2, HE1/2, etc.)
    // DISTANCE 2loy 2kzn dict key mismatches, written HE2% should be HE% (HE% mistake in original file, should be HE2%. GLN wildcards are HB%, HG%, and HE2%)
    // DISTANCE 2k07 2kko 2luz (previously passed, now fails) dict key mismatches, e.g. written HDx/y% should be HD1/2% (based on response below, mistake in original file. HDx/y% is correct, not HD1/2%)

    // ISSUES TO BE ADDRESSED
    // DISTANCE 2png dict key mismatches, e.g. written HG% should be HGy and HGx (multiple lines w/ same restraint not getting split correctly) 
    List<List<Object>> orig = new ArrayList<>();
    List<List<Object>> written = new ArrayList<>();

    @Test
    public void testFile2KO1() throws IOException {
        loadData("2ko1");
        testAll();
    }
//    @Test
//    public void testFile2PNG() throws IOException {
//        loadData("2png");
//        testAll();
//    }
//    @Test
//    public void testFile2KZN() throws IOException {
//        loadData("2kzn");
//        testAll();
//    }
//    @Test
//    public void testFile2KKO() throws IOException {
//        loadData("2kko");
//        testAll();
//    }
    @Test
    public void testFile2JUW() throws IOException {
        loadData("2juw");
        testAll();
    }

    @Test
    public void testFile2JR2() throws IOException {
        loadData("2jr2");
        testAll();
    }

    @Test
    public void testFile1PQX() throws IOException {
        loadData("1pqx");
        testAll();
    }
    
    @Test
    public void testFile1PQX2() throws IOException {
        loadData("1pqx_2");
        testAll();
    }

    @Test
    public void testFile2K2E() throws IOException {
        loadData("2k2e");
        testAll();
    }

    @Test
    public void testFile2KPU() throws IOException {
        loadData("2kpu");
        testAll();
    }

    @Test
    public void testFile2KW5() throws IOException {
        loadData("2kw5");
        testAll();
    }

//    @Test
//    public void testFile2LOY() throws IOException {
//        loadData("2loy");
//        testAll();
//    }
//    @Test
//    public void testFile2LUZ() throws IOException {
//        loadData("2luz");
//        testAll();
//    }
//    
//    @Test
//    public void testFile2K07() throws IOException {
//        loadData("2k07");
//        testAll();
//    }

    @Test
    public void testFile2KCU() throws IOException {
        loadData("2kcu");
        testAll();
    }
    
//    @Test
//    public void testFile6NBN() throws IOException {
//        loadData("6nbn");
//        testAll();
//    }

    private List<List<Object>> convertFileLines(String filePath) throws FileNotFoundException, IOException {
        List<List<Object>> convertedLines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            List<String> sLine = Arrays.asList(line.trim().split("\\s+"));
            List<Object> cLine = new ArrayList<>();
            for (String s : sLine) {
                try {
                    cLine.add(Integer.parseInt(s));
                } catch (NumberFormatException ex1) {
                    try {
                        double sD = Double.parseDouble(s);
                        cLine.add(sD);
                    } catch (NumberFormatException ex2) {
                        cLine.add(s);
                    }
                }
            }
            convertedLines.add(cLine);
        }
        return convertedLines;
    }

    private Map<String, List<Object>> buildSequenceMap(List<List<Object>> dataArray) {
        Map<String, List<Object>> seqMap = new HashMap<>();
        boolean inSeq = false;
        for (List<Object> line : dataArray) {
            if (line.size() > 0) {
                if (line.get(0).toString().contains("_sequence.index")) {
                    inSeq = true;
                }
                if (inSeq) {
                    int iChain = 1;
                    int iSeq = 2;
                    if (line.size() > 1 && line.get(0) instanceof Integer) {
                        String key = line.get(iChain) + "." + line.get(iSeq).toString();
                        List<Object> values = new ArrayList<>();
                        for (int i = 3; i < line.size(); i++) {
                            values.add(line.get(i));
                        }
                        seqMap.put(key, values);
                    } else if (line.contains("stop_")) {
                        break;
                    }
                }
            }
        }
        return seqMap;
    }

    private Map<String, List<Object>> buildChemShiftMap(List<List<Object>> dataArray) {
        Map<String, List<Object>> shiftMap = new HashMap<>();
        boolean inShift = false;
        for (List<Object> line : dataArray) {
            if (line.size() > 0) {
                if (line.get(0).toString().contains("chemical_shift")) {
                    inShift = true;
                }
                if (inShift) {
                    int iChain = 0;
                    int iSeq = 1;
                    int iAtomName = 3;
                    if (line.size() > 1 && line.get(1) instanceof Integer) {
                        String key = line.get(iChain) + "." + line.get(iSeq).toString() + "." + line.get(iAtomName);
                        List<Object> values = new ArrayList<>();
                        for (int i = 4; i < line.size(); i++) {
                            values.add(line.get(i));
                        }
                        if (shiftMap.containsKey(key)) {
                            shiftMap.put(key + "_dup", values);
                        } else {
                            shiftMap.put(key, values);
                        }
                    } else if (line.contains("stop_")) {
                        break;
                    }
                }
            }
        }
        return shiftMap;
    }

    private Map<String, List<Object>> buildDistanceMap(List<List<Object>> dataArray) {
        Map<String, List<Object>> distMap = new HashMap<>();
        Map<Integer, String> keys = new HashMap<>();
        boolean inDist = false;
        for (List<Object> line : dataArray) {
            if (line.size() > 0) {
                if (line.get(0).toString().contains("distance_restraint")) {
                    inDist = true;
                }
                if (inDist) {
                    int iChain = 3;
                    int iSeq = 4;
                    int iAtomName = 6;
                    int nAtoms = 2;
                    if (line.size() > 1 && line.get(0) instanceof Integer) {
                        int restraintID = (int) line.get(1);
                        String[] keyParts = new String[nAtoms];
                        for (int i = 0; i < keyParts.length; i++) {
                            keyParts[i] = line.get(iChain + 4 * i) + "." + line.get(iSeq + 4 * i).toString() + "." + line.get(iAtomName + 4 * i);
                        }
                        String key = String.join(";", keyParts);
                        List<Object> values = new ArrayList<>();
                        for (int i = iAtomName + 4 * (nAtoms - 1) + 1; i < line.size(); i++) {
                            values.add(line.get(i));
                        }
                        if (keys.containsKey(restraintID)) {
                            String oldKey = keys.get(restraintID);
                            String newKey = oldKey + "_" + key;
                            keys.put(restraintID, newKey);
                            distMap.remove(oldKey);
                        } else {
                            keys.put(restraintID, key);
                        }
                        key = keys.get(restraintID);
                        distMap.put(key, values);
                    } else if (line.contains("stop_")) {
                        break;
                    }
                }
            }
        }
        return distMap;
    }

    private Map<String, List<Object>> buildDihedralMap(List<List<Object>> dataArray) {
        Map<String, List<Object>> dihedralMap = new HashMap<>();
        boolean inDihedral = false;
        for (List<Object> line : dataArray) {
            if (line.size() > 0) {
                if (line.get(0).toString().contains("dihedral_restraint")) {
                    inDihedral = true;
                }
                if (inDihedral) {
                    int iChain = 3;
                    int iSeq = 4;
                    int iAtomName = 6;
                    int nAtoms = 4;
                    if (line.size() > 1 && line.get(0) instanceof Integer) {
                        String[] keyParts = new String[nAtoms];
                        for (int i = 0; i < keyParts.length; i++) {
                            keyParts[i] = line.get(iChain + 4 * i) + "." + line.get(iSeq + 4 * i).toString() + "." + line.get(iAtomName + 4 * i);
                        }
                        String key = String.join(";", keyParts);
                        List<Object> values = new ArrayList<>();
                        for (int i = iAtomName + 4 * (nAtoms - 1) + 1; i < line.size(); i++) {
                            values.add(line.get(i));
                        }
                        dihedralMap.put(key, values);
                    } else if (line.contains("stop_")) {
                        break;
                    }
                }
            }
        }
        return dihedralMap;
    }

    public void loadData(String nefFileName) throws IOException {
        String fileName = String.join(File.separator, "src", "test", "data", "neffiles", nefFileName + ".nef");
        String outPath = "tmp";
        File tmpDir = new File(outPath);
        if (!tmpDir.exists()) {
            Files.createDirectory(tmpDir.toPath());
        }
        String outFile = String.join(File.separator, outPath, nefFileName + "_nef_outTest.txt");
        try {
            if (orig.isEmpty()) {
                NMRNEFReader.read(fileName);
                NMRNEFWriter.writeAll(outFile);
                orig = convertFileLines(fileName);
                written = convertFileLines(outFile);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean compareMaps(String mode, Map<String, List<Object>> origMap, Map<String, List<Object>> writtenMap) throws IOException {
        try {
            boolean ok = true;
            for (Entry<String, List<Object>> entry : origMap.entrySet()) {
                String key = entry.getKey();
                if (!writtenMap.containsKey(key)) {
                    System.out.println(mode + " key " + key + " not in written");
                    ok = false;
                } else {
                    List<Object> origValues = entry.getValue();
                    List<Object> writtenValues = writtenMap.get(key);
                    List<List<Object>> allValues = new ArrayList<>();
                    allValues.add(origValues);
                    allValues.add(writtenValues);
                    if (mode.equals("dihedral")) {
                        for (int v=0; v<origValues.size(); v++) {
                            if ((v == 1 || v == 3 || v == 4) && 
                                    !origValues.get(v).equals(writtenValues.get(v))) {
                                for (int l=0; l<allValues.size(); l++) {
                                    List<Object> valList = allValues.get(l);
                                    double val = (double) valList.get(v);
                                    if (val >= 180.0 && val < 360.0) {
                                        valList.set(v, val - 180.0);
                                    } else if (val >= -360.0 && val < -180.0) {
                                        valList.set(v, val + 360.0);
                                    } else if (val >= -180.0 && val < 0.0) {
                                        valList.set(v, val + 180.0);
                                    }
                                }
                            }
                        }
                    }
                    for (int i = 0; i < origValues.size(); i++) {
                        if (!origValues.get(i).equals(writtenValues.get(i))) {
                            System.out.println(mode + " " + key + " " + origValues.toString() + " " + writtenValues.toString());
                            ok = false;
                        }
                    }
                }
            }
            for (Entry<String, List<Object>> entry : writtenMap.entrySet()) {
                String key = entry.getKey();
                if (!origMap.containsKey(key)) {
                    System.out.println(mode + " key " + key + " not in orig");
                    ok = false;
                }
            }
            return ok;
        } catch (Exception ex) {
            return false;
        }

    }

    public void testAll() throws IOException {
        testSeqBlock();
        testChemShiftBlock();
        testDistanceBlock();
        testDihedralBlock();
    }

    public void testSeqBlock() throws IOException {
        try {
            Map<String, List<Object>> origSeq = buildSequenceMap(orig);
            Map<String, List<Object>> writtenSeq = buildSequenceMap(written);
            boolean ok = compareMaps("seq", origSeq, writtenSeq);
            Assert.assertTrue(ok);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testChemShiftBlock() throws IOException {
        try {
            Map<String, List<Object>> origShift = buildChemShiftMap(orig);
            Map<String, List<Object>> writtenShift = buildChemShiftMap(written);
            boolean ok = compareMaps("shifts", origShift, writtenShift);
            Assert.assertTrue(ok);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testDistanceBlock() throws IOException {
        try {
            Map<String, List<Object>> origDist = buildDistanceMap(orig);
            Map<String, List<Object>> writtenDist = buildDistanceMap(written);
            boolean ok = compareMaps("distance", origDist, writtenDist);
            Assert.assertTrue(ok);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testDihedralBlock() throws IOException {
        try {
            Map<String, List<Object>> origDihedral = buildDihedralMap(orig);
            Map<String, List<Object>> writtenDihedral = buildDihedralMap(written);
            boolean ok = compareMaps("dihedral", origDihedral, writtenDihedral);
            Assert.assertTrue(ok);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}

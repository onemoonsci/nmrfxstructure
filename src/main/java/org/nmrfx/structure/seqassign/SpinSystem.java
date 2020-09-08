package org.nmrfx.structure.seqassign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.commons.math3.util.MultidimensionalCounter;
import org.apache.commons.math3.util.MultidimensionalCounter.Iterator;
import org.nmrfx.processor.datasets.peaks.Peak;
import org.nmrfx.processor.datasets.peaks.PeakDim;
import org.nmrfx.processor.datasets.peaks.PeakList;
import org.nmrfx.processor.datasets.peaks.PeakList.SearchDim;
import org.nmrfx.processor.datasets.peaks.SpectralDim;
import org.nmrfx.structure.seqassign.RunAbout.TypeInfo;
import static org.nmrfx.structure.seqassign.SpinSystems.comparePeaks;
import static org.nmrfx.structure.seqassign.SpinSystems.matchDims;
import smile.clustering.KMeans;

/**
 *
 * @author brucejohnson
 */
public class SpinSystem {

    SpinSystems spinSystems;
    final Peak rootPeak;
    List<PeakMatch> peakMatches = new ArrayList<>();
    List<SpinSystemMatch> spinMatchP = new ArrayList<>();
    List<SpinSystemMatch> spinMatchS = new ArrayList<>();
    Optional<SpinSystemMatch> confirmP = Optional.empty();
    Optional<SpinSystemMatch> confirmS = Optional.empty();
    Optional<SeqFragment> fragment = Optional.empty();
    static final String[] ATOM_TYPES = {"h", "n", "c", "ha", "ca", "cb"};
    static final Map<String, Integer> atomIndexMap = new HashMap<>();

    static {
        int atomIndex = 0;
        for (String atomType : ATOM_TYPES) {
            atomIndexMap.put(atomType, atomIndex++);
        }
    }
    static int[][] nAtmPeaks = {
        {0, 0, 2, 0, 2, 2},
        {7, 7, 1, 0, 1, 1}
    };
    static int[] RES_MTCH = {2, 4, 5};

    static final int CA_INDEX = ATOM_TYPES.length - 2;
    static final int CB_INDEX = ATOM_TYPES.length - 1;

    static double[] tols = {0.04, 0.5, 0.6, 0.04, 0.6, 0.6};
    double[][] values = new double[2][ATOM_TYPES.length];
    double[][] ranges = new double[2][ATOM_TYPES.length];
    int[][] nValues = new int[2][ATOM_TYPES.length];

    class ResAtomPattern {

        final Peak peak;
        final int[] resType;
        final String[] atomTypes;
        int[] atomTypeIndex;
        final boolean requireSign;
        final boolean positive;
        final boolean ambiguousRes;

        ResAtomPattern(Peak peak, String[] resType, String[] atomTypes, boolean requireSign, boolean positive, boolean ambiguousRes) {
            this.peak = peak;
            this.resType = new int[resType.length];
            for (int i = 0; i < resType.length; i++) {
                this.resType[i] = resType[i].equals("i-1") ? -1 : 0;
            }
            this.atomTypes = atomTypes.clone();
            this.atomTypeIndex = new int[atomTypes.length];
            this.requireSign = requireSign;
            this.positive = positive;
            this.ambiguousRes = ambiguousRes;
            int iType = 0;
            for (String aName : ATOM_TYPES) {
                int i = 0;
                for (String atomType : atomTypes) {
                    if (atomType.equalsIgnoreCase(aName)) {
                        atomTypeIndex[i] = iType;
                        break;
                    }
                    i++;
                }
                iType++;
            }
        }

        public String toString() {
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append(peak.getName()).append(" ");
            for (int res : resType) {
                sBuilder.append(res).append(" ");
            }
            for (String atomType : atomTypes) {
                sBuilder.append(atomType).append(" ");
            }
            for (int atomTypeI : atomTypeIndex) {
                sBuilder.append(atomTypeI).append(" ");
            }
            sBuilder.append(positive).append(" ").append(requireSign);
            return sBuilder.toString();
        }
    }

    class ResAtomPatternOld {

        final Peak peak;
        final int iDim;
        final int resType;
        final String atomType;
        int atomTypeIndex = -1;
        final boolean requireSign;
        final boolean positive;
        final boolean ambiguousRes;

        ResAtomPatternOld(Peak peak, int iDim, String resType, String atomType, boolean requireSign, boolean positive, boolean ambiguousRes) {
            this.peak = peak;
            this.iDim = iDim;
            this.resType = resType.equals("i-1") ? -1 : 0;
            this.atomType = atomType;
            this.requireSign = requireSign;
            this.positive = positive;
            this.ambiguousRes = ambiguousRes;
            int i = 0;
            for (String aName : ATOM_TYPES) {
                if (atomType.equals(aName)) {
                    atomTypeIndex = i;
                    break;
                }
                i++;
            }

        }
    }

    class PeakAtomMatch {

        final Peak peak;
        final int dim;

        PeakAtomMatch(Peak peak, int dim) {
            this.peak = peak;
            this.dim = dim;
        }
    }

    public class PeakMatch {

        final Peak peak;
        final double prob;
        final int[] atomIndexes;
        final boolean[] intraResidue;

        PeakMatch(Peak peak, double prob) {
            this.peak = peak;
            this.prob = prob;
            atomIndexes = new int[peak.getNDim()];
            intraResidue = new boolean[peak.getNDim()];
        }

        void setIntraResidue(int dim, boolean state) {
            intraResidue[dim] = state;
        }

        void setIndex(int dim, int index) {
            atomIndexes[dim] = index;
        }

        public boolean getIntraResidue(int dim) {
            return intraResidue[dim];
        }

        public boolean getPositive() {
            return peak.getIntensity() > 0.0;
        }

        public int getIndex(int dim) {
            return atomIndexes[dim];
        }

        public Peak getPeak() {
            return peak;
        }

        public boolean isType(PeakList peakList, String aName, boolean intraMode) {
            boolean ok = true;
            if (peak.getPeakList() != peakList) {
                ok = false;
            } else {
                boolean dimOK = false;
                for (int i = 0; i < atomIndexes.length; i++) {
                    String curName = ATOM_TYPES[atomIndexes[i]];
                    if (aName.equalsIgnoreCase(curName) && (intraMode == intraResidue[i])) {
                        dimOK = true;
                        break;
                    }
                }
                ok = dimOK;
            }
            return ok;

        }

        public String toString() {
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append(peak.getName()).append(" ").append(prob);
            for (int i = 0; i < atomIndexes.length; i++) {
                sBuilder.append(" ").append(atomIndexes[i]).append(" ").append(intraResidue[i]);
            }
            return sBuilder.toString();
        }

    }

    public SpinSystem(Peak peak, SpinSystems spinSystems) {
        this.spinSystems = spinSystems;
        this.rootPeak = peak;
        addPeak(peak, 1.0);
    }

    public Peak getRootPeak() {
        return rootPeak;
    }

    public List<PeakMatch> peakMatches() {
        return peakMatches;
    }

    public int getNPeaksWithList(PeakList peakList) {
        int n = 0;
        for (PeakMatch match : peakMatches) {
            if (peakList == match.getPeak().getPeakList()) {
                n++;
            }
        }
        return n;
    }

    public class AtomPresent {

        final String name;
        final boolean intraResidue;
        final boolean present;

        public String getName() {
            return name;
        }

        public boolean isIntraResidue() {
            return intraResidue;
        }

        public boolean isPresent() {
            return present;
        }

        public AtomPresent(String name, boolean intraResidue, boolean present) {
            this.name = name.toUpperCase();
            this.intraResidue = intraResidue;
            this.present = present;
        }

    }

    public List<AtomPresent> getTypesPresent(TypeInfo typeInfo, PeakList peakList, int iDim) {
        String[] names = typeInfo.getNames(iDim);
        List<AtomPresent> result = new ArrayList<>();
        boolean[] intraResidue = typeInfo.getIntraResidue(iDim);
        for (int i = 0; i < names.length; i++) {
            boolean ok = false;
            for (PeakMatch peakMatch : peakMatches) {
                if (peakMatch.isType(peakList, names[i], intraResidue[i])) {
                    ok = true;
                    break;
                }
            }
            AtomPresent atomPresent = new AtomPresent(names[i], intraResidue[i], ok);
            result.add(atomPresent);
        }
        return result;
    }

    public final void addPeak(Peak peak, double prob) {
        PeakMatch peakMatch = new PeakMatch(peak, prob);
        peakMatches.add(peakMatch);
    }

    public static int getNAtomTypes() {
        return ATOM_TYPES.length;
    }

    public static int getNPeaksForType(int k, int i) {
        return nAtmPeaks[k][i];
    }

    public static String getAtomName(int index) {
        return ATOM_TYPES[index];
    }

    public double getValue(int dir, int index) {
        return values[dir][index];
    }

    public void setValue(int dir, int index, double value) {
        values[dir][index] = value;
    }

    public double getRange(int dir, int index) {
        return ranges[dir][index];
    }

    public void setRange(int dir, int index, double value) {
        ranges[dir][index] = value;
    }

    public int getNValues(int dir, int index) {
        return nValues[dir][index];
    }

    public void setNValues(int dir, int index, int value) {
        nValues[dir][index] = value;
    }

    public boolean confirmed(SpinSystemMatch spinSys, boolean prev) {
        boolean result = false;
        if (prev) {
            if (confirmP.isPresent()) {
                result = confirmP.get().spinSystemB == this;
            }
        } else {
            if (confirmS.isPresent()) {
                result = confirmS.get().spinSystemA == this;
            }
        }
        return result;
    }

    public boolean confirmed(boolean prev) {
        return prev ? confirmP.isPresent() : confirmS.isPresent();
    }

    public void confirm(SpinSystemMatch spinSysMatch, boolean prev) {
        if (confirmed(prev)) {
            return;
        }
        if (prev) {
            SpinSystem target = spinSysMatch.spinSystemA;
            confirmP = Optional.of(spinSysMatch);
            System.out.println("confirm P " + spinSysMatch.toString() + " " + spinSysMatch.spinSystemA.getRootPeak().getName());
            target.confirmS = Optional.of(spinSysMatch);
        } else {
            SpinSystem target = spinSysMatch.spinSystemB;
            confirmS = Optional.of(spinSysMatch);
            System.out.println("confirm S " + spinSysMatch.toString() + " " + spinSysMatch.spinSystemB.getRootPeak().getName());
            target.confirmP = Optional.of(spinSysMatch);
        }
        SeqFragment fragment = SeqFragment.join(spinSysMatch, false);
        fragment.dump();
        fragment.getShifts();
    }

    public void unconfirm(SpinSystemMatch spinSysMatch, boolean prev) {
        if (!confirmed(spinSysMatch, prev)) {
            return;
        }
        if (prev) {
            SpinSystem target = spinSysMatch.spinSystemA;
            confirmP = Optional.empty();
            target.confirmS = Optional.empty();
        } else {
            SpinSystem target = spinSysMatch.spinSystemB;
            confirmS = Optional.empty();
            target.confirmP = Optional.empty();
        }
        List<SeqFragment> fragments = SeqFragment.remove(spinSysMatch, false);
        for (SeqFragment fragment : fragments) {

            if (fragment != null) {
                System.out.println("FRrag");
                fragment.dump();
                fragment.getShifts();
            }
        }
    }

    public Optional<SeqFragment> getFragment() {
        return fragment;
    }

    public static int[] getCounts(PeakList peakList) {
        int nDim = peakList.getNDim();
        int[] counts = new int[nDim];
        for (int i = 0; i < nDim; i++) {
            SpectralDim sDim = peakList.getSpectralDim(i);
            String fullPattern = sDim.getPattern();
            String[] resAtoms = fullPattern.split("\\.");
            String[] resPats = resAtoms[0].split(",");
            String[] atomPats = resAtoms[1].split(",");
            counts[i] = resPats.length * atomPats.length;
        }
        return counts;
    }

    List<ResAtomPattern> getPatterns(Peak peak) {
        int nDim = peak.getNDim();
        String[][] allResPats = new String[nDim][];
        String[][] allAtomPats = new String[nDim][];
        int[] counts = new int[nDim];
        int nResPats = 1;
        for (int i = 0; i < nDim; i++) {
            SpectralDim sDim = peak.getPeakList().getSpectralDim(i);
            String fullPattern = sDim.getPattern();
            String[] resAtoms = fullPattern.split("\\.");
            String[] resPats = resAtoms[0].split(",");
            if (resPats.length > nResPats) {
                nResPats = resPats.length;
            }
            String[] atomPats = resAtoms[1].split(",");
            counts[i] = resPats.length * atomPats.length;
            allResPats[i] = new String[counts[i]];
            allAtomPats[i] = new String[counts[i]];
            int j = 0;
            for (String resPat : resPats) {
                for (String atomPat : atomPats) {
                    allResPats[i][j] = resPat;
                    allAtomPats[i][j] = atomPat;
                    j++;
                }
            }
        }
        MultidimensionalCounter counter = new MultidimensionalCounter(counts);
        Iterator iter = counter.iterator();
        List<ResAtomPattern> result = new ArrayList<>();
        while (iter.hasNext()) {
            iter.next();
            int[] indices = iter.getCounts();
            String[] resPats = new String[nDim];
            String[] atomPats = new String[nDim];
            boolean requireSign = false;
            boolean positive = false;
            for (int i = 0; i < nDim; i++) {
                resPats[i] = allResPats[i][indices[i]];
                atomPats[i] = allAtomPats[i][indices[i]];
                if (atomPats[i].endsWith("-")) {
                    atomPats[i] = atomPats[i].substring(0, atomPats[i].length() - 1);
                    requireSign = true;
                    positive = false;
                } else if (atomPats[i].endsWith("+")) {
                    atomPats[i] = atomPats[i].substring(0, atomPats[i].length() - 1);
                    requireSign = true;
                    positive = true;
                }
            }
            ResAtomPattern resAtomPattern = new ResAtomPattern(peak, resPats, atomPats, requireSign, positive, nResPats > 1);
            result.add(resAtomPattern);
        }
        return result;
    }

    double getProb(String aName, double ppm) {
        if (aName.equalsIgnoreCase("ca")) {
            if (ppm < 38.0) {
                return 0.0;
            }
        }
        return 1.0;
    }

    boolean checkPat(ResAtomPattern pattern, double intensity) {
        boolean ok = true;

        if (pattern.requireSign && pattern.positive && (intensity < 0.0)) {
            ok = false;
        } else if (pattern.requireSign && !pattern.positive && (intensity > 0.0)) {
            ok = false;
        }
        boolean isGly = false;
        if (ok) {
            for (int i = 0; i < pattern.atomTypes.length; i++) {
                String aName = pattern.atomTypes[i];
                double shift = pattern.peak.getPeakDim(i).getAdjustedChemShiftValue();
                if (aName.equalsIgnoreCase("ca")) {
                    if (shift < 50.0) {
                        isGly = true;
                    }
                }
                double ppmProb = getProb(aName, shift);
                if (ppmProb < 1.0e-6) {
                    ok = false;
                    break;
                }
            }
        }
        if (ok) {
            if (pattern.ambiguousRes) {
                boolean isInter = false;
                for (int iRes : pattern.resType) {
                    if (iRes == -1) {
                        isInter = true;
                        break;
                    }
                }
                // 
                double limit = isGly ? 1.2 : 0.95;

                if (isInter && (Math.abs(intensity) > limit)) {
                    ok = false;
                }
            }
        }
        return ok;
    }

    double[] shiftRange(List<Double> shifts) {
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.NEGATIVE_INFINITY;
        for (double shift : shifts) {
            sum += shift;
            min = Math.min(min, shift);
            max = Math.max(max, shift);
        }
        double range = max - min;
        double mean = sum / shifts.size();
        double[] result = {mean, range};
        return result;
    }

    void dumpShifts(List<Double>[][] shiftList) {
        double score = analyzeShifts(shiftList);
        if (score > 0.0) {
            double pMissing = Math.exp(-1.0);
            int nAtoms = 0;
            double pCum = 1.0;
            boolean[] isGly = new boolean[2];
            for (int k = 0; k < 2; k++) {
                if (!shiftList[k][CA_INDEX].isEmpty()) {
                    double caShift = shiftRange(shiftList[k][CA_INDEX])[0];
                    if (caShift < 50.0) {
                        isGly[k] = true;
                    }
                }
            }
            for (int k = 0; k < 2; k++) {
                for (int i = 0; i < ATOM_TYPES.length; i++) {
                    int nExpected = nAtmPeaks[k][i];
                    if (isGly[k] && (i == CB_INDEX)) {
                        nExpected = 0;
                    }
                    if (!shiftList[k][i].isEmpty()) {
                        int nShifts = shiftList[k][i].size();
                        if (nShifts < nExpected) {
                            pCum *= Math.pow(pMissing, nExpected - nShifts);
                        }
                        double shift = shiftRange(shiftList[k][i])[0];
                        System.out.printf("%3s %5.1f %2d %2d ", ATOM_TYPES[i], shift, nShifts, nExpected);
                    } else {
                        System.out.printf("%3s %5.1f %2d %2d ", ATOM_TYPES[i], 0.0, 0, nExpected);
                    }
                }
                if (k == 0) {
                    System.out.print("     ");
                } else {
                    System.out.println(" " + score + " " + pCum + " " + isGly[0] + " " + isGly[1]);
                }
            }
        }
    }

    double analyzeShifts(List<Double>[][] shiftList) {
        double pMissing = Math.exp(-1.0);
        int nAtoms = 0;
        double pCum = 1.0;
        boolean[] isGly = new boolean[2];
        for (int k = 0; k < 2; k++) {
            if (!shiftList[k][CA_INDEX].isEmpty()) {
                double caShift = shiftRange(shiftList[k][CA_INDEX])[0];
                if (caShift < 50.0) {
                    isGly[k] = true;
                }
            }
        }
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < ATOM_TYPES.length; i++) {
                int nShifts = shiftList[k][i].size();
                int nExpected = nAtmPeaks[k][i];
                if (isGly[k] && (i == CB_INDEX)) {
                    nExpected = 0;
                }
                if ((nExpected == 0) && (nShifts > 0)) {
                    pCum = 0.0;
                    break;
                } else {
                    if (nExpected > 0) {
                        if (nShifts > 0) {
                            double[] range = shiftRange(shiftList[k][i]);
                            double mean = range[0];
                            for (double shift : shiftList[k][i]) {
                                double delta = Math.abs(mean - shift);
                                pCum *= Math.exp(-delta / tols[i]);
                            }
                        }
                        if (nShifts < nExpected) {
                            pCum *= Math.pow(pMissing, nExpected - nShifts);
                        }
                    }
                }
            }
        }
        return pCum;
    }

    void saveShifts(List<Double>[][] shiftList) {
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < ATOM_TYPES.length; i++) {
                int nShifts = shiftList[k][i].size();
                if (nShifts > 0) {
                    double[] range = shiftRange(shiftList[k][i]);
                    setValue(k, i, range[0]);
                    setRange(k, i, range[1]);
                    setNValues(k, i, nShifts);
                } else {
                    setValue(k, i, Double.NaN);
                    setRange(k, i, 0.0);
                    setNValues(k, i, 0);
                }
            }
        }

    }

    void writeShifts(List<Double>[][] shiftList) {
        int nAtoms = 0;

        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < ATOM_TYPES.length; i++) {
                int j = i;
                if (k == 0) {
                    j = ATOM_TYPES.length - i - 1;
                    if (j < 2) {
                        continue;
                    }
                }

                int nShifts = shiftList[k][j].size();
                if (nShifts > 0) {
                    double[] range = shiftRange(shiftList[k][j]);
                    System.out.printf(" %6.2f:%1d", range[0], shiftList[k][j].size());
                    nAtoms += shiftList[k][j].size();
                } else {
                    System.out.print("   NA    ");
                }
            }
        }
        double pCum = analyzeShifts(shiftList);
        System.out.printf("  nA %2d %6.4f\n", nAtoms, pCum);
    }

    boolean addShift(int nPeaks, List<ResAtomPattern>[] resAtomPatterns, List<Double>[][] shiftList, int[] pt) {
        int j = 0;
        for (int i = 0; i < nPeaks; i++) {
            int k = 0;
            if (resAtomPatterns[i].size() > 1) {
                k = pt[j++];
            }
            if (!resAtomPatterns[i].isEmpty()) {
                ResAtomPattern resAtomPattern = resAtomPatterns[i].get(k);
                if (resAtomPattern != null) {
//                    System.out.println("Ip " + i + " " + resAtomPattern.toString());
                    int nDim = resAtomPattern.atomTypeIndex.length;
                    for (int iDim = 0; iDim < nDim; iDim++) {
                        int iAtom = resAtomPattern.atomTypeIndex[iDim];
                        int iRes = resAtomPattern.resType[iDim];
                        List<Double> shifts = shiftList[iRes + 1][iAtom];
                        double newValue = (double) resAtomPattern.peak.getPeakDim(iDim).getChemShiftValue();
                        if (!shifts.isEmpty()) {
                            double current = shifts.get(0);
                            if (Math.abs(current - newValue) > 1.5 * tols[iAtom]) {
//                                System.out.println("shi " + iAtom + " " + iRes + " " + iDim + " " + current + " " + newValue + " " + shifts.size());
                                return false;
                            }
                        }
                        shifts.add(newValue);
                    }
                }
            }
        }
        return true;

    }

    double[] getNormalizedIntensities() {
        double[] intensities = new double[peakMatches.size()];
        Map<String, Double> intensityMap = new HashMap<>();
        Map<String, List<PeakMatch>> listOfMatches = new HashMap<>();

        for (PeakMatch peakMatch : peakMatches) {
            Peak peak = peakMatch.peak;
            peak.setFlag(1, false);
            String peakListName = peak.getPeakList().getName();
            double maxIntensity = 0.0;
            if (intensityMap.containsKey(peakListName)) {
                maxIntensity = intensityMap.get(peakListName);
            }
            maxIntensity = Math.max(maxIntensity, Math.abs(peak.getIntensity()));
            intensityMap.put(peakListName, maxIntensity);
            List<PeakMatch> listMatches = listOfMatches.get(peakListName);
            if (listMatches == null) {
                listMatches = new ArrayList<>();
                listOfMatches.put(peakListName, listMatches);
            }
            listMatches.add(peakMatch);
        }
        for (Entry<String, List<PeakMatch>> entry : listOfMatches.entrySet()) {
            String peakListName = entry.getKey();
            String typeName = spinSystems.runAbout.peakListTypes.get(peakListName);
            TypeInfo typeInfo = spinSystems.runAbout.typeInfoMap.get(typeName);
            int nExpected = typeInfo.nTotal;
            List<PeakMatch> matches = entry.getValue();
            matches.sort((a, b)
                    -> Double.compare(Math.abs(a.getPeak().getIntensity()),
                            Math.abs(b.getPeak().getIntensity())));
            int nExtra = matches.size() - nExpected;
            for (int i = 0; i < nExtra; i++) {
                matches.get(i).getPeak().setFlag(1, true);
            }

        }
        int iPeak = 0;
        for (PeakMatch peakMatch : peakMatches) {
            String peakListName = peakMatch.peak.getPeakList().getName();
            double maxIntensity = intensityMap.get(peakListName);
            intensities[iPeak++] = peakMatch.peak.getIntensity() / maxIntensity;
        }
        return intensities;
    }

    boolean getShifts(int nPeaks, List<ResAtomPattern>[] resAtomPatterns,
            List<Double>[][] shiftList, int[] pt
    ) {
        for (int i = 0; i < ATOM_TYPES.length; i++) {
            shiftList[0][i].clear();
            shiftList[1][i].clear();
        }
        boolean result = addShift(nPeaks, resAtomPatterns, shiftList, pt);
        return result;
    }

    public void calcCombinations(boolean display) {
        double[] intensities = getNormalizedIntensities();
        int nPeaks = peakMatches.size();
        List<ResAtomPattern>[] resAtomPatterns = new List[nPeaks];
        int nCountable = 0;
        int iPeak = 0;
        int[] counts = new int[nPeaks];
        for (PeakMatch peakMatch : peakMatches) {
            List<ResAtomPattern> okPats = new ArrayList<>();
            Peak peak = peakMatch.peak;
            if (!peak.getFlag(1)) {
                List<ResAtomPattern> patterns = getPatterns(peak);
                double intensity = intensities[iPeak];
                for (ResAtomPattern resAtomPattern : patterns) {
//                System.out.println(resAtomPattern.toString());
                    boolean added;
                    if (checkPat(resAtomPattern, intensity)) {
                        okPats.add(resAtomPattern);
                        // System.out.println("add");
                        added = true;
                    } else {
                        added = false;
                        // System.out.println("fail");

                    }
                    if (display) {
                        System.out.println(resAtomPattern.toString() + " " + intensity + " " + added);

                    }
                }
            }
            // allow all peaks but root peak to be unused (artifact)
            if (iPeak != 0) {
                okPats.add(null);
            }
            resAtomPatterns[iPeak] = okPats;
            if (okPats.size() > 1) {
                nCountable++;
            }
            counts[iPeak] = okPats.size();
            iPeak++;
        }
//        for (int i = 0; i < nPeaks; i++) {
        //           System.out.print(" " + counts[i]);
        //       }
        //       System.out.println(" " + nCountable);
        //       for (int i = 0; i < resAtomPatterns.length; i++) {
        //           for (int j = 0; j < resAtomPatterns[i].size(); j++) {
        //               System.out.println(i + " " + j + " " + resAtomPatterns[i].get(j));
        //           }
        //       }

        if (nCountable == 0) {
            List<Double>[][] shiftList = new ArrayList[2][ATOM_TYPES.length];
            for (int i = 0; i < ATOM_TYPES.length; i++) {
                shiftList[0][i] = new ArrayList<>();
                shiftList[1][i] = new ArrayList<>();
            }
            if (addShift(nPeaks, resAtomPatterns, shiftList, null)) {
                writeShifts(shiftList);
            }

        } else {
            int[] indices = new int[nCountable];
            int j = 0;
            for (int i = 0; i < nPeaks; i++) {
                if (resAtomPatterns[i].size() > 1) {
                    indices[j++] = resAtomPatterns[i].size();
                }
            }
            MultidimensionalCounter counter = new MultidimensionalCounter(indices);
            Iterator iter = counter.iterator();
            double best = 0.0;
            int bestIndex = -1;
            List<Double>[][] shiftList = new ArrayList[2][ATOM_TYPES.length];
            for (int i = 0; i < ATOM_TYPES.length; i++) {
                shiftList[0][i] = new ArrayList<>();
                shiftList[1][i] = new ArrayList<>();
            }
            while (iter.hasNext()) {
                iter.next();
                int[] pt = iter.getCounts();
                boolean validShifts = getShifts(nPeaks, resAtomPatterns, shiftList, pt);
                if (display) {
                    dumpShifts(shiftList);
                }
                if (validShifts) {
//                    writeShifts(shiftList);
                    double prob = analyzeShifts(shiftList);
                    if (prob > best) {
                        best = prob;
                        bestIndex = iter.getCount();
                    }
                }
            }
            if (!display && (bestIndex >= 0)) {
                int[] pt = counter.getCounts(bestIndex);
//                boolean validShifts = getShifts(nPeaks, resAtomPatterns, shiftList, pt);
                setUserFields(resAtomPatterns, pt);
                updateSpinSystem();
            }
        }
    }

    void getLinkedPeaks() {
        PeakList refList = rootPeak.getPeakList();
        for (Peak pkB : PeakList.getLinks(rootPeak, 0)) {// fixme calculate correct dim
            if (rootPeak != pkB) {
                PeakList peakListB = pkB.getPeakList();
                if (refList != peakListB) {
                    int[] aMatch = matchDims(refList, peakListB);
                    double f = comparePeaks(rootPeak, pkB, aMatch);
                    if (f >= 0.0) {
                        double p = f;
                        addPeak(pkB, p);
                    }
                }
            }
        }
        int nPeaks = peakMatches.size();
        System.out.println("cluster " + rootPeak.getName() + " " + nPeaks);

    }

    void updateSpinSystem() {
        List<Double>[][] shiftList = new ArrayList[2][ATOM_TYPES.length];
        for (int i = 0; i < ATOM_TYPES.length; i++) {
            shiftList[0][i] = new ArrayList<>();
            shiftList[1][i] = new ArrayList<>();
        }
        for (PeakMatch peakMatch : peakMatches) {
            Peak peak = peakMatch.peak;
            int iDim = 0;
            for (PeakDim peakDim : peak.getPeakDims()) {
                String userField = peakDim.getUser();
                if (userField.contains(".")) {
                    int dotIndex = userField.indexOf('.');
                    String resType = userField.substring(0, dotIndex);
                    String atomType = userField.substring(dotIndex + 1).toLowerCase();
                    int k = resType.endsWith("-1") ? 0 : 1;
                    Integer iAtom = atomIndexMap.get(atomType);
                    if (iAtom != null) {
                        shiftList[k][iAtom].add(peakDim.getChemShift().doubleValue());

                        peakMatch.setIndex(iDim, iAtom);
                        peakMatch.setIntraResidue(iDim, k == 1);
                    }
                }
                iDim++;

            }
        }
        writeShifts(shiftList);
        saveShifts(shiftList);
    }

    void setUserFields(List<ResAtomPattern>[] resAtomPatterns, int[] pt) {
        StringBuilder sBuilder = new StringBuilder();
        String linkDim = RunAbout.getNDimName(rootPeak.getPeakList()); // fixme
        List<Peak> linkedPeaks = PeakList.getLinks(rootPeak,
                rootPeak.getPeakList().getSpectralDim(linkDim).getDataDim());
        for (Peak peak : linkedPeaks) {
            for (PeakDim peakDim : peak.getPeakDims()) {
                peakDim.setUser("");
            }
        }

        int j = 0;
        for (int i = 0; i < resAtomPatterns.length; i++) {
            int k = 0;
            if (resAtomPatterns[i].size() > 1) {
                k = pt[j++];
            }
            if (!resAtomPatterns[i].isEmpty()) {
                ResAtomPattern resAtomPattern = resAtomPatterns[i].get(k);
                if (resAtomPattern != null) {
//                    System.out.println("Ip " + i + " " + resAtomPattern.toString());
                    int nDim = resAtomPattern.atomTypeIndex.length;
                    for (int iDim = 0; iDim < nDim; iDim++) {
                        int iAtom = resAtomPattern.atomTypeIndex[iDim];
                        int iRes = resAtomPattern.resType[iDim];
                        sBuilder.setLength(0);
                        sBuilder.append("i");
                        if (iRes < 0) {
                            sBuilder.append("-1");
                        }
                        sBuilder.append(".").append(ATOM_TYPES[iAtom]);
                        resAtomPattern.peak.getPeakDim(iDim).setUser(sBuilder.toString());
                    }
                }
            }
        }
    }

    public void assignAtoms() {
        for (String atomType : ATOM_TYPES) {
            for (PeakMatch peakMatch : peakMatches) {
                Peak peak = peakMatch.peak;
                int nDims = peak.getPeakList().getNDim();
                for (int iDim = 0; iDim < nDims; iDim++) {
                    double value = peak.getPeakDim(iDim).getChemShift();
                }

            }
        }

    }

    public Optional<SpinSystemMatch> compare(SpinSystem spinSysB, boolean prev) {
        int idxB = prev ? 1 : 0;
        int idxA = prev ? 0 : 1;
        double sum = 0.0;
        boolean ok = false;
        int nMatch = 0;
        boolean[] matched = new boolean[RES_MTCH.length];
        int j = 0;
        for (int i : RES_MTCH) {
            double vA = getValue(idxA, i);
            double vB = spinSysB.getValue(idxB, i);
            double tolA = tols[i];
            if (Double.isFinite(vA) && Double.isFinite(vB)) {
                double delta = Math.abs(vA - vB);
                ok = true;
                if (delta > 2.0 * tolA) {
                    ok = false;
                    break;
                } else {
                    matched[j] = true;
                    delta /= tolA;
                    sum += delta * delta;
                    nMatch++;
                }
            }
            j++;
        }
        Optional<SpinSystemMatch> result = Optional.empty();
        if (ok) {
            double dis = Math.sqrt(sum);
            double score = Math.exp(-dis);
            SpinSystemMatch spinMatch;
            if (prev) {
                spinMatch = new SpinSystemMatch(spinSysB, this, score, nMatch, matched);
            } else {
                spinMatch = new SpinSystemMatch(this, spinSysB, score, nMatch, matched);
            }
            result = Optional.of(spinMatch);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(rootPeak.getName());
        for (PeakMatch peakMatch : peakMatches) {
            sBuilder.append(" ");
            sBuilder.append(peakMatch.peak.getName()).append(":");
            sBuilder.append(String.format("%.2f", peakMatch.prob));
        }
        return sBuilder.toString();
    }

    public SpinSystem split(int[] nExpected) {
        double[][] values = new double[peakMatches.size()][];
        int i = 0;
        for (PeakMatch peakMatch : peakMatches) {
            Peak peak = peakMatch.peak;
            PeakList peakList = peak.getPeakList();
            List<SearchDim> searchDims = peakList.getSearchDims();
            values[i] = new double[searchDims.size()];
            int j = 0;
            for (SearchDim sDim : searchDims) {
                double shift = peak.getPeakDim(sDim.getDim()).getChemShiftValue();
                values[i][j] = shift;
            }
            i++;
        }
        KMeans kMeans = KMeans.lloyd(values, 2);
//        int[] labels = kMeans.getClusterLabel();
        int[] labels = kMeans.y;
        int origCluster = labels[0];
        int newCluster = origCluster == 0 ? 1 : 0;
//        double[][] centroids = kMeans.centroids();
        double[][] centroids = kMeans.centroids;
        Peak newRoot = rootPeak.peakList.getNewPeak();
        rootPeak.copyTo(newRoot);
        int j = 0;
        List<SearchDim> searchDims = rootPeak.peakList.getSearchDims();
        for (SearchDim sDim : searchDims) {
            rootPeak.getPeakDim(sDim.getDim()).setChemShiftValue((float) centroids[origCluster][j]);
            newRoot.getPeakDim(sDim.getDim()).setChemShiftValue((float) centroids[newCluster][j]);
            j++;
        }
        SpinSystem newSys = new SpinSystem(newRoot, spinSystems);
        List<PeakMatch> oldPeaks = new ArrayList<>();
        oldPeaks.addAll(peakMatches);
        peakMatches.clear();
        addPeak(rootPeak, 1.0);
        i = 0;
        for (PeakMatch peakMatch : oldPeaks) {
            if (peakMatch.peak != rootPeak) {
                if (labels[i] == origCluster) {
                    int[] aMatch = SpinSystems.matchDims(rootPeak.peakList, peakMatch.peak.peakList);
                    double f = SpinSystems.comparePeaks(rootPeak, peakMatch.peak, aMatch);
                    addPeak(peakMatch.peak, f);
                } else {
                    int[] aMatch = SpinSystems.matchDims(newRoot.peakList, peakMatch.peak.peakList);
                    double f = SpinSystems.comparePeaks(newRoot, peakMatch.peak, aMatch);
                    newSys.addPeak(peakMatch.peak, f);
                }
            }
            i++;

        }

        return newSys;

    }

    public List<SpinSystemMatch> getMatchToPrevious() {
        return spinMatchP;
    }

    public List<SpinSystemMatch> getMatchToNext() {
        return spinMatchS;
    }

    public void dumpPeakMatches() {
        for (PeakMatch peakMatch : peakMatches) {
            System.out.println(peakMatch.toString());
        }
    }

    public void compare() {
        spinMatchP.clear();
        spinMatchS.clear();
        double sumsP = 0.0;
        double sumsS = 0.0;
        for (SpinSystem spinSysB : spinSystems.systems) {
            if (this != spinSysB) {
                Optional<SpinSystemMatch> result = compare(spinSysB, true);
                if (result.isPresent()) {
                    spinMatchP.add(result.get());
                    sumsP += result.get().score;
                }
                result = compare(spinSysB, false);
                if (result.isPresent()) {
                    spinMatchS.add(result.get());
                    sumsS += result.get().score;
                }
            }
        }
        for (SpinSystemMatch spinMatch : spinMatchP) {
            spinMatch.norm(sumsP);
        }
        for (SpinSystemMatch spinMatch : spinMatchS) {
            spinMatch.norm(sumsS);
        }

        spinMatchP.sort((s1, s2) -> Double.compare(s2.score, s1.score));
        System.out.println(rootPeak.getName() + " " + spinMatchP);
        spinMatchS.sort((s1, s2) -> Double.compare(s2.score, s1.score));
        System.out.println(rootPeak.getName() + " " + spinMatchS);
    }

}

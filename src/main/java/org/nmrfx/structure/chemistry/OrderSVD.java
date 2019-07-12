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

package org.nmrfx.structure.chemistry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class OrderSVD {
    
    static double mu0 = 4.0e-7 * Math.PI;
    static double hbar = 1.054e-34;
    static HashMap<String, Double> maxRDCDict = new HashMap<>(); 
    static HashMap gammaIDict = new HashMap();
    static HashMap gammaSDict = new HashMap();
    
    static {maxRDCDict.put("HN", 24350.0);
        maxRDCDict.put("HC", -60400.0);
        maxRDCDict.put("HH", -240200.0);
        maxRDCDict.put("CN", 6125.0);
        maxRDCDict.put("CC", -15200.0);
        maxRDCDict.put("NH", 24350.0);
        maxRDCDict.put("CH", -60400.0);
        maxRDCDict.put("NC", 6125.0);
        gammaIDict.put("N", -2.71e7);
        gammaIDict.put("C", 6.73e7);
        gammaIDict.put("H", 2.68e8);
        gammaSDict.put("N", -2.71e7);
        gammaSDict.put("C", 6.73e7);
        gammaSDict.put("H", 2.68e8);}
    
    
    RealMatrix AR;
    ArrayRealVector bVec;
    RealVector xVec;
    RealMatrix SR;
    RealMatrix Seig;
    double Q;
    double[] maxRDCs;
    RealVector dcDiffs;
    double[] bCalc;
    double[] bCalcNorm;

    /**
     * This function calculates residual dipolar couplings. Based on orderten_svd_dipole.c
     * 
     * @param vectors List of bond vector coordinates
     * @param dc List of dipolar couplings
     * @param t List of maximum static dipolar coupling values (r^(-3))
     * @param error List of error values
     */
    public OrderSVD(ArrayList<Vector3D> vectors, double[] dc, double[] t, double[] error) {
        int nVectors = vectors.size();
        double[][] A = new double[nVectors][5];
        int iRow = 0;
        // calculate the direction cosines and construct the matrix A. Based on orderten_svd_dipole.c
        for (Vector3D vec3D : vectors) {
            Vector3D normVec = vec3D.normalize();
            double dcosX = normVec.getX();
            double dcosY = normVec.getY();
            double dcosZ = normVec.getZ();
            double ddcosX = dcosX * dcosX;
            double ddcosY = dcosY * dcosY;
            double ddcosZ = dcosZ * dcosZ;
            A[iRow][0] = ddcosY - ddcosX;
            A[iRow][1] = ddcosZ - ddcosX;
            A[iRow][2] = 2 * dcosX * dcosY; //2 * ddcosX * ddcosY;
            A[iRow][3] = 2 * dcosX * dcosZ; //2 * ddcosX * ddcosZ;
            A[iRow][4] = 2 * dcosY * dcosZ; //2 * ddcosY * ddcosZ;
            iRow++;
        }
        
        AR = new Array2DRowRealMatrix(A);
//        System.out.println("A = " + AR.toString());
        // perform SVD on the matrix A
        SingularValueDecomposition svd = new SingularValueDecomposition(AR);
        // construct the b vector, which contains the normalized dipolar couplings 
        double[] dcNorm = new double[dc.length];
        for (int i=0; i<dcNorm.length; i++) {
            dcNorm[i] = dc[i]/t[i];
        }
        bVec = new ArrayRealVector(dcNorm);
        int Slen = 3;
        double[][] S = new double[Slen][Slen];
        Random random = new Random();
        double[] rand = new double[error.length];
        int cycle = 0;
        while (true) {
            System.out.println("cycle " + cycle);
            // calculate the x vector for which |Ax - b| is minimized
            xVec = svd.getSolver().solve(bVec);
            // construct the order matrix S from the x vector and diagonalize it
            S[0][0] = -xVec.getEntry(0) - xVec.getEntry(1);
            S[0][1] = xVec.getEntry(2);
            S[0][2] = xVec.getEntry(3);
            S[1][0] = xVec.getEntry(2);
            S[1][1] = xVec.getEntry(0);
            S[1][2] = xVec.getEntry(4);
            S[2][0] = xVec.getEntry(3);
            S[2][1] = xVec.getEntry(4);
            S[2][2] = xVec.getEntry(1);

            if ((S[0][0] >= -0.5 && S[0][0] <= 1.0) && (S[1][1] >= -0.5 && S[1][1] <= 1.0) && (S[2][2] >= -0.5 && S[2][2] <= 1.0) && 
                (S[1][0] >= -0.75 && S[1][0] <= 0.75) && (S[2][0] >= -0.75 && S[2][0] <= 0.75) && (S[2][1] >= -0.75 && S[2][1] <= 0.75)) {
                SR = new Array2DRowRealMatrix(S);
                EigenDecomposition eig = new EigenDecomposition(SR);
                Seig = eig.getD();
                if ((Seig.getEntry(0, 0) >= -0.5 && Seig.getEntry(0, 0) <= 1.0) && (Seig.getEntry(1, 1) >= -0.5 && Seig.getEntry(1, 1) <= 1.0) && 
                    (Seig.getEntry(2, 2) >= -0.5 && Seig.getEntry(2, 2) <= 1.0)) {
                    System.out.println("Converged after " + cycle + " cycles");
                    System.out.println("x = " + xVec.toString());
                    System.out.println("S = " + SR.toString());
                    System.out.println("Seig = " + Seig.toString());
                    System.out.println("Sz'z' = " + Seig.getEntry(2, 2));
                    System.out.println("eta = " + (Seig.getEntry(1, 1) - Seig.getEntry(0, 0))/Seig.getEntry(2, 2));
                    break;
                } else {
                    for (int i=0; i<error.length; i++) {
                        rand[i] = random.nextGaussian()*error[i]+dcNorm[i];
                    }
                    ArrayRealVector randVec = new ArrayRealVector(rand);
                    bVec = randVec;
                    cycle++;
                }
            } else {
                for (int i=0; i<error.length; i++) {
                    rand[i] = random.nextGaussian()*error[i]+dcNorm[i];
                }
                ArrayRealVector randVec = new ArrayRealVector(rand);
                bVec = randVec;
                cycle++;
            }
        }
    }
    
    /**
     * Reads input files that are formatted in the same way as example.inp. Example file located in orderten_svd_dipole.
     * 
     * @param file String of the name of the file.
     * @return
     */
    public static List readInputFile(String file) {
        ArrayList<Vector3D> vectors = new ArrayList<>();
        ArrayList<Double> dc = new ArrayList<>();
        ArrayList<Double> t = new ArrayList<>();
        ArrayList<Double> errors = new ArrayList<>();
        ArrayList<String> atoms = new ArrayList<>();
        
        try {
            BufferedReader bf = new BufferedReader(new FileReader(file));
            LineNumberReader lineReader = new LineNumberReader(bf);
            while (true) {
                String line = lineReader.readLine();
                if (line == null) {
                    break;
                }
                String sline = line.trim();
                if (sline.length() == 0) {
                    continue;
                }
                if (sline.charAt(0) == '#') {
                    continue;
                }
                String[] sfields = sline.split("\\s+", -1);
                if (sfields.length > 7 && StringUtils.isNumeric(sfields[7])) {
                    double X = Double.parseDouble(sfields[0]) - Double.parseDouble(sfields[3]);
                    double Y = Double.parseDouble(sfields[1]) - Double.parseDouble(sfields[4]);
                    double Z = Double.parseDouble(sfields[2]) - Double.parseDouble(sfields[5]);
                    vectors.add(new Vector3D(X, Y, Z));
                    dc.add(Double.parseDouble(sfields[6]));
                    t.add(Double.parseDouble(sfields[7]));
                    errors.add(Double.parseDouble(sfields[8]));
                    atoms.add(sfields[10]);
                }
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
        }
        
        List info = new ArrayList<>();
        info.add(0, vectors);
        info.add(1, dc);
        info.add(2, t);
        info.add(3, errors);
        info.add(4, atoms);
        
        return info;
    }
    
    public RealMatrix getAMatrix() {
        return AR;
    }
    
    public ArrayRealVector getBVector() {
        return bVec;
    }
    
    public RealVector getXVector() {
        return xVec;
    }
    
    public RealMatrix getSMatrix() {
        return SR;
    }
    
    public RealMatrix getSDiag() {
        return Seig;
    }
    
    public double getSzz() {
        return Seig.getEntry(2, 2);
    }
    
    public double calcEta() {
        return (Seig.getEntry(1, 1) - Seig.getEntry(0, 0))/Seig.getEntry(2, 2);
    }
    
    public double getQ() {
        return Q;
    }
    
    public void setQ(double q) {
        Q = q;
    }
    
    public HashMap getMaxRDCDict() {
        return maxRDCDict;
    }
    
    public double[] getMaxRDCs() {
        return maxRDCs;
    }
    
    public void setMaxRDCs(double[] maxRDC) {
        maxRDCs = maxRDC;
    }
     
    public RealVector calcBVectorNorm() {
        return AR.operate(xVec);
    }
    
    public void setCalcBVectorNorm(RealVector calcBVecNorm) {
        bCalcNorm = calcBVecNorm.toArray();
    }
    
    public RealVector calcBVector() {
        RealVector calcBVecNorm = calcBVectorNorm();
        RealVector tValsR = new ArrayRealVector(maxRDCs);
        RealVector calcBVec = calcBVecNorm.ebeMultiply(tValsR);
        return calcBVec;
    }
    
    public void setCalcBVector(RealVector calcBVec) {
        bCalcNorm = calcBVec.toArray();
    }
    
    public RealVector getRDCDiffs() {
        return dcDiffs; 
    }
    
    public void setRDCDiffs(RealVector rdcDiffs) {
        dcDiffs = rdcDiffs; 
    }
    
    public double[] getCalcBVectorNorm() {
        return bCalcNorm;
    }
    
    public double[] getCalcBVector() {
        return bCalc;
    }
    
    public static HashMap calcVector(String atomName1, String atomName2, boolean calcTVal, boolean scale) {
      
        HashMap atomInfo = new HashMap();
        
        Atom atom1 = Molecule.getAtomByName(atomName1);
        Atom atom2 = Molecule.getAtomByName(atomName2);
        if (atom1 != null & atom2 != null) {
            Point3 v1 = atom1.getPoint();
            Point3 v2 = atom2.getPoint();
            Vector3D vector = v1.subtract(v2);
            String atom1Full = atom1.getName();//.split(":")[1].split("\\.")[1];
            String atom2Full = atom2.getName();//.split(":")[1].split("\\.")[1];
            atomInfo.put("Atom1", atom1Full);
            atomInfo.put("Atom2", atom2Full);
            atomInfo.put("Vector", vector);
            String a1 = atom1Full.substring(0,1);
            String a2 = atom2Full.substring(0,1);
            Double newFactor = maxRDCDict.get(a1+a2);
            if (calcTVal) {
                Double r = Vector3D.distance(v1, v2)*1e-10;
                Double preFactor = -(mu0*hbar)/(4*(Math.PI*Math.PI));
                Double gammaI = (Double) gammaIDict.get(a1);
                Double gammaS = (Double) gammaSDict.get(a2);
                newFactor = preFactor*((gammaI*gammaS)/(r*r*r));
//                System.out.print("calcTVal True new factor = " + newFactor + " ");
                if (scale) {
                    Double gammaH = (Double) gammaIDict.get("H");
                    Double gammaN = (Double) gammaSDict.get("N");
                    Double scaleHN = (gammaH*gammaN)/((1.0e-10)*(1.0e-10)*(1.0e-10));
                    newFactor = 24350.0*(gammaI*gammaS)/((r*r*r)*scaleHN);
                }
//                maxRDCDict.put(a1+a2, newFactor);
//                maxRDCDict.put(a2+a1, newFactor);
            } 
//            System.out.println(a1+a2+" new factor = " + newFactor);
            atomInfo.put("maxRDC", newFactor);//atomInfo.put("maxRDC", maxRDCDict.get(a1+a2));
        }
//        System.out.println("atomInfo = " + atomInfo);
        return atomInfo;
    }
    
    
    public static void calcRDC(String[][] atomPairs, double[] rdc, double[] errors, boolean calcTVal, boolean scale) {
        List<HashMap> infoMaps = new ArrayList<>();
        List<Double> rdc1 = new ArrayList<>();
        List<Double> errors1 = new ArrayList<>();
        for (int i=0; i<atomPairs.length; i++) {
            HashMap info = calcVector(atomPairs[i][0], atomPairs[i][1], calcTVal, scale);
            if (info.isEmpty()) {
                continue;
            }
            infoMaps.add(info);
            rdc1.add(rdc[i]);
            errors1.add(errors[i]);
        }
        ArrayList<Vector3D> vectors = new ArrayList<>();
        for (int i=0; i<infoMaps.size(); i++) {
            vectors.add((Vector3D) infoMaps.get(i).get("Vector"));
        }
        double[] maxRDC = new double[infoMaps.size()];
        for (int i=0; i<maxRDC.length; i++) {
            maxRDC[i] = (Double) infoMaps.get(i).get("maxRDC");
        }
        
        double[] rdc1a = new double[rdc1.size()];
        double[] errors1a = new double[errors1.size()];
        for (int i=0; i<rdc1a.length; i++) {
            rdc1a[i] = rdc1.get(i);
            errors1a[i] = errors1.get(i);
        }
        
     
        if (!vectors.isEmpty()) {
            OrderSVD orderSVD = new OrderSVD(vectors, rdc1a, maxRDC, errors1a);
        
            orderSVD.setMaxRDCs(maxRDC);

            double[] b = orderSVD.getBVector().toArray();
            RealVector bCalcVecNorm = orderSVD.calcBVectorNorm();
            orderSVD.setCalcBVectorNorm(bCalcVecNorm);
            RealVector bCalcVec = orderSVD.calcBVector();
            orderSVD.setCalcBVector(bCalcVec);

            RealVector rdcR = new ArrayRealVector(rdc1a);
            RealVector dcSq = rdcR.ebeMultiply(rdcR);
            RealVector dcDiff = bCalcVec.subtract(rdcR);
            orderSVD.setRDCDiffs(dcDiff);

            RealVector dcDiffs1 = orderSVD.getRDCDiffs();
            RealVector dcDiffSq = dcDiffs1.ebeMultiply(dcDiffs1); 
            double dcDiffSqSum = 0;
            for (int i=0; i<dcDiffSq.getDimension(); i++) {
                dcDiffSqSum += dcDiffSq.getEntry(i);
            }
            double dcSqSum = 0;
            for (int i=0; i<dcSq.getDimension(); i++) {
                dcSqSum += dcSq.getEntry(i);
            }

            double q = Math.sqrt(dcDiffSqSum/dcDiffs1.getDimension())/Math.sqrt(dcSqSum/rdc1a.length);
            orderSVD.setQ(q);
            System.out.println("Q = " + String.valueOf(orderSVD.getQ()));
        }
    }
    
}

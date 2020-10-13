package org.nmrfx.structure.chemistry.predict;

import java.util.Map;

public class ProteinPredictorGen {
public static ProteinPredictorResult predict(Map<String, Double> valueMap, double[] coefs, double[] minMax, boolean explain) {
double[] attrValue = new double[111];Double GLN= valueMap.get("GLN");
Double psiC= valueMap.get("psiC");
Double h3= valueMap.get("h3");
Double psiS= valueMap.get("psiS");
Double GLY= valueMap.get("GLY");
Double ARO_P= valueMap.get("ARO_P");
Double ARO_S= valueMap.get("ARO_S");
Double hshift3= valueMap.get("hshift3");
Double hshift2= valueMap.get("hshift2");
Double ring= valueMap.get("ring");
Double CHRG_S= valueMap.get("CHRG_S");
Double hshift1= valueMap.get("hshift1");
Double CHRG_P= valueMap.get("CHRG_P");
Double eshift= valueMap.get("eshift");
Double chi2C= valueMap.get("chi2C");
Double SER= valueMap.get("SER");
Double LYS= valueMap.get("LYS");
Double psiP= valueMap.get("psiP");
Double chiC= valueMap.get("chiC");
Double DIS= valueMap.get("DIS");
Double BULK_S= valueMap.get("BULK_S");
Double ASN= valueMap.get("ASN");
Double BULK_P= valueMap.get("BULK_P");
Double phiC= valueMap.get("phiC");
Double VAL= valueMap.get("VAL");
Double HPHB_P= valueMap.get("HPHB_P");
Double HPHB_S= valueMap.get("HPHB_S");
Double intercept= valueMap.get("intercept");
Double phiS= valueMap.get("phiS");
Double phiP= valueMap.get("phiP");
Double TRP= valueMap.get("TRP");
Double PHE= valueMap.get("PHE");
Double MET= valueMap.get("MET");
Double LEU= valueMap.get("LEU");
Double PRO_P= valueMap.get("PRO_P");
Double PRO_S= valueMap.get("PRO_S");
Double TYR= valueMap.get("TYR");
    if (ProteinPredictor.checkAngles(chiC,chi2C)) {
        attrValue[0] = Math.cos(chiC)*Math.sin(chi2C);
    } else {
        attrValue[0] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiS)) {
        attrValue[1] = Math.cos(2*psiS);
    } else {
        attrValue[1] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP,psiC)) {
        attrValue[2] = Math.cos(psiC)*Math.cos(psiP);
    } else {
        attrValue[2] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP)) {
        attrValue[3] = Math.sin(3*psiP);
    } else {
        attrValue[3] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chi2C)) {
        attrValue[4] = Math.sin(2*chi2C);
    } else {
        attrValue[4] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[5] = Math.sin(phiC)*PRO_P;
    } else {
        attrValue[5] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chiC)) {
        attrValue[6] = Math.cos(2*chiC);
    } else {
        attrValue[6] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiP)) {
        attrValue[7] = Math.sin(phiP);
    } else {
        attrValue[7] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[8] = Math.sin(3*psiC);
    } else {
        attrValue[8] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP)) {
        attrValue[9] = Math.cos(psiP);
    } else {
        attrValue[9] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiP)) {
        attrValue[10] = Math.cos(2*phiP);
    } else {
        attrValue[10] = 0.0;
    }
    attrValue[11] = LYS;
    if (ProteinPredictor.checkAngles(phiC,chiC)) {
        attrValue[12] = Math.cos(phiC)*Math.cos(chiC);
    } else {
        attrValue[12] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[13] = Math.sin(psiC)*ARO_S;
    } else {
        attrValue[13] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiP)) {
        attrValue[14] = Math.sin(3*phiP);
    } else {
        attrValue[14] = 0.0;
    }
    attrValue[15] = h3;
    if (ProteinPredictor.checkAngles(psiC,chi2C)) {
        attrValue[16] = Math.sin(psiC)*Math.cos(chi2C);
    } else {
        attrValue[16] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chi2C)) {
        attrValue[17] = Math.sin(chi2C);
    } else {
        attrValue[17] = 0.0;
    }
    attrValue[18] = VAL;
    if (ProteinPredictor.checkAngles(phiP)) {
        attrValue[19] = Math.cos(phiP);
    } else {
        attrValue[19] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiS)) {
        attrValue[20] = Math.sin(3*psiS);
    } else {
        attrValue[20] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiS)) {
        attrValue[21] = Math.cos(3*psiS);
    } else {
        attrValue[21] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,chiC)) {
        attrValue[22] = Math.cos(psiC)*Math.cos(chiC);
    } else {
        attrValue[22] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[23] = Math.cos(phiC)*BULK_P;
    } else {
        attrValue[23] = 0.0;
    }
    attrValue[24] = GLY;
    attrValue[25] = ring;
    attrValue[26] = PHE;
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[27] = Math.cos(2*psiC);
    } else {
        attrValue[27] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[28] = Math.cos(phiC)*ARO_P;
    } else {
        attrValue[28] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC,chi2C)) {
        attrValue[29] = Math.cos(phiC)*Math.cos(chi2C);
    } else {
        attrValue[29] = 0.0;
    }
    attrValue[30] = TYR;
    if (ProteinPredictor.checkAngles(psiC,phiC)) {
        attrValue[31] = Math.cos(psiC)*Math.cos(phiC);
    } else {
        attrValue[31] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[32] = Math.sin(2*psiC);
    } else {
        attrValue[32] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[33] = Math.sin(phiC)*ARO_P;
    } else {
        attrValue[33] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[34] = Math.cos(psiC)*ARO_S;
    } else {
        attrValue[34] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[35] = Math.cos(psiC)*CHRG_S;
    } else {
        attrValue[35] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[36] = Math.cos(psiC)*HPHB_S;
    } else {
        attrValue[36] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP,psiC)) {
        attrValue[37] = Math.sin(psiC)*Math.cos(psiP);
    } else {
        attrValue[37] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC,chi2C)) {
        attrValue[38] = Math.sin(phiC)*Math.cos(chi2C);
    } else {
        attrValue[38] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chi2C)) {
        attrValue[39] = Math.cos(chi2C);
    } else {
        attrValue[39] = 0.0;
    }
    attrValue[40] = hshift3;
    attrValue[41] = hshift2;
    attrValue[42] = hshift1;
    if (ProteinPredictor.checkAngles(phiC,chiC)) {
        attrValue[43] = Math.sin(phiC)*Math.sin(chiC);
    } else {
        attrValue[43] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[44] = Math.sin(psiC)*PRO_S;
    } else {
        attrValue[44] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC,chi2C)) {
        attrValue[45] = Math.sin(phiC)*Math.sin(chi2C);
    } else {
        attrValue[45] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[46] = Math.cos(psiC)*BULK_S;
    } else {
        attrValue[46] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chiC,chi2C)) {
        attrValue[47] = Math.cos(chiC)*Math.cos(chi2C);
    } else {
        attrValue[47] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[48] = Math.cos(3*psiC);
    } else {
        attrValue[48] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiS)) {
        attrValue[49] = Math.sin(psiS);
    } else {
        attrValue[49] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP)) {
        attrValue[50] = Math.cos(2*psiP);
    } else {
        attrValue[50] = 0.0;
    }
    attrValue[51] = 0.0;
int interceptCoef = 51;
    attrValue[52] = TRP;
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[53] = Math.cos(2*phiC);
    } else {
        attrValue[53] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,chiC)) {
        attrValue[54] = Math.sin(psiC)*Math.sin(chiC);
    } else {
        attrValue[54] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[55] = Math.sin(psiC)*HPHB_S;
    } else {
        attrValue[55] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiS)) {
        attrValue[56] = Math.cos(2*phiS);
    } else {
        attrValue[56] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiS)) {
        attrValue[57] = Math.sin(2*psiS);
    } else {
        attrValue[57] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiS)) {
        attrValue[58] = Math.sin(phiS);
    } else {
        attrValue[58] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC,chi2C)) {
        attrValue[59] = Math.cos(phiC)*Math.sin(chi2C);
    } else {
        attrValue[59] = 0.0;
    }
    attrValue[60] = GLN;
    if (ProteinPredictor.checkAngles(phiP)) {
        attrValue[61] = Math.sin(2*phiP);
    } else {
        attrValue[61] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP)) {
        attrValue[62] = Math.sin(psiP);
    } else {
        attrValue[62] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiS)) {
        attrValue[63] = Math.cos(phiS);
    } else {
        attrValue[63] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiS)) {
        attrValue[64] = Math.cos(3*phiS);
    } else {
        attrValue[64] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,chi2C)) {
        attrValue[65] = Math.cos(psiC)*Math.sin(chi2C);
    } else {
        attrValue[65] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiS)) {
        attrValue[66] = Math.cos(psiS);
    } else {
        attrValue[66] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[67] = Math.sin(psiC)*CHRG_S;
    } else {
        attrValue[67] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[68] = Math.sin(2*phiC);
    } else {
        attrValue[68] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,chiC)) {
        attrValue[69] = Math.cos(psiC)*Math.sin(chiC);
    } else {
        attrValue[69] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[70] = Math.cos(phiC);
    } else {
        attrValue[70] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[71] = Math.sin(phiC);
    } else {
        attrValue[71] = 0.0;
    }
    attrValue[72] = SER;
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[73] = Math.cos(3*phiC);
    } else {
        attrValue[73] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chiC,chi2C)) {
        attrValue[74] = Math.sin(chiC)*Math.sin(chi2C);
    } else {
        attrValue[74] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[75] = Math.cos(psiC)*PRO_S;
    } else {
        attrValue[75] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,chiC)) {
        attrValue[76] = Math.sin(psiC)*Math.cos(chiC);
    } else {
        attrValue[76] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[77] = Math.sin(psiC);
    } else {
        attrValue[77] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP)) {
        attrValue[78] = Math.sin(2*psiP);
    } else {
        attrValue[78] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[79] = Math.cos(phiC)*HPHB_P;
    } else {
        attrValue[79] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[80] = Math.cos(phiC)*CHRG_P;
    } else {
        attrValue[80] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[81] = Math.cos(psiC);
    } else {
        attrValue[81] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiS)) {
        attrValue[82] = Math.sin(2*phiS);
    } else {
        attrValue[82] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chiC,chi2C)) {
        attrValue[83] = Math.sin(chiC)*Math.cos(chi2C);
    } else {
        attrValue[83] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chiC)) {
        attrValue[84] = Math.cos(chiC);
    } else {
        attrValue[84] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[85] = Math.cos(phiC)*PRO_P;
    } else {
        attrValue[85] = 0.0;
    }
    attrValue[86] = ASN;
    attrValue[87] = MET;
    attrValue[88] = LEU;
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[89] = Math.sin(phiC)*CHRG_P;
    } else {
        attrValue[89] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,phiC)) {
        attrValue[90] = Math.cos(psiC)*Math.sin(phiC);
    } else {
        attrValue[90] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[91] = Math.sin(3*phiC);
    } else {
        attrValue[91] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP)) {
        attrValue[92] = Math.cos(3*psiP);
    } else {
        attrValue[92] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiP,psiC)) {
        attrValue[93] = Math.cos(psiC)*Math.sin(psiP);
    } else {
        attrValue[93] = 0.0;
    }
    attrValue[94] = DIS;
    if (ProteinPredictor.checkAngles(psiP,psiC)) {
        attrValue[95] = Math.sin(psiC)*Math.sin(psiP);
    } else {
        attrValue[95] = 0.0;
    }
    if (eshift != null) attrValue[96] = eshift;
    if (ProteinPredictor.checkAngles(phiC,chiC)) {
        attrValue[97] = Math.sin(phiC)*Math.cos(chiC);
    } else {
        attrValue[97] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,phiC)) {
        attrValue[98] = Math.sin(psiC)*Math.sin(phiC);
    } else {
        attrValue[98] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chiC)) {
        attrValue[99] = Math.sin(chiC);
    } else {
        attrValue[99] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[100] = Math.sin(phiC)*BULK_P;
    } else {
        attrValue[100] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,phiC)) {
        attrValue[101] = Math.sin(psiC)*Math.cos(phiC);
    } else {
        attrValue[101] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,chi2C)) {
        attrValue[102] = Math.sin(psiC)*Math.sin(chi2C);
    } else {
        attrValue[102] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC,chi2C)) {
        attrValue[103] = Math.cos(psiC)*Math.cos(chi2C);
    } else {
        attrValue[103] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chi2C)) {
        attrValue[104] = Math.cos(2*chi2C);
    } else {
        attrValue[104] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC)) {
        attrValue[105] = Math.sin(phiC)*HPHB_P;
    } else {
        attrValue[105] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiP)) {
        attrValue[106] = Math.cos(3*phiP);
    } else {
        attrValue[106] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiC,chiC)) {
        attrValue[107] = Math.cos(phiC)*Math.sin(chiC);
    } else {
        attrValue[107] = 0.0;
    }
    if (ProteinPredictor.checkAngles(chiC)) {
        attrValue[108] = Math.sin(2*chiC);
    } else {
        attrValue[108] = 0.0;
    }
    if (ProteinPredictor.checkAngles(phiS)) {
        attrValue[109] = Math.sin(3*phiS);
    } else {
        attrValue[109] = 0.0;
    }
    if (ProteinPredictor.checkAngles(psiC)) {
        attrValue[110] = Math.sin(psiC)*BULK_S;
    } else {
        attrValue[110] = 0.0;
    }

        double contactSum = valueMap.get("contacts");
        double scale = ProteinPredictor.calcDisorderScale(contactSum, minMax);
        double sum = coefs[interceptCoef];
        for (int i = 0; i < attrValue.length; i++) {
            sum += scale * attrValue[i] * coefs[i];
        }

        ProteinPredictorResult result;
        if (explain) {
            result =  new ProteinPredictorResult(coefs, attrValue, sum);
        }  else {
            result =  new ProteinPredictorResult(sum);
        }
        return result;
}

}


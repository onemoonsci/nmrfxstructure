from org.nmrfx.structure.chemistry.predict import RNAStats

RNAStats.readFile('data/rnadata.txt')
#rStats = RNAStats.get("GH5p P- A- G- CG Pp 8 9 10 11 - -", False)
rStats = RNAStats.get("CH41_Pp_CG_CG_-_-_-_-_-_-_-_-", True)

print rStats

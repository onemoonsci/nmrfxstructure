/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.project;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.io.MoleculeIOException;
import org.nmrfx.structure.chemistry.io.PDBFile;
import org.nmrfx.structure.chemistry.io.PPMFiles;
import org.nmrfx.structure.chemistry.io.SDFile;
import org.nmrfx.structure.chemistry.io.Sequence;
import static org.nmrfx.processor.project.ProjectLoader.currentProjectDir;

/**
 *
 * @author Bruce Johnson
 */
public class StructureProjectLoader extends ProjectLoader {

    public void loadStructureProject(Path projectDir) throws IOException, MoleculeIOException, IllegalStateException {
        loadProject(projectDir);
        FileSystem fileSystem = FileSystems.getDefault();

        String[] subDirTypes = {"molecules", "shifts", "refshifts"};
        if (projectDir != null) {
            for (String subDir : subDirTypes) {
                Path subDirectory = fileSystem.getPath(projectDir.toString(), subDir);
                if (Files.exists(subDirectory) && Files.isDirectory(subDirectory) && Files.isReadable(subDirectory)) {
                    switch (subDir) {
                        case "molecules":
                            loadMolecules(subDirectory);
                            break;
                        case "shifts":
                            loadShiftFiles(subDirectory, false);
                            break;
                        case "refshifts":
                            loadShiftFiles(subDirectory, false);
                            break;
                        default:
                            throw new IllegalStateException("Invalid subdir type");
                    }
                }

            }
        }
        currentProjectDir = projectDir;
    }

    public void saveProject() throws IOException {
        if (currentProjectDir == null) {
            throw new IllegalArgumentException("Project directory not set");
        }
        super.saveProject();
        saveShifts(false);
        saveShifts(true);
    }

    void loadMolecules(Path directory) throws MoleculeIOException {
        if (Files.isDirectory(directory)) {
            try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(directory)) {
                for (Path path : fileStream) {
                    if (Files.isDirectory(path)) {
                        loadMoleculeEntities(path);
                    } else {
                        loadMolecule(path);
                    }

                }
            } catch (DirectoryIteratorException | IOException ex) {
            }
        }
    }

    void loadMolecule(Path file) throws MoleculeIOException {
        if (file.toString().endsWith(".pdb")) {
            PDBFile pdbReader = new PDBFile();
            pdbReader.readSequence(file.toString(), false);
            System.out.println("read mol: " + file.toString());
        } else if (file.toString().endsWith(".sdf")) {
            SDFile.read(file.toString(), null);
        } else if (file.toString().endsWith(".seq")) {
            Sequence seq = new Sequence();
            seq.read(file.toString());
        }
        if (Molecule.getActive() == null) {
            throw new MoleculeIOException("Couldn't open any molecules");
        }
        System.out.println("active mol " + Molecule.getActive().getName());
    }

    void loadMoleculeEntities(Path directory) throws MoleculeIOException, IOException {
        String molName = directory.getFileName().toString();
        Molecule mol = new Molecule(molName);
        PDBFile pdbReader = new PDBFile();
        Pattern pattern = Pattern.compile("(.+)\\.(seq|pdb|mol|sdf)");
        Predicate<String> predicate = pattern.asPredicate();
        if (Files.isDirectory(directory)) {
            Files.list(directory).sequential().filter(path -> predicate.test(path.getFileName().toString())).
                    sorted(new FileComparator()).
                    forEach(path -> {
                        String pathName = path.toString();
                        String fileName = path.getFileName().toString();
                        Matcher matcher = pattern.matcher(fileName);
                        String baseName = matcher.group(1);
                        System.out.println("read mol: " + pathName);

                        try {
                            if (fileName.endsWith(".seq")) {
                                Sequence sequence = new Sequence();
                                sequence.read(pathName);
                            } else if (fileName.endsWith(".pdb")) {
                                if (mol.entities.isEmpty()) {
                                    pdbReader.readSequence(pathName, false);
                                } else {
                                    PDBFile.readResidue(pathName, null, mol, baseName);
                                }
                            } else if (fileName.endsWith(".sdf")) {
                                SDFile.readResidue(pathName, null, mol, baseName);
                            }
                        } catch (MoleculeIOException molE) {
                        }

                    });
        }
    }

    void loadShiftFiles(Path directory, boolean refMode) throws MoleculeIOException, IOException {
        Molecule mol = Molecule.getActive();
        Pattern pattern = Pattern.compile("(.+)\\.(txt|ppm)");
        Predicate<String> predicate = pattern.asPredicate();
        if (Files.isDirectory(directory)) {
            Files.list(directory).sequential().filter(path -> predicate.test(path.getFileName().toString())).
                    sorted(new FileComparator()).
                    forEach(path -> {
                        String fileName = path.getFileName().toString();
                        Optional<Integer> fileNum = getIndex(fileName);
                        int ppmSet = fileNum.isPresent() ? fileNum.get() : 0;
                        PPMFiles.readPPM(mol, path, ppmSet, refMode);
                    });
        }
    }

    void saveShifts(boolean refMode) throws IOException {
        Molecule mol = Molecule.getActive();
        if (mol == null) {
            return;
        }
        FileSystem fileSystem = FileSystems.getDefault();

        if (currentProjectDir == null) {
            throw new IllegalArgumentException("Project directory not set");
        }
        Path projectDir = currentProjectDir;
        int ppmSet = 0;
        String fileName = String.valueOf(ppmSet) + "_" + "ppm.txt";
        String subDir = refMode ? "refshifts" : "shifts";
        Path peakFilePath = fileSystem.getPath(projectDir.toString(), subDir, fileName);
        // fixme should only write if file doesn't already exist or peaklist changed since read
        try (FileWriter writer = new FileWriter(peakFilePath.toFile())) {
            PPMFiles.writePPM(mol, writer, ppmSet, refMode);
            writer.close();
        } catch (IOException ioE) {
            throw ioE;
        }
    }

}

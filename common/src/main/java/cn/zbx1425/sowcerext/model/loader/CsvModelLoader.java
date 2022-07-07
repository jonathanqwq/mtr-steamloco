package cn.zbx1425.sowcerext.model.loader;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.sowcer.batch.BatchProp;
import cn.zbx1425.sowcer.model.Model;
import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import cn.zbx1425.sowcerext.model.Face;
import cn.zbx1425.sowcerext.model.RawMesh;
import cn.zbx1425.sowcerext.model.Vertex;
import com.mojang.math.Vector3f;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class CsvModelLoader {

    public static Model loadModel(ResourceManager resourceManager, ResourceLocation objLocation, VertAttrMapping mapping) {
        String rawModelData = mtr.sound.bve.BveTrainSoundConfig.readResource(resourceManager, objLocation);
        String[] rawModelLines = rawModelData.split("[\\r\\n]+");

        List<RawMesh> builtMeshList = new ArrayList<>();
        RawMesh buildingMesh = new RawMesh(new BatchProp("rendertype_entity_cutout", null));
        for (String line : rawModelLines) {
            if (line.contains(";")) line = line.split(";", 1)[0];
            line = line.trim().toLowerCase();
            if (StringUtils.isEmpty(line)) continue;
            String[] tokens = line.split(",");
            switch (tokens[0]) {
                case "createmeshbuildingMesh":
                    if (!buildingMesh.checkVertIndex())
                        throw new IndexOutOfBoundsException("Invalid vertex index in AddFace/AddFace2.");
                    if (buildingMesh.faces.size() > 0) builtMeshList.add(buildingMesh);
                    buildingMesh = new RawMesh(new BatchProp("rendertype_entity_cutout", null));
                    break;
                case "addvertex":
                    if (tokens.length == 4) {
                        buildingMesh.vertices.add(new Vertex(
                                new Vector3f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]))
                        ));
                    } else if (tokens.length == 7) {
                        buildingMesh.vertices.add(new Vertex(
                                new Vector3f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])),
                                new Vector3f(Float.parseFloat(tokens[4]), Float.parseFloat(tokens[5]), Float.parseFloat(tokens[6]))
                        ));
                    } else {
                        throw new IllegalArgumentException("Invalid AddVertex command.");
                    }
                    break;
                case "addface":
                case "addface2":
                    if (tokens.length < 4) throw new IllegalArgumentException("Invalid AddFace/AddFace2 command.");
                    int[] vertIndices = new int[tokens.length - 1];
                    for (int i = 1; i < tokens.length; ++i) {
                        vertIndices[i - 1] = Integer.parseInt(tokens[i]);
                    }
                    buildingMesh.faces.addAll(Face.triangulate(vertIndices, tokens[0].equals("addface2")));
                    break;
                case "setcolor":
                case "setcolorall":
                    Integer[] setColorParams = parseParams(tokens, new Integer[]{0, 0, 0, 255}, Integer::parseInt);
                    if (tokens[0].equals("setcolorall")) {
                        for (RawMesh mesh : builtMeshList) {
                            mesh.batchProp.attrState.color = setColorParams[0] << 24 | setColorParams[1] << 16 | setColorParams[2] << 8 | setColorParams[3];
                        }
                    }
                    buildingMesh.batchProp.attrState.color = setColorParams[0] << 24 | setColorParams[1] << 16 | setColorParams[2] << 8 | setColorParams[3];
                    break;
                case "loadtexture":
                    if (tokens.length < 2) throw new IllegalArgumentException("Invalid LoadTexture command.");
                    String parentDirName = new File(objLocation.getPath()).getParent();
                    if (parentDirName == null) parentDirName = "";
                    String texFileName = tokens[1].toLowerCase(Locale.ROOT);
                    if (!texFileName.endsWith(".png")) texFileName += ".png";
                    buildingMesh.batchProp.texture = new ResourceLocation(objLocation.getNamespace(), parentDirName + "/" + texFileName);
                    break;
                case "settexturecoordinates":
                    if (tokens.length < 4) throw new IllegalArgumentException("Invalid SetTextureCoordinates command.");
                    int vertId = Integer.parseInt(tokens[1]);
                    if (vertId < 0 || vertId >= buildingMesh.vertices.size())
                        throw new IndexOutOfBoundsException("Invalid vertex index in SetTextureCoordinates.");
                    buildingMesh.vertices.get(vertId).u = Float.parseFloat(tokens[2]);
                    buildingMesh.vertices.get(vertId).v = Float.parseFloat(tokens[3]);
                    break;
                case "generatenormals":
                    break;
                case "cube":
                    Float[] cubeParams = parseParams(tokens, new Float[]{1F, 0F, 0F}, Float::parseFloat);
                    if (cubeParams[1] == 0F) cubeParams[1] = cubeParams[0];
                    if (cubeParams[2] == 0F) cubeParams[2] = cubeParams[0];
                    createCube(buildingMesh, cubeParams[0], cubeParams[1], cubeParams[2]);
                    break;
                case "cylinder":
                    Float[] cylinderParams = parseParams(tokens, new Float[]{8F, 1F, 1F, 1F}, Float::parseFloat);
                    createCylinder(buildingMesh, Math.round(cylinderParams[0]), cylinderParams[1], cylinderParams[2], cylinderParams[3]);
                    break;
                case "translate":
                case "translateall":
                    Float[] translateParams = parseParams(tokens, new Float[]{0F, 0F, 0F}, Float::parseFloat);
                    if (tokens[0].equals("translateall")) {
                        for (RawMesh mesh : builtMeshList) {
                            mesh.applyTranslation(translateParams[0], translateParams[1], translateParams[2]);
                        }
                    }
                    buildingMesh.applyTranslation(translateParams[0], translateParams[1], translateParams[2]);
                    break;
                case "scale":
                case "scaleall":
                    Float[] scaleParams = parseParams(tokens, new Float[]{1F, 1F, 1F}, Float::parseFloat);
                    if (tokens[0].equals("scaleall")) {
                        for (RawMesh mesh : builtMeshList) {
                            mesh.applyScale(scaleParams[0], scaleParams[1], scaleParams[2]);
                        }
                    }
                    buildingMesh.applyScale(scaleParams[0], scaleParams[1], scaleParams[2]);
                    break;
                case "rotate":
                case "rotateall":
                    Float[] rotateParams = parseParams(tokens, new Float[]{0F, 0F, 0F, 0F}, Float::parseFloat);
                    if (tokens[0].equals("rotateall")) {
                        for (RawMesh mesh : builtMeshList) {
                            mesh.applyRotation(new Vector3f(rotateParams[0], rotateParams[1], rotateParams[2]), rotateParams[3]);
                        }
                    }
                    buildingMesh.applyRotation(new Vector3f(rotateParams[0], rotateParams[1], rotateParams[2]), rotateParams[3]);
                    break;
                case "shear":
                case "shearall":
                    Float[] shearParams = parseParams(tokens, new Float[]{0F, 0F, 0F, 0F, 0F, 0F, 0F}, Float::parseFloat);
                    if (tokens[0].equals("shearall")) {
                        for (RawMesh mesh : builtMeshList) {
                            mesh.applyShear(
                                    new Vector3f(shearParams[0], shearParams[1], shearParams[2]),
                                    new Vector3f(shearParams[3], shearParams[4], shearParams[5]),
                                    shearParams[6]
                            );
                        }
                    }
                    buildingMesh.applyShear(
                            new Vector3f(shearParams[0], shearParams[1], shearParams[2]),
                            new Vector3f(shearParams[3], shearParams[4], shearParams[5]),
                            shearParams[6]
                    );
                    break;
                case "mirror":
                case "mirrorall":
                    Integer[] mirrorParams = parseParams(tokens, new Integer[]{0, 0, 0, 0, 0, 0}, Integer::parseInt);
                    if (tokens.length < 5) {
                        mirrorParams[3] = mirrorParams[0];
                        mirrorParams[4] = mirrorParams[1];
                        mirrorParams[5] = mirrorParams[2];
                    }
                    if (tokens[0].equals("mirrorall")) {
                        for (RawMesh mesh : builtMeshList) {
                            mesh.applyMirror(
                                    mirrorParams[0] != 0, mirrorParams[1] != 0, mirrorParams[2] != 0,
                                    mirrorParams[3] != 0, mirrorParams[4] != 0, mirrorParams[5] != 0
                            );
                        }
                    }
                    buildingMesh.applyMirror(
                            mirrorParams[0] != 0, mirrorParams[1] != 0, mirrorParams[2] != 0,
                            mirrorParams[3] != 0, mirrorParams[4] != 0, mirrorParams[5] != 0
                    );
                    break;
                case "setemissivecolor":
                case "setemissivecolorall":
                    Integer[] setEmissiveColorParams = parseParams(tokens, new Integer[]{0, 0, 0, 255}, Integer::parseInt);
                    boolean turnOnLight = setEmissiveColorParams[0] > 128;
                    if (tokens[0].equals("setemissivecolorall")) {
                        for (RawMesh mesh : builtMeshList) {
                            if (turnOnLight) {
                                mesh.batchProp.shaderName = "rendertype_beacon_beam";
                            }
                        }
                    }
                    buildingMesh.batchProp.shaderName = "rendertype_beacon_beam";
                    break;
                case "setblendmode":
                case "setwrapmode":
                case "setdecaltransparentcolor":
                case "enablecrossfading":
                    Main.LOGGER.warn("CSV command that cannot and will not be supported: " + tokens[0]);
                    break;
                default:
                    Main.LOGGER.warn("Unknown CSV command: " + tokens[0]);
                    break;
            }

        }
        if (!buildingMesh.checkVertIndex())
            throw new IndexOutOfBoundsException("Vertex index out of bound in " + objLocation);
        if (buildingMesh.faces.size() > 0) builtMeshList.add(buildingMesh);

        HashMap<BatchProp, RawMesh> optimizedMeshes = new HashMap<>();
        for (RawMesh mesh : builtMeshList) {
            RawMesh target = optimizedMeshes.computeIfAbsent(mesh.batchProp, RawMesh::new);
            target.append(mesh);
        }

        Model model = new Model();
        for (RawMesh mesh : optimizedMeshes.values()) {
            if (!mesh.checkVertIndex()) throw new AssertionError("Bad VertIndex before mesh distinct");
            mesh.distinct();
            if (!mesh.checkVertIndex()) throw new AssertionError("Bad VertIndex after mesh distinct");
            mesh.applyScale(0, 0, -1); // Convert DirectX coords to OpenGL coords
            mesh.generateNormals();
            if (mesh.faces.size() == 0) continue;
            model.meshList.add(mesh.upload(mapping));
        }

        return model;
    }

    private static <T> T[] parseParams(String[] tokens, T[] defaults, Function<String, T> parser) {
        T[] result = defaults.clone();
        for (int i = 0; i < defaults.length; ++i) {
            if (i + 1 >= tokens.length) return result;
            String valueStr = tokens[i + 1].trim();
            if (StringUtils.isEmpty(valueStr)) continue;
            result[i] = parser.apply(valueStr);
        }
        return result;
    }

    private static void createCube(RawMesh buildingMesh, float sx, float sy, float sz) {
        int v = buildingMesh.vertices.size();
        buildingMesh.vertices.add(new Vertex(new Vector3f(sx, sy, -sz)));
        buildingMesh.vertices.add(new Vertex(new Vector3f(sx, -sy, -sz)));
        buildingMesh.vertices.add(new Vertex(new Vector3f(-sx, -sy, -sz)));
        buildingMesh.vertices.add(new Vertex(new Vector3f(-sx, sy, -sz)));
        buildingMesh.vertices.add(new Vertex(new Vector3f(sx, sy, sz)));
        buildingMesh.vertices.add(new Vertex(new Vector3f(sx, -sy, sz)));
        buildingMesh.vertices.add(new Vertex(new Vector3f(-sx, -sy, sz)));
        buildingMesh.vertices.add(new Vertex(new Vector3f(-sx, sy, sz)));
        buildingMesh.faces.add(new Face(new int[]{v, v + 1, v + 2}));
        buildingMesh.faces.add(new Face(new int[]{v, v + 2, v + 3}));
        buildingMesh.faces.add(new Face(new int[]{v, v + 4, v + 5}));
        buildingMesh.faces.add(new Face(new int[]{v, v + 5, v + 1}));
        buildingMesh.faces.add(new Face(new int[]{v, v + 3, v + 7}));
        buildingMesh.faces.add(new Face(new int[]{v, v + 7, v + 4}));
        buildingMesh.faces.add(new Face(new int[]{v + 6, v + 5, v + 4}));
        buildingMesh.faces.add(new Face(new int[]{v + 6, v + 4, v + 7}));
        buildingMesh.faces.add(new Face(new int[]{v + 6, v + 7, v + 3}));
        buildingMesh.faces.add(new Face(new int[]{v + 6, v + 3, v + 2}));
        buildingMesh.faces.add(new Face(new int[]{v + 6, v + 2, v + 1}));
        buildingMesh.faces.add(new Face(new int[]{v + 6, v + 1, v + 5}));
    }

    // create cylinder
    private static void createCylinder(RawMesh buildingMesh, int n, float r1, float r2, float h) {
        // parameters
        boolean uppercap = r1 > 0.0;
        boolean lowercap = r2 > 0.0;
        int m = (uppercap ? 1 : 0) + (lowercap ? 1 : 0);
        r1 = Math.abs(r1);
        r2 = Math.abs(r2);
        float ns = h >= 0.0F ? 1.0F : -1.0F;
        // initialization
        float d = (float) (2.0 * Math.PI / n);
        float g = 0.5F * h;
        float t = 0.0F;
        float a = (float) (h != 0.0 ? Math.atan((r2 - r1) / h) : 0.0);
        // vertices and normals
        int v = buildingMesh.vertices.size();
        for (int i = 0; i < n; i++) {
            float dx = (float) Math.cos(t);
            float dz = (float) Math.sin(t);
            float lx = dx * r2;
            float lz = dz * r2;
            float ux = dx * r1;
            float uz = dz * r1;
            Vector3f normal = new Vector3f(dx * ns, 0.0F, dz * ns);
            Vector3f s = normal.copy();
            s.cross(Vector3f.YN);
            normal.transform(s.rotation(a));
            buildingMesh.vertices.add(new Vertex(new Vector3f(ux, g, uz), normal));
            buildingMesh.vertices.add(new Vertex(new Vector3f(lx, -g, lz), normal));
            t += d;
        }
        // faces

        for (int i = 0; i < n; i++) {
            int i0 = (2 * i + 2) % (2 * n);
            int i1 = (2 * i + 3) % (2 * n);
            int i2 = 2 * i + 1;
            int i3 = 2 * i;
            buildingMesh.faces.add(new Face(new int[]{v + i0, v + i1, v + i2}));
            buildingMesh.faces.add(new Face(new int[]{v + i0, v + i2, v + i3}));
        }

        for (int i = 0; i < m; i++) {
            List<Integer> verts = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (verts.size() > 2) {
                    verts.add(verts.get(0));
                    verts.add(verts.get(verts.size() - 2));
                }
                if (i == 0 & lowercap) {
                    // lower cap
                    verts.add(v + 2 * j + 1);
                } else {
                    // upper cap
                    verts.add(v + 2 * (n - j - 1));
                }
            }
            verts.add(verts.get(0));
            verts.add(verts.get(verts.size() - 1));
            verts.add(verts.get(1));
            for (int j = 0; j < verts.size() / 3; ++j) {
                buildingMesh.faces.add(new Face(new int[]{verts.get(j * 3), verts.get(j * 3 + 1), verts.get(j * 3 + 2)}));
            }
        }

    }

}

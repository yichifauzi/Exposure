package io.github.mortuusars.exposure.camera.capture.converter;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mortuusars.exposure.camera.capture.Capture;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.material.MapColor;

import java.util.Arrays;
import java.util.Objects;

public class SimpleColorConverter implements IImageToMapColorsConverter {

    public static MapColor[] getMapColors() {
        MapColor[] colors = new MapColor[64];
        for (int i = 0; i <= 63; i++){
            colors[i] = MapColor.byId(i);
        }
        return colors;
    }

    @Override
    public byte[] convert(Capture capture, NativeImage image) {
        return convert(image);
    }

    @Override
    public byte[] convert(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        MapColor[] mapColors = Arrays.stream(getMapColors()).filter(Objects::nonNull).toArray(MapColor[]::new);
        byte[] bytes = new byte[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixelRGBA = image.getPixelRGBA(x, y);

                int R = FastColor.ABGR32.red(pixelRGBA);
                int G = FastColor.ABGR32.green(pixelRGBA);
                int B = FastColor.ABGR32.blue(pixelRGBA);
                int A = FastColor.ABGR32.alpha(pixelRGBA);

                if (A == 0) {
                    bytes[x + y * width] = (byte)MapColor.NONE.id;
                }
                else {
                    byte mapColorIndex = (byte)nearestColor(mapColors, R, G, B, A);
                    bytes[x + y * width] = mapColorIndex;
                }
            }
        }

        return bytes;
    }

    private final double[] shadeCoeffs = { 0.71, 0.86, 1.0, 0.53 };

    private double[] applyShade(double[] color, int shadeIndex) {
        double coeff = shadeCoeffs[shadeIndex];
        return new double[] { color[0] * coeff, color[1] * coeff, color[2] * coeff };
    }

    private int nearestColor(MapColor[] colors, int r, int g, int b, int a) {
        double[] imageVector = { r / 255.0, g / 255.0, b / 255.0 };

        int best_color = 0;
        double lowest_distance = 10000;
        for (int colorIndex = 0; colorIndex < colors.length; colorIndex++) {
            int mapColor = colors[colorIndex].col;
            int mapR = FastColor.ARGB32.red(mapColor);
            int mapG = FastColor.ARGB32.green(mapColor);
            int mapB = FastColor.ARGB32.blue(mapColor);
            double[] mcColorVector = { mapR / 255.0, mapG / 255.0, mapB / 255.0 };

            for (int shadeInd = 0; shadeInd < shadeCoeffs.length; shadeInd++) {
                double distance = distance(imageVector, applyShade(mcColorVector, shadeInd));
                if (distance < lowest_distance) {
                    lowest_distance = distance;
                    if (colorIndex == 0 && a == 255) {
                        best_color = 119;
                    } else {
                        best_color = colorIndex * shadeCoeffs.length + shadeInd;
                    }
                }
            }
        }
        return best_color;
    }

    private double distance(double[] vectorA, double[] vectorB) {
        return Math.sqrt(Math.pow(vectorA[0] - vectorB[0], 2) + Math.pow(vectorA[1] - vectorB[1], 2)
                + Math.pow(vectorA[2] - vectorB[2], 2));
    }
}

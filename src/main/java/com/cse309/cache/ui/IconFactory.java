package com.cse309.cache.ui;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Procedural 16x16 icon generator using JavaFX Canvas snapshots.
 * Powers MenuItems and ToolBar icons without external binary images (Unit 5).
 */
public final class IconFactory {

    public enum IconType {
        SAVE,
        LOAD,
        EXPORT,
        REFRESH,
        CLEAR,
        PURGE,
        START,
        STOP,
        STATS,
        BENCHMARK,
        AWT,
        ABOUT,
        EXIT
    }

    private IconFactory() {
        // Utility class
    }

    public static ImageView createIcon(IconType type) {
        Canvas canvas = new Canvas(16, 16);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        switch (type) {
            case SAVE -> drawSave(gc);
            case LOAD -> drawLoad(gc);
            case EXPORT -> drawExport(gc);
            case REFRESH -> drawRefresh(gc);
            case CLEAR -> drawClear(gc);
            case PURGE -> drawPurge(gc);
            case START -> drawStart(gc);
            case STOP -> drawStop(gc);
            case STATS -> drawStats(gc);
            case BENCHMARK -> drawBenchmark(gc);
            case AWT -> drawAwt(gc);
            case ABOUT -> drawAbout(gc);
            case EXIT -> drawExit(gc);
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = canvas.snapshot(params, null);

        ImageView view = new ImageView(image);
        view.setFitWidth(16);
        view.setFitHeight(16);
        return view;
    }

    private static void drawSave(GraphicsContext gc) {
        gc.setFill(Color.DODGERBLUE);
        gc.fillRect(2, 2, 12, 12);
        gc.setFill(Color.WHITE);
        gc.fillRect(4, 2, 8, 4);
        gc.fillRect(4, 8, 8, 5);
    }

    private static void drawLoad(GraphicsContext gc) {
        gc.setFill(Color.GOLD);
        gc.fillRect(2, 4, 6, 2);
        gc.fillRect(2, 6, 12, 8);
    }

    private static void drawExport(GraphicsContext gc) {
        gc.setStroke(Color.FORESTGREEN);
        gc.setLineWidth(2);
        gc.strokeLine(8, 12, 8, 3);
        gc.strokeLine(4, 7, 8, 3);
        gc.strokeLine(12, 7, 8, 3);
    }

    private static void drawRefresh(GraphicsContext gc) {
        gc.setStroke(Color.MEDIUMPURPLE);
        gc.setLineWidth(2);
        gc.strokeArc(3, 3, 10, 10, 45, 270, javafx.scene.shape.ArcType.OPEN);
        gc.setFill(Color.MEDIUMPURPLE);
        gc.fillPolygon(new double[]{12, 14, 10}, new double[]{2, 6, 6}, 3);
    }

    private static void drawClear(GraphicsContext gc) {
        gc.setFill(Color.CRIMSON);
        gc.fillRect(3, 4, 10, 10);
        gc.setFill(Color.DARKRED);
        gc.fillRect(2, 2, 12, 2);
    }

    private static void drawPurge(GraphicsContext gc) {
        gc.setFill(Color.DARKORANGE);
        gc.fillRect(7, 2, 2, 6);
        gc.fillPolygon(new double[]{3, 13, 8}, new double[]{14, 14, 8}, 3);
    }

    private static void drawStart(GraphicsContext gc) {
        gc.setFill(Color.LIMEGREEN);
        gc.fillPolygon(new double[]{4, 13, 4}, new double[]{3, 8, 13}, 3);
    }

    private static void drawStop(GraphicsContext gc) {
        gc.setFill(Color.RED);
        gc.fillRect(3, 3, 10, 10);
    }

    private static void drawStats(GraphicsContext gc) {
        gc.setFill(Color.TEAL);
        gc.fillRect(2, 10, 3, 5);
        gc.fillRect(6, 6, 3, 9);
        gc.fillRect(10, 2, 3, 13);
    }

    private static void drawBenchmark(GraphicsContext gc) {
        gc.setStroke(Color.SLATEBLUE);
        gc.setLineWidth(2);
        gc.strokeOval(2, 2, 12, 12);
        gc.strokeLine(8, 8, 11, 5);
    }

    private static void drawAwt(GraphicsContext gc) {
        gc.setFill(Color.DARKSLATEGRAY);
        gc.fillRect(2, 2, 12, 12);
        gc.setFill(Color.LIME);
        gc.fillText(">", 4, 10);
    }

    private static void drawAbout(GraphicsContext gc) {
        gc.setFill(Color.ROYALBLUE);
        gc.fillOval(2, 2, 12, 12);
        gc.setFill(Color.WHITE);
        gc.fillRect(7, 7, 2, 5);
        gc.fillOval(7, 4, 2, 2);
    }

    private static void drawExit(GraphicsContext gc) {
        gc.setFill(Color.FIREBRICK);
        gc.fillRect(3, 2, 6, 12);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeLine(9, 8, 14, 8);
        gc.strokeLine(12, 6, 14, 8);
        gc.strokeLine(12, 10, 14, 8);
    }
}

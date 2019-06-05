package io.quarkus.panache.rx.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.RxModelInfo;
import io.reactiverse.axle.pgclient.Row;

public class RxDataTypes {

    public static char getCharacter(Row row, String column) {
        return row.getString(column).charAt(0);
    }

    public static Character getBoxedCharacter(Row row, String column) {
        String string = row.getString(column);
        return string == null ? null : string.charAt(0);
    }

    public static Object storeCharacter(char character) {
        return String.valueOf(character);
    }

    public static Object storeBoxedCharacter(Character character) {
        return character == null ? null : String.valueOf(character);
    }

    public static boolean getBoolean(Row row, String column) {
        return row.getBoolean(column);
    }

    public static Boolean getBoxedBoolean(Row row, String column) {
        return row.getBoolean(column);
    }

    public static byte getByte(Row row, String column) {
        return row.getShort(column).byteValue();
    }

    public static Byte getBoxedByte(Row row, String column) {
        Short s = row.getShort(column);
        return s == null ? null : s.byteValue();
    }

    public static short getShort(Row row, String column) {
        return row.getShort(column);
    }

    public static Short getBoxedShort(Row row, String column) {
        return row.getShort(column);
    }

    public static int getInteger(Row row, String column) {
        return row.getInteger(column);
    }

    public static Integer getBoxedInteger(Row row, String column) {
        return row.getInteger(column);
    }

    public static long getLong(Row row, String column) {
        return row.getLong(column);
    }

    public static Long getBoxedLong(Row row, String column) {
        return row.getLong(column);
    }

    public static float getFloat(Row row, String column) {
        return row.getFloat(column);
    }

    public static Float getBoxedFloat(Row row, String column) {
        return row.getFloat(column);
    }

    public static double getDouble(Row row, String column) {
        return row.getDouble(column);
    }

    public static Double getBoxedDouble(Row row, String column) {
        return row.getDouble(column);
    }

    public static String getString(Row row, String column) {
        return row.getString(column);
    }

    public static <T extends Enum<T>> Enum<T> getEnum(Row row, String column, Enum<T>[] values) {
        Integer index = row.getInteger(column);
        return index == null ? null : values[index];
    }

    public static Object storeEnum(Enum e) {
        return e == null ? null : e.ordinal();
    }

    public static <T extends PanacheRxEntityBase<T>> CompletionStage<T> getManyToOne(Row row, String column,
            RxModelInfo<T> modelInfo) {
        // FIXME: type of ID
        Long id = row.getLong(column);
        if (id == null)
            return CompletableFuture.completedFuture(null);
        return (CompletionStage<T>) RxOperations.deferCompletionStage(() -> RxOperations.findById(modelInfo, id));
    }
}

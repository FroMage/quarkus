package io.quarkus.panache.rx.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.panache.rx.PanacheRxEntityBase;
import io.quarkus.panache.rx.RxModelInfo;
import io.reactiverse.axle.pgclient.Row;
import io.reactiverse.pgclient.data.Numeric;
import io.vertx.axle.core.buffer.Buffer;

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

    public static BigInteger getBigInteger(Row row, String column) {
        Numeric numeric = row.getNumeric(column);
        return numeric == null ? null : numeric.bigIntegerValue();
    }

    public static BigDecimal getBigDecimal(Row row, String column) {
        return row.getBigDecimal(column);
    }

    public static java.sql.Date getSqlDate(Row row, String column) {
        LocalDate localDate = row.getLocalDate(column);
        return localDate == null ? null : java.sql.Date.valueOf(localDate);
    }

    public static Object storeSqlDate(java.sql.Date date) {
        // FIXME: remove unwanted time info?
        return date == null ? null : date.toLocalDate();
    }

    public static java.sql.Time getSqlTime(Row row, String column) {
        LocalTime localTime = row.getLocalTime(column);
        return localTime == null ? null : java.sql.Time.valueOf(localTime);
    }

    public static Object storeSqlTime(java.sql.Time time) {
        // FIXME: remove unwanted time info?
        return time == null ? null : time.toLocalTime();
    }

    public static java.sql.Timestamp getSqlTimestamp(Row row, String column) {
        LocalDateTime localDateTime = row.getLocalDateTime(column);
        return localDateTime == null ? null : java.sql.Timestamp.valueOf(localDateTime);
    }

    public static Object storeSqlTimestamp(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public static java.util.Date getUtilDateAsDate(Row row, String column) {
        return getSqlDate(row, column);
    }

    public static java.util.Date getUtilDateAsTime(Row row, String column) {
        return getSqlTime(row, column);
    }

    public static java.util.Date getUtilDateAsTimestamp(Row row, String column) {
        return getSqlTimestamp(row, column);
    }

    public static Object storeUtilDateAsDate(java.util.Date date) {
        // FIXME: remove unwanted time info?
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Object storeUtilDateAsTime(java.util.Date date) {
        // FIXME: remove unwanted date info?
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
    }

    public static Object storeUtilDateAsTimestamp(java.util.Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static java.util.Calendar getUtilCalendarAsDate(Row row, String column) {
        java.util.Date value = getSqlDate(row, column);
        if (value == null)
            return null;
        Calendar ret = GregorianCalendar.getInstance();
        ret.setTime(value);
        return ret;
    }

    public static java.util.Calendar getUtilCalendarAsTime(Row row, String column) {
        java.util.Date value = getSqlTime(row, column);
        if (value == null)
            return null;
        Calendar ret = GregorianCalendar.getInstance();
        ret.setTime(value);
        return ret;
    }

    public static java.util.Calendar getUtilCalendarAsTimestamp(Row row, String column) {
        java.util.Date value = getSqlTimestamp(row, column);
        if (value == null)
            return null;
        Calendar ret = GregorianCalendar.getInstance();
        ret.setTime(value);
        return ret;
    }

    public static Object storeUtilCalendarAsDate(java.util.Calendar date) {
        // FIXME: remove unwanted time info?
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Object storeUtilCalendarAsTime(java.util.Calendar date) {
        // FIXME: remove unwanted date info?
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
    }

    public static Object storeUtilCalendarAsTimestamp(java.util.Calendar date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalTime getLocalTime(Row row, String column) {
        return row.getLocalTime(column);
    }

    public static LocalDateTime getLocalDateTime(Row row, String column) {
        return row.getLocalDateTime(column);
    }

    public static LocalDate getLocalDate(Row row, String column) {
        return row.getLocalDate(column);
    }

    public static OffsetTime getOffsetTime(Row row, String column) {
        return row.getOffsetTime(column);
    }

    public static OffsetDateTime getOffsetDateTime(Row row, String column) {
        return row.getOffsetDateTime(column);
    }

    public static byte[] getPrimitiveByteArray(Row row, String column) {
        Buffer buffer = row.getBuffer(column);
        return buffer == null ? null : buffer.getBytes();
    }

    public static Object storePrimitiveByteArray(byte[] value) {
        // FIXME: need to get the delegate because we do Tuple.addValue() which does not unwrap:
        // https://github.com/eclipse-vertx/vertx-sql-client/issues/333
        return value == null ? null : Buffer.buffer(value).getDelegate();
    }

    public static Byte[] getBoxedByteArray(Row row, String column) {
        Buffer buffer = row.getBuffer(column);
        if(buffer == null)
            return null;
        Byte[] ret = new Byte[buffer.length()];
        for(int i=0;i<buffer.length();i++)
            ret[i] = buffer.getByte(i);
        return ret;
    }

    public static Object storeBoxedByteArray(Byte[] value) {
        if(value == null)
            return null;
        Buffer buffer = Buffer.buffer(value.length);
        for (Byte b : value) {
            buffer.appendByte(b);
        }
        // FIXME: need to get the delegate because we do Tuple.addValue() which does not unwrap:
        // https://github.com/eclipse-vertx/vertx-sql-client/issues/333
        return buffer.getDelegate();
    }

    public static char[] getPrimitiveCharArray(Row row, String column) {
        String string = row.getString(column);
        return string == null ? null : string.toCharArray();
    }

    public static Object storePrimitiveCharArray(char[] value) {
        return value == null ? null : String.valueOf(value);
    }

    public static Character[] getBoxedCharArray(Row row, String column) {
        String buffer = row.getString(column);
        if(buffer == null)
            return null;
        Character[] ret = new Character[buffer.length()];
        for(int i=0;i<buffer.length();i++)
            ret[i] = buffer.charAt(i);
        return ret;
    }

    public static Object storeBoxedCharArray(Character[] value) {
        if(value == null)
            return null;
        if(value.length == 0)
            return "";
        StringBuilder b = new StringBuilder(value.length);
        for (Character c : value) {
            b.append(c);
        }
        return b.toString();
    }

}

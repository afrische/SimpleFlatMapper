package org.sfm.map.column.joda;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.sfm.map.column.DateFormatProperty;
import org.sfm.map.column.FieldMapperColumnDefinition;
import org.sfm.map.column.TimeZoneProperty;

import java.util.TimeZone;

import static org.junit.Assert.*;

public class JodaHelperTest {

    private static final DateTimeZone CHICAGO_TZ = DateTimeZone.forID("America/Chicago");
    private static final DateTimeZone NY_TZ = DateTimeZone.forID("America/New_York");

    @SuppressWarnings("EmptyCatchBlock")
    @Test
    public void testFormatterFailWhenEmpty() {
        try {
            JodaHelper.getDateTimeFormatter(FieldMapperColumnDefinition.identity());
            fail();
        } catch(IllegalArgumentException e) {}
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testFormatterFromString() {
        final DateTimeFormatter yyyyMMdd = JodaHelper.getDateTimeFormatter(FieldMapperColumnDefinition.identity().add(new DateFormatProperty("yyyyMMdd")));
        final long instant = System.currentTimeMillis();
        assertEquals(DateTimeFormat.forPattern("yyyyMMdd").print(instant), yyyyMMdd.print(instant));
        assertEquals(DateTimeZone.getDefault(), yyyyMMdd.getZone());
    }

    @Test
    public void testFormatterFromFormatter() {
        final DateTimeFormatter yyyyMMdd = JodaHelper.getDateTimeFormatter(FieldMapperColumnDefinition.identity().add(new JodaDateTimeFormatterProperty(DateTimeFormat.forPattern("MMddyyyy"))));
        final long instant = System.currentTimeMillis();
        assertEquals(DateTimeFormat.forPattern("MMddyyyy").print(instant), yyyyMMdd.print(instant));
        assertEquals(DateTimeZone.getDefault(), yyyyMMdd.getZone());

    }

    @Test
    public void testFormatterFromFormatterWithOwnTZ() {
        final DateTimeFormatter yyyyMMdd = JodaHelper.getDateTimeFormatter(FieldMapperColumnDefinition.identity().add(new JodaDateTimeFormatterProperty(DateTimeFormat.forPattern("ddMMyyyy").withZone(CHICAGO_TZ))));
        final long instant = System.currentTimeMillis();
        assertEquals(DateTimeFormat.forPattern("ddMMyyyy").withZone(CHICAGO_TZ).print(instant), yyyyMMdd.print(instant));
        assertEquals(CHICAGO_TZ, yyyyMMdd.getZone());
    }


    @Test
    public void testFormatterFromFormatterWithSpecifiedTZ() {
        final DateTimeFormatter yyyyMMdd = JodaHelper.getDateTimeFormatter(FieldMapperColumnDefinition.identity().add(new JodaDateTimeFormatterProperty(DateTimeFormat.forPattern("ddMMyyyy").withZone(CHICAGO_TZ))).add(new TimeZoneProperty(TimeZone.getTimeZone("America/New_York"))));
        final long instant = System.currentTimeMillis();
        assertEquals(DateTimeFormat.forPattern("ddMMyyyy").withZone(NY_TZ).print(instant), yyyyMMdd.print(instant));
        assertEquals(NY_TZ, yyyyMMdd.getZone());
    }
    @Test
    public void testGetDateTimeZoneWithNone() {
        assertEquals(DateTimeZone.getDefault(), JodaHelper.getDateTimeZoneOrDefault(FieldMapperColumnDefinition.identity()));
    }

    @Test
    public void testGetDateTimeZoneFromTimeZone() {
        assertEquals(CHICAGO_TZ, JodaHelper.getDateTimeZoneOrDefault(FieldMapperColumnDefinition.identity().add(new TimeZoneProperty(TimeZone.getTimeZone("America/Chicago")))));
    }

    @Test
    public void testGetDateTimeZoneFromDateTimeZone() {
        assertEquals(CHICAGO_TZ, JodaHelper.getDateTimeZoneOrDefault(FieldMapperColumnDefinition.identity().add(new JodaDateTimeZoneProperty(CHICAGO_TZ))));
    }


    @Test
    public void testGetDateTimeZoneFromParams() {
        assertEquals(CHICAGO_TZ, JodaHelper.getDateTimeZoneOrDefault(new Object[]{FieldMapperColumnDefinition.identity().add(new TimeZoneProperty(TimeZone.getTimeZone("America/Chicago")))}));
        assertEquals(CHICAGO_TZ, JodaHelper.getDateTimeZoneOrDefault(new Object[]{FieldMapperColumnDefinition.identity().add(new JodaDateTimeZoneProperty(CHICAGO_TZ))}));
        assertEquals(CHICAGO_TZ, JodaHelper.getDateTimeZoneOrDefault(new Object[]{TimeZone.getTimeZone("America/Chicago")}));
        assertEquals(CHICAGO_TZ, JodaHelper.getDateTimeZoneOrDefault(new Object[]{CHICAGO_TZ}));
        assertEquals(CHICAGO_TZ, JodaHelper.getDateTimeZoneOrDefault(new Object[]{new TimeZoneProperty(TimeZone.getTimeZone("America/Chicago"))}));
        assertEquals(CHICAGO_TZ, JodaHelper.getDateTimeZoneOrDefault(new Object[]{new JodaDateTimeZoneProperty(CHICAGO_TZ)}));
    }

}
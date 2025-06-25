package be.cytomine.appengine.unit.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import be.cytomine.appengine.utils.units.Unit;

public class UnitTest {
    @Test
    public void testParseValidByteUnits() {
        Unit unit = new Unit("1kB");
        Assertions.assertEquals(1000, unit.getBytes());
        Assertions.assertEquals(8000, unit.getBits());

        unit = new Unit("1MiB");
        Assertions.assertEquals(1024 * 1024, unit.getBytes());
        Assertions.assertEquals(8 * 1024 * 1024, unit.getBits());

        unit = new Unit("1GByte");
        Assertions.assertEquals(1000 * 1000 * 1000, unit.getBytes());
        Assertions.assertEquals(8 * 1000 * 1000 * 1000L, unit.getBits());
    }

    @Test
    public void testParseValidBitUnits() {
        Unit unit = new Unit("1kb");
        Assertions.assertEquals(125, unit.getBytes());
        Assertions.assertEquals(1000, unit.getBits());

        unit = new Unit("1Mibit");
        Assertions.assertEquals(131072, unit.getBytes());
        Assertions.assertEquals(1048576, unit.getBits());

        unit = new Unit("1Gbit");
        Assertions.assertEquals(125000000, unit.getBytes());
        Assertions.assertEquals(1000000000, unit.getBits());
    }

    @Test
    public void testInvalidUnits() {
        Assertions.assertFalse(Unit.isValid("1XYZ"));
        Assertions.assertFalse(Unit.isValid("123"));
        Assertions.assertFalse(Unit.isValid("GByte"));
        Assertions.assertFalse(Unit.isValid("1.5.5GB"));
        Assertions.assertFalse(Unit.isValid("1 K B"));
    }

    @Test
    public void testValidUnits() {
        Assertions.assertTrue(Unit.isValid("1kB"));
        Assertions.assertTrue(Unit.isValid("1KiB"));
        Assertions.assertTrue(Unit.isValid("1.5 MB"));
        Assertions.assertTrue(Unit.isValid("1.5MiB"));
        Assertions.assertTrue(Unit.isValid("0.5GiByte"));
        Assertions.assertTrue(Unit.isValid("1Tbit"));
    }

    @Test
    public void testGetMultiplierForDecimal() {
        Unit unit = new Unit("1kB");
        Assertions.assertEquals(1000, unit.getBytes());
        Assertions.assertEquals(8000, unit.getBits());
    }

    @Test
    public void testGetMultiplierForBinary() {
        Unit unit = new Unit("1KiB");
        Assertions.assertEquals(1024, unit.getBytes());
        Assertions.assertEquals(8192, unit.getBits());
    }

    @Test
    public void testZeroValue() {
        Unit unit = new Unit("0B");
        Assertions.assertEquals(0, unit.getBytes());
        Assertions.assertEquals(0, unit.getBits());
    }

    @Test
    public void testFractionalValue() {
        Unit unit = new Unit("1.5MB");
        Assertions.assertEquals(1500000, unit.getBytes());
        Assertions.assertEquals(12000000, unit.getBits());

        unit = new Unit("1.5MiB");
        Assertions.assertEquals(1572864, unit.getBytes());
        Assertions.assertEquals(12582912, unit.getBits());
    }
}

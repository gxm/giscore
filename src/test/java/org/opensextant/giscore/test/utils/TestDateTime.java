package org.opensextant.giscore.test.utils;

import org.junit.Assert;
import org.junit.Test;
import org.opensextant.giscore.utils.DateTime.DateTimeType;
import org.opensextant.giscore.utils.DateTime;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;

/**
 * Tests for DateTime
 * @author Jason Mathews, MITRE Corp.
 */
public class TestDateTime {

	@Test
	public void testDateTime() {

		// allow lax date parsing
		// dateTime (YYYY-MM-DDThh:mm:ssZ) -> (YYYY-MM-DDThh:mm[:ss][Z])
		String[] timestamps = {
				"500",                  "0500", 		// gYear (YYYY)
				"2001",                 "2001", 		// gYear (YYYY)
				"3500",                 "3500", 		// gYear (YYYY)
				"12345",                "12345", 		// gYear (YYYY)
				"2009-03",				"2009-03",		// gYearMonth (YYYY-MM)
				"2009-03-14",			"2009-03-14",	// Date yyyy-MM-dd
				"2009-03-14T21:06:45",  "2009-03-14T21:06:45.000Z", // DateTime 'Z' suffix omitted
				"2009-03-14T21:06Z",    "2009-03-14T21:06:00.000Z", // DateTime seconds field omitted
				"2009-03-14T21:06",     "2009-03-14T21:06:00.000Z", // DateTime seconds + 'Z' suffix omitted
		};
		// does not handle alternate time zones format with missing second field: e.g. 2009-03-14T16:10-05:00
		for (int i = 0; i < timestamps.length; i += 2) {
			DateTime date = null;
			try {
				date = new DateTime(timestamps[i]);
			} catch (ParseException e) {
				Assert.fail("failed to parse " + timestamps[i]);
			}
			assertEquals(timestamps[i+1], date.toString());
			if (date.getType() == DateTimeType.gYear) {
				Assert.assertTrue(date.toString(DateTimeType.dateTime).endsWith("-01-01T00:00:00.000Z"));
			} else if (date.getType() != DateTimeType.dateTime) {
				Assert.assertTrue(date.toString(DateTimeType.dateTime).endsWith("T00:00:00.000Z"));
			}
			System.out.format("DEBUG %s => %s%n", timestamps[i], timestamps[i + 1]);
		}
	}

	@Test
	public void testUTCTime() {
		DateTime date = new DateTime();
		String utc1 = date.toString(DateTimeType.dateTime);
		String utc2 = date.toUTCString();
		Assert.assertEquals(utc1, utc2);
	}

	@Test
	public void testClone() {
		DateTime date = new DateTime(0);
		Assert.assertEquals(date, date.clone());
	}

	@Test(expected = ParseException.class)
	public void testBadDate() throws ParseException {
		new DateTime("abc");
	}

	@Test(expected = ParseException.class)
	public void testNullDate() throws ParseException {
		new DateTime((String)null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullType() {
		new DateTime(0, null);
	}

}
